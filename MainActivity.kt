package com.yunze.reels

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val cBg = Color(0xFF0B0B12)
private val cCard = Color(0xFF16161F)
private val cViolet = Color(0xFF8B5CF6)
private val cPink = Color(0xFFF43F5E)
private val cDim = Color(0xFF8E8EA8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

fun serviceEnabled(c: Context): Boolean {
    val s = Settings.Secure.getString(c.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        ?: return false
    return s.contains("${c.packageName}/${ReelsService::class.java.name}") ||
        s.contains("${c.packageName}/.ReelsService")
}

@Composable
fun App() {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            tick++
        }
    }
    var week by remember { mutableStateOf(false) }
    var sel by remember { mutableIntStateOf(-1) }
    var overlayOn by remember { mutableStateOf(Store.overlay(ctx)) }
    var debug by remember { mutableStateOf(false) }

    val enabled = remember(tick) { serviceEnabled(ctx) }
    val today = remember(tick) { Store.day(ctx, 0) }
    val yesterday = remember(tick) { Store.day(ctx, 1) }
    val days = remember(tick) { (6 downTo 0).map { Store.day(ctx, it) } }
    val hours = remember(tick) { Store.hours(ctx, 0) }

    val shown = if (week) {
        Day("Week", days.sumOf { it.swipes }, days.sumOf { it.replays }, days.sumOf { it.millis })
    } else today

    val values = if (week) days.map { it.total } else hours
    val labels = if (week) days.map { it.label } else (0..23).map { if (it % 6 == 0) "$it" else "" }

    MaterialTheme(colorScheme = darkColorScheme(primary = cViolet)) {
        Column(
            Modifier
                .fillMaxSize()
                .background(cBg)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column {
                Text("Yunze", color = cPink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Reels counter", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            }

            if (!enabled) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(cCard)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Turn on counting", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Open Accessibility settings, choose Installed apps, then Yunze Reels Counter, and switch it on.",
                        color = cDim,
                        fontSize = 14.sp
                    )
                    Text(
                        "If the switch is greyed out: Settings, Apps, Yunze Reels Counter, three dots, Allow restricted settings.",
                        color = cDim,
                        fontSize = 13.sp
                    )
                    Button(
                        onClick = {
                            ctx.startActivity(
                                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = cPink, contentColor = Color.White)
                    ) { Text("Open Accessibility settings", fontWeight = FontWeight.SemiBold) }
                }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(30.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF3B1D8F), Color(0xFF9B1F5F))))
                    .padding(22.dp)
            ) {
                Segment(week) {
                    week = it
                    sel = -1
                }
                Spacer(Modifier.height(18.dp))
                Text(shown.total.toString(), color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    if (week) "reels in the last 7 days" else "reels today",
                    color = Color(0xCCFFFFFF),
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(10.dp))
                val note = if (week) {
                    "Average ${shown.total / 7} per day"
                } else {
                    val diff = today.total - yesterday.total
                    when {
                        diff > 0 -> "$diff more than yesterday"
                        diff < 0 -> "${-diff} fewer than yesterday"
                        else -> "Same as yesterday"
                    }
                }
                Text(note, color = Color(0xFFFFD6DF), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stat("Swipes", shown.swipes.toString())
                Stat("Replays", shown.replays.toString())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stat("Time in reels", Store.fmt(shown.millis))
                Stat(
                    "Per reel",
                    if (shown.total > 0) "${shown.millis / shown.total / 1000}s" else "0s"
                )
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(cCard)
                    .padding(18.dp)
            ) {
                val head = when {
                    sel in values.indices && week -> "${days[sel].label}: ${values[sel]} reels"
                    sel in values.indices -> "$sel:00 to $sel:59: ${values[sel]} reels"
                    week -> "Reels per day"
                    else -> "Reels per hour"
                }
                Text(head, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Tap a bar for details", color = cDim, fontSize = 12.sp)
                Spacer(Modifier.height(14.dp))
                Bars(values, labels, sel) { sel = if (sel == it) -1 else it }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(cCard)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Floating counter", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Shows your total on top while you watch reels", color = cDim, fontSize = 12.sp)
                }
                Switch(
                    checked = overlayOn,
                    onCheckedChange = {
                        overlayOn = it
                        Store.setOverlay(ctx, it)
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = cPink)
                )
            }

            Text(
                if (debug) "Hide debug tools" else "Debug tools",
                color = cDim,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { debug = !debug }
                    .padding(8.dp)
            )

            if (debug) {
                val dump = remember(tick) { ReelsService.lastDump }
                val progress = remember(tick) { ReelsService.lastProgress }
                val live = remember(tick) { ReelsService.reelsActive }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(cCard)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Reels detected: $live   Progress bar: ${if (progress < 0f) "not found" else "%.2f".format(progress)}",
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Button(
                        onClick = {
                            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("dump", dump))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = cViolet, contentColor = Color.White)
                    ) { Text("Copy screen dump") }
                    Text(dump.take(2500), color = cDim, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun Segment(week: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0x33000000))
            .padding(3.dp)
    ) {
        listOf("Today" to false, "Week" to true).forEach { (title, isWeek) ->
            val on = week == isWeek
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (on) Color.White else Color.Transparent)
                    .clickable { onChange(isWeek) }
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(
                    title,
                    color = if (on) Color(0xFF1A1030) else Color(0xCCFFFFFF),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun RowScope.Stat(title: String, value: String) {
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(22.dp))
            .background(cCard)
            .padding(16.dp)
    ) {
        Text(title, color = cDim, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Text(value, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Bars(values: List<Int>, labels: List<String>, sel: Int, onSel: (Int) -> Unit) {
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    val paint = remember {
        Paint().apply {
            color = 0xFF8E8EA8.toInt()
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
    }
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(180.dp)
            .pointerInput(values.size) {
                detectTapGestures { o ->
                    onSel((o.x / size.width * values.size).toInt().coerceIn(0, values.size - 1))
                }
            }
    ) {
        val n = values.size
        val slot = size.width / n
        val bw = slot * 0.62f
        val chartH = size.height - 24.dp.toPx()
        paint.textSize = 11.sp.toPx()
        drawLine(Color(0xFF26263A), Offset(0f, chartH), Offset(size.width, chartH), 2.dp.toPx())
        drawLine(Color(0xFF1E1E2D), Offset(0f, chartH / 2f), Offset(size.width, chartH / 2f), 1.dp.toPx())
        for (i in 0 until n) {
            val v = values[i]
            val h = (v.toFloat() / max * (chartH - 6.dp.toPx())).coerceAtLeast(if (v > 0) 5.dp.toPx() else 3.dp.toPx())
            val x = i * slot + (slot - bw) / 2f
            val brush: Brush = if (v > 0) {
                Brush.verticalGradient(listOf(cPink, cViolet), startY = chartH - h, endY = chartH)
            } else SolidColor(Color(0xFF26263A))
            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, chartH - h),
                size = Size(bw, h),
                cornerRadius = CornerRadius(bw / 2.5f),
                alpha = if (sel == -1 || sel == i) 1f else 0.3f
            )
            if (labels[i].isNotEmpty()) {
                drawContext.canvas.nativeCanvas.drawText(labels[i], i * slot + slot / 2f, size.height - 4.dp.toPx(), paint)
            }
        }
    }
}
