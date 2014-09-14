package com.digero.maestro.view;

import java.util.prefs.Preferences;

public class SaveAndExportSettings
{
	public boolean promptSaveNewSong = true;
	public boolean showExportFileChooser = false;
	private final Preferences prefs;

	public SaveAndExportSettings(Preferences prefs)
	{
		this.prefs = prefs;
		promptSaveNewSong = prefs.getBoolean("promptSaveNewSong", promptSaveNewSong);
		showExportFileChooser = prefs.getBoolean("showExportFileChooser", showExportFileChooser);
	}

	public SaveAndExportSettings(SaveAndExportSettings that)
	{
		this.prefs = that.prefs;
		copyFrom(that);
	}

	public void copyFrom(SaveAndExportSettings that)
	{
		promptSaveNewSong = that.promptSaveNewSong;
		showExportFileChooser = that.showExportFileChooser;
	}

	public void saveToPrefs()
	{
		prefs.putBoolean("promptSaveNewSong", promptSaveNewSong);
		prefs.putBoolean("showExportFileChooser", showExportFileChooser);
	}

	public SaveAndExportSettings getCopy()
	{
		return new SaveAndExportSettings(this);
	}
}
