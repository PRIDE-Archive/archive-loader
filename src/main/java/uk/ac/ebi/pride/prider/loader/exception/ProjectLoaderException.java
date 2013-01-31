package uk.ac.ebi.pride.prider.loader.exception;

/**
 * {@code ProjectLoadingException} will be thrown if there is an exception during the
 * submission loading process
 *
 * @author Rui Wang
 * @version $Id$
 */
public class ProjectLoaderException extends RuntimeException {

    public ProjectLoaderException() {
    }

    public ProjectLoaderException(String message) {
        super(message);
    }

    public ProjectLoaderException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProjectLoaderException(Throwable cause) {
        super(cause);
    }
}
