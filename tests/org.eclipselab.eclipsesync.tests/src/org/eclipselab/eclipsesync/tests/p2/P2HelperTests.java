/**
 * Copyright (c) 2011, 2012 Eclipselab Eclipse Sync and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html 
 */
package org.eclipselab.eclipsesync.tests.p2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipselab.eclipsesync.p2.utils.P2Helper;
import org.junit.Test;

public class P2HelperTests extends AbstractProvisioningTest{

	@Test public void testQueryInstalledFeatures() {
		File testData = getTestData("Profile data", "testData/p2");  //$NON-NLS-1$//$NON-NLS-2$
		//assert that test data is intact (see bug 285158)
		File profileFile = new File(new File(testData, "SDKProfile.profile"), "1299295855883.profile.gz"); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Test profile is not found.", profileFile.exists()); //$NON-NLS-1$
		File tempFolder = getTempFolder();
		copy("Copy test data to temporary folder.", testData, tempFolder); //$NON-NLS-1$

		SimpleProfileRegistry registry = new SimpleProfileRegistry(getAgent(), tempFolder, null, false);
		IProfile profile = registry.getProfile("SDKProfile"); //$NON-NLS-1$
		assertNotNull("fail to load profile.", profile); //$NON-NLS-1$

		IInstallableUnit[] ius = P2Helper.getAllInstalledIUs(profile, null);
		assertEquals("Not find expected ius.", 8, ius.length); //$NON-NLS-1$
	}
}
