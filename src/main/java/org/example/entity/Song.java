package org.example.entity;

import jakarta.persistence.*;
import org.example.ItunesDTO;
import org.hibernate.proxy.HibernateProxy;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
public class Song implements DBObject {

    @Id
    @Column(name = "song_id")
    private Long id;

    @Column(name = "title")
    private String name;

    private Long length;

    private String previewUrl;

    @ManyToOne
    @JoinColumn(name = "album_id")
    private Album album;

    @ManyToMany(mappedBy = "songs")
    private Set<Playlist> playlist = new HashSet<>();

    protected Song() {
    }

    public Song(Long songId, String title, Long length, String previewUrl, Album album) {
        this.id = songId;
        this.name = title;
        this.length = length;
        this.previewUrl = previewUrl;
        this.album = album;
    }

    public static Song fromDTO(ItunesDTO dto, Album album) {
        if (dto.trackId() == null || dto.trackName() == null) {
            throw new IllegalArgumentException("Required fields (trackId, trackName) cannot be null");
        }
        return new Song(dto.trackId(), dto.trackName(), dto.trackTimeMillis(), dto.previewUrl(), album);
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String title) {
        this.name = title;
    }

    public Long getLength() {
        return length;
    }

    public void setLength(Long length) {
        this.length = length;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
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
        return getId() != null && Objects.equals(getId(), song.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" +
            "songId = " + id + ", " +
            "title = " + name + ", " +
            "length = " + length + ")";
    }
}
