package com.yunze.reels

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class ReelsWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { render(context, manager, it) }
    }

    companion object {
        fun refresh(c: Context) {
            val m = AppWidgetManager.getInstance(c)
            val ids = m.getAppWidgetIds(ComponentName(c, ReelsWidget::class.java))
            ids.forEach { render(c, m, it) }
        }

        private fun render(c: Context, m: AppWidgetManager, id: Int) {
            val d = Store.day(c, 0)
            val rv = RemoteViews(c.packageName, R.layout.widget)
            rv.setTextViewText(R.id.w_count, d.total.toString())
            rv.setTextViewText(R.id.w_sub, "${d.swipes} swipes, ${Store.fmt(d.millis)}")
            val pi = PendingIntent.getActivity(
                c,
                0,
                Intent(c, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            rv.setOnClickPendingIntent(R.id.w_root, pi)
            m.updateAppWidget(id, rv)
        }
    }
}
