package com.digero.common.view;

import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import com.digero.common.icons.IconLoader;
import com.digero.common.util.Util;
import com.digero.common.util.Version;

public final class AboutDialog {
	public static void show(JFrame parent, final String appName, final Version appVersion, final String appUrl,
			final String iconName) {
		ImageIcon aboutIcon;
		try {
			aboutIcon = new ImageIcon(ImageIO.read(IconLoader.class.getResourceAsStream(iconName)));
		}
		catch (IOException e1) {
			e1.printStackTrace();
			aboutIcon = null;
		}
		JLabel aboutMessage = new JLabel("<html>" //
				+ appName + "<br>" //
				+ "Version " + appVersion + "<br>" //
				+ "Created by Digero of Landroval<br>" //
				+ "Copyright &copy; 2011 Ben Howell<br>" //
				+ "<a href='" + appUrl + "'>" + appUrl + "</a>" //
				+ "</html>");
		aboutMessage.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		aboutMessage.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					Util.openURL(appUrl);
				}
			}
		});
		String aboutTitle = "About " + appName;
		JOptionPane.showMessageDialog(parent, aboutMessage, aboutTitle, JOptionPane.INFORMATION_MESSAGE, aboutIcon);
	}

	/** Static-only class */
	private AboutDialog() {
	}
}
