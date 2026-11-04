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

private const val IG = "com.instagram.android"

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
    private var msBuf = 0L

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

    private fun setActive(on: Boolean) {
        active = on
        reelsActive = on
        if (!on) {
            flushTime()
            overlay?.hide()
            prevSig = ""
            prevProgress = -1f
            scrollEvents = 0
            lastTick = 0L
            ReelsWidget.refresh(this)
        }
    }

    private fun tick() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isInteractive) {
            if (active) setActive(false)
            return
        }
        val now = SystemClock.elapsedRealtime()
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

        val dt = if (lastTick == 0L) 0L else (now - lastTick).coerceAtMost(1000L)
        lastTick = now
        msBuf += dt
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
            lastSwipeAt = now
            prevProgress = -1f
            changed = true
        } else if (s.progress >= 0f) {
            if (prevProgress > 0.85f && s.progress < 0.25f && now - lastSwipeAt > 1200L) {
                Store.addReplay(this)
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

        if (Store.overlay(this)) overlay?.show(Store.day(this, 0).total) else overlay?.hide()
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
    private var last = -1

    fun show(n: Int) {
        var v = view
        if (v == null) {
            val dp = ctx.resources.displayMetrics.density
            v = TextView(ctx)
            v.setTextColor(Color.WHITE)
            v.textSize = 14f
            v.typeface = Typeface.DEFAULT_BOLD
            v.setPadding((14 * dp).toInt(), (7 * dp).toInt(), (14 * dp).toInt(), (7 * dp).toInt())
            val bg = GradientDrawable()
            bg.cornerRadius = 100 * dp
            bg.setColor(Color.parseColor("#D9141420"))
            bg.setStroke(maxOf(1, dp.toInt()), Color.parseColor("#88F43F5E"))
            v.background = bg
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
            lp.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            lp.y = (56 * dp).toInt()
            try {
                wm.addView(v, lp)
            } catch (e: Exception) {
                return
            }
            view = v
            last = -1
        }
        if (n != last) {
            v.text = "$n reels"
            last = n
        }
    }

    fun hide() {
        val v = view ?: return
        try {
            wm.removeView(v)
        } catch (e: Exception) {
        }
        view = null
        last = -1
    }
}
