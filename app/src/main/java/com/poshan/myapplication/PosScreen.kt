package com.poshan.myapplication

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.PrintDisabled
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    cart: List<CartItem>,
    productList: List<Product>,
    user: User,
    printerManager: BluetoothPrinterManager,
    onCartChange: (List<CartItem>) -> Unit,
    goToPos: () -> Unit,
    goToTransaction: () -> Unit,
    goToHistory: () -> Unit,
    goToManageProducts: () -> Unit,
    goToLog: () -> Unit
) {
    val context = LocalContext.current
    var search by remember { mutableStateOf("") }
    var showPrinterDialog by remember { mutableStateOf(false) }
    var pairedDevices by remember { mutableStateOf(emptyList<BluetoothDevice>()) }
    
    val darkRed = Color(0xFFC62828)
    val mutedPrimary = Color(0xFF455A64)
    val softBackground = Color(0xFFF8F9FA)

    val filteredProducts = remember(search, productList) {
        productList.filter { it.name.contains(search, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(user.outletName, fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 18.sp)
                        Text("Kasir: ${user.username} (${user.role})", color = Color.LightGray, fontSize = 12.sp)
                    }
                },
                actions = {
                    if (user.role in listOf("OWNER", "ADMIN", "HEAD")) {
                        IconButton(onClick = goToManageProducts) {
                            Icon(Icons.Default.Inventory, contentDescription = null, tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White
                )
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
                        FloatingNavItem(Icons.Default.RestaurantMenu, "Menu", true, goToPos)
                        FloatingNavItem(Icons.AutoMirrored.Filled.ReceiptLong, "Nota", false, goToTransaction, cart.sumOf { it.qty })
                        FloatingNavItem(Icons.Default.History, "History", false, goToHistory)
                        if (user.role in listOf("OWNER", "ADMIN", "HEAD")) {
                            FloatingNavItem(Icons.AutoMirrored.Filled.ListAlt, "Log", false, goToLog)
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (cart.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = goToTransaction,
                    icon = { Icon(Icons.Default.ShoppingCart, null) },
                    text = { Text("Checkout (${cart.sumOf { it.qty }})") },
                    containerColor = darkRed,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(softBackground)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            val savedDevice = remember { printerManager.getSavedDevice() }
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(10.dp).clickable { 
                        pairedDevices = printerManager.getPairedDevices()
                        showPrinterDialog = true
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (savedDevice != null) Icons.Default.Print else Icons.Default.PrintDisabled,
                        contentDescription = null,
                        tint = if (savedDevice != null) Color(0xFF4CAF50) else Color(0xFFE57373),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (savedDevice != null) "Printer: ${savedDevice.name}" else "Printer Belum Terpasang",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF495057),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (showPrinterDialog.not()) {
                        Icon(Icons.Default.Bluetooth, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    }
                }
            }

            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Cari produk...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = mutedPrimary, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                textStyle = TextStyle(fontSize = 14.sp, color = Color.Black),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    focusedBorderColor = darkRed,
                    focusedLabelColor = darkRed
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredProducts, key = { it.id }) { product ->
                    val cartItem = cart.find { it.product.id == product.id }
                    ProductRowItem(
                        product = product,
                        qty = cartItem?.qty ?: 0,
                        accentColor = darkRed,
                        onAdd = {
                            if (cartItem != null) {
                                onCartChange(cart.map {
                                    if (it.product.id == product.id) it.copy(qty = it.qty + 1)
                                    else it
                                })
                            } else {
                                onCartChange(cart + CartItem(product, 1))
                            }
                        },
                        onRemove = {
                            if (cartItem != null) {
                                if (cartItem.qty > 1) {
                                    onCartChange(cart.map {
                                        if (it.product.id == product.id) it.copy(qty = it.qty - 1)
                                        else it
                                    })
                                } else {
                                    onCartChange(cart.filter { it.product.id != product.id })
                                }
                            }
                        }
                    )
                }
            }
        }
        
        if (showPrinterDialog) {
            val currentSavedDevice = printerManager.getSavedDevice()
            AlertDialog(
                onDismissRequest = { showPrinterDialog = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
                modifier = Modifier.fillMaxWidth(0.9f),
                confirmButton = {},
                containerColor = Color.White,
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pilih Printer", fontWeight = FontWeight.ExtraBold, color = Color.Black)
                        IconButton(onClick = { pairedDevices = printerManager.getPairedDevices() }) {
                            Icon(Icons.Default.Refresh, "Refresh", tint = darkRed)
                        }
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Pastikan printer sudah dipairing di pengaturan Bluetooth HP Anda.",
                            fontSize = 12.sp,
                            color = Color.DarkGray,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        if (pairedDevices.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(150.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Tidak ada perangkat Bluetooth ditemukan.", textAlign = TextAlign.Center, color = Color.Black)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 300.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(pairedDevices) { device ->
                                    val isSelected = currentSavedDevice?.address == device.address
                                    Surface(
                                        onClick = {
                                            printerManager.savePrinterAddress(device.address)
                                            Toast.makeText(context, "Printer ${device.name} Terpilih", Toast.LENGTH_SHORT).show()
                                            showPrinterDialog = false
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) Color(0xFFFFEBEE) else Color(0xFFF5F5F5),
                                        border = if (isSelected) BorderStroke(1.dp, darkRed) else null,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Print,
                                                null,
                                                tint = if (isSelected) darkRed else Color.Gray
                                            )
                                            Spacer(Modifier.width(16.dp))
                                            Column {
                                                Text(
                                                    device.name ?: "Unknown Device",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = Color.Black
                                                )
                                                Text(device.address, fontSize = 11.sp, color = Color.Gray)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        
                        Button(
                            onClick = { showPrinterDialog = false },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = darkRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Tutup", color = Color.White)
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ProductRowItem(
    product: Product,
    qty: Int,
    accentColor: Color,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().height(72.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(0xFF212529),
                    fontSize = 14.sp
                )
                Text(
                    text = "Rp ${product.price.formatThousand()}",
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }

            if (qty > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(32.dp).clickable { onRemove() },
                        color = Color(0xFFFFEBEE),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Remove, null, tint = accentColor, modifier = Modifier.padding(6.dp))
                    }
                    
                    Text(
                        text = qty.toString(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = Color.Black,
                        modifier = Modifier.widthIn(min = 20.dp),
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        modifier = Modifier.size(32.dp).clickable { onAdd() },
                        color = Color(0xFFE8F5E9),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color(0xFF4CAF50), modifier = Modifier.padding(6.dp))
                    }
                }
            } else {
                Button(
                    onClick = onAdd,
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                ) {
                    Text("Tambah", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
