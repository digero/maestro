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
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Element;

import com.digero.common.abc.LotroInstrument;
import com.digero.common.midi.MidiConstants;
import com.digero.common.midi.MidiDrum;
import com.digero.common.midi.Note;
import com.digero.common.util.IDiscardable;
import com.digero.common.util.ParseException;
import com.digero.common.util.Version;
import com.digero.maestro.MaestroMain;
import com.digero.maestro.util.SaveUtil;
import com.digero.maestro.util.XmlUtil;

public class DrumNoteMap implements IDiscardable
{
	public static final String FILE_SUFFIX = "drummap.txt";
	protected static final byte DISABLED_NOTE_ID = (byte) LotroDrumInfo.DISABLED.note.id;
	private static final String MAP_PREFS_KEY = "DrumNoteMap.map";

	private byte[] map = null;
	private List<ChangeListener> listeners = null;

	public boolean isModified()
	{
		return map != null;
	}

	public byte get(int midiNoteId)
	{
		if (midiNoteId < Byte.MIN_VALUE || midiNoteId > Byte.MAX_VALUE)
		{
			throw new IllegalArgumentException();
		}
		return get((byte) midiNoteId);
	}

	public byte get(byte midiNoteId)
	{
		// Map hasn't been initialized yet, use defaults
		if (map == null)
			return getDefaultMapping(midiNoteId);

		return map[midiNoteId];
	}

	public void set(int midiNoteId, int value)
	{
		if ((midiNoteId < Byte.MIN_VALUE || midiNoteId > Byte.MAX_VALUE)
				|| (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE))
		{
			throw new IllegalArgumentException();
		}
		set((byte) midiNoteId, (byte) value);
	}

	public void set(byte midiNoteId, byte value)
	{
		if (get(midiNoteId) != value)
		{
			ensureMap();
			map[midiNoteId] = value;
			fireChangeEvent();
		}
	}

	protected byte getDefaultMapping(byte noteId)
	{
		return DISABLED_NOTE_ID;
	}

	private void ensureMap()
	{
		if (map == null)
			map = getFailsafeDefault();
	}

	public void addChangeListener(ChangeListener listener)
	{
		if (listeners == null)
			listeners = new ArrayList<ChangeListener>(2);

		if (!listeners.contains(listener))
			listeners.add(listener);
	}

	public void removeChangeListener(ChangeListener listener)
	{
		if (listeners != null)
			listeners.remove(listener);
	}

	private void fireChangeEvent()
	{
		if (listeners != null)
		{
			ChangeEvent e = new ChangeEvent(this);
			for (ChangeListener l : listeners)
			{
				l.stateChanged(e);
			}
		}
	}

	@Override public void discard()
	{
		listeners = null;
	}

	@Override public boolean equals(Object obj)
	{
		if (obj == null || obj.getClass() != this.getClass())
			return false;

		return Arrays.equals(map, ((DrumNoteMap) obj).map);
	}

	@Override public int hashCode()
	{
		return Arrays.hashCode(map);
	}

	public void save(Preferences prefs)
	{
		ensureMap();
		prefs.putByteArray(MAP_PREFS_KEY, map);
	}

	public void load(Preferences prefs)
	{
		setLoadedByteArray(prefs.getByteArray(MAP_PREFS_KEY, null));
	}

	private void setLoadedByteArray(byte[] bytes)
	{
		if (bytes != null && bytes.length == MidiConstants.NOTE_COUNT)
		{
			map = bytes;
			byte[] failsafe = null;
			for (int i = 0; i < map.length; i++)
			{
				if (map[i] != DISABLED_NOTE_ID && !LotroInstrument.BASIC_DRUM.isPlayable(map[i]))
				{
					if (failsafe == null)
					{
						failsafe = getFailsafeDefault();
					}
					map[i] = failsafe[i];
				}
			}
		}
	}

	public void save(File outputFile) throws IOException
	{
		PrintStream outStream = null;
		try
		{
			outStream = new PrintStream(outputFile);
			save(outStream);
		}
		finally
		{
			if (outStream != null)
				outStream.close();
		}
	}

