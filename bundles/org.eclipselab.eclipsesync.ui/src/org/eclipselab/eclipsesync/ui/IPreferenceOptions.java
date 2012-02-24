/**
 * Copyright (c) 2012 Eclipselab Eclipse Sync and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html 
 */
package org.eclipselab.eclipsesync.ui;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Composite;
import org.eclipselab.eclipsesync.core.ISyncStorage;

public interface IPreferenceOptions {
	public void createOptions(Composite control, ISyncStorage storage, IPreferenceStore prefStore);

	public boolean performOk();

	public boolean performDefaults();
}
