package muse.zik.model.resolver;

import muse.zik.enums.MetadataFormat;
import muse.zik.exception.MuseException;
import muse.zik.interfaces.Resolver;
import muse.zik.model.audio.MP3File;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Resolver for MP3 using id3v1 and id3v2
 */
public class MP3Resolver implements Resolver<MP3File> {

    /**
     * Reads the metadata of a single MP3 file.
     *
     * @param file the MP3 file to read
     * @return the MP3 file filled with its metadata, or with "Unknown" values if no tag is found
     * @throws MuseException if the file cannot be read or its tag is invalid
     */
    @Override
    public MP3File resolve(File file) throws MuseException {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            return this.detectFormat(file, inputStream.readAllBytes());
        } catch (IOException exception) {
            throw new MuseException("No file detected");
        }
    }

    /**
     * Reads the metadata of every MP3 file of a folder. Sub folders are ignored.
     *
     * @param file the folder to scan
     * @return the MP3 files found in the folder
     * @throws MuseException if the folder path is invalid
     */
    @Override
    public List<MP3File> resolveFolder(File file) throws MuseException {
        List<MP3File> mp3Files = new ArrayList<>();

        try (Stream<Path> stream = Files.list(Path.of(file.getPath()))) {
            List<Path> filesPath = stream
                    .filter(Files::isRegularFile)
                    .filter(this::isMp3)
                    .toList();

            for (Path path : filesPath) {
                File currentFile = path.toFile();
                try (FileInputStream inputStream = new FileInputStream(currentFile)) {
                    byte[] filesBytes = inputStream.readAllBytes();
                    MP3File mp3File = this.detectFormat(currentFile, filesBytes);
                    mp3Files.add(mp3File);
                }
            }
        } catch (IOException e) {
            throw new MuseException("Invalid path");
        }

        return mp3Files;
    }

    /**
     * Check if a file is an MP3. The extension is checked first because
     * {@link Files#probeContentType(Path)} depends on the OS and may not detect every MP3.
     *
     * @param path the file to check
     * @return {@code true} if the file has the .mp3 extension or is detected as {@code audio/mpeg}
     */
    private boolean isMp3(Path path) {
        if (path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".mp3")) {
            return true;
        }
        try {
            return "audio/mpeg".equals(Files.probeContentType(path));
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Initialize metadata to Unknown for each field;
     *
     * @param file Original file
     * @return MP3File
     */
    private MP3File initializeMetaData(File file) {
        return new MP3File(file, true);
    }

    /**
     * Detect id3v1 tag from the files bytes
     *
     * @param file  Original files
     * @param bytes Files bytes
     * @return MP3files with information filled
     */
    private MP3File readId3v1(File file, byte[] bytes) {

        MP3File mp3File = new MP3File(file);
        mp3File.setMetadataFormat(MetadataFormat.ID3V1);

        byte[] titleBytes = Arrays.copyOfRange(bytes, bytes.length - 125, bytes.length - 95);
        mp3File.setTitle(titleBytes, StandardCharsets.ISO_8859_1);

        byte[] artistByte = Arrays.copyOfRange(bytes, bytes.length - 95, bytes.length - 65);
        mp3File.addArtist(artistByte, StandardCharsets.ISO_8859_1);

        byte[] albumBytes = Arrays.copyOfRange(bytes, bytes.length - 65, bytes.length - 35);
        mp3File.setAlbum(albumBytes, StandardCharsets.ISO_8859_1);

        byte[] year = Arrays.copyOfRange(bytes, bytes.length - 35, bytes.length - 31);
        mp3File.setYear(year, StandardCharsets.ISO_8859_1);

        return mp3File;
    }

    /**
     * Read the id3v2 tag (2.2, 2.3 or 2.4) located at the start of the file.
     * <p>
     * Only text frames (starting with {@code T}) and the comment frame are read:
     * title, artist, album, album artist, year, track number, disc number and comment.
     *
     * @param file  Original file
     * @param bytes Files bytes
     * @return MP3File with information filled
     * @throws MuseException if the version is unsupported or the tag/frame sizes are invalid
     */
    private MP3File readId3v2(File file, byte[] bytes) {
        if (bytes.length < 10) {
            throw new MuseException("Incomplete ID3 header.");
        }

        byte majorVersion = bytes[3];
        int headerFrameSize = 10;
        int frameIdSize = 4;

        int tagSize = this.readSynchsafeInt(bytes, 6);

        if (tagSize > bytes.length - 10) {
            throw new MuseException("ID3 tag exceeds file boundaries.");
        }

        int tagEnd = 10 + tagSize;
        var currentFile = new MP3File(file, true);

        if (majorVersion == 2) {
            // ID3v2.2 format
            headerFrameSize = 6;
            frameIdSize = 3;
            currentFile.setMetadataFormat(MetadataFormat.ID3V2_2);
        } else if (majorVersion == 3) {
            // ID3v2.3 format
            currentFile.setMetadataFormat(MetadataFormat.ID3V2_3);
        } else if (majorVersion == 4) {
            // ID3v2.4 format
            currentFile.setMetadataFormat(MetadataFormat.ID3V2_4);
        } else {
            throw new MuseException("Invalid ID3 version.");
        }

        int i = this.findFirstFrameOffset(bytes, majorVersion, tagEnd);

        while (i < tagEnd) {

            // Frame ids only use A-Z and 0-9, so a zero byte means the padding has started.
            // It must be checked before reading a frame header, padding has none.
            if (bytes[i] == 0) {
                if (!this.isOnlyZeros(bytes, i, tagEnd)) {
                    throw new MuseException("Invalid ID3 padding.");
                }
                break;
            }

            if (i + headerFrameSize > tagEnd) {
                throw new MuseException("Incomplete ID3 frame header.");
            }

            var attributeName = new String(bytes, i, frameIdSize, StandardCharsets.ISO_8859_1);
            if (!attributeName.matches("[A-Z0-9]+")) {
                throw new MuseException("Invalid ID3 frame id.");
            }

            int frameSize = switch (majorVersion) {
                case 2 -> ((bytes[i + 3] & 0xFF) << 16)
                        | ((bytes[i + 4] & 0xFF) << 8)
                        | (bytes[i + 5] & 0xFF);
                case 3 -> ByteBuffer.wrap(bytes, i + 4, 4).getInt();
                default -> this.readSynchsafeInt(bytes, i + 4);
            };

            if (frameSize < 0 || frameSize > tagEnd - i - headerFrameSize) {
                throw new MuseException("Invalid ID3 frame size.");
            }

            var content = Arrays.copyOfRange(bytes, i + headerFrameSize, i + frameSize + headerFrameSize);
            i += headerFrameSize + frameSize;

            if (content.length == 0) {
                continue;
            }

            if (!attributeName.startsWith("T") && !attributeName.equals("COMM") && !attributeName.equals("COM")) {
                continue;
            }

            Charset charset = this.resolveTextEncoding(content[0], majorVersion);

            byte[] textBytes = Arrays.copyOfRange(content, 1, content.length);

            switch (attributeName) {
                case "COMM", "COM" -> currentFile.setComment(this.extractComment(textBytes, charset), charset);
                case "TIT2", "TT2" -> currentFile.setTitle(textBytes, charset);
                case "TPE1", "TP1" -> currentFile.addArtist(textBytes, charset);
                case "TALB", "TAL" -> currentFile.setAlbum(textBytes, charset);
                case "TPE2", "TP2" -> currentFile.setAlbumArtist(textBytes, charset);
                case "TYER", "TDRC", "TYE" -> currentFile.setYear(textBytes, charset);
                case "TRCK", "TRK" -> currentFile.setTrackNumber(textBytes, charset);
                case "TPOS", "TPA" -> currentFile.setDiscNumber(textBytes, charset);
            }
        }
        return currentFile;
    }

    /**
     * Check the flags of the ID3 header and find where the first frame starts.
     * <p>
     * Unsynchronisation and ID3v2.2 compression change how the frames are stored and are not supported,
     * so the tag is rejected instead of being parsed incorrectly. The extended header (v2.3/v2.4) is skipped.
     *
     * @param bytes        Files bytes
     * @param majorVersion ID3v2 major version (2, 3 or 4)
     * @param tagEnd       offset of the end of the tag
     * @return offset of the first frame
     * @throws MuseException if a flag is unknown or unsupported, or the extended header is invalid
     */
    private int findFirstFrameOffset(byte[] bytes, byte majorVersion, int tagEnd) {
        int flags = bytes[5] & 0xFF;

        int definedFlags = switch (majorVersion) {
            case 2 -> 0x80 | 0x40;               // unsynchronisation, compression
            case 3 -> 0x80 | 0x40 | 0x20;        // unsynchronisation, extended header, experimental
            default -> 0x80 | 0x40 | 0x20 | 0x10; // same as v2.3 + footer
        };

        if ((flags & ~definedFlags) != 0) {
            throw new MuseException("Unknown ID3 header flags.");
        }
        if ((flags & 0x80) != 0) {
            throw new MuseException("ID3 unsynchronisation is not supported.");
        }
        if (majorVersion == 2 && (flags & 0x40) != 0) {
            throw new MuseException("ID3v2.2 compression is not supported.");
        }
        if (majorVersion == 2 || (flags & 0x40) == 0) {
            return 10;
        }

        if (tagEnd - 10 < 4) {
            throw new MuseException("Incomplete ID3 extended header.");
        }

        // v2.3: the size excludes its own 4 bytes. v2.4: synchsafe size including itself.
        int extendedHeaderEnd = majorVersion == 3
                ? 14 + ByteBuffer.wrap(bytes, 10, 4).getInt()
                : 10 + this.readSynchsafeInt(bytes, 10);

        if (extendedHeaderEnd < 14 || extendedHeaderEnd > tagEnd) {
            throw new MuseException("Invalid ID3 extended header size.");
        }
        return extendedHeaderEnd;
    }

    /**
     * Read a 4 bytes synchsafe integer (7 bits per byte).
     *
     * @param bytes  Files bytes
     * @param offset offset of the first byte
     * @return the decoded integer
     * @throws MuseException if a byte has its most significant bit set
     */
    private int readSynchsafeInt(byte[] bytes, int offset) {
        int value = 0;
        for (int k = offset; k < offset + 4; k++) {
            if ((bytes[k] & 0x80) != 0) {
                throw new MuseException("Invalid synchsafe integer.");
            }
            value = (value << 7) | bytes[k];
        }
        return value;
    }

    /**
     * Convert the encoding byte of a text frame to a charset.
     * ID3v2.2 and v2.3 only allow ISO-8859-1 (0) and UTF-16 (1), UTF-16BE (2) and UTF-8 (3) were added in v2.4.
     *
     * @param encoding     encoding byte of the frame
     * @param majorVersion ID3v2 major version (2, 3 or 4)
     * @return the charset of the text
     * @throws MuseException if the encoding is unknown or not allowed for this version
     */
    private Charset resolveTextEncoding(byte encoding, byte majorVersion) {
        if (encoding > 1 && majorVersion < 4) {
            throw new MuseException("Text encoding " + encoding + " is not allowed in ID3v2." + majorVersion);
        }
        return switch (encoding) {
            case 0 -> StandardCharsets.ISO_8859_1;
            case 1 -> StandardCharsets.UTF_16;
            case 2 -> StandardCharsets.UTF_16BE;
            case 3 -> StandardCharsets.UTF_8;
            default -> throw new MuseException("Unknown text encoding " + encoding);
        };
    }

    /**
     * @param bytes Files bytes
     * @param from  first offset (inclusive)
     * @param to    last offset (exclusive)
     * @return {@code true} if every byte between {@code from} and {@code to} is zero
     */
    private boolean isOnlyZeros(byte[] bytes, int from, int to) {
        for (int k = from; k < to; k++) {
            if (bytes[k] != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Detect which tag the file uses. Only the first format found is used:
     * id3v2 if the file starts with "ID3", otherwise id3v1 if a "TAG" block ends the file,
     * otherwise the metadata is initialized to Unknown.
     *
     * @param file  Original file
     * @param bytes Files bytes
     * @return MP3File with information filled
     */
    private MP3File detectFormat(File file, byte[] bytes) {
        if (bytes.length < 3) {
            return this.initializeMetaData(file);
        }
        if (new String(Arrays.copyOfRange(bytes, 0, 3), StandardCharsets.ISO_8859_1).equals("ID3")) {
            return this.readId3v2(file, bytes);
        } else if (bytes.length <= 128) {
            return this.initializeMetaData(file);
        } else if (bytes[bytes.length - 128] == 'T'
                && bytes[bytes.length - 127] == 'A'
                && bytes[bytes.length - 126] == 'G') {
            return this.readId3v1(file, bytes);
        } else {
            return this.initializeMetaData(file);
        }
    }

    private byte[] extractComment(byte[] bytes, Charset charset) {
        if (bytes.length < 4) {
            throw new MuseException("Invalid comment frame.");
        }

        int separatorSize = charset.equals(StandardCharsets.UTF_16)
                || charset.equals(StandardCharsets.UTF_16BE) ? 2 : 1;

        int commentStart = -1;

        for (int i = 3; i <= bytes.length - separatorSize; i += separatorSize) {
            boolean separator = bytes[i] == 0;

            if (separatorSize == 2) {
                separator = separator && bytes[i + 1] == 0;
            }

            if (separator) {
                commentStart = i + separatorSize;
                break;
            }
        }

        if (commentStart == -1) {
            throw new MuseException("Comment description separator not found.");
        }

        return Arrays.copyOfRange(bytes, commentStart, bytes.length);
    }
}
