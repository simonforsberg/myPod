package org.example.repo;

import org.example.entity.Artist;

import java.util.List;

public interface ArtistRepository {

    boolean existsByUniqueId(Artist artist);

    Long count();

    void save(Artist artist);

    List<Artist> findAll();

}
