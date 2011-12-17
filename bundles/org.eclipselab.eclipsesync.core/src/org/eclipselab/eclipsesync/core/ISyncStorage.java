/**
 * Copyright (c) 2011 Eclipselab Eclipse Sync and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html 
 */
package org.eclipselab.eclipsesync.core;

public interface ISyncStorage {
	/**
	 * Get the name of storage service.
	 * @return the name of storage service.
	 */
	public String getName();

	/**
	 * Get the description of storage service.
	 * @return the description of storage service.
	 */
	public String getDescription();

	/**
	 * Get the <code>IStorageNode</code> instance for given id and parent node
	 * @param nodeId the specified node id
	 * @param parent the parent node, or <code>null</code> for default parent
	 * @return the specified node or <code>null</code> if the given node doesn't exist
	 */
	public IStorageNode getNode(String nodeId, IStorageNode parent) throws StorageException;

	public IStorageNode createNode(String nodeId, IStorageNode parent) throws StorageException;
}
