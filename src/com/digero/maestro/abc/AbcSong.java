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
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.digero.common.abc.LotroInstrument;
import com.digero.common.abctomidi.AbcInfo;
import com.digero.common.abctomidi.AbcToMidi;
import com.digero.common.midi.KeySignature;
import com.digero.common.midi.TimeSignature;
import com.digero.common.util.ICompileConstants;
import com.digero.common.util.IDiscardable;
import com.digero.common.util.Listener;
import com.digero.common.util.ListenerList;
import com.digero.common.util.Pair;
import com.digero.common.util.ParseException;
import com.digero.common.util.Util;
import com.digero.common.util.Version;
import com.digero.maestro.MaestroMain;
import com.digero.maestro.abc.AbcPartEvent.AbcPartProperty;
import com.digero.maestro.abc.AbcSongEvent.AbcSongProperty;
import com.digero.maestro.midi.SequenceInfo;
import com.digero.maestro.midi.TrackInfo;
import com.digero.maestro.util.FileResolver;
import com.digero.maestro.util.ListModelWrapper;
import com.digero.maestro.util.SaveUtil;
import com.digero.maestro.util.XmlUtil;

public class AbcSong implements IDiscardable, AbcMetadataSource
{
	public static final String MSX_FILE_DESCRIPTION = MaestroMain.APP_NAME + " Song";
	public static final String MSX_FILE_DESCRIPTION_PLURAL = MaestroMain.APP_NAME + " Songs";
	public static final String MSX_FILE_EXTENSION_NO_DOT = "msx";
	public static final String MSX_FILE_EXTENSION = "." + MSX_FILE_EXTENSION_NO_DOT;
	public static final Version SONG_FILE_VERSION = new Version(1, 0, 0);

	private String title = "";
	private String composer = "";
	private String transcriber = "";
	private float tempoFactor = 1.0f;
	private int transpose = 0;
	private KeySignature keySignature = KeySignature.C_MAJOR;
	private TimeSignature timeSignature = TimeSignature.FOUR_FOUR;
	private boolean tripletTiming = false;
	private boolean skipSilenceAtStart = true;

	private final boolean fromAbcFile;
	private final boolean fromXmlFile;
	private SequenceInfo sequenceInfo;
	private final PartAutoNumberer partAutoNumberer;
	private final PartNameTemplate partNameTemplate;
	private QuantizedTimingInfo timingInfo;
	private AbcExporter abcExporter;
	private File sourceFile; // The MIDI or ABC file that this song was loaded from
	private File exportFile; // The ABC export file
	private File saveFile; // The XML Maestro song file

	private final ListModelWrapper<AbcPart> parts = new ListModelWrapper<AbcPart>(new DefaultListModel<AbcPart>());

	private final ListenerList<AbcSongEvent> listeners = new ListenerList<AbcSongEvent>();

