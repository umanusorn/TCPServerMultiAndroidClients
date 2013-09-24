package com.example;

import javax.swing.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The class extends the Thread class so we can receive and send messages at the
 * same time
 */
public class TCPServer extends Thread {

	public static final int SERVERPORT = 4444;
	public static final int MAX_CLIENT = 100;
	private PrintWriter[] mOuts = new PrintWriter[MAX_CLIENT];
	private static int avialableClient = 0;
	private OnMessageReceived messageListener;
	
	private final int STATUS_FIRST =0;
	private final int STATUS_OPERATOR =1;
	private final int STATUS_SECOND =2;
	private int[] data = new int[3];
	private int[] status = new int[MAX_CLIENT];
	int[] first = new int[MAX_CLIENT];
	int[] second= new int[MAX_CLIENT];
	int[] ans= new int[MAX_CLIENT];
	int clientId = 0;
	char[] charOperator= new char[MAX_CLIENT];
	

	public static void main(String[] args) {

		// opens the window where the messages will be received and sent
		ServerBoard frame = new ServerBoard();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);

	}

	/**
	 * Constructor of the class
	 * 
	 * @param messageListener
	 *            listens for the messages
	 */
	public TCPServer(OnMessageReceived messageListener) {
		this.messageListener = messageListener;
	}

	public void sendMessage(String message) {
	// if (mOut != null && !mOut.checkError()) 
		for (int i = 0; i < avialableClient; i++)
			mOuts[i].println("Server Reply :\n"+message);
	}

	/**
	 * Method to send the messages from server to client
	 * 
	 * @param message
	 *            the message sent by the server
	 */

	@Override
	public void run() {
		super.run();

		// running = true;

		try {
			int port = 4444;
			ServerSocket serverSocket = null;
			Socket socket = null;
			System.out.println("S: Connecting...");
			serverSocket = new ServerSocket(port);
			while (true) {
				socket = serverSocket.accept();
				ChatHandler handler = new ChatHandler(socket);
				handler.start();
			}
		} catch (IOException ex) {
			System.out.println("S: Error");
		}
	}

	public interface OnMessageReceived {
		public void messageReceived(String message);
	}

	// add for multiclient
	class ChatHandler extends Thread {
		ArrayList handlers = new ArrayList(MAX_CLIENT);
		private Socket socket;
		private BufferedReader read;
		private BufferedWriter write;

		public ChatHandler(Socket socket) {
			try {
				this.socket = socket;
				this.read = new BufferedReader(new InputStreamReader(
						socket.getInputStream()));

				// Check if there are too many clients.
				if (avialableClient >= MAX_CLIENT) {
					sendMessage("Client Limit Exceed!!!!!!");

				} else {
					mOuts[avialableClient] = new PrintWriter(
							new BufferedWriter(new OutputStreamWriter(
									socket.getOutputStream())), true);
					avialableClient++;
				}

			} catch (IOException ex) {
				Logger.getLogger(ChatHandler.class.getName()).log(Level.SEVERE,
						null, ex);
			}
		}
		
		public void acceptOperator(){
			sendMessage("Please enter second Integer value.");
			status[clientId] = STATUS_SECOND;
		}

		public void run() {
			String clientString = null;
			synchronized (handlers) {
				handlers.add(this);
			}
			try {
				synchronized (handlers) {
					while (!(clientString = read.readLine()).equalsIgnoreCase("/q")) {
						for (int i = 0; i < handlers.size(); i++) {
									
							ChatHandler handler = (ChatHandler) handlers.get(i);
							clientId =(int)handler.getId();
							messageListener.messageReceived("Client No."+clientId+" : "+clientString);
							
							
							
							switch(status[clientId]){
							case STATUS_FIRST :
								try{
									first[clientId] = Integer.valueOf(clientString);
									status[clientId] = STATUS_OPERATOR;
									sendMessage("Please choose operators (+ - x / %)");
								}catch(Exception exception){
									sendMessage("This is a calculator program\nPlease enter first Integer value again.");
								}
								break;
							case STATUS_OPERATOR :
								charOperator[clientId] = clientString.charAt(0);
									switch(charOperator[clientId] ){
									case '+':acceptOperator();break;
									case '-':acceptOperator();break;
									case 'x':acceptOperator();break;
									case '/':acceptOperator();break;
									case '%':acceptOperator();break;
									default : sendMessage("Please choose operators (+ - x / %) again.");
									}
								break;
							case STATUS_SECOND :
								try{
									second[clientId] = Integer.valueOf(clientString);
									status[clientId] = STATUS_FIRST;
									ans[clientId]=first[clientId]+second[clientId];
									switch(charOperator[clientId] ){
									case '+':ans[clientId]=first[clientId]+second[clientId];break;
									case '-':ans[clientId]=first[clientId]-second[clientId];break;
									case 'x':ans[clientId]=first[clientId]*second[clientId];break;
									case '/':ans[clientId]=first[clientId]/second[clientId];break;
									case '%':ans[clientId]=first[clientId]%second[clientId];break;
									default : sendMessage("Please choose operators (+ - x / %) again.");
									}
									sendMessage(String.valueOf(first[clientId])+" "+charOperator[clientId]+" "+String.valueOf(second[clientId])+" = "+String.valueOf(ans[clientId]));
									messageListener.messageReceived("Client No."+handler.getId()+" : "+String.valueOf(first[clientId])+" "+charOperator[clientId]+" "+String.valueOf(second[clientId])+" = "+String.valueOf(ans[clientId]));
								}catch(Exception exception){
									sendMessage("Please enter first Integer value.");
								}
								break;			
	
								
							}
							
						}
					}
				}

			} catch (IOException ioe) {
				ioe.printStackTrace();
			} finally {
				try {
					read.close();
					write.close();
					socket.close();
				} catch (IOException ioe) {
				} finally {                                   
					synchronized (handlers) {
						handlers.remove(this);
					}
				}
			}
		}
	}
}
