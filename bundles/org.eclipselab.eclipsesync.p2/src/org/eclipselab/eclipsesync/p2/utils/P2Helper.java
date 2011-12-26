/**
 * Copyright (c) 2011 Eclipselab Eclipse Sync and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html 
 */
package org.eclipselab.eclipsesync.p2.utils;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.query.UserVisibleRootQuery;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

public final class P2Helper {

	public static IInstallableUnit[] getAllInstalledIUs(IProfile profile, IProgressMonitor monitor) {
		return profile.query(new UserVisibleRootQuery(), monitor).toArray(IInstallableUnit.class);
	}
}
