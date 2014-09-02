package com.digero.maestro.abc;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;

import com.digero.common.abc.AbcField;
import com.digero.common.abc.Dynamics;
import com.digero.common.abc.LotroInstrument;
import com.digero.common.midi.KeySignature;
import com.digero.common.midi.MidiConstants;
import com.digero.common.midi.MidiFactory;
import com.digero.common.midi.Note;
import com.digero.common.midi.PanGenerator;
import com.digero.common.util.Pair;
import com.digero.common.util.Util;
import com.digero.maestro.MaestroMain;
import com.digero.maestro.midi.Chord;
import com.digero.maestro.midi.NoteEvent;

public class AbcExporter
{
	private final List<AbcPart> parts;
	private final AbcMetadataSource metadata;
	private QuantizedTimingInfo qtm;
	private KeySignature keySignature;

	private long exportStartTick;
	private long exportEndTick;

	public AbcExporter(List<AbcPart> parts, QuantizedTimingInfo timingInfo, KeySignature keySignature,
			AbcMetadataSource metadata) throws AbcConversionException
	{
		this.parts = parts;
		this.qtm = timingInfo;
		this.metadata = metadata;
		setKeySignature(keySignature);
	}

	public List<AbcPart> getParts()
	{
		return parts;
	}

	public QuantizedTimingInfo getTimingInfo()
	{
		return qtm;
	}

	public void setTimingInfo(QuantizedTimingInfo timingInfo)
	{
		this.qtm = timingInfo;
	}

	public KeySignature getKeySignature()
	{
		return keySignature;
	}

	public void setKeySignature(KeySignature keySignature) throws AbcConversionException
	{
		if (keySignature.sharpsFlats != 0)
			throw new AbcConversionException("Only C major and A minor are currently supported");

		this.keySignature = keySignature;
	}

	public AbcMetadataSource getMetadataSource()
	{
		return metadata;
	}

	public long getExportStartTick()
	{
		return exportStartTick;
	}

	public long getExportEndTick()
	{
		return exportEndTick;
	}

	@Deprecated public long getExportStartMicros()
	{
		return qtm.tickToMicros(getExportStartTick());
	}

	@Deprecated public long getExportEndMicros()
	{
		return qtm.tickToMicros(getExportEndTick());
	}

	public void exportToAbc(OutputStream os) throws AbcConversionException
	{
		Pair<Long, Long> startEnd = getSongStartEndTick(true /* lengthenToBar */, false /* accountForSustain */);
		exportStartTick = startEnd.first;
		exportEndTick = startEnd.second;

		PrintStream out = new PrintStream(os);
		if (!parts.isEmpty())
		{
			out.println(AbcField.SONG_TITLE + metadata.getSongTitle());
			if (metadata.getComposer().length() > 0)
			{
				out.println(AbcField.SONG_COMPOSER + metadata.getComposer());
			}
			out.println(AbcField.SONG_DURATION + Util.formatDuration(metadata.getSongLengthMicros()));
			if (metadata.getTranscriber().length() > 0)
			{
				out.println(AbcField.SONG_TRANSCRIBER + metadata.getTranscriber());
			}

			// TODO clean this up
			{
				out.println();
				out.println("%%tempo-ppq " + qtm.getMidiResolution());

				final String SONG_TEMPO = "%%tempo-map ";
				final int WRAP_LENGTH = 160 - SONG_TEMPO.length();
				int numTempoChanges = 0;
				StringBuilder sb = new StringBuilder(0);
				int primaryTempoBPM = qtm.getPrimaryExportTempoBPM();
				for (QuantizedTimingInfo.TimingInfoEvent event : qtm.getTimingInfoByTick().values())
				{
					if (event.tick > exportEndTick)
						break;

					long tick = event.tick - exportStartTick;
					if (tick <= 0)
					{
						tick = 0;
						numTempoChanges = 0;
						sb.setLength(0);
					}

					String tempoString = event.info.getExportTempoBPM() + "/" + primaryTempoBPM + "@" + tick + ";";
					if (sb.length() + tempoString.length() > WRAP_LENGTH)
					{
						out.print(SONG_TEMPO);
						out.println(sb.substring(0, sb.length() - 1)); // Exclude trailing ";"
						sb.setLength(0);
					}
					sb.append(tempoString);
					numTempoChanges++;
				}

				if (numTempoChanges > 1)
				{
					out.print(SONG_TEMPO);
					out.println(sb.substring(0, sb.length() - 1)); // Exclude trailing ";"
				}
			}

			out.println();
			out.println(AbcField.ABC_CREATOR + MaestroMain.APP_NAME + " v" + MaestroMain.APP_VERSION);
			out.println(AbcField.ABC_VERSION + "2.0");
			out.println();
		}

		for (AbcPart part : parts)
		{
			exportPartToAbc(part, exportStartTick, exportEndTick, out);
		}
	}

