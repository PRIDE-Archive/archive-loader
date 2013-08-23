package uk.ac.ebi.pride.prider.loader.assay;

import uk.ac.ebi.pride.data.controller.DataAccessController;
import uk.ac.ebi.pride.data.controller.impl.ControllerImpl.MzIdentMLControllerImpl;
import uk.ac.ebi.pride.data.controller.impl.ControllerImpl.MzMLControllerImpl;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.util.MassSpecFileFormat;
import uk.ac.ebi.pride.prider.dataprovider.file.ProjectFileType;
import uk.ac.ebi.pride.prider.loader.file.FileFinder;
import uk.ac.ebi.pride.prider.repo.assay.Assay;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Assay file scanner for mzIdentML
 *
 * @author Rui Wang
 * @version $Id$
 */
public class MzIdentMLAssayFileScanner extends PrideXmlAssayFileScanner {

    public MzIdentMLAssayFileScanner(FileFinder fileFinder) {
        super(fileFinder);
    }

    @Override
    public AssayFileSummary scan(Assay assay, DataFile dataFile) throws IOException {
        AssayFileSummary fileSummary = super.scan(assay, dataFile);

        List<DataFile> fileMappings = dataFile.getFileMappings();
        for (DataFile fileMapping : fileMappings) {
            File mappedFile = fileFinder.find(fileMapping.getFile());
            if (mappedFile != null) {
                MassSpecFileFormat fileFormat = MassSpecFileFormat.checkFormat(mappedFile);
                if (MassSpecFileFormat.MZML.equals(fileFormat)) {
                    if (getMzMLSummary(fileSummary, mappedFile))
                        break;
                }
            }
        }

        return fileSummary;

    }

    private boolean getMzMLSummary(AssayFileSummary fileSummary, File mappedFile) {
        MzMLControllerImpl mzMLController = null;
        try {
            mzMLController = new MzMLControllerImpl(mappedFile);
            if (mzMLController.hasChromatogram()) {
                fileSummary.setChromatogram(true);
                mzMLController.close();
                return true;
            }
        } finally {
            if (mzMLController != null) {
                mzMLController.close();
            }
        }
        return false;
    }

    @Override
    protected DataAccessController getDataAccessController(DataFile dataFile) throws IOException {
        File mzIdentMLFile = fileFinder.find(dataFile.getFile());

        MzIdentMLControllerImpl mzIdentMlController = new MzIdentMLControllerImpl(mzIdentMLFile);

        List<File> peakListFiles = new ArrayList<File>();
        for (DataFile file : dataFile.getFileMappings()) {
            if (file.getFileType().equals(ProjectFileType.PEAK)) {
                File peakFile = fileFinder.find(file.getFile());
                peakListFiles.add(peakFile);
            }
        }
        mzIdentMlController.addMSController(peakListFiles);

        return mzIdentMlController;
    }
}
