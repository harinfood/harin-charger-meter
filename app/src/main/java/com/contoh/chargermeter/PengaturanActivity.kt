package com.contoh.chargermeter

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class PengaturanActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Bar atas
        val bar = linear(this, false)
        bar.gravity = Gravity.CENTER_VERTICAL
        bar.setPadding(dp(16), dp(40), dp(16), dp(16))
        bar.setBackgroundColor(Warna.hijauTua)
        val kembali = teks(this, "←  Kembali", 16f, Color.WHITE, true)
        kembali.setOnClickListener { finish() }
        bar.addView(kembali)

        // Isi
        val scroll = ScrollView(this)
        scroll.setBackgroundColor(Warna.latarBelakang)
        val root = linear(this)
        root.setPadding(dp(16), dp(4), dp(16), dp(24))
        scroll.addView(root)

        // --- Pengingat pengisian ---
        root.addView(judulSeksi(this, "Pengingat pengisian"))
        val kIsi = kartu(this)
        val swIsi = Switch(this)
        swIsi.text = "Ingatkan saat baterai mencapai target"
        swIsi.textSize = 16f
        swIsi.isChecked = Pengaturan.pengingatIsi(this)
        swIsi.setOnCheckedChangeListener { _, aktif ->
            Pengaturan.setPengingatIsi(this, aktif)
            if (aktif) mintaIzinNotifikasi()
            PemantauService.sinkronkan(this)
        }
        kIsi.addView(swIsi)

        val txtIsi = teks(this, "", 14f, Warna.teksRedup)
        txtIsi.setPadding(0, dp(12), 0, dp(4))
        val skIsi = SeekBar(this)
        skIsi.max = 50 // 50% sampai 100%
        skIsi.progress = Pengaturan.targetIsi(this) - 50
        txtIsi.text = "Ingatkan saat mencapai ${skIsi.progress + 50}%"
        skIsi.onUbah { p ->
            Pengaturan.setTargetIsi(this, p + 50)
            txtIsi.text = "Ingatkan saat mencapai ${p + 50}%"
        }
        kIsi.addView(txtIsi)
        kIsi.addView(skIsi)
        root.addView(kIsi, kartuLp(this, 8))

        // --- Pengingat baterai rendah ---
        root.addView(judulSeksi(this, "Pengingat baterai rendah"))
        val kRendah = kartu(this)
        val swRendah = Switch(this)
        swRendah.text = "Ingatkan saat baterai rendah"
        swRendah.textSize = 16f
        swRendah.isChecked = Pengaturan.pengingatRendah(this)
        swRendah.setOnCheckedChangeListener { _, aktif ->
            Pengaturan.setPengingatRendah(this, aktif)
            if (aktif) mintaIzinNotifikasi()
            PemantauService.sinkronkan(this)
        }
        kRendah.addView(swRendah)

        val txtRendah = teks(this, "", 14f, Warna.teksRedup)
        txtRendah.setPadding(0, dp(12), 0, dp(4))
        val skRendah = SeekBar(this)
        skRendah.max = 25 // 5% sampai 30%
        skRendah.progress = Pengaturan.ambangRendah(this) - 5
        txtRendah.text = "Ingatkan saat tinggal ${skRendah.progress + 5}%"
        skRendah.onUbah { p ->
            Pengaturan.setAmbangRendah(this, p + 5)
            txtRendah.text = "Ingatkan saat tinggal ${p + 5}%"
        }
        kRendah.addView(txtRendah)
        kRendah.addView(skRendah)
        root.addView(kRendah, kartuLp(this, 8))

        // --- Picture-in-picture ---
        root.addView(judulSeksi(this, "Picture-in-picture"))
        val kPip = kartu(this)
        val swPip = Switch(this)
        swPip.text = "Masuk PiP otomatis saat keluar aplikasi"
        swPip.textSize = 16f
        swPip.isChecked = Pengaturan.pipOtomatis(this)
        swPip.setOnCheckedChangeListener { _, aktif -> Pengaturan.setPipOtomatis(this, aktif) }
        kPip.addView(swPip)
        root.addView(kPip, kartuLp(this, 8))

        // --- Statistik ---
        root.addView(judulSeksi(this, "Statistik"))
        val kStat = kartu(this)
        val reset = teks(this, "Setel ulang statistik arus", 16f, Color.parseColor("#1565C0"), true)
        reset.setOnClickListener {
            Pengaturan.resetStatistik(this)
            Toast.makeText(this, "Statistik arus disetel ulang", Toast.LENGTH_SHORT).show()
        }
        kStat.addView(reset)
        root.addView(kStat, kartuLp(this, 8))

        // --- Keterangan ---
        val info = kartu(this)
        info.addView(
            teks(
                this,
                "Pemantauan latar belakang berjalan saat pengingat aktif, sehingga pengingat tetap bunyi walau aplikasi ditutup. " +
                    "Widget layar utama diperbarui saat ada perubahan baterai dan setiap 30 menit.",
                13f,
                Warna.teksRedup
            )
        )
        root.addView(info, kartuLp(this, 16))

        val outer = linear(this)
        outer.addView(bar)
        outer.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(outer)
    }
}
