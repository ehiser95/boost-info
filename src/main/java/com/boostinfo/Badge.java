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
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.ui.overlay.components.TextComponent;

/**
 * Compact layout A: a small square with the icon filling it and a short time in the corner,
 * like a RuneLite infobox at about two-thirds size.
 */
class Badge implements LayoutableRenderableEntity
{
	static final int SIZE = 26;

	private final Entry entry;
	private final Color bg;

	@Setter
	private Point preferredLocation = new Point();
	@Setter
	private Dimension preferredSize = new Dimension();
	@Getter
	private final Rectangle bounds = new Rectangle();

	Badge(Entry entry, Color bg)
	{
		this.entry = entry;
		this.bg = bg;
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		int x = preferredLocation.x;
		int y = preferredLocation.y;
		Composite old = g.getComposite();
		Font oldFont = g.getFont();
		if (entry.alpha < 1f)
		{
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, entry.alpha));
		}

		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(bg);
		g.fillRoundRect(x, y, SIZE, SIZE, 6, 6);
		if (entry.icon != null)
		{
			Icons.draw(g, entry.icon, x + 2, y + 1, SIZE - 4);
		}

		g.setFont(FontManager.getRunescapeSmallFont());
		FontMetrics fm = g.getFontMetrics();
		String text = shortValue(entry.value);
		TextComponent value = new TextComponent();
		value.setText(text);
		value.setColor(entry.valueColor());
		value.setOutline(true);
		value.setPosition(new Point(x + SIZE - fm.stringWidth(text) - 1, y + SIZE - 2));
		value.render(g);

		g.setFont(oldFont);
		g.setComposite(old);
		Dimension d = new Dimension(SIZE, SIZE);
		bounds.setLocation(preferredLocation);
		bounds.setSize(d);
		return d;
	}

	/**
	 * Fit the value in the corner: "42" (seconds) under a minute, "3:05" under ten minutes,
	 * "18m" or "1h" beyond that, and "On" for effects without a countdown.
	 */
	static String shortValue(String value)
	{
		if ("Active".equals(value))
		{
			return "On";
		}
		String[] parts = value.split(":");
		try
		{
			if (parts.length == 3)
			{
				return Integer.parseInt(parts[0]) + "h";
			}
			if (parts.length == 2)
			{
				int m = Integer.parseInt(parts[0]);
				int s = Integer.parseInt(parts[1]);
				if (m == 0)
				{
					return String.valueOf(s);
				}
				if (m < 10)
				{
					return value;
				}
				return m >= 60 ? (m / 60) + "h" : m + "m";
			}
		}
		catch (NumberFormatException ignored)
		{
			// Not a time: counters and ammo counts are shown as they are
		}
		return value;
	}
}
