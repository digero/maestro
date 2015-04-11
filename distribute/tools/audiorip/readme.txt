########
audiorip
########


Version: 01-Jan-2010
Author: Markus Wolf
Internet: http://murkymind.de/projects/audiorip
License: GNU LGPL (http://www.gnu.org/copyleft/lesser.html)


Purpose:
--------
 - Scans for and extracts audio data from unpacked data files (game resources, joined audio files)
 - Detected audio streams are stored to a new directory
 - Files are stored with numbered file names

 Note:
 - There might be tools with specific data file support of game resource files that are able to extract the original file names (see http://wiki.xentax.com)
 - After demultiplexing a DVB recording, the MPEG audio file may contain different channel modes, confusing most players and encoders
  -> Format changes are detected and the stream will be splitted perfectly to separate files (convert and join to same format with an audio editor before recoding)

Supported formats:
------------------
 - Ogg Vorbis (*.ogg)  => search for file header (start "page") and step through Ogg "pages" to find the last one
 - MPEG audio          => search for concatenated frames with same format
 - Riff Wave (*.wav)   => search for file header (which contains the file size)


Usage:
------
 - Scans for and extracts all formats to a new extraction directory in current working directory by default (without options)
 - When specifying specific formats, it scans only for them

 audiorip <data_file> [options]

 Options:
 
  "-mpeg"   : Extract MPEG audio
  "-ogg"    : Extract Ogg Vorbis (or other Ogg streams)
  "-wav"    : Extract Riff Wave

  "-h"      : Print help
  "-s"      : Scan and list only, don't extract
              (Hint: Text output can be redirected to a separate file: "audiorip <data_file> > list.txt")
  "-o <path>: Create extraction directory under the given path and extract to it


Extraction Directory:
---------------------
 - Creates always a new numbered directory for extractions, where the program is called from (default) or at the specified path
   -> Doesn't overwrite any existing data


MPEG notes
-----------
 - Searching for mpeg can last a while because of the more complex scanning process
 - ID3V1 tag is kept if found.
 - ID3V2 tag is kept if it starts maximal 2000 bytes before the first mpeg frame
 - If 8 or more concatenated and valid MPEG audio frames (frame headers) of same format are found, they will be extracted to a file.
 - If a MPEG track follows another one without any non frame data and without format changes, they're detected as one track.
 - In MS Wave embedded MP3 may be detected twice: as MPEG and as Wave


MPEG formats and file extensions:
---------------------------------
  *.mp1 : MPEG 1 layer 1
  *.mp2 : MPEG 1 layer 2
  *.mp3 : MPEG 1 layer 3
  *.mpa : all other MPEG audio (MPEG 2)



Compilation notes:
------------------
 - Use "make" to compile (setup the "makefile" according to your system)
