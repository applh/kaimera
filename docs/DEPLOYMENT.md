# Deployment Guide

## Deploying to GitHub

### 🐳 Containerized Deployment (Recommended)

Use the included Docker-based deployment for isolated, secure GitHub publishing:

```bash
# Run the deployment script
./deploy-to-github.sh YOUR_GITHUB_USERNAME kaimera
```

**What it does:**
1. Builds a lightweight Alpine Linux container (~50MB)
2. Checks for existing SSH key in `.github-deploy-key` (gitignored)
3. If no key exists: generates new key and saves it locally
4. If key exists: reuses it (no need to re-add to GitHub!)
5. Displays the public key for you to add to GitHub (first time only)
6. Pushes the repository with all tags
7. Destroys the container (key remains on disk for reuse)

**Step-by-step:**

1. **Create GitHub repository** at https://github.com/new
   - Repository name: `kaimera`
   - Don't initialize with anything

2. **Run Deployment Script**
Run the deployment script with your GitHub username and repository name:

```bash
./deploy-to-github.sh <username> <repo_name>
```

Example:
```bash
./deploy-to-github.sh applh kaimera
```

**First Run:**
- The script will generate a new SSH key.
- It will display the public key and instructions to add it to your GitHub repository settings.
- Follow the on-screen instructions.

**Subsequent Runs:**
- The script automatically detects and reuses the saved SSH key (`.github-deploy-key`).
- No manual intervention is required.

> **Note:** The `docker-push.sh` script is an internal helper used by the container. You should always use `deploy-to-github.sh`.
   - Go to https://github.com/settings/ssh/new
   - Title: "Kaimera Deploy Key"
   - Paste the displayed key
   - Click "Add SSH key"

4. **Press ENTER** to continue the push

5. **Future deployments**: Just run step 2 again - the key is reused automatically!

**Advantages:**
- ✅ SSH key stored locally in `.github-deploy-key` (gitignored)
- ✅ Only add key to GitHub once
- ✅ Container is removed after push
- ✅ One command to deploy
- ✅ Automatic key reuse for updates

### Manual GitHub Deployment

If you prefer not to use Docker:

```bash
# Add GitHub remote
git remote add origin https://github.com/YOUR_USERNAME/kaimera.git

# Commit changes
git add .
git commit -m "Release v19.0.0"

# Push to GitHub
git push -u origin main --tags
```

**Note:** You'll need a GitHub Personal Access Token (Settings → Developer settings → Personal access tokens) or SSH key configured on your system.

## Publishing to Google Play

### Required Assets

1. **High-res icon** - 512x512 PNG (replace placeholder icons)
2. **Screenshots** - Phone and tablet screenshots
3. **Feature graphic** - 1024x500 promotional banner
4. **Privacy policy** - Required for camera permission

### Steps

1. Create app in [Google Play Console](https://play.google.com/console)
2. Upload `app-release.apk`
3. Complete store listing
4. Submit for review
