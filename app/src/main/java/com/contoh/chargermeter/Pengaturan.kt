package com.contoh.chargermeter

import android.content.Context

/** Penyimpanan pengaturan dan statistik aplikasi. */
object Pengaturan {
    private const val NAMA = "charger_meter"

    private fun prefs(c: Context) = c.getSharedPreferences(NAMA, Context.MODE_PRIVATE)

    fun pengingatIsi(c: Context): Boolean = prefs(c).getBoolean("pengingat_isi", true)

    fun setPengingatIsi(c: Context, v: Boolean) {
        prefs(c).edit().putBoolean("pengingat_isi", v).apply()
    }

    fun targetIsi(c: Context): Int = prefs(c).getInt("target_isi", 100)

    fun setTargetIsi(c: Context, v: Int) {
        prefs(c).edit().putInt("target_isi", v).apply()
    }

    fun pengingatRendah(c: Context): Boolean = prefs(c).getBoolean("pengingat_rendah", true)

    fun setPengingatRendah(c: Context, v: Boolean) {
        prefs(c).edit().putBoolean("pengingat_rendah", v).apply()
    }

    fun ambangRendah(c: Context): Int = prefs(c).getInt("ambang_rendah", 10)

    fun setAmbangRendah(c: Context, v: Int) {
        prefs(c).edit().putInt("ambang_rendah", v).apply()
    }

    fun pipOtomatis(c: Context): Boolean = prefs(c).getBoolean("pip_otomatis", false)

    fun setPipOtomatis(c: Context, v: Boolean) {
        prefs(c).edit().putBoolean("pip_otomatis", v).apply()
    }

    fun pengingatAktif(c: Context): Boolean = pengingatIsi(c) || pengingatRendah(c)

    // Status pengingat agar notifikasi tidak berulang
    fun sudahIsi(c: Context): Boolean = prefs(c).getBoolean("sudah_isi", false)

    fun setSudahIsi(c: Context, v: Boolean) {
        prefs(c).edit().putBoolean("sudah_isi", v).apply()
    }

    fun sudahRendah(c: Context): Boolean = prefs(c).getBoolean("sudah_rendah", false)

    fun setSudahRendah(c: Context, v: Boolean) {
        prefs(c).edit().putBoolean("sudah_rendah", v).apply()
    }

    // Statistik arus (0 = belum ada data)
    fun maksArus(c: Context): Int = prefs(c).getInt("maks_ma", 0)

    fun minArus(c: Context): Int = prefs(c).getInt("min_ma", 0)

    fun catatArus(c: Context, ma: Int) {
        if (ma <= 0) return
        val p = prefs(c)
        val maks = p.getInt("maks_ma", 0)
        val min = p.getInt("min_ma", 0)
        if (ma > maks || min == 0 || ma < min) {
            val e = p.edit()
            if (ma > maks) e.putInt("maks_ma", ma)
            if (min == 0 || ma < min) e.putInt("min_ma", ma)
            e.apply()
        }
    }

    fun resetStatistik(c: Context) {
        prefs(c).edit().remove("maks_ma").remove("min_ma").apply()
    }
}
