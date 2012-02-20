/**
 * Copyright (c) 2012 Eclipselab Eclipse Sync and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html 
 */
package org.eclipselab.eclipsesync.storage.dropbox;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipselab.eclipsesync.core.IStorageNode;
import org.eclipselab.eclipsesync.core.ISyncStorage;
import org.eclipselab.eclipsesync.core.StorageException;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.DropboxFileInfo;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxPartialFileException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.RequestTokenPair;
import com.dropbox.client2.session.Session.AccessType;
import com.dropbox.client2.session.WebAuthSession;

public class DropboxStorage implements ISyncStorage {

	public static final String SEPARATOR = "/"; //$NON-NLS-1$

	public class DropboxNode implements IStorageNode {

		private Entry entry;

		public DropboxNode(Entry dirEntry) {
			if (dirEntry == null)
				throw new IllegalArgumentException("Entry is null."); //$NON-NLS-1$
			this.entry = dirEntry;
		}

		public InputStream load(String configName) throws StorageException {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			final String path = SEPARATOR + entry.path + SEPARATOR + configName;
			try {
				DropboxFileInfo fileInfo = dropboxApi.getFile(path, null, outputStream, null);
				if (fileInfo.getMetadata().isDeleted)
					throw new StorageException(StorageException.ConfigNotFound, "Config doesn't exist.", null); //$NON-NLS-1$
				return new ByteArrayInputStream(outputStream.toByteArray());
			} catch (DropboxUnlinkedException e) {
				throw new StorageException(StorageException.AuthenticationFailure, e.getMessage(), e);
			} catch (DropboxServerException e) {
				if (e.error == DropboxServerException._404_NOT_FOUND)
					throw new StorageException(StorageException.ConfigNotFound, e.getMessage(), e);
				throw new StorageException(StorageException.UnkonwnException, e.getMessage(), e);
			} catch (DropboxIOException e) {
				throw new StorageException(StorageException.StorageIOException, e.getMessage(), e);
			}  catch (DropboxPartialFileException e) {
				throw new StorageException(StorageException.StorageIOException, e.getMessage(), e);
			} catch (DropboxException e) {
				throw new StorageException(StorageException.UnkonwnException, e.getMessage(), e);
			}
		}

		public OutputStream getStore(final String configName) throws StorageException {
			final String path = SEPARATOR + entry.path + SEPARATOR + configName;
			String parentRev = null;
			try {
				Entry existingEntry = dropboxApi.metadata(path, 1, null, false, null);
				parentRev = existingEntry.rev;
			} catch (DropboxUnlinkedException e) {
				throw new StorageException(StorageException.AuthenticationFailure, e.getMessage(), e);
			} catch (DropboxException e) {
				// do nothing
			}
			final String rev = parentRev;
			OutputStream out = new ByteArrayOutputStream() {
				@Override
				public void close() throws IOException {
					try {
						dropboxApi.putFile(path, new ByteArrayInputStream(toByteArray()), size(), rev, null);
					} catch (DropboxException e) {
						throw new IOException(e.getMessage(), e);
					}
				}
			};
			return out;
		}

		public String[] listConfigs() throws StorageException {
			try {
				Entry dirEntry = dropboxApi.metadata(SEPARATOR + entry.path, 0, null, true, null);
				if (dirEntry.isDeleted)
					throw new StorageException(StorageException.NodeNotExist, "Node doesn't exist any more.", null); //$NON-NLS-1$
				List<Entry> fileEntries = dirEntry.contents;
				List<String> configs = new ArrayList<String>(fileEntries.size());
				for (Entry fileEntry : fileEntries) {
					if (!fileEntry.isDir && !fileEntry.isDeleted)
						configs.add(fileEntry.fileName());
				}
				return configs.toArray(new String[configs.size()]);
			} catch (DropboxUnlinkedException e) {
				throw new StorageException(StorageException.AuthenticationFailure, e.getMessage(), e);
			} catch (DropboxIOException e) {
				throw new StorageException(StorageException.StorageIOException, e.getMessage(), e);
			} catch (DropboxException e) {
				throw new StorageException(StorageException.UnkonwnException, e.getMessage(), e);
			}
		}

		public Entry getEntry() {
			return this.entry;
		}

	}

