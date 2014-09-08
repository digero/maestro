package com.digero.maestro.abc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

import javax.sound.midi.InvalidMidiDataException;
import javax.swing.DefaultListModel;

import org.w3c.dom.Element;

import com.digero.common.abc.LotroInstrument;
import com.digero.common.abctomidi.AbcToMidi;
import com.digero.common.abctomidi.AbcToMidi.AbcInfo;
import com.digero.common.midi.KeySignature;
import com.digero.common.midi.TimeSignature;
import com.digero.common.util.ICompileConstants;
import com.digero.common.util.IDiscardable;
import com.digero.common.util.Pair;
import com.digero.common.util.ParseException;
import com.digero.maestro.abc.AbcPartEvent.AbcPartProperty;
import com.digero.maestro.abc.AbcSongEvent.AbcSongProperty;
import com.digero.maestro.midi.SequenceInfo;
import com.digero.maestro.midi.TrackInfo;
import com.digero.maestro.util.ListModelWrapper;
import com.digero.maestro.util.Listener;
import com.digero.maestro.util.ListenerList;

public class AbcSong implements IDiscardable, AbcMetadataSource
{
	private String title = "";
	private String composer = "";
	private String transcriber = "";
	private float tempoFactor = 1.0f;
	private int transpose = 0;
	private KeySignature keySignature = KeySignature.C_MAJOR;
	private TimeSignature timeSignature = TimeSignature.FOUR_FOUR;
	private boolean tripletTiming = false;

	private final boolean fromAbcFile;
	private final SequenceInfo sequenceInfo;
	private final PartAutoNumberer partAutoNumberer;
	private final PartNameTemplate partNameTemplate;
	private QuantizedTimingInfo timingInfo;
	private AbcExporter abcExporter;
	private File exportFile;

	private final ListModelWrapper<AbcPart> parts = new ListModelWrapper<AbcPart>(new DefaultListModel<AbcPart>());

	private final ListenerList<AbcSongEvent> listeners = new ListenerList<AbcSongEvent>();

	public AbcSong(File file, PartAutoNumberer partAutoNumberer, PartNameTemplate partNameTemplate) throws IOException,
			InvalidMidiDataException, ParseException
	{
		this.partAutoNumberer = partAutoNumberer;
		this.partAutoNumberer.setParts(Collections.unmodifiableList(parts));

		this.partNameTemplate = partNameTemplate;
		this.partNameTemplate.setMetadataSource(this);

		// TODO handle maestro project files
		String fileName = file.getName().toLowerCase();
		fromAbcFile = fileName.endsWith(".abc") || fileName.endsWith(".txt");

		AbcInfo abcInfo = new AbcInfo();

		if (fromAbcFile)
		{
			AbcToMidi.Params params = new AbcToMidi.Params(file);
			params.abcInfo = abcInfo;
			params.useLotroInstruments = false;
			sequenceInfo = SequenceInfo.fromAbc(params);
			exportFile = file;
		}
		else
		{
			sequenceInfo = SequenceInfo.fromMidi(file);
		}

		title = sequenceInfo.getTitle();
		composer = sequenceInfo.getComposer();
		transcriber = "";
		transpose = 0;
		tempoFactor = 1.0f;
		if (ICompileConstants.SHOW_KEY_FIELD)
			keySignature = sequenceInfo.getKeySignature();
		else
			keySignature = KeySignature.C_MAJOR;
		timeSignature = sequenceInfo.getTimeSignature();

		if (fromAbcFile)
		{
			int t = 0;
			for (TrackInfo trackInfo : sequenceInfo.getTrackList())
			{
				if (!trackInfo.hasEvents())
				{
					t++;
					continue;
				}

				AbcPart newPart = new AbcPart(sequenceInfo, getTranspose(), this);

				newPart.setTitle(abcInfo.getPartName(t));
				newPart.setPartNumber(abcInfo.getPartNumber(t));
				newPart.setTrackEnabled(t, true);

				Set<Integer> midiInstruments = trackInfo.getInstruments();
				for (LotroInstrument lotroInst : LotroInstrument.values())
				{
					if (midiInstruments.contains(lotroInst.midiProgramId))
					{
						newPart.setInstrument(lotroInst);
						break;
					}
				}

				int ins = Collections.binarySearch(parts, newPart, partNumberComparator);
				if (ins < 0)
					ins = -ins - 1;
				parts.add(ins, newPart);

				newPart.addAbcListener(abcPartListener);
				t++;
			}

			tripletTiming = abcInfo.hasTriplets();
			transcriber = abcInfo.getTranscriber();
		}
	}

