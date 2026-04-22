package com.deepeye.otg.core.executor

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

sealed class ExecutionResult {
    data class Done(
        val exitCode: Int,
        val output: String,
        val success: Boolean,
    ) : ExecutionResult()
    data class Error(val msg: String) : ExecutionResult()
    data class Timeout(val msg: String) : ExecutionResult()
}

class CommandExecutor @Inject constructor(
    private val context: Context
) {
    suspend fun runAdb(
        serial: String,
        args: List<String>,
        timeoutMs: Long = 30_000L,
    ): ExecutionResult = withContext(Dispatchers.IO) {
        try {
            val cmd = mutableListOf("adb", "-s", serial) + args
            val proc = ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start()

            val output = StringBuilder()
            val reader = proc.inputStream.bufferedReader()

            val job = launch {
                reader.forEachLine {
                    output.appendLine(it)
                }
            }

            val finished = proc.waitFor(timeoutMs, TimeUnit.MILLISECONDS)

            if (!finished) {
                proc.destroyForcibly()
                return@withContext ExecutionResult.Timeout("Timed out after ${timeoutMs}ms")
            }

            job.join()
            val exit = proc.exitValue()
            ExecutionResult.Done(
                exitCode = exit,
                output = output.toString(),
                success = exit == 0,
            )
        } catch (e: Exception) {
            ExecutionResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun runFastboot(
        serial: String,
        args: List<String>,
    ): ExecutionResult = withContext(Dispatchers.IO) {
        val cmd = mutableListOf("fastboot", "-s", serial) + args
        runProcess(cmd)
    }

    private suspend fun runProcess(
        cmd: List<String>,
        timeoutMs: Long = 60_000L,
    ): ExecutionResult = withContext(Dispatchers.IO) {
        try {
            val proc = ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start()
            val out = proc.inputStream.bufferedReader().readText()
            val ok = proc.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            if (!ok) proc.destroyForcibly()
            ExecutionResult.Done(
                exitCode = proc.exitValue(),
                output = out,
                success = proc.exitValue() == 0,
            )
        } catch(e: Exception) {
            ExecutionResult.Error(e.message ?: "Process execution failed")
        }
    }
}
