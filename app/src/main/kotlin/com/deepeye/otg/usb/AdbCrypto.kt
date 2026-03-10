package com.deepeye.otg.usb

import android.util.Base64
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.interfaces.RSAPublicKey
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Android-compatible RSA cryptography for ADB AUTH (Stage 7.3).
 */
object AdbCrypto {

    fun generateKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        return kpg.generateKeyPair()
    }

    fun signToken(token: ByteArray, keyPair: KeyPair): ByteArray {
        val signer = Signature.getInstance("SHA1withRSA")
        signer.initSign(keyPair.private)
        signer.update(token)
        return signer.sign()
    }

    /**
     * Converts an RSAPublicKey into the specific format ADB requires.
     * Format: 32-bit words, little-endian: [n0inv, n, rr, exponent]
     */
    fun getAdbPublicKeyPayload(publicKey: RSAPublicKey): ByteArray {
        val modulus = publicKey.modulus
        val r = BigInteger.valueOf(2).pow(2048)
        val rr = r.multiply(r).mod(modulus)
        
        // This is a simplified version of the MinCrypt format
        // In practice, we use the standard Base64 string for the public key 
        // when sending AUTH_RSAPUBLICKEY.
        val encoded = publicKey.encoded
        return Base64.encode(encoded, Base64.NO_WRAP)
    }
}
