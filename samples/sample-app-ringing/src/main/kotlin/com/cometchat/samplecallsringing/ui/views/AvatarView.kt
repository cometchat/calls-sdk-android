package com.cometchat.samplecallsringing.ui.views

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.cometchat.samplecallsringing.R

/**
 * A round avatar: the image when there is one, otherwise initials on a colour picked from the
 * name, so the same person always gets the same colour. An optional dot shows they are online.
 */
class AvatarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val initials = TextView(context).apply {
        gravity = Gravity.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        includeFontPadding = false
        maxLines = 1
    }
    private val image = ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
    private val presence = View(context).apply {
        setBackgroundResource(R.drawable.bg_presence_dot)
        visibility = GONE
    }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        addView(initials, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(image, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(presence, LayoutParams(0, 0, Gravity.BOTTOM or Gravity.END))
    }

    /**
     * Shows [avatarUrl], or the first letter of up to [initialsCount] words of [name] while it
     * loads and when there is none.
     */
    fun set(name: String?, avatarUrl: String?, initialsCount: Int = 1, isOnline: Boolean = false) {
        val color = palette[colorIndex(name.orEmpty())]
        initials.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
        initials.setTextColor(contrastOn(color))
        initials.text = initialsOf(name, initialsCount)

        Glide.with(this).clear(image)
        image.setImageDrawable(null)
        if (!avatarUrl.isNullOrBlank()) {
            Glide.with(this).load(avatarUrl).transform(CircleCrop()).into(image)
        }
        presence.visibility = if (isOnline) VISIBLE else GONE
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Sized here rather than after layout, so the dot is in place on the first pass.
        val size = MeasureSpec.getSize(widthMeasureSpec)
        initials.setTextSize(TypedValue.COMPLEX_UNIT_PX, size * 0.4f)
        val dot = maxOf(size * 0.3f, 8 * resources.displayMetrics.density).toInt()
        (presence.layoutParams as LayoutParams).apply {
            width = dot
            height = dot
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    companion object {
        /** Shared with the iOS sample, so a name gets the same colour on both. */
        private val palette = intArrayOf(
            0xFF7C6FCF.toInt(), 0xFF5B8FD9.toInt(), 0xFF4FA88C.toInt(), 0xFF8FA65B.toInt(), 0xFFD16B9B.toInt(),
            0xFF5FA9B5.toInt(), 0xFF5C6BC0.toInt(), 0xFF7A8CA6.toInt(), 0xFFC46A6A.toInt(), 0xFF9A6FBF.toInt()
        )

        /** djb2, so a name maps to the same colour on every launch and platform. */
        fun colorIndex(name: String): Int {
            var hash = 5381UL
            name.codePoints().forEach { hash = (hash shl 5) + hash + it.toULong() }
            return (hash % palette.size.toULong()).toInt()
        }

        fun initialsOf(name: String?, maxCount: Int): String {
            val letters = name.orEmpty().trim().split(Regex("\\s+"))
                .filter { it.isNotEmpty() }
                .take(maxOf(1, maxCount))
                .map { it.substring(0, it.offsetByCodePoints(0, 1)).uppercase() }
            return letters.joinToString("").ifEmpty { "?" }
        }

        /** Near-black on light fills, white on dark ones. */
        private fun contrastOn(color: Int): Int =
            if (ColorUtils.calculateLuminance(color) > 0.45) Color.rgb(24, 26, 32) else Color.WHITE
    }
}
