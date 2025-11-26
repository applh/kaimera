# Docker Build for Kaimera APK

This directory contains Docker-based build configurations for creating reproducible APK builds.

## Quick Start

### Option 1: Using the Build Script (Recommended)

```bash
# Build release APK
./docker-build.sh

# Build debug APK
./docker-build.sh --debug

# Clean build
./docker-build.sh --clean
```

### Option 2: Using Docker Compose

```bash
# Build release APK
docker-compose -f docker-compose.build.yml up builder

# Build debug APK
docker-compose -f docker-compose.build.yml up builder-debug
```

### Option 3: Manual Docker Commands

```bash
# Build the Docker image
docker build -f Dockerfile.build -t kaimera-builder .

# Run the build
docker run --rm \
  -v $(pwd)/build-output:/app/app/build/outputs \
  kaimera-builder \
  ./gradlew assembleRelease --no-daemon
```

## Output Location

Built APKs will be in:
```
build-output/
├── apk/
│   ├── debug/
│   │   └── app-debug.apk
│   └── release/
│       └── app-release.apk
└── logs/
```

## Benefits of Docker Builds

✅ **Reproducible**: Same build environment every time  
✅ **Isolated**: No conflicts with local SDK/tools  
✅ **CI/CD Ready**: Easy to integrate with GitHub Actions, GitLab CI, etc.  
✅ **Clean**: No local build artifacts  
✅ **Portable**: Works on any machine with Docker  

## Container Details

- **Base Image**: OpenJDK 17 (slim)
- **Android SDK**: Command Line Tools 9.0
- **Platform**: android-33
- **Build Tools**: 33.0.0
- **Size**: ~1.5GB (includes SDK)

## Customization

### Change Android SDK Version

Edit `Dockerfile.build`:
```dockerfile
RUN sdkmanager "platforms;android-34" "build-tools;34.0.0"
```

### Add Build Arguments

```bash
docker run --rm \
  -e GRADLE_OPTS="-Xmx2g" \
  -v $(pwd)/build-output:/app/app/build/outputs \
  kaimera-builder \
  ./gradlew assembleRelease --no-daemon
```

### Cache Gradle Dependencies

The Docker Compose setup includes a volume for Gradle cache:
```yaml
volumes:
  - gradle-cache:/root/.gradle
```

This speeds up subsequent builds significantly.

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Build APK

on: [push]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Build APK with Docker
        run: ./docker-build.sh
      
      - name: Upload APK
        uses: actions/upload-artifact@v3
        with:
          name: app-release
          path: build-output/apk/release/app-release.apk
```

### GitLab CI Example

```yaml
build:
  image: docker:latest
  services:
    - docker:dind
  script:
    - ./docker-build.sh
  artifacts:
    paths:
      - build-output/apk/release/app-release.apk
```

## Troubleshooting

### Build Fails with "Out of Memory"

Increase Docker memory limit or add to `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2048m
```

### SDK Licenses Not Accepted

The Dockerfile automatically accepts licenses with:
```dockerfile
RUN yes | sdkmanager --licenses
```

### Slow First Build

First build downloads SDK components (~1GB). Subsequent builds use cached layers.

### APK Not Found

Check the output directory:
```bash
ls -la build-output/apk/release/
```

## Cleanup

```bash
# Remove build output
rm -rf build-output/

# Remove Docker image
docker rmi kaimera-builder

# Remove Gradle cache volume
docker volume rm kaimera_gradle-cache
```

## Advanced Usage

### Multi-Stage Build (Smaller Image)

Create `Dockerfile.build.multistage`:
```dockerfile
# Build stage
FROM openjdk:17-slim AS builder
# ... (same as Dockerfile.build)

# Runtime stage (smaller)
FROM alpine:latest
COPY --from=builder /app/app/build/outputs /outputs
CMD ["sh"]
```

### Parallel Builds

```bash
# Build debug and release simultaneously
docker-compose -f docker-compose.build.yml up -d
```

---

**Made with 🐳 Docker for reproducible Android builds**
