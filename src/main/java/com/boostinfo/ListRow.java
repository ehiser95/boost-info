package com.boostinfo;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.ui.overlay.components.TextComponent;

/**
 * One line of the compact list: [icon] name ........ value
 * The value never overlaps the icon.
 */
@Setter
public class ListRow implements LayoutableRenderableEntity
{
	private static final int ICON_SIZE = 16;
	private static final int ICON_GAP = 5;

	private BufferedImage icon;
	private String name = "";
	private Color nameColor = new Color(232, 226, 214);
	private String value = "";
	private Color valueColor = Color.WHITE;
	private float alpha = 1f;

	private Point preferredLocation = new Point();
	private Dimension preferredSize = new Dimension(140, 0);

	@Getter
	private final Rectangle bounds = new Rectangle();

	@Override
	public Dimension render(Graphics2D graphics)
	{
		FontMetrics fm = graphics.getFontMetrics();
		int x = preferredLocation.x;
		int y = preferredLocation.y;
		int width = preferredSize.width;
		int height = Math.max(ICON_SIZE, fm.getHeight()) + 2;
		int baseline = y + (height - fm.getHeight()) / 2 + fm.getAscent();

		Composite old = graphics.getComposite();
		if (alpha < 1f)
		{
			graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, alpha)));
		}

		int textX = x;
		if (icon != null)
		{
			// Scale to fit 16x16, keeping the aspect ratio
			double scale = Math.min((double) ICON_SIZE / icon.getWidth(), (double) ICON_SIZE / icon.getHeight());
			int w = (int) Math.round(icon.getWidth() * scale);
			int h = (int) Math.round(icon.getHeight() * scale);
			graphics.drawImage(icon, x + (ICON_SIZE - w) / 2, y + (height - h) / 2, w, h, null);
			textX = x + ICON_SIZE + ICON_GAP;
		}

		int valueWidth = fm.stringWidth(value);
		String shownName = fit(fm, name, width - (textX - x) - valueWidth - 6);

		TextComponent nameText = new TextComponent();
		nameText.setText(shownName);
		nameText.setColor(nameColor);
		nameText.setPosition(new Point(textX, baseline));
		nameText.render(graphics);

		TextComponent valueText = new TextComponent();
		valueText.setText(value);
		valueText.setColor(valueColor);
		valueText.setPosition(new Point(x + width - valueWidth, baseline));
		valueText.render(graphics);

		graphics.setComposite(old);

		Dimension d = new Dimension(width, height);
		bounds.setLocation(preferredLocation);
		bounds.setSize(d);
		return d;
	}

	private static String fit(FontMetrics fm, String text, int maxWidth)
	{
		if (text.isEmpty() || fm.stringWidth(text) <= maxWidth)
		{
			return text;
		}
		String t = text;
		while (t.length() > 1 && fm.stringWidth(t + "…") > maxWidth)
		{
			t = t.substring(0, t.length() - 1);
		}
		return t + "…";
	}
}
