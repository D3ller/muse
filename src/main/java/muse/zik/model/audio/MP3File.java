package muse.zik.model.audio;

import muse.zik.enums.AudioFormat;
import muse.zik.model.Artist;

import java.io.File;
import java.nio.charset.Charset;
import java.util.*;

public class MP3File extends AudioFile {

    private String name;
    private final List<Artist> artists = new ArrayList<>();
    private String album;
    /**
    if year is equals to -1 the date is unknown.
     */
    private int year = -1;

    public MP3File(File file) {
        super(file, AudioFormat.MP3);
    }

    public MP3File (File file, boolean clear) {
        super(file, AudioFormat.MP3);
        if(clear) {
            this.album = "Unknown album";
            this.name = "Unknown title";
        }
    }

    public List<Artist> getArtists() {
        return Collections.unmodifiableList(this.artists);
    }

    public void setAlbum(byte[] bytes, Charset charset) {
        String album = this.transformBytesToStringAndTrim(bytes, charset);
        this.album = album.isEmpty() ? "Unknown album" : album;
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

    private String transformBytesToStringAndTrim(byte[] bytes, Charset charset) {
        return new String(bytes, charset).replaceAll("\u0000+$", "").trim();
    }

    //for debug
    @Override
    public String toString() {
        return "MP3File{" +
                "name='" + name + '\'' +
                ", artists=" + artists +
                ", album='" + album + '\'' +
                ", year=" + year +
                ", file=" + this.getOriginalFile().getName() +
                ", version" + this.getMetadataFormat() +
                '}';
    }
}
