#!/usr/bin/env bash
# ============================================================================
# DeepEyeUnlocker - Complete Build Verification Script
# ============================================================================
# This script validates that all build artifacts are present and correct
# after the CI/CD pipeline execution.
# ============================================================================

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counters
PASS_COUNT=0
FAIL_COUNT=0
WARN_COUNT=0

# ─────────────────────────────────────────────────────────────
# Helper Functions
# ─────────────────────────────────────────────────────────────

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[PASS]${NC} $1"
    PASS_COUNT=$((PASS_COUNT + 1))
}

log_fail() {
    echo -e "${RED}[FAIL]${NC} $1"
    FAIL_COUNT=$((FAIL_COUNT + 1))
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
    WARN_COUNT=$((WARN_COUNT + 1))
}

check_file_exists() {
    local file_path="$1"
    local description="$2"
    
    if [ -f "$file_path" ]; then
        local file_size
        file_size=$(du -h "$file_path" | cut -f1)
        log_success "${description}: ${file_size}"
    else
        log_fail "${description}: File not found at ${file_path}"
    fi
}

check_directory_not_empty() {
    local dir_path="$1"
    local description="$2"
    
    if [ -d "$dir_path" ] && [ "$(ls -A "$dir_path" 2>/dev/null)" ]; then
        local file_count
        file_count=$(find "$dir_path" -type f | wc -l)
        log_success "${description}: ${file_count} files"
    else
        log_fail "${description}: Directory empty or not found at ${dir_path}"
    fi
}

# ─────────────────────────────────────────────────────────────
# Main Verification
# ─────────────────────────────────────────────────────────────

