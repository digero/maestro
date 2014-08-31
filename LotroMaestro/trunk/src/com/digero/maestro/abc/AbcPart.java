package com.digero.maestro.abc;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.ListIterator;
import java.util.prefs.Preferences;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.digero.common.abc.LotroInstrument;
import com.digero.common.midi.MidiConstants;
import com.digero.common.midi.Note;
import com.digero.common.util.IDiscardable;
import com.digero.maestro.midi.ITempoCache;
import com.digero.maestro.midi.NoteEvent;
import com.digero.maestro.midi.SequenceInfo;

public class AbcPart implements AbcPartMetadataSource, NumberedAbcPart, IDiscardable
{
	private SequenceInfo sequenceInfo;
	private boolean enabled = true;
	private int partNumber = 1;
	private String title;
	private LotroInstrument instrument;
	private AbcProject ownerProject;
	private int baseTranspose;
	private int[] trackTranspose;
	private boolean[] trackEnabled;
	private int[] trackVolumeAdjust;
	private int enabledTrackCount = 0;
	private int previewSequenceTrackNumber = -1;
	private final List<AbcPartListener> changeListeners = new ArrayList<AbcPartListener>();

	public AbcPart(SequenceInfo sequenceInfo, int baseTranspose, AbcProject ownerProject)
	{
		this.sequenceInfo = sequenceInfo;
		this.baseTranspose = baseTranspose;
		this.ownerProject = ownerProject;
		this.instrument = LotroInstrument.LUTE;
		this.title = this.instrument.toString();

		int t = getTrackCount();
		this.trackTranspose = new int[t];
		this.trackEnabled = new boolean[t];
		this.trackVolumeAdjust = new int[t];
		this.drumNoteMap = new DrumNoteMap[t];
	}

	@Override public void discard()
	{
		changeListeners.clear();
		sequenceInfo = null;
		for (int i = 0; i < drumNoteMap.length; i++)
		{
			if (drumNoteMap[i] != null)
			{
				drumNoteMap[i].removeChangeListener(drumMapChangeListener);
				drumNoteMap[i] = null;
			}
		}
	}

	protected List<NoteEvent> getTrackEvents(int track)
	{
		return sequenceInfo.getTrackInfo(track).getEvents();
	}

	/**
	 * Maps from a MIDI note to an ABC note. If no mapping is available, returns
	 * <code>null</code>.
	 */
	public Note mapNote(int track, int noteId)
	{
		if (isDrumPart())
		{
			if (!isTrackEnabled(track) || !isDrumEnabled(track, noteId))
				return null;

			int dstNote;
			if (instrument == LotroInstrument.COWBELL)
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

	public AbcProject getOwnerProject()
	{
		return ownerProject;
	}

	public SequenceInfo getSequenceInfo()
	{
		return sequenceInfo;
	}

	public int getTrackCount()
	{
		return sequenceInfo.getTrackCount();
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

	public LotroInstrument[] getSupportedInstruments()
	{
		//return LotroInstrument.getNonDrumInstruments();
		return LotroInstrument.values();
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

	public boolean isEnabled()
	{
		return enabled;
	}

	public void setEnabled(boolean partEnabled)
	{
		if (this.enabled != partEnabled)
		{
			this.enabled = partEnabled;
			fireChangeEvent(AbcPartProperty.ENABLED);
		}
	}

	public int getBaseTranspose()
	{
		return isDrumPart() ? 0 : baseTranspose;
	}

	public void setBaseTranspose(int baseTranspose)
	{
		if (this.baseTranspose != baseTranspose)
		{
			this.baseTranspose = baseTranspose;
			fireChangeEvent(AbcPartProperty.BASE_TRANSPOSE, !isDrumPart() /* affectsAbcPreview */);
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
		return getBaseTranspose() + getTrackTranspose(track) - getInstrument().octaveDelta * 12;
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

	public void addAbcListener(AbcPartListener l)
	{
		changeListeners.add(l);
	}

	public void removeAbcListener(AbcPartListener l)
	{
		changeListeners.remove(l);
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
		if (changeListeners.size() > 0)
		{
			AbcPartEvent e = new AbcPartEvent(this, property, abcPreviewRelated, trackNumber);
			// Listener list might be modified in the callback
			List<AbcPartListener> listenerListCopy = new ArrayList<AbcPartListener>(changeListeners);
			for (AbcPartListener l : listenerListCopy)
			{
				l.abcPartChanged(e);
			}
		}
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
		return instrument == LotroInstrument.COWBELL || instrument == LotroInstrument.MOOR_COWBELL;
	}

	public boolean isDrumTrack(int track)
	{
		return sequenceInfo.getTrackInfo(track).isDrumTrack();
	}

	private DrumNoteMap[] drumNoteMap;
	private BitSet[] drumsEnabled;
	private BitSet[] cowbellsEnabled;
	private Preferences drumPrefs = Preferences.userNodeForPackage(AbcPart.class).node("drums");

	public DrumNoteMap getDrumMap(int track)
	{
		if (drumNoteMap[track] == null)
		{
			// For non-drum tracks, just use a straight pass-through
			if (!sequenceInfo.getTrackInfo(track).isDrumTrack())
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
			return !isCowbellPart() || (drumId == MidiConstants.COWBELL_DRUM_ID)
					|| !sequenceInfo.getTrackInfo(track).isDrumTrack();
		}

		return enabledSet[track].get(drumId);
	}

	public void setDrumEnabled(int track, int drumId, boolean enabled)
	{
		if (isDrumEnabled(track, drumId) != enabled)
		{
			if (drumsEnabled == null)
			{
				drumsEnabled = new BitSet[getTrackCount()];
			}
			if (cowbellsEnabled == null)
			{
				cowbellsEnabled = new BitSet[getTrackCount()];
			}

			BitSet[] enabledSet = isCowbellPart() ? cowbellsEnabled : drumsEnabled;
			if (enabledSet[track] == null)
			{
				enabledSet[track] = new BitSet(MidiConstants.NOTE_COUNT);
				if (isCowbellPart())
					enabledSet[track].set(MidiConstants.COWBELL_DRUM_ID, true);
				else
					enabledSet[track].set(0, MidiConstants.NOTE_COUNT, true);
			}
			enabledSet[track].set(drumId, enabled);
			fireChangeEvent(AbcPartProperty.DRUM_ENABLED);
		}
	}
}
