package org.example;

import org.example.entity.Album;
import org.example.entity.Artist;
import org.example.entity.Song;

import java.util.ArrayList;
import java.util.List;

public class DatabaseInitializer {

    private final ItunesApiClient apiClient;

    private final SongRepository songRepo;

    public DatabaseInitializer(ItunesApiClient apiClient, SongRepository songRepo) {
        this.apiClient = apiClient;
        this.songRepo = songRepo;
    }

    public void init() {
        if (songRepo.count() > 0) { //check if there is data already
            return;
        }

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
                    if (!songRepo.existsByUniqueId(ar)) {
                        songRepo.save(ar);
                    }

                    Album al = Album.fromDTO(dto, ar);
                    if (!songRepo.existsByUniqueId(al)) {
                        songRepo.save(al);
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
}
