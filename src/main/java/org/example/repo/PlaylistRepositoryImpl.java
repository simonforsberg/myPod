package org.example.repo;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import org.example.PersistenceManager;
import org.example.entity.Playlist;
import org.example.entity.Song;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class PlaylistRepositoryImpl implements PlaylistRepository {

    private final EntityManagerFactory emf = PersistenceManager.getEntityManagerFactory();

    @Override
    public void save(Playlist p) {
        if (p.getPlaylistId() == null) {
            emf.runInTransaction(em -> em.persist(p));

        } else {
            emf.runInTransaction(em -> em.merge(p)); // uppdatering
        }
    }

    @Override
    public List<Playlist> findAll() {
        try (var em = emf.createEntityManager()) {
            return em.createQuery(
                "SELECT DISTINCT p FROM Playlist p " +
                    "LEFT JOIN FETCH p.songs s " +
                    "LEFT JOIN FETCH s.album a " +
                    "LEFT JOIN FETCH a.artist",
                Playlist.class
            ).getResultList();
        }
    }

    @Override
    public boolean existsByUniqueId(Long id) {
        try (var em = emf.createEntityManager()) {
            return em.createQuery("select count(pl) from Playlist pl where pl.playlistId = :playlistId", Long.class)
                .setParameter("playlistId", id)
                .getSingleResult() > 0;
        }
    }

    @Override
    public Playlist findById(Long id) {
        try (var em = emf.createEntityManager()) {
            return em.createQuery(
                    "SELECT p FROM Playlist p " +
                        "LEFT JOIN FETCH p.songs s " +
                        "LEFT JOIN FETCH s.album a " +
                        "LEFT JOIN FETCH a.artist " +
                        "WHERE p.playlistId = :id",
                    Playlist.class
                )
                .setParameter("id", id)
                .getSingleResult();
        }
    }

    @Override
    public Set<Song> findSongsInPlaylist(Playlist playlist) {
        return emf.callInTransaction(em -> {
            Playlist managed = em.merge(playlist);
            return managed.getSongs();
        });
    }

    @Override
    public boolean isSongInPlaylist(Playlist playlist, Song song) {
        try (var em = emf.createEntityManager()) {
            return em.createQuery(
                "SELECT count(s) FROM Playlist p " +
                    "LEFT JOIN FETCH p.songs s" +
                    "WHERE p.playlistId = :pid" +
                    "AND s.songId = :sid",
                    Long.class)
                .setParameter("pid", playlist.getPlaylistId())
                .setParameter("sid", song.getSongId())
                .getSingleResult() > 0;
        }
    }

    @Override
    public Playlist createPlaylist(String name) {
        Playlist playlist = new Playlist(name);
        emf.runInTransaction(em -> em.persist(playlist));
        return playlist;
    }

    @Override
    public void deletePlaylist(Playlist playlist) {
        emf.runInTransaction(em -> {
            Playlist managed = em.merge(playlist);
            em.remove(managed);
        });
    }

    @Override
    public void addSong(Playlist playlist, Song song) {
        emf.runInTransaction(em -> {
            Playlist managed = em.merge(playlist);
            managed.addSong(song);
        });
    }

    @Override
    public void removeSong(Playlist playlist, Song song) {
        emf.runInTransaction(em -> {
            Playlist managed = em.merge(playlist);
            managed.removeSong(song);
        });
    }

    @Override
    public void renamePlaylist(Playlist playlist, String newName) {
        playlist.setName(newName);
        emf.runInTransaction(em -> em.merge(playlist));
    }
}
