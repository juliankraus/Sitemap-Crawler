package sitemap;

@SuppressWarnings("serial")
public class ProtocolException extends Exception {
    public ProtocolException() {
        super();
    }

    public ProtocolException(String message) {
        super(message);
    }

    public ProtocolException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProtocolException(Throwable cause) {
        super(cause);
    }
}
