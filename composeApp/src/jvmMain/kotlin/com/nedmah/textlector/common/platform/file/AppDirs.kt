package com.nedmah.textlector.common.platform.file

import java.io.File

object AppDirs {

    // mac: /Library/Application Support/TextLector, Windows: %APPDATA%\TextLector
    val dataDir: File by lazy {
        val home = System.getProperty("user.home")
        val os = System.getProperty("os.name").lowercase()
        val base = when {
            os.contains("mac") -> File(home, "Library/Application Support")
            os.contains("win") -> File(System.getenv("APPDATA") ?: "$home\\AppData\\Roaming")
            else -> File(System.getenv("XDG_DATA_HOME") ?: "$home/.local/share")
        }
        File(base, "TextLector").apply { mkdirs() }
    }
}