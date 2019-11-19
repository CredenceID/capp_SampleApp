package com.credenceid.sdkapp.util

import android.graphics.*
import java.io.ByteArrayOutputStream

object BitmapUtils {

    /**
     * Rotates given Bitmap by "angle" degrees.
	 *
	 * @param in Bitmap to rotate.
	 * @param angle Degrees to rotate Bitmap by.
	 *
	 * @return Rotated Bitmap.
	 */
    @JvmStatic
    fun rotate(bitmap: Bitmap,
               angle: Int): Bitmap {

        val mat = Matrix()
        mat.postRotate(angle.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, mat, true)
    }

    /**
     * Converts a given Bitmap to byte array.
	 *
	 * @param bitmap Bitmap to convert to byte array.
	 *
	 * @return If Bitmap is null empty byte array is returned, else converted Bitmap in byte form.
	 */
    fun toBytes(bitmap: Bitmap?): ByteArray {

        return if (null == bitmap)
            byteArrayOf()
        else {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.toByteArray()
        }
    }

    /**
     * Convert byte array to Bitmap.
	 *
	 * @param data Byte array to convert to Bitmap.
	 *
	 * @return If data is not Bitmap then NULL is returned.
	 */
    @JvmStatic
    fun decode(data: ByteArray): Bitmap? {

        return try {
            BitmapFactory.decodeByteArray(data, 0, data.size)
        } catch (ignore: Exception) {
            null
        }
    }
}