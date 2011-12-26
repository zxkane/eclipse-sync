/**
 * Copyright (c) 2011 Eclipselab Eclipse Sync and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html 
 */
package org.eclipselab.eclipsesync.core.internal;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
		InputStream input = null;
		try {
			input = new FileInputStream(new File(nodeLocation, configName));
		} catch (FileNotFoundException e) {
			throw new StorageException(StorageException.ConfigNotFound, e.getLocalizedMessage(), e);
		}
		return input;
	}

	public OutputStream getStore(String configName) throws StorageException {
		OutputStream output = null;
		try {
			final File configFile = new File(nodeLocation, configName);
			if (!configFile.exists())
				configFile.createNewFile();
			output = new FileOutputStream(configFile);
		} catch (FileNotFoundException e) {
			// won't happen
		} catch (IOException e) {
			throw new StorageException(StorageException.StorageIOException, e.getLocalizedMessage(), e);
		}
		return output;
	}

	public String[] listConfigs() {
		File[] files = nodeLocation.listFiles(new FileFilter() {

			public boolean accept(File pathname) {
				if (pathname.isFile())
					return true;
				return false;
			}
		});
		String[] names = new String[files.length];
		for (int i = 0; i < names.length; i++) {
			names[i] = files[i].getName();
		}
		return names;
	}

}
