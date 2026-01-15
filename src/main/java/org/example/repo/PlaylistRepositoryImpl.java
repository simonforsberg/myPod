package org.example.repo;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.NoResultException;
import org.example.entity.Playlist;
import org.example.entity.Song;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * JPA-based implementation of {@link PlaylistRepository}.
 *
 * <p>
 * This repository is responsible for all persistence operations related to
 * {@link Playlist} entities, including creation, retrieval, modification,
 * and deletion, as well as managing associations between playlists and songs.
 * </p>
 *
 * <p>
 * All write operations are executed within transactions. Read operations
 * use dedicated {@code EntityManager} instances to ensure proper resource handling.
 * </p>
 */
public class PlaylistRepositoryImpl implements PlaylistRepository {
    private static final Logger logger = LoggerFactory.getLogger(PlaylistRepositoryImpl.class);
    private final EntityManagerFactory emf;

    /**
     * Creates a new {@code PlaylistRepositoryImpl}.
     *
     * @param emf the {@link EntityManagerFactory} used to create entity managers
     */
    public PlaylistRepositoryImpl(EntityManagerFactory emf) {
        this.emf = emf;
    }

    /**
     * Checks whether a playlist exists with the given unique identifier.
     *
     * @param id the playlist ID
     * @return {@code true} if a playlist with the given ID exists, otherwise {@code false}
     * @throws IllegalArgumentException if {@code id} is {@code null}
     */
    @Override
    public boolean existsByUniqueId(Long id) {
        if (id == null) {
            logger.error("existsByUniqueId: id is null");
            throw new IllegalArgumentException("Playlist id can not be null");
        }
        try (var em = emf.createEntityManager()) {
            return em.createQuery("select count(pl) from Playlist pl where pl.id = :playlistId", Long.class)
                .setParameter("playlistId", id)
                .getSingleResult() > 0;
        }
    }

    /**
     * Retrieves all playlists with their associated songs, albums, and artists eagerly fetched.
     *
     * <p>
     * {@code DISTINCT} is used to avoid duplicate playlists caused by join fetching.
     * </p>
     *
     * @return a list of all playlists
     */
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

