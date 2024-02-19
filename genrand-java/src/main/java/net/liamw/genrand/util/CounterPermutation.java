package net.liamw.genrand.util;

/**
 * Functions for permuting a counter mod n to another value mod n.
 */
public class CounterPermutation {
	/**
	 * Permute v to another value 0..limit-1.
	 * @param v input
	 * @param limit bound
	 * @return permuted value
	 */
	public static int permute32(int v, int limit) {
		int output = v;
		do {
			output = mix32(output);
		} while (output < 0 || output >= limit);
		return output;
	}
	
	public static int mix32(int v) {
		// LWRand32 mix 2
		v += 0x38A341AF; v ^= v << 13;
		v += 0x682F2DF8; v ^= v >>> 10;
		v += 0x882611AA; v ^= v << 17;
		
		v += 0x2787052E; v ^= v >>> 3;
		v += 0x562885C4; v ^= v << 8;
		v += 0x1B6E2B3D; v ^= v >>> 20;
		
		v += 0x7EF79D81; v ^= v >>> 4;
		v += 0xA4DB621A; v ^= v << 9;
		v += 0xCC2C66ED; v ^= v >>> 15;
		return v;
	}
	
	/**
	 * Permute v to another value 0..limit-1.
	 * @param v input
	 * @param limit bound
	 * @return permuted value
	 */
	public static long permute64(long v, long limit) {
		long output = v;
		do {
			output = mix64(output);
		} while (output < 0 || output >= limit);
		return output;
	}
	
	public static long mix64(long v) {
		// LWRand64 mix 2
		v += 0x38D506988BA5CF97L; v ^= v << 33;
		v += 0x1CFF974774D783BBL; v ^= v >>> 10;
		v += 0xD26F6DAD13252AF1L; v ^= v << 1;
		v += 0x6057C672DC20E52AL; v ^= v >>> 24;
		
		v += 0x1B154FB993729895L; v ^= v << 38;
		v += 0xDA5A1A05BFA175F0L; v ^= v >>> 18;
		v += 0x80E81C053D0D9A0DL; v ^= v >>> 3;
		v += 0x3DB7D7C167BAE229L; v ^= v << 5;
		
		v += 0x19C1405A41403449L; v ^= v >>> 33;
		v += 0xCAC96BEA9B351A4AL; v ^= v << 23;
		return v;
	}
	
	/**
	 * Permute v to another value 0..limit-1, using a different mix function
	 * based on the limit.
	 * @param v input
	 * @param limit bound
	 * @return permuted value
	 */
	public static long permute(long v, long limit) {
		if (limit == (1L << 32)) {
			return mix32((int)v) & 0xFFFFFFFFL;
		} else if (limit > Integer.MAX_VALUE) {
			return permute64(v, limit);
		} else {
			return permute32((int)v, (int)limit) & 0xFFFFFFFFL;
		}
	}
}
