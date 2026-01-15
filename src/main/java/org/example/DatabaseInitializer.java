package org.example;

import org.example.entity.Album;
import org.example.entity.Artist;
import org.example.entity.Playlist;
import org.example.entity.Song;
import org.example.repo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Initializes and populates the application database.
 *
 * <p>This class is responsible for:</p>
 * <ul>
 *   <li>Fetching music data from the iTunes Search API</li>
 *   <li>Persisting artists, albums, and songs using repository abstractions</li>
 *   <li>Ensuring required default playlists exist</li>
 * </ul>
 *
 * <p>The initializer is designed to be idempotent: data is only inserted
 * if the database is empty or missing required entities.</p>
 */
public class DatabaseInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final ItunesApiClient apiClient;

    private final SongRepository songRepo;
    private final AlbumRepository albumRepo;
    private final ArtistRepository artistRepo;
    private final PlaylistRepository playlistRepo;

    /**
     * Creates a new database initializer.
     *
     * @param apiClient   client used to fetch data from the iTunes API
     * @param songRepo    repository for {@link Song} entities
     * @param albumRepo   repository for {@link Album} entities
     * @param artistRepo  repository for {@link Artist} entities
     * @param playlistRepo repository for {@link Playlist} entities
     */
    public DatabaseInitializer(ItunesApiClient apiClient, SongRepository songRepo, AlbumRepository albumRepo, ArtistRepository artistRepo, PlaylistRepository playlistRepo) {
        this.apiClient = apiClient;
        this.songRepo = songRepo;
        this.albumRepo = albumRepo;
        this.artistRepo = artistRepo;
        this.playlistRepo = playlistRepo;
    }

    /**
     * Initializes the database with music data and default playlists.
     *
     * <p>If the song table is empty, a predefined set of artist searches
     * is executed against the iTunes API. The resulting artists, albums,
     * and songs are persisted while avoiding duplicates.</p>
     *
     * <p>The method also ensures that required default playlists
     * ("Library" and "Favorites") exist.</p>
     *
     * @throws RuntimeException if data fetching or persistence fails
     */
    public void init() {
        // Check if database is populated, populate if empty
        if (songRepo.count() == 0) { // Limited artist set due to project scope
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
                    logger.error("init: search or persist failed for term: {}", term, e);
                    throw new RuntimeException("Failed to fetch or persist data for search term: " + term, e);
                }
            }
        }

        // Ensure default playlists exist
        if (!playlistRepo.existsByUniqueId(1L)) {
            Playlist library = playlistRepo.createPlaylist("Library");
            playlistRepo.addSongs(library, songRepo.findAll());
        }
        if (!playlistRepo.existsByUniqueId(2L)) {
            playlistRepo.createPlaylist("Favorites");
        }
    }
}
