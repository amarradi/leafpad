#!/usr/bin/env bash
set -euo pipefail

# Wurzel der Fastlane-Metadaten (hier aus deinem Listing abgeleitet)
BASE_DIR="."
FASTLANE_DIR="$BASE_DIR"
SRC_LOCALE="de-DE"              # Quelle zum Kopieren
SRC_DIR="$FASTLANE_DIR/$SRC_LOCALE"

if [[ ! -d "$SRC_DIR" ]]; then
  echo "Fehler: Quellordner '$SRC_DIR' nicht gefunden. Bitte 'de-DE' zuerst anlegen/befüllen." >&2
  exit 1
fi

# Android-Locales aus deinem Projekt (values-... Ordnern)
ANDROID_LOCALES=(
  aeb ar be bg br ca ce cs da de el es et fi fr got hi hu id it nb nl pl pt ru sq sv ta uk ur uz zh-rCN zh-rTW b+hi+Latn
)

# Mapping Android -> Fastlane (Play-Sprachcodes / übliche Kombinationen)
# Hinweis:
# - Manche Sprachen sind auch ohne Region üblich (z.B. 'ru', 'id', 'uk', 'uz', 'et').
# - Exotische/unsichere Codes (aeb, ce, got, b+hi+Latn) werden als reine Sprachcodes/Fallbacks angelegt.
declare -A MAP=(
  [aeb]="aeb"        # Tunisian Arabic (u.U. nicht in Play gelistet; Fallback-Ordner)
  [ar]="ar"
  [be]="be-BY"
  [bg]="bg-BG"
  [br]="br"          # Breton (evtl. nicht in Play, aber Ordner ist ok)
  [ca]="ca"
  [ce]="ce"          # Chechen (evtl. nicht in Play)
  [cs]="cs-CZ"
  [da]="da-DK"
  [de]="de-DE"
  [el]="el-GR"
  [es]="es-ES"
  [et]="et"          # häufig ohne Region
  [fi]="fi-FI"
  [fr]="fr-FR"
  [got]="got"        # Gothic (sehr wahrscheinlich nicht in Play)
  [hi]="hi-IN"
  [hu]="hu-HU"
  [id]="id"          # ohne Region üblich
  [it]="it-IT"
  [nb]="nb-NO"
  [nl]="nl-NL"
  [pl]="pl-PL"
  [pt]="pt-PT"       # ggf. zusätzlich pt-BR separat pflegen
  [ru]="ru"          # ohne Region vorhanden (bei dir schon so)
  [sq]="sq-AL"
  [sv]="sv-SE"
  [ta]="ta-IN"
  [uk]="uk"          # ohne Region vorhanden (bei dir schon so)
  [ur]="ur-PK"       # oft PK; je nach Zielgruppe evtl. 'ur-IN'
  [uz]="uz"          # ohne Region vorhanden (bei dir schon so)
  [zh-rCN]="zh-CN"
  [zh-rTW]="zh-TW"
  [b+hi+Latn]="hi-Latn"  # BCP-47; Play-Support unklar -> Fallback-Ordner
)

created=()
skipped=()

for a in "${ANDROID_LOCALES[@]}"; do
  target="${MAP[$a]:-}"
  if [[ -z "${target}" ]]; then
    echo "Warnung: Kein Mapping für '$a' gefunden – übersprungen." >&2
    continue
  fi
  dest="$FASTLANE_DIR/$target"
  if [[ -d "$dest" ]]; then
    skipped+=("$target")
    continue
  fi
  mkdir -p "$dest"
  # Inhalte aus de-DE kopieren (Dateien + Unterordner)
  cp -R "$SRC_DIR/." "$dest/"
  created+=("$target")
done

echo "Fertig."
((${#created[@]})) && echo "Neu erstellt: ${created[*]}"
((${#skipped[@]})) && echo "Bereits vorhanden (übersprungen): ${skipped[*]}"

# Hinweise zu potentiell nicht unterstützten Locales in der Play Console:
echo "Hinweis: Folgende Locales könnten in der Play Console nicht offiziell unterstützt sein:"
echo "  aeb, br, ce, got, hi-Latn (aus b+hi+Latn)"
