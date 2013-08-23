package uk.ac.ebi.pride.prider.loader.assay;

import uk.ac.ebi.pride.data.util.MassSpecFileFormat;
import uk.ac.ebi.pride.prider.loader.file.FileFinder;

/**
 * @author Rui Wang
 * @version $Id$
 */
public class AssayFileScannerFactory {
    private static final AssayFileScannerFactory assayFileScannerFactory = new AssayFileScannerFactory();

    private AssayFileScannerFactory() {
    }

    public static AssayFileScannerFactory getInstance() {
        return assayFileScannerFactory;
    }

    public AssayFileScanner getAssayFileScanner(MassSpecFileFormat fileFormat, FileFinder fileFinder) {
        switch (fileFormat) {
            case PRIDE:
                return new PrideXmlAssayFileScanner(fileFinder);
            case MZIDENTML:
                return new MzIdentMLAssayFileScanner(fileFinder);
            default:
                return null;
        }
    }
}
