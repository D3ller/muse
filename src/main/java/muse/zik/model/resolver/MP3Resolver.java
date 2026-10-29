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

    @Override
    public MP3File resolve(File file) {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            return this.detectFormat(file, inputStream.readAllBytes());
        } catch (IOException exception) {
            return null;
        }
    }

    @Override
    public List<MP3File> resolveFolder(File file) {
        List<MP3File> mp3Files = new ArrayList<>();

        try (Stream<Path> stream = Files.list(Path.of(file.getPath()))) {
            List<Path> filesPath = stream
                    .filter(path -> {
                        try {
                            return Objects.equals(Files.probeContentType(path), "audio/mpeg");
                        } catch (IOException e) {
                            throw new MuseException("Unknown file type");
                        }
                    }).toList();

            for (Path path : filesPath) {
                File currentFile = new File(path.toUri());
                try (FileInputStream inputStream = new FileInputStream(currentFile)) {
                    byte[] filesBytes = inputStream.readAllBytes();
                    MP3File mp3File = this.detectFormat(currentFile, filesBytes);
                    mp3Files.add(mp3File);
                }
            }
        } catch (IOException e) {
            System.err.println("Invalid path");
        }

        return mp3Files;
    }

    /**
     * Initialize metadata to Unknown for each field;
     * @param file Original file
     * @return MP3File
     */
    private MP3File initializeMetaData(File file) {
        return new MP3File(file, true);
    }

    /**
     * Detect id3v1 tag from the files bytes
     * @param file Original files
     * @param bytes Files bytes
     * @return MP3files with information filled
     */
    private MP3File readId3v1(File file, byte[] bytes) {

        MP3File mp3File = new MP3File(file);
        mp3File.setMetadataFormat(MetadataFormat.ID3V1);

        byte[] titleBytes = Arrays.copyOfRange(bytes, bytes.length - 125, bytes.length - 95);
        mp3File.setTitle(titleBytes, StandardCharsets.UTF_8);

        byte[] artistByte = Arrays.copyOfRange(bytes, bytes.length - 95, bytes.length - 65);
        mp3File.addArtist(artistByte, StandardCharsets.UTF_8);

        byte[] albumBytes = Arrays.copyOfRange(bytes, bytes.length - 65, bytes.length - 35);
        mp3File.setAlbum(albumBytes, StandardCharsets.UTF_8);

        byte[] year = Arrays.copyOfRange(bytes, bytes.length - 35, bytes.length - 31);
        mp3File.setYear(year, StandardCharsets.UTF_8);

        return mp3File;
    }

    private MP3File readId3v2(File file, byte[] bytes) {
        byte majorVersion = bytes[3];
        byte[] sizeBytes = Arrays.copyOfRange(bytes, 6, 10);
        int headerFrameSize = 10;
        int frameIdSize = 4;

        int tagSize = ((sizeBytes[0] & 0x7F) << 21)
                | ((sizeBytes[1] & 0x7F) << 14)
                | ((sizeBytes[2] & 0x7F) << 7)
                |  (sizeBytes[3] & 0x7F);

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
        }

        int up;

        for (int i = 10; i < tagEnd; i += up) {

            var attributeName = new String(Arrays.copyOfRange(bytes, i, i + frameIdSize), StandardCharsets.UTF_8).trim();
            ByteBuffer buffer = ByteBuffer.wrap(bytes);

            int frameSize;

            if (majorVersion == 2) {
                frameSize = ((bytes[i + 3] & 0xFF) << 16)
                        | ((bytes[i + 4] & 0xFF) << 8)
                        |  (bytes[i + 5] & 0xFF);
            } else {
                buffer.position(i + frameIdSize);
                frameSize = buffer.getInt();
            }

            byte[] frameId = Arrays.copyOfRange(bytes, i, i + frameIdSize);
            if (Arrays.equals(frameId, new byte[frameIdSize])) {
                break;
            }

            var content = Arrays.copyOfRange(bytes, i + headerFrameSize, i + frameSize + headerFrameSize);
            if(content.length == 0) {
                up = frameSize + headerFrameSize;
                continue;
            }

            if (!attributeName.startsWith("T")) {
                up = frameSize + headerFrameSize;
                continue;
            }

            Charset charset = switch (content[0]) {
                case 0 -> StandardCharsets.ISO_8859_1;
                case 1 -> StandardCharsets.UTF_16;
                case 2 -> StandardCharsets.UTF_16BE;
                case 3 -> StandardCharsets.UTF_8;
                default -> throw new IllegalArgumentException("Unknown text encoding " + content[0]);
            };

            byte[] textBytes = Arrays.copyOfRange(content, 1, content.length);

            switch (attributeName) {
                case "TIT2", "TT2" -> currentFile.setTitle(textBytes, charset);
                case "TPE1", "TP1" -> currentFile.addArtist(textBytes, charset);
                case "TALB", "TAL" -> currentFile.setAlbum(textBytes, charset);
                case "TYER", "TDRC", "TYE" -> currentFile.setYear(textBytes, charset);
            }

            up = frameSize + headerFrameSize;
        }
        System.out.println(currentFile.toString());
        return currentFile;
    }

    private MP3File detectFormat(File file, byte[] bytes) {
        if (bytes.length < 3) {
            return this.initializeMetaData(file);
        }
        if (new String(Arrays.copyOfRange(bytes, 0, 3), StandardCharsets.ISO_8859_1).equals("ID3")) {
            return this.readId3v2(file, bytes);
        } else if (bytes.length <= 128) {
            return this.initializeMetaData(file);
        } else if (Arrays.equals(new char[]{(char) bytes[bytes.length - 128], (char) bytes[bytes.length - 127], (char) bytes[bytes.length - 126]}, "TAG".toCharArray())) {
            return this.readId3v1(file, bytes);
        } else {
            return this.initializeMetaData(file);
        }
    }
}
