package com.boostinfo;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import net.runelite.api.gameval.ItemID;
import javax.inject.Inject;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * A big pulsing "Use X potion" alert with the potion's picture, in the middle of the screen.
 */
public class ReminderOverlay extends Overlay
{
	private static final Color BG = new Color(20, 16, 12, 200);
	private static final Color BORDER = new Color(255, 90, 79);
	private static final int ICON = 54;

	private final BoostInfoConfig config;
	private final PotionReminder reminder;
	private final ItemManager itemManager;

	@Inject
	ReminderOverlay(BoostInfoConfig config, PotionReminder reminder, ItemManager itemManager)
	{
		this.config = config;
		this.reminder = reminder;
		this.itemManager = itemManager;
		// A normal overlay: hold Alt and drag it anywhere. Starts at the top center of the screen.
		setPosition(OverlayPosition.TOP_CENTER);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setMovable(true);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		List<PotionReminder.Need> needs;
		if (config.preview())
		{
			// Preview: show a sample alert so you can drag it into place and size it
			if (!config.reminderAlert())
			{
				return null;
			}
			PotionReminder.Need sample = new PotionReminder.Need();
			sample.potionItemId = ItemID._4DOSERANGERSPOTION;
			sample.potionName = "Ranging potion";
			needs = Collections.singletonList(sample);
		}
		else
		{
			if (!config.reminderEnabled() || !config.reminderAlert())
			{
				return null;
			}
			needs = reminder.needs();
		}
		if (needs.isEmpty())
		{
			return null;
		}

		// Everything scales with the "Alert size" setting
		float scale = config.reminderScale() / 100f;
		int icon = Math.round(ICON * scale);
		int pad = Math.max(4, Math.round(12 * scale));
		int gap = Math.max(4, Math.round(12 * scale));

		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setFont(FontManager.getRunescapeBoldFont().deriveFont(22f * scale));
		FontMetrics fm = g.getFontMetrics();

		int rowHeight = icon + Math.round(8 * scale);
		int width = 0;
		for (PotionReminder.Need n : needs)
		{
			width = Math.max(width, icon + gap + fm.stringWidth("Use " + n.potionName));
		}
		width += pad * 2;
		int height = needs.size() * rowHeight + Math.round(12 * scale);
		int arc = Math.round(14 * scale);

		// Drawn from this overlay's own corner: hold Alt and drag it anywhere (RuneLite remembers the spot)
		Composite old = g.getComposite();
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.35f, Colors.pulse())));
		g.setColor(BG);
		g.fillRoundRect(0, 0, width, height, arc, arc);
		g.setColor(BORDER);
		g.setStroke(new BasicStroke(Math.max(1f, 2f * scale)));
		g.drawRoundRect(0, 0, width, height, arc, arc);

		int rowY = Math.round(6 * scale);
		int shadow = Math.max(1, Math.round(2 * scale));
		for (PotionReminder.Need n : needs)
		{
			BufferedImage img = itemManager.getImage(n.potionItemId);
			if (img != null)
			{
				g.drawImage(img, pad, rowY + (rowHeight - icon) / 2, icon * img.getWidth() / Math.max(1, img.getHeight()), icon, null);
			}
			String text = "Use " + n.potionName;
			int textX = pad + icon + gap;
			int textY = rowY + rowHeight / 2 + fm.getAscent() / 2 - Math.round(3 * scale);
			g.setColor(Color.BLACK);
			g.drawString(text, textX + shadow, textY + shadow);
			g.setColor(Color.WHITE);
			g.drawString(text, textX, textY);
			rowY += rowHeight;
		}
		g.setComposite(old);
		return new Dimension(width + 1, height + 1);
	}
}
