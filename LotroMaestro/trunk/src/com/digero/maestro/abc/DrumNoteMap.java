package com.digero.maestro.abc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.prefs.Preferences;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.digero.common.midi.MidiConstants;
import com.digero.common.midi.Note;
import com.digero.common.util.ParseException;
import com.digero.maestro.util.IDisposable;

public class DrumNoteMap implements IDisposable {
	public static final String FILE_SUFFIX = "drummap";
	protected static final byte DISABLED_NOTE_ID = (byte) LotroDrumInfo.DISABLED.note.id;
	private static final String MAP_PREFS_KEY = "DrumNoteMap.map";

	private byte[] map = null;
	private List<ChangeListener> listeners = null;

	public byte get(int midiNoteId) {
		if (midiNoteId < Byte.MIN_VALUE || midiNoteId > Byte.MAX_VALUE) {
			throw new IllegalArgumentException();
		}
		return get((byte) midiNoteId);
	}

	public byte get(byte midiNoteId) {
		// Map hasn't been initialized yet, use defaults
		if (map == null)
			return getDefaultMapping(midiNoteId);

		return map[midiNoteId];
	}

	public void set(int midiNoteId, int value) {
		if ((midiNoteId < Byte.MIN_VALUE || midiNoteId > Byte.MAX_VALUE)
				|| (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE)) {
			throw new IllegalArgumentException();
		}
		set((byte) midiNoteId, (byte) value);
	}

	public void set(byte midiNoteId, byte value) {
		if (get(midiNoteId) != value) {
			ensureMap();
			map[midiNoteId] = value;
			fireChangeEvent();
		}
	}

	protected byte getDefaultMapping(byte noteId) {
		return DISABLED_NOTE_ID;
	}

	private void ensureMap() {
		if (map == null) {
			map = new byte[MidiConstants.NOTE_COUNT];
			for (int i = 0; i < map.length; i++) {
				map[i] = getDefaultMapping((byte) i);
			}
		}
	}

	public void addChangeListener(ChangeListener listener) {
		if (listeners == null)
			listeners = new ArrayList<ChangeListener>(2);

		if (!listeners.contains(listener))
			listeners.add(listener);
	}

	public void removeChangeListener(ChangeListener listener) {
		if (listeners != null)
			listeners.remove(listener);
	}

	private void fireChangeEvent() {
		if (listeners != null) {
			ChangeEvent e = new ChangeEvent(this);
			for (ChangeListener l : listeners) {
				l.stateChanged(e);
			}
		}
	}

	@Override
	public void dispose() {
		listeners = null;
	}

	public void save(Preferences prefs) {
		ensureMap();
		prefs.putByteArray(MAP_PREFS_KEY, map);
	}

	public boolean load(Preferences prefs) {
		byte[] mapTmp = prefs.getByteArray(MAP_PREFS_KEY, null);
		if (mapTmp == null || mapTmp.length != MidiConstants.NOTE_COUNT) {
			return false;
		}
		else {
			for (int i = 0; i < mapTmp.length; i++) {
				if (mapTmp[i] != DISABLED_NOTE_ID && !Note.isPlayable(mapTmp[i])) {
					return false;
				}
			}
		}

		map = mapTmp;
		return true;
	}

	public void save(File outputFile) throws IOException {
		PrintStream outStream = null;
		try {
			outStream = new PrintStream(outputFile);
			save(outStream);
		}
		finally {
			if (outStream != null)
				outStream.close();
		}
	}

	public void save(PrintStream out) {
		ensureMap();

		out.println("% LOTRO Drum Map");
		out.println("%");
		out.println("% Format is: MIDI Drum ID => LOTRO Drum ID");
		out.println("% Comments begin with a percent sign");
		out.println();

		int maxDrumLen = MidiConstants.getDrumName(MidiConstants.LOWEST_DRUM_ID - 1).length();
		for (int i = MidiConstants.LOWEST_DRUM_ID; i <= MidiConstants.HIGHEST_DRUM_ID; i++) {
			String drumName = MidiConstants.getDrumName(i);
			if (maxDrumLen < drumName.length())
				maxDrumLen = drumName.length();
		}

		for (int midiNoteId = 0; midiNoteId < map.length; midiNoteId++) {
			// Only write non-drum IDs if they actually have a mapping
			if ((midiNoteId < MidiConstants.LOWEST_DRUM_ID || midiNoteId > MidiConstants.HIGHEST_DRUM_ID)
					&& map[midiNoteId] == DISABLED_NOTE_ID) {
				continue;
			}

			Note note = Note.fromId(map[midiNoteId]);
			if (note == null)
				note = LotroDrumInfo.DISABLED.note;

			LotroDrumInfo lotroDrum = LotroDrumInfo.getById(note.id);
			if (lotroDrum == null)
				lotroDrum = LotroDrumInfo.DISABLED;

			out.format("%2d => %2d  %% %-" + maxDrumLen + "s => %s", midiNoteId, note.id,
					MidiConstants.getDrumName(midiNoteId), lotroDrum.toString());
			out.println();
		}
	}

