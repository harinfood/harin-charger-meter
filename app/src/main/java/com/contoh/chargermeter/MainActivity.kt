package com.contoh.chargermeter

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Rational
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private var hitungan = 0

    private lateinit var header: View
    private lateinit var bagianDetail: View
    private lateinit var gauge: GaugeView
    private lateinit var txtSubjudul: TextView
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
    private val tile = HashMap<String, Pair<TextView, TextView>>()

    private val jadwal = object : Runnable {
        override fun run() {
            segarkan()
            hitungan++
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buatLayar())
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
        super.onDestroy()
    }

    // ---------- Tata letak ----------

    private fun buatLayar(): View {
        val scroll = ScrollView(this)
        scroll.setBackgroundColor(Warna.latarBelakang)
        scroll.isFillViewport = true

        val root = linear(this)
        root.setPadding(0, 0, 0, dp(28))
        scroll.addView(root)

        header = buatHeader()
        root.addView(header)
        root.addView(buatKartuMeter(), kartuLp(this, 16))

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

        // Nilai perangkat yang tetap
        isiTile("android", Build.VERSION.RELEASE, "", Warna.teksRedup)
        isiTile("pabrikan", Build.MANUFACTURER.replaceFirstChar { it.uppercase() }, "", Warna.teksRedup)
        isiTile("model", Build.MODEL, "", Warna.teksRedup)

        return scroll
    }

    private fun buatHeader(): View {
        val h = LinearLayout(this)
        h.orientation = LinearLayout.HORIZONTAL
        h.gravity = Gravity.CENTER_VERTICAL
        h.setPadding(dp(20), dp(40), dp(12), dp(20))
        h.background = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(Warna.hijauTua, Warna.hijauTerang)
        )

        val kiri = linear(this)
        kiri.addView(teks(this, "Charger Meter", 26f, Color.WHITE, true))
        txtSubjudul = teks(this, "Memuat…", 15f, Color.parseColor("#E8F5E9"))
        kiri.addView(txtSubjudul)
        h.addView(kiri, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        h.addView(tombolTeks("⚙") {
            startActivity(Intent(this, PengaturanActivity::class.java))
        })
        h.addView(tombolPip())
        return h
    }

    private fun tombolLatar(): GradientDrawable {
        val g = GradientDrawable()
        g.setShape(GradientDrawable.OVAL)
        g.setColor(Color.parseColor("#33FFFFFF"))
        return g
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
            t.background = kotak(this, Color.WHITE, 18, Color.parseColor("#E0E6EA"))

            val lbl = teks(this, judul, 13f, Warna.teksRedup)
            lbl.gravity = Gravity.CENTER_HORIZONTAL
            val nilai = teks(this, "-", 17f, Warna.teksUtama, true)
            nilai.gravity = Gravity.CENTER_HORIZONTAL
            val status = teks(this, "", 12f, Warna.teksRedup, true)
            status.gravity = Gravity.CENTER_HORIZONTAL

            t.addView(lbl)
            t.addView(nilai)
            t.addView(status)
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

        txtPersen.text = "${d.persen}%"
        txtSubjudul.text = "${d.statusTeks} · ${d.persen}%"
        gauge.nilai = d.arusBertanda
        txtMa.text = "${d.arusMa} mA"

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

        barBaterai.progress = d.persen
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

        if (hitungan % 5 == 0) WidgetBaterai.perbaruiSemua(this)
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
        val visibilitas = if (isInPip) View.GONE else View.VISIBLE
        header.visibility = visibilitas
        bagianDetail.visibility = visibilitas
    }
}
