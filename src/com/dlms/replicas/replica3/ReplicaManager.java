package com.dlms.replicas.replica3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.PriorityQueue;

import com.dlms.replicas.replica1.ActionServiceImpl;
import com.dlms.replicas.replica1.Concordia;
import com.dlms.replicas.replica1.McGill;
import com.dlms.replicas.replica1.MessageComparator;
import com.dlms.replicas.replica1.Montreal;

public class ReplicaManager {

	private static String result = "";

	public static void sendUDPMessage(int serverPort, String message) {
		DatagramSocket aSocket = null;
		try {
			aSocket = new DatagramSocket();
			byte[] msg = message.getBytes();
			InetAddress aHost = InetAddress.getByName("localhost");
			DatagramPacket request = new DatagramPacket(msg, msg.length, aHost, serverPort);
			aSocket.send(request);
			
		} catch (SocketException e) {
			System.out.println("Socket: " + e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("IO: " + e.getMessage());
		} finally {
			if (aSocket != null)
				aSocket.close();
		}
	}

	public static void main(String[] args){
		try {

			ActionserviceImpl conStub = ConcordiaLibrary.conStub;
			ActionserviceImpl mcStub = McGillLibrary.mcStub;
			ActionserviceImpl monStub = MontrealLibrary.monStub;
			ConcordiaLibrary.startConcordiaLibrary();
			MontrealLibrary.startMontrealLibrary();
			McGillLibrary.startMcGillLibrary();

			MulticastSocket aSocket = new MulticastSocket(1313);

			aSocket.joinGroup(InetAddress.getByName("230.1.1.5"));

			System.out.println("Server Started............");

			new Thread(() -> {
				while (true) {
					byte[] buffer = new byte[1000];
					DatagramPacket request = new DatagramPacket(buffer, buffer.length);
					
						try {
							aSocket.receive(request);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					
					System.out.println("abcd---" + request.getData().toString());
					String data = new String(request.getData());
					System.out.println(data);
//				String dataArray[] = data.split(",");
					// set data in queue

					PriorityQueue<String> queue = new PriorityQueue<String>(new MessageComparator());

					String message[] = queue.poll().split(",");
					String operation = message[0];
					String managerID = message[1];
					String userID = message[2];
					String itemID = message[3];
					String newItemID = message[4];
					String oldItemID = message[5];
					String itemName = message[6];
					int quantity = Integer.parseInt(message[7]);
					int numberOfDays = Integer.parseInt(message[8]);
					String failureType = message[9];

					if (failureType.equalsIgnoreCase("faultyBug")) {

					} else if (failureType.equalsIgnoreCase("faultyCrash")) {

					} else {
						ActionserviceImpl action;
						if(managerID!=null) {
						String idPrefix = managerID.substring(0, 3);
						action = idPrefix.equalsIgnoreCase("CON") ? conStub
								: idPrefix.equalsIgnoreCase("MCG") ? mcStub : monStub;
						}
						else {
							String idPrefix = userID.substring(0, 3);
							action = idPrefix.equalsIgnoreCase("CON") ? conStub
									: idPrefix.equalsIgnoreCase("MCG") ? mcStub : monStub;
							
						}
						
						if (operation.equalsIgnoreCase("addItem")) {

							result = action.addItem(managerID, oldItemID, itemName, quantity);
						} else if (operation.equalsIgnoreCase("removeItem")) {
							result = action.removeItem(managerID, oldItemID, quantity);
						} else if (operation.equalsIgnoreCase("listItemAvailability")) {
							result = action.listItemAvailability(managerID);
						} else if (operation.equalsIgnoreCase("borrowItem")) {
							result = action.borrowItem(userID, oldItemID, numberOfDays);
						} else if (operation.equalsIgnoreCase("waitList")) {
							result = action.waitList(userID, oldItemID, numberOfDays);
						} else if (operation.equalsIgnoreCase("findItem")) {
							result = action.findItem(userID, itemName);
						} else if (operation.equalsIgnoreCase("returnItem")) {
							result = action.returnItem(userID, oldItemID);
						} else if (operation.equalsIgnoreCase("exchangeItem")) {
							result = action.exchangeItem(userID, newItemID, oldItemID);
							
						}
					}
					sendUDPMessage(11111, result);
					

				}
			}).start();

		} catch (Exception e) {

		}
		

	}

}
