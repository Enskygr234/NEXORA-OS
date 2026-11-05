package com.winterdeveloper.nexora.data.apps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Process
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.winterdeveloper.nexora.domain.model.AppKey
import com.winterdeveloper.nexora.domain.model.IconShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads app icons off the main thread and keeps a memory-bounded cache.
 * Adaptive icons are re-masked to the chosen [IconShape]; the system shape is kept for [IconShape.SYSTEM].
 */
class IconLoader(
    private val context: Context,
    private val apps: AppsRepositoryImpl,
) {
    // Cache budget: 1/16 of the app's heap, in KB.
    private val cache = object : LruCache<String, ImageBitmap>(
        (Runtime.getRuntime().maxMemory() / 1024 / 16).toInt()
    ) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4 / 1024
    }

    fun cached(key: AppKey, sizePx: Int, shape: IconShape): ImageBitmap? =
        cache.get(cacheKey(key, sizePx, shape))

    suspend fun load(key: AppKey, sizePx: Int, shape: IconShape): ImageBitmap? {
        val ck = cacheKey(key, sizePx, shape)
        cache.get(ck)?.let { return it }
        return withContext(Dispatchers.Default) {
            val info = apps.activityInfo(key) ?: return@withContext null
            val density = context.resources.displayMetrics.densityDpi
            val bitmap: Bitmap? = try {
                val raw = info.getIcon(density)
                val user = info.user
                val base = render(raw, sizePx, shape)
                if (user != Process.myUserHandle()) {
                    // Work-profile apps keep the system briefcase badge.
                    context.packageManager
                        .getUserBadgedIcon(BitmapDrawable(context.resources, base), user)
                        .toBitmap(sizePx, sizePx)
                } else {
                    base
                }
            } catch (e: Exception) {
                null
            }
            if (bitmap == null) {
                null
            } else {
                val image = bitmap.asImageBitmap()
                cache.put(ck, image)
                image
            }
        }
    }

    fun clear() = cache.evictAll()

    private fun cacheKey(key: AppKey, sizePx: Int, shape: IconShape) = "${key.flatten()}@$sizePx/${shape.name}"

    private fun render(drawable: Drawable, size: Int, shape: IconShape): Bitmap {
        if (shape == IconShape.SYSTEM || drawable !is AdaptiveIconDrawable) {
            return drawable.toBitmap(size, size, Bitmap.Config.ARGB_8888)
        }
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.clipPath(shapePath(shape, size.toFloat()))
        // Adaptive layers are 108dp with a 72dp visible centre: extend bounds by 1/4 on each side.
        val inset = size / 4
        val left = -inset
        val right = size + inset
        drawable.background?.let {
            it.setBounds(left, left, right, right)
            it.draw(canvas)
        }
        drawable.foreground?.let {
            it.setBounds(left, left, right, right)
            it.draw(canvas)
        }
        return bitmap
    }

    private fun shapePath(shape: IconShape, s: Float): Path = Path().apply {
        when (shape) {
            IconShape.CIRCLE -> addCircle(s / 2f, s / 2f, s / 2f, Path.Direction.CW)
            IconShape.ROUNDED_SQUARE -> addRoundRect(RectF(0f, 0f, s, s), s * 0.22f, s * 0.22f, Path.Direction.CW)
            IconShape.SQUIRCLE -> {
                // Superellipse approximation built from four cubic curves.
                val c = s * 0.06f
                moveTo(s / 2f, 0f)
                cubicTo(s - c, 0f, s, c, s, s / 2f)
                cubicTo(s, s - c, s - c, s, s / 2f, s)
                cubicTo(c, s, 0f, s - c, 0f, s / 2f)
                cubicTo(0f, c, c, 0f, s / 2f, 0f)
                close()
            }
            IconShape.SYSTEM -> addRect(0f, 0f, s, s, Path.Direction.CW)
        }
    }
}
