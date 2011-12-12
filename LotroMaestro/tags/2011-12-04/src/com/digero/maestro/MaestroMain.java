package com.digero.maestro;

import javax.swing.JFrame;
import javax.swing.UIManager;

import com.digero.common.util.Version;
import com.digero.maestro.view.ProjectFrame;

public class MaestroMain {
	public static final String APP_NAME = "LOTRO Maestro";
	public static final String APP_URL = "http://lotro.acasylum.com/maestro";
	public static final Version APP_VERSION = new Version(0, 2, 1);
	
	public static void main(String[] args) throws Exception {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e) {
		}
		
		ProjectFrame project = new ProjectFrame();
		project.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		project.setVisible(true);
	}
}
