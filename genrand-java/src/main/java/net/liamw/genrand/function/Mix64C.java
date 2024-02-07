package net.liamw.genrand.function;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import net.liamw.genrand.util.LWRand64;
import net.liamw.genrand.util.Avalanche64.Diffuser64;

public class Mix64C implements Diffuser64 {
	/**
	 * Valid operations to be applied.
	 */
	public enum Operand {
		/**
		 * Add the right half rotated by constant into the left half
		 */
		LADDROLR,
		/**
		 * Bitwise XOR the right half rotated by constant into the left half
		 */
		LXORROLR,
		/**
		 * Subtract the right half rotated by constant into the left half
		 */
		LSUBROLR
	}
	
	/**
	 * A mix operation.
	 */
	public record MixEntry(Operand op, long arg) {};
	
	/**
	 * List of operations to apply in order.
	 */
	private List<MixEntry> operands = new ArrayList<MixEntry>();
	
	/**
	 * JIT-compiled function. Null if not compiled yet.
	 */
	private Diffuser64 compiled = null;
	
	/**
	 * Construct a function with no operators.
	 */
	public Mix64C() {}
	
	/**
	 * Construct a function that is a copy of the provided function.
	 * @param other function to copy
	 */
	public Mix64C(Mix64C other) {
		operands.addAll(other.operands);
		compiled = other.compiled;
	}
	
	@Override
	public long diffuse(long input) {
		// If not yet compiled, do so and run the interpreted version this time.
		if (compiled == null) {
			compiled = compileClass();
			// Start with the original value
			long v = input;
			int a = (int)(v >>> 32);
			int b = (int)(v);
			// Apply each operation in order
			for (MixEntry e : operands) {
				// apply operation
				switch (e.op) {
					case LADDROLR: a += Integer.rotateLeft(b, (int)e.arg); break;
					case LXORROLR: a ^= Integer.rotateLeft(b, (int)e.arg); break;
					case LSUBROLR: a -= Integer.rotateLeft(b, (int)e.arg); break;
				}
				// swap halves
				int t = a;
				a = b;
				b = t;
			}
			return ((a & 0xFFFFFFFFL) << 32) | (b & 0xFFFFFFFFL);
		} else {
			// Run the compiled code version
			return compiled.diffuse(input);
		}
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (MixEntry e : operands) {
			switch (e.op) {
				case LADDROLR: sb.append(String.format("a += Integer.rotateLeft(b,%d); t = a; a = b; b = t;\n", e.arg())); break;
				case LXORROLR: sb.append(String.format("a ^= Integer.rotateLeft(b,%d); t = a; a = b; b = t;\n", e.arg())); break;
				case LSUBROLR: sb.append(String.format("a -= Integer.rotateLeft(b,%d); t = a; a = b; b = t;\n", e.arg())); break;
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
		// they're all rotates so this is easy
		long arg = tlr.nextLong(32);
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
	 * Replace the operator at this index with another random operator.
	 */
	public void replaceRandomAt(int index, Operand... list) {
		if (operands.size() == 0) return;
		operands.set(index, randomMixEntry(list));
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
	private Diffuser64 compileClass() {
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		String packageName = getClass().getPackageName().replace('.', '/');
		// Create a class with an anonymous name - it'll be loaded as a hidden class so the name isn't important
		// The package is important, though.
		writer.visit(Opcodes.V19, 0, packageName + "/Anon", null, "java/lang/Object", new String[] { "net/liamw/genrand/util/Avalanche64$Diffuser64" });
		// constructor
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
			MethodVisitor mth = writer.visitMethod(Opcodes.ACC_PUBLIC, "diffuse", "(J)J", null, null);
			mth.visitCode();
			
			// 0: this
			// 1: arg c
			// 2: int a
			// 3: int b
			final int THIS = 0;
			final int ARG_C = 1;
			final int INT_A = 3;
			final int INT_B = 4;
			
			// int a = (int)(c >>> 32);
			mth.visitVarInsn(Opcodes.LLOAD,ARG_C);
			mth.visitLdcInsn(32);
			mth.visitInsn(Opcodes.LUSHR);
			mth.visitInsn(Opcodes.L2I);
			mth.visitVarInsn(Opcodes.ISTORE,INT_A);
			// int b = (int)(c);
			mth.visitVarInsn(Opcodes.LLOAD,ARG_C);
			mth.visitInsn(Opcodes.L2I);
			mth.visitVarInsn(Opcodes.ISTORE,INT_B);
			
			// handle ops
			
			for (MixEntry e : operands) {
				switch (e.op) {
					case LADDROLR:
						mth.visitVarInsn(Opcodes.ILOAD,INT_A);
						mth.visitVarInsn(Opcodes.ILOAD,INT_B);
						mth.visitIntInsn(Opcodes.BIPUSH,(int)e.arg());
						mth.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "rotateLeft","(II)I", false);
						mth.visitInsn(Opcodes.IADD);
						mth.visitVarInsn(Opcodes.ISTORE,INT_A);
						break;
					case LXORROLR:
						mth.visitVarInsn(Opcodes.ILOAD,INT_A);
						mth.visitVarInsn(Opcodes.ILOAD,INT_B);
						mth.visitIntInsn(Opcodes.BIPUSH,(int)e.arg());
						mth.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "rotateLeft","(II)I", false);
						mth.visitInsn(Opcodes.IXOR);
						mth.visitVarInsn(Opcodes.ISTORE,INT_A);
						break;
					case LSUBROLR:
						mth.visitVarInsn(Opcodes.ILOAD,INT_A);
						mth.visitVarInsn(Opcodes.ILOAD,INT_B);
						mth.visitIntInsn(Opcodes.BIPUSH,(int)e.arg());
						mth.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "rotateLeft","(II)I", false);
						mth.visitInsn(Opcodes.ISUB);
						mth.visitVarInsn(Opcodes.ISTORE,INT_A);
						break;
					default:
						throw new AssertionError("unimplemented opcode");
				}
				
				// swap A and B - <no direct Java analogue)
				mth.visitVarInsn(Opcodes.ILOAD,INT_A);
				mth.visitVarInsn(Opcodes.ILOAD,INT_B);
				mth.visitVarInsn(Opcodes.ISTORE,INT_A);
				mth.visitVarInsn(Opcodes.ISTORE,INT_B);
			}
			
			// return ((a & 0xFFFFFFFFL) << 32) | (b & 0xFFFFFFFFL);
			mth.visitVarInsn(Opcodes.ILOAD,INT_A);
			mth.visitInsn(Opcodes.I2L);
			mth.visitLdcInsn(0xFFFFFFFFL);
			mth.visitInsn(Opcodes.LAND);
			mth.visitIntInsn(Opcodes.BIPUSH, 32);
			mth.visitInsn(Opcodes.LSHL);
			
			mth.visitVarInsn(Opcodes.ILOAD,INT_B);
			mth.visitInsn(Opcodes.I2L);
			mth.visitLdcInsn(0xFFFFFFFFL);
			mth.visitInsn(Opcodes.LAND);
			
			mth.visitInsn(Opcodes.LOR);
			
			mth.visitInsn(Opcodes.LRETURN);
			
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
			return (Diffuser64) lookup.findConstructor(lookup.lookupClass(), MethodType.methodType(void.class)).invoke();
		} catch (Throwable ex) {
			throw new Error(ex);
		}
	}
}
