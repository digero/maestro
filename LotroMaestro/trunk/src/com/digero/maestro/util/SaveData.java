package com.digero.maestro.util;

import java.util.Properties;

import com.digero.common.midi.KeySignature;
import com.digero.common.midi.TimeSignature;

public class SaveData extends Properties {
	public static void main(String[] args) throws Exception {
		SaveData data = new SaveData();
		data.setBoolean("bool", true);
		data.setString("str", "hello, test=world");

		data.store(System.out, null);
//		System.out.println(data);
	}

	public void setString(String key, String value) {
		setProperty(key, value);
	}

	public String getString(String key, String defaultValue) {
		return getProperty(key, defaultValue);
	}

	public void setInt(String key, int value) {
		setProperty(key, String.valueOf(value));
	}

	public int getInt(String key, int defaultValue) {
		String strVal = getProperty(key);
		if (strVal != null) {
			try {
				return Integer.parseInt(strVal);
			}
			catch (NumberFormatException ex) {}
		}
		return defaultValue;
	}

	public void setLong(String key, long value) {
		setProperty(key, String.valueOf(value));
	}

	public long getLong(String key, long defaultValue) {
		String strVal = getProperty(key);
		if (strVal != null) {
			try {
				return Long.parseLong(strVal);
			}
			catch (NumberFormatException ex) {}
		}
		return defaultValue;
	}

	public void setDouble(String key, double value) {
		setProperty(key, String.valueOf(value));
	}

	public double getDouble(String key, double defaultValue) {
		String strVal = getProperty(key);
		if (strVal != null) {
			try {
				return Double.parseDouble(strVal);
			}
			catch (NumberFormatException ex) {}
		}
		return defaultValue;
	}

	public void setFloat(String key, float value) {
		setProperty(key, String.valueOf(value));
	}

	public float getFloat(String key, float defaultValue) {
		String strVal = getProperty(key);
		if (strVal != null) {
			try {
				return Float.parseFloat(strVal);
			}
			catch (NumberFormatException ex) {}
		}
		return defaultValue;
	}

	public void setBoolean(String key, boolean value) {
		setProperty(key, String.valueOf(value));
	}

	public boolean getBoolean(String key, boolean defaultValue) {
		String strVal = getProperty(key);
		if (strVal != null) {
			try {
				return Boolean.parseBoolean(strVal);
			}
			catch (NumberFormatException ex) {}
		}
		return defaultValue;
	}

	public void setKeySignature(String key, KeySignature value) {
		setProperty(key, String.valueOf(value));
	}

	public KeySignature getKeySignature(String key, KeySignature defaultValue) {
		String strVal = getProperty(key);
		if (strVal != null) {
			try {
				return new KeySignature(strVal);
			}
			catch (IllegalArgumentException ex) {}
		}
		return defaultValue;
	}

	public void setTimeSignature(String key, TimeSignature value) {
		setProperty(key, String.valueOf(value));
	}

	public TimeSignature getTimeSignature(String key, TimeSignature defaultValue) {
		String strVal = getProperty(key);
		if (strVal != null) {
			try {
				return new TimeSignature(strVal);
			}
			catch (IllegalArgumentException ex) {}
		}
		return defaultValue;
	}
}
