package org.example;

import org.example.entity.Artist;
import org.example.repo.AlbumRepositoryImpl;
import org.example.repo.ArtistRepositoryImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ArtistRepositoryImpl}.
 */
@DisplayName("Artist Repository Tests")
public class ArtistRepoTest extends RepoTest {

    @Test
    @DisplayName("Should confirm artist exists when checking by unique ID")
    void existsByUniqueId_shouldFindSpecificArtistIfPresent() {
        // Given, When
        boolean artistExists = artistRepo.existsByUniqueId(testArtist1);

        // Then
        assertThat(artistExists).isTrue();
    }

    @Test
    @DisplayName("Should return correct count of artists in database")
    void count_shouldReturnNumberOfArtists() {
        // Given, When
        Long count = artistRepo.count();

        // Then
        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("Should save new artist and make it findable")
    void save_shouldSaveNewArtist() {
        // Given
        Artist newArtist = new Artist(12L, "A Tribe Called Test", "USA");

        // When
        artistRepo.save(newArtist);
        List<Artist> artists = artistRepo.findAll();

        // Then
        assertThat(artists).contains(newArtist);
    }

    @Test
    @DisplayName("Should retrieve all artists from database")
    void findAll_shouldFindAllArtists() {
        // Given, When
        List<Artist> artists = artistRepo.findAll();

        // Then
        assertThat(artists).contains(testArtist1, testArtist2);
    }

    @Test
    @DisplayName("Should return false when artist does not exist")
    void existsByUniqueId_shouldReturnFalseForNonExistentArtist() {
        // Given
        Artist nonExistentArtist = new Artist(999L, "Jacub", "Denmark");

        // When
        boolean exists = artistRepo.existsByUniqueId(nonExistentArtist);

        // Then
        assertThat(exists).isFalse();
    }
}
