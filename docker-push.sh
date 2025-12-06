#!/bin/sh
set -e

SSH_KEY_FILE="/repo/.github-deploy-key"
SSH_PUB_FILE="/repo/.github-deploy-key.pub"

# Check if key already exists
if [ -f "$SSH_KEY_FILE" ] && [ -f "$SSH_PUB_FILE" ]; then
    echo "🔑 Using existing SSH key..."
    mkdir -p /root/.ssh
    cp "$SSH_KEY_FILE" /root/.ssh/id_ed25519
    cp "$SSH_PUB_FILE" /root/.ssh/id_ed25519.pub
    chmod 600 /root/.ssh/id_ed25519
    chmod 644 /root/.ssh/id_ed25519.pub
    
    echo ""
    echo "📋 Your SSH Public Key (already added to GitHub):"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    cat /root/.ssh/id_ed25519.pub
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
else
    echo "🔑 Generating new SSH key..."
    ssh-keygen -t ed25519 -f /root/.ssh/id_ed25519 -N "" -C "kaimera-deploy"
    
    # Save key to repository (gitignored)
    cp /root/.ssh/id_ed25519 "$SSH_KEY_FILE"
    cp /root/.ssh/id_ed25519.pub "$SSH_PUB_FILE"
    chmod 600 "$SSH_KEY_FILE"
    chmod 644 "$SSH_PUB_FILE"
    
    echo ""
    echo "📋 Your NEW SSH Public Key (add this to GitHub):"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    cat /root/.ssh/id_ed25519.pub
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    echo "📝 Steps to add this key to GitHub:"
    echo "1. Go to: https://github.com/settings/ssh/new"
    echo "2. Title: 'Kaimera Deploy Key'"
    echo "3. Paste the key above"
    echo "4. Click 'Add SSH key'"
    echo ""
    echo "💾 Key saved to: .github-deploy-key (gitignored)"
    echo ""
    read -p "Press ENTER after adding the key to GitHub..."
fi

# Configure SSH to skip host key verification (for container only)
mkdir -p /root/.ssh
cat > /root/.ssh/config <<EOF
Host github.com
    StrictHostKeyChecking no
    UserKnownHostsFile=/dev/null
EOF

# Configure git
git config --global user.email "${GIT_EMAIL:-deploy@kaimera.local}"
git config --global user.name "${GIT_NAME:-Kaimera Deploy}"

# Add remote if not exists
if ! git remote get-url origin 2>/dev/null; then
    echo "📡 Adding GitHub remote..."
    git remote add origin "${GITHUB_REPO}"
fi

# Commit any changes
echo "💾 Committing changes..."
git add .
git commit -m "${COMMIT_MESSAGE:-Release v19.0.0}" || echo "Nothing to commit"

# Push to GitHub
# Pull changes to resolve divergence
echo "⬇️ Pulling from GitHub..."
git pull origin main --rebase

# Push to GitHub
echo "🚀 Pushing to GitHub..."
git push -u origin main --tags

echo "✅ Successfully pushed to GitHub!"
echo "🌐 Repository: ${GITHUB_REPO}"
