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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
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

	@Test public void syncInstalledFeaturesToStorage() throws BundleException, ProvisionException, OperationCanceledException, StorageException {
		File tempFolder = new File(getTempFolder(), "p2Sync"); //$NON-NLS-1$
		tempFolder.mkdirs();
		FileStorage storage = new FileStorage();
		storage.setStorageLocation(tempFolder);

		File testData = getTestData("0.1", "testData/p2");  //$NON-NLS-1$//$NON-NLS-2$
		ServiceRegistration reg = mockLocationService(testData);
		try {
			IProfileRegistry registry = (IProfileRegistry) getAgent().getService(IProfileRegistry.SERVICE_NAME);
			IProfile profile = registry.getProfile(IProfileRegistry.SELF);
			assertNotNull("0.2", profile); //$NON-NLS-1$
			assertEquals("0.3", "Test", profile.getProfileId()); //$NON-NLS-1$ //$NON-NLS-2$			
			IMetadataRepositoryManager metaManager = (IMetadataRepositoryManager) getAgent().getService(IMetadataRepositoryManager.SERVICE_NAME);
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
}
