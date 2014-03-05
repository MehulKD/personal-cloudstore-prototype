# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# the purpose of this sample is to demonstrate how one can
# generate two distinct shared libraries and have them both
# uploaded in
#

LOCAL_PATH:= $(call my-dir)

# first lib, which will be built statically
#
include $(CLEAR_VARS)

LOCAL_MODULE    := librsync
LOCAL_SRC_FILES := librsync-0.9.7/base64.c		\
				   librsync-0.9.7/delta.c 		\
				   librsync-0.9.7/isprefix.c 	\
				   librsync-0.9.7/msg.c 		\
				   librsync-0.9.7/search.c 	\
				   librsync-0.9.7/sumset.c 	\
				   librsync-0.9.7/version.c 	\
				   librsync-0.9.7/buf.c 		\
				   librsync-0.9.7/emit.c 		\
				   librsync-0.9.7/job.c 		\
				   librsync-0.9.7/netint.c 	\
				   librsync-0.9.7/readsums.c 	\
				   librsync-0.9.7/snprintf.c 	\
				   librsync-0.9.7/checksum.c  	\
				   librsync-0.9.7/trace.c  	\
				   librsync-0.9.7/whole.c  	\
				   librsync-0.9.7/fileutil.c  	\
				   librsync-0.9.7/mdfour.c    	\
				   librsync-0.9.7/patch.c     	\
				   librsync-0.9.7/rollsum.c   	\
				   librsync-0.9.7/stats.c     	\
				   librsync-0.9.7/tube.c 		\
				   librsync-0.9.7/command.c   	\
				   librsync-0.9.7/hex.c       	\
				   librsync-0.9.7/mksum.c     	\
				   librsync-0.9.7/prototab.c  	\
				   librsync-0.9.7/scoop.c     	\
				   librsync-0.9.7/stream.c    	\
				   librsync-0.9.7/util.c
include $(BUILD_STATIC_LIBRARY)

# second lib, which will depend on and include the first one
#
include $(CLEAR_VARS)

LOCAL_MODULE    := librsync_util
LOCAL_SRC_FILES := librsync_util.c
LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_LDLIBS += -L$(SYSROOT)/usr/lib -llog
LOCAL_STATIC_LIBRARIES := librsync

include $(BUILD_SHARED_LIBRARY)
