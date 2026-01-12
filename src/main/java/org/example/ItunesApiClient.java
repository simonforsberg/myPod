package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ItunesApiClient {

    private final HttpClient client;
    private final ObjectMapper mapper;

    public ItunesApiClient() {
        this.client = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
    }

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

        // Kontrollera status
        if (response.statusCode() != 200) {
            throw new RuntimeException("API-fel: " + response.statusCode());
        }

        // Parse JSON
        JsonNode root = mapper.readTree(response.body());
        JsonNode results = root.get("results");
        if (results == null || !results.isArray()) {
            return List.of();
        }

        String normalizedTerm = normalize(term);

        List<ItunesDTO> songs = new ArrayList<>();
        for (JsonNode node : results) {
            ItunesDTO song = mapper.treeToValue(node, ItunesDTO.class);

            if (song.artistName() == null) {
                continue;
            }

            String normalizedArtist = normalize(song.artistName());

            if (normalizedTerm.equals(normalizedArtist)) {
                songs.add(song);
            }
        }

        return songs;
    }

    public String normalize(String s) {
        if (s == null) {
            return "";
        }
        return s.toLowerCase()
            .replaceAll("[+\\s]+", " ")
            .trim();
    }
}

