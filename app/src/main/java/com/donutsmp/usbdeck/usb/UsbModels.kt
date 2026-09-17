package com.donutsmp.usbdeck.usb

enum class UsbConnectionState {
    Disconnected,
    Connecting,
    PermissionRequired,
    RequestingPermission,
    Connected,
    PermissionDenied,
    Unavailable,
    Error
}

data class UsbDeviceInfo(
    val deviceName: String,
    val deviceKey: String,
    val vendorId: Int,
    val productId: Int,
    val manufacturer: String?,
    val product: String?,
    val deviceClass: Int,
    val interfaceCount: Int,
    val hasUsbPermission: Boolean
) {
    val displayName: String
        get() = product?.takeIf { it.isNotBlank() }
            ?: manufacturer?.takeIf { it.isNotBlank() }
            ?: deviceName

    val vidPid: String
        get() = "VID %04X / PID %04X".format(vendorId, productId)
}

data class VolumeInfo(
    val id: String,
    val description: String?,
    val state: String,
    val uuid: String?,
    val isRemovable: Boolean,
    val isPrimary: Boolean,
    val isEmulated: Boolean,
    val totalBytes: Long?,
    val freeBytes: Long?,
    val filesystemHint: String?
) {
    val usedBytes: Long?
        get() {
            val t = totalBytes ?: return null
            val f = freeBytes ?: return null
            return (t - f).coerceAtLeast(0L)
        }
}

data class UsbSnapshot(
    val connection: UsbConnectionState = UsbConnectionState.Disconnected,
    val devices: List<UsbDeviceInfo> = emptyList(),
    val volumes: List<VolumeInfo> = emptyList(),
    val selectedDeviceKey: String? = null,
    val treeUri: String? = null,
    val treeWritable: Boolean = false,
    val lastError: String? = null,
    val statusDetail: String? = null,
    val disconnectedDuringOperation: Boolean = false
) {
    val selectedDevice: UsbDeviceInfo?
        get() = devices.firstOrNull { it.deviceKey == selectedDeviceKey } ?: devices.firstOrNull()

    val removableVolume: VolumeInfo?
        get() = volumes.firstOrNull { it.isRemovable && !it.isPrimary }
            ?: volumes.firstOrNull { !it.isPrimary && it.state.equals("mounted", true) }

    val headline: String
        get() = when (connection) {
            UsbConnectionState.Connected -> "USB Connected"
            UsbConnectionState.Disconnected -> "No USB connected"
            UsbConnectionState.Connecting -> "Connecting…"
            UsbConnectionState.PermissionRequired -> "USB permission required"
            UsbConnectionState.RequestingPermission -> "Requesting USB permission…"
            UsbConnectionState.PermissionDenied -> "USB permission denied"
            UsbConnectionState.Unavailable -> "USB storage unavailable"
            UsbConnectionState.Error -> "USB error"
        }

    val subtitle: String
        get() = when (connection) {
            UsbConnectionState.Disconnected -> "Connect a USB drive to continue."
            UsbConnectionState.Connected -> selectedDevice?.displayName
                ?: removableVolume?.description
                ?: "Removable storage ready"
            UsbConnectionState.PermissionRequired ->
                selectedDevice?.displayName?.let { "Allow access to $it" }
                    ?: "Allow access to the connected USB device"
            UsbConnectionState.PermissionDenied ->
                "Permission was denied. You can request it again from the USB tab."
            UsbConnectionState.Unavailable ->
                "The volume is present but not mounted or not readable."
            UsbConnectionState.Error -> lastError ?: "An unexpected USB error occurred."
            UsbConnectionState.Connecting -> "Waiting for the system to mount storage…"
            UsbConnectionState.RequestingPermission ->
                selectedDevice?.displayName?.let { "Waiting for system prompt for $it" }
                    ?: "Waiting for the system permission prompt"
        }
}
