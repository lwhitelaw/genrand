package net.liamw.genrand;

import net.liamw.genrand.function.Mix32;
import net.liamw.genrand.function.Mix32.Operand;
import net.liamw.genrand.util.Avalanche32;
import net.liamw.genrand.util.Database;
import net.liamw.genrand.util.LWRand64;

public class Gen32BitAddXorshift {
	public static void run(Database database, int optimiseRounds, double threshold) {
		// Run forever
		int trial = 0;
		for (;;) {
			// Trial info
			trial++;
			System.out.println("Trial " + trial);
			// Create new mix with one addition operation and random xorshift to start with and score it
			Mix32 best = new Mix32();
			addRandomAddXorshift(best);
			double bestScore = Avalanche32.scoreAvalanche(best,32);
			// Print
			System.out.printf("Current: %f\n%s\n",bestScore,best.toString());
			// Begin trials
			while (bestScore > threshold) {
				// Add a random operator and score it
				addRandomAddXorshift(best);
				bestScore = Avalanche32.scoreAvalanche(best,32);
				System.out.printf("Adding an operator and rescored: %f\n%s\n",bestScore,best.toString());
				// Begin optimisation attempts
				int attempts = optimiseRounds;
				while (attempts > 0) {
					System.out.printf("%d operators, optimise phase (%d attempts remain)\n",best.oplen(),attempts);
					// Copy the mix and modify one operator
					Mix32 copy = new Mix32(best);
					replaceRandomAddXorshift(copy);
					double copyScore = Avalanche32.scoreAvalanche(copy,32);
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
				// Write out best so far
				database.submit(best);
			}
		}
	}
	
	private static void addRandomAddXorshift(Mix32 mix) {
		mix.addRandom(Operand.ADD);
		mix.addRandom(Operand.XSL,Operand.XSR);
	}
	
	private static void replaceRandomAddXorshift(Mix32 mix) {
		int opPairs = mix.oplen() / 2;
		int index = LWRand64.threadLocal().nextInt(opPairs) * 2;
		mix.replaceRandomAt(index, Operand.ADD);
		mix.replaceRandomAt(index+1, Operand.XSL,Operand.XSR);
	}
}
