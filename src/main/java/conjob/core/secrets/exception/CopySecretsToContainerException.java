package conjob.core.secrets.exception;

import conjob.core.job.exception.JobRunException;

public class CopySecretsToContainerException extends JobRunException {
    public CopySecretsToContainerException(Exception cause) {
        super(cause);
    }
}
