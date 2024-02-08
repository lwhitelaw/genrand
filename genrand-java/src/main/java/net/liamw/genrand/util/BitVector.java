package net.liamw.genrand.util;

import java.util.Random;

public class BitVector {
	private static final int LOWER_MASK = 0x1F;
	private static final int UPPER_MASK = ~LOWER_MASK;
	
	private final int[] bits;
	private final int size;
	
	/*
	 * Note: Bit array is laid out as in increasing order -> MSB is at highest array indices
	 * This is visually reverse order when the array is written out i.e.
	 * { 31..0, 63..32 }
	 */
	
	public BitVector(int size) {
		if (size < 1) throw new IllegalArgumentException("size must be >0");
		this.size = size;
		this.bits = new int[roundUp32(size) / 32];
	}
	
	public BitVector(BitVector other) {
		this.size = other.size;
		this.bits = new int[other.bits.length];
		System.arraycopy(other.bits, 0, this.bits, 0, this.bits.length);
	}
	
	public static BitVector random(Random r, int bits) {
		BitVector bv = new BitVector(bits);
		for (int i = 0; i < bv.bits.length; i++) {
			bv.bits[i] = r.nextInt();
		}
		// The remaining bits will never be seen anyway
		return bv;
	}
	
	public int getSize() {
		return size;
	}
	
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
	
	public int getBitAt(int idx) {
		if (idx >= size) throw new IndexOutOfBoundsException(idx);
		return (bits[indexInArray(idx)] >>> indexInWord(idx)) & 0x1;
	}
	
	public void setBitAt(int idx, int v) {
		if (idx >= size) throw new IndexOutOfBoundsException(idx);
		int aidx = indexInArray(idx);
		int widx = indexInWord(idx);
		int elem = bits[aidx];
		elem = elem & ~(1 << widx);
		elem = elem | ((v & 0x1) << widx);
		bits[aidx] = elem;
	}
	
	public void setBitAt(int idx) {
		if (idx >= size) throw new IndexOutOfBoundsException(idx);
		int aidx = indexInArray(idx);
		int widx = indexInWord(idx);
		int elem = bits[aidx];
		elem = elem | (1 << widx);
		bits[aidx] = elem;
	}
	
	public void clearBitAt(int idx) {
		if (idx >= size) throw new IndexOutOfBoundsException(idx);
		int aidx = indexInArray(idx);
		int widx = indexInWord(idx);
		int elem = bits[aidx];
		elem = elem & ~(1 << widx);
		bits[aidx] = elem;
	}
	
	public void flipBitAt(int idx) {
		if (idx >= size) throw new IndexOutOfBoundsException(idx);
		int aidx = indexInArray(idx);
		int widx = indexInWord(idx);
		int elem = bits[aidx];
		elem = elem ^ (1 << widx);
		bits[aidx] = elem;
	}
	
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
	
	public int get32BitsAlignedAt(int idxDiv32) {
		return bits[idxDiv32];
	}
	
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
	
	public BitVector dup() {
		return new BitVector(this);
	}
	
	public void xor(BitVector other) {
		for (int i = 0; i < other.bits.length; i++) {
			this.bits[i] ^= other.bits[i];
		}
	}
}
