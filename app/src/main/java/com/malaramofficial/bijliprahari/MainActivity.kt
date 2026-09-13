package com.malaramofficial.bijliprahari

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Base64
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.malaramofficial.bijliprahari.data.AuthRepository
import com.malaramofficial.bijliprahari.data.Feeder
import com.malaramofficial.bijliprahari.data.FeederListeners
import com.malaramofficial.bijliprahari.data.PowerEvent
import com.malaramofficial.bijliprahari.data.UserRole
import kotlinx.coroutines.launch
import java.security.MessageDigest
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
    var adminOpen by remember { mutableStateOf(false) }
    MaterialTheme(colorScheme = darkColorScheme(primary = Yellow, background = Bg, surface = CardBg)) {
        if (adminOpen) AdminPanel(auth) { adminOpen = false }
        else FarmerApp { adminOpen = true }
    }
}

@Composable
private fun FarmerApp(openAdmin: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    Scaffold(
        containerColor = Bg,
        topBar = { FarmerTop() },
        bottomBar = { NavigationBar(containerColor = CardBg) {
            listOf("होम", "इतिहास", "सेटिंग").forEachIndexed { i, label ->
                NavigationBarItem(
                    selected = tab == i,
                    onClick = { tab = i },
                    icon = { Icon(listOf(Icons.Default.Home, Icons.Default.History, Icons.Default.Settings)[i], null) },
                    label = { Text(label) }
                )
            }
        }}
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when (tab) {
                0 -> FarmerHome()
                1 -> FarmerHistory()
                else -> FarmerSettings(openAdmin)
            }
        }
    }
}

