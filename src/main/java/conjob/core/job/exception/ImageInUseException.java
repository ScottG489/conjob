package conjob.core.job.exception;

// Docker refused to remove an image because a container or another tag still needs it.
public class ImageInUseException extends RemoveImageException {
    public ImageInUseException(Exception e) {
        super(e);
    }
}
