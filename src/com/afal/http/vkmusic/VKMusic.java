package com.afal.http.vkmusic;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.prefs.Preferences;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.cookie.Cookie;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.annotations.SerializedName;

public class VKMusic extends Application{

	class Song {
		@SerializedName("title")
		private String name;
		@SerializedName("artist")
		private String artist;
		@SerializedName("url")
		private String url;
		
		Song(String n, String a) {
			name = n; 
			artist = a;
		}
		
		String getArtist() {
			return artist;
		}
		
		String getName() {
			return name;
		}
	}
	
	final static Preferences userPrefs = Preferences.userNodeForPackage(VKMusic.class);
	
	public static void main(String[] args) {
		launch(args);
	}
	
	String userID = null;
	String accessToken = null;
	final String apiVersion = "5.53";
	final String appID = "5557569";
	final String authScope = "audio";
	final String redirectURI = "https://oauth.vk.com/blank.html";
	final String responseType = "token";
	final String userAgent = "Mozilla/5.0";
	
	boolean firstRun = true;
	Stage mainStage = null;
	
	CloseableHttpClient httpClient = HttpClients.createDefault();
	CookieStore cookieStore = new BasicCookieStore();
	
	boolean makeDirForAuthor = false;
	CheckBox dirForAuthorCB = null;
	
	String musicPath = null;
	TextField musicPathField = null;
	
	public void start(Stage stage) {
		mainStage = stage;
		
		firstRun = userPrefs.getBoolean("firstRun", true);
		if(firstRun) {
			setUpGUI();
		}
		
		authVK();
		try {
			parseAndDownload();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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

		CookieHandler.setDefault(new CookieManager());
		String response = getPageContent(authURI);
		sendPost(authURI);
	}

	private String fixWindowsFileName(String pathname) {
		return "";
	}
	
	private String getAccessToken(String fromUri) {
		return fromUri
				.substring(fromUri.indexOf("access_token"))
				.split("&")[0]
				.split("=")[1];
	}

	private String getPageContent(URI uri) {
		String response = null;

		HttpGet httpGet = new HttpGet(uri);

		CloseableHttpResponse httpResponse = null;
		try {
			httpResponse = httpClient.execute(httpGet);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		HttpEntity respEntity = httpResponse.getEntity();
		if(respEntity != null) {
			try {
				response = EntityUtils.toString(respEntity, "UTF-8");
			} catch (ParseException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
			System.out.println("NULL");
		
		return response;
	}

	private String getUserID(String fromUri) {
		return fromUri
				.substring(fromUri.indexOf("user_id"))
				.split("=")[1];
	}
	
	private void openInBrowser(URI uri) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		
		Browser browser = new Browser(shell, SWT.NONE);
		browser.setUrl(uri.toASCIIString());
		browser.addLocationListener(new LocationListener() {
			public void changed(LocationEvent event) {
				if(event.location.contains("/blank.html")) {
					accessToken = getAccessToken(event.location);
					System.out.println(accessToken);
					userID = getUserID(event.location);
					
					BasicClientCookie cookie = new BasicClientCookie("JSESSIONID", 
							Browser.getCookie("JSESSIONID", "oauth.vk.com"));
					cookieStore.addCookie(cookie);
				}
			}
			public void changing(LocationEvent event) {}
		});

		shell.open();
		shell.setFocus();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}
	
	private void parseAndDownload() throws ClientProtocolException, IOException {
		URI getMusicUri = null;
		try {
			getMusicUri = new URIBuilder()
						.setScheme("https")
						.setHost("api.vk.com")
						.setPath("/method/audio.get")
						.setParameter("owner_id", userID)
						.setParameter("need_user", "0")
						.setParameter("count", "6000")
						.setParameter("offset", "0")
						.setParameter("access_token", accessToken)
						.build();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		CloseableHttpResponse response = httpClient.execute(new HttpGet(getMusicUri));
		String strResponse = EntityUtils.toString(response.getEntity());
		//System.out.println(strResponse);
		
		Gson gson = new Gson();
		JsonParser jparser = new JsonParser();
		JsonObject jobj = jparser.parse(strResponse).getAsJsonObject();
		JsonArray jarr = jobj.get("response").getAsJsonArray(); //jparser.parse(strResponse).getAsJsonArray();
		
		System.out.println(jarr.toString());
		
		Integer count = gson.fromJson(jarr.get(0), Integer.class);
		Song song = gson.fromJson(jarr.get(1), Song.class);
		System.out.println(count + " " + song.getArtist() + " " + song.getName());
	}
	
	private void setUpGUI() {
		 VBox mainLayout = new VBox();
		 HBox musicPathLayout = new HBox();
		 musicPathField = new TextField();
		 Button browse = new Button("Browse");
	
		 browse.setOnAction(new EventHandler<ActionEvent>() {
			 public void handle(ActionEvent e) {
				 final DirectoryChooser dirChooser = new DirectoryChooser();
				 final File choice = dirChooser.showDialog(mainStage);
				 if(choice != null) {
					 musicPathField.appendText(choice.getAbsolutePath());
				 }
			 }
		 });
		 musicPathLayout.getChildren().addAll(new Label("Set path to musci folder"), musicPathField, browse);

		 HBox dirForAuthorLayout = new HBox();
		 dirForAuthorCB = new CheckBox();
		 dirForAuthorLayout.getChildren().addAll(dirForAuthorCB, new Label("Create directory for each author"));
	
		 HBox buttonLayout = new HBox();
		 Button okButton = new Button("Ok");
		 okButton.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				musicPath = musicPathField.getText();
				userPrefs.put("musicPath", musicPath);
				makeDirForAuthor = dirForAuthorCB.isArmed();
				userPrefs.putBoolean("makeDirForAuthor", makeDirForAuthor);
				mainStage.hide();
			}
		 });
		 Button cancelButton = new Button("Cancel");
		 cancelButton.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				mainStage.close();
			}
		 });
		 buttonLayout.getChildren().addAll(okButton, cancelButton);
		 
		 mainLayout.getChildren().addAll(musicPathLayout, dirForAuthorLayout, buttonLayout);
		 
		 mainStage.setScene(new Scene(mainLayout));	
		 mainStage.show();
	}
	
	private void sendPost(URI url) { //, List<NameValuePair> postParams) {
		HttpPost post = new HttpPost(url);
		
		post.setHeader("Host", "oauth.vk.com");
		post.setHeader("User-Agent", userAgent);
		post.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		post.setHeader("Accept-Language", "en-US,en;q=0.5");
		post.setHeader("Cookie", cookieStore.getCookies().get(0).toString());//Browser.getCookie("JSESSIONID", "oauth.vk.com"));//url.toASCIIString()));
		post.setHeader("Connection", "keep-alive");
		post.setHeader("Referer", "https://oauth.vk.com/authorize");
		post.setHeader("Content-Type", "application/x-www-form-urlencoded");
		
		CloseableHttpResponse response = null;
		try {
			response = httpClient.execute(post);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		BufferedReader rd = null;
		try {
			rd = new BufferedReader(
			        new InputStreamReader(response.getEntity().getContent()));
		} catch (UnsupportedOperationException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		StringBuffer result = new StringBuffer();
		String line = "";
		try {
			while ((line = rd.readLine()) != null) {
			result.append(line);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(result.toString());
	}
}
