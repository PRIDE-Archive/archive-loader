package uk.ac.ebi.pride.prider.loader.assay;

import uk.ac.ebi.pride.data.controller.DataAccessController;
import uk.ac.ebi.pride.data.controller.DataAccessException;
import uk.ac.ebi.pride.data.controller.impl.ControllerImpl.MzIdentMLControllerImpl;
import uk.ac.ebi.pride.data.controller.impl.ControllerImpl.MzMLControllerImpl;
import uk.ac.ebi.pride.data.controller.impl.ControllerImpl.PrideXmlControllerImpl;
import uk.ac.ebi.pride.data.core.ExperimentMetaData;
import uk.ac.ebi.pride.data.core.Peptide;
import uk.ac.ebi.pride.data.core.PeptideSequence;
import uk.ac.ebi.pride.data.core.Spectrum;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.model.Param;
import uk.ac.ebi.pride.data.model.SampleMetaData;
import uk.ac.ebi.pride.data.util.MassSpecFileFormat;
import uk.ac.ebi.pride.prider.core.assay.Assay;
import uk.ac.ebi.pride.prider.core.iconfig.Instrument;
import uk.ac.ebi.pride.prider.core.param.CvParam;
import uk.ac.ebi.pride.prider.loader.util.DataConversionUtil;

import java.util.*;

/**
 * {@code AssayFactory} is a factory class that make assay object based on different data source
 * <p/>
 * At the moment, only pride xml and mzIdentML is supported
 *
 * @author Rui Wang
 * @version $Id$
 */
public final class AssayFactory {


    public static Assay makeAssay(DataFile dataFile, SampleMetaData sampleMetaData) throws DataAccessException {
        Assay assay = new Assay();

        // accession
        assay.setAccession(dataFile.getAssayAccession());

        // sample
        List<CvParam> samples = new ArrayList<CvParam>();
        samples.addAll(DataConversionUtil.convertCvParams(sampleMetaData.getSpecies()));
        samples.addAll(DataConversionUtil.convertCvParams(sampleMetaData.getTissues()));
        samples.addAll(DataConversionUtil.convertCvParams(sampleMetaData.getCellTypes()));
        samples.addAll(DataConversionUtil.convertCvParams(sampleMetaData.getDiseases()));
        assay.setSamples(samples);

        // quantification
        assay.setQuantificationMethods(DataConversionUtil.convertCvParams(sampleMetaData.getQuantificationMethods()));

        // instrument
        List<Instrument> instruments = new ArrayList<Instrument>();
        List<Param> instrumentModels = sampleMetaData.getInstruments();
        for (Param instrumentModel : instrumentModels) {
            Instrument instrument = new Instrument();
            instrument.setModel(DataConversionUtil.convertParam(instrumentModel));
        }
        assay.setInstruments(instruments);

        // experimental factor
        assay.setExperimentalFactor(sampleMetaData.getExperimentalFactor());

        // load assay details from file
        loadAssayDetailsFromFile(assay, dataFile);

        return assay;
    }


    private static Assay loadAssayDetailsFromFile(Assay assay, DataFile dataFile) throws DataAccessException {

        // pride xml controller
        DataAccessController dataAccessController = null;

        try {
            MassSpecFileFormat format = MassSpecFileFormat.checkFormat(dataFile.getFile());
            switch (format) {
                case PRIDE:
                    dataAccessController = new PrideXmlControllerImpl(dataFile.getFile());
                    break;
                case MZIDENTML:
                    dataAccessController = new MzIdentMLControllerImpl(dataFile.getFile());
                    break;
            }

            // get experiment metadata
            ExperimentMetaData experimentMetaData = dataAccessController.getExperimentMetaData();

            // set assay title
            assay.setTitle(experimentMetaData.getName());

            // set short label
            assay.setShortLabel(experimentMetaData.getShortLabel());

            // protein count
            assay.setProteinCount(dataAccessController.getNumberOfProteins());

            // peptide count
            assay.setPeptideCount(dataAccessController.getNumberOfPeptides());

            // scan result file
            FileScanResults resultFileScanner = scanResultFile(dataFile, dataAccessController);

            // unique peptide count
            assay.setUniquePeptideCount(resultFileScanner.getNumberOfUniquePeptides());

            // identified spectrum count
            assay.setIdentifiedSpectrumCount(resultFileScanner.getNumberOfIdentifiedSpectra());

            // total spectrum count
            assay.setTotalSpectrumCount(dataAccessController.getNumberOfSpectra());

            // ms2 annotation
            assay.setMs2Annotation(resultFileScanner.hasMs2Annotation());

            // chromatogram
            assay.setChromatogram(resultFileScanner.hasChromatogram());

        } finally {
            if (dataAccessController != null) {
                dataAccessController.close();
            }
        }


        return assay;
    }

