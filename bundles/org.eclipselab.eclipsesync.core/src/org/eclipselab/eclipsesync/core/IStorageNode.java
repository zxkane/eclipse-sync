/**
 * Copyright (c) 2011 Eclipselab Eclipse Sync and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html 
 */
package org.eclipselab.eclipsesync.core;

import java.io.InputStream;
import java.io.OutputStream;

public interface IStorageNode {
	/**
	 * Load specified configuration file
	 * @param configName the given configuration name
	 * @return
	 * @throws StorageException the given configuration is not found
	 */
	public InputStream load(String configName) throws StorageException;
	/**
	 * Get the storage for given configuration
	 * @param configName
	 * @return
	 */
	public OutputStream getStore(String configName) throws StorageException;
	/**
	 * List the configurations
	 * @return	the name of existing configurations
	 */
	public String[] listConfigs();
}
