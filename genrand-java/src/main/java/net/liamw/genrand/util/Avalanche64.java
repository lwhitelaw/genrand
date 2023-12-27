package net.liamw.genrand.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import javax.imageio.ImageIO;

import net.liamw.genrand.util.Avalanche32.Diffuser;

/**
 * Class for testing avalanche properties over 64 bit functions intended to be pseudorandom permutations.
 */
public class Avalanche64 {
	/**
	 * The number of bits in a 64 bit value
	 */
	public static final int BITS = 64;
	
	/**
	 * Function interface for 64 bit functions.
	 */
	public static interface Diffuser64 {
		/**
		 * Map a 64 bit value to another 64 bit value.
		 * @param input the input value
		 * @return the output value
		 */
		public long diffuse(long input);
		
		/**
		 * Create a counter-based pseudorandom number generator using this function as the counter transform.
		 * @return a Random instance based on this function
		 */
		default Random asRandom() {
			return new Random() {
				/**
				 * The counter value, incremented by one. Passes through the transform function to produce values.
				 */
				private long c = ThreadLocalRandom.current().nextLong();
				/**
				 * The remaining bits in the last 64 bit value generated.
				 */
				private long value;
				/**
				 * How many bits are available in value before a new value must be generated.
				 */
				private int haveBits;
				
				/**
				 * Advance the counter by one.
				 */
				public void advance() {
					c++;
				}
				
				/**
				 * Produce up to 32 pseudorandom bits.
				 */
				public int next(int bits) {
					// refill if needed
					if (haveBits == 0) {
						advance(); // advance counter
						value = mix(c); // produce the transformed output
						haveBits = 64; // we now have 64 fresh bits
					}
					// extract 32 bits
					int value32 = (int)(value & 0xFFFFFFFFL);
					// remove from 64bit value for accounting
					value = (value >>> 32);
					haveBits -= 32;
					// truncate the 32 bits to the amount requested
					return value32 >>> (32 - bits);
				}

				/**
				 * Produce the transform of the input value.
				 * @param c the input value
				 * @return the transform output
				 */
				private long mix(long c) {
					return diffuse(c);
				}
			};
		}
	}
	
	private static void doAvalancheTest(int[][] flipStatistics, Diffuser64 diffuser, int iterations) {
		Random random = ThreadLocalRandom.current();
		for (int i = 0; i < iterations; i++) {
			// Start with a random integer x and find f(x).
			long starting = random.nextInt();
			long diffused = diffuser.diffuse(starting);
			// For each of the 32 bit positions, try flipping the bit in that position.
			for (int bitFlipped = 0; bitFlipped < BITS; bitFlipped++) {
				// Determine what bits changed in f(x) when the given bit was flipped.
				long res = testDiffuse(starting, bitFlipped, diffused, diffuser);
				// Check each bit in the output result to see if it flipped.
				// Increment the appropriate input/output bit statistic in the array if it did.
				for (int bitTested = 0; bitTested < BITS; bitTested++) {
					if (testBit(res, bitTested)) {
						flipStatistics[bitFlipped][bitTested]++;
					}
				}
			}
		}
	}
	
	public static BufferedImage createAvalancheGraph(Diffuser64 diffuser) {
		final int[][] flipStatistics = new int[BITS][BITS];
		final int ITERATIONS = 1 << 20;
		BufferedImage bimg = new BufferedImage(BITS, BITS, BufferedImage.TYPE_INT_RGB);
		// Run the test to gain statistics
		doAvalancheTest(flipStatistics, diffuser, ITERATIONS);
		// Write out the image
		// For each row and column...
		for (int i = 0; i < BITS; i++) {
			for (int j = 0; j < BITS; j++) {
				// i = input bit flipped, along the x
				// j = output bit tested, along the y
				// get the numerator (number of flips)
				double num = flipStatistics[i][j];
				// denominator is the number of iterations total
				double denom = ITERATIONS;
				// calculate value as a fraction, then scale value from 0-255
				double val = num/denom * 255;
				// the output colour is a shade from black (0) to white (255)
				int ival = (int) val;
				int col = ival | ival << 8 | ival << 16;
				// set the pixel
				bimg.setRGB(i, j, col);
			}
		}
		return bimg;
	}
	
	public static double scoreAvalanche(Diffuser64 diffuser) {
		final int[][] flipStatistics = new int[BITS][BITS];
		final int ITERATIONS = 1 << 20;
		// Run the test to gain statistics
		doAvalancheTest(flipStatistics, diffuser, ITERATIONS);
		// The ideal is every output bit has a 50% chance of flipping when any input bit is flipped
		// Therefore, compare the observed values to this ideal 0.5.
		// Effectively, we want to calculate the Pythagorean distance between two 32x32 value vectors.
		double sum = 0.0;
		for (int i = 0; i < BITS; i++) {
			for (int j = 0; j < BITS; j++) {
				// i = bit flipped
				// j = bit tested
				double num = flipStatistics[i][j];
				double denom = ITERATIONS;
				// Calculate observed value for this input/output position
				double val = num/denom;
				// Calculate squared error from ideal 0.5
				double sqerr = (0.5 - val);
				sqerr *= sqerr;
				// add to sum
				sum += sqerr;
			}
		}
		// Run square root to get final distance
		return Math.sqrt(sum);
	}
	
	//flip a bit at specified position
	public static long flipBit(long input, int which) {
		return input ^ (1L << which);
	}
	
	public static boolean testBit(long input, int which) {
		return (input & (1L << which)) != 0;
	}
	
	//Return a 1 in every position where the bit changed
	public static long testDiffuse(long input, int whichBit, long diffusedInput, Diffuser64 diffuser) {
		long flipped = diffuser.diffuse(flipBit(input, whichBit));
		return diffusedInput ^ flipped;
	}
}
