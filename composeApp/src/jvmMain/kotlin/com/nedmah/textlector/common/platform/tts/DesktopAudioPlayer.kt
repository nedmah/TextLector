package com.nedmah.textlector.common.platform.tts

import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

internal class DesktopAudioPlayer {

    @Volatile private var currentLine: SourceDataLine? = null

    // blocks until the clip has played or stop() was called
    fun play(wav: ByteArray) {
        AudioSystem.getAudioInputStream(ByteArrayInputStream(wav)).use { stream ->
            val line = AudioSystem.getSourceDataLine(stream.format)
            line.open(stream.format)
            line.start()
            currentLine = line
            try {
                val buffer = ByteArray(8192)
                while (currentLine === line) {
                    val read = stream.read(buffer)
                    if (read < 0) break
                    line.write(buffer, 0, read)
                }
                if (currentLine === line) line.drain()
            } finally {
                line.close()
                if (currentLine === line) currentLine = null
            }
        }
    }

    fun stop() {
        val line = currentLine ?: return
        currentLine = null
        line.stop()
        line.flush() // unblocks a pending write()
    }
}