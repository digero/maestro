#include "stdafx.h"
#include "audiorip.h"

bool do_extract = true;    //scan only, if false
bool xdir_created = false; //indicates if extraction directory was created already

char out_path[FNBUFSIZE] = {'\0'}; //output path with extraction dir
char xdir_suffix[FNBUFSIZE];

char *ifname;


/*************************************************************************
search bytewise next matching binary string in file, in a specific range
*************************************************************************/
//- uses buffer for comparison
//- strlen should no be longer than bufsize/2 (else there is much redundant file reading)
//- returns first byte position of occurence (first byte of matched sequence), or -1 if not found
//- "pos" is start offset in the data file
//- "slen" is search length in bytes, range in which the complete search string may be found
std::streamoff SearchString(fstream *file, std::streamoff pos, std::streamoff slen, const char *str, int strlen)
{
 int  i;
 char buf[SBUFSIZE];
 std::streamoff  nstrcmps;
 std::streamoff  nstrcmps_block;
 std::streamoff  nstrcmps_rest;
 int  restbytes;	//length of string - 1

 //clear error state
 file->clear();

 //if string is too long (buffer too small)
 if(SBUFSIZE < strlen) {
   cout << "string search error: buffer for comparison too small\n" << endl;
   return -1;
 }

 //if range is too short
 if((slen <= 0) || (slen < strlen))
   return -1;

 //determine total number of comparisons
 nstrcmps = slen - strlen + 1;
 //determine number of max possible comparisons in one buffer block
 nstrcmps_block = SBUFSIZE - strlen + 1;
 //determine rest comparisons, where the buffer is not filled completly
 nstrcmps_rest = nstrcmps % nstrcmps_block;
 //determine rest bytes, that cannot be compared (where a comparison of the whole string is not possible anymore)
 restbytes = strlen - 1;

 //seek file position
 file->seekg(pos, ios::beg);
 //read rest bytes to buffer
 file->read(buf, restbytes);
 
 //compare whole blocks
 while(nstrcmps >= nstrcmps_block) {
    //fill buffer completly
    file->read(buf + restbytes, SBUFSIZE - restbytes);
 
    //compare a block
    for(i = 0; i < nstrcmps_block; i++)
       if(memcmp(buf + i, str, strlen) == 0)
          return pos + i;

    pos += nstrcmps_block;
    nstrcmps -= nstrcmps_block; 
    //copy rest bytes of block to beginning (-> no need to read again from file)
    memcpy(buf, buf + SBUFSIZE - restbytes, restbytes);
 }

 //compare rest
 file->read(buf + restbytes, nstrcmps_rest);
 for(i = 0; i < nstrcmps_rest; i++)
    if(memcmp(buf + i, str, strlen) == 0)
       return pos + i;

 return -1;
}



/*************************************************************************
search bytewise next matching byte
*************************************************************************/
//- byte search buffer should be very small to avoid redundant reading of data
//  -> when having a match at the beginning of the buffer, the rest might be ignored when file pointer is repositioned before extraction
#define BS_BUFSIZE 512
std::streamoff SearchByte(fstream *file, std::streamoff pos, std::streamoff searchlen, char byte)
{
 int  i;
 char buf[BS_BUFSIZE];

 //clear error state
 file->clear();

 //seek file position
 file->seekg(pos, ios::beg);
 
 //search full blocks
 while(searchlen > BS_BUFSIZE)
 {
   file->read(buf, BS_BUFSIZE);
   for(i = 0; i < BS_BUFSIZE; i++)
     if(buf[i] == byte)
       return pos + i;
   pos += BS_BUFSIZE;
   searchlen -= BS_BUFSIZE;
 }
 
 //search rest
 file->read(buf, searchlen);
 for(i = 0; i < searchlen; i++)
   if(buf[i] == byte)
     return pos + i;
 
 return -1;
}



