package com.malaramofficial.bijliprahari

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malaramofficial.bijliprahari.data.AuthRepository
import com.malaramofficial.bijliprahari.data.Feeder
import com.malaramofficial.bijliprahari.data.FeederListeners
import com.malaramofficial.bijliprahari.data.PowerEvent
import com.malaramofficial.bijliprahari.data.UserRole
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Bg = Color(0xFF070B12)
private val CardBg = Color(0xFF101722)
private val Yellow = Color(0xFFFFC107)
private val Green = Color(0xFF27D69B)
private val Red = Color(0xFFFF5D67)
private val Blue = Color(0xFF61A8FF)
private val White = Color(0xFFF5F7FA)
private val Muted = Color(0xFF91A0B5)

class MainActivity : ComponentActivity() {
    override fun onCreate(state: Bundle?) { super.onCreate(state); setContent { App() } }
}

@Composable
private fun App() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val auth = remember { AuthRepository(context) }
    var signedIn by remember { mutableStateOf(auth.isSignedIn()) }
    MaterialTheme(colorScheme = darkColorScheme(primary = Yellow, background = Bg, surface = CardBg)) {
        if (signedIn) Home(auth) { auth.signOut(); signedIn = false } else Login(auth) { signedIn = true }
    }
}

@Composable
private fun Login(auth: AuthRepository, done: () -> Unit) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }
    Box(Modifier.fillMaxSize().background(Bg).padding(24.dp), Alignment.Center) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(58.dp).background(Yellow, RoundedCornerShape(18.dp)), Alignment.Center) { Icon(Icons.Default.Bolt, null, tint = Bg, modifier = Modifier.size(35.dp)) }
                Spacer(Modifier.width(14.dp)); Column { Text("बिजली प्रहरी", color = White, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold); Text("GSS Power Operations", color = Muted, fontSize = 13.sp) }
            }
            Card(colors = CardDefaults.cardColors(CardBg), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Text("GSS में प्रवेश", color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Live feeder monitoring के लिए अधिकृत खाते से लॉगिन करें।", color = Muted, fontSize = 13.sp)
                    OutlinedTextField(email, { email = it; error = null }, Modifier.fillMaxWidth(), label = { Text("ईमेल") }, singleLine = true)
                    OutlinedTextField(password, { password = it; error = null }, Modifier.fillMaxWidth(), label = { Text("पासवर्ड") }, singleLine = true)
                    error?.let { Text(it, color = Red, fontSize = 12.sp) }
                    Button(onClick = {
                        if (email.isBlank() || password.isBlank()) { error = "ईमेल और पासवर्ड भरें"; return@Button }
                        busy = true; scope.launch { auth.signIn(email, password).onSuccess { done() }.onFailure { error = it.message ?: "लॉगिन विफल" }; busy = false }
                    }, enabled = !busy, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(15.dp)) { Text(if (busy) "लॉगिन हो रहा है…" else "सुरक्षित लॉगिन", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun Home(auth: AuthRepository, logout: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var role by remember { mutableStateOf(UserRole.UNKNOWN) }
    var feeders by remember { mutableStateOf<List<Feeder>>(emptyList()) }
    var selected by remember { mutableStateOf<String?>(null) }
    var events by remember { mutableStateOf<List<PowerEvent>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var tab by remember { mutableIntStateOf(0) }

    LaunchedEffect(refresh) {
        loading = true; error = null
        try {
            role = auth.getCurrentRole()
            val ids = auth.getAssignedFeederIds()
            val repo = com.malaramofficial.bijliprahari.data.FeederRepository(context)
            feeders = repo.getAssignedFeeders(ids)
            if (selected !in feeders.map { it.id }) selected = feeders.firstOrNull()?.id
        } catch (e: Exception) { error = e.message ?: "डेटा लोड नहीं हो सका" }
        loading = false
    }

    DisposableEffect(selected) {
        val id = selected
        if (id == null) return@DisposableEffect onDispose { }
        val l = FeederListeners(context)
        val a = l.listen(id) { f -> if (f != null) feeders = feeders.map { if (it.id == f.id) f else it } }
        val b = l.listenEvents(id) { events = it }
        onDispose { a?.remove(); b?.remove() }
    }

    if (role != UserRole.GSS && role != UserRole.ADMIN) { Restricted(role, logout); return }
    Scaffold(containerColor = Bg, topBar = { Top(role, logout) }, bottomBar = { Nav(tab) { tab = it } }) { pad ->
        when (tab) {
            0 -> Dashboard(feeders, selected, events, loading, error, { selected = it }, { refresh++ }, scope)
            1 -> FeederList(feeders, selected) { selected = it; tab = 0 }
            2 -> History(events)
            else -> Settings(role, logout)
        }
    }
}

@Composable private fun Top(role: UserRole, logout: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Bg).padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).background(Yellow, RoundedCornerShape(13.dp)), Alignment.Center) { Icon(Icons.Default.Bolt, null, tint = Bg) }
        Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text("बिजली प्रहरी", color = White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold); Text(if (role == UserRole.ADMIN) "ADMIN • GSS CONTROL" else "GSS • LIVE OPERATIONS", color = Muted, fontSize = 10.sp) }
        IconButton(logout) { Icon(Icons.Default.Logout, "लॉगआउट", tint = Muted) }
    }
}

