package org.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ItunesDTO(Long artistId,
                        Long collectionId,
                        Long trackId,
                        String trackName,
                        String artistName,
                        String collectionName,
                        String country,
                        String primaryGenreName,
                        LocalDate releaseDate,
                        Long trackCount,
                        Long trackTimeMillis) {

    public int releaseYear() {
        return releaseDate != null ? releaseDate.getYear() : 0;
    }
}
