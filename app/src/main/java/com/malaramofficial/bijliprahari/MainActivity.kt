package com.malaramofficial.bijliprahari

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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

private val Navy = Color(0xFF080C16)
private val Surface = Color(0xFF111827)
private val Surface2 = Color(0xFF172033)
private val Yellow = Color(0xFFFFC107)
private val Green = Color(0xFF34D399)
private val Red = Color(0xFFFB5B5B)
private val Muted = Color(0xFF94A3B8)

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
    MaterialTheme(colorScheme = darkColorScheme(primary = Yellow, background = Navy, surface = Surface)) {
        if (!signedIn) {
            LoginScreen(auth) { signedIn = true }
        } else {
            GssShell(auth) { auth.signOut(); signedIn = false }
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

    Box(Modifier.fillMaxSize().background(Navy), contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(58.dp).background(Yellow, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Bolt, null, tint = Navy, modifier = Modifier.size(34.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("बिजली प्रहरी", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text("GSS ऑपरेशन सिस्टम", color = Muted, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("GSS लॉगिन", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text("अपने GSS खाते से फीडर और बिजली स्थिति देखें।", color = Muted)
            OutlinedTextField(email, { email = it; error = null }, Modifier.fillMaxWidth(), label = { Text("ईमेल") }, singleLine = true)
            OutlinedTextField(password, { password = it; error = null }, Modifier.fillMaxWidth(), label = { Text("पासवर्ड") }, singleLine = true)
            if (error != null) Text(error!!, color = Red, fontSize = 13.sp)
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) { error = "ईमेल और पासवर्ड भरें"; return@Button }
                    busy = true
                    scope.launch {
                        val result = auth.signIn(email, password)
                        busy = false
                        result.onSuccess { onLoggedIn() }.onFailure { error = it.message ?: "लॉगिन विफल" }
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(15.dp)
            ) { Text(if (busy) "लॉगिन हो रहा है…" else "GSS में लॉगिन") }
            Text("सुरक्षित Firebase Authentication", color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun GssShell(auth: AuthRepository, onLogout: () -> Unit) {
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
                val repo = com.malaramofficial.bijliprahari.data.FeederRepository(androidx.compose.ui.platform.LocalContext.current)
                feeders = repo.getAssignedFeeders(ids)
                if (selectedId == null) selectedId = feeders.firstOrNull()?.id
            } catch (e: Exception) {
                error = e.message ?: "डेटा लोड नहीं हो सका"
            } finally { loading = false }
        }
    }

    LaunchedEffect(Unit) { reload() }

    val context = androidx.compose.ui.platform.LocalContext.current
    DisposableEffect(selectedId) {
        val id = selectedId
        if (id == null) return@DisposableEffect onDispose { }
        val listeners = FeederListeners(context)
        val a = listeners.listen(id) { feeder ->
            if (feeder != null) feeders = feeders.map { if (it.id == feeder.id) feeder else it }
        }
        val b = listeners.listenEvents(id) { events = it }
        onDispose { a?.remove(); b?.remove() }
    }

    if (role != UserRole.GSS && role != UserRole.ADMIN) {
        RestrictedScreen(role, onLogout)
        return
    }

    Scaffold(
        containerColor = Navy,
        topBar = { TopBar(role, onLogout) },
        bottomBar = { BottomBar(tab) { tab = it } }
    ) { pad ->
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

@Composable private fun TopBar(role: UserRole, onLogout: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Navy).padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("बिजली प्रहरी", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text(if (role == UserRole.ADMIN) "ADMIN • GSS नियंत्रण" else "GSS • ऑपरेशन डैशबोर्ड", color = Muted, fontSize = 12.sp)
        }
        IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, "लॉगआउट", tint = Muted) }
    }
}

@Composable private fun DashboardScreen(feeders: List<Feeder>, selectedId: String?, events: List<PowerEvent>, loading: Boolean, error: String?, select: (String) -> Unit, reload: () -> Unit) {
    val feeder = feeders.firstOrNull { it.id == selectedId } ?: feeders.firstOrNull()
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("आज की स्थिति", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                    Text(feeder?.name ?: "कोई assigned feeder नहीं", color = Muted)
                }
                IconButton(onClick = reload) { Icon(Icons.Default.Refresh, "रीफ्रेश", tint = Yellow) }
            }
        }
        if (error != null) item { AlertCard(error) }
        if (loading) item { LoadingCard() }
        if (feeders.isEmpty() && !loading) item { EmptyCard("इस GSS खाते को अभी कोई feeder assign नहीं है।") }
        if (feeder != null) {
            item { FeederSelector(feeders, feeder, select) }
            item { LiveStatusCard(feeder) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard("गाँव", feeder.villageCount.toString(), Yellow, Modifier.weight(1f))
                    MetricCard("किसान", feeder.farmerCount.toString(), Green, Modifier.weight(1f))
                    MetricCard("Events", events.size.toString(), Color.White, Modifier.weight(1f))
                }
            }
            item { SectionTitle("हाल की बिजली गतिविधि") }
            if (events.isEmpty()) item { EmptyCard("इस feeder के लिए अभी कोई power event नहीं मिला।") }
            items(events.take(8)) { EventCard(it) }
        }
    }
}

@Composable private fun FeederSelector(feeders: List<Feeder>, selected: Feeder, select: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) { Text("ACTIVE FEEDER", color = Muted, fontSize = 10.sp); Text(selected.name, color = Color.White, fontWeight = FontWeight.Bold) }
            Text(selected.currentState, color = if (selected.currentState == "ON") Green else Red, fontWeight = FontWeight.Bold)
        }
        DropdownMenu(expanded, { expanded = false }) { feeders.forEach { f -> DropdownMenuItem(text = { Text(f.name) }, onClick = { select(f.id); expanded = false }) } }
    }
}

