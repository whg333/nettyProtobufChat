#!/bin/sh

# Make sure prerequisite environment variables are set
if [ -z "$JAVA_HOME" -a -z "$JRE_HOME" ]; then
  if $darwin; then
    if [ -d "/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Home" ]; then
      export JAVA_HOME="/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Home"
    fi
  else
    JAVA_PATH=`which java 2>/dev/null`
    if [ "x$JAVA_PATH" != "x" ]; then
      JAVA_PATH=`dirname $JAVA_PATH 2>/dev/null`
      JRE_HOME=`dirname $JAVA_PATH 2>/dev/null`
    fi
    if [ "x$JRE_HOME" = "x" ]; then
      if [ -x /usr/bin/java ]; then
        JRE_HOME=/usr
      fi
    fi
  fi
  if [ -z "$JAVA_HOME" -a -z "$JRE_HOME" ]; then
    echo "Neither the JAVA_HOME nor the JRE_HOME environment variable is defined"
    echo "At least one of these environment variable is needed to run this program"
    exit 1
  fi
fi

if [ -z "$JAVA_HOME" -a "$1" = "debug" ]; then
  echo "JAVA_HOME should point to a JDK in order to run in debug mode."
  exit 1
fi

if [ -z "$JRE_HOME" ]; then
  JRE_HOME="$JAVA_HOME"
fi

if [ -z "$BASEDIR" ]; then
  echo "The BASEDIR environment variable is not defined"
  echo "This environment variable is needed to run this program"
  exit 1
fi
if [ ! -x "$BASEDIR"/classes/setenv.sh ]; then
  if $os400; then
    # -x will Only work on the os400 if the files are:
    # 1. owned by the user
    # 2. owned by the PRIMARY group of the user
    # this will not work if the user belongs in secondary groups
    eval
  else
    echo "The BASEDIR environment variable is not defined correctly"
    echo "This environment variable is needed to run this program"
    exit 1
  fi
fi

# Set standard CLASSPATH
if [ "$1" = "debug" -o "$1" = "javac" ] ; then
  if [ -f "$JAVA_HOME"/lib/tools.jar ]; then
    CLASSPATH="$JAVA_HOME"/lib/tools.jar
  fi
fi

for lib in "$BASEDIR"/lib/*.jar
do
  CLASSPATH=$CLASSPATH:$lib
done

# Set standard commands for invoking Java.
RUNJAVA="$JRE_HOME"/bin/java
