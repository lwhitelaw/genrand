package net.liamw.genrand.util;

import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Class for testing avalanche properties over 16/24/32 bit functions intended to be pseudorandom permutations.
 */
public class Avalanche32 {
	/**
	 * The number of bits in a 32 bit value
	 */
	public static final int BITS = 32;
	
	/**
	 * Function interface for 32 bit functions.
	 */
	public static interface Diffuser {
		/**
		 * Map a 32 bit value to another 32 bit value.
		 * @param input the input value
		 * @return the output value
		 */
		public int diffuse(int input);
		
		/**
		 * Create a counter-based PRNG from this function. (If the function is less than 32 bit, this will not work)
		 * @return a random number generator constructed from this function
		 */
		public default Random asRandom() {
			return new Random() {
				int c = LWRand64.threadLocal().nextInt(); // counter
				
				public void advance() {
					c++;
				}
				
				public int next(int bits) {
					advance();
					return mix(c) >>> (32 - bits);
				}

				private int mix(int c) {
					return diffuse(c);
				}
			};
		}
	}
	
	/**
	 * Perform an avalanche test on the given function for a given number of iterations and store the statistics into the given array.
	 * @param flipStatistics the array that statistics will be written into
	 * @param diffuser the function under test
	 * @param iterations number of iterations to run the test for
	 * @param bits number of bits in the input/output
	 */
	private static void doAvalancheTest(int[][] flipStatistics, Diffuser diffuser, int iterations, int bits) {
		Random random = ThreadLocalRandom.current();
		for (int i = 0; i < iterations; i++) {
			// Start with a random integer x and find f(x).
			int starting = random.nextInt();
			int diffused = diffuser.diffuse(starting);
			// For each of the 32 bit positions, try flipping the bit in that position.
			for (int bitFlipped = 0; bitFlipped < bits; bitFlipped++) {
				// Determine what bits changed in f(x) when the given bit was flipped.
				int res = testDiffuse(starting, bitFlipped, diffused, diffuser);
				// Check each bit in the output result to see if it flipped.
				// Increment the appropriate input/output bit statistic in the array if it did.
				for (int bitTested = 0; bitTested < bits; bitTested++) {
					if (testBit(res, bitTested)) {
						flipStatistics[bitFlipped][bitTested]++;
					}
				}
			}
		}
	}
	
	/**
	 * Produce an avalanche graph for the given function.
	 * @param diffuser the function under test
	 * @param bits number of bits in the input/output
	 * @return an image showing how the bits flip
	 */
	public static BufferedImage createAvalancheGraph(Diffuser diffuser, int bits) {
		final int[][] flipStatistics = new int[bits][bits];
		final int ITERATIONS = 1 << 16;
		BufferedImage bimg = new BufferedImage(bits, bits, BufferedImage.TYPE_INT_RGB);
		// Run the test to gain statistics
		doAvalancheTest(flipStatistics, diffuser, ITERATIONS, bits);
		// Write out the image
		// For each row and column...
		for (int i = 0; i < bits; i++) {
			for (int j = 0; j < bits; j++) {
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
	
	/**
	 * Test the function for avalanche and return a value describing its deviation from the ideal.
	 * Values closer to zero mean better avalanching properties.
	 * @param diffuser the function under test
	 * @param bits number of bits in the input/output
	 * @return a value describing the avalanche performance of this function
	 */
	public static double scoreAvalanche(Diffuser diffuser, int bits) {
		final int[][] flipStatistics = new int[bits][bits];
		// More iterations mean a value closer to the real value, down to a noise floor beyond which values are meaningless
		// 65536 iterations is a good tradeoff between accuracy/speed
		// Noise floor -> 0.06 @ 65536 iters
		final int ITERATIONS = 1 << 16;
		// Run the test to gain statistics
		doAvalancheTest(flipStatistics, diffuser, ITERATIONS, bits);
		// The ideal is every output bit has a 50% chance of flipping when any input bit is flipped
		// Therefore, compare the observed values to this ideal 0.5.
		// Effectively, we want to calculate the Pythagorean distance between two 32x32 value vectors.
		double sum = 0.0;
		for (int i = 0; i < bits; i++) {
			for (int j = 0; j < bits; j++) {
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
	
	/**
	 * Flip the given bit in the input at the given bit position.
	 * @param input the input to modify
	 * @param which the bit to flip, from 0 to 31
	 * @return the input with one flipped bit
	 */
	public static int flipBit(int input, int which) {
		return input ^ (1 << which);
	}
	
	/**
	 * Return true if the given bit from 0-31 in the input is set.
	 * @param input the input to test
	 * @param which the bit to test
	 * @return true if the given bit is set
	 */
	public static boolean testBit(int input, int which) {
		return (input & (1 << which)) != 0;
	}
	
	/**
	 * Return what output bits changed based on the input when a given bit is flipped.
	 * The diffused input is expected to match diffuser.diffuse(input).
	 * @param input Input to test
	 * @param whichBit the bit to flip
	 * @param diffusedInput diffused input, expected to be cached by caller
	 * @param diffuser function to use
	 * @return The bits that changed when the given bit was flipped
	 */
	private static int testDiffuse(int input, int whichBit, int diffusedInput, Diffuser diffuser) {
		int flipped = diffuser.diffuse(flipBit(input, whichBit));
		return diffusedInput ^ flipped;
	}
}
