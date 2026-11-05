package com.yunze.reels

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

object Notifier {
    private const val CH = "yunze_alerts"

    private fun send(c: Context, id: Int, title: String, text: String) {
        if (!Store.bool(c, "notif", true)) return
        try {
            val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(CH, "Reels alerts", NotificationManager.IMPORTANCE_DEFAULT)
            )
            val pi = PendingIntent.getActivity(
                c,
                0,
                Intent(c, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val n = NotificationCompat.Builder(c, CH)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build()
            nm.notify(id, n)
        } catch (e: Exception) {
        }
    }

    fun limit(c: Context, level: Int) {
        if (level == 1) {
            send(c, 11, "Almost at your limit", "You have used 80% of your daily reels limit.")
        } else if (level >= 2) {
            send(c, 12, "Daily limit reached", "You hit your limit for today. Time for a break?")
        }
    }

    fun breakTime(c: Context, ms: Long) {
        send(c, 13, "Time for a break", "You have been watching reels for ${Store.fmt(ms)} without stopping.")
    }

    fun weekly(c: Context) {
        var thisWeek = 0
        var lastWeek = 0
        for (i in 0..6) thisWeek += Store.day(c, i).total
        for (i in 7..13) lastWeek += Store.day(c, i).total
        val text = if (lastWeek > 0) {
            val pct = (thisWeek - lastWeek) * 100 / lastWeek
            when {
                pct < 0 -> "You watched $thisWeek reels this week, ${-pct}% less than last week."
                pct > 0 -> "You watched $thisWeek reels this week, $pct% more than last week."
                else -> "You watched $thisWeek reels this week, same as last week."
            }
        } else {
            "You watched $thisWeek reels this week."
        }
        send(c, 14, "Your weekly summary", text)
    }
}
