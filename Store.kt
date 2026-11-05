package com.yunze.reels

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class Day(val label: String, val swipes: Int, val replays: Int, val millis: Long) {
    val total: Int get() = swipes + replays
}

data class Rec(val key: String, val swipes: Int, val replays: Int, val millis: Long) {
    val total: Int get() = swipes + replays
}

data class Session(val start: Long, val end: Long, val reels: Int, val millis: Long)

data class Badge(val title: String, val desc: String, val done: Boolean)

object Store {
    private val keyFmt = SimpleDateFormat("yyyyMMdd", Locale.US)
    private val labelFmt = SimpleDateFormat("EEE", Locale.ENGLISH)
    private val niceFmt = SimpleDateFormat("MMM d", Locale.ENGLISH)

    private fun prefs(c: Context) = c.getSharedPreferences("yunze", Context.MODE_PRIVATE)

    fun int(c: Context, k: String, d: Int): Int = prefs(c).getInt(k, d)

    fun setInt(c: Context, k: String, v: Int) {
        prefs(c).edit().putInt(k, v).apply()
    }

    fun bool(c: Context, k: String, d: Boolean): Boolean = prefs(c).getBoolean(k, d)

    fun setBool(c: Context, k: String, v: Boolean) {
        prefs(c).edit().putBoolean(k, v).apply()
    }

    fun str(c: Context, k: String, d: String): String = prefs(c).getString(k, d) ?: d

    fun setStr(c: Context, k: String, v: String) {
        prefs(c).edit().putString(k, v).apply()
    }

