package org.example;


import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Huvudklass för GUI:t. Hanterar visning av bibliotek, spellistor och sökning.
 */
public class ItunesPlayList {

    // --- DATAMODELLER ---

    // En Map som lagrar alla spellistor. Nyckeln är namnet (t.ex. "Musik") och värdet är listan med låtar.
    private Map<String, ObservableList<DisplaySong>> allPlaylists = new HashMap<>();

    // Listan med namn på spellistor som visas i vänstermenyn (Sidebar).
    // "ObservableList" gör att GUI:t uppdateras automatiskt om vi lägger till/tar bort namn här.
    private ObservableList<String> playlistNames = FXCollections.observableArrayList();

    // --- GUI KOMPONENTER ---

    // Tabellen i mitten som visar låtarna
    private TableView<DisplaySong> songTable = new TableView<>();

    // Listan till vänster där man väljer spellista
    private ListView<String> sourceList = new ListView<>();

    // Textfält för den "digitala displayen" högst upp
    private Text lcdTitle = new Text("myTunes");
    private Text lcdArtist = new Text("Välj bibliotek eller spellista");

    /**
     * Bygger upp hela gränssnittet och visar fönstret.
     * @param dbSongs En lista med låtar hämtade från databasen/backend.
     */
    public void showLibrary(List<org.example.entity.Song> dbSongs) {
        Stage stage = new Stage();

        // Konvertera databas-objekten till vår interna DisplaySong-klass och skapa grundlistorna
        initData(dbSongs);

        // BorderPane är huvudlayouten: Top, Left, Center, Bottom
        BorderPane root = new BorderPane();

        // ---------------------------------------------------------
        // 1. TOPPEN (Knappar, LCD-display, Sökfält)
        // ---------------------------------------------------------
        HBox topPanel = new HBox(15); // HBox lägger saker på rad horisontellt
        topPanel.getStyleClass().add("top-panel"); // CSS-klass för styling
        topPanel.setPadding(new Insets(10, 15, 10, 15));
        topPanel.setAlignment(Pos.CENTER_LEFT);

        // Skapa LCD-displayen (den blå rutan med text)
        StackPane lcdDisplay = createLCDDisplay();
        // Säg åt displayen att växa och ta upp ledig plats i bredd
        HBox.setHgrow(lcdDisplay, Priority.ALWAYS);

        // Sökfältet
        TextField searchField = new TextField();
        searchField.setPromptText("Sök...");
        searchField.getStyleClass().add("itunes-search");

        // Lyssnare: När texten ändras i sökfältet, kör metoden filterSongs()
        searchField.textProperty().addListener((obs, old, newVal) -> filterSongs(newVal));

        // Lägg till allt i toppen
        topPanel.getChildren().addAll(
            createRoundButton("⏮"), createRoundButton("▶"), createRoundButton("⏭"),
            lcdDisplay, searchField
        );

        // ---------------------------------------------------------
        // 2. VÄNSTER (Spellistorna)
        // ---------------------------------------------------------
        sourceList.setItems(playlistNames); // Koppla data till listan
        sourceList.getStyleClass().add("source-list");
        sourceList.setPrefWidth(200);

        // Lyssnare: Vad händer när man klickar på en spellista i menyn?
        sourceList.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                searchField.clear(); // Rensa gammal sökning
                // Hämta låtlistan från vår Map baserat på namnet och visa i tabellen
                songTable.setItems(allPlaylists.get(newVal));
            }
        });
        sourceList.getSelectionModel().selectFirst(); // Välj första listan ("Musik") som startvärde

        // ---------------------------------------------------------
        // 3. MITTEN (Låttabellen)
        // ---------------------------------------------------------
        setupTable(); // Konfigurerar kolumner och beteende för tabellen

        // ---------------------------------------------------------
        // 4. BOTTEN (Knappar för att hantera listor)
        // ---------------------------------------------------------
        HBox bottomPanel = new HBox(10);
        bottomPanel.setPadding(new Insets(10));
        bottomPanel.getStyleClass().add("bottom-panel");

        Button btnAddList = new Button("+");
        btnAddList.getStyleClass().add("list-control-button");
        Button btnDeleteList = new Button("-");
        btnDeleteList.getStyleClass().add("list-control-button");
        Button btnMoveToPlaylist = new Button("Lägg till Låt i spellista");
        Button btnRemoveSong = new Button("Ta bort låt från lista");

        // Koppla knapparna till metoder
        btnAddList.setOnAction(e -> createNewPlaylist());
        btnDeleteList.setOnAction(e -> deleteSelectedPlaylist());
        btnRemoveSong.setOnAction(e -> removeSelectedSong());
        btnMoveToPlaylist.setOnAction(e -> addSelectedSong(btnMoveToPlaylist));

        bottomPanel.getChildren().addAll(btnAddList, btnDeleteList, new Separator(), btnMoveToPlaylist, btnRemoveSong);

        // ---------------------------------------------------------
        // SLUTMONTERING
        // ---------------------------------------------------------

        // SplitPane gör att användaren kan dra i gränsen mellan vänstermeny och tabell
        SplitPane splitPane = new SplitPane(sourceList, songTable);
        splitPane.setDividerPositions(0.25); // Sätt startposition för avdelaren

        root.setTop(topPanel);
        root.setCenter(splitPane);
        root.setBottom(bottomPanel);

        Scene scene = new Scene(root, 950, 600);
        // Ladda CSS-filen (måste ligga i resources-mappen)
        scene.getStylesheets().add(getClass().getResource("/ipod_style.css").toExternalForm());

        stage.setScene(scene);
        stage.setTitle("myTunes");
        stage.show();
    }

    /**
     * Hjälpmetod för att skapa LCD-displayen (bakgrund + text).
     */
    private StackPane createLCDDisplay() {
        StackPane stack = new StackPane();
        Rectangle bg = new Rectangle(350, 45);
        bg.getStyleClass().add("lcd-background");

        VBox textStack = new VBox(2);
        textStack.setAlignment(Pos.CENTER);
        lcdTitle.getStyleClass().add("lcd-title");
        lcdArtist.getStyleClass().add("lcd-artist");

        textStack.getChildren().addAll(lcdTitle, lcdArtist);
        stack.getChildren().addAll(bg, textStack);
        return stack;
    }

    /**
     * Hjälpmetod för att skapa en standardiserad knapp.
     */
    private Button createRoundButton(String icon) {
        Button b = new Button(icon);
        b.getStyleClass().add("itunes-button");
        return b;
    }

    /**
     * Konfigurerar kolumnerna i tabellen och hur data ska visas.
     */
    private void setupTable() {
        // Skapa kolumner
        TableColumn<DisplaySong, String> titleCol = new TableColumn<>("Namn");
        // Berätta för kolumnen vilket fält i DisplaySong den ska läsa från (name)
        titleCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().name));

        TableColumn<DisplaySong, String> artistCol = new TableColumn<>("Artist");
        artistCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().artist));

        TableColumn<DisplaySong, String> albumCol = new TableColumn<>("Album");
        albumCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().album));

        TableColumn<DisplaySong, String> timeCol = new TableColumn<>("Längd");
        timeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().time));

        songTable.getColumns().setAll(titleCol, artistCol, albumCol, timeCol);
        songTable.getStyleClass().add("song-table");

        // Gör så att kolumnerna fyller ut hela bredden
        songTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Lyssnare: När man klickar på en rad i tabellen -> Uppdatera LCD-displayen
        songTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                lcdTitle.setText(newVal.name);
                lcdArtist.setText(newVal.artist);
            }
        });
    }

    /**
     * Filtrerar låtarna i den aktiva listan baserat på söktexten.
     */
    private void filterSongs(String searchText) {
        String currentList = sourceList.getSelectionModel().getSelectedItem();
        if (currentList == null) return;

        // Hämta originaldatan för den valda spellistan
        ObservableList<DisplaySong> masterData = allPlaylists.get(currentList);

        // Om sökfältet är tomt, visa allt
        if (searchText == null || searchText.isEmpty()) {
            songTable.setItems(masterData);
            return;
        }

        // Skapa en filtrerad lista som omsluter masterData
        FilteredList<DisplaySong> filteredData = new FilteredList<>(masterData, song -> {
            String filter = searchText.toLowerCase();
            // Returnera true om sökordet finns i namn, artist eller album
            return song.name.toLowerCase().contains(filter) ||
                song.artist.toLowerCase().contains(filter) ||
                song.album.toLowerCase().contains(filter);
        });

        songTable.setItems(filteredData);
    }

    /**
     * Omvandlar databas-objekten till GUI-objekt och skapar standardlistor.
     */
    private void initData(List<org.example.entity.Song> dbSongs) {
        ObservableList<DisplaySong> library = FXCollections.observableArrayList();

        // Loopa igenom datan från databasen
        if (dbSongs != null) {
            for (org.example.entity.Song s : dbSongs) {
                // Hantera null-värden snyggt (om artist eller album saknas)
                String art = (s.getAlbum() != null && s.getAlbum().getArtist() != null) ? s.getAlbum().getArtist().getName() : "Okänd";
                String alb = (s.getAlbum() != null) ? s.getAlbum().getName() : "Okänt";

                // Skapa ett nytt DisplaySong-objekt
                library.add(new DisplaySong(s.getTitle(), art, alb, s.getLength()));
            }
        }

        // Lägg in huvudbiblioteket "Musik"
        allPlaylists.put("Musik", library);
        playlistNames.add("Musik");

        // Skapa en tom lista för "Favoriter"
        allPlaylists.put("Favoriter", FXCollections.observableArrayList());
        playlistNames.add("Favoriter");
    }

    /**
     * Visar en dialogruta för att skapa en ny spellista.
     */
    private void createNewPlaylist() {
        TextInputDialog d = new TextInputDialog("Ny lista");

        // Här ändrar du fönstrets titel och text
        d.setTitle("Skapa ny spellista");        // Ersätter "Bekräftelse"
        d.setHeaderText("Ange namn på din nya lista"); // Rubriken inuti rutan
        d.setContentText("Namn:");               // Texten bredvid inmatningsfältet

        d.showAndWait().ifPresent(name -> {
            // Kontrollera att namnet inte är tomt och inte redan finns
            if (!name.trim().isEmpty() && !allPlaylists.containsKey(name)) {
                allPlaylists.put(name, FXCollections.observableArrayList());
                playlistNames.add(name);
            }
        });
    }

    /**
     * Tar bort vald spellista (men tillåter inte att man tar bort "Musik").
     */
    private void deleteSelectedPlaylist() {
        String sel = sourceList.getSelectionModel().getSelectedItem();
        if (sel != null && !sel.equals("Musik")) {
            allPlaylists.remove(sel);
            playlistNames.remove(sel);
        }
    }

    /**
     * Tar bort vald låt från den aktiva spellistan (ej från huvudbiblioteket "Musik").
     */
    private void removeSelectedSong() {
        DisplaySong sel = songTable.getSelectionModel().getSelectedItem();
        String list = sourceList.getSelectionModel().getSelectedItem();
        // Skydd: Man får inte ta bort låtar direkt från "Musik"-biblioteket i denna vy
        if (sel != null && list != null && !list.equals("Musik")) {
            allPlaylists.get(list).remove(sel);
        }
    }

    /**
     * Visar en popup-meny för att lägga till vald låt i en annan spellista.
     */
    private void addSelectedSong(Button anchor) {
        DisplaySong sel = songTable.getSelectionModel().getSelectedItem();
        if (sel == null) return; // Ingen låt vald

        ContextMenu menu = new ContextMenu();
        for (String n : playlistNames) {
            if (n.equals("Musik")) continue; // Man kan inte lägga till i "Musik" (det är källan)

            MenuItem itm = new MenuItem(n);
            itm.setOnAction(e -> {
                // Om låten inte redan finns i listan, lägg till den
                if (!allPlaylists.get(n).contains(sel)) {
                    allPlaylists.get(n).add(sel);
                }
            });
            menu.getItems().add(itm);
        }
        // Visa menyn vid knappen
        menu.show(anchor, anchor.getScene().getWindow().getX() + anchor.getLayoutX(), anchor.getScene().getWindow().getY() + anchor.getLayoutY());
    }

    /**
     * En inre klass (DTO - Data Transfer Object) enbart för visning i tabellen.
     * Detta skiljer GUI-logiken från databas-entiteterna.
     */
    public static class DisplaySong {
        String name, artist, album, time;

        public DisplaySong(String n, String a, String al, Long t) {
            this.name = n;
            this.artist = a;
            this.album = al;
            this.time = formatTime(t);
        }

        // Metod för att konvertera och skriva ut millisekunder som MM:SS
        private String formatTime(Long millis) {
            if (millis == null) return "0:00";

            long seconds = millis / 1000;
            long minutes = seconds / 60;
            long remainingSeconds = seconds % 60;

            return String.format("%d:%02d", minutes, remainingSeconds);
        }
    }
}
