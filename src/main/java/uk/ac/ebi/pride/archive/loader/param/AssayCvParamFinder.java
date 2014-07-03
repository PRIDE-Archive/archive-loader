package uk.ac.ebi.pride.archive.loader.param;


import uk.ac.ebi.pride.archive.repo.assay.*;
import uk.ac.ebi.pride.archive.repo.assay.instrument.*;
import uk.ac.ebi.pride.archive.repo.assay.software.Software;
import uk.ac.ebi.pride.archive.repo.assay.software.SoftwareCvParam;
import uk.ac.ebi.pride.archive.repo.param.CvParam;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Rui Wang
 * @version $Id$
 */
public class AssayCvParamFinder implements CvParamFinder<Assay> {

    @Override
    public Collection<CvParam> find(Assay assay) {
        Collection<CvParam> cvParams = new ArrayList<CvParam>();

        addGeneralCvParams(assay, cvParams);

        addSampleCvParams(assay, cvParams);

        addQuantificationCvParams(assay, cvParams);

        addPTMCvParams(assay, cvParams);

        addSoftwareCvParams(assay, cvParams);

        addInstrumentCvParams(assay, cvParams);

        return cvParams;
    }

    private void addGeneralCvParams(Assay assay, Collection<CvParam> cvParams) {
        Collection<AssayGroupCvParam> assayGroupCvParams = assay.getAssayGroupCvParams();
        if (assayGroupCvParams != null) {
            for (AssayGroupCvParam assayGroupCvParam : assayGroupCvParams) {
                cvParams.add(assayGroupCvParam.getCvParam());
            }
        }
    }

    private void addSampleCvParams(Assay assay, Collection<CvParam> cvParams) {
        Collection<AssaySampleCvParam> samples = assay.getSamples();
        if (samples != null) {
            for (AssaySampleCvParam assaySampleCvParam : samples) {
                cvParams.add(assaySampleCvParam.getCvParam());
            }
        }
    }

    private void addQuantificationCvParams(Assay assay, Collection<CvParam> cvParams) {
        Collection<AssayQuantificationMethodCvParam> quantificationMethods = assay.getQuantificationMethods();
        if (quantificationMethods != null) {
            for (AssayQuantificationMethodCvParam assayQuantificationMethodCvParam : quantificationMethods) {
                cvParams.add(assayQuantificationMethodCvParam.getCvParam());
            }
        }
    }

    private void addPTMCvParams(Assay assay, Collection<CvParam> cvParams) {
        Collection<AssayPTM> ptms = assay.getPtms();
        if (ptms != null) {
            for (AssayPTM assayPTM : ptms) {
                cvParams.add(assayPTM.getCvParam());
            }
        }
    }

    private void addSoftwareCvParams(Assay assay, Collection<CvParam> cvParams) {
        Collection<Software> softwares = assay.getSoftwares();
        if (softwares != null) {
            for (Software software : softwares) {
                Collection<SoftwareCvParam> softwareCvParams = software.getSoftwareCvParams();
                if (softwareCvParams != null) {
                    for (SoftwareCvParam softwareCvParam : softwareCvParams) {
                        cvParams.add(softwareCvParam.getCvParam());
                    }
                }
            }
        }
    }

    private void addInstrumentCvParams(Assay assay, Collection<CvParam> cvParams) {
        Collection<Instrument> instruments = assay.getInstruments();
        if (instruments != null) {
            for (Instrument instrument : instruments) {
                CvParam model = instrument.getCvParam();
                if (model != null) {
                    cvParams.add(model);
                }

                // source
                Collection<SourceInstrumentComponent> sources = instrument.getSources();
                if (sources != null) {
                    for (SourceInstrumentComponent source : sources) {
                        Collection<InstrumentComponentCvParam> instrumentComponentCvParams = source.getInstrumentComponentCvParams();
                        if (instrumentComponentCvParams != null) {
                            for (InstrumentComponentCvParam instrumentComponentCvParam : instrumentComponentCvParams) {
                                cvParams.add(instrumentComponentCvParam.getCvParam());
                            }
                        }
                    }
                }

                // analyzer
                Collection<AnalyzerInstrumentComponent> analyzers = instrument.getAnalyzers();
                if (analyzers != null) {
                    for (AnalyzerInstrumentComponent analyzer : analyzers) {
                        Collection<InstrumentComponentCvParam> instrumentComponentCvParams = analyzer.getInstrumentComponentCvParams();
                        if (instrumentComponentCvParams != null) {
                            for (InstrumentComponentCvParam instrumentComponentCvParam : instrumentComponentCvParams) {
                                cvParams.add(instrumentComponentCvParam.getCvParam());
                            }
                        }
                    }
                }

                // detector
                Collection<DetectorInstrumentComponent> detectors = instrument.getDetectors();
                if (detectors != null) {
                    for (DetectorInstrumentComponent detector : detectors) {
                        Collection<InstrumentComponentCvParam> instrumentComponentCvParams = detector.getInstrumentComponentCvParams();
                        if (instrumentComponentCvParams != null) {
                            for (InstrumentComponentCvParam instrumentComponentCvParam : instrumentComponentCvParams) {
                                cvParams.add(instrumentComponentCvParam.getCvParam());
                            }
                        }
                    }
                }
            }
        }
    }
}
