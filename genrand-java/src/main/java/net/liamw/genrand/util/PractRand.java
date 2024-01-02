package net.liamw.genrand.util;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.random.RandomGenerator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class of utilities to facilitate testing with the PractRand test suite.
 */
public class PractRand {
	/**
	 * The type of input PractRand is expecting.
	 */
	public enum TestType {
		/**
		 * Unknown or unusual-sized inputs.
		 */
		STDIN("stdin"),
		/**
		 * Input is individual bytes.
		 */
		STDIN8("stdin8"),
		/**
		 * Input is 16-bit values.
		 */
		STDIN16("stdin16"),
		/**
		 * Input is 32-bit values.
		 */
		STDIN32("stdin32"),
		/**
		 * Input is 64-bit values.
		 */
		STDIN64("stdin64");
		
		/**
		 * The argument string to pass to PractRand.
		 */
		private final String arg;
		
		TestType(String arg) {
			this.arg = arg;
		}
		
		/**
		 * Returns the argument to be passed to PractRand as the input size.
		 * @return the type argument string
		 */
		public final String commandArg() {
			return arg; 
		}
	}
	
	/**
	 * Where to find the PractRand binary
	 */
	private static final String PRACTRAND = "";
	/**
	 * The PractRand process itself
	 */
	private final Process process;
	/**
	 * Thread monitoring PractRand's standard out
	 */
	private final Thread monitor;
	/**
	 * Thread killing PractRand if it does not emit any output
	 */
	private final Thread watchdog;
	/**
	 * Lock on test state
	 */
	private final Lock lock;
	/**
	 * Output stream that is fed as input to PractRand
	 */
	private final DataOutputStream os;
	
	/**
	 * If true, PractRand failed the RNG
	 */
	private boolean fail;
	/**
	 * The last 2^n bytes PractRand reached so far (or when the RNG is failed)
	 */
	private int lastLevel;
	/**
	 * Logged output from PractRand
	 */
	private StringBuilder output;
	
	/**
	 * Construct state for a PractRand test and kick off the testing process.
	 * @param type the type of test to run
	 */
	private PractRand(TestType type) {
		// Set up state
		this.fail = false;
		this.lastLevel = 0;
		this.output = new StringBuilder(65536);
		// Build the PractRand test and start it
		ProcessBuilder pb = new ProcessBuilder(PRACTRAND,type.commandArg(),"-tlmin","4K","-multithreaded");
		try {
			process = pb.start();
			os = new DataOutputStream(process.getOutputStream());
		} catch (IOException ex) {
			throw new RuntimeException("PractRand failed to start",ex);
		}
		// Create state lock
		lock = new ReentrantLock();
		// Create threads to monitor process (caller will start them)
		monitor = new Thread(this::monitor);
		watchdog = new Thread(this::watchdog);
	}
	
	/**
	 * Create a PractRand test for the given test type.
	 * @param type the type of test to run
	 * @return an object to manage the test
	 */
	public static PractRand startTest(TestType type) {
		PractRand practRand = new PractRand(type);
		practRand.monitor.start();
		practRand.watchdog.start();
		return practRand;
	}
	
