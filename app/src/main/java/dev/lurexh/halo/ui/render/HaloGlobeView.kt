package dev.lurexh.halo.ui.render

import android.app.ActivityManager
import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import android.view.MotionEvent

class HaloGlobeView(
    context: Context,
    onFailure: () -> Unit,
) : GLSurfaceView(context) {

    val renderer = GlobeRenderer { post(Runnable { onFailure() }) }
    var onCountryTap: ((String) -> Unit)? = null

    private var lastX = 0f
    private var lastY = 0f
    private var downX = 0f
    private var downY = 0f
    private var lastDistance = 0f
    private var moved = false

    init {
        val info = (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.deviceConfigurationInfo
        val supported = info != null && info.reqGlEsVersion >= 0x00030000
        if (!supported) {
            post(Runnable { onFailure() })
        } else {
            setEGLContextClientVersion(3)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            holder.setFormat(PixelFormat.TRANSLUCENT)
            setPreserveEGLContextOnPause(true)
            setRenderer(renderer)
            renderMode = RENDERMODE_CONTINUOUSLY
        }
    }

    fun setScene(scene: GlobeScene) {
        renderer.setScene(scene)
    }

    fun dispose() {
        runCatching { queueEvent { runCatching { renderer.dispose() } } }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.getX(0)
                lastY = event.getY(0)
                downX = lastX
                downY = lastY
                lastDistance = 0f
                moved = false
                return true
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount >= 2) {
                    lastDistance = distance(event)
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount >= 2) {
                    val distance = distance(event)
                    if (lastDistance > 0f) {
                        renderer.pinch(distance / lastDistance)
                    }
                    lastDistance = distance
                    moved = true
                } else {
                    val x = event.getX(0)
                    val y = event.getY(0)
                    val dx = x - lastX
                    val dy = y - lastY
                    renderer.drag(dx, dy)
                    lastX = x
                    lastY = y
                    if (kotlin.math.abs(x - downX) > 10f || kotlin.math.abs(y - downY) > 10f) {
                        moved = true
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (!moved) {
                    renderer.hitTest(event.x, event.y)?.let { code ->
                        performClick()
                        onCountryTap?.invoke(code)
                    }
                }
                return true
            }

            MotionEvent.ACTION_CANCEL -> return true
        }
        return super.onTouchEvent(event)
    }

    private fun distance(event: MotionEvent): Float {
        val dx = event.getX(0) - event.getX(1)
        val dy = event.getY(0) - event.getY(1)
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}
