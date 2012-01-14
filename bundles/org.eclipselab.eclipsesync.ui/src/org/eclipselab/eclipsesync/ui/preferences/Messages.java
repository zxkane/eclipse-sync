/**
 * Copyright (c) 2011 Eclipselab Eclipse Sync and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html 
 */
package org.eclipselab.eclipsesync.ui.preferences;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipselab.eclipsesync.ui.preferences.messages"; //$NON-NLS-1$
	public static String MainPreferencePage_ButtonSyncNow;
	public static String MainPreferencePage_GroupStorage;
	public static String PreferencePage_BooleanPref;
	public static String PreferencePage_TurnOn;
	public static String PreferencePage_TurnOnValue;
	public static String PreferencePage_TurnOff;
	public static String PreferencePage_TurnOffValue;
	public static String PreferencePage_Description;
	public static String PreferencePage_ChooseDirectory;
	public static String PreferencePage_SwitchGroup;
	public static String PreferencePage_TextPref;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
