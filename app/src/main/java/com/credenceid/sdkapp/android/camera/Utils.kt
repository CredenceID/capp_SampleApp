@file:Suppress("DEPRECATION")

package com.credenceid.sdkapp.android.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.util.Log
import kotlin.math.abs

@Suppress("DEPRECATION", "unused")
object Utils {
    private val TAG = Utils::class.java.simpleName

    /**
     * Gets best camera frame preview size based on display metrics.
     */
    fun getBestPreviewSize(width: Int,
                           height: Int,
                           parameters: Camera.Parameters): Camera.Size? {

        var result: Camera.Size? = null

        for (size in parameters.supportedPreviewSizes) {
            if (size.width <= width && size.height <= height) {
                if (result == null)
                    result = size
                else {
                    val resultArea = result.width * result.height
                    val newArea = size.width * size.height
                    if (newArea > resultArea)
                        result = size
                }
            }
        }
        return result
    }

    /**
     * Gets optimal camera frame preview size based on display metrics.
     */
    fun getOptimalPreviewSize(w: Int,
                              h: Int,
                              params: Camera.Parameters): Camera.Size? {

        val aspectTolerance = 0.1
        val targetRatio = w.toDouble() / h
        val sizes = params.supportedPictureSizes ?: return null

        var optimalSize: Camera.Size? = null

        var minDiff = java.lang.Double.MAX_VALUE

        for (size in sizes) {
            val ratio = size.width.toDouble() / size.height

            if (abs(ratio - targetRatio) > aspectTolerance)
                continue

            if (abs(size.height - h) < minDiff) {
                optimalSize = size
                minDiff = abs(size.height - h).toDouble()
            }
        }

        if (optimalSize == null) {
            minDiff = java.lang.Double.MAX_VALUE
            for (size in sizes) {
                if (Math.abs(size.height - h) < minDiff) {
                    optimalSize = size
                    minDiff = abs(size.height - h).toDouble()
                }
            }
        }
        return optimalSize
    }

    /**
     * Attempts to open front facing camera on device.
     */
    fun openFrontFacingCamera(): Camera? {

        var cam: Camera? = null
        val cameraInfo = CameraInfo()
        val cameraCount = Camera.getNumberOfCameras()

        for (camIdx in 0 until cameraCount) {
            Camera.getCameraInfo(camIdx, cameraInfo)
            if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    cam = Camera.open(camIdx)
                } catch (e: RuntimeException) {
                    Log.e(TAG, "Camera failed to open: " + e.localizedMessage)
                }

            }
        }
        return cam
    }

    fun getLargestPictureSize(parameters: Camera.Parameters): Camera.Size {

        val sizes = parameters.supportedPictureSizes
        var dimen: Camera.Size? = null

        for (dimens in sizes) {
            if (dimen == null || dimens.width > dimen.width && dimens.height > dimen.height) {
                dimen = dimens
            }
        }

        parameters.setPictureSize(dimen!!.width, dimen.height)
        return dimen
    }

    /**
     * Rotates a given bitmap by angle in degrees.
     *
     * @param source Bitmap to rotate.
     * @param angle Number of degrees by which to rotate image.
     * @return
     */
    fun rotateBitmap(source: Bitmap,
                     angle: Float): Bitmap {

        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
}