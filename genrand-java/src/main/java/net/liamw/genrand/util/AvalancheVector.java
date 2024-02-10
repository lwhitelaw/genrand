package net.liamw.genrand.util;

import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class AvalancheVector {
	/**
	 * Function interface for bit vector functions.
	 */
	public static interface DiffuserVector {
		/**
		 * Map a bit value to another bit value. Don't modify the input.
		 * @param input the input value
		 * @return the output value
		 */
		public BitVector diffuse(BitVector input);
		
		public int inputSize();
		public int outputSize();
	}
	
	/**
	 * Perform an avalanche test on the given function for a given number of iterations and store the statistics into the given array.
	 * @param flipStatistics the array that statistics will be written into
	 * @param diffuser the function under test
	 * @param iterations number of iterations to run the test for
	 */
	private static void doAvalancheTest(int[][] flipStatistics, DiffuserVector diffuser, int iterations) {
		Random random = ThreadLocalRandom.current();
		for (int i = 0; i < iterations; i++) {
			// Start with a random integer x and find f(x).
			BitVector starting = BitVector.random(random,diffuser.inputSize());
			BitVector diffused = diffuser.diffuse(starting);
			// For each of the possible bit positions, try flipping the bit in that position.
			for (int bitFlipped = 0; bitFlipped < diffuser.inputSize(); bitFlipped++) {
				// Determine what bits changed in f(x) when the given bit was flipped.
				BitVector res = testDiffuse(starting, bitFlipped, diffused, diffuser);
				// Check each bit in the output result to see if it flipped.
				// Increment the appropriate input/output bit statistic in the array if it did.
				for (int bitTested = 0; bitTested < diffuser.outputSize(); bitTested++) {
					if (res.getBitAt(bitTested) != 0) {
						flipStatistics[bitFlipped][bitTested]++;
					}
				}
			}
		}
	}
	
	/**
	 * Produce an avalanche graph for the given function.
	 * @param diffuser the function under test
	 * @return a 64x64 image showing how the bits flip
	 */
	public static BufferedImage createAvalancheGraph(DiffuserVector diffuser) {
		final int[][] flipStatistics = new int[diffuser.inputSize()][diffuser.outputSize()];
		final int ITERATIONS = 1 << 16;
		BufferedImage bimg = new BufferedImage(diffuser.inputSize(),diffuser.outputSize(), BufferedImage.TYPE_INT_RGB);
		// Run the test to gain statistics
		doAvalancheTest(flipStatistics, diffuser, ITERATIONS);
		// Write out the image
		// For each row and column...
		for (int i = 0; i < diffuser.inputSize(); i++) {
			for (int j = 0; j < diffuser.outputSize(); j++) {
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
	public static double scoreAvalanche(DiffuserVector diffuser) {
		final int[][] flipStatistics = new int[diffuser.inputSize()][diffuser.outputSize()];
		final int ITERATIONS = 1 << 16;
		// Run the test to gain statistics
		doAvalancheTest(flipStatistics, diffuser, ITERATIONS);
		// The ideal is every output bit has a 50% chance of flipping when any input bit is flipped
		// Therefore, compare the observed values to this ideal 0.5.
		// Effectively, we want to calculate the Pythagorean distance between two 32x32 value vectors.
		double sum = 0.0;
		for (int i = 0; i < diffuser.inputSize(); i++) {
			for (int j = 0; j < diffuser.outputSize(); j++) {
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
	 * Return what output bits changed based on the input when a given bit is flipped.
	 * The diffused input is expected to match diffuser.diffuse(input).
	 * @param input Input to test
	 * @param whichBit the bit to flip
	 * @param diffusedInput diffused input, expected to be cached by caller
	 * @param diffuser function to use
	 * @return The bits that changed when the given bit was flipped
	 */
	public static BitVector testDiffuse(BitVector input, int whichBit, BitVector diffusedInput, DiffuserVector diffuser) {
		BitVector flippedInput = input.dup();
		flippedInput.flipBitAt(whichBit);
		BitVector flippedDiffused = diffuser.diffuse(flippedInput);
		BitVector xorDiffused = diffusedInput.dup();
		xorDiffused.xor(flippedDiffused);
		return xorDiffused;
	}
}
