/**
 * Copyright (c) 2012 Eclipselab Eclipse Sync and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html 
 */
package org.eclipselab.eclipsesync.tests.p2;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.importexport.IUDetail;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipselab.eclipsesync.core.IStorageNode;
import org.eclipselab.eclipsesync.core.ISyncTask;
import org.eclipselab.eclipsesync.core.StorageException;
import org.eclipselab.eclipsesync.core.internal.FileStorage;
import org.eclipselab.eclipsesync.p2.internal.P2Sync;
import org.eclipselab.eclipsesync.tests.Activator;
import org.eclipselab.eclipsesync.tests.testserver.helper.AbstractTestServerClientCase;
import org.junit.Test;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;

public class P2SyncTest extends AbstractTestServerClientCase {

	@Test public void testComputing() throws BundleException, StorageException, IOException {
		File tempFolder = new File(getTempFolder(), "p2Sync"); //$NON-NLS-1$
		tempFolder.mkdirs();
		File testData = getTestData("0.1", "testData/p2/SDK371");  //$NON-NLS-1$//$NON-NLS-2$
		copy("0.15", testData, tempFolder); //$NON-NLS-1$
		File storageData = getTestData("0.2", "testData/storage/p2sync");  //$NON-NLS-1$//$NON-NLS-2$
		ServiceRegistration reg = mockLocationService(tempFolder);
		try {
			FileStorage storage = new FileStorage();
			storage.setStorageLocation(storageData);
			IStorageNode p2Node = storage.getNode(P2Sync.STORAGE_NODE, null);
			assertNotNull("0.3", p2Node); //$NON-NLS-1$
			ISyncTask p2Task = (ISyncTask) ServiceHelper.getService(Activator.getContext(), ISyncTask.class.getName(), "(type=p2)");  //$NON-NLS-1$
			assertNotNull("0.4", p2Task); //$NON-NLS-1$
			Set<IUDetail> theInstalledIUDetailsFromStorage = ((P2Sync)p2Task).loadIUDetailFromStorage(p2Node, new NullProgressMonitor());
			List<IUDetail> deltaToBeInstalledFeatures = new ArrayList<IUDetail>();
			List<IInstallableUnit> deltaToBeSyncFeatures = new ArrayList<IInstallableUnit>();
			((P2Sync)p2Task).computeSyncAndInstallItems(theInstalledIUDetailsFromStorage, deltaToBeInstalledFeatures, deltaToBeSyncFeatures, new NullProgressMonitor());
			assertEquals("0.5", 0, deltaToBeSyncFeatures.size()); //$NON-NLS-1$
			assertEquals("0.6", 2, deltaToBeInstalledFeatures.size()); //$NON-NLS-1$
		} finally {
			reg.unregister();
			delete(tempFolder);
		}
	}

	@Test public void syncInstalledFeaturesToStorage() throws BundleException, ProvisionException, OperationCanceledException, StorageException {
		File tempFolder = new File(getTempFolder(), "p2Sync"); //$NON-NLS-1$
		tempFolder.mkdirs();
		FileStorage storage = new FileStorage();
		storage.setStorageLocation(tempFolder);

		File testData = getTestData("0.1", "testData/p2/profile-feature1");  //$NON-NLS-1$//$NON-NLS-2$
		ServiceRegistration reg = mockLocationService(testData);
		try {
			IProfileRegistry registry = (IProfileRegistry) getAgent().getService(IProfileRegistry.SERVICE_NAME);
			IProfile profile = registry.getProfile(IProfileRegistry.SELF);
			assertNotNull("0.2", profile); //$NON-NLS-1$
			assertEquals("0.3", "Test", profile.getProfileId()); //$NON-NLS-1$ //$NON-NLS-2$			
			IMetadataRepositoryManager metaManager = (IMetadataRepositoryManager) getAgent().getService(IMetadataRepositoryManager.SERVICE_NAME);
			removeLegacyRepos(metaManager);
			IMetadataRepository metaTestRepo = metaManager.loadRepository(URI.create(getBaseURL() + "/repo1/"), null); //$NON-NLS-1$
			assertNotNull("0.4", metaTestRepo); //$NON-NLS-1$
			ISyncTask p2Task = (ISyncTask) ServiceHelper.getService(Activator.getContext(), ISyncTask.class.getName(), "(type=p2)");  //$NON-NLS-1$
			assertNotNull("0.5", p2Task); //$NON-NLS-1$
			assertTrue("0.6", p2Task.perform(storage, new NullProgressMonitor()).isOK()); //$NON-NLS-1$
			IStorageNode p2Node = storage.getNode(P2Sync.STORAGE_NODE, null);
			assertNotNull("0.7", p2Node); //$NON-NLS-1$
			assertEquals("0.8", 1, p2Node.listConfigs().length); //$NON-NLS-1$
		} finally {
			reg.unregister();
			delete(tempFolder);
		}
	}

