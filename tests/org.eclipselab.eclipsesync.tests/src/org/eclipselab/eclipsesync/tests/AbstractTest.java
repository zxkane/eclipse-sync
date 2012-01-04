/**
 * Copyright (c) 2011, 2012 Eclipselab Eclipse Sync and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html 
 */
package org.eclipselab.eclipsesync.tests;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.core.helpers.URLUtil;
import org.eclipse.osgi.service.datalocation.Location;
import org.junit.Assert;

public abstract class AbstractTest {
	private File testFolder = null;

	public File getTempFolder() {
		return getTestFolder(getUniqueString());
	}

	public static String getUniqueString() {
		return System.currentTimeMillis() + "-" + Math.random(); //$NON-NLS-1$
	}

	protected File getTestFolder(String name) {
		Location instanceLocation = (Location) ServiceHelper.getService(Activator.getContext(), Location.class.getName(), Location.INSTANCE_FILTER);
		URL url = instanceLocation != null ? instanceLocation.getURL() : null;
		if (instanceLocation == null || !instanceLocation.isSet() || url == null) {
			String tempDir = System.getProperty("java.io.tmpdir"); //$NON-NLS-1$
			testFolder = new File(tempDir, name);
		} else {
			File instance = URLUtil.toFile(url);
			testFolder = new File(instance, name);
		}

		if (testFolder.exists())
			delete(testFolder);
		testFolder.mkdirs();
		return testFolder;
	}

	public static boolean delete(File file) {
		if (!file.exists())
			return true;
		if (file.isDirectory()) {
			File[] children = file.listFiles();
			for (int i = 0; i < children.length; i++)
				delete(children[i]);
		}
		return file.delete();
	}

	public static void copy(String message, File source, File target) {
		copy(message, source, target, null);
	}

	/*
	 * Copy
	 * - if we have a file, then copy the file
	 * - if we have a directory then merge
	 */
	public static void copy(String message, File source, File target, FileFilter filter) {
		if (!source.exists())
			return;
		if (source.isDirectory()) {
			if (target.exists() && target.isFile())
				target.delete();
			if (!target.exists())
				target.mkdirs();
			File[] children = source.listFiles(filter);
			for (int i = 0; i < children.length; i++)
				copy(message, children[i], new File(target, children[i].getName()));
			return;
		}
		InputStream input = null;
		OutputStream output = null;
		try {
			input = new BufferedInputStream(new FileInputStream(source));
			output = new BufferedOutputStream(new FileOutputStream(target));

			byte[] buffer = new byte[8192];
			int bytesRead = 0;
			while ((bytesRead = input.read(buffer)) != -1)
				output.write(buffer, 0, bytesRead);
		} catch (IOException e) {
			fail(message, e);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					System.err.println("Exception while trying to close input stream on: " + source.getAbsolutePath()); //$NON-NLS-1$
					e.printStackTrace();
				}
			}
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					System.err.println("Exception while trying to close output stream on: " + target.getAbsolutePath()); //$NON-NLS-1$
					e.printStackTrace();
				}
			}
		}
	}
	/*
	 * Look up and return a file handle to the given entry in the bundle.
	 */
	protected File getTestData(String message, String entry) {
		if (entry == null)
			Assert.fail(message + " entry is null."); //$NON-NLS-1$
		URL base = Activator.getContext().getBundle().getEntry(entry);
		if (base == null)
			Assert.fail(message + " entry not found in bundle: " + entry); //$NON-NLS-1$
		try {
			String osPath = new Path(FileLocator.toFileURL(base).getPath()).toOSString();
			File result = new File(osPath);
			if (!result.getCanonicalPath().equals(result.getPath()))
				Assert.fail(message + " result path: " + result.getPath() + " does not match canonical path: " + result.getCanonicalFile().getPath()); //$NON-NLS-1$ //$NON-NLS-2$
			return result;
		} catch (IOException e) {
			fail(message, e);
		}
		// avoid compile error... should never reach this code
		return null;
	}

	/**
	 * Fails the test due to the given throwable.
	 */
	public static void fail(String message, Throwable e) {
		// If the exception is a CoreException with a multistatus
		// then print out the multistatus so we can see all the info.
		if (e instanceof CoreException) {
			IStatus status = ((CoreException) e).getStatus();
			//if the status does not have an exception, print the stack for this one
			if (status.getException() == null)
				e.printStackTrace();
			write(status, 0, System.err);
		} else {
			if (e != null)
				e.printStackTrace();
		}
		String msg = message;
		if (e != null)
			msg = message + ": " + e; //$NON-NLS-1$
		Assert.fail(msg);
	}

	private static void write(IStatus status, int indent, PrintStream output) {
		indent(output, indent);
		output.println("Severity: " + status.getSeverity()); //$NON-NLS-1$

		indent(output, indent);
		output.println("Plugin ID: " + status.getPlugin()); //$NON-NLS-1$

		indent(output, indent);
		output.println("Code: " + status.getCode()); //$NON-NLS-1$

		indent(output, indent);
		output.println("Message: " + status.getMessage()); //$NON-NLS-1$

		if (status.getException() != null) {
			indent(output, indent);
			output.print("Exception: "); //$NON-NLS-1$
			status.getException().printStackTrace(output);
		}

		if (status.isMultiStatus()) {
			IStatus[] children = status.getChildren();
			for (int i = 0; i < children.length; i++)
				write(children[i], indent + 1, output);
		}
	}

	private static void indent(OutputStream output, int indent) {
		for (int i = 0; i < indent; i++)
			try {
				output.write("\t".getBytes()); //$NON-NLS-1$
			} catch (IOException e) {
				// ignore
			}
	}
}
