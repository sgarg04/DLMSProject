package com.dlms.replicas.replica3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.HashSet;
import java.util.PriorityQueue;

import com.dlms.replicas.replica1.MessageComparator;

public class ReplicaManager {

	private static String result = "";
	private static int Bugcount = 0;

	private static PriorityQueue<String> queue = new PriorityQueue<String>(new MessageComparator());
	private static int crashCounter = 0;
	static ActionserviceImpl conStub;
	static ActionserviceImpl mcStub;
	static ActionserviceImpl monStub;
	private static PriorityQueue<String> messageBuffer = new PriorityQueue<String>(new MessageComparator());
	private static PriorityQueue<String> tempBuffer = new PriorityQueue<String>(new MessageComparator());
	private static HashSet<String> duplicateMessSet = new HashSet<String>();

	public static void sendUDPMessage(int serverPort, String message) {
		DatagramSocket aSocket = null;
		try {
			aSocket = new DatagramSocket();
			byte[] msg = message.getBytes();
			InetAddress aHost = InetAddress.getByName("132.205.64.195");
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

	public static void main(String[] args) {
		try {
			ConcordiaLibrary.startConcordiaLibrary();
			MontrealLibrary.startMontrealLibrary();
			McGillLibrary.startMcGillLibrary();

			conStub = ConcordiaLibrary.conStub;
			mcStub = McGillLibrary.mcStub;
			monStub = MontrealLibrary.monStub;

			MulticastSocket aSocket = new MulticastSocket(1314);

			aSocket.joinGroup(InetAddress.getByName("234.1.1.1"));

			System.out.println("\nReplica manager 3 Started............");

			new Thread(() -> {
				while (true) {
					byte[] buffer = new byte[1000];
					DatagramPacket request = new DatagramPacket(buffer, buffer.length);

					String seqNum = "";
					String operation = "";
					String managerID = "";
					String userID = "";
					String newItemID = "";
					String oldItemID = "";
					String itemName = "";
					int quantity = 0;
					int numberOfDays = 0;
					String failureType = "";
					String message[] = new String[10];

					try {
						aSocket.receive(request);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					String data = new String(request.getData());
//					System.out.println(data);
//					System.out.println("\n------------Request received at Replica 3 " + data + "---------");
					// String dataArray[] = data.split(",");
					// set data in queue

					System.out.println("Message recieved is : " + data);
					int s = duplicateMessSet.size();
					duplicateMessSet.add(data.trim());

					if (duplicateMessSet.size() == s) {
						System.out.println("\n Duplicate message. Message already in queue");
						continue;
					} else {

						if (request.getPort() != 11111) {

							System.out.println(data);
							queue.add(data.trim());
							messageBuffer.add(data.trim());

						}

					}
					int c = 0;
					System.out.println("\n Request Messages in buffer");
					for (String string : messageBuffer) {

						System.out.println(++c + ". " + string);

					}
					if (!queue.isEmpty()) {
						message = queue.poll().split(",");
						seqNum = message[0];
						operation = message[1];
						managerID = message[2];
						userID = message[3];
						newItemID = message[4];
						oldItemID = message[5];
						itemName = message[6];
						quantity = Integer.parseInt(message[7]);
						numberOfDays = Integer.parseInt(message[8]);
						failureType = message[9];
						
					} else {
						failureType = "faultyCrash";
					}

					if (failureType.equalsIgnoreCase("faultyBug")) {
						Bugcount += 1;
						System.out
								.println("\nNumber of fault intimation received by FE to Replica Manager 2: " + Bugcount);
					}
					if (failureType.equalsIgnoreCase("faultyCrash")) {

						if (crashCounter == 0) {
							ConcordiaLibrary.aSocket.close();
							McGillLibrary.aSocket.close();
							MontrealLibrary.aSocket.close();
							
							crashCounter++;				
						} else {

							ConcordiaLibrary.startConcordiaLibrary();
							MontrealLibrary.startMontrealLibrary();
							McGillLibrary.startMcGillLibrary();

							conStub = ConcordiaLibrary.conStub;
							mcStub = McGillLibrary.mcStub;
							monStub = MontrealLibrary.monStub;
							int size = messageBuffer.size();
							if (!queue.isEmpty()) {
								String crashedMessage = messageBuffer.poll();
								executeQueueMessages(crashedMessage);
								tempBuffer.add(crashedMessage);
								size--;
							}

							while (size != 0) {

								String mess = messageBuffer.poll();
								executeQueueMessages(mess);
								tempBuffer.add(mess);
								size--;

							}
							messageBuffer.addAll(tempBuffer);
							tempBuffer.clear();
						//	result = conStub.listItemAvailability(managerID);

							for (String string : messageBuffer) {
								System.out.println(string + "  Message");

							}
							crashCounter--;
							System.out.println("\n---------RESULT in RM3:" + result + "---------");
							// sendUDPMessage(11111, "rm3:" + result);

						}

					} else {

						ActionserviceImpl action = new ActionserviceImpl("Montreal");
					
						if (!userID.equalsIgnoreCase("")) {					
							String idPrefix = userID.substring(0, 3).toUpperCase().trim();
							action = idPrefix.equalsIgnoreCase("CON") ? conStub
									: idPrefix.equalsIgnoreCase("MCG") ? mcStub : monStub;
						} else if (!managerID.equalsIgnoreCase("")) {

							String idPrefix = managerID.substring(0, 3);
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
							String itemIdPrefix = oldItemID.substring(0, 3).toUpperCase().trim();
							action = itemIdPrefix.equalsIgnoreCase("CON") ? conStub
									: itemIdPrefix.equalsIgnoreCase("MCG") ? mcStub : monStub;

							result = action.waitList(userID, oldItemID, numberOfDays);
						} else if (operation.equalsIgnoreCase("findItem")) {
							result = action.findItem(userID, itemName);
						} else if (operation.equalsIgnoreCase("returnItem")) {
							result = action.returnItem(userID, oldItemID);
						} else if (operation.equalsIgnoreCase("exchangeItem")) {
							result = action.exchangeItem(userID, newItemID, oldItemID);

						}
						System.out.println("\n---------RESULT in RM3:" + result + "---------");
						sendUDPMessage(11111, "rm3:" + result);

					}
					// System.out.println("At the end");

				}
			}).start();

		} catch (Exception e) {

		}

	}

	public static void executeQueueMessages(String message) {

		message = message.trim();
		String m[] = message.split(",");
		String seqNum = m[0];
		String operation = m[1];
		String managerID = m[2];
		String userID = m[3];
		String newItemID = m[4];
		String oldItemID = m[5];
		String itemName = m[6];
		int quantity = Integer.parseInt(m[7]);
		int numberOfDays = Integer.parseInt(m[8]);
		String failureType = m[9];

		ActionserviceImpl action = new ActionserviceImpl("Montreal");
		;
		if (!userID.equalsIgnoreCase("")) {
			String idPrefix = userID.substring(0, 3);
			action = idPrefix.equalsIgnoreCase("CON") ? conStub : idPrefix.equalsIgnoreCase("MCG") ? mcStub : monStub;
		} else if (!managerID.equalsIgnoreCase("")) {
			String idPrefix = managerID.substring(0, 3);
			action = idPrefix.equalsIgnoreCase("CON") ? conStub : idPrefix.equalsIgnoreCase("MCG") ? mcStub : monStub;

		}

		if (operation.equalsIgnoreCase("addItem")) {

			result = action.addItem(managerID, oldItemID, itemName, quantity);
		} else if (operation.equalsIgnoreCase("removeItem")) {
			result = action.removeItem(managerID, oldItemID, quantity);
		} else if (operation.equalsIgnoreCase("listItemAvailability") || operation.equalsIgnoreCase("faultyCrash")) {
			result = action.listItemAvailability(managerID);
		} else if (operation.equalsIgnoreCase("borrowItem")) {
			result = action.borrowItem(userID, oldItemID, numberOfDays);
		} else if (operation.equalsIgnoreCase("waitList")) {
			String itemIdPrefix = oldItemID.substring(0, 3).toUpperCase().trim();
			action = itemIdPrefix.equalsIgnoreCase("CON") ? conStub
					: itemIdPrefix.equalsIgnoreCase("MCG") ? mcStub : monStub;

			result = action.waitList(userID, oldItemID, numberOfDays);
		} else if (operation.equalsIgnoreCase("findItem")) {
			result = action.findItem(userID, itemName);
		} else if (operation.equalsIgnoreCase("returnItem")) {
			result = action.returnItem(userID, oldItemID);
		} else if (operation.equalsIgnoreCase("exchangeItem")) {
			result = action.exchangeItem(userID, newItemID, oldItemID);

		}
		System.out.println("\nOperation: " + m[0] + "    result: " + result);
	}

}
