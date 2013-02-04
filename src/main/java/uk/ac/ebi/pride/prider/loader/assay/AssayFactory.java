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
import uk.ac.ebi.pride.prider.loader.util.Constant;
import uk.ac.ebi.pride.prider.loader.util.CvParamManager;
import uk.ac.ebi.pride.prider.loader.util.DataConversionUtil;
import uk.ac.ebi.pride.prider.repo.assay.Assay;
import uk.ac.ebi.pride.prider.repo.assay.AssaySample;
import uk.ac.ebi.pride.prider.repo.instrument.AnalyzerInstrumentComponent;
import uk.ac.ebi.pride.prider.repo.instrument.DetectorInstrumentComponent;
import uk.ac.ebi.pride.prider.repo.instrument.Instrument;
import uk.ac.ebi.pride.prider.repo.instrument.SourceInstrumentComponent;

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

    public static Assay makeAssay(DataFile dataFile) throws DataAccessException {
        Assay assay = new Assay();

        // accession
        assay.setAccession(dataFile.getAssayAccession());

        // sample
        List<AssaySample> samples = new ArrayList<AssaySample>();
        samples.addAll(DataConversionUtil.convertAssaySampleCvParams(assay, dataFile.getSampleMetaData().getMetaData(SampleMetaData.Type.SPECIES)));
        samples.addAll(DataConversionUtil.convertAssaySampleCvParams(assay, dataFile.getSampleMetaData().getMetaData(SampleMetaData.Type.TISSUE)));
        samples.addAll(DataConversionUtil.convertAssaySampleCvParams(assay, dataFile.getSampleMetaData().getMetaData(SampleMetaData.Type.CELL_TYPE)));
        samples.addAll(DataConversionUtil.convertAssaySampleCvParams(assay, dataFile.getSampleMetaData().getMetaData(SampleMetaData.Type.DISEASE)));
        assay.setSamples(samples);

        // quantification
        assay.setQuantificationMethods(DataConversionUtil.convertAssayQuantitationMethodCvParams(assay, dataFile.getSampleMetaData().getMetaData(SampleMetaData.Type.QUANTIFICATION_METHOD)));

        // instrument
        List<Instrument> instruments = new ArrayList<Instrument>();
        Set<Param> instrumentModels = dataFile.getSampleMetaData().getMetaData(SampleMetaData.Type.INSTRUMENT);
        for (Param instrumentModel : instrumentModels) {
            //check to see if the instrument param is already in PRIDE-R
            if (instrumentModel instanceof uk.ac.ebi.pride.data.model.CvParam) {
                uk.ac.ebi.pride.data.model.CvParam cvParam = (uk.ac.ebi.pride.data.model.CvParam) instrumentModel;
                uk.ac.ebi.pride.prider.repo.param.CvParam repoParam = CvParamManager.getInstance().getCvParam(cvParam.getAccession());
                //if param isn't already seen in db, store it
                if (repoParam == null) {
                    CvParamManager.getInstance().putCvParam(cvParam.getCvLabel(), cvParam.getAccession(), cvParam.getName());
                    repoParam = CvParamManager.getInstance().getCvParam(cvParam.getAccession());
                }
                Instrument instrument = new Instrument();
                instrument.setCvParam(repoParam);
                instrument.setValue(cvParam.getValue());
                //store assay link
                //todo - this might cause data duplication in case of identical instruments
                //todo - across multiple assays in the scope of a single project
                //todo - needs testing
                instrument.setAssays(Arrays.asList(assay));

            }
        }
        assay.setInstruments(instruments);

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
                        peakListFiles.add(file.getFile());
                    }
                    mzIdentMlController.addMSController(peakListFiles);
                    dataAccessController = mzIdentMlController;
                    break;
                default:
                    throw new UnsupportedOperationException("Could not get a DataAccessController for format: " + format.name());
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

            //merge instruments
            if (assay.getInstruments().size() == 1) {
                //todo - this is badness, but I can't think of a nicer way at the moment
                //todo - to reconcile the information coming from the PX file with the information in the submission file
                //todo - the px file only contains the instrument name but not the configuration
                //todo - the result fiel contains the instrument configuration but not necessarily the
                //todo - identical instrument name. Basically, we're screwed.
                if (!resultFileScanner.getInstruments().isEmpty()) {
                    Instrument assayInstrument = assay.getInstruments().iterator().next();
                    Instrument scanInstrument = resultFileScanner.getInstruments().iterator().next();
                    //instrument model is a wrapper around a cv param
                    scanInstrument.setCvParam(assayInstrument.getCvParam());
                    scanInstrument.setValue(assayInstrument.getValue());
                    Set<Instrument> instrumentSet = Collections.singleton(scanInstrument);
                    assay.setInstruments(instrumentSet);
                }
            } else {
                logger.warn("Could not resolve multiple instrument configurations from the PX summary file, ignoring.");
            }

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
            int orderIndex = 1;
            //source
            for (InstrumentComponent source : instrumentConfiguration.getSource()) {
                SourceInstrumentComponent sourceInstrumentComponent = new SourceInstrumentComponent();
                sourceInstrumentComponent.setInstrument(instrument);
                sourceInstrumentComponent.setOrder(orderIndex++);
                sourceInstrumentComponent.setInstrumentComponentCvParams(DataConversionUtil.convertInstrumentComponentCvParam(sourceInstrumentComponent, source.getCvParams()));
                sourceInstrumentComponent.setInstrumentComponentUserParams(DataConversionUtil.convertInstrumentComponentUserParam(sourceInstrumentComponent, source.getUserParams()));
            }
            //analyzer
            for (InstrumentComponent analyzer : instrumentConfiguration.getAnalyzer()) {
                AnalyzerInstrumentComponent analyzerInstrumentComponent = new AnalyzerInstrumentComponent();
                analyzerInstrumentComponent.setInstrument(instrument);
                analyzerInstrumentComponent.setOrder(orderIndex++);
                analyzerInstrumentComponent.setInstrumentComponentCvParams(DataConversionUtil.convertInstrumentComponentCvParam(analyzerInstrumentComponent, analyzer.getCvParams()));
                analyzerInstrumentComponent.setInstrumentComponentUserParams(DataConversionUtil.convertInstrumentComponentUserParam(analyzerInstrumentComponent, analyzer.getUserParams()));
            }
            //detector
            for (InstrumentComponent detector : instrumentConfiguration.getDetector()) {
                DetectorInstrumentComponent detectorInstrumentComponent = new DetectorInstrumentComponent();
                detectorInstrumentComponent.setInstrument(instrument);
                detectorInstrumentComponent.setOrder(orderIndex++);
                detectorInstrumentComponent.setInstrumentComponentCvParams(DataConversionUtil.convertInstrumentComponentCvParam(detectorInstrumentComponent, detector.getCvParams()));
                detectorInstrumentComponent.setInstrumentComponentUserParams(DataConversionUtil.convertInstrumentComponentUserParam(detectorInstrumentComponent, detector.getUserParams()));
            }
            //store instrument
            instruments.add(instrument);
            //todo - instrument level additional params are not captured and not inthe pride-r schema at this time
        }
        resultFileScanner.setInstruments(instruments);

        // software
        Set<Software> softwares = new HashSet<Software>();
        softwares.addAll(dataAccessController.getExperimentMetaData().getSoftwares());
        resultFileScanner.setSoftwares(softwares);

        // iterate over proteins
        Set<CvParam> ptms = new HashSet<CvParam>();
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

                // ptm
                List<Modification> modifications = peptide.getModifications();
                for (Modification modification : modifications) {
                    List<uk.ac.ebi.pride.data.core.CvParam> cvParams = modification.getCvParams();
                    for (uk.ac.ebi.pride.data.core.CvParam cvParam : cvParams) {
                        if (cvParam.getCvLookupID().equalsIgnoreCase(Constant.MS) || cvParam.getCvLookupID().equalsIgnoreCase(Constant.UNIMOD)) {
                            ptms.add(cvParam);
                        }
                    }
                }

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
