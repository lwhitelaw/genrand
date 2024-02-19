package net.liamw.genrand.function.arx;

import java.awt.image.BufferedImage;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;

import net.liamw.genrand.util.CounterPermutation;
import net.liamw.genrand.util.Database;

import java.lang.invoke.MethodType;
import java.lang.invoke.VolatileCallSite;

/**
 * Interface for ARX mixes. Mixes are expected to have an additional static method <code>T unpack(long)</code>
 * that can be called to unpack the object.
 *
 * @param <T> Type of the mix
 */
public interface ARXMix<T extends ARXMix<T>> {
	/**
	 * Pack this mix into a 64-bit value that defines it. Note that not all bits may be in use.
	 * See {@link ARXMixInfo#getDefinitionBits()} to determine how many bits are valid.
	 * @return a packed representation of this mix.
	 */
	long pack();
	
	/**
	 * Get info on this mix.
	 */
	ARXMixInfo<T> getInfo();
	
	/**
	 * Score this mix for a given number of rounds.
	 */
	double score(int rounds);
	
	/**
	 * Generate an avalanche graph for this mix for a given number of rounds.
	 */
	BufferedImage graph(int rounds);
	
	/**
	 * Generically unpack any mix based on the class type by reflectively invoking its static unpack method.
	 * An exception will be thrown if the mix does not have the static unpack method.
	 * @param <T> Type of the mix
	 * @param type the class type of the mix
	 * @param value the value to unpack
	 * @return a mix unpacked from the given value.
	 */
	public static <T extends ARXMix<T>> T unpack(Class<T> type, long value) {
		try {
			return (T) StaticCaller.INVOKER.invokeExact(type,value);
		} catch (Throwable ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Start a thread to generate and write mix combinations to the database. This method returns immediately.
	 * The started thread only terminates once all combinations have been enumerated.
	 * @param <T> Type to generate
	 * @param database handle to database API
	 * @param type type to generate.
	 */
	public static <T extends ARXMix<T>> void generateInNewThread(Database database, ARXMixInfo<T> type) {
		final long START = database.getCheckpoint(type.getDatabaseTag());
		final long LIMIT = (1L << type.getDefinitionBits());
		Thread t = new Thread(() -> {
			try {
				long c = START;
				while (c < LIMIT) {
					System.out.println("Try " + c + " of " + LIMIT + " for " + type.getDatabaseTag());
					T mix = type.unpack(CounterPermutation.permute(c,LIMIT));
					c++;
					final long databaseC = c;
					database.submit(mix, db -> {
						database.setCheckpoint(type.getDatabaseTag(),databaseC);
					});
				}
			} catch (RuntimeException ex) {
				ex.printStackTrace();
			}
		});
		t.setName("ARX" + type.getDatabaseTag() + " Gen Thread");
		t.start();
	}
}

/**
 * Assist class to allow efficiently unpacking arbitrary mixes through forcing the JVM to dynamically dispatch
 * to static methods. It's otherwise unused.
 */
class StaticCaller extends VolatileCallSite {
	private static final MethodType MT_CALLER = MethodType.methodType(ARXMix.class, Class.class, long.class);
	private static final MethodType MT_RECEIVER = MethodType.methodType(ARXMix.class, long.class);
	
	private final Lookup lookup;
	
	private final MethodHandle MH_FIND = methodHandle("findAndBindTarget", MethodType.methodType(MethodHandle.class,Class.class));
	private final MethodHandle MH_TEST = methodHandle("testSameClass", MethodType.methodType(boolean.class,Class.class,Class.class));
	private final MethodHandle MH_FIND_AND_CALL = MethodHandles.foldArguments(MethodHandles.exactInvoker(MT_CALLER), MH_FIND);
	
	// These are here for a reason
	static final StaticCaller INSTANCE = new StaticCaller();
	static final MethodHandle INVOKER = INSTANCE.dynamicInvoker();
	
	public StaticCaller() {
		super(MT_CALLER);
		lookup = MethodHandles.lookup();
		setTarget(MH_FIND_AND_CALL);
	}
	
	/**
	 * Find a local method handle on this class. <code>this</code> is already bound.
	 * @param name Method name
	 * @param type Method type
	 * @return a method handle on this class.
	 */
	private MethodHandle methodHandle(String name, MethodType type) {
		try {
			return MethodHandles.lookup().findSpecial(StaticCaller.class, name, type, StaticCaller.class).bindTo(this);
		} catch (Exception ex) {
			throw new Error("did not find a method that should have existed: " + name);
		}
	}
	
	/**
	 * Find static unpack method on target class, then adapt to generic type.
	 * @param target target class
	 * @return a method handle pointing to the static unpack method on the class
	 */
	private MethodHandle findTargetMethod(Class<?> target) {
		try {
			MethodHandle mh = lookup.findStatic(target,"unpack",MethodType.methodType(target, long.class));
			return mh.asType(MT_RECEIVER);
		} catch (NoSuchMethodException ex) {
			throw new RuntimeException("target class lacks unpack method");
		} catch (IllegalAccessException ex) {
			throw new RuntimeException("target class cannot be accessed");
		}
	}
	
	private MethodHandle findAndBindTarget(Class<?> target) {
		// Find target.
		MethodHandle targetMH = findTargetMethod(target); // (long)Packable
		targetMH = MethodHandles.dropArguments(targetMH, 0, Class.class); // insert arg that isn't important but has to exist to make the call work
		// Create a guard handle that will test the invoker class - if so, call the target, else force a lookup through this method and call that
		MethodHandle guardMH = MethodHandles.guardWithTest(MH_TEST.bindTo(target), targetMH, MH_FIND_AND_CALL);
		setTarget(guardMH);
		return targetMH;
	}
	
	private boolean testSameClass(Class<?> expected, Class<?> actual) {
		return expected.equals(actual);
	}
}
