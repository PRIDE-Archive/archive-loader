package uk.ac.ebi.pride.prider.loader.assay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.data.controller.DataAccessController;
import uk.ac.ebi.pride.data.controller.DataAccessException;
import uk.ac.ebi.pride.data.controller.impl.ControllerImpl.MzIdentMLControllerImpl;
import uk.ac.ebi.pride.data.controller.impl.ControllerImpl.MzMLControllerImpl;
import uk.ac.ebi.pride.data.controller.impl.ControllerImpl.PrideXmlControllerImpl;
import uk.ac.ebi.pride.data.core.*;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.model.Param;
import uk.ac.ebi.pride.data.model.SampleMetaData;
import uk.ac.ebi.pride.data.util.MassSpecFileFormat;
import uk.ac.ebi.pride.prider.dataprovider.file.ProjectFileType;
import uk.ac.ebi.pride.prider.loader.exception.ProjectLoaderException;
import uk.ac.ebi.pride.prider.loader.util.Constant;
import uk.ac.ebi.pride.prider.loader.util.CvParamManager;
import uk.ac.ebi.pride.prider.loader.util.DataConversionUtil;
import uk.ac.ebi.pride.prider.repo.assay.Assay;
import uk.ac.ebi.pride.prider.repo.assay.AssaySampleCvParam;
import uk.ac.ebi.pride.prider.repo.assay.instrument.AnalyzerInstrumentComponent;
import uk.ac.ebi.pride.prider.repo.assay.instrument.DetectorInstrumentComponent;
import uk.ac.ebi.pride.prider.repo.assay.instrument.Instrument;
import uk.ac.ebi.pride.prider.repo.assay.instrument.SourceInstrumentComponent;

