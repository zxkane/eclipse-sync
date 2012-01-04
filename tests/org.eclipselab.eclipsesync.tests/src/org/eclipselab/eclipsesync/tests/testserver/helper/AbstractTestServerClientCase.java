/*******************************************************************************
 *  Copyright (c) 2009, 2010 Cloudsmith and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      Cloudsmith - initial API and implementation
 *******************************************************************************/
package org.eclipselab.eclipsesync.tests.testserver.helper;

import org.eclipselab.eclipsesync.tests.p2.AbstractProvisioningTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

public class AbstractTestServerClientCase extends AbstractProvisioningTest {

	/**
	 * Returns a URL string part consisting of http://localhost:<port>
	 * @return String with first part of URL
	 */
	protected String getBaseURL() {
		return "http://localhost:" + System.getProperty(TestServerController.PROP_TESTSERVER_PORT, "8080"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@BeforeClass public static void oneTimeSetUp() throws Exception {
		TestServerController.checkSetUp();
	}

	@AfterClass public static void oneTimeTearDown() throws Exception {
		TestServerController.checkTearDown();
	}

	@After
	public void tearDown() throws Exception {
		// if a test is run out or order - this must be done
		TestServerController.checkTearDown();
	}

	@Before
	public void setUp() throws Exception {
		// if a test is run out or order - this must be done
		TestServerController.checkSetUp();
	}
}
