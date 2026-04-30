@file:OptIn(ExperimentalTime::class, ExperimentalAtomicApi::class, ExperimentalAtomicApi::class)

package com.nedmah.textlector.common.platform.tts

import com.nedmah.textlector.domain.model.Paragraph
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val TTS_QUEUE_LOGS = true

private fun ttsLog(message: String) {
    if (TTS_QUEUE_LOGS) println(
        "[TtsQueue ${
            Clock.System.now().toEpochMilliseconds() % 100_000
        }ms] $message"
    )
}

/**
 * Buffer for audio pre-generation (Piper/sherpa-onnx).
 *
 * Each paragraph index has its own CompletableDeferred<ByteArray>,
 * so getAudio(i) only waits for a specific element, not the entire batch.
 *
 * Deferred lifecycle:
 * pending[i] appears → generation in progress → deferred.complete(audio)
 * getAudio(i) called before complete → suspend until complete
 * getAudio(i) called after complete → returns immediately
 */
class TtsQueue(
    val engine: SherpaOnnxTtsEngine,
    val bufferSize: Int = 1
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mutex = Mutex()

    // key - paragraph index. value - deferred with audio (in-progress oor completed).
    private val pending = mutableMapOf<Int, CompletableDeferred<ByteArray>>()


    private val generationId = AtomicInt(0)

    /**
     * calls AFTER we began to play [currentIndex].
     * Launches background operations for [bufferSize] paragraphs.
     * If paragraph already pending — skip.
     */
    fun prefetchAhead(currentIndex: Int, paragraphs: List<Paragraph>, speed: Float) {
        scope.launch {
            val capturedGeneration = generationId.load()
            evictStale(currentIndex)

            val from = currentIndex + 1
            val until = minOf(from + bufferSize, paragraphs.size)

            if (from >= paragraphs.size) {
                ttsLog("prefetchAhead($currentIndex): end of the doc")
                return@launch
            }
            ttsLog("prefetchAhead($currentIndex): buffering paragraphs [$from, ${until - 1}]")

            for (i in from until until) {

                val isStale = generationId.load() != capturedGeneration
                if (isStale) {
                    ttsLog("  paragraph[$i]: is old (clear called), cancelling")
                    break
                }

                val (deferred, shouldGenerate) = acquireSlot(i)
                if (!shouldGenerate) {
                    ttsLog("  paragraph[$i]: already in buffer (HIT), skip")
                    continue
                }  // already in buffer or generating

                ttsLog("  paragraph[$i]: begin generating...")
                val startMs = Clock.System.now().toEpochMilliseconds()

                try {
                    val audio = engine.generate(paragraphs[i].text, speed)
                    if (audio.isEmpty()) {
                        ttsLog("  paragraph[$i]: generate returned empty array (stop was called), cancelling")
                        deferred.cancel()
                        break
                    }

                    val elapsed = Clock.System.now().toEpochMilliseconds() - startMs
                    val stillValid = generationId.load() == capturedGeneration
                    if (stillValid) {
                        deferred.complete(audio)
                        ttsLog("  paragraph[$i]: ready in ${elapsed}ms, size=${audio.size}b")
                    } else {
                        deferred.cancel()
                        ttsLog("  paragraph[$i]: ready in ${elapsed}ms, but is old - calcelling")
                        break
                    }
                } catch (e: CancellationException) {
                    deferred.cancel()
                    ttsLog("  paragraph[$i]: cancelled (CancellationException)")
                    break  // stop fully on cancel
                } catch (e: Exception) {
                    deferred.completeExceptionally(e) // continue on next paragraph
                    ttsLog("  paragraph[$i]: error — ${e.message}")
                }
            }
        }
    }

    /**
     * Returns audio for [index].
     * - If deferred already completed (is ready from prefetch) — returns in instant.
     * - If generating in-progress — suspend until finish.
     * - If paragraph is not in pending — generates now (cache miss).
     */
    suspend fun getAudio(index: Int, text: String, speed: Float): ByteArray {
        val (deferred, shouldGenerate) = acquireSlot(index)

        if (shouldGenerate) {
            ttsLog("getAudio($index): CACHE MISS — generating sync")
            val capturedGeneration = generationId.load()
            val startMs = Clock.System.now().toEpochMilliseconds()
            try {
                val audio = engine.generate(text, speed)
                if (audio.isEmpty()) {
                    deferred.cancel()
                    ttsLog("getAudio($index): generate returned empty array (stop was called)")
                    throw CancellationException("generate() returned empty audio")
                }
                val elapsed = Clock.System.now().toEpochMilliseconds() - startMs
                val isStale =
                    generationId.load() != capturedGeneration  // if while generating clear was called
                if (isStale) {
                    deferred.cancel()
                    ttsLog("getAudio($index): ready in ${elapsed}ms, but is old - calcelling")
                    throw CancellationException("Generation invalidated by clear()")
                }
                val completed = deferred.complete(audio)
                ttsLog("getAudio($index): ready in ${elapsed}ms, size=${audio.size}b, completed=$completed")

                mutex.withLock { pending.remove(index) }
                return audio
            } catch (e: Exception) {
                if (!deferred.isCompleted) deferred.completeExceptionally(e)
                throw e
            }
        } else {
            val audio = try {
                if (!deferred.isCompleted) {
                    val startMs = Clock.System.now().toEpochMilliseconds()
                    val result = deferred.await()
                    ttsLog("getAudio($index): waited for ${Clock.System.now().toEpochMilliseconds() - startMs}ms")
                    result
                } else {
                    ttsLog("getAudio($index): CACHE HIT - return instant")
                    deferred.await()
                }
            } catch (e: Exception) {
                mutex.withLock { pending.remove(index) }
                throw e
            }
            mutex.withLock { pending.remove(index) }
            return audio
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getCachedAudio(index: Int): ByteArray? {
        val deferred = pending[index] ?: return null
        if (!deferred.isCompleted || deferred.isCancelled) return null
        return runCatching { deferred.getCompleted() }.getOrNull()
    }

    fun clear() {
        generationId.addAndFetch(1)
        scope.launch {
            mutex.withLock {
                pending.values.forEach { it.cancel() }
                pending.clear()
            }
        }
    }

    suspend fun clearSync() {
        generationId.addAndFetch(1)
        mutex.withLock {
            pending.values.forEach { it.cancel() }
            pending.clear()
        }
    }

    fun shutdown() {
        scope.coroutineContext[Job]?.cancel()
        clear()
    }

    /**
     * Returns existing deferred (shouldGenerate=false),
     * or creates new slot (shouldGenerate=true).
     */
    private suspend fun acquireSlot(index: Int): Pair<CompletableDeferred<ByteArray>, Boolean> =
        mutex.withLock {
            val existing = pending[index]
            if (existing != null && !existing.isCancelled) {
                existing to false
            } else {
                val fresh = CompletableDeferred<ByteArray>()
                pending[index] = fresh
                fresh to true
            }
        }

    /**
     * Removes useless indexes (behind currentIndex).
     */
    private suspend fun evictStale(currentIndex: Int) {
        mutex.withLock {
            pending.keys
                .filter { it < currentIndex }
                .forEach { key ->
                    pending[key]?.cancel()
                    pending.remove(key)
                }
        }
    }
}