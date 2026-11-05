package com.yunze.reels

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.widget.RemoteViews

class ReelsWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { renderSmall(context, manager, it) }
    }

    companion object {
        fun refresh(c: Context) {
            val m = AppWidgetManager.getInstance(c)
            m.getAppWidgetIds(ComponentName(c, ReelsWidget::class.java)).forEach { renderSmall(c, m, it) }
            m.getAppWidgetIds(ComponentName(c, ReelsWidgetWeek::class.java)).forEach { renderWeek(c, m, it) }
        }

        private fun open(c: Context): PendingIntent = PendingIntent.getActivity(
            c,
            0,
            Intent(c, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        fun renderSmall(c: Context, m: AppWidgetManager, id: Int) {
            val d = Store.day(c, 0)
            val limit = Store.limitReels(c)
            val rv = RemoteViews(c.packageName, R.layout.widget)
            rv.setTextViewText(R.id.w_count, d.total.toString())
            rv.setTextViewText(
                R.id.w_sub,
                if (limit > 0) "of $limit, ${Store.fmt(d.millis)}" else "${d.swipes} swipes, ${Store.fmt(d.millis)}"
            )
            rv.setOnClickPendingIntent(R.id.w_root, open(c))
            m.updateAppWidget(id, rv)
        }

        fun renderWeek(c: Context, m: AppWidgetManager, id: Int) {
            val days = (6 downTo 0).map { Store.day(c, it) }
            val rv = RemoteViews(c.packageName, R.layout.widget_week)
            rv.setTextViewText(R.id.ww_total, days.sumOf { it.total }.toString())
            rv.setImageViewBitmap(R.id.ww_chart, chart(days))
            rv.setOnClickPendingIntent(R.id.ww_root, open(c))
            m.updateAppWidget(id, rv)
        }

        private fun chart(days: List<Day>): Bitmap {
            val w = 700
            val h = 260
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val cv = Canvas(bmp)
            val max = maxOf(1, days.maxOf { it.total })
            val slot = w / days.size.toFloat()
            val bw = slot * 0.56f
            val ch = h - 40f
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            val lp = Paint(Paint.ANTI_ALIAS_FLAG)
            lp.color = Color.parseColor("#8E8EA8")
            lp.textSize = 26f
            lp.textAlign = Paint.Align.CENTER
            days.forEachIndexed { i, d ->
                val bh = maxOf(10f, d.total.toFloat() / max * (ch - 10f))
                val x = i * slot + (slot - bw) / 2f
                if (d.total > 0) {
                    p.shader = LinearGradient(
                        0f, ch - bh, 0f, ch,
                        Color.parseColor("#F43F5E"), Color.parseColor("#8B5CF6"),
                        Shader.TileMode.CLAMP
                    )
                } else {
                    p.shader = null
                    p.color = Color.parseColor("#2A2A3C")
                }
                cv.drawRoundRect(RectF(x, ch - bh, x + bw, ch), 14f, 14f, p)
                cv.drawText(d.label, i * slot + slot / 2f, h - 8f, lp)
            }
            return bmp
        }
    }
}

class ReelsWidgetWeek : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { ReelsWidget.renderWeek(context, manager, it) }
    }
}
