package org.example.repo;

import javafx.application.Platform;
import org.example.entity.Playlist;
import org.example.entity.Song;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PlaylistRepository {

    List<Playlist> findAll();

    Set<Song> findSongsInPlaylist(Playlist playlist);

    boolean isSongInPlaylist(Playlist playlist, Song song);

    Playlist createPlaylist(String name);

    boolean existsByUniqueId(Long id);

    Playlist findById(Long id);

    void deletePlaylist(Playlist playlist);

    void addSong(Playlist playlist, Song song);

    void addSongs(Playlist playlist, Collection<Song> songs);

    void removeSong(Playlist playlist, Song song);

    void renamePlaylist(Playlist playlist, String newName);
}
