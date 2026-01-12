package org.example;

import org.example.entity.Album;
import org.example.entity.Artist;
import org.example.entity.Playlist;
import org.example.entity.Song;
import org.example.repo.*;

import java.util.List;

public class DatabaseInitializer {

    private final ItunesApiClient apiClient;

    private final SongRepository songRepo;
    private final AlbumRepository albumRepo;
    private final ArtistRepository artistRepo;
    private final PlaylistRepository playlistRepo;

    public DatabaseInitializer(ItunesApiClient apiClient, SongRepository songRepo, AlbumRepository albumRepo, ArtistRepository artistRepo, PlaylistRepository playlistRepo) {
        this.apiClient = apiClient;
        this.songRepo = songRepo;
        this.albumRepo = albumRepo;
        this.artistRepo = artistRepo;
        this.playlistRepo = playlistRepo;
    }

    public void init() {
        // Check if there is data already, fill if empty
        if (songRepo.count() == 0) {
            List<String> searches = List.of("the+war+on+drugs",
                "refused",
                "thrice",
                "16+horsepower",
                "viagra+boys",
                "geese",
                "ghost",
                "run+the+jewels",
                "rammstein",
                "salvatore+ganacci",
                "baroness"
            );
            for (String term : searches) {
                try {
                    apiClient.searchSongs(term).forEach(dto -> {
                        Artist ar = Artist.fromDTO(dto);
                        if (!artistRepo.existsByUniqueId(ar)) {
                            artistRepo.save(ar);
                        }

                        Album al = Album.fromDTO(dto, ar);
                        if (!albumRepo.existsByUniqueId(al)) {
                            albumRepo.save(al);
                        }

                        Song s = Song.fromDTO(dto, al);
                        if (!songRepo.existsByUniqueId(s)) {
                            songRepo.save(s);
                        }
                    });
                } catch (Exception e) {
                    throw new RuntimeException("Failed to fetch or persist data for search term: " + term, e);
                }
            }
        }

        if (!playlistRepo.existsByUniqueId(1L)) { // Finns det en playlist borde det vara "Bibliotek"
            Playlist library = playlistRepo.createPlaylist("Bibliotek");
            playlistRepo.addSongs(library, songRepo.findAll());
            //Lägger bara till låtar som fanns innan listan, om fler "laddas ner" behövs de manuellt läggas till
        }
        if (!playlistRepo.existsByUniqueId(2L)) { // Finns det två playlist borde den andra vara "Favoriter"
            playlistRepo.createPlaylist("Favoriter");
        }
    }
}
