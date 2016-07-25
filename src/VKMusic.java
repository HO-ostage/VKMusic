import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.*;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class VKMusic {

	boolean firstRun = true;
	
	
	final String appID = "5557569";
	final String redirectURI = "https://oauth.vk.com/blank.html";
	final String authScope = "audio";
	final String responseType = "token";
	final String apiVersion = "5.53";
	
	String userID = null;
	String accessToken = null;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		authVK();
		parseAndDownload();
	}
	
	private void authVK() {
		URI authURI = null;
		try {
			authURI = new URIBuilder ()
					 .setScheme("http")
					 .setHost("oauth.vk.com")
					 .setPath("/authorize")
					 .setParameter("client_id", appID)
					 .setParameter("redirect_uri", redirectURI)
					 .setParameter("scope", authScope)
					 .setParameter("response_type", responseType)
					 .setParameter("v", apiVersion)
					 .build();
		} catch (URISyntaxException e) {
			System.out.println("Wrong URI");
		}
		
		if(firstRun) {
			openInBrowser(authURI);
		}
		
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(authURI);
		CloseableHttpResponse response = null;
		try {
			response = httpClient.execute(httpGet);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void openInBrowser(URI uri) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		
		Browser browser = new Browser(shell, SWT.NONE);
		browser.setUrl(uri.toASCIIString());
		browser.addLocationListener(new LocationListener() {
			public void changing(LocationEvent event) {}
			public void changed(LocationEvent event) {
				if(event.location.contains("/blank.html")) {
					accessToken = getAccessToken(event.location);
					System.out.println(accessToken);
					userID = getUserID(event.location);
				}
			}
		});
		
		shell.open();
		shell.setFocus();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}
	
	private String getUserID(String fromUri) {
		return fromUri
				.substring(fromUri.indexOf("user_id"))
				.split("=", 1)[1];
	}
	private String getAccessToken(String fromUri) {
		return fromUri
				.substring(fromUri.indexOf("access_token"))
				.split("=", 1)[1];
		//String[] str = fromUri.split("#");
		//String[] parameters = str[1].split("&");
		//return parameters[0].split("=")[1];
	}

	private void parseAndDownload() {
		URI getMusicUri = null;
		try {
			getMusicUri = new URIBuilder()
						.setScheme("http")
						.setHost("api.vk.com")
						.setPath("/method/audio.get")
						.setParameter("oid", userID)
						.setParameter("need_user", "0")
						.setParameter("count", "6000")
						.setParameter("offset", "0")
						.setParameter("access_token", accessToken)
						.build();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private String fixWindowsFileName(String pathname) {
		return "";
	}
	
	class Song {
		private String name, artist;
		
		Song(String n, String a) {
			name = n; 
			artist = a;
		}
		
		String getName() {
			return name;
		}
		
		String getArtist() {
			return artist;
		}
	}
}
