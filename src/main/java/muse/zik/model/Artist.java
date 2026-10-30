package muse.zik.model;

import java.util.UUID;

/**
 * An artist of a song, identified by a randomly generated unique id.
 */
public class Artist {

    private final String name;
    private final UUID id;

    /**
     * Creates an artist with a new random id.
     *
     * @param name the artist name
     */
    public Artist(String name) {
        this.name = name;
        this.id = UUID.randomUUID();
    }

    /**
     * @return the unique id of the artist, as a string
     */
    public String getId() {
        return this.id.toString();
    }

    /**
     * @return the name of the artist
     */
    public String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        return "Artist{" +
                "name='" + name + '\'' +
                ", id=" + id +
                '}';
    }
}
