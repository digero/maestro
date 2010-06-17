package com.digero.abcplayer;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import javax.sound.midi.Track;
import javax.sound.midi.Transmitter;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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

import com.digero.common.abc.LotroInstrument;
import com.digero.common.icons.IconLoader;
import com.digero.common.midi.IMidiConstants;
import com.digero.common.midi.SequencerEvent;
import com.digero.common.midi.SequencerListener;
import com.digero.common.midi.SequencerWrapper;
import com.digero.common.midi.SynthesizerFactory;
import com.digero.common.midi.VolumeTransceiver;
import com.digero.common.util.ExtensionFileFilter;
import com.digero.common.util.FileFilterDropListener;
import com.digero.common.util.ParseException;
import com.digero.common.util.Util;
import com.digero.common.util.Version;
import com.digero.common.view.SongPositionBar;
import com.digero.common.view.SongPositionLabel;
import com.digero.common.view.VolumeBar;
import com.sun.media.sound.AudioSynthesizer;

public class AbcPlayer extends JFrame implements TableLayoutConstants, IMidiConstants {
	private static final String APP_NAME = "ABC Player";
	private static final String APP_URL = "http://lotro.acasylum.com/abcplayer/";
	private static final Version APP_VERSION = new Version(0, 1, 0, 0);
	
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
	private AudioSynthesizer synth;
	private Transmitter transmitter;
	private Receiver receiver;
	private VolumeTransceiver volumizer;
	private boolean useLotroInstruments = true;

	private JPanel content;
	private TrackListPanel trackListPanel;

	private SongPositionBar songPositionBar;
	private SongPositionLabel songPositionLabel;
	private VolumeBar volumeBar;

	private ImageIcon playIcon, pauseIcon, stopIcon;
	private JButton playButton, stopButton;

//	private JMenuItem exportMidiMenuItem;
	private JMenuItem exportWavMenuItem;

	private JFileChooser openFileDialog;
	private JFileChooser exportFileDialog;

	private Map<Integer, LotroInstrument> instrumentOverrideMap = new HashMap<Integer, LotroInstrument>();
	private Map<File, List<String>> abcData;

