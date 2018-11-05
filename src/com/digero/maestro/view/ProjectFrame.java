package com.digero.maestro.view;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.BadLocationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.digero.common.icons.IconLoader;
import com.digero.common.midi.MidiConstants;
import com.digero.common.midi.KeySignature;
import com.digero.common.midi.LotroSequencerWrapper;
import com.digero.common.midi.NoteFilterSequencerWrapper;
import com.digero.common.midi.SequencerEvent;
import com.digero.common.midi.SequencerEvent.SequencerProperty;
import com.digero.common.midi.SequencerWrapper;
import com.digero.common.midi.TimeSignature;
import com.digero.common.midi.VolumeTransceiver;
import com.digero.common.util.ExtensionFileFilter;
import com.digero.common.util.FileFilterDropListener;
import com.digero.common.util.ICompileConstants;
import com.digero.common.util.IDiscardable;
import com.digero.common.util.Listener;
import com.digero.common.util.ParseException;
import com.digero.common.util.Util;
import com.digero.common.view.AboutDialog;
import com.digero.common.view.BarNumberLabel;
import com.digero.common.view.ColorTable;
import com.digero.common.view.NativeVolumeBar;
import com.digero.common.view.SongPositionLabel;
import com.digero.maestro.MaestroMain;
import com.digero.maestro.abc.AbcConversionException;
import com.digero.maestro.abc.AbcExporter;
import com.digero.maestro.abc.AbcPart;
import com.digero.maestro.abc.AbcPartEvent;
import com.digero.maestro.abc.AbcPartEvent.AbcPartProperty;
import com.digero.maestro.abc.AbcSong;
import com.digero.maestro.abc.AbcSongEvent;
import com.digero.maestro.abc.PartAutoNumberer;
import com.digero.maestro.abc.PartNameTemplate;
import com.digero.maestro.midi.SequenceInfo;
import com.digero.maestro.util.FileResolver;
import com.digero.maestro.util.XmlUtil;

@SuppressWarnings("serial")
public class ProjectFrame extends JFrame implements TableLayoutConstants, ICompileConstants
{
	private static final int HGAP = 4, VGAP = 4;
	private static final double[] LAYOUT_COLS = new double[] { 180, FILL };
	private static final double[] LAYOUT_ROWS = new double[] { FILL };

	private Preferences prefs = Preferences.userNodeForPackage(MaestroMain.class);

	private AbcSong abcSong;
	private boolean abcSongModified = false;

	private boolean allowOverwriteSaveFile = false;
	private boolean allowOverwriteExportFile = false;
	private NoteFilterSequencerWrapper sequencer;
	private VolumeTransceiver volumeTransceiver;
	private NoteFilterSequencerWrapper abcSequencer;
	private VolumeTransceiver abcVolumeTransceiver;
	private PartAutoNumberer partAutoNumberer;
	private PartNameTemplate partNameTemplate;
	private SaveAndExportSettings saveSettings;
	private boolean usingNativeVolume;

	private JPanel content;
	private JTextField songTitleField;
	private JTextField composerField;
	private JTextField transcriberField;
	private PrefsDocumentListener transcriberFieldListener;
	private JSpinner transposeSpinner;
	private JSpinner tempoSpinner;
	private JButton resetTempoButton;
	private JFormattedTextField keySignatureField;
	private JFormattedTextField timeSignatureField;
	private JCheckBox tripletCheckBox;
	private JButton exportButton;
	private JLabel exportSuccessfulLabel;
	private Timer exportLabelHideTimer;
	private JMenuItem saveMenuItem;
	private JMenuItem saveAsMenuItem;
	private JMenuItem exportMenuItem;
	private JMenuItem exportAsMenuItem;

	private JList<AbcPart> partsList;
	private JButton newPartButton;
	private JButton deletePartButton;

	private PartPanel partPanel;

	private boolean abcPreviewMode = false;
	private JToggleButton abcModeRadioButton;
	private JToggleButton midiModeRadioButton;
	private JButton playButton;
	private JButton stopButton;
	private NativeVolumeBar volumeBar;
	private SongPositionLabel midiPositionLabel;
	private SongPositionLabel abcPositionLabel;
	private BarNumberLabel midiBarLabel;
	private BarNumberLabel abcBarLabel;

	private Icon playIcon;
	private Icon playIconDisabled;
	private Icon pauseIcon;
	private Icon pauseIconDisabled;
	private Icon abcPlayIcon;
	private Icon abcPlayIconDisabled;
	private Icon abcPauseIcon;
	private Icon abcPauseIconDisabled;

	private long abcPreviewStartTick = 0;
	private float abcPreviewTempoFactor = 1.0f;
	private boolean echoingPosition = false;

	private MainSequencerListener mainSequencerListener;
	private AbcSequencerListener abcSequencerListener;
	private boolean failedToLoadLotroInstruments = false;

	public ProjectFrame()
	{
		super(MaestroMain.APP_NAME);
		setMinimumSize(new Dimension(512, 384));
		Util.initWinBounds(this, prefs.node("window"), 800, 600);

		ToolTipManager.sharedInstance().setDismissDelay(8000);

		String welcomeMessage = formatInfoMessage("Hello Maestro", "Drag and drop a MIDI or ABC file to open it.\n"
				+ "Or use File > Open.");

		partAutoNumberer = new PartAutoNumberer(prefs.node("partAutoNumberer"));
		partNameTemplate = new PartNameTemplate(prefs.node("partNameTemplate"));
		saveSettings = new SaveAndExportSettings(prefs.node("saveAndExportSettings"));

		usingNativeVolume = MaestroMain.isNativeVolumeSupported();
		if (usingNativeVolume)
		{
			volumeTransceiver = null;
			abcVolumeTransceiver = null;
		}
		else
		{
			volumeTransceiver = new VolumeTransceiver();
			volumeTransceiver.setVolume(prefs.getInt("volumizer", NativeVolumeBar.MAX_VOLUME));

			abcVolumeTransceiver = new VolumeTransceiver();
			abcVolumeTransceiver.setVolume(volumeTransceiver.getVolume());
		}

		try
		{
			sequencer = new NoteFilterSequencerWrapper();
			if (volumeTransceiver != null)
				sequencer.addTransceiver(volumeTransceiver);

			abcSequencer = new LotroSequencerWrapper();
			if (abcVolumeTransceiver != null)
				abcSequencer.addTransceiver(abcVolumeTransceiver);

			if (LotroSequencerWrapper.getLoadLotroSynthError() != null)
			{
				welcomeMessage = formatErrorMessage("Could not load LOTRO instrument sounds",
						"ABC Preview will use standard MIDI instruments instead\n"
								+ "(drums do not sound good in this mode).\n\n" + "Error details:\n"
								+ LotroSequencerWrapper.getLoadLotroSynthError());
				failedToLoadLotroInstruments = true;
			}
		}
		catch (MidiUnavailableException e)
		{
			JOptionPane.showMessageDialog(null, "Failed to initialize MIDI sequencer.\nThe program will now exit.\n\n"
					+ "Error details:\n" + e.getMessage(), "Failed to initialize MIDI sequencer",
					JOptionPane.ERROR_MESSAGE);
			System.exit(1);
			return;
		}

		try
		{
			List<Image> icons = new ArrayList<Image>();
			icons.add(ImageIO.read(IconLoader.class.getResourceAsStream("maestro_16.png")));
			icons.add(ImageIO.read(IconLoader.class.getResourceAsStream("maestro_32.png")));
			setIconImages(icons);
		}
		catch (Exception ex)
		{
			// Ignore
			ex.printStackTrace();
		}

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter()
		{
			@Override public void windowClosing(WindowEvent e)
			{
				if (closeSong())
				{
					setVisible(false);
					dispose();
				}
			}
		});

