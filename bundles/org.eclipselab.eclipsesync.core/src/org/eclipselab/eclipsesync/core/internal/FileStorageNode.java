/**
 * Copyright (c) 2011 Eclipselab Eclipse Sync and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html 
 */
package org.eclipselab.eclipsesync.core.internal;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipselab.eclipsesync.core.IStorageNode;
import org.eclipselab.eclipsesync.core.StorageException;

public class FileStorageNode implements IStorageNode {

	File nodeLocation;

	public FileStorageNode(File location) throws StorageException {
		if (!(location.exists() && location.isDirectory()))
			throw new StorageException(StorageException.StorageIllegalArgument, Messages.FileStorageNode_IllegalLocationArugment, null);
		this.nodeLocation = location;
	}

	public InputStream load(String configName) throws StorageException {
		// TODO Auto-generated method stub
		return null;
	}

	public OutputStream getStore(String configName) throws StorageException {
		// TODO Auto-generated method stub
		return null;
	}

}
