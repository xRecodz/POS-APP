package com.poshan.myapplication

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageProductsScreen(onBack: () -> Unit, currentUser: User) {
    BackHandler(onBack = onBack)
    
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    val gson = remember { Gson() }
    
    val darkRed = Color(0xFFC62828)
    val softBackground = Color(0xFFF8F9FA)
    val mutedPrimary = Color(0xFF455A64)
    val softGrey = Color(0xFF6C757D)

    var products by remember { mutableStateOf(listOf<Product>()) }
    var savedConfigs by remember { mutableStateOf(listOf<ProductConfiguration>()) }
    
    var showDialog by remember { mutableStateOf(false) }
    var showConfigDialog by remember { mutableStateOf(false) }
    var showSaveConfigDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var configName by remember { mutableStateOf("") }
    var importJson by remember { mutableStateOf("") }

    // Role-based permissions
    val canEditPrice = currentUser.role in listOf("OWNER", "ADMIN", "HEAD")
    val canDeleteProduct = currentUser.role in listOf("OWNER", "ADMIN")
    val canManageConfigs = currentUser.role in listOf("OWNER", "ADMIN")

    fun refreshProducts() {
        scope.launch(Dispatchers.IO) {
            products = db.transactionDao().getAllProducts()
        }
    }

    fun refreshConfigs() {
        scope.launch(Dispatchers.IO) {
            savedConfigs = db.transactionDao().getAllConfigurations()
        }
    }

    LaunchedEffect(Unit) { 
        refreshProducts()
        refreshConfigs()
    }

    fun loadProductsFromList(productList: List<Product>, configTitle: String) {
        if (!canManageConfigs) {
            Toast.makeText(context, "Akses ditolak: Hanya Admin/Owner", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch(Dispatchers.IO) {
            db.transactionDao().deleteAllProducts()
            productList.forEach {
                db.transactionDao().insertProduct(it.copy(id = 0)) 
            }
            refreshProducts()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Konfigurasi $configTitle berhasil dimuat", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun setupPresetConfiguration(type: String) {
        val configProducts = when (type) {
            "BR" -> listOf(
                Product(name = "Ayam Goreng Lamongan", price = 15000, colorInt = Color.White.toArgb()),
                Product(name = "Bebek Goreng Lamongan", price = 22000, colorInt = Color.White.toArgb()),
                Product(name = "Lele Goreng", price = 12000, colorInt = Color.White.toArgb()),
                Product(name = "Tahu Tempe Goreng", price = 5000, colorInt = Color.White.toArgb()),
                Product(name = "Nasi Uduk", price = 6000, colorInt = Color.White.toArgb()),
                Product(name = "Es Teh Manis", price = 5000, colorInt = Color.White.toArgb())
            )
            "Pawon" -> listOf(
                Product(name = "Oseng Mercon", price = 18000, colorInt = Color.White.toArgb()),
                Product(name = "Sayur Lodeh", price = 10000, colorInt = Color.White.toArgb()),
                Product(name = "Oseng Kacang Panjang", price = 8000, colorInt = Color.White.toArgb()),
                Product(name = "Ikan Asin Balado", price = 12000, colorInt = Color.White.toArgb()),
                Product(name = "Nasi Putih", price = 5000, colorInt = Color.White.toArgb()),
                Product(name = "Es Jeruk", price = 7000, colorInt = Color.White.toArgb())
            )
            else -> emptyList()
        }
        loadProductsFromList(configProducts, type)
    }

    fun shareConfiguration(config: ProductConfiguration) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, config.productsJson)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Bagikan Konfigurasi: ${config.configName}")
        context.startActivity(shareIntent)
    }

    Scaffold(
        containerColor = softBackground,
        topBar = {
            TopAppBar(
                title = { Text("Kelola Produk", fontWeight = FontWeight.ExtraBold, color = Color.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = mutedPrimary)
                    }
                },
                actions = {
                    if (canManageConfigs) {
                        IconButton(onClick = { showConfigDialog = true }) {
                            Icon(Icons.Default.Settings, "Konfigurasi", tint = mutedPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            if (canEditPrice) {
                FloatingActionButton(
                    onClick = {
                        editingProduct = null
                        name = ""
                        price = ""
                        showDialog = true
                    },
                    containerColor = darkRed,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, null)
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(products) { product ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    ListItem(
                        headlineContent = { Text(product.name, fontWeight = FontWeight.Bold, color = Color(0xFF343A40)) },
                        supportingContent = { Text("Rp ${product.price}", color = darkRed, fontWeight = FontWeight.Bold) },
                        trailingContent = {
                            Row {
                                if (canEditPrice) {
                                    IconButton(onClick = {
                                        editingProduct = product
                                        name = product.name
                                        price = product.price.toString()
                                        showDialog = true
                                    }) {
                                        Icon(Icons.Default.Edit, null, tint = mutedPrimary)
                                    }
                                }
                                if (canDeleteProduct) {
                                    IconButton(onClick = {
                                        scope.launch(Dispatchers.IO) {
                                            db.transactionDao().deleteProduct(product)
                                            refreshProducts()
                                        }
                                    }) {
                                        Icon(Icons.Default.Delete, null, tint = Color(0xFFE57373))
                                    }
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }

        // --- DIALOGS ---

        if (showConfigDialog) {
            AlertDialog(
                onDismissRequest = { showConfigDialog = false },
                title = { Text("Konfigurasi Produk", fontWeight = FontWeight.Bold, color = Color.Black) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Text("Muat preset atau konfigurasi yang sudah disimpan.", fontSize = 13.sp, color = softGrey)
                        
                        Divider(color = Color(0xFFF1F3F5))
                        
                        Text("Presets", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = mutedPrimary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { setupPresetConfiguration("BR"); showConfigDialog = false },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = mutedPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("BR", fontSize = 12.sp) }
                            
                            Button(
                                onClick = { setupPresetConfiguration("Pawon"); showConfigDialog = false },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = mutedPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("Pawon", fontSize = 12.sp) }
                        }

                        Divider(color = Color(0xFFF1F3F5))

                        Text("Simpanan Anda", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = mutedPrimary)
                        if (savedConfigs.isEmpty()) {
                            Text("Belum ada konfigurasi disimpan", fontSize = 12.sp, color = Color.LightGray)
                        } else {
                            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                                items(savedConfigs) { config ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(config.configName, modifier = Modifier.weight(1f).clickable {
                                            val listType = object : TypeToken<List<Product>>() {}.type
                                            val productList: List<Product> = gson.fromJson(config.productsJson, listType)
                                            loadProductsFromList(productList, config.configName)
                                            showConfigDialog = false
                                        }, fontSize = 14.sp, color = Color.Black)
                                        
                                        IconButton(onClick = { shareConfiguration(config) }) {
                                            Icon(Icons.Default.Share, null, tint = mutedPrimary, modifier = Modifier.size(18.dp))
                                        }
                                        
                                        IconButton(onClick = {
                                            scope.launch(Dispatchers.IO) {
                                                db.transactionDao().deleteConfiguration(config)
                                                refreshConfigs()
                                            }
                                        }) {
                                            Icon(Icons.Default.Delete, null, tint = Color(0xFFE57373), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }

                        Divider(color = Color(0xFFF1F3F5))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { showSaveConfigDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, darkRed)
                            ) {
                                Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp), tint = darkRed)
                                Spacer(Modifier.width(4.dp))
                                Text("Simpan", color = darkRed, fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { showImportDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, mutedPrimary)
                            ) {
                                Icon(Icons.Default.Input, null, modifier = Modifier.size(16.dp), tint = mutedPrimary)
                                Spacer(Modifier.width(4.dp))
                                Text("Import", color = mutedPrimary, fontSize = 12.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showConfigDialog = false }) {
                        Text("Tutup", color = softGrey)
                    }
                }
            )
        }

        if (showSaveConfigDialog) {
            AlertDialog(
                onDismissRequest = { showSaveConfigDialog = false },
                title = { Text("Simpan Konfigurasi Saat Ini", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Simpan daftar produk saat ini agar bisa dimuat nanti.", fontSize = 14.sp)
                        OutlinedTextField(
                            value = configName,
                            onValueChange = { configName = it },
                            label = { Text("Nama Konfigurasi") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (configName.isNotBlank()) {
                                scope.launch(Dispatchers.IO) {
                                    val json = gson.toJson(products)
                                    db.transactionDao().insertConfiguration(ProductConfiguration(configName = configName, productsJson = json))
                                    refreshConfigs()
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Konfigurasi disimpan", Toast.LENGTH_SHORT).show()
                                        showSaveConfigDialog = false
                                        configName = ""
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = darkRed)
                    ) { Text("Simpan") }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveConfigDialog = false }) { Text("Batal") }
                }
            )
        }

        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                title = { Text("Import Konfigurasi", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Tempel JSON konfigurasi di sini.", fontSize = 14.sp)
                        OutlinedTextField(
                            value = importJson,
                            onValueChange = { importJson = it },
                            label = { Text("JSON Produk") },
                            modifier = Modifier.fillMaxWidth().height(150.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            try {
                                val listType = object : TypeToken<List<Product>>() {}.type
                                val productList: List<Product> = gson.fromJson(importJson, listType)
                                loadProductsFromList(productList, "Imported")
                                showImportDialog = false
                                importJson = ""
                            } catch (e: Exception) {
                                Toast.makeText(context, "Format JSON tidak valid", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = mutedPrimary)
                    ) { Text("Import") }
                },
                dismissButton = {
                    TextButton(onClick = { showImportDialog = false }) { Text("Batal") }
                }
            )
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(if (editingProduct == null) "Tambah Produk" else "Edit Produk", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nama Produk") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = price,
                            onValueChange = { price = it },
                            label = { Text("Harga") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val priceInt = price.toIntOrNull() ?: 0
                            scope.launch(Dispatchers.IO) {
                                if (editingProduct == null) {
                                    val newProduct = Product(name = name, price = priceInt, colorInt = Color.White.toArgb())
                                    db.transactionDao().insertProduct(newProduct)
                                    db.transactionDao().insertAuditLog(PriceAuditLog(
                                        productId = 0,
                                        productName = name,
                                        oldPrice = 0,
                                        newPrice = priceInt,
                                        changedBy = currentUser.username
                                    ))
                                } else {
                                    val oldPrice = editingProduct!!.price
                                    db.transactionDao().updateProduct(editingProduct!!.copy(name = name, price = priceInt))
                                    if (oldPrice != priceInt) {
                                        db.transactionDao().insertAuditLog(PriceAuditLog(
                                            productId = editingProduct!!.id,
                                            productName = name,
                                            oldPrice = oldPrice,
                                            newPrice = priceInt,
                                            changedBy = currentUser.username
                                        ))
                                    }
                                }
                                refreshProducts()
                                withContext(Dispatchers.Main) { showDialog = false }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = darkRed)
                    ) {
                        Text("Simpan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}
