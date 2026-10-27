package com.contoh.chargermeter

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlin.math.abs

/** Widget layar utama: level, arus, status, suhu, dan voltase. */
class WidgetBaterai : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        perbaruiKe(context, manager, appWidgetIds)
    }

    companion object {
        fun perbaruiSemua(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, WidgetBaterai::class.java))
            if (ids.isNotEmpty()) perbaruiKe(context, manager, ids)
        }

        private fun perbaruiKe(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val d = BatteriInfo.baca(context)
            val pi = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            for (id in ids) {
                val v = RemoteViews(context.packageName, R.layout.widget_baterai)
                v.setTextViewText(R.id.wPersen, "${d.persen}%")
                v.setTextViewText(R.id.wArus, "${abs(d.arusBertanda)} mA")
                v.setTextViewText(R.id.wStatus, d.statusTeks)
                v.setTextViewText(
                    R.id.wDetail,
                    "%.2f V · %.1f °C".format(d.voltase, d.suhuC)
                )
                v.setOnClickPendingIntent(R.id.wRoot, pi)
                manager.updateAppWidget(id, v)
            }
        }
    }
}
