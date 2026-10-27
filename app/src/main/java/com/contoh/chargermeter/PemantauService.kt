package com.contoh.chargermeter

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

/**
 * Layanan latar depan: memantau baterai dan mengirim pengingat
 * (pengisian sudah mencapai target, dan baterai rendah).
 */
class PemantauService : Service() {

    companion object {
        private const val ID_PERSISTEN = 1001
        private const val ID_PERINGATAN_ISI = 1002
        private const val ID_PERINGATAN_RENDAH = 1003
        private const val KANAL_PANTAU = "pemantauan"
        private const val KANAL_PERINGATAN = "pengingat"

        /** Menyalakan layanan bila ada pengingat aktif, atau mematikannya bila tidak ada. */
        fun sinkronkan(ctx: Context) {
            val intent = Intent(ctx, PemantauService::class.java)
            if (Pengaturan.pengingatAktif(ctx)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ctx.startForegroundService(intent)
                } else {
                    ctx.startService(intent)
                }
            } else {
                ctx.stopService(intent)
            }
        }
    }

    private var teksTerakhir = ""
    private var terdaftar = false

    private val penerima = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            proses()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        buatKanal()
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(penerima, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(penerima, filter)
        }
        terdaftar = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Wajib memanggil startForeground dulu, meski layanan akan dihentikan
        mulaiLatarDepan()
        if (!Pengaturan.pengingatAktif(this)) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        proses()
        return START_STICKY
    }

    override fun onDestroy() {
        if (terdaftar) {
            try {
                unregisterReceiver(penerima)
            } catch (e: IllegalArgumentException) {
                // sudah dilepas
            }
            terdaftar = false
        }
        super.onDestroy()
    }

    private fun mulaiLatarDepan() {
        val notif = bangunPersisten(BatteriInfo.baca(this))
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(ID_PERSISTEN, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(ID_PERSISTEN, notif)
        }
    }

    private fun proses() {
        val d = BatteriInfo.baca(this)
        Pengaturan.catatArus(this, d.arusMa)
        cekPengingat(d)

        val isi = "${d.persen}% · ${d.statusTeks}"
        if (isi != teksTerakhir) {
            teksTerakhir = isi
            getSystemService(NotificationManager::class.java).notify(ID_PERSISTEN, bangunPersisten(d))
        }
        WidgetBaterai.perbaruiSemua(this)
    }

    private fun cekPengingat(d: DataBaterai) {
        val p = Pengaturan
        val ctx = this

        // Reset status pengingat saat kondisi berubah
        if (!d.terhubung) {
            if (p.sudahIsi(ctx)) p.setSudahIsi(ctx, false)
            if (p.sudahRendah(ctx) && d.persen > p.ambangRendah(ctx) + 2) p.setSudahRendah(ctx, false)
        } else {
            if (p.sudahRendah(ctx)) p.setSudahRendah(ctx, false)
            if (p.sudahIsi(ctx) && d.persen < p.targetIsi(ctx) - 2) p.setSudahIsi(ctx, false)
        }

        if (p.pengingatIsi(ctx) && d.terhubung && d.persen >= p.targetIsi(ctx) && !p.sudahIsi(ctx)) {
            p.setSudahIsi(ctx, true)
            kirimPeringatan(
                ID_PERINGATAN_ISI,
                "Baterai sudah ${d.persen}%",
                "Silakan cabut charger agar baterai tidak terlalu panas."
            )
        }

        if (p.pengingatRendah(ctx) && !d.terhubung && d.persen <= p.ambangRendah(ctx) && !p.sudahRendah(ctx)) {
            p.setSudahRendah(ctx, true)
            kirimPeringatan(
                ID_PERINGATAN_RENDAH,
                "Baterai rendah: ${d.persen}%",
                "Segera hubungkan charger."
            )
        }
    }

    private fun bukaAplikasi(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun bangunPersisten(d: DataBaterai): Notification =
        Notification.Builder(this, KANAL_PANTAU)
            .setSmallIcon(R.drawable.ic_baterai)
            .setContentTitle("Charger Meter memantau baterai")
            .setContentText("${d.persen}% · ${d.statusTeks}")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(bukaAplikasi())
            .build()

    private fun kirimPeringatan(id: Int, judul: String, isi: String) {
        val notif = Notification.Builder(this, KANAL_PERINGATAN)
            .setSmallIcon(R.drawable.ic_baterai)
            .setContentTitle(judul)
            .setContentText(isi)
            .setAutoCancel(true)
            .setContentIntent(bukaAplikasi())
            .build()
        getSystemService(NotificationManager::class.java).notify(id, notif)
    }

    private fun buatKanal() {
        val nm = getSystemService(NotificationManager::class.java)
        val pantau = NotificationChannel(
            KANAL_PANTAU,
            "Pemantauan baterai",
            NotificationManager.IMPORTANCE_MIN
        )
        pantau.description = "Status baterai yang berjalan di latar belakang"
        nm.createNotificationChannel(pantau)

        val peringatan = NotificationChannel(
            KANAL_PERINGATAN,
            "Pengingat baterai",
            NotificationManager.IMPORTANCE_HIGH
        )
        peringatan.description = "Pengingat pengisian dan baterai rendah"
        peringatan.enableVibration(true)
        nm.createNotificationChannel(peringatan)
    }
}
