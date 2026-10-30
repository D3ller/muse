package muse.zik;

import muse.zik.model.audio.MP3File;
import muse.zik.model.resolver.MP3Resolver;

import java.io.File;
import java.util.List;

public class Main {


    static void main() {

        File file = new File("src/main/resources/song");
        List<MP3File> mp3Resolver = new MP3Resolver().resolveFolder(file);

        mp3Resolver.forEach(mp3File -> {
            System.out.println(mp3File.toString());
        });

    }

}
