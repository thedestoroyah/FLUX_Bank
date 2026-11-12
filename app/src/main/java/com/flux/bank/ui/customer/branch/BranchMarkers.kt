package com.flux.bank.ui.customer.branch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.flux.bank.R
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

/**
 * Icon ghim chi nhánh vẽ bằng Canvas: tròn obsidian viền trắng; đang chọn thì lớn hơn, viền emerald kèm quầng nhạt.
 * Chỉ gọi sau khi Google Maps đã khởi tạo (BitmapDescriptorFactory yêu cầu).
 */
internal class BranchMarkers(private val context: Context) {

    val normal: BitmapDescriptor by lazy { draw(selected = false) }
    val selected: BitmapDescriptor by lazy { draw(selected = true) }

    private fun draw(selected: Boolean): BitmapDescriptor {
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
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private companion object {
        const val HALO_ALPHA = 46 // ~18%
        const val SELECTED_CORE = 0.8f
        const val RING_DP = 2.5f
        const val RING_SELECTED_DP = 3.5f
        const val ICON_SCALE = 0.5f
    }
}
