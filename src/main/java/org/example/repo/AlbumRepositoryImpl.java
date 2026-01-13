package org.example.repo;

import jakarta.persistence.EntityManagerFactory;
import org.example.entity.Album;
import org.example.entity.Artist;

import java.util.List;

public class AlbumRepositoryImpl implements AlbumRepository {

    private final EntityManagerFactory emf;

    public AlbumRepositoryImpl (EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public boolean existsByUniqueId(Album album) {
        return emf.callInTransaction(em ->
            em.createQuery("select count(a) from Album a where a.id = :albumId", Long.class)
                .setParameter("albumId", album.getId())
                .getSingleResult() > 0
        );
    }

    @Override
    public void save(Album album) {
        emf.runInTransaction(em -> em.persist(album));
    }

    @Override
    public List<Album> findAll() {
        return emf.callInTransaction(em ->
            em.createQuery("select a from Album a", Album.class)
                .getResultList());
    }

    @Override
    public List<Album> findByArtist(Artist artist) {
        return emf.callInTransaction(em ->
            em.createQuery("select a from Album a where a.artist = :artist", Album.class)
                .setParameter("artist", artist)
                .getResultList()
        );
    }

    @Override
    public List<Album> findByGenre(String genre) {
        return emf.callInTransaction(em ->
            em.createQuery("select a from Album a where a.genre = :genre", Album.class)
                .setParameter("genre", genre)
                .getResultList()
        );
    }

    @Override
    public Long count() {
        return emf.callInTransaction(em ->
            em.createQuery("select count(a) from Album a", Long.class)
                .getSingleResult());
    }
}
