package com.boostinfo;

import lombok.RequiredArgsConstructor;

/**
 * Orientation for one section's list: the same as the top Orientation setting, or its own.
 */
@RequiredArgsConstructor
public enum SectionOrientation
{
	SAME("Same as Orientation", null),
	VERTICAL("Vertical", Orientation.VERTICAL),
	HORIZONTAL("Horizontal", Orientation.HORIZONTAL),
	;

	private final String name;
	private final Orientation orientation;

	Orientation resolve(Orientation main)
	{
		return orientation == null ? main : orientation;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
