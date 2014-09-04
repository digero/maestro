package com.digero.common.icons;

import java.net.URL;

import javax.swing.ImageIcon;

/**
 * This is just here for the purposes of IconLoader.class.getResource()
 */
public class IconLoader
{
	public static ImageIcon getImageIcon(String name)
	{
		return new ImageIcon(IconLoader.class.getResource(name));
	}

	public static URL getUrl(String name)
	{
		return IconLoader.class.getResource(name);
	}
}
