package com.digero.common.icons;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
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

	public static ImageIcon getDisabledIcon(String name)
	{
		try
		{
			BufferedImage img = ImageIO.read(IconLoader.class.getResource(name));
			int width = img.getWidth();
			int height = img.getHeight();
			int[] argbArray = new int[width];
			float[] hsb = null;
			final int H = 0, S = 1, B = 2;
			for (int y = 0; y < height; y++)
			{
				img.getRGB(0, y, width, 1, argbArray, 0, width);
				for (int x = 0; x < width; x++)
				{
					int argb = argbArray[x];
					int r = (argb >>> 16) & 0xFF;
					int g = (argb >>> 8) & 0xFF;
					int b = (argb >>> 0) & 0xFF;

					hsb = Color.RGBtoHSB(r, g, b, hsb);
					hsb[S] = 0.0f;
					final float c = 0.5f;
					final float d = 0.1f;
					hsb[B] = (c - d) + (1 - c) * hsb[B];

					argbArray[x] = (argb & 0xFF000000) | (Color.HSBtoRGB(hsb[H], hsb[S], hsb[B]) & 0x00FFFFFF);
				}
				img.setRGB(0, y, width, 1, argbArray, 0, width);
			}
			return new ImageIcon(img);
		}
		catch (IOException e)
		{
			assert false;
			e.printStackTrace();
			return new ImageIcon(IconLoader.class.getResource(name));
		}
	}
}
