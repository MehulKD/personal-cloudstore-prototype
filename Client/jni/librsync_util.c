/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
#include "librsync.h"
#include <jni.h>
#include<stdio.h>
#include<string.h>


/*
 *  * Class:     com_piasy_rsyncdemo_MainActivity
 *   * Method:    genSignature
 *    * Signature: (Ljava/lang/String;Ljava/lang/String;)I
 *     */
JNIEXPORT jint JNICALL Java_com_piasy_client_model_RsyncModel_genSignature
  (JNIEnv * env, jobject thiz, jstring oldFileName, jstring sigFileName)
{
	//__android_log_print(ANDROID_LOG_INFO, "JNIMsg", "get in genSig");
	const char * chOldFileName = (*env)->GetStringUTFChars(env, oldFileName, 0);
	//__android_log_print(ANDROID_LOG_INFO, "JNIMsg", "ok10");
	const char * chSigFileName = (*env)->GetStringUTFChars(env, sigFileName, 0);
	if (chOldFileName == NULL || chSigFileName == NULL)
	{
		(*env)->ReleaseStringUTFChars(env, oldFileName, chOldFileName);
		(*env)->ReleaseStringUTFChars(env, sigFileName, chSigFileName);
		return -1; //return value: -1 means illegal filename
	}
	else
	{
		FILE * oldFile = fopen(chOldFileName, "rb");
		FILE * sigFile = fopen(chSigFileName, "wb");
		rs_sig_file(oldFile, sigFile, 2048, 8, NULL);
		fclose(oldFile);
        fclose(sigFile);
	}
	(*env)->ReleaseStringUTFChars(env, oldFileName, chOldFileName);
	(*env)->ReleaseStringUTFChars(env, sigFileName, chSigFileName);
	//__android_log_print(ANDROID_LOG_INFO, "JNIMsg", "ok14");
    return 0;
}

/*
 *  * Class:     com_piasy_rsyncdemo_MainActivity
 *   * Method:    genDelta
 *    * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I
 *     */
JNIEXPORT jint JNICALL Java_com_piasy_client_model_RsyncModel_genDelta
  (JNIEnv * env, jobject thiz, jstring sigFileName, jstring newFileName, jstring deltaFileName)
{
	const char * chSigFileName = (*env)->GetStringUTFChars(env, sigFileName, 0);
	const char * chNewFileName = (*env)->GetStringUTFChars(env, newFileName, 0);
	const char * chDeltaFileName = (*env)->GetStringUTFChars(env, deltaFileName, 0);
	if (chSigFileName == NULL || chNewFileName == NULL || chDeltaFileName == NULL)
	{
		(*env)->ReleaseStringUTFChars(env, sigFileName, chSigFileName);
		(*env)->ReleaseStringUTFChars(env, newFileName, chNewFileName);
		(*env)->ReleaseStringUTFChars(env, deltaFileName, chDeltaFileName);
		return -1; //return value: -1 means illegal filename
	}
	else
	{
		FILE * sigFile = fopen(chSigFileName, "rb");
		rs_signature_t * mySig;
		rs_result result = rs_loadsig_file(sigFile, &mySig, NULL);
		fclose(sigFile);

        if (result != RS_DONE)
            return 1; //return value: 1 means runtime error

        if ((result = rs_build_hash_table(mySig)) != RS_DONE)
            return 1; //return value: 1 means runtime error

		FILE * newFile = fopen(chNewFileName, "rb");
		FILE * deltaFile = fopen(chDeltaFileName, "wb");
		rs_delta_file(mySig, newFile, deltaFile, NULL);
		rs_free_sumset(mySig);
		fclose(newFile);
        fclose(deltaFile);
	}
	(*env)->ReleaseStringUTFChars(env, sigFileName, chSigFileName);
	(*env)->ReleaseStringUTFChars(env, newFileName, chNewFileName);
	(*env)->ReleaseStringUTFChars(env, deltaFileName, chDeltaFileName);
    return 0;
}

  /*
   *  * Class:     com_piasy_rsyncdemo_MainActivity
   *   * Method:    patch
   *    * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I
   *     */
JNIEXPORT jint JNICALL Java_com_piasy_client_model_RsyncModel_patch
    (JNIEnv * env, jobject thiz, jstring oldFileName, jstring deltaFileName, jstring newFileName)
{
	const char * chOldFileName = (*env)->GetStringUTFChars(env, oldFileName, 0);
	const char * chDeltaFileName = (*env)->GetStringUTFChars(env, deltaFileName, 0);
	const char * chNewFileName = (*env)->GetStringUTFChars(env, newFileName, 0);
	if (chOldFileName == NULL || chNewFileName == NULL || chDeltaFileName == NULL)
	{
		(*env)->ReleaseStringUTFChars(env, oldFileName, chOldFileName);
		(*env)->ReleaseStringUTFChars(env, newFileName, chNewFileName);
		(*env)->ReleaseStringUTFChars(env, deltaFileName, chDeltaFileName);
		return -1; //return value: -1 means illegal filename
	}
	else
	{
		FILE * newFile = fopen(chNewFileName, "wb");
		FILE * oldFile = fopen(chOldFileName, "rb");
		FILE * deltaFile = fopen(chDeltaFileName, "rb");
		rs_patch_file(oldFile, deltaFile, newFile, NULL);
		fclose(newFile);
		fclose(oldFile);
        fclose(deltaFile);
	}
	(*env)->ReleaseStringUTFChars(env, oldFileName, chOldFileName);
	(*env)->ReleaseStringUTFChars(env, newFileName, chNewFileName);
	(*env)->ReleaseStringUTFChars(env, deltaFileName, chDeltaFileName);
    return 0;
}