    private fun cal(offset: Int): Calendar {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, -offset)
        return c
    }

    fun key(offset: Int): String = keyFmt.format(cal(offset).time)

    fun nice(key: String): String {
        val d = keyFmt.parse(key) ?: return key
        return niceFmt.format(d)
    }

    fun day(c: Context, offset: Int): Day {
        val k = key(offset)
        val sp = prefs(c)
        val label = labelFmt.format(cal(offset).time)
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

    fun overlay(c: Context): Boolean = bool(c, "overlay", true)

    fun limitReels(c: Context): Int = int(c, "limit", 100)

    fun limitMin(c: Context): Int = int(c, "limitMin", 0)

    fun paused(c: Context): Boolean = bool(c, "paused", false)

    fun quiet(c: Context): Boolean {
        if (!bool(c, "quiet", false)) return false
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val s = int(c, "qs", 23)
        val e = int(c, "qe", 7)
        return if (s < e) h in s until e else h >= s || h < e
    }

    fun counting(c: Context): Boolean = !paused(c) && !quiet(c)

    fun progress(c: Context): Float {
        val d = day(c, 0)
        val lr = limitReels(c)
        val lm = limitMin(c)
        var p = -1f
        if (lr > 0) p = maxOf(p, d.total.toFloat() / lr)
        if (lm > 0) p = maxOf(p, d.millis.toFloat() / (lm * 60000f))
        return p
    }

    fun level(c: Context): Int {
        val p = progress(c)
        return when {
            p < 0f -> 0
            p >= 1f -> 2
            p >= 0.8f -> 1
            else -> 0
        }
    }

    fun warned(c: Context): Int = int(c, "w_" + key(0), 0)

    fun setWarned(c: Context, v: Int) = setInt(c, "w_" + key(0), v)

    fun sessions(c: Context): List<Session> {
        val raw = str(c, "sessions", "")
        if (raw.isEmpty()) return emptyList()
        return raw.split("\n").mapNotNull { l ->
            try {
                val p = l.split(",")
                Session(p[0].toLong(), p[1].toLong(), p[2].toInt(), p[3].toLong())
            } catch (e: Exception) {
                null
            }
        }.reversed()
    }

    fun addSession(c: Context, s: Session) {
        val lines = str(c, "sessions", "").split("\n").filter { it.isNotEmpty() }.takeLast(199)
        val next = lines + "${s.start},${s.end},${s.reels},${s.millis}"
        setStr(c, "sessions", next.joinToString("\n"))
    }

    fun recs(c: Context): Map<String, Rec> {
        val sp = prefs(c)
        val keys = sp.all.keys
            .filter { it.length == 10 && (it.startsWith("s_") || it.startsWith("r_") || it.startsWith("t_")) }
            .map { it.substring(2) }
            .toSet()
        return keys.associateWith { k ->
            Rec(k, sp.getInt("s_$k", 0), sp.getInt("r_$k", 0), sp.getLong("t_$k", 0L))
        }
    }

    fun busiestHour(c: Context): Pair<Int, Int>? {
        val sums = IntArray(24)
        for ((k, v) in prefs(c).all) {
            if (k.length == 13 && k.startsWith("h_") && v is Int) {
                val h = k.substring(11).toIntOrNull() ?: continue
                if (h in 0..23) sums[h] += v
            }
        }
        var best = 0
        for (i in 1..23) if (sums[i] > sums[best]) best = i
        return if (sums[best] > 0) Pair(best, sums[best]) else null
    }

    fun streaks(c: Context): Pair<Int, Int> {
        val limit = limitReels(c)
        val m = recs(c)
        if (limit <= 0 || m.isEmpty()) return Pair(0, 0)
        val first = m.keys.minOrNull() ?: return Pair(0, 0)
        val t0 = keyFmt.parse(first)?.time ?: return Pair(0, 0)
        val n = ((System.currentTimeMillis() - t0) / 86400000L).toInt().coerceIn(0, 400)
        var run = 0
        var best = 0
        for (off in n downTo 0) {
            val k = key(off)
            if (k < first) continue
            if ((m[k]?.total ?: 0) <= limit) run++ else run = 0
            if (run > best) best = run
        }
        return Pair(run, best)
    }

    fun badges(c: Context): List<Badge> {
        val m = recs(c)
        val all = m.values.sumOf { it.total }
        val record = m.values.maxOfOrNull { it.total } ?: 0
        val longest = sessions(c).maxOfOrNull { it.millis } ?: 0L
        val best = streaks(c).second
        val today = key(0)
        val detox = m.values.any { it.key != today && it.total in 1..9 }
        return listOf(
            Badge("First scroll", "Count your first reel", all >= 1),
            Badge("Century", "100 reels in one day", record >= 100),
            Badge("Thousand club", "1,000 reels all time", all >= 1000),
            Badge("Long haul", "A 30 minute session", longest >= 30 * 60000L),
            Badge("Under control", "3 days under your limit", best >= 3),
            Badge("Iron will", "7 days under your limit", best >= 7),
            Badge("Month master", "30 days under your limit", best >= 30),
            Badge("Light day", "A day with under 10 reels", detox)
        )
    }

    fun csv(c: Context): String {
        val sb = StringBuilder("date,swipes,replays,seconds\n")
        recs(c).toSortedMap().values.forEach { r ->
            sb.append(r.key.substring(0, 4)).append('-')
                .append(r.key.substring(4, 6)).append('-')
                .append(r.key.substring(6)).append(',')
                .append(r.swipes).append(',')
                .append(r.replays).append(',')
                .append(r.millis / 1000).append('\n')
        }
        return sb.toString()
    }

    fun importCsv(c: Context, text: String): Int {
        var n = 0
        val e = prefs(c).edit()
        text.lines().forEach { l ->
            val p = l.trim().split(",")
            if (p.size >= 4 && p[0].length == 10 && p[0][4] == '-') {
                try {
                    val k = p[0].replace("-", "")
                    e.putInt("s_$k", p[1].trim().toInt())
                    e.putInt("r_$k", p[2].trim().toInt())
                    e.putLong("t_$k", p[3].trim().toLong() * 1000L)
                    n++
                } catch (ex: Exception) {
                }
            }
        }
        e.apply()
        return n
    }

    fun eraseData(c: Context) {
        val sp = prefs(c)
        val e = sp.edit()
        sp.all.keys
            .filter {
                it.startsWith("s_") || it.startsWith("r_") || it.startsWith("t_") ||
                    it.startsWith("h_") || it.startsWith("w_") || it == "sessions"
            }
            .forEach { e.remove(it) }
        e.apply()
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