	@Override public void discard()
	{
		if (partAutoNumberer != null)
			partAutoNumberer.setParts(null);

		if (partNameTemplate != null)
			partNameTemplate.setMetadataSource(null);

		listeners.discard();

		for (AbcPart part : parts)
		{
			if (part != null)
				part.discard();
		}
		parts.clear();
	}

	public void saveToXml(Element ele)
	{
		ele.setAttribute("title", title);
		ele.setAttribute("composer", composer);
		ele.setAttribute("transcriber", transcriber);
		ele.setAttribute("tempoFactor", String.valueOf(tempoFactor));
		ele.setAttribute("transpose", String.valueOf(transpose));
		if (ICompileConstants.SHOW_KEY_FIELD)
			ele.setAttribute("keySignature", String.valueOf(keySignature));
		ele.setAttribute("timeSignature", String.valueOf(timeSignature));
		ele.setAttribute("tripletTiming", String.valueOf(tripletTiming));
		if (exportFile != null)
			ele.setAttribute("exportFile", String.valueOf(exportFile));

		DrumNoteMap defaultDrumMap = null;

		for (AbcPart part : parts)
		{
			if (defaultDrumMap == null)
			{
				defaultDrumMap = new DrumNoteMap();
				defaultDrumMap.load(part.getDrumPrefs());
			}

			Element partEle = ele.getOwnerDocument().createElement("Part");
			ele.appendChild(partEle);
			part.saveToXml(partEle, defaultDrumMap);
		}

		if (defaultDrumMap != null)
		{
			Element drumMapEle = ele.getOwnerDocument().createElement("DefaultDrumMap");
			ele.appendChild(drumMapEle);
			defaultDrumMap.saveToXml(drumMapEle);
		}
	}

	public void exportAbc(File exportFile) throws FileNotFoundException, IOException, AbcConversionException
	{
		try (FileOutputStream out = new FileOutputStream(exportFile))
		{
			getAbcExporter().exportToAbc(out);
		}
	}

	public AbcPart createNewPart()
	{
		AbcPart newPart = new AbcPart(sequenceInfo, getTranspose(), this);
		newPart.addAbcListener(abcPartListener);
		partAutoNumberer.onPartAdded(newPart);

		int idx = Collections.binarySearch(parts, newPart, partNumberComparator);
		if (idx < 0)
			idx = (-idx - 1);
		parts.add(idx, newPart);

		fireChangeEvent(AbcSongProperty.PART_ADDED, newPart);
		return newPart;
	}

