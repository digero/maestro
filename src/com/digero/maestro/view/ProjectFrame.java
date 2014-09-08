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
import javax.swing.ImageIcon;
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
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import com.digero.common.icons.IconLoader;
import com.digero.common.midi.IMidiConstants;
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
import com.digero.maestro.util.Listener;

@SuppressWarnings("serial")
public class ProjectFrame extends JFrame implements TableLayoutConstants, ICompileConstants
{
	private static final int HGAP = 4, VGAP = 4;
	private static final double[] LAYOUT_COLS = new double[] { 180, FILL };
	private static final double[] LAYOUT_ROWS = new double[] { FILL };

	private static final int MAX_PARTS = IMidiConstants.CHANNEL_COUNT - 2; // Track 0 is reserved for metadata, and Track 9 is reserved for drums

	private Preferences prefs = Preferences.userNodeForPackage(MaestroMain.class);

	private AbcSong abcSong;

	private boolean allowOverwriteSaveFile = false;
	private NoteFilterSequencerWrapper sequencer;
	private VolumeTransceiver volumeTransceiver;
	private NoteFilterSequencerWrapper abcSequencer;
	private VolumeTransceiver abcVolumeTransceiver;
	private PartAutoNumberer partAutoNumberer;
	private PartNameTemplate partNameTemplate;
	private boolean usingNativeVolume;

	private JPanel content;
	private JTextField songTitleField;
	private JTextField composerField;
	private JTextField transcriberField;
	private JSpinner transposeSpinner;
	private JSpinner tempoSpinner; // TODO convert tempo to percentage
	private JButton resetTempoButton;
	private JFormattedTextField keySignatureField;
	private JFormattedTextField timeSignatureField;
	private JCheckBox tripletCheckBox;
	private JButton exportButton;
	private JMenuItem exportMenuItem;

	private JList<AbcPart> partsList;
	private JButton newPartButton;
	private JButton deletePartButton;

	private PartPanel partPanel;

	private boolean abcPreviewMode = false;
	private JRadioButton abcModeRadioButton;
	private JRadioButton midiModeRadioButton;
	private JButton playButton;
	private JButton stopButton;
	private NativeVolumeBar volumeBar;
	private SongPositionLabel midiPositionLabel;
	private SongPositionLabel abcPositionLabel;
	private BarNumberLabel midiBarLabel;
	private BarNumberLabel abcBarLabel;

	private Icon playIcon;
	private Icon pauseIcon;
	private Icon abcPlayIcon;
	private Icon abcPauseIcon;

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

		String welcomeMessage = formatInfoMessage("Hello Maestro", "Drag and drop a MIDI or ABC file to open it.\n"
				+ "Or use File > Open.");

