package com.yunze.reels

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class Day(val label: String, val swipes: Int, val replays: Int, val millis: Long) {
    val total: Int get() = swipes + replays
}

object Store {
    private fun prefs(c: Context) = c.getSharedPreferences("yunze", Context.MODE_PRIVATE)

    private fun cal(offset: Int): Calendar {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, -offset)
        return c
    }

    private fun key(offset: Int): String =
        SimpleDateFormat("yyyyMMdd", Locale.US).format(cal(offset).time)

    fun day(c: Context, offset: Int): Day {
        val k = key(offset)
        val sp = prefs(c)
        val label = SimpleDateFormat("EEE", Locale.ENGLISH).format(cal(offset).time)
        return Day(label, sp.getInt("s_$k", 0), sp.getInt("r_$k", 0), sp.getLong("t_$k", 0L))
    }

    fun hours(c: Context, offset: Int): List<Int> {
        val k = key(offset)
        val sp = prefs(c)
        return (0..23).map { sp.getInt("h_${k}_$it", 0) }
    }

    private fun bump(c: Context, prefix: String) {
        val k = key(0)
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val sp = prefs(c)
        sp.edit()
            .putInt("${prefix}_$k", sp.getInt("${prefix}_$k", 0) + 1)
            .putInt("h_${k}_$hour", sp.getInt("h_${k}_$hour", 0) + 1)
            .apply()
    }

    fun addSwipe(c: Context) = bump(c, "s")

    fun addReplay(c: Context) = bump(c, "r")

    fun addTime(c: Context, ms: Long) {
        val k = key(0)
        val sp = prefs(c)
        sp.edit().putLong("t_$k", sp.getLong("t_$k", 0L) + ms).apply()
    }

    fun overlay(c: Context): Boolean = prefs(c).getBoolean("overlay", true)

    fun setOverlay(c: Context, on: Boolean) {
        prefs(c).edit().putBoolean("overlay", on).apply()
    }

    fun fmt(ms: Long): String {
        val m = ms / 60000
        return when {
            m >= 60 -> "${m / 60}h ${(m % 60).toString().padStart(2, '0')}m"
            m > 0 -> "${m}m"
            else -> "${ms / 1000}s"
        }
    }
}