	public class ExportTrackInfo
	{
		public final int trackNumber;
		public final AbcPart part;
		public final List<NoteEvent> noteEvents;

		public ExportTrackInfo(int trackNumber, AbcPart part, List<NoteEvent> noteEvents)
		{
			this.trackNumber = trackNumber;
			this.part = part;
			this.noteEvents = noteEvents;
		}
	}

	public Pair<List<ExportTrackInfo>, Sequence> exportToPreview(boolean useLotroInstruments)
			throws AbcConversionException, InvalidMidiDataException
	{
		Sequence sequence = new Sequence(Sequence.PPQ, qtm.getMidiResolution());

		// Track 0: Title and meta info
		Track track0 = sequence.createTrack();
		track0.add(MidiFactory.createTrackNameEvent(metadata.getSongTitle()));
		addMidiTempoEvents(track0);

		Pair<Long, Long> startEndTick = getSongStartEndTick(true /* lengthenToBar */, false /* accountForSustain */);
		exportStartTick = startEndTick.first;
		exportEndTick = startEndTick.second;

		PanGenerator panner = new PanGenerator();
		List<ExportTrackInfo> infoList = new ArrayList<ExportTrackInfo>();
		for (AbcPart part : parts)
		{
			int pan = (parts.size() > 1) ? panner.get(part.getInstrument(), part.getTitle()) : PanGenerator.CENTER;
			infoList.add(exportPartToPreview(part, sequence, exportStartTick, exportEndTick, pan, useLotroInstruments));
		}

		return new Pair<List<ExportTrackInfo>, Sequence>(infoList, sequence);
	}

	private void addMidiTempoEvents(Track track0)
	{
		for (QuantizedTimingInfo.TimingInfoEvent event : qtm.getTimingInfoByTick().values())
		{
			track0.add(MidiFactory.createTempoEvent(event.info.getTempoMPQ(), event.tick));

			if (event.tick == 0)
			{
				// The Java MIDI sequencer can sometimes miss a tempo event at tick 0
				// Add another tempo event at tick 1 to work around the bug
				track0.add(MidiFactory.createTempoEvent(event.info.getTempoMPQ(), 1));
			}
		}
	}

