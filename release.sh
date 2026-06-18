#!/bin/bash

# =============================================================================
#  ChargeSplit – GitHub Release Script
#  Usage: ./release.sh [version] [build-type]
#  Example: ./release.sh 1.0.1 debug
#           ./release.sh 1.2.0 release
#  Defaults: version aus build.gradle.kts, build-type = debug
# =============================================================================

set -e  # Abbruch bei Fehler

# ── Farben ────────────────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# ── Hilfsfunktionen ───────────────────────────────────────────────────────────
echo_e() {
    printf '%b\n' "$*"
}
log()     { echo_e "${CYAN}[INFO]${NC}  $1"; }
success() { echo_e "${GREEN}[OK]${NC}    $1"; }
warn()    { echo_e "${YELLOW}[WARN]${NC}  $1"; }
error()   { echo_e "${RED}[ERROR]${NC} $1"; exit 1; }
step()    { echo_e "\n${BLUE}══════════════════════════════════════${NC}"; echo_e "${BLUE}  $1${NC}"; echo_e "${BLUE}══════════════════════════════════════${NC}"; }

# ── Projektverzeichnis ────────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# ── Parameter ─────────────────────────────────────────────────────────────────
BUILD_GRADLE="app/build.gradle.kts"

# Version aus build.gradle.kts lesen
GRADLE_VERSION=$(grep 'versionName' "$BUILD_GRADLE" | sed 's/.*"\(.*\)".*/\1/')
BASE_VERSION="${1:-$GRADLE_VERSION}"

# Vorhandene Tags abfragen
if git remote | grep -q 'origin'; then
    EXISTING_TAGS=$(git ls-remote --tags origin | awk -F'/' '{print $3}' | grep -v '\^{}' || true)
else
    EXISTING_TAGS=$(git tag || true)
fi

VERSION=$BASE_VERSION
while echo "$EXISTING_TAGS" | grep -q "^v${VERSION}$"; do
    major=$(echo "$VERSION" | cut -d. -f1)
    minor=$(echo "$VERSION" | cut -d. -f2)
    patch=$(echo "$VERSION" | cut -d. -f3)
    patch=$((patch + 1))
    VERSION="${major}.${minor}.${patch}"
done

TAG="v${VERSION}"
BUILD_TYPE="${2:-debug}"  # debug oder release

# Falls sich die Version geändert hat, in build.gradle.kts anpassen und committen
if [ "$VERSION" != "$GRADLE_VERSION" ]; then
    echo_e "${YELLOW}[INFO]${NC} Aktualisiere $BUILD_GRADLE von $GRADLE_VERSION auf $VERSION..."
    sed -i 's/versionName = "[^"]*"/versionName = "'"$VERSION"'"/' "$BUILD_GRADLE"
    
    GRADLE_CODE=$(grep 'versionCode' "$BUILD_GRADLE" | sed 's/[^0-9]//g')
    if [ -n "$GRADLE_CODE" ]; then
        NEW_CODE=$((GRADLE_CODE + 1))
        sed -i 's/versionCode = [0-9]*/versionCode = '"$NEW_CODE"'/' "$BUILD_GRADLE"
        echo_e "${YELLOW}[INFO]${NC} versionCode auf $NEW_CODE aktualisiert."
    fi
    
    git add "$BUILD_GRADLE"
    git commit -m "chore(release): Bump version to $VERSION"
    git push origin master || true
fi

# APK Pfad je nach Build-Typ
if [ "$BUILD_TYPE" = "release" ]; then
    APK_PATH="app/build/outputs/apk/release/app-release-unsigned.apk"
    GRADLE_TASK="assembleRelease"
    APK_LABEL="app-release-unsigned.apk"
else
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
    GRADLE_TASK="assembleDebug"
    APK_LABEL="app-debug.apk"
fi

# Benannter APK mit Version (für den Release)
OUTPUT_APK="ChargeSplit-${TAG}-${BUILD_TYPE}.apk"

# ── Start ─────────────────────────────────────────────────────────────────────
echo ""
echo_e "${CYAN}╔══════════════════════════════════════╗${NC}"
echo_e "${CYAN}║   ChargeSplit Release Script         ║${NC}"
echo_e "${CYAN}╚══════════════════════════════════════╝${NC}"
echo ""
log "Version:    ${TAG}"
log "Build-Typ:  ${BUILD_TYPE}"
log "APK-Output: ${OUTPUT_APK}"

# ── Voraussetzungen prüfen ────────────────────────────────────────────────────
step "Voraussetzungen prüfen"

if [ -z "$GITHUB_TOKEN" ]; then
    # Try to fetch GITHUB_TOKEN from git credential helper
    GIT_TOKEN=$(printf "protocol=https\nhost=github.com\n\n" | git credential fill 2>/dev/null | grep password | cut -d= -f2)
    if [ -n "$GIT_TOKEN" ]; then
        export GITHUB_TOKEN="$GIT_TOKEN"
    fi
fi

SKIP_GH_RELEASE=false

# gh CLI vorhanden?
if ! command -v gh &> /dev/null; then
    warn "GitHub CLI (gh) ist nicht installiert. GitHub Release wird übersprungen."
    SKIP_GH_RELEASE=true
else
    success "GitHub CLI gefunden: $(gh --version | head -1)"
fi

