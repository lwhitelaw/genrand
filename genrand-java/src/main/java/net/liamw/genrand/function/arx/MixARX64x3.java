package net.liamw.genrand.function.arx;

import java.awt.image.BufferedImage;

import net.liamw.genrand.util.AvalancheVector.DiffuserVector;
import net.liamw.genrand.util.AvalancheVector;
import net.liamw.genrand.util.BitVector;

public class MixARX64x3 implements DiffuserVector, ARXMix<MixARX64x3> {
	private static final int ROT_BITS = 6; // bits needed to define a rotation
	private static final int ROT_MASK = (1 << ROT_BITS) - 1; // bit mask for rotation constants
	private static final long ROT_MASK_LONG = (long) ROT_MASK; // long version
	private static final int TERMS = 3; // terms in use
	private static final int DEFINITION_BITS = 2 * TERMS * (1 + ROT_BITS);
	
	/**
	 * Info on this mix.
	 */
	public static final ARXMixInfo<MixARX64x3> INFO = new ARXMixInfo<MixARX64x3>("64x3", DEFINITION_BITS, MixARX64x3::unpack);
	
	// Rotation constants
	private final int a;
	private final int b;
	private final int c;
	private final int d;
	private final int e;
	private final int f;
	
	// Whether the operation is xor (true) or add (false)
	private final boolean xora;
	private final boolean xorb;
	private final boolean xorc;
	private final boolean xord;
	private final boolean xore;
	private final boolean xorf;
	
	/**
	 * Construct a function with all parameters given explictly
	 * @param a Rotation A
	 * @param b Rotation B
	 * @param c Rotation C
	 * @param d Rotation D
	 * @param e Rotation E
	 * @param f Rotation F
	 * @param xora Operator A is XOR
	 * @param xorb Operator B is XOR
	 * @param xorc Operator C is XOR
	 * @param xord Operator D is XOR
	 * @param xore Operator E is XOR
	 * @param xorf Operator F is XOR
	 */
	public MixARX64x3(int a, int b, int c, int d, int e, int f, boolean xora, boolean xorb, boolean xorc, boolean xord, boolean xore, boolean xorf) {
		this.a = a & ROT_MASK;
		this.b = b & ROT_MASK;
		this.c = c & ROT_MASK;
		this.d = d & ROT_MASK;
		this.e = e & ROT_MASK;
		this.f = f & ROT_MASK;
		this.xora = xora;
		this.xorb = xorb;
		this.xorc = xorc;
		this.xord = xord;
		this.xore = xore;
		this.xorf = xorf;
	}
	
	/**
	 * Pack parameters in to a long that describes this mix function
	 * @return packed long describing this function.
	 */
	public long pack() {
		long v = 0;
		v = (v << 1) | (xora? 1L : 0L);
		v = (v << 1) | (xorb? 1L : 0L);
		v = (v << 1) | (xorc? 1L : 0L);
		v = (v << 1) | (xord? 1L : 0L);
		v = (v << 1) | (xore? 1L : 0L);
		v = (v << 1) | (xorf? 1L : 0L);
		v = (v << ROT_BITS) | (a & ROT_MASK_LONG);
		v = (v << ROT_BITS) | (b & ROT_MASK_LONG);
		v = (v << ROT_BITS) | (c & ROT_MASK_LONG);
		v = (v << ROT_BITS) | (d & ROT_MASK_LONG);
		v = (v << ROT_BITS) | (e & ROT_MASK_LONG);
		v = (v << ROT_BITS) | (f & ROT_MASK_LONG);
		return v;
	}
	
	/**
	 * Unpack a long into a new mix function.
	 * @param v value to unpack
	 * @return a mix function from the packed long
	 */
	public static MixARX64x3 unpack(long v) {
		int f = (int)(v & ROT_MASK_LONG); v = (v >>> ROT_BITS);
		int e = (int)(v & ROT_MASK_LONG); v = (v >>> ROT_BITS);
		int d = (int)(v & ROT_MASK_LONG); v = (v >>> ROT_BITS);
		int c = (int)(v & ROT_MASK_LONG); v = (v >>> ROT_BITS);
		int b = (int)(v & ROT_MASK_LONG); v = (v >>> ROT_BITS);
		int a = (int)(v & ROT_MASK_LONG); v = (v >>> ROT_BITS);
		boolean xorf = ((v & 0x1L) == 0x1L); v = (v >>> 1);
		boolean xore = ((v & 0x1L) == 0x1L); v = (v >>> 1);
		boolean xord = ((v & 0x1L) == 0x1L); v = (v >>> 1);
		boolean xorc = ((v & 0x1L) == 0x1L); v = (v >>> 1);
		boolean xorb = ((v & 0x1L) == 0x1L); v = (v >>> 1);
		boolean xora = ((v & 0x1L) == 0x1L); v = (v >>> 1);
		return new MixARX64x3(a, b, c, d, e, f, xora, xorb, xorc, xord, xore, xorf);
	}
	
