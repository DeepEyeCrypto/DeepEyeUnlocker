#!/bin/bash

KEYSTORE_NAME="deepeye-release.jks"
ALIAS="deepeye"
PASS="DeepEye2026"

echo "=== Generating Release Keystore ==="
if [ -f "$KEYSTORE_NAME" ]; then
    echo "Keystore already exists. Skipping generation."
else
    keytool -genkey -v -keystore $KEYSTORE_NAME -alias $ALIAS \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -storepass $PASS -keypass $PASS \
        -dname "CN=DeepEye Admin, OU=Security, O=DeepEye Inc, L=Dubai, S=Dubai, C=AE"
    echo "Keystore created: $KEYSTORE_NAME"
fi

echo ""
echo "=== GitHub Secrets Setup ==="
echo "Go to: https://github.com/DeepEyeCrypto/DeepEyeUnlocker/settings/secrets/actions"
echo "Add the following secrets:"
echo ""

# Convert to Base64
BASE64_KEY=$(base64 -i $KEYSTORE_NAME | tr -d '\n')

echo "1. ANDROID_KEYSTORE_BASE64"
echo "   Value: (Copy the string below)"
echo "$BASE64_KEY"
echo ""
echo "2. ANDROID_KEYSTORE_PASSWORD"
echo "   Value: $PASS"
echo ""
echo "3. ANDROID_KEY_ALIAS"
echo "   Value: $ALIAS"
echo ""
echo "4. ANDROID_KEY_PASSWORD"
echo "   Value: $PASS"

echo ""
echo "=== Done! Run this script locally to get the secrets. ==="
