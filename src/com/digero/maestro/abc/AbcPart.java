package com.digero.maestro.abc;

import java.util.BitSet;
import java.util.List;
import java.util.ListIterator;
import java.util.SortedSet;
import java.util.prefs.Preferences;
import java.util.regex.MatchResult;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.digero.common.abc.LotroInstrument;
import com.digero.common.midi.ITempoCache;
import com.digero.common.midi.MidiConstants;
import com.digero.common.midi.MidiDrum;
import com.digero.common.midi.Note;
import com.digero.common.util.IDiscardable;
import com.digero.common.util.Listener;
import com.digero.common.util.ListenerList;
import com.digero.common.util.Pair;
import com.digero.common.util.ParseException;
import com.digero.common.util.Version;
import com.digero.maestro.abc.AbcPartEvent.AbcPartProperty;
import com.digero.maestro.abc.AbcSongEvent.AbcSongProperty;
import com.digero.maestro.midi.NoteEvent;
import com.digero.maestro.midi.SequenceInfo;
import com.digero.maestro.midi.TrackInfo;
import com.digero.maestro.util.SaveUtil;
import com.digero.maestro.util.XmlUtil;

public class AbcPart implements AbcPartMetadataSource, NumberedAbcPart, IDiscardable
{
	private int partNumber = 1;
	private String title;
	private LotroInstrument instrument;
	private int[] trackTranspose;
	private boolean[] trackEnabled;
	private int[] trackVolumeAdjust;
	private DrumNoteMap[] drumNoteMap;
	private BitSet[] drumsEnabled;
	private BitSet[] cowbellsEnabled;

	private final AbcSong abcSong;
	private int enabledTrackCount = 0;
	private int previewSequenceTrackNumber = -1;
	private final ListenerList<AbcPartEvent> listeners = new ListenerList<AbcPartEvent>();
	private Preferences drumPrefs = Preferences.userNodeForPackage(AbcPart.class).node("drums");

	public AbcPart(AbcSong abcSong)
	{
		this.abcSong = abcSong;
		abcSong.addSongListener(songListener);
		this.instrument = LotroInstrument.DEFAULT_INSTRUMENT;
		this.title = this.instrument.toString();

		int t = getTrackCount();
		this.trackTranspose = new int[t];
		this.trackEnabled = new boolean[t];
		this.trackVolumeAdjust = new int[t];
		this.drumNoteMap = new DrumNoteMap[t];
	}

	public AbcPart(AbcSong abcSong, Element loadFrom)
	{
		this(abcSong);
	}

	@Override public void discard()
	{
		abcSong.removeSongListener(songListener);
		listeners.discard();
		for (int i = 0; i < drumNoteMap.length; i++)
		{
			if (drumNoteMap[i] != null)
			{
				drumNoteMap[i].removeChangeListener(drumMapChangeListener);
				drumNoteMap[i] = null;
			}
		}
	}

