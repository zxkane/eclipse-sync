/**
 * Copyright (c) 2012 Eclipselab Eclipse Sync and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html 
 */
package org.eclipselab.eclipsesync.storage.dropbox;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipselab.eclipsesync.storage.dropbox.messages"; //$NON-NLS-1$
	public static String DropboxPreference_ConfirmDialogMessage;
	public static String DropboxPreference_ConfirmDialogTitle;
	public static String DropboxPreference_LinkText;
	public static String DropboxStorage_AppKey;
	public static String DropboxStorage_AppSecret;
	public static String DropboxStorage_AuthFailed;
	public static String DropboxStorage_ConfigNotExist;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
