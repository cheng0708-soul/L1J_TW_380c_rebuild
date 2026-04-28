#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAVA_CMD="java"

if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA_CMD="$JAVA_HOME/bin/java"
elif [ -x /usr/libexec/java_home ]; then
    JAVA8_HOME="$(/usr/libexec/java_home -v 1.8 2>/dev/null || true)"
    if [ -n "$JAVA8_HOME" ] && [ -x "$JAVA8_HOME/bin/java" ]; then
        JAVA_CMD="$JAVA8_HOME/bin/java"
    fi
fi

JAVA_VERSION="$("$JAVA_CMD" -version 2>&1 | head -n 1)"
case "$JAVA_VERSION" in
    *\"1.8*|*" version \"8"*)
        ;;
    *)
        echo "[warn] Java 8 not detected, using: $JAVA_VERSION" >&2
        ;;
esac

cd "$SCRIPT_DIR" || exit 1
exec "$JAVA_CMD" -Xmx512m -jar l1jserver.jar
