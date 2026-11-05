package com.yunze.reels

import android.content.Context
import android.graphics.Paint
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val cBg = Color(0xFF0B0B12)
val cCard = Color(0xFF15151F)
val cCard2 = Color(0xFF1E1E2C)
val cLine = Color(0xFF272738)
val cViolet = Color(0xFF8B5CF6)
val cPink = Color(0xFFF43F5E)
val cDim = Color(0xFF8E8EA8)
val cAmber = Color(0xFFF59E0B)
val cRed = Color(0xFFEF4444)
val cGreen = Color(0xFF10B981)
val accents = listOf(
    Color(0xFF8B5CF6),
    Color(0xFFF43F5E),
    Color(0xFF3B82F6),
    Color(0xFF10B981),
    Color(0xFFF59E0B)
)

@Composable
fun rememberTap(): () -> Unit {
    val h = LocalHapticFeedback.current
    return { h.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
}

@Composable
fun YCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(cCard)
            .border(1.dp, cLine, RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
fun Heading(title: String, sub: String = "") {
    Column {
        Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        if (sub.isNotEmpty()) Text(sub, color = cDim, fontSize = 12.sp)
    }
}

@Composable
fun Page(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content
    )
}

@Composable
fun Avatar(name: String) {
    val initial = if (name.isBlank()) "Y" else name.trim().take(1).uppercase()
    Box(
        Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(cViolet, cPink))),
        contentAlignment = Alignment.Center
    ) {
        Text(initial, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Ring(progress: Float, color: Color, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val anim by animateFloatAsState(progress.coerceIn(0f, 1f), tween(900), label = "ring")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val sw = 16.dp.toPx()
            val inset = sw / 2f
            val sz = Size(size.width - sw, size.height - sw)
            drawArc(
                color = cLine,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = sz,
                style = Stroke(sw, cap = StrokeCap.Round)
            )
            if (anim > 0.002f) {
                drawArc(
                    brush = Brush.verticalGradient(listOf(color, color.copy(alpha = 0.55f))),
                    startAngle = -90f,
                    sweepAngle = 360f * anim,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = sz,
                    style = Stroke(sw, cap = StrokeCap.Round)
                )
            }
        }
        content()
    }
}

@Composable
fun Chip(text: String, on: Boolean, onClick: () -> Unit) {
    val tap = rememberTap()
    val bg by animateColorAsState(if (on) cViolet else cCard2, label = "chip")
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable {
                tap()
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text, color = if (on) Color.White else cDim, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun Segment(options: List<String>, selected: Int, onChange: (Int) -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0x33000000))
            .padding(3.dp)
    ) {
        options.forEachIndexed { i, title ->
            val on = selected == i
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (on) Color.White else Color.Transparent)
                    .clickable { onChange(i) }
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
fun RowScope.Stat(title: String, value: String, sub: String = "") {
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(22.dp))
            .background(cCard)
            .border(1.dp, cLine, RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Text(title, color = cDim, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Text(value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        if (sub.isNotEmpty()) Text(sub, color = cDim, fontSize = 12.sp)
    }
}

@Composable
fun StepBtn(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(cCard2)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun IntSetting(
    ctx: Context,
    label: String,
    key: String,
    def: Int,
    min: Int,
    max: Int,
    step: Int,
    suffix: String,
    zero: String = "Off"
) {
    var v by remember { mutableIntStateOf(Store.int(ctx, key, def)) }
    val tap = rememberTap()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
        StepBtn("-") {
            if (v > min) {
                v = (v - step).coerceAtLeast(min)
                Store.setInt(ctx, key, v)
                tap()
            }
        }
        Text(
            if (v == 0 && zero.isNotEmpty()) zero else "$v$suffix",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(80.dp)
        )
        StepBtn("+") {
            if (v < max) {
                v = (v + step).coerceAtMost(max)
                Store.setInt(ctx, key, v)
                tap()
            }
        }
    }
}

@Composable
fun BoolSetting(ctx: Context, title: String, sub: String, key: String, def: Boolean) {
    var v by remember { mutableStateOf(Store.bool(ctx, key, def)) }
    val tap = rememberTap()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (sub.isNotEmpty()) Text(sub, color = cDim, fontSize = 12.sp)
        }
        Switch(
            checked = v,
            onCheckedChange = {
                v = it
                Store.setBool(ctx, key, it)
                tap()
            },
            colors = SwitchDefaults.colors(checkedTrackColor = cPink)
        )
    }
}

@Composable
fun ChoiceSetting(ctx: Context, label: String, key: String, def: Int, options: List<String>) {
    var v by remember { mutableIntStateOf(Store.int(ctx, key, def)) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = Color.White, fontSize = 15.sp)
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEachIndexed { i, o ->
                Chip(o, v == i) {
                    v = i
                    Store.setInt(ctx, key, i)
                }
            }
        }
    }
}