	private ExportTrackInfo exportPartToPreview(AbcPart part, Sequence sequence, long songStartTick, long songEndTick,
			int pan, boolean useLotroInstruments) throws AbcConversionException
	{
		List<Chord> chords = combineAndQuantize(part, false, songStartTick, songEndTick);

		int trackNumber = exportPartToMidi(part, sequence, chords, pan, useLotroInstruments);

		List<NoteEvent> noteEvents = new ArrayList<NoteEvent>(chords.size());
		for (Chord chord : chords)
		{
			for (int i = 0; i < chord.size(); i++)
			{
				NoteEvent ne = chord.get(i);
				// Skip rests and notes that are the continuation of a tied note
				if (ne.note == Note.REST || ne.tiesFrom != null)
					continue;

				// Convert tied notes into a single note event
				if (ne.tiesTo != null)
				{
					ne.setEndTick(ne.getTieEnd().getEndTick());
					ne.tiesTo = null;
					// Not fixing up the ne.tiesTo.tiesFrom pointer since we that for the    
					// (ne.tiesFrom != null) check above, and we otherwise don't care about ne.tiesTo.
				}

				noteEvents.add(ne);
			}
		}

		return new ExportTrackInfo(trackNumber, part, noteEvents);
	}

//	private void exportPartToMidi(AbcPart part, Sequence out, long songStartMicros, long songEndMicros, int pan,
//			boolean useLotroInstruments) throws AbcConversionException
//	{
//		if (out.getDivisionType() != Sequence.PPQ || out.getResolution() != qtm.getMidiResolution())
//		{
//			throw new AbcConversionException("Sequence has incorrect timing data");
//		}
//
//		List<Chord> chords = combineAndQuantize(part, false, songStartMicros, songEndMicros);
//		exportPartToMidi(part, out, chords, pan, useLotroInstruments);
//	}

	private int exportPartToMidi(AbcPart part, Sequence out, List<Chord> chords, int pan, boolean useLotroInstruments)
	{
		int trackNumber = out.getTracks().length;
		part.setPreviewSequenceTrackNumber(trackNumber);
		int channel = trackNumber;
		if (channel >= MidiConstants.DRUM_CHANNEL)
			channel++;

		Track track = out.createTrack();

		track.add(MidiFactory.createTrackNameEvent(part.getTitle()));
		track.add(MidiFactory.createProgramChangeEvent(part.getInstrument().midiProgramId, channel, 0));
		if (useLotroInstruments)
			track.add(MidiFactory.createChannelVolumeEvent(MidiConstants.MAX_VOLUME, channel, 1));
		track.add(MidiFactory.createPanEvent(pan, channel));

		List<NoteEvent> notesOn = new ArrayList<NoteEvent>();

		int noteDelta = 0;
		if (!useLotroInstruments)
			noteDelta = part.getInstrument().octaveDelta * 12;

		for (Chord chord : chords)
		{
			Dynamics dynamics = chord.calcDynamics();
			if (dynamics == null)
				dynamics = Dynamics.DEFAULT;
			for (int j = 0; j < chord.size(); j++)
			{
				NoteEvent ne = chord.get(j);
				// Skip rests and notes that are the continuation of a tied note
				if (ne.note == Note.REST || ne.tiesFrom != null)
					continue;

				// Add note off events for any notes that have been turned off by this point
				Iterator<NoteEvent> onIter = notesOn.iterator();
				while (onIter.hasNext())
				{
					NoteEvent on = onIter.next();

					// Shorten the note to end at the same time that the next one starts
					long endTick = on.getEndTick();
					if (on.note.id == ne.note.id && on.getEndTick() > ne.getStartTick())
						endTick = ne.getStartTick();

					if (endTick <= ne.getStartTick())
					{
						// This note has been turned off
						onIter.remove();
						track.add(MidiFactory.createNoteOffEvent(on.note.id + noteDelta, channel, endTick));
					}
				}

				long endTick = ne.getTieEnd().getEndTick();

				// Lengthen Lute, Harp, Drums, etc. to play the entire sound sample
				if (useLotroInstruments && !part.getInstrument().isSustainable(ne.note.id))
				{
					endTick = qtm.microsToTick(qtm.tickToMicros(ne.getStartTick()) + TimingInfo.ONE_SECOND_MICROS);
				}

				if (endTick != ne.getEndTick())
					ne = new NoteEvent(ne.note, ne.velocity, ne.getStartTick(), endTick, qtm);

				track.add(MidiFactory.createNoteOnEventEx(ne.note.id + noteDelta, channel,
						dynamics.getVol(useLotroInstruments), ne.getStartTick()));
				notesOn.add(ne);
			}
		}

		for (NoteEvent on : notesOn)
		{
			track.add(MidiFactory.createNoteOffEvent(on.note.id + noteDelta, channel, on.getEndTick()));
		}

		return trackNumber;
	}