import java.io.File;
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

    private static final Logger logger = LoggerFactory.getLogger(AssayFactory.class);

    private static CvParamManager cvParamManager;

    public static void setCvParamManager(CvParamManager cvParamManager) {
        AssayFactory.cvParamManager = cvParamManager;
    }

    public static Assay makeAssay(DataFile dataFile) throws DataAccessException {

        if (cvParamManager == null) {
            throw new ProjectLoaderException("CvParamManager not set, cannot continue!");
        }

        Assay assay = new Assay();

        // accession
        String accession = dataFile.getAssayAccession();
        if (accession == null || "".equals(accession.trim())) {
            throw new ProjectLoaderException("Accession not set for assay: " + dataFile.getFile().getAbsolutePath());
        }
        assay.setAccession(accession);

        // sample
        List<AssaySampleCvParam> samples = new ArrayList<AssaySampleCvParam>();
        samples.addAll(DataConversionUtil.convertAssaySampleCvParams(assay, dataFile.getSampleMetaData().getMetaData(SampleMetaData.Type.SPECIES)));
        samples.addAll(DataConversionUtil.convertAssaySampleCvParams(assay, dataFile.getSampleMetaData().getMetaData(SampleMetaData.Type.TISSUE)));
        samples.addAll(DataConversionUtil.convertAssaySampleCvParams(assay, dataFile.getSampleMetaData().getMetaData(SampleMetaData.Type.CELL_TYPE)));
        samples.addAll(DataConversionUtil.convertAssaySampleCvParams(assay, dataFile.getSampleMetaData().getMetaData(SampleMetaData.Type.DISEASE)));
        assay.setSamples(samples);

        // quantification
        assay.setQuantificationMethods(DataConversionUtil.convertAssayQuantitationMethodCvParams(assay, dataFile.getSampleMetaData().getMetaData(SampleMetaData.Type.QUANTIFICATION_METHOD)));

        // experimental factor
        Set<Param> experimentFactor = dataFile.getSampleMetaData().getMetaData(SampleMetaData.Type.EXPERIMENTAL_FACTOR);
        if (!experimentFactor.isEmpty()) {
            //do it this way because experiment factor is stored as the value of a single user param
            assay.setExperimentalFactor(experimentFactor.iterator().next().getValue());
        }

        // load assay details from file
        loadAssayDetailsFromFile(assay, dataFile);

        return assay;
    }


    private static Assay loadAssayDetailsFromFile(Assay assay, DataFile dataFile) throws DataAccessException {

        // pride xml controller
        DataAccessController dataAccessController = null;

        logger.warn("Reading datafile: " + dataFile.getFile().getAbsolutePath());

        try {
            MassSpecFileFormat format = MassSpecFileFormat.checkFormat(dataFile.getFile());
            switch (format) {
                case PRIDE:
                    dataAccessController = new PrideXmlControllerImpl(dataFile.getFile());
                    break;
                case MZIDENTML:
                    MzIdentMLControllerImpl mzIdentMlController = new MzIdentMLControllerImpl(dataFile.getFile());
                    List<File> peakListFiles = new ArrayList<File>();
                    for (DataFile file : dataFile.getFileMappings()) {
                        if (file.getFileType().equals(ProjectFileType.PEAK)) {
                            peakListFiles.add(file.getFile());
                        }
                    }
                    mzIdentMlController.addMSController(peakListFiles);
                    dataAccessController = mzIdentMlController;
                    break;
                default:
                    throw new ProjectLoaderException("Could not get a DataAccessController for format: " + format.name());
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

            // softwares
            assay.setSoftwares(DataConversionUtil.convertSoftware(assay, resultFileScanner.getSoftwares()));

            // ptms
            assay.setPtms(DataConversionUtil.convertAssayPTMs(assay, resultFileScanner.getPtms()));

            //contact
            assay.setContacts(DataConversionUtil.convertContact(assay, dataAccessController.getPersonContacts()));

            //additional params
            assay.setAssayGroupCvParams(DataConversionUtil.convertAssayGroupCvParams(assay, dataAccessController.getAdditional()));
            assay.setAssayGroupUserParams(DataConversionUtil.convertAssayGroupUserParams(assay, dataAccessController.getAdditional()));

            //instrument
            Collection<Instrument> instruments = new HashSet<Instrument>();
            for (Instrument instrument : resultFileScanner.getInstruments()) {
                instrument.setAssay(assay);
                instruments.add(instrument);
            }
            assay.setInstruments(instruments);

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

        // instrument
        Set<Instrument> instruments = new HashSet<Instrument>();
        Collection<InstrumentConfiguration> instrumentConfigurations = dataAccessController.getInstrumentConfigurations();
        for (InstrumentConfiguration instrumentConfiguration : instrumentConfigurations) {
            Instrument instrument = new Instrument();

            //set instrument cv param
            instrument.setCvParam(cvParamManager.getCvParam(Constant.MS_INSTRUMENT_MODEL_AC));
            instrument.setValue(instrumentConfiguration.getId());

            //build instrument components
            instrument.setSources(new ArrayList<SourceInstrumentComponent>());
            instrument.setAnalyzers(new ArrayList<AnalyzerInstrumentComponent>());
            instrument.setDetectors(new ArrayList<DetectorInstrumentComponent>());
            int orderIndex = 1;
            //source
            for (InstrumentComponent source : instrumentConfiguration.getSource()) {
                SourceInstrumentComponent sourceInstrumentComponent = new SourceInstrumentComponent();
                sourceInstrumentComponent.setInstrument(instrument);
                sourceInstrumentComponent.setOrder(orderIndex++);
                sourceInstrumentComponent.setInstrumentComponentCvParams(DataConversionUtil.convertInstrumentComponentCvParam(sourceInstrumentComponent, source.getCvParams()));
                sourceInstrumentComponent.setInstrumentComponentUserParams(DataConversionUtil.convertInstrumentComponentUserParam(sourceInstrumentComponent, source.getUserParams()));
                instrument.getSources().add(sourceInstrumentComponent);
            }
            //analyzer
            for (InstrumentComponent analyzer : instrumentConfiguration.getAnalyzer()) {
                AnalyzerInstrumentComponent analyzerInstrumentComponent = new AnalyzerInstrumentComponent();
                analyzerInstrumentComponent.setInstrument(instrument);
                analyzerInstrumentComponent.setOrder(orderIndex++);
                analyzerInstrumentComponent.setInstrumentComponentCvParams(DataConversionUtil.convertInstrumentComponentCvParam(analyzerInstrumentComponent, analyzer.getCvParams()));
                analyzerInstrumentComponent.setInstrumentComponentUserParams(DataConversionUtil.convertInstrumentComponentUserParam(analyzerInstrumentComponent, analyzer.getUserParams()));
                instrument.getAnalyzers().add(analyzerInstrumentComponent);
            }
            //detector
            for (InstrumentComponent detector : instrumentConfiguration.getDetector()) {
                DetectorInstrumentComponent detectorInstrumentComponent = new DetectorInstrumentComponent();
                detectorInstrumentComponent.setInstrument(instrument);
                detectorInstrumentComponent.setOrder(orderIndex++);
                detectorInstrumentComponent.setInstrumentComponentCvParams(DataConversionUtil.convertInstrumentComponentCvParam(detectorInstrumentComponent, detector.getCvParams()));
                detectorInstrumentComponent.setInstrumentComponentUserParams(DataConversionUtil.convertInstrumentComponentUserParam(detectorInstrumentComponent, detector.getUserParams()));
                instrument.getDetectors().add(detectorInstrumentComponent);
            }
            //store instrument
            instruments.add(instrument);
            //todo - instrument level additional params are not captured and not inthe pride-r schema at this time
        }
        resultFileScanner.setInstruments(instruments);

        // software
        Set<Software> softwares = new HashSet<Software>();
        //todo - dataProcessing params are not captured as software params
        //todo - there is a 1-1 mapping for pride XML, but how to deal with mzidentml?
        //todo - will need to call getspectrumprotocol and getproteinprotocol on dataaccesscontroller to get params
        softwares.addAll(dataAccessController.getExperimentMetaData().getSoftwares());
        resultFileScanner.setSoftwares(softwares);

        // iterate over proteins
        Set<CvParam> ptms = new HashSet<CvParam>();
        Set<String> peptideSequences = new HashSet<String>();
        Set<Comparable> spectrumIds = new HashSet<Comparable>();
        Collection<Comparable> proteinIds = dataAccessController.getProteinIds();
        for (Comparable proteinId : proteinIds) {
            Collection<Comparable> peptideIds = dataAccessController.getPeptideIds(proteinId);
            for (Comparable peptideId : peptideIds) {
                // peptide
                Peptide peptide = dataAccessController.getPeptideByIndex(proteinId, peptideId);
                PeptideSequence peptideSequence = peptide.getPeptideSequence();
                peptideSequences.add(peptideSequence.getSequence());

                // ptm
                List<Modification> modifications = peptide.getModifications();
                for (Modification modification : modifications) {
                    List<uk.ac.ebi.pride.data.core.CvParam> cvParams = modification.getCvParams();
                    for (uk.ac.ebi.pride.data.core.CvParam cvParam : cvParams) {
                        if (cvParam.getCvLookupID().equalsIgnoreCase(Constant.PSI_MOD) || cvParam.getCvLookupID().equalsIgnoreCase(Constant.UNIMOD)) {
                            ptms.add(cvParam);
                        }
                    }
                }

                // spectrum
                if (peptide.getSpectrumIdentification() != null && peptide.getSpectrumIdentification().getSpectrum() != null) {
                    Spectrum spectrum = peptide.getSpectrumIdentification().getSpectrum();
                    spectrumIds.add(spectrum.getId());
                }

                if (peptide.getFragmentation() != null && peptide.getFragmentation().size() > 0) {
                    resultFileScanner.setMs2Annotation(true);
                }
            }
        }
        resultFileScanner.setNumberOfUniquePeptides(peptideSequences.size());
        resultFileScanner.setNumberOfIdentifiedSpectra(spectrumIds.size());
        resultFileScanner.setPtms(ptms);

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
        private Set<CvParam> ptms;
        private Set<Instrument> instruments;
        private Set<Software> softwares;

        private FileScanResults() {
            this.numberOfUniquePeptides = 0;
            this.numberOfIdentifiedSpectra = 0;
            this.ms2Annotation = false;
            this.chromatogram = false;
            this.ptms = new LinkedHashSet<CvParam>();
            this.instruments = new LinkedHashSet<Instrument>();
            this.softwares = new LinkedHashSet<Software>();
        }

        public int getNumberOfUniquePeptides() {
            return numberOfUniquePeptides;
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

        public Set<CvParam> getPtms() {
            return ptms;
        }

        public void setPtms(Set<CvParam> ptms) {
            this.ptms = ptms;
        }

        public Set<Instrument> getInstruments() {
            return instruments;
        }

        public void setInstruments(Set<Instrument> instruments) {
            this.instruments = instruments;
        }

        public Set<Software> getSoftwares() {
            return softwares;
        }

        public void setSoftwares(Set<Software> softwares) {
            this.softwares = softwares;
        }
    }

}
