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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BijliPrahariApp() }
    }
}

private val Navy = Color(0xFF0B1020)
private val Card = Color(0xFF151D32)
private val Yellow = Color(0xFFFFC107)
private val Green = Color(0xFF35C759)
private val Red = Color(0xFFFF4D4F)

@Composable
fun BijliPrahariApp() {
    var powerOn by remember { mutableStateOf(true) }
    MaterialTheme(colorScheme = darkColorScheme(primary = Yellow, background = Navy, surface = Card)) {
        Scaffold(containerColor = Navy, bottomBar = { BottomBar() }) { pad ->
            LazyColumn(modifier = Modifier.fillMaxSize().padding(pad).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(vertical = 18.dp)) {
                item { Header() }
                item { StatusCard(powerOn) { powerOn = !powerOn } }
                item { TodaySummary() }
                item { SectionTitle("हाल की बिजली स्थिति") }
                items(listOf("02:17 PM  •  बिजली गई  •  49 मिनट", "01:28 PM  •  बिजली आई", "09:42 AM  •  बिजली गई  •  18 मिनट")) { EventRow(it) }
                item { DeviceCard() }
            }
        }
    }
}

@Composable private fun Header() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column { Text("बिजली प्रहरी", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold); Text("मीठी बेरी • खेत की लाइन", color = Color(0xFF9CA3AF), fontSize = 14.sp) }
        Icon(Icons.Default.Bolt, null, tint = Yellow, modifier = Modifier.size(34.dp))
    }
}

@Composable private fun StatusCard(on: Boolean, toggle: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = if (on) Color(0xFF123522) else Color(0xFF3A2025)), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text(if (on) "बिजली चालू है" else "बिजली बंद है", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold); Text(if (on) "लाइन अभी उपलब्ध है" else "लाइन बंद होने की सूचना भेजी गई", color = Color(0xFFCBD5E1)) }
                Box(Modifier.size(56.dp).background(if (on) Green else Red, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Bolt, null, tint = Color.White, modifier = Modifier.size(30.dp)) }
            }
            HorizontalDivider(color = Color.White.copy(alpha = .12f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("आखिरी अपडेट", color = Color(0xFF9CA3AF)); Text("अभी", color = Color.White, fontWeight = FontWeight.SemiBold) }
            OutlinedButton(onClick = toggle, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Yellow)) { Text("डेमो में स्थिति बदलें") }
        }
    }
}

@Composable private fun TodaySummary() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard("आज चालू", "07:42", Green, Modifier.weight(1f))
        StatCard("आज बंद", "01:18", Red, Modifier.weight(1f))
    }
}

@Composable private fun StatCard(title: String, value: String, color: Color, modifier: Modifier) { Card(modifier, colors = CardDefaults.cardColors(containerColor = Card), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp)) { Text(title, color = Color(0xFF9CA3AF), fontSize = 13.sp); Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold) } } }

@Composable private fun SectionTitle(text: String) { Text(text, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold) }

@Composable private fun EventRow(text: String) { Card(colors = CardDefaults.cardColors(containerColor = Card), shape = RoundedCornerShape(15.dp)) { Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.History, null, tint = Yellow); Spacer(Modifier.width(12.dp)); Text(text, color = Color(0xFFE5E7EB), fontSize = 14.sp) } } }

@Composable private fun DeviceCard() { Card(colors = CardDefaults.cardColors(containerColor = Card), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("मॉनिटर डिवाइस", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp); Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Wifi, null, tint = Green); Spacer(Modifier.width(8.dp)); Text("ऑनलाइन • 4G • सिग्नल अच्छा", color = Color(0xFFCBD5E1)) } } } }

@Composable private fun BottomBar() { NavigationBar(containerColor = Card) { NavigationBarItem(true, {}, icon = { Icon(Icons.Default.Bolt, null) }, label = { Text("होम") }); NavigationBarItem(false, {}, icon = { Icon(Icons.Default.NotificationsActive, null) }, label = { Text("सूचना") }); NavigationBarItem(false, {}, icon = { Icon(Icons.Default.History, null) }, label = { Text("इतिहास") }); NavigationBarItem(false, {}, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("सेटिंग") }) } }