	private void exportPartToAbc(AbcPart part, long songStartTick, long songEndTick, PrintStream out)
			throws AbcConversionException
	{
		List<Chord> chords = combineAndQuantize(part, true, songStartTick, songEndTick);

		out.println();
		out.println("X: " + part.getPartNumber());
		if (metadata != null)
			out.println("T: " + metadata.getPartName(part));
		else
			out.println("T: " + part.getTitle().trim());

		out.println(AbcField.PART_NAME + part.getTitle().trim());

		if (metadata != null)
		{
			if (metadata.getComposer().length() > 0)
				out.println("C: " + metadata.getComposer());

			if (metadata.getTranscriber().length() > 0)
				out.println("Z: " + metadata.getTranscriber());
		}

		out.println("M: " + qtm.getMeter());
		out.println("Q: " + qtm.getPrimaryExportTempoBPM());
		out.println("K: " + keySignature);
		out.println();

		// Keep track of which notes have been sharped or flatted so 
		// we can naturalize them the next time they show up.
		boolean[] sharps = new boolean[Note.MAX_PLAYABLE.id + 1];
		boolean[] flats = new boolean[Note.MAX_PLAYABLE.id + 1];

		// Write out ABC notation
		final int BAR_LENGTH = 160;
		final long songStartMicros = qtm.tickToMicros(songStartTick);
		final int firstBarNumber = qtm.tickToBarNumber(songStartTick);
		final int primaryTempoBPM = qtm.getPrimaryTempoBPM();
		int curBarNumber = firstBarNumber;
		int curTempoBPM = primaryTempoBPM;
		Dynamics curDyn = null;
		Dynamics initDyn = null;

		final StringBuilder bar = new StringBuilder();

		Runnable addLineBreaks = new Runnable()
		{
			@Override public void run()
			{
				// Trim end
				int length = bar.length();
				if (length == 0)
					return;

				while (Character.isWhitespace(bar.charAt(length - 1)))
					length--;
				bar.setLength(length);

				// Insert line breaks inside very long bars
				for (int i = BAR_LENGTH; i < bar.length(); i += BAR_LENGTH)
				{
					for (int j = 0; j < BAR_LENGTH - 1; j++, i--)
					{
						if (bar.charAt(i) == ' ')
						{
							bar.replace(i, i + 1, "\r\n\t");
							i += "\r\n\t".length() - 1;
							break;
						}
					}
				}
			}
		};

		for (Chord c : chords)
		{
			initDyn = c.calcDynamics();
			if (initDyn != null)
				break;
		}

		for (Chord c : chords)
		{
			if (c.size() == 0)
			{
				System.err.println("Chord has no notes!");
				continue;
			}

			c.sort();

			// Is this the start of a new bar?
			int barNumber = qtm.tickToBarNumber(c.getStartTick());
			assert curBarNumber <= barNumber;
			if (curBarNumber < barNumber)
			{
				// Print the previous bar
				if (bar.length() > 0)
				{
					addLineBreaks.run();
					out.print(bar);
					out.println(" |");
					bar.setLength(0);
				}

				curBarNumber = barNumber;

				int exportBarNumber = curBarNumber - firstBarNumber;
				if ((exportBarNumber + 1) % 10 == 0)
				{
					long micros = qtm.barNumberToMicrosecond(curBarNumber) - songStartMicros;
					out.println("%");
					out.println("% Bar " + (exportBarNumber + 1) + " (" + Util.formatDuration(micros) + ")");
					out.println("%");
				}

				Arrays.fill(sharps, false);
				Arrays.fill(flats, false);
			}

			// Is this the start of a new tempo?
			TimingInfo tm = qtm.getTimingInfo(c.getStartTick());
			if (curTempoBPM != tm.getTempoBPM())
			{
				curTempoBPM = tm.getTempoBPM();

//				// TODO Print the current bar?
//				if (bar.length() > 0)
//				{
//					addLineBreaks.run();
//					out.println(bar);
//					bar.setLength(0);
//					bar.append("\t");
//				}
//
//				out.println(AbcField.TEMPO + Integer.toString(curTempoBPM));
			}

			Dynamics newDyn = (initDyn != null) ? initDyn : c.calcDynamics();
			initDyn = null;
			if (newDyn != null && newDyn != curDyn)
			{
				bar.append('+').append(newDyn).append("+ ");
				curDyn = newDyn;
			}

			if (c.size() > 1)
			{
				bar.append('[');
			}

			int notesWritten = 0;
			for (int j = 0; j < c.size(); j++)
			{
				NoteEvent evt = c.get(j);
				if (evt.getLengthTicks() == 0)
				{
					System.err.println("Zero-length note");
					continue;
				}

				String noteAbc = evt.note.abc;
				if (evt.note != Note.REST)
				{
					if (evt.note.isSharp())
					{
						if (sharps[evt.note.naturalId])
							noteAbc = Note.fromId(evt.note.naturalId).abc;
						else
							sharps[evt.note.naturalId] = true;
					}
					else if (evt.note.isFlat())
					{
						if (flats[evt.note.naturalId])
							noteAbc = Note.fromId(evt.note.naturalId).abc;
						else
							flats[evt.note.naturalId] = true;
					}
					else if (sharps[evt.note.id] || flats[evt.note.id])
					{
						sharps[evt.note.id] = false;
						flats[evt.note.id] = false;
						bar.append('=');
					}
				}

				bar.append(noteAbc);

				int numerator = (int) (evt.getLengthTicks() / tm.getMinNoteLengthTicks()) * tm.getDefaultDivisor();
				int denominator = tm.getMinNoteDivisor();

				// Apply tempo
				if (curTempoBPM != primaryTempoBPM)
				{
					numerator *= primaryTempoBPM;
					denominator *= curTempoBPM;
				}

				// Reduce the fraction
				int gcd = Util.gcd(numerator, denominator);
				numerator /= gcd;
				denominator /= gcd;

				if (numerator != 1)
				{
					bar.append(numerator);
				}
				if (denominator != 1)
				{
					bar.append('/');
					if (numerator != 1 || denominator != 2)
						bar.append(denominator);
				}

				if (evt.tiesTo != null)
					bar.append('-');

				notesWritten++;
			}

			if (c.size() > 1)
			{
				if (notesWritten == 0)
				{
					// Remove the [
					bar.delete(bar.length() - 1, bar.length());
				}
				else
				{
					bar.append(']');
				}
			}

			bar.append(' ');
		}

		addLineBreaks.run();
		out.print(bar);
		out.println(" |]");
		out.println();
	}

