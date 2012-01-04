/**
 * Copyright (c) 2011, 2012 Eclipselab Eclipse Sync and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html 
 */
package org.eclipselab.eclipsesync.tests.p2;

import java.io.File;
import java.util.Hashtable;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.internal.p2.core.AgentLocation;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipselab.eclipsesync.tests.AbstractTest;
import org.eclipselab.eclipsesync.tests.Activator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

public abstract class AbstractProvisioningTest extends AbstractTest{
	protected static IProvisioningAgent getAgent() {
		//get the global agent for the currently running system
		return (IProvisioningAgent) ServiceHelper.getService(Activator.getContext(), IProvisioningAgent.SERVICE_NAME);
	}

	protected ServiceRegistration mockLocationService(final File newInstallArea) throws BundleException {
		IAgentLocation location = new AgentLocation(newInstallArea.toURI());
		Hashtable<String, Object> props = new Hashtable<String, Object>();
		props.put(Constants.SERVICE_RANKING, new Integer(99));
		ServiceRegistration reg = Activator.getContext().registerService(IAgentLocation.class.getName(), location, props);
		restartAgent();
		return reg;
	}

	private void restartAgent() throws BundleException {
		Bundle bundle = Platform.getBundle("org.eclipse.equinox.p2.core"); //$NON-NLS-1$
		bundle.stop(Bundle.STOP_TRANSIENT);
		bundle.start(Bundle.START_TRANSIENT);
	}
}
