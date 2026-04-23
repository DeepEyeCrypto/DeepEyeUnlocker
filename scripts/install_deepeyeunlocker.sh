#!/bin/bash
# ============================================================================
# DeepEyeUnlocker - Installation Script for macOS and Android
# ============================================================================
# This script installs DeepEyeUnlocker on both platforms:
# - macOS: Installs the .pkg installer
# - Android: Installs the .apk via ADB
# ============================================================================

set -euo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# Project root
PROJECT_ROOT="/Users/enayat/Documents/DeepEyeUnlocker"

# ─────────────────────────────────────────────────────────────
# Helper Functions
# ─────────────────────────────────────────────────────────────

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_header() {
    echo ""
    echo -e "${CYAN}════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  $1${NC}"
    echo -e "${CYAN}════════════════════════════════════════════════════════${NC}"
    echo ""
}

# ─────────────────────────────────────────────────────────────
# macOS Installation
# ─────────────────────────────────────────────────────────────

install_macos() {
    log_header "macOS Installation"
    
    PKG_PATH="${PROJECT_ROOT}/target/x86_64-apple-darwin/release/bundle/pkg/DeepEyeUnlocker_2027.18.1_x86_64.pkg"
    
    # Check if PKG exists
    if [ ! -f "$PKG_PATH" ]; then
        log_error "PKG installer not found at: $PKG_PATH"
        log_info "Building PKG installer..."
        cd "$PROJECT_ROOT"
        bash ./scripts/build_macos_pkg.sh --skip-build --target x86_64-apple-darwin
    fi
    
    if [ ! -f "$PKG_PATH" ]; then
        log_error "Failed to create PKG installer"
        exit 1
    fi
    
    log_info "PKG installer: $(du -h "$PKG_PATH" | cut -f1)"
    log_info "Starting macOS installation..."
    echo ""
    
    # Check if DeepEyeUnlocker is already installed
    if [ -d "/Applications/DeepEyeUnlocker.app" ]; then
        log_warn "DeepEyeUnlocker is already installed in /Applications/"
        echo -e "${YELLOW}Do you want to reinstall? (y/N)${NC}"
        read -r answer
        if [[ ! "$answer" =~ ^[Yy]$ ]]; then
            log_info "Installation cancelled"
            return 0
        fi
        
        # Remove existing installation
        log_info "Removing existing installation..."
        sudo rm -rf /Applications/DeepEyeUnlocker.app
        log_success "Previous version removed"
    fi
    
    # Install PKG
    log_info "Installing DeepEyeUnlocker.pkg..."
    echo ""
    
    sudo installer -pkg "$PKG_PATH" -target /
    
    if [ $? -eq 0 ]; then
        log_success "DeepEyeUnlocker installed successfully!"
        echo ""
        log_info "Installation location: /Applications/DeepEyeUnlocker.app"
        echo ""
        
        # Verify installation
        if [ -d "/Applications/DeepEyeUnlocker.app" ]; then
            log_success "Verification: Application found in /Applications/"
            echo ""
            log_info "You can now:"
            echo -e "  1. Launch from ${CYAN}/Applications/DeepEyeUnlocker.app${NC}"
            echo -e "  2. Or run: ${CYAN}open /Applications/DeepEyeUnlocker.app${NC}"
            echo ""
            
            # Ask to launch
            echo -e "${YELLOW}Do you want to launch DeepEyeUnlocker now? (y/N)${NC}"
            read -r launch_answer
            if [[ "$launch_answer" =~ ^[Yy]$ ]]; then
                log_info "Launching DeepEyeUnlocker..."
                open /Applications/DeepEyeUnlocker.app
                log_success "Application launched!"
            fi
        else
            log_error "Installation verification failed - app not found"
            exit 1
        fi
    else
        log_error "Installation failed"
        exit 1
    fi
}

# ─────────────────────────────────────────────────────────────
# Android Installation
# ─────────────────────────────────────────────────────────────

