package org.example;

import jakarta.persistence.EntityManagerFactory;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.application.Platform;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.control.ProgressBar;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.example.entity.Album;
import org.example.entity.Artist;
import org.example.entity.DBObject;
import org.example.entity.Playlist;
import org.example.entity.Song;
import org.example.repo.SongRepository;
import org.example.repo.AlbumRepository;
import org.example.repo.ArtistRepository;
import org.example.repo.PlaylistRepository;
import org.example.repo.PlaylistRepositoryImpl;
import org.example.repo.ArtistRepositoryImpl;
import org.example.repo.AlbumRepositoryImpl;
import org.example.repo.SongRepositoryImpl;

import java.util.ArrayList;
import java.util.List;
/**
 * Huvudklassen för applikationen "MyPod".
 * Denna klass bygger upp GUI:t (simulerar en iPod) och hanterar navigering.
 */
public class MyPod extends Application {

    private String currentScreenName = "";
    private Playlist currentActivePlaylist = null;

    // --- DATA-LAGER ---
    // Repositories används för att hämta data från databasen istället för att hårdkoda den.
    private final SongRepository songRepo = new SongRepositoryImpl(PersistenceManager.getEntityManagerFactory());
    private final ArtistRepository artistRepo = new ArtistRepositoryImpl(PersistenceManager.getEntityManagerFactory());
    private final AlbumRepository albumRepo = new AlbumRepositoryImpl(PersistenceManager.getEntityManagerFactory());
    private final PlaylistRepository playlistRepo = new PlaylistRepositoryImpl(PersistenceManager.getEntityManagerFactory());
    private final ItunesApiClient apiClient = new ItunesApiClient();

    // Listor som håller datan vi hämtat från databasen
    private List<Song> songs;
    private List<Artist> artists;
    private List<Album> albums;
    private List<Playlist> playlists;

    // --- MENY-DATA ---
    // Huvudmenyns alternativ. "ObservableList" är en speciell lista i JavaFX
    // som GUI:t kan "lyssna" på, även om vi här mest använder den som en vanlig lista.
    private final ObservableList<String> mainMenu = FXCollections.observableArrayList(
        "Songs", "Artists", "Albums", "Playlists");

    // En lista med själva Label-objekten som visas på skärmen (för att kunna markera/avmarkera dem)
    private final List<ObjectLabel> menuLabels = new ArrayList<>();

    // --- GUI-TILLSTÅND ---
    private int selectedIndex = 0;      // Håller koll på vilket menyval som är markerat just nu
    private VBox screenContent;         // Behållaren för texten/listan inuti "skärmen"
    private StackPane myPodScreen;       // Själva skärm-containern
    private ScrollPane scrollPane;      // Gör att vi kan scrolla om listan är lång
    private boolean isMainMenu = true; // Flagga för att veta om vi är i huvudmenyn eller en undermeny

    private MediaPlayer mediaPlayer;
    private ProgressBar progressBar;
    private ProgressBar volumeBar;
    private PauseTransition volumeHideTimer;

