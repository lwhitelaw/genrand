package net.liamw.genrand.function;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import net.liamw.genrand.util.Avalanche32.Diffuser;
import net.liamw.genrand.util.LWRand64;

/**
 * A 32 bit bijective permutation function with manipulatable operators. Instances are JIT compiled for speed.
 */
public class Mix32 implements Diffuser {
	/**
	 * Valid operations to be applied.
	 */
	public enum Operand {
		/**
		 * Add a constant.
		 */
		ADD,
		/**
		 * XOR-combine a constant.
		 */
		XOR,
		/**
		 * Multiply by a constant.
		 */
		MUL,
		/**
		 * Rotate value left by a constant number of positions.
		 */
		ROL,
		/**
		 * Rotate value right by a constant number of positions.
		 */
		ROR,
		/**
		 * XOR-combine value with a left-shifted copy of itself (xorshift left).
		 */
		XSL,
		/**
		 * XOR-combine value with a right-shifted copy of itself (xorshift right).
		 */
		XSR;
	}
	
	/**
	 * A mix operation.
	 */
	public record MixEntry(Operand op, int arg) {};
	
	/**
	 * List of operations to apply in order.
	 */
	private final List<MixEntry> operands = new ArrayList<MixEntry>();
	
	/**
	 * JIT-compiled function. Null if not compiled yet.
	 */
	private Diffuser compiled = null;
	
	/**
	 * Construct a function with no operators.
	 */
	public Mix32() {}
	
	/**
	 * Construct a function that is a copy of the provided function.
	 * @param other function to copy
	 */
	public Mix32(Mix32 other) {
		operands.addAll(other.operands);
		compiled = other.compiled;
	}
	
	@Override
	public int diffuse(int input) {
		// If not yet compiled, do so and run the interpreted version this time.
		if (compiled == null) {
			compiled = compileClass();
			// Start with the original value
			int v = input;
			// Apply each operation in order
			for (MixEntry e : operands) {
				switch (e.op) {
					case ADD: v += e.arg; break;
					case XOR: v ^= e.arg; break;
					case MUL: v *= e.arg; break;
					case ROL: v = Integer.rotateLeft(v, e.arg); break;
					case ROR: v = Integer.rotateRight(v, e.arg); break;
					case XSL: v ^= (v << e.arg); break;
					case XSR: v ^= (v >>> e.arg); break;
				}
			}
			return v;
		} else {
			// Run the compiled code version
			return compiled.diffuse(input);
		}
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (MixEntry e : operands) {
			switch (e.op) {
				case ADD: sb.append(String.format("v += 0x%08X;\n", e.arg())); break;
				case XOR: sb.append(String.format("v ^= 0x%08X;\n", e.arg())); break;
				case MUL: sb.append(String.format("v *= 0x%08X;\n", e.arg())); break;
				case ROL: sb.append(String.format("v = Integer.rotateLeft(v,%d);\n", e.arg())); break;
				case ROR: sb.append(String.format("v = Integer.rotateRight(v,%d);\n", e.arg())); break;
				case XSL: sb.append(String.format("v ^= v << %d;\n", e.arg())); break;
				case XSR: sb.append(String.format("v ^= v >>> %d;\n", e.arg())); break;
			}
		}
		return sb.toString();
	}
	
	/**
	 * Return the number of operators in this function.
	 * @return the number of operators
	 */
	public int oplen() {
		return operands.size();
	}
	
	/**
	 * Return the operator list.
	 * @return the operator list
	 */
	public List<MixEntry> getOperands() {
		return operands;
	}
	
	/**
	 * Create a random operator from among the given operator types.
	 * @param list The list of valid operator types to choose from
	 * @return a new random operator
	 */
	public static MixEntry randomMixEntry(Operand... list) {
		if (list.length == 0) throw new IllegalArgumentException("Must specify at least one operator type");
		RandomGenerator tlr = LWRand64.threadLocal();
		// get a random operator type
		Operand op = list[tlr.nextInt(list.length)];
		// get a random integer to become the operator argument
		int arg = tlr.nextInt();
		// adjust arg based on specific needs of operator
		switch (op) {
			case ADD: break; // no special needs
			case XOR: break; // no special needs
			case MUL: arg = (arg << 1) | 1; break; // odd value
			case ROL: arg = (arg & 0x1F); if (arg == 0) arg = 1; break; // valid nonzero shift value
			case ROR: arg = (arg & 0x1F); if (arg == 0) arg = 1; break; // valid nonzero shift value
			case XSL: arg = (arg & 0x1F); if (arg == 0) arg = 1; break; // valid nonzero shift value
			case XSR: arg = (arg & 0x1F); if (arg == 0) arg = 1; break; // valid nonzero shift value
		}
		return new MixEntry(op,arg);
	}
	
	/**
	 * Add a random operator from among the given operator types.
	 * @param list The list of valid operator types to choose from
	 */
	public void addRandom(Operand... list) {
		operands.add(randomMixEntry(list));
		compiled = null;
	}
	
