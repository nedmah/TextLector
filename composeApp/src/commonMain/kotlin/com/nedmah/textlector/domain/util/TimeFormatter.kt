package com.nedmah.textlector.domain.util

fun formatMinutes(minutes: Float): String {
    val total = minutes.toInt()
    val h = total / 60
    val m = total % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}