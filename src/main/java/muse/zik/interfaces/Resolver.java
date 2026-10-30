package muse.zik.interfaces;

import muse.zik.model.audio.AudioFile;

import java.io.File;
import java.util.List;

/**
 * Reads the metadata of audio files and turns them into {@link AudioFile} objects.
 *
 * @param <T> the type of audio file produced by this resolver
 */
public interface Resolver<T extends AudioFile> {

    /**
     * Reads the metadata of a single file.
     *
     * @param file the audio file to read
     * @return the audio file filled with its metadata
     */
    T resolve(File file);

    /**
     * Reads the metadata of every supported file of a folder (not recursive).
     *
     * @param file the folder to scan
     * @return the audio files found in the folder
     */
    List<T> resolveFolder(File file);

}
