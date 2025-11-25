#!/bin/bash
set -e

# Configuration
GITHUB_USERNAME="${1:-YOUR_USERNAME}"
REPO_NAME="${2:-kamerai}"
GITHUB_REPO="git@github.com:${GITHUB_USERNAME}/${REPO_NAME}.git"

echo "🐳 Building Docker image..."
docker build -f Dockerfile.gitpush -t kamerai-deploy .

echo ""
echo "🚀 Running deployment container..."
echo "Repository: ${GITHUB_REPO}"
echo ""

docker run -it --rm \
  -e GITHUB_REPO="${GITHUB_REPO}" \
  -e GIT_EMAIL="${GIT_EMAIL:-deploy@kamerai.local}" \
  -e GIT_NAME="${GIT_NAME:-Kamerai Deploy}" \
  -e COMMIT_MESSAGE="${COMMIT_MESSAGE:-Release v19.0.0}" \
  kamerai-deploy

echo ""
echo "✨ Deployment complete!"
echo "🧹 Container and SSH key have been removed."
