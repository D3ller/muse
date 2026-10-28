package muse.zik.interfaces;

import muse.zik.model.audio.AudioFile;

import java.io.File;
import java.util.List;

public interface Resolver<T extends AudioFile> {

    T resolve(File file);

    List<T> resolveFolder(File file);

}
