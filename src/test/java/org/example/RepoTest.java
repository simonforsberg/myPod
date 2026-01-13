package org.example;

import org.example.entity.Album;
import org.example.entity.Artist;
import org.example.entity.Playlist;
import org.example.entity.Song;
import org.example.repo.AlbumRepositoryImpl;
import org.example.repo.ArtistRepositoryImpl;
import org.example.repo.PlaylistRepositoryImpl;
import org.example.repo.SongRepositoryImpl;
import org.junit.jupiter.api.BeforeAll;

public class RepoTest {

    private AlbumRepositoryImpl albumRepo;
    private ArtistRepositoryImpl artistRepo;
    private PlaylistRepositoryImpl playlistRepo;
    private SongRepositoryImpl songRepo;

    private Artist testArtist1;
    private Artist testArtist2;

    private Album testAlbum1;
    private Album testAlbum2;

    private Song testSong1;
    private Song testSong2;
    private Song testSong3;
    private Song testSong4;
    private Song testSong5;

    void initTestObjects(){
        artistRepo = new ArtistRepositoryImpl(TestPersistenceManager.get());
        albumRepo = new AlbumRepositoryImpl(TestPersistenceManager.get());
        songRepo = new SongRepositoryImpl(TestPersistenceManager.get());
        playlistRepo = new PlaylistRepositoryImpl(TestPersistenceManager.get());


        testArtist1 = new Artist(1L, "Test and Test", "Testistan");
        testArtist2 = new Artist(2L, "T.E.S.T", "United Tests");

        testAlbum1 = new Album(11L, "Best of Test", "Test Rock", 1993, 3L, null, testArtist1);
        testAlbum2 = new Album(22L, "Test volume 2", "Prog Test", 1980, 2L, null, testArtist2);

        testSong1 = new Song(111L, "Test Me Tender", 185000L, "", testAlbum1);
        testSong2 = new Song(112L, "Testing Ain't Easy", 190000L, "", testAlbum1);
        testSong3 = new Song(113L, "Crazy Little Thing Called Test", 180000L, "", testAlbum1);
        testSong4 = new Song(221L, "Another One Bites the Test", 185000L, "", testAlbum2);
        testSong5 = new Song(222L, "Here Comes the Test", 190000L, "", testAlbum2);
    }

}