	public void save(PrintStream out)
	{
		ensureMap();

		out.println("% LOTRO Drum Map");
		out.println("% Created using " + MaestroMain.APP_NAME + " v" + MaestroMain.APP_VERSION);
		out.println("%");
		out.println("% Format is: [MIDI Drum ID] => [LOTRO Drum ID]");
		out.format("%% LOTRO Drum IDs are in the range %d (%s) to %d (%s)", //
				Note.MIN_PLAYABLE.id, Note.MIN_PLAYABLE.abc, //
				Note.MAX_PLAYABLE.id, Note.MAX_PLAYABLE.abc);
		out.println();
		out.println("% A LOTRO Drum ID of -1 indicates that the drum is not mapped");
		out.println("% Comments begin with %");
		out.println();

		int maxDrumLen = MidiDrum.INVALID.name.length();
		for (MidiDrum drum : MidiDrum.values())
		{
			if (maxDrumLen < drum.name.length())
				maxDrumLen = drum.name.length();
		}

		for (int midiNoteId = 0; midiNoteId < map.length; midiNoteId++)
		{
			MidiDrum drum = MidiDrum.fromId(midiNoteId);

			// Only write non-drum IDs if they actually have a mapping
			if (drum == MidiDrum.INVALID && map[midiNoteId] == DISABLED_NOTE_ID)
				continue;

			Note note = Note.fromId(map[midiNoteId]);
			if (note == null)
				note = LotroDrumInfo.DISABLED.note;

			LotroDrumInfo lotroDrum = LotroDrumInfo.getById(note.id);
			if (lotroDrum == null)
				lotroDrum = LotroDrumInfo.DISABLED;

			String drumName = drum.name;
			if (drumName.equals(MidiDrum.INVALID.name))
				drumName = "(" + drumName + ")";

			out.format("%2d => %2d  %% %-" + maxDrumLen + "s => %s", midiNoteId, note.id, drumName,
					lotroDrum.toString());
			out.println();
		}
	}

	public void load(File inputFile) throws IOException, ParseException
	{
		FileInputStream inputStream = null;
		try
		{
			inputStream = new FileInputStream(inputFile);
			load(inputStream, inputFile.getName());
		}
		finally
		{
			if (inputStream != null)
				inputStream.close();
		}
	}

	public void load(InputStream inputStream) throws IOException, ParseException
	{
		load(inputStream, null);
	}

	private void load(InputStream inputStream, String inputFileName) throws IOException, ParseException
	{
		if (map == null)
			map = new byte[MidiConstants.NOTE_COUNT];

		Arrays.fill(map, DISABLED_NOTE_ID);

		BufferedReader rdr = new BufferedReader(new InputStreamReader(inputStream));
		String line;
		int lineNumber = 0;
		while ((line = rdr.readLine()) != null)
		{
			lineNumber++;

			int commentIndex = line.indexOf('%');
			if (commentIndex >= 0)
			{
				line = line.substring(0, commentIndex);
			}
			line = line.trim();
			if (line.isEmpty())
				continue;

			byte midiNote;
			byte lotroNote;
			try
			{
				StringTokenizer tokenizer = new StringTokenizer(line, " \t=>");
				midiNote = Byte.parseByte(tokenizer.nextToken());
				lotroNote = Byte.parseByte(tokenizer.nextToken());
				if (tokenizer.hasMoreTokens())
				{
					throw new ParseException("Invalid line (too many tokens)", inputFileName, lineNumber);
				}
			}
			catch (NoSuchElementException nse)
			{
				throw new ParseException("Invalid line (too few tokens)", inputFileName, lineNumber);
			}
			catch (NumberFormatException nfe)
			{
				throw new ParseException("Invalid note ID", inputFileName, lineNumber);
			}

			if (midiNote < MidiConstants.LOWEST_NOTE_ID || midiNote > MidiConstants.HIGHEST_NOTE_ID)
			{
				throw new ParseException("MIDI note is invalid", inputFileName, lineNumber);
			}
			if (lotroNote != DISABLED_NOTE_ID && !LotroInstrument.BASIC_DRUM.isPlayable(lotroNote))
			{
				throw new ParseException("ABC note is invalid", inputFileName, lineNumber);
			}

			map[midiNote] = lotroNote;
		}

		fireChangeEvent();
	}

