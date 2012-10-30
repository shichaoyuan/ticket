package yuan.ticket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Consts;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

public class Ticket {

	private HttpClient httpClient;

	private String username;
	private String password;
	private String loginURL;
	private String ticketURL;
	
	public Ticket() {
	}

	public Ticket(HttpClient httpClient, String username, String password,
			String loginURL, String ticketURL) {
		this.httpClient = httpClient;
		this.username = username;
		this.password = password;
		this.loginURL = loginURL;
		this.ticketURL = ticketURL;
	}
	
	/**
	 * set up network proxy
	 * @param url
	 * @param port
	 * @param scheme
	 */
	public void setProxy(String url, int port, String scheme) {
		HttpHost proxy = new HttpHost(url, port, scheme);
		httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
	}
	
	/**
	 * shutdown connections and release resources
	 */
	public void shutdown() {
		this.httpClient.getConnectionManager().shutdown();
	}
	
	/**
	 * login the system
	 * 
	 * @return returnCode 0--success 1--fail
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public int login() throws ClientProtocolException, IOException {
		int returnCode = 1;
		HttpPost loginMethod = new HttpPost(loginURL);

		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("uid", username));
		nvps.add(new BasicNameValuePair("pwd", password));

		loginMethod.setEntity(new UrlEncodedFormEntity(nvps, Consts.UTF_8));
		HttpResponse response = httpClient.execute(loginMethod);

		if ("HTTP/1.1 302 Found".equals(response.getStatusLine().toString())) {
			returnCode = 0;
			System.out.println("login successful");
		} else {
			returnCode = 1;
			System.out.println("login failed");
		}
		loginMethod.releaseConnection();
		return returnCode;
	}

	/**
	 * rob the ticket
	 * 
	 * @param deptID
	 * @param dateType
	 * @param timeType
	 * @return returnCode 0--success 1-already have 2--fail 3--unknown
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public int rob(String deptID, String dateType, String timeType)
			throws ClientProtocolException, IOException {

		int returnCode = 3;
		// compose URL
		String postTicktURL = ticketURL + "?deptID=" + deptID + "&dateType=" + dateType
				+ "&timeType=" + timeType;

		// get necessary information
		HttpGet getInfoMethod = new HttpGet(postTicktURL);
		HttpResponse response = httpClient.execute(getInfoMethod);

		if (!"HTTP/1.1 200 OK".equals(response.getStatusLine().toString())) {
			System.out.println("get info failed");
			returnCode = 3;
		} else {
			System.out.println("get info successful");
			
			String content = EntityUtils.toString(response.getEntity());

			String viewstateRegex = "<input type=\"hidden\" name=\"__VIEWSTATE\" id=\"__VIEWSTATE\" value=\"(.*)\" />";
			String eventvalidationRegex = "<input type=\"hidden\" name=\"__EVENTVALIDATION\" id=\"__EVENTVALIDATION\" value=\"(.*)\" />";
			String dropdownlist2Regex = "<option value=\"(.*)\">.*</option>";

			String viewstate = extractValue(viewstateRegex, content);
			String eventvalidation = extractValue(eventvalidationRegex, content);
			String dropdownlist2 = extractValue(dropdownlist2Regex, content);
			
			if ("0".equals(dropdownlist2)) {
				// post request
				HttpPost postTicketMethod = new HttpPost(postTicktURL);

				List<NameValuePair> nvps = new ArrayList<NameValuePair>();
				nvps.add(new BasicNameValuePair("__VIEWSTATE", viewstate));
				nvps.add(new BasicNameValuePair("ImageButton1.x", "164"));
				nvps.add(new BasicNameValuePair("ImageButton1.y", "24"));
				nvps.add(new BasicNameValuePair("DropDownList2", dropdownlist2));
				nvps.add(new BasicNameValuePair("deptID", deptID));
				nvps.add(new BasicNameValuePair("dateType", dateType));
				nvps.add(new BasicNameValuePair("timeType", timeType));
				nvps.add(new BasicNameValuePair("__EVENTVALIDATION", eventvalidation));
				postTicketMethod.setEntity(new UrlEncodedFormEntity(nvps, Consts.UTF_8));

				HttpResponse ticketResponse = httpClient.execute(postTicketMethod);

				if (!"HTTP/1.1 200 OK".equals(ticketResponse.getStatusLine().toString())) {
					System.out.println("post request failed");
					returnCode = 3;
				} else {
					String ticketContent = EntityUtils.toString(ticketResponse.getEntity());

					String resultRegex = "<SCRIPT LANGUAGE='javascript'>alert\\('(.*)'\\);</SCRIPT>";
					String result = extractValue(resultRegex, ticketContent);

					if (result != null) {
						if (result.startsWith("取号失败")) {
							System.out.println(result);
							returnCode = 2;
						} else if (result.startsWith("取号成功")) {
							System.out.println(result);
							returnCode = 0;
						} else {
							System.out.println(result);
							returnCode = 3;
						}
					} else {
						System.out.println("can not get result");
						returnCode = 3;
					}
				}

			} else {
				System.out.println("you have got a ticket");
				returnCode = 1;
			}
		}

		return returnCode;

	}

	private String extractValue(String regex, String content) {
		Matcher matcher = Pattern.compile(regex).matcher(content);
		String value = null;
		if (matcher.find()) {
			value = matcher.group(1);
		}
		return value;
	}

	public HttpClient getHttpClient() {
		return httpClient;
	}

	public void setHttpClient(HttpClient httpClient) {
		this.httpClient = httpClient;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getLoginURL() {
		return loginURL;
	}

	public void setLoginURL(String loginURL) {
		this.loginURL = loginURL;
	}

	public String getTicketURL() {
		return ticketURL;
	}

	public void setTicketURL(String ticketURL) {
		this.ticketURL = ticketURL;
	}

}
