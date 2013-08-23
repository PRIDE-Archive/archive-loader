package uk.ac.ebi.pride.prider.loader.exception;

/**
 * {@code ProjectLoadingException} will be thrown if there is an exception during the
 * submission loading process
 *
 * @author Rui Wang
 * @version $Id$
 */
public class SubmissionLoaderException extends RuntimeException {

    public SubmissionLoaderException() {
    }

    public SubmissionLoaderException(String message) {
        super(message);
    }

    public SubmissionLoaderException(String message, Throwable cause) {
        super(message, cause);
    }

    public SubmissionLoaderException(Throwable cause) {
        super(cause);
    }
}
