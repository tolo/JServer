/*
 * Copyright 2007 the project originators.
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
 */

#include "SystemInfo.h"
#include "com_teletalk_jserver_util_SystemInfo.h"

BOOL APIENTRY DllMain( HANDLE hModule, 
                       DWORD  ul_reason_for_call, 
                       LPVOID lpReserved
					 )
{
    return TRUE;
}

/* Inaccessible static: libraryLoaded */
/*
 * Class:     com_teletalk_jserver_util_FileSystemInfo
 * Method:    getDiskFreeSpaceNative
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_com_teletalk_jserver_util_SystemInfo_getDiskFreeSpaceNative
		(JNIEnv *env, jclass, jstring path)
{
	LPSTR diskPath;
	
	diskPath = (LPSTR)env->GetStringUTFChars(path, NULL);

	if(diskPath != NULL)
	{
		ULARGE_INTEGER freeBytesAvailable;
		ULARGE_INTEGER totalNumberOfBytes;
		ULARGE_INTEGER totalNumberOfFreeBytes;
  
		int result = GetDiskFreeSpaceEx(diskPath,  // directory name
		(PULARGE_INTEGER)&freeBytesAvailable,    // bytes available to caller
		(PULARGE_INTEGER)&totalNumberOfBytes,    // bytes on disk
		(PULARGE_INTEGER)&totalNumberOfFreeBytes); // free bytes on disk

		if( result != 0 )
		{
			return totalNumberOfFreeBytes.QuadPart;
		}
		else
		{
			return -1;
		}
	}
	else
	{
		return -1;
	}
}

