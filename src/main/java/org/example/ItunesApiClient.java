package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Client for interacting with the iTunes Search API.
 *
 * <p>This class is responsible for executing HTTP requests against
 * Apple's public iTunes Search API and mapping the results into
 * {@link ItunesDTO} objects.</p>
 *
 * <p>It performs basic response validation and result filtering
 * to ensure that only relevant data is returned.</p>
 */

public class ItunesApiClient {
    private static final Logger logger = LoggerFactory.getLogger(ItunesApiClient.class);
    private final HttpClient client;
    private final ObjectMapper mapper;

    /**
     * Creates a new iTunes API client.
     *
     * <p>Initializes an {@link HttpClient} and configures a Jackson
     * {@link ObjectMapper} with Java Time support.</p>
     */
    public ItunesApiClient() {
        this.client = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
    }

    /**
     * Searches for songs by artist name using the iTunes Search API.
     *
     * <p>The search results are filtered so that only songs whose
     * artist name matches the provided term (after normalization)
     * are returned.</p>
     *
     * @param term artist search term
     * @return list of matching {@link ItunesDTO} objects
     * @throws Exception if the HTTP request or JSON parsing fails
     */
    public List<ItunesDTO> searchSongs(String term) throws Exception {
        String encodedTerm = URLEncoder.encode(term, StandardCharsets.UTF_8);
        String url = "https://itunes.apple.com/search?term=" + encodedTerm + "&entity=song&attribute=artistTerm&limit=20";

        HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .build();

        HttpResponse<String> response =
            client.send(request, HttpResponse.BodyHandlers.ofString());

        // Validate HTTP response
        if (response.statusCode() != 200) {
            logger.error("searchSongs: status code {}", response.statusCode());
            throw new RuntimeException("API error: " + response.statusCode());
        }

        // Parse JSON response
        JsonNode root = mapper.readTree(response.body());
        JsonNode results = root.get("results");
        if (results == null || !results.isArray()) {
            logger.debug("searchSongs: no results");
            return List.of();
        }

        String normalizedTerm = normalize(term);

        List<ItunesDTO> songs = new ArrayList<>();
        for (JsonNode node : results) {
            ItunesDTO song = mapper.treeToValue(node, ItunesDTO.class);

            if (song.artistName() == null) {
                logger.warn("searchSongs: artistName is null");
                continue;
            }

            String normalizedArtist = normalize(song.artistName());

            if (normalizedTerm.equals(normalizedArtist)) {
                songs.add(song);
            }
        }

        return songs;
    }

    /**
     * Normalizes a string for comparison purposes.
     *
     * <p>The normalization process converts the string to lowercase,
     * collapses whitespace and plus characters, and trims leading
     * and trailing spaces.</p>
     *
     * @param s input string
     * @return normalized string, or an empty string if {@code s} is {@code null}
     */
    public String normalize(String s) {
        if (s == null) {
            return "";
        }
        return s.toLowerCase()
            .replaceAll("[+\\s]+", " ")
            .trim();
    }
}