	public void saveToXml(Element ele)
	{
		Document doc = ele.getOwnerDocument();

		ele.setAttribute("id", String.valueOf(partNumber));
		SaveUtil.appendChildTextElement(ele, "title", String.valueOf(title));
		SaveUtil.appendChildTextElement(ele, "instrument", String.valueOf(instrument));
		for (int t = 0; t < getTrackCount(); t++)
		{
			if (!isTrackEnabled(t))
				continue;

			TrackInfo trackInfo = abcSong.getSequenceInfo().getTrackInfo(t);

			Element trackEle = (Element) ele.appendChild(doc.createElement("track"));
			trackEle.setAttribute("id", String.valueOf(t));
			if (trackInfo.hasName())
				trackEle.setAttribute("name", trackInfo.getName());

			if (trackTranspose[t] != 0)
				SaveUtil.appendChildTextElement(trackEle, "transpose", String.valueOf(trackTranspose[t]));
			if (trackVolumeAdjust[t] != 0)
				SaveUtil.appendChildTextElement(trackEle, "volumeAdjust", String.valueOf(trackVolumeAdjust[t]));

			if (instrument.isPercussion)
			{
				BitSet[] enabledSetByTrack = isCowbellPart() ? cowbellsEnabled : drumsEnabled;
				BitSet enabledSet = (enabledSetByTrack == null) ? null : enabledSetByTrack[t];
				if (enabledSet != null)
				{
					Element drumsEnabledEle = ele.getOwnerDocument().createElement("drumsEnabled");
					trackEle.appendChild(drumsEnabledEle);

					if (isCowbellPart())
					{
						drumsEnabledEle.setAttribute("defaultEnabled", String.valueOf(false));

						// Only store the drums that are enabled
						for (int i = enabledSet.nextSetBit(0); i >= 0; i = enabledSet.nextSetBit(i + 1))
						{
							Element drumEle = ele.getOwnerDocument().createElement("note");
							drumsEnabledEle.appendChild(drumEle);
							drumEle.setAttribute("id", String.valueOf(i));
							drumEle.setAttribute("isEnabled", String.valueOf(true));
						}
					}
					else
					{
						drumsEnabledEle.setAttribute("defaultEnabled", String.valueOf(true));

						// Only store the drums that are disabled
						for (int i = enabledSet.nextClearBit(0); i >= 0; i = enabledSet.nextClearBit(i + 1))
						{
							if (i >= MidiConstants.NOTE_COUNT)
								break;

							Element drumEle = ele.getOwnerDocument().createElement("note");
							drumsEnabledEle.appendChild(drumEle);
							drumEle.setAttribute("id", String.valueOf(i));
							drumEle.setAttribute("isEnabled", String.valueOf(false));
						}
					}
				}

				if (!isCowbellPart())
				{
					if (drumNoteMap[t] != null)
						drumNoteMap[t].saveToXml((Element) trackEle.appendChild(doc.createElement("drumMap")));
				}
			}
		}
	}

	public static AbcPart loadFromXml(AbcSong abcSong, Element ele, Version fileVersion) throws ParseException
	{
		AbcPart part = new AbcPart(abcSong);
		part.initFromXml(ele, fileVersion);
		return part;
	}

	private void initFromXml(Element ele, Version fileVersion) throws ParseException
	{
		try
		{
			partNumber = SaveUtil.parseValue(ele, "@id", partNumber);
			title = SaveUtil.parseValue(ele, "title", title);
			instrument = SaveUtil.parseValue(ele, "instrument", instrument);
			for (Element trackEle : XmlUtil.selectElements(ele, "track"))
			{
				// Try to find the specified track in the midi sequence by name, in case it moved
				int t = findTrackNumberByName(SaveUtil.parseValue(trackEle, "@name", ""));
				// Fall back to the track ID if that didn't work
				if (t == -1)
					t = SaveUtil.parseValue(trackEle, "@id", -1);

				if (t < 0 || t >= getTrackCount())
				{
					throw SaveUtil.invalidValueException(trackEle, "Could not find track number " + t
							+ " in original MIDI file");
				}

				// Now set the track info
				trackEnabled[t] = true;
				enabledTrackCount++;
				trackTranspose[t] = SaveUtil.parseValue(trackEle, "transpose", trackTranspose[t]);
				trackVolumeAdjust[t] = SaveUtil.parseValue(trackEle, "volumeAdjust", trackVolumeAdjust[t]);

				if (instrument.isPercussion)
				{
					Element drumsEle = XmlUtil.selectSingleElement(trackEle, "drumsEnabled");
					if (drumsEle != null)
					{
						boolean defaultEnabled = SaveUtil.parseValue(drumsEle, "@defaultEnabled", !isCowbellPart());

						BitSet[] enabledSet;
						if (isCowbellPart())
						{
							if (cowbellsEnabled == null)
								cowbellsEnabled = new BitSet[getTrackCount()];
							enabledSet = cowbellsEnabled;
						}
						else
						{
							if (drumsEnabled == null)
								drumsEnabled = new BitSet[getTrackCount()];
							enabledSet = drumsEnabled;
						}

						enabledSet[t] = new BitSet(MidiConstants.NOTE_COUNT);
						if (defaultEnabled)
							enabledSet[t].set(0, MidiConstants.NOTE_COUNT, true);

						for (Element drumEle : XmlUtil.selectElements(drumsEle, "note"))
						{
							int id = SaveUtil.parseValue(drumEle, "@id", -1);
							if (id >= 0 && id < MidiConstants.NOTE_COUNT)
								enabledSet[t].set(id, SaveUtil.parseValue(drumEle, "@isEnabled", !defaultEnabled));
						}
					}

					Element drumMapEle = XmlUtil.selectSingleElement(trackEle, "drumMap");
					if (drumMapEle != null)
						drumNoteMap[t] = DrumNoteMap.loadFromXml(drumMapEle, fileVersion);
				}
			}
		}
		catch (XPathExpressionException e)
		{
			throw new ParseException("XPath error: " + e.getMessage(), null);
		}
	}

