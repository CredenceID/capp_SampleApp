package com.credenceid.sdkapp.android.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.RelativeLayout

class PreviewFrameLayout(context: Context,
                         attrs: AttributeSet) : RelativeLayout(context, attrs) {

    private var aspectRatio = 4.0 / 3.0

    init {
        setAspectRatio(4.0 / 3.0)
    }

    fun setAspectRatio(ratio: Double) {

        require(ratio > 0.0)
        if (this.aspectRatio != ratio) {
            this.aspectRatio = ratio
            requestLayout()
        }
    }

    override fun onMeasure(widthSpec: Int,
                           heightSpec: Int) {

        var previewWidth = MeasureSpec.getSize(widthSpec)
        var previewHeight = MeasureSpec.getSize(heightSpec)

        // Get the padding of the border background.
        val hPadding = this.paddingLeft + this.paddingRight
        val vPadding = this.paddingTop + this.paddingBottom

        // Resize the preview frame with correct aspect ratio.
        previewWidth -= hPadding
        previewHeight -= vPadding

        val widthLonger = previewWidth > previewHeight
        var longSide = if (widthLonger) previewWidth else previewHeight
        var shortSide = if (widthLonger) previewHeight else previewWidth

        if (longSide > shortSide * this.aspectRatio)
            longSide = (shortSide.toDouble() * aspectRatio).toInt()
        else
            shortSide = (longSide.toDouble() / aspectRatio).toInt()

        if (widthLonger) {
            previewWidth = longSide
            previewHeight = shortSide
        } else {
            previewWidth = shortSide
            previewHeight = longSide
        }

        // Add the padding of the border.
        previewWidth += hPadding
        previewHeight += vPadding

        // Ask children to follow the new preview dimension.
        val width = MeasureSpec.makeMeasureSpec(previewWidth, MeasureSpec.EXACTLY)
        val height = MeasureSpec.makeMeasureSpec(previewHeight, MeasureSpec.EXACTLY)
        super.onMeasure(width, height)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {

        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x
            val y = event.y
            val touchMajor = event.touchMajor
            val touchMinor = event.touchMinor

            val touchRect = Rect(
                    (x - touchMajor / 2).toInt(),
                    (y - touchMinor / 2).toInt(),
                    (x + touchMajor / 2).toInt(),
                    (y + touchMinor / 2).toInt())
            //(this.context as CameraActivity).performTapToFocus(touchRect)
        }
        return true
    }
}