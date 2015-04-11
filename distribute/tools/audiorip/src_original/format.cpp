#include "format.h"



/* Wave ######################################################################*/

/******************************
find and extract wav file
*******************************/
int ripwav(fstream *file, int filesize)
{
 int  cnt;          //counter for extracted file
 int  pos;
 int  outsize;
 char fname[FNBUFSIZE];   //file name
 char strcmpbuf[5];

 //init position and file counter
 pos = 0;
 cnt = 0;
 cout << "Searching for MS Riff Wave..." << endl;

 while(1) {
   //search for "RIFFxxxxWAVE"
   pos = SearchString(file, pos, filesize - pos, "RIFF", 4);
   if(pos == -1)
     break;
   file->seekg(pos + 8, ios::beg);
   file->read(strcmpbuf, 4);
   if(memcmp(strcmpbuf, "WAVE", 4) != 0) {
     pos += 4;
     continue;
   }
   //get size of .wav file (4 bytes after "RIFF")
   file->seekg(pos+4,ios::beg);
   outsize = 8 + file->get() + (file->get()<<8) + (file->get()<<16) + (file->get()<<24);
   //create file name for extraction
   mkfname(cnt, 8, fname, "", ".wav");
   //extract audio data to new file
   if(ExtractFile(file, pos, fname, outsize) == -1)
     return -1;
   //update number
   pos += outsize;
   cnt++;
 }
 //print statistic
 printResult(cnt, ".wav");
 return 0;
}


/* Ogg Vorbis ################################################################*/
// Ogg bitstream is made of concatenated "pages" (headers and vorbis packets)
// We only need to find the first page (marked with flag) and step through the following until the last one (marked with another flag) is found

#define OGG_1ST_PAGESIZE  58  //size of start page of Ogg bitstream
#define OGG_MIN_PAGESIZE  27  //minimum size of a page

/******************************
find and extract Ogg Vorbis file
*******************************/
//minimal page header size is 27 bytes
int ripogg(fstream *file, int filesize)
{
 int i;
 int cnt;
 int pos;
 int outsize;
 char fname[FNBUFSIZE];
 char strbuf[4];

 int nsegments;
 int pagesize;
 int flags;

 cnt = 0;
 pos = 0;  //file position to extract from
 cout << "Searching for Ogg Vorbis..." << endl;

 while(1)
 {
   //search for sync word (page)
   pos = SearchString(file, pos, filesize - pos, "OggS", 4);
   if(pos == -1)
     break;
   //check if a valid ogg file fits into the rest of file
   if(pos + OGG_1ST_PAGESIZE + OGG_MIN_PAGESIZE > filesize) {
     pos += 4;
     continue;  //fail
   }
   //check flags if first page
   file->seekg(pos + 5, ios::beg);
   flags = file->get();
   if(!(flags & 0x2)) {
     pos += 4;
     continue;  //fail
   }
   // check identification header (without it, Ogg stream may contain not Vorbis data)
   // ... not implemented yet, maybe never because this way all Ogg encapsulated formats are found
   
   //check if another page follows the first (58 bytes)
   file->seekg(pos + OGG_1ST_PAGESIZE, ios::beg);
   file->read(strbuf, 4);
   if(memcmp(strbuf,"OggS",4) != 0) {
     pos += 4;
     continue;  //fail
   }

   //at this point: valid Ogg bitstream file

   outsize = 0;

   //stop if "end of stream" is set
   //at the beginning of the loop "flags" has value of first page
   while(!(flags & 0x4))
   {
     //check page id
     file->seekg(pos + outsize, ios::beg);
     file->read(strbuf,4);
     if(memcmp(strbuf,"OggS",4) != 0)
       break;
     //get flags of current page
     file->seekg(pos + outsize + 5, ios::beg);
     flags= file->get();
     //get number of segments in page
     file->seekg(pos + outsize + 26, ios::beg);
     nsegments = file->get();
     //calc page size
     pagesize = OGG_MIN_PAGESIZE + nsegments;
     for(i = 0; i < nsegments; i++)
       pagesize += file->get();
     //check if the page fits completly into the remaining file
     if(pos + outsize + pagesize > filesize)
       break;
     outsize += pagesize;
	 
     //check if there can be another page in the remaining file
     if(pos + outsize + OGG_MIN_PAGESIZE > filesize)
       break;
   }

   //check if a valid file
   if(outsize == 0) {
     pos += 4;
     continue;  //fail
   }
   
   //at this point: range of valid file found -> extract
   
   //make file name
   mkfname(cnt, 8, fname, "", ".ogg");
   //extract audio data to new file
   if(ExtractFile(file, pos, fname, outsize) == -1)
     return -1;
   //update number
   cnt++;
   pos += outsize;
 }

 printResult(cnt, ".ogg");
 return 0;
}






/* MPEG ######################################################################*/
#define MPEG_VERSION1   0x3 //11 MPEG 1
#define MPEG_VERSION2   0x2 //10 MPEG 2
#define MPEG_VERSION2_5 0x0 //00 MPEG 2.5

