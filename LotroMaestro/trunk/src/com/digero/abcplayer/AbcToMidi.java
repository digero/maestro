package com.digero.abcplayer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import com.digero.common.abc.Dynamics;
import com.digero.common.abc.LotroInstrument;
import com.digero.common.abc.TimingInfo;
import com.digero.common.midi.KeySignature;
import com.digero.common.midi.MidiConstants;
import com.digero.common.midi.MidiFactory;
import com.digero.common.midi.Note;
import com.digero.common.midi.PanGenerator;
import com.digero.common.util.LotroParseException;
import com.digero.common.util.ParseException;
import com.digero.maestro.midi.Chord;

public class AbcToMidi {
	/** This is a static-only class */
	private AbcToMidi() {
	}

	public static class AbcInfo {
		private boolean empty = true;
		private String titlePrefix;
		private Map<Character, String> metadata = new HashMap<Character, String>();
		private NavigableMap<Long, Integer> bars = new TreeMap<Long, Integer>();
		private int tempoBPM = 120;

		public String getMetadata(char key) {
			return metadata.get(Character.toUpperCase(key));
		}
		
		public String getArtist() {
			return getMetadata('C');
		}

		public int getBarNumber(long tick) {
			Entry<Long, Integer> e = bars.floorEntry(tick);
			if (e == null)
				return 0;
			return e.getValue();
		}

		public int getBarCount() {
			return bars.size();
		}

		public int getTempoBPM() {
			return tempoBPM;
		}

		public boolean isEmpty() {
			return empty;
		}

		private void setMetadata(char key, String value) {
			this.empty = false;
			if (!metadata.containsKey(key))
				metadata.put(key, value);

			if (Character.toUpperCase(key) == 'T') {
				if (titlePrefix == null)
					titlePrefix = value;
				else
					titlePrefix = longestCommonPrefix(titlePrefix, value);
			}
		}

		private void addBar(long chordStartTick) {
			if (!bars.containsKey(chordStartTick)) {
				this.empty = false;
				bars.put(chordStartTick, bars.size() + 1);
			}
		}

		private void setTempoBPM(int tempoBPM) {
			this.tempoBPM = tempoBPM;
			this.empty = false;
		}

		private static final String openPunct = "[-:;\\(\\[\\{\\s]*";
		private static final Pattern trailingPunct = Pattern.compile(openPunct
				+ "([\\(\\[\\{]\\d{1,2}:\\d{2}[\\)\\]\\}])?" + openPunct + "$");

		public String getTitlePrefix() {
			if (titlePrefix == null || titlePrefix.length() == 0) {
				if (metadata.containsKey('T'))
					return metadata.get('T');
				return "(Untitled)";
			}

			String ret = titlePrefix;
			ret = trailingPunct.matcher(ret).replaceFirst("");
			return ret;
		}

		private void reset() {
			empty = true;
			titlePrefix = null;
			metadata.clear();
			bars.clear();
			tempoBPM = 120;
		}

		private static String longestCommonPrefix(String a, String b) {
			if (a.length() > b.length())
				a = a.substring(0, b.length());

			for (int j = 0; j < a.length(); j++) {
				if (a.charAt(j) != b.charAt(j)) {
					a = a.substring(0, j);
					break;
				}
			}
			return a;
		}
	}

	private static final Pattern INFO_PATTERN = Pattern.compile("^([A-Z]):\\s*(.*)\\s*$");
	private static final int INFO_TYPE = 1;
	private static final int INFO_VALUE = 2;

	private static final Pattern NOTE_PATTERN = Pattern.compile("(_{1,2}|=|\\^{1,2})?" + "([xzA-Ga-g])"
			+ "(,{1,5}|'{1,5})?" + "(\\d+)?" + "(//?\\d*)?" + "(>{1,3}|<{1,3})?" + "(-)?");
	private static final int NOTE_ACCIDENTAL = 1;
	private static final int NOTE_LETTER = 2;
	private static final int NOTE_OCTAVE = 3;
	private static final int NOTE_LEN_NUMER = 4;
	private static final int NOTE_LEN_DENOM = 5;
	private static final int NOTE_BROKEN_RHYTHM = 6;
	private static final int NOTE_TIE = 7;

