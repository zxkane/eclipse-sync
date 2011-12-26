/**
 * Copyright (c) 2011 Eclipselab Eclipse Sync and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html 
 */
package org.eclipselab.eclipsesync.tests.p2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.core.runtime.Platform;
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

			public String[] listConfigs() {
				return null;
			}
		});
	}

	@Test public void getStore() throws StorageException, IOException {
		FileStorage storage = new FileStorage();
		storage.setStorageLocation(storageLocation);
		final String nodeid = "nodeid"; //$NON-NLS-1$
		IStorageNode node = storage.createNode(nodeid, null);
		assertNotNull("0.5", node); //$NON-NLS-1$
		final String configName = "configid"; //$NON-NLS-1$
		OutputStream output = node.getStore(configName); 
		assertNotNull("0.6", output); //$NON-NLS-1$
		final String content = "test"; //$NON-NLS-1$
		output.write(new String(content).getBytes()); 
		output.close();
		File outputFile = new File(new File(storageLocation, nodeid), configName);
		assertTrue("0.8", outputFile.exists()); //$NON-NLS-1$
		assertEquals("0.9", 4, outputFile.length()); //$NON-NLS-1$
	}

	@Test(expected=StorageException.class) public void getStoreWithIllegalCharaters() throws IOException, StorageException {
		FileStorage storage = new FileStorage();
		storage.setStorageLocation(storageLocation);
		final String nodeid = "nodeid"; //$NON-NLS-1$
		IStorageNode node = null;
		try {
			node = storage.createNode(nodeid, null);
		} catch (StorageException e) {
			fail("0.4", e); //$NON-NLS-1$
		}
		assertNotNull("0.5", node); //$NON-NLS-1$
		if (node != null) {
			final String configName = "configid"; //$NON-NLS-1$
			String invalidChars;
			if (Platform.OS_WIN32.equals(Platform.getOS())) {
				invalidChars = "\\/:*?\"<>|"; //$NON-NLS-1$
			} else if (Platform.OS_MACOSX.equals(Platform.getOS())) {
				invalidChars = "/:"; //$NON-NLS-1$
			} else { // assume Unix/Linux
				invalidChars = "/"; //$NON-NLS-1$
			}
			OutputStream output = node.getStore(configName + invalidChars + configName); 
			output.close();
		}
	}	

	@Test public void testNodeList() throws IOException, StorageException {		
		FileStorage storage = new FileStorage();
		storage.setStorageLocation(storageLocation);
		final String nodeid = "nodeid"; //$NON-NLS-1$
		File nodeFile = new File(storageLocation, nodeid);
		assertTrue("0.2", nodeFile.mkdirs()); //$NON-NLS-1$
		final String configName = "configid"; //$NON-NLS-1$
		File configStoreFile = new File(nodeFile, configName);
		assertTrue("0.3", configStoreFile.createNewFile()); //$NON-NLS-1$
		IStorageNode node = storage.getNode(nodeid, null);
		assertNotNull("0.5", node); //$NON-NLS-1$
		String[] configs = node.listConfigs();
		assertEquals("0.7", 1, configs.length); //$NON-NLS-1$
		assertEquals("0.9", configName, configs[0]); //$NON-NLS-1$
	}

	@Test(expected=StorageException.class) public void loadConfig() throws IOException, StorageException {
		FileStorage storage = new FileStorage();
		storage.setStorageLocation(storageLocation);
		final String nodeid = "nodeid"; //$NON-NLS-1$
		File nodeFile = new File(storageLocation, nodeid);
		assertTrue("0.2", nodeFile.mkdirs()); //$NON-NLS-1$
		final String configName = "configid"; //$NON-NLS-1$
		File configStoreFile = new File(nodeFile, configName);
		assertTrue("0.3", configStoreFile.createNewFile()); //$NON-NLS-1$
		IStorageNode node = null;
		try {
			node = storage.getNode(nodeid, null);
			assertNotNull("0.5", node); //$NON-NLS-1$
		} catch (StorageException e) {
			fail("0.4", e); //$NON-NLS-1$
		}
		if (node != null) {
			try {
				InputStream input = node.load(configName);
				input.close();
			} catch (StorageException e) {
				assertNotNull("0.6", node); //$NON-NLS-1$
			}
			InputStream input = node.load("noexisting"); //$NON-NLS-1$
			input.close();
		}
	}
}
