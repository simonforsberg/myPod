package org.example.repo;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.NoResultException;
import org.example.entity.Playlist;
import org.example.entity.Song;

import java.util.List;
import java.util.Set;

public class PlaylistRepositoryImpl implements PlaylistRepository {

    private final EntityManagerFactory emf;

    public PlaylistRepositoryImpl(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public void save(Playlist p) {
        if (p == null) {
            throw new IllegalArgumentException("Playlist cannot be null");
        }

        if (p.getId() == null) {
            emf.runInTransaction(em -> em.persist(p));

        } else {
            emf.runInTransaction(em -> em.merge(p));
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
        if (id == null) {
            return false;
        }
        try (var em = emf.createEntityManager()) {
            return em.createQuery("select count(pl) from Playlist pl where pl.id = :playlistId", Long.class)
                .setParameter("playlistId", id)
                .getSingleResult() > 0;
        }
    }

    @Override
    public Playlist findById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Playlist id cannot be null");
        }
        try (var em = emf.createEntityManager()) {
            try {
                return em.createQuery(
                        "SELECT p FROM Playlist p " +
                            "LEFT JOIN FETCH p.songs s " +
                            "LEFT JOIN FETCH s.album a " +
                            "LEFT JOIN FETCH a.artist " +
                            "WHERE p.id = :id",
                        Playlist.class
                    )
                    .setParameter("id", id)
                    .getSingleResult();
            } catch (NoResultException e) {
                throw new EntityNotFoundException("Playlist not found with id: " + id);
            }
        }
    }

    @Override
    public Set<Song> findSongsInPlaylist(Playlist playlist) {
        if (playlist == null) {
            throw new IllegalArgumentException("Playlist cannot be null");
        }
        return emf.callInTransaction(em -> {
            Playlist managed = em.merge(playlist);
            return managed.getSongs();
        });
    }

    @Override
    public boolean isSongInPlaylist(Playlist playlist, Song song) {
        if (playlist == null || song == null) {
            throw new IllegalArgumentException("Playlist and song cannot be null");
        }
        try (var em = emf.createEntityManager()) {
            Playlist managed = em.find(Playlist.class, playlist.getId());
            if (managed == null) {
                return false;
            }
            return managed.getSongs().contains(song);
        }
    }

    @Override
    public Playlist createPlaylist(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Playlist name cannot be null or empty");
        }
        Playlist playlist = new Playlist(name);
        emf.runInTransaction(em -> em.persist(playlist));
        return playlist;
    }

    @Override
    public void deletePlaylist(Playlist playlist) {
        if (playlist == null) {
            throw new IllegalArgumentException("Playlist cannot be null");
        }
        emf.runInTransaction(em -> {
            Playlist managed = em.merge(playlist);
            em.remove(managed);
        });
    }

    @Override
    public void addSong(Playlist playlist, Song song) {
        if (playlist == null || song == null) {
            throw new IllegalArgumentException("Playlist and song cannot be null");
        }
        emf.runInTransaction(em -> {
            Playlist managedPlaylist =
                em.find(Playlist.class, playlist.getId());
            if (managedPlaylist == null) {
                throw new IllegalStateException("Playlist not found with id: " + playlist.getId());
            }
            Song managedSong =
                em.find(Song.class, song.getId());
            if (managedSong == null) {
                throw new IllegalStateException("Song not found with id: " + song.getId());
            }
            managedPlaylist.addSong(managedSong);
        });
    }

    @Override
    public void removeSong(Playlist playlist, Song song) {
        if (playlist == null || song == null) {
            throw new IllegalArgumentException("Playlist and song cannot be null");
        }
        emf.runInTransaction(em -> {
            Playlist managedPlaylist =
                em.find(Playlist.class, playlist.getId());

            if (managedPlaylist == null) {
                throw new IllegalStateException("Playlist not found with id: " + playlist.getId());
            }
            Song managedSong =
                em.find(Song.class, song.getId());

            if (managedSong == null) {
                throw new IllegalStateException("Song not found with id: " + song.getId());
            }
            managedPlaylist.removeSong(managedSong);
        });
    }

    @Override
    public void renamePlaylist(Playlist playlist, String newName) {
        if (playlist == null || newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("Playlist and new name cannot be null or empty");
        }
        emf.runInTransaction(em -> {
            Playlist managed = em.find(Playlist.class, playlist.getId());
            if (managed == null) {
                throw new IllegalStateException("Playlist not found with id: " + playlist.getId());
            }
            managed.setName(newName);
        });
    }
}
