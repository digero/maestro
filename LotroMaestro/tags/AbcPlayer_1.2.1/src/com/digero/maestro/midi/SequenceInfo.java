package com.digero.maestro.midi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import com.digero.common.abctomidi.AbcToMidi;
import com.digero.common.midi.IMidiConstants;
import com.digero.common.midi.KeySignature;
import com.digero.common.midi.MidiFactory;
import com.digero.common.midi.TimeSignature;
import com.digero.common.util.ParseException;
import com.sun.media.sound.MidiUtils;
import com.sun.media.sound.MidiUtils.TempoCache;

/**
 * Container for a MIDI sequence. If necessary, converts type 0 MIDI files to
 * type 1.
 */
public class SequenceInfo implements IMidiConstants {
	private File file;
	private Sequence sequence;
	private String title;
	private int tempoBPM;
	private long endMicros;
	private List<TrackInfo> trackInfoList;

	public SequenceInfo(File file, boolean isAbc) throws InvalidMidiDataException, IOException, ParseException {
		this.file = file;
		if (isAbc) {
			sequence = AbcToMidi.convert(file, false, null, null, false, true);
		}
		else {
			sequence = MidiSystem.getSequence(file);
		}

		// Since the drum track separation is only applicable to type 1 midi sequences, 
		// do it before we convert this sequence to type 1, to avoid doing unnecessary work
		separateDrumTracks(sequence);
		convertToType1(sequence);
		fixupTrackLength(sequence);

		Track[] tracks = sequence.getTracks();
		if (tracks.length == 0) {
			throw new InvalidMidiDataException("The MIDI file doesn't have any tracks");
		}

		MidiUtils.TempoCache tempoCache = new MidiUtils.TempoCache(sequence);
		SequenceDataCache sequenceCache = new SequenceDataCache(sequence);

		trackInfoList = new ArrayList<TrackInfo>(tracks.length);
		endMicros = 0;
		for (int i = 0; i < tracks.length; i++) {
			TrackInfo track = new TrackInfo(this, tracks[i], i, tempoCache, sequenceCache);
			trackInfoList.add(track);

			if (track.hasEvents())
				endMicros = Math.max(endMicros, track.getEvents().get(track.getEventCount() - 1).endMicros);
		}

		tempoBPM = findMainTempo(sequence, tempoCache);

		if (trackInfoList.get(0).hasName()) {
			title = trackInfoList.get(0).getName();
		}
		else {
			title = file.getName();
			int dot = title.lastIndexOf('.');
			if (dot > 0)
				title = title.substring(0, dot);
			title = title.replace('_', ' ');
		}

		trackInfoList = Collections.unmodifiableList(trackInfoList);
	}

	public File getFile() {
		return file;
	}

	public Sequence getSequence() {
		return sequence;
	}

	public String getTitle() {
		return title;
	}

	public int getTrackCount() {
		return trackInfoList.size();
	}

	public TrackInfo getTrackInfo(int track) {
		return trackInfoList.get(track);
	}

	public List<TrackInfo> getTrackList() {
		return trackInfoList;
	}

	public int getTempoBPM() {
		return tempoBPM;
	}

	public long getEndMicros() {
		return endMicros;
	}

	public KeySignature getKeySignature() {
		for (TrackInfo track : trackInfoList) {
			if (track.getKeySignature() != null)
				return track.getKeySignature();
		}
		return KeySignature.C_MAJOR;
	}

	public TimeSignature getTimeSignature() {
		for (TrackInfo track : trackInfoList) {
			if (track.getTimeSignature() != null)
				return track.getTimeSignature();
		}
		return TimeSignature.FOUR_FOUR;
	}

	@Override
	public String toString() {
		return getTitle();
	}

	public static int findMainTempo(Sequence sequence, TempoCache tempoCache) {
		Map<Integer, Long> tempoLengths = new HashMap<Integer, Long>();

		long bestTempoLength = 0;
		int bestTempoBPM = 120;

		long curTempoStart = 0;
		int curTempoBPM = 120;

		Track track0 = sequence.getTracks()[0];
		int c = track0.size();
		for (int i = 0; i < c; i++) {
			MidiEvent evt = track0.get(i);
			MidiMessage msg = evt.getMessage();
			if (MidiUtils.isMetaTempo(msg)) {
				long nextTempoStart = MidiUtils.tick2microsecond(sequence, evt.getTick(), tempoCache);

				Long lengthObj = tempoLengths.get(curTempoBPM);
				long length = (lengthObj == null) ? 0 : lengthObj;
				length += nextTempoStart - curTempoStart;

				if (length > bestTempoLength) {
					bestTempoLength = length;
					bestTempoBPM = curTempoBPM;
				}

				tempoLengths.put(curTempoBPM, length);

				curTempoBPM = (int) (MidiUtils.convertTempo(MidiUtils.getTempoMPQ(msg)) + 0.5);
				curTempoStart = nextTempoStart;
			}
		}

		Long lengthObj = tempoLengths.get(curTempoBPM);
		long length = (lengthObj == null) ? 0 : lengthObj;
		length += sequence.getMicrosecondLength() - curTempoStart;

		if (length > bestTempoLength) {
			bestTempoLength = length;
			bestTempoBPM = curTempoBPM;
		}

		return bestTempoBPM;
	}

