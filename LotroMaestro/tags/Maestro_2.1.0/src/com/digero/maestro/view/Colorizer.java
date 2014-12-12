package com.digero.maestro.view;

import info.clearthought.layout.TableLayout;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.digero.common.view.ColorTable;

public class Colorizer extends JPanel
{
	private JComboBox<ColorTable> picker;
	private SpinnerNumberModel hue;
	private SpinnerNumberModel sat;
	private SpinnerNumberModel brt;
	private boolean updating = false;
	private JPanel refresher;

	public Colorizer(JPanel coloredPanel)
	{
		super(new BorderLayout());
		this.refresher = coloredPanel;
		picker = new JComboBox<ColorTable>(ColorTable.values());

		picker.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				updateSpinners();
			}
		});

		hue = new SpinnerNumberModel(0.0, 0.0, 1.0, 0.01);
		sat = new SpinnerNumberModel(0.0, 0.0, 1.0, 0.05);
		brt = new SpinnerNumberModel(0.0, 0.0, 1.0, 0.05);

		ChangeListener cl = new ChangeListener()
		{
			@Override public void stateChanged(ChangeEvent e)
			{
				if (!updating)
				{
					float h = hue.getNumber().floatValue();
					float s = sat.getNumber().floatValue();
					float b = brt.getNumber().floatValue();

					((ColorTable) picker.getSelectedItem()).set(new Color(Color.HSBtoRGB(h, s, b)));
					refresher.repaint();
				}
			}
		};

		hue.addChangeListener(cl);
		sat.addChangeListener(cl);
		brt.addChangeListener(cl);

		JPanel spinners = new JPanel(new TableLayout(//
				new double[] { TableLayout.PREFERRED, 0.33, TableLayout.PREFERRED, 0.33, TableLayout.PREFERRED, 0.34 },//
				new double[] { TableLayout.PREFERRED }));
		spinners.add(new JLabel("H:"), "0, 0");
		spinners.add(new JSpinner(hue), "1, 0");
		spinners.add(new JLabel(" S:"), "2, 0");
		spinners.add(new JSpinner(sat), "3, 0");
		spinners.add(new JLabel(" B:"), "4, 0");
		spinners.add(new JSpinner(brt), "5, 0");

		add(picker, BorderLayout.NORTH);
		add(spinners, BorderLayout.CENTER);

		updateSpinners();
	}

	private void updateSpinners()
	{
		boolean updatingSav = updating;
		updating = true;

		Color c = ((ColorTable) picker.getSelectedItem()).get();
		float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);

		hue.setValue(Math.round(hsb[0] * 100.0f) / 100.0f);
		sat.setValue(Math.round(hsb[1] * 100.0f) / 100.0f);
		brt.setValue(Math.round(hsb[2] * 100.0f) / 100.0f);

		updating = updatingSav;
	}
}
