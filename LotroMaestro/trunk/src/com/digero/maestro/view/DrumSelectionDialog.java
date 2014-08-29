package com.digero.maestro.view;

import javax.sound.midi.Receiver;
import javax.swing.JDialog;
import javax.swing.JFrame;

public class DrumSelectionDialog extends JDialog
{
	public DrumSelectionDialog(JFrame owner, Receiver midiReceiver, Receiver lotroReceiver)
	{
		super(owner);
	}
}
