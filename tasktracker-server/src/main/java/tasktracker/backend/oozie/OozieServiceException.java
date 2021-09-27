package tasktracker.backend.oozie;

public class OozieServiceException extends Exception {

    public OozieServiceException() {
    }

    public OozieServiceException(String s) {
        super(s);
    }

    public OozieServiceException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public OozieServiceException(Throwable throwable) {
        super(throwable);
    }
}
