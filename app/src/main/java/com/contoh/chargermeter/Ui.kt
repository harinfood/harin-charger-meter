package com.contoh.chargermeter

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

fun Context.dp(v: Int): Int = Math.round(v * resources.displayMetrics.density)

fun Context.dpF(v: Float): Float = v * resources.displayMetrics.density

object Warna {
    val hijauTua = Color.parseColor("#1B5E20")
    val hijau = Color.parseColor("#2E7D32")
    val hijauTerang = Color.parseColor("#43A047")
    val oranye = Color.parseColor("#E65100")
    val merah = Color.parseColor("#C62828")
    val biru = Color.parseColor("#1565C0")
    val latarBelakang = Color.parseColor("#F3F6F8")
    val teksUtama = Color.parseColor("#1B2A33")
    val teksRedup = Color.parseColor("#607D8B")
}

fun kotak(ctx: Context, warna: Int, radiusDp: Int, garis: Int = 0): GradientDrawable {
    val g = GradientDrawable()
    g.setColor(warna)
    g.setCornerRadius(ctx.dpF(radiusDp.toFloat()))
    if (garis != 0) g.setStroke(ctx.dp(1), garis)
    return g
}

fun teks(ctx: Context, isi: String, sp: Float, warna: Int, tebal: Boolean = false): TextView {
    val t = TextView(ctx)
    t.text = isi
    t.textSize = sp
    t.setTextColor(warna)
    if (tebal) t.typeface = Typeface.DEFAULT_BOLD
    return t
}

fun linear(ctx: Context, vertikal: Boolean = true): LinearLayout {
    val l = LinearLayout(ctx)
    l.orientation = if (vertikal) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
    return l
}

fun lp(
    lebar: Int,
    tinggi: Int,
    bobot: Float = 0f,
    atas: Int = 0,
    kiri: Int = 0,
    kanan: Int = 0,
    bawah: Int = 0
): LinearLayout.LayoutParams {
    val p = LinearLayout.LayoutParams(lebar, tinggi, bobot)
    p.setMargins(kiri, atas, kanan, bawah)
    return p
}

fun kartu(ctx: Context): LinearLayout {
    val k = linear(ctx)
    k.setPadding(ctx.dp(18), ctx.dp(16), ctx.dp(18), ctx.dp(16))
    k.background = kotak(ctx, Color.WHITE, 22, Color.parseColor("#E0E6EA"))
    return k
}

fun kartuLp(ctx: Context, atas: Int = 16): LinearLayout.LayoutParams =
    lp(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
        0f,
        ctx.dp(atas),
        ctx.dp(16),
        ctx.dp(16),
        0
    )

fun judulSeksi(ctx: Context, isi: String): TextView {
    val t = teks(ctx, isi, 15f, Warna.teksRedup, true)
    t.setPadding(ctx.dp(18), ctx.dp(18), ctx.dp(16), ctx.dp(4))
    return t
}

fun garisPemisah(ctx: Context): View {
    val v = View(ctx)
    v.setBackgroundColor(Color.parseColor("#E0E6EA"))
    return v
}

fun SeekBar.onUbah(aksi: (Int) -> Unit) {
    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
            aksi(progress)
        }

        override fun onStartTrackingTouch(sb: SeekBar?) {}

        override fun onStopTrackingTouch(sb: SeekBar?) {}
    })
}

fun Activity.mintaIzinNotifikasi() {
    if (Build.VERSION.SDK_INT >= 33 &&
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 201)
    }
}
