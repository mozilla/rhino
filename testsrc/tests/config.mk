# -*- Mode: makefile -*-
# 
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

# This file was adapted from mozilla/js/src/config.mk

# Set os+release dependent make variables
OS_ARCH         := $(subst /,_,$(shell uname -s | sed /\ /s//_/))

# Attempt to differentiate between SunOS 5.4 and x86 5.4
OS_CPUARCH      := $(shell uname -m)
ifeq ($(OS_CPUARCH),i86pc)
OS_RELEASE      := $(shell uname -r)_$(OS_CPUARCH)
else
ifeq ($(OS_ARCH),AIX)
OS_RELEASE      := $(shell uname -v).$(shell uname -r)
else
OS_RELEASE      := $(shell uname -r)
endif
endif
ifeq ($(OS_ARCH),IRIX64)
OS_ARCH         := IRIX
endif

# Handle output from win32 unames other than Netscape's version
ifeq (,$(filter-out Windows_95 Windows_98 CYGWIN_95-4.0 CYGWIN_98-4.10, $(OS_ARCH)))
	OS_ARCH   := WIN95
endif
ifeq ($(OS_ARCH),WIN95)
	OS_ARCH	   := WINNT
	OS_RELEASE := 4.0
endif
ifeq ($(OS_ARCH), Windows_NT)
	OS_ARCH    := WINNT
	OS_MINOR_RELEASE := $(shell uname -v)
	ifeq ($(OS_MINOR_RELEASE),00)
		OS_MINOR_RELEASE = 0
	endif
	OS_RELEASE := $(OS_RELEASE).$(OS_MINOR_RELEASE)
endif
ifeq (CYGWIN_NT,$(findstring CYGWIN_NT,$(OS_ARCH)))
	OS_RELEASE := $(patsubst CYGWIN_NT-%,%,$(OS_ARCH))
	OS_ARCH    := WINNT
endif
ifeq ($(OS_ARCH), CYGWIN32_NT)
	OS_ARCH    := WINNT
endif

# Virtually all Linux versions are identical.
# Any distinctions are handled in linux.h
ifeq ($(OS_ARCH),Linux)
OS_CONFIG      := Linux_All
else
ifeq ($(OS_ARCH),dgux)
OS_CONFIG      := dgux
else
ifeq ($(OS_ARCH),Darwin)
OS_CONFIG      := Darwin
else
OS_CONFIG       := $(OS_ARCH)$(OS_OBJTYPE)$(OS_RELEASE)
endif
endif
endif

ifeq "$(TEST_OPTDEBUG)" "opt"
OBJDIR_TAG = _OPT
else
OBJDIR_TAG = _DBG
endif


# Name of the binary code directories
ifdef BUILD_IDG
JS_OBJDIR          = $(OS_CONFIG)$(OBJDIR_TAG).OBJD
else
JS_OBJDIR          = $(OS_CONFIG)$(OBJDIR_TAG).OBJ
endif