	/**
	 * Standard output monitor thread. Reads and parses output lines while waiting for
	 * the PractRand process to terminate.
	 */
	private void monitor() {
		Pattern NUMBER = Pattern.compile("2\\^([0-9]+)");
		Scanner scanner = new Scanner(process.getInputStream());
		// Continually read lines
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			try {
				// take the lock
				lock.lock();
				// parse the current line
				if (line.contains("2^")) {
					// line is of form
					// length= 16 kilobytes (2^14 bytes), time= 0.2 seconds
					
					// extract the level or balk if we don't find any such number in that line
					// that should not happen, though
					Matcher matcher = NUMBER.matcher(line);
					boolean found = matcher.find();
					if (!found) throw new AssertionError("Did not find number! Offending line: " + line);
					// set the last level reached to the extracted and parsed integer
					lastLevel = Integer.parseInt(matcher.group(1));
					// kick the watchdog as it's not needed anymore
					watchdog.interrupt();
				} else if (line.contains("FAIL")) {
					// The RNG is failed at this level. Set the flag.
					// Thread should terminate soon since PractRand isn't ever going to produce more lines
					fail = true;
				}
				// Append read lines to the log.
				output.append(line);
				output.append('\n');
			} finally {
				lock.unlock();
			}
		}
	}
	
	/**
	 * Watchdog thread. Used to workaround PractRand stalling when a generator is so poor that
	 * it fails basically instantly.
	 */
	private void watchdog() {
		// Kill PractRand after five seconds. The watchdog will get terminated when the level changes.
		try {
			Thread.sleep(5000);
			System.err.println("Watchdog killed PractRand process!");
			process.destroyForcibly();
		} catch (InterruptedException ex) {
			// deliberate
		}
	}
	
	/**
	 * Write a byte array to PractRand.
	 * @param bytes input data
	 */
	public void insertBytes(byte[] bytes) {
		try {
			os.write(bytes);
		} catch (IOException ex) {
			// deliberate
		}
	}
	
	/**
	 * Write 32 bits to PractRand.
	 * @param i input value
	 */
	public void insertInt(int i) {
		try {
			os.writeInt(i);
		} catch (IOException ex) {
			// deliberate
		}
	}
	
	/**
	 * Write 64 bits to PractRand.
	 * @param i input value
	 */
	public void insertLong(long i) {
		try {
			os.writeLong(i);
		} catch (IOException ex) {
			// deliberate
		}
	}
	
	/**
	 * Get the last level PractRand reached.
	 * @return the last level 2^n bytes reached
	 */
	public int getLevel() {
		int v;
		try {
			// take the lock
			lock.lock();
			v = lastLevel;
		} finally {
			lock.unlock();
		}
		return v;
	}
	
	/**
	 * Return true if PractRand failed the generator.
	 * @return true if PractRand failed the generator.
	 */
	public boolean getFailure() {
		boolean v;
		try {
			// take the lock
			lock.lock();
			v = fail;
		} finally {
			lock.unlock();
		}
		return v;
	}
	
	/**
	 * Return all PractRand output so far as a string.
	 * @return all logged output
	 */
	public String getPractRandOutput() {
		String v;
		try {
			// take the lock
			lock.lock();
			v = output.toString();
		} finally {
			lock.unlock();
		}
		return v;
	}
	
	/**
	 * Return true if PractRand is running.
	 * @return true if the PractRand process is running.
	 */
	public boolean running() {
		return process.isAlive();
	}
	
	/**
	 * Wait until the PractRand process exits or the current thread is interrupted. In this case, the interrupt flag on the current flag is set.
	 */
	public void waitUntilEnd() {
		try {
			process.waitFor();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}
	
	/**
	 * Kill the PractRand process.
	 */
	public void kill() {
		process.destroyForcibly();
	}
	
	/**
	 * Run a PractRand test on the given generator and return the level at which the test stops. Testing will
	 * also stop if 2^limit bytes have been processed.
	 * @param r the RNG to test
	 * @param type the type of test to run
	 * @param limit the limit at which testing should stop
	 * @return the point at which PractRand stops
	 */
	public static int testWithGenerator(RandomGenerator r, TestType type, int limit) {
		// Allocate a byte array to send to PractRand for efficiency
		byte[] b = new byte[4096];
		// Run test and start pumping bytes
		PractRand test = PractRand.startTest(type);
		while (test.running()) {
			// Kill the test if the imposed limit is reached
			if (test.getLevel() >= limit) {
				test.kill();
				return test.getLevel();
			}
			// Generate and pump bytes
			r.nextBytes(b);
			test.insertBytes(b);
		}
		// Test stopped
		return test.getLevel() - 1; // last level that passed
	}
	
	/**
	 * Run a PractRand test on the given generator and return the level at which the test stops. Testing will
	 * also stop if 2^limit bytes have been processed. Progress will be output to standard out.
	 * <br><br>
	 * This version intended for convenience.
	 * @param r the RNG to test
	 * @param type the type of test to run
	 * @param limit the limit at which testing should stop
	 * @return the point at which PractRand stops
	 */
	public static int testWithGeneratorPrintLevel(RandomGenerator r, TestType type, int limit) {
		byte[] b = new byte[4096];
		PractRand test = PractRand.startTest(type);
		int level = 0;
		while (test.running()) {
			if (test.getLevel() > level) {
				level = test.getLevel();
				System.out.println("PRACTRAND 2^" + level);
			}
			if (test.getLevel() >= limit) {
				test.kill();
				return test.getLevel();
			}
			r.nextBytes(b);
			test.insertBytes(b);
		}
		System.out.println(test.getPractRandOutput());
		return test.getLevel() - 1; // last level that passed
	}
}
