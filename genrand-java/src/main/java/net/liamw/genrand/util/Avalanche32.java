package net.liamw.genrand.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

import javax.imageio.ImageIO;

/**
 * Class for testing avalanche properties over 32 bit functions intended to be pseudorandom permutations.
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
	}
	
	/**
	 * Perform an avalanche test on the given function for a given number of iterations and store the statistics into the given array.
	 * @param flipStatistics the 32x32 array that statistics will be written into
	 * @param diffuser the function under test
	 * @param iterations number of iterations to run the test for
	 */
	private static void doAvalancheTest(int[][] flipStatistics, Diffuser diffuser, int iterations) {
		Random random = ThreadLocalRandom.current();
		for (int i = 0; i < iterations; i++) {
			// Start with a random integer x and find f(x).
			int starting = random.nextInt();
			int diffused = diffuser.diffuse(starting);
			// For each of the 32 bit positions, try flipping the bit in that position.
			for (int bitFlipped = 0; bitFlipped < BITS; bitFlipped++) {
				// Determine what bits changed in f(x) when the given bit was flipped.
				int res = testDiffuse(starting, bitFlipped, diffused, diffuser);
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
	
	/**
	 * Produce an avalanche graph for the given function.
	 * @param diffuser the function under test
	 * @return a 32x32 image showing how the bits flip
	 */
	public static BufferedImage createAvalancheGraph(Diffuser diffuser) {
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
	
	/**
	 * Test the function for avalanche and return a value describing its deviation from the ideal.
	 * Values closer to zero mean better avalanching properties.
	 * @param diffuser the function under test
	 * @return a value describing the avalanche performance of this function
	 */
	public static double scoreAvalanche(Diffuser diffuser) {
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
	 * 
	 * @param input
	 * @param whichBit
	 * @param diffusedInput
	 * @param diffuser
	 * @return
	 */
	private static int testDiffuse(int input, int whichBit, int diffusedInput, Diffuser diffuser) {
		int flipped = diffuser.diffuse(flipBit(input, whichBit));
		return diffusedInput ^ flipped;
	}
}
