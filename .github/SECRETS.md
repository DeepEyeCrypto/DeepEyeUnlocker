# DeepEyeUnlocker Desktop Secrets Configuration

This project requires several secrets configured in the GitHub repository (`Settings > Secrets and variables > Actions`) for the CI/CD pipeline to function correctly.

## Always Required

| Secret Name                          | Description                        | Setup Command / Origin                                     |
| ------------------------------------ | ---------------------------------- | ---------------------------------------------------------- |
| `TAURI_SIGNING_PRIVATE_KEY`          | Tauri updater signing key          | `npm run tauri signer generate -- -w ~/.tauri/deepeye.key` |
| `TAURI_SIGNING_PRIVATE_KEY_PASSWORD` | Password for the signing key above | Generated during the step above                            |
| `GITHUB_TOKEN`                       | Auto-provided                      | No setup needed                                            |

## macOS App Notarization & Code Signing

| Secret Name                  | Description                          | Setup Command / Origin                                  |
| ---------------------------- | ------------------------------------ | ------------------------------------------------------- |
| `APPLE_CERTIFICATE`          | Developer ID Application Certificate | Base64 of `.p12` file (`base64 -i cert.p12 \| pbcopy`)  |
| `APPLE_CERTIFICATE_PASSWORD` | Password for the `.p12` file         | Export password set in Keychain Access                  |
| `APPLE_SIGNING_IDENTITY`     | The string identity name             | E.g., `Developer ID Application: Company Name (TEAMID)` |
| `APPLE_ID`                   | Apple ID email address               | Apple Developer Account email                           |
| `APPLE_PASSWORD`             | App-specific password                | Generated at `appleid.apple.com`                        |
| `APPLE_TEAM_ID`              | 10-character team ID                 | E.g., `ABC123XYZ0`                                      |
| `KEYCHAIN_PASSWORD`          | Arbitrary temp password              | Just use a random string like `ci-keychain-pass`        |

## Windows Code Signing (Optional)

| Secret Name                    | Description                  | Setup Command / Origin |
| ------------------------------ | ---------------------------- | ---------------------- |
| `WINDOWS_CERTIFICATE`          | Authenticode Certificate     | Base64 of `.pfx` file  |
| `WINDOWS_CERTIFICATE_PASSWORD` | Password for the `.pfx` file | Export password        |

_Note: If no Windows cert is provided, builds will still work but SmartScreen will show a warning on first run._

## License Backend (Optional if applicable)

| Secret Name              | Description                                |
| ------------------------ | ------------------------------------------ |
| `LICENSE_VALIDATION_URL` | Backend endpoint for online license checks |
| `LICENSE_API_KEY`        | API key for validation calls               |
