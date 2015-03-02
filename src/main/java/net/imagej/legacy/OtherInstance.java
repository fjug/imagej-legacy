/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2015 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
 * Institute of Molecular Cell Biology and Genetics.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package net.imagej.legacy;

import ij.IJ;
import ij.ImageJ;
import ij.io.OpenDialog;
import ij.io.Opener;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Properties;

/**
 * This class tries to contact another instance on the same machine, started by
 * the current user. If such an instance is found, the arguments are sent to
 * that instance. If no such an instance is found, listen for clients.
 * <p>
 * No need for extra security, as the stub (and its serialization) contain a
 * hard-to-guess hash code.
 * </p>
 *
 * @author Johannes Schindelin
 */
public class OtherInstance {

	interface ImageJInstance extends Remote {

		void sendArgument(String arg) throws RemoteException;
	}

	static class Implementation implements ImageJInstance {

		int counter = 0;

		@Override
		public void sendArgument(final String cmd) {
			if (IJ.debugMode) IJ.log("SocketServer.sendArgument: \"" + cmd + "\"");
			if (cmd.startsWith("open ")) (new Opener()).openAndAddToRecent(cmd
				.substring(5));
			else if (cmd.startsWith("macro ")) {
				String name = cmd.substring(6);
				final String name2 = name;
				String arg = null;
				if (name2.endsWith(")")) {
					final int index = name2.lastIndexOf("(");
					if (index > 0) {
						name = name2.substring(0, index);
						arg = name2.substring(index + 1, name2.length() - 1);
					}
				}
				IJ.runMacroFile(name, arg);
			}
			else if (cmd.startsWith("run ")) IJ.run(cmd.substring(4));
			else if (cmd.startsWith("eval ")) {
				final String rtn = IJ.runMacro(cmd.substring(5));
				if (rtn != null) System.out.print(rtn);
			}
			else if (cmd.startsWith("user.dir ")) OpenDialog.setDefaultDirectory(cmd
				.substring(9));
		}
	}

	public static String getStubPath() {
		String display = System.getenv("DISPLAY");
		if (display != null) {
			display = display.replace(':', '_');
			display = display.replace('/', '_');
		}
		String tmpDir = System.getProperty("java.io.tmpdir");
		if (!tmpDir.endsWith(File.separator)) tmpDir = tmpDir + File.separator;

		return tmpDir + "ImageJ-" + System.getProperty("user.name") + "-" +
			(display == null ? "" : display + "-") + ImageJ.getPort() + ".stub";
	}

	public static void makeFilePrivate(final String path) {
		final File file = new File(path);
		file.deleteOnExit();
		file.setReadable(false, false);
		file.setReadable(true, true);
		file.setWritable(false);
	}

	public static boolean sendArguments(final String[] args) {
		if (!isRMIEnabled()) return false;
		final String file = getStubPath();
		if (args.length > 0) try {
			final FileInputStream in = new FileInputStream(file);
			final ImageJInstance instance =
				(ImageJInstance) new ObjectInputStream(in).readObject();
			in.close();
			if (instance == null) return false;

			// IJ.log("sendArguments3: "+instance);
			instance.sendArgument("user.dir " + System.getProperty("user.dir"));
			int macros = 0;
			for (int i = 0; i < args.length; i++) {
				final String arg = args[i];
				if (arg == null) continue;
				String cmd = null;
				if (macros == 0 && arg.endsWith(".ijm")) {
					cmd = "macro " + arg;
					macros++;
				}
				else if (arg.startsWith("-macro") && i + 1 < args.length) {
					final String macroArg =
						i + 2 < args.length ? "(" + args[i + 2] + ")" : "";
					cmd = "macro " + args[i + 1] + macroArg;
					instance.sendArgument(cmd);
					break;
				}
				else if (arg.startsWith("-eval") && i + 1 < args.length) {
					cmd = "eval " + args[i + 1];
					args[i + 1] = null;
				}
				else if (arg.startsWith("-run") && i + 1 < args.length) {
					cmd = "run " + args[i + 1];
					args[i + 1] = null;
				}
				else if (arg.indexOf("ij.ImageJ") == -1 && !arg.startsWith("-")) cmd =
					"open " + arg;
				if (cmd != null) instance.sendArgument(cmd);
			} // for

			// IJ.log("sendArguments: return true");
			return true;
		}
		catch (final Exception e) {
			if (IJ.debugMode) {
				System.err.println("Client exception: " + e);
				e.printStackTrace();
			}
			new File(file).delete();
		}
		if (!new File(file).exists()) startServer();
		// IJ.log("sendArguments: return false ");
		return false;
	}

	static ImageJInstance stub;
	static Implementation implementation;

	public static void startServer() {
		if (IJ.debugMode) System.err.println("OtherInstance: starting server");
		try {
			implementation = new Implementation();
			stub =
				(ImageJInstance) UnicastRemoteObject.exportObject(implementation, 0);

			// Write serialized object
			final String path = getStubPath();
			final FileOutputStream out = new FileOutputStream(path);
			makeFilePrivate(path);
			new ObjectOutputStream(out).writeObject(stub);
			out.close();

			if (IJ.debugMode) System.err.println("OtherInstance: server ready");
		}
		catch (final Exception e) {
			if (IJ.debugMode) {
				System.err.println("Server exception: " + e);
				e.printStackTrace();
			}
		}
	}

	private static final String OPTIONS = "prefs.options";
	private static final int RUN_SOCKET_LISTENER = 1 << 22;

	public static boolean isRMIEnabled() {
		if (System.getProperty("os.name").startsWith("Mac")) return true;
		final Properties ijProps = loadPrefs();
		if (ijProps == null) return true;
		final int options = getInt(ijProps, OPTIONS);
		if (options == -1) return true;
		return (options & RUN_SOCKET_LISTENER) != 0;
	}

	protected static int getInt(final Properties props, final String key) {
		final String s = props.getProperty(key);
		if (s != null) {
			try {
				return Integer.decode(s).intValue();
			}
			catch (final NumberFormatException e) {
				IJ.write("" + e);
			}
		}
		return -1;
	}

	protected static Properties loadPrefs() {
		final Properties result = new Properties();
		final File file = new File(getPrefsDirectory(), "IJ_Prefs.txt");
		try {
			final InputStream in = new BufferedInputStream(new FileInputStream(file));
			result.load(in);
			in.close();
		}
		catch (final IOException e) { /* ignore */}
		return result;
	}

	protected static String getPrefsDirectory() {
		final String env = System.getenv("IJ_PREFS_DIR");
		if (env != null) return env;
		if (IJ.isWindows()) return System.getProperty("user.dir");
		String prefsDir = System.getProperty("user.home");
		if (IJ.isMacOSX()) prefsDir += "/Library/Preferences";
		else prefsDir += "/.imagej";
		return prefsDir;
	}
}
