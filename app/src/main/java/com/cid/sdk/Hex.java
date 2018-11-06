package com.cid.sdk;

/* This is a helper class used for converting bytes into human readable Hex strings. */
@SuppressWarnings("WeakerAccess")
class Hex {
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

	public static String
	toString(byte data) {
		return Hex.toString(new byte[]{data});
	}

	public static String
	toString(byte[] aBytes) {
		return toString(aBytes, 0, aBytes.length);
	}

	public static String
	toString(byte[] aBytes,
			 int aOffset,
			 int aLength) {
		char[] dst = new char[aLength * 2];

		for (int si = aOffset, di = 0; si < aOffset + aLength; si++) {
			byte b = aBytes[si];
			dst[di++] = CHARS_TABLES[(b & 0xf0) >>> 4];
			dst[di++] = CHARS_TABLES[(b & 0x0f)];
		}

		return new String(dst);
	}
}