	public AbcSong(File file, PartAutoNumberer partAutoNumberer, PartNameTemplate partNameTemplate,
			FileResolver fileResolver) throws IOException, InvalidMidiDataException, ParseException, SAXException
	{
		sourceFile = file;

		this.partAutoNumberer = partAutoNumberer;
		this.partAutoNumberer.setParts(Collections.unmodifiableList(parts));

		this.partNameTemplate = partNameTemplate;
		this.partNameTemplate.setMetadataSource(this);

		String fileName = file.getName().toLowerCase();
		fromXmlFile = fileName.endsWith(MSX_FILE_EXTENSION);
		fromAbcFile = fileName.endsWith(".abc") || fileName.endsWith(".txt");

		if (fromXmlFile)
			initFromXml(file, fileResolver);
		else if (fromAbcFile)
			initFromAbc(file);
		else
			initFromMidi(file);
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

	private void initFromMidi(File file) throws IOException, InvalidMidiDataException, ParseException
	{
		sequenceInfo = SequenceInfo.fromMidi(file);
		title = sequenceInfo.getTitle();
		composer = sequenceInfo.getComposer();
		keySignature = (ICompileConstants.SHOW_KEY_FIELD) ? sequenceInfo.getKeySignature() : KeySignature.C_MAJOR;
		timeSignature = sequenceInfo.getTimeSignature();
	}

	private void initFromAbc(File file) throws IOException, InvalidMidiDataException, ParseException
	{
		AbcInfo abcInfo = new AbcInfo();

		AbcToMidi.Params params = new AbcToMidi.Params(file);
		params.abcInfo = abcInfo;
		params.useLotroInstruments = false;
		sequenceInfo = SequenceInfo.fromAbc(params);
		exportFile = file;

		title = sequenceInfo.getTitle();
		composer = sequenceInfo.getComposer();
		keySignature = (ICompileConstants.SHOW_KEY_FIELD) ? abcInfo.getKeySignature() : KeySignature.C_MAJOR;
		timeSignature = abcInfo.getTimeSignature();

		int t = 0;
		for (TrackInfo trackInfo : sequenceInfo.getTrackList())
		{
			if (!trackInfo.hasEvents())
			{
				t++;
				continue;
			}

			AbcPart newPart = new AbcPart(this);

			newPart.setTitle(abcInfo.getPartName(t));
			newPart.setPartNumber(abcInfo.getPartNumber(t));
			newPart.setTrackEnabled(t, true);

			Set<Integer> midiInstruments = trackInfo.getInstruments();
			for (LotroInstrument lotroInst : LotroInstrument.values())
			{
				if (midiInstruments.contains(lotroInst.midi.id()))
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

	private void initFromXml(File file, FileResolver fileResolver) throws SAXException, IOException, ParseException
	{
		try
		{
			saveFile = file;
			Document doc = XmlUtil.openDocument(sourceFile);
			Element songEle = XmlUtil.selectSingleElement(doc, "song");
			if (songEle == null)
			{
				throw new ParseException("Does not appear to be a valid Maestro file. Missing <song> root element.",
						sourceFile.getName());
			}
			Version fileVersion = SaveUtil.parseValue(songEle, "@fileVersion", SONG_FILE_VERSION);

			sourceFile = SaveUtil.parseValue(songEle, "sourceFile", (File) null);
			if (sourceFile == null)
			{
				throw SaveUtil.missingValueException(songEle, "<sourceFile>");
			}

			exportFile = SaveUtil.parseValue(songEle, "exportFile", exportFile);

			sequenceInfo = null;
			while (sequenceInfo == null)
			{
				String name = sourceFile.getName().toLowerCase();
				boolean isAbc = name.endsWith(".abc") || name.endsWith(".txt");

				try
				{
					if (isAbc)
					{
						AbcInfo abcInfo = new AbcInfo();

						AbcToMidi.Params params = new AbcToMidi.Params(sourceFile);
						params.abcInfo = abcInfo;
						params.useLotroInstruments = false;
						sequenceInfo = SequenceInfo.fromAbc(params);

						tripletTiming = abcInfo.hasTriplets();
						transcriber = abcInfo.getTranscriber();
					}
					else
					{
						sequenceInfo = SequenceInfo.fromMidi(sourceFile);
					}

					title = sequenceInfo.getTitle();
					composer = sequenceInfo.getComposer();
					keySignature = (ICompileConstants.SHOW_KEY_FIELD) ? sequenceInfo.getKeySignature()
							: KeySignature.C_MAJOR;
					timeSignature = sequenceInfo.getTimeSignature();
				}
				catch (FileNotFoundException e)
				{
					String msg = "Could not find the file used to create this song:\n" + sourceFile;
					sourceFile = fileResolver.locateFile(sourceFile, msg);
				}
				catch (InvalidMidiDataException | IOException | ParseException e)
				{
					String msg = "Could not load the file used to create this song:\n" + sourceFile + "\n\n"
							+ e.getMessage();
					sourceFile = fileResolver.resolveFile(sourceFile, msg);
				}

				if (sourceFile == null)
					throw new ParseException("Failed to load file", name);
			}

			title = SaveUtil.parseValue(songEle, "title", sequenceInfo.getTitle());
			composer = SaveUtil.parseValue(songEle, "composer", sequenceInfo.getComposer());
			transcriber = SaveUtil.parseValue(songEle, "transcriber", transcriber);

			tempoFactor = SaveUtil.parseValue(songEle, "exportSettings/@tempoFactor", tempoFactor);
			transpose = SaveUtil.parseValue(songEle, "exportSettings/@transpose", transpose);
			if (ICompileConstants.SHOW_KEY_FIELD)
				keySignature = SaveUtil.parseValue(songEle, "exportSettings/@keySignature", keySignature);
			timeSignature = SaveUtil.parseValue(songEle, "exportSettings/@timeSignature", timeSignature);
			tripletTiming = SaveUtil.parseValue(songEle, "exportSettings/@tripletTiming", tripletTiming);

			for (Element ele : XmlUtil.selectElements(songEle, "part"))
			{
				AbcPart part = AbcPart.loadFromXml(this, ele, fileVersion);
				int ins = Collections.binarySearch(parts, part, partNumberComparator);
				if (ins < 0)
					ins = -ins - 1;
				parts.add(ins, part);
				part.addAbcListener(abcPartListener);
			}
		}
		catch (XPathExpressionException e)
		{
			e.printStackTrace();
			throw new ParseException("XPath error: " + e.getMessage(), null);
		}
	}

	public Document saveToXml()
	{
		Document doc = XmlUtil.createDocument();
		Element songEle = (Element) doc.appendChild(doc.createElement("song"));
		songEle.setAttribute("fileVersion", String.valueOf(SONG_FILE_VERSION));

		SaveUtil.appendChildTextElement(songEle, "sourceFile", String.valueOf(sourceFile));
		if (exportFile != null)
			SaveUtil.appendChildTextElement(songEle, "exportFile", String.valueOf(exportFile));

		SaveUtil.appendChildTextElement(songEle, "title", title);
		SaveUtil.appendChildTextElement(songEle, "composer", composer);
		SaveUtil.appendChildTextElement(songEle, "transcriber", transcriber);

		{
			Element exportSettingsEle = doc.createElement("exportSettings");

			if (tempoFactor != 1.0f)
				exportSettingsEle.setAttribute("tempoFactor", String.valueOf(tempoFactor));

			if (transpose != 0)
				exportSettingsEle.setAttribute("transpose", String.valueOf(transpose));

			if (ICompileConstants.SHOW_KEY_FIELD)
			{
				if (!keySignature.equals(sequenceInfo.getKeySignature()))
					exportSettingsEle.setAttribute("keySignature", String.valueOf(keySignature));
			}

			if (!timeSignature.equals(sequenceInfo.getTimeSignature()))
				exportSettingsEle.setAttribute("timeSignature", String.valueOf(timeSignature));

			if (tripletTiming)
				exportSettingsEle.setAttribute("tripletTiming", String.valueOf(tripletTiming));

			if (exportSettingsEle.getAttributes().getLength() > 0 || exportSettingsEle.getChildNodes().getLength() > 0)
				songEle.appendChild(exportSettingsEle);
		}

		for (AbcPart part : parts)
		{
			part.saveToXml((Element) songEle.appendChild(doc.createElement("part")));
		}

		return doc;
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
		AbcPart newPart = new AbcPart(this);
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
		title = Util.emptyIfNull(title);
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
		composer = Util.emptyIfNull(composer);
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
		transcriber = Util.emptyIfNull(transcriber);
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

	public boolean isSkipSilenceAtStart()
	{
		return skipSilenceAtStart;
	}

	public void setSkipSilenceAtStart(boolean skipSilenceAtStart)
	{
		if (this.skipSilenceAtStart != skipSilenceAtStart)
		{
			this.skipSilenceAtStart = skipSilenceAtStart;
			fireChangeEvent(AbcSongProperty.SKIP_SILENCE_AT_START);
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

	public boolean isFromXmlFile()
	{
		return fromXmlFile;
	}

	@Override public String getPartName(AbcPartMetadataSource abcPart)
	{
		return partNameTemplate.formatName(abcPart);
	}

	public File getSourceFile()
	{
		return sourceFile;
	}

	public File getSaveFile()
	{
		return saveFile;
	}

	public void setSaveFile(File saveFile)
	{
		this.saveFile = saveFile;
	}

	@Override public File getExportFile()
	{
		return exportFile;
	}

	public void setExportFile(File exportFile)
	{
		if (this.exportFile == null && exportFile == null)
			return;
		if (this.exportFile != null && this.exportFile.equals(exportFile))
			return;

		this.exportFile = exportFile;
		fireChangeEvent(AbcSongProperty.EXPORT_FILE);
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
		if (listeners.size() > 0)
			listeners.fire(new AbcSongEvent(this, property, part));
	}

	public QuantizedTimingInfo getAbcTimingInfo() throws AbcConversionException
	{
		if (timingInfo == null //
				|| timingInfo.getExportTempoFactor() != getTempoFactor() //
				|| timingInfo.getMeter() != getTimeSignature() //
				|| timingInfo.isTripletTiming() != isTripletTiming())
		{
			timingInfo = new QuantizedTimingInfo(sequenceInfo, getTempoFactor(), getTimeSignature(), isTripletTiming());
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

			if (abcExporter.isSkipSilenceAtStart() != skipSilenceAtStart)
				abcExporter.setSkipSilenceAtStart(skipSilenceAtStart);
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
