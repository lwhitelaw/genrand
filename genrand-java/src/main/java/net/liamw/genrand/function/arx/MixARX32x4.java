package net.liamw.genrand.function.arx;

import java.awt.image.BufferedImage;

import net.liamw.genrand.util.AvalancheVector.DiffuserVector;
import net.liamw.genrand.util.AvalancheVector;
import net.liamw.genrand.util.BitVector;

public class MixARX32x4 implements DiffuserVector, ARXMix<MixARX32x4> {
	private static final int ROT_BITS = 5; // bits needed to define a rotation
	private static final int ROT_MASK = (1 << ROT_BITS) - 1; // bit mask for rotation constants
	private static final long ROT_MASK_LONG = (long) ROT_MASK; // long version
	private static final int TERMS = 4; // terms in use
	private static final int DEFINITION_BITS = 2 * TERMS * (1 + ROT_BITS);
	
	/**
	 * Info on this mix.
	 */
	public static final ARXMixInfo<MixARX32x4> INFO = new ARXMixInfo<MixARX32x4>("32x4", DEFINITION_BITS, MixARX32x4::unpack);
	
	// Rotation constants
	private final int a;
	private final int b;
	private final int c;
	private final int d;
	private final int e;
	private final int f;
	private final int g;
	private final int h;
	
	// Whether the operation is xor (true) or add (false)
	private final boolean xora;
	private final boolean xorb;
	private final boolean xorc;
	private final boolean xord;
	private final boolean xore;
	private final boolean xorf;
	private final boolean xorg;
	private final boolean xorh;
	
	/**
	 * Construct a function with all parameters given explictly
	 * @param a Rotation A
	 * @param b Rotation B
	 * @param c Rotation C
	 * @param d Rotation D
	 * @param e Rotation E
	 * @param f Rotation F
	 * @param g Rotation G
	 * @param h Rotation H
	 * @param xora Operator A is XOR
	 * @param xorb Operator B is XOR
	 * @param xorc Operator C is XOR
	 * @param xord Operator D is XOR
	 * @param xore Operator E is XOR
	 * @param xorf Operator F is XOR
	 * @param xorg Operator G is XOR
	 * @param xorh Operator H is XOR
	 */
	public MixARX32x4(int a, int b, int c, int d, int e, int f, int g, int h, boolean xora, boolean xorb, boolean xorc, boolean xord, boolean xore, boolean xorf, boolean xorg, boolean xorh) {
		this.a = a & ROT_MASK;
		this.b = b & ROT_MASK;
		this.c = c & ROT_MASK;
		this.d = d & ROT_MASK;
		this.e = e & ROT_MASK;
		this.f = f & ROT_MASK;
		this.g = g & ROT_MASK;
		this.h = h & ROT_MASK;
		this.xora = xora;
		this.xorb = xorb;
		this.xorc = xorc;
		this.xord = xord;
		this.xore = xore;
		this.xorf = xorf;
		this.xorg = xorg;
		this.xorh = xorh;
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
		v = (v << 1) | (xorg? 1L : 0L);
		v = (v << 1) | (xorh? 1L : 0L);
		v = (v << ROT_BITS) | (a & ROT_MASK_LONG);
		v = (v << ROT_BITS) | (b & ROT_MASK_LONG);
		v = (v << ROT_BITS) | (c & ROT_MASK_LONG);
		v = (v << ROT_BITS) | (d & ROT_MASK_LONG);
		v = (v << ROT_BITS) | (e & ROT_MASK_LONG);
		v = (v << ROT_BITS) | (f & ROT_MASK_LONG);
		v = (v << ROT_BITS) | (g & ROT_MASK_LONG);
		v = (v << ROT_BITS) | (h & ROT_MASK_LONG);
		return v;
	}
	
