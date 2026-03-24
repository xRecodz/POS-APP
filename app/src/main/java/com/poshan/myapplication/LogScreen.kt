package com.poshan.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    cart: List<CartItem>,
    goToPos: () -> Unit,
    goToTransaction: () -> Unit,
    goToHistory: () -> Unit,
    goToLog: () -> Unit,
    db: AppDatabase
) {
    val logs by db.transactionDao().getAllActivityLogsFlow().collectAsState(initial = emptyList())
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity Log", fontWeight = FontWeight.ExtraBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black, titleContentColor = Color.White)
            )
        },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(bottom = 12.dp, top = 8.dp), contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.width(360.dp).height(60.dp),
                    color = Color.Black,
                    shape = CircleShape,
                    shadowElevation = 8.dp
                ) {
                    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        FloatingNavItem(Icons.Default.RestaurantMenu, "Menu", false, goToPos)
                        FloatingNavItem(Icons.AutoMirrored.Filled.ReceiptLong, "Nota", false, goToTransaction, cart.sumOf { it.qty })
                        FloatingNavItem(Icons.Default.History, "History", false, goToHistory)
                        FloatingNavItem(Icons.Default.ListAlt, "Log", true, goToLog)
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(logs) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(log.user, fontWeight = FontWeight.Bold, color = Color(0xFFC62828), fontSize = 12.sp)
                            Text(sdf.format(Date(log.timestamp)), fontSize = 10.sp, color = Color.Gray)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(log.action, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        Text(log.details, fontSize = 12.sp, color = Color.DarkGray)
                    }
                }
            }
        }
    }
}
