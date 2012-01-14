/**
 * Copyright (c) 2011, 2012 Eclipselab Eclipse Sync and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html 
 */
package org.eclipselab.eclipsesync.core.internal;

import java.io.File;

import org.eclipse.osgi.util.NLS;
import org.eclipselab.eclipsesync.core.IStorageNode;
import org.eclipselab.eclipsesync.core.ISyncStorage;
import org.eclipselab.eclipsesync.core.StorageException;

public class FileStorage implements ISyncStorage {

	private File location;

	public String getName() {
		return Messages.FileStorage_StorageName;
	}

	public String getDescription() {
		return Messages.FileStorage_StorageDescription;
	}

	public IStorageNode getNode(String nodeId, IStorageNode parent)
			throws StorageException {
		if (location == null)
			throw new StorageException(StorageException.StorageIllegalState, Messages.FileStorage_MissRootFolder, null);
		File node = null;
		if (parent == null) {
			node = new File(location, nodeId);
		} else {
			if (parent instanceof FileStorageNode) {
				node = new File(((FileStorageNode) parent).nodeLocation, nodeId);
			} else
				throw new StorageException(StorageException.StorageIllegalArgument, Messages.FileStorage_IllegalNode, null);
		}
		if (node.exists() && node.isDirectory())
			return new FileStorageNode(node);
		return null;
	}

	public IStorageNode createNode(String nodeId, IStorageNode parent)
			throws StorageException {
		if (location == null)
			throw new StorageException(StorageException.StorageIllegalState, Messages.FileStorage_MissRootFolder, null);
		File newNode = null;
		if (parent == null) {
			newNode = new File(location, nodeId);
		} else {
			if (parent instanceof FileStorageNode) {
				newNode = new File(((FileStorageNode) parent).nodeLocation, nodeId);
			} else
				throw new StorageException(StorageException.StorageIllegalArgument, Messages.FileStorage_IllegalNode, null);
		}
		if (newNode.exists())
			throw new StorageException(StorageException.StorageNodeExisting, NLS.bind(Messages.FileStorage_NodeAlreadyExist, nodeId), null);
		if (newNode.mkdirs())
			return new FileStorageNode(newNode);
		throw new StorageException(StorageException.StorageIOException, Messages.FileStorage_FailCreateNode, null);
	}

	public void setStorageLocation(File location) {
		this.location = location;
	}
}
