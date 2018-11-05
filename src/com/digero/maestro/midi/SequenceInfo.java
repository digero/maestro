package com.digero.maestro.midi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import com.digero.common.abctomidi.AbcInfo;
import com.digero.common.abctomidi.AbcToMidi;
import com.digero.common.midi.MidiConstants;
import com.digero.common.midi.KeySignature;
import com.digero.common.midi.MidiFactory;
import com.digero.common.midi.TimeSignature;
import com.digero.common.util.Pair;
import com.digero.common.util.ParseException;
import com.digero.maestro.abc.AbcConversionException;
import com.digero.maestro.abc.AbcExporter;
import com.digero.maestro.abc.AbcExporter.ExportTrackInfo;
import com.digero.maestro.abc.AbcMetadataSource;
import com.sun.media.sound.MidiUtils;

/**
 * Container for a MIDI sequence. If necessary, converts type 0 MIDI files to type 1.
 */
public class SequenceInfo implements MidiConstants
{
	private final Sequence sequence;
	private final SequenceDataCache sequenceCache;
	private final String fileName;
	private String title;
	private String composer;
	private int primaryTempoMPQ;
	private final List<TrackInfo> trackInfoList;

	public static SequenceInfo fromAbc(AbcToMidi.Params params) throws InvalidMidiDataException, ParseException
	{
		if (params.abcInfo == null)
			params.abcInfo = new AbcInfo();
		SequenceInfo sequenceInfo = new SequenceInfo(params.filesData.get(0).file.getName(), AbcToMidi.convert(params));
		sequenceInfo.title = params.abcInfo.getTitle();
		sequenceInfo.composer = params.abcInfo.getComposer();
		sequenceInfo.primaryTempoMPQ = (int) Math.round(MidiUtils.convertTempo(params.abcInfo.getPrimaryTempoBPM()));
		return sequenceInfo;
	}

	public static SequenceInfo fromMidi(File midiFile) throws InvalidMidiDataException, IOException, ParseException
	{
		return new SequenceInfo(midiFile.getName(), MidiSystem.getSequence(midiFile));
	}

	public static SequenceInfo fromAbcParts(AbcExporter abcExporter, boolean useLotroInstruments)
			throws InvalidMidiDataException, AbcConversionException
	{
		return new SequenceInfo(abcExporter, useLotroInstruments);
	}

	private SequenceInfo(String fileName, Sequence sequence) throws InvalidMidiDataException, ParseException
	{
		this.fileName = fileName;
		this.sequence = sequence;

		// Since the drum track separation is only applicable to type 1 midi sequences, 
		// do it before we convert this sequence to type 1, to avoid doing unnecessary work
		separateDrumTracks(sequence);
		convertToType1(sequence);
		fixupTrackLength(sequence);

		Track[] tracks = sequence.getTracks();
		if (tracks.length == 0)
		{
			throw new InvalidMidiDataException("The MIDI file doesn't have any tracks");
		}

		sequenceCache = new SequenceDataCache(sequence);
		primaryTempoMPQ = sequenceCache.getPrimaryTempoMPQ();

		List<TrackInfo> trackInfoList = new ArrayList<TrackInfo>(tracks.length);
		for (int i = 0; i < tracks.length; i++)
		{
			trackInfoList.add(new TrackInfo(this, tracks[i], i, sequenceCache));
		}

		composer = "";
		if (trackInfoList.get(0).hasName())
		{
			title = trackInfoList.get(0).getName();
		}
		else
		{
			title = fileName;
			int dot = title.lastIndexOf('.');
			if (dot > 0)
				title = title.substring(0, dot);
			title = title.replace('_', ' ');
		}

		this.trackInfoList = Collections.unmodifiableList(trackInfoList);
	}

