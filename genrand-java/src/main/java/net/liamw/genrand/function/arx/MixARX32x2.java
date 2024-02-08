package net.liamw.genrand.function.arx;

import net.liamw.genrand.util.Avalanche64.Diffuser64;

public class MixARX32x2 implements Diffuser64 {
	private final int a;
	private final int b;
	private final int c;
	private final int d;
	
	private final boolean xora;
	private final boolean xorb;
	private final boolean xorc;
	private final boolean xord;
	
	public MixARX32x2(int a, int b, int c, int d, boolean xora, boolean xorb, boolean xorc, boolean xord) {
		this.a = a & 0x1F;
		this.b = b & 0x1F;
		this.c = c & 0x1F;
		this.d = d & 0x1F;
		this.xora = xora;
		this.xorb = xorb;
		this.xorc = xorc;
		this.xord = xord;
	}
	
	public long pack() {
		long v = 0;
		v = (v << 1) | (xora? 1L : 0L);
		v = (v << 1) | (xorb? 1L : 0L);
		v = (v << 1) | (xorc? 1L : 0L);
		v = (v << 1) | (xord? 1L : 0L);
		v = (v << 5) | (a & 0x1FL);
		v = (v << 5) | (b & 0x1FL);
		v = (v << 5) | (c & 0x1FL);
		v = (v << 5) | (d & 0x1FL);
		return v;
	}
	
	public static MixARX32x2 unpack(long v) {
		int d = (int)(v & 0x1FL); v = (v >>> 5);
		int c = (int)(v & 0x1FL); v = (v >>> 5);
		int b = (int)(v & 0x1FL); v = (v >>> 5);
		int a = (int)(v & 0x1FL); v = (v >>> 5);
		boolean xord = ((v & 0x1L) == 0x1L); v = (v >>> 1);
		boolean xorc = ((v & 0x1L) == 0x1L); v = (v >>> 1);
		boolean xorb = ((v & 0x1L) == 0x1L); v = (v >>> 1);
		boolean xora = ((v & 0x1L) == 0x1L); v = (v >>> 1);
		return new MixARX32x2(a, b, c, d, xora, xorb, xorc, xord);
	}

	@Override
	public long diffuse(long input) {
		return diffuse(input,1);
	}
	
	public long diffuse(long input, int rounds) {
		int v1 = (int)(input >>> 32);
		int v2 = (int)(input & 0xFFFFFFFFL);
		
		for (int i = 0; i < rounds; i++) {
			v1 = xora? (v1 ^ rot32(v2,a)) : (v1 + rot32(v2,a));
			v2 = xorb? (v2 ^ rot32(v1,b)) : (v2 + rot32(v1,b));
			v1 = xorc? (v1 ^ rot32(v2,c)) : (v1 + rot32(v2,c));
			v2 = xord? (v2 ^ rot32(v1,d)) : (v2 + rot32(v1,d));
		}
		
		return ((v1 & 0xFFFFFFFFL) << 32) | (v2 & 0xFFFFFFFFL);
	}
	
	private static int rot32(int v, int r) {
		return Integer.rotateLeft(v, r);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format(xora? "a ^= ROT32(b,%d);\n" : "a += ROT32(b,%d);\n", a));
		sb.append(String.format(xorb? "b ^= ROT32(a,%d);\n" : "b += ROT32(a,%d);\n", b));
		sb.append(String.format(xorc? "a ^= ROT32(b,%d);\n" : "a += ROT32(b,%d);\n", c));
		sb.append(String.format(xord? "b ^= ROT32(a,%d);\n" : "b += ROT32(a,%d);\n", d));
		return sb.toString();
	}
}
