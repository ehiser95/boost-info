package com.boostinfo;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum LayoutStyle
{
	CHIPS("Chips"),
	TILES("Tiles with a time bar"),
	LIST("Compact list"),
	BADGES("Compact: icon badges"),
	MINI_CHIPS("Compact: mini chips"),
	STRIP("Compact: strip"),
	;

	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
