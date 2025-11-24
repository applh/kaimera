#!/bin/bash
# Kamerai Development Environment Setup Script
# This script sets up the required tools for building the Kamerai Android app

set -e  # Exit on error

echo "🚀 Kamerai Development Environment Setup"
echo "========================================"
echo ""

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if running on macOS
if [[ "$OSTYPE" != "darwin"* ]]; then
    echo -e "${RED}❌ This script is designed for macOS${NC}"
    exit 1
fi

echo "📋 Checking prerequisites..."
echo ""

# Check for Homebrew
if ! command -v brew &> /dev/null; then
    echo -e "${YELLOW}⚠️  Homebrew not found. Installing Homebrew...${NC}"
    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
else
    echo -e "${GREEN}✅ Homebrew installed${NC}"
fi

# Check for Java (JDK 17)
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [[ "$JAVA_VERSION" -ge 17 ]]; then
        echo -e "${GREEN}✅ Java $JAVA_VERSION installed${NC}"
    else
        echo -e "${YELLOW}⚠️  Java $JAVA_VERSION found, but JDK 17+ required${NC}"
        echo "Installing OpenJDK 17..."
        brew install openjdk@17
    fi
else
    echo -e "${YELLOW}⚠️  Java not found. Installing OpenJDK 17...${NC}"
    brew install openjdk@17
fi

# Check for Android SDK
if [ -d "$HOME/Library/Android/sdk" ]; then
    echo -e "${GREEN}✅ Android SDK found at $HOME/Library/Android/sdk${NC}"
else
    echo -e "${YELLOW}⚠️  Android SDK not found${NC}"
    echo ""
    echo "Please install Android Studio to get the Android SDK:"
    echo "1. Download from: https://developer.android.com/studio"
    echo "2. Install Android Studio"
    echo "3. Open Android Studio and install SDK Platform 33"
    echo "4. Run this script again"
    echo ""
    read -p "Press Enter to open download page..." 
    open "https://developer.android.com/studio"
    exit 1
fi

# Check for required SDK components
SDK_MANAGER="$HOME/Library/Android/sdk/cmdline-tools/latest/bin/sdkmanager"
if [ -f "$SDK_MANAGER" ]; then
    echo ""
    echo "📦 Checking Android SDK components..."
    
    # Check for Platform 33
    if [ -d "$HOME/Library/Android/sdk/platforms/android-33" ]; then
        echo -e "${GREEN}✅ Android SDK Platform 33 installed${NC}"
    else
        echo -e "${YELLOW}⚠️  Installing Android SDK Platform 33...${NC}"
        yes | $SDK_MANAGER "platforms;android-33"
    fi
    
    # Check for Build Tools
    if [ -d "$HOME/Library/Android/sdk/build-tools/33.0.0" ] || [ -d "$HOME/Library/Android/sdk/build-tools/34.0.0" ]; then
        echo -e "${GREEN}✅ Android Build Tools installed${NC}"
    else
        echo -e "${YELLOW}⚠️  Installing Android Build Tools...${NC}"
        yes | $SDK_MANAGER "build-tools;33.0.0"
    fi
fi

# Check for Git
if ! command -v git &> /dev/null; then
    echo -e "${YELLOW}⚠️  Git not found. Installing Git...${NC}"
    brew install git
else
    echo -e "${GREEN}✅ Git installed${NC}"
fi

# Create local.properties if it doesn't exist
if [ ! -f "local.properties" ]; then
    echo ""
    echo "📝 Creating local.properties..."
    echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties
    echo -e "${GREEN}✅ local.properties created${NC}"
fi

echo ""
echo "========================================"
echo -e "${GREEN}✅ Setup complete!${NC}"
echo ""
echo "📱 Next steps:"
echo "1. Build the app: ./gradlew assembleRelease"
echo "2. Or use the build script: sh build_app.sh"
echo "3. Install on device: adb install app/build/outputs/apk/release/app-release.apk"
echo ""
echo "📚 Documentation: See README.md"
echo ""
