package muse.zik.model.audio;

import muse.zik.enums.AudioFormat;
import java.io.File;

public class MP3File extends AudioFile {

    public MP3File(File file) {
        super(file, AudioFormat.MP3);
    }

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
