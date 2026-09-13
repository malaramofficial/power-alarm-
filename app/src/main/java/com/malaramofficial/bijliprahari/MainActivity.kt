package com.malaramofficial.bijliprahari

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
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
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContent { App() }
    }
}

@Composable
private fun App() {
    val context = LocalContext.current
    val auth = remember { AuthRepository(context) }
    var signedIn by remember { mutableStateOf(auth.isSignedIn()) }
    MaterialTheme(colorScheme = darkColorScheme(primary = Yellow, background = Bg, surface = CardBg)) {
        if (signedIn) Home(auth) { auth.signOut(); signedIn = false } else GoogleLogin(auth) { signedIn = true }
    }
}

@Composable
private fun GoogleLogin(auth: AuthRepository, done: () -> Unit) {
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            busy = false
            error = "Google लॉगिन रद्द किया गया।"
            return@rememberLauncherForActivityResult
        }
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            val token = account.idToken
            if (token.isNullOrBlank()) {
                busy = false
                error = "Google ID token नहीं मिला। Firebase/Google configuration जाँचें।"
                return@rememberLauncherForActivityResult
            }
            scope.launch {
                auth.signInWithGoogleIdToken(token)
                    .onSuccess { done() }
                    .onFailure { error = it.message ?: "Firebase लॉगिन विफल हुआ।" }
                busy = false
            }
        } catch (e: ApiException) {
            busy = false
            error = when (e.statusCode) {
                10 -> "Google configuration error (DEVELOPER_ERROR)। Firebase में SHA-1/SHA-256 और OAuth configuration जाँचें।"
                12501 -> "Google लॉगिन रद्द किया गया।"
                12500 -> "Google लॉगिन सेवा में समस्या है। फिर कोशिश करें।"
                else -> "Google लॉगिन विफल (code ${e.statusCode})।"
            }
        } catch (e: Exception) {
            busy = false
            error = e.message ?: "Google लॉगिन विफल हुआ।"
        }
    }
    Box(Modifier.fillMaxSize().background(Bg).padding(24.dp), Alignment.Center) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Box(Modifier.size(76.dp).background(Yellow, RoundedCornerShape(22.dp)), Alignment.Center) { Icon(Icons.Default.Bolt, null, tint = Bg, modifier = Modifier.size(46.dp)) }
            Text("बिजली प्रहरी", color = White, fontSize = 31.sp, fontWeight = FontWeight.ExtraBold)
            Text("GSS Power Operations", color = Muted, fontSize = 14.sp)
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(CardBg), shape = RoundedCornerShape(26.dp)) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("GSS में प्रवेश", color = White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Text("केवल अधिकृत Google खाते से साइन-इन करें।", color = Muted, fontSize = 13.sp)
                    error?.let { Text(it, color = Red, fontSize = 12.sp) }
                    Button(onClick = {
                        if (busy) return@Button
                        busy = true
                        error = null
                        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(context.getString(R.string.default_web_client_id))
                            .requestEmail()
                            .build()
                        launcher.launch(GoogleSignIn.getClient(context, options).signInIntent)
                    }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)) {
                        Text(if (busy) "Google से कनेक्ट हो रहा है…" else "G  Google से साइन-इन", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("आपकी भूमिका Firebase में GSS/Admin के रूप में निर्धारित होगी।", color = Muted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun Home(auth: AuthRepository, logout: () -> Unit) {
    val context = LocalContext.current
    var role by remember { mutableStateOf(UserRole.UNKNOWN) }
    var feeders by remember { mutableStateOf<List<Feeder>>(emptyList()) }
    var selected by remember { mutableStateOf<String?>(null) }
    var events by remember { mutableStateOf<List<PowerEvent>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var tab by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        try {
            role = auth.getCurrentRole()
            val repo = com.malaramofficial.bijliprahari.data.FeederRepository(context)
            feeders = repo.getAssignedFeeders(auth.getAssignedFeederIds())
            selected = feeders.firstOrNull()?.id
        } catch (e: Exception) {
            error = e.message ?: "डेटा लोड नहीं हो सका"
        }
        loading = false
    }
    DisposableEffect(selected) {
        val id = selected ?: return@DisposableEffect onDispose { }
        val l = FeederListeners(context)
        val a = l.listen(id) { f -> if (f != null) feeders = feeders.map { if (it.id == f.id) f else it } }
        val b = l.listenEvents(id) { events = it }
        onDispose { a?.remove(); b?.remove() }
    }
    if (role != UserRole.GSS && role != UserRole.ADMIN) { Restricted(role, logout); return }
    Scaffold(containerColor = Bg, topBar = { Top(role, logout) }, bottomBar = { Nav(tab) { tab = it } }) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when (tab) {
                0 -> Dashboard(feeders, selected, events, loading, error) { selected = it }
                1 -> Feeders(feeders, selected) { selected = it; tab = 0 }
                2 -> History(events)
                else -> Settings(role, logout)
            }
        }
    }
}

@Composable private fun Top(role: UserRole, logout: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Bg).padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).background(Yellow, RoundedCornerShape(13.dp)), Alignment.Center) { Icon(Icons.Default.Bolt, null, tint = Bg) }
        Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text("बिजली प्रहरी", color = White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold); Text(if (role == UserRole.ADMIN) "ADMIN • GSS CONTROL" else "GSS • LIVE OPERATIONS", color = Muted, fontSize = 10.sp) }
        IconButton(onClick = logout) { Icon(Icons.Default.Logout, "लॉगआउट", tint = Muted) }
    }
}

