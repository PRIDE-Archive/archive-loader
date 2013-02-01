package uk.ac.ebi.pride.prider.loader.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.data.core.*;
import uk.ac.ebi.pride.data.model.CvParam;
import uk.ac.ebi.pride.data.model.Param;
import uk.ac.ebi.pride.prider.loader.exception.ProjectLoaderException;
import uk.ac.ebi.pride.prider.repo.assay.*;
import uk.ac.ebi.pride.prider.repo.assay.software.SoftwareCvParam;
import uk.ac.ebi.pride.prider.repo.assay.software.SoftwareUserParam;
import uk.ac.ebi.pride.prider.repo.instrument.InstrumentComponent;
import uk.ac.ebi.pride.prider.repo.instrument.InstrumentComponentCvParam;
import uk.ac.ebi.pride.prider.repo.instrument.InstrumentComponentUserParam;
import uk.ac.ebi.pride.prider.repo.project.*;
import uk.ac.ebi.pride.prider.repo.project.Reference;

import java.util.*;

/**
 * Convert various data model
 *
 * @author Rui Wang
 * @version $Id$
 */
public final class DataConversionUtil {

    private static final Logger logger = LoggerFactory.getLogger(DataConversionUtil.class);

    /**
     * Combine a list of strings to a single string
     *
     * @param strToCombine strings to be combined
     * @param separator    string separator
     */
    public static String combineToString(Set<String> strToCombine, String separator) {
        StringBuilder builder = new StringBuilder();

        for (String s : strToCombine) {
            builder.append(s);
            builder.append(separator);
        }

        // remove the last the separator
        String str = builder.toString();
        if (strToCombine.size() > 0) {
            str = str.substring(0, str.length() - separator.length());
        }
        return str;
    }


    /**
     * Convert pubmed ids to prider reference objects
     *
     * @param pubmedIds pubmed ids
     * @return a list of prider references
     */
    public static List<Reference> convertReferences(Set<String> pubmedIds) {
        List<Reference> references = new ArrayList<Reference>();

        for (String pubmedId : pubmedIds) {
            Reference reference = new Reference();
            reference.setPubmedId(Integer.parseInt(pubmedId));
            references.add(reference);
        }

        return references;
    }

    public static Collection<AssaySample> convertAssaySampleCvParams(Assay assay, Set<Param> sampleParams) {
        Collection<AssaySample> retval = new HashSet<AssaySample>();
        retval.addAll(convertAssayCvParams(assay, AssaySample.class, sampleParams));
        return retval;
    }

    public static Collection<AssayQuantificationMethod> convertAssayQuantitationMethodCvParams(Assay assay, Set<Param> sampleParams) {
        Collection<AssayQuantificationMethod> retval = new HashSet<AssayQuantificationMethod>();
        retval.addAll(convertAssayCvParams(assay, AssayQuantificationMethod.class, sampleParams));
        return retval;
    }


    @SuppressWarnings("unchecked")
    private static Collection convertAssayCvParams(Assay assay, Class clz, Set<? extends Param> sampleParams) {

        try {
            Collection<AssayCvParam> retval = new HashSet<AssayCvParam>();
            for (Param param : sampleParams) {
                if (param instanceof CvParam) {
                    CvParam cvParam = (CvParam) param;
                    uk.ac.ebi.pride.prider.repo.param.CvParam repoParam = CvParamManager.getInstance().getCvParam(cvParam.getAccession());
                    //if param isn't already seen in db, store it
                    if (repoParam == null) {
                        CvParamManager.getInstance().putCvParam(cvParam.getCvLabel(), cvParam.getAccession(), cvParam.getName());
                        repoParam = CvParamManager.getInstance().getCvParam(cvParam.getAccession());
                    }
                    AssayCvParam acvParam = (AssayCvParam) clz.newInstance();
                    acvParam.setAssay(assay);
                    acvParam.setCvParam(repoParam);
                    acvParam.setValue(cvParam.getValue());
                    retval.add(acvParam);
                } else {
                    logger.warn("Ignored sample userParam" + param.getName() + "->" + param.getValue());
                }
            }
            return retval;

        } catch (InstantiationException e) {
            throw new ProjectLoaderException("Error creating cv param.", e);
        } catch (IllegalAccessException e) {
            throw new ProjectLoaderException("Error creating cv param.", e);
        }

    }


