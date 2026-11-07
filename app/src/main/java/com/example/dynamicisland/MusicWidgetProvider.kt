package com.example.dynamicisland

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class MusicWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            // Intent untuk membuka MainActivity saat widget diketuk
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Hubungkan layout widget dengan PendingIntent
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
