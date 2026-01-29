#!/bin/bash
# DeepEyeUnlocker - GitHub Labels Setup Script
# Run: bash scripts/setup-labels.sh

REPO="DeepEyeCrypto/DeepEyeUnlocker"

echo "🏷️  Creating GitHub labels for DeepEyeUnlocker..."

# Feature labels
gh label create "feature/health-center" -d "Device Health Center" -c "1D76DB" -R $REPO --force
gh label create "feature/backup-center" -d "Partition Backup Center" -c "0E8A16" -R $REPO --force
gh label create "feature/rom-sandbox" -d "ROM Sandbox (DSU)" -c "5319E7" -R $REPO --force
gh label create "feature/cloak-center" -d "Cloak Center" -c "D93F0B" -R $REPO --force
gh label create "meta/architecture" -d "Architecture/refactoring" -c "FBCA04" -R $REPO --force

# Priority labels
gh label create "P0" -d "Must ship" -c "B60205" -R $REPO --force
gh label create "P1" -d "Nice to have" -c "D93F0B" -R $REPO --force
gh label create "P2" -d "Stretch goal" -c "FBCA04" -R $REPO --force

# Type labels
gh label create "type/feature" -d "New feature" -c "0075CA" -R $REPO --force
gh label create "type/bug" -d "Bug fix" -c "D73A4A" -R $REPO --force
gh label create "type/refactor" -d "Code refactoring" -c "A2EEEF" -R $REPO --force
gh label create "type/docs" -d "Documentation" -c "0075CA" -R $REPO --force
gh label create "type/test" -d "Tests" -c "BFD4F2" -R $REPO --force

# Status labels
gh label create "Next Milestone" -d "Part of next release" -c "7057FF" -R $REPO --force
gh label create "Later" -d "Backlog" -c "C5DEF5" -R $REPO --force
gh label create "WIP" -d "Work in progress" -c "FEF2C0" -R $REPO --force

echo "✅ Labels created successfully!"
echo ""
echo "View labels at: https://github.com/$REPO/labels"