@Composable private fun Dashboard(feeders: List<Feeder>, selected: String?, events: List<PowerEvent>, loading: Boolean, error: String?, choose: (String) -> Unit) {
    val f = feeders.firstOrNull { it.id == selected } ?: feeders.firstOrNull()
    val on = f?.currentState == "ON"
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(13.dp), contentPadding = PaddingValues(bottom = 25.dp)) {
        item { Text("GSS कंट्रोल सेंटर", color = White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold); Text("Real-time power monitoring", color = Muted, fontSize = 13.sp) }
        error?.let { item { Text(it, color = Red, fontSize = 12.sp) } }
        if (loading) item { Text("Live data लोड हो रहा है…", color = Muted) }
        if (f == null && !loading) item { Empty("अभी कोई assigned feeder नहीं है।") }
        if (f != null) {
            item { Selector(feeders, f, choose) }
            item { Card(colors = CardDefaults.cardColors(if (on) Color(0xFF0D2B22) else Color(0xFF33171E)), shape = RoundedCornerShape(26.dp)) { Column(Modifier.padding(23.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(68.dp).background(if (on) Green else Red, CircleShape), Alignment.Center) { Icon(if (on) Icons.Default.CheckCircle else Icons.Default.ErrorOutline, null, tint = White, modifier = Modifier.size(40.dp)) }; Spacer(Modifier.width(16.dp)); Column { Text(if (on) "बिजली चालू" else "बिजली बंद", color = White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold); Text(if (on) "Feeder सामान्य स्थिति में" else "Power outage सक्रिय", color = Muted) } } } } }
            item { Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) { Metric("गाँव", f.villageCount.toString(), Yellow, Modifier.weight(1f)); Metric("किसान", f.farmerCount.toString(), Green, Modifier.weight(1f)); Metric("EVENTS", events.size.toString(), Blue, Modifier.weight(1f)) } }
            item { Text("हाल की गतिविधि", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            items(events.take(7)) { Event(it) }
        }
    }
}

