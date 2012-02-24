/**
 * Copyright (c) 2011, 2012 Eclipselab Eclipse Sync and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html 
 */
package org.eclipselab.eclipsesync.ui.preferences;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipselab.eclipsesync.core.ISyncService;
import org.eclipselab.eclipsesync.core.ISyncStorage;
import org.eclipselab.eclipsesync.core.internal.FileStorage;
import org.eclipselab.eclipsesync.ui.IPreferenceOptions;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class MainPreferencePage extends PreferencePage implements IWorkbenchPreferencePage{

	private static final String STATE = "state"; //$NON-NLS-1$
	public static final String FileStorageId = "filesystem"; //$NON-NLS-1$
	private static final String PREFOPTION_FILTER = "(id={0})"; //$NON-NLS-1$
	String storageValue;
	boolean syncOnOff;
	Button switchOnOff;
	Group group;
	private Button[] storageButtons;

	public MainPreferencePage() {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription(Messages.PreferencePage_Description);
	}

	public IPreferenceOptions getPreferenceOptions(String filter) {
		try {
			Collection<ServiceReference<IPreferenceOptions>> storages = Activator.getDefault().getBundle().getBundleContext().getServiceReferences(IPreferenceOptions.class, filter);
			if (storages.size() > 0) {
				ServiceReference<IPreferenceOptions> ref = storages.iterator().next();
				try {
					return Activator.getDefault().getBundle().getBundleContext().getService(ref);
				} finally {
					Activator.getDefault().getBundle().getBundleContext().ungetService(ref);	
				}
			}
			return null;
		} catch (InvalidSyntaxException e) {
			// won't happen
			return null;
		}
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.marginHeight = 20;
		gridLayout.verticalSpacing = 20;
		composite.setLayout(gridLayout);
		GridData griddata = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(griddata);
		// it's an option to turn on/off synchronization operation
		switchOnOff = new Button(composite, SWT.CHECK);
		switchOnOff.setText(Messages.PreferencePage_BooleanPref);

		// storage group
		group = new Group(composite, SWT.NONE);
		group.setFont(getFont());
		group.setText(Messages.MainPreferencePage_GroupStorage);
		griddata = new GridData(GridData.FILL, GridData.BEGINNING, true, false);
		group.setLayoutData(griddata);
		Composite radioBox = group;
		GridLayout layout = new GridLayout();
		layout.horizontalSpacing = 8;
		layout.numColumns = 1;
		radioBox.setLayout(layout);

		final ISyncService syncService = Activator.getDefault().getService(ISyncService.class);
		Map<String, ISyncStorage> storages = syncService.getStorages();

		int i = 0;
		storageButtons = new Button[storages.size()];
		for (String propertyId : storages.keySet()) {
			ISyncStorage storage = storages.get(propertyId);
			final Button radio = new Button(radioBox, SWT.RADIO | SWT.LEFT);
			storageButtons[i++] = radio;
			radio.setText(storage.getName());
			radio.setToolTipText(storage.getDescription());
			radio.setFont(getFont());
			radio.setData(propertyId);
			radio.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					storageValue = (String) event.widget.getData();
				}
			});
			if (i == 1) {
				radio.setSelection(true);
			}

			IPreferenceOptions prefOptions = getPreferenceOptions(NLS.bind(PREFOPTION_FILTER, propertyId));
			if (prefOptions != null) {
				final Composite optionComp = new Composite(radioBox, SWT.NONE);
				griddata = new GridData(GridData.FILL_HORIZONTAL);
				optionComp.setLayoutData(griddata);
				prefOptions.createOptions(optionComp, storages.get(propertyId), getPreferenceStore());
				radio.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						if (e.widget == radio) {
							recursiveSetEnabled(optionComp, radio.getSelection(), true, false);
						}
					}
				});
				recursiveSetEnabled(optionComp, radio.getSelection(), true, false);
			}
		}
		switchOnOff.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				syncOnOff = switchOnOff.getSelection();
				if (syncOnOff)
					recursiveSetEnabled(group, syncOnOff, false, true);
				else
					recursiveSetEnabled(group, syncOnOff, true, false);
			}
		});

		String defaultStorageId = getPreferenceStore().getString(PreferenceConstants.Storage);
		storageValue = defaultStorageId;
		updateStorageButtons();
		syncService.setStorage(storages.get(storageValue));

		syncOnOff = getPreferenceStore().getBoolean(PreferenceConstants.OnOff);
		if (syncOnOff) {
			switchOnOff.setSelection(true);
		} else
			recursiveSetEnabled(group, false, true, false);
		final Button syncNow = new Button(composite, SWT.PUSH);
		syncNow.setText(Messages.MainPreferencePage_ButtonSyncNow);
		syncNow.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				syncService.schedule(0);
			}
		});
		return composite;
	}

	private void updateStorageButtons() {
		for (Button storageButton : storageButtons) {
			if (storageValue.equals(storageButton.getData())) {
				storageButton.setSelection(true);
				break;
			}
		}
	}

	private void updateSyncService() {
		ISyncService syncService = Activator.getDefault().getService(ISyncService.class);
		if (syncOnOff) {
			ISyncStorage storage = syncService.getStorages().get(storageValue);
			if (storage instanceof FileStorage) {
				((FileStorage) storage).setStorageLocation(new File(getPreferenceStore().getString(PreferenceConstants.FileStoragePath)));
			}
			syncService.setStorage(storage);
		} else
			syncService.stop();
	}

	public void recursiveSetEnabled(Control control, boolean enabled, boolean remember, boolean restore) {
		if (control instanceof Composite) {
			Composite comp = (Composite) control;
			for (Control c : comp.getChildren())
				recursiveSetEnabled(c, enabled, remember, restore);
		} else {
			// ignore the widget always is disable
			if (control.getData("disable") != null) //$NON-NLS-1$
				return;
			Boolean previousValue = (Boolean) control.getData(STATE);
			if (remember) {
				control.setData(STATE, control.getEnabled());
			}
			if (restore && previousValue != null) {
				control.setEnabled(previousValue.booleanValue());
			} else
				control.setEnabled(enabled);
		}
	}

	public void init(IWorkbench workbench) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean performOk() {
		syncOnOff = switchOnOff.getSelection();
		getPreferenceStore().setValue(PreferenceConstants.OnOff, syncOnOff);
		getPreferenceStore().setValue(PreferenceConstants.Storage, storageValue);
		final ISyncService syncService = Activator.getDefault().getService(ISyncService.class);
		for (String propertyId : syncService.getStorages().keySet()) {
			IPreferenceOptions option = getPreferenceOptions(NLS.bind(PREFOPTION_FILTER, propertyId));
			if (option != null)
				option.performOk();
		}
		updateSyncService();
		return super.performOk();
	}

	@Override
	protected void performDefaults() {
		syncOnOff = true;
		switchOnOff.setSelection(syncOnOff);
		recursiveSetEnabled(group, true, false, true);
		final ISyncService syncService = Activator.getDefault().getService(ISyncService.class);
		for (String propertyId : syncService.getStorages().keySet()) {
			IPreferenceOptions option = getPreferenceOptions(NLS.bind(PREFOPTION_FILTER, propertyId));
			if (option != null)
				option.performDefaults();
		}
		storageValue = FileStorageId;
		updateStorageButtons();
		super.performDefaults();
	}
}