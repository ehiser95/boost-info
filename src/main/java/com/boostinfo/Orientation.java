package com.boostinfo;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Orientation
{
	VERTICAL("Vertical"),
	HORIZONTAL("Horizontal");

	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
