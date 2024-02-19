package net.liamw.genrand.function.arx;

/**
 * Information about a particular ARX-based mix including how to generate and serialise them to databases.
 *
 * @param <T> The class type
 */
public final class ARXMixInfo<T extends ARXMix<T>> {
	/**
	 * Functional interface for method unpacking long to a mix type.
	 *
	 * @param <T> Target mix type.
	 */
	public interface Unpacker<T> {
		/**
		 * Unpack the value into a mix.
		 * @param value value to unpack
		 * @return the mix from this definition value
		 */
		T unpack(long value);
	}
	
	/**
	 * Type tag used in the database.
	 */
	private final String databaseTag;
	/**
	 * The number of bits used in the mix definition.
	 */
	private final int definitionBits;
	/**
	 * The function that lets mixes be unpacked from a long.
	 */
	private final Unpacker<T> unpacker;
	
	/**
	 * Create an info object with the given values.
	 * @param databaseTag tag in the database
	 * @param definitionBits bits used in the packed definition
	 * @param unpacker method that unpacks longs
	 */
	public ARXMixInfo(String databaseTag, int definitionBits, Unpacker<T> unpacker) {
		this.databaseTag = databaseTag;
		this.definitionBits = definitionBits;
		this.unpacker = unpacker;
	}
	
	/**
	 * Get the tag this mix uses in the database.
	 * @return the tag used
	 */
	public final String getDatabaseTag() {
		return databaseTag;
	}
	
	/**
	 * Get the number of bits used in packed definitions.
	 * @return the number of bits used
	 */
	public final int getDefinitionBits() {
		return definitionBits;
	}
	
	/**
	 * Call the unpacker to unpack a packed definition into a mix of this type.
	 * @param value value to unpack
	 * @return an unpacked mix
	 */
	public final T unpack(long value) {
		return unpacker.unpack(value);
	}
}
