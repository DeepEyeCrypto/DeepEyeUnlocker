package com.deepeye.otg.research.fuzz

import javax.inject.Inject
import javax.inject.Named

import android.util.Log
import java.io.File
import java.security.SecureRandom

// ──────────────────────────────────────────────────────────────
// Corpus Manager — Seed + Crash Input Management
// DeepEye OTG — Research Module (Part 4)
// ──────────────────────────────────────────────────────────────

private const val TAG = "CorpusManager"

/**
 * Manages the fuzzing corpus:
 * - Seed loading from disk
 * - Runtime corpus tracking
 * - Crash input persistence
 * - Random input generation
 * - Corpus minimization helpers
 *
 * Directory structure:
 * ```
 * <baseDir>/
 *   seeds/           ← initial seed inputs
 *   corpus/          ← runtime-discovered inputs
 *   crashes/         ← inputs that triggered crashes
 *     <bucket>/      ← grouped by crash bucket
 *   minimized/       ← minimized crash inputs
 * ```
 */
class CorpusManager @Inject constructor(@Named("fuzzCorpusDir") private val baseDir: File) {

    private val seedDir = File(baseDir, "seeds")
    private val corpusDir = File(baseDir, "corpus")
    private val crashDir = File(baseDir, "crashes")
    private val minimizedDir = File(baseDir, "minimized")

    private val rng = SecureRandom()

    // In-memory corpus for fast random picking
    private val inMemoryCorpus = mutableListOf<FuzzTestCase>()

    init {
        seedDir.mkdirs()
        corpusDir.mkdirs()
        crashDir.mkdirs()
        minimizedDir.mkdirs()
    }

    // ── Seed Management ─────────────────────────────────────────

    /**
     * Load seed inputs from the seeds/ directory.
     * Each file becomes one seed test case.
     */
    fun loadSeeds(): List<FuzzTestCase> {
        val seeds = seedDir.listFiles()?.filter { it.isFile }?.mapIndexed { idx, file ->
            FuzzTestCase(
                id = "seed_$idx",
                inputData = file.readBytes(),
                generation = 0,
                metadata = mapOf("source" to "seed", "filename" to file.name)
            )
        } ?: emptyList()

        // Add seeds to in-memory corpus
        inMemoryCorpus.clear()
        inMemoryCorpus.addAll(seeds)

        Log.i(TAG, "Loaded ${seeds.size} seed(s) from ${seedDir.absolutePath}")
        return seeds
    }

    /**
     * Generate random seed inputs if none exist.
     */
    fun generateRandomSeeds(
        count: Int = 10,
        maxSize: Int = 4096,
        minSize: Int = 1
    ): List<FuzzTestCase> {
        val generated = (0 until count).map { idx ->
            val size = minSize + rng.nextInt(maxSize - minSize + 1)
            val data = ByteArray(size)
            rng.nextBytes(data)

            val testCase = FuzzTestCase(
                id = "random_seed_$idx",
                inputData = data,
                generation = 0,
                metadata = mapOf("source" to "random")
            )

            // Persist to seeds dir
            File(seedDir, "random_seed_$idx.bin").writeBytes(data)

            testCase
        }

        inMemoryCorpus.addAll(generated)
        Log.i(TAG, "Generated $count random seeds (${minSize}–${maxSize} bytes)")
        return generated
    }

    /**
     * Add a specific seed input.
     */
    fun addSeed(name: String, data: ByteArray) {
        File(seedDir, name).writeBytes(data)
        inMemoryCorpus.add(
            FuzzTestCase(
                id = "seed_${name.hashCode()}",
                inputData = data,
                generation = 0,
                metadata = mapOf("source" to "manual", "filename" to name)
            )
        )
    }

    // ── Runtime Corpus ──────────────────────────────────────────