	/**
	 * Maps a note name (a, b, c, etc.) to the number of semitones it is above
	 * the beginning of the octave (c)
	 */
	private static final int[] CHR_NOTE_DELTA = {
			9, 11, 0, 2, 4, 5, 7
	};

	// Lots of prime factors for divisibility goodness
	private static final long DEFAULT_NOTE_PULSES = (2 * 2 * 2 * 2 * 2 * 2) * (3 * 3) * 5 * 7;

	public static List<String> readLines(File inputFile) throws IOException {
		FileInputStream fileInputStream = null;
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferedReader = null;
		try {
			fileInputStream = new FileInputStream(inputFile);
			inputStreamReader = new InputStreamReader(fileInputStream);
			bufferedReader = new BufferedReader(inputStreamReader);

			String line;
			ArrayList<String> lines = new ArrayList<String>();
			while ((line = bufferedReader.readLine()) != null) {
				lines.add(line);
			}
			return lines;
		}
		finally {
			if (fileInputStream != null)
				fileInputStream.close();
			if (inputStreamReader != null)
				inputStreamReader.close();
			if (bufferedReader != null)
				bufferedReader.close();
		}
	}

	public static Sequence convert(File file, boolean useLotroInstruments,
			Map<Integer, LotroInstrument> instrumentOverrideMap, AbcInfo abcInfo, final boolean enableLotroErrors,
			final boolean stereo) throws ParseException, IOException {
		List<FileAndData> filesData = new ArrayList<FileAndData>();
		filesData.add(new FileAndData(file, readLines(file)));
		return convert(filesData, useLotroInstruments, instrumentOverrideMap, abcInfo, enableLotroErrors, stereo);
	}