	public void saveToXml(Element ele)
	{
		if (map == null)
			return;

		for (int midiId = 0; midiId < MidiConstants.NOTE_COUNT; midiId++)
		{
			int lotroId = get(midiId);
			if (lotroId == DISABLED_NOTE_ID)
				continue;

			Element noteEle = ele.getOwnerDocument().createElement("note");
			ele.appendChild(noteEle);
			noteEle.setAttribute("id", String.valueOf(midiId));
			noteEle.setAttribute("lotroId", String.valueOf(lotroId));
		}
	}

	public static DrumNoteMap loadFromXml(Element ele, Version fileVersion) throws ParseException
	{
		try
		{
			boolean isPassthrough = SaveUtil.parseValue(ele, "@isPassthrough", false);
			DrumNoteMap retVal = isPassthrough ? new PassThroughDrumNoteMap() : new DrumNoteMap();
			retVal.loadFromXmlInternal(ele, fileVersion);
			return retVal;
		}
		catch (XPathExpressionException e)
		{
			throw new RuntimeException(e);
		}
	}

	private void loadFromXmlInternal(Element ele, Version fileVersion) throws ParseException, XPathExpressionException
	{
		if (map == null)
			map = new byte[MidiConstants.NOTE_COUNT];

		Arrays.fill(map, DISABLED_NOTE_ID);

		for (Element noteEle : XmlUtil.selectElements(ele, "note"))
		{
			int midiId = SaveUtil.parseValue(noteEle, "@id", DISABLED_NOTE_ID);
			byte lotroId = SaveUtil.parseValue(noteEle, "@lotroId", DISABLED_NOTE_ID);
			if (midiId >= 0 && midiId < map.length && LotroInstrument.BASIC_DRUM.isPlayable(lotroId))
				map[midiId] = lotroId;
		}
	}

	/**
	 * This can be used as a backup in the event that loading the drum map from a file fails.
	 */
	public byte[] getFailsafeDefault()
	{
		byte[] failsafe = new byte[MidiConstants.NOTE_COUNT];

		Arrays.fill(failsafe, DISABLED_NOTE_ID);

		failsafe[26] = 49;
		failsafe[27] = 72;
		failsafe[28] = 70;
		// failsafe[29] = DISABLED_NOTE_ID;
		// failsafe[30] = DISABLED_NOTE_ID;
		failsafe[31] = 51;
		failsafe[32] = 50;
		failsafe[33] = 39;
		// failsafe[34] = DISABLED_NOTE_ID;
		failsafe[35] = 49;
		failsafe[36] = 58;
		failsafe[37] = 51;
		failsafe[38] = 52;
		failsafe[39] = 53;
		failsafe[40] = 54;
		failsafe[41] = 49;
		failsafe[42] = 37;
		failsafe[43] = 69;
		failsafe[44] = 59;
		failsafe[45] = 47;
		failsafe[46] = 60;
		failsafe[47] = 63;
		failsafe[48] = 43;
		failsafe[49] = 57;
		failsafe[50] = 45;
		failsafe[51] = 55;
		failsafe[52] = 57;
		failsafe[53] = 43;
		failsafe[54] = 46;
		failsafe[55] = 57;
		failsafe[56] = 45;
		failsafe[57] = 57;
		failsafe[58] = 53;
		failsafe[59] = 60;
		failsafe[60] = 38;
		failsafe[61] = 69;
		failsafe[62] = 39;
		failsafe[63] = 70;
		failsafe[64] = 48;
		failsafe[65] = 65;
		failsafe[66] = 64;
		failsafe[67] = 43;
		failsafe[68] = 47;
		failsafe[69] = 37;
		failsafe[70] = 42;
		// failsafe[71] = DISABLED_NOTE_ID;
		// failsafe[72] = DISABLED_NOTE_ID;
		failsafe[73] = 64;
		failsafe[74] = 62;
		failsafe[75] = 43;
		failsafe[76] = 51;
		failsafe[77] = 67;
		failsafe[78] = 65;
		failsafe[79] = 64;
		failsafe[80] = 43;
		failsafe[81] = 43;
		failsafe[82] = 42;
		failsafe[83] = 44;
		// failsafe[84] = DISABLED_NOTE_ID;
		failsafe[85] = 72;
		failsafe[86] = 48;
		failsafe[87] = 58;

		return failsafe;
	}
}