@Composable
private fun FarmerTop() {
    Row(Modifier.fillMaxWidth().background(Bg).padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).background(Yellow, RoundedCornerShape(13.dp)), Alignment.Center) {
            Icon(Icons.Default.Bolt, null, tint = Bg, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("बिजली प्रहरी", color = White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text("किसान सेवा", color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun FarmerHome() {
    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text("नमस्ते किसान 👋", color = White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
            Text("अपने फीडर की बिजली की स्थिति देखें", color = Muted, fontSize = 13.sp)
        }
        item {
            Card(colors = CardDefaults.cardColors(Color(0xFF0D2B22)), shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(23.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(62.dp).background(Green, CircleShape), Alignment.Center) {
                            Icon(Icons.Default.CheckCircle, null, tint = White, modifier = Modifier.size(38.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("बिजली चालू", color = White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                            Text("मीठी बेरी फीडर", color = Muted)
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                    Text("जब बिजली जाएगी या आएगी, आपको अलर्ट मिलेगा।", color = Muted, fontSize = 12.sp)
                }
            }
        }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FarmerMetric("आज", "ON", Green, Modifier.weight(1f))
            FarmerMetric("स्थिति", "सामान्य", Yellow, Modifier.weight(1f))
        }}
        item {
            Card(colors = CardDefaults.cardColors(CardBg), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, null, tint = Yellow, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("बिजली अलर्ट", color = White, fontWeight = FontWeight.Bold)
                        Text("ON/OFF की सूचना अपने आप मिलेगी", color = Muted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun FarmerHistory() {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("बिजली इतिहास", color = White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold); Text("हाल की बिजली गतिविधि", color = Muted) }
        item { Empty("अभी कोई इतिहास उपलब्ध नहीं है। IoT डिवाइस जुड़ने पर वास्तविक रिकॉर्ड यहाँ आएगा।") }
    }
}

@Composable
private fun FarmerSettings(openAdmin: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("सेटिंग", color = White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
        Text("बिजली प्रहरी की किसान सेवा", color = Muted)
        Card(colors = CardDefaults.cardColors(CardBg), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().clickable { openAdmin() }) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).background(Color(0xFF24223A), CircleShape), Alignment.Center) {
                    Icon(Icons.Default.AdminPanelSettings, null, tint = Yellow)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Admin Panel", color = White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("GSS और मुख्य प्रशासन", color = Muted, fontSize = 12.sp)
                }
                Icon(Icons.Default.ChevronRight, null, tint = Muted)
            }
        }
        Text("Admin Panel खोलने के लिए पासवर्ड जरूरी है।", color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun AdminPanel(auth: AuthRepository, close: () -> Unit) {
    val context = LocalContext.current
    var unlocked by remember { mutableStateOf(AdminGate.isConfigured(context) && false) }
    var showSetup by remember { mutableStateOf(!AdminGate.isConfigured(context)) }
    var authenticated by remember { mutableStateOf(false) }

    if (!authenticated) {
        AdminGateScreen(
            setup = showSetup,
            onSuccess = { authenticated = true; showSetup = false },
            onCancel = close
        )
        return
    }

    var role by remember { mutableStateOf(UserRole.UNKNOWN) }
    var feeders by remember { mutableStateOf<List<Feeder>>(emptyList()) }
    var selected by remember { mutableStateOf<String?>(null) }
    var events by remember { mutableStateOf<List<PowerEvent>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var tab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        if (!auth.isSignedIn()) { loading = false; return@LaunchedEffect }
        try {
            role = auth.getCurrentRole()
            val repo = com.malaramofficial.bijliprahari.data.FeederRepository(context)
            feeders = repo.getAssignedFeeders(auth.getAssignedFeederIds())
            selected = feeders.firstOrNull()?.id
        } catch (e: Exception) { error = e.message ?: "डेटा लोड नहीं हो सका" }
        loading = false
    }
    DisposableEffect(selected) {
        val id = selected ?: return@DisposableEffect onDispose { }
        val l = FeederListeners(context)
        val a = l.listen(id) { f -> if (f != null) feeders = feeders.map { if (it.id == f.id) f else it } }
        val b = l.listenEvents(id) { events = it }
        onDispose { a?.remove(); b?.remove() }
    }

    if (!auth.isSignedIn()) {
        AdminGoogleLogin(auth, close)
        return
    }
    Scaffold(
        containerColor = Bg,
        topBar = { AdminTop(role, close) },
        bottomBar = { NavigationBar(containerColor = CardBg) {
            listOf("डैशबोर्ड", "GSS/फीडर", "इतिहास", "सेटिंग").forEachIndexed { i, label ->
                NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Icon(listOf(Icons.Default.Dashboard, Icons.Default.AccountTree, Icons.Default.History, Icons.Default.Settings)[i], null) }, label = { Text(label) })
            }
        }}
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when (tab) {
                0 -> Dashboard(feeders, selected, events, loading, error) { selected = it }
                1 -> Feeders(feeders, selected) { selected = it; tab = 0 }
                2 -> History(events)
                else -> AdminSettings(role, close)
            }
        }
    }
}

@Composable
private fun AdminGateScreen(setup: Boolean, onSuccess: () -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().background(Bg).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.AdminPanelSettings, null, tint = Yellow, modifier = Modifier.size(62.dp))
        Spacer(Modifier.height(16.dp))
        Text(if (setup) "Admin Panel सेटअप" else "Admin Panel", color = White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Text(if (setup) "पहली बार एक Admin पासवर्ड बनाएं" else "पासवर्ड डालकर प्रशासन खोलें", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(value = password, onValueChange = { password = it; error = null }, label = { Text(if (setup) "नया पासवर्ड" else "Admin पासवर्ड") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        if (setup) {
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = confirm, onValueChange = { confirm = it; error = null }, label = { Text("पासवर्ड दोबारा") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        error?.let { Text(it, color = Red, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp)) }
        Spacer(Modifier.height(18.dp))
        Button(onClick = {
            if (password.length < 6) { error = "पासवर्ड कम से कम 6 अक्षरों का रखें।"; return@Button }
            if (setup && password != confirm) { error = "दोनों पासवर्ड समान नहीं हैं।"; return@Button }
            if (setup) { AdminGate.setPassword(context, password); onSuccess() }
            else if (AdminGate.verify(context, password)) onSuccess() else error = "गलत Admin पासवर्ड।"
        }, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text(if (setup) "पासवर्ड बनाएं" else "Admin Panel खोलें", fontWeight = FontWeight.Bold) }
        TextButton(onClick = onCancel) { Text("वापस किसान ऐप में") }
    }
}

private object AdminGate {
    private const val PREF = "bijli_prahari_admin"
    private const val KEY = "password_hash"
    fun isConfigured(context: Context): Boolean = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).contains(KEY)
    fun setPassword(context: Context, password: String) = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY, hash(password)).apply()
    fun verify(context: Context, password: String): Boolean = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, null) == hash(password)
    private fun hash(value: String): String = Base64.encodeToString(MessageDigest.getInstance("SHA-256").digest(value.toByteArray()), Base64.NO_WRAP)
}

@Composable
private fun AdminGoogleLogin(auth: AuthRepository, close: () -> Unit) {
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) { busy = false; error = "Google लॉगिन रद्द किया गया।"; return@rememberLauncherForActivityResult }
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(ApiException::class.java)
            val token = account.idToken
            if (token.isNullOrBlank()) { busy = false; error = "Google ID token नहीं मिला।"; return@rememberLauncherForActivityResult }
            scope.launch { auth.signInWithGoogleIdToken(token).onFailure { error = it.message ?: "Firebase लॉगिन विफल हुआ।" }; busy = false }
        } catch (e: ApiException) { busy = false; error = "Google लॉगिन विफल (code ${e.statusCode})।" }
    }
    Column(Modifier.fillMaxSize().background(Bg).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Security, null, tint = Yellow, modifier = Modifier.size(58.dp))
        Text("Admin / GSS Login", color = White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("पासवर्ड के बाद अधिकृत Google खाते से लॉगिन करें।", color = Muted, fontSize = 12.sp)
        error?.let { Text(it, color = Red, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp)) }
        Spacer(Modifier.height(18.dp))
        Button(onClick = {
            busy = true; error = null
            val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(context.getString(R.string.default_web_client_id)).requestEmail().build()
            launcher.launch(GoogleSignIn.getClient(context, options).signInIntent)
        }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text(if (busy) "कनेक्ट हो रहा है…" else "Google से लॉगिन", fontWeight = FontWeight.Bold) }
        TextButton(onClick = close) { Text("बंद करें") }
    }
}

