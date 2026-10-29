package muse.zik.model.audio;

import muse.zik.enums.AudioFormat;
import muse.zik.enums.MetadataFormat;
import muse.zik.model.Artist;

import java.io.File;
import java.nio.charset.Charset;
import java.util.*;

public abstract class AudioFile {

    private String title;
    private final List<Artist> artists = new ArrayList<>();
    private String album;
    private int year;
    private String comment;
    private int trackNumber;
    private int discNumber;
    private int duration;
    private String albumArtist;
    private byte[] albumCover;

    private final File file;
    protected final AudioFormat audioFormat;
    protected MetadataFormat metadataFormat;

    public AudioFile(File file, AudioFormat audioFormat) {
        this.file = file;
        this.audioFormat = audioFormat;
    }

    public AudioFile(File file, AudioFormat audioFormat, boolean clear) {
        if (!clear) {
            throw new IllegalArgumentException("Clear must be true");
        }
        this.file = file;
        this.audioFormat = audioFormat;
        this.album = "Unknown album";
        this.title = "Unknown title";
        this.year = -1;
        this.comment = "";
        this.trackNumber = -1;
        this.discNumber = -1;
        this.duration = -1;
        this.albumArtist = "Unknown";
        this.albumCover = null;
    }

    public File getOriginalFile() {
        return this.file;
    }

    public AudioFormat getAudioFormat() {
        return audioFormat;
    }

    public String getTitle() {
        return this.title;
    }
    public void setTitle(byte[] bytesName, Charset charset) {
        String name = this.transformBytesToStringAndTrim(bytesName, charset);
        this.title = name.isEmpty() ? "Unknown title" : name;
    }
    public void setTitle(String title) {
        this.title = title == null || title.isBlank()
                ? "Unknown title"
                : title;
    }

    public List<Artist> getArtists() {
        return Collections.unmodifiableList(artists);
    }
    public boolean addArtist(byte[] bytes, Charset charset) {
        var a = this.transformBytesToStringAndTrim(bytes, charset);
        String artistName = a.isEmpty() ? "Unknown artist" : a;

        boolean isArtistAlreadyExist = this.isArtistAlreadyInSong(artistName);
        if(!isArtistAlreadyExist) {
            Artist newArtist = new Artist(artistName);
            this.artists.add(newArtist);
            return true;
        }
        return false;
    }
    public boolean addArtist(String artistName) {
        var artistNameTrim = artistName == null || artistName.isBlank() ? "Unknown artist" : artistName.trim();
        boolean isArtistAlreadyExist = this.isArtistAlreadyInSong(artistNameTrim);

        if(!isArtistAlreadyExist) {
            Artist newArtist = new Artist(artistNameTrim);
            this.artists.add(newArtist);
            return true;
        }
        return false;
    }

    public void setAlbum(byte[] bytes, Charset charset) {
        String album = this.transformBytesToStringAndTrim(bytes, charset);
        this.album = album.isEmpty() ? "Unknown album" : album;
    }
    public String getAlbum() {
        return album;
    }

    public void setYear(byte[] bytes, Charset charset) {
        String stringYear = this.transformBytesToStringAndTrim(bytes, charset);
        if(stringYear.length() < 4) {
            this.year = -1;
            return;
        }
        try {
            this.year = Integer.parseInt(stringYear.substring(0, 4));
        } catch (NumberFormatException e) {
            this.year = -1;
        }
    }
    public void setYear(int year) {
        if(year < 0) {
            this.year = -1;
            return;
        }
        this.year = year;
    }
    public int getYear() {
        return year;
    }

    public String getComment() {
        return this.comment;
    }
    public void setComment(byte[] bytes, Charset charset) {
        this.comment = this.transformBytesToStringAndTrim(bytes, charset);
    }
    public void setComment(String comment) {
        this.comment = this.trimAndRemoveInvalidChar(comment);
    }

    public int getDuration() {
        return duration;
    }
    public void setDuration(byte[] bytes, Charset charset) {
        String stringDuration = this.transformBytesToStringAndTrim(bytes, charset);
        if(stringDuration.isEmpty()) {
            this.duration = -1;
            return;
        }
        try {
            this.duration = Integer.parseInt(stringDuration);
        } catch (NumberFormatException e) {
            this.duration = -1;
        }
    }
    public void setDuration(int duration) {
        if(duration < 0) {
            this.duration = -1;
            return;
        }
        this.duration = duration;
    }

    public int getTrackNumber() {
        return trackNumber;
    }

    public void setTrackNumber(int trackNumber) {
        if(trackNumber < 0) {
            this.trackNumber = -1;
            return;
        }
        this.trackNumber = trackNumber;
    }
    public void setTrackNumber(byte[] bytes, Charset charset) {
        String stringTrackNumber = this.transformBytesToStringAndTrim(bytes, charset);
        if(stringTrackNumber.isEmpty()) {
            this.trackNumber = -1;
            return;
        }
        try {
            this.trackNumber = Integer.parseInt(stringTrackNumber);
        } catch (NumberFormatException e) {
            this.trackNumber = -1;
        }
    }

    public MetadataFormat getMetadataFormat() {
        return metadataFormat;
    }

    public void setMetadataFormat(MetadataFormat metadataFormat) {
        this.metadataFormat = metadataFormat;
    }

    private boolean isArtistAlreadyInSong(String artistName) {
        return this.artists.stream()
                .anyMatch(artist -> Objects.equals(artist.getName(), artistName));
    }

    private String transformBytesToStringAndTrim(byte[] bytes, Charset charset) {
        return new String(bytes, charset).replaceAll("\u0000+$", "").trim();
    }

    private String trimAndRemoveInvalidChar(String s) {
        return s.replaceAll("\u0000+$", "").trim();
    }

}
