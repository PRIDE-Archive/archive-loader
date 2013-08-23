package uk.ac.ebi.pride.prider.loader.assay;

import uk.ac.ebi.pride.data.controller.DataAccessController;
import uk.ac.ebi.pride.data.controller.impl.ControllerImpl.PrideXmlControllerImpl;
import uk.ac.ebi.pride.data.core.*;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.prider.loader.file.FileFinder;
import uk.ac.ebi.pride.prider.loader.util.Constant;
import uk.ac.ebi.pride.prider.loader.util.DataConversionUtil;
import uk.ac.ebi.pride.prider.repo.assay.Assay;
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
public class PrideXmlAssayFileScanner implements AssayFileScanner {
    protected final FileFinder fileFinder;

    public PrideXmlAssayFileScanner(FileFinder fileFinder) {
        this.fileFinder = fileFinder;
    }

    @Override
    public AssayFileSummary scan(Assay assay, DataFile dataFile) throws IOException {
        AssayFileSummary fileSummary = new AssayFileSummary();

        // pride xml controller
        DataAccessController dataAccessController = getDataAccessController(dataFile);

        try {
            scanForGeneralMetadata(fileSummary, dataAccessController);

            // instrument
            scanForInstrument(fileSummary, dataAccessController);

            // software
            scanForSoftware(fileSummary, dataAccessController);

            // iterate over proteins entries
            scanEntryByEntry(fileSummary, dataAccessController);

        } finally {
            if (dataAccessController != null) {
                dataAccessController.close();
            }
        }

        return fileSummary;
    }

    private void scanForGeneralMetadata(AssayFileSummary fileSummary, DataAccessController dataAccessController) {
        ExperimentMetaData experimentMetaData = dataAccessController.getExperimentMetaData();

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
        fileSummary.addContacts(experimentMetaData.getPersons());

        //additional params
        fileSummary.setAdditional(dataAccessController.getExperimentMetaData().getAdditional());
    }

    private void scanForSoftware(AssayFileSummary fileSummary, DataAccessController dataAccessController) {
        ExperimentMetaData experimentMetaData = dataAccessController.getExperimentMetaData();

        Set<Software> softwares = new HashSet<Software>();
        //todo - dataProcessing params are not captured as software params
        //todo - there is a 1-1 mapping for pride XML, but how to deal with mzidentml?
        //todo - will need to call getspectrumprotocol and getproteinprotocol on dataaccesscontroller to get params
        softwares.addAll(experimentMetaData.getSoftwares());
        fileSummary.addSoftwares(softwares);
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

    private void scanEntryByEntry(AssayFileSummary fileSummary, DataAccessController dataAccessController) {
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
                    List<CvParam> cvParams = modification.getCvParams();
                    for (CvParam cvParam : cvParams) {
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
                    fileSummary.setMs2Annotation(true);
                }
            }
        }
        fileSummary.setNumberOfUniquePeptides(peptideSequences.size());
        fileSummary.setNumberOfIdentifiedSpectra(spectrumIds.size());
        fileSummary.addPtms(ptms);
    }


    protected DataAccessController getDataAccessController(DataFile dataFile) throws IOException {

        File file = fileFinder.find(dataFile.getFile());

        return new PrideXmlControllerImpl(file);
    }
}
