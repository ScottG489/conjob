package conjob.core.job.exception;

public class JobRunException extends RuntimeException {
    public JobRunException(Exception e) {
        super(e);
    }
}
