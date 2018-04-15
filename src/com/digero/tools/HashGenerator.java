package com.digero.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import com.digero.common.util.ExtensionFileFilter;

public class HashGenerator
{
	static boolean verbose = true;

	public static void main(String[] args)
	{
		System.exit(run(args));

//		String[] args_movedir_u16_instruments = new String[] { "MOVE_INCLUDE_DIR",
//			"F:\\Games\\LOTRO\\u16\\ogg\\instruments", "F:\\Games\\LOTRO\\u22\\ogg",
//			"F:\\Games\\LOTRO\\u22\\instruments" };
//
//		String[] args_movedir_u15_instruments = new String[] { "MOVE_INCLUDE_DIR",
//			"F:\\Games\\LOTRO\\u15\\instruments", "F:\\Games\\LOTRO\\u22\\ogg", "F:\\Games\\LOTRO\\u22\\instruments" };
//
//		String[] args_movedir_u16_known_not_instruments = new String[] { "MOVE_INCLUDE_DIR",
//			"F:\\Games\\LOTRO\\u16\\ogg\\known_not_instruments", "F:\\Games\\LOTRO\\u22\\ogg",
//			"F:\\Games\\LOTRO\\u22\\known_not_instruments\\u16" };
//
//		String[] args_movedir_u15_known_not_instruments = new String[] { "MOVE_INCLUDE_DIR",
//			"F:\\Games\\LOTRO\\u15\\known_not_instruments", "F:\\Games\\LOTRO\\u22\\ogg",
//			"F:\\Games\\LOTRO\\u22\\known_not_instruments\\u15" };
//
//		run(args_movedir_u15_known_not_instruments);
//		run(args_movedir_u16_known_not_instruments);
//
//		System.out.println("Done");
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
			if (args[0].equalsIgnoreCase("GENERATE_HASHES"))
			{
				if (args.length != 3)
				{
					System.err.println("Incorrect number of args to GENERATE_HASHES");
					return -1;
				}

				File sourceDirectory = new File(args[1]);
				File outputHashFile = new File(args[2]);
				outputHashListToFile(generateHashes(sourceDirectory, true), outputHashFile);
			}
			else if (args[0].equalsIgnoreCase("MOVE_EXCLUDE_DIR") || args[0].equalsIgnoreCase("MOVE_INCLUDE_DIR"))
			{
				if (args.length != 4)
				{
					System.err.println("Incorrect number of args to " + args[0]);
					return -1;
				}

				boolean include = args[0].equalsIgnoreCase("MOVE_INCLUDE_DIR");
				File truthDirectory = new File(args[1]);
				File sourceDirectory = new File(args[2]);
				File targetDirectory = new File(args[3]);

				if (!targetDirectory.exists())
					targetDirectory.mkdirs();

				Map<FileHash, File> truthHashes = generateHashes(truthDirectory, /* recursive = */true);
				Map<FileHash, File> sourceHashes = generateHashes(sourceDirectory, /* recursive = */false);

				System.out.println("Moving files " + (include ? "in" : "not in") + " in directory: "
						+ sourceDirectory.getAbsolutePath());
				int movedCount = 0;
				for (Map.Entry<FileHash, File> entry : sourceHashes.entrySet())
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
					}
				}
				System.out.println("Moved " + movedCount + " files");
			}
			else if (args[0].equalsIgnoreCase("MOVE_EXCLUDE") || args[0].equalsIgnoreCase("MOVE_INCLUDE"))
			{
				if (args.length != 4)
				{
					System.err.println("Incorrect number of args to " + args[0]);
					return -1;
				}

				boolean include = args[0].equalsIgnoreCase("MOVE_INCLUDE");
				File sourceDirectory = new File(args[1]);
				File targetDirectory = new File(args[2]);
				File existingHashFile = new File(args[3]);

				Map<FileHash, File> existingHashes = inputHashListFromFile(existingHashFile);
				Map<FileHash, File> sourceHashes = generateHashes(sourceDirectory, false);

				for (Map.Entry<FileHash, File> entry : sourceHashes.entrySet())
				{
					boolean isInList = existingHashes.containsKey(entry.getKey());
					if (include == isInList)
					{
						File sourceFile = entry.getValue();
						File targetFile = new File(targetDirectory, sourceFile.getName());
						sourceFile.renameTo(targetFile);
					}
				}
			}
			else if (args[0].equalsIgnoreCase("LIST_EXCLUDE") || args[0].equalsIgnoreCase("LIST_INCLUDE"))
			{
				if (args.length != 3)
				{
					System.err.println("Incorrect number of args to " + args[0]);
					return -1;
				}

				boolean include = args[0].equalsIgnoreCase("LIST_INCLUDE");
				File sourceDirectory = new File(args[1]);
				File existingHashFile = new File(args[2]);

				Map<FileHash, File> existingHashes = inputHashListFromFile(existingHashFile);
				Map<FileHash, File> sourceHashes = generateHashes(sourceDirectory, false);

				System.out.println("Listing files " + (include ? "included" : "not included") + " in hash file: ");
				for (Map.Entry<FileHash, File> entry : sourceHashes.entrySet())
				{
					boolean isInList = existingHashes.containsKey(entry.getKey());
					if (include == isInList)
					{
						System.out.println(entry.getValue().getAbsolutePath());
					}
				}
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

	private static void printHelp()
	{
		System.out.println("Usage:");
		System.out.println("");
		System.out.println("GENERATE_HASHES <sourceDirectory> <outputHashFile>");
		System.out.println("    Generates a list of hashes for all files in the given directory");
		System.out.println("");
		System.out.println("MOVE_EXCLUDE <sourceDirectory> <targetDirectory> <excludeHashFile>");
		System.out.println("    Moves any file from the source to target UNLESS it is in the exclude hash list");
		System.out.println("");
		System.out.println("MOVE_INCLUDE <sourceDirectory> <targetDirectory> <includeHashFile>");
		System.out.println("    Moves any file from the source to target ONLY IF it is in the include hash list");
		System.out.println("");
		System.out.println("MOVE_EXCLUDE_DIR <truthDir> <sourceDirectory> <targetDirectory>");
		System.out.println("    Moves any file from the source to target UNLESS it is in <truthDir>");
		System.out.println("");
		System.out.println("MOVE_INCLUDE_DIR <truthDir> <sourceDirectory> <targetDirectory>");
		System.out.println("    Moves any file from the source to target ONLY IF it is in <truthDir>");
	}

	public static Map<FileHash, File> generateHashes(File directory, boolean recursive) throws IOException
	{
		Map<File, FileHash> cachedHashes = new HashMap<File, FileHash>();
		File cachedHashesFile = new File(directory, "_hashes.txt");
		if (cachedHashesFile.exists())
		{
			for (Map.Entry<FileHash, File> entry : inputHashListFromFile(cachedHashesFile).entrySet())
			{
				cachedHashes.put(entry.getValue(), entry.getKey());
			}
		}

		Map<FileHash, File> hashes = new HashMap<FileHash, File>();
		ExtensionFileFilter filter = new ExtensionFileFilter("", /* matchDirectories = */true, "wav", "ogg");
		File[] fileList = directory.listFiles(filter);
		if (fileList == null)
		{
			System.err.println("Directory does not contain any files: " + directory);
			return hashes;
		}

		List<File> files = new ArrayList<File>(Arrays.asList(fileList));
		int i = 0, lastPct = 0;
		for (int j = 0; j < files.size(); j++)
		{
			if (files.get(j).isDirectory())
			{
				if (recursive)
				{
					File[] fileList2 = files.get(j).listFiles(filter);
					if (fileList2 != null)
						files.addAll(Arrays.asList(fileList2));
				}
				continue;
			}

			File wavFile = files.get(j);
			i++;
			if (verbose)
			{
				int pct = (i * 10) / files.size();
				if (pct > lastPct)
				{
					lastPct = pct;
					System.out.format("Generating hashes in %s: %3d%% [%4d/%4d]\n", directory.getPath(), pct * 10, i,
							files.size());
				}
			}

			FileHash cachedHash = cachedHashes.get(wavFile);
			if (cachedHash != null)
				hashes.put(cachedHash, wavFile);
			else
				hashes.put(new FileHash(hashFile(wavFile)), wavFile);
		}

		Map<FileHash, File> cachedHashesOutput = new HashMap<FileHash, File>(hashes);
		for (Map.Entry<File, FileHash> entry : cachedHashes.entrySet())
		{
			if (!cachedHashesOutput.containsKey(entry.getValue()))
				cachedHashesOutput.put(entry.getValue(), entry.getKey());
		}
		outputHashListToFile(cachedHashesOutput, cachedHashesFile);

		return hashes;
	}

	private static void outputHashListToFile(Map<FileHash, File> hashes, File outputFile) throws IOException
	{
		if (verbose)
			System.out.println("Writing hashes to: " + outputFile.getAbsolutePath());

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile)))
		{
			for (Map.Entry<FileHash, File> entry : hashes.entrySet())
			{
				writer.write(entry.getKey() + "\t" + entry.getValue().getAbsolutePath() + "\r\n");
			}
		}

		if (verbose)
			System.out.println("Done writing hashes");
	}

	private static Map<FileHash, File> inputHashListFromFile(File hashListFile) throws IOException
	{
		if (verbose)
			System.out.println("Reading hashes from: " + hashListFile.getAbsolutePath());

		Map<FileHash, File> hashes = new HashMap<FileHash, File>();
		try (BufferedReader reader = new BufferedReader(new FileReader(hashListFile)))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				int tab = line.indexOf('\t');
				if (tab <= 0)
					continue;

				File file = new File(line.substring(tab + 1));
				if (!file.exists())
					continue;

				hashes.put(new FileHash(line.substring(0, tab)), file);
			}
		}

		if (verbose)
			System.out.println("Done reading hashes");

		return hashes;
	}

	private static byte[] buffer = null;
	private static MessageDigest hashFunction = null;

	private static byte[] hashFile(File file) throws IOException
	{
		if (buffer == null)
			buffer = new byte[8192];

		try
		{
			if (hashFunction == null)
				hashFunction = MessageDigest.getInstance("SHA-256");
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new RuntimeException(e);
		}

		hashFunction.reset();

		try (FileInputStream stream = new FileInputStream(file))
		{
			int bytesRead;
			while ((bytesRead = stream.read(buffer)) != -1)
				hashFunction.update(buffer, 0, bytesRead);
		}

		return hashFunction.digest();
	}

	private static class FileHash
	{
		public final byte[] value;

		public FileHash(byte[] value)
		{
			this.value = value;
		}

		public FileHash(String hexString)
		{
			this.value = DatatypeConverter.parseHexBinary(hexString);
		}

		@Override public String toString()
		{
			return DatatypeConverter.printHexBinary(value);
		}

		@Override public boolean equals(Object obj)
		{
			if (obj == null || obj.getClass() != FileHash.class)
				return false;

			return Arrays.equals(value, ((FileHash) obj).value);
		}

		@Override public int hashCode()
		{
			return Arrays.hashCode(value);
		}
	}

}
