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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.DatatypeConverter;

import com.digero.common.util.ExtensionFileFilter;

public class HashGenerator
{
	static boolean verbose = true;

	public static void main(String[] args)
	{
		System.exit(run(args));
	}

	private static void listMatchesInDirectories(File dirA, File dirB)
	{
		try
		{
			Map<FileHash, File> hashesA = generateHashes(dirA);
			Map<FileHash, File> hashesB = generateHashes(dirB);

			for (Map.Entry<FileHash, File> entryA : hashesA.entrySet())
			{
				if (hashesB.containsKey(entryA.getKey()))
				{
					System.out.println("\"" + entryA.getValue() + "\"\t\"" + hashesB.get(entryA.getKey()) + "\"");
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
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
				outputHashListToFile(generateHashes(sourceDirectory).keySet(), outputHashFile);
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

				Set<FileHash> existingHashes = inputHashListFromFile(existingHashFile);
				Map<FileHash, File> sourceHashes = generateHashes(sourceDirectory);

				for (Map.Entry<FileHash, File> entry : sourceHashes.entrySet())
				{
					boolean isInList = existingHashes.contains(entry.getKey());
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

				Set<FileHash> existingHashes = inputHashListFromFile(existingHashFile);
				Map<FileHash, File> sourceHashes = generateHashes(sourceDirectory);

				System.out.println("Listing files " + (include ? "included" : "not included") + " in hash file: ");
				for (Map.Entry<FileHash, File> entry : sourceHashes.entrySet())
				{
					boolean isInList = existingHashes.contains(entry.getKey());
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
	}

	public static Map<FileHash, File> generateHashes(File directory) throws IOException
	{
		Map<FileHash, File> hashes = new HashMap<FileHash, File>();
		ExtensionFileFilter filter = new ExtensionFileFilter("", true, "wav", "ogg");
		List<File> files = new ArrayList<File>(Arrays.asList(directory.listFiles(filter)));
		int i = 0, lastPct = 0;
		for (int j = 0; j < files.size(); j++)
		{
			if (files.get(j).isDirectory())
			{
				files.addAll(Arrays.asList(files.get(j).listFiles(filter)));
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
			hashes.put(new FileHash(hashFile(wavFile)), wavFile);
		}
		return hashes;
	}

	private static void outputHashListToFile(Set<FileHash> hashes, File outputFile) throws IOException
	{
		if (verbose)
			System.out.println("Writing hashes to: " + outputFile.getAbsolutePath());

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile)))
		{
			for (FileHash hash : hashes)
			{
				writer.write(hash + "\r\n");
			}
		}
	}

	private static Set<FileHash> inputHashListFromFile(File hashListFile) throws IOException
	{
		if (verbose)
			System.out.println("Reading hashes from: " + hashListFile.getAbsolutePath());

		Set<FileHash> hashes = new HashSet<FileHash>();
		try (BufferedReader reader = new BufferedReader(new FileReader(hashListFile)))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				hashes.add(new FileHash(line));
			}
		}
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