	private SequenceInfo(AbcExporter abcExporter, boolean useLotroInstruments) throws InvalidMidiDataException,
			AbcConversionException
	{
		AbcMetadataSource metadata = abcExporter.getMetadataSource();
		this.fileName = metadata.getSongTitle() + ".abc";
		this.composer = metadata.getComposer();
		this.title = metadata.getSongTitle();

		Pair<List<ExportTrackInfo>, Sequence> result = abcExporter.exportToPreview(useLotroInstruments);

		sequence = result.second;
		sequenceCache = new SequenceDataCache(sequence);
		primaryTempoMPQ = sequenceCache.getPrimaryTempoMPQ();

		List<TrackInfo> trackInfoList = new ArrayList<TrackInfo>(result.first.size());
		for (ExportTrackInfo i : result.first)
		{
			trackInfoList.add(new TrackInfo(this, i.trackNumber, i.part.getTitle(), i.part.getInstrument(), abcExporter
					.getTimingInfo().getMeter(), abcExporter.getKeySignature(), i.noteEvents));
		}

		this.trackInfoList = Collections.unmodifiableList(trackInfoList);
	}

	public String getFileName()
	{
		return fileName;
	}

	public Sequence getSequence()
	{
		return sequence;
	}

	public String getTitle()
	{
		return title;
	}

	public String getComposer()
	{
		return composer;
	}

	public int getTrackCount()
	{
		return trackInfoList.size();
	}

	public TrackInfo getTrackInfo(int track)
	{
		return trackInfoList.get(track);
	}

	public List<TrackInfo> getTrackList()
	{
		return trackInfoList;
	}

	public int getPrimaryTempoMPQ()
	{
		return primaryTempoMPQ;
	}

	public int getPrimaryTempoBPM()
	{
		return (int) Math.round(MidiUtils.convertTempo(getPrimaryTempoMPQ()));
	}

	public boolean hasTempoChanges()
	{
		return sequenceCache.getTempoEvents().size() > 1;
	}

	public KeySignature getKeySignature()
	{
		for (TrackInfo track : trackInfoList)
		{
			if (track.getKeySignature() != null)
				return track.getKeySignature();
		}
		return KeySignature.C_MAJOR;
	}

	public TimeSignature getTimeSignature()
	{
		for (TrackInfo track : trackInfoList)
		{
			if (track.getTimeSignature() != null)
				return track.getTimeSignature();
		}
		return TimeSignature.FOUR_FOUR;
	}

	public SequenceDataCache getDataCache()
	{
		return sequenceCache;
	}

	@Override public String toString()
	{
		return getTitle();
	}

	public long calcFirstNoteTick()
	{
		long firstNoteTick = Long.MAX_VALUE;
		for (Track t : sequence.getTracks())
		{
			for (int j = 0; j < t.size(); j++)
			{
				MidiEvent evt = t.get(j);
				MidiMessage msg = evt.getMessage();
				if (msg instanceof ShortMessage)
				{
					ShortMessage m = (ShortMessage) msg;
					if (m.getCommand() == ShortMessage.NOTE_ON)
					{
						if (evt.getTick() < firstNoteTick)
						{
							firstNoteTick = evt.getTick();
						}
						break;
					}
				}
			}
		}
		if (firstNoteTick == Long.MAX_VALUE)
			return 0;
		return firstNoteTick;
	}

	public long calcLastNoteTick()
	{
		long lastNoteTick = 0;
		for (Track t : sequence.getTracks())
		{
			for (int j = t.size() - 1; j >= 0; j--)
			{
				MidiEvent evt = t.get(j);
				MidiMessage msg = evt.getMessage();
				if (msg instanceof ShortMessage)
				{
					ShortMessage m = (ShortMessage) msg;
					if (m.getCommand() == ShortMessage.NOTE_OFF)
					{
						if (evt.getTick() > lastNoteTick)
						{
							lastNoteTick = evt.getTick();
						}
						break;
					}
				}
			}
		}

		return lastNoteTick;
	}

