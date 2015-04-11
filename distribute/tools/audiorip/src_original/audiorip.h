/*
 Purpose: Scans and extracts audio data from any files
 Author: M. Wolf
 Internet: http://www.murkymind.de
 License: LGPL

 Note: Interleaved data cannot be detected, audio must be a continuous byte stream
 
 Todo:
   put each format related code into a separate file (unsure if useful)
   parallel scan mode -> check for all formats parallely using the cache so that data is only read once from the disk (store scanning state for each format in a specific structure)

 Program changes sinced first release:
  29-Nov-2007: MPEG: "split file on channel mode change" added (nearly all players continue playing with wrong channel mode, that sounds like playing with wrong sample rate)
  11-Jan-2010: code restructured; additional options added; create extraction dir only when extracting first file
			   character arrays size for path increased (buffer overflow was possible)

 compiled and tested with:
   linux gcc
   mingw
*/

#ifndef _audiorip_H_
#define _audiorip_H_



#include <iostream>
#include <fstream>
#include <stdlib.h>

#include <stdio.h>
#include <string.h>
#include <direct.h>           // functions to read and create directory (POSIX)
#include <sys/stat.h>         // "mkdir" may be also defined here on some systems

#include "format.h"

using namespace std;


//here we define the function for creating a directory, for a specific system adjustments may be neccessary
#ifdef __unix__
 #define MKDIR(X)    mkdir(X, 0777)
#else
 #define MKDIR(X)    mkdir(X)   
#endif



/* Common   ####################################################################*/
#define VERSION "1.0004, Jan-2010 (GNU g++)"

//some static definitions
#define CPBUFSIZE  4096		//buffer size for copying data to new file (no restrictions)
#define SBUFSIZE   4096		//search buffer size for string comparison (strlen must not exceed buffer size)
#define FNBUFSIZE  1000     //filename buffer


//prototypes
int SearchString(fstream *file, int pos, int slen, const char *str, int strlen);
int SearchByte(fstream *file, int pos, int searchlen, char byte);
int ExtractFile(fstream *srcf, int srcofs, char *fname, int nbytes);
void mkfname(int val, int ndigits, char *fname, const char *prefix, const char *ext);
int createDir();
void printResult(int n, const char *type);
void printhelp();
int main(int argc, char *argv[]);




#endif
