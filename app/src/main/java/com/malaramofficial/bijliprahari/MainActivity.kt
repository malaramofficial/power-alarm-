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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
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

private val Background = Color(0xFF070B12)
private val Surface = Color(0xFF101722)
private val Surface2 = Color(0xFF172231)
private val Yellow = Color(0xFFFFC107)
private val Green = Color(0xFF27D69B)
private val Red = Color(0xFFFF5D67)
private val Blue = Color(0xFF61A8FF)
private val TextPrimary = Color(0xFFF5F7FA)
private val TextMuted = Color(0xFF91A0B5)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BijliPrahariApp() }
    }
}

@Composable
fun BijliPrahariApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val auth = remember { AuthRepository(context) }
    var signedIn by remember { mutableStateOf(auth.isSignedIn()) }
    MaterialTheme(colorScheme = darkColorScheme(primary = Yellow, background = Background, surface = Surface)) {
        if (signedIn) {
            GssShell(auth) { auth.signOut(); signedIn = false }
        } else {
            LoginScreen(auth) { signedIn = true }
        }
    }
}

@Composable
private fun LoginScreen(auth: AuthRepository, onLoggedIn: () -> Unit) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(62.dp).background(Yellow, RoundedCornerShape(19.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Bolt, null, tint = Background, modifier = Modifier.size(37.dp))
                }
                Spacer(Modifier.width(15.dp))
                Column {
                    Text("बिजली प्रहरी", color = TextPrimary, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold)
                    Text("GSS Power Operations", color = TextMuted, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(18.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(25.dp)) {
                Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("GSS में प्रवेश", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Assigned feeders की live स्थिति और power events नियंत्रित करें।", color = TextMuted, fontSize = 13.sp)
                    OutlinedTextField(email, { email = it; error = null }, Modifier.fillMaxWidth(), label = { Text("ईमेल") }, singleLine = true)
                    OutlinedTextField(password, { password = it; error = null }, Modifier.fillMaxWidth(), label = { Text("पासवर्ड") }, singleLine = true)
                    if (error != null) Text(error!!, color = Red, fontSize = 13.sp)
                    Button(onClick = {
                        if (email.isBlank() || password.isBlank()) { error = "ईमेल और पासवर्ड भरें"; return@Button }
                        busy = true
                        scope.launch {
                            auth.signIn(email, password).onSuccess { onLoggedIn() }.onFailure { error = it.message ?: "लॉगिन विफल" }
                            busy = false
                        }
                    }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(15.dp)) {
                        Text(if (busy) "प्रमाणित किया जा रहा है…" else "सुरक्षित लॉगिन", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text("Firebase Authentication • अधिकृत GSS/Admin उपयोगकर्ताओं के लिए", color = TextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun GssShell(auth: AuthRepository, onLogout: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var role by remember { mutableStateOf(UserRole.UNKNOWN) }
    var feeders by remember { mutableStateOf<List<Feeder>>(emptyList()) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var events by remember { mutableStateOf<List<PowerEvent>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableIntStateOf(0) }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            try {
                role = auth.getCurrentRole()
                val ids = auth.getAssignedFeederIds()
                val repo = com.malaramofficial.bijliprahari.data.FeederRepository(context)
                feeders = repo.getAssignedFeeders(ids)
                if (selectedId !in feeders.map { it.id }) selectedId = feeders.firstOrNull()?.id
            } catch (e: Exception) {
                error = e.message ?: "डेटा लोड नहीं हो सका"
            } finally { loading = false }
        }
    }

    LaunchedEffect(Unit) { reload() }

    DisposableEffect(selectedId) {
        val id = selectedId ?: return@DisposableEffect onDispose { }
        val listeners = FeederListeners(context)
        val feederListener = listeners.listen(id) { feeder ->
            if (feeder != null) feeders = feeders.map { if (it.id == feeder.id) feeder else it }
        }
        val eventListener = listeners.listenEvents(id) { events = it }
        onDispose { feederListener?.remove(); eventListener?.remove() }
    }

    if (role != UserRole.GSS && role != UserRole.ADMIN) {
        RestrictedScreen(role, onLogout)
        return
    }

    Scaffold(containerColor = Background, topBar = { AppTopBar(role, onLogout) }, bottomBar = { BottomBar(tab) { tab = it } }) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when (tab) {
                0 -> DashboardScreen(feeders, selectedId, events, loading, error, { selectedId = it }, { reload() })
                1 -> FeedersScreen(feeders, selectedId) { selectedId = it; tab = 0 }
                2 -> HistoryScreen(events)
                else -> SettingsScreen(role, onLogout)
            }
        }
    }
}

@Composable
private fun AppTopBar(role: UserRole, onLogout: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Background).padding(horizontal = 18.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).background(Yellow, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Bolt, null, tint = Background, modifier = Modifier.size(25.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text("बिजली प्रहरी", color = TextPrimary, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
            Text(if (role == UserRole.ADMIN) "ADMIN • GSS नियंत्रण" else "GSS • LIVE OPERATIONS", color = TextMuted, fontSize = 11.sp)
        }
        IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, "लॉगआउट", tint = TextMuted) }
    }
}

@Composable
private fun DashboardScreen(feeders: List<Feeder>, selectedId: String?, events: List<PowerEvent>, loading: Boolean, error: String?, select: (String) -> Unit, reload: () -> Unit) {
    val feeder = feeders.firstOrNull { it.id == selectedId } ?: feeders.firstOrNull()
    val on = feeder?.currentState == "ON"
    val outageMinutes = events.firstOrNull { it.state == "OFF" && !it.verified }?.let { ((System.currentTimeMillis() - it.timestamp) / 60000).toInt().coerceAtLeast(0) }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(13.dp), contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("GSS कंट्रोल सेंटर", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Live feeder monitoring", color = TextMuted, fontSize = 13.sp)
                }
                IconButton(onClick = reload) { Icon(Icons.Default.Refresh, "रीफ्रेश", tint = Yellow) }
            }
        }
        if (error != null) item { AlertCard(error) }
        if (loading) item { LoadingCard() }
        if (feeders.isEmpty() && !loading) item { EmptyCard("इस खाते को अभी कोई feeder assign नहीं है।") }
        if (feeder != null) {
            item { FeederSelector(feeders, feeder, select) }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = if (on) Color(0xFF0D2B22) else Color(0xFF33171E)), shape = RoundedCornerShape(26.dp)) {
                    Column(Modifier.fillMaxWidth().padding(23.dp), verticalArrangement = Arrangement.spacedBy(17.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(67.dp).background(if (on) Green else Red, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(if (on) Icons.Default.CheckCircle else Icons.Default.ErrorOutline, null, tint = Color.White, modifier = Modifier.size(39.dp))
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(if (on) "बिजली चालू" else "बिजली बंद", color = TextPrimary, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold)
                                Text(if (on) "Feeder सामान्य स्थिति में" else "Power outage सक्रिय", color = Color(0xFFD5DCE5), fontSize = 13.sp)
                            }
                            Box(Modifier.size(11.dp).background(if (on) Green else Red, CircleShape))
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = .10f))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column { Text("LAST EVENT", color = TextMuted, fontSize = 10.sp); Text(feeder.lastEventAt?.let(::formatTime) ?: "—", color = TextPrimary, fontWeight = FontWeight.Bold) }
                            Column(horizontalAlignment = Alignment.End) { Text("OUTAGE", color = TextMuted, fontSize = 10.sp); Text(if (!on && outageMinutes != null) durationText(outageMinutes) else "—", color = if (!on) Red else TextPrimary, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard("गाँव", feeder.villageCount.toString(), Yellow, Modifier.weight(1f))
                    MetricCard("किसान", feeder.farmerCount.toString(), Green, Modifier.weight(1f))
                    MetricCard("EVENTS", events.size.toString(), Blue, Modifier.weight(1f))
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("हाल की गतिविधि", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("LIVE", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (events.isEmpty()) item { EmptyCard("इस feeder के लिए अभी power event उपलब्ध नहीं है।") }
            items(events.take(6)) { EventCard(it) }
        }
    }
}

@Composable
private fun FeederSelector(feeders: List<Feeder>, selected: Feeder, select: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth().clickable { expanded = true }) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("ACTIVE FEEDER", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(selected.name, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Text(selected.currentState, color = if (selected.currentState == "ON") Green else Red, fontWeight = FontWeight.ExtraBold)
            }
        }
        DropdownMenu(expanded, { expanded = false }) {
            feeders.forEach { f -> DropdownMenuItem(text = { Text(f.name) }, onClick = { select(f.id); expanded = false }) }
        }
    }
}