	final static private String APP_KEY = Messages.DropboxStorage_AppKey;
	final static private String APP_SECRET = Messages.DropboxStorage_AppSecret;
	final static private AccessType ACCESS_TYPE = AccessType.APP_FOLDER;
	private final static String TOKEN = "token"; //$NON-NLS-1$
	private final static String SECRET = "secret"; //$NON-NLS-1$
	private String token = null;
	private String secret = null;

	// In the class declaration section:
	DropboxAPI<WebAuthSession> dropboxApi;

	public DropboxStorage() {
		ISecurePreferences node = getPreferenceNode();
		try {
			token = node.get(TOKEN, null);
			secret = node.get(SECRET, null);
		} catch (org.eclipse.equinox.security.storage.StorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private ISecurePreferences getPreferenceNode() {
		ISecurePreferences prefs = SecurePreferencesFactory.getDefault();
		ISecurePreferences node = prefs.node(Activator.ID);
		return node;
	}

	public String getName() {
		return "Dropbox"; //$NON-NLS-1$
	}

	public String getDescription() {
		return "Cloud storage based on Dropbox."; //$NON-NLS-1$
	}

	public IStorageNode getNode(String nodeId, IStorageNode parent)
			throws StorageException {
		if (parent != null && ! (parent instanceof DropboxNode))
			throw new IllegalArgumentException("Node is invalid."); //$NON-NLS-1$
		try {
			authentication();
		} catch (DropboxException e) {
			throw new StorageException(StorageException.StorageIOException, e.getMessage(), e);
		}
		String nodePath = SEPARATOR + nodeId; 
		if (parent != null) {
			final Entry parentEntry = ((DropboxNode) parent).getEntry();
			if (!parentEntry.isDir)
				throw new IllegalArgumentException("Node is invalid."); //$NON-NLS-1$
			nodePath = parentEntry.path + nodePath;
		}
		Entry direntry = null;
		try {
			direntry = dropboxApi.metadata(nodePath, 0, null, false, null); 
		} catch (DropboxUnlinkedException e) {
			throw new StorageException(StorageException.AuthenticationFailure, e.getMessage(), e);
		} catch (DropboxServerException e) {
			if (e.error == DropboxServerException._404_NOT_FOUND) {
				return null;
			}
			throw new StorageException(StorageException.UnkonwnException, e.getMessage(), e);
		} catch (DropboxIOException e) {
			throw new StorageException(StorageException.StorageIOException, e.getMessage(), e);
		} catch (DropboxException e) {
			throw new StorageException(StorageException.UnkonwnException, e.getMessage(), e);
		}		
		if (direntry.isDeleted)
			return null;
		return new DropboxNode(direntry);
	}

	public IStorageNode createNode(String nodeId, IStorageNode parent)
			throws StorageException {
		if (parent != null && ! (parent instanceof DropboxNode))
			throw new IllegalArgumentException("Node is invalid."); //$NON-NLS-1$
		try {
			authentication();
		} catch (DropboxException e) {
			throw new StorageException(StorageException.StorageIOException, e.getMessage(), e);
		}
		String nodePath = SEPARATOR + nodeId; 
		if (parent != null) {
			final Entry parentEntry = ((DropboxNode) parent).getEntry();
			if (!parentEntry.isDir)
				throw new IllegalArgumentException("Node is invalid."); //$NON-NLS-1$
			nodePath = parentEntry.path + nodePath;
		}
		Entry direntry = null;
		try {
			direntry = dropboxApi.metadata(nodePath, 0, null, false, null);
			if (direntry.isDeleted)
				direntry = createDir(nodePath);
		} catch (DropboxUnlinkedException e) {
			throw new StorageException(StorageException.AuthenticationFailure, e.getMessage(), e);
		} catch (DropboxServerException e) {
			if (e.error == DropboxServerException._404_NOT_FOUND) {
				direntry = createDir(nodePath);
			} else
				throw new StorageException(StorageException.UnkonwnException, e.getMessage(), e);
		} catch (DropboxIOException e) {
			throw new StorageException(StorageException.StorageIOException, e.getMessage(), e);
		} catch (DropboxException e) {
			throw new StorageException(StorageException.UnkonwnException, e.getMessage(), e);
		}

		return new DropboxNode(direntry);
	}

	private Entry createDir(String nodePath)
			throws StorageException {
		try {
			return dropboxApi.createFolder(nodePath);
		} catch (DropboxUnlinkedException e1) {
			throw new StorageException(StorageException.AuthenticationFailure, e1.getMessage(), e1);
		}  catch (DropboxServerException e1) {
			if (e1.error == DropboxServerException._507_INSUFFICIENT_STORAGE) {
				throw new StorageException(StorageException.OverQuota, e1.getMessage(), e1);
			} else if (e1.error == DropboxServerException._403_FORBIDDEN) {
				throw new StorageException(StorageException.StorageNodeExisting, e1.getMessage(), e1);
			}
			throw new StorageException(StorageException.UnkonwnException, e1.getMessage(), e1);
		} catch (DropboxException e1) {
			throw new StorageException(StorageException.UnkonwnException, e1.getMessage(), e1);
		}
	}

	public void removeNode(String nodeId, IStorageNode parent) throws StorageException {
		if (parent != null && ! (parent instanceof DropboxNode))
			throw new IllegalArgumentException("Parent node is invalid."); //$NON-NLS-1$
		try {
			authentication();
		} catch (DropboxException e) {
			throw new StorageException(StorageException.StorageIOException, e.getMessage(), e);
		}
		String nodePath = SEPARATOR + nodeId; 
		if (parent != null) {
			final Entry parentEntry = ((DropboxNode) parent).getEntry();
			if (!parentEntry.isDir)
				throw new IllegalArgumentException("Parent node is invalid."); //$NON-NLS-1$
			nodePath = parentEntry.path + nodePath;
		}
		try {
			dropboxApi.delete(nodePath);
		}catch (DropboxUnlinkedException e) {
			throw new StorageException(StorageException.AuthenticationFailure, e.getMessage(), e);
		} catch (DropboxServerException e) {
			if (e.error == DropboxServerException._404_NOT_FOUND) {
				throw new StorageException(StorageException.NodeNotExist, e.getMessage(), e);
			}
			throw new StorageException(StorageException.UnkonwnException, e.getMessage(), e);
		} catch (DropboxIOException e) {
			throw new StorageException(StorageException.StorageIOException, e.getMessage(), e);
		} catch (DropboxException e) {
			throw new StorageException(StorageException.UnkonwnException, e.getMessage(), e);
		}
	}

	private void authentication() throws DropboxException {
		AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
		WebAuthSession session = new WebAuthSession(appKeys, ACCESS_TYPE);
		dropboxApi = new DropboxAPI(session);
		if (token == null || secret == null) {
			initialAuthentication(session);
		} else {
			// re-auth specific stuff
			// ACCESS_TOKEN_KEY & SECRET both correspond from the two values that
			// you should have stored in the initial auth from above
			AccessTokenPair reAuthTokens = new AccessTokenPair(token, secret);
			dropboxApi.getSession().setAccessTokenPair(reAuthTokens);
		}
	}

	private void initialAuthentication(WebAuthSession session) throws DropboxException {
		Display display = Display.getDefault();
		final Shell shell = new Shell(Display.getDefault());
		shell.setLayout(new FillLayout(SWT.HORIZONTAL | SWT.VERTICAL));
		final String oauthURL = dropboxApi.getSession().getAuthInfo().url;
		int style = SWT.MOZILLA | SWT.WEBKIT;
		if (Platform.OS_WIN32.equals(Platform.getOS()))
			style = SWT.NONE;
		Browser browser = new Browser(shell, style);
		browser.addLocationListener(new LocationListener() {
			boolean entering = false;
			public void changing(LocationEvent event) {
				// do nothing
			}

			public void changed(LocationEvent event) {
				System.out.println(event.location);
				if (oauthURL.equals(event.location))
					entering = true;
				if ("https://www.dropbox.com/1/oauth/authorize".equals(event.location) ||  entering) //$NON-NLS-1$
					shell.close();
			}
		});

		browser.setUrl(oauthURL);
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				// If no more entries in event queue
				display.sleep();
			}
		}
		AccessTokenPair tokenPair = dropboxApi.getSession().getAccessTokenPair();
		// wait for user to allow app in above URL, 
		// then return back to executing code below
		RequestTokenPair tokens = new RequestTokenPair(tokenPair.key, tokenPair.secret);
		dropboxApi.getSession().retrieveWebAccessToken(tokens); // completes initial auth

		//these two calls will retrive access tokens for future use
		token = session.getAccessTokenPair().key;    // store String returned by this call somewhere
		secret = session.getAccessTokenPair().secret; // same for this line
		ISecurePreferences node = getPreferenceNode();
		try {
			node.put(TOKEN, token, true);
			node.put(SECRET, secret, true);
		} catch (org.eclipse.equinox.security.storage.StorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
