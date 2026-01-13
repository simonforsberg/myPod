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
import org.example.entity.Playlist;
import org.example.entity.Song;
import org.example.repo.PlaylistRepository;

import java.util.List;

/**
 * Huvudklass för GUI:t. Hanterar visning av bibliotek, spellistor och sökning.
 */
public class ItunesPlayList {

    private final PlaylistRepository pri;


    /// NY KOD ///
    private Runnable onUpdateCallback;
    public void setOnUpdate(Runnable callback) {
        this.onUpdateCallback = callback;
    }

    private void refresh() {
        if(onUpdateCallback != null) {
            onUpdateCallback.run();
        }
    }
    /// NY KOD SLUT ///

    public ItunesPlayList(PlaylistRepository playlistRepository) {
        this.pri = playlistRepository;
    }

    // --- DATAMODELL ---

    // En lista med alla playlist som finns i databasen
    private ObservableList<Playlist> allPlaylistList = FXCollections.observableArrayList();

    // --- GUI KOMPONENTER ---

    // Tabellen i mitten som visar låtarna
    private TableView<Song> songTable = new TableView<>();

    // Listan till vänster där man väljer spellista
    private ListView<Playlist> sourceList = new ListView<>();

    // Textfält för den "digitala displayen" högst upp
    private Text lcdTitle = new Text("myTunes");
    private Text lcdArtist = new Text("Välj bibliotek eller spellista");

    /**
     * Bygger upp hela gränssnittet och visar fönstret.
     *
     * @param dbPlaylists En lista med playlist hämtade från databasen/backend.
     */
    public void showLibrary(List<Playlist> dbPlaylists) {
        Stage stage = new Stage();

        // Lägg till existerande playlist i vår lokala lista
        allPlaylistList.setAll(dbPlaylists);

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

        sourceList.setItems(allPlaylistList); // Koppla data till listan
        sourceList.getStyleClass().add("source-list");
        sourceList.setPrefWidth(200);

        /// Ändrad Kod ///
        sourceList.setCellFactory(sl -> {
            ListCell<Playlist> cell = new ListCell<>() {
                @Override
                protected void updateItem(Playlist playlist, boolean empty) {
                    super.updateItem(playlist, empty);
                    if (empty || playlist == null) {
                        setText(null);
                        setContextMenu(null); // Ingen meny på tom rad
                    } else {
                        setText(playlist.getName());
                    }
                }
            };

            ContextMenu contextMenu = new ContextMenu();

            MenuItem renameItem = new MenuItem("Byt Namn");
            renameItem.setOnAction(event -> {
                Playlist selected = cell.getItem();
                if (selected != null) {
                    sourceList.getSelectionModel().select(selected);
                    renameSelectedPlaylist();
                }

            });

            MenuItem deleteItem = new MenuItem("Ta Bort");
            deleteItem.setOnAction(event -> {
                Playlist selected = cell.getItem();
                if (selected != null) {
                    sourceList.getSelectionModel().select(selected);
                    deleteSelectedPlaylist();
                }
            });

            contextMenu.getItems().addAll(renameItem, deleteItem);

            cell.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> {
                if (isNowEmpty) {
                    cell.setContextMenu(null);
                } else {
                    cell.setContextMenu(contextMenu);
                }
            });
            return cell;
        });

        //////////////////////////////////////////////////

