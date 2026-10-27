package com.contoh.chargermeter

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

data class DataBaterai(
    val persen: Int,
    val status: Int,
    val statusTeks: String,
    val terhubung: Boolean,
    val sumberCharger: String,
    val arusMa: Int,          // besar arus dalam mA (selalu positif)
    val arusBertanda: Int,    // + saat mengisi, - saat memakai baterai, 0 jika tidak keduanya
    val dayaWatt: Float,
    val voltase: Float,       // volt
    val suhuC: Float,         // derajat Celsius
    val teknologi: String,
    val kesehatan: String,
    val kapasitasMah: Int,    // 0 jika HP tidak melaporkan kapasitas
    val menitPenuh: Int?,
    val jamPenuh: String?,
    val persenPerJam: Float?
)

/** Membaca data baterai dari sistem Android (API publik). */
object BatteriInfo {

    fun baca(context: Context): DataBaterai {
        val intent: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val skala = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val persen = if (level >= 0 && skala > 0) {
            Math.round(level * 100f / skala)
        } else {
            bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100)
        }

        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
            ?: BatteryManager.BATTERY_STATUS_UNKNOWN
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val terhubung = plugged != 0
        val voltMv = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val suhuC = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val teknologi = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "-"
        val kode = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
            ?: BatteryManager.BATTERY_HEALTH_UNKNOWN

        // Arus dalam mikroampere (µA) -> mA
        val arusRaw = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val arusMa = if (arusRaw == Int.MIN_VALUE) 0 else abs(arusRaw) / 1000

        val mengisi = status == BatteryManager.BATTERY_STATUS_CHARGING
        val memakai = status == BatteryManager.BATTERY_STATUS_DISCHARGING
        val arusBertanda = when {
            mengisi -> arusMa
            memakai -> -arusMa
            else -> 0
        }

        // Kapasitas penuh tidak tersedia sebagai API publik. Diperkirakan dari muatan
        // saat ini (CHARGE_COUNTER, µAh) dibagi persentase level, hanya jika level >= 10%
        // agar pembulatan persen tidak terlalu besar pengaruhnya.
        val muatanUah = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        val kapasitasMah = if (muatanUah > 0L && persen >= 10) {
            (muatanUah * 100L / persen / 1000L).toInt()
        } else {
            0
        }

        val voltase = voltMv / 1000f
        // mV x mA / 1.000.000 = watt
        val dayaWatt = voltMv.toFloat() * arusMa / 1_000_000f

        var menitPenuh: Int? = null
        var jamPenuh: String? = null
        var persenPerJam: Float? = null
        if (kapasitasMah > 0 && arusMa > 0) {
            persenPerJam = arusMa * 100f / kapasitasMah
            if (mengisi) {
                val sisaMah = kapasitasMah * (100 - persen) / 100f
                val menit = Math.round(sisaMah / arusMa * 60f)
                menitPenuh = menit
                val waktu = Calendar.getInstance()
                waktu.add(Calendar.MINUTE, menit)
                jamPenuh = SimpleDateFormat("HH.mm", Locale("id", "ID")).format(waktu.time)
            }
        }

        val sumber = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Nirkabel"
            BatteryManager.BATTERY_PLUGGED_DOCK -> "Dok"
            else -> "Tidak terhubung"
        }

        val statusTeks = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Mengisi daya"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Memakai baterai"
            BatteryManager.BATTERY_STATUS_FULL -> "Penuh"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Tidak mengisi"
            else -> "Tidak diketahui"
        }

        val kesehatan = when (kode) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Baik"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Panas"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Mati"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Tegangan tinggi"
            BatteryManager.BATTERY_HEALTH_COLD -> "Dingin"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Gagal"
            else -> "Tidak diketahui"
        }

        return DataBaterai(
            persen = persen,
            status = status,
            statusTeks = statusTeks,
            terhubung = terhubung,
            sumberCharger = sumber,
            arusMa = arusMa,
            arusBertanda = arusBertanda,
            dayaWatt = dayaWatt,
            voltase = voltase,
            suhuC = suhuC,
            teknologi = teknologi,
            kesehatan = kesehatan,
            kapasitasMah = kapasitasMah,
            menitPenuh = menitPenuh,
            jamPenuh = jamPenuh,
            persenPerJam = persenPerJam
        )
    }
}
