package com.afal.http.vkmusic;
import java.io.File;
import java.io.IOException;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class VKMusic extends Application{

	class Song {
		private String name, artist;
		
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
	
	boolean firstRun = true;
	CloseableHttpClient httpClient = HttpClients.createDefault();
	Stage mainStage = null;
	
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

		CookieHandler.setDefault(new CookieManager());
		String responce = getPageContent(authURI);
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
		
		setCookies(httpResponse.getFirstHeader("Set-Cookie") == null ? "" : 
            httpResponse.getFirstHeader("Set-Cookie").toString());

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
				}
			}
			public void changing(LocationEvent event) {}
		});

		shell.open();
		shell.setFocus();
	//	while (!shell.isDisposed()) {
		//	if (!display.readAndDispatch())
			//	display.sleep();
		//}
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
	
}
