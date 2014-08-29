package com.digero.common.util;

public class Version implements Comparable<Version>
{
	public static Version parseVersion(String versionString)
	{
		if (versionString == null)
			return null;

		String[] parts = versionString.trim().split("[\\._]");
		int major, minor = 0, revision = 0, build = -1;

		try
		{
			if (parts.length == 0)
				return null;

			major = Integer.parseInt(parts[0]);
			if (parts.length > 1)
				minor = Integer.parseInt(parts[1]);
			if (parts.length > 2)
				revision = Integer.parseInt(parts[2]);
			if (parts.length > 3)
				build = Integer.parseInt(parts[3]);

			return new Version(major, minor, revision, build);
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	private final int major, minor, revision, build;

	public Version(int major, int minor, int revision)
	{
		this(major, minor, revision, -1);
	}

	public Version(int major, int minor, int revision, int build)
	{
		this.major = major;
		this.minor = minor;
		this.revision = revision;
		this.build = build;
	}

	public int getMajor()
	{
		return major;
	}

	public int getMinor()
	{
		return minor;
	}

	public int getRevision()
	{
		return revision;
	}

	public int getBuild()
	{
		return build;
	}

	public boolean hasBuild()
	{
		return build != -1;
	}

	@Override public int compareTo(Version that)
	{
		if (that == null)
			return 1;

		if (this.major != that.major)
			return this.major - that.major;

		if (this.minor != that.minor)
			return this.minor - that.minor;

		if (this.revision != that.revision)
			return this.revision - that.revision;

		return this.build - that.build;
	}

	@Override public boolean equals(Object obj)
	{
		if (obj == null || obj.getClass() != this.getClass())
		{
			return false;
		}

		Version that = (Version) obj;
		return (this.major == that.major) && (this.minor == that.minor) && (this.revision == that.revision)
				&& (this.build == that.build);
	}

	@Override public int hashCode()
	{
		return Integer.rotateLeft(major, 15) + Integer.rotateLeft(minor, 10) + Integer.rotateLeft(revision, 5) + build;
	}

	@Override public String toString()
	{
		String s = major + "." + minor + "." + revision;
		if (hasBuild())
			s += "." + build;
		return s;
	}
}