	private Preferences prefs = Preferences.userNodeForPackage(AbcPlayer.class);
	private Preferences windowPrefs = prefs.node("window");

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
			icons.add(ImageIO.read(IconLoader.class.getResource("abcplayer_16.png")));
			icons.add(ImageIO.read(IconLoader.class.getResource("abcplayer_32.png")));
			setIconImages(icons);
		}
		catch (Exception ex) {
			// Ignore
		}

		final FileFilterDropListener dropListener = new FileFilterDropListener(true, "abc", "txt");
		dropListener.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new OpenSongRunnable(dropListener.getDroppedFiles().toArray(new File[0])));
			}
		});
		new DropTarget(this, dropListener);

		try {
			Sequencer seqTmp = MidiSystem.getSequencer(false);
			transmitter = seqTmp.getTransmitter();
			synth = null;

			if (useLotroInstruments) {
				try {
					synth = SynthesizerFactory.getLotroAudioSynthesizer();
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
			transmitter.setReceiver(volumizer = new VolumeTransceiver(receiver));

			sequencer = new SequencerWrapper(seqTmp, transmitter, receiver);
			sequencer.open();
		}
		catch (InvalidMidiDataException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "MIDI error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}
		catch (MidiUnavailableException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "MIDI error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}

		content = new JPanel(new TableLayout(new double[] { // Columns
						4, FILL, 4
				}, new double[] { // Rows
						FILL, 8, PREFERRED
				}));
		setContentPane(content);

		trackListPanel = new TrackListPanel();
		JScrollPane trackListScroller = new JScrollPane(trackListPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		trackListScroller.getVerticalScrollBar().setUnitIncrement(TRACKLIST_ROWHEIGHT);

		JPanel controlPanel = new JPanel(new TableLayout(new double[] {
				4, 0.50, VolumeBar.WIDTH, 4, PREFERRED, 4, PREFERRED, 4, VolumeBar.WIDTH, 0.50, 4, PREFERRED, 4
		}, new double[] {
				4, PREFERRED, 4, PREFERRED, 4
		}));

		songPositionBar = new SongPositionBar(sequencer);
		songPositionLabel = new SongPositionLabel(sequencer);

		playIcon = new ImageIcon(IconLoader.class.getResource("play.png"));
		pauseIcon = new ImageIcon(IconLoader.class.getResource("pause.png"));
		stopIcon = new ImageIcon(IconLoader.class.getResource("stop.png"));

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

		volumeBar = new VolumeBar(volumizer);

		controlPanel.add(songPositionBar, "1, 1, 9, 1");
		controlPanel.add(songPositionLabel, "11, 1");
		controlPanel.add(playButton, "4, 3");
		controlPanel.add(stopButton, "6, 3");
		controlPanel.add(volumeBar, "8, 3, f, c");

		sequencer.addChangeListener(new SequencerListener() {
			public void propertyChanged(SequencerEvent evt) {
				updateButtonStates();
			}
		});

		add(trackListScroller, "0, 0, 2, 0");
		add(controlPanel, "1, 2");

		initMenu();

		updateButtonStates();
		initializeWindowBounds();
	}

	private void initializeWindowBounds() {
		setMinimumSize(new Dimension(320, 168));

		Dimension mainScreen = Toolkit.getDefaultToolkit().getScreenSize();

		int width = windowPrefs.getInt("width", 416);
		int height = windowPrefs.getInt("height", 272);
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

		exportWavMenuItem = fileMenu.add(new JMenuItem("Save as Wave file..."));
		exportWavMenuItem.setMnemonic(KeyEvent.VK_S);
		exportWavMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
		exportWavMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				exportWav();
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

		JMenu helpMenu = mainMenu.add(new JMenu(" Help "));
		helpMenu.setMnemonic(KeyEvent.VK_H);

		JMenuItem about = helpMenu.add(new JMenuItem("About " + APP_NAME + "..."));
		about.setMnemonic(KeyEvent.VK_A);
		about.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ImageIcon aboutIcon = new ImageIcon(IconLoader.class.getResource("abcplayer_64.png"));
				JLabel aboutMessage = new JLabel("<html>ABC Player for The Lord of the Rings Online<br>" + "Version "
						+ APP_VERSION + "<br>" + "Created by Digero of Landroval<br>" + "<a href='" + APP_URL + "'>"
						+ APP_URL + "</a><br>" + "&copy; 2010 Ben Howell</html>");
				aboutMessage.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				aboutMessage.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent e) {
						if (e.getButton() == MouseEvent.BUTTON1) {
							Util.openURL(APP_URL);
						}
					}
				});
				String aboutTitle = "About ABC Player";
				JOptionPane.showMessageDialog(AbcPlayer.this, aboutMessage, aboutTitle,
						JOptionPane.INFORMATION_MESSAGE, aboutIcon);
			}
		});
	}

	private void openSongDialog() {
		if (openFileDialog == null) {
			openFileDialog = new JFileChooser(prefs.get("openFileDialog.currentDirectory", Util
					.getLotroMusicPath(false).getAbsolutePath()));

			openFileDialog.setMultiSelectionEnabled(true);
			openFileDialog.setFileFilter(new ExtensionFileFilter("ABC Files", "abc", "txt"));
		}

		int result = openFileDialog.showOpenDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			prefs.put("openFileDialog.currentDirectory", openFileDialog.getCurrentDirectory().getAbsolutePath());

			if (openSong(openFileDialog.getSelectedFiles()))
				sequencer.start();
		}
	}

	private boolean openSongFromCommandLine(String[] args) {
		mainWindow.setExtendedState(mainWindow.getExtendedState() & ~JFrame.ICONIFIED);

		if (args.length > 0) {
			File[] argFiles = new File[args.length];
			for (int i = 0; i < args.length; i++) {
				argFiles[i] = new File(args[i]);
			}
			if (openSong(argFiles)) {
				sequencer.start();
				return true;
			}
		}
		return false;
	}

	private class OpenSongRunnable implements Runnable {
		private File[] abcFiles;

		public OpenSongRunnable(File... abcFiles) {
			this.abcFiles = abcFiles;
		}

		public void run() {
			if (openSong(abcFiles))
				sequencer.start();
		}
	}

	private boolean openSong(File[] abcFiles) {
		Map<File, List<String>> data = new HashMap<File, List<String>>();

		try {
			for (File abcFile : abcFiles) {
				data.put(abcFile, AbcToMidi.readLines(abcFile));
			}
		}
		catch (IOException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Failed to open file", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		return openSong(data);
	}

	private boolean openSong(Map<File, List<String>> data) {
		sequencer.stop(); // pause
		updateButtonStates();

		Sequence song;
		try {
			song = AbcToMidi.convert(data, useLotroInstruments, null);
		}
		catch (ParseException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Error reading ABC file", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		exportFileDialog = null;
		abcData = data;
		instrumentOverrideMap.clear();
		stop();

		try {
			sequencer.setSequence(song);
		}
		catch (InvalidMidiDataException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "MIDI error", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		for (int i = 0; i < 16; i++) {
			sequencer.setTrackMute(i, false);
			sequencer.setTrackSolo(i, false);
		}

		String fileNames = "";
		int c = 0;
		for (File f : data.keySet()) {
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
		trackListPanel.songChanged();
		updateButtonStates();

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

		exportWavMenuItem.setEnabled(loaded);
//		exportMidiMenuItem.setEnabled(loaded);
	}

	private void exportWav() {
		if (exportFileDialog == null) {
			exportFileDialog = new JFileChooser(prefs.get("exportFileDialog.currentDirectory", Util.getUserMusicPath()
					.getAbsolutePath()));

			File openedFile = null;
			for (File f : abcData.keySet()) {
				openedFile = f;
				break;
			}
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

			JDialog waitFrame = new JDialog(this, APP_NAME);
			JPanel waitContent = new JPanel(new BorderLayout(5, 5));
			waitFrame.setContentPane(waitContent);
			waitContent.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			waitContent.add(new JLabel("Saving " + saveFile.getName() + ". Please wait..."), BorderLayout.CENTER);
			JProgressBar waitProgress = new JProgressBar();
			waitProgress.setIndeterminate(true);
			waitContent.add(waitProgress, BorderLayout.SOUTH);
			waitFrame.pack();
			waitFrame.setLocation(getX() + (getWidth() - waitFrame.getWidth()) / 2, getY()
					+ (getHeight() - waitFrame.getHeight()) / 2);
			waitFrame.setResizable(false);
			waitFrame.setModal(false);
			waitFrame.setEnabled(false);
			waitFrame.setIconImages(getIconImages());
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
			SwingUtilities.invokeLater(new ExportWavFinishedTask(error, waitFrame));
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

				long positon = sequencer.getPosition();
				instrumentOverrideMap.put(i, (LotroInstrument) comboBox.getSelectedItem());
				Sequence song;

				try {
					song = AbcToMidi.convert(abcData, useLotroInstruments, instrumentOverrideMap);
				}
				catch (ParseException e) {
					JOptionPane.showMessageDialog(this, e.getMessage(), "Error changing instrument",
							JOptionPane.ERROR_MESSAGE);
					return;
				}

				try {
					sequencer.setSequence(song);
					sequencer.setPosition(positon);
				}
				catch (InvalidMidiDataException e) {
					JOptionPane.showMessageDialog(this, e.getMessage(), "MIDI error", JOptionPane.ERROR_MESSAGE);
					return;
				}
			}
		}
	}
}
