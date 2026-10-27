package com.contoh.chargermeter

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Rational
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.roundToInt

/** Angka yang bergerak halus dari nilai lama ke nilai baru. */
private class AnimasiAngka(private val tampilkan: (Float) -> Unit) {
    private var nilai = 0f
    private var animator: ValueAnimator? = null

    fun ke(target: Float) {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(nilai, target).apply {
            duration = 700L
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                nilai = it.animatedValue as Float
                tampilkan(nilai)
            }
            start()
        }
    }
}

class MainActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private var hitungan = 0

    private lateinit var header: View
    private lateinit var logoHeader: ImageView
    private lateinit var dotStatus: View
    private lateinit var txtSubjudul: TextView
    private lateinit var kartuMeter: LinearLayout
    private lateinit var kartuTip: LinearLayout
    private lateinit var txtTip: TextView
    private lateinit var bagianDetail: View
    private lateinit var gauge: GaugeView
    private lateinit var txtPersen: TextView
    private lateinit var txtMa: TextView
    private lateinit var txtPerJam: TextView
    private lateinit var txtPerJamSub: TextView
    private lateinit var txtPenuh: TextView
    private lateinit var txtPenuhSub: TextView
    private lateinit var txtMaks: TextView
    private lateinit var txtMin: TextView
    private lateinit var barBaterai: ProgressBar
    private lateinit var txtKapasitas: TextView
    private lateinit var txtKesehatan: TextView
    private lateinit var animMa: AnimasiAngka
    private lateinit var animPersen: AnimasiAngka
    private lateinit var animBar: AnimasiAngka
    private var animDot: ObjectAnimator? = null
    private var warnaDotTerakhir = 0
    private val tile = HashMap<String, Pair<TextView, TextView>>()

    // Panel PiP
    private lateinit var scrollUtama: View
    private lateinit var pipPanel: FrameLayout
    private lateinit var pipGauge: GaugeView
    private lateinit var pipPersen: TextView
    private lateinit var pipAngka: TextView
    private lateinit var pipSub: TextView
    private lateinit var pipPill: TextView
    private lateinit var pipPanah: TextView
    private lateinit var pipKolom: LinearLayout
    private lateinit var pipFlash: View
    private val pipDots = mutableListOf<View>()
    private lateinit var animPipPersen: AnimasiAngka
    private var pipHalaman = 0
    private var pipData: DataBaterai? = null
    private var animPanah: ObjectAnimator? = null
    private var arahPanah = 0

    private val jadwal = object : Runnable {
        override fun run() {
            segarkan()
            hitungan++
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scrollUtama = buatLayar()
        pipPanel = buatPipPanel()
        setContentView(scrollUtama)
        mintaIzinNotifikasi()
        PemantauService.sinkronkan(this)
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(jadwal)
        handler.post(jadwal)
    }

    override fun onDestroy() {
        handler.removeCallbacks(jadwal)
        animDot?.cancel()
        super.onDestroy()
    }

    // ---------- Tata letak ----------

    private fun buatLayar(): View {
        val scroll = ScrollView(this)
        scroll.setBackgroundColor(Warna.latarBelakang)
        scroll.isFillViewport = true

        animMa = AnimasiAngka { v -> txtMa.text = formatArus(v.roundToInt()) }
        animPersen = AnimasiAngka { v -> txtPersen.text = "${v.roundToInt()}%" }
        animBar = AnimasiAngka { v -> barBaterai.progress = v.roundToInt() }

        val root = linear(this)
        root.setPadding(0, 0, 0, dp(28))
        scroll.addView(root)

        header = buatHeader()
        root.addView(header)

        kartuMeter = buatKartuMeter()
        root.addView(kartuMeter, kartuLp(this, 16))

        kartuTip = buatKartuTip()
        root.addView(kartuTip, kartuLp(this, 12))

        val detail = linear(this)
        detail.addView(buatKartuKapasitas(), kartuLp(this, 16))
        detail.addView(judulSeksi(this, "Baterai"))
        detail.addView(buatBarisTile("voltase" to "Voltase", "suhu" to "Suhu", "level" to "Level"), kartuLp(this, 4))
        detail.addView(buatBarisTile("daya" to "Daya", "status" to "Status", "charger" to "Charger"), kartuLp(this, 8))
        detail.addView(buatBarisTile("kesehatan" to "Kesehatan", "teknologi" to "Teknologi", "kapasitas" to "Kapasitas"), kartuLp(this, 8))
        detail.addView(judulSeksi(this, "Perangkat"))
        detail.addView(buatBarisTile("android" to "Versi Android", "pabrikan" to "Pabrikan", "model" to "Model"), kartuLp(this, 4))
        bagianDetail = detail
        root.addView(detail)

        isiTile("android", Build.VERSION.RELEASE, "", Warna.teksRedup)
        isiTile("pabrikan", Build.MANUFACTURER.replaceFirstChar { it.uppercase() }, "", Warna.teksRedup)
        isiTile("model", Build.MODEL, "", Warna.teksRedup)

        // Kemunculan bertahap
        munculkan(header, 0)
        munculkan(kartuMeter, 1)
        munculkan(kartuTip, 2)
        munculkan(detail, 3)

        return scroll
    }

    /** Animasi masuk: naik sedikit sambil memudar, dengan jeda berurutan. */
    private fun munculkan(v: View, urutan: Int) {
        v.alpha = 0f
        v.translationY = dpF(28f)
        v.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(urutan * 110L)
            .setDuration(520L)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun titik(warna: Int): GradientDrawable {
        val g = GradientDrawable()
        g.setShape(GradientDrawable.OVAL)
        g.setColor(warna)
        return g
    }

    private fun buatHeader(): View {
        val h = linear(this, false)
        h.gravity = Gravity.CENTER_VERTICAL
        h.setPadding(dp(18), dp(40), dp(12), dp(18))
        h.background = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(Warna.hijauTua, Warna.hijauTerang)
        )

        // Logo aplikasi dengan sudut luar transparan (tanpa bingkai putih)
        logoHeader = ImageView(this)
        logoHeader.setImageResource(R.drawable.logo_charger)
        logoHeader.scaleType = ImageView.ScaleType.FIT_CENTER
        logoHeader.scaleX = 0.5f
        logoHeader.scaleY = 0.5f
        logoHeader.animate().scaleX(1f).scaleY(1f).setDuration(800L).setInterpolator(OvershootInterpolator()).start()
        h.addView(logoHeader, LinearLayout.LayoutParams(dp(66), dp(66)).apply { marginEnd = dp(12) })
        ObjectAnimator.ofFloat(logoHeader, "translationY", 0f, -dpF(5f)).apply {
            setDuration(1600L)
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            start()
        }

        val kolom = linear(this)
        kolom.addView(teks(this, "Harin Charger Meter", 20f, Color.WHITE, true))

        val baris = linear(this, false)
        baris.gravity = Gravity.CENTER_VERTICAL
        dotStatus = View(this)
        dotStatus.background = titik(Color.parseColor("#B0BEC5"))
        baris.addView(dotStatus, LinearLayout.LayoutParams(dp(9), dp(9)).apply { marginEnd = dp(6) })
        txtSubjudul = teks(this, "Memuat…", 14f, Color.parseColor("#E8F5E9"))
        baris.addView(txtSubjudul)
        kolom.addView(baris)

        h.addView(kolom, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        h.addView(tombolTeks("⚙") { startActivity(Intent(this, PengaturanActivity::class.java)) })
        h.addView(tombolPip())
        return h
    }

    private fun tombolLatar(): RippleDrawable {
        val g = GradientDrawable()
        g.setShape(GradientDrawable.OVAL)
        g.setColor(Color.parseColor("#33FFFFFF"))
        return RippleDrawable(ColorStateList.valueOf(Color.parseColor("#66FFFFFF")), g, null)
    }

    private fun tombolTeks(label: String, aksi: () -> Unit): TextView {
        val t = TextView(this)
        t.text = label
        t.textSize = 22f
        t.setTextColor(Color.WHITE)
        t.gravity = Gravity.CENTER
        t.background = tombolLatar()
        t.layoutParams = LinearLayout.LayoutParams(dp(44), dp(44)).apply { marginStart = dp(8) }
        t.setOnClickListener { aksi() }
        return t
    }

    private fun tombolPip(): ImageView {
        val iv = ImageView(this)
        iv.setImageResource(R.drawable.ic_pip)
        iv.setPadding(dp(10), dp(10), dp(10), dp(10))
        iv.background = tombolLatar()
        iv.layoutParams = LinearLayout.LayoutParams(dp(44), dp(44)).apply { marginStart = dp(8) }
        iv.setOnClickListener { masukPip() }
        return iv
    }

    private fun buatKartuMeter(): LinearLayout {
        val k = kartu(this)

        val atas = linear(this, false)
        atas.gravity = Gravity.CENTER_VERTICAL
        val kiriAtas = linear(this)
        txtPersen = teks(this, "--%", 34f, Warna.teksUtama, true)
        kiriAtas.addView(txtPersen)
        kiriAtas.addView(teks(this, "Baterai", 14f, Warna.teksRedup))
        atas.addView(kiriAtas, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        k.addView(atas)

        gauge = GaugeView(this)
        k.addView(gauge, lp(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0f, dp(4), 0, 0, 0))

        txtMa = teks(this, "0 mA", 42f, Warna.teksUtama, true)
        txtMa.gravity = Gravity.CENTER_HORIZONTAL
        k.addView(txtMa, lp(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        val stat = linear(this, false)
        val statKiri = linear(this)
        txtPerJam = teks(this, "--", 20f, Warna.teksUtama, true)
        txtPerJamSub = teks(this, "-", 13f, Warna.teksRedup)
        statKiri.addView(txtPerJam)
        statKiri.addView(txtPerJamSub)
        val statKanan = linear(this)
        txtPenuh = teks(this, "--", 20f, Warna.teksUtama, true)
        txtPenuhSub = teks(this, "-", 13f, Warna.teksRedup)
        statKanan.addView(txtPenuh)
        statKanan.addView(txtPenuhSub)
        stat.addView(statKiri, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        stat.addView(statKanan, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        k.addView(stat, lp(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0f, dp(12), 0, 0, 0))

        k.addView(garisPemisah(this), lp(LinearLayout.LayoutParams.MATCH_PARENT, dp(1), 0f, dp(14), 0, 0, 0))

        val bawah = linear(this, false)
        bawah.gravity = Gravity.CENTER_VERTICAL
        txtMaks = teks(this, "--", 14f, Warna.teksUtama, true)
        txtMin = teks(this, "--", 14f, Warna.teksUtama, true)
        val kiriBawah = linear(this)
        kiriBawah.addView(barisNilai("Maksimum sepanjang waktu", txtMaks))
        kiriBawah.addView(barisNilai("Minimum sepanjang waktu", txtMin))
        bawah.addView(kiriBawah, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val reset = teks(this, "Setel ulang", 14f, Color.parseColor("#1565C0"), true)
        reset.setPadding(dp(8), dp(8), 0, dp(8))
        reset.setOnClickListener {
            Pengaturan.resetStatistik(this)
            Toast.makeText(this, "Statistik arus disetel ulang", Toast.LENGTH_SHORT).show()
            segarkan()
        }
        bawah.addView(reset)
        k.addView(bawah, lp(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0f, dp(12), 0, 0, 0))
        return k
    }

    private fun buatKartuTip(): LinearLayout {
        val k = linear(this, false)
        k.gravity = Gravity.CENTER_VERTICAL
        k.setPadding(dp(16), dp(14), dp(16), dp(14))
        k.background = kotak(this, Color.parseColor("#E8F5E9"), 18)
        txtTip = teks(this, "Memuat…", 14f, Warna.teksUtama)
        k.addView(txtTip, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        return k
    }

    private fun barisNilai(label: String, nilai: TextView): LinearLayout {
        val b = linear(this, false)
        b.gravity = Gravity.CENTER_VERTICAL
        b.addView(teks(this, label, 14f, Warna.teksRedup), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        b.addView(nilai)
        return b
    }

    private fun buatKartuKapasitas(): LinearLayout {
        val k = kartu(this)

        val atas = linear(this, false)
        atas.gravity = Gravity.CENTER_VERTICAL
        atas.addView(
            teks(this, "Kapasitas baterai", 17f, Warna.teksUtama, true),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )
        txtKesehatan = teks(this, "Kesehatan -", 13f, Color.parseColor("#8D6E00"), true)
        txtKesehatan.background = kotak(this, Color.parseColor("#FFF3E0"), 14)
        txtKesehatan.setPadding(dp(12), dp(6), dp(12), dp(6))
        atas.addView(txtKesehatan)
        k.addView(atas)

        txtKapasitas = teks(this, "-", 14f, Warna.teksRedup)
        k.addView(txtKapasitas, lp(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0f, dp(6), 0, 0, 0))

        barBaterai = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        barBaterai.max = 100
        barBaterai.progressTintList = ColorStateList.valueOf(Warna.hijauTerang)
        barBaterai.progressBackgroundTintList = ColorStateList.valueOf(Color.parseColor("#E0E6EA"))
        k.addView(barBaterai, lp(LinearLayout.LayoutParams.MATCH_PARENT, dp(14), 0f, dp(12), 0, 0, 0))
        return k
    }

    private fun buatBarisTile(vararg daftar: Pair<String, String>): LinearLayout {
        val b = linear(this, false)
        for ((kunci, judul) in daftar) {
            val t = linear(this)
            t.gravity = Gravity.CENTER_HORIZONTAL
            t.setPadding(dp(6), dp(14), dp(6), dp(14))
            t.background = RippleDrawable(
                ColorStateList.valueOf(Color.parseColor("#33FB8C00")),
                kotak(this, Color.WHITE, 18, Color.parseColor("#E0E6EA")),
                null
            )

            val lbl = teks(this, judul, 13f, Warna.teksRedup)
            lbl.gravity = Gravity.CENTER_HORIZONTAL
            val nilai = teks(this, "-", 17f, Warna.teksUtama, true)
            nilai.gravity = Gravity.CENTER_HORIZONTAL
            val status = teks(this, "", 12f, Warna.teksRedup, true)
            status.gravity = Gravity.CENTER_HORIZONTAL

            t.addView(lbl)
            t.addView(nilai)
            t.addView(status)
            t.setOnClickListener {
                val info = if (status.text.isNullOrEmpty()) nilai.text else "${nilai.text} (${status.text})"
                Toast.makeText(this, "$judul: $info", Toast.LENGTH_SHORT).show()
            }
            tile[kunci] = Pair(nilai, status)

            val p = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            p.setMargins(dp(4), dp(4), dp(4), dp(4))
            b.addView(t, p)
        }
        return b
    }

    private fun isiTile(kunci: String, nilai: String, status: String, warnaStatus: Int) {
        val p = tile[kunci] ?: return
        p.first.text = nilai
        p.second.text = status
        p.second.setTextColor(warnaStatus)
    }

    // ---------- Pembaruan data ----------

    private fun segarkan() {
        val d = BatteriInfo.baca(this)
        Pengaturan.catatArus(this, d.arusMa)

        // Header & status
        txtSubjudul.text = "${d.statusTeks} · ${d.persen}%"
        val warnaDot = warnaDot(d)
        if (warnaDot != warnaDotTerakhir) {
            warnaDotTerakhir = warnaDot
            dotStatus.background = titik(warnaDot)
        }
        aturDenyut(d.arusBertanda > 0)

        // Meter & angka yang bergerak halus
        animPersen.ke(d.persen.toFloat())
        gauge.setNilai(d.arusBertanda)
        animMa.ke(d.arusBertanda.toFloat())
        txtMa.setTextColor(warnaArus(d))
        animBar.ke(d.persen.toFloat())

        txtPerJam.text = d.persenPerJam?.let { "%.0f%% per jam".format(it) } ?: "--"
        txtPerJamSub.text = if (d.terhubung) {
            "${d.sumberCharger} · %.1f W".format(d.dayaWatt)
        } else {
            "Dipakai · %.1f W".format(d.dayaWatt)
        }
        txtPenuh.text = when {
            d.menitPenuh == null -> "--"
            d.menitPenuh == 0 -> "Penuh"
            else -> "${d.menitPenuh} mnt"
        }
        txtPenuhSub.text = when {
            d.jamPenuh != null -> "Penuh pukul ${d.jamPenuh}"
            d.terhubung -> "Menghitung…"
            else -> "Belum mengisi daya"
        }

        val maks = Pengaturan.maksArus(this)
        val min = Pengaturan.minArus(this)
        txtMaks.text = if (maks > 0) "$maks mA" else "--"
        txtMin.text = if (min > 0) "$min mA" else "--"

        // Tips yang menyesuaikan kondisi baterai
        val (tipTeks, tipWarna) = tipUntuk(d)
        txtTip.text = tipTeks
        kartuTip.background = kotak(this, tipWarna, 18)

        // Kapasitas
        txtKapasitas.text = if (d.kapasitasMah > 0) {
            "Kapasitas terbaca ${d.kapasitasMah} mAh"
        } else {
            "Kapasitas tidak tersedia di HP ini"
        }
        txtKesehatan.text = "Kesehatan ${d.kesehatan}"

        // Kartu-kartu baterai
        val (sv, cv) = statusVoltase(d.voltase)
        isiTile("voltase", "%.2f V".format(d.voltase), sv, cv)
        val (ss, cs) = statusSuhu(d.suhuC)
        isiTile("suhu", "%.1f °C".format(d.suhuC), ss, cs)
        val (sl, cl) = statusLevel(d.persen)
        isiTile("level", "${d.persen}%", sl, cl)
        isiTile(
            "daya",
            "%.2f W".format(d.dayaWatt),
            if (d.terhubung) (if (d.dayaWatt >= 10f) "Cepat" else "Normal") else "Dipakai",
            if (d.terhubung) Warna.hijau else Warna.teksRedup
        )
        isiTile("status", d.statusTeks, if (d.terhubung) "Terhubung" else "Lepas", warnaStatus(d))
        isiTile("charger", d.sumberCharger, "", Warna.teksUtama)
        isiTile("kesehatan", d.kesehatan, "", if (d.kesehatan == "Baik") Warna.hijau else Warna.oranye)
        isiTile("teknologi", d.teknologi, "", Warna.teksUtama)
        isiTile(
            "kapasitas",
            if (d.kapasitasMah > 0) "${d.kapasitasMah} mAh" else "-",
            "",
            Warna.teksUtama
        )

        perbaruiPip(d)
        if (hitungan % 5 == 0) WidgetBaterai.perbaruiSemua(this)
    }

    /** Pesan singkat yang menyesuaikan kondisi, dengan warna latar yang sesuai. */
    private fun tipUntuk(d: DataBaterai): Pair<String, Int> {
        val hijau = Color.parseColor("#E8F5E9")
        val merah = Color.parseColor("#FFEBEE")
        val biru = Color.parseColor("#E3F2FD")
        return when {
            d.suhuC >= 45f -> Pair(
                "🌡️ Suhu baterai tinggi (%.1f °C). Lepas charger sementara dan kurangi pemakaian berat.".format(d.suhuC),
                merah
            )
            d.terhubung && d.persen >= Pengaturan.targetIsi(this) -> Pair(
                "🔌 Baterai sudah ${d.persen}%. Cabut charger agar baterai lebih awet.",
                hijau
            )
            d.terhubung && d.jamPenuh != null -> Pair(
                "⚡ Sedang mengisi daya. Perkiraan penuh pukul ${d.jamPenuh}, sekitar ${d.menitPenuh} menit lagi.",
                hijau
            )
            d.terhubung -> Pair("⚡ Charger terhubung. Menunggu data pengisian…", hijau)
            d.persen <= Pengaturan.ambangRendah(this) -> Pair(
                "⚠️ Baterai tinggal ${d.persen}%. Segera hubungkan charger.",
                merah
            )
            else -> Pair("🔋 Sedang memakai baterai. Kurangi kecerahan layar untuk menghemat daya.", biru)
        }
    }

    private fun warnaDot(d: DataBaterai): Int = when {
        d.arusBertanda > 0 -> Color.parseColor("#B9F6CA")
        d.arusBertanda < 0 -> Color.parseColor("#FFB74D")
        d.terhubung -> Color.parseColor("#80D8FF")
        else -> Color.parseColor("#B0BEC5")
    }

    /** Titik status berdenyut saat baterai sedang diisi. */
    private fun aturDenyut(aktif: Boolean) {
        if (aktif && animDot == null) {
            animDot = ObjectAnimator.ofPropertyValuesHolder(
                dotStatus,
                PropertyValuesHolder.ofFloat("scaleX", 1f, 1.7f),
                PropertyValuesHolder.ofFloat("scaleY", 1f, 1.7f)
            ).apply {
                setDuration(900L)
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.REVERSE
                start()
            }
        } else if (!aktif && animDot != null) {
            animDot?.cancel()
            animDot = null
            dotStatus.scaleX = 1f
            dotStatus.scaleY = 1f
        }
    }

    /** Arus bertanda: + saat mengisi, - saat memakai baterai. */
    private fun formatArus(ma: Int): String = when {
        ma < 0 -> "-${-ma} mA"
        ma > 0 -> "+$ma mA"
        else -> "0 mA"
    }

    private fun warnaArus(d: DataBaterai): Int = when {
        d.arusBertanda < 0 -> Color.parseColor("#D32F2F")
        d.arusBertanda > 0 -> Warna.hijau
        else -> Warna.teksUtama
    }

    private fun statusVoltase(v: Float): Pair<String, Int> = when {
        v < 3.4f -> Pair("Rendah", Warna.oranye)
        v > 4.35f -> Pair("Tinggi", Warna.oranye)
        else -> Pair("Normal", Warna.hijau)
    }

    private fun statusSuhu(c: Float): Pair<String, Int> = when {
        c < 15f -> Pair("Dingin", Warna.biru)
        c < 40f -> Pair("Normal", Warna.hijau)
        c < 45f -> Pair("Hangat", Warna.oranye)
        else -> Pair("Panas", Warna.merah)
    }

    private fun statusLevel(p: Int): Pair<String, Int> = when {
        p <= 15 -> Pair("Rendah", Warna.merah)
        p <= 50 -> Pair("Sedang", Warna.oranye)
        else -> Pair("Baik", Warna.hijau)
    }

    private fun warnaStatus(d: DataBaterai): Int = when {
        d.arusBertanda > 0 -> Warna.hijau
        d.arusBertanda < 0 -> Warna.oranye
        d.terhubung -> Warna.biru
        else -> Warna.teksRedup
    }

    // ---------- Picture-in-picture ----------

    private fun masukPip() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Toast.makeText(this, "Perangkat ini tidak mendukung PiP", Toast.LENGTH_SHORT).show()
            return
        }
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .build()
        enterPictureInPictureMode(params)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Pengaturan.pipOtomatis(this)) masukPip()
    }

    override fun onPictureInPictureModeChanged(isInPip: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPip, newConfig)
        if (isInPip) {
            pipData?.let { renderPipKolom(it) }
            setContentView(pipPanel)
            pipPanel.alpha = 0f
            pipPanel.scaleX = 0.9f
            pipPanel.scaleY = 0.9f
            pipPanel.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(420L).setInterpolator(OvershootInterpolator()).start()
        } else {
            setContentView(scrollUtama)
        }
    }

    /** Panel PiP: gauge arus di kiri, angka dan status di kanan, bisa ditekan untuk ganti halaman. */
    private fun buatPipPanel(): FrameLayout {
        val root = FrameLayout(this)
        val latar = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Warna.hijauTua, Warna.hijauTerang))
        latar.cornerRadius = dpF(18f)
        root.background = RippleDrawable(ColorStateList.valueOf(Color.parseColor("#40FFFFFF")), latar, null)
        root.isClickable = true
        root.setOnClickListener { onPipTap() }

        val baris = linear(this, false)
        baris.gravity = Gravity.CENTER_VERTICAL
        baris.setPadding(dp(8), dp(6), dp(10), dp(6))

        // Kartu putih untuk gauge
        val kartuGauge = linear(this)
        kartuGauge.gravity = Gravity.CENTER
        kartuGauge.setPadding(dp(4), dp(4), dp(4), dp(4))
        kartuGauge.background = kotak(this, Color.WHITE, 14)
        pipGauge = GaugeView(this, kompak = true)
        kartuGauge.addView(pipGauge, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        baris.addView(kartuGauge, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.4f))

        // Kolom informasi
        pipKolom = linear(this)
        pipKolom.setPadding(dp(10), 0, 0, 0)
        pipPersen = teks(this, "--%", 26f, Color.WHITE, true)
        pipAngka = teks(this, "0 mA", 19f, Color.WHITE, true)
        pipSub = teks(this, "-", 10f, Color.parseColor("#C8E6C9"))
        pipKolom.addView(pipPersen)
        pipKolom.addView(pipAngka)
        pipKolom.addView(pipSub)

        val barisPill = linear(this, false)
        barisPill.gravity = Gravity.CENTER_VERTICAL
        pipPill = teks(this, "-", 10f, Warna.hijauTua, true)
        pipPill.setPadding(dp(7), dp(2), dp(7), dp(2))
        pipPill.background = kotak(this, Color.WHITE, 10)
        barisPill.addView(pipPill)
        pipPanah = teks(this, "»»»", 11f, Color.parseColor("#B9F6CA"), true)
        pipPanah.setPadding(dp(6), 0, 0, 0)
        barisPill.addView(pipPanah)
        pipKolom.addView(barisPill, lp(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0f, dp(4), 0, 0, 0))

        // Indikator halaman
        val barisDots = linear(this, false)
        for (i in 0 until 3) {
            val titikHal = View(this)
            titikHal.background = titik(Color.WHITE)
            barisDots.addView(titikHal, LinearLayout.LayoutParams(dp(6), dp(6)).apply { setMargins(dp(3), 0, dp(3), 0) })
            pipDots.add(titikHal)
        }
        pipKolom.addView(barisDots, lp(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0f, dp(6), 0, 0, 0))

        baris.addView(pipKolom, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(baris, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        // Kilatan cahaya saat ditekan
        pipFlash = View(this)
        pipFlash.setBackgroundColor(Color.WHITE)
        pipFlash.alpha = 0f
        root.addView(pipFlash, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        animPipPersen = AnimasiAngka { v -> pipPersen.text = "${v.roundToInt()}%" }
        return root
    }

    /** Animasi tekan: panel membal, kilatan cahaya, lalu isi kolom berganti halaman dengan geser halus. */
    private fun onPipTap() {
        pipPanel.animate().cancel()
        pipPanel.scaleX = 0.94f
        pipPanel.scaleY = 0.94f
        pipPanel.animate().scaleX(1f).scaleY(1f).setDuration(460L)
            .setInterpolator(OvershootInterpolator(2f)).start()

        pipFlash.animate().cancel()
        pipFlash.alpha = 0.3f
        pipFlash.animate().alpha(0f).setDuration(400L).start()

        pipHalaman = (pipHalaman + 1) % 3
        pipKolom.animate().cancel()
        pipKolom.animate().alpha(0f).translationX(dpF(16f)).setDuration(130L).withEndAction {
            pipKolom.translationX = -dpF(16f)
            pipData?.let { renderPipKolom(it) }
            pipKolom.animate().alpha(1f).translationX(0f).setDuration(240L)
                .setInterpolator(DecelerateInterpolator()).start()
        }.start()
    }

    private fun renderPipKolom(d: DataBaterai) {
        when (pipHalaman) {
            0 -> {
                pipAngka.text = formatArus(d.arusBertanda)
                pipSub.text = when {
                    d.arusBertanda > 0 -> "Arus mengalir masuk"
                    d.arusBertanda < 0 -> "Arus mengalir keluar"
                    else -> "Tidak ada arus"
                }
                pipPill.text = d.statusTeks
            }
            1 -> {
                pipAngka.text = "%.1f W".format(d.dayaWatt)
                pipSub.text = d.persenPerJam?.let { "%.0f%% per jam".format(it) } ?: "Kecepatan belum terbaca"
                pipPill.text = if (d.terhubung) d.sumberCharger else "Dipakai"
            }
            else -> {
                pipAngka.text = when {
                    d.menitPenuh == null -> "--"
                    d.menitPenuh == 0 -> "Penuh"
                    else -> "${d.menitPenuh} mnt"
                }
                pipSub.text = d.jamPenuh?.let { "Penuh pukul $it" } ?: "Belum mengisi daya"
                pipPill.text = "Perkiraan penuh"
            }
        }
        pipPill.setTextColor(warnaStatus(d))
        for (i in pipDots.indices) pipDots[i].alpha = if (i == pipHalaman) 1f else 0.35f
    }

    private fun perbaruiPip(d: DataBaterai) {
        pipData = d
        pipGauge.setNilai(d.arusBertanda)
        animPipPersen.ke(d.persen.toFloat())
        renderPipKolom(d)
        aturAliranPip(d.arusBertanda)
    }

    /** Panah berjalan searah aliran arus: ke kanan saat mengisi, ke kiri saat memakai baterai. */
    private fun aturAliranPip(arus: Int) {
        val arah = when {
            arus > 0 -> 1
            arus < 0 -> -1
            else -> 0
        }
        if (arah == arahPanah) return
        arahPanah = arah
        animPanah?.cancel()
        animPanah = null
        pipPanah.translationX = 0f
        if (arah == 0) {
            pipPanah.alpha = 0.3f
            return
        }
        pipPanah.alpha = 1f
        animPanah = ObjectAnimator.ofFloat(pipPanah, "translationX", 0f, dpF(10f) * arah).apply {
            setDuration(700L)
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.RESTART
            interpolator = LinearInterpolator()
            start()
        }
    }
}
