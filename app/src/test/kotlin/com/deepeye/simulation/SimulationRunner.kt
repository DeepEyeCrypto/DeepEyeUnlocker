package com.deepeye.simulation

/**
 * Run all simulations and print detailed pass/fail report.
 * Run with: ./gradlew test --tests "*.SimulationRunner*"
 */
object SimulationReport {

    data class SimResult(
        val id: String,
        val device: String,
        val scenario: String,
        val passed: Boolean,
        val message: String,
        val durationMs: Long
    )

    fun printReport(results: List<SimResult>) {
        val passed = results.count { it.passed }
        val failed = results.count { !it.passed }
        val total = results.size

        println("""
╔════════════════════════════════════════════════════╗
║      DEEPEYE UNLOCKER — SIMULATION REPORT          ║
╚════════════════════════════════════════════════════╝
Total: $total | ✅ Passed: $passed | ❌ Failed: $failed
        """.trimIndent())

        // Group by protocol logic
        results.forEach { sim ->
            val icon = if (sim.passed) "✅" else "❌"
            println("  $icon [${sim.id}] ${sim.device} — ${sim.scenario} (${sim.durationMs}ms)")
            if (!sim.passed) println("       └─ ${sim.message}")
        }

        println("""
════════════════════════════════════════════════════
${if (failed == 0) "🎉 ALL SIMULATIONS PASSED — READY FOR REAL DEVICE"
  else "💥 $failed SIMULATIONS FAILED — FIX BEFORE TESTING ON REAL DEVICE"}
════════════════════════════════════════════════════
        """.trimIndent())
    }
}
