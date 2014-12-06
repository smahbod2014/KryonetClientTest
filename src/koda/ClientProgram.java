package koda;

import java.net.InetAddress;
import java.util.Scanner;

import javax.swing.JOptionPane;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

public class ClientProgram extends Listener {

	static Client client;
	//static String ip = "128.54.228.219";
	static int tcpPort = 27960;
	static int udpPort = 27961;
	
	static boolean messageReceived = false;
	static boolean chatMessageReceived = false;
	
	public static void main(String[] args) throws Exception {
		client = new Client();
		registerPackets();
		client.start();
		InetAddress address = client.discoverHost(udpPort, 5000);
		client.connect(5000, address, tcpPort, udpPort);
		//client.connect(5000, ip, tcpPort, udpPort);
		client.addListener(new ClientProgram());
		
		while (!messageReceived) {
			Thread.sleep(1000);
		}
		
		
		
		Scanner scan = new Scanner(System.in);
		String input = "";
		System.out.print("Enter message: ");
		while (!(input = scan.nextLine()).equals("done")) {
			chatMessageReceived = false;
			ChatMessage pkt = new ChatMessage();
			pkt.message = input;
			client.sendTCP(pkt);
			waitForResponse();
			System.out.print("Enter message: ");
		}
		scan.close();
	}
	
	public static void registerPackets() {
		client.getKryo().register(PacketMessage.class);
		client.getKryo().register(ChatMessage.class);
		client.getKryo().register(StatusMessage.class);
	}
	
	private static void waitForResponse() throws InterruptedException {
		while (!chatMessageReceived) {
			Thread.sleep(1);
		}
	}
	
	@Override
	public void received(Connection c, Object p) {
		if (p instanceof PacketMessage) {
			PacketMessage message = (PacketMessage) p;
			System.out.println("Server says: " + message.message);
			messageReceived = true;
		} else if (p instanceof ChatMessage) {
			ChatMessage pkt = (ChatMessage) p;
			System.out.println("Server says: " + pkt.message);
			chatMessageReceived = true;
		} else if (p instanceof StatusMessage) {
			StatusMessage pkt = (StatusMessage) p;
			switch (pkt.status) {
			case StatusMessage.KICKED:
				JOptionPane.showMessageDialog(null, "You were kicked!", "Kicked", JOptionPane.INFORMATION_MESSAGE);
				System.exit(0);
				break;
			case StatusMessage.SERVER_SHUTTING_DOWN:
				JOptionPane.showMessageDialog(null, "Server shutting down.", "Server shutting down", JOptionPane.INFORMATION_MESSAGE);
				System.exit(0);
				break;
			}
		}
	}
}
