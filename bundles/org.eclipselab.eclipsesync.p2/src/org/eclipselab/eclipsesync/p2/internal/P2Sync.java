/**
 * Copyright (c) 2011, 2012 Eclipselab Eclipse Sync and others.
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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.importexport.IUDetail;
import org.eclipse.equinox.internal.p2.importexport.P2ImportExport;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipselab.eclipsesync.core.IStorageNode;
import org.eclipselab.eclipsesync.core.ISyncStorage;
import org.eclipselab.eclipsesync.core.ISyncTask;
import org.eclipselab.eclipsesync.core.StorageException;
import org.eclipselab.eclipsesync.p2.utils.P2Helper;
import org.osgi.framework.BundleContext;

public class P2Sync implements ISyncTask {

	private static final String BUNDLE_ID = "org.eclipselab.eclipsesync.p2"; //$NON-NLS-1$
	public static final String STORAGE_NODE = "p2"; //$NON-NLS-1$
	private P2ImportExport importExportService;
	private IProvisioningAgent provisioningAgent;
	private BundleContext bundleContext;

	public void active(BundleContext context) {
		this.bundleContext = context;
	}

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
		IStatus status = new Status(IStatus.OK, BUNDLE_ID, Messages.P2Sync_AllInSync);
		try {
			SubMonitor loadingProgress = submonitor.newChild(200, SubMonitor.SUPPRESS_ALL_LABELS);
			IStorageNode p2Node = storage.getNode(STORAGE_NODE, null); 
			Set<IUDetail> theInstalledIUDetailsFromStorage = loadIUDetailFromStorage(p2Node, loadingProgress);

			SubMonitor computingProgress = submonitor.newChild(100, SubMonitor.SUPPRESS_ALL_LABELS);
			List<IUDetail> deltaToBeInstalledFeatures = new ArrayList<IUDetail>();
			List<IInstallableUnit> deltaToBeSyncFeatures = new ArrayList<IInstallableUnit>();
			computeSyncAndInstallItems(theInstalledIUDetailsFromStorage, deltaToBeInstalledFeatures, deltaToBeSyncFeatures, computingProgress);

			if (deltaToBeInstalledFeatures.size() == 0 && deltaToBeSyncFeatures.size() == 0) {
				submonitor.worked(700);
			} else {
				int installWight = 500;
				int syncWight = 200;			
				if (deltaToBeInstalledFeatures.size() == 0)
					syncWight += installWight;
				if (deltaToBeSyncFeatures.size() == 0)
					installWight += syncWight;

				if (deltaToBeSyncFeatures.size() > 0) {
					if (p2Node == null) {
						p2Node = storage.createNode(STORAGE_NODE, null);
					}
					status = exportDeltaToStorage(p2Node, submonitor.newChild(syncWight), deltaToBeSyncFeatures.toArray(new IInstallableUnit[deltaToBeSyncFeatures.size()]));
					if (status.matches(IStatus.INFO)) {
						logInfoStatus(status);
					}
				}

				if (deltaToBeInstalledFeatures.size() > 0) {
					if (status.isOK() || status.matches(IStatus.INFO)) {
						SubMonitor installDeltaProgress = submonitor.newChild(installWight);
						installDeltaProgress.setWorkRemaining(1000);
						List<IInstallableUnit> units = new ArrayList<IInstallableUnit>(deltaToBeInstalledFeatures.size());
						Set<URI> repos = new HashSet<URI>();
						SubMonitor queryToBeInstalled = installDeltaProgress.newChild(300);
						queryToBeInstalled.setWorkRemaining(deltaToBeInstalledFeatures.size() * 2);
						for (IUDetail detail : deltaToBeInstalledFeatures) {
							List<URI> uris = detail.getReferencedRepositories();
							ProvisioningContext tmpContext = new ProvisioningContext(provisioningAgent);
							if (uris.size() > 0)
								tmpContext.setMetadataRepositories(uris.toArray(new URI[uris.size()]));
							IQueryResult<IInstallableUnit> realIUs = tmpContext.getMetadata(queryToBeInstalled.newChild(1)).query(QueryUtil.createIUQuery(detail.getIU().getId(), detail.getIU().getVersion()), queryToBeInstalled.newChild(1));
							if (!realIUs.isEmpty()) {
								units.addAll(realIUs.toUnmodifiableSet());
								repos.addAll(detail.getReferencedRepositories());
							} // TODO if none is found
						}
						ProvisioningSession session = new ProvisioningSession(provisioningAgent);
						InstallOperation installOp = new InstallOperation(session, units);
						ProvisioningContext context = new ProvisioningContext(provisioningAgent);
						context.setArtifactRepositories(repos.toArray(new URI[repos.size()]));
						context.setMetadataRepositories(repos.toArray(new URI[repos.size()]));
						installOp.setProvisioningContext(context);
						status = installOp.resolveModal(installDeltaProgress.newChild(100, SubMonitor.SUPPRESS_ALL_LABELS));
						if (status.isOK()) {
							status = installOp.getProvisioningJob(null).run(installDeltaProgress.newChild(600, SubMonitor.SUPPRESS_ALL_LABELS));
						}
					}
				} else 
					submonitor.worked(installWight);
			}
		} catch (StorageException e) {
			status = new Status(IStatus.ERROR, BUNDLE_ID, 0, e.getMessage(), e);
		} catch (IOException e) {
			status = new Status(IStatus.ERROR, BUNDLE_ID, 0, Messages.P2Sync_FailLoadConfiguration, e);
		}

		return status;
	}

	private void logInfoStatus(IStatus status) {
		if (status.isMultiStatus()) {
			for (IStatus child : status.getChildren())
				logInfoStatus(child);
		} else if (status.matches(IStatus.INFO)) {
			Platform.getLog(bundleContext.getBundle()).log(status);
		}
	}

	public Set<IUDetail> loadIUDetailFromStorage(IStorageNode p2Node, IProgressMonitor monitor) throws StorageException, IOException {
		Set<IUDetail> theInstalledIUDetails = new HashSet<IUDetail>();
		SubMonitor loadingProgress = SubMonitor.convert(monitor, 100);
		if (p2Node != null) {
			String[] nodeNames = p2Node.listConfigs();
			if (nodeNames.length > 0) {
				loadingProgress.setWorkRemaining(nodeNames.length);
			} else
				loadingProgress.setWorkRemaining(1).worked(1);
			for (String nodeName : nodeNames) {
				InputStream input = null;
				try {
					input = p2Node.load(nodeName);
					theInstalledIUDetails.addAll(importExportService.importP2F(input));
					loadingProgress.worked(1);
				} finally {
					if (input != null) {
						try {
							input.close();
						} catch (IOException e) {
							// TODO
							e.printStackTrace();
						}
					}
				}
			}
		} else
			loadingProgress.setWorkRemaining(1).worked(1);
		return theInstalledIUDetails;
	}

	public void computeSyncAndInstallItems(Set<IUDetail> theInstalledIUDetailsFromStorage, List<IUDetail> deltaToBeInstalledFeatures, List<IInstallableUnit> deltaToBeSyncFeatures, IProgressMonitor monitor) {
		IProfileRegistry registry = (IProfileRegistry) provisioningAgent.getService(IProfileRegistry.SERVICE_NAME);
		IProfile currentProfile = registry.getProfile(IProfileRegistry.SELF);

		// compute delta features to be sync to storage or to be installed
		SubMonitor computingDeltaProgress = SubMonitor.convert(monitor, 1000);
		IInstallableUnit[] installedFeatures = P2Helper.getAllInstalledIUs(currentProfile, computingDeltaProgress.newChild(500, SubMonitor.SUPPRESS_ALL_LABELS));
		deltaToBeSyncFeatures.addAll(Arrays.asList(installedFeatures));
		syncloop : for (IUDetail hasSync : theInstalledIUDetailsFromStorage) {
			if (!isLatest(hasSync, theInstalledIUDetailsFromStorage))
				continue;
			for (Iterator<IInstallableUnit> iter = deltaToBeSyncFeatures.iterator(); iter.hasNext(); ) {
				IInstallableUnit installedIU = iter.next();
				if (hasSync.getIU().getId().equals(installedIU.getId())) {
					int compareValue = hasSync.getIU().compareTo(installedIU);
					if (compareValue == 0) {
						iter.remove();
					} else if (compareValue > 0) {
						iter.remove();
						deltaToBeInstalledFeatures.add(hasSync);						
					} 
					continue syncloop;
				}
			}
			deltaToBeInstalledFeatures.add(hasSync);
		}
		computingDeltaProgress.worked(500);
	}

	private boolean isLatest(IUDetail iuDetail, Set<IUDetail> details) {
		for (IUDetail toBeCompared : details) {
			if (toBeCompared.getIU().getId().equals(iuDetail.getIU().getId())) {
				if (iuDetail.getIU().getVersion().compareTo(toBeCompared.getIU().getVersion()) < 0)
					return false;
			}
		}
		return true;
	}

	private IStatus exportDeltaToStorage(IStorageNode p2Node,
			IProgressMonitor monitor, IInstallableUnit[] installedFeatures) {
		ByteArrayOutputStream memoryOut = new ByteArrayOutputStream();
		IStatus status = importExportService.exportP2F(memoryOut, installedFeatures, monitor);
		if (status.isOK() || status.matches(IStatus.INFO)) {
			try {
				// use time stamp as the name of node
				OutputStream stream = new BufferedOutputStream(p2Node.getStore(String.valueOf(new Date().getTime()))); 
				BufferedInputStream input = new BufferedInputStream(new ByteArrayInputStream(memoryOut.toByteArray()));
				FileUtils.copyStream(input, true, stream, true);
				memoryOut.close();
			} catch (StorageException e) {
				status = new Status(IStatus.ERROR, BUNDLE_ID, 0, e.getMessage(), e);
			} catch (IOException e) {
				status = new Status(IStatus.ERROR, BUNDLE_ID, 0, e.getMessage(), e);
			}
		}
		return status;
	}

}
