package org.example.repo;

import org.example.entity.Playlist;
import org.example.entity.Song;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface PlaylistRepository {

    boolean existsByUniqueId(Long id);

    List<Playlist> findAll();

    Playlist findById(Long id);

    Set<Song> findSongsInPlaylist(Playlist playlist);

    boolean isSongInPlaylist(Playlist playlist, Song song);

    Playlist createPlaylist(String name);

    void renamePlaylist(Playlist playlist, String newName);

    void deletePlaylist(Playlist playlist);

    void addSong(Playlist playlist, Song song);

    void addSongs(Playlist playlist, Collection<Song> songs);

    void removeSong(Playlist playlist, Song song);

}
