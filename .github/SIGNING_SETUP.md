# Android APK Signing Setup for GitHub Actions

This document explains how to set up APK signing for automated releases in GitHub Actions.

## Prerequisites

1. A keystore file for signing your Android app
2. Repository secrets configured in GitHub

## Creating a Keystore (if you don't have one)

```bash
keytool -genkey -v -keystore my-release-key.keystore -alias my-key-alias -keyalg RSA -keysize 2048 -validity 10000
```

Follow the prompts to create your keystore file.

## Setting up GitHub Secrets

Go to your repository settings → Secrets and variables → Actions, and add these secrets:

### Required Secrets:

1. **SIGNING_KEY**
   ```bash
   # Convert your keystore to base64
   base64 -i my-release-key.keystore | pbcopy  # macOS
   # or
   base64 -w 0 my-release-key.keystore        # Linux
   ```
   Copy the base64 string and add it as a secret.

2. **ALIAS**
   - The alias you used when creating the keystore (e.g., `my-key-alias`)

3. **KEY_STORE_PASSWORD**
   - The password for the keystore file

4. **KEY_PASSWORD**
   - The password for the key alias (often the same as keystore password)

## GitHub Actions Workflows

### Release Workflow (`android-release.yml`)

**Triggers:**
- Push to tags starting with `v` (e.g., `v1.0.0`, `v2.1.3`)
- Manual workflow dispatch

**What it does:**
- Builds debug and release APKs
- Signs the release APK (only for tagged releases)
- Creates GitHub releases with APK files
- Runs tests and lint checks

### CI Workflow (`android-ci.yml`)

**Triggers:**
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop` branches

**What it does:**
- Runs unit tests
- Performs lint checks
- Builds debug APK
- Uploads artifacts for download

## Creating a Release

### Method 1: Git Tags (Recommended)

```bash
# Create and push a tag
git tag v1.0.0
git push origin v1.0.0
```

This will automatically trigger the release workflow and create a GitHub release.

### Method 2: Manual Trigger

1. Go to Actions tab in your GitHub repository
2. Click on "Android Release Build" workflow
3. Click "Run workflow"
4. Select release type (draft/prerelease/release)
5. Click "Run workflow"

## Release Assets

Each release will include:

- **AndroidCamera-debug.apk**: Debug version for testing
- **AndroidCamera-release.apk**: Signed release version (when secrets are configured)
- **AndroidCamera-release-unsigned.apk**: Unsigned release version (fallback)

## Version Naming Convention

Use semantic versioning for tags:
- `v1.0.0` - Major release
- `v1.1.0` - Minor release with new features
- `v1.1.1` - Patch release with bug fixes
- `v1.2.0-alpha1` - Pre-release (will be marked as prerelease)
- `v1.2.0-beta1` - Beta release (will be marked as prerelease)
- `v1.2.0-rc1` - Release candidate (will be marked as prerelease)

## Troubleshooting

### APK Not Signed

**Problem**: The release APK is not signed (shows as unsigned)

**Solution**: Ensure all four secrets (SIGNING_KEY, ALIAS, KEY_STORE_PASSWORD, KEY_PASSWORD) are correctly set in repository secrets.

### Build Fails with Gradle Error

**Problem**: Gradle build fails

**Solution**:
- Check if `gradlew` has execute permissions
- Verify Java version compatibility
- Check dependency versions in `build.gradle.kts`

### Invalid Keystore Format

**Problem**: Signing fails with keystore format error

**Solution**: Make sure the base64 encoding of the keystore is correct:
```bash
# Verify base64 encoding
base64 -d <<< "YOUR_BASE64_STRING" > test-keystore.keystore
# Test the keystore
keytool -list -keystore test-keystore.keystore
```

## Security Notes

- Never commit keystore files to your repository
- Use strong passwords for keystores
- Regularly rotate signing keys for production apps
- Consider using GitHub's dependency review features
- Limit repository access to trusted collaborators

## Support

If you encounter issues:
1. Check the Actions logs for detailed error messages
2. Verify all secrets are correctly configured
3. Test the build locally before pushing
4. Review the workflow files for any syntax errors