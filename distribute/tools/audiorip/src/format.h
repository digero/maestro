#ifndef _format_H_
#define _format_H_


#include "audiorip.h"

using namespace std;


int ripwav(fstream *file,  std::streamoff filesize);
int ripogg(fstream *file,  std::streamoff filesize);
int ripmpeg(fstream *file, std::streamoff filesize);


#endif