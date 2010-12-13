package com.digero.abcplayer;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;
import javax.sound.midi.Transmitter;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import com.digero.abcplayer.AbcToMidi.AbcInfo;
import com.digero.common.abc.LotroInstrument;
import com.digero.common.icons.IconLoader;
import com.digero.common.midi.IMidiConstants;
import com.digero.common.midi.SequencerEvent;
import com.digero.common.midi.SequencerListener;
import com.digero.common.midi.SequencerProperty;
import com.digero.common.midi.SequencerWrapper;
import com.digero.common.midi.SynthesizerFactory;
import com.digero.common.util.ExtensionFileFilter;
import com.digero.common.util.FileFilterDropListener;
import com.digero.common.util.LotroParseException;
import com.digero.common.util.ParseException;
import com.digero.common.util.Util;
import com.digero.common.util.Version;
import com.digero.common.view.SongPositionBar;
import com.digero.common.view.SongPositionLabel;
import com.digero.common.view.TempoBar;

public class AbcPlayer extends JFrame implements TableLayoutConstants, IMidiConstants {
	private static final ExtensionFileFilter ABC_FILE_FILTER = new ExtensionFileFilter("ABC Files", "abc", "txt");
	static final String APP_NAME = "ABC Player";
	private static final String APP_NAME_LONG = APP_NAME + " for The Lord of the Rings Online";
	private static final String APP_URL = "http://lotro.acasylum.com/abcplayer/";
	private static final String LAME_URL = "http://lame.sourceforge.net/";
	private static final Version APP_VERSION = new Version(1, 1, 1);