@Composable private fun Selector(feeders: List<Feeder>, selected: Feeder, choose: (String) -> Unit) { var open by remember { mutableStateOf(false) }; Card(colors = CardDefaults.cardColors(CardBg), shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth().clickable { open = true }) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("ACTIVE FEEDER", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold); Text(selected.name, color = White, fontWeight = FontWeight.Bold) }; Text(selected.currentState, color = if (selected.currentState == "ON") Green else Red, fontWeight = FontWeight.ExtraBold) } }; DropdownMenu(expanded = open, onDismissRequest = { open = false }) { feeders.forEach { f -> DropdownMenuItem(text = { Text(f.name) }, onClick = { choose(f.id); open = false }) } } }
@Composable private fun Feeders(feeders: List<Feeder>, selected: String?, choose: (String) -> Unit) { LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { item { Text("Feeders", color = White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold); Text("Assigned GSS feeders", color = Muted) }; items(feeders) { f -> Card(colors = CardDefaults.cardColors(if (f.id == selected) Color(0xFF24223A) else CardBg), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().clickable { choose(f.id) }) { Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(f.name, color = White, fontWeight = FontWeight.Bold); Text("${f.gssName} • ${f.villageCount} गाँव • ${f.farmerCount} किसान", color = Muted, fontSize = 11.sp) }; Text(f.currentState, color = if (f.currentState == "ON") Green else Red, fontWeight = FontWeight.ExtraBold) } } } } }
@Composable private fun History(events: List<PowerEvent>) { LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { item { Text("Power History", color = White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold) }; items(events) { Event(it) } } }
@Composable private fun Event(e: PowerEvent) { val on = e.state == "ON"; Card(colors = CardDefaults.cardColors(CardBg), shape = RoundedCornerShape(17.dp)) { Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Bolt, null, tint = if (on) Green else Red); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(if (on) "बिजली वापस आई" else "बिजली चली गई", color = White, fontWeight = FontWeight.Bold); Text("${dateTime(e.timestamp)} • ${e.source}", color = Muted, fontSize = 11.sp) }; Text(if (e.verified) "सत्यापित" else "Pending", color = if (e.verified) Green else Muted, fontSize = 10.sp) } } }
@Composable private fun Settings(role: UserRole, logout: () -> Unit) { Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Settings", color = White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold); Text("भूमिका: ${role.name}", color = Muted); Text("Backend: Firebase", color = Muted); OutlinedButton(onClick = logout, modifier = Modifier.fillMaxWidth()) { Text("लॉगआउट") } } }
@Composable private fun Nav(tab: Int, set: (Int) -> Unit) { NavigationBar(containerColor = CardBg) { listOf("डैशबोर्ड", "फीडर", "इतिहास", "सेटिंग").forEachIndexed { i, label -> NavigationBarItem(selected = tab == i, onClick = { set(i) }, icon = { Icon(listOf(Icons.Default.Dashboard, Icons.Default.List, Icons.Default.History, Icons.Default.Settings)[i], null) }, label = { Text(label) }) } } }
@Composable private fun Metric(title: String, value: String, tint: Color, modifier: Modifier) { Card(modifier = modifier, colors = CardDefaults.cardColors(CardBg), shape = RoundedCornerShape(17.dp)) { Column(Modifier.padding(15.dp)) { Text(title, color = Muted, fontSize = 10.sp); Text(value, color = tint, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold) } } }
@Composable private fun Empty(text: String) { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(CardBg), shape = RoundedCornerShape(18.dp)) { Text(text, Modifier.padding(18.dp), color = Muted) } }
@Composable private fun Restricted(role: UserRole, logout: () -> Unit) { Box(Modifier.fillMaxSize().background(Bg).padding(24.dp), Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) { Icon(Icons.Default.Lock, null, tint = Red, modifier = Modifier.size(48.dp)); Text("Access restricted", color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold); Text("आपकी भूमिका: ${role.name}", color = Muted); Button(onClick = logout) { Text("लॉगआउट") } } } }
private fun dateTime(ms: Long): String = SimpleDateFormat("dd MMM, HH:mm", Locale("hi", "IN")).format(Date(ms))
