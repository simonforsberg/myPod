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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Main application class for {@code MyPod}.
 * <p>
 * This class is responsible for:
 * <ul>
 *     <li>Bootstrapping the JavaFX application</li>
 *     <li>Constructing the iPod-style graphical user interface</li>
 *     <li>Handling keyboard navigation and menu state</li>
 *     <li>Coordinating playback of song previews</li>
 * </ul>
 */
public class MyPod extends Application {
    private static final Logger logger = LoggerFactory.getLogger(MyPod.class);
    private String currentScreenName = "";
    private Playlist currentActivePlaylist = null;

    // -------------------------------------------------------------------------
    // Data layer
    // -------------------------------------------------------------------------

    /**
     * Repositories used for song/artist/album/playlist persistence operations.
     */
    private final SongRepository songRepo = new SongRepositoryImpl(PersistenceManager.getEntityManagerFactory());
    private final ArtistRepository artistRepo = new ArtistRepositoryImpl(PersistenceManager.getEntityManagerFactory());
    private final AlbumRepository albumRepo = new AlbumRepositoryImpl(PersistenceManager.getEntityManagerFactory());
    private final PlaylistRepository playlistRepo = new PlaylistRepositoryImpl(PersistenceManager.getEntityManagerFactory());

    /**
     * Client used to fetch preview data from the iTunes API.
     */
    private final ItunesApiClient apiClient = new ItunesApiClient();

    /**
     * Cached data loaded from the database.
     */
    private List<Song> songs;
    private List<Artist> artists;
    private List<Album> albums;
    private List<Playlist> playlists;

    // -------------------------------------------------------------------------
    // Menu data
    // -------------------------------------------------------------------------

    /**
     * Entries displayed in the main menu.
     * <p>
     * {@link ObservableList} is used so JavaFX can react to changes if needed.
     */
    private final ObservableList<String> mainMenu = FXCollections.observableArrayList(
        "Songs", "Artists", "Albums", "Playlists");

    /**
     * Labels currently rendered on screen.
     * Used to apply selection highlighting and resolve user actions.
     */
    private final List<ObjectLabel> menuLabels = new ArrayList<>();

    // -------------------------------------------------------------------------
    // UI state
    // -------------------------------------------------------------------------

    private int selectedIndex = 0;      // Index of the currently selected menu item
    private VBox screenContent;         // Container holding the current screen content
    private StackPane myPodScreen;      // Root container representing the device screen
    private ScrollPane scrollPane;      // Scroll container used for long lists
    private boolean isMainMenu = true;  // Flag indicating whether the user is currently in the main menu

    private MediaPlayer mediaPlayer;        // Media player instance for song previews
    private ProgressBar progressBar;        // Progress bar showing playback position
    private ProgressBar volumeBar;          // Overlay progress bar used to display volume changes
    private PauseTransition volumeHideTimer;// Timer controlling how long the volume overlay is visible

    // -------------------------------------------------------------------------
    // Application lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void start(Stage primaryStage) {
        // Root layout
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        root.getStyleClass().add("ipod-body");

        // Device screen
        myPodScreen = createMyPodScreen();
        root.setTop(myPodScreen);

        // Click wheel
        StackPane clickWheel = createClickWheel();
        root.setBottom(clickWheel);
        BorderPane.setMargin(clickWheel, new Insets(30, 0, 0, 0));

        // ---------------------------------------------------------------------
        // Background initialization
        // ---------------------------------------------------------------------

        Task<Void> initTask = new Task<>() {
            @Override
            protected Void call() {
                initializeData();
                return null;
            }
        };

        initTask.setOnSucceeded(e -> {
            if (isMainMenu) showMainMenu();
        });

        initTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                Label error = new Label("Failed to load data.");
                error.setStyle("-fx-text-fill: red;");
                screenContent.getChildren().add(error);
            });
        });

        new Thread(initTask).start();

        // ---------------------------------------------------------------------
        // Scene and styling
        // ---------------------------------------------------------------------

        Scene scene = new Scene(root, 300, 500);
        try {
            scene.getStylesheets().add(getClass().getResource("/ipod_style.css").toExternalForm());
        } catch (Exception e) {
            logger.info("Start: CSS not found");
        }

        myPodScreen.setFocusTraversable(true);
        myPodScreen.setOnMouseClicked(e -> myPodScreen.requestFocus());

        setupNavigation(scene);
        showMainMenu();

        primaryStage.setTitle("myPod");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    // -------------------------------------------------------------------------
    // UI construction
    // -------------------------------------------------------------------------

    /**
     * Creates the visual screen area of the device.
     *
     * @return configured {@link StackPane} representing the display
     */
    private StackPane createMyPodScreen() {
        StackPane screenContainer = new StackPane();
        screenContainer.getStyleClass().add("ipod-screen");

        double width = 260;
        double height = 195;
        screenContainer.setPrefSize(width, height);
        screenContainer.setMaxSize(width, height);

        Rectangle clip = new Rectangle(width, height);
        clip.setArcWidth(15);
        clip.setArcHeight(15);
        screenContainer.setClip(clip);

        scrollPane = new ScrollPane();
        screenContent = new VBox(2);
        screenContent.setAlignment(Pos.TOP_LEFT);
        screenContent.setPadding(new Insets(10, 5, 10, 5));

        scrollPane.setContent(screenContent);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        screenContainer.getChildren().add(scrollPane);
        return screenContainer;
    }

    /**
     * Creates the click wheel with menu, navigation and playback controls.
     */
    private StackPane createClickWheel() {
        StackPane wheel = new StackPane();
        wheel.setPrefSize(200, 200);

        Circle outerWheel = new Circle(100);
        outerWheel.getStyleClass().add("outer-wheel");

        Circle centerButton = new Circle(30);
        centerButton.getStyleClass().add("center-button");

        Label menu = new Label("MENU");
        menu.getStyleClass().add("wheel-text-menu");
        menu.setOnMouseClicked(e -> showMainMenu());

        Label ff = new Label("⏭");
        ff.getStyleClass().add("wheel-text");
        ff.setId("ff-button");

        Label rew = new Label("⏮");
        rew.getStyleClass().add("wheel-text");
        rew.setId("rew-button");

        Label playPauseLabel = new Label("▶/⏸");
        playPauseLabel.getStyleClass().add("wheel-text-play");
        playPauseLabel.setFocusTraversable(true);

        playPauseLabel.setOnMouseClicked(e -> playPauseFunction());
        playPauseLabel.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.SPACE) {
                playPauseFunction();
            }
        });

        wheel.getChildren().addAll(
            outerWheel, centerButton, menu, ff, rew, playPauseLabel);

        return wheel;
    }

    /**
     * Toggles playback state of the active {@link MediaPlayer}.
     */
    private void playPauseFunction() {
        if (mediaPlayer != null) {
            if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                mediaPlayer.pause();
            } else {
                mediaPlayer.play();
            }
        }
    }

    // -------------------------------------------------------------------------
    // Navigation & interaction
    // -------------------------------------------------------------------------

    /**
     * Configures keyboard navigation for menus and playback.
     * <p>
     * Supported keys:
     * <ul>
     *     <li>UP / DOWN – navigate menu or adjust volume</li>
     *     <li>ENTER – select item</li>
     *     <li>ESC – navigate back</li>
     * </ul>
     */
    private void setupNavigation(Scene scene) {
        scene.setOnKeyPressed(event -> {
            if ("NowPlaying".equals(currentScreenName) && mediaPlayer != null) {
                if (event.getCode() == KeyCode.UP) {
                    adjustVolume(0.05);
                    event.consume();
                    return;
                }
                if (event.getCode() == KeyCode.DOWN) {
                    adjustVolume(-0.05);
                    event.consume();
                    return;
                }
            }

            if (event.getCode() == KeyCode.ESCAPE) {

                if ("NowPlaying".equals(currentScreenName)) {
                    showMainMenu();
                } else if ("ArtistAlbums".equals(currentScreenName)) {
                    showScreen("Artists");
                } else if ("AlbumSongs".equals(currentScreenName)) {
                    showScreen("Artists");
                } else if ("PlaylistSongs".equals(currentScreenName)) {
                    showScreen("Playlists");
                } else {
                    showMainMenu();
                }
                event.consume();
                return;
            }

            int totalItems = menuLabels.size();
            if (totalItems == 0) return;

            int newIndex = selectedIndex;

            if (event.getCode() == KeyCode.DOWN) {
                newIndex = (selectedIndex + 1) % totalItems; // Modulo (%) gör att om vi trycker ner på sista elementet, hamnar vi på 0 igen.
            } else if (event.getCode() == KeyCode.UP) {
                newIndex = (selectedIndex - 1 + totalItems) % totalItems; // Matematisk formel för att loop'a bakåt (från 0 till sista)
            } else if (event.getCode() == KeyCode.ENTER) {
                if (isMainMenu) {
                    showScreen(mainMenu.get(selectedIndex));
                } else {
                    handleSelection(menuLabels.get(selectedIndex));
                }
                return;
            }

            if (newIndex != selectedIndex) {
                selectedIndex = newIndex;
                updateMenu();
            }
        });
    }

    /**
     * Adjusts playback volume and displays the volume overlay.
     *
     * @param delta positive or negative volume delta
     */
    private void adjustVolume(double delta) {
        if (mediaPlayer == null) return;
        ensureVolumeBarExists();

        double newVolume = Math.max(0, Math.min(1, mediaPlayer.getVolume() + delta));
        mediaPlayer.setVolume(newVolume);
        volumeBar.setProgress(newVolume);
        showVolumeOverlay();
    }

    /**
     * Displays the volume overlay temporarily using fade animations.
     */
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
     * Updates menu selection styling and ensures the selected item is visible.
     */
    private void updateMenu() {
        for (int i = 0; i < menuLabels.size(); i++) {
            if (i == selectedIndex) {
                menuLabels.get(i).label().getStyleClass().add("selected-item"); // Gör texten markerad
                ensureVisible(menuLabels.get(i).label());                       // Se till att scrollbar flyttas så vi ser valet
            } else {
                menuLabels.get(i).label().getStyleClass().remove("selected-item"); // Ta bort markering
            }
        }
    }

    /**
     * Automatically scrolls the {@link ScrollPane} so the given label is visible.
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
     * Displays a secondary screen such as Songs, Artists, Albums or Playlists.
     * <p>
     * This method clears the current screen, resets navigation state and
     * populates the view based on the selected screen name.
     *
     * @param screenName the identifier of the screen to display
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

        // Fyll på med rätt data beroende på användarens val
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
     * Adds a static text-based menu entry to the current screen.
     *
     * @param text the text to display in the menu
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
     * Adds a menu entry that represents a domain object.
     * <p>
     * The object's name is displayed and the object itself is stored
     * for later selection handling.
     *
     * @param object the domain object associated with this menu entry
     */
    private void addMenuItem(DBObject object) {
        ObjectLabel objectLabel = new ObjectLabel(new Label(object.getName()), object);
        objectLabel.label().getStyleClass().add("menu-item");
        objectLabel.label().setMaxWidth(Double.MAX_VALUE); // Gör att raden fyller hela bredden (snyggare markering)

        menuLabels.add(objectLabel);
        screenContent.getChildren().add(objectLabel.label());
    }

    /**
     * Displays the main menu and resets all navigation state.
     */
    private void showMainMenu() {
        screenContent.getChildren().clear();
        menuLabels.clear();
        isMainMenu = true;
        selectedIndex = 0;
        currentScreenName = "";

        Label title = new Label("myPod");
        title.getStyleClass().add("screen-title");
        screenContent.getChildren().add(title);

        for (String item : mainMenu) {
            addMenuItem(item);
        }

        updateMenu();
    }

    /**
     * Handles user selection when the ENTER key is pressed.
     * <p>
     * Behavior depends on the currently active screen and the selected item.
     *
     * @param selection the selected menu entry
     */
    private void handleSelection(ObjectLabel selection) {
        System.out.println("User selected: " + selection.getText());

        if ("Artists".equals(currentScreenName)) {
            showArtistAlbums(selection);
        } else if ("ArtistAlbums".equals(currentScreenName)) {
            showAlbumSongs(selection);
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

    /**
     * Opens a playlist and displays its contained songs.
     *
     * @param p the playlist to open
     */
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
     * Opens the external playlist management window.
     * <p>
     * When playlists are modified, the current view is refreshed accordingly.
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
                    logger.error("openMusicPlayer: Failed to refresh playlists", e);
                }
            })
                .start();
        });

        itunesPlayList.showLibrary();
    }

    /**
     * Displays all albums belonging to the selected artist.
     *
     * @param selection the selected artist entry
     */
    private void showArtistAlbums(ObjectLabel selection) {
        screenContent.getChildren().clear();
        menuLabels.clear();
        selectedIndex = 0;
        currentScreenName = "ArtistAlbums";

        Label titleLabel = new Label(selection.getText());
        titleLabel.getStyleClass().add("screen-title");
        screenContent.getChildren().add(titleLabel);

        if (selection.object() == null) {
            addMenuItem("No albums found");
            updateMenu();
            return;
        }

        Artist artist = (Artist) selection.object();
        List<Album> artistAlbums = albumRepo.findByArtist(artist);

        if (!artistAlbums.isEmpty()) {
            artistAlbums.forEach(this::addMenuItem);
        } else {
            addMenuItem("No albums found");
        }

        updateMenu();
    }

    /**
     * Displays all songs belonging to the selected album.
     *
     * @param selection the selected album entry
     */
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

        Album album = (Album) selection.object();
        List<Song> albumSongs = songRepo.findByAlbum(album);

        if (!albumSongs.isEmpty()) {
            albumSongs.forEach(this::addMenuItem);
        } else {
            addMenuItem("No songs found");
        }

        updateMenu();
    }

    /**
     * Displays the "Now Playing" screen and starts playback of a song preview.
     *
     * @param selection the selected song entry
     */
    private void showNowPlaying(ObjectLabel selection) {
        screenContent.getChildren().clear();
        menuLabels.clear();
        selectedIndex = 0;
        currentScreenName = "NowPlaying";

        if (selection.object() == null) {
            return;
        }

        Song currentSong = (Song) selection.object();

        // Header
        Label header = new Label("▶ NOW PLAYING");
        header.getStyleClass().add("now-playing-header");

        // Album art
        ImageView albumArtView = new ImageView();
        if (currentSong.getAlbum() != null) {
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

        // Song title
        Label titleLabel = new Label(currentSong.getName());
        titleLabel.getStyleClass().add("now-playing-title");
        titleLabel.setWrapText(true);

        String artistName;
        if (currentSong.getAlbum() != null && currentSong.getAlbum().getArtist() != null) {
            artistName = currentSong.getAlbum().getArtist().getName();
        } else {
            artistName = "Unknown Artist";
        }
        Label artistLabel = new Label(artistName);
        artistLabel.getStyleClass().add("now-playing-artist");

        String albumName;
        if (currentSong.getAlbum() != null) {
            albumName = currentSong.getAlbum().getName();
        } else {
            albumName = "Unknown Album";
        }
        Label albumLabel = new Label(albumName);
        albumLabel.getStyleClass().add("now-playing-album");

        // Progress bar
        progressBar = new ProgressBar(0);
        progressBar.getStyleClass().add("ipod-progress-bar");

        // Volume overlay (positioned on top of progress bar)
        ensureVolumeBarExists();
        volumeBar.setOpacity(0); // start hidden

        // Stack the volume bar on top of progress bar
        StackPane progressStack = new StackPane(progressBar, volumeBar);
        progressStack.setAlignment(Pos.CENTER);

        VBox layout = new VBox(3);
        layout.getStyleClass().add("now-playing-container");
        layout.setAlignment(Pos.CENTER);
        layout.getChildren().addAll(header, albumArtView, titleLabel, artistLabel, albumLabel, progressStack);

        screenContent.getChildren().add(layout);

        // Play preview
        String previewUrl = currentSong.getPreviewUrl();
        if (previewUrl != null && !previewUrl.isBlank()) {
            playPreview(previewUrl);
        }
    }

    /**
     * Plays a song preview from the given URL and binds playback progress
     * to the progress bar.
     *
     * @param url preview stream URL
     */
    private void playPreview(String url) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            }

            Media media = new Media(url);
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setVolume(volumeBar.getProgress());

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
            logger.error("playPreview: Could not play preview: ", e);
        }
    }

    /**
     * Lazily creates the volume overlay progress bar if it does not exist.
     */
    private void ensureVolumeBarExists() {
        if (volumeBar == null) {
            volumeBar = new ProgressBar(0.5); // Standard volume set to 50%
            volumeBar.getStyleClass().add("ipod-volume-bar");
            volumeBar.setOpacity(0); // Hidden by default
        }
    }

    /**
     * Initializes the database and loads all required data into memory.
     * <p>
     * This method is executed on a background thread.
     */
    private void initializeData() {
        try {
            DatabaseInitializer initializer = new DatabaseInitializer(apiClient, songRepo, albumRepo, artistRepo, playlistRepo);
            initializer.init();

            this.songs = songRepo.findAll();
            this.artists = artistRepo.findAll();
            this.albums = albumRepo.findAll();
            this.playlists = playlistRepo.findAll();
        } catch (Exception e) {
            logger.error("initializeData: Failed to load data ", e);
        }
    }

    /**
     * Wrapper record binding a UI label to an optional domain object.
     * <p>
     * Used to distinguish static menu items from selectable entities.
     */
    private record ObjectLabel(
        Label label,
        DBObject object) { // Object is null for static menu items like "Edit Playlists"

        /**
         * @return the text displayed by this menu item
         */
        public String getText() {
            return label.getText();
        }
    }
}
