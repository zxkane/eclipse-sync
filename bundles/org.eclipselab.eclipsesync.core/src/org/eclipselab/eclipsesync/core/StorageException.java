/**
 * Copyright (c) 2011 Eclipselab Eclipse Sync and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html 
 */
package org.eclipselab.eclipsesync.core;

public class StorageException extends Exception {

	public static final int ConfigNotFound = 0x01;
	public static final int StorageIllegalState = 0x02;
	public static final int StorageIllegalArgument = 0x04;
	public static final int StorageNodeExisting = 0x08;
	public static final int StorageIOException = 0x10;

	private static final long serialVersionUID = 4751716513296093654L;
	private int type;

	public StorageException(int type, String message, Throwable throwable) {
		super(message, throwable);
		this.type = type;
	}

	public int getExceptionType() {
		return type;
	}
}
