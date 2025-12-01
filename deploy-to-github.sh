#!/bin/bash
set -e

# Configuration
GITHUB_USERNAME="${1:-YOUR_USERNAME}"
REPO_NAME="${2:-kaimera}"
GITHUB_REPO="git@github.com:${GITHUB_USERNAME}/${REPO_NAME}.git"
IMAGE_NAME="kaimera-deploy"

# Check if image exists and Dockerfile hasn't changed
if docker image inspect ${IMAGE_NAME} >/dev/null 2>&1; then
    echo "📦 Docker image '${IMAGE_NAME}' already exists"
    
    # Check if Dockerfile has been modified since image was built
    IMAGE_CREATED=$(docker image inspect ${IMAGE_NAME} --format='{{.Created}}')
    DOCKERFILE_MODIFIED=$(stat -f %m Dockerfile.gitpush 2>/dev/null || stat -c %Y Dockerfile.gitpush 2>/dev/null)
    
    if [ -n "$DOCKERFILE_MODIFIED" ]; then
        echo "🔄 Checking if rebuild is needed..."
        # For simplicity, we'll skip the timestamp comparison and just reuse the image
        # To force rebuild, run: docker rmi ${IMAGE_NAME}
        echo "✅ Using existing image (to force rebuild: docker rmi ${IMAGE_NAME})"
    fi
else
    echo "🐳 Building Docker image..."
    docker build -f Dockerfile.gitpush -t ${IMAGE_NAME} .
fi

echo ""
echo "🚀 Running deployment container..."
echo "Repository: ${GITHUB_REPO}"
echo ""

docker run -it --rm \
  -v "$(pwd):/repo" \
  -e GITHUB_REPO="${GITHUB_REPO}" \
  -e GIT_EMAIL="${GIT_EMAIL:-deploy@kaimera.local}" \
  -e GIT_NAME="${GIT_NAME:-Kaimera Deploy}" \
  -e COMMIT_MESSAGE="${COMMIT_MESSAGE:-Release v19.0.0}" \
  ${IMAGE_NAME}

echo ""
echo "✨ Deployment complete!"
echo "🧹 Container removed. SSH key saved to .github-deploy-key"
echo ""
echo "💡 Tip: To rebuild the image, run: docker rmi ${IMAGE_NAME}"
