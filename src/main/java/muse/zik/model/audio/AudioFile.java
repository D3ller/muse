package muse.zik.model.audio;

import java.io.File;

public abstract class AudioFile {

    private final File file;

    public AudioFile(File file) {
        this.file = file;
    }

    public File getOriginalFile() {
        return this.file;
    }
}
