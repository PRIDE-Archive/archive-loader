package uk.ac.ebi.pride.prider.loader.assay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.data.controller.DataAccessController;
import uk.ac.ebi.pride.data.controller.impl.ControllerImpl.MzIdentMLControllerImpl;
import uk.ac.ebi.pride.data.controller.impl.ControllerImpl.MzMLControllerImpl;
import uk.ac.ebi.pride.data.controller.impl.ControllerImpl.PrideXmlControllerImpl;
import uk.ac.ebi.pride.data.core.*;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.model.SampleMetaData;
import uk.ac.ebi.pride.data.model.Submission;
import uk.ac.ebi.pride.data.util.FileUtil;
import uk.ac.ebi.pride.data.util.MassSpecFileFormat;
import uk.ac.ebi.pride.mol.MoleculeUtilities;
import uk.ac.ebi.pride.prider.dataprovider.file.ProjectFileType;
import uk.ac.ebi.pride.prider.dataprovider.project.SubmissionType;
import uk.ac.ebi.pride.prider.loader.file.FileFinder;
import uk.ac.ebi.pride.prider.loader.util.Constant;
import uk.ac.ebi.pride.prider.loader.util.DataConversionUtil;
import uk.ac.ebi.pride.prider.repo.assay.AssaySampleCvParam;
import uk.ac.ebi.pride.prider.repo.assay.instrument.AnalyzerInstrumentComponent;
import uk.ac.ebi.pride.prider.repo.assay.instrument.DetectorInstrumentComponent;
import uk.ac.ebi.pride.prider.repo.assay.instrument.Instrument;
import uk.ac.ebi.pride.prider.repo.assay.instrument.SourceInstrumentComponent;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Assay file summary scanner for PRIDE XML
 *
 * @author Rui Wang
 * @version $Id$
 */
public class AssayFileScanner {
    private static final Logger logger = LoggerFactory.getLogger(AssayFileScanner.class);

    protected final FileFinder fileFinder;

    public AssayFileScanner(FileFinder fileFinder) {
        this.fileFinder = fileFinder;
    }

    public Collection<AssayFileSummary> scan(Submission submission) throws IOException {
        ArrayList<AssayFileSummary> assayFileSummaries = new ArrayList<AssayFileSummary>();

        SubmissionType submissionType = submission.getProjectMetaData().getSubmissionType();
        if (submissionType.equals(SubmissionType.COMPLETE) || submissionType.equals(SubmissionType.PRIDE)) {
            List<DataFile> dataFiles = submission.getDataFiles();
            for (DataFile dataFile : dataFiles) {
                if (dataFile.isFile() && dataFile.getFileType().equals(ProjectFileType.RESULT)) {
                    File actualAssayFile = fileFinder.find(dataFile.getFile());
                    logger.info("Generating assay file summary on " + actualAssayFile.getAbsolutePath());
                    AssayFileSummary assayFileSummary = scan(dataFile);
                    assayFileSummaries.add(assayFileSummary);
                }
            }
        }

        return assayFileSummaries;
    }

    public AssayFileSummary scan(DataFile dataFile) throws IOException {
        AssayFileSummary fileSummary = new AssayFileSummary();

        // pride xml controller
        DataAccessController dataAccessController = getDataAccessController(dataFile);

        try {
            File actualAssayFile = fileFinder.find(dataFile.getFile());
            logger.info("Scanning for general metadata on assay file: " + actualAssayFile.getAbsolutePath());

            scanForGeneralMetadata(fileSummary, dataFile, dataAccessController);

            // instrument
            logger.info("Scanning for instrument metadata on assay file: " + actualAssayFile.getAbsolutePath());
            scanForInstrument(fileSummary, dataAccessController);

            // software
            logger.info("Scanning for software metadata on assay file: " + actualAssayFile.getAbsolutePath());
            scanForSoftware(fileSummary, dataAccessController);

            // protein group, protein accession and search database
            logger.info("Scanning for protein group, protein accession and search database on assay file: " + actualAssayFile.getAbsolutePath());
            scanForSearchDetails(fileSummary, dataAccessController);

            // iterate over proteins entries
            logger.info("Scanning for protein entries on assay file: " + actualAssayFile.getAbsolutePath());
            scanEntryByEntry(fileSummary, dataAccessController);

            // mzIdentML
            scanMzIdentMLSpecificDetails(dataFile, fileSummary, dataAccessController);

        } finally {
            if (dataAccessController != null) {
                dataAccessController.close();
            }
        }

        return fileSummary;
    }

