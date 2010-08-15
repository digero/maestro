package com.digero.maestro;

import javax.swing.JFrame;
import javax.swing.UIManager;

import com.digero.maestro.view.ProjectFrame;

public class MaestroMain {
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