	@Override
	public ARXMixInfo<MixARX64x3> getInfo() {
		return INFO;
	}
	
	@Override
	public double score(int rounds) {
		return AvalancheVector.scoreAvalanche(roundDiffuser(rounds));
	}
	
	@Override
	public BufferedImage graph(int rounds) {
		return AvalancheVector.createAvalancheGraph(roundDiffuser(rounds));
	}

	@Override
	public BitVector diffuse(BitVector input) {
		return diffuse(input,1);
	}
	
	/**
	 * Perform diffuse operation for given number of rounds.
	 * @param input the input value
	 * @param rounds number of times to run through the operations
	 * @return the output value
	 */
	public BitVector diffuse(BitVector input, int rounds) {
		long v1 = ((input.get32BitsAlignedAt(5) & 0xFFFFFFFFL) << 32) | (input.get32BitsAlignedAt(4) & 0xFFFFFFFFL);
		long v2 = ((input.get32BitsAlignedAt(3) & 0xFFFFFFFFL) << 32) | (input.get32BitsAlignedAt(2) & 0xFFFFFFFFL);
		long v3 = ((input.get32BitsAlignedAt(1) & 0xFFFFFFFFL) << 32) | (input.get32BitsAlignedAt(0) & 0xFFFFFFFFL);
		
		for (int i = 0; i < rounds; i++) {
			v1 = xora? (v1 ^ rot64(v3,a)) : (v1 + rot64(v3,a));
			v2 = xorb? (v2 ^ rot64(v1,b)) : (v2 + rot64(v1,b));
			v3 = xorc? (v3 ^ rot64(v2,c)) : (v3 + rot64(v2,c));
			v1 = xord? (v1 ^ rot64(v3,d)) : (v1 + rot64(v3,d));
			v2 = xore? (v2 ^ rot64(v1,e)) : (v2 + rot64(v1,e));
			v3 = xorf? (v3 ^ rot64(v2,f)) : (v3 + rot64(v2,f));
		}
		
		BitVector output = new BitVector(192);
		output.set32BitsAlignedAt(5, (int)(v1 >>> 32));
		output.set32BitsAlignedAt(4, (int)(v1 >>> 0));
		output.set32BitsAlignedAt(3, (int)(v2 >>> 32));
		output.set32BitsAlignedAt(2, (int)(v2 >>> 0));
		output.set32BitsAlignedAt(1, (int)(v3 >>> 32));
		output.set32BitsAlignedAt(0, (int)(v3 >>> 0));
		return output;
	}
	
	private static long rot64(long v, int r) {
		return Long.rotateLeft(v, r);
	}
	
	@Override
	public int inputSize() {
		return 192;
	}
	
	@Override
	public int outputSize() {
		return 192;
	}
	
	private DiffuserVector roundDiffuser(int rounds) {
		return new DiffuserVector() {
			
			@Override
			public int outputSize() {
				return MixARX64x3.this.outputSize();
			}
			
			@Override
			public int inputSize() {
				return MixARX64x3.this.inputSize();
			}
			
			@Override
			public BitVector diffuse(BitVector input) {
				return MixARX64x3.this.diffuse(input, rounds);
			}
		};
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format(xora? "a ^= ROT64(c,%d);\n" : "a += ROT64(c,%d);\n", a));
		sb.append(String.format(xorb? "b ^= ROT64(a,%d);\n" : "b += ROT64(a,%d);\n", b));
		sb.append(String.format(xorc? "c ^= ROT64(b,%d);\n" : "c += ROT64(b,%d);\n", c));
		sb.append(String.format(xord? "a ^= ROT64(c,%d);\n" : "a += ROT64(c,%d);\n", d));
		sb.append(String.format(xore? "b ^= ROT64(a,%d);\n" : "b += ROT64(a,%d);\n", e));
		sb.append(String.format(xorf? "c ^= ROT64(b,%d);\n" : "c += ROT64(b,%d);\n", f));
		return sb.toString();
	}
}
