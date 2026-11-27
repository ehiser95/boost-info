package com.boostinfo;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.ui.overlay.components.TextComponent;

/**
 * Layout B: icon on top, value underneath, and a thin bar showing time left (timers only).
 */
class Tile implements LayoutableRenderableEntity
{
	private static final int ICON = 16;
	static final Color BG = new Color(36, 31, 25);
	private static final Color BAR_BG = new Color(58, 49, 40);

	private final Entry entry;
	private final Color bg;
	private final int width;

	@Setter
	private Point preferredLocation = new Point();
	@Setter
	private Dimension preferredSize = new Dimension();
	@Getter
	private final Rectangle bounds = new Rectangle();

	Tile(Entry entry, int width, Color bg)
	{
		this.entry = entry;
		this.bg = bg;
		this.width = width;
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		FontMetrics fm = g.getFontMetrics();
		boolean bar = entry.fraction >= 0;
		int height = 3 + ICON + 1 + fm.getHeight() + (bar ? 5 : 2);
		int x = preferredLocation.x;
		int y = preferredLocation.y;

		Composite old = g.getComposite();
		if (entry.alpha < 1f)
		{
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, entry.alpha));
		}

		g.setColor(bg);
		g.fillRect(x, y, width, height);

		if (entry.icon != null)
		{
			Icons.draw(g, entry.icon, x + (width - ICON) / 2, y + 3, ICON);
		}

		int valueWidth = fm.stringWidth(entry.value);
		TextComponent value = new TextComponent();
		value.setText(entry.value);
		value.setColor(entry.valueColor());
		value.setPosition(new Point(x + (width - valueWidth) / 2, y + 3 + ICON + 1 + fm.getAscent()));
		value.render(g);

		if (bar)
		{
			int barY = y + height - 3;
			g.setColor(BAR_BG);
			g.fillRect(x, barY, width, 3);
			g.setColor(entry.color);
			g.fillRect(x, barY, (int) Math.round(width * Math.min(1, entry.fraction)), 3);
		}

		g.setComposite(old);

		Dimension d = new Dimension(width, height);
		bounds.setLocation(preferredLocation);
		bounds.setSize(d);
		return d;
	}
}
