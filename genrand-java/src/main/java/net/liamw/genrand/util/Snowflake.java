package net.liamw.genrand.util;

import java.security.SecureRandom;

/**
 * Compact 64 bit UUID generation. The upper 32 bits are derived from the Unix time to the granularity of one second.
 * The lower 32 bits are randomly generated. These are not expected to run out for the next 136 years. Note that
 * generating too many in a second (>100 calls/s to be conservative) WILL run the risk of collisions!
 */
public class Snowflake {
	private static final SecureRandom SRANDOM = new SecureRandom();
	public static final long EPOCH = 1704085200; // 2024-01-01 00:00:00 in Unix seconds
	
	/**
	 * Generate a compact snowflake.
	 * @return a newly-generated snowflake
	 */
	public static long generate() {
		long unixtimeAdj = (System.currentTimeMillis() / 1000) - EPOCH;
		long random = SRANDOM.nextInt() & 0xFFFFFFFFL;
		return (unixtimeAdj << 32) | random;
	}
}
