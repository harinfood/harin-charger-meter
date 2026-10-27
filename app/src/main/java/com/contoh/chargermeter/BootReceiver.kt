package com.contoh.chargermeter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Menyalakan kembali pemantauan setelah HP dinyalakan ulang. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            PemantauService.sinkronkan(context)
        }
    }
}
