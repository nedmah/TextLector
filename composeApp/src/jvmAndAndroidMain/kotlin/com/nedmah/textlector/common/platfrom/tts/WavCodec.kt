package com.nedmah.textlector.common.platfrom.tts

import java.nio.ByteBuffer
import java.nio.ByteOrder

object WavCodec {

    private const val HEADER_SIZE = 44

    // Mono PCM16 WAV
    fun fromSamples(samples: FloatArray, sampleRate: Int): ByteArray {
        val dataSize = samples.size * 2
        val buffer = ByteBuffer.allocate(HEADER_SIZE + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray())
        buffer.putInt(36 + dataSize)
        buffer.put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16)              // fmt chunk size
        buffer.putShort(1)             // PCM
        buffer.putShort(1)             // mono
        buffer.putInt(sampleRate)
        buffer.putInt(sampleRate * 2)  // byte rate
        buffer.putShort(2)             // block align
        buffer.putShort(16)            // bits per sample
        buffer.put("data".toByteArray())
        buffer.putInt(dataSize)
        for (sample in samples) {
            buffer.putShort((sample.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort())
        }
        return buffer.array()
    }
}