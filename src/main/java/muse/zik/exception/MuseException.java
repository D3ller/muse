package muse.zik.exception;

public class MuseException extends RuntimeException {
    public MuseException(String message) {
        super("[MUSE] " + message);
    }
}
