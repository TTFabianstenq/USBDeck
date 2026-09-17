package com.donutsmp.usbdeck.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageManager.StorageVolumeCallback
import android.os.storage.StorageStatsManager
import android.os.storage.StorageVolume
import androidx.core.content.ContextCompat
import com.donutsmp.usbdeck.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class UsbMonitor(
    private val appContext: Context,
    private val settings: SettingsRepository
) {
    companion object {
        const val ACTION_USB_PERMISSION = "com.donutsmp.usbdeck.USB_PERMISSION"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val usbManager = appContext.getSystemService(Context.USB_SERVICE) as UsbManager
    private val storageManager = appContext.getSystemService(Context.STORAGE_SERVICE) as StorageManager
    private val _snapshot = MutableStateFlow(UsbSnapshot())
    val snapshot: StateFlow<UsbSnapshot> = _snapshot.asStateFlow()
    private val started = AtomicBoolean(false)
    private val operationTokens = mutableSetOf<String>()

    private val permissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_USB_PERMISSION) return
            onPermissionResult(extractDevice(intent), intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
        }
    }
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val device = extractDevice(intent)
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> onDeviceAttached(device)
                UsbManager.ACTION_USB_DEVICE_DETACHED -> onDeviceDetached(device)
            }
        }
    }
    private val mediaReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) { refresh() }
    }

    fun start() {
        if (!started.compareAndSet(false, true)) { refresh(); return }
        ContextCompat.registerReceiver(appContext, permissionReceiver, IntentFilter(ACTION_USB_PERMISSION), ContextCompat.RECEIVER_NOT_EXPORTED)
        val usbFilter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        ContextCompat.registerReceiver(appContext, usbReceiver, usbFilter, ContextCompat.RECEIVER_NOT_EXPORTED)
        val mediaFilter = IntentFilter().apply {
            addAction(Intent.ACTION_MEDIA_MOUNTED)
            addAction(Intent.ACTION_MEDIA_UNMOUNTED)
            addAction(Intent.ACTION_MEDIA_REMOVED)
            addAction(Intent.ACTION_MEDIA_BAD_REMOVAL)
            addAction(Intent.ACTION_MEDIA_EJECT)
            addDataScheme("file")
        }
        ContextCompat.registerReceiver(appContext, mediaReceiver, mediaFilter, ContextCompat.RECEIVER_NOT_EXPORTED)
        if (Build.VERSION.SDK_INT >= 30) {
            storageManager.registerStorageVolumeCallback(appContext.mainExecutor, object : StorageVolumeCallback() {
                override fun onStateChanged(volume: StorageVolume) { refresh() }
            })
        }
        scope.launch {
            settings.flow.collect { prefs ->
                _snapshot.update { it.copy(treeUri = prefs.usbTreeUri) }
                refresh()
            }
        }
        refresh()
    }

    fun beginOperation(token: String) { synchronized(operationTokens) { operationTokens += token } }
    fun endOperation(token: String) { synchronized(operationTokens) { operationTokens -= token } }
    fun hasActiveOperations(): Boolean = synchronized(operationTokens) { operationTokens.isNotEmpty() }

    fun onDeviceAttached(device: UsbDevice?) {
        refresh()
        if (device == null || !settings.current.autoDetect) return
        if (!usbManager.hasPermission(device)) requestPermission(device)
    }

    fun onDeviceDetached(device: UsbDevice?) {
        val hadOps = hasActiveOperations()
        synchronized(operationTokens) { operationTokens.clear() }
        _snapshot.update {
            it.copy(
                disconnectedDuringOperation = hadOps || it.disconnectedDuringOperation,
                lastError = if (hadOps) "USB disconnected during an operation. Nothing was marked successful." else it.lastError
            )
        }
        refresh()
    }

    fun requestPermissionForSelected() {
        val device = currentUsbDevices().firstOrNull { !usbManager.hasPermission(it) } ?: currentUsbDevices().firstOrNull()
        if (device == null) {
            _snapshot.update { it.copy(connection = UsbConnectionState.Disconnected, statusDetail = "No USB host device is visible.") }
            return
        }
        requestPermission(device)
    }

    fun requestPermission(device: UsbDevice) {
        if (usbManager.hasPermission(device)) { refresh(); return }
        val key = deviceKey(device)
        _snapshot.update {
            it.copy(connection = UsbConnectionState.RequestingPermission, selectedDeviceKey = key, statusDetail = "Requesting access to ${displayName(device)}")
        }
        val intent = Intent(ACTION_USB_PERMISSION).setPackage(appContext.packageName)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
        usbManager.requestPermission(device, PendingIntent.getBroadcast(appContext, 7101 + (device.vendorId xor device.productId), intent, flags))
    }

    private fun onPermissionResult(device: UsbDevice?, granted: Boolean) {
        if (device == null) {
            _snapshot.update { it.copy(connection = UsbConnectionState.Error, lastError = "Permission result did not include a USB device.") }
            refresh(); return
        }
        val key = deviceKey(device)
        if (!granted) {
            _snapshot.update { it.copy(connection = UsbConnectionState.PermissionDenied, selectedDeviceKey = key, lastError = "Permission denied for ${displayName(device)}.") }
        } else {
            _snapshot.update { it.copy(selectedDeviceKey = key, lastError = null, statusDetail = "Permission granted for ${displayName(device)}") }
        }
        refresh()
    }

    fun setTreeUri(uri: Uri?, writable: Boolean) {
        scope.launch { settings.setUsbTreeUri(uri?.toString()) }
        _snapshot.update { it.copy(treeUri = uri?.toString(), treeWritable = writable) }
        refresh()
    }

    fun clearDisconnectedFlag() { _snapshot.update { it.copy(disconnectedDuringOperation = false) } }

    fun refresh() {
        val devices = currentUsbDevices().map { toInfo(it) }
        val volumes = currentVolumes()
        val removable = volumes.firstOrNull { it.isRemovable && !it.isPrimary } ?: volumes.firstOrNull { !it.isPrimary }
        val selected = devices.firstOrNull { !it.hasUsbPermission } ?: devices.firstOrNull()
        val mounted = removable?.state.equals("mounted", true) == true
        val anyDevice = devices.isNotEmpty()
        val needsPermission = devices.any { !it.hasUsbPermission }
        val denied = _snapshot.value.connection == UsbConnectionState.PermissionDenied && needsPermission
        val requesting = _snapshot.value.connection == UsbConnectionState.RequestingPermission && needsPermission
        val connection = when {
            !anyDevice && removable == null -> UsbConnectionState.Disconnected
            !anyDevice && removable != null && !mounted -> UsbConnectionState.Unavailable
            requesting -> UsbConnectionState.RequestingPermission
            denied -> UsbConnectionState.PermissionDenied
            anyDevice && needsPermission && !mounted -> UsbConnectionState.PermissionRequired
            anyDevice && needsPermission && mounted ->
                if (_snapshot.value.treeUri != null) UsbConnectionState.Connected else UsbConnectionState.PermissionRequired
            anyDevice && !mounted && removable == null -> UsbConnectionState.Connecting
            mounted || _snapshot.value.treeUri != null || (anyDevice && devices.all { it.hasUsbPermission }) -> UsbConnectionState.Connected
            else -> UsbConnectionState.Connecting
        }
        _snapshot.update { prev ->
            prev.copy(connection = connection, devices = devices, volumes = volumes, selectedDeviceKey = selected?.deviceKey ?: prev.selectedDeviceKey)
        }
    }

    fun currentUsbDevices(): List<UsbDevice> = usbManager.deviceList.values.toList()

    private fun currentVolumes(): List<VolumeInfo> = storageManager.storageVolumes.map { vol ->
        var total: Long? = null
        var free: Long? = null
        val uuidStr = vol.uuid
        if (!uuidStr.isNullOrBlank() && Build.VERSION.SDK_INT >= 26) {
            try {
                val stats = appContext.getSystemService(StorageStatsManager::class.java)
                val uuid = runCatching { UUID.fromString(uuidStr) }.getOrNull()
                if (stats != null && uuid != null) {
                    total = stats.getTotalBytes(uuid)
                    free = stats.getFreeBytes(uuid)
                }
            } catch (_: Exception) { }
        }
        if (total == null && Build.VERSION.SDK_INT >= 30) {
            vol.directory?.let {
                total = it.totalSpace.takeIf { s -> s > 0 }
                free = it.freeSpace.takeIf { s -> s >= 0 }
            }
        }
        VolumeInfo(
            id = vol.uuid ?: vol.hashCode().toString(),
            description = vol.getDescription(appContext),
            state = vol.state ?: Environment.MEDIA_UNKNOWN,
            uuid = vol.uuid,
            isRemovable = vol.isRemovable,
            isPrimary = vol.isPrimary,
            isEmulated = vol.isEmulated,
            totalBytes = total,
            freeBytes = free,
            filesystemHint = null
        )
    }

    fun createOpenDocumentTreeIntent(): Intent? {
        val volume = storageManager.storageVolumes.firstOrNull { it.isRemovable && !it.isPrimary }
            ?: storageManager.storageVolumes.firstOrNull { !it.isPrimary } ?: return null
        return if (Build.VERSION.SDK_INT >= 29) volume.createOpenDocumentTreeIntent() else null
    }

    fun storageSettingsIntent(): Intent =
        Intent(android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun toInfo(device: UsbDevice) = UsbDeviceInfo(
        deviceName = device.deviceName, deviceKey = deviceKey(device),
        vendorId = device.vendorId, productId = device.productId,
        manufacturer = device.manufacturerName, product = device.productName,
        deviceClass = device.deviceClass, interfaceCount = device.interfaceCount,
        hasUsbPermission = usbManager.hasPermission(device)
    )
    private fun deviceKey(device: UsbDevice) = "${device.vendorId}:${device.productId}:${device.deviceName}"
    private fun displayName(device: UsbDevice) = device.productName?.takeIf { it.isNotBlank() } ?: device.deviceName
    @Suppress("unused")
    private fun looksLikeStorage(device: UsbDevice): Boolean {
        if (device.deviceClass == UsbConstants.USB_CLASS_MASS_STORAGE) return true
        for (i in 0 until device.interfaceCount) {
            if (device.getInterface(i).interfaceClass == UsbConstants.USB_CLASS_MASS_STORAGE) return true
        }
        return false
    }
    private fun extractDevice(intent: Intent): UsbDevice? =
        if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        else @Suppress("DEPRECATION") intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
}