install_android() {
    log_header "Android Installation"
    
    APK_PATH="${PROJECT_ROOT}/app/build/outputs/apk/release/app-release.apk"
    
    # Check if APK exists
    if [ ! -f "$APK_PATH" ]; then
        log_error "APK not found at: $APK_PATH"
        log_info "Building APK..."
        cd "$PROJECT_ROOT"
        ./gradlew assembleRelease
    fi
    
    if [ ! -f "$APK_PATH" ]; then
        log_error "Failed to build APK"
        exit 1
    fi
    
    log_info "APK file: $(du -h "$APK_PATH" | cut -f1)"
    echo ""
    
    # Check ADB
    if ! command -v adb &> /dev/null; then
        log_error "ADB not found. Please install Android SDK Platform Tools"
        exit 1
    fi
    
    # Check for connected devices
    log_info "Checking for Android devices..."
    DEVICES=$(adb devices | grep -v "List of devices attached" | grep -v "^$" | grep -v "unauthorized" || true)
    
    if [ -z "$DEVICES" ]; then
        log_error "No authorized Android devices found"
        echo ""
        log_info "Troubleshooting:"
        echo -e "  1. Enable ${CYAN}USB Debugging${NC} on your Android device"
        echo -e "     - Settings → About phone → Tap 'Build number' 7 times"
        echo -e "     - Settings → Developer options → USB debugging → Enable"
        echo ""
        echo -e "  2. Connect device via USB"
        echo ""
        echo -e "  3. Authorize the connection on your device"
        echo -e "     - Tap 'Allow' when prompted for USB debugging authorization"
        echo ""
        echo -e "  4. Verify connection: ${CYAN}adb devices${NC}"
        echo ""
        
        # Check for unauthorized devices
        UNAUTHORIZED=$(adb devices | grep "unauthorized" || true)
        if [ -n "$UNAUTHORIZED" ]; then
            log_warn "Found unauthorized device(s):"
            echo "$UNAUTHORIZED"
            echo ""
            log_info "Please check your device screen and tap 'Allow' to authorize"
            echo ""
            
            # Wait for authorization
            echo -e "${YELLOW}Waiting for device authorization (press Ctrl+C to cancel)...${NC}"
            timeout=60
            elapsed=0
            while [ $elapsed -lt $timeout ]; do
                sleep 2
                elapsed=$((elapsed + 2))
                DEVICES=$(adb devices | grep -v "List of devices attached" | grep -v "^$" | grep -v "unauthorized" || true)
                if [ -n "$DEVICES" ]; then
                    log_success "Device authorized!"
                    break
                fi
                echo -n "."
            done
            echo ""
            
            if [ -z "$DEVICES" ]; then
                log_error "Device authorization timed out"
                exit 1
            fi
        else
            exit 1
        fi
    fi
    
    # Show connected devices
    log_success "Connected device(s):"
    echo "$DEVICES"
    echo ""
    
    # Install APK
    log_info "Installing DeepEyeUnlocker APK..."
    echo ""
    
    # Uninstall existing version if present
    log_info "Checking for existing installation..."
    if adb shell pm list packages | grep -q "com.deepeye.otg"; then
        log_warn "DeepEyeUnlocker (com.deepeye.otg) is already installed"
        echo -e "${YELLOW}Do you want to reinstall? (y/N)${NC}"
        read -r answer
        if [[ "$answer" =~ ^[Yy]$ ]]; then
            log_info "Uninstalling previous version..."
            adb uninstall com.deepeye.otg || true
            log_success "Previous version removed"
        fi
    fi
    
    # Install with downgrade flag
    log_info "Installing APK..."
    adb install -r -d "$APK_PATH"
    
    if [ $? -eq 0 ]; then
        log_success "DeepEyeUnlocker installed successfully on Android!"
        echo ""
        log_info "Package name: com.deepeye.otg"
        echo ""
        
        # Verify installation
        log_info "Verifying installation..."
        if adb shell pm list packages | grep -q "com.deepeye.otg"; then
            log_success "Verification: Package com.deepeye.otg found"
            echo ""
            
            # Ask to launch
            echo -e "${YELLOW}Do you want to launch DeepEyeUnlocker on the device? (y/N)${NC}"
            read -r launch_answer
            if [[ "$launch_answer" =~ ^[Yy]$ ]]; then
                log_info "Launching DeepEyeUnlocker..."
                adb shell am start -n com.deepeye.otg/.MainActivity
                log_success "Application launched on device!"
            fi
        else
            log_error "Installation verification failed"
            exit 1
        fi
    else
        log_error "Installation failed"
        exit 1
    fi
}

