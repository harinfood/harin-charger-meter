
package com.contoh.chargermeter

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.view.View.MeasureSpec
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Meter setengah lingkaran: kiri (merah) = memakai baterai, kanan (hijau) = mengisi daya.
 * Skala -5.000 sampai +5.000 mA. Jarum bergerak halus ke nilai baru.
 * kompak = true untuk tampilan kecil (PiP): tanpa label, garis lebih tipis.
 */
class GaugeView(context: Context, private val kompak: Boolean = false) : View(context) {

    companion object {
        const val MAKS_MA = 5000f
    }

    private var tampil = 0f
    private var targetTerakhir = Float.NaN
    private var animator: ValueAnimator? = null

    /** Menggerakkan jarum dari posisi sekarang ke nilai baru (mA, + mengisi / - memakai). */
    fun setNilai(target: Int) {
        val t = target.toFloat()
        if (t == targetTerakhir) return
        targetTerakhir = t
        animator?.cancel()
        animator = ValueAnimator.ofFloat(tampil, t).apply {
            duration = 800L
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                tampil = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private val tebalGaris = context.dpF(if (kompak) 11f else 20f)
    private val sisaSamping = context.dpF(if (kompak) 6f else 44f)
    private val rect = RectF()

    private val paintLatar = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = tebalGaris
    }
    private val paintAktif = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = tebalGaris
    }
    private val paintGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = tebalGaris * 1.9f
        color = Color.parseColor("#4D2E7D32")
    }
    private val paintTick = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = context.dpF(2f)
        color = Color.parseColor("#90A4AE")
    }
    private val paintJarum = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = context.dpF(if (kompak) 3f else 5f)
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#1B2A33")
    }
    private val paintHub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#1B2A33")
    }
    private val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#607D8B")
        textSize = context.dpF(if (kompak) 9f else 13f)
        textAlign = Paint.Align.CENTER
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val lebar = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(lebar, (lebar * 0.62f).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h * 0.88f
        val r = minOf(w / 2f - sisaSamping, h * 0.80f)
        rect.set(cx - r, cy - r, cx + r, cy + r)

        // Latar: kiri merah muda (memakai), kanan hijau muda (mengisi)
        paintLatar.color = Color.parseColor("#FFB3AE")
        canvas.drawArc(rect, 180f, 90f, false, paintLatar)
        paintLatar.color = Color.parseColor("#A5D6A7")
        canvas.drawArc(rect, 270f, 90f, false, paintLatar)

        val frac = (tampil / MAKS_MA).coerceIn(-1f, 1f)
        val lebarAktif = 90f * abs(frac)
        if (frac > 0f) {
            canvas.drawArc(rect, 270f, lebarAktif, false, paintGlow)
            paintAktif.color = Color.parseColor("#2E7D32")
            canvas.drawArc(rect, 270f, lebarAktif, false, paintAktif)
        } else if (frac < 0f) {
            paintAktif.color = Color.parseColor("#E53935")
            canvas.drawArc(rect, 270f - lebarAktif, lebarAktif, false, paintAktif)
        }

        // Tanda skala setiap 1.000 mA
        val rDalam = r - tebalGaris / 2f
        for (i in -5..5) {
            val sudut = Math.toRadians(270.0 + i * 18.0)
            val cs = cos(sudut).toFloat()
            val sn = sin(sudut).toFloat()
            val r2 = rDalam - context.dpF(3f)
            val panjang = if (i % 5 == 0) context.dpF(if (kompak) 7f else 12f) else context.dpF(if (kompak) 4f else 6f)
            val r1 = r2 - panjang
            canvas.drawLine(cx + cs * r1, cy + sn * r1, cx + cs * r2, cy + sn * r2, paintTick)
        }

        // Label skala (tidak ditampilkan pada mode kompak)
        if (!kompak) {
            val rLabel = r + tebalGaris / 2f + context.dpF(18f)
            gambarLabel(canvas, "-5k", 180.0, cx, cy, rLabel)
            gambarLabel(canvas, "0", 270.0, cx, cy, rLabel)
            gambarLabel(canvas, "5k", 360.0, cx, cy, rLabel)
        }

        // Jarum
        val sudutJarum = Math.toRadians(270.0 + frac * 90.0)
        val panjangJarum = r * 0.9f
        canvas.drawLine(
            cx,
            cy,
            cx + (cos(sudutJarum) * panjangJarum).toFloat(),
            cy + (sin(sudutJarum) * panjangJarum).toFloat(),
            paintJarum
        )
        canvas.drawCircle(cx, cy, context.dpF(if (kompak) 5f else 9f), paintHub)
    }

    private fun gambarLabel(canvas: Canvas, teks: String, derajat: Double, cx: Float, cy: Float, jarak: Float) {
        val rad = Math.toRadians(derajat)
        val x = cx + (cos(rad) * jarak).toFloat()
        val y = cy + (sin(rad) * jarak).toFloat() + paintLabel.textSize / 3f
        canvas.drawText(teks, x, y, paintLabel)
    }
}
