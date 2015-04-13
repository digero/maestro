package com.digero.tools;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import com.digero.common.abc.LotroInstrument;
import com.digero.common.midi.Note;
import com.digero.common.util.ExtensionFileFilter;
import com.digero.common.view.ColorTable;

public class AudioSorter
{

	public static void main(String[] args) throws Exception
	{
		printMain();
	}

	public static void sortMain() throws Exception
	{
		String sourceRoot = "F:\\Games\\LOTRO\\u15\\instruments_wav_sorted";
		String targetRoot = "F:\\Games\\LOTRO\\u15\\instruments_wav_sorted";

//		copyToFinalNames(sourceRoot, targetRoot, "theorbo", false);
//		copyToFinalNames(sourceRoot, targetRoot, "harp", false);
//		copyToFinalNames(sourceRoot, targetRoot, "lute", false);
//		copyToFinalNames(sourceRoot, targetRoot, "pibgorn", false);

//		copyToFinalNames(sourceRoot, targetRoot, "bagpipe", true);
//		copyToFinalNames(sourceRoot, targetRoot, "clarinet", true);
//		copyToFinalNames(sourceRoot, targetRoot, "flute", true);
//		copyToFinalNames(sourceRoot, targetRoot, "horn", true);

		//
		//
		//

//		File source = new File("F:\\Games\\LOTRO\\u15\\instruments_wav");
//		File target = new File("F:\\Games\\LOTRO\\u15\\instruments_wav_sorting");
//		sortFolder("flute", source, target);
//		sortFolder("bagpipe", source, target);
//		sortFolder("clarinet", source, target);
//		sortFolder("theorbo", source, target);
//		sortFolder("pibgorn", source, target);
//		sortFolder("lute", source, target);
//		sortFolder("harp", source, target);
//		sortFolder("horn", source, target);
	}

	private static void copyToFinalNames(final String sourceRoot, final String targetRoot, final String instrumentName,
			final boolean checkEquals) throws IOException
	{
		Files.walkFileTree(Paths.get(sourceRoot, instrumentName), new SimpleFileVisitor<Path>()
		{
			private int i = 35;
			private Path previous = null;

			@Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
			{
				boolean equals = false;
				if (checkEquals)
				{
					if (previous != null)
					{
						String line = "";
						while (line.length() == 0)
						{
							System.out.print("Equal? (Y/N): ");
							AudioPlayer.playAudioFile(previous.toFile(), 400);
							AudioPlayer.playAudioFile(file.toFile(), 400);
							BufferedReader rdr = new BufferedReader(new InputStreamReader(System.in));
							line = rdr.readLine();
							equals = "Y".equalsIgnoreCase(line);
						}
					}
				}

				if (!equals)
				{
					i++;
					previous = file;
				}
				else
				{
					previous = null;
				}

				String targetFileName = instrumentName + "_" + i + (equals ? "a" : "") + ".wav";
				Path target = equals ? Paths.get(targetRoot, "a", targetFileName) : Paths.get(targetRoot,
						targetFileName);

				System.out.println(target);
				Files.copy(file, target);
				return FileVisitResult.SKIP_SUBTREE;
			}
		});
	}

	private static void resortClarinet() throws IOException
	{
		String source = "F:\\Games\\LOTRO\\u15\\instruments_wav\\clarinet";
		String target = "F:\\Games\\LOTRO\\u15\\instruments_wav_sort_done\\clarinet";
		String[] files = new String[] { "00001108.wav", "00001076.wav", "00004281.wav", "00002402.wav", "00001968.wav",
			"00000057.wav", "00004288.wav", "00004268.wav", "00004670.wav", "00004264.wav", "00003949.wav",
			"00007134.wav", "00004267.wav", "00000061.wav", "00004387.wav", "00004298.wav", "00004263.wav",
			"00003947.wav", "00004255.wav", "00001449.wav", "00002096.wav", "00007240.wav", "00001448.wav",
			"00002403.wav", "00001764.wav", "00004299.wav", "00005024.wav", "00001453.wav", "00000855.wav",
			"00007130.wav", "00004289.wav", "00000054.wav", "00004291.wav", "00004388.wav", "00003945.wav",
			"00001455.wav", "00000386.wav", "00002503.wav", "00004398.wav", "00000496.wav", "00007642.wav",
			"00000058.wav", "00004304.wav", "00004122.wav", "00001098.wav", "00005686.wav", "00004301.wav",
			"00004356.wav", "00004804.wav", "00007639.wav", "00001062.wav", "00004674.wav", "00004393.wav",
			"00005010.wav", };

		File targetDir = new File(target);
		if (!targetDir.exists())
			targetDir.mkdirs();

		for (int i = 0; i < files.length; i++)
		{
			String targetName = String.format("%02d0 - %s", i + 1, files[i]);
			Path from = Paths.get(source, files[i]);
			Path to = Paths.get(target, targetName);
			System.out.println(from + " => " + to);
			Files.copy(from, to);
		}
	}

