#!/usr/bin/env bash
set -euo pipefail

PACKAGE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
UPSTREAM_DIR="$PACKAGE_DIR/upstream"
ARTIFACT_DIR="$PACKAGE_DIR/artifacts"
LIB_DIR="$ARTIFACT_DIR/lib"
BRIDGE_SOURCE_DIR="$PACKAGE_DIR/src/bridge"
BRIDGE_CLASSES_DIR="$PACKAGE_DIR/.build/bridge-classes"
INIT_SCRIPT="$PACKAGE_DIR/.build/print-runtime-classpath.init.gradle"

mkdir -p "$ARTIFACT_DIR" "$PACKAGE_DIR/.build"

cat > "$INIT_SCRIPT" <<'GRADLE'
allprojects {
    if (project.path == ':core') {
        tasks.register('printRuntimeClasspath') {
            doLast {
                configurations.runtimeClasspath.files.each { println it.absolutePath }
            }
        }
    }
}
GRADLE

cd "$UPSTREAM_DIR"
./gradlew :core:jar -x test

CORE_JAR="$(find "$UPSTREAM_DIR/core/build/libs" -maxdepth 1 -type f -name 'core-*.jar' \
  ! -name '*-sources.jar' ! -name '*-javadoc.jar' | sort | head -n1)"
if [[ -z "$CORE_JAR" ]]; then
  echo "Could not find the generated Core Jar." >&2
  exit 2
fi

RUNTIME_JARS="$(
  ./gradlew -q --no-daemon --init-script "$INIT_SCRIPT" :core:printRuntimeClasspath
)"

rm -rf "$LIB_DIR"
mkdir -p "$LIB_DIR"
while IFS= read -r dependency; do
  [[ -z "$dependency" ]] && continue
  cp "$dependency" "$LIB_DIR/"
done <<< "$RUNTIME_JARS"

cp "$CORE_JAR" "$ARTIFACT_DIR/openrocket-core-runtime.jar"
cp "$CORE_JAR" "$ARTIFACT_DIR/openrocket-core.jar"

rm -rf "$BRIDGE_CLASSES_DIR"
mkdir -p "$BRIDGE_CLASSES_DIR"
mapfile -t BRIDGE_SOURCES < <(find "$BRIDGE_SOURCE_DIR" -name '*.java' -print)
javac -cp "$ARTIFACT_DIR/openrocket-core-runtime.jar:$LIB_DIR/*" \
  -d "$BRIDGE_CLASSES_DIR" \
  "${BRIDGE_SOURCES[@]}"

jar cfe "$ARTIFACT_DIR/openrocket-core-bridge.jar" \
  com.example.openrocket.browser.CoreBridge -C "$BRIDGE_CLASSES_DIR" .

{
  printf '{\n  "coreJar": "openrocket-core-runtime.jar",\n'
  printf '  "bridgeJar": "openrocket-core-bridge.jar",\n'
  printf '  "dependencies": [\n'
  find "$LIB_DIR" -maxdepth 1 -type f -name '*.jar' -printf '    "%f",\n' |
    sort | sed '$ s/,$//'
  printf '  ]\n}\n'
} > "$ARTIFACT_DIR/runtime-manifest.json"

echo '{"ok":false,"message":"not run yet"}' > "$ARTIFACT_DIR/core-result.json"

echo "Built package in $ARTIFACT_DIR"
find "$ARTIFACT_DIR" -maxdepth 2 -type f -printf '%p %s bytes\n' | sort
