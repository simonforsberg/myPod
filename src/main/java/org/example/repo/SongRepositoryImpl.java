package org.example.repo;

import jakarta.persistence.EntityManagerFactory;
import org.example.entity.Album;
import org.example.entity.Artist;
import org.example.entity.Song;

import java.util.ArrayList;
import java.util.List;

public class SongRepositoryImpl implements SongRepository {

    private final EntityManagerFactory emf;

    public SongRepositoryImpl(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public Long count() {
        try (var em = emf.createEntityManager()) {
            return em.createQuery("select count(s) from Song s", Long.class)
                .getSingleResult();
        }
    }

    @Override
    public boolean existsByUniqueId(Song song) {
        try (var em = emf.createEntityManager()) {
            return em.createQuery("select count(s) from Song s where s.id = :songId", Long.class)
                .setParameter("songId", song.getId())
                .getSingleResult() > 0;
        }
    }

    @Override
    public void save(Song song) {
        emf.runInTransaction(em -> em.persist(song));
    }

    @Override
    public List<Song> findAll() {
        return emf.callInTransaction(em ->
            em.createQuery("select s from Song s", Song.class)
                .getResultList());
    }

    @Override
    public List<Song> findByArtist(Artist artist) {
        if (artist == null) return new ArrayList<>();

        return emf.callInTransaction(em ->
            em.createQuery(
                    """
                        select s
                        from Song s
                        join fetch s.album a
                        join fetch a.artist art
                        where art = :artist
                        """,
                    Song.class
                )
                .setParameter("artist", artist)
                .getResultList());
    }

    @Override
    public List<Song> findByAlbum(Album album) {
        if (album == null) return new ArrayList<>();

        return emf.callInTransaction(em ->
            em.createQuery(
                    """
                        select s
                        from Song s
                        join fetch s.album a
                        join fetch a.artist art
                        where a = :album
                        """,
                    Song.class
                )
                .setParameter("album", album)
                .getResultList());
    }

    @Override
    public List<Song> findByGenre(String genre) {
        if (genre == null || genre.isBlank()) return new ArrayList<>();

        return emf.callInTransaction(em ->
            em.createQuery(
                    """
                        select s
                        from Song s
                        join fetch s.album a
                        join fetch a.artist art
                        where lower(a.genre) = lower(:genre)
                        """,
                    Song.class
                )
                .setParameter("genre", genre)
                .getResultList());
    }
}
