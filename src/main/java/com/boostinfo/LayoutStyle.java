package com.boostinfo;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum LayoutStyle
{
	CHIPS("Chips"),
	TILES("Tiles with a time bar"),
	LIST("Compact list"),
	;

	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
