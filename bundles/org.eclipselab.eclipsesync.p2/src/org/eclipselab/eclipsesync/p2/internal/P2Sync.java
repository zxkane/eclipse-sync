/**
 * Copyright (c) 2011 Eclipselab Eclipse Sync and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html 
 */
package org.eclipselab.eclipsesync.p2.internal;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.importexport.P2ImportExport;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipselab.eclipsesync.core.IStorageNode;
import org.eclipselab.eclipsesync.core.ISyncStorage;
import org.eclipselab.eclipsesync.core.ISyncTask;
import org.eclipselab.eclipsesync.core.StorageException;
import org.eclipselab.eclipsesync.p2.utils.P2Helper;

public class P2Sync implements ISyncTask {

	private static final String BUNDLE_ID = "org.eclipselab.eclipsesync.p2"; //$NON-NLS-1$
	public static final String STORAGE_NODE = "p2"; //$NON-NLS-1$
	private P2ImportExport importExportService;
	private IProvisioningAgent provisioningAgent;

	public void bindImportExportService(P2ImportExport service) {
		this.importExportService = service;
	}

	/**
	 * @param service  
	 */
	public void unbindImportExportService(P2ImportExport service) {
		this.importExportService = null;
	}

	public void bindAgent(IProvisioningAgent agent) {
		this.provisioningAgent = agent;
	}

	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	public IStatus perform(ISyncStorage storage, IProgressMonitor monitor) {
		if (storage == null)
			throw new IllegalArgumentException(Messages.P2Sync_StorageInvalid);
		SubMonitor submonitor = SubMonitor.convert(monitor, Messages.P2Sync_TaskName, 1000);
		IProfileRegistry registry = (IProfileRegistry) provisioningAgent.getService(IProfileRegistry.SERVICE_NAME);
		IProfile currentProfile = registry.getProfile(IProfileRegistry.SELF);
		IInstallableUnit[] installedFeatures = P2Helper.getAllInstalledIUs(currentProfile,submonitor.newChild(50, SubMonitor.SUPPRESS_ALL_LABELS));

		ByteArrayOutputStream memoryOut = new ByteArrayOutputStream();
		IStatus status = importExportService.exportP2F(memoryOut, installedFeatures, submonitor.newChild(500));
		if (status.isOK()) {
			try {
				IStorageNode p2Node = storage.getNode(STORAGE_NODE, null);
				if (p2Node == null) {
					p2Node = storage.createNode(STORAGE_NODE, null);
				}

				// TODO
				OutputStream stream = new BufferedOutputStream(p2Node.getStore("newconfig")); //$NON-NLS-1$
				BufferedInputStream input = new BufferedInputStream(new ByteArrayInputStream(memoryOut.toByteArray()));
				FileUtils.copyStream(input, true, stream, true);
				memoryOut.close();
			} catch (StorageException e) {
				status = new Status(IStatus.ERROR, BUNDLE_ID, 0, e.getMessage(), e);
			} catch (IOException e) {
				status = new Status(IStatus.ERROR, BUNDLE_ID, 0, e.getMessage(), e);
			}
		}

		submonitor.worked(450);
		return status;
	}

}
