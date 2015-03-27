package com.digero.maestro;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.digero.common.util.Version;
import com.digero.maestro.view.ProjectFrame;

public class MaestroMain
{
	public static final String APP_NAME = "Maestro";
	public static final String APP_URL = "https://github.com/digero/maestro/";
	public static Version APP_VERSION = new Version(0, 0, 0);

	private static ProjectFrame mainWindow = null;

	public static void main(final String[] args) throws Exception
	{
		try
		{
			Properties props = new Properties();
			props.load(MaestroMain.class.getResourceAsStream("version.txt"));
			String versionString = props.getProperty("version.Maestro");
			if (versionString != null)
				APP_VERSION = Version.parseVersion(versionString);
		}
		catch (IOException ex)
		{
		}

		System.setProperty("sun.sound.useNewAudioEngine", "true");

		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e)
		{
		}

		mainWindow = new ProjectFrame();
		SwingUtilities.invokeAndWait(new Runnable()
		{
			@Override public void run()
			{
				mainWindow.setVisible(true);
				openSongFromCommandLine(args);
			}
		});
		try
		{
			ready();
		}
		catch (UnsatisfiedLinkError err)
		{
			// Ignore (we weren't started via WinRun4J)
		}
	}

	/** Tells the WinRun4J launcher that we're ready to accept activate() calls. */
	public static native void ready();

	/** A new activation from WinRun4J (a.k.a. a file was opened) */
	public static void activate(final String[] args)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override public void run()
			{
				openSongFromCommandLine(args);
			}
		});
	}

	public static void execute(String cmdLine)
	{
		openSongFromCommandLine(new String[] { cmdLine });
	}

	private static void openSongFromCommandLine(String[] args)
	{
		if (mainWindow == null)
			return;

		int state = mainWindow.getExtendedState();
		if ((state & JFrame.ICONIFIED) != 0)
			mainWindow.setExtendedState(state & ~JFrame.ICONIFIED);

		if (args.length > 0)
		{
			File file = new File(args[0]);
			if (file.exists())
				mainWindow.openFile(file);
		}
	}

	/** @deprecated Use isNativeVolumeSupported() instead. */
	@Deprecated public static native boolean isVolumeSupported();

	public static boolean isNativeVolumeSupported()
	{
		try
		{
			return isVolumeSupported();
		}
		catch (UnsatisfiedLinkError err)
		{
			return false;
		}
	}

	public static native float getVolume();

	public static native void setVolume(float volume);

	public static void onVolumeChanged()
	{
		if (mainWindow != null)
			mainWindow.onVolumeChanged();
	}
}
