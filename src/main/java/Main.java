import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import yuan.ticket.Ticket;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		String username = args[0];
		String password = args[1];
		String timeType = args[2];
		
		HttpClient httpClient = new DefaultHttpClient();

		Ticket ticket = new Ticket();
		ticket.setHttpClient(httpClient);
		ticket.setUsername(username);
		ticket.setPassword(password);
		ticket.setLoginURL("http://hscw.scnu.edu.cn/web/login_gr.aspx");
		ticket.setTicketURL("http://hscw.scnu.edu.cn/web/baobiao/scnu/QueueSystem.aspx");
		
		try {
			int returnCode = ticket.login();
			
			if (returnCode == 0) {
				do {
					System.out.println("===========================");
					returnCode = ticket.rob("1", "NextDday", timeType);
				} while (((returnCode != 0) && (returnCode != 1)));
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			ticket.shutdown();
		}
	}

}
