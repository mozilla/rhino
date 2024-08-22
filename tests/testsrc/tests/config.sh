# -*- Mode: Shell-script; tab-width: 4; indent-tabs-mode: nil; -*-
# 
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

# This file was adapted from mozilla/js/src/config.mk

# Set os+release dependent make variables
OS_ARCH="`uname -s | sed /\ /s//_/`"

# Attempt to differentiate between SunOS 5.4 and x86 5.4
OS_CPUARCH=`uname -m`

if [[ "$OS_CPUARCH" == "i86pc" ]]; then
    OS_RELEASE="`uname -r`_$OS_CPUARCH"
elif [[ "$OS_ARCH" == "AIX" ]]; then
    OS_RELEASE="`uname -v`.`uname -r`"
else
    OS_RELEASE="`uname -r`"
fi

if [[ "$OS_ARCH" == "IRIX64" ]]; then
    OS_ARCH="IRIX"
fi

# Handle output from win32 unames other than Netscape's version
if echo "Windows_95 Windows_98 CYGWIN_95-4.0 CYGWIN_98-4.10" | grep -iq "$OS_ARCH"; then
    OS_ARCH="WIN95"
fi

if [[ "$OS_ARCH" == "WIN95" ]]; then
    OS_ARCH="WINNT"
    OS_RELEASE="4.0"
fi

if [[ "$OS_ARCH" == "Windows_NT" ]]; then
    OS_ARCH="WINNT"
    OS_MINOR_RELEASE="`uname -v`"

    if [[ "$OS_MINOR_RELEASE" == "00" ]]; then
        OS_MINOR_RELEASE=0
    fi

    OS_RELEASE="$OS_RELEASE.$OS_MINOR_RELEASE"
fi

if echo "$OS_ARCH" | grep -iq CYGWIN_NT; then
    OS_RELEASE="`echo $OS_ARCH|sed 's/CYGWIN_NT-//'`"
    OS_ARCH="WINNT"
fi

if [[ "$OS_ARCH" == "CYGWIN32_NT" ]]; then
    OS_ARCH="WINNT"
fi

if echo "$OS_ARCH" | grep -iq MINGW32_NT; then
    OS_RELEASE="`echo $OS_ARCH|sed 's/MINGW32_NT-//'`"
    OS_ARCH="WINNT"
fi

if [[ "$OS_ARCH" == "MINGW32_NT" ]]; then
    OS_ARCH="WINNT"
fi

# Virtually all Linux versions are identical.
# Any distinctions are handled in linux.h

case "$OS_ARCH" in
    "Linux")
        OS_CONFIG="Linux_All"
        ;;
    "dgux")
        OS_CONFIG="dgux"
        ;;
    "Darwin")
        OS_CONFIG="Darwin"
        ;;
    *)
        OS_CONFIG="$OS_ARCH$OS_OBJTYPE$OS_RELEASE"
        ;;
esac

case "$buildtype" in
    "opt")
        OBJDIR_TAG="_OPT"
        ;;
    "debug")
        OBJDIR_TAG="_DBG"
        ;;
    *)
        error "Unknown build type $buildtype"
esac



# Name of the binary code directories
if [[ -z "$OBJDIR_TAG" ]]; then
    true
elif [[ -n "$BUILD_IDG" ]]; then
    JS_OBJDIR="$OS_CONFIG$OBJDIR_TAG.OBJD"
else
    JS_OBJDIR="$OS_CONFIG$OBJDIR_TAG.OBJ"
fi

