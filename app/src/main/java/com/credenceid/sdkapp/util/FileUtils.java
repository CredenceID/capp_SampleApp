package com.credenceid.sdkapp.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@SuppressWarnings({"UnusedReturnValue", "unused"})
public class FileUtils {

	private static final String TAG = FileUtils.class.getName();

	private static final int COMPRESSION_QUALITY = 90;

	/* Copies file contents from source file to destination file.
	 *
	 * @param src, Source file whose contents will be copied.
	 * @param dst, Destination file where contents will be copied to.
	 *
	 * @return True if copy was successful, false otherwise.
	 */
	public static boolean
	copy(File src,
	     File dst) {

		try (InputStream in = new FileInputStream(src)) {
			OutputStream out = new FileOutputStream(dst);
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0)
				out.write(buf, 0, len);

			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/* Reads a given file's data as a byte array and returns read data.
	 *
	 * @param absFilePath, Absolute path on disk of file to read.
	 *
	 * @return If file was not found or could not be read an empty byte array is returned.
	 */
	public static byte[]
	getBytes(String absFilePath) {

		try (FileInputStream fis = new FileInputStream(absFilePath)) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream(0x20000);
			byte[] buf = new byte[1024];

			for (int readNum; (readNum = fis.read(buf)) != -1; )
				bos.write(buf, 0, readNum);
			return bos.toByteArray();
		} catch (Exception e) {
			Log.w(TAG, "getBytes(String): Unable to read byes from file.");
			return new byte[]{};
		}
	}

	/* Writes a given byte array to absolute file path on disk.
	 *
	 * @param bytes, Data to write to file.
	 * @param absFilePath, Absolute path on disk of where to getSnapshot bytes.
	 *
	 * @return True if write was successful, false otherwise.
	 */
	public static boolean
	write(byte[] bytes,
	      String absFilePath) {

		File f = new File(absFilePath);
		try (FileOutputStream fos = new FileOutputStream(f.getPath())) {
			fos.write(bytes);
			fos.close();
			return true;
		} catch (IOException e) {
			Log.w(TAG, "write(byte[], String): Unable to getSnapshot bytes to file.");
			return false;
		}
	}

	/* Saves a given Bitmap to given path on disk.
	 *
	 * @param bitmap, Image to getSnapshot.
	 * @param absFilePath, Absolute path on disk of where to getSnapshot image.
	 *
	 * @return True if image successfully written, false otherwise.
	 */
	@SuppressWarnings("ResultOfMethodCallIgnored")
	public static boolean
	saveBitmap(Bitmap bitmap,
	           String absFilePath) {

		if (null == bitmap || StringUtils.isEmpty(absFilePath))
			return true;

		/* If a previous file already exists then delete it. */
		File file = new File(absFilePath);
		if (file.exists())
			file.delete();

		try (FileOutputStream out = new FileOutputStream(file)) {
			bitmap.compress(Bitmap.CompressFormat.PNG, COMPRESSION_QUALITY, out);
			out.flush();
			out.close();
			return true;
		} catch (Exception e) {
			Log.w(TAG, "FileUtils: Failed to save Bitmpa image to disk.");
			return false;
		}
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	public static void
	delete(String absFilePath) {

		/* If a previous file already exists then delete it. */
		File file = new File(absFilePath);
		if (file.exists())
			file.delete();
	}

	public static Bitmap
	readBitmap(String absFilePath) {

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		return BitmapFactory.decodeFile(absFilePath, options);
	}

	/* Determines if a given file path exists on disk.
	 *
	 * @param absFilePath, Absolute location of file to check.
	 * @return True if file exists, false otherwise.
	 */
	public static boolean
	exists(String absFilePath) {

		return new File(absFilePath).exists();
	}
}
