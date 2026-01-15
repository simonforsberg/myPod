package org.example;

import org.example.entity.Artist;
import org.example.entity.Song;
import org.example.repo.AlbumRepositoryImpl;
import org.example.repo.SongRepositoryImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SongRepositoryImpl}.
 */
@DisplayName("Song Repository Tests")
public class SongRepoTest extends RepoTest {

    @Test
    @DisplayName("Should return correct count of songs in database")
    void count_shouldReturnNumberOfSongs() {
        // Given, When
        Long count = songRepo.count();

        // Then
        assertThat(count).isEqualTo(5L);
    }

    @Test
    @DisplayName("Should confirm song exists when checking by unique ID")
    void existsByUniqueId_shouldFindSpecificSongIfPresent() {
        // Given, When
        boolean songExists = songRepo.existsByUniqueId(testSong1);

        // Then
        assertThat(songExists).isTrue();
    }

    @Test
    @DisplayName("Should save new song and make it findable")
    void saveSong_shouldSaveSong() {
        // Given
        Song testSong = new Song(12L, "Tester of Muppets", 666L, "http", testAlbum1);

        // When
        songRepo.save(testSong);
        List<Song> testSongs = songRepo.findAll();

        // Then
        assertThat(testSongs).contains(testSong);
    }

    @Test
    @DisplayName("Should retrieve all songs from database")
    void findAll_shouldFindAllSongs() {
        // Given, When
        List<Song> testSongs = songRepo.findAll();

        // Then
        assertThat(testSongs).contains(testSong1, testSong2, testSong3, testSong4, testSong5);
    }

    @Test
    @DisplayName("Should find all songs by specific artist")
    void findByArtist_shouldFindSongBySpecificArtist() {
        // Given, When
        List<Song> testSongs = songRepo.findByArtist(testArtist1);

        // Then
        assertThat(testSongs).contains(testSong1, testSong2, testSong3);
    }

    @Test
    @DisplayName("Should return empty list when artist has no songs")
    void findByArtist_shouldReturnEmptyListWhenArtistHasNoSongs() {
        // Given
        Artist artistWithNoSongs = new Artist(99L, "Jacub", "Denmark");
        artistRepo.save(artistWithNoSongs);

        // When
        List<Song> songs = songRepo.findByArtist(artistWithNoSongs);

        // Then
        assertThat(songs).isEmpty();
    }
}
