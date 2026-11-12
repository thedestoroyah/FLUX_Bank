package com.flux.bank.ui.customer.branch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.flux.bank.R
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

/**
 * Icon vẽ bằng Canvas cho bản đồ: ghim chi nhánh (tròn obsidian viền trắng; đang chọn thì lớn hơn, viền emerald
 * kèm quầng nhạt) và chấm người dùng (xanh dương viền trắng kèm quầng, theo quy ước app bản đồ).
 */
internal class BranchMarkers(private val context: Context) {

    val normal: Drawable by lazy { draw(selected = false) }
    val selected: Drawable by lazy { draw(selected = true) }
    val user: Drawable by lazy { drawUser() }

    private fun drawUser(): Drawable {
        val res = context.resources
        val size = res.getDimensionPixelSize(R.dimen.map_marker_selected)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val c = size / 2f
        val dot = res.getDimensionPixelSize(R.dimen.map_marker) / 3f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val blue = ContextCompat.getColor(context, R.color.cat_blue)
        paint.color = ColorUtils.setAlphaComponent(blue, USER_HALO_ALPHA)
        canvas.drawCircle(c, c, c, paint)
        paint.color = ContextCompat.getColor(context, R.color.flux_surface)
        canvas.drawCircle(c, c, dot + RING_SELECTED_DP * res.displayMetrics.density, paint)
        paint.color = blue
        canvas.drawCircle(c, c, dot, paint)
        return BitmapDrawable(res, bitmap)
    }

    private fun draw(selected: Boolean): Drawable {
        val res = context.resources
        val size = res.getDimensionPixelSize(if (selected) R.dimen.map_marker_selected else R.dimen.map_marker)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val c = size / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val ink = ContextCompat.getColor(context, R.color.flux_primary)
        val emerald = ContextCompat.getColor(context, R.color.flux_emerald)
        val white = ContextCompat.getColor(context, R.color.flux_surface)
        val density = res.displayMetrics.density
        var radius = c
        if (selected) {
            paint.color = ColorUtils.setAlphaComponent(emerald, HALO_ALPHA)
            canvas.drawCircle(c, c, radius, paint)
            radius = c * SELECTED_CORE
        }
        paint.color = if (selected) emerald else white
        canvas.drawCircle(c, c, radius, paint)
        paint.color = ink
        canvas.drawCircle(c, c, radius - (if (selected) RING_SELECTED_DP else RING_DP) * density, paint)
        val icon = ContextCompat.getDrawable(context, R.drawable.ic_bank)!!.mutate()
        icon.setTint(white)
        val half = (radius * ICON_SCALE).toInt()
        icon.setBounds((c - half).toInt(), (c - half).toInt(), (c + half).toInt(), (c + half).toInt())
        icon.draw(canvas)
        return BitmapDrawable(res, bitmap)
    }

    private companion object {
        const val HALO_ALPHA = 46 // ~18%
        const val USER_HALO_ALPHA = 31 // ~12%
        const val SELECTED_CORE = 0.8f
        const val RING_DP = 2.5f
        const val RING_SELECTED_DP = 3.5f
        const val ICON_SCALE = 0.5f
    }
}
