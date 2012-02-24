/**
 * Copyright (c) 2012 Eclipselab Eclipse Sync and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html 
 */
package org.eclipselab.eclipsesync.ui.storage;

import java.io.File;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipselab.eclipsesync.core.ISyncStorage;
import org.eclipselab.eclipsesync.core.internal.FileStorage;
import org.eclipselab.eclipsesync.ui.IPreferenceOptions;
import org.eclipselab.eclipsesync.ui.preferences.Messages;
import org.eclipselab.eclipsesync.ui.preferences.PreferenceConstants;

public class FileStoragePrefOptions implements IPreferenceOptions {

	private static final String STATE = "state"; //$NON-NLS-1$
	Text directoryText;
	String fileStoragePath;
	private IPreferenceStore store = null;

	public void createOptions(final Composite control, ISyncStorage storage, IPreferenceStore prefStore) {
		store = prefStore;
		GridLayout layout = new GridLayout();
		layout.horizontalSpacing = 8;
		layout.numColumns = 3;
		control.setLayout(layout);
		Label directoryLabel = new Label(control, SWT.NONE);
		directoryLabel.setText(Messages.PreferencePage_ChooseDirectory);
		directoryLabel.setData(STATE, Boolean.TRUE);
		GridData griddata = new GridData(GridData.FILL_HORIZONTAL);
		directoryText = new Text(control, SWT.BORDER);
		directoryText.setLayoutData(griddata);
		directoryText.setData(STATE, Boolean.TRUE);
		fileStoragePath = prefStore.getString(PreferenceConstants.FileStoragePath);
		directoryText.setText(fileStoragePath);
		if (!fileStoragePath.equals("")) { //$NON-NLS-1$
			((FileStorage) storage).setStorageLocation(new File(fileStoragePath));
		}
		Button directoryChoose = new Button(control, SWT.PUSH);
		directoryChoose.setData(STATE, Boolean.TRUE);
		directoryChoose.setText(JFaceResources.getString("openBrowse")); //$NON-NLS-1$
		directoryChoose.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog directoryDialog = new DirectoryDialog(control.getShell());
				if (!fileStoragePath.equals("")) //$NON-NLS-1$
					directoryDialog.setFilterPath(fileStoragePath);
				String userChosen = directoryDialog.open();
				if (userChosen != null) {
					directoryText.setText(userChosen);
				}
			}
		});
	}

	public boolean performOk() {
		fileStoragePath = directoryText.getText().trim();
		store.setValue(PreferenceConstants.FileStoragePath, fileStoragePath);
		return true;
	}

	public boolean performDefaults() {
		fileStoragePath = ""; //$NON-NLS-1$
		directoryText.setText(fileStoragePath);
		return true;
	}

}
