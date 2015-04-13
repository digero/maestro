package com.digero.tools;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.digero.common.abc.LotroInstrument;

public class InstrumentPicker
{
	@SuppressWarnings("resource") public static void main(String[] args)
	{
		File root = new File("F:\\Games\\LOTRO\\u15\\instruments");

		Map<LotroInstrument, File> dirs = new HashMap<LotroInstrument, File>();

		String pickString = "[0] Replay";
		int i = 0;
		LotroInstrument[] instruments = LotroInstrument.values();
		for (LotroInstrument instrument : instruments)
		{
			File dir = new File(root, instrument.name().toLowerCase());
			if (!dir.exists())
				dir.mkdir();

			dirs.put(instrument, dir);

			i++;
			pickString += ", [" + i + "] " + instrument.toString();
		}

		for (File file : root.listFiles())
		{
			if (file.isDirectory())
				continue;

			while (true)
			{
				System.out.print(pickString + ": ");
				AudioPlayer.playAudioFile(file, 1000);
				String input = new Scanner(System.in).nextLine();
				int val;
				try
				{
					val = Integer.parseInt(input);
				}
				catch (NumberFormatException e)
				{
					continue;
				}

				if (val == 0 || val > instruments.length)
					continue;

				LotroInstrument instrument = instruments[val - 1];

				File destFile = new File(dirs.get(instrument), file.getName());
				file.renameTo(destFile);
				System.out.println(destFile);
				break;
			}
		}
	}
}
