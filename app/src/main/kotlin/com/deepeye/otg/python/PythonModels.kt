package com.deepeye.otg.python

import org.json.JSONObject

sealed class MtkChipResult {
    data class Success(val hwCode: Int, val chipName: String) : MtkChipResult()
    data class Error(val message: String) : MtkChipResult()
}

sealed class DaValidationResult {
    data class Valid(val sha256: String, val info: JSONObject) : DaValidationResult()
    data class Invalid(val error: String?) : DaValidationResult()
}

data class ImeiValidationResult(
    val isValid: Boolean,
    val tac: String,
    val manufacturer: String
)
