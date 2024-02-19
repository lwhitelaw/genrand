package net.liamw.genrand.util;

import java.util.Random;

/**
 * A vector of bits of an arbitrary size.
 */
public class BitVector {
	/**
	 * Shift mask for 32 bit values
	 */
	private static final int LOWER_MASK = 0x1F;
	/**
	 * Shift mask for the bits not in a 32 bit shift
	 */
	private static final int UPPER_MASK = ~LOWER_MASK;
	
	/**
	 * The bit array.
	 */
	private final int[] bits;
	/**
	 * The logical number of bits this vector holds.
	 */
	private final int size;
	
	/*
	 * Note: Bit array is laid out as in increasing order -> MSB is at highest array indices
	 * This is visually reverse order when the array is written out i.e.
	 * { 31..0, 63..32 }
	 */
	
	/**
	 * Construct an empty vector of the given size.
	 * @param size size in bits
	 */
	public BitVector(int size) {
		if (size < 1) throw new IllegalArgumentException("size must be >0");
		this.size = size;
		this.bits = new int[roundUp32(size) / 32];
	}
	
	/**
	 * Copy a vector.
	 * @param other vector to copy.
	 */
	public BitVector(BitVector other) {
		this.size = other.size;
		this.bits = new int[other.bits.length];
		System.arraycopy(other.bits, 0, this.bits, 0, this.bits.length);
	}
	
	/**
	 * Generate a random vector with the given number of bits.
	 * @param r RNG to use
	 * @param bits bits in the result
	 * @return a random bit vector
	 */
	public static BitVector random(Random r, int bits) {
		BitVector bv = new BitVector(bits);
		for (int i = 0; i < bv.bits.length; i++) {
			bv.bits[i] = r.nextInt();
		}
		// The remaining bits will never be seen anyway
		return bv;
	}
	
	/**
	 * Get the logical size of this bit vector.
	 * @return number of bits in this bit vector
	 */
	public int getSize() {
		return size;
	}
	
	/**
	 * Round an integer up to the nearest multiple of 32.
	 * @param v value to round
	 * @return value rounded up to multiple of 32
	 */
	private static int roundUp32(int v) {
		// is it already divisible by 32? return it
		if ((v & LOWER_MASK) == 0) {
			return v;
		} else {
			// otherwise round value down then add 32 to round up
			return (v & LOWER_MASK) + 32;
		}
	}
	
	/**
	 * Convert the logical index into index into the bit array.
	 * @param idx logical index to convert
	 * @return array index
	 */
	private static int indexInArray(int idx) {
		return (idx & UPPER_MASK) >>> 5;
	}
	
	/**
	 * Convert the logical index into index into a 32 bit word.
	 * @param idx logical index to convert
	 * @return word bit index
	 */
	private static int indexInWord(int idx) {
		return idx & LOWER_MASK;
	}
	
	/**
	 * Get the bit at the given index.
	 * @param idx bit index
	 * @return the bit at that index
	 */
	public int getBitAt(int idx) {
		if (idx >= size) throw new IndexOutOfBoundsException(idx);
		return (bits[indexInArray(idx)] >>> indexInWord(idx)) & 0x1;
	}
	
	/**
	 * Set the bit at the given index to the bit in v.
	 * @param idx bit index
	 * @param v bit value to use
	 */
	public void setBitAt(int idx, int v) {
		if (idx >= size) throw new IndexOutOfBoundsException(idx);
		int aidx = indexInArray(idx);
		int widx = indexInWord(idx);
		int elem = bits[aidx];
		elem = elem & ~(1 << widx);
		elem = elem | ((v & 0x1) << widx);
		bits[aidx] = elem;
	}
	
	/**
	 * Set the bit at the given index to 1.
	 * @param idx bit index
	 */
	public void setBitAt(int idx) {
		if (idx >= size) throw new IndexOutOfBoundsException(idx);
		int aidx = indexInArray(idx);
		int widx = indexInWord(idx);
		int elem = bits[aidx];
		elem = elem | (1 << widx);
		bits[aidx] = elem;
	}
	
	/**
	 * Clear the bit at the given index to 0.
	 * @param idx bit index
	 */
	public void clearBitAt(int idx) {
		if (idx >= size) throw new IndexOutOfBoundsException(idx);
		int aidx = indexInArray(idx);
		int widx = indexInWord(idx);
		int elem = bits[aidx];
		elem = elem & ~(1 << widx);
		bits[aidx] = elem;
	}
	
