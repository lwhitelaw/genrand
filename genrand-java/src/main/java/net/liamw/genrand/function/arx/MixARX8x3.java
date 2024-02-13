package net.liamw.genrand.function.arx;

import java.awt.image.BufferedImage;

import net.liamw.genrand.util.Avalanche32;
import net.liamw.genrand.util.Avalanche32.Diffuser;

/**
 * Mixing function using 6 add/xor Feistel-like operations on rotated values iterating through 3 terms.
 */
public class MixARX8x3 implements Diffuser, ARXMix<MixARX8x3> {
	/**
	 * Info on this mix.
	 */
	public static final ARXMixInfo<MixARX8x3> INFO = new ARXMixInfo<MixARX8x3>("8x3", 24, MixARX8x3::unpack);
	
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
	public MixARX8x3(int a, int b, int c, int d, int e, int f, boolean xora, boolean xorb, boolean xorc, boolean xord, boolean xore, boolean xorf) {
		this.a = a & 0x07;
		this.b = b & 0x07;
		this.c = c & 0x07;
		this.d = d & 0x07;
		this.e = e & 0x07;
		this.f = f & 0x07;
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
		v = (v << 3) | (a & 0x07L);
		v = (v << 3) | (b & 0x07L);
		v = (v << 3) | (c & 0x07L);
		v = (v << 3) | (d & 0x07L);
		v = (v << 3) | (e & 0x07L);
		v = (v << 3) | (f & 0x07L);
		return v;
	}
	
	/**
	 * Unpack a long into a new mix function.
	 * @param v value to unpack
	 * @return a mix function from the packed long
	 */
	public static MixARX8x3 unpack(long v) {
		int f = (int)(v & 0x07L); v = (v >>> 3);
		int e = (int)(v & 0x07L); v = (v >>> 3);
		int d = (int)(v & 0x07L); v = (v >>> 3);
		int c = (int)(v & 0x07L); v = (v >>> 3);
		int b = (int)(v & 0x07L); v = (v >>> 3);
		int a = (int)(v & 0x07L); v = (v >>> 3);
		boolean xorf = ((v & 0x1L) == 0x1L); v = (v >>> 1);
		boolean xore = ((v & 0x1L) == 0x1L); v = (v >>> 1);
		boolean xord = ((v & 0x1L) == 0x1L); v = (v >>> 1);
		boolean xorc = ((v & 0x1L) == 0x1L); v = (v >>> 1);
		boolean xorb = ((v & 0x1L) == 0x1L); v = (v >>> 1);
		boolean xora = ((v & 0x1L) == 0x1L); v = (v >>> 1);
		return new MixARX8x3(a, b, c, d, e, f, xora, xorb, xorc, xord, xore, xorf);
	}
	
	@Override
	public ARXMixInfo<MixARX8x3> getInfo() {
		return INFO;
	}
	
	@Override
	public double score(int rounds) {
		return Avalanche32.scoreAvalanche(v -> diffuse(v,rounds),24);
	}
	
	@Override
	public BufferedImage graph(int rounds) {
		return Avalanche32.createAvalancheGraph(v -> diffuse(v,rounds),24);
	}

	@Override
	public int diffuse(int input) {
		return diffuse(input,1);
	}
	
	/**
	 * Perform diffuse operation for given number of rounds.
	 * @param input the input value
	 * @param rounds number of times to run through the operations
	 * @return the output value
	 */
	public int diffuse(int input, int rounds) {
		int v1 = (input >>> 16) & 0xFF;
		int v2 = (input >>> 8) & 0xFF;
		int v3 = (input & 0xFF);
		
		for (int i = 0; i < rounds; i++) {
			v1 = xora? (v1 ^ rot8(v2,a)) & 0xFF : (v1 + rot8(v2,a)) & 0xFF;
			v2 = xorb? (v2 ^ rot8(v1,b)) & 0xFF : (v2 + rot8(v1,b)) & 0xFF;
			v1 = xorc? (v1 ^ rot8(v2,c)) & 0xFF : (v1 + rot8(v2,c)) & 0xFF;
			v2 = xord? (v2 ^ rot8(v1,d)) & 0xFF : (v2 + rot8(v1,d)) & 0xFF;
		}
		
		return (v1 << 8) | v2;
	}
	
	private static int rot8(int v, int r) {
		return ((v & 0xFF) << r) | ((v & 0xFF) >>> (8-r));
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format(xora? "a ^= ROT8(b,%d);\n" : "a += ROT8(b,%d);\n", a));
		sb.append(String.format(xorb? "b ^= ROT8(a,%d);\n" : "b += ROT8(a,%d);\n", b));
		sb.append(String.format(xorc? "a ^= ROT8(b,%d);\n" : "a += ROT8(b,%d);\n", c));
		sb.append(String.format(xord? "b ^= ROT8(a,%d);\n" : "b += ROT8(a,%d);\n", d));
		return sb.toString();
	}
}