# Eingeloggt?
if [ "$SKIP_GH_RELEASE" = "false" ]; then
    if ! gh auth status &> /dev/null; then
        warn "Du bist nicht bei GitHub eingeloggt. GitHub Release wird übersprungen."
        SKIP_GH_RELEASE=true
    else
        success "GitHub Login OK"
    fi
fi

# Gradle vorhanden?
if [ ! -f "./gradlew" ]; then
    error "gradlew nicht gefunden. Bist du im richtigen Verzeichnis?"
fi
success "gradlew gefunden"

# Git-Status prüfen (warnung bei uncommited changes)
if [ -n "$(git status --porcelain)" ]; then
    warn "Es gibt uncommittete Änderungen. Der Release wird trotzdem erstellt."
fi

# ── Git Tag prüfen ────────────────────────────────────────────────────────────
step "Git Tag prüfen"

if git rev-parse "$TAG" &> /dev/null; then
    warn "Tag ${TAG} existiert bereits."
    if [ "$NON_INTERACTIVE" = "true" ]; then
        OVERWRITE="y"
    else
        read -p "  Soll der bestehende Tag überschrieben werden? (j/N): " OVERWRITE
    fi
    if [ "$OVERWRITE" = "j" ] || [ "$OVERWRITE" = "J" ] || [ "$OVERWRITE" = "y" ] || [ "$OVERWRITE" = "Y" ]; then
        log "Lösche alten Tag ${TAG}..."
        git tag -d "$TAG" 2>/dev/null || true
        git push origin --delete "$TAG" 2>/dev/null || true
        # Auch den GitHub Release löschen falls vorhanden
        if [ "$SKIP_GH_RELEASE" = "false" ]; then
            gh release delete "$TAG" --yes 2>/dev/null || true
        fi
    else
        error "Abgebrochen. Bitte eine andere Version angeben: ./release.sh <version>"
    fi
fi

# ── APK bauen ─────────────────────────────────────────────────────────────────
step "APK bauen (${BUILD_TYPE})"
log "Starte: ./gradlew ${GRADLE_TASK} ..."
echo ""

./gradlew "$GRADLE_TASK" || error "Gradle Build fehlgeschlagen!"

echo ""
if [ ! -f "$APK_PATH" ]; then
    error "APK nicht gefunden unter: ${APK_PATH}"
fi
success "APK erfolgreich gebaut!"

# APK umbenennen/kopieren mit Versions-Label
cp "$APK_PATH" "$OUTPUT_APK"
log "APK kopiert als: ${OUTPUT_APK}"

APK_SIZE=$(du -sh "$OUTPUT_APK" | cut -f1)
log "APK Größe: ${APK_SIZE}"

# ── Git Tag erstellen ─────────────────────────────────────────────────────────
step "Git Tag erstellen"

git tag "$TAG"
log "Tag ${TAG} erstellt"

git push origin "$TAG" || warn "Tag konnte nicht gepusht werden (kein Remote?)"
success "Tag ${TAG} auf GitHub gepusht"

# ── Release Notes eingeben ────────────────────────────────────────────────────
step "Release Notes"

RELEASE_NOTES=""
if [ "$NON_INTERACTIVE" = "true" ]; then
    log "Non-interactive mode: using auto release notes."
else
    echo_e "${YELLOW}Was gibt es Neues in Version ${VERSION}?${NC}"
    echo "(Leer lassen für automatische Notiz, CTRL+D zum Abschließen):"
    echo ""
    while IFS= read -r line; do
        RELEASE_NOTES+="${line}"$'\n'
    done
fi

if [ -z "$(echo "$RELEASE_NOTES" | tr -d '[:space:]')" ]; then
    RELEASE_NOTES="## ChargeSplit ${TAG}

Neue Version der ChargeSplit App.

### Download
Lade die APK herunter und installiere sie direkt auf deinem Android-Gerät.

> **Hinweis:** Aktiviere in den Android-Einstellungen \"Unbekannte Quellen\" um die APK zu installieren."
    log "Automatische Release Notes verwendet."
fi

# ── GitHub Release erstellen ──────────────────────────────────────────────────
step "GitHub Release erstellen"

if [ "$SKIP_GH_RELEASE" = "true" ]; then
    warn "Überspringe Erstellung des GitHub-Releases (nicht eingeloggt / kein gh CLI)."
else
    log "Erstelle Release ${TAG} auf GitHub..."
    gh release create "$TAG" \
        "$OUTPUT_APK" \
        --title "ChargeSplit ${TAG}" \
        --notes "$RELEASE_NOTES" \
        --latest
    echo ""
    success "🎉 Release erfolgreich veröffentlicht!"
fi

# ── Aufräumen ────────────────────────────────────────────────────────────────
rm -f "$OUTPUT_APK"
log "Temporäre APK-Kopie gelöscht."

# ── Release URL anzeigen ──────────────────────────────────────────────────────
step "Fertig!"

REPO_URL=""
if [ "$SKIP_GH_RELEASE" = "false" ]; then
    REPO_URL=$(gh repo view --json url -q .url 2>/dev/null || echo "")
fi
if [ -n "$REPO_URL" ]; then
    echo_e "${GREEN}✅ APK ist jetzt herunterladbar unter:${NC}"
    echo_e "   ${CYAN}${REPO_URL}/releases/tag/${TAG}${NC}"
fi

echo ""
echo_e "${GREEN}Nächste Schritte:${NC}"
echo "  1. Öffne den Link oben um den Release zu sehen"
echo "  2. Teile den Link mit anderen zum Herunterladen"
echo ""
