package org.example.entity;

import jakarta.persistence.*;
import org.example.ItunesDTO;
import org.hibernate.proxy.HibernateProxy;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
public class Song {

    @Id
    @Column(name = "song_id")
    private Long songId;

    private String title;

    private Long length;

    @ManyToOne
    @JoinColumn(name = "album_id")
    private Album album;

    @ManyToMany(mappedBy = "songs")
    private Set<Playlist> playlist = new HashSet<>();

    protected Song() {
    }

    public Song(Long songId, String title, Long length, Album album) {
        this.songId = songId;
        this.title = title;
        this.length = length;
        this.album = album;
    }

    public static Song fromDTO(ItunesDTO dto, Album album) {
        if (dto.trackId() == null || dto.trackName() == null) {
            throw new IllegalArgumentException("Required fields (trackId, trackName) cannot be null");
        }
        return new Song(dto.trackId(), dto.trackName(), dto.trackTimeMillis(), album);
    }

    public String getFormattedLength() {
        if (length == null) return "0:00";

        long seconds = length / 1000;
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;

        return String.format("%d:%02d", minutes, remainingSeconds);
    }

    public Set<Playlist> getPlaylist() {
        return playlist;
    }

    public void setPlaylist(Set<Playlist> playlist) {
        this.playlist = playlist;
    }

    public Long getSongId() {
        return songId;
    }

    public void setSongId(Long id) {
        this.songId = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getLength() {
        return length;
    }

    public void setLength(Long length) {
        this.length = length;
    }

    public Album getAlbum() {
        return album;
    }

    public void setAlbum(Album album) {
        this.album = album;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Song song = (Song) o;
        return getSongId() != null && Objects.equals(getSongId(), song.getSongId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" +
            "songId = " + songId + ", " +
            "title = " + title + ", " +
            "length = " + length + ")";
    }
}
