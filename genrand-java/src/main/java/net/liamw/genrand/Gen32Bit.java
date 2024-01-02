package net.liamw.genrand;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import net.liamw.genrand.function.Mix32;
import net.liamw.genrand.util.Avalanche32;
import net.liamw.genrand.util.PractRand;
import net.liamw.genrand.util.PractRand.TestType;

public class Gen32Bit {
	public static void run() {
		// Test parameters
		
		// The target "good enough" score for avalanche test
		final double OPTIMISE_TOLERANCE = 0.5;
		// PractRand target to hit
		final int OPTIMISE_PRACTRAND = 1;
		// PractRand limit. Do not run any further than this.
		final int PRACTRAND_LIMIT = 40;
		// Path to store files
		final Path OUTPUT_FOLDER = Paths.get("E:\\ProjectTesting\\Random\\Generators\\32bit\\Av");
		{
			int trial = 0;
			int[] numPractRandThreads = new int[1];
			ReentrantLock prLock = new ReentrantLock();
			Condition condIsRunPermissible = prLock.newCondition();
			for (;;) {
				trial++;
				System.out.println("Trial " + trial);
				// Create new mix with one operation replaced or removed
				Mix32 newMix = new Mix32();
				for (int i = 0; i < 3; i++) newMix.addRandom(Mix32.Operand.XSR);				
				// Run tests.
				
				// Run avalanche test.
				double newScore = Avalanche32.scoreAvalanche(newMix);
				System.out.println("Av: " + newScore);
				
				if (newScore > OPTIMISE_TOLERANCE) {
					continue; // Value not in tolerance. Skip.
				}
				
				// Wait until less PractRand tasks are running
				prLock.lock();
				try {
					while (numPractRandThreads[0] >= 4) {
						System.out.println("Awaiting a PractRand task to finish");
						condIsRunPermissible.awaitUninterruptibly();
					}
					numPractRandThreads[0]++;
				} finally {
					prLock.unlock();
				}
				// Validated.
				// Run PractRand in seperate thread
				Thread.startVirtualThread(() -> {
					try {
						int newPractRand = PractRand.testWithGenerator(newMix.asRandom(), TestType.STDIN32, PRACTRAND_LIMIT);
						System.out.println("PractRand: 2^" + newPractRand);
						
						if (newPractRand < OPTIMISE_PRACTRAND) {
							return; // Value not in tolerance. Skip.
						}
						
						System.out.println("=== FOUND ===");
						System.out.println(newMix.toString());
						System.out.println(newScore);
						System.out.println("PractRand 2^" + newPractRand);
						System.out.println("=============");
						
						StringBuilder sb = new StringBuilder();
						sb.append("// Avalanche score: ").append(newScore).append("\n");
						sb.append("// PractRand: 2^").append(newPractRand).append("\n");
						sb.append(newMix.toString());
						writeNewFile(sb.toString(), OUTPUT_FOLDER, "-PRM" + newPractRand);
					} finally {
						// decrement counter
						prLock.lock();
						if (numPractRandThreads[0] > 0) numPractRandThreads[0]--;
						condIsRunPermissible.signal();
						prLock.unlock();
					}
				});
			}
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
*/