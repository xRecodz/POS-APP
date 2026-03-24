package com.poshan.myapplication

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY id DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isSynced = 0")
    suspend fun getUnsyncedTransactions(): List<TransactionEntity>

    @Query("UPDATE transactions SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Int>)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    // Produk
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("SELECT * FROM products")
    fun getAllProductsFlow(): Flow<List<Product>>

    @Query("SELECT * FROM products")
    suspend fun getAllProducts(): List<Product>

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()

    // Users
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<User>

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()

    // Audit Logs
    @Insert
    suspend fun insertAuditLog(log: PriceAuditLog)

    @Query("SELECT * FROM price_audit_logs ORDER BY id DESC")
    fun getAllAuditLogsFlow(): Flow<List<PriceAuditLog>>

    // Activity Logs
    @Insert
    suspend fun insertActivityLog(log: ActivityLog)

    @Query("SELECT * FROM activity_logs ORDER BY id DESC")
    fun getAllActivityLogsFlow(): Flow<List<ActivityLog>>

    // Product Configurations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfiguration(config: ProductConfiguration)

    @Query("SELECT * FROM product_configurations")
    suspend fun getAllConfigurations(): List<ProductConfiguration>

    @Delete
    suspend fun deleteConfiguration(config: ProductConfiguration)
}