main() {
    echo "====================================================================="
    echo "DeepEyeUnlocker Build Verification"
    echo "====================================================================="
    echo ""
    
    local build_type="${1:-all}"
    local artifact_dir="${2:-.}"
    
    log_info "Build type: ${build_type}"
    log_info "Artifact directory: ${artifact_dir}"
    echo ""
    
    # ─────────────────────────────────────────────────────────
    # Verify React Frontend Build
    # ─────────────────────────────────────────────────────────
    log_info "Checking React Frontend Build..."
    check_file_exists "dist/index.html" "React index.html"
    check_directory_not_empty "dist/assets" "React assets"
    echo ""
    
    # ─────────────────────────────────────────────────────────
    # Verify Tauri Desktop Builds
    # ─────────────────────────────────────────────────────────
    if [[ "$build_type" == "all" || "$build_type" == "desktop" ]]; then
        log_info "Checking Tauri Desktop Builds..."
        
        # macOS ARM64
        log_info "  macOS ARM64:"
        check_directory_not_empty "target/aarch64-apple-darwin/release/bundle/dmg" "macOS ARM64 DMG"
        check_directory_not_empty "target/aarch64-apple-darwin/release/bundle/app" "macOS ARM64 App"
        
        # macOS x86_64
        log_info "  macOS x86_64:"
        check_directory_not_empty "target/x86_64-apple-darwin/release/bundle/dmg" "macOS x86_64 DMG"
        check_directory_not_empty "target/x86_64-apple-darwin/release/bundle/app" "macOS x86_64 App"
        
        # Linux
        log_info "  Linux:"
        check_directory_not_empty "target/x86_64-unknown-linux-gnu/release/bundle/appimage" "Linux AppImage"
        check_directory_not_empty "target/x86_64-unknown-linux-gnu/release/bundle/deb" "Linux DEB"
        
        # Windows
        log_info "  Windows:"
        check_directory_not_empty "target/x86_64-pc-windows-msvc/release/bundle/nsis" "Windows NSIS"
        
        echo ""
    fi
    
    # ─────────────────────────────────────────────────────────
    # Verify Android Builds
    # ─────────────────────────────────────────────────────────
    if [[ "$build_type" == "all" || "$build_type" == "android" ]]; then
        log_info "Checking Android Builds..."
        
        # Debug APK
        log_info "  Debug APK:"
        check_directory_not_empty "app/build/outputs/apk/debug" "Android Debug APK"
        
        # Release APK
        log_info "  Release APK:"
        check_directory_not_empty "app/build/outputs/apk/release" "Android Release APK"
        
        # Verify APK structure
        if ls app/build/outputs/apk/release/*.apk 1> /dev/null 2>&1; then
            local release_apk
            release_apk=$(ls app/build/outputs/apk/release/*.apk | head -n1)
            local apk_size
            apk_size=$(du -h "$release_apk" | cut -f1)
            log_success "Release APK size: ${apk_size}"
            
            # Verify APK contains expected components
            if command -v unzip &> /dev/null; then
                log_info "  Verifying APK contents..."
                if unzip -l "$release_apk" | grep -q "lib/arm64-v8a"; then
                    log_success "APK contains arm64-v8a native libs"
                else
                    log_warn "APK missing arm64-v8a native libs"
                fi
                
                if unzip -l "$release_apk" | grep -q "classes.dex"; then
                    log_success "APK contains DEX files"
                else
                    log_fail "APK missing DEX files"
                fi
            fi
        fi
        
        echo ""
    fi
    
    # ─────────────────────────────────────────────────────────
    # Verify Rust Components
    # ─────────────────────────────────────────────────────────
    if [[ "$build_type" == "all" || "$build_type" == "desktop" ]]; then
        log_info "Checking Rust Components..."
        
        check_file_exists "src-tauri/Cargo.toml" "Rust Cargo.toml"
        check_file_exists "src-tauri/src/main.rs" "Rust main.rs"
        check_file_exists "src-tauri/tauri.conf.json" "Tauri config"
        
        # Check for compiled Rust binary
        if [ -f "target/release/deep-eye-unlocker-desktop" ]; then
            log_success "Rust binary compiled successfully"
        elif [ -f "target/aarch64-apple-darwin/release/deep-eye-unlocker-desktop" ]; then
            log_success "Rust binary (macOS ARM64) compiled successfully"
        elif [ -f "target/x86_64-apple-darwin/release/deep-eye-unlocker-desktop" ]; then
            log_success "Rust binary (macOS x86_64) compiled successfully"
        else
            log_warn "Rust binary not found in expected locations"
        fi
        
        echo ""
    fi
    
    # ─────────────────────────────────────────────────────────
    # Verify Python Components
    # ─────────────────────────────────────────────────────────
    log_info "Checking Python Components..."
    
    if [ -d "src-tauri/python" ]; then
        check_directory_not_empty "src-tauri/python" "Python scripts (Tauri)"
        log_success "Python integration module present"
    else
        log_warn "Python scripts directory not found"
    fi
    
    if [ -d "app/src/main/python" ]; then
        check_directory_not_empty "app/src/main/python" "Python scripts (Android)"
        log_success "Python Android integration (Chaquopy) present"
    else
        log_warn "Python Android directory not found"
    fi
    
    echo ""
    
    # ─────────────────────────────────────────────────────────
    # Verify Build Configuration Files
    # ─────────────────────────────────────────────────────────
    log_info "Checking Build Configuration..."
    
    check_file_exists "build.gradle.kts" "Root build.gradle.kts"
    check_file_exists "app/build.gradle.kts" "App build.gradle.kts"
    check_file_exists "Cargo.toml" "Workspace Cargo.toml"
    check_file_exists "package.json" "package.json"
    check_file_exists "tsconfig.json" "TypeScript config"
    check_file_exists "vite.config.ts" "Vite config"
    
    echo ""
    
    # ─────────────────────────────────────────────────────────
    # Verify CI/CD Pipeline Files
    # ─────────────────────────────────────────────────────────
    log_info "Checking CI/CD Pipeline Configuration..."
    
    check_file_exists ".github/workflows/complete-pipeline.yml" "Complete pipeline"
    check_file_exists ".github/workflows/release.yml" "Release workflow"
    check_file_exists ".github/workflows/build.yml" "Build workflow"
    
    echo ""
    
    # ─────────────────────────────────────────────────────────
    # Summary
    # ─────────────────────────────────────────────────────────
    echo "====================================================================="
    echo "Verification Summary"
    echo "====================================================================="
    echo -e "${GREEN}Passed: ${PASS_COUNT}${NC}"
    echo -e "${RED}Failed: ${FAIL_COUNT}${NC}"
    echo -e "${YELLOW}Warnings: ${WARN_COUNT}${NC}"
    echo ""
    
    if [ $FAIL_COUNT -eq 0 ]; then
        echo -e "${GREEN}✓ All critical checks passed!${NC}"
        exit 0
    else
        echo -e "${RED}✗ ${FAIL_COUNT} check(s) failed. Review the output above.${NC}"
        exit 1
    fi
}

# Run main function
main "$@"
