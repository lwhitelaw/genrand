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

import net.liamw.genrand.util.LWRand64;
import net.liamw.genrand.util.Avalanche64.Diffuser64;

/**
 * A 64 bit bijective permutation function with manipulatable operators. Instances are JIT compiled for speed.
 */
public class Mix64 implements Diffuser64 {
	/**
	 * Valid operations to be applied.
	 */
	enum Operand {
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
	record MixEntry(Operand op, long arg) {};
	
	/**
	 * List of operations to apply in order.
	 */
	private List<MixEntry> operands = new ArrayList<MixEntry>();
	
	/**
	 * JIT-compiled function. Null if not compiled yet.
	 */
	private Diffuser64 compiled = null;
	
	@Override
	public long diffuse(long input) {
		// If not yet compiled, do so and run the interpreted version this time.
		if (compiled == null) {
			compiled = compileClass();
			// Start with the original value
			long v = input;
			// Apply each operation in order
			for (MixEntry e : operands) {
				switch (e.op) {
					case ADD: v += e.arg; break;
					case XOR: v ^= e.arg; break;
					case MUL: v *= e.arg; break;
					case ROL: v = Long.rotateLeft(v, (int) e.arg); break;
					case ROR: v = Long.rotateRight(v, (int) e.arg); break;
					case XSL: v ^= (v << ((int) e.arg)); break;
					case XSR: v ^= (v >>> ((int) e.arg)); break;
				}
			}
			return v;
		} else {
			// Run the compiled code version
			return compiled.diffuse(input);
		}
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (MixEntry e : operands) {
			switch (e.op) {
				case ADD: sb.append(String.format("v += 0x%016XL;\n", e.arg())); break;
				case XOR: sb.append(String.format("v ^= 0x%016XL;\n", e.arg())); break;
				case MUL: sb.append(String.format("v *= 0x%016XL;\n", e.arg())); break;
				case ROL: sb.append(String.format("v = Long.rotateLeft(v,%d);\n", e.arg())); break;
				case ROR: sb.append(String.format("v = Long.rotateRight(v,%d);\n", e.arg())); break;
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
		long arg = tlr.nextLong();
		// adjust arg based on specific needs of operator
		switch (op) {
			case ADD: arg = (arg << 1) | 1; break; // odd value
			case XOR: break; // no special needs
			case MUL: arg = (arg << 1) | 1; break; // odd value
			case ROL: arg = (arg & 0x3F); if (arg == 0) arg = 1; break; // valid nonzero shift value
			case ROR: arg = (arg & 0x3F); if (arg == 0) arg = 1; break; // valid nonzero shift value
			case XSL: arg = (arg & 0x3F); if (arg == 0) arg = 1; break; // valid nonzero shift value
			case XSR: arg = (arg & 0x3F); if (arg == 0) arg = 1; break; // valid nonzero shift value
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
	 * Create a counter-based PRNG from this function.
	 * @return a random number generator constructed from this function
	 */
	public Random asRandom() {
		return new Random() {
			long c = LWRand64.threadLocal().nextLong(); // counter
			
			long value;
			int haveBits;
			
			public void advance() {
				c++;
			}
			
			public int next(int bits) {
				// refill if needed
				if (haveBits == 0) {
					advance();
					value = mix(c);
					haveBits = 64;
				}
				// extract
				int value32 = (int)(value & 0xFFFFFFFFL);
				// remove from 64bit value for accounting
				value = (value >>> 32);
				haveBits -= 32;
				// return
				return value32 >>> (32 - bits);
			}

			private long mix(long c) {
				return diffuse(c);
			}
		};
	}
	
	/**
	 * Create a chaotic PRNG from this function.
	 * @return a random number generator constructed from this function
	 */
	public Random asRandomChaotic() {
		return new Random() {
			long c = LWRand64.threadLocal().nextLong(); // counter
			
			long value;
			int haveBits;
			
			public void advance() {
				c = mix(c);
			}
			
			public int next(int bits) {
				// refill if needed
				if (haveBits == 0) {
					advance();
					value = c;
					haveBits = 64;
				}
				// extract
				int value32 = (int)(value & 0xFFFFFFFFL);
				// remove from 64bit value for accounting
				value = (value >>> 32);
				haveBits -= 32;
				// return
				return value32 >>> (32 - bits);
			}

			private long mix(long c) {
				return diffuse(c);
			}
		};
	}
	
	/**
	 * Compile this function to a new diffuser instance.
	 * @return a compiled version of this function
	 */
	public Diffuser64 compileClass() {
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
			// 3: long v
			
			// long v = c;
			mth.visitVarInsn(Opcodes.LLOAD,1);
			mth.visitVarInsn(Opcodes.LSTORE,3);
			
			// handle ops
			
			for (MixEntry e : operands) {
				switch (e.op) {
					case ADD:
						mth.visitVarInsn(Opcodes.LLOAD,3);
						mth.visitLdcInsn(e.arg());
						mth.visitInsn(Opcodes.LADD);
						mth.visitVarInsn(Opcodes.LSTORE,3);
						break;
					case XOR:
						mth.visitVarInsn(Opcodes.LLOAD,3);
						mth.visitLdcInsn(e.arg());
						mth.visitInsn(Opcodes.LXOR);
						mth.visitVarInsn(Opcodes.LSTORE,3);
						break;
					case MUL:
						mth.visitVarInsn(Opcodes.LLOAD,3);
						mth.visitLdcInsn(e.arg());
						mth.visitInsn(Opcodes.LMUL);
						mth.visitVarInsn(Opcodes.LSTORE,3);
						break;
					case ROL:
						mth.visitVarInsn(Opcodes.LLOAD,3);
						mth.visitIntInsn(Opcodes.BIPUSH, (int)e.arg());
						mth.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "rotateLeft","(JI)J", false);
						mth.visitVarInsn(Opcodes.LSTORE,3);
						break;
					case ROR:
						mth.visitVarInsn(Opcodes.LLOAD,3);
						mth.visitIntInsn(Opcodes.BIPUSH, (int)e.arg());
						mth.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "rotateRight","(JI)J", false);
						mth.visitVarInsn(Opcodes.LSTORE,3);
						break;
					case XSL:
						mth.visitVarInsn(Opcodes.LLOAD,3);
						mth.visitInsn(Opcodes.DUP2);
						mth.visitIntInsn(Opcodes.BIPUSH, (int)e.arg());
						mth.visitInsn(Opcodes.LSHL);
						mth.visitInsn(Opcodes.LXOR);
						mth.visitVarInsn(Opcodes.LSTORE,3);
						break;
					case XSR:
						mth.visitVarInsn(Opcodes.LLOAD,3);
						mth.visitInsn(Opcodes.DUP2);
						mth.visitIntInsn(Opcodes.BIPUSH, (int)e.arg());
						mth.visitInsn(Opcodes.LUSHR);
						mth.visitInsn(Opcodes.LXOR);
						mth.visitVarInsn(Opcodes.LSTORE,3);
						break;
					default:
						throw new AssertionError("unimplemented opcode");
				}
			}
			
			// return v;
			mth.visitVarInsn(Opcodes.LLOAD,3);
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


