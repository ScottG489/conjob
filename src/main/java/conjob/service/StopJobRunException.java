package conjob.service;

public class StopJobRunException extends Exception {
    public StopJobRunException(Exception e) {
        super(e);
    }
}
