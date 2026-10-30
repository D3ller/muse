package muse.zik.model.audio;

import muse.zik.enums.AudioFormat;
import muse.zik.enums.MetadataFormat;
import muse.zik.model.Artist;

import java.io.File;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Base class of an audio file and its metadata.
 * <p>
 * Numeric fields (year, track, disc, duration) use {@code -1} when the value is unknown.
 * Text fields fall back to an "Unknown ..." value when empty.
 */
public abstract class AudioFile {

    private String title;
    private final List<Artist> artists = new ArrayList<>();
    private String album;
    private int year;
    private String comment;
    private int trackNumber;
    private int discNumber;
    private long duration;
    private Artist albumArtist;
    private byte[] albumCover;

    private final File file;
    protected final AudioFormat audioFormat;
    protected MetadataFormat metadataFormat;

    /**
     * Creates an audio file whose metadata is left unset.
     *
     * @param file        the original file
     * @param audioFormat the container format of the file
     */
    public AudioFile(File file, AudioFormat audioFormat) {
        this.file = file;
        this.audioFormat = audioFormat;
    }

    /**
     * Creates an audio file whose metadata is initialized to default values
     * ("Unknown album", "Unknown title", {@code -1} for numbers, empty comment).
     *
     * @param file        the original file
     * @param audioFormat the container format of the file
     * @param clear       must be {@code true}
     * @throws IllegalArgumentException if {@code clear} is {@code false}
     */
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
        this.albumArtist = null;
        this.albumCover = null;
    }

    /**
     * @return the file this metadata was read from
     */
    public File getOriginalFile() {
        return this.file;
    }

    /**
     * @return the container format of the file
     */
    public AudioFormat getAudioFormat() {
        return audioFormat;
    }

    /**
     * @return the title, or "Unknown title" if none
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * Sets the title from raw tag bytes. Trailing null characters and spaces are removed.
     *
     * @param bytesName the raw title bytes
     * @param charset   the charset used to decode the bytes
     */
    public void setTitle(byte[] bytesName, Charset charset) {
        String name = this.transformBytesToStringAndTrim(bytesName, charset);
        this.title = name.isEmpty() ? "Unknown title" : name;
    }

    /**
     * Sets the title.
     *
     * @param title the title, "Unknown title" is used if {@code null} or blank
     */
    public void setTitle(String title) {
        this.title = title == null || title.isBlank()
                ? "Unknown title"
                : title;
    }

    /**
     * @return an unmodifiable view of the artists of the song
     */
    public List<Artist> getArtists() {
        return Collections.unmodifiableList(artists);
    }

    /**
     * Adds an artist from raw tag bytes. Does nothing if an artist with the same name already exists.
     *
     * @param bytes   the raw artist name bytes
     * @param charset the charset used to decode the bytes
     * @return {@code true} if the artist was added, {@code false} if it already existed
     */
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

    /**
     * Adds an artist. Does nothing if an artist with the same name already exists.
     *
     * @param artistName the artist name, "Unknown artist" is used if {@code null} or blank
     * @return {@code true} if the artist was added, {@code false} if it already existed
     */
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

    /**
     * Sets the album from raw tag bytes.
     *
     * @param bytes   the raw album bytes
     * @param charset the charset used to decode the bytes
     */
    public void setAlbum(byte[] bytes, Charset charset) {
        String album = this.transformBytesToStringAndTrim(bytes, charset);
        this.album = album.isEmpty() ? "Unknown album" : album;
    }

    /**
     * @return the album, or "Unknown album" if none
     */
    public String getAlbum() {
        return album;
    }

    /**
     * @return the album artist, or {@code null} if unknown
     */
    public Artist getAlbumArtist() {
        return this.albumArtist;
    }

    /**
     * Sets the album artist from raw tag bytes.
     *
     * @param bytes   the raw album artist bytes
     * @param charset the charset used to decode the bytes
     */
    public void setAlbumArtist(byte[] bytes, Charset charset) {
        this.setAlbumArtist(this.transformBytesToStringAndTrim(bytes, charset));
    }

    /**
     * Sets the album artist.
     *
     * @param albumArtistName the album artist name, {@code null} is used if {@code null} or blank
     */
    public void setAlbumArtist(String albumArtistName) {
        this.albumArtist = albumArtistName == null || albumArtistName.isBlank()
                ? null
                : new Artist(albumArtistName.trim());
    }

    /**
     * Sets the year from raw tag bytes. Only the first 4 characters are read.
     *
     * @param bytes   the raw year bytes
     * @param charset the charset used to decode the bytes
     */
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

    /**
     * Sets the year.
     *
     * @param year the year, {@code -1} is used if negative
     */
    public void setYear(int year) {
        if(year < 0) {
            this.year = -1;
            return;
        }
        this.year = year;
    }

    /**
     * @return the year, or {@code -1} if unknown
     */
    public int getYear() {
        return year;
    }

    /**
     * @return the comment, empty if none
     */
    public String getComment() {
        return this.comment;
    }

    /**
     * Sets the comment from raw tag bytes.
     *
     * @param bytes   the raw comment bytes
     * @param charset the charset used to decode the bytes
     */
    public void setComment(byte[] bytes, Charset charset) {
        this.comment = this.transformBytesToStringAndTrim(bytes, charset);
    }

    /**
     * Sets the comment. Trailing null characters and spaces are removed.
     *
     * @param comment the comment
     */
    public void setComment(String comment) {
        this.comment = this.trimAndRemoveInvalidChar(comment);
    }

    /**
     * @return the duration, or {@code -1} if unknown
     */
    public long getDuration() {
        return duration;
    }

    /**
     * Sets the duration from raw tag bytes holding a number as text.
     *
     * @param bytes   the raw duration bytes
     * @param charset the charset used to decode the bytes
     */
    public void setDuration(byte[] bytes, Charset charset) {
        String stringDuration = this.transformBytesToStringAndTrim(bytes, charset);
        if(stringDuration.isEmpty()) {
            this.duration = -1;
            return;
        }
        try {
            this.duration = Long.parseLong(stringDuration);
        } catch (NumberFormatException e) {
            this.duration = -1;
        }
    }

    /**
     * Sets the duration.
     *
     * @param duration the duration, {@code -1} is used if negative
     */
    public void setDuration(long duration) {
        if(duration < 0) {
            this.duration = -1;
            return;
        }
        this.duration = duration;
    }

    /**
     * @return the disc number, or {@code -1} if unknown
     */
    public int getDiscNumber() {
        return discNumber;
    }

    /**
     * Sets the disc number.
     *
     * @param discNumber the disc number, {@code -1} is used if negative
     */
    public void setDiscNumber(int discNumber) {
        if(discNumber < 0) {
            this.discNumber = -1;
            return;
        }
        this.discNumber = discNumber;
    }

    /**
     * Sets the disc number from raw tag bytes holding a number as text.
     *
     * @param bytes   the raw disc number bytes
     * @param charset the charset used to decode the bytes
     */
    public void setDiscNumber(byte[] bytes, Charset charset) {
        String stringDiscNumber = this.transformBytesToStringAndTrim(bytes, charset);
        if(stringDiscNumber.isEmpty()) {
            this.discNumber = -1;
            return;
        }
        try {
            this.discNumber = Integer.parseInt(stringDiscNumber);
        } catch (NumberFormatException e) {
            this.discNumber = -1;
        }
    }

    /**
     * @return the track number, or {@code -1} if unknown
     */
    public int getTrackNumber() {
        return trackNumber;
    }

    /**
     * Sets the track number.
     *
     * @param trackNumber the track number, {@code -1} is used if negative
     */
    public void setTrackNumber(int trackNumber) {
        if(trackNumber < 0) {
            this.trackNumber = -1;
            return;
        }
        this.trackNumber = trackNumber;
    }

    /**
     * Sets the track number from raw tag bytes holding a number as text.
     *
     * @param bytes   the raw track number bytes
     * @param charset the charset used to decode the bytes
     */
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

    /**
     * @return the format of the tag the metadata was read from, {@code null} if not set
     */
    public MetadataFormat getMetadataFormat() {
        return metadataFormat;
    }

    /**
     * @param metadataFormat the format of the tag the metadata was read from
     */
    public void setMetadataFormat(MetadataFormat metadataFormat) {
        this.metadataFormat = metadataFormat;
    }

    /**
     * Checks whether an artist with this exact name is already registered on the song.
     *
     * @param artistName the artist name to look for
     * @return {@code true} if the artist already exists
     */
    private boolean isArtistAlreadyInSong(String artistName) {
        return this.artists.stream()
                .anyMatch(artist -> Objects.equals(artist.getName(), artistName));
    }

    /**
     * Decodes bytes to a string, removing trailing null characters and surrounding spaces.
     *
     * @param bytes   the bytes to decode
     * @param charset the charset used to decode the bytes
     * @return the cleaned string
     */
    protected String transformBytesToStringAndTrim(byte[] bytes, Charset charset) {
        return new String(bytes, charset).replaceAll("\u0000+$", "").trim();
    }

    /**
     * Removes trailing null characters and surrounding spaces from a string.
     *
     * @param s the string to clean
     * @return the cleaned string
     */
    protected String trimAndRemoveInvalidChar(String s) {
        return s.replaceAll("\u0000+$", "").trim();
    }

}
