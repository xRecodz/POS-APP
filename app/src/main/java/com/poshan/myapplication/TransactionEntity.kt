package com.poshan.myapplication

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val items: String,
    val total: Int,
    val discount: String,
    val tableNumber: String,
    val customerName: String,
    val customerPhone: String,
    val paymentMethod: String = "Cash",
    val status: String, // "COMPLETED", "VOID", "PENDING"
    val createdBy: String, // Username pembuat transaksi
    val updatedBy: String? = null, // Username yang mengubah (untuk VOID/Edit)
    val voidReason: String? = null, // Alasan jika dibatalkan
    val isSynced: Boolean = false, // Untuk sinkronisasi cloud nanti
    val timestamp: Long = System.currentTimeMillis()
)
