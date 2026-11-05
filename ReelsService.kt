package com.yunze.reels

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import java.util.Calendar

private const val IG = "com.instagram.android"

private val ovColors = intArrayOf(
    Color.parseColor("#8B5CF6"),
    Color.parseColor("#F43F5E"),
    Color.parseColor("#3B82F6"),
    Color.parseColor("#10B981"),
    Color.parseColor("#F59E0B")
)

private class Scan {
    var viewer = false
    var tab = false
    var progress = -1f
    var count = 0
    val sig = StringBuilder()
    var dump: StringBuilder? = null
}

class ReelsService : AccessibilityService() {

    companion object {
        var lastDump = "Open Instagram Reels once, then come back here."
        var lastProgress = -1f
        var reelsActive = false
    }

    private val handler = Handler(Looper.getMainLooper())
    private var overlay: Overlay? = null
    private var active = false
    private var prevSig = ""
    private var prevProgress = -1f
    private var lastScrollAt = 0L
    private var scrollEvents = 0
    private var lastTick = 0L
    private var lastDumpAt = 0L
    private var lastSwipeAt = 0L
    private var lastWidgetAt = 0L
    private var lastMiscAt = 0L
    private var lastActiveAt = 0L
    private var msBuf = 0L
    private var sessionOpen = false
    private var sessionStart = 0L
    private var sessionReels = 0
    private var sessionMs = 0L
    private var nextBreakMs = 0L

    private val loop = object : Runnable {
        override fun run() {
            try {
                tick()
            } catch (e: Exception) {
            }
            handler.postDelayed(this, if (active) 300L else 1000L)
        }
    }

    override fun onServiceConnected() {
        overlay = Overlay(this)
        handler.removeCallbacksAndMessages(null)
        handler.post(loop)
    }

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        if (e == null || e.packageName?.toString() != IG) return
        if (e.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            lastScrollAt = SystemClock.elapsedRealtime()
            scrollEvents++
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        closeSession()
        overlay?.hide()
        reelsActive = false
        super.onDestroy()
    }

    private fun flushTime() {
        if (msBuf > 0L) {
            Store.addTime(this, msBuf)
            msBuf = 0L
        }
    }

    private fun openSession() {
        sessionOpen = true
        sessionStart = System.currentTimeMillis()
        sessionReels = 0
        sessionMs = 0L
        nextBreakMs = 0L
    }

    private fun closeSession() {
        if (!sessionOpen) return
        flushTime()
        if (sessionReels > 0 || sessionMs > 5000L) {
            Store.addSession(this, Session(sessionStart, System.currentTimeMillis(), sessionReels, sessionMs))
        }
        sessionOpen = false
    }

    private fun setActive(on: Boolean) {
        active = on
        reelsActive = on
        if (on) {
            if (!sessionOpen) openSession()
        } else {
            flushTime()
            overlay?.hide()
            prevSig = ""
            prevProgress = -1f
            scrollEvents = 0
            lastTick = 0L
            ReelsWidget.refresh(this)
        }
    }

    private fun periodic() {
        val cal = Calendar.getInstance()
        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY && cal.get(Calendar.HOUR_OF_DAY) >= 20) {
            val k = "${cal.get(Calendar.YEAR)}_${cal.get(Calendar.WEEK_OF_YEAR)}"
            if (Store.str(this, "lastWeekly", "") != k) {
                Store.setStr(this, "lastWeekly", k)
                Notifier.weekly(this)
            }
        }
    }

    private fun tick() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastMiscAt > 30000L) {
            lastMiscAt = now
            periodic()
        }
        if (sessionOpen && !active && now - lastActiveAt > 60000L) closeSession()

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isInteractive || !Store.counting(this)) {
            if (active) setActive(false)
            return
        }
        val root = rootInActiveWindow
        if (root == null || root.packageName?.toString() != IG) {
            if (active) setActive(false)
            return
        }

        val s = Scan()
        val dumpNow = now - lastDumpAt > 2000L
        if (dumpNow) {
            s.dump = StringBuilder()
            lastDumpAt = now
        }
        walk(root, s, 0)
        val on = s.viewer || s.tab
        if (dumpNow) {
            lastDump = "viewer=${s.viewer} tab=${s.tab} progress=${s.progress}\nsignature=${s.sig}\n\n" +
                (s.dump?.toString() ?: "").take(9000)
        }
        lastProgress = s.progress

        if (on != active) setActive(on)
        if (!on) return

        lastActiveAt = now
        val dt = if (lastTick == 0L) 0L else (now - lastTick).coerceAtMost(1000L)
        lastTick = now
        msBuf += dt
        sessionMs += dt
        if (msBuf >= 3000L) flushTime()

        val sig = s.sig.toString()
        val settled = now - lastScrollAt > 450L
        var swiped = false
        if (sig.isNotEmpty()) {
            if (prevSig.isNotEmpty() && sig != prevSig && now - lastScrollAt < 3000L) swiped = true
            prevSig = sig
            if (settled) scrollEvents = 0
        } else if (scrollEvents >= 3 && settled) {
            swiped = true
            scrollEvents = 0
        }
        if (swiped && now - lastSwipeAt < 600L) swiped = false

        var changed = false
        if (swiped) {
            Store.addSwipe(this)
            sessionReels++
            lastSwipeAt = now
            prevProgress = -1f
            changed = true
        } else if (s.progress >= 0f) {
            if (prevProgress > 0.85f && s.progress < 0.25f && now - lastSwipeAt > 1200L) {
                Store.addReplay(this)
                sessionReels++
                changed = true
            }
            prevProgress = s.progress
        }

        if (changed) {
            flushTime()
            if (now - lastWidgetAt > 1500L) {
                ReelsWidget.refresh(this)
                lastWidgetAt = now
            }
        }

        val bm = Store.int(this, "breakMin", 15)
        if (bm > 0) {
            if (nextBreakMs <= 0L) nextBreakMs = bm * 60000L
            if (sessionMs >= nextBreakMs) {
                Notifier.breakTime(this, sessionMs)
                nextBreakMs += bm * 60000L
            }
        }

        val lv = Store.level(this)
        if (lv > Store.warned(this)) {
            Store.setWarned(this, lv)
            Notifier.limit(this, lv)
        }

        if (Store.overlay(this)) overlay?.show(Store.day(this, 0), lv) else overlay?.hide()
    }

    private fun walk(n: AccessibilityNodeInfo?, s: Scan, d: Int) {
        if (n == null || d > 45 || s.count > 900) return
        s.count++
        val id = (n.viewIdResourceName ?: "").substringAfter(":id/")
        if (id.contains("clips_viewer")) s.viewer = true
        if (id.contains("clips_tab") && n.isSelected) s.tab = true
        if (id.contains("clips") &&
            (id.contains("author") || id.contains("username") || id.contains("caption") || id.contains("description"))
        ) {
            val t = n.text ?: n.contentDescription
            if (t != null) s.sig.append(t.toString().take(60)).append('|')
        }
        val ri = n.rangeInfo
        if (ri != null && s.progress < 0f && ri.max > ri.min) {
            s.progress = ((ri.current - ri.min) / (ri.max - ri.min)).coerceIn(0f, 1f)
        }
        val dump = s.dump
        if (dump != null) {
            dump.append(" ".repeat(d.coerceAtMost(12)))
                .append(if (id.isEmpty()) "-" else id)
                .append(' ')
                .append(n.className?.toString()?.substringAfterLast('.') ?: "")
                .append(" [")
                .append(n.text?.toString()?.take(30) ?: "")
                .append("] {")
                .append(n.contentDescription?.toString()?.take(30) ?: "")
                .append("}\n")
        }
        for (i in 0 until n.childCount) walk(n.getChild(i), s, d + 1)
    }
}