	public static Sequence convert(List<FileAndData> filesData, boolean useLotroInstruments,
			Map<Integer, LotroInstrument> instrumentOverrideMap, AbcInfo abcInfo, final boolean enableLotroErrors,
			final boolean stereo) throws ParseException {
		if (abcInfo != null)
			abcInfo.reset();

		TuneInfo info = new TuneInfo();
		Sequence seq = null;
		Track track = null;

		int channel = 0;
		int trackNumber = 0;
		int noteDivisorChangeLine = 0;

		long chordStartTick = 0;
		long chordEndTick = 0;
		long PPQN = 0;
		long MPQN = 0;
		Map<Integer, Integer> tiedNotes = new HashMap<Integer, Integer>(); // noteId => (line << 16) | column
		Map<Integer, Integer> accidentals = new HashMap<Integer, Integer>(); // noteId => deltaNoteId
		PanGenerator pan = new PanGenerator();
		MidiEvent lastPanEvent = null;

		List<MidiEvent> noteOffEvents = new ArrayList<MidiEvent>();
		for (FileAndData fileAndData : filesData) {
			String fileName = fileAndData.file.getName();
			int lineNumber = 0;
			int partStartLine = 0;
			for (String line : fileAndData.lines) {
				lineNumber++;

				int comment = line.indexOf('%');
				if (comment >= 0)
					line = line.substring(0, comment);
				if (line.trim().length() == 0)
					continue;

				int chordSize = 0;

				Matcher infoMatcher = INFO_PATTERN.matcher(line);
				if (infoMatcher.matches()) {
					char type = Character.toUpperCase(infoMatcher.group(INFO_TYPE).charAt(0));
					String value = infoMatcher.group(INFO_VALUE).trim();

					if (abcInfo != null)
						abcInfo.setMetadata(type, value);

					try {
						switch (type) {
						case 'X':
							for (int lineAndColumn : tiedNotes.values()) {
								throw new ParseException("Tied note does not connect to another note", fileName,
										lineAndColumn >>> 16, lineAndColumn & 0xFFFF);
							}

							accidentals.clear();
							noteOffEvents.clear();

							info.newPart(Integer.parseInt(value));
							trackNumber++;
							partStartLine = lineNumber;
							chordStartTick = 0;
							track = null; // Will create a new track after the header is done
							if (instrumentOverrideMap != null && instrumentOverrideMap.containsKey(trackNumber)) {
								info.setInstrument(instrumentOverrideMap.get(trackNumber));
							}
							break;
						case 'T':
							if (track != null)
								throw new ParseException("Can't specify the title in the middle of a part", fileName,
										lineNumber, 0);

							info.setTitle(value);
							if (instrumentOverrideMap == null || !instrumentOverrideMap.containsKey(trackNumber)) {
								info.setInstrument(TuneInfo.findInstrumentName(value, info.getInstrument()));
							}
							break;
						case 'K':
							info.setKey(value);
							break;
						case 'L':
							info.setNoteDivisor(value);
							noteDivisorChangeLine = lineNumber;
							break;
						case 'M':
							info.setMeter(value);
							noteDivisorChangeLine = lineNumber;
							break;
						case 'Q': {
							int tempo = info.getTempo();
							info.setTempo(value);
							if (seq != null && (info.getTempo() != tempo)) {
								throw new ParseException("The tempo must be the same for all parts of the song",
										fileName, lineNumber);
							}
							break;
						}
						}
					}
					catch (IllegalArgumentException e) {
						throw new ParseException(e.getMessage(), fileName, lineNumber, infoMatcher.start(INFO_VALUE));
					}
				}
				else {
					// The line contains notes

					if (trackNumber == 0) {
						// This ABC file doesn't have an "X:" line before notes. Tsk tsk.
						trackNumber = 1;
						if (instrumentOverrideMap != null && instrumentOverrideMap.containsKey(trackNumber)) {
							info.setInstrument(instrumentOverrideMap.get(trackNumber));
						}
					}

					if (seq == null) {
						try {
							// Apparently LotRO ignores the tempo note length (e.g. Q: 1/4=120)
							PPQN = info.getPpqn();
							MPQN = (long) TimingInfo.ONE_MINUTE_MICROS / info.getTempo();
							seq = new Sequence(Sequence.PPQ, (int) PPQN);

							if (abcInfo != null)
								abcInfo.setTempoBPM(info.getTempo());

							// Track 0: Title and meta info
							Track track0 = seq.createTrack();
							track0.add(MidiFactory.createTrackNameEvent(info.getTitle()));
							track0.add(MidiFactory.createTempoEvent((int) MPQN, 0));

							track = null;
						}
						catch (InvalidMidiDataException mde) {
							throw new ParseException("Midi Error: " + mde.getMessage(), fileName);
						}
					}

					if (track == null) {
						channel = seq.getTracks().length;
						if (channel >= MidiConstants.DRUM_CHANNEL)
							channel++;
						if (channel > MidiConstants.CHANNEL_COUNT - 1)
							throw new ParseException(
									"Too many parts (max = " + (MidiConstants.CHANNEL_COUNT - 1) + ")", fileName,
									partStartLine);
						track = seq.createTrack();

						track.add(MidiFactory.createTrackNameEvent(info.getPartNumber() + ". " + info.getTitle()));
						track.add(MidiFactory.createProgramChangeEvent(info.getInstrument().midiProgramId, channel, 0));

						if (stereo) {
							track.add(lastPanEvent = MidiFactory.createPanEvent(pan.get(info.getInstrument(), info
									.getTitle()), channel));
						}
						else {
							track.add(MidiFactory.createPanEvent(64, channel));
						}
					}

					Matcher m = NOTE_PATTERN.matcher(line);
					int i = 0;
					boolean inChord = false;
					Tuplet tuplet = null;
					int brokenRhythmNumerator = 1; // The numerator of the note after the broken rhythm sign
					int brokenRhythmDenominator = 1; // The denominator of the note after the broken rhythm sign
					while (true) {
						boolean found = m.find(i);
						int parseEnd = found ? m.start() : line.length();
						// Parse anything that's not a note
						for (; i < parseEnd; i++) {
							char ch = line.charAt(i);
							if (Character.isWhitespace(ch)) {
								if (inChord)
									throw new ParseException("Unexpected whitespace inside a chord", fileName,
											lineNumber, i);
								continue;
							}

							switch (ch) {
							case '[': // Chord start
								if (inChord) {
									throw new ParseException("Unexpected '" + ch + "' inside a chord", fileName,
											lineNumber, i);
								}

								if (brokenRhythmDenominator != 1 || brokenRhythmNumerator != 1) {
									throw new ParseException("Can't have broken rhythm (< or >) within a chord",
											fileName, lineNumber, i);
								}

								chordSize = 0;
								inChord = true;
								break;

							case ']': // Chord end
								if (!inChord) {
									throw new ParseException("Unexpected '" + ch + "'", fileName, lineNumber, i);
								}
								inChord = false;
								chordStartTick = chordEndTick;
								break;

							case '|': // Bar line
								if (inChord) {
									throw new ParseException("Unexpected '" + ch + "' inside a chord", fileName,
											lineNumber, i);
								}

								if (abcInfo != null && trackNumber == 1)
									abcInfo.addBar(chordStartTick);

								accidentals.clear();
								if (i + 1 < line.length() && line.charAt(i + 1) == ']') {
									i++; // Skip |]
								}
								else if (abcInfo != null && trackNumber == 1) {
									abcInfo.addBar(chordStartTick);
								}
								break;

							case '+': {
								int j = line.indexOf('+', i + 1);
								if (j < 0) {
									throw new ParseException("There is no matching '+'", fileName, lineNumber, i);
								}
								try {
									info.setDynamics(line.substring(i + 1, j));
								}
								catch (IllegalArgumentException iae) {
									throw new ParseException("Unsupported +decoration+", fileName, lineNumber, i);
								}

								if (enableLotroErrors && inChord) {
									throw new LotroParseException("Can't include a +decoration+ inside a chord",
											fileName, lineNumber, i);
								}

								i = j;
								break;
							}

							case '(':
								// Tuplet or slur start
								if (i + 1 < line.length() && Character.isDigit(line.charAt(i + 1))) {
									// If it has a digit following it, it's a tuplet
									if (tuplet != null)
										throw new ParseException("Unexpected '" + ch + "' before end of tuplet",
												fileName, lineNumber, i);

									try {
										for (int j = i + 1; j < line.length(); j++) {
											if (line.charAt(i) != ':' && !Character.isDigit(line.charAt(i))) {
												tuplet = new Tuplet(line.substring(i + 1, j + 1), info
														.isCompoundMeter());
												i = j;
												break;
											}
										}
									}
									catch (IllegalArgumentException e) {
										throw new ParseException("Invalid tuplet", fileName, lineNumber, i);
									}
								}
								else {
									// Otherwise it's a slur, which LotRO conveniently ignores
									if (inChord) {
										throw new ParseException("Unexpected '" + ch + "' inside a chord", fileName,
												lineNumber, i);
									}
								}
								break;

							case ')':
								// End of a slur, ignore
								if (inChord) {
									throw new ParseException("Unexpected '" + ch + "' inside a chord", fileName,
											lineNumber, i);
								}
								break;

							case '\\':
								// Ignore backslashes
								break;

							default:
								throw new ParseException("Unknown/unexpected character '" + ch + "'", fileName,
										lineNumber, i);
							}
						}

						if (i >= line.length())
							break;

						// The matcher might find +f+, +ff+, or +fff+ and think it's a note
						if (i > m.start())
							continue;

						if (inChord)
							chordSize++;

						if (enableLotroErrors && inChord && chordSize > Chord.MAX_CHORD_NOTES) {
							throw new LotroParseException("Too many notes in a chord", fileName, lineNumber, m.start());
						}

						// Parse the note
						int numerator;
						int denominator;

						numerator = (m.group(NOTE_LEN_NUMER) == null) ? 1 : Integer.parseInt(m.group(NOTE_LEN_NUMER));
						String denom = m.group(NOTE_LEN_DENOM);
						if (denom == null)
							denominator = 1;
						else if (denom.equals("/"))
							denominator = 2;
						else if (denom.equals("//"))
							denominator = 4;
						else
							denominator = Integer.parseInt(denom.substring(1));

						String brokenRhythm = m.group(NOTE_BROKEN_RHYTHM);
						if (brokenRhythm != null) {
							if (brokenRhythmDenominator != 1 || brokenRhythmNumerator != 1) {
								throw new ParseException("Invalid broken rhythm: " + brokenRhythm, fileName,
										lineNumber, m.start(NOTE_BROKEN_RHYTHM));
							}
							if (inChord) {
								throw new ParseException("Can't have broken rhythm (< or >) within a chord", fileName,
										lineNumber, m.start(NOTE_BROKEN_RHYTHM));
							}
							if (m.group(NOTE_TIE) != null) {
								throw new ParseException("Tied notes can't have broken rhythms (< or >)", fileName,
										lineNumber, m.start(NOTE_BROKEN_RHYTHM));
							}

							int factor = 1 << brokenRhythm.length();

							if (brokenRhythm.charAt(0) == '>') {
								numerator *= 2 * factor - 1;
								denominator *= factor;
								brokenRhythmDenominator = factor;
							}
							else {
								brokenRhythmNumerator = 2 * factor - 1;
								brokenRhythmDenominator = factor;
								denominator *= factor;
							}
						}
						else {
							numerator *= brokenRhythmNumerator;
							denominator *= brokenRhythmDenominator;
							brokenRhythmNumerator = 1;
							brokenRhythmDenominator = 1;
						}

						if (tuplet != null) {
							if (!inChord || chordSize == 1)
								tuplet.r--;
							numerator *= tuplet.q;
							denominator *= tuplet.p;
							if (tuplet.r == 0)
								tuplet = null;
						}

						long noteEndTick = chordStartTick + DEFAULT_NOTE_PULSES * numerator / denominator;
						// A chord is as long as its shortest note
						if (chordEndTick == chordStartTick || noteEndTick < chordEndTick)
							chordEndTick = noteEndTick;

						char noteLetter = m.group(NOTE_LETTER).charAt(0);
						String octaveStr = m.group(NOTE_OCTAVE);
						if (octaveStr == null)
							octaveStr = "";
						if (noteLetter == 'z' || noteLetter == 'x') {
							if (m.group(NOTE_ACCIDENTAL) != null && m.group(NOTE_ACCIDENTAL).length() > 0) {
								throw new ParseException("Unexpected accidental on a rest", fileName, lineNumber, m
										.start(NOTE_ACCIDENTAL));
							}
							if (octaveStr.length() > 0) {
								throw new ParseException("Unexpected octave indicator on a rest", fileName, lineNumber,
										m.start(NOTE_OCTAVE));
							}
						}
						else {
							int octave = Character.isUpperCase(noteLetter) ? 3 : 4;
							if (octaveStr.indexOf('\'') >= 0)
								octave += octaveStr.length();
							else if (octaveStr.indexOf(',') >= 0)
								octave -= octaveStr.length();

							int noteId;
							int lotroNoteId;

							lotroNoteId = noteId = (octave + 1) * 12
									+ CHR_NOTE_DELTA[Character.toLowerCase(noteLetter) - 'a'];
							if (!useLotroInstruments)
								noteId += 12 * info.getInstrument().octaveDelta;

							if (m.group(NOTE_ACCIDENTAL) != null) {
								if (m.group(NOTE_ACCIDENTAL).startsWith("_"))
									accidentals.put(noteId, -m.group(NOTE_ACCIDENTAL).length());
								else if (m.group(NOTE_ACCIDENTAL).startsWith("^"))
									accidentals.put(noteId, m.group(NOTE_ACCIDENTAL).length());
								else if (m.group(NOTE_ACCIDENTAL).equals("="))
									accidentals.put(noteId, 0);
							}

							int noteDelta;
							if (accidentals.containsKey(noteId)) {
								noteDelta = accidentals.get(noteId);
							}
							else {
								// Use the key signature to determine the accidental
								noteDelta = info.getKey().getDefaultAccidental(noteId).deltaNoteId;
							}
							lotroNoteId += noteDelta;
							noteId += noteDelta;

							if (enableLotroErrors && lotroNoteId < Note.MIN_PLAYABLE.id)
								throw new LotroParseException("Note is too low", fileName, lineNumber, m.start());
							else if (enableLotroErrors && lotroNoteId > Note.MAX_PLAYABLE.id)
								throw new LotroParseException("Note is too high", fileName, lineNumber, m.start());

							if (info.getInstrument() == LotroInstrument.COWBELL
									|| info.getInstrument() == LotroInstrument.MOOR_COWBELL) {
								if (useLotroInstruments) {
									// Randomize the noteId unless it's part of a note tie
									if (m.group(NOTE_TIE) == null && !tiedNotes.containsKey(noteId)) {
										int min = info.getInstrument().lowestPlayable.id;
										int max = info.getInstrument().highestPlayable.id;
										lotroNoteId = noteId = min + (int) (Math.random() * (max - min));
									}
								}
								else {
									noteId = (info.getInstrument() == LotroInstrument.COWBELL) ? 76 : 71;
									lotroNoteId = 71;
								}
							}

							// Check for overlapping notes, and remove extra note off events
							Iterator<MidiEvent> noteOffIter = noteOffEvents.iterator();
							while (noteOffIter.hasNext()) {
								MidiEvent evt = noteOffIter.next();
								if (evt.getTick() <= chordStartTick) {
									noteOffIter.remove();
									continue;
								}

								int noteOffId = ((ShortMessage) evt.getMessage()).getData1();
								if (noteOffId == noteId) {
									track.remove(evt);
									evt.setTick(chordStartTick);
									track.add(evt);
									noteOffIter.remove();
									break;
								}
							}

							if (!tiedNotes.containsKey(noteId)) {
								if (info.getPpqn() != PPQN) {
									throw new ParseException(
											"The default note length must be the same for all parts of the song",
											fileName, noteDivisorChangeLine);
								}
								track.add(MidiFactory.createNoteOnEventEx(noteId, channel, info.getDynamics().abcVol,
										chordStartTick));
							}

							if (m.group(NOTE_TIE) != null) {
								int lineAndColumn = (lineNumber << 16) | m.start();
								tiedNotes.put(noteId, lineAndColumn);
							}
							else {
								long lengthMicros = (noteEndTick - chordStartTick) * MPQN / PPQN;
								if (enableLotroErrors && lengthMicros < TimingInfo.SHORTEST_NOTE_MICROS) {
									throw new LotroParseException("Note's duration is too short", fileName, lineNumber,
											m.start());
								}
								else if (enableLotroErrors && lengthMicros > TimingInfo.LONGEST_NOTE_MICROS) {
									throw new LotroParseException("Note's duration is too long", fileName, lineNumber,
											m.start());
								}

								// Stringed instruments, drums, and woodwind breath sounds always play the 
								// sound sample in its entirety. Since Gervill doesn't support the SoundFont 
								// extension that specifies this, we have to increase the note length.
								// One second should do the trick.
								long noteEndTickTmp = noteEndTick;
								if (useLotroInstruments && !info.getInstrument().isSustainable(lotroNoteId)) {
									noteEndTickTmp = Math.max(noteEndTick, chordStartTick
											+ TimingInfo.ONE_SECOND_MICROS * PPQN / MPQN);
								}
								MidiEvent noteOff = MidiFactory.createNoteOffEventEx(noteId, channel, info
										.getDynamics().abcVol, noteEndTickTmp);
								track.add(noteOff);
								noteOffEvents.add(noteOff);

								tiedNotes.remove((Integer) noteId);
							}
						}

						if (!inChord)
							chordStartTick = noteEndTick;
						i = m.end();
					}

					if (tuplet != null)
						throw new ParseException("Tuplet not finished by end of line", fileName, lineNumber, i);

					if (inChord)
						throw new ParseException("Chord not closed at end of line", fileName, lineNumber, i);

					if (brokenRhythmDenominator != 1 || brokenRhythmNumerator != 1)
						throw new ParseException("Broken rhythm unfinished at end of line", fileName, lineNumber, i);
				}
			}

			if (seq == null)
				throw new ParseException("The file contains no notes", fileName, lineNumber);

			for (int lineAndColumn : tiedNotes.values()) {
				throw new ParseException("Tied note does not connect to another note", fileName, lineAndColumn >>> 16,
						lineAndColumn & 0xFFFF);
			}
		}

		if (trackNumber == 1 && track != null && lastPanEvent != null) {
			// Only one track
			track.remove(lastPanEvent);
			track.add(MidiFactory.createPanEvent(64, channel));
		}

		return seq;
	}

