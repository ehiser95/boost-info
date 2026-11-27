package com.boostinfo;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.ui.overlay.components.TextComponent;

/**
 * Compact layout C: every timer on one shared bar, icon and time, with small dots between them.
 * Vertical stacks them in a narrow column instead.
 */
class Strip implements LayoutableRenderableEntity
{
	private static final int ICON = 12;
	private static final int PAD = 4;
	private static final int GAP = 4;
	private static final Color DOT = new Color(106, 95, 78);

	private final List<Entry> entries;
	private final boolean vertical;
	private final Color bg;

	@Setter
	private Point preferredLocation = new Point();
	@Setter
	private Dimension preferredSize = new Dimension();
	@Getter
	private final Rectangle bounds = new Rectangle();

	Strip(List<Entry> entries, boolean vertical, Color bg)
	{
		this.entries = entries;
		this.vertical = vertical;
		this.bg = bg;
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		Font oldFont = g.getFont();
		g.setFont(FontManager.getRunescapeSmallFont());
		FontMetrics fm = g.getFontMetrics();
		int rowHeight = Math.max(ICON, fm.getHeight()) + 1;

		// Size first: one row (horizontal) or one row per entry (vertical)
		int width = 0;
		int height;
		if (vertical)
		{
			for (Entry e : entries)
			{
				width = Math.max(width, ICON + 3 + fm.stringWidth(e.value));
			}
			width += PAD * 2;
			height = entries.size() * rowHeight + PAD;
		}
		else
		{
			for (int i = 0; i < entries.size(); i++)
			{
				width += ICON + 3 + fm.stringWidth(entries.get(i).value) + (i > 0 ? GAP * 2 + 3 : 0);
			}
			width += PAD * 2;
			height = rowHeight + PAD;
		}

		int x0 = preferredLocation.x;
		int y0 = preferredLocation.y;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(bg);
		g.fillRoundRect(x0, y0, width, height, 6, 6);

		int x = x0 + PAD;
		int y = y0 + PAD / 2;
		for (int i = 0; i < entries.size(); i++)
		{
			Entry e = entries.get(i);
			if (!vertical && i > 0)
			{
				g.setColor(DOT);
				g.fillOval(x + GAP, y + rowHeight / 2 - 1, 3, 3);
				x += GAP * 2 + 3;
			}

			Composite old = g.getComposite();
			if (e.alpha < 1f)
			{
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, e.alpha));
			}
			if (e.icon != null)
			{
				Icons.draw(g, e.icon, x, y + (rowHeight - ICON) / 2, ICON);
			}
			TextComponent value = new TextComponent();
			value.setText(e.value);
			value.setColor(e.valueColor());
			value.setPosition(new Point(x + ICON + 3, y + (rowHeight - fm.getHeight()) / 2 + fm.getAscent()));
			value.render(g);
			g.setComposite(old);

			if (vertical)
			{
				y += rowHeight;
			}
			else
			{
				x += ICON + 3 + fm.stringWidth(e.value);
			}
		}

		g.setFont(oldFont);
		Dimension d = new Dimension(width, height);
		bounds.setLocation(preferredLocation);
		bounds.setSize(d);
		return d;
	}
}
