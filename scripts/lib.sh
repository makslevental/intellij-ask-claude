# Sourced by the other scripts (not run directly). Finds an installed IDE and a JDK, so nothing here is
# specific to one machine.
#
#   IDE_HOME   optional: the IDE to build against. May be the macOS bundle (/Applications/CLion.app), its
#              Contents directory, or the install directory on Linux.
#   JAVA_HOME  optional: a JDK 21 or newer, used when the IDE's bundled runtime has no compiler.

MIN_BUILD=262

ide_die() {
  echo "error: $*" >&2
  exit 1
}

# Echo the directory that holds lib/ and plugins/ for a candidate install, or nothing.
_ide_root() {
  if [ -d "$1/Contents/lib" ]; then echo "$1/Contents"
  elif [ -d "$1/lib" ] && [ -d "$1/plugins" ]; then echo "$1"
  fi
}

_ide_product_info() {
  for f in "$1/Resources/product-info.json" "$1/product-info.json"; do
    [ -f "$f" ] && { echo "$f"; return; }
  done
}

_ide_build() {
  info=$(_ide_product_info "$1")
  [ -n "$info" ] || return 0
  sed -n 's/.*"buildNumber"[^"]*"\([0-9.]*\)".*/\1/p' "$info" | head -1
}

_ide_name() {
  info=$(_ide_product_info "$1")
  [ -n "$info" ] || return 0
  sed -n 's/^[[:space:]]*"name"[^"]*"\([^"]*\)".*/\1/p' "$info" | head -1
}

# Prints the root of the IDE to build against; explains on stderr which one was chosen.
find_ide() {
  if [ -n "${IDE_HOME:-}" ]; then
    root=$(_ide_root "${IDE_HOME%/}")
    [ -n "$root" ] || ide_die "IDE_HOME=$IDE_HOME is not an IDE install (no lib/ directory found)"
    echo "Using IDE_HOME: $root" >&2
    echo "$root"
    return
  fi
  list=""
  for cand in \
    "$HOME"/Applications/*.app /Applications/*.app \
    "$HOME/Library/Application Support/JetBrains/Toolbox/apps"/*/*/*/*.app \
    /opt/*/ /opt/*/*/ \
    "$HOME/.local/share/JetBrains/Toolbox/apps"/*/*/*/ "$HOME/.local/share/JetBrains/Toolbox/apps"/*/*/ \
    /snap/*/current/ ; do
    [ -d "$cand" ] || continue
    root=$(_ide_root "${cand%/}")
    [ -n "$root" ] || continue
    [ -d "$root/plugins/terminal" ] || continue # needs the bundled Terminal plugin
    build=$(_ide_build "$root")
    [ -n "$build" ] || continue
    [ "${build%%.*}" -ge "$MIN_BUILD" ] 2>/dev/null || continue
    list="$list$build	$root
"
  done
  best=$(printf '%s' "$list" | sort -V | tail -1)
  [ -n "$best" ] || ide_die "no IDE of build $MIN_BUILD or newer with the Terminal plugin found. Set IDE_HOME, e.g. IDE_HOME=/Applications/CLion.app"
  root=${best#*	}
  echo "Using $(_ide_name "$root") (build $(_ide_build "$root")) at $root" >&2
  echo "$root"
}

# Prints a javac of version 21 or newer: the IDE's bundled runtime if it has one, then JAVA_HOME, then PATH.
# $1: the IDE root, if known (optional).
find_javac() {
  for c in \
    "${1:+$1/jbr/Contents/Home/bin/javac}" "${1:+$1/jbr/bin/javac}" \
    "${JAVA_HOME:+$JAVA_HOME/bin/javac}" "$(command -v javac 2>/dev/null || true)"; do
    [ -n "$c" ] && [ -x "$c" ] || continue
    major=$("$c" -version 2>&1 | sed -n 's/^javac \([0-9]*\).*/\1/p')
    [ -n "$major" ] && [ "$major" -ge 21 ] 2>/dev/null && { echo "$c"; return; }
  done
  ide_die "no JDK 21 or newer found. Set JAVA_HOME, or use an IDE that bundles a runtime with javac"
}
