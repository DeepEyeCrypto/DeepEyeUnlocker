using System;
using System.IO;
using System.Security.Cryptography;
using System.Text;
using DeepEyeUnlocker.Core;

namespace DeepEyeUnlocker.Core.Security
{
    /// <summary>
    /// DeepEye Secure Vault (v5.1): Implements AES-256-GCM encryption for hardware dumps.
    /// Provides zero-knowledge data protection for the Neural Nexus.
    /// </summary>
    public class VaultEngine
    {
        private const int KeySize = 32; // 256 bits
        private const int NonceSize = 12; // 96 bits for GCM
        private const int TagSize = 16; // 128 bits for GCM

        public static byte[] EncryptPartition(byte[] data, string masterPass)
        {
            Logger.Info("[VAULT] Initiating military-grade encryption for partition dump...");
            
            using var aes = new AesGcm(DeriveKey(masterPass), TagSize);
            var nonce = new byte[NonceSize];
            RandomNumberGenerator.Fill(nonce);

            var ciphertext = new byte[data.Length];
            var tag = new byte[TagSize];

            aes.Encrypt(nonce, data, ciphertext, tag);

            // Structure: [Nonce][Tag][Ciphertext]
            var result = new byte[NonceSize + TagSize + ciphertext.Length];
            Buffer.BlockCopy(nonce, 0, result, 0, NonceSize);
            Buffer.BlockCopy(tag, 0, result, NonceSize, TagSize);
            Buffer.BlockCopy(ciphertext, 0, result, NonceSize + TagSize, ciphertext.Length);

            Logger.Success($"[VAULT] Data sealed. Original: {data.Length} bytes | Sealed: {result.Length} bytes");
            return result;
        }

        public static byte[] DecryptPartition(byte[] encryptedData, string masterPass)
        {
            Logger.Info("[VAULT] Breaking seal on encrypted partition...");
            
            using var aes = new AesGcm(DeriveKey(masterPass), TagSize);
            
            var nonce = new byte[NonceSize];
            var tag = new byte[TagSize];
            var ciphertext = new byte[encryptedData.Length - NonceSize - TagSize];

            Buffer.BlockCopy(encryptedData, 0, nonce, 0, NonceSize);
            Buffer.BlockCopy(encryptedData, NonceSize, tag, 0, TagSize);
            Buffer.BlockCopy(encryptedData, NonceSize + TagSize, ciphertext, 0, ciphertext.Length);

            var plaintext = new byte[ciphertext.Length];
            aes.Decrypt(nonce, ciphertext, tag, plaintext);

            Logger.Success("[VAULT] Decryption successful. Integrity verified.");
            return plaintext;
        }

        private static byte[] DeriveKey(string password)
        {
            // In a real scenario, use PBKDF2 with a salt.
            // This is a placeholder for the v5.1.0-alpha foundation.
            using var sha256 = SHA256.Create();
            return sha256.ComputeHash(Encoding.UTF8.GetBytes(password));
        }
    }
}
