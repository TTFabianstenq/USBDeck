package com.donutsmp.usbdeck.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.donutsmp.usbdeck.UsbDeckApp

class UsbEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val device = if (android.os.Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }
        val app = context.applicationContext as? UsbDeckApp ?: return
        when (intent.action) {
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> app.usbMonitor.onDeviceAttached(device)
            UsbManager.ACTION_USB_DEVICE_DETACHED -> app.usbMonitor.onDeviceDetached(device)
        }
    }
}
