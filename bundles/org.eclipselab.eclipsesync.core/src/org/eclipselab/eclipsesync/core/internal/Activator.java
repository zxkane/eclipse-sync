/**
 * Copyright (c) 2011, 2012 Eclipselab Eclipse Sync and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html 
 */
package org.eclipselab.eclipsesync.core.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	public static final String BUNDLE_ID = "org.eclipselab.eclipsesync.core"; //$NON-NLS-1$
	private static BundleContext bundleContext = null;

	public void start(BundleContext context) throws Exception {
		Activator.bundleContext = context;
	}

	public void stop(BundleContext context) throws Exception {
		Activator.bundleContext = null;
	}

	public static BundleContext getContext() {
		return bundleContext;
	}
}
