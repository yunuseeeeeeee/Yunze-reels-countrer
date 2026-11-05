package com.yunze.reels

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
    var tab by remember { mutableIntStateOf(0) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    MaterialTheme(colorScheme = darkColorScheme(primary = cViolet)) {
        Column(Modifier.fillMaxSize().background(cBg)) {
            Box(Modifier.weight(1f)) {
                Crossfade(targetState = tab, label = "tabs") { t ->
                    when (t) {
                        0 -> HomeScreen(ctx, tick)
                        1 -> StatsScreen(ctx, tick)
                        2 -> HistoryScreen(ctx, tick)
                        else -> SettingsScreen(ctx, tick)
                    }
                }
            }
            NavBar(tab) { tab = it }
        }
    }
}

@Composable
fun SetupCard(ctx: Context) {
    YCard {
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

@Composable
fun HomeScreen(ctx: Context, tick: Int) {
    val enabled = remember(tick) { serviceEnabled(ctx) }
    val today = remember(tick) { Store.day(ctx, 0) }
    val yesterday = remember(tick) { Store.day(ctx, 1) }
    val hours = remember(tick) { Store.hours(ctx, 0) }
    val limit = remember(tick) { Store.limitReels(ctx) }
    val prog = remember(tick) { Store.progress(ctx) }
    val level = remember(tick) { Store.level(ctx) }
    val name = remember(tick) { Store.str(ctx, "name", "") }
    val quiet = remember(tick) { Store.quiet(ctx) }
    val accent = accents[Store.int(ctx, "ovColor", 0).coerceIn(0, accents.size - 1)]
    var paused by remember { mutableStateOf(Store.paused(ctx)) }
    var sel by remember { mutableIntStateOf(-1) }
    val ringColor = when (level) {
        2 -> cRed
        1 -> cAmber
        else -> accent
    }
    val count by animateIntAsState(today.total, tween(600), label = "count")
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greet = when {
        hour < 12 -> "Good morning"
        hour < 18 -> "Good afternoon"
        else -> "Good evening"
    }
    val status = when {
        paused -> "Paused"
        quiet -> "Quiet hours"
        enabled -> "Counting"
        else -> "Not running"
    }
    val statusColor = if (status == "Counting") cGreen else cAmber

    Page {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(greet, color = cDim, fontSize = 14.sp)
                Text(
                    if (name.isBlank()) "Yunze" else name,
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Avatar(name)
        }

        if (!enabled) SetupCard(ctx)

        YCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Ring(if (prog < 0f) 0f else prog, ringColor, Modifier.size(230.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(count.toString(), color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.ExtraBold)
                        Text("reels today", color = cDim, fontSize = 14.sp)
                        Text(
                            if (limit > 0) "of $limit" else "no limit set",
                            color = ringColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(9.dp).clip(RoundedCornerShape(50)).background(statusColor))
                    Text("  $status", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                Chip(if (paused) "Resume" else "Pause", false) {
                    paused = !paused
                    Store.setBool(ctx, "paused", paused)
                }
            }
            val diff = today.total - yesterday.total
            Text(
                when {
                    diff > 0 -> "$diff more than yesterday"
                    diff < 0 -> "${-diff} fewer than yesterday"
                    else -> "Same as yesterday"
                },
                color = cDim,
                fontSize = 13.sp
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat("Swipes", today.swipes.toString())
            Stat("Replays", today.replays.toString())
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat("Time in reels", Store.fmt(today.millis))
            Stat("Per reel", if (today.total > 0) "${today.millis / today.total / 1000}s" else "0s")
        }

        YCard {
            val head = if (sel in 0..23) "$sel:00 to $sel:59: ${hours[sel]} reels" else "Reels per hour"
            Heading(head, "Tap a bar for details")
            Bars(hours, (0..23).map { if (it % 6 == 0) "$it" else "" }, sel) { sel = if (sel == it) -1 else it }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun StatsScreen(ctx: Context, tick: Int) {
    val days = remember(tick) { (13 downTo 0).map { Store.day(ctx, it) } }
    val week = days.takeLast(7)
    val thisW = week.sumOf { it.total }
    val lastW = days.take(7).sumOf { it.total }
    val heat = remember(tick) { (83 downTo 0).map { Store.day(ctx, it).total } }
    val recs = remember(tick) { Store.recs(ctx) }
    val rec = recs.values.maxByOrNull { it.total }
    val busiest = remember(tick) { Store.busiestHour(ctx) }
    val sessions = remember(tick) { Store.sessions(ctx) }
    val longest = sessions.maxOfOrNull { it.millis } ?: 0L
    val streak = remember(tick) { Store.streaks(ctx) }
    val badges = remember(tick) { Store.badges(ctx) }
    val monthMs = remember(tick) { (0..29).sumOf { Store.day(ctx, it).millis } }
    var selDay by remember { mutableIntStateOf(-1) }
    var selHeat by remember { mutableIntStateOf(-1) }
    var range by remember { mutableIntStateOf(0) }

    val delta = when {
        lastW == 0 -> "First week of data"
        thisW > lastW -> "${(thisW - lastW) * 100 / lastW}% more than last week"
        thisW < lastW -> "${(lastW - thisW) * 100 / lastW}% less than last week"
        else -> "Same as last week"
    }

    Page {
        Column {
            Text("Insights", color = cDim, fontSize = 14.sp)
            Text("Stats", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
        }

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF3B1D8F), Color(0xFF9B1F5F))))
                .padding(22.dp)
        ) {
            Text("This week", color = Color(0xCCFFFFFF), fontSize = 14.sp)
            Text(thisW.toString(), color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.ExtraBold)
            Text(delta, color = Color(0xFFFFD6DF), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                "Average ${thisW / 7} per day, ${Store.fmt(week.sumOf { it.millis })} in total",
                color = Color(0xCCFFFFFF),
                fontSize = 13.sp
            )
        }

        YCard {
            val head = if (selDay in 0..6) "${week[selDay].label}: ${week[selDay].total} reels" else "Last 7 days"
            Heading(head, "Tap a bar for details")
            Bars(week.map { it.total }, week.map { it.label }, selDay) { selDay = if (selDay == it) -1 else it }
        }

        YCard {
            val head = if (selHeat in heat.indices) {
                "${Store.nice(Store.key(83 - selHeat))}: ${heat[selHeat]} reels"
            } else "Last 12 weeks"
            Heading(head, "Darker means more reels")
            Heatmap(heat, selHeat) { selHeat = if (selHeat == it) -1 else it }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat("Record day", rec?.total?.toString() ?: "0", rec?.let { Store.nice(it.key) } ?: "no data yet")
            Stat("Busiest hour", busiest?.let { "${it.first}:00" } ?: "-", busiest?.let { "${it.second} reels total" } ?: "no data yet")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat("Longest session", if (longest > 0) Store.fmt(longest) else "-", "in one go")
            Stat(
                "Streak",
                if (Store.limitReels(ctx) > 0) "${streak.first} days" else "-",
                if (Store.limitReels(ctx) > 0) "best ${streak.second}" else "set a limit"
            )
        }

        YCard {
            Heading("Perspective", "Your last 30 days in reels")
            if (monthMs > 0) {
                val mins = monthMs / 60000.0
                Text(Store.fmt(monthMs), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("About ${"%.1f".format(mins / 110.0)} movies", color = cDim, fontSize = 14.sp)
                Text("About ${(mins / 3.5).toInt()} songs", color = cDim, fontSize = 14.sp)
                Text("About ${"%.1f".format(mins / 480.0)} nights of sleep", color = cDim, fontSize = 14.sp)
            } else {
                Text("Nothing yet. Watch some reels first.", color = cDim, fontSize = 14.sp)
            }
        }

        YCard {
            Heading("Achievements", "${badges.count { it.done }} of ${badges.size} unlocked")
            badges.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { b ->
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (b.done) Color(0x33F43F5E) else cCard2)
                                .padding(12.dp)
                        ) {
                            Text(
                                b.title,
                                color = if (b.done) Color.White else cDim,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(b.desc, color = cDim, fontSize = 11.sp)
                            Text(
                                if (b.done) "Unlocked" else "Locked",
                                color = if (b.done) cGreen else cDim,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun HistoryScreen(ctx: Context, tick: Int) {
    val sessions = remember(tick) { Store.sessions(ctx).take(60) }
    val dayFmt = remember { SimpleDateFormat("EEEE, MMM d", Locale.ENGLISH) }
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.ENGLISH) }
    val todayKey = Store.key(0)
    val todaySessions = sessions.filter { SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(it.start)) == todayKey }
    val avg = if (sessions.isNotEmpty()) sessions.sumOf { it.millis } / sessions.size else 0L

    Page {
        Column {
            Text("Every time you opened reels", color = cDim, fontSize = 14.sp)
            Text("History", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat("Sessions today", todaySessions.size.toString())
            Stat("Average length", if (avg > 0) Store.fmt(avg) else "-")
        }
        if (sessions.isEmpty()) {
            YCard {
                Text("No sessions yet", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    "A session is saved after you leave Reels for a minute.",
                    color = cDim,
                    fontSize = 13.sp
                )
            }
        } else {
            val groups = sessions.groupBy { dayFmt.format(Date(it.start)) }
            groups.forEach { (title, list) ->
                YCard {
                    Row(Modifier.fillMaxWidth()) {
                        Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("${list.sumOf { it.reels }} reels", color = cPink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    list.forEach { s ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${timeFmt.format(Date(s.start))} - ${timeFmt.format(Date(s.end))}",
                                color = cDim,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${s.reels} reels, ${Store.fmt(s.millis)}",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun SettingsScreen(ctx: Context, tick: Int) {
    var name by remember { mutableStateOf(Store.str(ctx, "name", "")) }
    var debug by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var confirmErase by remember { mutableStateOf(false) }

    Page {
        Column {
            Text("Make it yours", color = cDim, fontSize = 14.sp)
            Text("Settings", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
        }

        YCard {
            Heading("Profile")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Avatar(name)
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(cCard2)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    if (name.isEmpty()) Text("Your name", color = cDim, fontSize = 16.sp)
                    BasicTextField(
                        value = name,
                        onValueChange = {
                            name = it.take(20)
                            Store.setStr(ctx, "name", name)
                        },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                        cursorBrush = SolidColor(cPink),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        YCard {
            Heading("Daily limit", "The ring and warnings follow these")
            IntSetting(ctx, "Reels per day", "limit", 100, 0, 1000, 10, "")
            IntSetting(ctx, "Minutes per day", "limitMin", 0, 0, 480, 5, "m")
        }

        YCard {
            Heading("Reminders")
            BoolSetting(ctx, "Notifications", "Limit warnings, breaks and weekly summary", "notif", true)
            IntSetting(ctx, "Break reminder after", "breakMin", 15, 0, 120, 5, "m")
        }

        YCard {
            Heading("Quiet hours", "Counting and alerts stop during this time")
            BoolSetting(ctx, "Enable quiet hours", "", "quiet", false)
            IntSetting(ctx, "From", "qs", 23, 0, 23, 1, ":00", "")
            IntSetting(ctx, "Until", "qe", 7, 0, 23, 1, ":00", "")
        }

        YCard {
            Heading("Floating counter", "Shown on top of Instagram while you watch reels")
            BoolSetting(ctx, "Show floating counter", "", "overlay", true)
            ChoiceSetting(ctx, "Position", "ovPos", 1, listOf("Top left", "Top", "Top right", "Bottom left", "Bottom", "Bottom right"))
            ChoiceSetting(ctx, "Size", "ovSize", 1, listOf("Small", "Medium", "Large"))
            ChoiceSetting(ctx, "Text", "ovStyle", 1, listOf("Number", "With label", "With time", "Progress"))
            ColorSetting(ctx, "Color", "ovColor")
        }

        YCard {
            Heading("Your data", "Everything stays on this phone")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        val i = Intent(Intent.ACTION_SEND)
                        i.type = "text/plain"
                        i.putExtra(Intent.EXTRA_TEXT, Store.csv(ctx))
                        ctx.startActivity(Intent.createChooser(i, "Export data").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = cViolet, contentColor = Color.White)
                ) { Text("Export CSV") }
                Button(
                    onClick = {
                        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val t = cm.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                        val n = Store.importCsv(ctx, t)
                        message = if (n > 0) "Imported $n days" else "Copy an exported CSV first"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = cCard2, contentColor = Color.White)
                ) { Text("Import from clipboard") }
            }
            if (message.isNotEmpty()) Text(message, color = cGreen, fontSize = 13.sp)
            Button(
                onClick = {
                    if (confirmErase) {
                        Store.eraseData(ctx)
                        confirmErase = false
                        message = "All data erased"
                    } else confirmErase = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (confirmErase) cRed else cCard2,
                    contentColor = Color.White
                )
            ) { Text(if (confirmErase) "Tap again to erase everything" else "Erase all data") }
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
            YCard {
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
        Spacer(Modifier.height(8.dp))
    }
}
