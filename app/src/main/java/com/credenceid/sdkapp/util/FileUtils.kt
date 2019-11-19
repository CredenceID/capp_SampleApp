package com.credenceid.sdkapp.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.*

@Suppress("unused")
object FileUtils {

    private val TAG = FileUtils::class.java.name
    private const val COMPRESSION_QUALITY = 90

    /**
     * Copies file contents from source file to destination file.
	 *
	 * @param src, Source file whose contents will be copied.
	 * @param dst, Destination file where contents will be copied to.
	 *
	 * @return True if copy was successful, false otherwise.
	 */
    fun copy(src: File?,
             dst: File?): Boolean {

        try {
            FileInputStream(src).use { `in` ->
                val out: OutputStream = FileOutputStream(dst)
                val buf = ByteArray(1024)
                var len: Int
                while (`in`.read(buf).also { len = it } > 0) out.write(buf, 0, len)
                return true
            }
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * Reads a given file's data as a byte array and returns read data.
	 *
	 * @param absFilePath, Absolute path on disk of file to read.
	 *
	 * @return If file was not found or could not be read an empty byte array is returned.
	 */
    fun getBytes(absFilePath: String): ByteArray {

        try {
            FileInputStream(absFilePath).use { fis ->
                val bos = ByteArrayOutputStream(0x20000)
                val buf = ByteArray(1024)
                var readNum: Int
                while (fis.read(buf).also { readNum = it } != -1) {
                    bos.write(buf, 0, readNum)
                }
                return bos.toByteArray()
            }
        } catch (e: Exception) {
            Log.w(TAG, "getBytes(String): Unable to read byes from file.")
            return byteArrayOf()
        }
    }

    /**
     * Writes a given byte array to absolute file path on disk.
	 *
	 * @param bytes, Data to write to file.
	 * @param absFilePath, Absolute path on disk of where to getSnapshot bytes.
	 *
	 * @return True if write was successful, false otherwise.
	 */
    fun write(bytes: ByteArray?,
              absFilePath: String): Boolean {

        val f = File(absFilePath)
        try {
            FileOutputStream(f.path).use { fos ->
                fos.write(bytes)
                fos.close()
                return true
            }
        } catch (e: IOException) {
            Log.w(TAG, "write(byte[], String): Unable to getSnapshot bytes to file.")
            return false
        }
    }

    /**
     * Saves a given Bitmap to given path on disk.
	 *
	 * @param bitmap, Image to getSnapshot.
	 * @param absFilePath, Absolute path on disk of where to getSnapshot image.
	 *
	 * @return True if image successfully written, false otherwise.
	 */
    @JvmStatic
    fun saveBitmap(bitmap: Bitmap?,
                   absFilePath: String): Boolean {

        if (null == bitmap || absFilePath.isEmpty())
            return true

        /* If a previous file already exists then delete it. */
        val file = File(absFilePath)
        if (file.exists()) file.delete()
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, COMPRESSION_QUALITY, out)
                out.flush()
                out.close()
                return true
            }
        } catch (e: Exception) {
            Log.w(TAG, "FileUtils: Failed to save Bitmap image to disk.")
            return false
        }
    }

    fun delete(absFilePath: String?) {

        val file = File(absFilePath)
        /* If a previous file already exists then delete it. */
        if (file.exists())
            file.delete()
    }

    fun readBitmap(absFilePath: String?): Bitmap {

        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        return BitmapFactory.decodeFile(absFilePath, options)
    }

    /**
     * Determines if a given file path exists on disk.
	 *
	 * @param absFilePath, Absolute location of file to check.
	 * @return True if file exists, false otherwise.
	 */
    fun exists(absFilePath: String?): Boolean = File(absFilePath).exists()
}