# ─────────────────────────────────────────────────────────────
# Main Menu
# ─────────────────────────────────────────────────────────────

show_menu() {
    log_header "DeepEyeUnlocker Installation Menu"
    
    echo -e "${BLUE}1.${NC} Install on macOS (.pkg)"
    echo -e "${BLUE}2.${NC} Install on Android (.apk via ADB)"
    echo -e "${BLUE}3.${NC} Install on both platforms"
    echo -e "${BLUE}4.${NC} Show installation status"
    echo -e "${BLUE}5.${NC} Exit"
    echo ""
    echo -e "${YELLOW}Select option (1-5):${NC}"
}

show_status() {
    log_header "Installation Status"
    
    # macOS status
    if [ -d "/Applications/DeepEyeUnlocker.app" ]; then
        log_success "macOS: Installed (/Applications/DeepEyeUnlocker.app)"
    else
        log_warn "macOS: Not installed"
    fi
    
    # Android status
    APK_STATUS=$(adb shell pm list packages 2>/dev/null | grep "com.deepeye.otg" || true)
    if [ -n "$APK_STATUS" ]; then
        log_success "Android: Installed (com.deepeye.otg)"
    else
        log_warn "Android: Not installed or no device connected"
    fi
    
    # Build artifacts
    echo ""
    log_info "Build Artifacts:"
    
    PKG_PATH="${PROJECT_ROOT}/target/x86_64-apple-darwin/release/bundle/pkg/DeepEyeUnlocker_2027.18.1_x86_64.pkg"
    APK_PATH="${PROJECT_ROOT}/app/build/outputs/apk/release/app-release.apk"
    
    if [ -f "$PKG_PATH" ]; then
        log_success "PKG: Available ($(du -h "$PKG_PATH" | cut -f1))"
    else
        log_warn "PKG: Not built"
    fi
    
    if [ -f "$APK_PATH" ]; then
        log_success "APK: Available ($(du -h "$APK_PATH" | cut -f1))"
    else
        log_warn "APK: Not built"
    fi
}

# ─────────────────────────────────────────────────────────────
# Main
# ─────────────────────────────────────────────────────────────

main() {
    cd "$PROJECT_ROOT"
    
    # If argument provided, use it directly
    if [ $# -eq 1 ]; then
        case "$1" in
            macos)
                install_macos
                ;;
            android)
                install_android
                ;;
            both)
                install_macos
                install_android
                ;;
            status)
                show_status
                ;;
            *)
                echo "Usage: $0 {macos|android|both|status}"
                exit 1
                ;;
        esac
        exit 0
    fi
    
    # Interactive menu
    while true; do
        show_menu
        read -r choice
        
        case "$choice" in
            1)
                install_macos
                ;;
            2)
                install_android
                ;;
            3)
                install_macos
                install_android
                ;;
            4)
                show_status
                ;;
            5)
                log_info "Goodbye!"
                exit 0
                ;;
            *)
                log_error "Invalid option"
                ;;
        esac
        
        echo ""
        echo -e "${YELLOW}Press Enter to continue...${NC}"
        read -r
    done
}

# Run main
main "$@"
