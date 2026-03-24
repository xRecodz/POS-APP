package com.poshan.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.IOException
import java.io.OutputStream
import java.util.*

class BluetoothPrinterManager(private val context: Context) {

    private val sharedPreferences = context.getSharedPreferences("printer_prefs", Context.MODE_PRIVATE)

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private val PRINTER_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        if (!hasBluetoothPermissions()) return emptyList()
        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }

    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            context.checkSelfPermission(Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun savePrinterAddress(address: String) {
        sharedPreferences.edit().putString("printer_address", address).apply()
    }

    fun getSavedPrinterAddress(): String? {
        return sharedPreferences.getString("printer_address", null)
    }

    @SuppressLint("MissingPermission")
    fun getSavedDevice(): BluetoothDevice? {
        val address = getSavedPrinterAddress() ?: return null
        return bluetoothAdapter?.getRemoteDevice(address)
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice, onResult: (Boolean) -> Unit) {
        try {
            disconnect()
            // Menggunakan Insecure RFCOMM untuk koneksi yang lebih cepat
            bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(PRINTER_UUID)
            bluetoothSocket?.connect()
            outputStream = bluetoothSocket?.outputStream
            onResult(true)
        } catch (e: IOException) {
            try {
                // Fallback ke secure jika insecure gagal
                bluetoothSocket = device.createRfcommSocketToServiceRecord(PRINTER_UUID)
                bluetoothSocket?.connect()
                outputStream = bluetoothSocket?.outputStream
                onResult(true)
            } catch (e2: IOException) {
                e2.printStackTrace()
                onResult(false)
            }
        }
    }

    fun disconnect() {
        try {
            outputStream?.flush()
            outputStream?.close()
            bluetoothSocket?.close()
            outputStream = null
            bluetoothSocket = null
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun printText(text: String) {
        try {
            outputStream?.write(text.toByteArray())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun printNewLine() {
        printText("\n")
    }

    // ESC/POS Commands
    fun resetPrinter() {
        outputStream?.write(byteArrayOf(0x1B, 0x40))
    }

    fun centerAlign() {
        outputStream?.write(byteArrayOf(0x1B, 0x61, 1))
    }

    fun leftAlign() {
        outputStream?.write(byteArrayOf(0x1B, 0x61, 0))
    }
}