	public long calcFirstNoteTick() {
		long firstNoteTick = Long.MAX_VALUE;
		for (Track t : sequence.getTracks()) {
			for (int j = 0; j < t.size(); j++) {
				MidiEvent evt = t.get(j);
				MidiMessage msg = evt.getMessage();
				if (msg instanceof ShortMessage) {
					ShortMessage m = (ShortMessage) msg;
					if (m.getCommand() == ShortMessage.NOTE_ON) {
						if (evt.getTick() < firstNoteTick) {
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

	@SuppressWarnings("unchecked")
	public static void fixupTrackLength(Sequence song) {
//		System.out.println("Before: " + Util.formatDuration(song.getMicrosecondLength()));
//		TempoCache tempoCache = new TempoCache(song);
		Track[] tracks = song.getTracks();
		List<MidiEvent>[] suspectEvents = new List[tracks.length];
		long endTick = 0;

		for (int i = 0; i < tracks.length; i++) {
			Track track = tracks[i];
			for (int j = track.size() - 1; j >= 0; --j) {
				MidiEvent evt = track.get(j);
				if (MidiUtils.isMetaEndOfTrack(evt.getMessage())) {
					if (suspectEvents[i] == null)
						suspectEvents[i] = new ArrayList<MidiEvent>();
					suspectEvents[i].add(evt);
				}
				else if (evt.getTick() > endTick) {
					// Seems like some songs have extra meta messages way past the end
					if (evt.getMessage() instanceof MetaMessage) {
						if (suspectEvents[i] == null)
							suspectEvents[i] = new ArrayList<MidiEvent>();
						suspectEvents[i].add(0, evt);
					}
					else {
						endTick = evt.getTick();
						break;
					}
				}
			}
		}

		for (int i = 0; i < tracks.length; i++) {
			for (MidiEvent evt : suspectEvents[i]) {
				if (evt.getTick() > endTick) {
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
	public static void convertToType1(Sequence song) {
		if (song.getTracks().length == 1) {
			Track track0 = song.getTracks()[0];
			Track[] tracks = new Track[CHANNEL_COUNT];

			int trackNumber = 1;
			int i = 0;
			while (i < track0.size()) {
				MidiEvent evt = track0.get(i);
				if (evt.getMessage() instanceof ShortMessage) {
					int chan = ((ShortMessage) evt.getMessage()).getChannel();
					if (tracks[chan] == null) {
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
	public static void separateDrumTracks(Sequence song) {
		Track[] tracks = song.getTracks();
		// This doesn't work on Type 0 MIDI files
		if (tracks.length <= 1)
			return;

		final int DRUMS = 0x1;
		final int NOTES = 0x2;
		final int MIXED = DRUMS | NOTES;

		int[] trackContents = new int[tracks.length];
		boolean hasMixed = false;

		for (int i = 0; i < tracks.length; i++) {
			Track track = tracks[i];
			for (int j = 0; j < track.size(); j++) {
				MidiEvent evt = track.get(j);
				MidiMessage msg = evt.getMessage();
				if (msg instanceof ShortMessage) {
					ShortMessage m = (ShortMessage) msg;
					if (m.getCommand() == ShortMessage.NOTE_ON) {
						if (m.getChannel() == DRUM_CHANNEL) {
							trackContents[i] |= DRUMS;
						}
						else {
							trackContents[i] |= NOTES;
						}

						if (trackContents[i] == MIXED) {
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
		for (int i = 0; i < tracks.length; i++) {
			Track track = tracks[i];
			if (trackContents[i] == MIXED) {
				// Mixed track: copy only the events on the drum channel
				for (int j = 0; j < track.size(); j++) {
					MidiEvent evt = track.get(j);
					MidiMessage msg = evt.getMessage();
					if ((msg instanceof ShortMessage) && ((ShortMessage) msg).getChannel() == DRUM_CHANNEL) {
						drumTrack.add(evt);
						if (track.remove(evt))
							j--;
					}
				}
			}
		}
	}
}
