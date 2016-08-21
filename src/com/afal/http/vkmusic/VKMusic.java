package com.afal.http.vkmusic;
import java.awt.AWTException;
import java.awt.Font;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.StringReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
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
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.annotations.SerializedName;

public class VKMusic extends Application{

	class Song implements Serializable{
		/**
		 * 
		 */
		private static final long serialVersionUID = 152600442802102041L;
		
		@SerializedName("title")
		private String title;
		@SerializedName("artist")
		private String artist;
		@SerializedName("url")
		private String url;
		private String path;
		
		Song(String t, String a) {
			title = t; 
			artist = a;
		}
		
		Song(String t, String a, String p) {
			title = t; 
			artist = a;
			path = p;
		}
		
		Song() {
			title = null;
			artist = null;
			url = null;
			path = null;
		}
		
		String getPath() {
			return path;
		}
		
		String getArtist() {
			return artist;
		}
		
		String getTitle() {
			return title;
		}
		
		String getURL() {
			return url;
		}
		
		void setArtist(String sArtist) {
			artist = sArtist;
		}
		
		void setTitle(String sTitle) {
			title = sTitle;
		}
		
		void setUrl(String sUrl) {
			url = sUrl;
		}
		
		void setPath(String sPath) {
			path = sPath;
		}

		public void fixExtraSpaces() {
			artist = artist.trim();
			title  = title.trim();
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result
					+ ((artist == null) ? 0 : artist.hashCode());
			result = prime * result + ((title == null) ? 0 : title.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof Song)) {
				return false;
			}
			if (this == obj) {
				return true;
			}
			Song other = (Song) obj;
			if (!getOuterType().equals(other.getOuterType())) {
				return false;
			}
			if (artist == null) {
				if (other.artist != null) {
					return false;
				}
			} else if (!artist.equals(other.artist)) {
				return false;
			}
			if (title == null) {
				if (other.title != null) {
					return false;
				}
			} else if (!title.equals(other.title)) {
				return false;
			}
			return true;
		}