	private static void sortFolder(String instrument, File sourceRoot, File targetRoot) throws IOException
	{
		sortFolder(new File(sourceRoot, instrument), new File(targetRoot, instrument));
	}

	private static void sortFolder(File sourceFolder, File targetFolder) throws IOException
	{
		SortedSet<FileFft> sorted = new TreeSet<FileFft>();

		int compressionFactor = 1;
		try
		{
			int octaveDelta = LotroInstrument.parseInstrument(sourceFolder.getName()).octaveDelta;
			compressionFactor = (octaveDelta <= 0) ? 1 : 2;
		}
		catch (IllegalArgumentException e)
		{
		}

		System.out.println("Calculating...");
		for (File file : sourceFolder.listFiles(new ExtensionFileFilter("", false, "ogg", "wav")))
		{
			System.out.println(file.getAbsolutePath());
			sorted.add(new FileFft(file, compressionFactor));
		}

		System.out.println();
		System.out.println("Copying...");
		if (!targetFolder.exists())
			targetFolder.mkdirs();

		int i = 0;
		for (FileFft fileFft : sorted)
		{
//			File outputFile = getTargetFileName(fileFft, targetFolder);
			File outputFile = new File(targetFolder, String.format("%02d0.%05d - %s", ++i, fileFft.number,
					fileFft.file.getName()));
			System.out.println(outputFile.getAbsolutePath());
			Files.copy(Paths.get(fileFft.file.getAbsolutePath()), Paths.get(outputFile.getAbsolutePath()),
					StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static void printMain() throws Exception
	{
		File audioRoot = new File("C:/Users/Ben/workspace/maestro/audio/LotroInstruments/");
		print(audioRoot, "lute");
		print(audioRoot, "harp");
		print(audioRoot, "theorbo");
		print(audioRoot, "flute");
		print(audioRoot, "clarinet");
		print(audioRoot, "horn");
		print(audioRoot, "bagpipe");
		print(audioRoot, "pibgorn");
	}

	private static void print(File directory, String instrument) throws IOException
	{
		File outputRoot = new File("F:\\Games\\fft-new");
		if (!outputRoot.exists())
			outputRoot.mkdirs();
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(outputRoot, instrument + ".txt"))))
		{
			for (int noteId = 36; noteId <= 72; noteId++)
			{
//				System.out.println(noteId);
				File wavFile = new File(directory, instrument + "_" + noteId + ".wav");
				if (!wavFile.exists())
					continue;

				int octaveDelta = LotroInstrument.parseInstrument(instrument).octaveDelta;
//				if (octaveDelta < 0)
//					octaveDelta = 0;
				int compressionFactor = (octaveDelta <= 0) ? 1 : 2;

				double[] fft = calculateWavFft(wavFile, compressionFactor);

//				fft = calcWindow(fft);
//				fft = calcBuckets(calcWindow(fft));
				fft = calcWindow(calcBuckets(calcWindow(fft)));

				makeGraph(fft, outputRoot, instrument, noteId);

				System.out.println(instrument + "_" + noteId + " = " + getNumber(fft));
			}
		}
	}

