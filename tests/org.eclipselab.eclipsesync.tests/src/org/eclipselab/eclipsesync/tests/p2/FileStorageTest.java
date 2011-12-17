/**
 * Copyright (c) 2011 Eclipselab Eclipse Sync and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html 
 */
package org.eclipselab.eclipsesync.tests.p2;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipselab.eclipsesync.core.IStorageNode;
import org.eclipselab.eclipsesync.core.StorageException;
import org.eclipselab.eclipsesync.core.internal.FileStorage;
import org.eclipselab.eclipsesync.tests.AbstractTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FileStorageTest extends AbstractTest{

	private File storageLocation;

	@Before public void makeStorage() {
		storageLocation = getTestFolder("filestoragetest"); //$NON-NLS-1$
		assertNotNull("0.1", storageLocation); //$NON-NLS-1$		
	}

	@After public void cleanStorage() {
		assertTrue("1.0", delete(storageLocation)); //$NON-NLS-1$
	}

	@Test(expected=StorageException.class) public void getNodeWithoutProperLocation() throws StorageException {
		FileStorage storage = new FileStorage();
		storage.getNode("testNode", null); //$NON-NLS-1$
	}

	@Test public void getNonExistingNode() throws StorageException {
		FileStorage storage = new FileStorage();
		storage.setStorageLocation(storageLocation);
		final String nodeid = "testnode"; //$NON-NLS-1$
		assertNull("0.3", storage.getNode(nodeid, null)); //$NON-NLS-1$

		IStorageNode testnode = storage.createNode(nodeid, null);
		assertNotNull("0.5", testnode); //$NON-NLS-1$

		assertNull("0.8", storage.getNode(nodeid, testnode)); //$NON-NLS-1$
	}

	@Test public void createNode() throws StorageException {
		FileStorage storage = new FileStorage();
		storage.setStorageLocation(storageLocation);
		final String nodeid = "createtest"; //$NON-NLS-1$
		assertNotNull("0.5", storage.createNode(nodeid, null)); //$NON-NLS-1$
	}

	@Test(expected=StorageException.class) public void createExistingNode() throws StorageException {
		FileStorage storage = new FileStorage();
		storage.setStorageLocation(storageLocation);
		final String nodeid = "createtest"; //$NON-NLS-1$
		File testnode = new File(storageLocation, nodeid);
		assertTrue("0.3", testnode.mkdirs()); //$NON-NLS-1$
		assertNotNull("0.4", storage.getNode(nodeid, null)); //$NON-NLS-1$
		assertNotNull("0.5", storage.createNode(nodeid, null)); //$NON-NLS-1$
	}

	@Test(expected=StorageException.class) public void useIllegalNodeArgument() throws StorageException {
		FileStorage storage = new FileStorage();
		storage.setStorageLocation(storageLocation);
		final String nodeid = "createtest"; //$NON-NLS-1$
		storage.createNode(nodeid, new IStorageNode() {

			public InputStream load(String configName) throws StorageException {
				return null;
			}

			public OutputStream getStore(String configName) throws StorageException {
				return null;
			}
		});
	}
}
