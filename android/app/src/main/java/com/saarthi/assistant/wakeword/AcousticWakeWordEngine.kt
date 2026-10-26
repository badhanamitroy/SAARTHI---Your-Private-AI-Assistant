package com.saarthi.assistant.wakeword

/**
 * Architectural interface detailing how raw audio is supplied to an acoustic inference engine.
 *
 * Technical Reality & Gap Documentation:
 * Acoustic keyword spotting (KWS) requires:
 * 1. An AudioRecord instance continuously sampling at 16,000 Hz, 16-bit Mono PCM.
 * 2. An audio ring buffer slicing samples into windows (e.g. 1280 samples = 80ms stride).
 * 3. Feature extraction (e.g. 32-bin or 40-bin Mel-Frequency spectrogram or melspectrogram).
 * 4. An embedded neural network inference runtime (e.g. TensorFlow Lite Micro C++ or ONNX Runtime Mobile).
 * 5. A trained model file (e.g. `hey_saarthi.tflite`, typically 1.5MB - 4.5MB).
 *
 * In the current SAARTHI release (APK size 1.35 MB, under the strict 5.0 MB budget),
 * no proprietary or heavy third-party acoustic model binary is bundled.
 * This interface defines the contract for feeding audio frames to the inference runtime
 * when the model asset is integrated.
 */
interface AcousticWakeWordEngine {

    /** Expected sample rate in Hz (standard is 16000). */
    val sampleRateHz: Int get() = 16000

    /** Expected buffer size per audio frame (e.g. 1280 shorts = 80ms). */
    val frameSizeSamples: Int get() = 1280

    /**
     * Process a chunk of raw 16-bit linear PCM audio samples.
     * @param pcmBuffer Array of PCM 16-bit shorts.
     * @param offset Starting index in buffer.
     * @param length Number of valid samples in buffer.
     * @return Confidence score between 0.0f and 1.0f.
     */
    fun processFrame(pcmBuffer: ShortArray, offset: Int, length: Int): Float

    /** Reset internal recurrent / hidden state buffers. */
    fun reset()

    /** Release native model tensors and memory. */
    fun release()
}
