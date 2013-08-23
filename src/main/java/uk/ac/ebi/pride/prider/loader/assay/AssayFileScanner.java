package uk.ac.ebi.pride.prider.loader.assay;

import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.prider.repo.assay.Assay;

import java.io.IOException;

/**
 * Interface for enriching an assay using the metadata from a given data file
 *
 * @author Rui Wang
 * @version $Id$
 */
public interface AssayFileScanner {

    AssayFileSummary scan(Assay assay, DataFile dataFile) throws IOException;
}