    public static Collection<ProjectExperimentType> convertProjectExperimentTypeCvParams(Project project, Set<CvParam> params) {
        Collection<ProjectExperimentType> retval = new HashSet<ProjectExperimentType>();
        retval.addAll(convertProjectCvParams(project, ProjectExperimentType.class, params));
        return retval;
    }

    public static Collection<ProjectSample> convertProjectSampleCvParams(Project project, Set<CvParam> params) {
        Collection<ProjectSample> retval = new HashSet<ProjectSample>();
        retval.addAll(convertProjectCvParams(project, ProjectSample.class, params));
        return retval;
    }

    public static Collection<ProjectGroupCvParam> convertProjectGroupCvParams(Project project, Set<Param> params) {
        Collection<ProjectGroupCvParam> retval = new HashSet<ProjectGroupCvParam>();
        retval.addAll(convertProjectCvParams(project, ProjectGroupCvParam.class, params));
        return retval;
    }

    public static Collection<ProjectQuantificationMethod> convertProjectQuantificationMethodCvParams(Project project, Set<CvParam> params) {
        Collection<ProjectQuantificationMethod> retval = new HashSet<ProjectQuantificationMethod>();
        retval.addAll(convertProjectCvParams(project, ProjectQuantificationMethod.class, params));
        return retval;
    }


    @SuppressWarnings("unchecked")
    private static Collection convertProjectCvParams(Project project, Class clz, Set<? extends Param> projectParams) {

        try {
            Collection<ProjectCvParam> retval = new HashSet<ProjectCvParam>();
            for (Param param : projectParams) {
                if (param instanceof CvParam) {
                    CvParam cvParam = (CvParam) param;
                    uk.ac.ebi.pride.prider.repo.param.CvParam repoParam = CvParamManager.getInstance().getCvParam(cvParam.getAccession());
                    //if param isn't already seen in db, store it
                    if (repoParam == null) {
                        CvParamManager.getInstance().putCvParam(cvParam.getCvLabel(), cvParam.getAccession(), cvParam.getName());
                        repoParam = CvParamManager.getInstance().getCvParam(cvParam.getAccession());
                    }
                    ProjectCvParam projectCvParam = (ProjectCvParam) clz.newInstance();
                    projectCvParam.setProject(project);
                    projectCvParam.setCvParam(repoParam);
                    projectCvParam.setValue(cvParam.getValue());
                    retval.add(projectCvParam);
                } else {
                    logger.warn("Ignored project userParam" + param.getName() + "->" + param.getValue());
                }
            }
            return retval;

        } catch (InstantiationException e) {
            throw new ProjectLoaderException("Error creating cv param.", e);
        } catch (IllegalAccessException e) {
            throw new ProjectLoaderException("Error creating cv param.", e);
        }

    }

    public static Collection<uk.ac.ebi.pride.prider.repo.assay.software.Software> convertSoftware(Assay assay, Set<Software> softwares) {

        Set<uk.ac.ebi.pride.prider.repo.assay.software.Software> softwareSet = new HashSet<uk.ac.ebi.pride.prider.repo.assay.software.Software>();
        int orderIndex = 0;
        for (Software oldSoftware : softwares) {
            uk.ac.ebi.pride.prider.repo.assay.software.Software newSoftware = new uk.ac.ebi.pride.prider.repo.assay.software.Software();
            newSoftware.setAssay(assay);
            newSoftware.setName(oldSoftware.getName());
            newSoftware.setOrder(orderIndex++);
            newSoftware.setSoftwareCvParams(convertSoftwareCvParams(newSoftware, oldSoftware.getCvParams()));
            newSoftware.setSoftwareUserParams(convertSoftwareUserParams(newSoftware, oldSoftware.getUserParams()));
            newSoftware.setVersion(oldSoftware.getVersion());
            newSoftware.setCustomization(oldSoftware.getCustomization());
            softwareSet.add(newSoftware);
        }

        return softwareSet;

    }

    private static Collection<SoftwareUserParam> convertSoftwareUserParams(uk.ac.ebi.pride.prider.repo.assay.software.Software software,
                                                                           List<uk.ac.ebi.pride.data.core.UserParam> userParams) {

        Collection<SoftwareUserParam> retval = new HashSet<SoftwareUserParam>();
        for (uk.ac.ebi.pride.data.core.UserParam userParam : userParams) {
            SoftwareUserParam swuPavam = new SoftwareUserParam();
            swuPavam.setName(userParam.getName());
            swuPavam.setValue(userParam.getValue());
            swuPavam.setSoftware(software);
            retval.add(swuPavam);
        }

        return retval;

    }

