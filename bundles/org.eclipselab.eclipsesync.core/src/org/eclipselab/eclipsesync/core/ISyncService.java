/**
 * Copyright (c) 2011 Eclipselab Eclipse Sync and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html 
 */
package org.eclipselab.eclipsesync.core;

public interface ISyncService {

	/**
	 * Get the available storage services.
	 * @return the array of available storage services
	 */
	public ISyncStorage[] getStorages();

	/**
	 * Schedule the synchronizing tasks with given interval.
	 * @param interval the interval in milliseconds
	 */
	public void schedule(long interval);

	/**
	 * Stop the synchronize. 
	 */
	public void stop();

	/**
	 * Specify the given storage to store the synchronized data.
	 * @param storage the storage service is about to be used
	 */
	public void setStorage(ISyncStorage storage);
}
