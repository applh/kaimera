# Git Workflow

The repository is initialized with Git and includes a comprehensive `.gitignore`.

## Initial Setup

```bash
# Already initialized with:
git init
git add -A
git commit -m "Initial commit: Kaimera camera app with CameraX"
```

## Adding Remote and Pushing

```bash
# Add your remote repository
git remote add origin <your-repo-url>

# Push to GitHub/GitLab/etc
git push -u origin main
```

## What's Tracked

- ✅ Source code (Kotlin, XML)
- ✅ Build configuration (Gradle files)
- ✅ Documentation (README, scripts)
- ✅ Resources (icons, strings, themes)
- ❌ Build artifacts (APKs, build folders)
- ❌ IDE files (.idea, *.iml)
- ❌ Local configuration (local.properties)
