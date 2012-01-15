package org.eclipselab.eclipsesync.ui.preferences;

import java.io.File;
import java.util.Map;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipselab.eclipsesync.core.ISyncService;
import org.eclipselab.eclipsesync.core.ISyncStorage;
import org.eclipselab.eclipsesync.core.internal.FileStorage;

public class MainPreferencePage extends PreferencePage implements IWorkbenchPreferencePage{

	public static final String FileStorageId = "filesystem"; //$NON-NLS-1$
	String storageValue;
	String fileStoragePath;
	boolean syncOnOff;
	Button switchOnOff;
	Group group;
	Text directoryText;
	private Button[] storageButtons;

	public MainPreferencePage() {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription(Messages.PreferencePage_Description);
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

			if (FileStorageId.equals(propertyId)) { 
				final Composite direcotryComp = new Composite(radioBox, SWT.NONE);
				griddata = new GridData(GridData.FILL_HORIZONTAL);
				direcotryComp.setLayoutData(griddata);
				layout = new GridLayout();
				layout.horizontalSpacing = 8;
				layout.numColumns = 3;
				direcotryComp.setLayout(layout);
				Label directoryLabel = new Label(direcotryComp, SWT.NONE);
				directoryLabel.setText(Messages.PreferencePage_ChooseDirectory);
				griddata = new GridData(GridData.FILL_HORIZONTAL);
				directoryText = new Text(direcotryComp, SWT.BORDER);
				directoryText.setLayoutData(griddata);
				fileStoragePath = getPreferenceStore().getString(PreferenceConstants.FileStoragePath);
				directoryText.setText(fileStoragePath);
				if (!fileStoragePath.equals("")) { //$NON-NLS-1$
					((FileStorage) storage).setStorageLocation(new File(fileStoragePath));
				}
				Button directoryChoose = new Button(direcotryComp, SWT.PUSH);
				directoryChoose.setText(JFaceResources.getString("openBrowse")); //$NON-NLS-1$
				directoryChoose.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						DirectoryDialog directoryDialog = new DirectoryDialog(getShell());
						if (!fileStoragePath.equals("")) //$NON-NLS-1$
							directoryDialog.setFilterPath(fileStoragePath);
						String userChosen = directoryDialog.open();
						if (userChosen != null) {
							directoryText.setText(userChosen);
						}
					}
				});
			}
		}
		switchOnOff.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				syncOnOff = switchOnOff.getSelection();
				recursiveSetEnabled(group, syncOnOff);
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
			recursiveSetEnabled(group, false);
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

	public void recursiveSetEnabled(Control control, boolean enabled) {
		if (control instanceof Composite) {
			Composite comp = (Composite) control;
			for (Control c : comp.getChildren())
				recursiveSetEnabled(c, enabled);
		} else {
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
		fileStoragePath = directoryText.getText().trim();
		getPreferenceStore().setValue(PreferenceConstants.FileStoragePath, fileStoragePath);
		updateSyncService();
		return super.performOk();
	}

	@Override
	protected void performDefaults() {
		syncOnOff = true;
		switchOnOff.setSelection(syncOnOff);
		recursiveSetEnabled(group, true);
		fileStoragePath = ""; //$NON-NLS-1$
		directoryText.setText(fileStoragePath);
		storageValue = FileStorageId;
		updateStorageButtons();
		super.performDefaults();
	}
}