package org.example;

import org.example.entity.Artist;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
        Artist testArtist3 = new Artist(12L, "A Tribe Called Test", "USA");

        // When
        artistRepo.save(testArtist3);
        List<Artist> artists = artistRepo.findAll();

        // Then
        assertThat(artists).contains(testArtist3);
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
        Artist nonExistent = new Artist(999L, "Not In DB", "Nowhere");

        // When
        boolean exists = artistRepo.existsByUniqueId(nonExistent);

        // Then
        assertThat(exists).isFalse();
    }
}