@Composable
fun ColorSetting(ctx: Context, label: String, key: String) {
    var v by remember { mutableIntStateOf(Store.int(ctx, key, 0)) }
    val tap = rememberTap()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = Color.White, fontSize = 15.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            accents.forEachIndexed { i, c ->
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(c)
                        .then(if (v == i) Modifier.border(3.dp, Color.White, CircleShape) else Modifier)
                        .clickable {
                            v = i
                            Store.setInt(ctx, key, i)
                            tap()
                        }
                )
            }
        }
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
            val h = (v.toFloat() / max * (chartH - 6.dp.toPx()))
                .coerceAtLeast(if (v > 0) 5.dp.toPx() else 3.dp.toPx())
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
                drawContext.canvas.nativeCanvas.drawText(
                    labels[i],
                    i * slot + slot / 2f,
                    size.height - 4.dp.toPx(),
                    paint
                )
            }
        }
    }
}

@Composable
fun Heatmap(values: List<Int>, sel: Int, onSel: (Int) -> Unit) {
    val cols = values.size / 7
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    Canvas(
        Modifier
            .fillMaxWidth()
            .aspectRatio(cols / 7f)
            .pointerInput(values.size) {
                detectTapGestures { o ->
                    val cw = size.width / cols.toFloat()
                    val col = (o.x / cw).toInt().coerceIn(0, cols - 1)
                    val row = (o.y / cw).toInt().coerceIn(0, 6)
                    onSel(col * 7 + row)
                }
            }
    ) {
        val cw = size.width / cols
        val gap = 3.dp.toPx()
        for (i in values.indices) {
            val col = i / 7
            val row = i % 7
            val v = values[i]
            val c = if (v == 0) cLine else cPink.copy(alpha = 0.25f + 0.75f * (v.toFloat() / max))
            drawRoundRect(
                color = c,
                topLeft = Offset(col * cw + gap / 2f, row * cw + gap / 2f),
                size = Size(cw - gap, cw - gap),
                cornerRadius = CornerRadius(4.dp.toPx()),
                alpha = if (sel == -1 || sel == i) 1f else 0.35f
            )
        }
    }
}

private fun DrawScope.navIcon(kind: Int, c: Color) {
    val s = size.width
    val st = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (kind) {
        0 -> {
            val p = Path()
            p.moveTo(s * 0.14f, s * 0.5f)
            p.lineTo(s * 0.5f, s * 0.15f)
            p.lineTo(s * 0.86f, s * 0.5f)
            p.lineTo(s * 0.86f, s * 0.85f)
            p.lineTo(s * 0.14f, s * 0.85f)
            p.close()
            drawPath(p, c, style = st)
        }
        1 -> {
            val xs = listOf(0.25f, 0.5f, 0.75f)
            val tops = listOf(0.5f, 0.18f, 0.66f)
            for (i in 0..2) {
                drawLine(
                    color = c,
                    start = Offset(s * xs[i], s * 0.85f),
                    end = Offset(s * xs[i], s * tops[i]),
                    strokeWidth = 3.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
        2 -> {
            drawCircle(c, radius = s * 0.38f, center = Offset(s / 2f, s / 2f), style = st)
            drawLine(c, Offset(s / 2f, s / 2f), Offset(s / 2f, s * 0.28f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            drawLine(c, Offset(s / 2f, s / 2f), Offset(s * 0.67f, s * 0.58f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        }
        else -> {
            val ys = listOf(0.25f, 0.5f, 0.75f)
            val ks = listOf(0.3f, 0.68f, 0.42f)
            for (i in 0..2) {
                drawLine(c, Offset(s * 0.12f, s * ys[i]), Offset(s * 0.88f, s * ys[i]), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                drawCircle(c, radius = 3.5.dp.toPx(), center = Offset(s * ks[i], s * ys[i]))
            }
        }
    }
}

@Composable
fun NavBar(tab: Int, onTab: (Int) -> Unit) {
    val names = listOf("Home", "Stats", "History", "Settings")
    val tap = rememberTap()
    Column {
        Spacer(Modifier.height(1.dp).fillMaxWidth().background(cLine))
        Row(
            Modifier
                .fillMaxWidth()
                .background(cCard)
                .padding(vertical = 8.dp)
        ) {
            names.forEachIndexed { i, n ->
                val on = tab == i
                val col by animateColorAsState(if (on) cPink else cDim, label = "nav")
                Column(
                    Modifier
                        .weight(1f)
                        .clickable {
                            tap()
                            onTab(i)
                        }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Canvas(Modifier.size(24.dp)) { navIcon(i, col) }
                    Text(n, color = col, fontSize = 11.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
}
