package com.boostinfo;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

final class Icons
{
	private Icons()
	{
	}

	/**
	 * Draw an icon scaled to fit a size x size square, keeping its aspect ratio.
	 */
	static void draw(Graphics2D g, BufferedImage icon, int x, int y, int size)
	{
		double scale = Math.min((double) size / icon.getWidth(), (double) size / icon.getHeight());
		int w = (int) Math.round(icon.getWidth() * scale);
		int h = (int) Math.round(icon.getHeight() * scale);
		g.drawImage(icon, x + (size - w) / 2, y + (size - h) / 2, w, h, null);
	}
}
