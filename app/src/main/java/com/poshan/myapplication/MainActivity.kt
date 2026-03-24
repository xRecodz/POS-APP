package com.poshan.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.poshan.myapplication.ui.theme.Pos_appTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private lateinit var printerManager: BluetoothPrinterManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Toast.makeText(this, "Izin Bluetooth diberikan", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Izin Bluetooth ditolak, printer mungkin tidak berfungsi", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        printerManager = BluetoothPrinterManager(this)
        checkPermissions()

        setContent {
            Pos_appTheme {
                val context = LocalContext.current
                val db = remember { AppDatabase.getDatabase(context) }
                val scope = rememberCoroutineScope()
                
                var loggedInUser by remember { mutableStateOf<User?>(null) }
                var currentScreen by remember { mutableStateOf("pos") }
                
                var editingTransactionId by remember { mutableStateOf<Int?>(null) }
                var resumedTableNumber by remember { mutableStateOf("") }
                var resumedCustomerName by remember { mutableStateOf("") }
                var resumedCustomerPhone by remember { mutableStateOf("") }
                var resumedDiscount by remember { mutableStateOf("No Diskon") }
                
                val productList by db.transactionDao().getAllProductsFlow().collectAsState(initial = emptyList())

                LaunchedEffect(Unit) {
                    scope.launch(Dispatchers.IO) {
                        // Seed Default Products if empty or need refresh
                        // We refresh to ensure the new menu is applied
                        db.transactionDao().deleteAllProducts()
                        val defaultProducts = listOf(
                            // Makanan
                            Product(name = "Bebek 1/4", price = 23000, colorInt = 0xFFFFCC80.toInt()),
                            Product(name = "Bebek Ingkung", price = 99000, colorInt = 0xFFFFB74D.toInt()),
                            Product(name = "Kepala Bebek (isi 2)", price = 5000, colorInt = 0xFFFFCCBC.toInt()),
                            Product(name = "Ikan Bumbu", price = 13000, colorInt = 0xFFFFF59D.toInt()),
                            Product(name = "Ayam Bumbu", price = 14000, colorInt = 0xFFFFF176.toInt()),
                            Product(name = "Sate Kambing", price = 28500, colorInt = 0xFFFFAB91.toInt()),
                            Product(name = "Tahu/Tempe", price = 1500, colorInt = 0xFFE6EE9C.toInt()),
                            Product(name = "Kerupuk", price = 1500, colorInt = 0xFFF5F5F5.toInt()),
                            Product(name = "Aneka Gorengan", price = 1500, colorInt = 0xFFFFF9C4.toInt()),
                            Product(name = "Aneka Telur", price = 5000, colorInt = 0xFFFFF59D.toInt()),
                            Product(name = "Rempelo Ati", price = 5000, colorInt = 0xFFFFCC80.toInt()),
                            Product(name = "Aneka Oseng", price = 5000, colorInt = 0xFFA5D6A7.toInt()),
                            Product(name = "Terong / Kobis Goreng", price = 5000, colorInt = 0xFFC5E1A5.toInt()),
                            Product(name = "Sop Ceker Isi 3", price = 10000, colorInt = 0xFFE1F5FE.toInt()),
                            Product(name = "Pete Goreng", price = 10000, colorInt = 0xFFC8E6C9.toInt()),
                            
                            // Minuman
                            Product(name = "Teh Tawar (H/ES)", price = 1500, colorInt = 0xFFF1F8E9.toInt()),
                            Product(name = "Teh Manis (H/ES)", price = 4000, colorInt = 0xFFDCEDC8.toInt()),
                            Product(name = "Air Mineral", price = 6000, colorInt = 0xFFE1F5FE.toInt()),
                            Product(name = "Jeruk (H/ES)", price = 6500, colorInt = 0xFFFFF3E0.toInt()),
                            Product(name = "Lemon Tea (H/ES)", price = 6500, colorInt = 0xFFFFFDE7.toInt()),
                            Product(name = "Es Dawet", price = 8000, colorInt = 0xFFF1F8E9.toInt()),
                            Product(name = "Es Tape Ketan", price = 10000, colorInt = 0xFFF3E5F5.toInt()),
                            Product(name = "Es Kopi Hitam", price = 8000, colorInt = 0xFFEFEBE9.toInt()),
                            Product(name = "Kopi Tubruk", price = 8000, colorInt = 0xFFD7CCC8.toInt()),
                            Product(name = "Kopi Susu Tubruk", price = 10000, colorInt = 0xFFBCAAA4.toInt()),
                            Product(name = "Es Kopi Gula Aren", price = 12000, colorInt = 0xFF8D6E63.toInt()),
                            Product(name = "Es Cappucino", price = 12000, colorInt = 0xFFBCAAA4.toInt()),
                            Product(name = "Es Moccacino", price = 12000, colorInt = 0xFFBCAAA4.toInt())
                        )
                        defaultProducts.forEach { db.transactionDao().insertProduct(it) }
                        
                        // Seed Users
                        if (db.transactionDao().getAllUsers().isEmpty()) {
                            val initialUsers = listOf(
                                User(username = "Ownerbebek", password = "Ownerbebek", role = "OWNER", outletId = 1, outletName = "Outlet Bebek"),
                                User(username = "Kasirbebek", password = "Kasirbebek", role = "KASIR", outletId = 1, outletName = "Outlet Bebek"),
                                User(username = "Ownerpawon", password = "Ownerpawon", role = "OWNER", outletId = 2, outletName = "Outlet Pawon"),
                                User(username = "Kasirpawon", password = "Kasirpawon", role = "KASIR", outletId = 2, outletName = "Outlet Pawon")
                            )
                            initialUsers.forEach { db.transactionDao().insertUser(it) }
                        }
                    }
                }
                
                var cart by remember { mutableStateOf(listOf<CartItem>()) }

                if (loggedInUser == null) {
                    LoginScreen(
                        onLoginSuccess = { user ->
                            loggedInUser = user
                            currentScreen = "pos"
                            scope.launch(Dispatchers.IO) {
                                db.transactionDao().insertActivityLog(ActivityLog(action = "LOGIN", details = "User logged in", user = user.username))
                            }
                        }
                    )
                } else {
                    val user = loggedInUser!!
                    when (currentScreen) {
                        "pos" -> PosScreen(
                            cart = cart,
                            productList = productList,
                            user = user,
                            printerManager = printerManager,
                            onCartChange = { cart = it },
                            goToPos = { currentScreen = "pos" },
                            goToTransaction = { currentScreen = "transaction" },
                            goToHistory = { currentScreen = "history" },
                            goToManageProducts = { currentScreen = "manage_products" },
                            goToLog = { currentScreen = "log" }
                        )
                        "transaction" -> TransactionScreen(
                            cart = cart,
                            onCartChange = { cart = it },
                            goToPos = { currentScreen = "pos" },
                            goToTransaction = { currentScreen = "transaction" },
                            goToHistory = { currentScreen = "history" },
                            goToLog = { currentScreen = "log" },
                            printerManager = printerManager,
                            user = user,
                            editingTransactionId = editingTransactionId,
                            initialTableNumber = resumedTableNumber,
                            initialCustomerName = resumedCustomerName,
                            initialCustomerPhone = resumedCustomerPhone,
                            initialDiscount = resumedDiscount,
                            onTransactionSuccess = {
                                cart = emptyList()
                                editingTransactionId = null
                                resumedTableNumber = ""
                                resumedCustomerName = ""
                                resumedCustomerPhone = ""
                                resumedDiscount = "No Diskon"
                                currentScreen = "pos"
                            }
                        )
                        "history" -> HistoryScreen(
                            cart = cart,
                            goToPos = { currentScreen = "pos" },
                            goToTransaction = { currentScreen = "transaction" },
                            goToHistory = { currentScreen = "history" },
                            goToLog = { currentScreen = "log" },
                            printerManager = printerManager,
                            user = user,
                            onResumeTransaction = { trx ->
                                editingTransactionId = trx.id
                                resumedTableNumber = trx.tableNumber
                                resumedCustomerName = trx.customerName
                                resumedCustomerPhone = trx.customerPhone
                                resumedDiscount = trx.discount

                                val itemsList = trx.items.split(", ").mapNotNull { entry ->
                                    val parts = entry.split(" x")
                                    if (parts.size == 2) {
                                        val productName = parts[0]
                                        val qty = parts[1].toIntOrNull() ?: 1
                                        val originalProduct = productList.find { it.name == productName }
                                        if (originalProduct != null) {
                                            CartItem(originalProduct, qty)
                                        } else {
                                            CartItem(Product(name = productName, price = 0, colorInt = 0xFFCCCCCC.toInt()), qty)
                                        }
                                    } else null
                                }
                                cart = itemsList
                                currentScreen = "transaction"
                                scope.launch(Dispatchers.IO) {
                                    db.transactionDao().insertActivityLog(ActivityLog(action = "RESUME TRANSACTION", details = "Resumed nota #${trx.id}", user = user.username))
                                }
                            }
                        )
                        "manage_products" -> ManageProductsScreen(
                            onBack = { currentScreen = "pos" },
                            currentUser = user
                        )
                        "log" -> LogScreen(
                            cart = cart,
                            goToPos = { currentScreen = "pos" },
                            goToTransaction = { currentScreen = "transaction" },
                            goToHistory = { currentScreen = "history" },
                            goToLog = { currentScreen = "log" },
                            db = db
                        )
                    }
                }
            }
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        val toRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (toRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(toRequest.toTypedArray())
        }
    }
}