	@SuppressWarnings("unused")
	private static void dbgout(String text) {
		System.out.println(text);
	}

	// From http://abcnotation.com/abc2mtex/abc.txt:
	//
	//   Duplets, triplets, quadruplets, etc.
	//   ====================================
	// These can be simply coded with the notation (2ab  for  a  duplet,
	// (3abc  for  a triplet or (4abcd for a quadruplet, etc., up to (9.
	// The musical meanings are:
	//
	//  (2 2 notes in the time of 3
	//  (3 3 notes in the time of 2
	//  (4 4 notes in the time of 3
	//  (5 5 notes in the time of n
	//  (6 6 notes in the time of 2
	//  (7 7 notes in the time of n
	//  (8 8 notes in the time of 3
	//  (9 9 notes in the time of n
	//
	// If the time signature is compound (3/8, 6/8, 9/8, 3/4, etc.) then
	// n is three, otherwise n is two.
	//
	// More general tuplets can be specified  using  the  syntax  (p:q:r
	// which  means  `put  p  notes  into  the  time of q for the next r
	// notes'.  If q is not given, it defaults as above.  If  r  is  not
	// given,  it  defaults  to p.  For example, (3:2:2 is equivalent to
	// (3::2 and (3:2:3 is equivalent to (3:2 , (3 or even (3:: .   This
	// can  be  useful  to  include  notes of different lengths within a
	// tuplet, for example (3:2:2G4c2 or (3:2:4G2A2Bc and also describes
	// more precisely how the simple syntax works in cases like (3D2E2F2
	// or even (3D3EF2. The number written over the tuplet is p.
	private static class Tuplet {
		public int p;
		public int q;
		public int r;