@Composable private fun Dashboard(feeders: List<Feeder>, selected: String?, events: List<PowerEvent>, loading: Boolean, error: String?, choose: (String) -> Unit, reload: () -> Unit, scope: kotlinx.coroutines.CoroutineScope) {
    val feeder = feeders.firstOrNull { it.id == selected } ?: feeders.firstOrNull()
    val isOn = feeder?.currentState == "ON"
    val outage = if (!isOn) events.firstOrNull { it.state == "OFF" }?.let { ((System.currentTimeMillis() - it.timestamp) / 60000).toInt().coerceAtLeast(0) } else null
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(13.dp), contentPadding = PaddingValues(bottom = 28.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("GSS कंट्रोल सेंटर", color = White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold); Text("Real-time power monitoring", color = Muted, fontSize = 13.sp) }; IconButton(reload) { Icon(Icons.Default.Refresh, "रीफ्रेश", tint = Yellow) } } }
        error?.let { item { Alert(it) } }
        if (loading) item { Loading() }
        if (!loading && feeder == null) item { Empty("कोई assigned feeder नहीं मिला।") }
        if (feeder != null) {
            item { Selector(feeders, feeder, choose) }
            item {
                Card(colors = CardDefaults.cardColors(if (isOn) Color(0xFF0D2B22) else Color(0xFF33171E)), shape = RoundedCornerShape(26.dp)) {
                    Column(Modifier.padding(23.dp), verticalArrangement = Arrangement.spacedBy(17.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(68.dp).background(if (isOn) Green else Red, CircleShape), Alignment.Center) { Icon(if (isOn) Icons.Default.CheckCircle else Icons.Default.ErrorOutline, null, tint = White, modifier = Modifier.size(40.dp)) }
                            Spacer(Modifier.width(16.dp)); Column(Modifier.weight(1f)) { Text(if (isOn) "बिजली चालू" else "बिजली बंद", color = White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold); Text(if (isOn) "Feeder सामान्य स्थिति में" else "Power outage सक्रिय", color = White.copy(.75f), fontSize = 13.sp) }
                        }
                        HorizontalDivider(color = White.copy(.1f))
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Stat("LAST EVENT", feeder.lastEventAt?.let(::time) ?: "—"); Stat("OUTAGE", outage?.let(::duration) ?: "—") }
                    }
                }
            }
            item { Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) { Metric("गाँव", feeder.villageCount.toString(), Yellow, Modifier.weight(1f)); Metric("किसान", feeder.farmerCount.toString(), Green, Modifier.weight(1f)); Metric("EVENTS", events.size.toString(), Blue, Modifier.weight(1f)) } }
            item { Row(verticalAlignment = Alignment.CenterVertically) { Text("हाल की गतिविधि", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text("LIVE", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold) } }
            if (events.isEmpty()) item { Empty("इस feeder के लिए अभी power event उपलब्ध नहीं है।") }
            items(events.take(7)) { Event(it) }
        }
    }
}

@Composable private fun Selector(feeders: List<Feeder>, selected: Feeder, choose: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(CardBg), shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth().clickable { open = true }) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("ACTIVE FEEDER", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold); Text(selected.name, color = White, fontWeight = FontWeight.Bold) }; Text(selected.currentState, color = if (selected.currentState == "ON") Green else Red, fontWeight = FontWeight.ExtraBold) }
    }
    DropdownMenu(open, { open = false }) { feeders.forEach { f -> DropdownMenuItem({ Text(f.name) }, { choose(f.id); open = false }) } }
}

@Composable private fun FeederList(feeders: List<Feeder>, selected: String?, choose: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Feeders", color = White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold); Text("Assigned GSS feeders", color = Muted) }
        if (feeders.isEmpty()) item { Empty("अभी कोई feeder assign नहीं है।") }
        items(feeders) { f -> Card(colors = CardDefaults.cardColors(if (f.id == selected) Color(0xFF24223A) else CardBg), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().clickable { choose(f.id) }) { Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(f.name, color = White, fontWeight = FontWeight.Bold); Text("${f.gssName} • ${f.villageCount} गाँव • ${f.farmerCount} किसान", color = Muted, fontSize = 11.sp) }; Text(f.currentState, color = if (f.currentState == "ON") Green else Red, fontWeight = FontWeight.ExtraBold) } } }
    }
}

