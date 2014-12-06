package koda;

import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.InetAddress;
import java.util.Scanner;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

public class ClientProgram extends Listener {

	static Client client;
	//static String ip = "128.54.228.219";
	static int tcpPort = 27960;
	static int udpPort = 27961;
	
	static boolean loginReceived = false;
	static boolean chatMessageReceived = false;
	
	static LoginResponse response;
	
	static final JFrame frame = new JFrame("Login");
	static final JTextField username_field = new JTextField();
	static final JPasswordField password_field = new JPasswordField();
	static final JButton btn_connect = new JButton("Connect");
	
	public static void main(String[] args) throws Exception {
		showLoginDialog();
		
	}
	
	private static void showLoginDialog() {
		username_field.setActionCommand("connect");
		password_field.setActionCommand("connect");
		btn_connect.setActionCommand("connect");
		JLabel username = new JLabel("Username: ");
		JLabel password = new JLabel("Password: ");
		JPanel input = new JPanel();
		input.setLayout(new GridLayout(2, 2, 1, 1));
		input.add(username);
		input.add(username_field);
		input.add(password);
		input.add(password_field);
		JPanel content = new JPanel();
		//content.setLayout(new GridLayout(0, 1, 1, 1));
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		
		content.add(input);
		content.add(btn_connect);
		frame.setContentPane(content);
		frame.pack();
		//frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			
			@Override
			public void windowClosing(WindowEvent e) {
				if (!loginReceived) {
					System.exit(0);
				}
			}
		});
		frame.setSize(250, 100);
		frame.setLocationRelativeTo(null);
		frame.setResizable(false);
		frame.setVisible(true);
		
		username_field.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				btn_connect.doClick();
			}
		});
		
		password_field.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				btn_connect.doClick();
			}
		});
		
		btn_connect.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (username_field.getText().length() == 0 || password_field.getPassword().length == 0) {
					JOptionPane.showMessageDialog(frame, "Username/password cannot be empty", "Invalid login", JOptionPane.ERROR_MESSAGE);
					resetLoginFields();
					return;
				}
				
				try {
					username_field.setEnabled(false);
					password_field.setEnabled(false);
					btn_connect.setEnabled(false);
					connectToServer(username_field.getText(), new String(password_field.getPassword()));
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});
	}
	
	private static void connectToServer(String username, String password) throws Exception {
		client = new Client();
		registerPackets();
		client.start();
		InetAddress address = client.discoverHost(udpPort, 5000);
		if (address == null) {
			JOptionPane.showMessageDialog(frame, "No server currently running", "No server", JOptionPane.ERROR_MESSAGE);
			resetLoginFields();
			return;
		}
		client.connect(5000, address, tcpPort, udpPort);
		//client.connect(5000, ip, tcpPort, udpPort);
		client.addListener(new ClientProgram());
		
		client.setName(username);
		
		LoginMessage login = new LoginMessage();
		login.username = username;
		login.password = password;
		client.sendTCP(login);
		
		while (response == null) {
			Thread.sleep(1);
		}
		
		switch (response.login_status) {
		case LoginResponse.LOGIN_SUCCESSFUL:
		case LoginResponse.LOGIN_NEW_USER:
			JOptionPane.showMessageDialog(frame, response.message, "Login", JOptionPane.INFORMATION_MESSAGE);
			loginReceived = true;
			frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
			break;
		case LoginResponse.LOGIN_BAD_PASSWORD:
			JOptionPane.showMessageDialog(frame, response.message, "Login", JOptionPane.ERROR_MESSAGE);
			resetLoginFields();
			break;
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
		
		System.exit(0);
	}
	
	private static void showDialog(String message, String title, int messageType) {
		JOptionPane.showMessageDialog(null, message, title, messageType);
	}
	
	private static void resetLoginFields() {
		username_field.setText("");
		password_field.setText("");
		username_field.requestFocus();
		username_field.setEnabled(true);
		password_field.setEnabled(true);
		btn_connect.setEnabled(true);
	}
	
	private static void registerPackets() {
		client.getKryo().register(LoginResponse.class);
		client.getKryo().register(ChatMessage.class);
		client.getKryo().register(StatusMessage.class);
		client.getKryo().register(LoginMessage.class);
	}
	
	private static void waitForResponse() throws InterruptedException {
		while (!chatMessageReceived) {
			Thread.sleep(1);
		}
	}
	
	@Override
	public void received(Connection c, Object p) {
		if (p instanceof LoginResponse) {
			LoginResponse response = (LoginResponse) p;
			ClientProgram.response = response;
			
			

		} else if (p instanceof ChatMessage) {
			ChatMessage pkt = (ChatMessage) p;
			System.out.println("Server says: " + pkt.message);
			chatMessageReceived = true;
		} else if (p instanceof StatusMessage) {
			StatusMessage pkt = (StatusMessage) p;
			
			switch (pkt.status) {
			case StatusMessage.KICKED:
				System.out.println("You were kicked!");
				System.exit(0);
				break;
			case StatusMessage.SERVER_SHUTTING_DOWN:
				System.out.println("Server shutting down.");
				System.exit(0);
				break;
			}
		}
	}
}
