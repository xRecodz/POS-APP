# POS Application

Sebuah aplikasi Point of Sale (POS) sederhana yang dibangun menggunakan Jetpack Compose dan Room Database untuk manajemen transaksi toko.

## Fitur Utama

- **Manajemen Transaksi**: Mencatat penjualan, menghitung total otomatis, dan menangani berbagai tipe pembayaran (Cash, Transfer).
- **Sistem Harga Dinamis**: Mendukung berbagai skema harga seperti Promo, Harga Karyawan, Tuslah, GoFood, dan Shopee.
- **Cetak Struk**: Integrasi dengan printer Bluetooth untuk mencetak struk transaksi secara langsung.
- **Manajemen Produk**: Menambah, mengubah, dan menghapus data produk.
- **Riwayat Transaksi**: Melihat riwayat penjualan yang telah dilakukan.
- **Log Aktivitas**: Mencatat log aktivitas untuk audit (khusus Owner/Admin).
- **Sistem Login**: Keamanan akses berdasarkan role user (Owner, Admin, Head, Kasir).

## Teknologi yang Digunakan

- **Bahasa**: Kotlin
- **UI Framework**: Jetpack Compose
- **Database**: Room Persistence Library
- **Architecture**: MVVM (Model-View-ViewModel) - *sedang dalam pengembangan*
- **Dependency**:
    - Material 3 untuk UI components
    - Room untuk penyimpanan lokal
    - Bluetooth API untuk printing

## Struktur Project

- `MainActivity.kt`: Entry point aplikasi.
- `PosScreen.kt`: Layar utama untuk memilih produk.
- `TransactionScreen.kt`: Layar untuk detail pembayaran dan cetak struk.
- `ManageProductsScreen.kt`: Layar manajemen stok/produk.
- `AppDatabase.kt`: Konfigurasi Room Database.
- `BluetoothPrinterManager.kt`: Helper untuk komunikasi dengan printer Bluetooth.

## Cara Menjalankan

1. Clone repository ini.
2. Buka project di Android Studio (Koala atau versi terbaru).
3. Pastikan sudah menginstal SDK Android yang sesuai.
4. Build dan jalankan di Emulator atau Device Android fisik.

## Lisensi

Copyright © 2024 POShan Application.
