package koda;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.InetAddress;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

public class ClientProgram extends Listener {

	static Client client;
	static String ip = "128.54.226.135";
	static int tcpPort = 27960;
	static int udpPort = 27961;
	
	static boolean loginReceived = false;
	static boolean chatMessageReceived = false;
	static boolean connected = false;
	
	static LoginResponse response;
	
	static final JFrame frame = new JFrame("Login");
	static final JTextField username_field = new JTextField();
	static final JPasswordField password_field = new JPasswordField();
	static final JButton btn_connect = new JButton("Connect");
	
	//static final JTextArea text_area = new JTextArea();
	static final JTextPane text_area = new JTextPane();
	
	static String name;
	
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
		client.setName(username);
		registerPackets();
		client.start();
		
		if (ip == "") {
			InetAddress address = client.discoverHost(udpPort, 5000);
			if (address == null) {
				JOptionPane.showMessageDialog(frame, "No server currently running", "No server", JOptionPane.ERROR_MESSAGE);
				resetLoginFields();
				return;
			}
			
			client.connect(5000, address, tcpPort, udpPort);
		} else {
			client.connect(5000, ip, tcpPort, udpPort);
		}
		
		
		//client.connect(5000, ip, tcpPort, udpPort);
		client.addListener(new ClientProgram());
		
		
		
		LoginMessage login = new LoginMessage();
		login.username = username;
		login.password = password;
		
		System.out.println("Sent login info");
		client.sendTCP(login);
		
		while (response == null) {
			Thread.sleep(1);
		}
		
		
		switch (response.login_status) {
		case LoginResponse.LOGIN_SUCCESSFUL:
		case LoginResponse.LOGIN_NEW_USER:
			JOptionPane.showMessageDialog(frame, response.message, "Login", JOptionPane.INFORMATION_MESSAGE);
			loginReceived = true;
			connected = true;
			frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
			break;
		case LoginResponse.LOGIN_BAD_PASSWORD:
			JOptionPane.showMessageDialog(frame, response.message, "Login", JOptionPane.ERROR_MESSAGE);
			response = null;
			client.close();
			resetLoginFields();
			return;
		}
		
		name = login.username;
		createChatWindowGUI();
		
		/*Scanner scan = new Scanner(System.in);
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
		scan.close();*/
		
		//System.exit(0);
	}
	
	private static void createChatWindowGUI() {
		JFrame chat_frame = new JFrame("Chat Window - " + name);
		JPanel input_area = new JPanel();
		JPanel master_panel = new JPanel();
		//master_panel.setLayout(new GridLayout(2, 1, 1, 1));
		master_panel.setLayout(new BorderLayout(2, 2));
		final JTextField text_field = new JTextField();
		
		text_area.setEditable(false);
		
		final JButton btn_send = new JButton("Send");
		
		input_area.setLayout(new BorderLayout(2, 2));
		input_area.add(text_field, BorderLayout.CENTER);
		input_area.add(btn_send, BorderLayout.EAST);
		
		JPanel text_panel = new JPanel();
		text_panel.setLayout(new BorderLayout(1, 1));
		
		//text_panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 2, 5));
		text_panel.setBorder(new TitledBorder(new EtchedBorder(), "Chat Area"));
		
		master_panel.add(text_panel, BorderLayout.CENTER);
		master_panel.add(input_area, BorderLayout.SOUTH);
		//master_panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		chat_frame.setContentPane(master_panel);
		chat_frame.pack();
		chat_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		chat_frame.setSize(400, 400);
		chat_frame.setLocationRelativeTo(null);
		chat_frame.setResizable(false);
		chat_frame.setVisible(true);
		
		JScrollPane text_scroll = new JScrollPane(text_area);
		text_scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		//text_panel.add(text_area, BorderLayout.CENTER);
		text_panel.add(text_scroll, BorderLayout.CENTER);
		DefaultCaret caret = (DefaultCaret) text_area.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		
		
		
		text_field.requestFocus();
		
		text_field.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				btn_send.doClick();
			}
		});
		
		btn_send.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String s = text_field.getText();
				if (s.length() > 0) {
					s = name + ": " + s + "\n";
					ChatMessage message = new ChatMessage();
					message.message = s;
					//text_area.setText(text_area.getText() + s);
					appendToPane(text_area, s, Color.MAGENTA);
					text_field.setText("");
					text_field.requestFocusInWindow();
					client.sendTCP(message);
				}
			}
		});
	}
	
	private static void appendToPane(JTextPane tp, String msg, Color c) {
		StyledDocument doc = tp.getStyledDocument();
		
		Style style = tp.addStyle("colored text", null);
		StyleConstants.setForeground(style, c);
		
		try {
			doc.insertString(doc.getLength(), msg, style);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		
		tp.setCaretPosition(doc.getLength());
	}
	
	private static void resetLoginFields() {
		username_field.setText("");
		password_field.setText("");
		username_field.requestFocusInWindow();
		username_field.setEnabled(true);
		password_field.setEnabled(true);
		btn_connect.setEnabled(true);
	}
	
	private static void registerPackets() {
		client.getKryo().register(LoginResponse.class);
		client.getKryo().register(ChatMessage.class);
		client.getKryo().register(StatusMessage.class);
		client.getKryo().register(LoginMessage.class);
		client.getKryo().register(AnnouncementMessage.class);
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
			appendToPane(text_area, pkt.message, Color.BLACK);
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
		} else if (p instanceof AnnouncementMessage) {
			AnnouncementMessage msg = (AnnouncementMessage) p;
			
			switch (msg.announcement_type) {
			case AnnouncementMessage.ANNOUNCEMENT_REGULAR:
				appendToPane(text_area, msg.message, Color.DARK_GRAY);
				break;
			case AnnouncementMessage.ANNOUNCEMENT_NOTIFICATION:
				appendToPane(text_area, msg.message, Color.RED);
				break;
			}
		}
	}
}
