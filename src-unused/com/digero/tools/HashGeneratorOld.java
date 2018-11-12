package com.digero.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.bind.DatatypeConverter;

import com.digero.common.util.ExtensionFileFilter;
import com.digero.common.util.Util;

public class HashGeneratorOld
{
	private static final String cachedHashesFileName = "_hashes.txt";

	public static void main(String[] args)
	{
		System.exit(run(args));

//		new File("F:\\Games\\LOTRO\\u23\\ogg\\"+cachedHashesFileName).delete();
//		new File("F:\\Games\\LOTRO\\u23\\ogg\\h.txt").delete();
//		String[] args_tmp = new String[] { "GENERATE_HASHES", "F:\\Games\\LOTRO\\u23\\ogg",
//			"F:\\Games\\LOTRO\\u23\\ogg\\h.txt" };
//		run(args_tmp);
	}

	private static void printHelp()
	{
		System.out.println("Usage:");
		System.out.println("");
		System.out.println("GENERATE_HASHES <sourceDirectory> <outputHashFile>");
		System.out.println("    Generates a list of hashes for all files in the given directory");
		System.out.println("");
		System.out.println("MOVE_EXCLUDE <truthHashFile> <sourceDirectory> <targetDirectory>");
		System.out.println("    Moves any file from the source to target UNLESS it is in truthHashFile");
		System.out.println("");
		System.out.println("MOVE_INCLUDE <truthHashFile> <sourceDirectory> <targetDirectory>");
		System.out.println("    Moves any file from the source to target ONLY IF it is in truthHashFile");
		System.out.println("");
		System.out.println("MOVE_EXCLUDE_DIR <truthDir> <sourceDirectory> <targetDirectory>");
		System.out.println("    Moves any file from the source to target UNLESS it is in <truthDir>");
		System.out.println("");
		System.out.println("MOVE_INCLUDE_DIR <truthDir> <sourceDirectory> <targetDirectory>");
		System.out.println("    Moves any file from the source to target ONLY IF it is in <truthDir>");
	}