    private static Collection<SoftwareCvParam> convertSoftwareCvParams(uk.ac.ebi.pride.prider.repo.assay.software.Software software,
                                                                       List<uk.ac.ebi.pride.data.core.CvParam> cvParams) {

        Collection<SoftwareCvParam> retval = new HashSet<SoftwareCvParam>();
        for (uk.ac.ebi.pride.data.core.CvParam cvParam : cvParams) {

            uk.ac.ebi.pride.prider.repo.param.CvParam repoParam = CvParamManager.getInstance().getCvParam(cvParam.getAccession());
            //if param isn't already seen in db, store it
            if (repoParam == null) {
                CvParamManager.getInstance().putCvParam(cvParam.getCvLookupID(), cvParam.getAccession(), cvParam.getName());
                repoParam = CvParamManager.getInstance().getCvParam(cvParam.getAccession());
            }

            SoftwareCvParam swcvPavam = new SoftwareCvParam();
            swcvPavam.setSoftware(software);
            swcvPavam.setCvParam(repoParam);
            swcvPavam.setValue(cvParam.getValue());
            retval.add(swcvPavam);
        }

        return retval;

    }

    public static Collection<InstrumentComponentCvParam> convertInstrumentComponentCvParam(InstrumentComponent instrumentComponent,
                                                                                           List<uk.ac.ebi.pride.data.core.CvParam> cvParams) {
        Collection<InstrumentComponentCvParam> retval = new HashSet<InstrumentComponentCvParam>();
        for (uk.ac.ebi.pride.data.core.CvParam cvParam : cvParams) {

            uk.ac.ebi.pride.prider.repo.param.CvParam repoParam = CvParamManager.getInstance().getCvParam(cvParam.getAccession());
            //if param isn't already seen in db, store it
            if (repoParam == null) {
                CvParamManager.getInstance().putCvParam(cvParam.getCvLookupID(), cvParam.getAccession(), cvParam.getName());
                repoParam = CvParamManager.getInstance().getCvParam(cvParam.getAccession());
            }

            InstrumentComponentCvParam iccvParam = new InstrumentComponentCvParam();
            iccvParam.setInstrumentComponent(instrumentComponent);
            iccvParam.setCvParam(repoParam);
            iccvParam.setValue(cvParam.getValue());
            retval.add(iccvParam);

        }

        return retval;
    }


    public static Collection<InstrumentComponentUserParam> convertInstrumentComponentUserParam(InstrumentComponent instrumentComponent,
                                                                                               List<uk.ac.ebi.pride.data.core.UserParam> userParams) {
        Collection<InstrumentComponentUserParam> retval = new HashSet<InstrumentComponentUserParam>();
        for (uk.ac.ebi.pride.data.core.UserParam userParam : userParams) {

            InstrumentComponentUserParam icuserParam = new InstrumentComponentUserParam();
            icuserParam.setInstrumentComponent(instrumentComponent);
            icuserParam.setName(userParam.getName());
            icuserParam.setValue(userParam.getValue());
            retval.add(icuserParam);

        }

        return retval;
    }


    public static Collection<AssayPTM> convertAssayPTMs(Assay assay, Set<uk.ac.ebi.pride.data.core.CvParam> ptms) {

        Set<AssayPTM> retval = new HashSet<AssayPTM>();
        for (uk.ac.ebi.pride.data.core.CvParam cvParam : ptms) {

            uk.ac.ebi.pride.prider.repo.param.CvParam repoParam = CvParamManager.getInstance().getCvParam(cvParam.getAccession());
            //if param isn't already seen in db, store it
            if (repoParam == null) {
                CvParamManager.getInstance().putCvParam(cvParam.getCvLookupID(), cvParam.getAccession(), cvParam.getName());
                repoParam = CvParamManager.getInstance().getCvParam(cvParam.getAccession());
            }

            AssayPTM aPTM = new AssayPTM();
            aPTM.setAssay(assay);
            aPTM.setCvParam(repoParam);
            aPTM.setValue(cvParam.getValue());
            retval.add(aPTM);

        }

        return retval;

    }

