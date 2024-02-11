package net.liamw.genrand.function.arx;

import java.awt.image.BufferedImage;

/**
 * Information about a particular ARX-based mix including how to generate and serialise them to databases.
 *
 * @param <T> The class type
 */
public final class ARXMixInfo<T extends ARXMix<T>> {
	public interface Unpacker<T> {
		T unpack(long value);
	}
	
	private final String databaseTag;
	private final int definitionBits;
	private final Unpacker<T> unpacker;
	
	public ARXMixInfo(String databaseTag, int definitionBits, Unpacker<T> unpacker) {
		this.databaseTag = databaseTag;
		this.definitionBits = definitionBits;
		this.unpacker = unpacker;
	}
	
	public final String getDatabaseTag() {
		return databaseTag;
	}
	
	public final int getDefinitionBits() {
		return definitionBits;
	}
	
	public final T unpack(long value) {
		return unpacker.unpack(value);
	}
}
