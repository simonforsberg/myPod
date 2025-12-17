package org.example;

import org.example.entity.Album;
import org.example.entity.Artist;
import org.example.entity.Song;

import java.util.List;

public interface SongRepository {
    List<Song> findSongByArtist();

    Long count();

    boolean existsByUniqueId(Song song);

    boolean existsByUniqueId(Album album);

    boolean existsByUniqueId(Artist artist);

    void save(Song song);

    void save(Album album);

    void save(Artist artist);
}
