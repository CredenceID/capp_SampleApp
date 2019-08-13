package com.credenceid.sdkapp.util;

public class StringUtils {

	/* Checks to see if a string is non-null and non-zero length.
	 *
	 * @param str, String to check.
	 *
	 * @return True if String if null or empty, false otherwise.
	 */
	public static boolean
	isEmpty(String str) {

		return (str == null) || (str.isEmpty());
	}
}
