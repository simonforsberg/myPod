package org.example;

import org.example.entity.Album;
import org.example.entity.Artist;
import org.example.repo.AlbumRepositoryImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link AlbumRepositoryImpl}.
 */
@DisplayName("Album Repository Tests")
public class AlbumRepoTest extends RepoTest {

    @Test
    @DisplayName("Should confirm album exists when checking by unique ID")
    void existsByUniqueId_shouldFindSpecificAlbumIfPresent() {
        // Given, When
        boolean albumExists = albumRepo.existsByUniqueId(testAlbum1);

        // Then
        assertThat(albumExists).isTrue();
    }

    @Test
    @DisplayName("Should return correct count of albums in database")
    void count_shouldReturnNumberOfAlbums() {
        // Given, When
        Long count = albumRepo.count();

        // Then
        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("Should save new album and make it findable")
    void save_shouldSaveNewAlbum() {
        // Given
        Album newAlbum = new Album(33L, "To Test A Butterfly", "Hiphop", 2015, 16L, null, testArtist1);

        // When
        albumRepo.save(newAlbum);
        List<Album> albums = albumRepo.findAll();

        // Then
        assertThat(albums).contains(newAlbum);
    }

    @Test
    @DisplayName("Should retrieve all albums from database")
    void findAll_shouldFindAllAlbums() {
        // Given, When
        List<Album> albums = albumRepo.findAll();

        // Then
        assertThat(albums).contains(testAlbum1, testAlbum2);
    }

    @Test
    @DisplayName("Should find all albums by specific artist")
    void findByArtist_shouldFindSpecificAlbum() {
        // Given, When
        List<Album> testAlbums = albumRepo.findByArtist(testArtist1);

        // Then
        assertThat(testAlbums).contains(testAlbum1);
    }

    @Test
    @DisplayName("Should return empty list when artist has no albums")
    void findByArtist_shouldReturnEmptyListWhenNoAlbums() {
        // Given
        Artist artistWithNoAlbums = new Artist(99L, "Jacub", "Denmark");
        artistRepo.save(artistWithNoAlbums);

        // When
        List<Album> albums = albumRepo.findByArtist(artistWithNoAlbums);

        // Then
        assertThat(albums).isEmpty();
    }
}
