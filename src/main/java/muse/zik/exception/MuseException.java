package muse.zik.exception;

public class MuseException extends RuntimeException {

    /**
     * @param message the detail message, prefixed with {@code [MUSE]}
     */
    public MuseException(String message) {
        super("[MUSE] " + message);
    }
}