		public Tuplet(String str, boolean compoundMeter) {
			try {
				String[] parts = str.split(":");
				if (parts.length < 1 || parts.length > 3)
					throw new IllegalArgumentException();

				p = Integer.parseInt(parts[0]);

				if (p < 2 || p > 9)
					throw new IllegalArgumentException();

				if (parts.length >= 2)
					q = Integer.parseInt(parts[1]);
				else if (p == 3 || p == 6)
					q = 2;
				else if (p == 2 || p == 4 || p == 8)
					q = 3;
				else if (p == 5 || p == 7 || p == 9)
					q = compoundMeter ? 3 : 2;
				else
					throw new IllegalArgumentException();

				if (parts.length >= 3)
					r = Integer.parseInt(parts[2]);
				else
					r = p;
			}
			catch (NumberFormatException e) {
				throw new IllegalArgumentException(e);
			}
		}
	}

	private static class TuneInfo {
		private int partNumber;
		private String title;
		private KeySignature key;
		private long ppqn;
		private int tempo;
		private LotroInstrument instrument;
		private Dynamics dynamics;
		private boolean compoundMeter;

		public TuneInfo() {
			partNumber = 0;
			title = "";
			key = KeySignature.C_MAJOR;
			ppqn = 8 * DEFAULT_NOTE_PULSES / 4;
			tempo = 120;
			instrument = LotroInstrument.LUTE;
			dynamics = Dynamics.mf;
			compoundMeter = false;
		}

