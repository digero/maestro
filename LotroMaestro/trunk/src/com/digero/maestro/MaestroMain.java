package com.digero.maestro;

import java.io.File;

import javax.swing.JFrame;
import javax.swing.UIManager;

import com.digero.maestro.midi.SequenceInfo;
import com.digero.maestro.view.ProjectFrame;

public class MaestroMain {
	public static void main(String[] args) throws Exception {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e) {
		}
		
		File mid = new File("C:\\Users\\Ben\\Documents\\Midi\\Folk\\Banana Boat Song.mid");
		SequenceInfo seqInfo = new SequenceInfo(mid);

		ProjectFrame project = new ProjectFrame(seqInfo);
		project.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		project.setVisible(true);
	}
}