        // Lyssnare: Vad händer när man klickar på en spellista i menyn?
        sourceList.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                searchField.clear(); // Rensa gammal sökning
                // Hämta låtlistan från vår Map baserat på namnet och visa i tabellen
                ObservableList<Song> songList = FXCollections.observableArrayList(newVal.getSongs().stream().toList());
                songTable.setItems(songList);
            }
        });

        sourceList.getSelectionModel().selectFirst(); // Välj första listan ("Bibliotek") som startvärde

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
        TableColumn<Song, String> titleCol = new TableColumn<>("Namn");
        // Berätta för kolumnen vilket fält i DisplaySong den ska läsa från (name)
        titleCol.setCellValueFactory(d -> {
            Song s = d.getValue();
            if (s.getTitle() != null) {
                return new SimpleStringProperty(s.getTitle());
            }
            return new SimpleStringProperty("Okänd titel");
        });

        TableColumn<Song, String> artistCol = new TableColumn<>("Artist");
        artistCol.setCellValueFactory(d -> {
            Song s = d.getValue();
            if (s.getAlbum() != null && s.getAlbum().getArtist() != null && s.getAlbum().getArtist().getName() != null) {
                return new SimpleStringProperty(s.getAlbum().getArtist().getName());
            }
            return new SimpleStringProperty("Okänd artist");
        });

        TableColumn<Song, String> albumCol = new TableColumn<>("Album");
        albumCol.setCellValueFactory(d -> {
            Song s = d.getValue();
            if (s.getAlbum() != null && s.getAlbum().getName() != null) {
                return new SimpleStringProperty(s.getAlbum().getName());
            }
            return new SimpleStringProperty("Okänt album");
        });

        TableColumn<Song, String> timeCol = new TableColumn<>("Längd");
        timeCol.setCellValueFactory(d -> {
            Song s = d.getValue();
            if (s.getFormattedLength() != null) {
                return new SimpleStringProperty(s.getFormattedLength());
            }
            return new SimpleStringProperty("Okänd längd");
        });

        songTable.getColumns().setAll(titleCol, artistCol, albumCol, timeCol);
        songTable.getStyleClass().add("song-table");

        // Gör så att kolumnerna fyller ut hela bredden
        songTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Lyssnare: När man klickar på en rad i tabellen -> Uppdatera LCD-displayen
        songTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                lcdTitle.setText(newVal.getTitle());
                String artistName = "Okänd artist";
                if (newVal.getAlbum() != null && newVal.getAlbum().getArtist() != null && newVal.getAlbum().getArtist().getName() != null) {
                    artistName = newVal.getAlbum().getArtist().getName();
                }
                lcdArtist.setText(artistName);
            }
        });

        /// NY KOD - Högerklicksfunktion på låtar för att lägga till och ta bort från spellista ////
        songTable.setRowFactory(songTableView -> {
            TableRow<Song> row = new TableRow<>();
            ContextMenu contextMenu = new ContextMenu();

            row.setOnContextMenuRequested(e -> {
                if (!row.isEmpty()) {
                    songTableView.getSelectionModel().select(row.getIndex());
                }
            });

            Menu addSongSubMenu = new Menu("Lägg till i spellistan");
            MenuItem removeSongItem = new MenuItem("Ta bort från Spellistan");

            removeSongItem.setOnAction(e -> {
                removeSelectedSong();
            });

            // VIKTIGT: Uppdatera när hela ContextMenu visas
            contextMenu.setOnShowing(event -> {
                addSongSubMenu.getItems().clear();
                Song selectedSong = row.getItem();

                if (selectedSong != null && !allPlaylistList.isEmpty()) {
                    for (Playlist pl : allPlaylistList) {
                        // Hoppa över biblioteket/huvudlistan om id är 1
                        if (pl.getPlaylistId() != null && pl.getPlaylistId().equals(1L)) continue;

                        MenuItem playListItem = new MenuItem(pl.getName());
                        playListItem.setOnAction(e -> {
                            try {
                                if (!pri.isSongInPlaylist(pl, selectedSong)) {
                                    pri.addSong(pl, selectedSong);
                                    pl.getSongs().add(selectedSong);
                                }
                            } catch (IllegalStateException ex) {
                                new Alert(Alert.AlertType.ERROR, "Kunde inte lägga till låten: " + ex.getMessage()).showAndWait();
                            }
                        });
                        addSongSubMenu.getItems().add(playListItem);
                    }
                }

                if (addSongSubMenu.getItems().isEmpty()) {
                    MenuItem emptyItem = new MenuItem("Inga spellistor tillgängliga");
                    emptyItem.setDisable(true);
                    addSongSubMenu.getItems().add(emptyItem);
                }

                Playlist currentList = sourceList.getSelectionModel().getSelectedItem();
                removeSongItem.setVisible(currentList != null && currentList.getPlaylistId() != null && !currentList.getPlaylistId().equals(1L));
            });

            contextMenu.getItems().addAll(addSongSubMenu, new SeparatorMenuItem(), removeSongItem);

            row.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> {
                if (isNowEmpty) {
                    row.setContextMenu(null);
                } else {
                    row.setContextMenu(contextMenu);
                }
            });

            return row;
        });
    }
    /// ///////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Filtrerar låtarna i den aktiva listan baserat på söktexten.
     */
    private void filterSongs(String searchText) {
        Playlist selectedPlaylist = sourceList.getSelectionModel().getSelectedItem();
        if (selectedPlaylist == null) return;
        Long currentList = selectedPlaylist.getPlaylistId();

        // Hämta originaldatan för den valda spellistan
        ObservableList<Song> masterData = FXCollections.observableArrayList(pri.findById(currentList).getSongs());

        // Om sökfältet är tomt, visa allt
        if (searchText == null || searchText.isEmpty()) {
            songTable.setItems(masterData);
            return;
        }

        // Skapa en filtrerad lista som omsluter masterData
        FilteredList<Song> filteredData = new FilteredList<>(masterData, song -> {
            String filter = searchText.toLowerCase();
            // Returnera true om sökordet finns i namn, artist eller album
            boolean titleMatch = song.getTitle() != null && song.getTitle().toLowerCase().contains(filter);
            boolean artistMatch = song.getAlbum() != null &&
                song.getAlbum().getArtist() != null &&
                song.getAlbum().getArtist().getName() != null &&
                song.getAlbum().getArtist().getName().toLowerCase().contains(filter);
            boolean albumMatch = song.getAlbum() != null &&
                song.getAlbum().getName() != null &&
                song.getAlbum().getName().toLowerCase().contains(filter);
            return titleMatch || artistMatch || albumMatch;
        });

        songTable.setItems(filteredData);
    }

    /**
     * Visar en dialogruta för att skapa en ny spellista.
     */
    private void createNewPlaylist() {
        TextInputDialog d = new TextInputDialog("Ny lista");
        // Här ändrar du fönstrets titel och text
        d.setTitle("Skapa ny spellista");           // Ersätter "Bekräftelse"
        d.setHeaderText("Ange namn på spellista");  // Rubriken inuti rutan
        d.setContentText("Namn:");                  // Texten bredvid inmatningsfältet

        d.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                Playlist pl = pri.createPlaylist(name);
                allPlaylistList.add(pl);
            }
            refresh();
        });
    }

    /**
     * Ändra namn på vald spellista (men tillåter inte att man ändrar "Bibliotek"). ??
     */
    private void renameSelectedPlaylist() {
        Playlist sel = sourceList.getSelectionModel().getSelectedItem();

        if (sel == null || sel.getPlaylistId() == null || sel.getPlaylistId().equals(1L) || sel.getPlaylistId().equals(2L)) {
            return;
        }

        TextInputDialog d = new TextInputDialog("Ändra namn");
        d.setTitle("Byt namn på spellista");
        d.setHeaderText("Ändra namn på spellistan");
        d.setContentText("Nytt namn:");

        d.showAndWait().ifPresent(newName -> {
            if (!newName.trim().isEmpty()) {
                try {
                    pri.renamePlaylist(sel, newName);
                    sel.setName(newName);
                    sourceList.refresh();
                } catch (IllegalStateException ex) {
                    new Alert(Alert.AlertType.ERROR, "Kunde inte byta namn: " + ex.getMessage()).showAndWait();
                }
            }
            refresh();
        });
    }

    /**
     * Tar bort vald spellista (men tillåter inte att man tar bort "Bibliotek" eller "Favoriter").
     */
    private void deleteSelectedPlaylist() {
        Playlist sel = sourceList.getSelectionModel().getSelectedItem();
        if (sel != null && sel.getPlaylistId() != null && !sel.getPlaylistId().equals(1L) && !sel.getPlaylistId().equals(2L)) {
            pri.deletePlaylist(sel);
            allPlaylistList.remove(sel);

            refresh();
        }
    }

    /**
     * Tar bort vald låt från den aktiva spellistan (ej från huvudbiblioteket "Bibliotek").
     */
    private void removeSelectedSong() {
        Song sel = songTable.getSelectionModel().getSelectedItem();
        Playlist list = sourceList.getSelectionModel().getSelectedItem();
        // Skydd: Man får inte ta bort låtar direkt från biblioteket i denna vy
        if (sel != null && list != null && list.getPlaylistId() != null && !list.getPlaylistId().equals(1L)) {
            pri.removeSong(list, sel);
            list.getSongs().remove(sel);
            songTable.getItems().remove(sel);

            refresh();
        }
    }

    /**
     * Visar en popup-meny för att lägga till vald låt i en annan spellista.
     */
    private void addSelectedSong(Button anchor) {
        Song sel = songTable.getSelectionModel().getSelectedItem();
        if (sel == null) return; // Ingen låt vald

        ContextMenu menu = new ContextMenu();
        for (Playlist pl : allPlaylistList) {
            if (pl.getPlaylistId() != null && pl.getPlaylistId().equals(1L))
                continue; // Man kan inte lägga till i "Bibliotek" (det är källan)

            MenuItem itm = new MenuItem(pl.getName());
            itm.setOnAction(e -> {
                // Om låten inte redan finns i listan, lägg till den
                if (!pri.isSongInPlaylist(pl, sel)) {
                    try {
                        pri.addSong(pl, sel);
                        pl.getSongs().add(sel);
                        refresh();

                    } catch (IllegalStateException ex) {
                        new Alert(Alert.AlertType.ERROR, "Could not add song: " + ex.getMessage()).showAndWait();
                    }
                }
            });
            menu.getItems().add(itm);
        }
        // Visa menyn vid knappen
        var bounds = anchor.localToScreen(anchor.getBoundsInLocal());
        menu.show(anchor, bounds.getMinX(), bounds.getMaxY());
    }
}
