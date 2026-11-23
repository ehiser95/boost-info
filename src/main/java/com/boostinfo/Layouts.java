package com.boostinfo;

import java.awt.Dimension;
import java.awt.Point;
import java.util.List;
import net.runelite.client.ui.overlay.components.ComponentOrientation;
import net.runelite.client.ui.overlay.components.PanelComponent;

/**
 * Fills an overlay panel with entries in the chosen layout and orientation.
 */
final class Layouts
{
	private Layouts()
	{
	}

	/**
	 * @param timers true for timer lists (wider rows/tiles with a time bar)
	 */
	static void fill(PanelComponent panel, LayoutStyle style, Orientation orientation, List<Entry> entries, boolean timers)
	{
		boolean horizontal = orientation == Orientation.HORIZONTAL;
		switch (style)
		{
			case CHIPS:
				if (horizontal)
				{
					// Side by side, wrapping onto a new row when wide
					horizontalWrap(panel, 170, 3);
				}
				else
				{
					vertical(panel, 3);
				}
				for (Entry e : entries)
				{
					panel.getChildren().add(new Chip(e));
				}
				break;
			case TILES:
				if (horizontal)
				{
					horizontalWrap(panel, timers ? 172 : 176, 4);
				}
				else
				{
					vertical(panel, 4);
				}
				for (Entry e : entries)
				{
					panel.getChildren().add(new Tile(e, timers ? 84 : 40));
				}
				break;
			case LIST:
			default:
			{
				int rowWidth = timers ? 140 : 130;
				if (horizontal)
				{
					// One long row: rows keep their own width
					horizontalWrap(panel, entries.size() * (rowWidth + 6) + 1, 6);
				}
				else
				{
					panel.setOrientation(ComponentOrientation.VERTICAL);
					panel.setWrap(false);
					panel.setPreferredSize(new Dimension(rowWidth, 0));
					panel.setGap(new Point(0, 1));
				}
				for (Entry e : entries)
				{
					ListRow row = new ListRow();
					row.setPreferredSize(new Dimension(rowWidth, 0));
					row.setIcon(e.icon);
					row.setName(e.name);
					row.setValue(e.value);
					row.setValueColor(e.valueColor());
					if (e.nameColor != null)
					{
						row.setNameColor(e.nameColor);
					}
					row.setAlpha(e.alpha);
					panel.getChildren().add(row);
				}
				break;
			}
		}
	}

	private static void horizontalWrap(PanelComponent panel, int width, int gap)
	{
		panel.setOrientation(ComponentOrientation.HORIZONTAL);
		panel.setWrap(true);
		panel.setPreferredSize(new Dimension(width, 0));
		panel.setGap(new Point(gap, gap));
	}

	private static void vertical(PanelComponent panel, int gap)
	{
		panel.setOrientation(ComponentOrientation.VERTICAL);
		panel.setWrap(false);
		panel.setPreferredSize(new Dimension(0, 0));
		panel.setGap(new Point(gap, gap));
	}
}
