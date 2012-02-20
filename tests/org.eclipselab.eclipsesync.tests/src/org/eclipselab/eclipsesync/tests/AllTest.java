/**
 * Copyright (c) 2011, 2012 Eclipselab Eclipse Sync and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html 
 */
package org.eclipselab.eclipsesync.tests;

import org.eclipselab.eclipsesync.tests.p2.P2HelperTests;
import org.eclipselab.eclipsesync.tests.p2.P2SyncTest;
import org.eclipselab.eclipsesync.tests.storage.DropboxStorageTest;
import org.eclipselab.eclipsesync.tests.storage.FileStorageTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({P2HelperTests.class,
	FileStorageTest.class,
	P2SyncTest.class,
	DropboxStorageTest.class})
public class AllTest {
	//
}
