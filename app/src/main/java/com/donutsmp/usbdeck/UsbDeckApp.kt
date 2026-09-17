package com.donutsmp.usbdeck

import android.app.Application
import com.donutsmp.usbdeck.download.DownloadEngine
import com.donutsmp.usbdeck.files.UsbFileRepository
import com.donutsmp.usbdeck.settings.SettingsRepository
import com.donutsmp.usbdeck.usb.UsbMonitor

class UsbDeckApp : Application() {
    lateinit var settings: SettingsRepository
        private set
    lateinit var usbMonitor: UsbMonitor
        private set
    lateinit var files: UsbFileRepository
        private set
    lateinit var downloads: DownloadEngine
        private set

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository(this)
        usbMonitor = UsbMonitor(this, settings)
        files = UsbFileRepository(this, usbMonitor)
        downloads = DownloadEngine(this, files, usbMonitor, settings)
        usbMonitor.start()
    }
}
