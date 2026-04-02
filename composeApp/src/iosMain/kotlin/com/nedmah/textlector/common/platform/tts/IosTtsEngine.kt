package com.nedmah.textlector.common.platform.tts

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechSynthesizerDelegateProtocol
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.AVSpeechUtteranceDefaultSpeechRate
import platform.AVFAudio.setActive
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
class IosTtsEngine : TtsEngine {

    private val synthesizer = AVSpeechSynthesizer()
    private var onDoneCallback: (() -> Unit)? = null

    init {
        val audioSession = AVAudioSession.sharedInstance()
        audioSession.setCategory(AVAudioSessionCategoryPlayback, null)
        AVAudioSession.sharedInstance().setActive(true, null)

        synthesizer.delegate = object : NSObject(), AVSpeechSynthesizerDelegateProtocol {
            override fun speechSynthesizer(
                synthesizer: AVSpeechSynthesizer,
                didFinishSpeechUtterance: AVSpeechUtterance
            ) {
                onDoneCallback?.invoke()
            }
        }
    }

    override fun speak(text: String, speed: Float, onDone: () -> Unit) {
        onDoneCallback = onDone
        val utterance = AVSpeechUtterance(string = text)
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate * speed
        synthesizer.speakUtterance(utterance)
    }

    override fun stop() {
        synthesizer.stopSpeakingAtBoundary(platform.AVFAudio.AVSpeechBoundary.AVSpeechBoundaryImmediate)
    }

    override fun shutdown() {
        stop()
        onDoneCallback = null
    }
}