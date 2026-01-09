package org.example.entity;

import jakarta.persistence.*;
import org.example.ItunesDTO;
import org.hibernate.proxy.HibernateProxy;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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


    @ManyToOne
    @JoinColumn(name = "artist_id")
    private Artist artist;

    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Song> song = new ArrayList<>();

    protected Album (){}

    public Album(Long albumId, String name, String genre, int year, Long trackCount, byte[] cover, Artist artist) {
        this.albumId = albumId;
        this.name = name;
        this.genre = genre;
        this.year = year;
        this.trackCount = trackCount;
        this.artist = artist;
        this.cover = cover;
    }

    public static Album fromDTO(ItunesDTO dto, Artist artist) {
        if (dto.collectionId() == null || dto.collectionName() == null){
            throw new IllegalArgumentException("Required fields (albumId, albumName) cannot be null");
        }

        //Try getting cover from url first, if null go for backup image in resources
        //Backup might be unnecessary here, better to store as null and load default in ui?
        byte[] cover = generateAlbumCover(dto.artworkUrl100());
        //todo do this async?

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

    public byte[] getCover() {
        return cover;
    }

    public Image getCoverImage() {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(getCover());){
            return ImageIO.read(bais);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setCover(byte[] cover) {
        this.cover = cover;
    }

    /**
     * generate and returns byte array with cover art
     * @param url url pointing to desired cover
     * @return a byte array of either the desired cover or default from resources, returns null if both the URL image and default image fail to load
     */
    public static byte[] generateAlbumCover(URL url){
        BufferedImage bi = loadUrlImage(url);

        if (bi != null ) {
            return imageToBytes(bi);
        }
        else return imageToBytes(loadDefaultImage());
    }

    /**
     * converts image to byte array to be stored as BLOB
     * @param bi buffered jpg image
     * @return image converted to byte array
     */
    public static byte[] imageToBytes(BufferedImage bi){
        if (bi == null) return null;

        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()){
            ImageIO.write(bi, "jpg", stream); //should always be jpg for this application
            return stream.toByteArray();
        } catch(IOException e) {
            System.err.println(e);
            return null;
        }
    }

    /**
     *
     * @param url url pointing to desired cover
     * @return bufferedImage of desired cover or null if not available
     */
    public static BufferedImage loadUrlImage(URL url){
        if(url == null) return null;

        BufferedImage bi;
        try {
            bi = ImageIO.read(url);
        } catch (IOException e) {
            return null;
        }

        if (bi == null) {
            System.err.println("The URL does not point to a valid image.");
            return null;
        }

        return bi;
    }

    /**
     *
     * @return default cover art from resources
     * @throws IOException
     */
    public static BufferedImage loadDefaultImage() {
        try (InputStream is = Image.class.getResourceAsStream("/itunescover.jpg")) {
            if (is == null) {
                System.err.println("Could not load default image");
                return null;
            }
            return ImageIO.read(is);

        } catch (IOException e) {
            return null;
        }
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