#define MPEG_LAYER1 0x3 //11 - layer 1
#define MPEG_LAYER2 0x2 //10 - layer 2
#define MPEG_LAYER3 0x1 //01 - layer 3

#define MPEG_MINFRAMES 8	  // there must be at least this number of frames following one another for extraction
#define ID3V2_MAXLEN   2000	  // length in which the id3v2 tag is searched before first mpeg frame (at least 10 bytes)

/********************************
find and extract mpeg audio
*********************************/

int ripmpeg(fstream *file, int filesize)
{
 // tables

 int mpeg_freq[3][3] =
 {
  {44100, 22050, 11025}, // 00
  {48000, 24000, 12000}, // 01
  {32000, 16000,  8000}  // 10
 };

 short mpeg_bitrate[15][5] =
 {
  {0,    0,   0,  0,  0},  // 0000
  {32,  32,  32, 32,  8},  // 0001
  {64,  48,  40, 48, 16},  // 0010
  {96,  56,  48, 56, 24},  // 0011
  {128, 64,  56, 64, 32},  // 0100
  {160, 80,  64, 80, 40},  // 0101
  {192, 96,  80, 96, 48},  // 0110
  {224,112,  96,112, 56},  // 0111
  {256,128, 112,128, 64},  // 1000
  {288,160, 128,144, 80},  // 1001
  {320,192, 160,160, 96},  // 1010
  {352,224, 192,176,112},  // 1011
  {384,256, 224,192,128},  // 1100
  {416,320, 256,224,144},  // 1101
  {448,384, 320,256,160}   // 1110
 };


 /*
 audio samples per frame, needed to calculate the frame size
 MPEG 1 Layer 1 = 384
 MPEG 1 Layer 2 = 1152
 MPEG 1 Layer 3 = 1152

 MPEG 2 Layer 1 = 384
 MPEG 2 Layer 2 = 1152
 MPEG 2 Layer 3 = 576
 */

 short mpeg_spf[3][2] =      // number of sample frames in a mpeg frame (needed to calculate frame size)
 {
  //MPEG 1  MPEG 2(2.5)
  { 384,    384},   // layer 1
  {1152,   1152},   // layer 2
  {1152,    576}    // layer 3
 };
 

 int  i;
 int  pos;
 int  outsize;
 int  cnt;          //count extracted files
 char fname[FNBUFSIZE];   //file name
 char strbuf[5];

 unsigned char head[4];   // buffer for the current frame header, must be compatible to the first header of the current file
 int  framesize;
 int  framecnt;      // frame counter for current mpeg stream

 char  type;         // MPEG type: 1,2,2.5
 char  layer;        // 1,2,3
 short bitrate;
 int   samplerate;
 char  padding;      // padding bit
 int   channelmode;  // channel mode (should be constant within a stream)

 //for comparison with the first frame, must remain the same in one mpeg stream
 char first_type;
 char first_layer;
 int  first_samplerate;
 int  first_channelmode;   
 
 pos = 0;
 cnt = 0;
 cout << "Searching for MPEG audio..." << endl;


 while(1) 
 {
   //search for 0xFF (SYNC) byte, to find a possible first frame
   pos = SearchByte(file, pos, (filesize - pos), 0xff);
   if(pos == -1)
     break;
     
   //prepare
   framecnt = 0;
   outsize = 0;

   //count valid frames, look at the frame headers only and skip the data to find the next header
   while(1)
   {
      file->seekg(pos + outsize, ios::beg);
      //read 4 header bytes
      file->read((char*)head, 4);

      //check 11 sync bits
      if((head[0] != 0xFF) || ((head[1] & 0xe0) != 0xe0))
        break;
        
      //byte 1: bits 4,3,2,1: 1101 mpeg 1, layer 3 (ignore CRC bit)
      type = (head[1] >> 3) & 0x03;
      layer = (head[1] >> 1) & 0x03;
      if((type == 0x01) || (layer == 0x00))
        break;  //break while loop
        
      //byte 2: bits 7,6,5,4: bitrate index
      bitrate = (head[2] >> 4) & 0x0f;
      if((bitrate == 0x00) || (bitrate == 0x0f))
        break;
        
      //get bitrate from the table, neccessary for calculating the frame size, depends on mpeg type and layer
      if(type == MPEG_VERSION1)     // MPEG 1
      {
        switch(layer) {   
          case 0x3: bitrate = mpeg_bitrate[bitrate][0]; break;  // layer 1
          case 0x2: bitrate = mpeg_bitrate[bitrate][1]; break;  // layer 2
          case 0x1: bitrate = mpeg_bitrate[bitrate][2]; break;  // layer 3 
        }
      }
      else {               // MPEG 2 or 2.5
        if(layer == MPEG_LAYER1)
          bitrate = mpeg_bitrate[bitrate][3];
        else //layer 2 or 3
          bitrate = mpeg_bitrate[bitrate][4];
      }

      //bits 3,2: sampling rate index, must not be "11"
      samplerate = (head[2] >> 2) & 0x3;
      if(samplerate == 0x3)
        break;

      //get sampling rate from the table
      switch(type) {
        case MPEG_VERSION1:   samplerate = mpeg_freq[samplerate][0]; break;  // MPEG 1
        case MPEG_VERSION2:   samplerate = mpeg_freq[samplerate][1]; break;  // MPEG 2
        case MPEG_VERSION2_5: samplerate = mpeg_freq[samplerate][2]; break;  // MPEG 2.5
      }
      
      //get Padding bit 1, ignore bit 0 (private bit)
      padding = (head[2] >> 1) & 0x1;

      //byte 4, bit 7,6: get channel mode
      channelmode = head[3] >> 6;

      //get "sample frames per MPEG frame"
      char  spf_index;   // depends on layer (table row)
      switch(layer) {
        case MPEG_LAYER1: spf_index = 0; break;
        case MPEG_LAYER2: spf_index = 1; break;
        case MPEG_LAYER3: spf_index = 2; break;
      }      
      short spf;         // sample frames per mpeg frame
      if(type == MPEG_VERSION1)    // if MPEG 1 
        spf = mpeg_spf[spf_index][0];
      else
        spf = mpeg_spf[spf_index][1];
        
      //calculate mpeg frame size
      if(layer == MPEG_LAYER1)      // if layer 1 => "slot" is 4 byte long
        framesize = (spf / 32 * (1000 * bitrate) / samplerate + padding) * 4;
      else                  // layer 2, 3
        framesize = spf / 8 * (1000 * bitrate) / samplerate + padding;

      //check if the frame fits completly into the remaining file, else the frame is open
      if((pos + outsize + framesize) > filesize)
        break;

      //check if critical values has changed compared to first frame header -> extract to new file
      //check for different sample rate / channel mode (are constant within one file, else -> new file)
      if(framecnt == 0) {
        first_type = type;
        first_layer = layer;
        first_samplerate = samplerate;
        first_channelmode = channelmode;
      }
      else
        if((type != first_type) ||
           (layer != first_layer) ||
           (samplerate != first_samplerate) ||
           (channelmode != first_channelmode))
          break;  //break while loop

      //frame seems to be part of the stream
      outsize += framesize;
      framecnt++;
  
   } // end of counting frames of a track


   //check for valid frame number (minimum number of frames is defined), else continue scanning
   if(framecnt < MPEG_MINFRAMES) {    // track isn't extracted if number of frames is too low
     pos++;
     continue;
   }

   //check for "TAG" at end (ID3v1), if there is enough data to read
   if((pos + outsize + 128) <= filesize) {
     file->seekg(pos + outsize, ios::beg);
     file->read(strbuf,3);
     if(memcmp(strbuf,"TAG",3) == 0)
       outsize += 128;
   }
     
   //search for ID3 v2 Tag info before mp3 data
   int tagpos;
   if(pos < ID3V2_MAXLEN)
     tagpos = SearchString(file, 0, pos, "ID3", 3);
   else
     tagpos = SearchString(file, pos - ID3V2_MAXLEN, ID3V2_MAXLEN, "ID3", 3); 

   if((tagpos != -1) && (pos - tagpos >= 10))           // "10" bytes is minimum for id3v2
   {      
     //check for valid "tag size" (synchsafe integer, 4 byte)
     file->seekg(tagpos + 6, ios::beg);
     for(i = 0; (i < 4) && (file->get() & 0x80); i++);
     if(i = 4) {
       outsize += pos - tagpos;
       pos = tagpos;
     } 
   }
   
   //print some file properties
   cout << "MPEG ";
   switch (first_type) {
     case MPEG_VERSION1:   cout << "1"; break;    // MPEG 1
     case MPEG_VERSION2:   cout << "2"; break;    // MPEG 2
     case MPEG_VERSION2_5: cout << "2.5"; break;  // MPEG 2.5
   }
   cout << ", layer ";
   switch (first_layer) {
     case MPEG_LAYER1: cout << "1"; break;    // layer 1
     case MPEG_LAYER2: cout << "2"; break;    // layer 2 
     case MPEG_LAYER3: cout << "3"; break;    // layer 3
   }
   cout << ", sample rate: " << first_samplerate << ", channel mode: 0x" << hex << first_channelmode << dec << ", frames: " << framecnt << endl;

   //create file name for extraction
   const char *ext;
   if(first_type != 3)
     ext = ".mpa";
   else        // MPEG 1
     switch(first_layer) {
       case 0x3: ext = ".mp1"; break;
       case 0x2: ext = ".mp2"; break;
       case 0x1: ext = ".mp3"; break;
     }
   mkfname(cnt, 8, fname, "", ext);
   //extract audio data to new file
   if(ExtractFile(file, pos, fname, outsize) == -1)
     return -1;

   //update file counter and position
   cnt++;
   pos += outsize;   
 } // end of searchig the file

 printResult(cnt, "MPEG");
 return 0;
}

