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
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipselab.eclipsesync.core.IStorageNode;
import org.eclipselab.eclipsesync.core.ISyncStorage;
import org.eclipselab.eclipsesync.core.StorageException;
import org.eclipselab.eclipsesync.ui.IPreferenceOptions;
import org.osgi.framework.wiring.BundleWiring;

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

public class DropboxStorage implements ISyncStorage, IPreferenceOptions {

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
					throw new StorageException(StorageException.ConfigNotFound, Messages.DropboxStorage_ConfigNotExist, null);
				return new ByteArrayInputStream(outputStream.toByteArray());
			} catch (DropboxUnlinkedException e) {
				throw new StorageException(StorageException.AuthenticationFailure, Messages.DropboxStorage_AuthFailed, e);
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
				throw new StorageException(StorageException.AuthenticationFailure, Messages.DropboxStorage_AuthFailed, e);
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
				throw new StorageException(StorageException.AuthenticationFailure, Messages.DropboxStorage_AuthFailed, e);
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

	static private String APP_KEY = Messages.DropboxStorage_AppKey;
	static private String APP_SECRET = Messages.DropboxStorage_AppSecret;
	final static private AccessType ACCESS_TYPE = AccessType.APP_FOLDER;
	private final static String TOKEN = "token"; //$NON-NLS-1$
	private final static String SECRET = "secret"; //$NON-NLS-1$
	String token = null;
	String secret = null;

	// In the class declaration section:
	DropboxAPI<WebAuthSession> dropboxApi;

	public DropboxStorage() {
		ISecurePreferences node = getPreferenceNode();
		try {
			token = node.get(TOKEN, null);
			secret = node.get(SECRET, null);
		} catch (org.eclipse.equinox.security.storage.StorageException e) {
			Platform.getLog(Activator.getContext().getBundle()).log(new Status(IStatus.ERROR, Activator.ID,
					Messages.DropboxStorage_FailGetUserToken, e));
		}
		InputStream input = null;
		try {
			final BundleWiring wiring = Activator.getContext().getBundle().adapt(BundleWiring.class);
			List<URL> entries = wiring.findEntries("/", "token", BundleWiring.FINDENTRIES_RECURSE); //$NON-NLS-1$ //$NON-NLS-2$
			if (entries.size() == 0) {
				Platform.getLog(Activator.getContext().getBundle()).log(new Status(IStatus.ERROR, Activator.ID,
						Messages.DropboxStorage_FailGetAppToken, null));
				return;
			}
			input = entries.get(0).openStream();
			SecretKeySpec keyspec = new SecretKeySpec(APP_KEY.getBytes(), "AES"); //$NON-NLS-1$
			Cipher cipher = Cipher.getInstance("AES"); //$NON-NLS-1$
			cipher.init(Cipher.DECRYPT_MODE, keyspec);
			CipherInputStream aesIn = new CipherInputStream(input, cipher);
			Properties props = new Properties();
			props.load(aesIn);
			APP_KEY = props.getProperty("AppKey"); //$NON-NLS-1$
			APP_SECRET = props.getProperty("AppSecret"); //$NON-NLS-1$
		} catch (IOException e) {
			Platform.getLog(Activator.getContext().getBundle()).log(new Status(IStatus.ERROR, Activator.ID,
					Messages.DropboxStorage_FailGetAppToken, e));
		} catch (NoSuchAlgorithmException e) {
			Platform.getLog(Activator.getContext().getBundle()).log(new Status(IStatus.ERROR, Activator.ID,
					Messages.DropboxStorage_FailGetAppToken, e));
		} catch (NoSuchPaddingException e) {
			Platform.getLog(Activator.getContext().getBundle()).log(new Status(IStatus.ERROR, Activator.ID,
					Messages.DropboxStorage_FailGetAppToken, e));
		} catch (InvalidKeyException e) {
			Platform.getLog(Activator.getContext().getBundle()).log(new Status(IStatus.ERROR, Activator.ID,
					Messages.DropboxStorage_FailGetAppToken, e));
		} finally {
			if (input != null)
				try {
					input.close();
				} catch (IOException e) {
					//
				}
		}
	}

	ISecurePreferences getPreferenceNode() {
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
			throw new StorageException(StorageException.AuthenticationFailure, Messages.DropboxStorage_AuthFailed, e);
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
			throw new StorageException(StorageException.AuthenticationFailure, Messages.DropboxStorage_AuthFailed, e);
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
			throw new StorageException(StorageException.AuthenticationFailure, Messages.DropboxStorage_AuthFailed, e);
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
			throw new StorageException(StorageException.AuthenticationFailure, Messages.DropboxStorage_AuthFailed, e);
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
			throw new StorageException(StorageException.AuthenticationFailure, Messages.DropboxStorage_AuthFailed, e1);
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
			throw new StorageException(StorageException.AuthenticationFailure, Messages.DropboxStorage_AuthFailed, e);
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
			throw new StorageException(StorageException.AuthenticationFailure, Messages.DropboxStorage_AuthFailed, e);
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
		class OAuthRunnable implements Runnable {
			DropboxException dropboxException;
			Shell browserShell; 

			public void run() {
				Display display = Display.getDefault();
				browserShell = new Shell(display, SWT.APPLICATION_MODAL | SWT.SHELL_TRIM);
				browserShell.setText(Messages.DropboxStorage_OAuthTitle);
				browserShell.setLayout(new FillLayout(SWT.HORIZONTAL | SWT.VERTICAL));
				try {
					final String oauthURL = dropboxApi.getSession().getAuthInfo().url;
					int style = SWT.MOZILLA | SWT.WEBKIT;
					if (Platform.OS_WIN32.equals(Platform.getOS()))
						style = SWT.NONE;
					Browser browser = new Browser(browserShell, style);
					browser.addLocationListener(new LocationListener() {
						boolean entering = false;
						public void changing(LocationEvent event) {
							// do nothing
						}

						public void changed(LocationEvent event) {
							if (oauthURL.equals(event.location))
								entering = true;
							if ("https://www.dropbox.com/1/oauth/authorize".equals(event.location) ||  entering) //$NON-NLS-1$
								browserShell.close();
						}
					});

					browser.setUrl(oauthURL);
				} catch (DropboxException e) {
					dropboxException = e;
					if (browserShell != null && !browserShell.isDisposed())
						browserShell.dispose();
					return;
				}
				browserShell.open();
			}
		}

		OAuthRunnable oAuthRunnable = new OAuthRunnable();
		Display.getDefault().syncExec(oAuthRunnable);		
		while (!oAuthRunnable.browserShell.isDisposed()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// 
			}
		}

		if (oAuthRunnable.dropboxException != null)
			throw oAuthRunnable.dropboxException;

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
			Platform.getLog(Activator.getContext().getBundle()).log(new Status(IStatus.ERROR, Activator.ID,
					Messages.DropboxStorage_FailSaveUserToken, e));
		}
	}

	public void createOptions(final Composite control, ISyncStorage storage, IPreferenceStore store) {
		control.setLayout(new FillLayout());
		final Link unlinkDrop = new Link(control, SWT.NONE);
		unlinkDrop.setText(Messages.DropboxPreference_LinkText);
		unlinkDrop.setData("state", Boolean.TRUE); //$NON-NLS-1$
		if (token == null || secret == null) {
			disableLink(unlinkDrop);
		}

		unlinkDrop.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (MessageDialog.openConfirm(control.getShell(), Messages.DropboxPreference_ConfirmDialogTitle, Messages.DropboxPreference_ConfirmDialogMessage)) {
					disableLink(unlinkDrop);
					token = null;
					secret = null;
					ISecurePreferences node = getPreferenceNode();
					try {
						node.remove(TOKEN);
						node.remove(SECRET);
					} catch (IllegalStateException exception) {
						// do nothing
					}
					disableLink(unlinkDrop);
				}
			}
		});
	}

	void disableLink(Link unlinkDrop) {
		unlinkDrop.setEnabled(false);
		unlinkDrop.setData("disable", new Boolean(false)); //$NON-NLS-1$
		unlinkDrop.setData("state", Boolean.FALSE); //$NON-NLS-1$
	}

	public boolean performOk() {
		return true;
	}

	public boolean performDefaults() {
		return true;
	}
}
