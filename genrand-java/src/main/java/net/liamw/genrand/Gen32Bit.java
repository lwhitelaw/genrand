package net.liamw.genrand;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.random.RandomGenerator;

import net.liamw.genrand.function.Mix32;
import net.liamw.genrand.function.Mix32.Operand;
import net.liamw.genrand.util.Avalanche32;
import net.liamw.genrand.util.LWRand64;
import net.liamw.genrand.util.PractRand;
import net.liamw.genrand.util.PractRand.TestType;

public class Gen32Bit {
	public static void run(int optimiseRounds, double threshold, Operand... types) {
		// Test parameters
		
		// The target "good enough" score for avalanche test
		final double OPTIMISE_TOLERANCE = threshold;
		// PractRand target to hit
		final int OPTIMISE_PRACTRAND = 1;
		// PractRand limit. Do not run any further than this.
		final int PRACTRAND_LIMIT = 40;
		// Path to store files
		final Path OUTPUT_FOLDER = Paths.get("E:\\ProjectTesting\\Random\\Generators\\32bit\\Av");
		{
			// Run forever
			int trial = 0;
			for (;;) {
				// Trial info
				trial++;
				System.out.println("Trial " + trial);
				// Create new mix with one addition operation to start with and score it
				Mix32 best = new Mix32();
				best.addRandom(Operand.ADD);
				double bestScore = Avalanche32.scoreAvalanche(best);
				// Print
				System.out.printf("Current: %f\n%s\n",bestScore,best.toString());
				// Begin trials
				while (bestScore > OPTIMISE_TOLERANCE) {
					// Add a random operator and score it
					best.addRandom(types);
					bestScore = Avalanche32.scoreAvalanche(best);
					System.out.printf("Adding an operator and rescored: %f\n%s\n",bestScore,best.toString());
					// Begin optimisation attempts
					int attempts = optimiseRounds;
					while (attempts > 0) {
						System.out.printf("%d operators, optimise phase (%d attempts remain)\n",best.oplen(),attempts);
						// Copy the mix and modify one operator
						Mix32 copy = new Mix32(best);
						copy.replaceRandom(types);
						double copyScore = Avalanche32.scoreAvalanche(copy);
						// If it is better, save it and restart the attempt counter
						if (copyScore < bestScore) {
							best = copy;
							bestScore = copyScore;
							attempts = optimiseRounds;
							System.out.printf("New best: %f\n%s\nRestarted the attempt counter.\n",bestScore,best.toString());
						} else {
							attempts--;
						}
					}
				}
			}
			// Begin another trial, write out
		}
	}
	
	private static void writeNewFile(String str, Path folder, String suffix) {
		int i = 0;
		while (Files.exists(folder.resolve(String.format("%d%s.txt", i, suffix)))) i++;
		try {
			Files.writeString(folder.resolve(String.format("%d%s.txt", i, suffix)), str);
		} catch (IOException e) {} // ignore
	}
}

/*
Table properties
- ID
- Bitfield of operators used, add, mul, xorsh, etc.
- Number of operators used
- Avalanche score
- PractRand score
- Source code (CAS?)
- Avalanche graph image (CAS?)

Gen plan
- Start with 1 operator, addition
- Stack random operator and calculate avalanche
  - Give N tries to improve this mix by replacing any operator at random
  - After N tries up, stack again and repeat
- Keep stacking until avalanche hits a threshold, then try to improve it one last time
- Save the end-result to database
*/