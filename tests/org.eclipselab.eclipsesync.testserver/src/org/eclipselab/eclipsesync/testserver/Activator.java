/**
 * Copyright (c) 2012 Eclipselab Eclipse Sync and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html 
 */
package org.eclipselab.eclipsesync.testserver;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator, ServiceTrackerCustomizer {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	private ServiceTracker httpTracker;
	private HttpService httpService;
	private static Activator instance;

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;
		httpTracker = new ServiceTracker(context, HttpService.class.getName(), this);
		httpTracker.open();
		instance = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		httpTracker.close();
		Activator.context = null;
	}

	public static Activator getInstance() {
		return instance;
	}

	public Object addingService(ServiceReference reference) {
		httpService = (HttpService) context.getService(reference);
		try {
			httpService.registerResources("/repo1", "/webfiles/testRepo", null);  //$NON-NLS-1$//$NON-NLS-2$
		} catch (NamespaceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return httpService;
	}

	public void modifiedService(ServiceReference reference, Object service) {
		// TODO Auto-generated method stub

	}

	public void removedService(ServiceReference reference, Object service) {
		httpService = (HttpService) service;
		httpService.unregister("/repo1"); //$NON-NLS-1$
	}

}