	public static int run(String[] args)
	{
		if (args.length < 1 || args[0].equals("-?") || args[0].equals("/?"))
		{
			printHelp();
			return -2;
		}

		try
		{
			String action = args[0].toUpperCase();
			if (action.equals("GENERATE_HASHES"))
			{
				if (args.length != 3)
				{
					System.err.println("Incorrect number of args to GENERATE_HASHES");
					return -1;
				}

				outputHashListToFile(generateHashes(new File(args[1]), true), new File(args[2]));
			}
			else if (action.equals("MOVE_EXCLUDE_DIR") || action.equals("MOVE_INCLUDE_DIR"))
			{
				if (args.length != 4)
				{
					System.err.println("Incorrect number of args to " + args[0]);
					return -1;
				}

				moveDir(action.equals("MOVE_INCLUDE_DIR"), new File(args[1]), new File(args[2]), new File(args[3]));
			}
			else if (action.equals("MOVE_EXCLUDE") || action.equals("MOVE_INCLUDE"))
			{
				if (args.length != 4)
				{
					System.err.println("Incorrect number of args to " + args[0]);
					return -1;
				}

				move(action.equals("MOVE_INCLUDE"), new File(args[1]), new File(args[2]), new File(args[3]));
			}
			else if (action.equals("LIST_EXCLUDE") || action.equals("LIST_INCLUDE"))
			{
				if (args.length != 3)
				{
					System.err.println("Incorrect number of args to " + args[0]);
					return -1;
				}

				list(action.equals("LIST_INCLUDE"), new File(args[1]), new File(args[2]));
			}
			else
			{
				System.err.println("Unknown mode: " + args[0]);
				return -1;
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return -1;
		}

		return 0;
	}

	private static void moveDir(boolean include, File truthDirectory, File sourceDirectory, File targetDirectory)
			throws IOException
	{
		if (!targetDirectory.exists())
			targetDirectory.mkdirs();

		Map<Hash, File> truthHashes = generateHashes(truthDirectory, /* recursive = */true);
		Map<Hash, File> sourceHashes = generateHashes(sourceDirectory, /* recursive = */false);
		Map<Hash, File> targetHashes = new HashMap<Hash, File>();

		File cachedHashesFile = new File(targetDirectory, cachedHashesFileName);
		if (cachedHashesFile.exists())
			targetHashes = inputHashListFromFile(cachedHashesFile);

		final String match = include ? "match" : "don't match";

		System.out.println("Moving from \"" + sourceDirectory.getAbsolutePath() + "\" to \""
				+ targetDirectory.getAbsolutePath() + "\" if they " + match + " files in \""
				+ truthDirectory.getAbsolutePath() + "\"");
		int movedCount = 0;
		for (Map.Entry<Hash, File> entry : sourceHashes.entrySet())
		{
			File truthFile = truthHashes.get(entry.getKey());
			boolean isInList = (truthFile != null);
			if (include == isInList)
			{
				File subDirectory = new File(targetDirectory, truthFile.getParentFile().getAbsolutePath()
						.substring(truthDirectory.getAbsolutePath().length()));

				if (!subDirectory.exists())
					subDirectory.mkdirs();

				File sourceFile = entry.getValue();
				File targetFile = new File(subDirectory, sourceFile.getName());

				sourceFile.renameTo(targetFile);
				movedCount++;

				targetHashes.put(entry.getKey(), targetFile);
			}
		}
		System.out.println("Moved " + movedCount + " files");

		outputHashListToFile(targetHashes, cachedHashesFile);
	}

	private static void move(boolean include, File existingHashFile, File sourceDirectory, File targetDirectory)
			throws IOException
	{
		Map<Hash, File> existingHashes = inputHashListFromFile(existingHashFile);
		Map<Hash, File> sourceHashes = generateHashes(sourceDirectory, /* recursive = */false);
		Map<Hash, File> targetHashes = new HashMap<Hash, File>();

		File cachedHashesFile = new File(targetDirectory, cachedHashesFileName);
		if (cachedHashesFile.exists())
			targetHashes = inputHashListFromFile(cachedHashesFile);

		for (Map.Entry<Hash, File> entry : sourceHashes.entrySet())
		{
			boolean isInList = existingHashes.containsKey(entry.getKey());
			if (include == isInList)
			{
				File sourceFile = entry.getValue();
				File targetFile = new File(targetDirectory, sourceFile.getName());
				sourceFile.renameTo(targetFile);

				targetHashes.put(entry.getKey(), targetFile);
			}
		}

		outputHashListToFile(targetHashes, cachedHashesFile);
	}

	private static void list(boolean include, File existingHashFile, File sourceDirectory) throws IOException
	{
		Map<Hash, File> existingHashes = inputHashListFromFile(existingHashFile);
		Map<Hash, File> sourceHashes = generateHashes(sourceDirectory, false);

		System.out.println("Listing files " + (include ? "included" : "not included") + " in hash file: ");
		for (Map.Entry<Hash, File> entry : sourceHashes.entrySet())
		{
			boolean isInList = existingHashes.containsKey(entry.getKey());
			if (include == isInList)
			{
				System.out.println(entry.getValue().getAbsolutePath());
			}
		}
	}

	private static List<File> listFiles(File directory, boolean recursive)
	{
		FileFilter filter = new ExtensionFileFilter("", recursive, "ogg", "wav");

		List<File> files = new ArrayList<File>();
		files.add(directory);

		for (int i = 0; i < files.size(); i++)
		{
			File file = files.get(i);
			if (file.isDirectory())
			{
				files.remove(i--);
				File[] fileArray = file.listFiles(filter);
				if (fileArray != null)
					files.addAll(Arrays.asList(fileArray));
			}
		}

		return files;
	}

	public static Map<Hash, File> generateHashes(File directory, boolean recursive) throws IOException
	{
		List<File> allFiles = listFiles(directory, recursive);

		ConcurrentHashMap<Hash, File> hashes = new ConcurrentHashMap<Hash, File>();
		File cachedHashesFile = new File(directory, cachedHashesFileName);
		if (cachedHashesFile.exists())
			hashes = new ConcurrentHashMap<Hash, File>(inputHashListFromFile(cachedHashesFile));

		// Only hash the files we don't have a cached hash for
		List<File> filesToHash = new ArrayList<File>(allFiles);
		filesToHash.removeAll(new HashSet<File>(hashes.values()));

		HashTask.State state = new HashTask.State(directory, filesToHash, hashes);

		if (filesToHash.size() < 100)
		{
			new HashTask(state).run();
		}
		else
		{
			Thread[] threads = new Thread[8];
			for (int i = 0; i < threads.length; i++)
			{
				threads[i] = new Thread(new HashTask(state));
				threads[i].start();
			}

			for (int i = 0; i < threads.length; i++)
			{
				try
				{
					threads[i].join();
				}
				catch (InterruptedException e)
				{
					throw new RuntimeException(e);
				}
			}
		}

		// Write out the cached hashes to the fiel
		outputHashListToFile(hashes, cachedHashesFile);

		// Remove any extra hashes that were read from cachedHashesFile
		// but aren't for the files we care about
		hashes.values().retainAll(new HashSet<File>(allFiles));

		return hashes;
	}

	private static class HashTask implements Runnable
	{
		public static class State
		{
			private final File directory;
			private final List<File> files;
			private final ConcurrentHashMap<Hash, File> hashes;

			private final long startMillis = System.currentTimeMillis();
			private final AtomicInteger index = new AtomicInteger(-1);
			private final AtomicInteger lastPrintedCount = new AtomicInteger(-1);

			public State(File directory, List<File> files, ConcurrentHashMap<Hash, File> hashes)
			{
				this.directory = directory;
				this.files = Collections.unmodifiableList(files);
				this.hashes = hashes;
			}
		}

		private final State s;

		public HashTask(State state)
		{
			this.s = state;
		}

		@Override public void run()
		{
			try
			{
				runThrows();
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}

		public void runThrows() throws IOException
		{
			Hasher hasher = new Hasher();
			final int count = s.files.size();
			for (int i = s.index.incrementAndGet(); i < count; i = s.index.incrementAndGet())
			{
				File file = s.files.get(i);

				File prevFile = s.hashes.put(hasher.hashFile(file), file);

				printStatus(i);

				if (prevFile != null && !fileContentEquals(prevFile, file))
				{
					synchronized (System.out)
					{
						System.out.println("WARNING Duplicate Hashes:");
						System.out.println("    " + prevFile.getAbsolutePath());
						System.out.println("    " + file.getAbsolutePath());
					}
				}
			}
		}

		private void printStatus(int i)
		{
			final int count = s.files.size();
			final int printFreq = Math.max(1000, (int) Math.ceil(0.1 * count));

			int printCount = i + 1;
			if (printCount < count)
				printCount = (printCount / printFreq) * printFreq;

			int lastPrintedCount = s.lastPrintedCount.get();
			while (printCount > lastPrintedCount && !s.lastPrintedCount.compareAndSet(lastPrintedCount, printCount))
				lastPrintedCount = s.lastPrintedCount.get();

			if (printCount > lastPrintedCount)
			{
				int pct = (printCount * 100) / count;
				long elapsedMicros = (System.currentTimeMillis() - s.startMillis) * 1000;
				int w = (int) Math.ceil(Math.log10(count));
				synchronized (System.out)
				{
					System.out.format("Generating hashes in %s: [%" + w + "d/%" + w + "d] %3d%% (%s)\n",
							s.directory.getPath(), printCount, count, pct, Util.formatDuration(elapsedMicros));
				}
			}
		}
	}

	private static void outputHashListToFile(Map<Hash, File> hashes, File outputFile) throws IOException
	{
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile)))
		{
			for (Map.Entry<Hash, File> entry : hashes.entrySet())
			{
				writer.write(entry.getKey() + "\t" + entry.getValue().getAbsolutePath() + "\r\n");
			}
		}

