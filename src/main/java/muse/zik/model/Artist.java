package muse.zik.model;

import java.util.UUID;

public class Artist {

    private final String name;
    private final UUID id;

    public Artist(String name) {
        this.name = name;
        this.id = UUID.randomUUID();
    }

    public String getId() {
        return this.id.toString();
    }

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
