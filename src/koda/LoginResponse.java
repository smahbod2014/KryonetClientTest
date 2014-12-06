package koda;

public class LoginResponse {

	public static final int LOGIN_SUCCESSFUL = 0;
	//public static final int LOGIN_BAD_USERNAME = 1;
	public static final int LOGIN_BAD_PASSWORD = 2;
	public static final int LOGIN_NEW_USER = 3;
	
	public String message;
	public int login_status;
}
