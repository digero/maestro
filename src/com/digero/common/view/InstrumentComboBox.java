package com.digero.common.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.ListDataListener;

import com.digero.common.abc.LotroInstrument;
import com.digero.common.abc.LotroInstrumentGroup;

@SuppressWarnings("rawtypes")
public class InstrumentComboBox extends JComboBox<LotroInstrument>
{
	private final List<Object> items;
	private int selectedIndex = 0;

	@SuppressWarnings("unchecked") public InstrumentComboBox()
	{
		ArrayList<Object> items = new ArrayList<Object>(Arrays.asList(LotroInstrument.values()));

		items.addAll(Arrays.asList(LotroInstrumentGroup.values()));
		Collections.sort(items, new Comparator<Object>()
		{
			@Override public int compare(Object a, Object b)
			{
				if (a == b)
					return 0;

				LotroInstrumentGroup groupA = groupOf(a);
				LotroInstrumentGroup groupB = groupOf(b);
				if (groupA != groupB)
					return groupA.ordinal() - groupB.ordinal();

				// Groups always come before their items
				if (a instanceof LotroInstrumentGroup)
					return -1;
				if (b instanceof LotroInstrumentGroup)
					return 1;

				LotroInstrument instA = (LotroInstrument) a;
				LotroInstrument instB = (LotroInstrument) b;
				return instA.ordinal() - instB.ordinal();
			}

			private LotroInstrumentGroup groupOf(Object o)
			{
				return (o instanceof LotroInstrumentGroup) ? (LotroInstrumentGroup) o : //
						LotroInstrumentGroup.groupOf((LotroInstrument) o);
			}
		});

		this.items = Collections.unmodifiableList(items);

		while (!(items.get(selectedIndex) instanceof LotroInstrument))
			selectedIndex++;

		setMaximumRowCount(items.size());
		setModel(new Model());
		setRenderer(new Renderer());
	}

	private class Model implements ComboBoxModel
	{
		@Override public int getSize()
		{
			return items.size();
		}

		@Override public Object getElementAt(int index)
		{
			return items.get(index);
		}

		@Override public void setSelectedItem(Object item)
		{
			for (int i = items.indexOf(item); i >= 0 && i < items.size(); i += (i < selectedIndex) ? -1 : 1)
			{
				if (items.get(i) instanceof LotroInstrument)
				{
					selectedIndex = i;
					break;
				}
			}
		}

		@Override public Object getSelectedItem()
		{
			return items.get(selectedIndex);
		}

		@Override public void addListDataListener(ListDataListener l)
		{
			// Nothing ever changes
		}

		@Override public void removeListDataListener(ListDataListener l)
		{
		}
	}

	private static class Renderer implements ListCellRenderer
	{
		private final JLabel label;
		private final Font font, fontGroupHeader;
		private final Border border, borderFocused;
		private final Color foreground, foregroundHighlight, foregroundGroup;
		private final Color background, backgroundHighlight, backgroundGroup;

		private Renderer()
		{
			label = new JLabel();
			font = label.getFont();
			fontGroupHeader = font.deriveFont(Font.BOLD);

			foreground = UIManager.getColor("List.foreground");
			background = UIManager.getColor("List.background");
			foregroundHighlight = UIManager.getColor("List.selectionForeground");
			backgroundHighlight = UIManager.getColor("List.selectionBackground");

			foregroundGroup = foreground;

			float[] hsb = Color.RGBtoHSB(background.getRed(), background.getGreen(), background.getBlue(), null);
			hsb[2] += (hsb[2] < 0.5f) ? 0.08f : -0.08f;
			backgroundGroup = Color.getHSBColor(hsb[0], hsb[1], hsb[2]);

			final int PAD_X = 4, PAD_Y = 2;
			border = BorderFactory.createEmptyBorder(PAD_Y, PAD_X, PAD_Y, PAD_X);
			borderFocused = BorderFactory.createCompoundBorder(BorderFactory.createDashedBorder(null),
					BorderFactory.createEmptyBorder(PAD_Y - 1, PAD_X - 1, PAD_Y - 1, PAD_X - 1));
		}

		@Override public Component getListCellRendererComponent(JList list, Object value, int index,
				boolean isSelected, boolean cellHasFocus)
		{
			label.setText(value.toString());
			if (value instanceof LotroInstrumentGroup)
			{
				label.setFont(fontGroupHeader);
				label.setBorder(border);
				label.setForeground(foregroundGroup);
				label.setBackground(backgroundGroup);
				label.setOpaque(true);
			}
			else
			{
				boolean highlight = isSelected && (index >= 0);
				label.setFont(font);
				label.setBorder(cellHasFocus || (isSelected && index < 0) ? borderFocused : border);
				label.setForeground(highlight ? foregroundHighlight : foreground);
				label.setBackground(highlight ? backgroundHighlight : background);
				label.setOpaque(highlight);
			}
			return label;
		}
	}

}
