package muse.zik.model.audio;

import muse.zik.enums.AudioFormat;
import java.io.File;

/**
 * An MP3 audio file and its metadata.
 */
public class MP3File extends AudioFile {

    /**
     * Creates an MP3 file whose metadata is left unset.
     *
     * @param file the original MP3 file
     */
    public MP3File(File file) {
        super(file, AudioFormat.MP3);
    }

    /**
     * Creates an MP3 file whose metadata is initialized to default "Unknown" values.
     *
     * @param file  the original MP3 file
     * @param clear must be {@code true}, otherwise an {@link IllegalArgumentException} is thrown
     */
    public MP3File (File file, boolean clear) {
        super(file, AudioFormat.MP3, clear);
    }

    @Override
    public String toString() {
        return "MP3File{" +
                "name='" + this.getTitle() + '\'' +
                ", artists=" + this.getArtists() +
                ", album='" + this.getAlbum() + '\'' +
                ", year=" + this.getYear() +
                ", trackNumber=" + this.getTrackNumber() +
                ", discNumber=" + this.getDiscNumber() +
                ", file=" + this.getOriginalFile().getName() +
                ", version" + this.getMetadataFormat() +
                '}';
    }
}
