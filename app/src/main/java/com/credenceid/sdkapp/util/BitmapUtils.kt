package com.credenceid.sdkapp.util

import android.graphics.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.IntBuffer
import kotlin.experimental.and

object BitmapUtils {

    private const val FACE_CROP_WIDTH = 0.5f
    private const val FACE_CROP_HEIGHT = 0.5f


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
    @JvmStatic
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
     * yuv to rgb
     * @param  yuv yuv data source
     * @param  width Picture width
     * @param  height Picture height
     * @return
     */
    @JvmStatic
    fun yuvToRgb(yuv: ByteArray?, width: Int, height: Int): ByteArray? {
        val bitmap = yuvToBitmap(yuv, width, height)
        return bitmap?.let { bitmapToRgb(it) }
    }

    /**
     * yuv to rgb
     * @param  yuv yuv data source
     * @param  width Picture width
     * @param  height Picture height
     * @return
     */
    @JvmStatic
    fun yuvToRgb(yuvBitmap: Bitmap?): Bitmap? {

        if (null != yuvBitmap) {
            val bitmapBytes = toBytes(yuvBitmap);
            val imageWidth = yuvBitmap.width
            val imageHeight = yuvBitmap.height
            var bitmapOutput = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888)
            val numPixels = (imageWidth * imageHeight) -1

            val dataIs16To240 = false

            // the buffer we fill up which we then fill the bitmap with
            val intBuffer = IntBuffer.allocate(imageWidth * imageHeight);
            // If you're reusing a buffer, next line imperative to refill from the start,
            // if not good practice
            intBuffer.position(0);

            // Set the alpha for the image: 0 is transparent, 255 fully opaque
            val byte_alpha: Byte = 255.toByte();

            // Holding variables for the loop calculation
            var R = 0
            var G = 0
            var B = 0

            // Get each pixel, one at a time
            for (y in 0..imageHeight) {
                for (x in 0..imageWidth) {
                    // Get the Y value, stored in the first block of data
                    // The logical "AND 0xff" is needed to deal with the signed issue
                    var Y = bitmapBytes[y * imageWidth + x] and 255.toByte();

                    // Get U and V values, stored after Y values, one per 2x2 block
                    // of pixels, interleaved. Prepare them as floats with correct range
                    // ready for calculation later.
                    var xby2 = x / 2;
                    var yby2 = y / 2;

                    // make this V for NV12/420SP
                    var U = (bitmapBytes[numPixels + 2 * xby2 + yby2 * imageWidth] and 255.toByte()) - 128.0f;

                    // make this U for NV12/420SP
                    var V = (bitmapBytes[numPixels + 2 * xby2 + 1 + yby2 * imageWidth] and 255.toByte()) - 128.0f;

                    if (dataIs16To240) {
                        // Correct Y to allow for the fact that it is [16..235] and not [0..255]
                        Y = (1.164 * (Y - 16.0)).toByte();

                        // Do the YUV -> RGB conversion
                        // These seem to work, but other variations are quoted
                        // out there.
                        R = ((Y + 1.596f * V).toInt());
                        G = ((Y - 0.813f * V - 0.391f * U).toInt());
                        B = ((Y + 2.018f * U).toInt());
                    } else {
                        // No need to correct Y
                        // These are the coefficients proposed by @AlexCohn
                        // for [0..255], as per the wikipedia page referenced
                        // above
                        R = ((Y + 1.370705f * V).toInt());
                        G = ((Y - 0.698001f * V - 0.337633f * U).toInt());
                        B = ((Y + 1.732446f * U).toInt());
                    }

                    // Clip rgb values to 0-255
                    R = if (R < 0) 0 else (if (R > 255) 255 else R)
                    G = if (G < 0) 0 else (if (G > 255) 255 else G)
                    B = if (B < 0) 0 else (if (B > 255) 255 else B)

                    // Put that pixel in the buffer
                    intBuffer.put(byte_alpha * 16777216 + R * 65536 + G * 256 + B);
                }
            }

            // Get buffer ready to be read
            intBuffer.flip();

            // Push the pixel information from the buffer onto the bitmap.
            bitmapOutput.copyPixelsFromBuffer(intBuffer);

            return bitmapOutput
        } else {
            return null
        }

    }

    /**
     * bitmap to rgb
     * @param  bitmap bitmap data source
     * @return
     */
    @JvmStatic
    fun bitmapToRgb(bitmap: Bitmap): ByteArray? {
        val bytes = bitmap.byteCount
        val buffer: ByteBuffer = ByteBuffer.allocate(bytes)
        bitmap.copyPixelsToBuffer(buffer)
        val rgba: ByteArray = buffer.array()
        val pixels = ByteArray(rgba.size / 4 * 3)
        val count = rgba.size / 4
        for (i in 0 until count) {
            pixels[i * 3] = rgba[i * 4] //R
            pixels[i * 3 + 1] = rgba[i * 4 + 1] //G
            pixels[i * 3 + 2] = rgba[i * 4 + 2] //B
        }
        return pixels
    }

    /**
     * yuv to bitmap
     * @param  nv21 yuv data source
     * @param  width Picture width
     * @param  height Picture height
     * @return
     */
    @JvmStatic
    fun yuvToBitmap(nv21: ByteArray?, width: Int, height: Int): Bitmap? {
        var bitmap: Bitmap? = null
        try {
            val image = YuvImage(nv21, ImageFormat.NV21, width, height, null)
            val stream = ByteArrayOutputStream()
            image.compressToJpeg(Rect(0, 0, width, height), 100, stream)
            bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size())
            stream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return bitmap
    }

    /**
     * Convert byte array to Bitmap.
     *
     * @param data Byte array to convert to Bitmap.
     *
     * @return If data is not Bitmap then NULL is returned.
     */
    @JvmStatic
    fun decode(data: ByteArray?): Bitmap? {

        if (data != null) {
            return try {
                BitmapFactory.decodeByteArray(data, 0, data.size)
            } catch (ignore: Exception) {
                null
            }
        }
        return null
    }

    @JvmStatic
    fun saveBitmap(file: File, bitmap: Bitmap): Boolean {
        try {
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            out.flush()
            out.close()
            return true
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            return false
        }
    }

}