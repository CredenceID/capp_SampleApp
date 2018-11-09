package com.cid.sdk.util;

/* This is a helper class used for converting bytes into human readable Hex strings. */
@SuppressWarnings("WeakerAccess")
public class Hex {
	@SuppressWarnings("SpellCheckingInspection")
	private static final char[] CHARS_TABLES = "0123456789ABCDEF".toCharArray();
	@SuppressWarnings("MismatchedReadAndWriteOfArray")
	private static final byte[] BYTES = new byte[128];

	static {
		for (int i = 0; i < 10; i++) {
			BYTES['0' + i] = (byte) i;
			BYTES['A' + i] = (byte) (10 + i);
			BYTES['a' + i] = (byte) (10 + i);
		}
	}

	/* Converts a single byte into its String representation.
	 *
	 * @param data Data to convert into string format.
	 * @return String representation of byte.
	 */
	public static String
	toString(byte data) {
		return Hex.toString(new byte[]{data});
	}

	/* Converts a byte array into its String representation.
	 *
	 * @param data Data to convert into string format.
	 * @return String representation of byte array.
	 */
	public static String
	toString(byte[] aBytes) {
		return toString(aBytes, 0, aBytes.length);
	}

	/* Converts a byte array into its String representation.
	 *
	 * @param data Data to convert into string format.
	 * @param offset Offset to start conversion from, in case a string representation is only
	 * 				 required for part of byte array.
	 * @param length Number of bytes to starting from offset to make string representation for.
	 * @return
	 */
	public static String
	toString(byte[] data,
			 int offset,
			 int length) {

		char[] dst = new char[length * 2];

		for (int si = offset, di = 0; si < offset + length; si++) {
			byte b = data[si];
			dst[di++] = CHARS_TABLES[(b & 0xf0) >>> 4];
			dst[di++] = CHARS_TABLES[(b & 0x0f)];
		}

		return new String(dst);
	}
}