@Composable private fun LiveStatusCard(feeder: Feeder) {
    val on = feeder.currentState == "ON"
    Card(colors = CardDefaults.cardColors(containerColor = if (on) Color(0xFF102D23) else Color(0xFF321B21)), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(62.dp).background(if (on) Green else Red, CircleShape), contentAlignment = Alignment.Center) { Icon(if (on) Icons.Default.CheckCircle else Icons.Default.ErrorOutline, null, tint = Color.White, modifier = Modifier.size(34.dp)) }
                Spacer(Modifier.width(15.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (on) "बिजली चालू" else "बिजली बंद", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text(if (on) "Feeder सामान्य स्थिति में" else "Outage सक्रिय है", color = Color(0xFFD1D5DB))
                }
                Box(Modifier.size(10.dp).background(if (on) Green else Red, CircleShape))
            }
            HorizontalDivider(color = Color.White.copy(alpha = .10f))
            Text("Last update: ${feeder.lastEventAt?.let { "डेटा प्राप्त" } ?: "अभी उपलब्ध नहीं"}", color = Muted, fontSize = 13.sp)
        }
    }
}

@Composable private fun EventCard(event: PowerEvent) {
    val on = event.state == "ON"
    Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(if (on) Color(0xFF123C2F) else Color(0xFF432329), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Bolt, null, tint = if (on) Green else Red) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(if (on) "बिजली आई" else "बिजली गई", color = Color.White, fontWeight = FontWeight.Bold)
                Text("${event.source} • ${if (event.verified) "सत्यापित" else "सत्यापन बाकी"}", color = Muted, fontSize = 12.sp)
            }
            Text(if (event.durationMinutes != null) "${event.durationMinutes} min" else "—", color = Yellow, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable private fun FeedersScreen(feeders: List<Feeder>, selectedId: String?, select: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { Text("Assigned Feeders", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold); Text("केवल आपके GSS को सौंपे गए feeder", color = Muted) }
        if (feeders.isEmpty()) item { EmptyCard("अभी कोई feeder assign नहीं है।") }
        items(feeders) { feeder ->
            Card(colors = CardDefaults.cardColors(containerColor = if (feeder.id == selectedId) Color(0xFF26213B) else Surface), shape = RoundedCornerShape(18.dp), onClick = { select(feeder.id) }) {
                Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(feeder.name, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold); Text("${feeder.gssName} • ${feeder.villageCount} गाँव • ${feeder.farmerCount} किसान", color = Muted, fontSize = 12.sp) }
                    Text(feeder.currentState, color = if (feeder.currentState == "ON") Green else Red, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable private fun HistoryScreen(events: List<PowerEvent>) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { Text("Power History", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold); Text("Live Firestore events", color = Muted) }
        if (events.isEmpty()) item { EmptyCard("अभी कोई event नहीं है।") }
        items(events) { EventCard(it) }
    }
}

@Composable private fun SettingsScreen(role: UserRole, onLogout: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        SettingCard("Role", role.name)
        SettingCard("Backend", "Firebase Authentication + Firestore")
        SettingCard("Notifications", "FCM power alerts")
        SettingCard("Access", "Assigned feeder only")
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Logout, null); Spacer(Modifier.width(8.dp)); Text("लॉगआउट") }
    }
}

@Composable private fun RestrictedScreen(role: UserRole, onLogout: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Navy).padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.ErrorOutline, null, tint = Red, modifier = Modifier.size(54.dp))
            Text("Access Restricted", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text("इस खाते की role ${role.name} है। GSS dashboard के लिए GSS role चाहिए।", color = Muted)
            Button(onClick = onLogout) { Text("लॉगआउट") }
        }
    }
}

@Composable private fun MetricCard(title: String, value: String, color: Color, modifier: Modifier) { Card(modifier, colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(17.dp)) { Column(Modifier.padding(13.dp)) { Text(title, color = Muted, fontSize = 11.sp); Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold) } } }
@Composable private fun SettingCard(title: String, value: String) { Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(17.dp)) { Column(Modifier.fillMaxWidth().padding(16.dp)) { Text(title, color = Muted, fontSize = 11.sp); Text(value, color = Color.White, fontWeight = FontWeight.Bold) } } }
@Composable private fun SectionTitle(text: String) { Text(text, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold) }
@Composable private fun EmptyCard(text: String) { Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(16.dp)) { Text(text, color = Muted, modifier = Modifier.padding(17.dp)) } }
@Composable private fun AlertCard(text: String) { Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF3A2025)), shape = RoundedCornerShape(16.dp)) { Text(text, color = Color(0xFFFFB4B4), modifier = Modifier.padding(17.dp), fontSize = 13.sp) } }
@Composable private fun LoadingCard() { Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(16.dp)) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp); Spacer(Modifier.width(12.dp)); Text("Firebase से data लोड हो रहा है…", color = Muted) } } }

@Composable private fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    NavigationBar(containerColor = Surface) {
        NavigationBarItem(selected == 0, { onSelect(0) }, icon = { Icon(Icons.Default.Bolt, null) }, label = { Text("Dashboard") })
        NavigationBarItem(selected == 1, { onSelect(1) }, icon = { Icon(Icons.Default.Wifi, null) }, label = { Text("Feeders") })
        NavigationBarItem(selected == 2, { onSelect(2) }, icon = { Icon(Icons.Default.History, null) }, label = { Text("History") })
        NavigationBarItem(selected == 3, { onSelect(3) }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") })
    }
}