	private static AbcPlayer mainWindow = null;

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e) {}

		mainWindow = new AbcPlayer();
		mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainWindow.setVisible(true);
		mainWindow.openSongFromCommandLine(args);
		try {
			ready();
		}
		catch (UnsatisfiedLinkError err) {
			// Ignore (we weren't started via WinRun4j)
		}
	}

	/** Tells the WinRun4J launcher that we're ready to accept activate() calls. */
	public static native void ready();

	/** A new activation (a.k.a. a file was opened) */
	public static void activate(String[] args) {
		mainWindow.openSongFromCommandLine(args);
	}

	public static void execute(String cmdLine) {
		mainWindow.openSongFromCommandLine(new String[] {
			cmdLine
		});
	}

	private SequencerWrapper sequencer;
	private Synthesizer synth;
	private Transmitter transmitter;
	private Receiver receiver;
	private boolean useLotroInstruments = true;

	private JPanel content;

	private JLabel titleLabel;

	private TrackListPanel trackListPanel;

	private SongPositionBar songPositionBar;
	private SongPositionLabel songPositionLabel;
	private JLabel barCountLabel;
	private JLabel tempoLabel;
	private TempoBar tempoBar;

	private ImageIcon playIcon, pauseIcon, stopIcon;
	private JButton playButton, stopButton;

	private JCheckBoxMenuItem lotroErrorsMenuItem;
	private JCheckBoxMenuItem stereoMenuItem;

	private JFileChooser openFileDialog;
	private JFileChooser saveFileDialog;
	private JFileChooser exportFileDialog;

	private Map<Integer, LotroInstrument> instrumentOverrideMap = new HashMap<Integer, LotroInstrument>();
	private List<FileAndData> abcData;
	private AbcInfo abcInfo = new AbcInfo();

	private Preferences prefs = Preferences.userNodeForPackage(AbcPlayer.class);
	private Preferences windowPrefs = prefs.node("window");

	private boolean isExporting = false;

	public AbcPlayer() {
		super(APP_NAME);

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				if (synth != null)
					synth.close();
				if (receiver != null)
					receiver.close();
				if (transmitter != null)
					transmitter.close();
				if (sequencer != null)
					sequencer.close();
			}
		});

		try {
			List<Image> icons = new ArrayList<Image>();
			icons.add(ImageIO.read(IconLoader.class.getResourceAsStream("abcplayer_16.png")));
			icons.add(ImageIO.read(IconLoader.class.getResourceAsStream("abcplayer_32.png")));
			setIconImages(icons);
		}
		catch (Exception ex) {
			// Ignore
		}

		FileFilterDropListener dropListener = new FileFilterDropListener(true, "abc", "txt");
		dropListener.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				FileFilterDropListener l = (FileFilterDropListener) e.getSource();
				boolean append = (l.getDropEvent().getDropAction() == DnDConstants.ACTION_COPY);
				SwingUtilities.invokeLater(new OpenSongRunnable(append, l.getDroppedFiles().toArray(new File[0])));
			}
		});
		new DropTarget(this, dropListener);

		try {
			Sequencer seqTmp = MidiSystem.getSequencer(false);
			transmitter = seqTmp.getTransmitter();
			synth = null;

			if (useLotroInstruments) {
				try {
					synth = SynthesizerFactory.getLotroSynthesizer();
					if (synth == null)
						throw new IOException("Could not get synthesizer");
					else
						receiver = synth.getReceiver();
				}
				catch (IOException e) {
					JOptionPane.showMessageDialog(this, "There was an error loading the LotRO instrument sounds.\n"
							+ "Playback will use MIDI instruments instead "
							+ "(drums do not sound good in this mode).\n\nError details:\n" + e.getMessage(),
							"Failed to load LotRO instruments", JOptionPane.ERROR_MESSAGE);

					if (synth != null)
						synth.close();
					if (receiver != null)
						receiver.close();
					synth = null;
					useLotroInstruments = false;
				}
			}

			if (!useLotroInstruments) {
				receiver = MidiSystem.getReceiver();
			}
			transmitter.setReceiver(receiver);

			sequencer = new SequencerWrapper(seqTmp, transmitter, receiver);
			sequencer.open();
		}
		catch (InvalidMidiDataException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "MIDI error", JOptionPane.ERROR_MESSAGE);
			throw new RuntimeException(e);
		}
		catch (MidiUnavailableException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "MIDI error", JOptionPane.ERROR_MESSAGE);
			throw new RuntimeException(e);
		}

		content = new JPanel(new TableLayout(new double[] { // Columns
						4, FILL, 4
				}, new double[] { // Rows
						PREFERRED, 0, FILL, 8, PREFERRED
				}));
		setContentPane(content);

		titleLabel = new JLabel(" ");
		Font f = titleLabel.getFont();
		titleLabel.setFont(f.deriveFont(Font.BOLD, f.getSize() + 1));
		titleLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

		trackListPanel = new TrackListPanel();
		JScrollPane trackListScroller = new JScrollPane(trackListPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		trackListScroller.getVerticalScrollBar().setUnitIncrement(TRACKLIST_ROWHEIGHT);

		JPanel controlPanel = new JPanel(new TableLayout(new double[] {
				4, 0.25, PREFERRED, 0.25, 4, PREFERRED, 4, PREFERRED, 4, TempoBar.WIDTH, 0.50, 4, PREFERRED, 4
		}, new double[] {
				4, PREFERRED, 4, PREFERRED, 4
		}));

		songPositionBar = new SongPositionBar(sequencer);
		songPositionLabel = new SongPositionLabel(sequencer);
		barCountLabel = new JLabel("0/0");
		barCountLabel.setToolTipText("Bar number");

		try {
			playIcon = new ImageIcon(ImageIO.read(IconLoader.class.getResourceAsStream("play.png")));
			pauseIcon = new ImageIcon(ImageIO.read(IconLoader.class.getResourceAsStream("pause.png")));
			stopIcon = new ImageIcon(ImageIO.read(IconLoader.class.getResourceAsStream("stop.png")));
		}
		catch (IOException e1) {
			throw new RuntimeException(e1);
		}

		playButton = new JButton(playIcon);
		playButton.setEnabled(false);
		playButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (sequencer.isRunning())
					sequencer.stop();
				else
					sequencer.start();
			}
		});

		stopButton = new JButton(stopIcon);
		stopButton.setEnabled(false);
		stopButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				stop();
			}
		});

		tempoBar = new TempoBar(sequencer);

		tempoLabel = new JLabel();
		updateTempoLabel();
		JPanel tempoPanel = new JPanel(new BorderLayout());
		tempoPanel.add(tempoLabel, BorderLayout.NORTH);
		tempoPanel.add(tempoBar, BorderLayout.CENTER);

		controlPanel.add(songPositionBar, "1, 1, 10, 1");
		controlPanel.add(songPositionLabel, "12, 1");
		controlPanel.add(playButton, "5, 3");
		controlPanel.add(stopButton, "7, 3");
		controlPanel.add(tempoPanel, "2, 3, f, c");
		controlPanel.add(barCountLabel, "10, 3, 12, 3, r, t");

		sequencer.addChangeListener(new SequencerListener() {
			public void propertyChanged(SequencerEvent evt) {
				updateButtonStates();
				SequencerProperty p = evt.getProperty();
				if (barCountLabel.isVisible() && p.isInMask(SequencerProperty.THUMB_POSITION_MASK)) {
					updateBarCountLabel();
				}
				if (p.isInMask(SequencerProperty.TEMPO.mask | SequencerProperty.SEQUENCE.mask)) {
					updateTempoLabel();
				}
			}
		});

		add(titleLabel, "0, 0, 2, 0");
		add(trackListScroller, "0, 2, 2, 2");
		add(controlPanel, "1, 4");

		initMenu();

		updateButtonStates();
		initializeWindowBounds();
	}

	private void updateTempoLabel() {
		float tempo = sequencer.getTempoFactor();
		int t = (int) Math.round(tempo * 100);
//		int bpm = (int) Math.round(tempo * (abcInfo.isEmpty() ? 120 : abcInfo.getTempoBPM()));
		tempoLabel.setText("Tempo: " + t + "%");
	}

	private void updateBarCountLabel() {
		int bar = abcInfo.getBarNumber(sequencer.getThumbTick()) + 1;
		barCountLabel.setText(bar + "/" + abcInfo.getBarCount());
	}

	private void initializeWindowBounds() {
		setMinimumSize(new Dimension(320, 168));

		Dimension mainScreen = Toolkit.getDefaultToolkit().getScreenSize();

		int width = windowPrefs.getInt("width", 450);
		int height = windowPrefs.getInt("height", 282);
		int x = windowPrefs.getInt("x", (mainScreen.width - width) / 2);
		int y = windowPrefs.getInt("y", (mainScreen.height - height) / 2);

		// Handle the case where the window was last saved on
		// a screen that is no longer connected
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gs = ge.getScreenDevices();
		Rectangle onScreen = null;
		for (int i = 0; i < gs.length; i++) {
			Rectangle monitorBounds = gs[i].getDefaultConfiguration().getBounds();
			if (monitorBounds.intersects(x, y, width, height)) {
				onScreen = monitorBounds;
				break;
			}
		}
		if (onScreen == null) {
			x = (mainScreen.width - width) / 2;
			y = (mainScreen.height - height) / 2;
		}
		else {
			if (x < onScreen.x)
				x = onScreen.x;
			else if (x + width > onScreen.x + onScreen.width)
				x = onScreen.x + onScreen.width - width;

			if (y < onScreen.y)
				y = onScreen.y;
			else if (y + height > onScreen.y + onScreen.height)
				y = onScreen.y + onScreen.height - height;
		}

		setBounds(x, y, width, height);

		int maximized = windowPrefs.getInt("maximized", 0);
		setExtendedState((getExtendedState() & ~JFrame.MAXIMIZED_BOTH) | maximized);

		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				if ((getExtendedState() & JFrame.MAXIMIZED_BOTH) == 0) {
					windowPrefs.putInt("width", getWidth());
					windowPrefs.putInt("height", getHeight());
				}
			}

			@Override
			public void componentMoved(ComponentEvent e) {
				if ((getExtendedState() & JFrame.MAXIMIZED_BOTH) == 0) {
					windowPrefs.putInt("x", getX());
					windowPrefs.putInt("y", getY());
				}
			}
		});

		addWindowStateListener(new WindowStateListener() {
			@Override
			public void windowStateChanged(WindowEvent e) {
				windowPrefs.putInt("maximized", e.getNewState() & JFrame.MAXIMIZED_BOTH);
			}
		});
	}

	private void initMenu() {
		JMenuBar mainMenu = new JMenuBar();
		setJMenuBar(mainMenu);

		JMenu fileMenu = mainMenu.add(new JMenu(" File "));
		fileMenu.setMnemonic(KeyEvent.VK_F);

		JMenuItem open = fileMenu.add(new JMenuItem("Open ABC file(s)..."));
		open.setMnemonic(KeyEvent.VK_O);
		open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
		open.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				openSongDialog();
			}
		});

		JMenuItem openAppend = fileMenu.add(new JMenuItem("Append ABC file(s)..."));
		openAppend.setMnemonic(KeyEvent.VK_D);
		openAppend.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK
				| InputEvent.SHIFT_DOWN_MASK));
		openAppend.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				appendSongDialog();
			}
		});

		final JMenuItem pasteMenuItem = fileMenu.add(new JMenuItem("Open from clipboard"));
		pasteMenuItem.setMnemonic(KeyEvent.VK_P);
		pasteMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK));
		pasteMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ArrayList<File> files = new ArrayList<File>();
				if (getFileListFromClipboard(files)) {
					openSong(files.toArray(new File[files.size()]));
					return;
				}

				ArrayList<String> lines = new ArrayList<String>();
				if (getAbcDataFromClipboard(lines, false)) {
					List<FileAndData> filesData = new ArrayList<FileAndData>();
					filesData.add(new FileAndData(new File("[Clipboard]"), lines));
					openSong(filesData);
					return;
				}

				Toolkit.getDefaultToolkit().beep();
			}
		});

		final JMenuItem pasteAppendMenuItem = fileMenu.add(new JMenuItem("Append from clipboard"));
		pasteAppendMenuItem.setMnemonic(KeyEvent.VK_N);
		pasteAppendMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK
				| InputEvent.SHIFT_DOWN_MASK));
		pasteAppendMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ArrayList<File> files = new ArrayList<File>();
				if (getFileListFromClipboard(files)) {
					appendSong(files.toArray(new File[files.size()]));
					return;
				}

				ArrayList<String> lines = new ArrayList<String>();
				if (getAbcDataFromClipboard(lines, true)) {
					List<FileAndData> data = new ArrayList<FileAndData>();
					data.add(new FileAndData(new File("[Clipboard]"), lines));
					appendSong(data);
					return;
				}

				Toolkit.getDefaultToolkit().beep();
			}
		});

		fileMenu.addSeparator();

		final JMenuItem saveMenuItem = fileMenu.add(new JMenuItem("Save a copy as ABC..."));
		saveMenuItem.setMnemonic(KeyEvent.VK_S);
		saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK
				| InputEvent.SHIFT_DOWN_MASK));
		saveMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!sequencer.isLoaded()) {
					Toolkit.getDefaultToolkit().beep();
					return;
				}
				saveSongDialog();
			}
		});

		final JMenuItem exportWavMenuItem = fileMenu.add(new JMenuItem("Save as Wave file..."));
		exportWavMenuItem.setMnemonic(KeyEvent.VK_E);
		exportWavMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK
				| InputEvent.SHIFT_DOWN_MASK));
		exportWavMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!sequencer.isLoaded() || isExporting) {
					Toolkit.getDefaultToolkit().beep();
					return;
				}
				exportWav();
			}
		});

		final JMenuItem exportMp3MenuItem = fileMenu.add(new JMenuItem("Save as MP3 file..."));
		exportMp3MenuItem.setMnemonic(KeyEvent.VK_M);
		exportMp3MenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK));
		exportMp3MenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!sequencer.isLoaded() || isExporting) {
					Toolkit.getDefaultToolkit().beep();
					return;
				}
				exportMp3();
			}
		});

		fileMenu.addSeparator();

		JMenuItem exit = fileMenu.add(new JMenuItem("Exit"));
		exit.setMnemonic(KeyEvent.VK_X);
		exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_DOWN_MASK));
		exit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});

		fileMenu.addMenuListener(new MenuListener() {
			@Override
			public void menuSelected(MenuEvent e) {
				boolean pasteEnabled = getFileListFromClipboard(null);
				pasteMenuItem.setEnabled(pasteEnabled || getAbcDataFromClipboard(null, false));
				pasteAppendMenuItem.setEnabled(pasteEnabled || getAbcDataFromClipboard(null, true));

				boolean saveEnabled = sequencer.isLoaded();
				saveMenuItem.setEnabled(saveEnabled);
				exportWavMenuItem.setEnabled(saveEnabled && !isExporting);
				exportMp3MenuItem.setEnabled(saveEnabled && !isExporting);
			}

			@Override
			public void menuDeselected(MenuEvent e) {
				menuCanceled(e);
			}

			@Override
			public void menuCanceled(MenuEvent e) {
				pasteMenuItem.setEnabled(true);
				pasteAppendMenuItem.setEnabled(true);

				saveMenuItem.setEnabled(true);
				exportWavMenuItem.setEnabled(true);
				exportMp3MenuItem.setEnabled(true);
			}
		});

		JMenu toolsMenu = mainMenu.add(new JMenu(" Tools "));
		toolsMenu.setMnemonic(KeyEvent.VK_T);

		toolsMenu.add(lotroErrorsMenuItem = new JCheckBoxMenuItem("Ignore LotRO-specific errors"));
		lotroErrorsMenuItem.setSelected(prefs.getBoolean("ignoreLotroErrors", false));
		lotroErrorsMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				prefs.putBoolean("ignoreLotroErrors", lotroErrorsMenuItem.isSelected());
			}
		});

		toolsMenu.add(stereoMenuItem = new JCheckBoxMenuItem("Stereo pan in multi-part songs"));
		stereoMenuItem.setToolTipText("<html>Separates the parts of a multi-part song by <br>"
				+ "panning them towards the left or right speaker.</html>");
		stereoMenuItem.setSelected(prefs.getBoolean("stereoMenuItem", true));
		stereoMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				prefs.putBoolean("stereoMenuItem", stereoMenuItem.isSelected());
				refreshSequence();
			}
		});

		toolsMenu.addSeparator();

		JMenuItem about = toolsMenu.add(new JMenuItem("About " + APP_NAME + "..."));
		about.setMnemonic(KeyEvent.VK_A);
		about.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ImageIcon aboutIcon;
				try {
					aboutIcon = new ImageIcon(ImageIO.read(IconLoader.class.getResourceAsStream("abcplayer_64.png")));
				}
				catch (IOException e1) {
					throw new RuntimeException(e1);
				}
				JLabel aboutMessage = new JLabel("<html>" //
						+ APP_NAME_LONG + "<br>" //
						+ "Version " + APP_VERSION + "<br>" //
						+ "Created by Digero of Landroval<br>" //
						+ "Copyright &copy; 2010 Ben Howell<br>" //
						+ "<a href='" + APP_URL + "'>" + APP_URL + "</a><br>" //
						+ "<br>" //
						+ "No affiliation with Turbine, Inc. or Warner Bros." //
						+ "</html>");
				aboutMessage.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				aboutMessage.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent e) {
						if (e.getButton() == MouseEvent.BUTTON1) {
							Util.openURL(APP_URL);
						}
					}
				});
				String aboutTitle = "About " + APP_NAME;
				JOptionPane.showMessageDialog(AbcPlayer.this, aboutMessage, aboutTitle,
						JOptionPane.INFORMATION_MESSAGE, aboutIcon);
			}
		});
	}

	private boolean getAbcDataFromClipboard(ArrayList<String> data, boolean checkContents) {
		if (data != null)
			data.clear();

		Transferable xfer = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
		if (xfer == null || !xfer.isDataFlavorSupported(DataFlavor.stringFlavor))
			return false;

		String text;
		try {
			text = (String) xfer.getTransferData(DataFlavor.stringFlavor);
		}
		catch (UnsupportedFlavorException e) {
			return false;
		}
		catch (IOException e) {
			return false;
		}

		if (!checkContents && data == null)
			return true;

		StringTokenizer tok = new StringTokenizer(text, "\r\n");
		int i = 0;
		boolean isValid = !checkContents;
		while (tok.hasMoreTokens()) {
			String line = tok.nextToken();

			if (!isValid) {
				if (line.startsWith("X:") || line.startsWith("x:")) {
					isValid = true;
					if (data == null)
						break;
				}
				else {
					String lineTrim = line.trim();
					// If we find a line that's not a comment before the 
					// X: line, then this isn't an ABC file
					if (lineTrim.length() > 0 && !lineTrim.startsWith("%")) {
						isValid = false;
						break;
					}
				}
			}

			if (data != null)
				data.add(line);
			else if (i >= 100)
				break;

			i++;
		}

		if (!isValid && data != null)
			data.clear();

		return isValid;
	}

	@SuppressWarnings("unchecked")
	private boolean getFileListFromClipboard(ArrayList<File> data) {
		if (data != null)
			data.clear();

		Transferable xfer = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
		if (xfer == null || !xfer.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
			return false;

		List<File> fileList;
		try {
			fileList = (List<File>) xfer.getTransferData(DataFlavor.javaFileListFlavor);
		}
		catch (UnsupportedFlavorException e) {
			return false;
		}
		catch (IOException e) {
			return false;
		}

		if (fileList.size() == 0)
			return false;

		for (File file : fileList) {
			if (!ABC_FILE_FILTER.accept(file)) {
				if (data != null)
					data.clear();
				return false;
			}

			if (data != null)
				data.add(file);
		}

		return true;
	}

	private void initOpenFileDialog() {
		if (openFileDialog == null) {
			openFileDialog = new JFileChooser(prefs.get("openFileDialog.currentDirectory", Util
					.getLotroMusicPath(false).getAbsolutePath()));

			openFileDialog.setMultiSelectionEnabled(true);
			openFileDialog.setFileFilter(ABC_FILE_FILTER);
		}
	}

	private void openSongDialog() {
		initOpenFileDialog();

		int result = openFileDialog.showOpenDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			prefs.put("openFileDialog.currentDirectory", openFileDialog.getCurrentDirectory().getAbsolutePath());

			openSong(openFileDialog.getSelectedFiles());
		}
	}

	private void appendSongDialog() {
		if (this.abcData == null || this.abcData.size() == 0) {
			openSongDialog();
			return;
		}

		initOpenFileDialog();
		int result = openFileDialog.showOpenDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			prefs.put("openFileDialog.currentDirectory", openFileDialog.getCurrentDirectory().getAbsolutePath());

			appendSong(openFileDialog.getSelectedFiles());
		}
	}

	private void saveSongDialog() {
		if (this.abcData == null || this.abcData.size() == 0) {
			Toolkit.getDefaultToolkit().beep();
			return;
		}

		if (saveFileDialog == null) {
			saveFileDialog = new JFileChooser(prefs.get("saveFileDialog.currentDirectory", Util
					.getLotroMusicPath(false).getAbsolutePath()));

			saveFileDialog.setFileFilter(ABC_FILE_FILTER);
		}

		int result = saveFileDialog.showSaveDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			prefs.put("saveFileDialog.currentDirectory", saveFileDialog.getCurrentDirectory().getAbsolutePath());

			String fileName = saveFileDialog.getSelectedFile().getName();
			int dot = fileName.lastIndexOf('.');
			if (dot <= 0 || !fileName.substring(dot).equalsIgnoreCase(".abc"))
				fileName += ".abc";

			File saveFileTmp = new File(saveFileDialog.getSelectedFile().getParent(), fileName);
			if (saveFileTmp.exists()) {
				int res = JOptionPane.showConfirmDialog(this, "File " + fileName + " already exists. Overwrite?",
						"Confirm Overwrite", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if (res != JOptionPane.YES_OPTION)
					return;
			}

			saveFileDialog.setSelectedFile(saveFileTmp);
			saveSong(saveFileTmp);
		}
	}

	private void updateTitleLabel() {
		String text = abcInfo.getTitlePrefix();
		String artist = abcInfo.getMetadata('C');
		if (artist != null)
			text += " - " + artist;

		titleLabel.setText(text);
		titleLabel.setToolTipText(text);
	}

	private boolean openSongFromCommandLine(String[] args) {
		mainWindow.setExtendedState(mainWindow.getExtendedState() & ~JFrame.ICONIFIED);

		if (args.length > 0) {
			File[] argFiles = new File[args.length];
			for (int i = 0; i < args.length; i++) {
				argFiles[i] = new File(args[i]);
			}
			return openSong(argFiles);
		}
		return false;
	}

	private class OpenSongRunnable implements Runnable {
		private File[] abcFiles;
		private boolean append;

		public OpenSongRunnable(boolean append, File... abcFiles) {
			this.append = append;
			this.abcFiles = abcFiles;
		}

		public void run() {
			if (append)
				appendSong(abcFiles);
			else
				openSong(abcFiles);
		}
	}

	private boolean openSong(File[] abcFiles) {
		List<FileAndData> data = new ArrayList<FileAndData>();

		try {
			for (File abcFile : abcFiles) {
				data.add(new FileAndData(abcFile, AbcToMidi.readLines(abcFile)));
			}
		}
		catch (IOException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Failed to open file", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		return openSong(data);
	}

	private boolean appendSong(File[] abcFiles) {
		List<FileAndData> data = new ArrayList<FileAndData>();

		try {
			for (File abcFile : abcFiles) {
				data.add(new FileAndData(abcFile, AbcToMidi.readLines(abcFile)));
			}
		}
		catch (IOException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Failed to open file", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		return appendSong(data);
	}

	private boolean onLotroParseError(LotroParseException lpe) {
		JCheckBox checkBox = new JCheckBox("Ignore LotRO-specific errors");
		Object[] message = new Object[] {
				lpe.getMessage(), checkBox
		};
		JOptionPane.showMessageDialog(this, message, "Error reading ABC file", JOptionPane.WARNING_MESSAGE);
		prefs.putBoolean("ignoreLotroErrors", checkBox.isSelected());
		lotroErrorsMenuItem.setSelected(checkBox.isSelected());
		return checkBox.isSelected();
	}

	private boolean openSong(List<FileAndData> data) {
		sequencer.stop(); // pause
		updateButtonStates();

		Sequence song = null;
		AbcInfo info = new AbcInfo();
		boolean retry;
		do {
			retry = false;
			try {
				song = AbcToMidi.convert(data, useLotroInstruments, null, info, !lotroErrorsMenuItem.isSelected(),
						stereoMenuItem.isSelected());
			}
			catch (LotroParseException e) {
				if (onLotroParseError(e)) {
					retry = lotroErrorsMenuItem.isSelected();
				}
				else {
					return false;
				}
			}
			catch (ParseException e) {
				JOptionPane.showMessageDialog(this, e.getMessage(), "Error reading ABC", JOptionPane.ERROR_MESSAGE);
				return false;
			}
		} while (retry);

		this.exportFileDialog = null;
		this.saveFileDialog = null;
		this.abcData = data;
		this.abcInfo = info;
		this.instrumentOverrideMap.clear();
		stop();

		try {
			sequencer.setSequence(song);
		}
		catch (InvalidMidiDataException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "MIDI error", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		sequencer.setTempoFactor(1.0f);
		for (int i = 0; i < CHANNEL_COUNT; i++) {
			sequencer.setTrackMute(i, false);
			sequencer.setTrackSolo(i, false);
		}

		updateWindowTitle();
		trackListPanel.songChanged();
		updateButtonStates();
		updateTitleLabel();

		barCountLabel.setVisible(abcInfo.getBarCount() > 0);
		updateBarCountLabel();

		sequencer.start();

		return true;
	}

	private boolean appendSong(List<FileAndData> appendData) {
		if (this.abcData == null || this.abcData.size() == 0) {
			return openSong(appendData);
		}

		boolean running = sequencer.isRunning() || !sequencer.isLoaded();
		long position = sequencer.getPosition();
		sequencer.stop(); // pause

		List<FileAndData> data = new ArrayList<FileAndData>(abcData);
		data.addAll(appendData);

		Sequence song = null;
		AbcInfo info = new AbcInfo();
		boolean retry;
		do {
			retry = false;
			try {
				song = AbcToMidi.convert(data, useLotroInstruments, instrumentOverrideMap, info, !lotroErrorsMenuItem
						.isSelected(), stereoMenuItem.isSelected());
			}
			catch (LotroParseException e) {
				if (onLotroParseError(e)) {
					retry = lotroErrorsMenuItem.isSelected();
				}
				else {
					return false;
				}
			}
			catch (ParseException e) {
				String thisFile = appendData.size() == 1 ? "this file" : "these files";
				String msg = e.getMessage() + "\n\nWould you like to close the current song and retry opening "
						+ thisFile + "?";
				int result = JOptionPane.showConfirmDialog(this, msg, "Error appending ABC", JOptionPane.YES_NO_OPTION,
						JOptionPane.ERROR_MESSAGE);
				if (result == JOptionPane.YES_OPTION) {
					boolean success = openSong(appendData);
					sequencer.setRunning(success && running);
					return success;
				}
				else {
					return false;
				}
			}
		} while (retry);

		this.abcData = data;
		this.abcInfo = info;

		int oldTrackCount = sequencer.getSequence().getTracks().length;

		try {
			sequencer.reset(false);
			sequencer.setSequence(song);
			sequencer.setPosition(position);
			sequencer.setRunning(running);
		}
		catch (InvalidMidiDataException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "MIDI error", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		// Make sure the new tracks are unmuted
		for (int i = oldTrackCount; i < CHANNEL_COUNT; i++) {
			sequencer.setTrackMute(i, false);
			sequencer.setTrackSolo(i, false);
		}

		updateWindowTitle();
		trackListPanel.songChanged();
		updateButtonStates();
		updateTitleLabel();

		barCountLabel.setVisible(abcInfo.getBarCount() > 0);
		updateBarCountLabel();

		return true;
	}

	private void updateWindowTitle() {
		String fileNames = "";
		int c = 0;
		for (FileAndData fd : abcData) {
			File f = fd.file;
			if (++c > 1)
				fileNames += ", ";

			if (c > 2) {
				fileNames += "...";
				break;
			}

			fileNames += f.getName();
		}
		if (fileNames == "")
			setTitle(APP_NAME);
		else
			setTitle(APP_NAME + " - " + fileNames);
	}

	private boolean saveSong(File file) {
		PrintStream out = null;
		try {
			out = new PrintStream(file);
			int i = 0;
			for (FileAndData fileData : abcData) {
				for (String line : fileData.lines)
					out.println(line);

				if (++i < abcData.size())
					out.println();
			}
		}
		catch (IOException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Failed to save file", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		finally {
			if (out != null)
				out.close();
		}
		return true;
	}

	private void stop() {
		sequencer.stop();
		sequencer.setPosition(0);
		updateButtonStates();
	}

	private void updateButtonStates() {
		boolean loaded = (sequencer.getSequence() != null);
		playButton.setEnabled(loaded);
		playButton.setIcon(sequencer.isRunning() ? pauseIcon : playIcon);
		stopButton.setEnabled(loaded && (sequencer.isRunning() || sequencer.getPosition() != 0));
	}

	private class WaitDialog extends JDialog {
		public WaitDialog(JFrame owner, File saveFile) {
			super(owner, APP_NAME, false);
			JPanel waitContent = new JPanel(new BorderLayout(5, 5));
			setContentPane(waitContent);
			waitContent.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			waitContent.add(new JLabel("Saving " + saveFile.getName() + ". Please wait..."), BorderLayout.CENTER);
			JProgressBar waitProgress = new JProgressBar();
			waitProgress.setIndeterminate(true);
			waitContent.add(waitProgress, BorderLayout.SOUTH);
			pack();
			setLocation(getOwner().getX() + (getOwner().getWidth() - getWidth()) / 2, getOwner().getY()
					+ (getOwner().getHeight() - getHeight()) / 2);
			setResizable(false);
			setEnabled(false);
			setIconImages(AbcPlayer.this.getIconImages());
		}
	}

	private void exportWav() {
		if (exportFileDialog == null) {
			exportFileDialog = new JFileChooser(prefs.get("exportFileDialog.currentDirectory", Util.getUserMusicPath()
					.getAbsolutePath()));

			File openedFile = null;
			if (abcData.size() > 0)
				openedFile = abcData.get(0).file;

			if (openedFile != null) {
				String openedName = openedFile.getName();
				int dot = openedName.lastIndexOf('.');
				if (dot >= 0) {
					openedName = openedName.substring(0, dot);
				}
				openedName += ".wav";
				exportFileDialog.setSelectedFile(new File(exportFileDialog.getCurrentDirectory() + "/" + openedName));
			}
		}

		exportFileDialog.setFileFilter(new ExtensionFileFilter("WAV Files", "wav"));

		int result = exportFileDialog.showSaveDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			prefs.put("exportFileDialog.currentDirectory", exportFileDialog.getCurrentDirectory().getAbsolutePath());

			File saveFile = exportFileDialog.getSelectedFile();
			if (saveFile.getName().indexOf('.') < 0) {
				saveFile = new File(saveFile.getParent() + "/" + saveFile.getName() + ".wav");
				exportFileDialog.setSelectedFile(saveFile);
			}

			JDialog waitFrame = new WaitDialog(this, saveFile);
			waitFrame.setVisible(true);
			new Thread(new ExportWavTask(sequencer.getSequence(), saveFile, waitFrame)).start();
		}
	}

	private class ExportWavTask implements Runnable {
		private Sequence sequence;
		private File file;
		private JDialog waitFrame;

		public ExportWavTask(Sequence sequence, File file, JDialog waitFrame) {
			this.sequence = sequence;
			this.file = file;
			this.waitFrame = waitFrame;
		}

		public void run() {
			isExporting = true;
			Exception error = null;
			try {
				FileOutputStream fos = new FileOutputStream(file);
				try {
					MidiToWav.render(sequence, fos);
				}
				finally {
					fos.close();
				}
			}
			catch (Exception e) {
				error = e;
			}
			finally {
				isExporting = false;
				SwingUtilities.invokeLater(new ExportWavFinishedTask(error, waitFrame));
			}
		}
	}

	private class ExportWavFinishedTask implements Runnable {
		private Exception error;
		private JDialog waitFrame;

		public ExportWavFinishedTask(Exception error, JDialog waitFrame) {
			this.error = error;
			this.waitFrame = waitFrame;
		}

		public void run() {
			if (error != null) {
				JOptionPane.showMessageDialog(AbcPlayer.this, error.getMessage(), "Error saving WAV file",
						JOptionPane.ERROR_MESSAGE);
			}
			waitFrame.setVisible(false);
		}
	}

	private void exportMp3() {
		File openedFile = null;
		if (abcData.size() > 0)
			openedFile = abcData.get(0).file;

		Preferences mp3Prefs = prefs.node("mp3");
		File lameExe = new File(mp3Prefs.get("lameExe", "./lame.exe"));
		if (!lameExe.exists()) {
			outerLoop: for (File dir : new File(".").listFiles()) {
				if (dir.isDirectory()) {
					for (File file : dir.listFiles()) {
						if (file.getName().toLowerCase().equals("lame.exe")) {
							lameExe = file;
							break outerLoop;
						}
					}
				}
			}

			if (!lameExe.exists()) {
				JLabel hyperlink = new JLabel("<html><a href='" + LAME_URL + "'>" + LAME_URL + "</a></html>");
				hyperlink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				hyperlink.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent e) {
						if (e.getClickCount() == 1 && e.getButton() == MouseEvent.BUTTON1) {
							Util.openURL(LAME_URL);
						}
					}
				});
				Object[] message = new Object[] {
						"Exporting to MP3 requires LAME. To download it, visit: ", hyperlink,
						"\nAfter you download and unzip it, click OK to locate lame.exe",
				};
				int result = JOptionPane.showConfirmDialog(this, message, "Export to MP3 requires LAME",
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
				if (result != JOptionPane.OK_OPTION)
					return;

				outerLoop: for (File dir : new File(".").listFiles()) {
					if (dir.isDirectory()) {
						for (File file : dir.listFiles()) {
							if (file.getName().toLowerCase().equals("lame.exe")) {
								lameExe = file;
								break outerLoop;
							}
						}
					}
				}
			}

			while (!lameExe.exists()) {
				JFileChooser fc = new JFileChooser();
				fc.setFileFilter(new ExtensionFileFilter("Executable (*.exe)", "exe"));
				fc.setSelectedFile(lameExe);
				int result = fc.showOpenDialog(this);

				if (result == JFileChooser.ERROR_OPTION)
					continue; // Try again
				else if (result != JFileChooser.APPROVE_OPTION)
					return;

				lameExe = fc.getSelectedFile();
				if (!lameExe.exists()) {
					JOptionPane.showMessageDialog(this, "File does not exist:\n" + lameExe.getAbsolutePath(),
							"LAME not found", JOptionPane.ERROR_MESSAGE);
				}
			}

			mp3Prefs.put("lameExe", lameExe.getAbsolutePath());
		}

		ExportMp3Dialog mp3Dialog = new ExportMp3Dialog(this, lameExe, mp3Prefs, openedFile, abcInfo.getTitlePrefix(),
				abcInfo.getArtist());
		mp3Dialog.setIconImages(AbcPlayer.this.getIconImages());
		mp3Dialog.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ExportMp3Dialog dialog = (ExportMp3Dialog) e.getSource();
				JDialog waitFrame = new WaitDialog(AbcPlayer.this, dialog.getSaveFile());
				waitFrame.setVisible(true);
				new Thread(new ExportMp3Task(sequencer.getSequence(), dialog, waitFrame)).start();
			}
		});
		mp3Dialog.setVisible(true);
	}

	private class ExportMp3Task implements Runnable {
		private Sequence sequence;
		private ExportMp3Dialog mp3Dialog;
		private JDialog waitFrame;

		public ExportMp3Task(Sequence sequence, ExportMp3Dialog mp3Dialog, JDialog waitFrame) {
			this.sequence = sequence;
			this.mp3Dialog = mp3Dialog;
			this.waitFrame = waitFrame;
		}

		public void run() {
			isExporting = true;
			Exception error = null;
			try {
				File wavFile = File.createTempFile("AbcPlayer-", ".wav");
				FileOutputStream fos = new FileOutputStream(wavFile);
				try {
					MidiToWav.render(sequence, fos);
					fos.close();
					Process p = Runtime.getRuntime().exec(mp3Dialog.getCommandLine(wavFile));
					if (p.waitFor() != 0)
						throw new Exception("LAME failed");
				}
				finally {
					fos.close();
					wavFile.delete();
				}
			}
			catch (Exception e) {
				error = e;
			}
			finally {
				isExporting = false;
				SwingUtilities.invokeLater(new ExportMp3FinishedTask(error, waitFrame));
			}
		}
	}

	private class ExportMp3FinishedTask implements Runnable {
		private Exception error;
		private JDialog waitFrame;

		public ExportMp3FinishedTask(Exception error, JDialog waitFrame) {
			this.error = error;
			this.waitFrame = waitFrame;
		}

		public void run() {
			if (error != null) {
				JOptionPane.showMessageDialog(AbcPlayer.this, error.getMessage(), "Error saving MP3 file",
						JOptionPane.ERROR_MESSAGE);
			}
			waitFrame.setVisible(false);
		}
	}

	//
	// Track list
	//
	private static final int TRACKLIST_ROWHEIGHT = 18;

	private class TrackListPanel extends JPanel implements ActionListener {
		private TableLayout layout;

		private Object trackIndexKey = new Object();

		public TrackListPanel() {
			super(new TableLayout(new double[] {
					0, FILL, PREFERRED, 0
			}, new double[] {
					0, 0
			}));

			layout = (TableLayout) getLayout();
			layout.setVGap(4);
			layout.setHGap(4);

			setBackground(Color.WHITE);
		}

		public void clear() {
			for (Component c : getComponents()) {
				if (c instanceof JCheckBox) {
					((JCheckBox) c).removeActionListener(this);
				}
			}
			removeAll();
			for (int i = layout.getNumRow() - 2; i >= 1; i--) {
				layout.deleteRow(i);
			}
			revalidate();
			repaint();
		}

		public void songChanged() {
			clear();
			if (sequencer.getSequence() == null)
				return;

			Track[] tracks = sequencer.getSequence().getTracks();
			for (int i = 0; i < tracks.length; i++) {
				Track track = tracks[i];

				// Only show tracks with at least one note
				boolean hasNotes = false;
				String title = null;
				LotroInstrument instrument = LotroInstrument.LUTE;
				for (int j = 0; j < track.size(); j++) {
					MidiEvent evt = track.get(j);
					if (evt.getMessage() instanceof ShortMessage) {
						ShortMessage m = (ShortMessage) evt.getMessage();
						if (m.getCommand() == ShortMessage.NOTE_ON) {
							hasNotes = true;
						}
						else if (m.getCommand() == ShortMessage.PROGRAM_CHANGE) {
							for (LotroInstrument inst : LotroInstrument.values()) {
								if (m.getData1() == inst.midiProgramId) {
									instrument = inst;
									break;
								}
							}
						}
					}
					else if (evt.getMessage() instanceof MetaMessage) {
						MetaMessage meta = (MetaMessage) evt.getMessage();
						if (meta.getType() == META_TRACK_NAME) {
							try {
								title = new String(meta.getData(), "US-ASCII");
							}
							catch (UnsupportedEncodingException e) {
								// ASCII is supported...
							}
						}
					}
				}

				if (hasNotes) {
					if (title == null)
						title = "Track " + (i + 1);

					JCheckBox checkBox = new JCheckBox(title);
					checkBox.putClientProperty(trackIndexKey, i);
					checkBox.setBackground(getBackground());
					checkBox.setSelected(!sequencer.getTrackMute(i));
					checkBox.addActionListener(this);

					JComboBox comboBox = new JComboBox(LotroInstrument.values());
					comboBox.setMaximumRowCount(12);
					comboBox.putClientProperty(trackIndexKey, i);
					comboBox.setBackground(getBackground());
					comboBox.setSelectedItem(instrument);
					comboBox.addActionListener(this);

					int r = layout.getNumRow() - 1;
					layout.insertRow(r, TRACKLIST_ROWHEIGHT);
					add(checkBox, "1, " + r);
					add(comboBox, "2, " + r);
				}
			}

			revalidate();
			repaint();
		}

		@Override
		public void actionPerformed(ActionEvent evt) {
			if (evt.getSource() instanceof JCheckBox) {
				JCheckBox checkBox = (JCheckBox) evt.getSource();
				int i = (Integer) checkBox.getClientProperty(trackIndexKey);
				sequencer.setTrackMute(i, !checkBox.isSelected());
			}
			else if (evt.getSource() instanceof JComboBox) {
				JComboBox comboBox = (JComboBox) evt.getSource();
				int i = (Integer) comboBox.getClientProperty(trackIndexKey);

				instrumentOverrideMap.put(i, (LotroInstrument) comboBox.getSelectedItem());
				refreshSequence();
			}
		}
	}

	private void refreshSequence() {
		long position = sequencer.getPosition();
		Sequence song;

		try {
			song = AbcToMidi.convert(abcData, useLotroInstruments, instrumentOverrideMap, abcInfo, false,
					stereoMenuItem.isSelected());
		}
		catch (ParseException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Error changing instrument", JOptionPane.ERROR_MESSAGE);
			return;
		}

		try {
			boolean running = sequencer.isRunning();
			sequencer.reset(false);
			sequencer.setSequence(song);
			sequencer.setPosition(position);
			sequencer.setRunning(running);
		}
		catch (InvalidMidiDataException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "MIDI error", JOptionPane.ERROR_MESSAGE);
			return;
		}
	}
}
