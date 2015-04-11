#ifndef _format_H_
#define _format_H_


#include "audiorip.h"

using namespace std;


int ripwav(fstream *file, int filesize);
int ripogg(fstream *file, int filesize);
int ripmpeg(fstream *file, int filesize);


#endif