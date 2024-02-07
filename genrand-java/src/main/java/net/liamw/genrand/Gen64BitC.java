package net.liamw.genrand;

import net.liamw.genrand.function.Mix64C;
import net.liamw.genrand.function.Mix64C.Operand;
import net.liamw.genrand.util.Avalanche64;
import net.liamw.genrand.util.Database;

public class Gen64BitC {
	public static void run(Database database, int optimiseRounds, double threshold, Operand... types) {
		// Run forever
		int trial = 0;
		for (;;) {
			// Trial info
			trial++;
			System.out.println("Trial " + trial);
			// Create new mix with one addition operation to start with and score it
			Mix64C best = new Mix64C();
			best.addRandom(Operand.LADDROLR,Operand.LXORROLR);
			double bestScore = Avalanche64.scoreAvalanche(best);
			// Print
			System.out.printf("Current: %f\n%s\n",bestScore,best.toString());
			// Begin trials
			while (bestScore > threshold) {
				// Add a random operator and score it
				best.addRandom(Operand.LADDROLR,Operand.LXORROLR);
				bestScore = Avalanche64.scoreAvalanche(best);
				System.out.printf("Adding an operator and rescored: %f\n%s\n",bestScore,best.toString());
				// Begin optimisation attempts
				int attempts = optimiseRounds;
				while (attempts > 0) {
					System.out.printf("%d operators, optimise phase (%d attempts remain)\n",best.oplen(),attempts);
					// Copy the mix and modify one operator
					Mix64C copy = new Mix64C(best);
					copy.replaceRandom(Operand.LADDROLR,Operand.LXORROLR);
					double copyScore = Avalanche64.scoreAvalanche(copy);
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
//				database.submit(best);
			}
		}
	}
}
