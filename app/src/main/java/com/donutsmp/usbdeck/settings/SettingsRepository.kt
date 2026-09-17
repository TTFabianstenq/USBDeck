package com.donutsmp.usbdeck.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.settingsStore by preferencesDataStore(name = "usbdeck_settings")

enum class ThemeMode { System, Light, Dark }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val verifySha256: Boolean = true,
    val askBeforeOverwrite: Boolean = true,
    val autoDetect: Boolean = true,
    val confirmDestructive: Boolean = true,
    val defaultDownloadFolder: String = "USBDeck",
    val usbTreeUri: String? = null
)

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.settingsStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private object Keys {
        val theme = stringPreferencesKey("theme")
        val verify = booleanPreferencesKey("verify_sha")
        val overwrite = booleanPreferencesKey("ask_overwrite")
        val auto = booleanPreferencesKey("auto_detect")
        val confirm = booleanPreferencesKey("confirm_destructive")
        val folder = stringPreferencesKey("download_folder")
        val tree = stringPreferencesKey("usb_tree_uri")
    }

    val flow: StateFlow<AppSettings> = store.data.map { prefs ->
        AppSettings(
            themeMode = runCatching {
                ThemeMode.valueOf(prefs[Keys.theme] ?: ThemeMode.System.name)
            }.getOrDefault(ThemeMode.System),
            verifySha256 = prefs[Keys.verify] ?: true,
            askBeforeOverwrite = prefs[Keys.overwrite] ?: true,
            autoDetect = prefs[Keys.auto] ?: true,
            confirmDestructive = prefs[Keys.confirm] ?: true,
            defaultDownloadFolder = prefs[Keys.folder] ?: "USBDeck",
            usbTreeUri = prefs[Keys.tree]
        )
    }.stateIn(scope, SharingStarted.Eagerly, AppSettings())

    val current: AppSettings
        get() = flow.value

    suspend fun snapshot(): AppSettings = store.data.map { prefs ->
        AppSettings(
            themeMode = runCatching {
                ThemeMode.valueOf(prefs[Keys.theme] ?: ThemeMode.System.name)
            }.getOrDefault(ThemeMode.System),
            verifySha256 = prefs[Keys.verify] ?: true,
            askBeforeOverwrite = prefs[Keys.overwrite] ?: true,
            autoDetect = prefs[Keys.auto] ?: true,
            confirmDestructive = prefs[Keys.confirm] ?: true,
            defaultDownloadFolder = prefs[Keys.folder] ?: "USBDeck",
            usbTreeUri = prefs[Keys.tree]
        )
    }.first()

    fun blockingSnapshot(): AppSettings = runBlocking { snapshot() }

    suspend fun setTheme(mode: ThemeMode) { store.edit { it[Keys.theme] = mode.name } }
    suspend fun setVerifySha256(value: Boolean) { store.edit { it[Keys.verify] = value } }
    suspend fun setAskBeforeOverwrite(value: Boolean) { store.edit { it[Keys.overwrite] = value } }
    suspend fun setAutoDetect(value: Boolean) { store.edit { it[Keys.auto] = value } }
    suspend fun setConfirmDestructive(value: Boolean) { store.edit { it[Keys.confirm] = value } }
    suspend fun setDefaultDownloadFolder(value: String) { store.edit { it[Keys.folder] = value.ifBlank { "USBDeck" } } }
    suspend fun setUsbTreeUri(value: String?) {
        store.edit {
            if (value.isNullOrBlank()) it.remove(Keys.tree) else it[Keys.tree] = value
        }
    }
}
