package org.example;

import org.example.entity.Album;
import org.example.entity.Artist;
import org.example.entity.Playlist;
import org.example.entity.Song;
import org.example.repo.AlbumRepositoryImpl;
import org.example.repo.ArtistRepositoryImpl;
import org.example.repo.PlaylistRepositoryImpl;
import org.example.repo.SongRepositoryImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlaylistRepoTest {
    private AlbumRepositoryImpl albumRepo;
    private PlaylistRepositoryImpl playlistRepo;
    private SongRepositoryImpl songRepo;
    private ArtistRepositoryImpl artistRepo;

    @BeforeEach
    void setup() {
        albumRepo = new AlbumRepositoryImpl(TestPersistenceManager.get());
        playlistRepo = new PlaylistRepositoryImpl(TestPersistenceManager.get());
        artistRepo = new ArtistRepositoryImpl(TestPersistenceManager.get());
        songRepo = new SongRepositoryImpl(TestPersistenceManager.get());
    }

    @AfterAll
    static void tearDown() {
        TestPersistenceManager.close();
    }

    @Test
    void test() {
        assertThat(true).isTrue();
    }

    @Test
    void createPlaylist_shouldPersistAndBeFindable() {
        Playlist playlist = playlistRepo.createPlaylist("playlist");

        assertThat(playlist.getPlaylistId()).isNotNull();
        assertThat(playlistRepo.existsByUniqueId(playlist.getPlaylistId())).isTrue();
    }

    @Test
    void addSongToPlaylist_shouldPersistRelation() {
        // given
        Artist artist = new Artist(1L, "Blabla", "Sweden");
        artistRepo.save(artist);
        Album album = new Album(1L, "Title", "Rock", 1992, 12L, artist);
        albumRepo.save(album);
        Song song = new Song(1L, "Title", 33333L, album);
        songRepo.save(song);

        Playlist playlist = playlistRepo.createPlaylist("Playlist");
        
        // when
        playlistRepo.addSong(playlist, song);

        // then
        Playlist reloaded =
            playlistRepo.findById(playlist.getPlaylistId());

        assertThat(reloaded.getSongs())
            .hasSize(1)
            .contains(song);
    }
}
