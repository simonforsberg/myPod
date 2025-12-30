package org.example.entity;

import jakarta.persistence.*;
import org.example.ItunesDTO;
import org.hibernate.proxy.HibernateProxy;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Entity
public class Album {

    @Id
    @Column(name="album_id")
    private Long albumId;

    private String name;

    private String genre;

    private int year;

    private Long trackCount;

    @Lob
    private byte[] cover;

//    @OneToMany(mappedBy = "album")
    //private List<Song> song;

   // @ManyToOne(cascade = CascadeType.PERSIST)
   // @JoinColumn(name="artist_id")
 //   private Artist artist;

    @ManyToOne
    @JoinColumn(name = "artist_id")
    private Artist artist;

    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Song> song = new ArrayList<>();

    protected Album (){}

    public Album(Long albumId, String name, String genre, int year, Long trackCount, Optional<BufferedImage> cover, Artist artist) {
        this.albumId = albumId;
        this.name = name;
        this.genre = genre;
        this.year = year;
        this.trackCount = trackCount;
        this.artist = artist;
        if(cover.isPresent()){
            //todo convert image to bytearray
        }
        else{
            //todo default album cover
        }
    }

    public static Album fromDTO(ItunesDTO dto, Artist artist) {
        if (dto.collectionId() == null || dto.collectionName() == null){
            throw new IllegalArgumentException("Required fields (albumId, albumName) cannot be null");
        }
        Optional<BufferedImage> cover;
        try {
            cover = Optional.ofNullable(ImageIO.read(dto.artworkUrl100()));
        } catch (IOException e) {
            cover = Optional.empty();
        }


        return new Album(dto.collectionId(), dto.collectionName(), dto.primaryGenreName(), dto.releaseYear(), dto.trackCount(), cover,artist);
    }

    public Long getAlbumId() {
        return albumId;
    }

    public void setAlbumId(Long albumId) {
        this.albumId = albumId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public Long getTrackCount() {
        return trackCount;
    }

    public void setTrackCount(Long trackCount) {
        this.trackCount = trackCount;
    }

    public List<Song> getSong() {
        return song;
    }

    public void setSong(List<Song> song) {
        this.song = song;
    }

    public Artist getArtist() {
        return artist;
    }

    public void setArtist(Artist artist) {
        this.artist = artist;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Album album = (Album) o;
        return getAlbumId() != null && Objects.equals(getAlbumId(), album.getAlbumId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