@Composable
private fun EventCard(event: PowerEvent) {
    val isOn = event.state == "ON"
    Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(17.dp)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(43.dp).background(if (isOn) Color(0xFF10382D) else Color(0xFF421E25), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Bolt, null, tint = if (isOn) Green else Red, modifier = Modifier.size(23.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(if (isOn) "बिजली वापस आई" else "बिजली चली गई", color = TextPrimary, fontWeight = FontWeight.Bold)
                Text("${formatDateTime(event.timestamp)} • ${event.source}", color = TextMuted, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (event.durationMinutes != null) Text(durationText(event.durationMinutes), color = Yellow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(if (event.verified) "सत्यापित" else "Pending", color = if (event.verified) Green else TextMuted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun FeedersScreen(feeders: List<Feeder>, selectedId: String?, select: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 28.dp)) {
        item { Text("Feeders", color = TextPrimary, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold); Text("आपके GSS को assigned live feeders", color = TextMuted, fontSize = 13.sp) }
        if (feeders.isEmpty()) item { EmptyCard("अभी कोई feeder assign नहीं है।") }
        items(feeders) { feeder ->
            Card(colors = CardDefaults.cardColors(containerColor = if (feeder.id == selectedId) Color(0xFF22233A) else Surface), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().clickable { select(feeder.id) }) {
                Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(feeder.name, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("${feeder.gssName} • ${feeder.villageCount} गाँव • ${feeder.farmerCount} किसान", color = TextMuted, fontSize = 11.sp)
                    }
                    Text(feeder.currentState, color = if (feeder.currentState == "ON") Green else Red, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun HistoryScreen(events: List<PowerEvent>) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 28.dp)) {
        item { Text("Power History", color = TextPrimary, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold); Text("Latest recorded feeder events", color = TextMuted, fontSize = 13.sp) }
        if (events.isEmpty()) item { EmptyCard("अभी कोई history उपलब्ध नहीं है।") }
        items(events) { EventCard(it) }
    }
}

@Composable
private fun SettingsScreen(role: UserRole, onLogout: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Settings", color = TextPrimary, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold); Text("Account और system जानकारी", color = TextMuted) }
        item { InfoCard("भूमिका", if (role == UserRole.ADMIN) "ADMIN" else "GSS", Icons.Default.Settings) }
        item { InfoCard("सिस्टम", "Firebase connected architecture", Icons.Default.Wifi) }
        item { InfoCard("Notifications", "FCM integration ready", Icons.Default.Bolt) }
        item {
            OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(15.dp)) {
                Icon(Icons.Default.Logout, null); Spacer(Modifier.width(8.dp)); Text("लॉगआउट")
            }
        }
    }
}

@Composable
private fun BottomBar(tab: Int, select: (Int) -> Unit) {
    NavigationBar(containerColor = Surface) {
        NavigationBarItem(tab == 0, { select(0) }, icon = { Icon(Icons.Default.Bolt, null) }, label = { Text("डैशबोर्ड") })
        NavigationBarItem(tab == 1, { select(1) }, icon = { Icon(Icons.Default.Wifi, null) }, label = { Text("Feeders") })
        NavigationBarItem(tab == 2, { select(2) }, icon = { Icon(Icons.Default.History, null) }, label = { Text("History") })
        NavigationBarItem(tab == 3, { select(3) }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") })
    }
}

@Composable private fun MetricCard(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(15.dp)) { Text(label, color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(5.dp)); Text(value, color = accent, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold) }
    }
}

@Composable private fun InfoCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Yellow); Spacer(Modifier.width(13.dp)); Column { Text(title, color = TextMuted, fontSize = 11.sp); Text(value, color = TextPrimary, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable private fun AlertCard(message: String) { Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF351B20)), shape = RoundedCornerShape(16.dp)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.ErrorOutline, null, tint = Red); Spacer(Modifier.width(10.dp)); Text(message, color = TextPrimary, fontSize = 13.sp) } } }
@Composable private fun LoadingCard() { Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(16.dp)) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp); Spacer(Modifier.width(12.dp)); Text("GSS data load हो रहा है…", color = TextMuted) } } }
@Composable private fun EmptyCard(message: String) { Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(18.dp)) { Text(message, color = TextMuted, modifier = Modifier.padding(18.dp), fontSize = 13.sp) } }
@Composable private fun RestrictedScreen(role: UserRole, onLogout: () -> Unit) { Box(Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Icon(Icons.Default.ErrorOutline, null, tint = Red, modifier = Modifier.size(54.dp)); Text("Access restricted", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold); Text("इस account की भूमिका ${role.name} है। GSS/Admin access आवश्यक है।", color = TextMuted); Button(onClick = onLogout) { Text("लॉगआउट") } } } }

private fun formatTime(millis: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
private fun formatDateTime(millis: Long): String = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(millis))
private fun durationText(minutes: Int): String = if (minutes < 60) "${minutes} min" else "${minutes / 60}h ${minutes % 60}m"
