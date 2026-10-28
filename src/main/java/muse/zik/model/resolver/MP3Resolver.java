package muse.zik.model.resolver;

import muse.zik.exception.MuseException;
import muse.zik.interfaces.Resolver;
import muse.zik.model.audio.MP3File;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
                    //this.detectFormat(currentFile, filesBytes);
                    //char[] threeFirstBytes = new char[]{(char) filesBytes[filesBytes.length - 128], (char) filesBytes[filesBytes.length - 127], (char) filesBytes[filesBytes.length - 126]};
                    //if (!Arrays.equals(threeFirstBytes, "TAG".toCharArray())) continue;

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
     * @param file
     * @param bytes
     * @return
     */
    private MP3File readId3v1(File file, byte[] bytes) {
        MP3File mp3File = new MP3File(file);

        byte[] titleBytes = Arrays.copyOfRange(bytes, bytes.length - 125, bytes.length - 95);
        mp3File.setName(titleBytes);

        byte[] artistByte = Arrays.copyOfRange(bytes, bytes.length - 95, bytes.length - 65);
        mp3File.setArtist(artistByte);

        byte[] albumBytes = Arrays.copyOfRange(bytes, bytes.length - 65, bytes.length - 35);
        mp3File.setAlbum(albumBytes);

        byte[] year = Arrays.copyOfRange(bytes, bytes.length - 35, bytes.length - 31);
        mp3File.setYear(year);

        return mp3File;
    }

    private MP3File readId3v2(File file, byte[] bytes) {
        byte majorVersion = bytes[3];
        int headerFrameSize = 10;

        if (majorVersion == 2) {
            headerFrameSize = 6;
            // Format ID3v2.2
        } else if (majorVersion == 3) {
            // Format ID3v2.3
            System.out.println(Arrays.toString(Arrays.copyOfRange(bytes, 10, 10 + headerFrameSize)));
        } else if (majorVersion == 4) {
            // Format ID3v2.4
        }

        return new MP3File(file);
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