	/**
	 * Unpack a long into a new mix function.
	 * @param v value to unpack
	 * @return a mix function from the packed long
	 */
	public static MixARX32x4 unpack(long v) {
		int h = (int)(v & ROT_MASK_LONG); v = (v >>> ROT_BITS);
		int g = (int)(v & ROT_MASK_LONG); v = (v >>> ROT_BITS);
		int f = (int)(v & ROT_MASK_LONG); v = (v >>> ROT_BITS);
		int e = (int)(v & ROT_MASK_LONG); v = (v >>> ROT_BITS);
		int d = (int)(v & ROT_MASK_LONG); v = (v >>> ROT_BITS);
		int c = (int)(v & ROT_MASK_LONG); v = (v >>> ROT_BITS);
		int b = (int)(v & ROT_MASK_LONG); v = (v >>> ROT_BITS);
		int a = (int)(v & ROT_MASK_LONG); v = (v >>> ROT_BITS);
		boolean xorh = ((v & 0x1L) == 0x1L); v = (v >>> 1);
		boolean xorg = ((v & 0x1L) == 0x1L); v = (v >>> 1);
		boolean xorf = ((v & 0x1L) == 0x1L); v = (v >>> 1);
		boolean xore = ((v & 0x1L) == 0x1L); v = (v >>> 1);
		boolean xord = ((v & 0x1L) == 0x1L); v = (v >>> 1);
		boolean xorc = ((v & 0x1L) == 0x1L); v = (v >>> 1);
		boolean xorb = ((v & 0x1L) == 0x1L); v = (v >>> 1);
		boolean xora = ((v & 0x1L) == 0x1L); v = (v >>> 1);
		return new MixARX32x4(a, b, c, d, e, f, g, h, xora, xorb, xorc, xord, xore, xorf, xorg, xorh);
	}
	
	@Override
	public ARXMixInfo<MixARX32x4> getInfo() {
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
		int v1 = input.get32BitsAlignedAt(3);
		int v2 = input.get32BitsAlignedAt(2);
		int v3 = input.get32BitsAlignedAt(1);
		int v4 = input.get32BitsAlignedAt(0);
		
		for (int i = 0; i < rounds; i++) {
			v1 = xora? (v1 ^ rot32(v4,a)) : (v1 + rot32(v4,a));
			v2 = xorb? (v2 ^ rot32(v1,b)) : (v2 + rot32(v1,b));
			v3 = xorc? (v3 ^ rot32(v2,c)) : (v3 + rot32(v2,c));
			v4 = xord? (v4 ^ rot32(v3,d)) : (v4 + rot32(v3,d));
			v1 = xore? (v1 ^ rot32(v4,e)) : (v1 + rot32(v4,e));
			v2 = xorf? (v2 ^ rot32(v1,f)) : (v2 + rot32(v1,f));
			v3 = xorg? (v3 ^ rot32(v2,g)) : (v3 + rot32(v2,g));
			v4 = xorh? (v4 ^ rot32(v3,h)) : (v4 + rot32(v3,h));
		}
		
		BitVector output = new BitVector(128);
		output.set32BitsAlignedAt(3, v1);
		output.set32BitsAlignedAt(2, v2);
		output.set32BitsAlignedAt(1, v3);
		output.set32BitsAlignedAt(0, v4);
		return output;
	}
	
	private static int rot32(int v, int r) {
		return Integer.rotateLeft(v, r);
	}
	
	@Override
	public int inputSize() {
		return 128;
	}
	
	@Override
	public int outputSize() {
		return 128;
	}
	
	private DiffuserVector roundDiffuser(int rounds) {
		return new DiffuserVector() {
			
			@Override
			public int outputSize() {
				return MixARX32x4.this.outputSize();
			}
			
			@Override
			public int inputSize() {
				return MixARX32x4.this.inputSize();
			}
			
			@Override
			public BitVector diffuse(BitVector input) {
				return MixARX32x4.this.diffuse(input, rounds);
			}
		};
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format(xora? "a ^= ROT32(d,%d);\n" : "a += ROT32(d,%d);\n", a));
		sb.append(String.format(xorb? "b ^= ROT32(a,%d);\n" : "b += ROT32(a,%d);\n", b));
		sb.append(String.format(xorc? "c ^= ROT32(b,%d);\n" : "c += ROT32(b,%d);\n", c));
		sb.append(String.format(xord? "d ^= ROT32(c,%d);\n" : "d += ROT32(c,%d);\n", d));
		sb.append(String.format(xore? "a ^= ROT32(d,%d);\n" : "a += ROT32(d,%d);\n", e));
		sb.append(String.format(xorf? "b ^= ROT32(a,%d);\n" : "b += ROT32(a,%d);\n", f));
		sb.append(String.format(xorg? "c ^= ROT32(b,%d);\n" : "c += ROT32(b,%d);\n", g));
		sb.append(String.format(xorh? "d ^= ROT32(c,%d);\n" : "d += ROT32(c,%d);\n", h));
		return sb.toString();
	}
}