class Overlay(private val ctx: Context) {
    private val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: TextView? = null
    private var bg: GradientDrawable? = null
    private var sig = ""
    private var lastText = ""
    private var lastLevel = -1
    private var strokePx = 2

    private fun paint(level: Int, accent: Int) {
        val b = bg ?: return
        if (level == 0) {
            b.setColor(Color.parseColor("#E6141420"))
            b.setStroke(strokePx, accent)
        } else {
            val c = if (level == 1) Color.parseColor("#F59E0B") else Color.parseColor("#EF4444")
            b.setColor(c)
            b.setStroke(strokePx, c)
        }
    }

    fun show(d: Day, level: Int) {
        val pos = Store.int(ctx, "ovPos", 1).coerceIn(0, 5)
        val size = Store.int(ctx, "ovSize", 1).coerceIn(0, 2)
        val col = Store.int(ctx, "ovColor", 0).coerceIn(0, ovColors.size - 1)
        val style = Store.int(ctx, "ovStyle", 1)
        val s = "$pos,$size,$col,$style"
        if (view != null && s != sig) hide()

        var v = view
        if (v == null) {
            val dp = ctx.resources.displayMetrics.density
            val scale = when (size) {
                0 -> 0.85f
                2 -> 1.35f
                else -> 1f
            }
            strokePx = maxOf(1, (2 * dp).toInt())
            v = TextView(ctx)
            v.setTextColor(Color.WHITE)
            v.textSize = 14f * scale
            v.typeface = Typeface.DEFAULT_BOLD
            v.setPadding(
                (14 * dp * scale).toInt(),
                (7 * dp * scale).toInt(),
                (14 * dp * scale).toInt(),
                (7 * dp * scale).toInt()
            )
            val b = GradientDrawable()
            b.cornerRadius = 100 * dp
            v.background = b
            bg = b
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
            val h = when (pos % 3) {
                0 -> Gravity.LEFT
                1 -> Gravity.CENTER_HORIZONTAL
                else -> Gravity.RIGHT
            }
            val vt = if (pos < 3) Gravity.TOP else Gravity.BOTTOM
            lp.gravity = h or vt
            lp.x = if (pos % 3 == 1) 0 else (12 * dp).toInt()
            lp.y = if (pos < 3) (56 * dp).toInt() else (96 * dp).toInt()
            try {
                wm.addView(v, lp)
            } catch (e: Exception) {
                bg = null
                return
            }
            view = v
            sig = s
            lastText = ""
            lastLevel = -1
        }

        val text = when (style) {
            0 -> "${d.total}"
            2 -> "${d.total} \u00b7 ${Store.fmt(d.millis)}"
            3 -> {
                val l = Store.limitReels(ctx)
                if (l > 0) "${d.total} / $l" else "${d.total} reels"
            }
            else -> "${d.total} reels"
        }
        if (text != lastText) {
            v.text = text
            lastText = text
        }
        if (level != lastLevel) {
            paint(level, ovColors[col])
            lastLevel = level
        }
    }

    fun hide() {
        val v = view
        if (v != null) {
            try {
                wm.removeView(v)
            } catch (e: Exception) {
            }
        }
        view = null
        bg = null
        sig = ""
        lastText = ""
        lastLevel = -1
    }
}
