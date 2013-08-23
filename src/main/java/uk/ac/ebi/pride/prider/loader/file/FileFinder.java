package uk.ac.ebi.pride.prider.loader.file;

import java.io.File;
import java.io.IOException;

/**
 * Interface find a file using a given file
 *
 * NOTE: it could be the same file
 *
 * @author Rui Wang
 * @version $Id$
 */
public interface FileFinder {

    File find(File file) throws IOException;
}