	/**
	 * Combine the tracks into one, quantize the note lengths, separate into chords.
	 */
	private List<Chord> combineAndQuantize(AbcPart part, boolean addTies, final long songStartTick,
			final long songEndTick) throws AbcConversionException
	{
		// Combine the events from the enabled tracks
		List<NoteEvent> events = new ArrayList<NoteEvent>();
		for (int t = 0; t < part.getTrackCount(); t++)
		{
			if (part.isTrackEnabled(t))
			{
				for (NoteEvent ne : part.getTrackEvents(t))
				{
					// Skip notes that are outside of the play range.
					if (ne.getEndTick() <= songStartTick || ne.getStartTick() >= songEndTick)
						continue;

					Note mappedNote = part.mapNote(t, ne.note.id);
					if (mappedNote != null)
					{
						assert mappedNote.id >= part.getInstrument().lowestPlayable.id : mappedNote;
						assert mappedNote.id <= part.getInstrument().highestPlayable.id : mappedNote;
						long startTick = Math.max(ne.getStartTick(), songStartTick);
						long endTick = Math.min(ne.getEndTick(), songEndTick);
						int velocity = ne.velocity + part.getTrackVolumeAdjust(t);
						events.add(new NoteEvent(mappedNote, velocity, startTick, endTick, qtm));
					}
				}
			}
		}

		if (events.isEmpty())
			return Collections.emptyList();

		Collections.sort(events);

		// Quantize the events
		Iterator<NoteEvent> neIter = events.iterator();
		while (neIter.hasNext())
		{
			NoteEvent ne = neIter.next();

			ne.setStartTick(qtm.quantize(ne.getStartTick()));
			ne.setEndTick(qtm.quantize(ne.getEndTick()));

			// Make sure the note didn't get quantized to zero length
			if (ne.getLengthTicks() == 0)
			{
				if (ne.note == Note.REST)
					neIter.remove();
				else
					ne.setLengthTicks(qtm.getTimingInfo(ne.getStartTick()).getMinNoteLengthTicks());
			}
		}

		// Add initial rest if necessary
		long quantizedStartTick = qtm.quantize(songStartTick);
		if (events.get(0).getStartTick() > quantizedStartTick)
		{
			events.add(0, new NoteEvent(Note.REST, Dynamics.DEFAULT.midiVol, quantizedStartTick, events.get(0)
					.getStartTick(), qtm));
		}

		// Add a rest at the end if necessary
		if (songEndTick < Long.MAX_VALUE)
		{
			long quantizedEndTick = qtm.quantize(songEndTick);
			NoteEvent lastEvent = events.get(events.size() - 1);
			if (lastEvent.getEndTick() < quantizedEndTick)
			{
				if (lastEvent.note == Note.REST)
				{
					lastEvent.setEndTick(quantizedEndTick);
				}
				else
				{
					events.add(new NoteEvent(Note.REST, Dynamics.DEFAULT.midiVol, lastEvent.getEndTick(),
							quantizedEndTick, qtm));
				}
			}
		}

		// Remove duplicate notes
		List<NoteEvent> notesOn = new ArrayList<NoteEvent>();
		neIter = events.iterator();
		dupLoop: while (neIter.hasNext())
		{
			NoteEvent ne = neIter.next();
			Iterator<NoteEvent> onIter = notesOn.iterator();
			while (onIter.hasNext())
			{
				NoteEvent on = onIter.next();
				if (on.getEndTick() < ne.getStartTick())
				{
					// This note has already been turned off
					onIter.remove();
				}
				else if (on.note.id == ne.note.id)
				{
					if (on.getStartTick() == ne.getStartTick())
					{
						// If they start at the same time, remove the second event.
						// Lengthen the first one if it's shorter than the second one.
						if (on.getEndTick() < ne.getEndTick())
							on.setEndTick(ne.getEndTick());

						// Remove the duplicate note
						neIter.remove();
						continue dupLoop;
					}
					else
					{
						// Otherwise, if they don't start at the same time:
						// 1. Lengthen the second note if necessary, so it doesn't end before 
						//    the first note would have ended.
						if (ne.getEndTick() < on.getEndTick())
							ne.setEndTick(on.getEndTick());

						// 2. Shorten the note that's currently on to end at the same time that 
						//    the next one starts.
						on.setEndTick(ne.getStartTick());
						onIter.remove();
					}
				}
			}
			notesOn.add(ne);
		}

		breakLongNotes(part, events, addTies);

		List<Chord> chords = new ArrayList<Chord>(events.size() / 2);
		List<NoteEvent> tmpEvents = new ArrayList<NoteEvent>();

		// Combine notes that play at the same time into chords
		Chord curChord = new Chord(events.get(0));
		chords.add(curChord);
		for (int i = 1; i < events.size(); i++)
		{
			NoteEvent ne = events.get(i);
			if (curChord.getStartTick() == ne.getStartTick())
			{
				// This note starts at the same time as the rest of the notes in the chord
				if (!curChord.add(ne))
				{
					// Couldn't add the note (too many notes in the chord)
					removeNote(events, i);
					i--;
				}
			}
			else
			{
				// Create a new chord
				Chord nextChord = new Chord(ne);

				if (addTies)
				{
					// The curChord has all the notes it will get. But before continuing, 
					// normalize the chord so that all notes end at the same time and end 
					// before the next chord starts.
					boolean reprocessCurrentNote = false;
					long targetEndTick = Math.min(nextChord.getStartTick(), curChord.getEndTick());

					for (int j = 0; j < curChord.size(); j++)
					{
						NoteEvent jne = curChord.get(j);
						if (jne.getEndTick() > targetEndTick)
						{
							// This note extends past the end of the chord; break it into two tied notes
							NoteEvent next = jne.splitWithTieAtTick(targetEndTick);

							int ins = Collections.binarySearch(events, next);
							if (ins < 0)
								ins = -ins - 1;
							assert (ins >= i);
							// If we're inserting before the current note, back up and process the added note
							if (ins == i)
								reprocessCurrentNote = true;

							events.add(ins, next);
						}
					}

					// The shorter notes will have changed the chord's duration
					if (targetEndTick < curChord.getEndTick())
						curChord.recalcEndTick();

					if (reprocessCurrentNote)
					{
						i--;
						continue;
					}
				}
				else
				{
					// If we're not allowed to add ties, use the old method of shortening the 
					// chord by inserting a short rest. 

					// The next chord starts playing immediately after the *shortest* note (or rest) in
					// the current chord is finished, so we may need to add a rest inside the chord to
					// shorten it, or a rest after the chord to add a pause.

					if (curChord.getEndTick() > nextChord.getStartTick())
					{
						// Make sure there's room to add the rest
						while (curChord.size() >= Chord.MAX_CHORD_NOTES)
						{
							removeNote(events, curChord.remove(curChord.size() - 1));
						}
					}

					// Check the chord length again, since removing a note might have changed its length
					if (curChord.getEndTick() > nextChord.getStartTick())
					{
						// If the chord is too long, add a short rest in the chord to shorten it
						curChord.add(new NoteEvent(Note.REST, Dynamics.DEFAULT.midiVol, curChord.getStartTick(),
								nextChord.getStartTick(), qtm));
					}
				}

				// Insert a rest between the chords if needed
				if (curChord.getEndTick() < nextChord.getStartTick())
				{
					tmpEvents.clear();
					tmpEvents.add(new NoteEvent(Note.REST, Dynamics.DEFAULT.midiVol, curChord.getEndTick(), nextChord
							.getStartTick(), qtm));
					breakLongNotes(part, tmpEvents, addTies);

					for (NoteEvent restEvent : tmpEvents)
						chords.add(new Chord(restEvent));
				}

				chords.add(nextChord);
				curChord = nextChord;
			}
		}

		return chords;
	}

