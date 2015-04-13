package com.digero.tools;

import java.io.File;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

public class AudioPlayer
{
	public static void playAudioFile(File file)
	{
		playAudioFile(file, 0);
	}

	public static void playAudioFile(File file, int playDurationMillis)
	{
		try
		{
			// Get AudioInputStream from given file.	
			AudioInputStream compressedInput = AudioSystem.getAudioInputStream(file);
			AudioInputStream decodedInput = null;
			if (compressedInput != null)
			{
				AudioFormat baseFormat = compressedInput.getFormat();
				AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16,
						baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
				// Get AudioInputStream that will be decoded by underlying VorbisSPI
				decodedInput = AudioSystem.getAudioInputStream(format, compressedInput);

				int oneSecondBytes = (int) Math.ceil(format.getFrameSize() * format.getFrameRate());
				int fadeOutDurationBytes = oneSecondBytes / 20;
				int playDurationBytes;
				if (playDurationMillis <= 0)
					playDurationBytes = Integer.MAX_VALUE;
				else
					playDurationBytes = oneSecondBytes * playDurationMillis / 1000;

				byte[] data = new byte[1024];
				SourceDataLine line = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class,
						format));
				line.open(format);

				FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
				float initialGain = gainControl.getValue();

				// Start
				line.start();
				int bytesRead;
				int totalBytes = 0;
				while ((bytesRead = decodedInput.read(data, 0, data.length)) != -1)
				{
					totalBytes += bytesRead;
					int playedBytes = totalBytes;
					if (totalBytes >= playDurationBytes)
					{
						bytesRead -= totalBytes - playDurationBytes;
						playedBytes = playDurationBytes;
					}

					if (playDurationBytes - playedBytes < fadeOutDurationBytes)
					{
						float fadePct = ((float) (playDurationBytes - playedBytes)) / fadeOutDurationBytes;
						gainControl.setValue(initialGain * fadePct + gainControl.getMinimum() * (1.0f - fadePct));
					}

					line.write(data, 0, bytesRead);

					if (totalBytes >= playDurationBytes)
						break;
				}
				// Stop
				line.drain();
				gainControl.setValue(initialGain);
				line.stop();
				line.close();
				decodedInput.close();
				compressedInput.close();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

}
