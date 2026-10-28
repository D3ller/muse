package muse.zik.model.audio;

import muse.zik.exception.ArtistAlreadyDeclared;
import muse.zik.model.Artist;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MP3File extends AudioFile {

    private String name;
    private final List<Artist> artists = new ArrayList<>();
    private String album;
    // if year is equals to -1 the date is unknown.
    private int year = -1;

    public MP3File(File file) {
        super(file);
    }

    public MP3File (File file, boolean clear) {
        super(file);
        if(clear) {
            this.album = "Unknown album";
            this.name = "Unknown title";
        }
    }

    public String getName() {
        return this.name;
    }

    public void setName(byte[] bytesName) {
        String name = this.transformBytesToStringAndTrim(bytesName);
        this.name = name.isEmpty() ? "Unknown" : name;
    }

    public void setArtist(byte[] bytes) throws ArtistAlreadyDeclared {
        var artistName = this.transformBytesToStringAndTrim(bytes);

         Optional<Artist> optionalArtist = this.artists.stream().filter(artist -> Objects.equals(artist.getName(), artistName)).findFirst();
         if(optionalArtist.isEmpty()) {
            Artist newArtist = new Artist(artistName);
            this.artists.add(newArtist);
         } else {
             throw new ArtistAlreadyDeclared("Artist is already declared");
         }
    }

    public List<Artist> getArtists() {
        return this.artists;
    }

    public void setAlbum(byte[] bytes) {
        String album = this.transformBytesToStringAndTrim(bytes);
        this.album = album.isEmpty() ? "Unknown" : album;
    }

    public void setYear(byte[] bytes) {
        String stringYear = this.transformBytesToStringAndTrim(bytes);
        try {
            this.year = Integer.parseInt(stringYear);
        } catch (NumberFormatException e) {
            this.year = -1;
        }
    }

    public String getAlbum() {
        return album;
    }

    public int getYear() {
        return year;
    }
/*
    public ... getInfo() {
        return;
    }
     */

    private String transformBytesToStringAndTrim(byte[] bytes) {
        return new String(bytes, StandardCharsets.ISO_8859_1).trim();
    }

}