	static final int IGNORE_DC_FREQ_SAMPLES = 32;
	static final int SAMPLE_COUNT = 32768 * 2;
	static final int FFT_WIDTH = SAMPLE_COUNT / 4;
	static final int SAMPLES_PER_BUCKET = 8;
	static final int WINDOW_SIZE = 1;
	static byte[] buffer = new byte[SAMPLE_COUNT * 2];
	static double[] samples = new double[SAMPLE_COUNT];
	static FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);

	private static class FileFft implements Comparable<FileFft>
	{
		public final File file;
		public final int number;

		public FileFft(File file, int compressionFactor) throws IOException
		{
			this.file = file;
			double[] fft = calculateWavFft(file, compressionFactor);
			fft = calcWindow(calcBuckets(calcWindow(fft)));
			this.number = getNumber(fft);
		}

		@Override public boolean equals(Object obj)
		{
			if (obj == null || obj.getClass() != FileFft.class)
				return false;

			return this.file.equals(((FileFft) obj).file);
		}

		@Override public int hashCode()
		{
			return number ^ file.hashCode();
		}

		@Override public int compareTo(FileFft o)
		{
			if (o == null)
				return 1;

			if (this.number != o.number)
				return this.number - o.number;

			return this.file.compareTo(o.file);
		}
	}

	private static double[] calculateWavFft(File file, int compressionFactor) throws IOException
	{
		double[] output = new double[FFT_WIDTH];

		try (AudioInputStream stream = AudioSystem.getAudioInputStream(file))
		{
			AudioFormat format = stream.getFormat();

			if (format.getChannels() != 1)
				throw new RuntimeException();
			if (format.getFrameSize() != 2)
				throw new RuntimeException();
			if (format.getEncoding() != Encoding.PCM_SIGNED)
				throw new RuntimeException();

			int samplesRead;
			int passCount = 0;
			while ((samplesRead = stream.read(buffer) / 2) > 0)
			{
				if (samplesRead < SAMPLE_COUNT / 2 && passCount > 0)
					break;

				for (int i = 0; i < samplesRead; i++)
				{
					int b1 = ((int) buffer[2 * i]) & 0xff;
					int b2 = ((int) buffer[2 * i + 1]) & 0xff;
					samples[i] = ((b2 << 8) | b1) / (double) Short.MAX_VALUE;
				}

				if (samplesRead < SAMPLE_COUNT)
					Arrays.fill(samples, samplesRead, SAMPLE_COUNT - 1, 0);

				Complex[] fft = transformer.transform(samples, TransformType.FORWARD);

				for (int i = 0; i < output.length; i++)
				{
					for (int j = 0; j < compressionFactor; j++)
					{
						int k = i * compressionFactor + j;
						if (k > fft.length / 2)
							throw new RuntimeException();
						output[i] += fft[k].abs() / compressionFactor;
					}
				}
				passCount++;
			}

			for (int i = 0; i < output.length; i++)
			{
				output[i] /= passCount;
			}
		}
		catch (UnsupportedAudioFileException e)
		{
			throw new RuntimeException(e);
		}

		return output;
	}

	private static double[] calcWindow(double[] fft)
	{
		double[] fftWindow = new double[fft.length];
		for (int i = WINDOW_SIZE; i < fft.length - WINDOW_SIZE; i++)
		{
			for (int j = i - WINDOW_SIZE; j <= i + WINDOW_SIZE; j++)
			{
				fftWindow[i] += fft[j] / (2 * WINDOW_SIZE + 1);
			}
		}
		return fftWindow;
	}

	private static double[] calcBuckets(double[] fft)
	{
		int maxIndex = getNumber(fft);

		double[] buckets = new double[fft.length / SAMPLES_PER_BUCKET];
		for (int delta = IGNORE_DC_FREQ_SAMPLES; delta < buckets.length; delta++)
		{
			int c = 0;
			for (int j = maxIndex - delta; j > IGNORE_DC_FREQ_SAMPLES; j -= delta)
			{
				buckets[delta] += fft[j];
				c++;
				if (c == SAMPLES_PER_BUCKET / 2)
					break;
			}
			for (int j = maxIndex + delta; j < fft.length; j += delta)
			{
				buckets[delta] += fft[j];
				c++;
				if (c == SAMPLES_PER_BUCKET)
					break;
			}

			if (c < SAMPLES_PER_BUCKET)
				buckets[delta] = 0;
			else
				buckets[delta] = buckets[delta] / c;
		}

		return buckets;
	}

	private static int getNumber(double[] fft)
	{
		double maxValue = 0;
		int maxIndex = 0;
		for (int i = IGNORE_DC_FREQ_SAMPLES; i < fft.length; i++)
		{
			if (fft[i] > maxValue * 1.1)
			{
				maxIndex = i;
				maxValue = fft[i];
			}
		}

		return maxIndex;
	}

	static final double MAX_Y = 2000;
	static final int MAX_X = 1024;// FFT_WIDTH / 8;

	private static void makeGraph(double[] fft, File folder, String instrument, int noteId) throws IOException
	{
		final int barWidth = 1;
		final int barPadding = 0;
		final int barRun = barWidth + barPadding;
		final int imageHeight = 32;
		final int width = Math.min(MAX_X, fft.length);
		BufferedImage image = new BufferedImage(width * barRun, imageHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();

//		System.out.println(maxValue);

		boolean compress = instrument.equalsIgnoreCase("flute") || instrument.equalsIgnoreCase("pibgorn")
				|| instrument.equalsIgnoreCase("clarinet") || instrument.equalsIgnoreCase("bagpipe");
		compress = compress && ((width * 2) <= fft.length);

		int compressionFactor = compress ? 2 : 1;

		g.setColor(ColorTable.NOTE_ENABLED.get());
		for (int i = 0; i < width; i++)
		{
			int x = barRun * i;
			int w = barWidth;
			double value = compress ? (fft[i * 2] + fft[i * 2 + 1]) : fft[i];
			int h = (int) Math.round(imageHeight * value / MAX_Y);
			int y = imageHeight - h;
			g.fillRect(x, y, w, h);
		}

		int i = getNumber(fft);
		int x = barRun * i / compressionFactor - 1;
		int w = barWidth + 2;
		int h = (int) Math.round(imageHeight * fft[i] * compressionFactor / MAX_Y);
		int y = imageHeight - h;
		g.setColor(ColorTable.NOTE_BAD_ENABLED.get());
		g.fillRect(x, y, w, h);

		g.setColor(ColorTable.CONTROLS_TEXT.get());

		String name = LotroInstrument.parseInstrument(instrument) + " " + Note.fromId(noteId).getDisplayName();

		g.drawString(name, 0, 20);
//		Rectangle2D nameRect = g.getFontMetrics().getStringBounds(name, g);
//		g.drawString(name, (int) (image.getWidth() - nameRect.getWidth() - 10), (int) (nameRect.getHeight() + 10));

		ImageIO.write(image, "png", new File(folder, instrument + "_" + noteId + ".png"));
	}
}