	@Test public void syncFeaturesFromStorage() throws BundleException {
		File tempFolder = new File(getTempFolder(), "p2Sync"); //$NON-NLS-1$
		tempFolder.mkdirs();
		try {
			File tmpStorage = new File(tempFolder, "storage"); //$NON-NLS-1$
			tmpStorage.mkdirs();
			File storageData = getTestData("0.1", "testData/storage/feature1");  //$NON-NLS-1$//$NON-NLS-2$
			copy("0.2", storageData, tmpStorage); //$NON-NLS-1$
			File config = new File(tmpStorage, "p2/newconfig"); //$NON-NLS-1$
			updateConfig("0.3", config); //$NON-NLS-1$
			FileStorage storage = new FileStorage();
			storage.setStorageLocation(tmpStorage);
			File tmpProfile = new File(tempFolder, "profiles"); //$NON-NLS-1$
			File testData = getTestData("0.4", "testData/p2/profile-empty");  //$NON-NLS-1$//$NON-NLS-2$
			copy("0.45", testData, tmpProfile); //$NON-NLS-1$
			ServiceRegistration reg = mockLocationService(tmpProfile);
			try {
				ISyncTask p2Task = (ISyncTask) ServiceHelper.getService(Activator.getContext(), ISyncTask.class.getName(), "(type=p2)");  //$NON-NLS-1$
				assertNotNull("0.5", p2Task); //$NON-NLS-1$
				assertTrue("0.6", p2Task.perform(storage, new NullProgressMonitor()).isOK()); //$NON-NLS-1$
				IProfileRegistry registry = (IProfileRegistry) getAgent().getService(IProfileRegistry.SERVICE_NAME);
				IProfile profile = registry.getProfile(IProfileRegistry.SELF);
				assertNotNull("0.7", profile); //$NON-NLS-1$
				assertFalse("0.8", profile.available(QueryUtil.ALL_UNITS, null).isEmpty());	 //$NON-NLS-1$
			} finally {
				reg.unregister();
			}
		} finally {
			delete(tempFolder);			
		}
	}

	void removeLegacyRepos(IRepositoryManager manager) {
		URI[] uris = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		for (URI uri : uris) {
			manager.removeRepository(uri);
		}
	}

	private void updateConfig(String message, File config) {
		BufferedReader input = null;
		StringBuilder sb = new StringBuilder();
		final String hostURL = "http://localhost:(\\d){2,5}"; //$NON-NLS-1$
		final String matchAll = ".*"; //$NON-NLS-1$
		final Pattern pattern = Pattern.compile(matchAll + hostURL + matchAll);
		try {
			input = new BufferedReader(new FileReader(config));
			String line = null;
			while ((line = input.readLine()) != null) {
				if (pattern.matcher(line).matches()) {
					line = line.replaceAll(hostURL, getBaseURL());
				}
				sb.append(line).append("\n"); //$NON-NLS-1$
			}
		} catch (IOException e) {
			fail(message, e);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					System.err.println("Exception while trying to close input stream on: " + config.getAbsolutePath()); //$NON-NLS-1$
					e.printStackTrace();
				}
			}
		}
		OutputStream output = null;
		try {
			output = new BufferedOutputStream(new FileOutputStream(config));
			ByteArrayInputStream input1 = new ByteArrayInputStream(sb.toString().getBytes());
			byte[] buffer = new byte[8192];
			int bytesRead = 0;
			while ((bytesRead = input1.read(buffer)) != -1)
				output.write(buffer, 0, bytesRead);
			input1.close();
		} catch (IOException e) {
			fail(message, e);
		} finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					System.err.println("Exception while trying to close output stream on: " + config.getAbsolutePath()); //$NON-NLS-1$
					e.printStackTrace();
				}
			}
		}
	}
}
