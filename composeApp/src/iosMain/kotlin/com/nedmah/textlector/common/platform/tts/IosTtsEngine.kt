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
    private var pendingCallbacks = mutableMapOf<AVSpeechUtterance, () -> Unit>()  // we need to bind particular cb to utterance to avoid race

    private val delegate = object : NSObject(), AVSpeechSynthesizerDelegateProtocol {
        override fun speechSynthesizer(
            synthesizer: AVSpeechSynthesizer,
            didFinishSpeechUtterance: AVSpeechUtterance
        ) {
            pendingCallbacks.remove(didFinishSpeechUtterance)?.invoke()
        }
    }

    init {
        val audioSession = AVAudioSession.sharedInstance()
        audioSession.setCategory(AVAudioSessionCategoryPlayback, null)
        AVAudioSession.sharedInstance().setActive(true, null)

        synthesizer.delegate = delegate
    }

    override fun speak(text: String, speed: Float, onDone: () -> Unit) {
        val utterance = AVSpeechUtterance(string = text)
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate * speed
        pendingCallbacks[utterance] = onDone
        synthesizer.speakUtterance(utterance)
    }

    override fun stop() {
        pendingCallbacks.clear()
        synthesizer.stopSpeakingAtBoundary(platform.AVFAudio.AVSpeechBoundary.AVSpeechBoundaryImmediate)
    }

    override fun shutdown() {
        stop()
    }
}