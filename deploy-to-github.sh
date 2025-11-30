#!/bin/bash
set -e

# Configuration
GITHUB_USERNAME="${1:-YOUR_USERNAME}"
REPO_NAME="${2:-kaimera}"
GITHUB_REPO="git@github.com:${GITHUB_USERNAME}/${REPO_NAME}.git"

echo "🐳 Building Docker image..."
docker build -f Dockerfile.gitpush -t kaimera-deploy .

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
  kaimera-deploy

echo ""
echo "✨ Deployment complete!"
echo "🧹 Container removed. SSH key saved to .github-deploy-key"
