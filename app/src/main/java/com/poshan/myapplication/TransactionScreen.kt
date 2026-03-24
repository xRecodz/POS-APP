package com.poshan.myapplication

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(
    cart: List<CartItem>,
    onCartChange: (List<CartItem>) -> Unit,
    goToPos: () -> Unit,
    goToTransaction: () -> Unit,
    goToHistory: () -> Unit,
    goToLog: () -> Unit,
    onTransactionSuccess: () -> Unit,
    printerManager: BluetoothPrinterManager,
    user: User, // Tambahkan parameter user
    editingTransactionId: Int? = null,
    initialTableNumber: String = "",
    initialCustomerName: String = "",
    initialCustomerPhone: String = "",
    initialDiscount: String = "No Diskon"
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    val mutedPrimary = Color(0xFF455A64) 
    val softBackground = Color(0xFFF8F9FA)
    val darkRed = Color(0xFFC62828)
    val successGreen = Color(0xFF4CAF50)

    var selectedDiscount by remember { mutableStateOf(initialDiscount) }
    val discountOptions = listOf("No Diskon", "Promo", "Harga Karyawan", "Tuslah", "GoFood", "Shopee")
    var discountExpanded by remember { mutableStateOf(false) }

    var paymentMethod by remember { mutableStateOf("Cash") }
    val paymentOptions = listOf("Cash", "QRIS", "Transfer")

    var selectedBank by remember { mutableStateOf("Mandiri") }
    val bankOptions = listOf("Mandiri", "BCA", "BSI")
    var bankExpanded by remember { mutableStateOf(false) }

    var tableNumber by remember { mutableStateOf(initialTableNumber) }
    var customerName by remember { mutableStateOf(initialCustomerName) }
    var customerPhone by remember { mutableStateOf(initialCustomerPhone) }
    var cashAmount by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    LaunchedEffect(initialTableNumber, initialCustomerName, initialCustomerPhone, initialDiscount) {
        tableNumber = initialTableNumber
        customerName = initialCustomerName
        customerPhone = initialCustomerPhone
        selectedDiscount = initialDiscount
    }

    val total = remember(cart, selectedDiscount) {
        cart.sumOf { item ->
            val basePrice = item.product.price
            val adjustedPrice = when (selectedDiscount) {
                "Promo" -> when (item.product.name) {
                    "Bebek 1/4" -> 19000
                    "Bebek Ingkung" -> 79000
                    else -> basePrice
                }
                "Harga Karyawan", "Karyawan" -> (basePrice * 0.85).toInt()
                "Tuslah" -> (basePrice * 1.15).toInt()
                "GoFood" -> (basePrice * 1.3).toInt()
                "Shopee" -> (basePrice * 1.25).toInt()
                else -> basePrice
            }
            adjustedPrice * item.qty
        }
    }

    val cashPaid = cashAmount.toIntOrNull() ?: 0
    val diff = cashPaid - total

    Scaffold(
        containerColor = softBackground,
        topBar = {
            TopAppBar(
                title = { Text(if (editingTransactionId != null) "Edit Nota #$editingTransactionId" else "Checkout", fontWeight = FontWeight.ExtraBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black, titleContentColor = Color.White)
            )
        },
        bottomBar = {
            Column(modifier = Modifier.background(Color.Transparent)) {
                if (cart.isNotEmpty()) {
                    Surface(color = Color.White, shadowElevation = 16.dp, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("Total Pembayaran", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text("Rp ${total.formatThousand()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = darkRed)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            val paymentDetail = if (paymentMethod == "Transfer") "Transfer ($selectedBank)" else paymentMethod
                                            saveOrUpdateTransaction(db, cart, selectedDiscount, tableNumber, customerName, customerPhone, paymentDetail, "OPEN", user.username, editingTransactionId)
                                            onTransactionSuccess()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(40.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0), contentColor = Color.Black)
                                    ) {
                                        Text("Simpan", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Button(
                                        onClick = {
                                            val paymentDetail = if (paymentMethod == "Transfer") "Transfer ($selectedBank)" else paymentMethod
                                            saveOrUpdateTransaction(db, cart, selectedDiscount, tableNumber, customerName, customerPhone, paymentDetail, "COMPLETED", user.username, editingTransactionId)
                                            scope.launch {
                                                val device = printerManager.getSavedDevice()
                                                if (device != null) {
                                                    withContext(Dispatchers.IO) {
                                                        printerManager.connectToDevice(device) { success ->
                                                            if (success) {
                                                                printReceipt(printerManager, cart, total, selectedDiscount, tableNumber, customerName, customerPhone, paymentDetail, cashPaid, diff, user.username)
                                                                scope.launch { delay(1000); printerManager.disconnect() }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            onTransactionSuccess()
                                        },
                                        enabled = paymentMethod != "Cash" || cashPaid >= total,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(40.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = successGreen)
                                    ) {
                                        Text("Bayar + Print", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                
                // LUXURY FLOATING NAVBAR
                Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(bottom = 12.dp, top = 8.dp), contentAlignment = Alignment.Center) {
                    Surface(
                        modifier = Modifier.width(360.dp).height(60.dp),
                        color = Color.Black,
                        shape = CircleShape,
                        shadowElevation = 8.dp
                    ) {
                        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                            FloatingNavItem(Icons.Default.RestaurantMenu, "Menu", false, goToPos)
                            FloatingNavItem(Icons.AutoMirrored.Filled.ReceiptLong, "Nota", true, goToTransaction, cart.sumOf { it.qty })
                            FloatingNavItem(Icons.Default.History, "History", false, goToHistory)
                            if (user.role in listOf("OWNER", "ADMIN", "HEAD")) {
                                FloatingNavItem(Icons.AutoMirrored.Filled.ListAlt, "Log", false, goToLog)
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(scrollState).padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Form Section - Smaller
            Text("Pelanggan", fontWeight = FontWeight.Bold, color = mutedPrimary, fontSize = 12.sp)
            Surface(color = Color.White, shape = RoundedCornerShape(12.dp), shadowElevation = 1.dp) {
                Column(modifier = Modifier.padding(10.dp)) {
                    SmallInput(tableNumber, { tableNumber = it }, "No. Meja")
                    Spacer(Modifier.height(8.dp))
                    SmallInput(customerName, { customerName = it }, "Nama Pelanggan")
                    Spacer(Modifier.height(8.dp))
                    SmallInput(customerPhone, { if (it.all { c -> c.isDigit() }) customerPhone = it }, "Nomor HP", KeyboardType.Phone)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Payment Section - Smaller
            Text("Pembayaran", fontWeight = FontWeight.Bold, color = mutedPrimary, fontSize = 12.sp)
            Surface(color = Color.White, shape = RoundedCornerShape(12.dp), shadowElevation = 1.dp) {
                Column(modifier = Modifier.padding(10.dp)) {
                    ExposedDropdownMenuBox(expanded = discountExpanded, onExpandedChange = { discountExpanded = it }) {
                        OutlinedTextField(
                            value = selectedDiscount, onValueChange = {}, readOnly = true, label = { Text("Tipe Harga", fontSize = 11.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = discountExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable).heightIn(min = 44.dp),
                            shape = RoundedCornerShape(8.dp), textStyle = TextStyle(color = Color.Black, fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = darkRed, unfocusedBorderColor = Color.LightGray, unfocusedLabelColor = Color.Black)
                        )
                        ExposedDropdownMenu(expanded = discountExpanded, onDismissRequest = { discountExpanded = false }, modifier = Modifier.background(Color.White)) {
                            discountOptions.forEach { option ->
                                DropdownMenuItem(text = { Text(option, color = Color.Black) }, onClick = { selectedDiscount = option; discountExpanded = false })
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        paymentOptions.forEach { option ->
                            val selected = paymentMethod.startsWith(option)
                            FilterChip(
                                selected = selected,
                                onClick = { paymentMethod = option },
                                label = { Text(option, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = darkRed, selectedLabelColor = Color.White),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (paymentMethod == "Transfer") {
                        Spacer(Modifier.height(8.dp))
                        ExposedDropdownMenuBox(expanded = bankExpanded, onExpandedChange = { bankExpanded = it }) {
                            OutlinedTextField(
                                value = selectedBank, onValueChange = {}, readOnly = true, label = { Text("Pilih Bank", fontSize = 11.sp, color = Color.Black) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bankExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable).heightIn(min = 44.dp),
                                shape = RoundedCornerShape(8.dp), textStyle = TextStyle(color = Color.Black, fontSize = 13.sp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = darkRed, unfocusedBorderColor = Color.LightGray, unfocusedLabelColor = Color.Black)
                            )
                            ExposedDropdownMenu(expanded = bankExpanded, onDismissRequest = { bankExpanded = false }, modifier = Modifier.background(Color.White)) {
                                bankOptions.forEach { bank ->
                                    DropdownMenuItem(text = { Text(bank, color = Color.Black) }, onClick = { selectedBank = bank; bankExpanded = false })
                                }
                            }
                        }
                    }

                    if (paymentMethod == "Cash") {
                        Spacer(Modifier.height(8.dp))
                        SmallInput(
                            value = cashAmount, 
                            onValueChange = { 
                                if (it.all { c -> c.isDigit() }) {
                                    cashAmount = it
                                    // Auto scroll to bottom when typing cash amount
                                    scope.launch {
                                        delay(100)
                                        scrollState.animateScrollTo(scrollState.maxValue)
                                    }
                                }
                            }, 
                            label = "Uang Diterima", 
                            keyboardType = KeyboardType.Number
                        )
                        if (cashPaid > 0) {
                            Text("Kembalian: Rp ${diff.formatThousand()}", fontWeight = FontWeight.Bold, color = if (diff >= 0) successGreen else darkRed, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Cart Items Section - Smaller
            Text("Ringkasan Pesanan", fontWeight = FontWeight.Bold, color = mutedPrimary, fontSize = 12.sp)
            Surface(color = Color.White, shape = RoundedCornerShape(12.dp), shadowElevation = 1.dp) {
                Column(modifier = Modifier.padding(10.dp)) {
                    cart.forEach { item ->
                        val basePrice = item.product.price
                        val adjustedPrice = when (selectedDiscount) {
                            "Promo" -> when (item.product.name) {
                                "Bebek 1/4" -> 19000
                                "Bebek Ingkung" -> 79000
                                else -> basePrice
                            }
                            "Harga Karyawan", "Karyawan" -> (basePrice * 0.85).toInt()
                            "Tuslah" -> (basePrice * 1.15).toInt()
                            "GoFood" -> (basePrice * 1.3).toInt()
                            "Shopee" -> (basePrice * 1.25).toInt()
                            else -> basePrice
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.product.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("${item.qty} x Rp ${adjustedPrice.formatThousand()}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = {
                                    val newCart = cart.toMutableList()
                                    if (item.qty > 1) {
                                        newCart[cart.indexOf(item)] = item.copy(qty = item.qty - 1)
                                    } else {
                                        newCart.remove(item)
                                    }
                                    onCartChange(newCart)
                                }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Remove, contentDescription = null, tint = darkRed, modifier = Modifier.size(18.dp))
                                }
                                Text(item.qty.toString(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                IconButton(onClick = {
                                    val newCart = cart.toMutableList()
                                    newCart[cart.indexOf(item)] = item.copy(qty = item.qty + 1)
                                    onCartChange(newCart)
                                }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = successGreen, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                    }
                }
            }
            Spacer(Modifier.height(100.dp)) 
        }
    }
}

@Composable
fun SmallInput(value: String, onValueChange: (String) -> Unit, label: String, keyboardType: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 11.sp, color = Color.Black) },
        modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
        shape = RoundedCornerShape(8.dp),
        singleLine = true,
        textStyle = TextStyle(color = Color.Black, fontSize = 13.sp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFC62828), unfocusedBorderColor = Color.LightGray, unfocusedLabelColor = Color.Black)
    )
}

fun printReceipt(printerManager: BluetoothPrinterManager, cart: List<CartItem>, total: Int, discount: String, table: String, name: String, phone: String, payment: String, cashPaid: Int, change: Int, cashierName: String) {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) // Menghapus jam
    val currentDate = sdf.format(Date())
    printerManager.resetPrinter()
    printerManager.centerAlign()
    printerManager.printText("Bebek Rakyat xxx\n")
    printerManager.leftAlign()
    printerManager.printText("Tanggal: $currentDate\n")
    printerManager.printText("Kasir  : $cashierName\n")
    printerManager.printText("--------------------------------\n")
    printerManager.printText("Meja: $table\n")
    printerManager.printText("Pelanggan: ${name.ifEmpty { "-" }}\n")
    if (phone.isNotEmpty()) {
        printerManager.printText("No. HP: $phone\n")
    }
    printerManager.printText("--------------------------------\n")
    
    var totalNormalPrice = 0
    cart.forEach { item ->
        val basePrice = item.product.price
        val finalPrice = when (discount) {
            "Promo" -> when (item.product.name) {
                "Bebek 1/4" -> 19000
                "Bebek Ingkung" -> 79000
                else -> basePrice
            }
            "Harga Karyawan", "Karyawan" -> (basePrice * 0.85).toInt()
            "Tuslah" -> (basePrice * 1.15).toInt()
            "GoFood" -> (basePrice * 1.3).toInt()
            "Shopee" -> (basePrice * 1.25).toInt()
            else -> basePrice
        }
        totalNormalPrice += (basePrice * item.qty)
        printerManager.printText("${item.product.name}\n")
        printerManager.printText("${item.qty} x ${finalPrice.formatThousand()} = ${(item.qty * finalPrice).formatThousand()}\n")
    }
    
    val totalDiscountAmount = totalNormalPrice - total
    printerManager.printText("--------------------------------\n")
    printerManager.printText("Total      : Rp ${total.formatThousand()}\n")
    if (totalDiscountAmount > 0) {
        printerManager.printText("Total Diskon: Rp ${totalDiscountAmount.formatThousand()}\n")
    }
    printerManager.printText("Metode     : $payment\n")
    if (payment.startsWith("Cash")) {
        printerManager.printText("Bayar Tunai: Rp ${cashPaid.formatThousand()}\n")
        printerManager.printText("Kembalian  : Rp ${change.formatThousand()}\n")
    }
    printerManager.printText("--------------------------------\n")
    printerManager.centerAlign()
    printerManager.printText("Terima Kasih!\n")
    printerManager.printText("Untuk Reservasi dan Nasi Box\n")
    printerManager.printText("Hub: Whatsapp 0881 0101 0101\n")
    printerManager.printNewLine(); printerManager.printNewLine(); printerManager.printNewLine()
}

fun saveOrUpdateTransaction(db: AppDatabase, cart: List<CartItem>, discount: String, table: String, name: String, phone: String, payment: String, status: String, username: String, id: Int? = null) {
    if (cart.isEmpty()) return
    val total = cart.sumOf { item ->
        val basePrice = item.product.price
        val adjustedPrice = when (discount) {
            "Promo" -> when (item.product.name) {
                "Bebek 1/4" -> 19000
                "Bebek Ingkung" -> 79000
                else -> basePrice
            }
            "Harga Karyawan", "Karyawan" -> (basePrice * 0.85).toInt()
            "Tuslah" -> (basePrice * 1.15).toInt()
            "GoFood" -> (basePrice * 1.3).toInt()
            "Shopee" -> (basePrice * 1.25).toInt()
            else -> basePrice
        }
        adjustedPrice * item.qty
    }
    val transaction = TransactionEntity(
        id = id ?: 0, 
        items = cart.joinToString { "${it.product.name} x${it.qty}" }, 
        total = total, 
        discount = discount, 
        tableNumber = table, 
        customerName = name, 
        customerPhone = phone, 
        paymentMethod = payment, 
        status = status,
        createdBy = username,
        updatedBy = if (id != null) username else null
    )
    CoroutineScope(Dispatchers.IO).launch { db.transactionDao().insert(transaction) }
}