	private int findTrackNumberByName(String trackName)
	{
		if (trackName.equals(""))
			return -1;

		int namedTrackNumber = -1;
		for (TrackInfo trackInfo : abcSong.getSequenceInfo().getTrackList())
		{
			if (trackInfo.hasName() && trackName.equals(trackInfo.getName()))
			{
				if (namedTrackNumber == -1)
				{
					namedTrackNumber = trackInfo.getTrackNumber();
				}
				else
				{
					// Found multiple tracks with the same name; don't know which one it could be
					return -1;
				}
			}
		}
		return namedTrackNumber;
	}

	private Listener<AbcSongEvent> songListener = new Listener<AbcSongEvent>()
	{
		@Override public void onEvent(AbcSongEvent e)
		{
			if (e.getProperty() == AbcSongProperty.TRANSPOSE)
			{
				fireChangeEvent(AbcPartProperty.BASE_TRANSPOSE, !isDrumPart() /* affectsAbcPreview */);
			}
		}
	};

	public List<NoteEvent> getTrackEvents(int track)
	{
		return abcSong.getSequenceInfo().getTrackInfo(track).getEvents();
	}

	/**
	 * Maps from a MIDI note to an ABC note. If no mapping is available, returns <code>null</code>.
	 */
	public Note mapNote(int track, int noteId)
	{
		if (isDrumPart())
		{
			if (!isTrackEnabled(track) || !isDrumEnabled(track, noteId))
				return null;

			int dstNote;
			if (instrument == LotroInstrument.BASIC_COWBELL)
				dstNote = Note.G2.id; // "Tom High 1"
			else if (instrument == LotroInstrument.MOOR_COWBELL)
				dstNote = Note.A2.id; // "Tom High 2"
			else
				dstNote = getDrumMap(track).get(noteId);

			return (dstNote == LotroDrumInfo.DISABLED.note.id) ? null : Note.fromId(dstNote);
		}
		else
		{
			noteId += getTranspose(track);
			while (noteId < instrument.lowestPlayable.id)
				noteId += 12;
			while (noteId > instrument.highestPlayable.id)
				noteId -= 12;
			return Note.fromId(noteId);
		}
	}

	public long firstNoteStartTick()
	{
		long startTick = Long.MAX_VALUE;

		for (int t = 0; t < getTrackCount(); t++)
		{
			if (isTrackEnabled(t))
			{
				for (NoteEvent ne : getTrackEvents(t))
				{
					if (mapNote(t, ne.note.id) != null)
					{
						if (ne.getStartTick() < startTick)
							startTick = ne.getStartTick();
						break;
					}
				}
			}
		}

		if (startTick == Long.MAX_VALUE)
			startTick = 0;

		return startTick;
	}

	public long lastNoteEndTick(boolean accountForSustain)
	{
		long endTick = Long.MIN_VALUE;

		// The last note to start playing isn't necessarily the last note to end.
		// Check the last several notes to find the one that ends last.
		int notesToCheck = 1000;

		for (int t = 0; t < getTrackCount(); t++)
		{
			if (isTrackEnabled(t))
			{
				List<NoteEvent> evts = getTrackEvents(t);
				ListIterator<NoteEvent> iter = evts.listIterator(evts.size());
				while (iter.hasPrevious())
				{
					NoteEvent ne = iter.previous();
					if (mapNote(t, ne.note.id) != null)
					{
						long noteEndTick;
						if (!accountForSustain || instrument.isSustainable(ne.note.id))
							noteEndTick = ne.getEndTick();
						else
						{
							ITempoCache tc = ne.getTempoCache();
							noteEndTick = tc.microsToTick(tc.tickToMicros(ne.getStartTick())
									+ TimingInfo.ONE_SECOND_MICROS);
						}

						if (noteEndTick > endTick)
							endTick = noteEndTick;

						if (--notesToCheck <= 0)
							break;
					}
				}
			}
		}

		return endTick;
	}