	/**
	 * Replace a random operator from this function with another random operator.
	 */
	public void replaceRandom(Operand... list) {
		if (operands.size() == 0) return;
		RandomGenerator tlr = LWRand64.threadLocal();
		int which = tlr.nextInt(operands.size());
		operands.set(which, randomMixEntry(list));
		compiled = null;
	}
	
	/**
	 * Remove a random operator.
	 */
	public void removeRandom() {
		if (operands.size() == 0) return;
		RandomGenerator tlr = LWRand64.threadLocal();
		int which = tlr.nextInt(operands.size());
		operands.remove(which);
		compiled = null;
	}
	
	/**
	 * Compile this function to a new diffuser instance.
	 * @return a compiled version of this function
	 */
	private Diffuser compileClass() {
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		String packageName = getClass().getPackageName().replace('.', '/');
		// Create a class with an anonymous name - it'll be loaded as a hidden class so the name isn't important
		// The package is important, though.
		writer.visit(Opcodes.V19, 0, packageName + "/Anon", null, "java/lang/Object", new String[] { "net/liamw/genrand/util/Avalanche32$Diffuser" });
		// constructor - does nothing but call the superclass constructor
		{
			MethodVisitor ctor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
			ctor.visitCode();
			
			ctor.visitVarInsn(Opcodes.ALOAD,0);
			ctor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
			ctor.visitInsn(Opcodes.RETURN);
			
			ctor.visitMaxs(0, 0);
			ctor.visitEnd();
		}
		// long diffuse(long v)
		{
			MethodVisitor mth = writer.visitMethod(Opcodes.ACC_PUBLIC, "diffuse", "(I)I", null, null);
			mth.visitCode();
			
			// 0: this
			// 1: arg c
			// 2: int v
			
			// int v = c;
			mth.visitVarInsn(Opcodes.ILOAD,1);
			mth.visitVarInsn(Opcodes.ISTORE,2);
			
			// handle ops
			
			for (MixEntry e : operands) {
				switch (e.op) {
					case ADD:
						mth.visitVarInsn(Opcodes.ILOAD,2);
						mth.visitLdcInsn(e.arg());
						mth.visitInsn(Opcodes.IADD);
						mth.visitVarInsn(Opcodes.ISTORE,2);
						break;
					case XOR:
						mth.visitVarInsn(Opcodes.ILOAD,2);
						mth.visitLdcInsn(e.arg());
						mth.visitInsn(Opcodes.IXOR);
						mth.visitVarInsn(Opcodes.ISTORE,2);
						break;
					case MUL:
						mth.visitVarInsn(Opcodes.ILOAD,2);
						mth.visitLdcInsn(e.arg());
						mth.visitInsn(Opcodes.IMUL);
						mth.visitVarInsn(Opcodes.ISTORE,2);
						break;
					case ROL:
						mth.visitVarInsn(Opcodes.ILOAD,2);
						mth.visitIntInsn(Opcodes.BIPUSH, e.arg());
						mth.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "rotateLeft","(II)I", false);
						mth.visitVarInsn(Opcodes.ISTORE,2);
						break;
					case ROR:
						mth.visitVarInsn(Opcodes.ILOAD,2);
						mth.visitIntInsn(Opcodes.BIPUSH, e.arg());
						mth.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "rotateRight","(II)I", false);
						mth.visitVarInsn(Opcodes.ISTORE,2);
						break;
					case XSL:
						mth.visitVarInsn(Opcodes.ILOAD,2);
						mth.visitInsn(Opcodes.DUP);
						mth.visitIntInsn(Opcodes.BIPUSH, e.arg());
						mth.visitInsn(Opcodes.ISHL);
						mth.visitInsn(Opcodes.IXOR);
						mth.visitVarInsn(Opcodes.ISTORE,2);
						break;
					case XSR:
						mth.visitVarInsn(Opcodes.ILOAD,2);
						mth.visitInsn(Opcodes.DUP);
						mth.visitIntInsn(Opcodes.BIPUSH, e.arg());
						mth.visitInsn(Opcodes.IUSHR);
						mth.visitInsn(Opcodes.IXOR);
						mth.visitVarInsn(Opcodes.ISTORE,2);
						break;
					default:
						throw new AssertionError("unimplemented opcode");
				}
			}
			
			// return v;
			mth.visitVarInsn(Opcodes.ILOAD,2);
			mth.visitInsn(Opcodes.IRETURN);
			
			mth.visitMaxs(0, 0);
			mth.visitEnd();
		}
		writer.visitEnd();
		// Get the classfile bytes and load them
		byte[] data = writer.toByteArray();
		try {
			// Define the hidden class and acquire a handle lookup
			MethodHandles.Lookup lookup = MethodHandles.lookup().defineHiddenClass(data, true);
			// Construct the class and return
			return (Diffuser) lookup.findConstructor(lookup.lookupClass(), MethodType.methodType(void.class)).invoke();
		} catch (Throwable ex) {
			throw new Error(ex);
		}
	}
}