@Composable private fun History(events: List<PowerEvent>) { LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { item { Text("Power History", color = White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold); Text("Latest recorded events", color = Muted) }; if (events.isEmpty()) item { Empty("अभी कोई history उपलब्ध नहीं है।") }; items(events) { Event(it) } } }

@Composable private fun Event(e: PowerEvent) { val on = e.state == "ON"; Card(colors = CardDefaults.cardColors(CardBg), shape = RoundedCornerShape(17.dp)) { Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(43.dp).background(if (on) Color(0xFF10382D) else Color(0xFF421E25), CircleShape), Alignment.Center) { Icon(Icons.Default.Bolt, null, tint = if (on) Green else Red) }; Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(if (on) "बिजली वापस आई" else "बिजली चली गई", color = White, fontWeight = FontWeight.Bold); Text("${dateTime(e.timestamp)} • ${e.source}", color = Muted, fontSize = 11.sp) }; Column(horizontalAlignment = Alignment.End) { e.durationMinutes?.let { Text(duration(it), color = Yellow, fontWeight = FontWeight.Bold, fontSize = 11.sp) }; Text(if (e.verified) "सत्यापित" else "Pending", color = if (e.verified) Green else Muted, fontSize = 10.sp) } } } }

@Composable private fun Settings(role: UserRole, logout: () -> Unit) { LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { item { Text("Settings", color = White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold); Text("System और account", color = Muted) }; item { Info("भूमिका", role.name, Icons.Default.Settings) }; item { Info("Backend", "Firebase", Icons.Default.Wifi) }; item { Info("Notifications", "FCM ready", Icons.Default.Notifications) }; item { OutlinedButton(logout, Modifier.fillMaxWidth().height(52.dp)) { Icon(Icons.Default.Logout, null); Spacer(Modifier.width(8.dp)); Text("लॉगआउट") } } } }

@Composable private fun Info(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) { Card(colors = CardDefaults.cardColors(CardBg), shape = RoundedCornerShape(18.dp)) { Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Yellow); Spacer(Modifier.width(12.dp)); Column { Text(title, color = Muted, fontSize = 11.sp); Text(value, color = White, fontWeight = FontWeight.Bold) } } } }

@Composable private fun Metric(label: String, value: String, accent: Color, modifier: Modifier) { Card(modifier, colors = CardDefaults.cardColors(CardBg), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(15.dp)) { Text(label, color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold); Text(value, color = accent, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold) } } }
@Composable private fun Stat(label: String, value: String) { Column { Text(label, color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold); Text(value, color = White, fontWeight = FontWeight.Bold) } }
@Composable private fun Alert(msg: String) { Card(colors = CardDefaults.cardColors(Color(0xFF351B20)), shape = RoundedCornerShape(15.dp)) { Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.ErrorOutline, null, tint = Red); Spacer(Modifier.width(9.dp)); Text(msg, color = White, fontSize = 12.sp) } } }
@Composable private fun Loading() { Card(colors = CardDefaults.cardColors(CardBg), shape = RoundedCornerShape(15.dp)) { Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(21.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)); Text("GSS data load हो रहा है…", color = Muted) } } }
@Composable private fun Empty(msg: String) { Card(colors = CardDefaults.cardColors(CardBg), shape = RoundedCornerShape(17.dp)) { Text(msg, color = Muted, modifier = Modifier.padding(17.dp), fontSize = 12.sp) } }
@Composable private fun Restricted(role: UserRole, logout: () -> Unit) { Box(Modifier.fillMaxSize().background(Bg), Alignment.Center) { Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { Icon(Icons.Default.ErrorOutline, null, tint = Red, modifier = Modifier.size(52.dp)); Text("Access restricted", color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold); Text("Role: ${role.name}. GSS/Admin access आवश्यक है।", color = Muted); Button(logout) { Text("लॉगआउट") } } } }
@Composable private fun Nav(tab: Int, select: (Int) -> Unit) { NavigationBar(containerColor = CardBg) { NavigationBarItem(tab == 0, { select(0) }, { Icon(Icons.Default.Bolt, null) }, label = { Text("डैशबोर्ड") }); NavigationBarItem(tab == 1, { select(1) }, { Icon(Icons.Default.Wifi, null) }, label = { Text("Feeders") }); NavigationBarItem(tab == 2, { select(2) }, { Icon(Icons.Default.History, null) }, label = { Text("History") }); NavigationBarItem(tab == 3, { select(3) }, { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") }) } }

private fun time(v: Long) = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(v))
private fun dateTime(v: Long) = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(v))
private fun duration(m: Int) = if (m < 60) "${m} min" else "${m / 60}h ${m % 60}m"
