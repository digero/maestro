package com.digero.tools.soundfont;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.digero.common.abc.AbcConstants;
import com.digero.common.abc.LotroInstrument;

public class SampleInfo implements Comparable<SampleInfo>
{
	public static class Key implements Comparable<Key>
	{
		public final LotroInstrument lotroInstrument;
		public final int noteId;

		public Key(LotroInstrument lotroInstrument, int noteId)
		{
			this.lotroInstrument = lotroInstrument;
			this.noteId = noteId;
		}

		@Override public int compareTo(Key o)
		{
			if (o == null)
				return 1;

			if (lotroInstrument != o.lotroInstrument)
				return lotroInstrument.name().compareTo(o.lotroInstrument.name());

			return noteId - o.noteId;
		}

		@Override public boolean equals(Object obj)
		{
			if (obj == null || obj.getClass() != Key.class)
				return false;

			return compareTo((Key) obj) == 0;
		}

		@Override public int hashCode()
		{
			return (lotroInstrument.ordinal() << 8) | noteId;
		}
	}

	public final Key key;
	public final File file;
	public final String name;
	public final float sampleRate;

	private static final Pattern FILE_NAME_REGEX = Pattern.compile("(([a-z_]+)(_([0-9][0-9]).*)?)\\.wav",
			Pattern.CASE_INSENSITIVE);
	private static final int FILE_NAME_GROUP = 1;
	private static final int FILE_INSTRUMENT_GROUP = 2;
	private static final int FILE_NOTEID_GROUP = 4;

	public static boolean isSampleFile(File file)
	{
		return FILE_NAME_REGEX.matcher(file.getName()).matches();
	}

	public SampleInfo(File file) throws IOException
	{
		this.file = file;
		Matcher matcher = FILE_NAME_REGEX.matcher(file.getName());
		if (!matcher.matches())
			throw new RuntimeException();

		this.name = matcher.group(FILE_NAME_GROUP);

		LotroInstrument lotroInstrument = LotroInstrument.parseInstrument(matcher.group(FILE_INSTRUMENT_GROUP));

		int noteId;
		if (lotroInstrument == LotroInstrument.COWBELL || lotroInstrument == LotroInstrument.MOOR_COWBELL)
			noteId = AbcConstants.COWBELL_NOTE_ID;
		else
			noteId = Integer.parseInt(matcher.group(FILE_NOTEID_GROUP));

		this.key = new Key(lotroInstrument, noteId);

		try
		{
			AudioFileFormat format = AudioSystem.getAudioFileFormat(file);
			this.sampleRate = format.getFormat().getSampleRate();
		}
		catch (UnsupportedAudioFileException e)
		{
			throw new RuntimeException(e);
		}
	}

	public void print(PrintStream out)
	{
		out.println("    SampleName=" + name);
		out.println("        SampleRate=" + ((int) sampleRate));
		out.println("        Key=" + key.noteId);
		out.println("        FineTune=0");
		out.println("        Type=1");
		out.println();
	}

	@Override public int compareTo(SampleInfo o)
	{
		if (o == null)
			return 1;

		int result = key.compareTo(o.key);
		if (result != 0)
			return result;

		return file.compareTo(o.file);
	}

	@Override public boolean equals(Object obj)
	{
		if (obj == null || obj.getClass() != SampleInfo.class)
			return false;

		return compareTo((SampleInfo) obj) == 0;
	}

	@Override public int hashCode()
	{
		return key.hashCode() ^ file.hashCode();
	}
}