		playIcon = IconLoader.getImageIcon("play_blue.png");
		playIconDisabled = IconLoader.getDisabledIcon("play_blue.png");
		pauseIcon = IconLoader.getImageIcon("pause_blue.png");
		pauseIconDisabled = IconLoader.getDisabledIcon("pause_blue.png");
		abcPlayIcon = IconLoader.getImageIcon("play_yellow.png");
		abcPlayIconDisabled = IconLoader.getDisabledIcon("play_yellow.png");
		abcPauseIcon = IconLoader.getImageIcon("pause.png");
		abcPauseIconDisabled = IconLoader.getDisabledIcon("pause.png");
		Icon stopIcon = IconLoader.getImageIcon("stop.png");
		Icon stopIconDisabled = IconLoader.getDisabledIcon("stop.png");

		partPanel = new PartPanel(sequencer, partAutoNumberer, abcSequencer);
		partPanel.addSettingsActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				doSettingsDialog(SettingsDialog.NUMBERING_TAB);
			}
		});

		TableLayout tableLayout = new TableLayout(LAYOUT_COLS, LAYOUT_ROWS);
		tableLayout.setHGap(HGAP);
		tableLayout.setVGap(VGAP);

		content = new JPanel(tableLayout, false);
		setContentPane(content);

		songTitleField = new JTextField();
		songTitleField.setToolTipText("Song Title");
		songTitleField.getDocument().addDocumentListener(new SimpleDocumentListener()
		{
			@Override public void changedUpdate(DocumentEvent e)
			{
				if (abcSong != null)
					abcSong.setTitle(songTitleField.getText());
			}
		});

		composerField = new JTextField();
		composerField.setToolTipText("Song Composer");
		composerField.getDocument().addDocumentListener(new SimpleDocumentListener()
		{
			@Override public void changedUpdate(DocumentEvent e)
			{
				if (abcSong != null)
					abcSong.setComposer(composerField.getText());
			}
		});

		transcriberField = new JTextField(prefs.get("transcriber", ""));
		transcriberField.setToolTipText("Song Transcriber (your name)");
		transcriberFieldListener = new PrefsDocumentListener(prefs, "transcriber");
		transcriberField.getDocument().addDocumentListener(transcriberFieldListener);
		transcriberField.getDocument().addDocumentListener(new SimpleDocumentListener()
		{
			@Override public void changedUpdate(DocumentEvent e)
			{
				if (abcSong != null)
					abcSong.setTranscriber(transcriberField.getText());
			}
		});

		keySignatureField = new MyFormattedTextField(KeySignature.C_MAJOR, 5);
		keySignatureField.setToolTipText("<html>Adjust the key signature of the ABC file. "
				+ "This only affects the display, not the sound of the exported file.<br>"
				+ "Examples: C maj, Eb maj, F# min</html>");
		if (SHOW_KEY_FIELD)
		{
			keySignatureField.addPropertyChangeListener("value", new PropertyChangeListener()
			{
				@Override public void propertyChange(PropertyChangeEvent evt)
				{
					if (abcSong != null)
						abcSong.setKeySignature((KeySignature) keySignatureField.getValue());

				}
			});
		}

		timeSignatureField = new MyFormattedTextField(TimeSignature.FOUR_FOUR, 5);
		timeSignatureField.setToolTipText("<html>Adjust the time signature of the ABC file.<br><br>"
				+ "This only affects the display, not the sound of the exported file.<br>"
				+ "Examples: 4/4, 3/8, 2/2</html>");
		timeSignatureField.addPropertyChangeListener("value", new PropertyChangeListener()
		{
			@Override public void propertyChange(PropertyChangeEvent evt)
			{
				if (abcSong != null)
					abcSong.setTimeSignature((TimeSignature) timeSignatureField.getValue());
			}
		});

		transposeSpinner = new JSpinner(new SpinnerNumberModel(0, -48, 48, 1));
		transposeSpinner.setToolTipText("<html>Transpose the entire song by semitones.<br>"
				+ "12 semitones = 1 octave</html>");
		transposeSpinner.addChangeListener(new ChangeListener()
		{
			@Override public void stateChanged(ChangeEvent e)
			{
				if (abcSong != null)
					abcSong.setTranspose(getTranspose());
			}
		});

		tempoSpinner = new JSpinner(new SpinnerNumberModel(MidiConstants.DEFAULT_TEMPO_BPM /* value */, 8 /* min */,
				960 /* max */, 1 /* step */));
		tempoSpinner.setToolTipText("<html>Tempo in beats per minute.<br><br>"
				+ "This number represents the <b>Main Tempo</b>, which is the tempo that covers<br>"
				+ "the largest portion of the song. If parts of the song play at a different tempo,<br>"
				+ "they will all be adjusted proportionally.</html>");
		tempoSpinner.addChangeListener(new ChangeListener()
		{
			@Override public void stateChanged(ChangeEvent e)
			{
				if (abcSong != null)
				{
					abcSong.setTempoBPM((Integer) tempoSpinner.getValue());

					abcSequencer.setTempoFactor(abcSong.getTempoFactor());

					if (abcSequencer.isRunning())
					{
						float delta = abcPreviewTempoFactor / abcSequencer.getTempoFactor();
						if (Math.max(delta, 1 / delta) > 1.5f)
							refreshPreviewSequence(false);
					}
				}
				else
				{
					abcSequencer.setTempoFactor(1.0f);
				}
			}
		});

		resetTempoButton = new JButton("Reset");
		resetTempoButton.setMargin(new Insets(2, 8, 2, 8));
		resetTempoButton.setToolTipText("Set the tempo back to the source file's tempo");
		resetTempoButton.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				if (abcSong == null)
				{
					tempoSpinner.setValue(MidiConstants.DEFAULT_TEMPO_BPM);
				}
				else
				{
					float tempoFactor = abcSequencer.getTempoFactor();
					tempoSpinner.setValue(abcSong.getSequenceInfo().getPrimaryTempoBPM());
					if (tempoFactor != 1.0f)
						refreshPreviewSequence(false);
				}
				tempoSpinner.requestFocus();
			}
		});

		tripletCheckBox = new JCheckBox("Triplets/swing rhythm");
		tripletCheckBox.setToolTipText("<html>Tweak the timing to allow for triplets or a swing rhythm.<br><br>"
				+ "This can cause short/fast notes to incorrectly be detected as triplets.<br>"
				+ "Leave it unchecked unless the song has triplets or a swing rhythm.</html>");
		tripletCheckBox.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				if (abcSong != null)
					abcSong.setTripletTiming(tripletCheckBox.isSelected());

				if (abcSequencer.isRunning())
					refreshPreviewSequence(false);
			}
		});

		exportButton = new JButton(); // Label set in onSaveAndExportSettingsChanged()
		exportButton.setToolTipText("<html><b>Export ABC</b><br>(Ctrl+E)</html>");
		exportButton.setIcon(IconLoader.getImageIcon("abcfile_32.png"));
		exportButton.setDisabledIcon(IconLoader.getDisabledIcon("abcfile_32.png"));
		exportButton.setHorizontalAlignment(SwingConstants.LEFT);
		exportButton.getModel().addChangeListener(new ChangeListener()
		{
			private boolean pressed = false;

			@Override public void stateChanged(ChangeEvent e)
			{
				if (exportButton.getModel().isPressed() != pressed)
				{
					pressed = exportButton.getModel().isPressed();
					if (pressed)
						exportSuccessfulLabel.setVisible(false);
				}
			}
		});
		exportButton.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				exportAbc();
			}
		});

		exportSuccessfulLabel = new JLabel("Exported");
		exportSuccessfulLabel.setIcon(IconLoader.getImageIcon("check_16.png"));
		exportSuccessfulLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
		exportSuccessfulLabel.setVisible(false);

		partsList = new JList<AbcPart>();
		partsList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		partsList.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			@Override public void valueChanged(ListSelectionEvent e)
			{
				AbcPart abcPart = partsList.getSelectedValue();
				sequencer.getFilter().onAbcPartChanged(abcPart != null);
				abcSequencer.getFilter().onAbcPartChanged(abcPart != null);
				partPanel.setAbcPart(abcPart);
			}
		});

		JScrollPane partsListScrollPane = new JScrollPane(partsList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		newPartButton = new JButton("New Part");
		newPartButton.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				if (abcSong != null)
					abcSong.createNewPart();
			}
		});

		deletePartButton = new JButton("Delete");
		deletePartButton.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				if (abcSong != null)
					abcSong.deletePart(partsList.getSelectedValue());
			}
		});

		TableLayout songInfoLayout = new TableLayout(//
				new double[] { PREFERRED, FILL },//
				new double[] { PREFERRED, PREFERRED, PREFERRED });
		songInfoLayout.setHGap(HGAP);
		songInfoLayout.setVGap(VGAP);
		JPanel songInfoPanel = new JPanel(songInfoLayout);
		{
			int row = 0;
			songInfoPanel.add(new JLabel("T:"), "0, " + row);
			songInfoPanel.add(songTitleField, "1, " + row);
			row++;
			songInfoPanel.add(new JLabel("C:"), "0, " + row);
			songInfoPanel.add(composerField, "1, " + row);
			row++;
			songInfoPanel.add(new JLabel("Z:"), "0, " + row);
			songInfoPanel.add(transcriberField, "1, " + row);

			songInfoPanel.setBorder(BorderFactory.createTitledBorder("Song Info"));
		}

		JPanel partsButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, HGAP, VGAP));
		partsButtonPanel.add(newPartButton);
		partsButtonPanel.add(deletePartButton);

		JPanel partsListPanel = new JPanel(new BorderLayout(HGAP, VGAP));
		partsListPanel.setBorder(BorderFactory.createTitledBorder("Song Parts"));
		partsListPanel.add(partsButtonPanel, BorderLayout.NORTH);
		partsListPanel.add(partsListScrollPane, BorderLayout.CENTER);

		TableLayout settingsLayout = new TableLayout(//
				new double[] { PREFERRED, PREFERRED, FILL },//
				new double[] { });
		settingsLayout.setVGap(VGAP);
		settingsLayout.setHGap(HGAP);

		JPanel settingsPanel = new JPanel(settingsLayout);
		settingsPanel.setBorder(BorderFactory.createTitledBorder("Export Settings"));

		{
			int row = 0;
			settingsLayout.insertRow(row, PREFERRED);
			settingsPanel.add(new JLabel("Transpose:"), "0, " + row);
			settingsPanel.add(transposeSpinner, "1, " + row);

			row++;
			settingsLayout.insertRow(row, PREFERRED);
			settingsPanel.add(new JLabel("Main Tempo:"), "0, " + row);
			settingsPanel.add(tempoSpinner, "1, " + row);
			settingsPanel.add(resetTempoButton, "2, " + row + ", L, F");

			row++;
			settingsLayout.insertRow(row, PREFERRED);
			settingsPanel.add(new JLabel("Meter:"), "0, " + row);
			settingsPanel.add(timeSignatureField, "1, " + row + ", 2, " + row + ", L, F");

			if (SHOW_KEY_FIELD)
			{
				row++;
				settingsLayout.insertRow(row, PREFERRED);
				settingsPanel.add(new JLabel("Key:"), "0, " + row);
				settingsPanel.add(keySignatureField, "1, " + row + ", 2, " + row + ", L, F");
			}

			row++;
			settingsLayout.insertRow(row, PREFERRED);
			settingsPanel.add(tripletCheckBox, "0, " + row + ", 2, " + row + ", L, C");

			row++;
			settingsLayout.insertRow(row, PREFERRED);
			settingsPanel.add(exportSuccessfulLabel, "0, " + row + ", 2, " + row + ", F, F");

			row++;
			settingsLayout.insertRow(row, PREFERRED);
			settingsPanel.add(exportButton, "0, " + row + ", 2, " + row + ", F, F");
		}

		if (!SHOW_TEMPO_SPINNER)
			tempoSpinner.setEnabled(false);
		if (!SHOW_METER_TEXTBOX)
			timeSignatureField.setEnabled(false);
		if (!SHOW_KEY_FIELD)
			keySignatureField.setEnabled(false);

		volumeBar = new NativeVolumeBar(new VolumeManager());
		JPanel volumePanel = new JPanel(new TableLayout(//
				new double[] { PREFERRED },//
				new double[] { PREFERRED, PREFERRED }));
		volumePanel.add(new JLabel("Volume"), "0, 0, c, c");
		volumePanel.add(volumeBar, "0, 1, f, c");

		ActionListener modeButtonListener = new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				updatePreviewMode(abcModeRadioButton.isSelected());
			}
		};

		midiModeRadioButton = new JRadioButton("Original");
		midiModeRadioButton.addActionListener(modeButtonListener);
		midiModeRadioButton.setMargin(new Insets(1, 5, 1, 5));

		abcModeRadioButton = new JRadioButton("ABC Preview");
		abcModeRadioButton.addActionListener(modeButtonListener);
		abcModeRadioButton.setMargin(new Insets(1, 5, 1, 5));

		ButtonGroup modeButtonGroup = new ButtonGroup();
		modeButtonGroup.add(abcModeRadioButton);
		modeButtonGroup.add(midiModeRadioButton);

		midiModeRadioButton.setSelected(true);
		abcPreviewMode = abcModeRadioButton.isSelected();

		final Insets playControlButtonMargin = new Insets(5, 20, 5, 20);

		playButton = new JButton(playIcon);
		playButton.setDisabledIcon(playIconDisabled);
		playButton.setMargin(playControlButtonMargin);
		playButton.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				SequencerWrapper curSequencer = abcPreviewMode ? abcSequencer : sequencer;

				boolean running = !curSequencer.isRunning();
				if (abcPreviewMode && running)
				{
					if (!refreshPreviewSequence(true))
						running = false;
				}

				curSequencer.setRunning(running);
				updateButtons(false);
			}
		});

		stopButton = new JButton(stopIcon);
		stopButton.setDisabledIcon(stopIconDisabled);
		stopButton.setToolTipText("Stop");
		stopButton.setMargin(playControlButtonMargin);
		stopButton.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				abcSequencer.stop();
				sequencer.stop();
				abcSequencer.reset(false);
				sequencer.reset(false);
			}
		});

		JPanel modeButtonPanel = new JPanel(new BorderLayout());
		modeButtonPanel.add(midiModeRadioButton, BorderLayout.NORTH);
		modeButtonPanel.add(abcModeRadioButton, BorderLayout.SOUTH);

		JPanel playButtonPanel = new JPanel(new TableLayout(//
				new double[] { 0.5, 0.5 },//
				new double[] { PREFERRED }));
		playButtonPanel.add(playButton, "0, 0");
		playButtonPanel.add(stopButton, "1, 0");

		midiPositionLabel = new SongPositionLabel(sequencer);

		abcPositionLabel = new SongPositionLabel(abcSequencer, true /* adjustForTempo */);
		abcPositionLabel.setVisible(!midiPositionLabel.isVisible());

		midiBarLabel = new BarNumberLabel(sequencer, null);
		midiBarLabel.setToolTipText("Bar number");

		abcBarLabel = new BarNumberLabel(abcSequencer, null);
		abcBarLabel.setToolTipText("Bar number");
		abcBarLabel.setVisible(!midiBarLabel.isVisible());

		JPanel playControlPanel = new JPanel(new TableLayout(//
				new double[] { 4, 0.50, 4, PREFERRED, 4, 0.50, 4, PREFERRED, 4 },//
				new double[] { PREFERRED, 4, PREFERRED }));
		playControlPanel.add(playButtonPanel, "3, 0, 3, 2, C, C");
		playControlPanel.add(modeButtonPanel, "1, 0, 1, 2, C, F");
		playControlPanel.add(volumePanel, "5, 0, 5, 2, C, C");
		playControlPanel.add(midiPositionLabel, "7, 0, R, B");
		playControlPanel.add(abcPositionLabel, "7, 0, R, B");
		playControlPanel.add(midiBarLabel, "7, 2, R, T");
		playControlPanel.add(abcBarLabel, "7, 2, R, T");

		JPanel abcPartsAndSettings = new JPanel(new BorderLayout(HGAP, VGAP));
		abcPartsAndSettings.add(songInfoPanel, BorderLayout.NORTH);
		JPanel partsListAndColorizer = new JPanel(new BorderLayout(HGAP, VGAP));
		partsListAndColorizer.add(partsListPanel, BorderLayout.CENTER);
		if (SHOW_COLORIZER)
			partsListAndColorizer.add(new Colorizer(partPanel), BorderLayout.SOUTH);
		abcPartsAndSettings.add(partsListAndColorizer, BorderLayout.CENTER);
		abcPartsAndSettings.add(settingsPanel, BorderLayout.SOUTH);

		JPanel midiPartsAndControls = new JPanel(new BorderLayout(HGAP, VGAP));
		midiPartsAndControls.add(partPanel, BorderLayout.CENTER);
		midiPartsAndControls.add(playControlPanel, BorderLayout.SOUTH);
		midiPartsAndControls.setBorder(BorderFactory.createTitledBorder("Part Settings"));

		add(abcPartsAndSettings, "0, 0");
		add(midiPartsAndControls, "1, 0");

		final FileFilterDropListener dropListener = new FileFilterDropListener(false, "mid", "midi", "abc", "txt",
				AbcSong.MSX_FILE_EXTENSION_NO_DOT);
		dropListener.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				final File file = dropListener.getDroppedFile();
				SwingUtilities.invokeLater(new Runnable()
				{
					@Override public void run()
					{
						openFile(file);
					}
				});
			}
		});
		new DropTarget(this, dropListener);

		mainSequencerListener = new MainSequencerListener();
		sequencer.addChangeListener(mainSequencerListener);

		abcSequencerListener = new AbcSequencerListener();
		abcSequencer.addChangeListener(abcSequencerListener);

		initMenu();
		onSaveAndExportSettingsChanged();
		partPanel.showInfoMessage(welcomeMessage);
		updateButtons(true);
	}

	private static void discardObject(IDiscardable object)
	{
		if (object != null)
			object.discard();
	}

	@Override public void dispose()
	{
		if (abcSong != null)
		{
			abcSong.getParts().getListModel().removeListDataListener(partsListListener);
		}

		discardObject(sequencer);
		discardObject(abcSequencer);
		discardObject(abcSong);
		discardObject(midiPositionLabel);
		discardObject(abcPositionLabel);
		discardObject(midiBarLabel);
		discardObject(abcBarLabel);

		super.dispose();
	}

	private void initMenu()
	{
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		JMenu fileMenu = menuBar.add(new JMenu(" File "));
		fileMenu.setMnemonic('F');

		JMenuItem openItem = fileMenu.add(new JMenuItem("Open file..."));
		openItem.setMnemonic('O');
		openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
		openItem.addActionListener(new ActionListener()
		{
			JFileChooser openFileChooser;

			@Override public void actionPerformed(ActionEvent e)
			{
				if (openFileChooser == null)
				{
					openFileChooser = new JFileChooser(prefs.get("openFileChooser.path", null));
					openFileChooser.setMultiSelectionEnabled(false);
					openFileChooser.setFileFilter(new ExtensionFileFilter("MIDI, ABC, and "
							+ AbcSong.MSX_FILE_DESCRIPTION_PLURAL, "mid", "midi", "abc", "txt",
							AbcSong.MSX_FILE_EXTENSION_NO_DOT));
				}

				int result = openFileChooser.showOpenDialog(ProjectFrame.this);
				if (result == JFileChooser.APPROVE_OPTION)
				{
					openFile(openFileChooser.getSelectedFile());
					prefs.put("openFileChooser.path", openFileChooser.getCurrentDirectory().getAbsolutePath());
				}
			}
		});

		fileMenu.addSeparator();

		saveMenuItem = fileMenu.add(new JMenuItem("Save " + AbcSong.MSX_FILE_DESCRIPTION));
		saveMenuItem.setIcon(IconLoader.getImageIcon("msxfile_16.png"));
		saveMenuItem.setDisabledIcon(IconLoader.getDisabledIcon("msxfile_16.png"));
		saveMenuItem.setMnemonic('S');
		saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
		saveMenuItem.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				save();
			}
		});

		saveAsMenuItem = fileMenu.add(new JMenuItem("Save " + AbcSong.MSX_FILE_DESCRIPTION + " As..."));
		saveAsMenuItem.setMnemonic('A');
		saveAsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK
				| KeyEvent.SHIFT_DOWN_MASK));
		saveAsMenuItem.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				saveAs();
			}
		});

		fileMenu.addSeparator();

		exportMenuItem = fileMenu.add(new JMenuItem("Export ABC"));
		exportMenuItem.setIcon(IconLoader.getImageIcon("abcfile_16.png"));
		exportMenuItem.setDisabledIcon(IconLoader.getDisabledIcon("abcfile_16.png"));
		exportMenuItem.setMnemonic('E');
		exportMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_DOWN_MASK));
		exportMenuItem.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				exportAbc();
			}
		});

		exportAsMenuItem = fileMenu.add(new JMenuItem("Export ABC As..."));
		exportAsMenuItem.setMnemonic('p');
		exportAsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_DOWN_MASK
				| KeyEvent.SHIFT_DOWN_MASK));
		exportAsMenuItem.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				exportAbcAs();
			}
		});

		fileMenu.addSeparator();

		JMenuItem exitItem = fileMenu.add(new JMenuItem("Exit"));
		exitItem.setMnemonic('x');
		exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, KeyEvent.ALT_DOWN_MASK));
		exitItem.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				if (closeSong())
				{
					setVisible(false);
					dispose();
				}
			}
		});

		JMenu toolsMenu = menuBar.add(new JMenu(" Tools "));
		toolsMenu.setMnemonic('T');

		JMenuItem settingsItem = toolsMenu.add(new JMenuItem("Options..."));
		settingsItem.setIcon(IconLoader.getImageIcon("gear_16.png"));
		settingsItem.setDisabledIcon(IconLoader.getDisabledIcon("gear_16.png"));
		settingsItem.setMnemonic('O');
		settingsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.CTRL_DOWN_MASK));
		settingsItem.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				doSettingsDialog();
			}
		});

		toolsMenu.addSeparator();

		JMenuItem aboutItem = toolsMenu.add(new JMenuItem("About " + MaestroMain.APP_NAME + "..."));
		aboutItem.setMnemonic('A');
		aboutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
		aboutItem.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				AboutDialog.show(ProjectFrame.this, MaestroMain.APP_NAME, MaestroMain.APP_VERSION, MaestroMain.APP_URL,
						"maestro_64.png");
			}
		});
	}

	private int currentSettingsDialogTab = 0;

	private void doSettingsDialog()
	{
		doSettingsDialog(currentSettingsDialogTab);
	}

	private void doSettingsDialog(int tab)
	{
		SettingsDialog dialog = new SettingsDialog(ProjectFrame.this, partAutoNumberer.getSettingsCopy(),
				partNameTemplate, saveSettings.getCopy());
		dialog.setActiveTab(tab);
		dialog.setVisible(true);
		if (dialog.isSuccess())
		{
			if (dialog.isNumbererSettingsChanged())
			{
				partAutoNumberer.setSettings(dialog.getNumbererSettings());
				partAutoNumberer.renumberAllParts();
			}
			partNameTemplate.setSettings(dialog.getNameTemplateSettings());
			partPanel.settingsChanged();

			saveSettings.copyFrom(dialog.getSaveAndExportSettings());
			saveSettings.saveToPrefs();
			onSaveAndExportSettingsChanged();
		}
		currentSettingsDialogTab = dialog.getActiveTab();
		dialog.dispose();
	}

	private void onSaveAndExportSettingsChanged()
	{
		if (saveSettings.showExportFileChooser)
		{
			exportAsMenuItem.setVisible(false);
			exportMenuItem.setText("Export ABC As...");
		}
		else
		{
			exportAsMenuItem.setVisible(true);
			exportMenuItem.setText("Export ABC");
		}

		if (shouldExportAbcAs())
			exportButton.setText("Export ABC As...");
		else
			exportButton.setText("Export ABC");

		if (abcSong != null)
			abcSong.setSkipSilenceAtStart(saveSettings.skipSilenceAtStart);
	}

	public void onVolumeChanged()
	{
		volumeBar.repaint();
	}

	private class VolumeManager implements NativeVolumeBar.Callback
	{
		@Override public void setVolume(int volume)
		{
			if (usingNativeVolume)
			{
				MaestroMain.setVolume((float) volume / NativeVolumeBar.MAX_VOLUME);
			}
			else
			{
				if (volumeTransceiver != null)
					volumeTransceiver.setVolume(volume);
				if (abcVolumeTransceiver != null)
					abcVolumeTransceiver.setVolume(volume);
				prefs.putInt("volumizer", volume);
			}
		}

		@Override public int getVolume()
		{
			if (usingNativeVolume)
			{
				return (int) (MaestroMain.getVolume() * NativeVolumeBar.MAX_VOLUME);
			}
			else
			{
				if (volumeTransceiver != null)
					return volumeTransceiver.getVolume();
				if (abcVolumeTransceiver != null)
					return abcVolumeTransceiver.getVolume();
				return NativeVolumeBar.MAX_VOLUME;
			}
		}
	}

	private class MainSequencerListener implements Listener<SequencerEvent>
	{
		@Override public void onEvent(SequencerEvent evt)
		{
			updateButtons(false);
			if (evt.getProperty() == SequencerProperty.IS_RUNNING)
			{
				if (sequencer.isRunning())
					abcSequencer.stop();
			}
			else if (!echoingPosition)
			{
				try
				{
					echoingPosition = true;
					if (evt.getProperty() == SequencerProperty.POSITION)
					{
						abcSequencer.setTickPosition(Util.clamp(sequencer.getTickPosition(), abcPreviewStartTick,
								abcSequencer.getTickLength()));
					}
					else if (evt.getProperty() == SequencerProperty.DRAG_POSITION)
					{
						abcSequencer.setDragTick(Util.clamp(sequencer.getDragTick(), abcPreviewStartTick,
								abcSequencer.getTickLength()));
					}
					else if (evt.getProperty() == SequencerProperty.IS_DRAGGING)
					{
						abcSequencer.setDragging(sequencer.isDragging());
					}
				}
				finally
				{
					echoingPosition = false;
				}
			}
		}
	}

	private class AbcSequencerListener implements Listener<SequencerEvent>
	{
		@Override public void onEvent(SequencerEvent evt)
		{
			updateButtons(false);
			if (evt.getProperty() == SequencerProperty.IS_RUNNING)
			{
				if (abcSequencer.isRunning())
					sequencer.stop();
			}
			else if (!echoingPosition)
			{
				try
				{
					echoingPosition = true;
					if (evt.getProperty() == SequencerProperty.POSITION)
					{
						sequencer.setTickPosition(Util.clamp(abcSequencer.getTickPosition(), 0,
								sequencer.getTickLength()));
					}
					else if (evt.getProperty() == SequencerProperty.DRAG_POSITION)
					{
						sequencer.setDragTick(Util.clamp(abcSequencer.getDragTick(), 0, sequencer.getTickLength()));
					}
					else if (evt.getProperty() == SequencerProperty.IS_DRAGGING)
					{
						sequencer.setDragging(abcSequencer.isDragging());
					}
				}
				finally
				{
					echoingPosition = false;
				}
			}
		}
	}

	private static abstract class SimpleDocumentListener implements DocumentListener
	{
		@Override public void insertUpdate(DocumentEvent e)
		{
			this.changedUpdate(e);
		}

		@Override public void removeUpdate(DocumentEvent e)
		{
			this.changedUpdate(e);
		}
	}

	private static class PrefsDocumentListener implements DocumentListener
	{
		private Preferences prefs;
		private String prefName;
		private boolean ignoreChanges = false;

		public PrefsDocumentListener(Preferences prefs, String prefName)
		{
			this.prefs = prefs;
			this.prefName = prefName;
		}

		public void setIgnoreChanges(boolean ignoringChanges)
		{
			this.ignoreChanges = ignoringChanges;
		}

		private void updatePrefs(javax.swing.text.Document doc)
		{
			if (ignoreChanges)
				return;

			String txt;
			try
			{
				txt = doc.getText(0, doc.getLength());
			}
			catch (BadLocationException e)
			{
				txt = "";
			}
			prefs.put(prefName, txt);
		}

		@Override public void changedUpdate(DocumentEvent e)
		{
			updatePrefs(e.getDocument());
		}

		@Override public void insertUpdate(DocumentEvent e)
		{
			updatePrefs(e.getDocument());
		}

		@Override public void removeUpdate(DocumentEvent e)
		{
			updatePrefs(e.getDocument());
		}
	}

	private boolean updateButtonsPending = false;
	private Runnable updateButtonsTask = new Runnable()
	{
		@Override public void run()
		{
			boolean hasAbcNotes = false;
			if (abcSong != null)
			{
				for (AbcPart part : abcSong.getParts())
				{
					if (part.getEnabledTrackCount() > 0)
					{
						hasAbcNotes = true;
						break;
					}
				}
			}

			boolean midiLoaded = sequencer.isLoaded();

			SequencerWrapper curSequencer = abcPreviewMode ? abcSequencer : sequencer;
			Icon curPlayIcon = abcPreviewMode ? abcPlayIcon : playIcon;
			Icon curPlayIconDisabled = abcPreviewMode ? abcPlayIconDisabled : playIconDisabled;
			Icon curPauseIcon = abcPreviewMode ? abcPauseIcon : pauseIcon;
			Icon curPauseIconDisabled = abcPreviewMode ? abcPauseIconDisabled : pauseIconDisabled;
			playButton.setIcon(curSequencer.isRunning() ? curPauseIcon : curPlayIcon);
			playButton.setDisabledIcon(curSequencer.isRunning() ? curPauseIconDisabled : curPlayIconDisabled);

			if (!hasAbcNotes)
			{
				midiModeRadioButton.setSelected(true);
				abcSequencer.setRunning(false);
				updatePreviewMode(false);
			}

			playButton.setEnabled(midiLoaded);
			midiModeRadioButton.setEnabled(midiLoaded || hasAbcNotes);
			abcModeRadioButton.setEnabled(hasAbcNotes);
			stopButton.setEnabled((midiLoaded && (sequencer.isRunning() || sequencer.getPosition() != 0))
					|| (abcSequencer.isLoaded() && (abcSequencer.isRunning() || abcSequencer.getPosition() != 0)));

			newPartButton.setEnabled(abcSong != null);
			deletePartButton.setEnabled(partsList.getSelectedIndex() != -1);
			exportButton.setEnabled(hasAbcNotes);
			exportMenuItem.setEnabled(hasAbcNotes);
			exportAsMenuItem.setEnabled(hasAbcNotes);
			saveMenuItem.setEnabled(abcSong != null);
			saveAsMenuItem.setEnabled(abcSong != null);

			songTitleField.setEnabled(midiLoaded);
			composerField.setEnabled(midiLoaded);
			transcriberField.setEnabled(midiLoaded);
			transposeSpinner.setEnabled(midiLoaded);
			tempoSpinner.setEnabled(midiLoaded);
			resetTempoButton.setEnabled(midiLoaded && abcSong != null && abcSong.getTempoFactor() != 1.0f);
			resetTempoButton.setVisible(resetTempoButton.isEnabled());
			keySignatureField.setEnabled(midiLoaded);
			timeSignatureField.setEnabled(midiLoaded);
			tripletCheckBox.setEnabled(midiLoaded);

			updateButtonsPending = false;
		}
	};

	private void updateButtons(boolean immediate)
	{
		if (immediate)
		{
			updateButtonsTask.run();
		}
		else if (!updateButtonsPending)
		{
			updateButtonsPending = true;
			SwingUtilities.invokeLater(updateButtonsTask);
		}
	}

	private boolean updateTitlePending = false;

	private void updateTitle()
	{
		if (!updateTitlePending)
		{
			updateTitlePending = true;
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override public void run()
				{
					updateTitlePending = false;
					String title = MaestroMain.APP_NAME;
					if (abcSong != null)
					{
						if (abcSong.getSaveFile() != null)
						{
							title += " - " + abcSong.getSaveFile().getName();
							if (abcSong.getSourceFile() != null)
								title += " [" + abcSong.getSourceFile().getName() + "]";
						}
						else if (abcSong.getSourceFile() != null)
						{
							title += " - " + abcSong.getSourceFile().getName();
						}

						if (isAbcSongModified())
							title += "*";
					}
					setTitle(title);
				}
			});
		}
	}

	private Listener<AbcPartEvent> abcPartListener = new Listener<AbcPartEvent>()
	{
		@Override public void onEvent(AbcPartEvent e)
		{
			if (e.getProperty() == AbcPartProperty.TRACK_ENABLED)
				updateButtons(false);

			partsList.repaint();

			setAbcSongModified(true);

			if (e.isAbcPreviewRelated() && abcSequencer.isRunning())
				refreshPreviewSequence(false);
		}
	};

	private Listener<AbcSongEvent> abcSongListener = new Listener<AbcSongEvent>()
	{
		@Override public void onEvent(AbcSongEvent e)
		{
			if (abcSong == null || abcSong != e.getSource())
				return;

			int idx;

			switch (e.getProperty())
			{
			case TITLE:
				if (!songTitleField.getText().equals(abcSong.getTitle()))
				{
					songTitleField.setText(abcSong.getTitle());
					songTitleField.select(0, 0);
				}
				break;
			case COMPOSER:
				if (!composerField.getText().equals(abcSong.getComposer()))
				{
					composerField.setText(abcSong.getComposer());
					composerField.select(0, 0);
				}
				break;
			case TRANSCRIBER:
				if (!transcriberField.getText().equals(abcSong.getTranscriber()))
				{
					transcriberFieldListener.setIgnoreChanges(true);
					transcriberField.setText(abcSong.getTranscriber());
					transcriberField.select(0, 0);
					transcriberFieldListener.setIgnoreChanges(false);
				}
				break;

			case TEMPO_FACTOR:
				if (getTempo() != abcSong.getTempoBPM())
					tempoSpinner.setValue(abcSong.getTempoBPM());
				break;
			case TRANSPOSE:
				if (getTranspose() != abcSong.getTranspose())
					transposeSpinner.setValue(abcSong.getTranspose());
				break;
			case KEY_SIGNATURE:
				if (SHOW_KEY_FIELD)
				{
					if (!keySignatureField.getValue().equals(abcSong.getKeySignature()))
						keySignatureField.setValue(abcSong.getKeySignature());
				}
				break;
			case TIME_SIGNATURE:
				if (!timeSignatureField.getValue().equals(abcSong.getTimeSignature()))
					timeSignatureField.setValue(abcSong.getTimeSignature());
				break;
			case TRIPLET_TIMING:
				if (tripletCheckBox.isSelected() != abcSong.isTripletTiming())
					tripletCheckBox.setSelected(abcSong.isTripletTiming());
				break;

			case PART_ADDED:
				e.getPart().addAbcListener(abcPartListener);

				idx = abcSong.getParts().indexOf(e.getPart());
				partsList.setSelectedIndex(idx);
				partsList.ensureIndexIsVisible(idx);
				partsList.repaint();
				updateButtons(false);
				break;

			case BEFORE_PART_REMOVED:
				e.getPart().removeAbcListener(abcPartListener);

				idx = abcSong.getParts().indexOf(e.getPart());
				if (idx > 0)
					partsList.setSelectedIndex(idx - 1);
				else if (abcSong.getParts().size() > 0)
					partsList.setSelectedIndex(0);

				if (abcSong.getParts().size() == 0)
				{
					sequencer.stop();
					partPanel.showInfoMessage(formatInfoMessage("Add a part", "This ABC song has no parts.\n" + //
							"Click the " + newPartButton.getText() + " button to add a new part."));
				}

				if (abcSequencer.isRunning())
					refreshPreviewSequence(false);

				partsList.repaint();
				updateButtons(false);
				break;

			case PART_LIST_ORDER:
				partsList.setSelectedIndex(abcSong.getParts().indexOf(partPanel.getAbcPart()));
				partsList.repaint();
				updateButtons(false);
				break;

			case SKIP_SILENCE_AT_START:
				if (saveSettings.skipSilenceAtStart != abcSong.isSkipSilenceAtStart())
				{
					saveSettings.skipSilenceAtStart = abcSong.isSkipSilenceAtStart();
					saveSettings.saveToPrefs();
				}
				break;

			case EXPORT_FILE:
				// Don't care
				break;
			}

			setAbcSongModified(true);
		}
	};

	private ListDataListener partsListListener = new ListDataListener()
	{
		@Override public void intervalAdded(ListDataEvent e)
		{
			partsList.repaint();
			updateButtons(false);
		}

		@Override public void intervalRemoved(ListDataEvent e)
		{
			partsList.repaint();
			updateButtons(false);
		}

		@Override public void contentsChanged(ListDataEvent e)
		{
			partsList.repaint();
			updateButtons(false);
		}
	};

	private void setAbcSongModified(boolean abcSongModified)
	{
		if (this.abcSongModified != abcSongModified)
		{
			this.abcSongModified = abcSongModified;
			updateTitle();
		}
	}

	private boolean isAbcSongModified()
	{
		return abcSong != null && abcSongModified;
	}

	public int getTranspose()
	{
		return (Integer) transposeSpinner.getValue();
	}

	public int getTempo()
	{
		return (Integer) tempoSpinner.getValue();
	}

	private boolean closeSong()
	{
		sequencer.stop();
		abcSequencer.stop();

		boolean promptSave = isAbcSongModified() && (saveSettings.promptSaveNewSong || abcSong.getSaveFile() != null);
		if (promptSave)
		{
			String message;
			if (abcSong.getSaveFile() == null)
				message = "Do you want to save this new song?";
			else
				message = "Do you want to save changes to \"" + abcSong.getSaveFile().getName() + "\"?";

			int result = JOptionPane.showConfirmDialog(this, message, "Save Changes", JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE, IconLoader.getImageIcon("msxfile_32.png"));
			if (result == JOptionPane.CANCEL_OPTION)
				return false;

			if (result == JOptionPane.YES_OPTION)
			{
				if (!save())
					return false;
			}
		}

		if (abcSong != null)
		{
			abcSong.getParts().getListModel().removeListDataListener(partsListListener);
			abcSong.discard();
			abcSong = null;
		}

		allowOverwriteSaveFile = false;
		allowOverwriteExportFile = false;

		sequencer.clearSequence();
		abcSequencer.clearSequence();
		sequencer.reset(true);
		abcSequencer.reset(false);
		abcSequencer.setTempoFactor(1.0f);
		abcPreviewStartTick = 0;

		songTitleField.setText("");
		composerField.setText("");
		transposeSpinner.setValue(0);
		tempoSpinner.setValue(MidiConstants.DEFAULT_TEMPO_BPM);
		keySignatureField.setValue(KeySignature.C_MAJOR);
		timeSignatureField.setValue(TimeSignature.FOUR_FOUR);
		tripletCheckBox.setSelected(false);

		midiBarLabel.setBarNumberCache(null);
		abcBarLabel.setBarNumberCache(null);
		abcBarLabel.setInitialOffsetTick(abcPreviewStartTick);
		abcPositionLabel.setInitialOffsetTick(abcPreviewStartTick);

		setAbcSongModified(false);
		updateButtons(false);
		updateTitle();

		return true;
	}

	public void openFile(File file)
	{
		if (!closeSong())
			return;

		file = Util.resolveShortcut(file);
		allowOverwriteSaveFile = false;
		allowOverwriteExportFile = false;
		setAbcSongModified(false);

		try
		{
			abcSong = new AbcSong(file, partAutoNumberer, partNameTemplate, openFileResolver);
			abcSong.addSongListener(abcSongListener);
			for (AbcPart part : abcSong.getParts())
			{
				part.addAbcListener(abcPartListener);
			}

			songTitleField.setText(abcSong.getTitle());
			songTitleField.select(0, 0);
			composerField.setText(abcSong.getComposer());
			composerField.select(0, 0);

			if (abcSong.isFromAbcFile() || abcSong.isFromXmlFile())
			{
				transcriberFieldListener.setIgnoreChanges(true);
				transcriberField.setText(abcSong.getTranscriber());
				transcriberField.select(0, 0);
				transcriberFieldListener.setIgnoreChanges(false);
			}
			else
			{
				abcSong.setTranscriber(transcriberField.getText());
			}

			transposeSpinner.setValue(abcSong.getTranspose());
			tempoSpinner.setValue(abcSong.getTempoBPM());
			keySignatureField.setValue(abcSong.getKeySignature());
			timeSignatureField.setValue(abcSong.getTimeSignature());
			tripletCheckBox.setSelected(abcSong.isTripletTiming());

			SequenceInfo sequenceInfo = abcSong.getSequenceInfo();
			sequencer.setSequence(sequenceInfo.getSequence());
			sequencer.setTickPosition(sequenceInfo.calcFirstNoteTick());
			midiBarLabel.setBarNumberCache(sequenceInfo.getDataCache());

			partsList.setModel(abcSong.getParts().getListModel());
			abcSong.getParts().getListModel().addListDataListener(partsListListener);

			if (abcSong.isFromXmlFile())
			{
				allowOverwriteSaveFile = true;
			}

			if (abcSong.isFromAbcFile() || abcSong.isFromXmlFile())
			{
				if (abcSong.getParts().isEmpty())
				{
					updateButtons(true);
					abcSong.createNewPart();
				}
				else
				{
					partsList.setSelectedIndex(0);
					updatePreviewMode(true, true);
					updateButtons(true);
				}
			}
			else
			{
				updateButtons(true);
				if (abcSong.getParts().isEmpty())
				{
					abcSong.createNewPart();
				}
				sequencer.start();
			}

			abcSong.setSkipSilenceAtStart(saveSettings.skipSilenceAtStart);

			setAbcSongModified(false);
			updateTitle();
		}
		catch (SAXParseException e)
		{
			String message = e.getMessage();
			if (e.getLineNumber() >= 0)
			{
				message += "\nLine " + e.getLineNumber();
				if (e.getColumnNumber() >= 0)
					message += ", column " + e.getColumnNumber();
			}

			partPanel.showInfoMessage(formatErrorMessage("Could not open " + file.getName(), message));
		}
		catch (InvalidMidiDataException | IOException | ParseException | SAXException e)
		{
			partPanel.showInfoMessage(formatErrorMessage("Could not open " + file.getName(), e.getMessage()));
		}
	}

	/** Used when the MIDI file in a Maestro song project can't be loaded. */
	private FileResolver openFileResolver = new FileResolver()
	{
		@Override public File locateFile(File original, String message)
		{
			message += "\n\nWould you like to try to locate the file?";
			return resolveHelper(original, message);
		}

		@Override public File resolveFile(File original, String message)
		{
			message += "\n\nWould you like to pick a different file?";
			return resolveHelper(original, message);
		}

		private File resolveHelper(File original, String message)
		{
			int result = JOptionPane.showConfirmDialog(ProjectFrame.this, message, "Failed to open file",
					JOptionPane.OK_CANCEL_OPTION);

			File alternateFile = null;
			if (result == JOptionPane.OK_OPTION)
			{
				JFileChooser jfc = new JFileChooser();
				if (original != null)
					jfc.setSelectedFile(original);

				if (jfc.showOpenDialog(ProjectFrame.this) == JFileChooser.APPROVE_OPTION)
					alternateFile = jfc.getSelectedFile();
			}

			return alternateFile;
		}
	};

	private static String formatInfoMessage(String title, String message)
	{
		return "<html><h3>" + Util.htmlEscape(title) + "</h3>" + Util.htmlEscape(message).replace("\n", "<br>")
				+ "<h3>&nbsp;</h3></html>";
	}

	private static String formatErrorMessage(String title, String message)
	{
		return "<html><h3><font color=\"" + ColorTable.PANEL_TEXT_ERROR.getHtml() + "\">" + Util.htmlEscape(title)
				+ "</font></h3>" + Util.htmlEscape(message).replace("\n", "<br>") + "<h3>&nbsp;</h3></html>";
	}

	private void updatePreviewMode(boolean abcPreviewModeNew)
	{
		SequencerWrapper oldSequencer = abcPreviewMode ? abcSequencer : sequencer;
		updatePreviewMode(abcPreviewModeNew, oldSequencer.isRunning());
	}

	private void updatePreviewMode(boolean newAbcPreviewMode, boolean running)
	{
		boolean runningNow = abcPreviewMode ? abcSequencer.isRunning() : sequencer.isRunning();

		if (newAbcPreviewMode != abcPreviewMode || runningNow != running)
		{
			if (running && newAbcPreviewMode)
			{
				if (!refreshPreviewSequence(true))
				{
					running = false;

					SequencerWrapper oldSequencer = abcPreviewMode ? abcSequencer : sequencer;
					oldSequencer.stop();
				}
			}

			midiPositionLabel.setVisible(!newAbcPreviewMode);
			abcPositionLabel.setVisible(newAbcPreviewMode);
			midiBarLabel.setVisible(!newAbcPreviewMode);
			abcBarLabel.setVisible(newAbcPreviewMode);
			midiModeRadioButton.setSelected(!newAbcPreviewMode);
			abcModeRadioButton.setSelected(newAbcPreviewMode);

			SequencerWrapper newSequencer = newAbcPreviewMode ? abcSequencer : sequencer;
			newSequencer.setRunning(running);

			abcPreviewMode = newAbcPreviewMode;

			partPanel.setAbcPreviewMode(abcPreviewMode);
			updateButtons(false);
		}
	}

	private boolean refreshPreviewPending = false;

	private class RefreshPreviewTask implements Runnable
	{
		@Override public void run()
		{
			if (refreshPreviewPending)
			{
				if (!refreshPreviewSequence(true))
					abcSequencer.stop();
			}
		}
	}

	private boolean refreshPreviewSequence(boolean immediate)
	{
		if (!immediate)
		{
			if (!refreshPreviewPending)
			{
				refreshPreviewPending = true;
				SwingUtilities.invokeLater(new RefreshPreviewTask());
			}
			return true;
		}

		refreshPreviewPending = false;

		if (abcSong == null)
		{
			abcPreviewStartTick = 0;
			abcPreviewTempoFactor = 1.0f;
			abcSequencer.clearSequence();
			abcSequencer.reset(false);
			abcBarLabel.setBarNumberCache(null);
			abcBarLabel.setInitialOffsetTick(abcPreviewStartTick);
			abcPositionLabel.setInitialOffsetTick(abcPreviewStartTick);
			return false;
		}

		try
		{
			abcSong.setSkipSilenceAtStart(saveSettings.skipSilenceAtStart);
			AbcExporter exporter = abcSong.getAbcExporter();
			SequenceInfo previewSequenceInfo = SequenceInfo.fromAbcParts(exporter, !failedToLoadLotroInstruments);

			long tick = sequencer.getTickPosition();
			abcPreviewStartTick = exporter.getExportStartTick();
			abcPreviewTempoFactor = abcSequencer.getTempoFactor();
			abcBarLabel.setBarNumberCache(exporter.getTimingInfo());
			abcBarLabel.setInitialOffsetTick(abcPreviewStartTick);
			abcPositionLabel.setInitialOffsetTick(abcPreviewStartTick);

			boolean running = abcSequencer.isRunning();
			abcSequencer.reset(false);
			abcSequencer.setSequence(previewSequenceInfo.getSequence());

			if (tick < abcPreviewStartTick)
				tick = abcPreviewStartTick;

			if (tick >= abcSequencer.getTickLength())
			{
				tick = 0;
				running = false;
			}

			if (running && sequencer.isRunning())
				sequencer.stop();

			abcSequencer.setTickPosition(tick);
			abcSequencer.setRunning(running);
		}
		catch (InvalidMidiDataException e)
		{
			sequencer.stop();
			abcSequencer.stop();
			JOptionPane.showMessageDialog(ProjectFrame.this, e.getMessage(), "Error previewing ABC",
					JOptionPane.WARNING_MESSAGE);
			return false;
		}
		catch (AbcConversionException e)
		{
			sequencer.stop();
			abcSequencer.stop();
			JOptionPane.showMessageDialog(ProjectFrame.this, e.getMessage(), "Error previewing ABC",
					JOptionPane.WARNING_MESSAGE);
			return false;
		}

		return true;
	}

	private void commitAllFields()
	{
		try
		{
			partPanel.commitAllFields();
			transposeSpinner.commitEdit();
			tempoSpinner.commitEdit();
			timeSignatureField.commitEdit();
			keySignatureField.commitEdit();
		}
		catch (java.text.ParseException e)
		{
			// Ignore
		}
	}

	private File doSaveDialog(File defaultFile, File allowOverwriteFile, String extension, FileFilter fileFilter)
	{
		JFileChooser jfc = new JFileChooser();
		jfc.setFileFilter(fileFilter);
		jfc.setSelectedFile(defaultFile);

		while (true)
		{
			int result = jfc.showSaveDialog(this);
			if (result != JFileChooser.APPROVE_OPTION || jfc.getSelectedFile() == null)
				return null;

			File selectedFile = jfc.getSelectedFile();
			String fileName = selectedFile.getName();
			int dot = fileName.lastIndexOf('.');
			if (dot <= 0 || !fileName.substring(dot).equalsIgnoreCase(extension))
			{
				fileName += extension;
				selectedFile = new File(selectedFile.getParent(), fileName);
			}

			if (selectedFile.exists() && !selectedFile.equals(allowOverwriteFile))
			{
				int res = JOptionPane.showConfirmDialog(this, "File \"" + fileName + "\" already exists.\n"
						+ "Do you want to replace it?", "Confirm Replace File", JOptionPane.YES_NO_CANCEL_OPTION);
				if (res == JOptionPane.CANCEL_OPTION)
					return null;
				if (res != JOptionPane.YES_OPTION)
					continue;
			}

			return selectedFile;
		}
	}

	private boolean exportAbcAs()
	{
		exportSuccessfulLabel.setVisible(false);

		if (abcSong == null)
		{
			JOptionPane.showMessageDialog(this, "No ABC Song is open", "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		File exportFile = abcSong.getExportFile();
		File allowOverwriteFile = allowOverwriteExportFile ? exportFile : null;

		if (exportFile == null)
		{
			String defaultFolder = Util.getLotroMusicPath(false).getAbsolutePath();
			String folder = prefs.get("exportDialogFolder", defaultFolder);
			if (!new File(folder).exists())
				folder = defaultFolder;

			exportFile = abcSong.getSourceFile();
			if (exportFile == null)
				exportFile = new File(folder, abcSong.getSequenceInfo().getFileName());

			String fileName = exportFile.getName();
			int dot = fileName.lastIndexOf('.');
			if (dot > 0)
				fileName = fileName.substring(0, dot);
			fileName += ".abc";

			exportFile = new File(folder, fileName);
		}

		exportFile = doSaveDialog(exportFile, allowOverwriteFile, ".abc", new ExtensionFileFilter(
				"ABC files (*.abc, *.txt)", "abc", "txt"));

		if (exportFile == null)
		{
			return false;
		}

		prefs.put("exportDialogFolder", exportFile.getAbsoluteFile().getParent());

		abcSong.setExportFile(exportFile);
		allowOverwriteExportFile = true;
		return finishExportAbc();
	}

	private boolean shouldExportAbcAs()
	{
		return saveSettings.showExportFileChooser || !allowOverwriteExportFile || abcSong.getExportFile() == null
				|| !abcSong.getExportFile().exists();
	}

	private boolean exportAbc()
	{
		exportSuccessfulLabel.setVisible(false);
		if (abcSong == null)
		{
			JOptionPane.showMessageDialog(this, "No ABC Song is open", "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		if (shouldExportAbcAs())
			return exportAbcAs();

		return finishExportAbc();
	}

	private boolean finishExportAbc()
	{
		exportSuccessfulLabel.setVisible(false);
		commitAllFields();

		try
		{
			abcSong.exportAbc(abcSong.getExportFile());

			SwingUtilities.invokeLater(new Runnable()
			{
				@Override public void run()
				{
					exportSuccessfulLabel.setText(abcSong.getExportFile().getName());
					exportSuccessfulLabel.setToolTipText("Exported " + abcSong.getExportFile().getName());
					exportSuccessfulLabel.setVisible(true);
					if (exportLabelHideTimer == null)
					{
						exportLabelHideTimer = new Timer(8000, new ActionListener()
						{
							@Override public void actionPerformed(ActionEvent e)
							{
								exportSuccessfulLabel.setVisible(false);
							}
						});
						exportLabelHideTimer.setRepeats(false);
					}
					exportLabelHideTimer.stop();
					exportLabelHideTimer.start();
					onSaveAndExportSettingsChanged();
				}
			});
			return true;
		}
		catch (FileNotFoundException e)
		{
			JOptionPane.showMessageDialog(this, "Failed to create file!\n" + e.getMessage(), "Failed to create file",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		catch (IOException | AbcConversionException e)
		{
			JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}

	private boolean saveAs()
	{
		if (abcSong == null)
		{
			JOptionPane.showMessageDialog(this, "No ABC Song is open", "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		File saveFile = abcSong.getSaveFile();
		File allowOverwriteFile = allowOverwriteSaveFile ? saveFile : null;

		if (saveFile == null)
		{
			String defaultFolder;
			if (abcSong.getExportFile() != null)
				defaultFolder = abcSong.getExportFile().getAbsoluteFile().getParent();
			else
				defaultFolder = Util.getLotroMusicPath(false).getAbsolutePath();

			String folder = prefs.get("saveDialogFolder", defaultFolder);
			if (!new File(folder).exists())
				folder = defaultFolder;

			saveFile = abcSong.getExportFile();
			if (saveFile == null)
				saveFile = abcSong.getSourceFile();
			if (saveFile == null)
				saveFile = new File(folder, abcSong.getSequenceInfo().getFileName());

			String fileName = saveFile.getName();
			int dot = fileName.lastIndexOf('.');
			if (dot > 0)
				fileName = fileName.substring(0, dot);
			fileName += AbcSong.MSX_FILE_EXTENSION;

			saveFile = new File(folder, fileName);
		}

		saveFile = doSaveDialog(saveFile, allowOverwriteFile, AbcSong.MSX_FILE_EXTENSION, new ExtensionFileFilter(
				AbcSong.MSX_FILE_DESCRIPTION_PLURAL + " (*" + AbcSong.MSX_FILE_EXTENSION + ")",
				AbcSong.MSX_FILE_EXTENSION_NO_DOT));

		if (saveFile == null)
			return false;

		prefs.put("saveDialogFolder", saveFile.getAbsoluteFile().getParent());
		abcSong.setSaveFile(saveFile);
		allowOverwriteSaveFile = true;
		return finishSave();
	}

	private boolean save()
	{
		if (abcSong == null)
		{
			JOptionPane.showMessageDialog(this, "No ABC Song is open", "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		if (!allowOverwriteSaveFile || abcSong.getSaveFile() == null || !abcSong.getSaveFile().exists())
		{
			return saveAs();
		}

		return finishSave();
	}

	private boolean finishSave()
	{
		commitAllFields();

		try
		{
			XmlUtil.saveDocument(abcSong.saveToXml(), abcSong.getSaveFile());
		}
		catch (FileNotFoundException e)
		{
			JOptionPane.showMessageDialog(this, "Failed to create file!\n" + e.getMessage(), "Failed to create file",
					JOptionPane.ERROR_MESSAGE);

			return false;
		}
		catch (IOException | TransformerException e)
		{
			JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		setAbcSongModified(false);
		return true;
	}

	/**
	 * Slight modification to JFormattedTextField to select the contents when it receives focus.
	 */
	private static class MyFormattedTextField extends JFormattedTextField
	{
		public MyFormattedTextField(Object value, int columns)
		{
			super(value);
			setColumns(columns);
		}

		@Override protected void processFocusEvent(FocusEvent e)
		{
			super.processFocusEvent(e);
			if (e.getID() == FocusEvent.FOCUS_GAINED)
				selectAll();
		}
	}
}