    private void scanMzIdentMLSpecificDetails(DataFile dataFile, AssayFileSummary fileSummary, DataAccessController dataAccessController) throws IOException {
        File file = fileFinder.find(dataFile.getFile());
        MassSpecFileFormat fileFormat = MassSpecFileFormat.checkFormat(file);
        if (fileFormat.equals(MassSpecFileFormat.MZIDENTML)) {
            logger.info("Scanning for mzIdentML related details on assay file: " + file.getAbsolutePath());

            //chromatogram
            List<DataFile> fileMappings = dataFile.getFileMappings();
            for (DataFile fileMapping : fileMappings) {
                File mappedFile = fileFinder.find(fileMapping.getFile());
                if (mappedFile != null) {
                    MassSpecFileFormat mappedFileFormat = MassSpecFileFormat.checkFormat(mappedFile);
                    if (MassSpecFileFormat.MZML.equals(mappedFileFormat)) {
                        if (getMzMLSummary(fileSummary, mappedFile))
                            break;
                    }
                }
            }

            // spectra and peak list file
            MzIdentMLControllerImpl mzIdentMLController = (MzIdentMLControllerImpl)dataAccessController;
            List<SpectraData> spectraDataFiles = mzIdentMLController.getSpectraDataFiles();
            for (SpectraData spectraDataFile : spectraDataFiles) {
                String location = spectraDataFile.getLocation();
                String realFileName = FileUtil.getRealFileName(location);

                logger.info("Searching for peak list file: " + realFileName);

                boolean peakFilePresent = false;
                for (DataFile fileMapping : dataFile.getFileMappings()) {
                    if (fileMapping.getFileType().equals(ProjectFileType.PEAK)) {
                        File decompressedPeakFile = fileFinder.find(fileMapping.getFile());
                        if (decompressedPeakFile != null && realFileName.equalsIgnoreCase(decompressedPeakFile.getName())) {
                            peakFilePresent = true;
                        }
                    }
                }

                Integer numberOfSpectrabySpectraData = mzIdentMLController.getNumberOfSpectrabySpectraData(spectraDataFile);

                fileSummary.addPeakFileSummary(new PeakFileSummary(realFileName, !peakFilePresent, numberOfSpectrabySpectraData));
            }
        }
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

    private void scanForGeneralMetadata(AssayFileSummary fileSummary, DataFile dataFile, DataAccessController dataAccessController) {
        ExperimentMetaData experimentMetaData = dataAccessController.getExperimentMetaData();

        // file id
        fileSummary.setId(dataFile.getFileId());

        // accession
        fileSummary.setAccession(dataFile.getAssayAccession());

        // set assay title
        fileSummary.setName(experimentMetaData.getName());

        // set short label
        fileSummary.setShortLabel(experimentMetaData.getShortLabel());

        // protein count
        fileSummary.setNumberOfProteins(dataAccessController.getNumberOfProteins());

        // peptide count
        fileSummary.setNumberOfPeptides(dataAccessController.getNumberOfPeptides());

        // total spectrum count
        fileSummary.setNumberOfSpectra(dataAccessController.getNumberOfSpectra());

        //contact
        fileSummary.addContacts(DataConversionUtil.convertContact(experimentMetaData.getPersons()));

        // sample
        List<AssaySampleCvParam> samples = new ArrayList<AssaySampleCvParam>();
        SampleMetaData sampleMetaData = dataFile.getSampleMetaData();
        samples.addAll(DataConversionUtil.convertAssaySampleCvParams(sampleMetaData.getMetaData(SampleMetaData.Type.SPECIES)));
        samples.addAll(DataConversionUtil.convertAssaySampleCvParams(sampleMetaData.getMetaData(SampleMetaData.Type.TISSUE)));
        samples.addAll(DataConversionUtil.convertAssaySampleCvParams(sampleMetaData.getMetaData(SampleMetaData.Type.CELL_TYPE)));
        samples.addAll(DataConversionUtil.convertAssaySampleCvParams(sampleMetaData.getMetaData(SampleMetaData.Type.DISEASE)));
        fileSummary.addSamples(samples);

        // quantification
        Set<uk.ac.ebi.pride.data.model.CvParam> quantification = sampleMetaData.getMetaData(SampleMetaData.Type.QUANTIFICATION_METHOD);
        fileSummary.addQuantificationMethods(DataConversionUtil.convertAssayQuantitationMethodCvParams(quantification));

        // experimental factor
        Set<uk.ac.ebi.pride.data.model.CvParam> experimentFactor = sampleMetaData.getMetaData(SampleMetaData.Type.EXPERIMENTAL_FACTOR);
        if (experimentFactor != null && !experimentFactor.isEmpty()) {
            //do it this way because experiment factor is stored as the value of a single user param
            fileSummary.setExperimentalFactor(experimentFactor.iterator().next().getValue());
        }

        //additional params
        ParamGroup additional = dataAccessController.getExperimentMetaData().getAdditional();
        fileSummary.addCvParams(DataConversionUtil.convertAssayGroupCvParams(additional));
        fileSummary.addUserParams(DataConversionUtil.convertAssayGroupUserParams(additional));
    }

    private void scanForSoftware(AssayFileSummary fileSummary, DataAccessController dataAccessController) {
        ExperimentMetaData experimentMetaData = dataAccessController.getExperimentMetaData();

        Set<Software> softwares = new HashSet<Software>();
        //todo - dataProcessing params are not captured as software params
        //todo - there is a 1-1 mapping for pride XML, but how to deal with mzidentml?
        //todo - will need to call getspectrumprotocol and getproteinprotocol on dataaccesscontroller to get params
        softwares.addAll(experimentMetaData.getSoftwares());

        fileSummary.addSoftwares(DataConversionUtil.convertSoftware(softwares));
    }

    private void scanForInstrument(AssayFileSummary fileSummary, DataAccessController dataAccessController) {
        Set<Instrument> instruments = new HashSet<Instrument>();
        //check to see if we have instrument configurations in the result file to scan
        //this isn't always present
        if (dataAccessController.getMzGraphMetaData() != null) {

            Collection<InstrumentConfiguration> instrumentConfigurations = dataAccessController.getMzGraphMetaData().getInstrumentConfigurations();
            for (InstrumentConfiguration instrumentConfiguration : instrumentConfigurations) {
                Instrument instrument = new Instrument();

                //set instrument cv param
                uk.ac.ebi.pride.prider.repo.param.CvParam cvParam = new uk.ac.ebi.pride.prider.repo.param.CvParam();
                cvParam.setCvLabel(Constant.MS);
                cvParam.setName(Constant.MS_INSTRUMENT_MODEL_NAME);
                cvParam.setAccession(Constant.MS_INSTRUMENT_MODEL_AC);
                instrument.setCvParam(cvParam);
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
        }

        fileSummary.addInstruments(instruments);
    }

    private void scanForSearchDetails(AssayFileSummary fileSummary, DataAccessController dataAccessController) {
        // protein group
        boolean proteinGroupPresent = dataAccessController.hasProteinAmbiguityGroup();
        fileSummary.setProteinGroupPresent(proteinGroupPresent);

        Collection<Comparable> proteinIds = dataAccessController.getProteinIds();
        if (proteinIds != null && !proteinIds.isEmpty()) {
            Comparable firstProteinId = proteinIds.iterator().next();

            // protein accession
            String accession = dataAccessController.getProteinAccession(firstProteinId);
            fileSummary.setExampleProteinAccession(accession);

            // search database
            SearchDataBase searchDatabase = dataAccessController.getSearchDatabase(firstProteinId);
            if (searchDatabase != null) {
                fileSummary.setSearchDatabase(searchDatabase.getName());
            }
        }

    }

    private void scanEntryByEntry(AssayFileSummary fileSummary, DataAccessController dataAccessController) {
        Set<CvParam> ptms = new HashSet<CvParam>();
        Set<String> peptideSequences = new HashSet<String>();
        Set<Comparable> spectrumIds = new HashSet<Comparable>();
        double errorPSMCount = 0.0;
        double totalPSMCount = 0.0;

        Collection<Comparable> proteinIds = dataAccessController.getProteinIds();
        for (Comparable proteinId : proteinIds) {
            Collection<Comparable> peptideIds = dataAccessController.getPeptideIds(proteinId);
            for (Comparable peptideId : peptideIds) {
                totalPSMCount++;

                // peptide
                Peptide peptide = dataAccessController.getPeptideByIndex(proteinId, peptideId);
                PeptideSequence peptideSequence = peptide.getPeptideSequence();
                peptideSequences.add(peptideSequence.getSequence());

                // ptm
                List<Modification> modifications = new ArrayList<Modification>(dataAccessController.getPTMs(proteinId, peptideId));
                List<Double> ptmMasses = new ArrayList<Double>();
                for (Modification modification : modifications) {
                    // ptm mass
                    List<Double> monoMasses = modification.getMonoisotopicMassDelta();
                    if (monoMasses != null && !monoMasses.isEmpty()) {
                        ptmMasses.add(monoMasses.get(0));
                    }

                    // record ptm
                    List<CvParam> cvParams = modification.getCvParams();
                    for (CvParam cvParam : cvParams) {
                        if (cvParam.getCvLookupID().equalsIgnoreCase(Constant.PSI_MOD) || cvParam.getCvLookupID().equalsIgnoreCase(Constant.UNIMOD)) {
                            ptms.add(cvParam);
                        }
                    }
                }

                // precursor charge
                Integer charge = dataAccessController.getPeptidePrecursorCharge(proteinId, peptideId);
                double mz = dataAccessController.getPeptidePrecursorMz(proteinId, peptideId);
                Comparable specId = dataAccessController.getPeptideSpectrumId(proteinId, peptideId);
                if ((charge == null || mz == -1) && specId != null) {
                    charge = dataAccessController.getSpectrumPrecursorCharge(specId);
                    mz = dataAccessController.getSpectrumPrecursorMz(specId);
                    if (charge == null || charge == 0) {
                        charge = null;
                    }
                }

                // delta mass
                if (charge == null) {
                    errorPSMCount++;
                } else {
                    Double deltaMass = MoleculeUtilities.calculateDeltaMz(peptideSequence.getSequence(), mz, charge, ptmMasses);
                    if (!isDeltaMzInRange(deltaMass)) {
                        errorPSMCount++;
                    }
                }

                // spectrum
                if (peptide.getSpectrumIdentification() != null && peptide.getSpectrumIdentification().getSpectrum() != null) {
                    Spectrum spectrum = peptide.getSpectrumIdentification().getSpectrum();
                    spectrumIds.add(spectrum.getId());
                }

                if (peptide.getFragmentation() != null && peptide.getFragmentation().size() > 0) {
                    fileSummary.setMs2Annotation(true);
                }
            }
        }

        fileSummary.setNumberOfUniquePeptides(peptideSequences.size());
        fileSummary.setNumberOfIdentifiedSpectra(spectrumIds.size());
        fileSummary.addPtms(DataConversionUtil.convertAssayPTMs(ptms));
        fileSummary.setDeltaMzErrorRate(errorPSMCount / totalPSMCount);
    }

    protected boolean isDeltaMzInRange(Double deltaMz) {
        return deltaMz != null && (deltaMz >= -Constant.MZ_OUTLIER) && (deltaMz <= Constant.MZ_OUTLIER);
    }

    protected DataAccessController getDataAccessController(DataFile dataFile) throws IOException {

        File file = fileFinder.find(dataFile.getFile());

        MassSpecFileFormat fileFormat = MassSpecFileFormat.checkFormat(file);

        switch (fileFormat) {
            case PRIDE:
                return new PrideXmlControllerImpl(file);
            case MZIDENTML:
                MzIdentMLControllerImpl mzIdentMlController = new MzIdentMLControllerImpl(file);

                List<File> peakListFiles = new ArrayList<File>();
                for (DataFile df : dataFile.getFileMappings()) {
                    if (df.getFileType().equals(ProjectFileType.PEAK)) {
                        File peakFile = fileFinder.find(df.getFile());
                        peakListFiles.add(peakFile);
                    }
                }

                if (!peakListFiles.isEmpty()) {
                    mzIdentMlController.addMSController(peakListFiles);
                }

                return mzIdentMlController;
            default:
                return null;
        }
    }
}