	public void deletePart(AbcPart part)
	{
		if (part == null || !parts.contains(part))
			return;

		fireChangeEvent(AbcSongProperty.BEFORE_PART_REMOVED, part);
		parts.remove(part);
		partAutoNumberer.onPartDeleted(part);
		part.discard();
	}

	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		if (!this.title.equals(title))
		{
			this.title = title;
			fireChangeEvent(AbcSongProperty.TITLE);
		}
	}

	@Override public String getComposer()
	{
		return composer;
	}

	public void setComposer(String composer)
	{
		if (!this.composer.equals(composer))
		{
			this.composer = composer;
			fireChangeEvent(AbcSongProperty.COMPOSER);
		}
	}

	@Override public String getTranscriber()
	{
		return transcriber;
	}

	public void setTranscriber(String transcriber)
	{
		if (!this.transcriber.equals(transcriber))
		{
			this.transcriber = transcriber;
			fireChangeEvent(AbcSongProperty.TRANSCRIBER);
		}
	}

	public float getTempoFactor()
	{
		return tempoFactor;
	}

	public void setTempoFactor(float tempoFactor)
	{
		if (this.tempoFactor != tempoFactor)
		{
			this.tempoFactor = tempoFactor;
			fireChangeEvent(AbcSongProperty.TEMPO_FACTOR);
		}
	}

	public int getTempoBPM()
	{
		return Math.round(tempoFactor * sequenceInfo.getPrimaryTempoBPM());
	}

	public void setTempoBPM(int tempoBPM)
	{
		setTempoFactor((float) tempoBPM / sequenceInfo.getPrimaryTempoBPM());
	}

	public int getTranspose()
	{
		return transpose;
	}

	public void setTranspose(int transpose)
	{
		if (this.transpose != transpose)
		{
			this.transpose = transpose;
			for (AbcPart part : parts)
			{
				part.setBaseTranspose(transpose);
			}
			fireChangeEvent(AbcSongProperty.TRANSPOSE);
		}
	}

	public KeySignature getKeySignature()
	{
		if (ICompileConstants.SHOW_KEY_FIELD)
			return keySignature;
		else
			return KeySignature.C_MAJOR;
	}

	public void setKeySignature(KeySignature keySignature)
	{
		if (!ICompileConstants.SHOW_KEY_FIELD)
			keySignature = KeySignature.C_MAJOR;

		if (!this.keySignature.equals(keySignature))
		{
			this.keySignature = keySignature;
			fireChangeEvent(AbcSongProperty.KEY_SIGNATURE);
		}
	}

	public TimeSignature getTimeSignature()
	{
		return timeSignature;
	}

	public void setTimeSignature(TimeSignature timeSignature)
	{
		if (!this.timeSignature.equals(timeSignature))
		{
			this.timeSignature = timeSignature;
			fireChangeEvent(AbcSongProperty.TIME_SIGNATURE);
		}
	}

	public boolean isTripletTiming()
	{
		return tripletTiming;
	}

	public void setTripletTiming(boolean tripletTiming)
	{
		if (this.tripletTiming != tripletTiming)
		{
			this.tripletTiming = tripletTiming;
			fireChangeEvent(AbcSongProperty.TRIPLET_TIMING);
		}
	}

	public SequenceInfo getSequenceInfo()
	{
		return sequenceInfo;
	}

	public boolean isFromAbcFile()
	{
		return fromAbcFile;
	}

	@Override public String getPartName(AbcPartMetadataSource abcPart)
	{
		return partNameTemplate.formatName(abcPart);
	}

	@Override public File getExportFile()
	{
		return exportFile;
	}

	public void setExportFile(File exportFile)
	{
		this.exportFile = exportFile;
	}

	@Override public String getSongTitle()
	{
		return getTitle();
	}

	@Override public long getSongLengthMicros()
	{
		if (parts.size() == 0 || sequenceInfo == null)
			return 0;

		try
		{
			AbcExporter exporter = getAbcExporter();
			Pair<Long, Long> startEndTick = exporter
					.getSongStartEndTick(false /* lengthenToBar */, true /* accountForSustain */);
			QuantizedTimingInfo qtm = exporter.getTimingInfo();

			return qtm.tickToMicros(startEndTick.second) - qtm.tickToMicros(startEndTick.first);
		}
		catch (AbcConversionException e)
		{
			return 0;
		}
	}

	public ListModelWrapper<AbcPart> getParts()
	{
		return parts;
	}

	public void addSongListener(Listener<AbcSongEvent> l)
	{
		listeners.add(l);
	}

	public void removeSongListener(Listener<AbcSongEvent> l)
	{
		listeners.remove(l);
	}

	private void fireChangeEvent(AbcSongProperty property)
	{
		fireChangeEvent(property, null);
	}

	private void fireChangeEvent(AbcSongProperty property, AbcPart part)
	{
		if (listeners.size() == 0)
			return;

		listeners.fire(new AbcSongEvent(this, property, part));
	}

	public QuantizedTimingInfo getAbcTimingInfo() throws AbcConversionException
	{
		if (timingInfo == null //
				|| timingInfo.getExportTempoFactor() != getTempoFactor() //
				|| timingInfo.getMeter() != getTimeSignature() //
				|| timingInfo.isTripletTiming() != isTripletTiming())
		{
			timingInfo = new QuantizedTimingInfo(sequenceInfo.getDataCache(), getTempoFactor(), getTimeSignature(),
					isTripletTiming());
		}

		return timingInfo;
	}

	public AbcExporter getAbcExporter() throws AbcConversionException
	{
		QuantizedTimingInfo qtm = getAbcTimingInfo();
		KeySignature key = getKeySignature();

		if (abcExporter == null)
		{
			abcExporter = new AbcExporter(parts, qtm, key, this);
		}
		else
		{
			if (abcExporter.getTimingInfo() != qtm)
				abcExporter.setTimingInfo(qtm);

			if (abcExporter.getKeySignature() != key)
				abcExporter.setKeySignature(key);
		}

		return abcExporter;
	}

	private Comparator<AbcPart> partNumberComparator = new Comparator<AbcPart>()
	{
		@Override public int compare(AbcPart p1, AbcPart p2)
		{
			int base1 = partAutoNumberer.getFirstNumber(p1.getInstrument());
			int base2 = partAutoNumberer.getFirstNumber(p2.getInstrument());

			if (base1 != base2)
				return base1 - base2;
			return p1.getPartNumber() - p2.getPartNumber();
		}
	};

	private Listener<AbcPartEvent> abcPartListener = new Listener<AbcPartEvent>()
	{
		@Override public void onEvent(AbcPartEvent e)
		{
			if (e.getProperty() == AbcPartProperty.PART_NUMBER)
			{
				Collections.sort(parts, partNumberComparator);
				fireChangeEvent(AbcSongProperty.PART_LIST_ORDER, e.getSource());
			}
		}
	};
}
