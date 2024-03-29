/**
 * Copyright (c) 2011 Eclipselab Eclipse Sync and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html 
 */
package org.eclipselab.eclipsesync.tests;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	private static BundleContext bundleContext = null;

	public static BundleContext getContext() {
		return bundleContext;
	}

	public void start(BundleContext context) throws Exception {
		bundleContext = context;
	}

	public void stop(BundleContext context) throws Exception {
		bundleContext = null;
	}

}
