package com.digero.maestro.abc;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ListIterator;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.digero.common.abc.LotroInstrument;
import com.digero.common.util.Pair;
import com.digero.common.util.Util;
import com.digero.maestro.view.SettingsDialog.MockMetadataSource;

public class PartNameTemplate
{
	public static class Settings
	{
		private String partNamePattern;

		private Settings(Preferences prefs)
		{
			partNamePattern = prefs.get("partNamePattern", "$SongTitle ($SongLength) - $PartName");
		}

		public Settings(Settings source)
		{
			copyFrom(source);
		}

		private void save(Preferences prefs)
		{
			prefs.put("partNamePattern", partNamePattern);
		}

		private void copyFrom(Settings source)
		{
			this.partNamePattern = source.partNamePattern;
		}

		public String getPartNamePattern()
		{
			return partNamePattern;
		}

		public void setPartNamePattern(String partNamePattern)
		{
			this.partNamePattern = partNamePattern;
		}
	}

	public static abstract class Variable
	{
		private String description;

		private Variable(String description)
		{
			this.description = description;
		}

		public abstract String getValue();

		public String getDescription()
		{
			return description;
		}

		@Override public String toString()
		{
			return getValue();
		}
	}

	private Settings settings;
	private Preferences prefsNode;

	private AbcMetadataSource metadata = null;
	private AbcPartMetadataSource currentAbcPart;

	private SortedMap<String, Variable> variables;

	public PartNameTemplate(Preferences prefsNode)
	{
		this.prefsNode = prefsNode;
		this.settings = new Settings(prefsNode);

		Comparator<String> caseInsensitiveStringComparator = new Comparator<String>()
		{
			@Override public int compare(String a, String b)
			{
				return a.compareToIgnoreCase(b);
			}
		};

		variables = new TreeMap<String, Variable>(caseInsensitiveStringComparator);

		variables.put("$SongTitle", new Variable("The title of the song, as entered in the \"T:\" field")
		{
			@Override public String getValue()
			{
				return getMetadataSource().getSongTitle().trim();
			}
		});
		variables.put("$SongLength", new Variable("The playing time of the song in mm:ss format")
		{
			@Override public String getValue()
			{
				return Util.formatDuration(getMetadataSource().getSongLengthMicros());
			}
		});
		variables.put("$SongComposer", new Variable("The song composer's name, as entered in the \"C:\" field")
		{
			@Override public String getValue()
			{
				return getMetadataSource().getComposer().trim();
			}
		});
		variables.put("$SongTranscriber", new Variable("Your name, as entered in the \"Z:\" field")
		{
			@Override public String getValue()
			{
				return getMetadataSource().getTranscriber().trim();
			}
		});
		variables.put("$PartName", new Variable("The part name for the individual ABC part")
		{
			@Override public String getValue()
			{
				if (currentAbcPart == null)
					return "";

				return currentAbcPart.getTitle().trim();
			}
		});
		variables.put("$PartNumber", new Variable("The part number for the individual ABC part")
		{
			@Override public String getValue()
			{
				if (currentAbcPart == null)
					return "0";

				return "" + currentAbcPart.getPartNumber();
			}
		});
		variables.put("$PartInstrument", new Variable("The instrument for the individual ABC part")
		{
			@Override public String getValue()
			{
				if (currentAbcPart == null)
					return LotroInstrument.DEFAULT_INSTRUMENT.toString();

				return currentAbcPart.getInstrument().toString();
			}
		});
		variables.put("$FilePath", new Variable("The path to the ABC file including the ABC file name, "
				+ "if it is in a subdirectory of the LOTRO/Music directory.\n"
				+ "If the file is saved directly in the LOTRO/Music directory, "
				+ "this will be the same as <b>$FileName</b>.")
		{
			@Override public String getValue()
			{
				if (getMetadataSource().getExportFile() == null)
					return "";

				File root = Util.getLotroMusicPath(false);
				String saveFileName = Util.fileNameWithoutExtension(getMetadataSource().getExportFile());

				String path = saveFileName;
				boolean foundRoot = false;
				for (File file = getMetadataSource().getExportFile().getParentFile(); file != null; file = file
						.getParentFile())
				{
					if (root.equals(file))
					{
						foundRoot = true;
						break;
					}
					path = file.getName() + "/" + path;
				}

				if (foundRoot)
					return path;

				return saveFileName;
			}
		});
		variables.put("$FileName", new Variable("The name of the ABC file, without the .abc extension")
		{
			@Override public String getValue()
			{
				if (getMetadataSource().getExportFile() == null)
					return "";

				return Util.fileNameWithoutExtension(getMetadataSource().getExportFile());
			}
		});
	}

	public Settings getSettingsCopy()
	{
		return new Settings(settings);
	}

	public void setSettings(Settings settings)
	{
		this.settings.copyFrom(settings);
		this.settings.save(prefsNode);
	}

	public AbcMetadataSource getMetadataSource()
	{
		if (metadata == null)
			metadata = new MockMetadataSource(null);

		return metadata;
	}

	public void setMetadataSource(AbcMetadataSource metadata)
	{
		this.metadata = metadata;
	}

	public void setCurrentAbcPart(AbcPartMetadataSource currentAbcPart)
	{
		this.currentAbcPart = currentAbcPart;
	}

	public AbcPartMetadataSource getCurrentAbcPart()
	{
		return currentAbcPart;
	}

	public SortedMap<String, Variable> getVariables()
	{
		return Collections.unmodifiableSortedMap(variables);
	}

	public String formatName(AbcPartMetadataSource currentAbcPart)
	{
		return formatName(settings.getPartNamePattern(), currentAbcPart);
	}

	public String formatName(String partNamePattern, AbcPartMetadataSource currentAbcPart)
	{
		this.currentAbcPart = currentAbcPart;
		String name = partNamePattern;

		Pattern regex = Pattern.compile("\\$[A-Za-z]+");
		Matcher matcher = regex.matcher(name);

		ArrayList<Pair<Integer, Integer>> matches = new ArrayList<Pair<Integer, Integer>>();
		while (matcher.find())
		{
			matches.add(new Pair<Integer, Integer>(matcher.start(), matcher.end()));
		}

		ListIterator<Pair<Integer, Integer>> reverseIter = matches.listIterator(matches.size());
		while (reverseIter.hasPrevious())
		{
			Pair<Integer, Integer> match = reverseIter.previous();
			Variable var = variables.get(name.substring(match.first, match.second));
			if (var != null)
			{
				name = name.substring(0, match.first) + var.getValue() + name.substring(match.second);
			}
		}

		this.currentAbcPart = null;
		return name;
	}
}
