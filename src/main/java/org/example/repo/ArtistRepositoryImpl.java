package org.example.repo;

import jakarta.persistence.EntityManagerFactory;
import org.example.entity.Artist;

import java.util.List;

public class ArtistRepositoryImpl implements ArtistRepository {

    private final EntityManagerFactory emf;

    public ArtistRepositoryImpl (EntityManagerFactory emf){
        this.emf = emf;
    }

    @Override
    public boolean existsByUniqueId(Artist artist) {
        return emf.callInTransaction(em ->
            em.createQuery("select count(a) from Artist a where a.id = :artistId", Long.class)
                .setParameter("artistId", artist.getId())
                .getSingleResult() > 0
        );
    }

    @Override
    public void save(Artist artist) {
        emf.runInTransaction(em -> em.persist(artist));
    }

    @Override
    public List<Artist> findAll() {
        return emf.callInTransaction(em ->
            em.createQuery("select a from Artist a", Artist.class)
                .getResultList());
    }

    @Override
    public Long count() {
        return emf.callInTransaction(em ->
            em.createQuery("select count(a) from Artist a", Long.class)
                .getSingleResult());
    }
}