	private void breakLongNotes(AbcPart part, List<NoteEvent> events, boolean addTies)
	{
		for (int i = 0; i < events.size(); i++)
		{
			NoteEvent ne = events.get(i);
			TimingInfo tmStart = qtm.getTimingInfo(ne.getStartTick());
			long maxNoteEndTick = ne.getStartTick() + tmStart.getMaxNoteLengthTicks();

			// Make a hard break for notes that are longer than LotRO can play
			// Bagpipe notes up to B2 can sustain indefinitey; don't break them
			if (ne.getEndTick() > maxNoteEndTick
					&& !(part.getInstrument() == LotroInstrument.BAGPIPE && ne.note.id <= Note.B2.id))
			{
				// Align with a bar boundary if it extends across 1 or more full bars.
				long endBarTick = qtm.tickToBarStartTick(maxNoteEndTick);
				if (qtm.tickToBarEndTick(ne.getStartTick()) < endBarTick)
				{
					maxNoteEndTick = endBarTick;
					assert ne.getEndTick() > maxNoteEndTick;
				}

				// If the note is a rest or sustainable, add another one after 
				// this ends to keep it going...
				if (ne.note == Note.REST || part.getInstrument().isSustainable(ne.note.id))
				{
					NoteEvent next = new NoteEvent(ne.note, ne.velocity, maxNoteEndTick, ne.getEndTick(), qtm);
					int ins = Collections.binarySearch(events, next);
					if (ins < 0)
						ins = -ins - 1;
					assert (ins > i);
					events.add(ins, next);

					/* If the final note is less than a full bar length, just tie it to the original
					 * note rather than creating a hard break. We don't want the last piece of a
					 * long sustained note to be a short blast. LOTRO won't complain about a note
					 * being too long if it's part of a tie. */
					TimingInfo tmNext = qtm.getTimingInfo(next.getStartTick());
					if (next.getLengthTicks() < tmNext.getBarLengthTicks() && ne.note != Note.REST)
					{
						next.tiesFrom = ne;
						ne.tiesTo = next;
					}
				}

				ne.setEndTick(maxNoteEndTick);
			}

			if (addTies)
			{
				// Tie notes across bar boundaries
				long boundaryTick = qtm.tickToBarEndTick(ne.getStartTick());

				// Tie notes across tempo boundaries
				QuantizedTimingInfo.TimingInfoEvent nextTempoEvent = qtm.getNextTimingEvent(ne.getStartTick());
				if (nextTempoEvent != null && nextTempoEvent.tick < boundaryTick)
					boundaryTick = nextTempoEvent.tick;

				if (ne.getEndTick() > boundaryTick)
				{
					NoteEvent next = ne.splitWithTieAtTick(boundaryTick);
					int ins = Collections.binarySearch(events, next);
					if (ins < 0)
						ins = -ins - 1;
					assert (ins > i);
					events.add(ins, next);
				}
			}
		}
	}

