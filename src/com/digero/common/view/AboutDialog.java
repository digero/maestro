package com.digero.common.view;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.digero.common.icons.IconLoader;
import com.digero.common.util.Util;
import com.digero.common.util.Version;

public final class AboutDialog
{
	public static void show(JFrame parent, final String appName, final Version appVersion, final String appUrl,
			final String iconName)
	{
		ImageIcon aboutIcon;
		try
		{
			aboutIcon = new ImageIcon(ImageIO.read(IconLoader.class.getResourceAsStream(iconName)));
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
			aboutIcon = null;
		}
		JLabel aboutMessage = new JLabel("<html>" //
				+ appName + "<br>" //
				+ "Version " + appVersion + "<br>" //
				+ "Created by Digero of Landroval<br>" //
				+ "Copyright &copy; 2015 Ben Howell<br>" //
				+ "<a href='" + appUrl + "'>" + appUrl + "</a>" //
				+ "</html>");
		aboutMessage.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		aboutMessage.addMouseListener(new MouseAdapter()
		{
			@Override public void mouseClicked(MouseEvent e)
			{
				if (e.getButton() == MouseEvent.BUTTON1)
				{
					Util.openURL(appUrl);
				}
			}
		});

		JLabel javaMessage = new JLabel("Java version " + System.getProperty("java.version"));

		JPanel aboutPanel = new JPanel(new BorderLayout(0, 8));
		aboutPanel.add(aboutMessage, BorderLayout.CENTER);
		aboutPanel.add(javaMessage, BorderLayout.SOUTH);

		String aboutTitle = "About " + appName;
		JOptionPane.showMessageDialog(parent, aboutPanel, aboutTitle, JOptionPane.INFORMATION_MESSAGE, aboutIcon);
	}

	/** Static-only class */
	private AboutDialog()
	{
	}
}