		private VKMusic getOuterType() {
			return VKMusic.this;
		}
	}
	
	class VKMusicHandler extends DefaultHandler {
		boolean bArtist = false;
		boolean bTitle = false;
		boolean bUrl = false;
		
		Song song = null;
		
		public void startElement(String uri, String localName, 
				String qName, Attributes attributes) {
			if(qName.equalsIgnoreCase("audio")) {
				song = new Song();
			} else if(qName.equalsIgnoreCase("artist")) {
				bArtist = true;
			} else if (qName.equalsIgnoreCase("title")) {
				bTitle = true;
			} else if (qName.equalsIgnoreCase("url")) {
				bUrl = true;
			}
		}
		
		public void characters(char ch[], int start, int length) {
			if(bArtist) {
				song.setArtist(new String(ch, start, length));
				bArtist = false;
			} else if(bTitle) {
				song.setTitle(new String(ch, start, length));
				bTitle = false;
			} else if(bUrl) {
				song.setUrl(new String(ch, start, length));
				bUrl = false;
			}
		}
		
		public void endElement(String uri, String localName, String qName) {
			if(qName.equalsIgnoreCase("audio")) {
				song.fixExtraSpaces();
				songArr.add(song);
			}
		}
	}
	
	class VKMusicFileVisitor extends SimpleFileVisitor<Path> {
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
			if(file.getFileName().toString().contains(".mp3")){
				String artist = file.getFileName().toString().split(" - ")[0];
				String title  = file.getFileName().toString().split(" - ")[1].replace(".mp3", "");
				String path = file.toAbsolutePath().toString();
				
				localSongArr.add(new Song(title, artist, path));
			}
			return FileVisitResult.CONTINUE;
		}
	}
	
	final static Preferences userPrefs = Preferences.userNodeForPackage(VKMusic.class);
	
	public static void main(String[] args) {
		launch(args);
	}
	
	Thread mainLoop = null;
	
	String userID = null;
	String accessToken = null;
	final String apiVersion = "5.53";
	final String appID = "5557569";
	final String authScope = "audio";
	final String redirectURI = "https://oauth.vk.com/blank.html";
	final String responseType = "token";
	final String userAgent = "Mozilla/5.0";
	final String noValue = "nVal";
	
	boolean firstRun = true;
	Stage mainStage = null;
	Stage guiStage = null;
	
	CloseableHttpClient httpClient = HttpClients.createDefault();

	boolean makeDirForArtist = false;
	CheckBox dirForArtistCB = null;
	
	String musicPath = null;
	TextField musicPathField = null;
	
	ArrayList<Song> songArr = null;
	ArrayList<Song> localSongArr = null;
	
	public void start(Stage stage) {
		mainStage = stage;
		
		Platform.setImplicitExit(false);
		addAppToTray();
		
		firstRun = userPrefs.getBoolean("firstRun", true);
		accessToken = userPrefs.get("accessToken", noValue);
		userID = userPrefs.get("userID", noValue);
		musicPath = userPrefs.get("musicPath", noValue);
		
		if(firstRun) {
			setUpGUI();
			authVK();
			parseFromJson(getMusicJSON());
			downloadAllSongs();
		}

		getLocalMusic();
		
		mainLoop = new Thread() {
			@Override
			public void run() {
				while(true) {
					parseFromJson(getMusicJSON());
					//downloadMissingMusic();
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();	
					}
				}
			}
		};
		mainLoop.start();
	}

	private void downloadMissingMusic() {
		for(Song song : songArr) {
			if(!localSongArr.contains(song))
				downloadSong(song);
				localSongArr.add(song);
		}
	}

	private void getLocalMusic() {
		localSongArr = new ArrayList<Song>();
		try {
			Files.walkFileTree(Paths.get(musicPath), new VKMusicFileVisitor());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void authVK() {
		URI authURI = null;
	
		try {
			authURI = new URIBuilder ()
						 .setScheme("https")
						 .setHost("oauth.vk.com")
						 .setPath("/authorize")
						 .setParameter("client_id", appID)
						 .setParameter("redirect_uri", redirectURI)
						 .setParameter("scope", authScope)
						 .setParameter("response_type", responseType)
						 .setParameter("v", apiVersion)
						 .build();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		openInBrowser(authURI);
	}

	private String fixWindowsFileName(String pathname) {	
		String[] forbiddenSymbols = new String[] {"<", ">", ":", "\"", "/", "\\", "|", "?", "*"}; // для windows
        String result = pathname;
        for (String forbiddenSymbol: forbiddenSymbols) {
            result = StringUtils.replace(result, forbiddenSymbol, "");
        }
        // амперсанд в названиях передаётся как '& amp', приводим его к читаемому виду
        return StringEscapeUtils.unescapeXml(result); 
	}

	private String getAccessToken(String fromUri) {
		return fromUri
				.substring(fromUri.indexOf("access_token"))
				.split("&")[0]
				.split("=")[1];
	}

	private String getUserID(String fromUri) {
		return fromUri
				.substring(fromUri.indexOf("user_id"))
				.split("=")[1];
	}
	/*
	private void openInBrowser(URI uri) {
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		
		Browser browser = new Browser(shell, SWT.NONE);
		browser.setUrl(uri.toASCIIString());
		browser.addLocationListener(new LocationListener() {
			public void changed(LocationEvent event) {
				System.out.println("Changed: " + event.location);
				if(event.location.contains("access_token")) {
					accessToken = getAccessToken(event.location);
					System.out.println(accessToken);
					userID = getUserID(event.location);
					
					//BasicClientCookie cookie = new BasicClientCookie("JSESSIONID", 
					//		Browser.getCookie("JSESSIONID", "oauth.vk.com"));
					//cookieStore.addCookie(cookie);
				}
			}
			public void changing(LocationEvent event) {
				System.out.println("Changing: " + event.location);
				if(event.location.contains("access_token")) {
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
	*/
	
	private void openInBrowser(URI uri) {
		WebView browser = new WebView();
		WebEngine eng = browser.getEngine();
		Stage tmpStage = new Stage();
		
		eng.getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {
			@Override
			public void changed(ObservableValue<? extends State> ov, State oldState, State newState) {
				if(newState == State.SUCCEEDED) {
					System.out.println(eng.getLocation());
					if(eng.getLocation().contains("access_token")) {
						accessToken = getAccessToken(eng.getLocation());
						userPrefs.put("accessToken", accessToken);
						//System.out.println(accessToken);
						
						userID = getUserID(eng.getLocation());
						userPrefs.put("userID", userID);
						
						tmpStage.close();
					}
				}
			}
		});
		
		tmpStage.setScene(new Scene(browser));
		eng.load(uri.toASCIIString());
		tmpStage.showAndWait();
	}
	
	private void parseAndDownloadXML() {
		if(accessToken == null) {
			Display display = Display.getDefault();
			Shell shell = new Shell(display);
			shell.setLayout(new FillLayout());
			MessageBox msgBox = new MessageBox(shell);
		}
		URI getMusicUri = null;
		
		try {
			getMusicUri = new URIBuilder()
							.setScheme("https")
							.setHost("api.vk.com")
							.setPath("/method/audio.get.xml")
							.setParameter("owner_id", userID)
							.setParameter("need_user", "0")
							.setParameter("count", "6000")
							.setParameter("offset", "0")
							.setParameter("access_token", accessToken)
							.build();
		} catch (URISyntaxException e) {
			throw new RuntimeException("This should never happen, because URI is hardcoded", e);
		}

		System.out.println(getMusicUri.toASCIIString());
		CloseableHttpResponse response = null;
		try {
			response = httpClient.execute(new HttpGet(getMusicUri));
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String strResponse = null;
		try {
			strResponse = EntityUtils.toString(response.getEntity());
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(strResponse);
		
		songArr = new ArrayList<Song>();
		SAXParser saxParser = null;
		try {
			saxParser = SAXParserFactory.newInstance().newSAXParser();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			saxParser.parse(new InputSource(new StringReader(strResponse)), new VKMusicHandler());
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		boolean result = false;
		File musicDir = new File(musicPath + "\\VKMusic");	
		System.out.println(musicDir.canWrite());
		System.out.println(new File(musicPath).canWrite());
		if(!musicDir.exists()) {
			result = musicDir.mkdirs();
			//Files.createDirectories(musicDir.toPath());
		}
		//for(Song song : songArr)
			//downloadSong(song);
	}
	
	private void parseFromJson(JsonElement musicJson) {
		Gson gson = new Gson();
		JsonArray jarr = musicJson.getAsJsonObject().get("items").getAsJsonArray();
		Integer count = jarr.size();
		
		songArr = new ArrayList<Song>();
		for(int i = 0; i < count; i++) {
			jarr.get(i);
			Song song = gson.fromJson(jarr.get(i), Song.class);
			song.fixExtraSpaces();
			songArr.add(song);
		}
	}
	
	private JsonElement getMusicJSON() {
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
						.setParameter("v", apiVersion)
						.build();
		} catch (URISyntaxException e) {
			throw new RuntimeException("This should never happen, because URI is hardcoded", e);
		}
		
		Gson gson = new Gson();
		JsonParser jparser = new JsonParser();
		String strResponse = null;
		
		strResponse = execRequest(getMusicUri);
		if(jparser.parse(strResponse).getAsJsonObject().get("error") != null)
			authVK();
		
		strResponse = execRequest(getMusicUri);
		
		return jparser.parse(strResponse).getAsJsonObject().get("response");
	}
	
	private String execRequest(URI uri) {
		CloseableHttpResponse response = null;
		try {
			response = httpClient.execute(new HttpGet(uri));
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		
		String sResponse = null;
		try {
			sResponse = EntityUtils.toString(response.getEntity());
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return sResponse;
	}
	
	private void downloadAllSongs() {
		File musicDir = new File(musicPath);
		if(!musicDir.exists())
			musicDir.mkdirs();
		/*
		for(Song song : songArr) {
			downloadSong(song);
		}
		*/
		downloadSong(songArr.get(0));
	}
	
	private void downloadSong(Song song) {
		String path = musicPath;
		if(makeDirForArtist) {
			path += "\\" + fixWindowsFileName(song.getArtist());
			try {
				Files.createDirectory(Paths.get(path));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		path += "\\" + fixWindowsFileName(song.getTitle() + " - " + song.getArtist());
		
		File dest = new File(path + ".mp3");
		if(!dest.exists()) {
			try {
				FileUtils.copyURLToFile(new URL(song.getURL()), dest);
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			song.setPath(dest.getAbsolutePath());
		}
	}
	
	private void showGUI() {
		if(guiStage != null){
			guiStage.showAndWait();
			//guiStage.toFront();
		}
		else
			setUpGUI();
	}
	
	private void setUpGUI() {
		 guiStage = new Stage();
		 guiStage.setTitle("VKMusic");
		 guiStage.setOnCloseRequest(new EventHandler<WindowEvent>() {

			@Override
			public void handle(WindowEvent arg0) {
				Platform.exit();
				System.exit(0);
			}
		 });
		 
		 VBox mainLayout = new VBox();
		 
		 HBox musicPathLayout = new HBox();
		 musicPathField = new TextField("C:\\Program Files");
		 Button browse = new Button("Browse");
		
		 browse.setOnAction(new EventHandler<ActionEvent>() {
			 public void handle(ActionEvent e) {
				 final DirectoryChooser dirChooser = new DirectoryChooser();
				 final File choice = dirChooser.showDialog(mainStage);
				 if(choice != null) {
					 musicPathField.clear();
					 musicPathField.appendText(choice.getAbsolutePath());
				 }
			 }
		 });
		 musicPathLayout.getChildren().addAll(new Label("Set path to musci folder"), musicPathField, browse);

		 HBox dirForArtistLayout = new HBox();
		 dirForArtistCB = new CheckBox();
		 dirForArtistLayout.getChildren().addAll(dirForArtistCB, new Label("Create directory for each author"));
	
		 HBox buttonLayout = new HBox();
		 Button okButton = new Button("Ok");
		 okButton.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				musicPath = musicPathField.getText() + "\\VKMusic";
				userPrefs.put("musicPath", musicPath);
				makeDirForArtist = dirForArtistCB.isSelected();
				userPrefs.putBoolean("makeDirForArtist", makeDirForArtist);
				guiStage.hide();
			}
		 });
		 Button cancelButton = new Button("Cancel");
		 cancelButton.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				Platform.exit();
				System.exit(0);
			}
		 });
		 buttonLayout.getChildren().addAll(okButton, cancelButton);
		 
		 mainLayout.getChildren().addAll(musicPathLayout, dirForArtistLayout, buttonLayout);
		 
		 guiStage.setScene(new Scene(mainLayout));	
		 guiStage.showAndWait();
	}

	private void addAppToTray() {
	    // ensure awt toolkit is initialized.
	    Toolkit.getDefaultToolkit();
	
	    // app requires system tray support, just exit if there is no support.
	    if (!SystemTray.isSupported()) {
	        System.out.println("No system tray support, application exiting.");
	        Platform.exit();
	    }
	
	    // set up a system tray icon.
	    SystemTray tray = SystemTray.getSystemTray();
	    URL imageLoc = null;
		try {
			imageLoc = new URL("https://cdn3.iconfinder.com/data/icons/social-media-chat-1/512/VK-128.png");
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	
	    Image image = null;
		try {
			image = ImageIO.read(imageLoc);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	    TrayIcon trayIcon = new java.awt.TrayIcon(image);
	
	    // if the user double-clicks on the tray icon, show the main app stage.
	    trayIcon.addActionListener(event -> Platform.runLater(this::showGUI));
	
	    MenuItem openItem = new java.awt.MenuItem("Open");
	    openItem.addActionListener(event -> Platform.runLater(this::showGUI));
	
	    // the convention for tray icons seems to be to set the default icon for opening
	    // the application stage in a bold font.
	    Font defaultFont = Font.decode(null);
	    Font boldFont = defaultFont.deriveFont(Font.BOLD);
	    openItem.setFont(boldFont);
	    // to really exit the application, the user must go to the system tray icon
	    // and select the exit option, this will shutdown JavaFX and remove the
	    // tray icon (removing the tray icon will also shut down AWT).
	    MenuItem exitItem = new MenuItem("Exit");
	    exitItem.addActionListener(new ActionListener() {
	
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				Platform.exit();
	            tray.remove(trayIcon);
	            System.exit(0);
			}
	    });
	
	    // setup the popup menu for the application.
	    final java.awt.PopupMenu popup = new java.awt.PopupMenu();
	    popup.add(openItem);
	    popup.addSeparator();
	    popup.add(exitItem);
	    trayIcon.setPopupMenu(popup);
	
	    // add the application tray icon to the system tray.
	    try {
			tray.add(trayIcon);
		} catch (AWTException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    }

}