		partAutoNumberer = new PartAutoNumberer(prefs.node("partAutoNumberer"));
		partNameTemplate = new PartNameTemplate(prefs.node("partNameTemplate"));

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

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		playIcon = new ImageIcon(IconLoader.class.getResource("play_blue.png"));
		pauseIcon = new ImageIcon(IconLoader.class.getResource("pause_blue.png"));
		abcPlayIcon = new ImageIcon(IconLoader.class.getResource("play_yellow.png"));
		abcPauseIcon = new ImageIcon(IconLoader.class.getResource("pause.png"));
		Icon stopIcon = new ImageIcon(IconLoader.class.getResource("stop.png"));

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
		transcriberField.getDocument().addDocumentListener(new PrefsDocumentListener(prefs, "transcriber"));
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
		keySignatureField.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				if (abcSong != null)
					abcSong.setKeySignature(getKeySignature());
			}
		});

		timeSignatureField = new MyFormattedTextField(TimeSignature.FOUR_FOUR, 5);
		timeSignatureField.setToolTipText("<html>Adjust the time signature of the ABC file. "
				+ "This only affects the display, not the sound of the exported file.<br>"
				+ "Examples: 4/4, 3/8, 2/2</html>");
		timeSignatureField.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				if (abcSong != null)
					abcSong.setTimeSignature(getTimeSignature());
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

		tempoSpinner = new JSpinner(new SpinnerNumberModel(IMidiConstants.DEFAULT_TEMPO_BPM /* value */, 8 /* min */,
				960 /* max */, 1 /* step */));
		tempoSpinner.setToolTipText("Tempo in beats per minute");
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
					tempoSpinner.setValue(IMidiConstants.DEFAULT_TEMPO_BPM);
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

		exportButton = new JButton("<html><center><b>Export ABC</b><br>(Ctrl+S)</center></html>");
		exportButton.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				exportAbc();
			}
		});

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
			settingsPanel.add(new JLabel("Tempo:"), "0, " + row);
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
		midiModeRadioButton.setMargin(new Insets(0, 0, 0, 0));

		abcModeRadioButton = new JRadioButton("ABC Preview");
		abcModeRadioButton.addActionListener(modeButtonListener);
		abcModeRadioButton.setMargin(new Insets(0, 0, 0, 0));

		ButtonGroup modeButtonGroup = new ButtonGroup();
		modeButtonGroup.add(abcModeRadioButton);
		modeButtonGroup.add(midiModeRadioButton);

		midiModeRadioButton.setSelected(true);
		abcPreviewMode = abcModeRadioButton.isSelected();

		playButton = new JButton(playIcon);
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
		stopButton.setToolTipText("Stop");
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

		final FileFilterDropListener dropListener = new FileFilterDropListener(false, "mid", "midi", "abc", "txt");
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

		JMenuItem openItem = fileMenu.add(new JMenuItem("Open MIDI or ABC file..."));
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
					openFileChooser.setFileFilter(new ExtensionFileFilter("MIDI and ABC files", "mid", "midi", "abc",
							"txt"));
				}

				int result = openFileChooser.showOpenDialog(ProjectFrame.this);
				if (result == JFileChooser.APPROVE_OPTION)
				{
					openFile(openFileChooser.getSelectedFile());
					prefs.put("openFileChooser.path", openFileChooser.getCurrentDirectory().getAbsolutePath());
				}
			}
		});

		exportMenuItem = fileMenu.add(new JMenuItem("Export ABC..."));
		exportMenuItem.setMnemonic('E');
		exportMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
		exportMenuItem.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				exportAbc();
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
				switch (ProjectFrame.this.getDefaultCloseOperation())
				{
				case EXIT_ON_CLOSE:
					System.exit(0);
					break;
				case DISPOSE_ON_CLOSE:
					setVisible(false);
					dispose();
					break;
				case DO_NOTHING_ON_CLOSE:
				case HIDE_ON_CLOSE:
					setVisible(false);
					break;
				}
			}
		});

		JMenu toolsMenu = menuBar.add(new JMenu(" Tools "));
		toolsMenu.setMnemonic('T');

		JMenuItem settingsItem = toolsMenu.add(new JMenuItem("Options..."));
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
				partNameTemplate);
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
		}
		currentSettingsDialogTab = dialog.getActiveTab();
		dialog.dispose();
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

		public PrefsDocumentListener(Preferences prefs, String prefName)
		{
			this.prefs = prefs;
			this.prefName = prefName;
		}

		private void updatePrefs(Document doc)
		{
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
			Icon curPauseIcon = abcPreviewMode ? abcPauseIcon : pauseIcon;
			playButton.setIcon(curSequencer.isRunning() ? curPauseIcon : curPlayIcon);

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
			exportMenuItem.setEnabled(exportButton.isEnabled());

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

	private Listener<AbcPartEvent> abcPartListener = new Listener<AbcPartEvent>()
	{
		@Override public void onEvent(AbcPartEvent e)
		{
			if (e.getProperty() == AbcPartProperty.TRACK_ENABLED)
				updateButtons(false);

			partsList.repaint();

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
					transcriberField.setText(abcSong.getTranscriber());
					transcriberField.select(0, 0);
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
				if (!getKeySignature().equals(abcSong.getKeySignature()))
					keySignatureField.setValue(abcSong.getKeySignature());
				break;
			case TIME_SIGNATURE:
				if (!getTimeSignature().equals(abcSong.getTimeSignature()))
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
			}
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

	public int getTranspose()
	{
		return (Integer) transposeSpinner.getValue();
	}

	public int getTempo()
	{
		return (Integer) tempoSpinner.getValue();
	}

	public KeySignature getKeySignature()
	{
		if (SHOW_KEY_FIELD)
			return (KeySignature) keySignatureField.getValue();
		else
			return KeySignature.C_MAJOR;
	}

	public TimeSignature getTimeSignature()
	{
		return (TimeSignature) timeSignatureField.getValue();
	}

	private void close()
	{
		if (abcSong != null)
		{
			abcSong.getParts().getListModel().removeListDataListener(partsListListener);
			abcSong.discard();
			abcSong = null;
		}

		allowOverwriteSaveFile = false;

		sequencer.clearSequence();
		abcSequencer.clearSequence();
		sequencer.reset(true);
		abcSequencer.reset(false);
		abcSequencer.setTempoFactor(1.0f);
		abcPreviewStartTick = 0;

		songTitleField.setText("");
		composerField.setText("");
		transposeSpinner.setValue(0);
		tempoSpinner.setValue(IMidiConstants.DEFAULT_TEMPO_BPM);
		keySignatureField.setValue(KeySignature.C_MAJOR);
		timeSignatureField.setValue(TimeSignature.FOUR_FOUR);
		tripletCheckBox.setSelected(false);
		setTitle(MaestroMain.APP_NAME);
		midiBarLabel.setBarNumberCache(null);
		abcBarLabel.setBarNumberCache(null);
		abcBarLabel.setInitialOffsetTick(abcPreviewStartTick);
		abcPositionLabel.setInitialOffsetTick(abcPreviewStartTick);

		updateButtons(false);
	}

	public void openFile(File file)
	{
		close();

		file = Util.resolveShortcut(file);
		allowOverwriteSaveFile = false;

		try
		{
			abcSong = new AbcSong(file, partAutoNumberer, partNameTemplate);
			abcSong.addSongListener(abcSongListener);
			for (AbcPart part : abcSong.getParts())
			{
				part.addAbcListener(abcPartListener);
			}

			songTitleField.setText(abcSong.getTitle());
			songTitleField.select(0, 0);
			composerField.setText(abcSong.getComposer());
			composerField.select(0, 0);

			// TODO handler transcriber better
			abcSong.setTranscriber(transcriberField.getText());

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

			if (abcSong.isFromAbcFile())
			{
				if (abcSong.getParts().isEmpty())
				{
					updateButtons(true);
					abcSong.createNewPart();
				}
				else
				{
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

			setTitle(MaestroMain.APP_NAME + " - " + file.getName());
		}
		catch (InvalidMidiDataException e)
		{
			partPanel.showInfoMessage(formatErrorMessage("Could not open " + file.getName(), e.getMessage()));
		}
		catch (IOException e)
		{
			partPanel.showInfoMessage(formatErrorMessage("Could not open " + file.getName(), e.getMessage()));
		}
		catch (ParseException e)
		{
			partPanel.showInfoMessage(formatErrorMessage("Could not open " + file.getName(), e.getMessage()));
		}
	}

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
			if (abcSong.getParts().size() > MAX_PARTS)
			{
				throw new AbcConversionException("Songs with more than " + MAX_PARTS + " parts cannot be previewed.\n"
						+ "This song currently has " + abcSong.getParts().size() + " parts.");
			}

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

	private void exportAbc()
	{
		if (abcSong == null)
			return;

		JFileChooser jfc = new JFileChooser();
		String fileName;
		int dot;
		File origSaveFile = abcSong.getExportFile();

		if (abcSong.getExportFile() == null)
		{
			fileName = abcSong.getSequenceInfo().getFileName();
			dot = fileName.lastIndexOf('.');
			if (dot > 0)
				fileName = fileName.substring(0, dot);
			fileName = fileName.replace(' ', '_') + ".abc";

			abcSong.setExportFile(new File(Util.getLotroMusicPath(false).getAbsolutePath() + "/" + fileName));
		}
		jfc.setSelectedFile(abcSong.getExportFile());

		int result = jfc.showSaveDialog(this);
		if (result != JFileChooser.APPROVE_OPTION || jfc.getSelectedFile() == null)
			return;

		fileName = jfc.getSelectedFile().getName();
		dot = fileName.lastIndexOf('.');
		if (dot <= 0 || !fileName.substring(dot).equalsIgnoreCase(".abc"))
			fileName += ".abc";

		File saveFileTmp = new File(jfc.getSelectedFile().getParent(), fileName);
		if (saveFileTmp.exists() && (!saveFileTmp.equals(origSaveFile) || !allowOverwriteSaveFile))
		{
			int res = JOptionPane.showConfirmDialog(this, "File " + fileName + " already exists. Overwrite?",
					"Confirm Overwrite", JOptionPane.YES_NO_OPTION);
			if (res != JOptionPane.YES_OPTION)
				return;
		}
		abcSong.setExportFile(saveFileTmp);
		allowOverwriteSaveFile = true;

		try
		{
			abcSong.exportAbc(abcSong.getExportFile());
		}
		catch (FileNotFoundException e)
		{
			JOptionPane.showMessageDialog(this, "Failed to create file!\n" + e.getMessage(), "Failed to create file",
					JOptionPane.ERROR_MESSAGE);
		}
		catch (IOException e)
		{
			JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
		catch (AbcConversionException e)
		{
			JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
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
