package muse.zik.enums;

/**
 * Format of the metadata (tags) embedded in an audio file.
 */
public enum MetadataFormat {
    /** ID3v1 tag, a fixed 128 bytes block at the end of the file. */
    ID3V1,
    /** ID3v2.2 tag, stored at the start of the file (3 characters frame ids). */
    ID3V2_2,
    /** ID3v2.3 tag, stored at the start of the file. */
    ID3V2_3,
    /** ID3v2.4 tag, stored at the start of the file (synchsafe frame sizes). */
    ID3V2_4,
    /** RIFF INFO chunk, used by WAV files. */
    RIFF_INFO,
    /** Vorbis comments, used by OGG and FLAC files. */
    VORBIS_COMMENT
}
