/**
 * Copyright (c) 2011, 2012 Eclipselab Eclipse Sync and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html 
 */
package org.eclipselab.eclipsesync.core.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipselab.eclipsesync.core.internal.messages"; //$NON-NLS-1$
	public static String FileStorage_FailCreateNode;
	public static String FileStorage_IllegalNode;
	public static String FileStorage_MissRootFolder;
	public static String FileStorage_NodeAlreadyExist;
	public static String FileStorage_StorageDescription;
	public static String FileStorage_StorageName;
	public static String FileStorageNode_IllegalLocationArugment;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
