package org.example;

import org.example.entity.Playlist;
import org.example.entity.Song;
import org.example.repo.AlbumRepositoryImpl;
import org.example.repo.PlaylistRepositoryImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link PlaylistRepositoryImpl}.
 */
@DisplayName("Playlist Repository Tests")
public class PlaylistRepoTest extends RepoTest {

    @Test
    @DisplayName("Should confirm playlist exists when checking by unique ID")
    void existsByUniqueId_shouldFindSpecificPlaylistIfPresent() {
        // Given
        Playlist playlist = playlistRepo.createPlaylist("Playlist");

        // When
        boolean playlistExists = playlistRepo.existsByUniqueId(playlist.getId());

        // Then
        assertThat(playlistExists).isTrue();
    }

    @Test
    @DisplayName("Should retrieve all playlists from database")
    void findAll_shouldFindAllPlaylists() {
        // Given
        Playlist playlist1 = playlistRepo.createPlaylist("Playlist");
        Playlist playlist2 = playlistRepo.createPlaylist("Another playlist");

        // When
        List<Playlist> playlists = playlistRepo.findAll();

        // Then
        assertThat(playlists).contains(playlist1, playlist2);
    }

    @Test
    @DisplayName("Should confirm song is in playlist")
    void isSongInPlaylist_shouldConfirmSongInPlaylist() {
        // Given
        Playlist playlist = playlistRepo.createPlaylist("Playlist");
        playlistRepo.addSong(playlist, testSong1);

        // When
        boolean songInPlaylist = playlistRepo.isSongInPlaylist(playlist, testSong1);

        // Then
        assertThat(songInPlaylist).isTrue();
    }

    @Test
    @DisplayName("Should create playlist and persist it to database")
    void createPlaylist_shouldPersistAndBeFindable() {
        // Given, when
        Playlist playlist = playlistRepo.createPlaylist("Playlist");

        // Then
        assertThat(playlist.getId()).isNotNull();
        assertThat(playlistRepo.existsByUniqueId(playlist.getId())).isTrue();
    }

    @Test
    @DisplayName("Should rename playlist and save new name")
    void renamePlaylist_shouldSavePlaylistWithNewName() {
        // Given
        Playlist playlist = playlistRepo.createPlaylist("Playlist");

        // When
        playlistRepo.renamePlaylist(playlist, "NewPlaylist");

        // Then
        Playlist reloaded = playlistRepo.findById(playlist.getId());
        assertThat(reloaded.getName()).isEqualTo("NewPlaylist");
    }

    @Test
    @DisplayName("Should delete playlist from database")
    void deletePlaylist_shouldDeletePlaylist() {
        // Given
        Playlist playlist = playlistRepo.createPlaylist("Playlist");

        // When
        playlistRepo.deletePlaylist(playlist);

        // Then
        assertThat(playlistRepo.existsByUniqueId(playlist.getId())).isFalse();
    }

    @Test
    @DisplayName("Should add song to playlist and persist relationship")
    void addSongToPlaylist_shouldPersistRelation() {
        // Given
        Playlist playlist = playlistRepo.createPlaylist("Playlist");

        // When
        playlistRepo.addSong(playlist, testSong1);

        // Then
        Playlist reloaded = playlistRepo.findById(playlist.getId());

        assertThat(reloaded.getSongs())
            .hasSize(1)
            .contains(testSong1);
    }

    @Test
    @DisplayName("Should add multiple songs to playlist")
    void addSongsToPlaylist_shouldPersistRelation() {
        // Given
        Playlist playlist = playlistRepo.createPlaylist("Playlist");
        List<Song> testSongs = List.of(testSong1, testSong2, testSong3);

        // When
        playlistRepo.addSongs(playlist, testSongs);

        // Then
        Playlist reloaded = playlistRepo.findById(playlist.getId());

        assertThat(reloaded.getSongs())
            .hasSize(3)
            .contains(testSong1, testSong2, testSong3);
    }

    @Test
    @DisplayName("Should remove song from playlist")
    void removeSong_shouldRemoveSongFromPlaylist() {
        // Given
        Playlist playlist = playlistRepo.createPlaylist("Playlist");
        playlistRepo.addSong(playlist, testSong1);

        // When
        playlistRepo.removeSong(playlist, testSong1);

        // Then
        Playlist reloaded = playlistRepo.findById(playlist.getId());

        assertThat(reloaded.getSongs()).isEmpty();
    }
}