    /**
     * Retrieves a playlist by its identifier, including all associated songs,
     * albums, and artists.
     *
     * @param id the playlist ID
     * @return the matching {@link Playlist}
     * @throws IllegalArgumentException if {@code id} is {@code null}
     * @throws EntityNotFoundException if no playlist with the given ID exists
     */
    @Override
    public Playlist findById(Long id) {
        if (id == null) {
            logger.error("findById: id is null");
            throw new IllegalArgumentException("Playlist id can not be null");
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
                logger.error("findById: Playlist not found with id: {}", id);
                throw new EntityNotFoundException("Playlist with id " + id + " not found");
            }
        }
    }

    /**
     * Checks whether a given song is part of a specific playlist.
     *
     * @param playlist the playlist to check
     * @param song the song to look for
     * @return {@code true} if the song is contained in the playlist, otherwise {@code false}
     * @throws IllegalArgumentException if {@code playlist} or {@code song} is {@code null}
     */
    @Override
    public boolean isSongInPlaylist(Playlist playlist, Song song) {
        if (playlist == null || song == null) {
            logger.error("isSongInPlaylist: playlist or song is null");
            throw new IllegalArgumentException("playlist and song cannot be null");
        }
        try (var em = emf.createEntityManager()) {
            Playlist managed = em.find(Playlist.class, playlist.getId());
            if (managed == null) {
                return false;
            }
            return managed.getSongs().contains(song);
        }
    }

    /**
     * Creates and persists a new playlist with the given name.
     *
     * @param name the name of the new playlist
     * @return the persisted {@link Playlist}
     * @throws IllegalArgumentException if {@code name} is {@code null} or blank
     */
    @Override
    public Playlist createPlaylist(String name) {
        if (name == null || name.trim().isEmpty()) {
            logger.error("createPlaylist: name is null or empty");
            throw new IllegalArgumentException("name cannot be null or empty");
        }
        Playlist playlist = new Playlist(name);
        emf.runInTransaction(em -> em.persist(playlist));
        return playlist;
    }

    /**
     * Renames an existing playlist.
     *
     * @param playlist the playlist to rename
     * @param newName the new name
     * @throws IllegalArgumentException if arguments are invalid or playlist does not exist
     */
    @Override
    public void renamePlaylist(Playlist playlist, String newName) {
        if (playlist == null || newName == null || newName.trim().isEmpty()) {
            logger.error("renamePlaylist: playlist or name is null or empty");
            throw new IllegalArgumentException("Playlist and new name cannot be null or empty");
        }
        emf.runInTransaction(em -> {
            Playlist managed = em.find(Playlist.class, playlist.getId());
            if (managed == null) {
                logger.error("renamePlaylist: playlist not found with id: {}", playlist.getId());
                throw new IllegalArgumentException("Playlist not found with id: " + playlist.getId());
            }
            managed.setName(newName);
        });
    }

    /**
     * Deletes the given playlist.
     *
     * @param playlist the playlist to delete
     * @throws IllegalArgumentException if {@code playlist} is {@code null}
     */
    @Override
    public void deletePlaylist(Playlist playlist) {
        if (playlist == null) {
            logger.error("deletePlaylist: playlist is null");
            throw new IllegalArgumentException("Playlist cannot be null");
        }
        emf.runInTransaction(em -> {
            Playlist managed = em.merge(playlist);
            em.remove(managed);
        });
    }

    /**
     * Adds a single song to a playlist.
     *
     * @param playlist the target playlist
     * @param song the song to add
     * @throws IllegalArgumentException if playlist or song does not exist
     */
    @Override
    public void addSong(Playlist playlist, Song song) {
        if (playlist == null || song == null) {
            logger.error("addSong: playlist or song is null");
            throw new IllegalArgumentException("Playlist and song cannot be null");
        }
        emf.runInTransaction(em -> {
            Playlist managedPlaylist =
                em.find(Playlist.class, playlist.getId());
            if (managedPlaylist == null) {
                logger.error("addSong: playlist not found with id: {}", playlist.getId());
                throw new IllegalArgumentException("Playlist not found with id: " + playlist.getId());
            }
            Song managedSong =
                em.find(Song.class, song.getId());
            if (managedSong == null) {
                logger.error("addSong: song not found with id: {}", song.getId());
                throw new IllegalArgumentException("Song not found with id: " + song.getId());
            }
            managedPlaylist.addSong(managedSong);
        });
    }

    /**
     * Adds multiple songs to a playlist.
     *
     * @param playlist the target playlist
     * @param songs the songs to add
     * @throws IllegalArgumentException if playlist or songs are invalid
     */
    @Override
    public void addSongs(Playlist playlist, Collection<Song> songs) {
        if (playlist == null || songs == null) {
            logger.error("addSongs: playlist or songs is null");
            throw new IllegalArgumentException("Playlist and songs cannot be null");
        }
        emf.runInTransaction(em -> {
            Playlist managedPlaylist =
                em.find(Playlist.class, playlist.getId());
            if (managedPlaylist == null) {
                logger.error("addSongs: playlist not found with id: {}", playlist.getId());
                throw new IllegalArgumentException("Playlist not found with id: " + playlist.getId());
            }
            for (Song s : songs) {
                Song managedSong =
                    em.find(Song.class, s.getId());
                if (managedSong == null) {
                    logger.error("addSongs: song not found with id: {}", s.getId());
                    throw new IllegalArgumentException("Song not found with id: " + s.getId());
                }
                managedPlaylist.addSong(managedSong);
            }
        });
    }

    /**
     * Removes a song from a playlist.
     *
     * @param playlist the playlist to modify
     * @param song the song to remove
     * @throws IllegalArgumentException if playlist or song does not exist
     */
    @Override
    public void removeSong(Playlist playlist, Song song) {
        if (playlist == null || song == null) {
            logger.error("removeSong: playlist or song is null");
            throw new IllegalArgumentException("Playlist and song cannot be null");
        }
        emf.runInTransaction(em -> {
            Playlist managedPlaylist =
                em.find(Playlist.class, playlist.getId());

            if (managedPlaylist == null) {
                logger.error("removeSong: playlist not found with id: {}", playlist.getId());
                throw new IllegalArgumentException("Playlist not found with id: " + playlist.getId());
            }
            Song managedSong =
                em.find(Song.class, song.getId());

            if (managedSong == null) {
                logger.error("removeSong: song not found with id: {}", song.getId());
                throw new IllegalArgumentException("Song not found with id: " + song.getId());
            }
            managedPlaylist.removeSong(managedSong);
        });
    }
}