	@SuppressWarnings("unchecked")//
	public static void fixupTrackLength(Sequence song)
	{
//		System.out.println("Before: " + Util.formatDuration(song.getMicrosecondLength()));
//		TempoCache tempoCache = new TempoCache(song);
		Track[] tracks = song.getTracks();
		List<MidiEvent>[] suspectEvents = new List[tracks.length];
		long endTick = 0;

		for (int i = 0; i < tracks.length; i++)
		{
			Track track = tracks[i];
			for (int j = track.size() - 1; j >= 0; --j)
			{
				MidiEvent evt = track.get(j);
				if (MidiUtils.isMetaEndOfTrack(evt.getMessage()))
				{
					if (suspectEvents[i] == null)
						suspectEvents[i] = new ArrayList<MidiEvent>();
					suspectEvents[i].add(evt);
				}
				else if (evt.getTick() > endTick)
				{
					// Seems like some songs have extra meta messages way past the end
					if (evt.getMessage() instanceof MetaMessage)
					{
						if (suspectEvents[i] == null)
							suspectEvents[i] = new ArrayList<MidiEvent>();
						suspectEvents[i].add(0, evt);
					}
					else
					{
						endTick = evt.getTick();
						break;
					}
				}
			}
		}

		for (int i = 0; i < tracks.length; i++)
		{
			for (MidiEvent evt : suspectEvents[i])
			{
				if (evt.getTick() > endTick)
				{
					tracks[i].remove(evt);
//					System.out.println("Moving event from "
//							+ Util.formatDuration(MidiUtils.tick2microsecond(song, evt.getTick(), tempoCache)) + " to "
//							+ Util.formatDuration(MidiUtils.tick2microsecond(song, endTick, tempoCache)));
					evt.setTick(endTick);
					tracks[i].add(evt);
				}
			}
		}

//		System.out.println("Real song duration: "
//				+ Util.formatDuration(MidiUtils.tick2microsecond(song, endTick, tempoCache)));
//		System.out.println("After: " + Util.formatDuration(song.getMicrosecondLength()));
	}

	/**
	 * Separates the MIDI file to have one track per channel (Type 1).
	 */
	public static void convertToType1(Sequence song)
	{
		if (song.getTracks().length == 1)
		{
			Track track0 = song.getTracks()[0];
			Track[] tracks = new Track[CHANNEL_COUNT];

			int trackNumber = 1;
			int i = 0;
			while (i < track0.size())
			{
				MidiEvent evt = track0.get(i);
				if (evt.getMessage() instanceof ShortMessage)
				{
					int chan = ((ShortMessage) evt.getMessage()).getChannel();
					if (tracks[chan] == null)
					{
						tracks[chan] = song.createTrack();
						String trackName = (chan == DRUM_CHANNEL) ? "Drums" : ("Track " + trackNumber);
						trackNumber++;
						tracks[chan].add(MidiFactory.createTrackNameEvent(trackName));
					}
					tracks[chan].add(evt);
					if (track0.remove(evt))
						continue;
				}
				i++;
			}
		}
	}

	/**
	 * Ensures that there are no tracks with both drums and notes.
	 */
	public static void separateDrumTracks(Sequence song)
	{
		Track[] tracks = song.getTracks();
		// This doesn't work on Type 0 MIDI files
		if (tracks.length <= 1)
			return;

		final int DRUMS = 0x1;
		final int NOTES = 0x2;
		final int MIXED = DRUMS | NOTES;

		int[] trackContents = new int[tracks.length];
		boolean hasMixed = false;

		for (int i = 0; i < tracks.length; i++)
		{
			Track track = tracks[i];
			for (int j = 0; j < track.size(); j++)
			{
				MidiEvent evt = track.get(j);
				MidiMessage msg = evt.getMessage();
				if (msg instanceof ShortMessage)
				{
					ShortMessage m = (ShortMessage) msg;
					if (m.getCommand() == ShortMessage.NOTE_ON)
					{
						if (m.getChannel() == DRUM_CHANNEL)
						{
							trackContents[i] |= DRUMS;
						}
						else
						{
							trackContents[i] |= NOTES;
						}

						if (trackContents[i] == MIXED)
						{
							hasMixed = true;
							break;
						}
					}
				}
			}
		}

		// If there are no mixed tracks, don't need to do anything
		if (!hasMixed)
			return;

		Track drumTrack = song.createTrack();
		drumTrack.add(MidiFactory.createTrackNameEvent("Drums"));
		for (int i = 0; i < tracks.length; i++)
		{
			Track track = tracks[i];
			if (trackContents[i] == MIXED)
			{
				// Mixed track: copy only the events on the drum channel
				for (int j = 0; j < track.size(); j++)
				{
					MidiEvent evt = track.get(j);
					MidiMessage msg = evt.getMessage();
					if ((msg instanceof ShortMessage) && ((ShortMessage) msg).getChannel() == DRUM_CHANNEL)
					{
						drumTrack.add(evt);
						if (track.remove(evt))
							j--;
					}
				}
			}
		}
	}
}
