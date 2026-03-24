package com.poshan.myapplication

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.NumberFormat
import java.util.Locale

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val price: Int,
    val colorInt: Int
) {
    val color: Color get() = Color(colorInt)
}

@Entity(tableName = "product_configurations")
data class ProductConfiguration(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val configName: String,
    val productsJson: String
)

data class CartItem(
    val product: Product,
    var qty: Int
)

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val password: String,
    val role: String, // "OWNER", "ADMIN", "HEAD", "FINANCE", "KASIR"
    val outletId: Int,
    val outletName: String // Nama outlet untuk ditampilkan di header
)

@Entity(tableName = "price_audit_logs")
data class PriceAuditLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val productName: String,
    val oldPrice: Int,
    val newPrice: Int,
    val changedBy: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val action: String,
    val details: String,
    val user: String,
    val timestamp: Long = System.currentTimeMillis()
)

fun Int.formatThousand(): String {
    val formatter = NumberFormat.getInstance(Locale("id", "ID"))
    return formatter.format(this)
}