    /**
     * Add a test case to the runtime corpus.
     */
    fun addToCorpus(testCase: FuzzTestCase) {
        inMemoryCorpus.add(testCase)

        // Persist if corpus is small enough
        if (inMemoryCorpus.size <= 10_000) {
            File(corpusDir, "${testCase.id}.bin").writeBytes(testCase.inputData)
        }
    }

    /**
     * Pick a random test case from the corpus.
     */
    fun pickRandom(): FuzzTestCase? {
        if (inMemoryCorpus.isEmpty()) return null
        return inMemoryCorpus[rng.nextInt(inMemoryCorpus.size)]
    }

    /**
     * Current corpus size.
     */
    fun corpusSize(): Int = inMemoryCorpus.size

    /**
     * Get all corpus entries.
     */
    fun getAllEntries(): List<FuzzTestCase> = inMemoryCorpus.toList()

    // ── Crash Input Management ──────────────────────────────────

    /**
     * Save an input that triggered a crash.
     *
     * @param testCase the input that crashed
     * @param classified the classified crash result
     */
    fun saveCrashInput(
        testCase: FuzzTestCase,
        classified: CrashClassifier.ClassifiedCrash
    ) {
        val bucketDir = File(crashDir, classified.bucket.replace(Regex("[^a-zA-Z0-9_-]"), "_"))
        bucketDir.mkdirs()

        // Save the raw input
        File(bucketDir, "${testCase.id}.bin").writeBytes(testCase.inputData)

        // Save metadata
        val meta = buildString {
            appendLine("# Crash Report")
            appendLine("test_case_id: ${testCase.id}")
            appendLine("bucket: ${classified.bucket}")
            appendLine("signature: ${classified.signature}")
            appendLine("severity: ${classified.severity}")
            appendLine("component: ${classified.component}")
            appendLine("crash_type: ${classified.crashType}")
            appendLine("timestamp: ${testCase.createdAt}")
            appendLine("input_size: ${testCase.inputData.size}")
            appendLine("mutation_strategy: ${testCase.mutationStrategy ?: "N/A"}")
            appendLine("parent_id: ${testCase.parentId ?: "N/A"}")
            appendLine("generation: ${testCase.generation}")
            appendLine()
            appendLine("# Crash Details")
            appendLine(classified.details)
        }
        File(bucketDir, "${testCase.id}_report.txt").writeText(meta)

        Log.i(TAG, "Saved crash input: ${bucketDir.absolutePath}/${testCase.id}.bin")
    }

    /**
     * List all crash buckets with their input counts.
     */
    fun listCrashBuckets(): Map<String, Int> {
        return crashDir.listFiles()?.filter { it.isDirectory }?.associate { dir ->
            dir.name to (dir.listFiles()?.count { it.extension == "bin" } ?: 0)
        } ?: emptyMap()
    }

    /**
     * Load crash inputs for a specific bucket.
     */
    fun loadCrashInputs(bucket: String): List<FuzzTestCase> {
        val bucketDir = File(crashDir, bucket)
        if (!bucketDir.exists()) return emptyList()

        return bucketDir.listFiles()?.filter { it.extension == "bin" }?.mapIndexed { idx, file ->
            FuzzTestCase(
                id = file.nameWithoutExtension,
                inputData = file.readBytes(),
                metadata = mapOf("source" to "crash_replay", "bucket" to bucket)
            )
        } ?: emptyList()
    }

    // ── Corpus Stats ────────────────────────────────────────────

    data class CorpusStats(
        val seedCount: Int,
        val corpusCount: Int,
        val crashBuckets: Int,
        val totalCrashInputs: Int,
        val totalDiskBytes: Long
    )

    fun getStats(): CorpusStats {
        val seedCount = seedDir.listFiles()?.filter { it.isFile }?.size ?: 0
        val corpusCount = inMemoryCorpus.size
        val buckets = listCrashBuckets()
        val totalCrashInputs = buckets.values.sum()
        val diskBytes = baseDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

        return CorpusStats(seedCount, corpusCount, buckets.size, totalCrashInputs, diskBytes)
    }
}