	/** Removes a note and breaks any ties the note has. */
	private void removeNote(List<NoteEvent> events, int i)
	{
		NoteEvent ne = events.remove(i);

		// If the note is tied from another (previous) note, break the incoming tie
		if (ne.tiesFrom != null)
		{
			ne.tiesFrom.tiesTo = null;
			ne.tiesFrom = null;
		}

		// Remove the remainder of the notes that this is tied to (if any)
		for (NoteEvent neTie = ne.tiesTo; neTie != null; neTie = neTie.tiesTo)
		{
			events.remove(neTie);
		}
	}

	/** Removes a note and breaks any ties the note has. */
	private void removeNote(List<NoteEvent> events, NoteEvent ne)
	{
		removeNote(events, events.indexOf(ne));
	}

	public Pair<Long, Long> getSongStartEndTick(boolean lengthenToBar, boolean accountForSustain)
	{
		// Remove silent bars before the song starts
		long startTick = Long.MAX_VALUE;
		long endTick = Long.MIN_VALUE;
		for (AbcPart part : parts)
		{
			long firstNoteStart = part.firstNoteStartTick();
			if (firstNoteStart < startTick)
			{
				// Remove integral number of bars
				startTick = qtm.tickToBarStartTick(firstNoteStart);
			}
			long lastNoteEnd = part.lastNoteEndTick(accountForSustain);
			if (lastNoteEnd > endTick)
			{
				// Lengthen to an integral number of bars
				if (lengthenToBar)
					endTick = qtm.tickToBarEndTick(lastNoteEnd);
				else
					endTick = lastNoteEnd;
			}
		}

		if (startTick == Long.MAX_VALUE)
			startTick = 0;
		if (endTick == Long.MIN_VALUE)
			endTick = 0;

		return new Pair<Long, Long>(startTick, endTick);
	}
}