/******************************
extract file (using buffer)
*******************************/
int ExtractFile(fstream *srcf, std::streamoff srcofs, char *fname, std::streamoff nbytes)
{
 if(!do_extract) {
	 cout << " At position " << srcofs << ", bytes: " << nbytes << endl;
	 return 0;
 }

 if(!xdir_created) {
   if(createDir() != 0)
     return -1;
   else
     xdir_created = true;
 }

 fstream dstf;
 char buf[CPBUFSIZE];

 //check parameters for validity
 if (srcofs < 0) {
   cerr << "Extraction error: invalid start offset" << endl;
   return -1;
 }
 if (nbytes < 0) {
   cerr << "Extraction error: invalid byte length" << endl;
   return -1;
 }

 //clear possible error states caused by previous access
 srcf->clear();
 //set start offset of source
 srcf->seekg(srcofs,ios::beg);

 char dstp[FNBUFSIZE]; //destination path with file name
 strcpy(dstp, out_path);
 strcat(dstp, fname);

 //open out file
 dstf.open(dstp, ios::out | ios::binary);
 if(dstf.is_open() == 0) {
   cout << "error: can't create file \"" << dstp << "\"" << endl;
   return -1;
 }

 cout << "Extracting to file \"" << fname << "\"" << endl;
 cout << " from position " << srcofs << ", bytes: " << nbytes;

 //copy whole blocks
 while(nbytes >= CPBUFSIZE) {
   srcf->read(buf, CPBUFSIZE);
   dstf.write(buf, CPBUFSIZE);
   nbytes -= CPBUFSIZE;
 }

 //copy rest
 srcf->read(buf, nbytes);
 dstf.write(buf, nbytes);
 dstf.close();

 cout << " ...done" << endl;
 return 0;
}




/*******************************
create a numbered file name
********************************/
//-with a specified number of digits, a "prefix" and a file extension
//-note "fname" must be large enough for: prefix, ndigits, ext !!!
// parameters
//   val:     the number for the string
//   ndigits: number of digits for the number (prefixed with "0"s
//   fname:   string buffer for resulting string (must be large enough)
void mkfname(int val, int ndigits, char *fname, const char *prefix, const char *ext)
{
 // the sprintf format string must be dynamically generated considering "ndigits"
 //  so we create it using another "sprintf()"
 char formstring[50];
 sprintf(formstring, "%%s%%0%dd%%s", ndigits);  //"%%" results in '%' in the string
 sprintf(fname, formstring,   prefix, val,ext);
}



/*********************
Create directory
**********************/
// create a directory in current working directory or at specified path
int createDir()
{
 int i = 0;
 int error = 0;
 char xdir[FNBUFSIZE] = {'\0'};	//extraction path and dir
 
 if(strlen(out_path) == 0)
   strcat(out_path, "./");
 else {
   char c = out_path[ strlen(out_path) - 1];
   if( c != '/'  &&  c != '\\' )  //make sure string ends with slash
     strcat(out_path, "/");
 }
 strcat(out_path, "AUDIORIP_");
 
 do
 {
   mkfname(i, 3, xdir, out_path, xdir_suffix);
   error = _mkdir(xdir);
   if(((error != 0) && (errno != EEXIST)) || (i > 999)) {
       cout << "Can't create extraction dir: \"" << xdir << "\""<< endl;;
       switch(errno) {
         case(EACCES) : cout << "Write permission is denied for the parent directory in which the new directory is to be added."; break;
         case(EMLINK) : cout << "The parent directory has too many links (entries)."; break;
         case(ENAMETOOLONG) : cout << "Path or name to long."; break;
         case(ENOENT) : cout << "A component of the path prefix does not name an existing directory."; break;
         case(ENOSPC) : cout << "The file system doesn't have enough room to create the new directory."; break;
         case(EROFS)  : cout << "The parent directory of the directory being created is on a read-only file system and cannot be modified."; break;
         default      : cout << strerror(errno); break;
       }
       cout << endl;
       return -1;
   }
   i++;
 } while(error!=0);

 strcat(xdir,"/");
 strcpy(out_path, xdir);
 return 0;
}