    public static Collection<ProjectPTM> convertProjectPTMs(Project project, Set<CvParam> ptms) {

        Set<ProjectPTM> retval = new HashSet<ProjectPTM>();
        for (CvParam cvParam : ptms) {

            uk.ac.ebi.pride.prider.repo.param.CvParam repoParam = CvParamManager.getInstance().getCvParam(cvParam.getAccession());
            //if param isn't already seen in db, store it
            if (repoParam == null) {
                CvParamManager.getInstance().putCvParam(cvParam.getCvLabel(), cvParam.getAccession(), cvParam.getName());
                repoParam = CvParamManager.getInstance().getCvParam(cvParam.getAccession());
            }

            ProjectPTM projectPTM = new ProjectPTM();
            projectPTM.setProject(project);
            projectPTM.setCvParam(repoParam);
            projectPTM.setValue(cvParam.getValue());
            retval.add(projectPTM);

        }

        return retval;
    }

    public static ProjectSample convertAssaySampleToProjectSample(Project project, AssaySample sample) {

        ProjectSample retval = new ProjectSample();
        retval.setCvParam(sample.getCvParam());
        retval.setValue(sample.getValue());
        retval.setProject(project);
        return retval;

    }

    public static ProjectPTM convertAssayPTMtoProjectPTM(Project project, AssayPTM ptm) {

        ProjectPTM projectPTM = new ProjectPTM();
        projectPTM.setProject(project);
        projectPTM.setCvParam(ptm.getCvParam());
        projectPTM.setValue(ptm.getValue());
        return projectPTM;

    }

    public static ProjectQuantificationMethod convertAssayQuantitationMethodToProjectQuantitationMethod(Project project, AssayQuantificationMethod param) {

        ProjectQuantificationMethod method = new ProjectQuantificationMethod();
        method.setCvParam(param.getCvParam());
        method.setValue(param.getValue());
        method.setProject(project);
        return method;

    }

    public static Collection<Contact> convertContact(Assay assay, Collection<Person> personContacts) {
        Set<Contact> retval = new HashSet<Contact>();
        for (Person person : personContacts) {

            Contact contact = new Contact();
            //todo - contact.setTitle(); - no value available
            StringBuilder sb = new StringBuilder(person.getFirstname());
            if (person.getMidInitials() != null) {
                sb.append(" ").append(person.getMidInitials());
            }
            contact.setFirstName(sb.toString());
            contact.setLastName(person.getLastname());
            sb = new StringBuilder();
            for (Organization org : person.getAffiliation()) {
                sb.append(org.getMail()).append(", ");
            }
            //remove last 2 chars
            String affiliation = sb.toString();
            affiliation = affiliation.substring(0, affiliation.length() - 2);
            contact.setAffiliation(affiliation);
            contact.setEmail(person.getMail());
            contact.setAssay(assay);
            retval.add(contact);
        }

        return retval;
    }

    public static Collection<AssayGroupCvParam> convertAssayGroupCvParams(Assay assay, ParamGroup additional) {

        Set<AssayGroupCvParam> retval = new HashSet<AssayGroupCvParam>();
        for (uk.ac.ebi.pride.data.core.CvParam cvParam : additional.getCvParams()) {

            uk.ac.ebi.pride.prider.repo.param.CvParam repoParam = CvParamManager.getInstance().getCvParam(cvParam.getAccession());
            //if param isn't already seen in db, store it
            if (repoParam == null) {
                CvParamManager.getInstance().putCvParam(cvParam.getCvLookupID(), cvParam.getAccession(), cvParam.getName());
                repoParam = CvParamManager.getInstance().getCvParam(cvParam.getAccession());
            }

            AssayGroupCvParam agcvParam = new AssayGroupCvParam();
            agcvParam.setAssay(assay);
            agcvParam.setCvParam(repoParam);
            agcvParam.setValue(cvParam.getValue());
            retval.add(agcvParam);

        }

        return retval;
    }

    public static Collection<AssayUserParam> convertAssayGroupUserParams(Assay assay, ParamGroup additional) {

        Set<AssayUserParam> retval = new HashSet<AssayUserParam>();
        for (UserParam userParam : additional.getUserParams()) {

            AssayUserParam auserParam = new AssayUserParam();
            auserParam.setAssay(assay);
            auserParam.setName(userParam.getName());
            auserParam.setValue(userParam.getValue());
            retval.add(auserParam);

        }

        return retval;

    }
}
