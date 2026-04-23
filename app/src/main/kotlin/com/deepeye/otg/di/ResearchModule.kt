package com.deepeye.otg.di

import android.content.Context
import com.deepeye.otg.feature.forensics.ReportExporter
import com.deepeye.otg.hid.HidCorpusGenerator
import com.deepeye.otg.research.fuzz.CorpusManager
import com.deepeye.otg.research.fuzz.CrashClassifier
import com.deepeye.otg.research.fuzz.FuzzConfig
import com.deepeye.otg.research.fuzz.FuzzResult
import com.deepeye.otg.research.fuzz.FuzzTarget
import com.deepeye.otg.research.fuzz.FuzzTestCase
import com.deepeye.otg.research.fuzz.Mutator
import com.deepeye.otg.research.fuzz.MutatorFactory
import com.deepeye.otg.research.fuzz.ReproRecorder
import com.deepeye.otg.research.fuzz.TargetSurface
import com.deepeye.otg.security.RemediationGenerator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.security.SecureRandom
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ResearchModule {

    private fun researchDir(context: Context, vararg segments: String): File {
        var dir = File(context.filesDir, "research")
        segments.forEach { segment ->
            dir = File(dir, segment)
        }
        dir.mkdirs()
        return dir
    }

    @Provides
    @Singleton
    @Named("fuzzCorpusDir")
    fun provideFuzzCorpusDir(@ApplicationContext context: Context): File {
        return researchDir(context, "fuzz", "corpus")
    }

    @Provides
    @Singleton
    @Named("fuzzReproDir")
    fun provideFuzzReproDir(@ApplicationContext context: Context): File {
        return researchDir(context, "fuzz", "repro")
    }

    @Provides
    @Singleton
    @Named("fuzzSessionDir")
    fun provideFuzzSessionDir(@ApplicationContext context: Context): File {
        return researchDir(context, "fuzz", "sessions")
    }

    @Provides
    @Singleton
    @Named("forensicsReportsDir")
    fun provideForensicsReportsDir(@ApplicationContext context: Context): File {
        return researchDir(context, "forensics", "reports")
    }

    @Provides
    @Singleton
    @Named("hidCorpusDir")
    fun provideHidCorpusDir(@ApplicationContext context: Context): File {
        return researchDir(context, "hid", "corpus")
    }

    @Provides
    @Singleton
    fun provideSecureRandom(): SecureRandom {
        return SecureRandom()
    }

    @Provides
    @Singleton
    fun provideCrashClassifier(): CrashClassifier {
        return CrashClassifier()
    }

    @Provides
    @Singleton
    fun provideFuzzConfig(@Named("fuzzSessionDir") outputDir: File): FuzzConfig {
        return FuzzConfig(
            targetSurface = TargetSurface.USB_HID,
            outputDir = outputDir,
            notes = "Default Hilt-provided research harness configuration"
        )
    }

    @Provides
    @Singleton
    fun provideFuzzTarget(config: FuzzConfig): FuzzTarget {
        return object : FuzzTarget {
            override val name: String = "DefaultNoOpTarget"
            override val surface: TargetSurface = config.targetSurface

            override suspend fun initialize(): Boolean = true

            override suspend fun execute(testCase: FuzzTestCase): FuzzResult {
                return FuzzResult(
                    testCaseId = testCase.id,
                    crashed = false,
                    stdout = "NO_OP_TARGET:${testCase.inputData.size}"
                )
            }

            override suspend fun reset() = Unit

            override suspend fun teardown() = Unit
        }
    }

    @Provides
    @Singleton
    fun provideMutators(corpusManager: CorpusManager): List<Mutator> {
        return MutatorFactory.createDefaultSet(corpusManager)
    }

    @Provides
    @Singleton
    fun provideRemediationGenerator(@ApplicationContext context: Context): RemediationGenerator {
        return RemediationGenerator(researchDir(context, "security", "remediation"))
    }
}