    @Override
    public void start(Stage primaryStage) {
        // --- LAYOUT SETUP ---
        // BorderPane är bra för att placera saker i Top, Bottom, Center, Left, Right.
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20)); // Lite luft runt kanten
        root.getStyleClass().add("ipod-body"); // CSS-klass för själva iPod-kroppen

        // 1. Skapa och placera skärmen högst upp
        myPodScreen = createMyPodScreen();
        root.setTop(myPodScreen);

        // 2. Skapa och placera klickhjulet längst ner
        StackPane clickWheel = createClickWheel();
        root.setBottom(clickWheel);
        BorderPane.setMargin(clickWheel, new Insets(30, 0, 0, 0)); // Marginal ovanför hjulet

        // --- BAKGRUNDSLADDNING ---
        // Vi använder en Task för att ladda databasen. Detta är kritiskt!
        // Om vi laddar databasen direkt i start() fryser hela fönstret tills det är klart.
        Task<Void> initTask = new Task<>() {
            @Override
            protected Void call() {
                initializeData(); // Detta tunga jobb körs i en separat tråd
                return null;
            }
        };

        // När datan är laddad och klar:
        initTask.setOnSucceeded(e -> {
            if (isMainMenu) showMainMenu(); // Rita upp menyn nu när vi har data
        });

        // Om något går fel (t.ex. ingen databasanslutning):
        initTask.setOnFailed(e -> {
            // Platform.runLater måste användas när vi ändrar GUI:t från en annan tråd
            Platform.runLater(() -> {
                Label error = new Label("Failed to load data.");
                error.setStyle("-fx-text-fill: red;");
                screenContent.getChildren().add(error);
            });
        });

        // Starta laddningstråden
        new Thread(initTask).start();

        // --- SCEN OCH CSS ---
        Scene scene = new Scene(root, 300, 500);
        try {
            // Försök ladda CSS-filen för styling
            scene.getStylesheets().add(getClass().getResource("/ipod_style.css").toExternalForm());
        } catch (Exception e) {
            System.out.println("CSS hittades inte, kör utan styling.");
        }

        myPodScreen.setFocusTraversable(true);
        myPodScreen.setOnMouseClicked(e -> myPodScreen.requestFocus());

        // Koppla tangentbordslyssnare för att kunna styra menyn
        setupNavigation(scene);
        showMainMenu(); // Initiera första vyn (tom tills datan laddats klart)

        primaryStage.setTitle("myPod");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    /**
     * Skapar den visuella skärmen (den "lysande" rutan).
     */
    private StackPane createMyPodScreen() {
        StackPane screenContainer = new StackPane();
        screenContainer.getStyleClass().add("ipod-screen");

        double width = 260;
        double height = 195;
        screenContainer.setPrefSize(width, height);
        screenContainer.setMaxSize(width, height);

        // Skapa en "mask" (Rectangle) för att klippa innehållet så hörnen blir rundade
        Rectangle clip = new Rectangle(width, height);
        clip.setArcWidth(15);
        clip.setArcHeight(15);
        screenContainer.setClip(clip);

        scrollPane = new ScrollPane();
        screenContent = new VBox(2); // VBox staplar element vertikalt. 2px mellanrum.
        screenContent.setAlignment(Pos.TOP_LEFT);
        screenContent.setPadding(new Insets(10, 5, 10, 5));

        // Konfigurera scrollbaren så den inte syns men fungerar
        scrollPane.setContent(screenContent);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // Ingen horisontell scroll
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // Ingen synlig vertikal scroll
        scrollPane.setFitToWidth(true); // Innehållet ska fylla bredden
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        screenContainer.getChildren().add(scrollPane);
        return screenContainer;
    }

    /**
     * Skapar klickhjulet med knappar.
     */
    private StackPane createClickWheel() {
        StackPane wheel = new StackPane();
        wheel.setPrefSize(200, 200);


        // Det stora yttre hjulet
        Circle outerWheel = new Circle(100);
        outerWheel.getStyleClass().add("outer-wheel");

        // Den lilla knappen i mitten
        Circle centerButton = new Circle(30);
        centerButton.getStyleClass().add("center-button");

        // Etiketter för knapparna (MENU, Play, Fram, Bak)
        Label menu = new Label("MENU");
        menu.getStyleClass().add("wheel-text-menu");
        // Om man klickar på ordet MENU med musen går man tillbaka
        menu.setOnMouseClicked(e -> showMainMenu());

        Label ff = new Label("⏭");
        ff.getStyleClass().add("wheel-text");
        ff.setId("ff-button");
        Label rew = new Label("⏮");
        rew.getStyleClass().add("wheel-text");
        rew.setId("rew-button");
        Label play = new Label("▶");
        play.getStyleClass().add("wheel-text-play");

        wheel.getChildren().addAll(outerWheel, centerButton, menu, ff, rew, play);
        return wheel;
    }

    /**
     * Hanterar tangentbordsnavigering (Upp, Ner, Enter, Escape).
     */
    private void setupNavigation(Scene scene) {
        scene.setOnKeyPressed(event -> {
            if ("NowPlaying".equals(currentScreenName)) {
                if (event.getCode() == KeyCode.UP) {
                    adjustVolume(0.05);
                    return;
                }
                if (event.getCode() == KeyCode.DOWN) {
                    adjustVolume(-0.05);
                    return;
                }
            }

            if (event.getCode() == KeyCode.ESCAPE) {

                if ("NowPlaying".equals(currentScreenName)) {
                    showMainMenu();
                } else if ("ArtistSongs".equals(currentScreenName)) {
                    showScreen("Artists");
                } else if ("AlbumSongs".equals(currentScreenName)) {
                    showScreen("Albums");
                } else if ("PlaylistSongs".equals(currentScreenName)) {
                    showScreen("Playlists");
                }
                // ESCAPE fungerar som "Back"-knapp
                else {
                    showMainMenu();
                }
                return;
            }

            int totalItems = menuLabels.size();
            if (totalItems == 0) return; // Gör inget om listan är tom

            int newIndex = selectedIndex;

            // Navigera NER
            if (event.getCode() == KeyCode.DOWN) {
                // Modulo (%) gör att om vi trycker ner på sista elementet, hamnar vi på 0 igen.
                newIndex = (selectedIndex + 1) % totalItems;
            }
            // Navigera UPP
            else if (event.getCode() == KeyCode.UP) {
                // Matematisk formel för att loopa bakåt (från 0 till sista)
                newIndex = (selectedIndex - 1 + totalItems) % totalItems;
            }
            // Välj (ENTER)
            else if (event.getCode() == KeyCode.ENTER) {
                if (isMainMenu) {
                    // Om vi är i huvudmenyn, gå in i en undermeny (t.ex. "Songs")
                    showScreen(mainMenu.get(selectedIndex));
                } else {
                    // Om vi är i en lista, hantera valet (spela låt etc)
                    handleSelection(menuLabels.get(selectedIndex));
                }
                return;
            }

            // Om indexet ändrades, uppdatera grafiken
            if (newIndex != selectedIndex) {
                selectedIndex = newIndex;
                updateMenu();
            }
        });
    }

    private void adjustVolume(double delta) {
        if (mediaPlayer == null) return;

        double newVolume = Math.max(0, Math.min(1, mediaPlayer.getVolume() + delta));
        mediaPlayer.setVolume(newVolume);

        volumeBar.setProgress(newVolume);
        showVolumeOverlay();
    }

    private void showVolumeOverlay() {
        if (volumeHideTimer != null) {
            volumeHideTimer.stop();
        }

        FadeTransition fadeIn = new FadeTransition(Duration.millis(150), volumeBar);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        volumeHideTimer = new PauseTransition(Duration.seconds(1.5));
        volumeHideTimer.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), volumeBar);
            fadeOut.setToValue(0.0);
            fadeOut.play();
        });
        volumeHideTimer.play();

    }

    /**
     * Uppdaterar visuellt vilken rad som är vald (lägger till CSS-klass).
     */
    private void updateMenu() {
        for (int i = 0; i < menuLabels.size(); i++) {
            if (i == selectedIndex) {
                menuLabels.get(i).label().getStyleClass().add("selected-item"); // Gör texten markerad
                ensureVisible(menuLabels.get(i).label()); // Se till att scrollbaren flyttas så vi ser valet
            } else {
                menuLabels.get(i).label().getStyleClass().remove("selected-item"); // Ta bort markering
            }
        }
    }

    /**
     * Avancerad metod för att automatiskt scrolla ScrollPane till det markerade elementet.
     */
    private void ensureVisible(Label node) {
        Platform.runLater(() -> {
            double contentHeight = screenContent.getBoundsInLocal().getHeight();
            double viewportHeight = scrollPane.getViewportBounds().getHeight();
            double nodeY = node.getBoundsInParent().getMinY();

            // Om innehållet är högre än skärmen, räkna ut var vi ska scrolla
            if (contentHeight > viewportHeight) {
                // Beräkna positionen (0.0 är toppen, 1.0 är botten)
                double scrollTarget = nodeY / (contentHeight - viewportHeight);
                // Sätt värdet men tvinga det att vara mellan 0 och 1
                scrollPane.setVvalue(Math.min(1.0, Math.max(0.0, scrollTarget)));
            }
        });
    }

    /**
     * Visar en undermeny (Songs, Artists etc).
     */
    private void showScreen(String screenName) {
        screenContent.getChildren().clear(); // Rensa skärmen
        menuLabels.clear();                  // Rensa listan med menyval
        isMainMenu = false;                  // Vi är inte i huvudmenyn längre
        selectedIndex = 0;                   // Återställ markör till toppen
        currentScreenName = screenName;
        // Rubrik
        Label screenTitle = new Label(screenName);
        screenTitle.getStyleClass().add("screen-title");
        screenContent.getChildren().add(screenTitle);

        // Fyll på med rätt data beroende på vad användaren valde
        switch (screenName) {
            case "Songs" -> {
                if (songs != null && !songs.isEmpty()) {
                    songs.forEach(this::addMenuItem);
                } else addMenuItem("No songs found");
            }
            case "Artists" -> {
                if (artists != null && !artists.isEmpty()) {
                    artists.forEach(this::addMenuItem);
                } else addMenuItem("No artists found");
            }
            case "Albums" -> {
                if (albums != null && !albums.isEmpty()) {
                    albums.forEach(this::addMenuItem);
                } else addMenuItem("No albums found");
            }
            case "Playlists" -> {
                addMenuItem("Edit Playlists");
                if (playlists != null && !playlists.isEmpty()) {
                    playlists.forEach(this::addMenuItem);
                } else addMenuItem("No playlists found");
            }
        }
        updateMenu(); // Uppdatera så första valet är markerat
    }

    /**
     * Hjälpmetod för att lägga till en rad i listan på skärmen.
     */
    private void addMenuItem(String text) {
        ObjectLabel stringLabel = new ObjectLabel(new Label(text), null);
        stringLabel.label().getStyleClass().add("menu-item");
        stringLabel.label().setMaxWidth(Double.MAX_VALUE); // Gör att raden fyller hela bredden (snyggare markering)

        if ("Edit Playlists".equals(text)) {
            stringLabel.label().setStyle("-fx-font-weight: bold; -fx-underline: true;");
        }

        menuLabels.add(stringLabel);
        screenContent.getChildren().add(stringLabel.label());
    }

    /**
     * Hjälpmetod för att lägga till en rad i listan på skärmen som pekar på ett object
     */
    private void addMenuItem(DBObject object) {
        ObjectLabel objectLabel = new ObjectLabel(new Label(object.getName()), object);
        objectLabel.label().getStyleClass().add("menu-item");
        objectLabel.label().setMaxWidth(Double.MAX_VALUE); // Gör att raden fyller hela bredden (snyggare markering)

        menuLabels.add(objectLabel);
        screenContent.getChildren().add(objectLabel.label());
    }

    /**
     * Visar huvudmenyn igen.
     */
    private void showMainMenu() {
        screenContent.getChildren().clear();
        menuLabels.clear();
        isMainMenu = true;
        selectedIndex = 0;

        Label title = new Label("myPod");
        title.getStyleClass().add("screen-title");
        screenContent.getChildren().add(title);

        for (String item : mainMenu) {
            addMenuItem(item);
        }
        updateMenu();

    }

    /**
     * Vad som händer när man trycker Enter på en låt/artist.
     */
    private void handleSelection(ObjectLabel selection) {
        // Här kan du lägga till logik för att spela låten eller öppna albumet
        System.out.println("User selected: " + selection.getText());

        if ("Artists".equals(currentScreenName)) {
            showArtistSongs(selection);
        } else if ("Albums".equals(currentScreenName)) {
            showAlbumSongs(selection);
        } else if ("Playlists".equals(currentScreenName)) {
            if ("Edit Playlists".equals(selection.getText())) {
                openMusicPlayer();
                return;
            }

            if (selection.object() == null) {
                return;
            }

            Playlist selectedPlaylist = playlists.stream()
                .filter(p -> p.getId()
                    .equals(selection.object().getId()))
                .findFirst().orElse(null);

            if (selectedPlaylist != null) {
                openPlaylist(selectedPlaylist);
            }
        } else {
            if (selection.getText().startsWith("No ") && selection.getText().endsWith(" found")) {
                return;
            }
            showNowPlaying(selection);
        }
    }

    private void openPlaylist(Playlist p) {
        Playlist updatedPlaylist = playlistRepo.findById(p.getId());

        if (updatedPlaylist == null) {
            showScreen("Playlists");
            return;
        }

        screenContent.getChildren().clear();
        menuLabels.clear();
        selectedIndex = 0;

        currentScreenName = "PlaylistSongs";
        currentActivePlaylist = updatedPlaylist;

        Label title = new Label(updatedPlaylist.getName());
        title.getStyleClass().add("screen-title");
        screenContent.getChildren().add(title);

        if (updatedPlaylist.getSongs() != null && !updatedPlaylist.getSongs().isEmpty()) {
            List<Song> playlistSongs = new ArrayList<>(updatedPlaylist.getSongs());
            for (Song s : playlistSongs) {
                addMenuItem(s);
            }
        } else {
            addMenuItem("No songs found");
        }
        updateMenu();

    }

    /**
     * Öppnar det externa fönstret "ItunesPlayList".
     */
    private void openMusicPlayer() {

        if (this.playlists == null) {
            this.playlists = new ArrayList<>();
        }

        ItunesPlayList itunesPlayList = new ItunesPlayList(playlistRepo);

        itunesPlayList.setOnUpdate(() -> {
            new Thread(() -> {
                try {
                    List<Playlist> updatedPlaylists = playlistRepo.findAll();
                    Platform.runLater(() -> {
                        this.playlists = updatedPlaylists;
                        if ("Playlists".equals(currentScreenName)) {
                            showScreen("Playlists");
                        } else if ("PlaylistSongs".equals(currentScreenName) && currentActivePlaylist != null) {
                            playlists.stream()
                                .filter(p -> p.getId().equals(currentActivePlaylist.getId()))
                                .findFirst()
                                .ifPresent(this::openPlaylist);
                        }
                    });
                } catch (Exception e) {
                    System.err.println("Failed to refresh playlists: " + e.getMessage());
                }
            })
                .start();
        });

        itunesPlayList.showLibrary();
    }

    private void showArtistSongs(ObjectLabel selection) {
        screenContent.getChildren().clear();
        menuLabels.clear();
        selectedIndex = 0;

        currentScreenName = "ArtistSongs";

        Label titleLabel = new Label(selection.getText());
        titleLabel.getStyleClass().add("screen-title");
        screenContent.getChildren().add(titleLabel);

        if (selection.object() == null) {
            addMenuItem("No songs found");
            updateMenu();
            return;
        }

        if (songs != null && !songs.isEmpty()) {
            List<Song> artistSongs = songs.stream()
                .filter(s -> s.getAlbum() != null &&
                    s.getAlbum().getArtist() != null &&
                    s.getAlbum().getArtist().getId().equals(selection.object().getId()))
                .toList();

            if (!artistSongs.isEmpty()) {
                artistSongs.forEach(this::addMenuItem);
            } else {
                addMenuItem("No songs found");
            }
        } else {
            addMenuItem("No songs found");
        }
        updateMenu();
    }

    private void showAlbumSongs(ObjectLabel selection) {
        screenContent.getChildren().clear();
        menuLabels.clear();
        selectedIndex = 0;

        currentScreenName = "AlbumSongs";

        Label titleLabel = new Label(selection.getText());
        titleLabel.getStyleClass().add("screen-title");
        screenContent.getChildren().add(titleLabel);

        if (selection.object() == null) {
            addMenuItem("No songs found");
            updateMenu();
            return;
        }

        if (songs != null && !songs.isEmpty()) {
            List<Song> albumSongs = songs.stream()
                .filter(al -> al.getAlbum() != null &&
                    al.getAlbum().getId().equals(selection.object().getId())).toList();

            if (!albumSongs.isEmpty()) {
                albumSongs.forEach(this::addMenuItem);
            } else {
                addMenuItem("No songs found");
            }
        } else {
            addMenuItem("No songs found");
        }
        updateMenu();
    }

    private void showNowPlaying(ObjectLabel selection) {

        screenContent.getChildren().clear();
        menuLabels.clear();
        selectedIndex = 0;
        currentScreenName = "NowPlaying";

        Song currentSong = null;
        if (songs != null && selection.object() != null) {
            currentSong = songs.stream()
                .filter(s -> s.getId().equals(selection.object().getId()))
                .findFirst()
                .orElse(null);
        }

        // Skapa elementen och tilldela klasser
        Label header = new Label("▶ NOW PLAYING");
        header.getStyleClass().add("now-playing-header");

        ImageView albumArtView = new ImageView();

        if (currentSong != null && currentSong.getAlbum() != null) {
            Image cover = currentSong.getAlbum().getCoverImage();
            if (cover != null) {
                albumArtView.setImage(cover);
            }
        }

        albumArtView.setFitWidth(70);
        albumArtView.setFitHeight(70);
        albumArtView.setPreserveRatio(true);
        albumArtView.setSmooth(true);
        albumArtView.setStyle("""
                -fx-border-color: #ccc;
                -fx-border-width: 1;
                -fx-background-color: white;
            """);

        Label titleLabel = new Label(selection.getText());
        titleLabel.getStyleClass().add("now-playing-title");
        titleLabel.setWrapText(true);

        String artistName;
        if (currentSong != null && currentSong.getAlbum() != null && currentSong.getAlbum().getArtist() != null) {
            artistName = currentSong.getAlbum().getArtist().getName();
        } else {
            artistName = "Unknown Artist";
        }
        Label artistLabel = new Label(artistName);
        artistLabel.getStyleClass().add("now-playing-artist");

        String albumName;
        if (currentSong != null && currentSong.getAlbum() != null) {
            albumName = currentSong.getAlbum().getName();
        } else {
            albumName = "Unknown Album";
        }
        Label albumLabel = new Label(albumName);
        albumLabel.getStyleClass().add("now-playing-album");

        progressBar = new ProgressBar(0);
        progressBar.getStyleClass().add("ipod-progress-bar");

        // Layout-behållaren
        VBox layout = new VBox(3);
        layout.getStyleClass().add("now-playing-container");
        layout.getChildren().addAll(header, albumArtView, titleLabel, artistLabel, albumLabel, progressBar);

        layout.setAlignment(Pos.CENTER);

        // --- Volume overlay ---
        volumeBar = new ProgressBar(mediaPlayer != null ? mediaPlayer.getVolume() : 0.5);
        volumeBar.getStyleClass().add("ipod-volume-bar");
        volumeBar.setOpacity(0); // start hidden
        volumeBar.setPrefWidth(220);

        StackPane volumeOverlay = new StackPane(volumeBar);
        volumeOverlay.setMouseTransparent(true);
        StackPane.setAlignment(volumeOverlay, Pos.BOTTOM_CENTER);
        StackPane.setMargin(volumeOverlay, new Insets(0, 0, 18, 0));


        StackPane nowPlayingStack = new StackPane(layout, volumeOverlay);
        screenContent.getChildren().add(nowPlayingStack);


        if (currentSong != null) {
            String previewUrl = currentSong.getPreviewUrl();
            if (previewUrl != null && !previewUrl.isBlank()) {
                playPreview(previewUrl);
            }
        }
    }

    private void playPreview(String url) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            }

            Media media = new Media(url);
            mediaPlayer = new MediaPlayer(media);

            mediaPlayer.setOnReady(() -> {
                Duration total = mediaPlayer.getTotalDuration();

                progressBar.progressProperty().bind(
                    Bindings.createDoubleBinding(
                        () -> {
                            Duration current = mediaPlayer.getCurrentTime();
                            if (total == null || total.isUnknown() || total.toMillis() == 0) {
                                return 0.0;
                            }
                            return current.toMillis() / total.toMillis();
                        },
                        mediaPlayer.currentTimeProperty()
                    )
                );
            });

            mediaPlayer.setOnEndOfMedia(() -> {
                progressBar.progressProperty().unbind();
                progressBar.setProgress(1.0);
            });

            mediaPlayer.play();
        } catch (Exception e) {
            System.err.println("Could not play preview: " + e.getMessage());
        }
    }


    /**
     * Initierar databasen och hämtar all data.
     * OBS: Denna körs i en bakgrundstråd (via Task i start-metoden).
     */
    private void initializeData() {
        try {
            EntityManagerFactory emf = PersistenceManager.getEntityManagerFactory();
            DatabaseInitializer initializer = new DatabaseInitializer(apiClient, songRepo, albumRepo, artistRepo, playlistRepo);
            initializer.init(); // Fyll databasen om den är tom

            // Hämta data till minnet
            this.songs = songRepo.findAll();
            this.artists = artistRepo.findAll();
            this.albums = albumRepo.findAll();
            this.playlists = playlistRepo.findAll();
        } catch (Exception e) {
            System.err.println("Kunde inte ladda data: " + e.getMessage());
        }
    }

    private record ObjectLabel(
        Label label,
        DBObject object) { // object is null for static menu items like "Edit Playlists"

        public String getText() {
            return label.getText();
        }
    }
}
