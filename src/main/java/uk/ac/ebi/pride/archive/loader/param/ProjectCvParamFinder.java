package uk.ac.ebi.pride.archive.loader.param;

import uk.ac.ebi.pride.archive.repo.models.param.CvParam;
import uk.ac.ebi.pride.archive.repo.models.project.*;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Rui Wang
 * @version $Id$
 */
public class ProjectCvParamFinder implements CvParamFinder<Project> {

    public Collection<CvParam> find(Project project) {
        Collection<CvParam> cvParams = new ArrayList<CvParam>();

        // general cv params
        addGeneralCvParams(project, cvParams);

        // ptms
        addPTMCvParams(project, cvParams);

        // samples
        addSampleCvParams(project, cvParams);

        // experiment type
        addExperimentTypeCvParams(project, cvParams);

        // quantification method
        addQuantificationCvParams(project, cvParams);

        // instrument
        addInstrumentCvParams(project, cvParams);

        // software
        addSoftwareCvParams(project, cvParams);

        return cvParams;
    }

    private void addSoftwareCvParams(Project project, Collection<CvParam> cvParams) {
        Collection<ProjectSoftwareCvParam> software = project.getSoftware();
        if (software !=  null) {
            for (ProjectSoftwareCvParam projectSoftwareCvParam : software) {
                cvParams.add(projectSoftwareCvParam.getCvParam());
            }
        }
    }

    private void addInstrumentCvParams(Project project, Collection<CvParam> cvParams) {
        Collection<ProjectInstrumentCvParam> instruments = project.getInstruments();
        if (instruments != null) {
            for (ProjectInstrumentCvParam instrument : instruments) {
                cvParams.add(instrument.getCvParam());
            }
        }
    }

    private void addQuantificationCvParams(Project project, Collection<CvParam> cvParams) {
        Collection<ProjectQuantificationMethodCvParam> quantificationMethods = project.getQuantificationMethods();
        if (quantificationMethods != null) {
            for (ProjectQuantificationMethodCvParam quantificationMethod : quantificationMethods) {
                cvParams.add(quantificationMethod.getCvParam());
            }
        }
    }

    private void addExperimentTypeCvParams(Project project, Collection<CvParam> cvParams) {
        Collection<ProjectExperimentType> experimentTypes = project.getExperimentTypes();
        if (experimentTypes != null) {
            for (ProjectExperimentType experimentType : experimentTypes) {
                cvParams.add(experimentType.getCvParam());
            }
        }
    }

    private void addSampleCvParams(Project project, Collection<CvParam> cvParams) {
        Collection<ProjectSampleCvParam> samples = project.getSamples();
        if (samples != null) {
            for (ProjectSampleCvParam sample : samples) {
                cvParams.add(sample.getCvParam());
            }
        }
    }

    private void addPTMCvParams(Project project, Collection<CvParam> cvParams) {
        Collection<ProjectPTM> ptms = project.getPtms();
        if (ptms != null) {
            for (ProjectPTM ptm : ptms) {
                cvParams.add(ptm.getCvParam());
            }
        }
    }

    private void addGeneralCvParams(Project project, Collection<CvParam> cvParams) {
        Collection<ProjectGroupCvParam> projectGroupCvParams = project.getProjectGroupCvParams();
        if (projectGroupCvParams != null) {
            for (ProjectGroupCvParam projectGroupCvParam : projectGroupCvParams) {
                cvParams.add(projectGroupCvParam.getCvParam());
            }
        }
    }
}
