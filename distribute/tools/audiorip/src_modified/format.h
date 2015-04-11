#ifndef _format_H_
#define _format_H_


#include "audiorip.h"

using namespace std;


std::streamoff ripwav(fstream *file, std::streamoff filesize);
std::streamoff ripogg(fstream *file, std::streamoff filesize);
std::streamoff ripmpeg(fstream *file, std::streamoff filesize);


#endif