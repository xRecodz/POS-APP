package com.poshan.myapplication

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    cart: List<CartItem>,
    goToPos: () -> Unit,
    goToTransaction: () -> Unit,
    goToHistory: () -> Unit,
    goToLog: () -> Unit,
    printerManager: BluetoothPrinterManager,
    user: User, // Tambahkan parameter user
    onResumeTransaction: (TransactionEntity) -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    
    // Use collectAsState for flow
    val transactions by db.transactionDao().getAllTransactionsFlow().collectAsState(initial = emptyList())
    
    val softBackground = Color(0xFFF8F9FA)
    val darkRed = Color(0xFFC62828)
    val mutedPrimary = Color(0xFF455A64)

    val currentDate = remember { SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date()) }

    // Optimization: Pre-calculate filtered and reversed list
    val todayTransactions = remember(transactions) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfToday = calendar.timeInMillis

        transactions.filter { it.timestamp >= startOfToday }
    }

    var showVoidDialog by remember { mutableStateOf<TransactionEntity?>(null) }
    var voidReason by remember { mutableStateOf("") }

    Scaffold(
        containerColor = softBackground,
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Riwayat Hari Ini", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                        Text(currentDate, style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White)
            )
        },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp, top = 8.dp), contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.width(360.dp).height(60.dp),
                    color = Color.Black,
                    shape = CircleShape,
                    shadowElevation = 8.dp
                ) {
                    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        FloatingNavItem(Icons.Default.RestaurantMenu, "Menu", false, goToPos)
                        FloatingNavItem(Icons.AutoMirrored.Filled.ReceiptLong, "Nota", false, goToTransaction, cart.sumOf { it.qty })
                        FloatingNavItem(Icons.Default.History, "History", true, goToHistory)
                        if (user.role in listOf("OWNER", "ADMIN", "HEAD")) {
                            FloatingNavItem(Icons.AutoMirrored.Filled.ListAlt, "Log", false, goToLog)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)
        ) {
            if (todayTransactions.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("Belum ada transaksi hari ini", color = Color.Gray)
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Total Pesanan:", fontWeight = FontWeight.Bold, color = mutedPrimary)
                        Surface(color = darkRed.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                            Text(
                                text = "${todayTransactions.size} Transaksi",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                color = darkRed,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(todayTransactions, key = { it.id }) { trx ->
                        TransactionItem(
                            trx = trx,
                            onPrint = {
                                scope.launch {
                                    val device = printerManager.getSavedDevice()
                                    if (device != null) {
                                        withContext(Dispatchers.IO) {
                                            printerManager.connectToDevice(device) { success ->
                                                if (success) {
                                                    printHistoryReceipt(printerManager, trx)
                                                    scope.launch { delay(1000); printerManager.disconnect() }
                                                } else {
                                                    scope.launch { Toast.makeText(context, "Printer sibuk", Toast.LENGTH_SHORT).show() }
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            onResume = { 
                                if (trx.status == "OPEN" || user.role != "KASIR") {
                                    onResumeTransaction(trx) 
                                } else {
                                    Toast.makeText(context, "Kasir tidak boleh edit nota LUNAS", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onVoid = {
                                if (user.role != "KASIR") {
                                    showVoidDialog = trx
                                } else {
                                    Toast.makeText(context, "Kasir tidak boleh VOID nota", Toast.LENGTH_SHORT).show()
                                }
                            },
                            darkRed = darkRed,
                            mutedPrimary = mutedPrimary,
                            softBackground = softBackground
                        )
                    }
                }
            }
        }
    }

    if (showVoidDialog != null) {
        AlertDialog(
            onDismissRequest = { showVoidDialog = null; voidReason = "" },
            title = { Text("Batalkan Nota #${showVoidDialog!!.id}") },
            text = {
                Column {
                    Text("Alasan Pembatalan:")
                    OutlinedTextField(
                        value = voidReason,
                        onValueChange = { voidReason = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updatedTrx = showVoidDialog!!.copy(
                            status = "VOID",
                            updatedBy = user.username,
                            voidReason = voidReason
                        )
                        scope.launch(Dispatchers.IO) {
                            db.transactionDao().update(updatedTrx)
                        }
                        showVoidDialog = null
                        voidReason = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = darkRed)
                ) {
                    Text("VOID")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVoidDialog = null; voidReason = "" }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun TransactionItem(
    trx: TransactionEntity,
    onPrint: () -> Unit,
    onResume: () -> Unit,
    onVoid: () -> Unit,
    darkRed: Color,
    mutedPrimary: Color,
    softBackground: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onResume() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        border = if (trx.status == "OPEN") BorderStroke(1.dp, Color(0xFFFFCDD2)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text("Nota #${trx.id}", fontWeight = FontWeight.Bold, color = Color.Black)
                    Surface(
                        color = when (trx.status) {
                            "OPEN" -> Color(0xFFFFEBEE)
                            "VOID" -> Color.Gray.copy(alpha = 0.1f)
                            else -> Color(0xFFE8F5E9)
                        },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = when (trx.status) {
                                "OPEN" -> "BELUM BAYAR"
                                "VOID" -> "VOID"
                                else -> "LUNAS"
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = when (trx.status) {
                                "OPEN" -> Color.Red
                                "VOID" -> Color.Gray
                                else -> Color(0xFF4CAF50)
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Row {
                    if (trx.status != "VOID") {
                        IconButton(
                            onClick = onVoid,
                            modifier = Modifier.background(softBackground, CircleShape).size(36.dp)
                        ) {
                            Icon(Icons.Default.History, null, modifier = Modifier.size(18.dp), tint = darkRed)
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    IconButton(
                        onClick = onPrint,
                        modifier = Modifier.background(softBackground, CircleShape).size(36.dp)
                    ) {
                        Icon(Icons.Default.Print, null, modifier = Modifier.size(18.dp), tint = mutedPrimary)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color(0xFFF1F3F5), shape = RoundedCornerShape(4.dp)) {
                    Text("Meja ${trx.tableNumber}", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(trx.customerName.ifEmpty { "Umum" }, style = MaterialTheme.typography.bodySmall, color = Color.Black, fontWeight = FontWeight.Medium)
                    if (trx.customerPhone.isNotEmpty()) {
                        Text(trx.customerPhone, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = Color(0xFFF1F3F5))
            
            Column {
                Text("Oleh: ${trx.createdBy}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                if (trx.status == "VOID") {
                    Text("Alasan: ${trx.voidReason ?: "-"}", style = MaterialTheme.typography.labelSmall, color = darkRed)
                }
                Spacer(Modifier.height(4.dp))
                Text("Promo/Diskon: ${trx.discount}", style = MaterialTheme.typography.labelSmall, color = darkRed, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(trx.items, style = MaterialTheme.typography.bodySmall, color = Color.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(trx.paymentMethod, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text("Total: Rp ${trx.total.formatThousand()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = if(trx.status == "VOID") Color.Gray else darkRed)
            }
        }
    }
}

fun printHistoryReceipt(printerManager: BluetoothPrinterManager, trx: TransactionEntity) {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) // Menghapus jam
    val dateStr = sdf.format(Date(trx.timestamp))

    printerManager.resetPrinter()
    printerManager.centerAlign()
    printerManager.printText("Restoran Keluarga\n")
    if (trx.status == "VOID") printerManager.printText("*** VOID ***\n")
    printerManager.leftAlign()
    printerManager.printText("Tanggal: $dateStr\n")
    printerManager.printText("Kasir: ${trx.createdBy}\n")
    printerManager.printText("--------------------------------\n")
    printerManager.printText("Meja: ${trx.tableNumber}\n")
    printerManager.printText("Pelanggan: ${trx.customerName.ifEmpty { "-" }}\n")
    printerManager.printText("--------------------------------\n")
    
    trx.items.split(", ").forEach { item ->
        printerManager.printText("$item\n")
    }
    
    printerManager.printText("--------------------------------\n")
    printerManager.printText("Total: Rp ${trx.total.formatThousand()}\n")
    printerManager.printText("Diskon: ${trx.discount}\n")
    printerManager.printText("Metode: ${trx.paymentMethod}\n")
    printerManager.printText("--------------------------------\n")
    printerManager.centerAlign()
    printerManager.printText("Terima Kasih\n")
    printerManager.printText("Selamat Menikmati!\n")
    printerManager.printNewLine()
    printerManager.printNewLine()
    printerManager.printNewLine()
}
