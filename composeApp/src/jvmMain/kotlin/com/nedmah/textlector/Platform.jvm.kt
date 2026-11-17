package com.nedmah.textlector

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
    override val hasSystemTts = false
    override val hasCamera = false
}

actual fun getPlatform(): Platform = JVMPlatform()