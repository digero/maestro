package com.digero.maestro.util.singleinstance;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.SwingUtilities;

/**
 * Ensures that only one instance of an application is running at once.
 */
public class SingleInstanceManager extends Thread {
	private static final String CLIENT_NEW_INSTANCE = "$$_CLIENT_NEW_INSTANCE_$$";
	private static final String SERVER_RESPONSE_SUCCESS = "$$_SERVER_RESPONSE_SUCCESS_$$";

	private ServerSocket listenSocket;
	private SingleInstanceListener listener;

	/**
	 * If there is no other application running with the given port, creates and
	 * returns a new SingleInstanceManager.
	 * 
	 * @param uniquePort A port number that is unique to this application.
	 * @param args The arguments to the main() function of the current process.
	 * @return A new SingleInstanceManager, or null if the program is already
	 *         running.
	 */
	public static SingleInstanceManager createInstance(int uniquePort, String[] args) throws SingleInstanceException {
		try {
			return new SingleInstanceManager(uniquePort);
		}
		catch (BindException e) {
			// Socket is already open by another instance
			sendArgs(uniquePort, args);
			return null;
		}
		catch (IOException e) {
			throw new SingleInstanceException(e);
		}
		catch (SecurityException e) {
			throw new SingleInstanceException(e);
		}
	}

	private static void sendArgs(int port, String[] args) throws SingleInstanceException {
		Socket client = null;
		OutputStream clientOutput = null;
		ObjectOutputStream oos = null;

		try {
			client = new Socket(InetAddress.getLocalHost(), port);
			clientOutput = client.getOutputStream();
			oos = new ObjectOutputStream(clientOutput);
			oos.writeObject(CLIENT_NEW_INSTANCE);
			oos.writeObject(args);
		}
		catch (IOException e) {
			throw new SingleInstanceException(e);
		}
		finally {
			try {
				if (oos != null)
					oos.close();
			}
			catch (IOException e) {}
			try {
				if (clientOutput != null)
					clientOutput.close();
			}
			catch (IOException e) {}
			try {
				if (client != null)
					client.close();
			}
			catch (IOException e) {}
		}
	}

	private SingleInstanceManager(int uniquePort) throws BindException, IOException, SecurityException {
		super("SingleInstanceManager");
		listenSocket = new ServerSocket(uniquePort, 0, InetAddress.getLocalHost());
	}

	public void setListener(SingleInstanceListener listener) {
		this.listener = listener;
	}

	@Override
	public void run() {
		while (true) {
			Socket client = null;
			ObjectInputStream ois = null;
			try {
				client = listenSocket.accept();
				ois = new ObjectInputStream(client.getInputStream());

				Object header = ois.readObject();
				if (CLIENT_NEW_INSTANCE.equals(header)) {
					Object argsObj = ois.readObject();
					if (argsObj instanceof String[]) {
						String[] args = (String[]) argsObj;
						// Call the listener on the main thread
						SwingUtilities.invokeLater(new NewActivationRunnable(args));
					}
				}
			}
			catch (IOException e) {
				// Ignore
			}
			catch (ClassNotFoundException e) {
				// Ignore
			}
			finally {
				try {
					if (ois != null)
						ois.close();
				}
				catch (IOException e) {}
				try {
					if (client != null)
						client.close();
				}
				catch (IOException e) {}
			}
		}
	}

	private class NewActivationRunnable implements Runnable {
		private String[] args;

		public NewActivationRunnable(String[] args) {
			this.args = args;
		}

		@Override
		public void run() {
			if (listener != null) {
				listener.newActivation(args);
			}
		}
	}
}
