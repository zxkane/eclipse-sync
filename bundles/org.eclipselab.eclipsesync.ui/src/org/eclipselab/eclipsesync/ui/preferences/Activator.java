/**
 * Copyright (c) 2011, 2012 Eclipselab Eclipse Sync and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html 
 */
package org.eclipselab.eclipsesync.ui.preferences;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class Activator extends AbstractUIPlugin {

	private static Activator instance = null;
	public static final String PLUGIN_ID = "org.eclipselab.eclipsesync.ui"; //$NON-NLS-1$

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		instance  = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		instance = null;
		super.stop(context);
	}

	public static Activator getDefault() {
		return instance;
	}

	/**
	 * Return a service from our context.
	 * 
	 * @param name name of the service
	 * @return the service
	 */
	@SuppressWarnings("unchecked")
	public <T> T getService(Class<T> clazz) {
		BundleContext context = getBundle().getBundleContext();
		ServiceReference ref = context.getServiceReference(clazz.getName());
		try{
			return (ref != null) ? (T)context.getService(ref) : null;
		} finally {
			if(ref != null)
				context.ungetService(ref);
		}
	}	
}