	public AbcSong getAbcSong()
	{
		return abcSong;
	}

	public SequenceInfo getSequenceInfo()
	{
		return abcSong.getSequenceInfo();
	}

	public int getTrackCount()
	{
		return abcSong.getSequenceInfo().getTrackCount();
	}

	@Override public String getTitle()
	{
		return title;
	}

	@Override public String toString()
	{
		String val = getPartNumber() + ". " + getTitle();
		if (getEnabledTrackCount() == 0)
			val += "*";
		return val;
	}

	public void setTitle(String name)
	{
		if (name == null)
			throw new NullPointerException();

		if (!this.title.equals(name))
		{
			this.title = name;
			fireChangeEvent(AbcPartProperty.TITLE);
		}
	}

	public void replaceTitleInstrument(LotroInstrument replacement)
	{
		Pair<LotroInstrument, MatchResult> result = LotroInstrument.matchInstrument(title);
		if (result == null)
		{
			// No instrument currently in title
			if (title.isEmpty())
				setTitle(replacement.toString());
			else
				setTitle(replacement + " " + title);
		}
		else
		{
			MatchResult m = result.second;
			setTitle(title.substring(0, m.start()) + replacement + title.substring(m.end()));
		}
	}

	@Override public LotroInstrument getInstrument()
	{
		return instrument;
	}

	@Override public void setInstrument(LotroInstrument instrument)
	{
		if (instrument == null)
			throw new NullPointerException();

		if (this.instrument != instrument)
		{
			this.instrument = instrument;
			boolean affectsPreview = false;
			for (boolean enabled : trackEnabled)
			{
				if (enabled)
				{
					affectsPreview = true;
					break;
				}
			}
			fireChangeEvent(AbcPartProperty.INSTRUMENT, affectsPreview);
		}
	}

	public int getTrackTranspose(int track)
	{
		return isDrumPart() ? 0 : trackTranspose[track];
	}

	public void setTrackTranspose(int track, int transpose)
	{
		if (trackTranspose[track] != transpose)
		{
			trackTranspose[track] = transpose;
			fireChangeEvent(AbcPartProperty.TRACK_TRANSPOSE, isTrackEnabled(track) /* previewRelated */, track);
		}
	}

	public int getTranspose(int track)
	{
		if (isDrumPart())
			return 0;
		return abcSong.getTranspose() + trackTranspose[track] - getInstrument().octaveDelta * 12;
	}

	public boolean isTrackEnabled(int track)
	{
		return trackEnabled[track];
	}

	public void setTrackEnabled(int track, boolean enabled)
	{
		if (trackEnabled[track] != enabled)
		{
			trackEnabled[track] = enabled;
			enabledTrackCount += enabled ? 1 : -1;
			fireChangeEvent(AbcPartProperty.TRACK_ENABLED, track);
		}
	}

	public int getTrackVolumeAdjust(int track)
	{
		return trackVolumeAdjust[track];
	}

	public void setTrackVolumeAdjust(int track, int volumeAdjust)
	{
		if (trackVolumeAdjust[track] != volumeAdjust)
		{
			trackVolumeAdjust[track] = volumeAdjust;
			fireChangeEvent(AbcPartProperty.VOLUME_ADJUST, track);
		}
	}

	public int getEnabledTrackCount()
	{
		return enabledTrackCount;
	}

	public void setPreviewSequenceTrackNumber(int previewSequenceTrackNumber)
	{
		this.previewSequenceTrackNumber = previewSequenceTrackNumber;
	}

	public int getPreviewSequenceTrackNumber()
	{
		return previewSequenceTrackNumber;
	}

	@Override public int getPartNumber()
	{
		return partNumber;
	}

	@Override public void setPartNumber(int partNumber)
	{
		if (this.partNumber != partNumber)
		{
			this.partNumber = partNumber;
			fireChangeEvent(AbcPartProperty.PART_NUMBER);
		}
	}

	public void addAbcListener(Listener<AbcPartEvent> l)
	{
		listeners.add(l);
	}

	public void removeAbcListener(Listener<AbcPartEvent> l)
	{
		listeners.remove(l);
	}

	protected void fireChangeEvent(AbcPartProperty property)
	{
		fireChangeEvent(property, property.isAbcPreviewRelated(), AbcPartEvent.NO_TRACK_NUMBER);
	}