		public void newPart(int partNumber) {
			this.partNumber = partNumber;
			instrument = LotroInstrument.LUTE;
			dynamics = Dynamics.mf;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public void setKey(String str) {
			this.key = new KeySignature(str);
		}

		public void setNoteDivisor(String str) {
			this.ppqn = parseDivisor(str) * DEFAULT_NOTE_PULSES / 4;
		}

		public void setMeter(String str) {
			str = str.trim();
			int numerator;
			int denominator;
			if (str.equals("C")) {
				numerator = 4;
				denominator = 4;
			}
			else if (str.equals("C|")) {
				numerator = 2;
				denominator = 2;
			}
			else {
				String[] parts = str.split("[/:| ]");
				if (parts.length != 2) {
					throw new IllegalArgumentException("The string: \"" + str
							+ "\" is not a valid time signature (expected format: 4/4)");
				}
				numerator = Integer.parseInt(parts[0]);
				denominator = Integer.parseInt(parts[1]);
			}

			this.ppqn = ((4 * numerator / denominator) < 3 ? 16 : 8) * DEFAULT_NOTE_PULSES / denominator;
			this.compoundMeter = (numerator % 3) == 0;
		}

		public void setTempo(String str) {
			try {
				String[] parts = str.split("=");
				if (parts.length == 1) {
					this.tempo = Integer.parseInt(parts[0]);
				}
				else if (parts.length == 2) {
					this.tempo = Integer.parseInt(parts[1]);
				}
				else {
					throw new IllegalArgumentException("Unable to read tempo");
				}
			}
			catch (NumberFormatException nfe) {
				throw new IllegalArgumentException("Unable to read tempo");
			}
		}

		private int parseDivisor(String str) {
			String[] parts = str.trim().split("[/:| ]");
			if (parts.length != 2) {
				throw new IllegalArgumentException("\"" + str + "\" is not a valid note length"
						+ " (example of valid note length: 1/4)");
			}
			int numerator = Integer.parseInt(parts[0]);
			int denominator = Integer.parseInt(parts[1]);
			if (numerator != 1) {
				throw new IllegalArgumentException("The numerator of the note length must be 1"
						+ " (example of valid note length: 1/4)");
			}
			if (denominator < 1) {
				throw new IllegalArgumentException("The denominator of the note length must be positive"
						+ " (example of valid note length: 1/4)");
			}

			return denominator;
		}

		private static Map<String, LotroInstrument> instrNicknames = null;
		private static Pattern instrRegex = null;

		public static LotroInstrument findInstrumentName(String str, LotroInstrument defaultInstrument) {
			if (instrNicknames == null) {
				instrNicknames = new HashMap<String, LotroInstrument>();
				// Must be all-caps
				instrNicknames.put("BANJO", LotroInstrument.LUTE);
				instrNicknames.put("GUITAR", LotroInstrument.LUTE);
				instrNicknames.put("DRUM", LotroInstrument.DRUMS);
				instrNicknames.put("BASS", LotroInstrument.THEORBO);
				instrNicknames.put("THEO", LotroInstrument.THEORBO);
				instrNicknames.put("BAGPIPES", LotroInstrument.BAGPIPE);
				instrNicknames.put("MOORCOWBELL", LotroInstrument.MOOR_COWBELL);
				instrNicknames.put("MOOR COWBELL", LotroInstrument.MOOR_COWBELL);
				instrNicknames.put("MORE COWBELL", LotroInstrument.MOOR_COWBELL);
			}

			if (instrRegex == null) {
				String regex = "";
				for (LotroInstrument instr : LotroInstrument.values()) {
					regex += "|" + instr;
				}
				for (String nick : instrNicknames.keySet()) {
					regex += "|" + nick;
				}
				regex = "\\b(" + regex.substring(1) + ")\\b";
				instrRegex = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
			}

			Matcher m = instrRegex.matcher(str);
			if (m.find()) {
				String name = m.group(1).toUpperCase();
				if (instrNicknames.containsKey(name))
					return instrNicknames.get(name);

				return LotroInstrument.valueOf(name);
			}
			return defaultInstrument;
		}

		public void setInstrument(LotroInstrument instrument) {
			this.instrument = instrument;
		}

		public void setDynamics(String str) {
			dynamics = Dynamics.valueOf(str);
		}

		public int getPartNumber() {
			return partNumber;
		}

		public String getTitle() {
			return title;
		}

		public KeySignature getKey() {
			return key;
		}

		public long getPpqn() {
			return ppqn;
		}

		public boolean isCompoundMeter() {
			return compoundMeter;
		}

		public int getTempo() {
			return tempo;
		}

		public LotroInstrument getInstrument() {
			return instrument;
		}

		public Dynamics getDynamics() {
			return dynamics;
		}
	}
}
