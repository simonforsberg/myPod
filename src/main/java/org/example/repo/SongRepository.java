package org.example.repo;

import org.example.entity.Album;
import org.example.entity.Artist;
import org.example.entity.Song;

import java.util.List;

public interface SongRepository {

    boolean existsByUniqueId(Song song);

    Long count();

    void save(Song song);

    List<Song> findAll();

    List<Song> findByArtist(Artist artist);

    List<Song> findByAlbum(Album album);

    List<Song> findByGenre(String genre);
}
