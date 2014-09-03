#!/bin/sh

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

# Get standard environment variables
PRGDIR=`dirname "$PRG"`

[ -z "$PROJECT_HOME" ] && PROJECT_HOME=`cd "$PRGDIR/../../.." ; pwd`

protoc -I=$PROJECT_HOME/src/main/proto --java_out=$PROJECT_HOME/src/main/java $PROJECT_HOME/src/main/proto/*.proto
