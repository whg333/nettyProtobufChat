#!/usr/bin/env bash

# The Sango command script
#
# Environment Variables
#
#   JAVA_HOME        The java implementation to use.  Overrides JAVA_HOME.
#
#   JAVA_GC_TUNING   The java gc tuning options to use.
#
#   SANGO_CLASSPATH Extra Java CLASSPATH entries.
#
#   SANGO_HEAPSIZE  The maximum amount of heap to use, in MB. 
#
#   SANGO_OPTS      Extra Java runtime options.
#   

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
os400=false
darwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
OS400*) os400=true;;
Darwin*) darwin=true;;
esac

if [ -z "$DEBUG_PORT"] ; then
  DEBUG_PORT="8000"
fi

if [ "$1" = "debug" ] ; then
  DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,address=$DEBUG_PORT,server=y,suspend=n"
  shift
fi
if [ "$1" = "suspend" ] ; then
  DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,address=$DEBUG_PORT,server=y,suspend=y"
  shift
fi

if [ "$DEBUG_OPTS" ] ; then
  if [ -z "$JAVA_OPTS" ]; then
    JAVA_OPTS=$DEBUG_OPTS
  else
    JAVA_OPTS="$JAVA_OPTS $DEBUG_OPTS"
  fi
fi

if [ -z "$SANGO_CLASS" ] && [ -z "$1" ]; then
  echo "No main class is provided"
  exit 1
fi

if [ -z "$WEBINF_HOME" ]; then
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
  
  [ -z "$WEBINF_HOME" ] && WEBINF_HOME=`cd "$PRGDIR/.." ; pwd`
fi

if [ -r "$WEBINF_HOME"/classes/setenv.sh ]; then
  BASEDIR="$WEBINF_HOME"
  . "$WEBINF_HOME"/classes/setenv.sh
else
  echo "Cannot find $WEBINF_HOME/classes/setenv.sh"
  echo "This file is needed to run this program"
  exit 1
fi

if [ -x "$BASEDIR"/classes/setopt.sh ]; then
  . "$BASEDIR"/classes/setopt.sh
fi

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin; then
  [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
  [ -n "$JRE_HOME" ] && JRE_HOME=`cygpath --unix "$JRE_HOME"`
  [ -n "$WEBINF_HOME" ] && WEBINF_HOME=`cygpath --unix "$WEBINF_HOME"`
  [ -n "$CLASSPATH" ] && CLASSPATH=`cygpath --path --unix "$CLASSPATH"`
fi

if $darwin; then
  JAVA_OPTS="$JAVA_OPTS -d32"
fi

# check envvars which might override default args
if [ "$SANGO_HEAPSIZE" != "" ]; then
  JAVA_HEAP_MAX="-Xmx""$SANGO_HEAPSIZE""m"
fi

if [ "$JAVA_HEAP_MAX" == "" ]; then
  JAVA_HEAP_MAX="-Xmx2000m"
fi

for lib in ${WEBINF_HOME}/lib/*.jar; do
  if [ -n "$CLASSPATH" ] ; then
    CLASSPATH=$CLASSPATH:$lib
  else
    CLASSPATH=$lib
  fi
done

CLASSPATH=$CLASSPATH:${WEBINF_HOME}/classes

# so that filenames w/ spaces are handled correctly in loops below
IFS=

# add user-specified CLASSPATH last
if [ "$SANGO_CLASSPATH" != "" ]; then
  CLASSPATH=${CLASSPATH}:${SANGO_CLASSPATH}
fi

# restore ordinary behaviour
unset IFS

SANGO_OPTS='-Dfile.encoding=UTF-8'

# cygwin path translation
if $cygwin; then
  CLASSPATH=`cygpath -p -w "$CLASSPATH"`
  WEBINF_HOME=`cygpath -d "$WEBINF_HOME"`
  SANGO_LOG_DIR=`cygpath -d "$SANGO_LOG_DIR"`
  TOOL_PATH=`cygpath -p -w "$TOOL_PATH"`
fi

# run it
ulimit -c unlimited
exec "$RUNJAVA" $JAVA_OPTS $JAVA_HEAP_MAX $SANGO_OPTS -classpath "$CLASSPATH" $SANGO_CLASS "$@"
