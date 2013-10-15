package com.epam.telepresence.web;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum Command {
	EMPTY((byte) 0x0),
	FULL((byte) 0x1),
	READY((byte) 0x2),
	STOP((byte) 0x3),
	FORWARD((byte) 0x4),
	BACKWARD((byte) 0x5),
	LEFT((byte) 0x6),
	RIGHT((byte) 0x7);

	public static final Map<Byte, Command> BY_CODE = Collections.unmodifiableMap(
			new HashMap<Byte, Command>() {
				{
					for (Command command : Command.values()) {
						put(command.getBinaryCode(), command);
					}
				}
			}
	);

	private byte binaryCode;

	Command(byte binaryCode) {
		this.binaryCode = binaryCode;
	}

	public byte getBinaryCode() {
		return binaryCode;
	}
}