	public void load(File inputFile) throws IOException, ParseException {
		FileInputStream inputStream = null;
		try {
			inputStream = new FileInputStream(inputFile);
			load(inputStream, inputFile.getName());
		}
		finally {
			if (inputStream != null)
				inputStream.close();
		}
	}

	public void load(InputStream inputStream) throws IOException, ParseException {
		load(inputStream, null);
	}

	private void load(InputStream inputStream, String inputFileName) throws IOException, ParseException {
		if (map == null)
			map = new byte[MidiConstants.NOTE_COUNT];

		Arrays.fill(map, DISABLED_NOTE_ID);

		BufferedReader rdr = new BufferedReader(new InputStreamReader(inputStream));
		String line;
		int lineNumber = 0;
		while ((line = rdr.readLine()) != null) {
			lineNumber++;

			int commentIndex = line.indexOf('%');
			if (commentIndex >= 0) {
				line = line.substring(0, commentIndex);
			}
			line = line.trim();
			if (line.isEmpty())
				continue;

			byte midiNote;
			byte lotroNote;
			try {
				StringTokenizer tokenizer = new StringTokenizer(line, " \t=>");
				midiNote = Byte.parseByte(tokenizer.nextToken());
				lotroNote = Byte.parseByte(tokenizer.nextToken());
				if (tokenizer.hasMoreTokens()) {
					throw new ParseException("Invalid line (too many tokens)", inputFileName, lineNumber);
				}
			}
			catch (NoSuchElementException nse) {
				throw new ParseException("Invalid line (too few tokens)", inputFileName, lineNumber);
			}
			catch (NumberFormatException nfe) {
				throw new ParseException("Invalid note ID", inputFileName, lineNumber);
			}

			if (midiNote < MidiConstants.LOWEST_NOTE_ID || midiNote > MidiConstants.HIGHEST_NOTE_ID) {
				throw new ParseException("MIDI note is invalid", inputFileName, lineNumber);
			}
			if (lotroNote != DISABLED_NOTE_ID && !Note.isPlayable(lotroNote)) {
				throw new ParseException("ABC note is invalid", inputFileName, lineNumber);
			}

			map[midiNote] = lotroNote;
		}

		fireChangeEvent();
	}

	/**
	 * This can be used as a backup in the event that loading the drum map from
	 * a file fails.
	 */
	public void loadFailsafeDefault() {
		if (map == null)
			map = new byte[MidiConstants.NOTE_COUNT];

		Arrays.fill(map, DISABLED_NOTE_ID);

		map[26] = 49;
		map[27] = 72;
		map[28] = 70;
		map[29] = -1;
		map[30] = -1;
		map[31] = 51;
		map[32] = 50;
		map[33] = 39;
		map[34] = -1;
		map[35] = 49;
		map[36] = 58;
		map[37] = 51;
		map[38] = 52;
		map[39] = 53;
		map[40] = 54;
		map[41] = 49;
		map[42] = 37;
		map[43] = 69;
		map[44] = 59;
		map[45] = 47;
		map[46] = 60;
		map[47] = 63;
		map[48] = 43;
		map[49] = 57;
		map[50] = 45;
		map[51] = 55;
		map[52] = 57;
		map[53] = 43;
		map[54] = 46;
		map[55] = 57;
		map[56] = 45;
		map[57] = 57;
		map[58] = 53;
		map[59] = 60;
		map[60] = 38;
		map[61] = 69;
		map[62] = 39;
		map[63] = 70;
		map[64] = 48;
		map[65] = 65;
		map[66] = 64;
		map[67] = 43;
		map[68] = 47;
		map[69] = 37;
		map[70] = 42;
		map[71] = -1;
		map[72] = -1;
		map[73] = 64;
		map[74] = 62;
		map[75] = 43;
		map[76] = 51;
		map[77] = 67;
		map[78] = 65;
		map[79] = 64;
		map[80] = 43;
		map[81] = 43;
		map[82] = 42;
		map[83] = 44;
		map[84] = -1;
		map[85] = 72;
		map[86] = 48;
		map[87] = 58;
	}
}
