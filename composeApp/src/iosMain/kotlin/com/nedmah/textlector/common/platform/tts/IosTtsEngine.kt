package com.nedmah.textlector.common.platform.tts

import com.nedmah.textlector.domain.model.VoiceModel
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechSynthesizerDelegateProtocol
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.AVSpeechUtteranceDefaultSpeechRate
import platform.AVFAudio.setActive
import platform.darwin.NSObject
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
class IosTtsEngine : TtsEngine {

    private val synthesizer = AVSpeechSynthesizer()
    private var currentContinuation: CancellableContinuation<Unit>? = null
    private var activeUtterance: AVSpeechUtterance? = null

    private val delegate = object : NSObject(), AVSpeechSynthesizerDelegateProtocol {
        override fun speechSynthesizer(
            synthesizer: AVSpeechSynthesizer,
            didFinishSpeechUtterance: AVSpeechUtterance
        ) {
            if (didFinishSpeechUtterance === activeUtterance) {
                currentContinuation?.resume(Unit)
                currentContinuation = null
                activeUtterance = null
            }
        }
    }

    init {
        val audioSession = AVAudioSession.sharedInstance()
        audioSession.setCategory(AVAudioSessionCategoryPlayback, null)
        AVAudioSession.sharedInstance().setActive(true, null)
        synthesizer.delegate = delegate
    }

    override suspend fun speak(text: String, speed: Float) {
        suspendCancellableCoroutine { continuation ->
            val utterance = AVSpeechUtterance(string = text)
            utterance.rate = AVSpeechUtteranceDefaultSpeechRate * speed

            activeUtterance = utterance
            currentContinuation = continuation

            synthesizer.speakUtterance(utterance)

            continuation.invokeOnCancellation {
                activeUtterance = null
                currentContinuation = null
                synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
            }
        }
    }

    override suspend fun loadVoice(model: VoiceModel) {
        TODO("Not yet implemented")
    }

    override fun stop() {
        currentContinuation = null
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
    }

    override fun shutdown() {
        stop()
    }
}