	protected void fireChangeEvent(AbcPartProperty property, boolean abcPreviewRelated)
	{
		fireChangeEvent(property, abcPreviewRelated, AbcPartEvent.NO_TRACK_NUMBER);
	}

	protected void fireChangeEvent(AbcPartProperty property, int trackNumber)
	{
		fireChangeEvent(property, property.isAbcPreviewRelated(), trackNumber);
	}

	protected void fireChangeEvent(AbcPartProperty property, boolean abcPreviewRelated, int trackNumber)
	{
		if (listeners.size() == 0)
			return;

		listeners.fire(new AbcPartEvent(this, property, abcPreviewRelated, trackNumber));
	}

	//
	// DRUMS
	//

	public boolean isDrumPart()
	{
		return instrument.isPercussion;
	}

	public boolean isCowbellPart()
	{
		return instrument == LotroInstrument.BASIC_COWBELL || instrument == LotroInstrument.MOOR_COWBELL;
	}

	public boolean isDrumTrack(int track)
	{
		return abcSong.getSequenceInfo().getTrackInfo(track).isDrumTrack();
	}

	public DrumNoteMap getDrumMap(int track)
	{
		if (drumNoteMap[track] == null)
		{
			// For non-drum tracks, just use a straight pass-through
			if (!abcSong.getSequenceInfo().getTrackInfo(track).isDrumTrack())
			{
				drumNoteMap[track] = new PassThroughDrumNoteMap();
			}
			else
			{
				drumNoteMap[track] = new DrumNoteMap();
				drumNoteMap[track].load(drumPrefs);
			}
			drumNoteMap[track].addChangeListener(drumMapChangeListener);
		}
		return drumNoteMap[track];
	}

	private final ChangeListener drumMapChangeListener = new ChangeListener()
	{
		@Override public void stateChanged(ChangeEvent e)
		{
			if (e.getSource() instanceof DrumNoteMap)
			{
				DrumNoteMap map = (DrumNoteMap) e.getSource();

				// Don't write pass-through drum maps to the prefs node 
				// these are used for non-drum tracks and their mapping 
				// isn't desirable to save.
				if (!(map instanceof PassThroughDrumNoteMap))
					map.save(drumPrefs);

				fireChangeEvent(AbcPartProperty.DRUM_MAPPING);
			}
		}
	};

	Preferences getDrumPrefs()
	{
		return drumPrefs;
	}

	public boolean isDrumPlayable(int track, int drumId)
	{
		if (isCowbellPart())
			return true;

		return getDrumMap(track).get(drumId) != LotroDrumInfo.DISABLED.note.id;
	}

	public boolean isDrumEnabled(int track, int drumId)
	{
		BitSet[] enabledSet = isCowbellPart() ? cowbellsEnabled : drumsEnabled;

		if (enabledSet == null || enabledSet[track] == null)
		{
			return !isCowbellPart() || (drumId == MidiDrum.COWBELL.id())
					|| !abcSong.getSequenceInfo().getTrackInfo(track).isDrumTrack();
		}

		return enabledSet[track].get(drumId);
	}

	public void setDrumEnabled(int track, int drumId, boolean enabled)
	{
		if (isDrumEnabled(track, drumId) != enabled)
		{
			BitSet[] enabledSet;
			if (isCowbellPart())
			{
				if (cowbellsEnabled == null)
					cowbellsEnabled = new BitSet[getTrackCount()];
				enabledSet = cowbellsEnabled;
			}
			else
			{
				if (drumsEnabled == null)
					drumsEnabled = new BitSet[getTrackCount()];
				enabledSet = drumsEnabled;
			}

			if (enabledSet[track] == null)
			{
				enabledSet[track] = new BitSet(MidiConstants.NOTE_COUNT);
				if (isCowbellPart())
				{
					SortedSet<Integer> notesInUse = abcSong.getSequenceInfo().getTrackInfo(track).getNotesInUse();
					if (notesInUse.contains(MidiDrum.COWBELL.id()))
						enabledSet[track].set(MidiDrum.COWBELL.id(), true);
				}
				else
				{
					enabledSet[track].set(0, MidiConstants.NOTE_COUNT, true);
				}
			}
			enabledSet[track].set(drumId, enabled);
			fireChangeEvent(AbcPartProperty.DRUM_ENABLED);
		}
	}
}
