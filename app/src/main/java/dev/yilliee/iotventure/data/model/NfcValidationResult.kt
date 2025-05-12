package dev.yilliee.iotventure.data.model

sealed class NfcValidationResult {
    object Processing : NfcValidationResult()
    data class Valid(val challenge: Challenge) : NfcValidationResult()
    object Invalid : NfcValidationResult()
    object AlreadySolved : NfcValidationResult()
}
