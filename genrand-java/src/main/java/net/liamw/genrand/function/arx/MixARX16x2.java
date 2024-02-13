package net.liamw.genrand.function.arx;

import java.awt.image.BufferedImage;

import net.liamw.genrand.util.Avalanche32;
import net.liamw.genrand.util.Avalanche32.Diffuser;

/**
 * Mixing function using 4 add/xor Feistel-like operations on rotated values.
 */
public class MixARX16x2 implements Diffuser, ARXMix<MixARX16x2> {
	/**
	 * Info on this mix.
	 */
	public static final ARXMixInfo<MixARX16x2> INFO = new ARXMixInfo<MixARX16x2>("16x2", 20, MixARX16x2::unpack);
	
	// Rotation constants
	private final int a;
	private final int b;
	private final int c;
	private final int d;
	
	// Whether the operation is xor (true) or add (false)
	private final boolean xora;
	private final boolean xorb;
	private final boolean xorc;
	private final boolean xord;
	
	/**
	 * Construct a function with all parameters given explictly
	 * @param a Rotation A
	 * @param b Rotation B
	 * @param c Rotation C
	 * @param d Rotation D
	 * @param xora Operator A is XOR
	 * @param xorb Operator B is XOR
	 * @param xorc Operator C is XOR
	 * @param xord Operator D is XOR
	 */
	public MixARX16x2(int a, int b, int c, int d, boolean xora, boolean xorb, boolean xorc, boolean xord) {
		this.a = a & 0x0F;
		this.b = b & 0x0F;
		this.c = c & 0x0F;
		this.d = d & 0x0F;
		this.xora = xora;
		this.xorb = xorb;
		this.xorc = xorc;
		this.xord = xord;
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
		v = (v << 4) | (a & 0x0FL);
		v = (v << 4) | (b & 0x0FL);
		v = (v << 4) | (c & 0x0FL);
		v = (v << 4) | (d & 0x0FL);
		return v;
	}
	
	/**
	 * Unpack a long into a new mix function.
	 * @param v value to unpack
	 * @return a mix function from the packed long
	 */
	public static MixARX16x2 unpack(long v) {
		int d = (int)(v & 0x0FL); v = (v >>> 4);
		int c = (int)(v & 0x0FL); v = (v >>> 4);
		int b = (int)(v & 0x0FL); v = (v >>> 4);
		int a = (int)(v & 0x0FL); v = (v >>> 4);
		boolean xord = ((v & 0x1L) == 0x1L); v = (v >>> 1);
		boolean xorc = ((v & 0x1L) == 0x1L); v = (v >>> 1);
		boolean xorb = ((v & 0x1L) == 0x1L); v = (v >>> 1);
		boolean xora = ((v & 0x1L) == 0x1L); v = (v >>> 1);
		return new MixARX16x2(a, b, c, d, xora, xorb, xorc, xord);
	}
	
	@Override
	public ARXMixInfo<MixARX16x2> getInfo() {
		return INFO;
	}
	
	@Override
	public double score(int rounds) {
		return Avalanche32.scoreAvalanche(v -> diffuse(v,rounds),32);
	}
	
	@Override
	public BufferedImage graph(int rounds) {
		return Avalanche32.createAvalancheGraph(v -> diffuse(v,rounds),32);
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
		int v1 = (input >>> 16);
		int v2 = (input & 0xFFFF);
		
		for (int i = 0; i < rounds; i++) {
			v1 = xora? (v1 ^ rot16(v2,a)) & 0xFFFF : (v1 + rot16(v2,a)) & 0xFFFF;
			v2 = xorb? (v2 ^ rot16(v1,b)) & 0xFFFF : (v2 + rot16(v1,b)) & 0xFFFF;
			v1 = xorc? (v1 ^ rot16(v2,c)) & 0xFFFF : (v1 + rot16(v2,c)) & 0xFFFF;
			v2 = xord? (v2 ^ rot16(v1,d)) & 0xFFFF : (v2 + rot16(v1,d)) & 0xFFFF;
		}
		
		return (v1 << 16) | v2;
	}
	
	private static int rot16(int v, int r) {
		return ((v & 0xFFFF) << r) | ((v & 0xFFFF) >>> (16-r));
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format(xora? "a ^= ROT16(b,%d);\n" : "a += ROT16(b,%d);\n", a));
		sb.append(String.format(xorb? "b ^= ROT16(a,%d);\n" : "b += ROT16(a,%d);\n", b));
		sb.append(String.format(xorc? "a ^= ROT16(b,%d);\n" : "a += ROT16(b,%d);\n", c));
		sb.append(String.format(xord? "b ^= ROT16(a,%d);\n" : "b += ROT16(a,%d);\n", d));
		return sb.toString();
	}
}