		if (!outputFile.getName().equals(cachedHashesFileName))
			System.out.println("Wrote " + hashes.size() + " hashes to: " + outputFile.getAbsolutePath());
	}

	private static Map<Hash, File> inputHashListFromFile(File hashListFile) throws IOException
	{
		Map<Hash, File> hashes = new HashMap<Hash, File>();
		try (BufferedReader reader = new BufferedReader(new FileReader(hashListFile)))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				int tab = line.indexOf('\t');
				if (tab <= 0)
					continue;

				hashes.put(new Hash(line.substring(0, tab)), new File(line.substring(tab + 1)));
			}
		}

		if (!hashListFile.getName().equals(cachedHashesFileName))
			System.out.println("Read " + hashes.size() + " hashes from: " + hashListFile.getAbsolutePath());

		return hashes;
	}

	private static class Hasher
	{
		private final byte[] buffer = new byte[32768];
		private final MessageDigest hashFunction;

		public Hasher()
		{
			try
			{
				hashFunction = MessageDigest.getInstance("SHA-256");
			}
			catch (NoSuchAlgorithmException e)
			{
				throw new RuntimeException(e);
			}
		}

		public Hash hashFile(File file) throws IOException
		{
			try (FileInputStream stream = new FileInputStream(file))
			{
				return hashStream(stream);
			}
		}

		public Hash hashStream(InputStream stream) throws IOException
		{
			hashFunction.reset();

			int bytesRead;
			while ((bytesRead = stream.read(buffer)) != -1)
				hashFunction.update(buffer, 0, bytesRead);

			return new Hash(hashFunction.digest());
		}
	}

	private static class Hash
	{
		public final byte[] value;
		private int hashCode = 0;

		public Hash(byte[] value)
		{
			this.value = value;
		}

		public Hash(String hexString)
		{
			this.value = DatatypeConverter.parseHexBinary(hexString);
		}

		@Override public String toString()
		{
			return DatatypeConverter.printHexBinary(value);
		}

		@Override public boolean equals(Object obj)
		{
			if (obj == null || obj.getClass() != Hash.class)
				return false;

			return Arrays.equals(value, ((Hash) obj).value);
		}

		@Override public int hashCode()
		{
			// If the hash code is actually 0, this will calculate every time. Oh well.
			if (hashCode == 0)
				hashCode = Arrays.hashCode(value);

			return hashCode;
		}
	}

	public static boolean fileContentEquals(File fileA, File fileB) throws IOException
	{
		if (!fileA.exists() || !fileB.exists())
			return false;

		if (!fileA.isFile() || !fileB.isFile())
			return false;

		if (fileA.equals(fileB))
			return true;

		final long fileSize = fileA.length();
		if (fileSize != fileB.length() || fileSize < 0)
			return false;

		if (fileSize == 0)
			return true;

		final int bufferSize = (int) Math.min(fileSize, 8192);
		byte[] bufferA = new byte[bufferSize];
		byte[] bufferB = new byte[bufferSize];

		try (FileInputStream streamA = new FileInputStream(fileA); FileInputStream streamB = new FileInputStream(fileB))
		{
			while (true)
			{
				int readA = readMax(streamA, bufferA);
				int readB = readMax(streamB, bufferB);

				if (readA != readB || !Arrays.equals(bufferA, bufferB))
					return false;

				// Reached end-of-stream
				if (readA < bufferSize || readA == fileSize)
					return true;
			}
		}
	}

	/**
	 * Reads as many bytes as possible from the stream. Blocks until either the buffer is full or
	 * end of stream is reached.
	 * 
	 * @return The number of bytes read. A value less than buffer.length indicates the end of stream
	 *         was reached.
	 */
	private static int readMax(InputStream stream, byte[] buffer) throws IOException
	{
		int total = 0;
		while (total < buffer.length)
		{
			int lastRead = stream.read(buffer, total, buffer.length - total);
			if (lastRead < 0)
				return total;

			total += lastRead;
		}
		return total;
	}
}
