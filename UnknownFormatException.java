package sitemap;

@SuppressWarnings("serial")
public class UnknownFormatException extends Exception {
    private String error;

    public UnknownFormatException() {
        super();
        error = "unknown";
    }

    public UnknownFormatException(String err) {
        super(err);
        error = err;
    }

    public String getError() {
        return error;
    }
}
