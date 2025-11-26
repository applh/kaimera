#!/bin/bash

# Kaimera APK Builder Script
# Builds the app in a Docker container for reproducible builds

set -e

echo "🐳 Kaimera Docker Build Script"
echo "=============================="
echo ""

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
IMAGE_NAME="kaimera-builder"
CONTAINER_NAME="kaimera-build-$(date +%s)"
OUTPUT_DIR="./build-output"

# Parse arguments
BUILD_TYPE="release"
CLEAN_BUILD=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --debug)
            BUILD_TYPE="debug"
            shift
            ;;
        --clean)
            CLEAN_BUILD=true
            shift
            ;;
        --help)
            echo "Usage: ./docker-build.sh [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --debug    Build debug APK instead of release"
            echo "  --clean    Clean build (remove previous build artifacts)"
            echo "  --help     Show this help message"
            echo ""
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# Create output directory
mkdir -p "$OUTPUT_DIR"

echo -e "${BLUE}Step 1: Building Docker image...${NC}"
docker build -f Dockerfile.build -t "$IMAGE_NAME" .

if [ $? -ne 0 ]; then
    echo -e "${YELLOW}❌ Docker build failed${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Docker image built successfully${NC}"
echo ""

# Determine Gradle task
if [ "$BUILD_TYPE" = "debug" ]; then
    GRADLE_TASK="assembleDebug"
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
else
    GRADLE_TASK="assembleRelease"
    APK_PATH="app/build/outputs/apk/release/app-release.apk"
fi

# Add clean task if requested
if [ "$CLEAN_BUILD" = true ]; then
    GRADLE_TASK="clean $GRADLE_TASK"
fi

echo -e "${BLUE}Step 2: Building APK in container...${NC}"
echo "Build type: $BUILD_TYPE"
echo "Gradle task: $GRADLE_TASK"
echo ""

# Run the build in container
docker run --rm \
    --name "$CONTAINER_NAME" \
    -v "$(pwd)/build-output:/app/app/build/outputs" \
    "$IMAGE_NAME" \
    ./gradlew $GRADLE_TASK --no-daemon

if [ $? -ne 0 ]; then
    echo -e "${YELLOW}❌ Build failed${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}✅ Build completed successfully!${NC}"
echo ""
echo -e "${BLUE}📦 APK Location:${NC}"
echo "  Container: /app/$APK_PATH"
echo "  Host: $OUTPUT_DIR/apk/$BUILD_TYPE/"
echo ""

# Check if APK exists
if [ -f "$OUTPUT_DIR/apk/$BUILD_TYPE/app-$BUILD_TYPE.apk" ]; then
    APK_SIZE=$(du -h "$OUTPUT_DIR/apk/$BUILD_TYPE/app-$BUILD_TYPE.apk" | cut -f1)
    echo -e "${GREEN}APK Size: $APK_SIZE${NC}"
    echo ""
    echo "To install:"
    echo "  adb install $OUTPUT_DIR/apk/$BUILD_TYPE/app-$BUILD_TYPE.apk"
else
    echo -e "${YELLOW}⚠️  APK not found in expected location${NC}"
    echo "Check: $OUTPUT_DIR/apk/$BUILD_TYPE/"
fi

echo ""
echo -e "${GREEN}🎉 Done!${NC}"
