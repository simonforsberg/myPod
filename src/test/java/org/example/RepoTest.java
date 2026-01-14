package org.example;

import org.example.entity.Album;
import org.example.entity.Artist;
import org.example.entity.Playlist;
import org.example.entity.Song;
import org.example.repo.AlbumRepositoryImpl;
import org.example.repo.ArtistRepositoryImpl;
import org.example.repo.PlaylistRepositoryImpl;
import org.example.repo.SongRepositoryImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

public class RepoTest {

    protected AlbumRepositoryImpl albumRepo;
    protected ArtistRepositoryImpl artistRepo;
    protected PlaylistRepositoryImpl playlistRepo;
    protected SongRepositoryImpl songRepo;

    protected Artist testArtist1;
    protected Artist testArtist2;

    protected Album testAlbum1;
    protected Album testAlbum2;

    protected Song testSong1;
    protected Song testSong2;
    protected Song testSong3;
    protected Song testSong4;
    protected Song testSong5;

    @BeforeEach
    void setup() {
        initTestObjects();
    }

    @AfterEach
    void tearDown() {
        TestPersistenceManager.close();
    }

    void initTestObjects() {
        artistRepo = new ArtistRepositoryImpl(TestPersistenceManager.get());
        albumRepo = new AlbumRepositoryImpl(TestPersistenceManager.get());
        songRepo = new SongRepositoryImpl(TestPersistenceManager.get());
        playlistRepo = new PlaylistRepositoryImpl(TestPersistenceManager.get());

        testArtist1 = new Artist(1L, "Test and Test", "Testistan");
        testArtist2 = new Artist(2L, "T.E.S.T", "United Tests");
        artistRepo.save(testArtist1);
        artistRepo.save(testArtist2);

        testAlbum1 = new Album(11L, "Best of Test", "Test Rock", 1993, 3L, null, testArtist1);
        testAlbum2 = new Album(22L, "Test volume 2", "Prog Test", 1980, 2L, null, testArtist2);
        albumRepo.save(testAlbum1);
        albumRepo.save(testAlbum2);

        testSong1 = new Song(111L, "Test Me Tender", 185000L, "", testAlbum1);
        testSong2 = new Song(112L, "Testing Ain't Easy", 190000L, "", testAlbum1);
        testSong3 = new Song(113L, "Crazy Little Thing Called Test", 180000L, "", testAlbum1);
        testSong4 = new Song(221L, "Another One Bites the Test", 185000L, "", testAlbum2);
        testSong5 = new Song(222L, "Here Comes the Test", 190000L, "", testAlbum2);
        songRepo.save(testSong1);
        songRepo.save(testSong2);
        songRepo.save(testSong3);
        songRepo.save(testSong4);
        songRepo.save(testSong5);
    }
}