@Composable private fun AdminTop(role: UserRole, close: () -> Unit) { Row(Modifier.fillMaxWidth().background(Bg).padding(17.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.AdminPanelSettings, null, tint = Yellow, modifier = Modifier.size(36.dp)); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text("Admin Panel", color = White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold); Text(if (role == UserRole.ADMIN) "MAIN ADMIN • FULL CONTROL" else "GSS • LIVE OPERATIONS", color = Muted, fontSize = 10.sp) }; IconButton(onClick = close) { Icon(Icons.Default.Close, "बंद", tint = Muted) } } }

@Composable private fun AdminSettings(role: UserRole, close: () -> Unit) { Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Admin Settings", color = White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold); Text("भूमिका: ${role.name}", color = Muted); Text("यहाँ आगे Main Admin, GSS, किसान, IoT और notification management जोड़ा जाएगा।", color = Muted, fontSize = 13.sp); OutlinedButton(onClick = close, modifier = Modifier.fillMaxWidth()) { Text("किसान ऐप में वापस") } } }

@Composable private fun Dashboard(feeders: List<Feeder>, selected: String?, events: List<PowerEvent>, loading: Boolean, error: String?, choose: (String) -> Unit) { val f = feeders.firstOrNull { it.id == selected } ?: feeders.firstOrNull(); val on = f?.currentState == "ON"; LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(13.dp), contentPadding = PaddingValues(bottom = 25.dp)) { item { Text("GSS कंट्रोल सेंटर", color = White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold); Text("Real-time power monitoring", color = Muted, fontSize = 13.sp) }; error?.let { item { Text(it, color = Red, fontSize = 12.sp) } }; if (loading) item { Text("Live data लोड हो रहा है…", color = Muted) }; if (f == null && !loading) item { Empty("Assigned feeder डेटा नहीं मिला। पहले GSS account को feeder assign करें।") }; if (f != null) { item { Selector(feeders, f, choose) }; item { Card(colors = CardDefaults.cardColors(if (on) Color(0xFF0D2B22) else Color(0xFF33171E)), shape = RoundedCornerShape(26.dp)) { Row(Modifier.padding(23.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(68.dp).background(if (on) Green else Red, CircleShape), Alignment.Center) { Icon(if (on) Icons.Default.CheckCircle else Icons.Default.ErrorOutline, null, tint = White, modifier = Modifier.size(40.dp)) }; Spacer(Modifier.width(16.dp)); Column { Text(if (on) "बिजली चालू" else "बिजली बंद", color = White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold); Text(if (on) "Feeder सामान्य स्थिति में" else "Power outage सक्रिय", color = Muted) } } } }; item { Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) { Metric("गाँव", f.villageCount.toString(), Yellow, Modifier.weight(1f)); Metric("किसान", f.farmerCount.toString(), Green, Modifier.weight(1f)); Metric("EVENTS", events.size.toString(), Blue, Modifier.weight(1f)) } }; item { Text("हाल की गतिविधि", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }; items(events.take(7)) { Event(it) } } } }
@Composable private fun Selector(feeders: List<Feeder>, selected: Feeder, choose: (String) -> Unit) { var open by remember { mutableStateOf(false) }; Card(colors = CardDefaults.cardColors(CardBg), shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth().clickable { open = true }) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("ACTIVE FEEDER", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold); Text(selected.name, color = White, fontWeight = FontWeight.Bold) }; Text(selected.currentState, color = if (selected.currentState == "ON") Green else Red, fontWeight = FontWeight.ExtraBold) } }; DropdownMenu(expanded = open, onDismissRequest = { open = false }) { feeders.forEach { f -> DropdownMenuItem(text = { Text(f.name) }, onClick = { choose(f.id); open = false }) } } }
@Composable private fun Feeders(feeders: List<Feeder>, selected: String?, choose: (String) -> Unit) { LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { item { Text("GSS / Feeders", color = White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold); Text("Assigned feeders", color = Muted) }; items(feeders) { f -> Card(colors = CardDefaults.cardColors(if (f.id == selected) Color(0xFF24223A) else CardBg), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().clickable { choose(f.id) }) { Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(f.name, color = White, fontWeight = FontWeight.Bold); Text("${f.gssName} • ${f.villageCount} गाँव • ${f.farmerCount} किसान", color = Muted, fontSize = 11.sp) }; Text(f.currentState, color = if (f.currentState == "ON") Green else Red, fontWeight = FontWeight.ExtraBold) } } } } }
@Composable private fun History(events: List<PowerEvent>) { LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { item { Text("Power History", color = White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold) }; items(events) { Event(it) } } }
@Composable private fun Event(e: PowerEvent) { val on = e.state == "ON"; Card(colors = CardDefaults.cardColors(CardBg), shape = RoundedCornerShape(17.dp)) { Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Bolt, null, tint = if (on) Green else Red); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(if (on) "बिजली वापस आई" else "बिजली चली गई", color = White, fontWeight = FontWeight.Bold); Text("${dateTime(e.timestamp)} • ${e.source}", color = Muted, fontSize = 11.sp) }; Text(if (e.verified) "सत्यापित" else "Pending", color = if (e.verified) Green else Muted, fontSize = 10.sp) } } }
@Composable private fun Metric(title: String, value: String, tint: Color, modifier: Modifier) { Card(modifier = modifier, colors = CardDefaults.cardColors(CardBg), shape = RoundedCornerShape(17.dp)) { Column(Modifier.padding(15.dp)) { Text(title, color = Muted, fontSize = 10.sp); Text(value, color = tint, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold) } } }
@Composable private fun FarmerMetric(title: String, value: String, tint: Color, modifier: Modifier) { Card(modifier = modifier, colors = CardDefaults.cardColors(CardBg), shape = RoundedCornerShape(17.dp)) { Column(Modifier.padding(15.dp)) { Text(title, color = Muted, fontSize = 10.sp); Text(value, color = tint, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold) } } }
@Composable private fun Empty(text: String) { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(CardBg), shape = RoundedCornerShape(18.dp)) { Text(text, Modifier.padding(18.dp), color = Muted) } }
private fun dateTime(ms: Long): String = SimpleDateFormat("dd MMM, HH:mm", Locale("hi", "IN")).format(Date(ms))
