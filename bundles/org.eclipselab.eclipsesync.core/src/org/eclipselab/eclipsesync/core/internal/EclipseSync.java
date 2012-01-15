/**
 * Copyright (c) 2012 Eclipselab Eclipse Sync and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html 
 */
package org.eclipselab.eclipsesync.core.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipselab.eclipsesync.core.ISyncService;
import org.eclipselab.eclipsesync.core.ISyncStorage;
import org.eclipselab.eclipsesync.core.ISyncTask;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class EclipseSync implements ISyncService {

	private class EclipseSyncJob extends Job {

		public EclipseSyncJob(String name) {
			super(name);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<ISyncTask> tasks = getTasks();
			if (tasks.size() == 0) 
				return new Status(IStatus.OK, Activator.BUNDLE_ID, Messages.EclipseSync_NoTask);
			SubMonitor sub = SubMonitor.convert(monitor, 1000 * tasks.size());
			MultiStatus rt = new MultiStatus(Activator.BUNDLE_ID, 0,  Messages.EclipseSync_SyncReport, null);
			for (ISyncTask task : tasks) {
				if (sub.isCanceled())
					return new Status(IStatus.CANCEL, Activator.BUNDLE_ID, Messages.EclipseSync_CancelByUser);
				rt.add(task.perform(currentStorage, sub.newChild(1000)));
			}
			return rt;
		}

		@Override
		public boolean belongsTo(Object family) {
			if ("eclipsesync".equals(family)) { //$NON-NLS-1$
				return true;
			}
			return super.belongsTo(family);
		}

		private List<ISyncTask> getTasks() {
			try {
				Collection<ServiceReference<ISyncTask>> taskRefs = Activator.getContext().getServiceReferences(ISyncTask.class, null);
				List<ISyncTask> tasks = new ArrayList<ISyncTask>(taskRefs.size());
				for (ServiceReference<ISyncTask> ref : taskRefs) {
					tasks.add(Activator.getContext().getService(ref));
					Activator.getContext().ungetService(ref);
				}
				return tasks;
			} catch (InvalidSyntaxException e) {
				// won't happen
				return Collections.EMPTY_LIST;
			}
		}

	}

	ISyncStorage currentStorage = null;

	public Map<String, ISyncStorage> getStorages() {
		try {
			Collection<ServiceReference<ISyncStorage>> storages = Activator.getContext().getServiceReferences(ISyncStorage.class, null);
			Map<String, ISyncStorage> storageImpls = new HashMap<String, ISyncStorage>(storages.size());
			for (ServiceReference<ISyncStorage> storageRef : storages) {
				String storageId = (String) storageRef.getProperty("id"); //$NON-NLS-1$
				if (storageId == null)
					storageId = generateId(storageRef);
				storageImpls.put(storageId, Activator.getContext().getService(storageRef));
				Activator.getContext().ungetService(storageRef);
			}
			return Collections.unmodifiableMap(storageImpls);
		} catch (InvalidSyntaxException e) {
			// won't happen
			return Collections.EMPTY_MAP;
		}
	}

	private String generateId(ServiceReference<ISyncStorage> storageRef) {
		try {
			return storageRef.getBundle() + Activator.getContext().getService(storageRef).getName();
		} finally {
			Activator.getContext().ungetService(storageRef);
		}
	}

	public void schedule(long interval) {
		EclipseSyncJob job = new EclipseSyncJob(Messages.EclipseSync_JobName);
		job.setUser(true);
		job.schedule();
	}

	public void stop() {
		// TODO Auto-generated method stub

	}

	public void setStorage(ISyncStorage storage) {
		currentStorage = storage;
	}

}
