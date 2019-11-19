package com.credenceid.sdkapp.android.camera

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class DrawingView(context: Context,
                  attrs: AttributeSet) : View(context, attrs) {

    /**
     * Purposely use two separate paints, one for TapToFocus and another for drawing face Rect.
     * TapToFocus is drawn with white paint and face Rect. with green. We could use same object
     * and change paint settings, but this reduces performance. In face detection we want processing
     * and visualization to be very quick, so use two separate objects.
     */
    private val touchPaint: Paint = Paint()
    private val facePaint: Paint = Paint()
    /**
     * Flag indicating is user has touched View since last Draw().
     */
    private var hasTouch: Boolean = false
    /**
     * Rect to hold our user touched area.
     */
    private var touchRect: Rect = Rect()
    /**
     * Scaling used for displaying face Rect.
     */
    private var bitmapScale: Float = 0.toFloat()
    /**
     * Rect. containing found face region.
     */
    private val faceRect: RectF
    /**
     * If true, View will draw "faceRect" to surface.
     */
    private var hasFace: Boolean = false
    /**
     * Used for calculating "true" width of detected face Rect. We need to take into account the
     * given Bitmap image width vs. DrawingView Layout width.
     */
    private var faceImageWidth: Int = 0

    init {
        touchPaint.color = Color.WHITE
        touchPaint.style = Paint.Style.STROKE
        touchPaint.strokeWidth = 4f
        touchPaint.isAntiAlias = true

        facePaint.color = Color.GREEN
        facePaint.style = Paint.Style.STROKE
        facePaint.strokeWidth = 4f
        facePaint.isAntiAlias = true

        hasTouch = false
        hasFace = false

        faceRect = RectF()
    }

    public override fun onDraw(canvas: Canvas) {
        /* If we detected a touch we will draw a green circle where user touched. */
        if (this.hasTouch) {
            /* Grab CENTER coordinates of touch rectangle. */
            val x = (this.touchRect.left + this.touchRect.right) / 2
            val y = (this.touchRect.top + this.touchRect.bottom) / 2

            /* Draw circle at x, y with radius 55 and paint Green. */
            val ttfRadius = 75
            canvas.drawCircle(x.toFloat(), y.toFloat(), ttfRadius.toFloat(), this.touchPaint)
        }

        /* If face was found, then draw face rect. */
        if (this.hasFace)
            canvas.drawRoundRect(faceRect, 10f, 10f, facePaint)
    }

    /**
     * This function is to be called when a user touch event is detected, it tells the DrawingView
     * that the user touched "me" and now "I" need to draw some stuff.
     *
     * @param hasTouch If true this view will draw auto-focus circle, else will not.
     * @param rect Region where tap-to-focus was initiated. This is used to calculate center point
     * 			   from where to draw circle.
     */
    fun setHasTouch(hasTouch: Boolean,
                    rect: Rect) {

        this.hasTouch = hasTouch
        this.touchRect = rect
    }

    /**
     * Tells this view dimensions of Bitmap on which face detections are ran. This view and actual
     * camera preview images are different sizes, so in order for this View to know how to map a
     * point from camera preview image onto its dimensions it needs to be told what camera preview
     * image size it. This way it can determine proper scaling, etc.
     *
     * @param width Width of camera preview frames.
     * @param height Height of camera preview frames.
     */
    fun setBitmapDimensions(width: Int,
                            height: Int) {

        this.bitmapScale = this.layoutParams.height.toFloat() / height
        this.faceImageWidth = (this.bitmapScale * width).toInt()
    }

    /**
     * Tell this View if it should draw facial Rect. on next OnDraw() call. */
    fun setHasFace(hasFace: Boolean) {

        this.hasFace = hasFace
    }

    /**
     * Set coordinates of where to draw face detection rect on next onDraw() call.
     *
     * @param left Top left point of detected face Rect.
     * @param top Top point of detected face Rect.
     * @param right Bottom right point of detected face Rect.
     * @param bottom Bottom point of detected face Rect.
     */
    fun setFaceRect(left: Float,
                    top: Float,
                    right: Float,
                    bottom: Float) {

        val shift = (this.layoutParams.width - this.faceImageWidth) * 0.5f

        this.faceRect.set(shift + left * this.bitmapScale,
                top * this.bitmapScale,
                shift + right * this.bitmapScale,
                bottom * this.bitmapScale)
    }
}