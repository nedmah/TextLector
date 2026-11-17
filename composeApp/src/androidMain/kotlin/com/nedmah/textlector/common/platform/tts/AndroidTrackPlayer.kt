package com.nedmah.textlector.common.platform.tts

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

object AndroidTrackPlayer {

    fun play(wav: ByteArray, trackRef: (AudioTrack) -> Unit) {
        val samples = wavToSamples(wav)
        val sampleRate = wavSampleRate(wav)
        if (samples.isEmpty()) return
        playPcm(samples, sampleRate, trackRef)
    }

    fun play(samples: FloatArray, sampleRate: Int, trackRef: (AudioTrack) -> Unit) {
        if (samples.isEmpty()) return
        playPcm(samples, sampleRate, trackRef)
    }

    fun stop(track: AudioTrack?) {
        track?.apply {
            pause()
            flush()
        }
    }

    private fun playPcm(samples: FloatArray, sampleRate: Int, trackRef: (AudioTrack) -> Unit) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        )
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        trackRef(track)
        track.play()

        // write by chunks - if stop() is called, cycle ends
        val chunkSize = minBufferSize / 4 // Float = 4 bytes
        var offset = 0

        while (offset < samples.size && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
            val end = minOf(offset + chunkSize, samples.size)
            track.write(samples, offset, end - offset, AudioTrack.WRITE_BLOCKING)
            offset = end
        }
        // wait until it finishes
        if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
    }

    private fun wavToSamples(wav: ByteArray): FloatArray {
        val dataOffset = 44 // we skip WAV header (first 44 bytes)
        if (wav.size <= dataOffset) return FloatArray(0)
        val pcm = wav.copyOfRange(dataOffset, wav.size)
        return FloatArray(pcm.size / 2) { i ->
            val s = (pcm[i * 2].toInt() and 0xFF) or (pcm[i * 2 + 1].toInt() shl 8)
            s.toShort() / Short.MAX_VALUE.toFloat()
        }
    }

    private fun wavSampleRate(wav: ByteArray): Int {
        if (wav.size < 28) return 22050
        return (wav[24].toInt() and 0xFF) or
                (wav[25].toInt() and 0xFF shl 8) or
                (wav[26].toInt() and 0xFF shl 16) or
                (wav[27].toInt() and 0xFF shl 24)
    }
}