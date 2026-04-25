#!/bin/sh
# Maven Wrapper script
# Downloads and uses Maven automatically

set -e

MAVEN_HOME="${HOME}/.m2/wrapper/dists/apache-maven-3.9.6"
MAVEN_URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip"

if [ ! -f "${MAVEN_HOME}/bin/mvn" ]; then
    echo "Downloading Maven 3.9.6..."
    mkdir -p "${MAVEN_HOME}"
    TMPFILE=$(mktemp /tmp/maven-XXXXXX.zip)
    curl -fsSL "${MAVEN_URL}" -o "${TMPFILE}"
    unzip -qo "${TMPFILE}" -d "${MAVEN_HOME}/.."
    rm "${TMPFILE}"
fi

exec "${MAVEN_HOME}/bin/mvn" "$@"