/*********************
Create directory
**********************/
void printResult(int n, const char *type)
{
  cout << ">>> Finished: " << n << " " << type << " files found" << (do_extract ? "/extracted" : "") << endl << endl;
}





/* Main Program ##############################################################*/

/*********************
print help
**********************/
void printhelp()
{
 cout << endl;
 cout << "-- audiorip help --" << endl;
 cout << "Version: " << VERSION << endl;
 cout << "Internet: http://murkymind.de/projects/audiorip" << endl;
 cout << "Purpose: Scans a file and extracts audio data to separate directory" << endl;
 cout << "Usage: audiorip \"file\" [options]" << endl;
 cout << "       Extracts all file types by default (without options)." << endl;
 cout << "Options:" << endl;
 cout << " -mpeg     : Extract MPEG audio" << endl;
 cout << " -ogg      : Extract Ogg Vorbis" << endl;
 cout << " -wav      : Extract Riff Wave" << endl;
 cout << " -h        : Print this help" << endl;
 cout << " -s        : Scan and list only, don't extract" << endl;
 cout << " -o <path> : Create extraction directory under the given path" << endl;
}


/***************************
open file, handle parameter
****************************/
int main(int argc, char *argv[])
{
 std::streamoff  filesize;		//size of scanned file

 //check parameter options
 bool scan_wav = false;  // default value
 bool scan_ogg = false;
 bool scan_mpeg = false;

 if(argc > 2)
 {
   for(int i = 2; i < argc; i++)
   {
      if(strcmp(argv[i], "-wav") == 0)
        scan_wav = true;
      else if(strcmp(argv[i], "-ogg") == 0)
        scan_ogg = true;
      else if(strcmp(argv[i], "-mpeg") == 0)
        scan_mpeg = true;
		
      else if(strcmp(argv[i], "-h") == 0){
        printhelp();
        return 0;
      }
      else if(strcmp(argv[i], "-s") == 0)
        do_extract = false;
	  else if(strcmp(argv[i], "-o") == 0){
        i++;
		if(argv[i] == NULL){
			cout << "Output path missing" << endl;
			printhelp();
			return -1;
		}
	    strcpy(out_path, argv[i]);
	  }
	  else {
		  cout << "Unknown parameter: \"" << argv[i] << "\""<< endl;
		  printhelp();
		  return -1;
	  }
   }
 }

 if((!scan_wav) && (!scan_ogg) && (!scan_mpeg)){
   //no format options -> scan for all by default
   scan_wav = true;
   scan_ogg = true;
   scan_mpeg = true;
 }

 //open file
 if(argc < 2) {
   cout << "No source file specified." << endl;
   printhelp();
   return -1;
 }

 fstream file;
 file.open(argv[1], ios::in | ios::binary);
 if(file.is_open() == 0) {
   cout << "Can't open file \"" << argv[1] << "\"" << endl; 
   printhelp();
   return -1;
 }

 //determine filesize of data file
 file.seekg(0, ios::end); 
 filesize = file.tellg();

 //extract the file name, if a directory path was given (the last part behind "/" or "\")
 ifname = argv[1];
 char *tptr = strtok(ifname, "/");
 while(tptr != NULL) {
   ifname = tptr;
   tptr = strtok(NULL, "/");
 }
 tptr = strtok(ifname, "\\");   // For MS Windows or DOS pathes
 while(tptr != NULL) {       
   ifname = tptr;
   tptr = strtok(NULL, "\\");
 }

 //suffix (source file's name) for the extraction dir
 sprintf(xdir_suffix, "_[%s]", ifname);

 //scan and extract
 if(scan_wav == true)  ripwav(&file, filesize);
 if(scan_ogg == true)  ripogg(&file, filesize);
 if(scan_mpeg == true) ripmpeg(&file, filesize);

 if(do_extract){
   if(xdir_created)
     cout << ">>>>>  Extraction dir: \"" << out_path << "\"" << endl;
 }
 else
   cout << ">>>>>  Scan mode, nothing extracted" << endl;


 file.close();
 return 0;
}