    /**
     * Scan result file for statistics
     */
    private static FileScanResults scanResultFile(DataFile dataFile, DataAccessController dataAccessController) throws DataAccessException {
        FileScanResults resultFileScanner = new FileScanResults();

        // iterate over proteins
        Set<PeptideSequence> peptideSequences = new HashSet<PeptideSequence>();
        Set<Comparable> spectrumIds = new HashSet<Comparable>();
        Collection<Comparable> proteinIds = dataAccessController.getProteinIds();
        for (Comparable proteinId : proteinIds) {
            Collection<Comparable> peptideIds = dataAccessController.getPeptideIds(proteinId);
            for (Comparable peptideId : peptideIds) {
                // peptide
                Peptide peptide = dataAccessController.getPeptideByIndex(proteinId, peptideId);
                PeptideSequence peptideSequence = peptide.getPeptideSequence();
                peptideSequences.add(peptideSequence);

                // spectrum
                Spectrum spectrum = peptide.getSpectrumIdentification().getSpectrum();
                spectrumIds.add(spectrum.getId());

                if (peptide.getFragmentation() != null && peptide.getFragmentation().size() > 0) {
                    resultFileScanner.setMs2Annotation(true);
                }
            }
        }
        resultFileScanner.setNumberOfUniquePeptides(peptideSequences.size());
        resultFileScanner.setNumberOfIdentifiedSpectra(spectrumIds.size());

        // check chromatogram
        List<DataFile> fileMappings = dataFile.getFileMappings();
        for (DataFile fileMapping : fileMappings) {
            MassSpecFileFormat fileFormat = MassSpecFileFormat.checkFormat(fileMapping.getFile());
            if (MassSpecFileFormat.MZML.equals(fileFormat)) {
                MzMLControllerImpl mzMLController = null;
                try {
                    mzMLController = new MzMLControllerImpl(dataFile.getFile());
                    if (mzMLController.hasChromatogram()) {
                        resultFileScanner.setChromatogram(true);
                        mzMLController.close();
                        break;
                    }
                } finally {
                    if (mzMLController != null) {
                        mzMLController.close();
                    }
                }
            }
        }

        return resultFileScanner;
    }

    /**
     * File scan results
     */
    private static class FileScanResults {
        private int numberOfUniquePeptides;
        private int numberOfIdentifiedSpectra;
        private boolean ms2Annotation;
        private boolean chromatogram;

        private FileScanResults() {
            this(0, 0, false, false);
        }

        private FileScanResults(int numberOfUniquePeptides,
                                int numberOfIdentifiedSpectra,
                                boolean ms2Annotation,
                                boolean chromatogram) {
            this.numberOfUniquePeptides = numberOfUniquePeptides;
            this.numberOfIdentifiedSpectra = numberOfIdentifiedSpectra;
            this.ms2Annotation = ms2Annotation;
            this.chromatogram = chromatogram;
        }

        public int getNumberOfUniquePeptides() {
            return numberOfUniquePeptides;
        }

        public void incrementNumberOfUqniePeptides() {
            numberOfUniquePeptides++;
        }

        public void setNumberOfUniquePeptides(int numberOfUniquePeptides) {
            this.numberOfUniquePeptides = numberOfUniquePeptides;
        }

        public int getNumberOfIdentifiedSpectra() {
            return numberOfIdentifiedSpectra;
        }

        public void setNumberOfIdentifiedSpectra(int numberOfIdentifiedSpectra) {
            this.numberOfIdentifiedSpectra = numberOfIdentifiedSpectra;
        }

        public void incrementNumberOfIdentifiedSpectra() {
            numberOfIdentifiedSpectra++;
        }

        public boolean hasMs2Annotation() {
            return ms2Annotation;
        }

        public void setMs2Annotation(boolean ms2Annotation) {
            this.ms2Annotation = ms2Annotation;
        }

        public boolean hasChromatogram() {
            return chromatogram;
        }

        public void setChromatogram(boolean chromatogram) {
            this.chromatogram = chromatogram;
        }
    }

}
