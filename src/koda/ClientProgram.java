package koda;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

public class ClientProgram extends Listener {

	static Client client;
	static String ip = "128.54.228.219";
	static int tcpPort = 27961;
	static int udpPort = 27961;
	
	static boolean messageReceived = false;
	
	public static void main(String[] args) throws Exception {
		client = new Client();
		client.getKryo().register(PacketMessage.class);
		client.start();
		client.connect(5000, ip, tcpPort, udpPort);
		client.addListener(new ClientProgram());
		
		while (!messageReceived) {
			Thread.sleep(1000);
		}
		
		System.out.println("Client received the message. Exiting");
	}
	
	@Override
	public void received(Connection c, Object p) {
		if (p instanceof PacketMessage) {
			PacketMessage message = (PacketMessage) p;
			System.out.println("Server says: " + message.message);
			messageReceived = true;
		}
	}
}
