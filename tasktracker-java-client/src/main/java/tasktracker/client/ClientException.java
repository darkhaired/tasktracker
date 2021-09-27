package tasktracker.client;


public class ClientException extends Exception {
    ClientException(final Exception ex) {
        super(ex);
    }
}