	/**
	 * Flip the bit at the given index.
	 * @param idx bit index
	 */
	public void flipBitAt(int idx) {
		if (idx >= size) throw new IndexOutOfBoundsException(idx);
		int aidx = indexInArray(idx);
		int widx = indexInWord(idx);
		int elem = bits[aidx];
		elem = elem ^ (1 << widx);
		bits[aidx] = elem;
	}
	
	/**
	 * Get bits {idx..idx+count-1} from the given index. Only 1 to 32 bits may be returned at a time.
	 * Bounds-checking is best-effort and not guaranteed.
	 * @param idx bit index
	 * @param count number of bits
	 * @return the given bits
	 */
	public int getBitsAt(int idx, int count) {
		if (count > 32 || count < 1) {
			throw new IllegalArgumentException("count");
		}
		if (idx+count > size) throw new IndexOutOfBoundsException(idx);
		final int MASK = count == 32? 0xFFFFFFFF : (1 << count) - 1;
		// Indices based on idx, which is the LSB
		int aidx = indexInArray(idx);
		int widx = indexInWord(idx);
		// Number of bits immediately available at the first word
		int bitsAvailable = 32-widx;
		if (bitsAvailable >= count) {
			// Enough bits to do it in one go.
			return (bits[aidx] >>> widx) & MASK;
		} else {
			// The index and count straddle boundaries between two words. Needs reads of two consecutive words - zero fill if needed.
			if (aidx == bits.length-1) {
				// in order to make this work an out-of-bound read/write would be required
				throw new IndexOutOfBoundsException(idx);
			}
			int h = bits[aidx+1];
			int l = bits[aidx];
			return ((h << (32-widx)) | ((l >>> widx))) & MASK;
		}
	}
	
	/**
	 * Set bits {idx..idx+count-1} at the given index to bits in v. Only 1 to 32 bits may be set at a time.
	 * Bounds-checking is best-effort and not guaranteed.
	 * @param idx bit index
	 * @param count number of bits
	 * @param v bits to set
	 */
	public void setBitsAt(int idx, int count, int v) {
		if (count > 32 || count < 1) {
			throw new IllegalArgumentException("count");
		}
		if (idx+count > size) throw new IndexOutOfBoundsException(idx);
		final int MASK = count == 32? 0xFFFFFFFF : (1 << count) - 1;
		// Indices based on idx, which is the LSB
		int aidx = indexInArray(idx);
		int widx = indexInWord(idx);
		// Number of bits immediately available at the first word
		int bitsAvailable = 32-widx;
		if (bitsAvailable >= count) {
			// Enough bits to do it in one go.
			// Extract and modify the word by shifting the mask into place.
			int elem = bits[aidx];
			elem = elem & ~(MASK << widx);
			elem = elem | ((v & MASK) << widx);
			bits[aidx] = elem;
		} else {
			// The index and count straddle boundaries between two words. Needs reads of two consecutive words.
			if (aidx == bits.length-1) {
				// in order to make this work an out-of-bound read/write would be required
				throw new IndexOutOfBoundsException(idx);
			}
			int h = bits[aidx+1];
			int l = bits[aidx];
			l = l & ~(MASK << widx);
			l = l | ((v & MASK) << widx);
			h = h & ~(MASK >>> (32-widx));
			h = h | ((v & MASK) >>> (32-widx));
			bits[aidx+1] = h;
			bits[aidx] = l;
		}
	}
	
	/**
	 * Get 32 bits on a 32 bit alignment. This ignores the logical size and may return
	 * bits that are otherwise inaccessible normally.
	 * @param idxDiv32 the index, divided by 32
	 * @return 32 bits
	 */
	public int get32BitsAlignedAt(int idxDiv32) {
		return bits[idxDiv32];
	}
	
	/**
	 * Set 32 bits on a 32 bit alignment.
	 * @param idxDiv32 the index, divided by 32
	 * @param v bits to set
	 */
	public void set32BitsAlignedAt(int idxDiv32, int v) {
		bits[idxDiv32] = v;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = size-1; i >= 0; i--) {
			sb.append(getBitAt(i));
			if ((i % 8) == 0) sb.append(" ");
		}
		return sb.toString();
	}
	
	/**
	 * Duplicate this vector using the copy constructor.
	 * @return a copy of this vector.
	 */
	public BitVector dup() {
		return new BitVector(this);
	}
	
	/**
	 * Set this vector to the XOR of the bits in the other vector.
	 * If the other vector is larger, an exception will be thrown.
	 * The results are not well-defined if the other vector is a smaller size.
	 * @param other the vector to XOR
	 */
	public void xor(BitVector other) {
		for (int i = 0; i < other.bits.length; i++) {
			this.bits[i] ^= other.bits[i];
		}
	}
}
