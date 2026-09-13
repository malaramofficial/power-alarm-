package com.malaramofficial.bijliprahari

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
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
import com.malaramofficial.bijliprahari.data.Feeder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BijliPrahariApp() }
    }
}

private val Navy = Color(0xFF090D18)
private val Card = Color(0xFF151C2E)
private val Yellow = Color(0xFFFFC107)
private val Green = Color(0xFF35C759)
private val Red = Color(0xFFFF4D4F)
private val DemoFeeder = Feeder("meethi-beri", "मीठी बेरी फीडर", "GSS मीठी बेरी", 6, 128, true, "ON")

@Composable
fun BijliPrahariApp() {
    var tab by remember { mutableIntStateOf(0) }
    var powerOn by remember { mutableStateOf(true) }
    var selectedFeeder by remember { mutableStateOf(DemoFeeder) }
    MaterialTheme(colorScheme = darkColorScheme(primary = Yellow, background = Navy, surface = Card)) {
        Scaffold(
            containerColor = Navy,
            bottomBar = { BottomBar(tab) { tab = it } }
        ) { pad ->
            when (tab) {
                0 -> HomeScreen(selectedFeeder, powerOn) { powerOn = !powerOn }
                1 -> FeedersScreen(selectedFeeder) { selectedFeeder = it }
                2 -> HistoryScreen()
                else -> SettingsScreen()
            }
        }
    }
}

@Composable private fun HomeScreen(feeder: Feeder, powerOn: Boolean, toggle: () -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 18.dp).padding(top = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item { Header(feeder) }
        item { StatusCard(powerOn, toggle) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("आज चालू", "07:42", Green, Modifier.weight(1f))
                StatCard("आज बंद", "01:18", Red, Modifier.weight(1f))
                StatCard("गाँव", feeder.villageCount.toString(), Yellow, Modifier.weight(1f))
            }
        }
        item { SectionTitle("हाल की बिजली स्थिति") }
        items(listOf("02:17 PM • बिजली गई • 49 मिनट", "01:28 PM • बिजली आई", "09:42 AM • बिजली गई • 18 मिनट")) { EventRow(it) }
        item { DeviceCard() }
    }
}

@Composable private fun Header(feeder: Feeder) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("बिजली प्रहरी", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text(feeder.gssName + " • GSS Dashboard", color = Color(0xFF9CA3AF), fontSize = 14.sp)
        }
        Icon(Icons.Default.Bolt, null, tint = Yellow, modifier = Modifier.size(34.dp))
    }
}

@Composable private fun StatusCard(on: Boolean, toggle: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = if (on) Color(0xFF123522) else Color(0xFF3A2025)), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(if (on) "बिजली चालू है" else "बिजली बंद है", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text(if (on) "फीडर सामान्य स्थिति में है" else "Outage दर्ज है • GSS कार्रवाई देखें", color = Color(0xFFCBD5E1))
                }
                Box(Modifier.size(56.dp).background(if (on) Green else Red, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Bolt, null, tint = Color.White, modifier = Modifier.size(30.dp))
                }
            }
            HorizontalDivider(color = Color.White.copy(alpha = .12f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("आखिरी अपडेट", color = Color(0xFF9CA3AF)); Text("अभी", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(onClick = toggle, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Yellow)) {
                Text("डेमो में स्थिति बदलें")
            }
        }
    }
}

@Composable private fun StatCard(title: String, value: String, color: Color, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Card), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(13.dp)) { Text(title, color = Color(0xFF9CA3AF), fontSize = 12.sp); Text(value, color = color, fontSize = 19.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable private fun FeedersScreen(selected: Feeder, select: (Feeder) -> Unit) {
    val feeders = listOf(
        DemoFeeder,
        Feeder("demo-2", "बाड़मेर ग्रामीण फीडर", "GSS बाड़मेर", 9, 203, true, "OFF"),
        Feeder("demo-3", "नौखड़ा फीडर", "GSS नौखड़ा", 7, 154, true, "ON")
    )
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("फीडर", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold); Text("GSS को सौंपे गए फीडर", color = Color(0xFF9CA3AF)) }
        items(feeders) { feeder ->
            Card(colors = CardDefaults.cardColors(containerColor = if (feeder.id == selected.id) Color(0xFF26203D) else Card), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(17.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(feeder.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp); Text("${feeder.villageCount} गाँव • ${feeder.farmerCount} किसान", color = Color(0xFF9CA3AF)) }
                    AssistChip(onClick = { select(feeder) }, label = { Text(if (feeder.currentState == "ON") "चालू" else "बंद") }, colors = AssistChipDefaults.assistChipColors(labelColor = if (feeder.currentState == "ON") Green else Red))
                }
            }
        }
    }
}

@Composable private fun HistoryScreen() {
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("बिजली इतिहास", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold); Text("Event source और verification देखें", color = Color(0xFF9CA3AF)) }
        items(listOf("OFF • 02:17 PM • device • सत्यापन बाकी", "ON • 01:28 PM • device • verified", "OFF • 09:42 AM • GSS • verified", "ON • 09:24 AM • device • verified")) { EventRow(it) }
    }
}

@Composable private fun SettingsScreen() {
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("सेटिंग", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        SettingCard("GSS भूमिका", "GSS • केवल assigned feeders")
        SettingCard("Notification", "FCM • बिजली ON/OFF alerts")
        SettingCard("सुरक्षा", "Firebase rules enabled")
        SettingCard("Version", "1.0.0 • Development")
    }
}

@Composable private fun SettingCard(title: String, value: String) { Card(colors = CardDefaults.cardColors(containerColor = Card), shape = RoundedCornerShape(18.dp)) { Column(Modifier.fillMaxWidth().padding(17.dp)) { Text(title, color = Color.White, fontWeight = FontWeight.Bold); Text(value, color = Color(0xFF9CA3AF)) } } }
@Composable private fun SectionTitle(text: String) { Text(text, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold) }
@Composable private fun EventRow(text: String) { Card(colors = CardDefaults.cardColors(containerColor = Card), shape = RoundedCornerShape(15.dp)) { Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.History, null, tint = Yellow); Spacer(Modifier.width(12.dp)); Text(text, color = Color(0xFFE5E7EB), fontSize = 14.sp) } } }
@Composable private fun DeviceCard() { Card(colors = CardDefaults.cardColors(containerColor = Card), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("GSS मॉनिटर डिवाइस", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp); Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Wifi, null, tint = Green); Spacer(Modifier.width(8.dp)); Text("ऑनलाइन • 4G • सिग्नल अच्छा • बैटरी 92%", color = Color(0xFFCBD5E1)) } } } }

@Composable private fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    NavigationBar(containerColor = Card) {
        NavigationBarItem(selected == 0, { onSelect(0) }, icon = { Icon(Icons.Default.Bolt, null) }, label = { Text("डैशबोर्ड") })
        NavigationBarItem(selected == 1, { onSelect(1) }, icon = { Icon(Icons.Default.Wifi, null) }, label = { Text("फीडर") })
        NavigationBarItem(selected == 2, { onSelect(2) }, icon = { Icon(Icons.Default.History, null) }, label = { Text("इतिहास") })
        NavigationBarItem(selected == 3, { onSelect(3) }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("सेटिंग") })
    }
}
