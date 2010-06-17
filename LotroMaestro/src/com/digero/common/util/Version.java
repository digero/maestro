package com.digero.common.util;

public class Version implements Comparable<Version> {
	public static Version parseVersion(String versionString) {
		if (versionString == null)
			return null;

		String[] parts = versionString.trim().split("\\.");
		int major, minor = 0, revision = 0, build = 0;

		try {
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
		catch (NumberFormatException e) {
			return null;
		}
	}

	public final int major, minor, revision, build;

	public Version(int major, int minor, int revision, int build) {
		this.major = major;
		this.minor = minor;
		this.revision = revision;
		this.build = build;
	}

	@Override
	public int compareTo(Version that) {
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

	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}

		Version that = (Version) obj;
		return (this.major == that.major) && (this.minor == that.minor) && (this.revision == that.revision)
				&& (this.build == that.build);
	}

	@Override
	public int hashCode() {
		return (major << 15) ^ (minor << 10) ^ (revision << 5) ^ build;
	}

	@Override
	public String toString() {
		return major + "." + minor + "." + revision + "." + build;
	}
}