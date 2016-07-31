package com.afal.http.vkmusic;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.prefs.Preferences;

import javafx.application.Application;
import javafx.stage.Stage;

import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class VKMusic extends Application{

	final static Preferences userPrefs = Preferences.userNodeForPackage(VKMusic.class);
	
	boolean firstRun = true;
	
	Stage mainStage = null;
	
	final String appID = "5557569";
	final String redirectURI = "https://oauth.vk.com/blank.html";
	final String authScope = "audio";
	final String responseType = "token";
	final String apiVersion = "5.53";
	
	String userID = null;
	String accessToken = null;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		launch(args);
	}
	
	public void start(Stage stage) {
		mainStage = stage;
		
		firstRun = userPrefs.getBoolean("firstRun", true);
		if(firstRun) {
			setUpGUI();
		}
		
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
		//CloseableHttpClient httpClient = HttpClientBuilder.create()
		//			.setRedirectStrategy(new LaxRedirectStrategy()).build();
		HttpGet httpGet = new HttpGet(authURI);
		//CookieStore cookieStore = new BasicCookieStore();
		//HttpContext localContext = new BasicHttpContext();
		//localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
		//BasicClientCookie tmp = new BasicClientCookie(, accessToken);
		CloseableHttpResponse response = null;
		try {
			response = httpClient.execute(httpGet); //, localContext);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(response.toString());
	/*
		HttpEntity respEntity = response.getEntity();
		if(respEntity != null) {
			try {
				System.out.println(EntityUtils.toString(respEntity, "UTF-8"));
			} catch (ParseException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
			System.out.println("NULL");
	*/
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
	//	while (!shell.isDisposed()) {
		//	if (!display.readAndDispatch())
			//	display.sleep();
		//}
	}
	
	private String getUserID(String fromUri) {
		return fromUri
				.substring(fromUri.indexOf("user_id"))
				.split("=")[1];
	}
	private String getAccessToken(String fromUri) {
		return fromUri
				.substring(fromUri.indexOf("access_token"))
				.split("&")[0]
				.split("=")[1];
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
	
	private void setUpGUI() {
		 
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
