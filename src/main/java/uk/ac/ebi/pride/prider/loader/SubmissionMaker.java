package uk.ac.ebi.pride.prider.loader;

import uk.ac.ebi.pride.data.controller.DataAccessException;
import uk.ac.ebi.pride.data.model.*;
import uk.ac.ebi.pride.data.util.MassSpecFileFormat;
import uk.ac.ebi.pride.prider.dataprovider.file.ProjectFileSource;
import uk.ac.ebi.pride.prider.dataprovider.file.ProjectFileType;
import uk.ac.ebi.pride.prider.dataprovider.project.SubmissionType;
import uk.ac.ebi.pride.prider.loader.assay.AssayFileScanner;
import uk.ac.ebi.pride.prider.loader.assay.AssayFileScannerFactory;
import uk.ac.ebi.pride.prider.loader.assay.AssayFileSummary;
import uk.ac.ebi.pride.prider.loader.file.FileFinder;
import uk.ac.ebi.pride.prider.loader.util.Constant;
import uk.ac.ebi.pride.prider.loader.util.DataConversionUtil;
import uk.ac.ebi.pride.prider.repo.assay.Assay;
import uk.ac.ebi.pride.prider.repo.assay.AssayPTM;
import uk.ac.ebi.pride.prider.repo.assay.AssaySampleCvParam;
import uk.ac.ebi.pride.prider.repo.assay.instrument.Instrument;
import uk.ac.ebi.pride.prider.repo.assay.software.Software;
import uk.ac.ebi.pride.prider.repo.file.ProjectFile;
import uk.ac.ebi.pride.prider.repo.param.CvParam;
import uk.ac.ebi.pride.prider.repo.project.*;
import uk.ac.ebi.pride.prider.repo.user.User;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * ProjectMaker is responsible for constructing the object to be persisted into the
 * repository.
 * <p/>
 * NOTE: it reads metadata from both the submission summary file and the the submitted
 * result files.
 *
 * @author Rui Wang
 * @version $Id$
 */
public class SubmissionMaker {

    private final FileFinder fileFinder;

    public SubmissionMaker(FileFinder fileFinder) {
        this.fileFinder = fileFinder;
    }

    /**
     * Generate a list of assays based on a given submission and its internal file path
     *
     * @param submission submission object
     * @return a list of assays
     * @throws IOException error while reading the result files
     */
    public List<Assay> makeAssays(final Submission submission) throws IOException {
        List<Assay> assays = new ArrayList<Assay>();

        if (submission.getProjectMetaData().getSubmissionType().equals(SubmissionType.COMPLETE) ||
                submission.getProjectMetaData().getSubmissionType().equals(SubmissionType.PRIDE)) {
            List<DataFile> dataFiles = submission.getDataFiles();
            for (DataFile dataFile : dataFiles) {
                if (dataFile.isFile() && dataFile.getFileType().equals(ProjectFileType.RESULT)) {
                    Assay assay = makeAssay(dataFile);
                    File resultFile = fileFinder.find(dataFile.getFile());
                    MassSpecFileFormat fileFormat = MassSpecFileFormat.checkFormat(resultFile);
                    AssayFileScanner assayFileScanner = AssayFileScannerFactory.getInstance().getAssayFileScanner(fileFormat, fileFinder);
                    AssayFileSummary fileSummary = assayFileScanner.scan(assay, dataFile);
                    enrichAssay(assay, fileSummary);
                    assays.add(assay);
                }
            }
        }

        return assays;
    }

    private static Assay makeAssay(DataFile dataFile) throws DataAccessException, IOException {
        Assay assay = new Assay();

        // accession
        String accession = dataFile.getAssayAccession();
        assay.setAccession(accession);

        // sample
        List<AssaySampleCvParam> samples = new ArrayList<AssaySampleCvParam>();
        SampleMetaData sampleMetaData = dataFile.getSampleMetaData();
        samples.addAll(DataConversionUtil.convertAssaySampleCvParams(assay, sampleMetaData.getMetaData(SampleMetaData.Type.SPECIES)));
        samples.addAll(DataConversionUtil.convertAssaySampleCvParams(assay, sampleMetaData.getMetaData(SampleMetaData.Type.TISSUE)));
        samples.addAll(DataConversionUtil.convertAssaySampleCvParams(assay, sampleMetaData.getMetaData(SampleMetaData.Type.CELL_TYPE)));
        samples.addAll(DataConversionUtil.convertAssaySampleCvParams(assay, sampleMetaData.getMetaData(SampleMetaData.Type.DISEASE)));
        assay.setSamples(samples);

        // quantification
        Set<uk.ac.ebi.pride.data.model.CvParam> quantification = sampleMetaData.getMetaData(SampleMetaData.Type.QUANTIFICATION_METHOD);
        assay.setQuantificationMethods(DataConversionUtil.convertAssayQuantitationMethodCvParams(assay, quantification));

        // experimental factor
        Set<uk.ac.ebi.pride.data.model.CvParam> experimentFactor = sampleMetaData.getMetaData(SampleMetaData.Type.EXPERIMENTAL_FACTOR);
        if (!experimentFactor.isEmpty()) {
            //do it this way because experiment factor is stored as the value of a single user param
            assay.setExperimentalFactor(experimentFactor.iterator().next().getValue());
        }

        return assay;
    }

    private void enrichAssay(Assay assay, AssayFileSummary assayFileSummary) {

        // set assay title
        assay.setTitle(assayFileSummary.getName());

        // set short label
        assay.setShortLabel(assayFileSummary.getShortLabel());

        // protein count
        assay.setProteinCount(assayFileSummary.getNumberOfProteins());

        // peptide count
        assay.setPeptideCount(assayFileSummary.getNumberOfPeptides());

        // unique peptide count
        assay.setUniquePeptideCount(assayFileSummary.getNumberOfUniquePeptides());

        // identified spectrum count
        assay.setIdentifiedSpectrumCount(assayFileSummary.getNumberOfIdentifiedSpectra());

        // total spectrum count
        assay.setTotalSpectrumCount(assayFileSummary.getNumberOfSpectra());

        // ms2 annotation
        assay.setMs2Annotation(assayFileSummary.hasMs2Annotation());

        // chromatogram
        assay.setChromatogram(assayFileSummary.hasChromatogram());

        // softwares
        assay.setSoftwares(DataConversionUtil.convertSoftware(assay, assayFileSummary.getSoftwares()));

        // ptms
        assay.setPtms(DataConversionUtil.convertAssayPTMs(assay, assayFileSummary.getPtms()));

        //contact
        assay.setContacts(DataConversionUtil.convertContact(assay, assayFileSummary.getContacts()));

        //additional params
        assay.setAssayGroupCvParams(DataConversionUtil.convertAssayGroupCvParams(assay, assayFileSummary.getAdditional()));
        assay.setAssayGroupUserParams(DataConversionUtil.convertAssayGroupUserParams(assay, assayFileSummary.getAdditional()));

        //instrument
        Collection<Instrument> instruments = new HashSet<Instrument>();
        for (Instrument instrument : assayFileSummary.getInstruments()) {
            instrument.setAssay(assay);
            instruments.add(instrument);
        }
        assay.setInstruments(instruments);
    }

    /**
     * Generate project based on a given submission and a set of assays
     *
     * @param projectAccession project accession
     * @param doi              doi
     * @param submitter        submitter
     * @param submission       submission object
     * @param assays           a set of assays
     * @return project object
     */
    public Project makeProject(final String projectAccession,
                               final String doi,
                               final User submitter,
                               final Submission submission,
                               final Collection<Assay> assays) {
        Project project = new Project();

        ProjectMetaData projectMetaData = submission.getProjectMetaData();

        // project accession
        project.setAccession(projectAccession);

        // doi
        project.setDoi(doi);

        // submitter
        project.setSubmitter(submitter);

        // lab head
        Contact labHeadContact = projectMetaData.getLabHeadContact();
        if (labHeadContact.getName() != null &&
                labHeadContact.getEmail() != null &&
                labHeadContact.getAffiliation() != null) {

            LabHead labHead = DataConversionUtil.convertLabHead(project, labHeadContact);
            List<LabHead> labHeads = new ArrayList<LabHead>();
            labHeads.add(labHead);
            project.setLabHeads(labHeads);
        }

        // project title
        project.setTitle(projectMetaData.getProjectTitle());

        // project description
        project.setProjectDescription(projectMetaData.getProjectDescription());

        // project tags
        project.setProjectTags(DataConversionUtil.convertProjectTags(project, projectMetaData.getProjectTags()));

        // sample processing protocol
        project.setSampleProcessingProtocol(projectMetaData.getSampleProcessingProtocol());

        // data processing protocol
        project.setDataProcessingProtocol(projectMetaData.getDataProcessingProtocol());

        // other omics link
        project.setOtherOmicsLink(projectMetaData.getOtherOmicsLink());

        // keywords
        project.setKeywords(projectMetaData.getKeywords());

        // submission type
        project.setSubmissionType(projectMetaData.getSubmissionType());

        //submission date
        project.setSubmissionDate(Calendar.getInstance().getTime());
        project.setUpdateDate(project.getSubmissionDate());

        // experiment type
        project.setExperimentTypes(DataConversionUtil.convertProjectExperimentTypeCvParams(project, projectMetaData.getMassSpecExperimentMethods()));

        // additional
        List<ProjectGroupCvParam> additionals = new ArrayList<ProjectGroupCvParam>();
        additionals.addAll(DataConversionUtil.convertProjectGroupCvParams(project, projectMetaData.getAdditional()));
        project.setProjectGroupCvParams(additionals);
        List<ProjectGroupUserParam> additionalUserParams = new ArrayList<ProjectGroupUserParam>();
        additionalUserParams.addAll(DataConversionUtil.convertProjectGroupUserParams(project, projectMetaData.getAdditional()));
        project.setProjectGroupUserParams(additionalUserParams);

        // pubmed
        project.setReferences(DataConversionUtil.convertReferences(project, projectMetaData.getPubmedIds()));

        // reanalysis px
        project.setReanalysis(DataConversionUtil.combineToString(projectMetaData.getReanalysisAccessions(), Constant.COMMA));

        // set to private
        project.setPublicProject(false);

        // set number of assays
        project.setNumAssays(assays.size());

        // sample
        List<ProjectSampleCvParam> samples = new ArrayList<ProjectSampleCvParam>();
        samples.addAll(DataConversionUtil.convertProjectSampleCvParams(project, projectMetaData.getSpecies()));
        samples.addAll(DataConversionUtil.convertProjectSampleCvParams(project, projectMetaData.getTissues()));
        samples.addAll(DataConversionUtil.convertProjectSampleCvParams(project, projectMetaData.getCellTypes()));
        samples.addAll(DataConversionUtil.convertProjectSampleCvParams(project, projectMetaData.getDiseases()));
        project.setSamples(samples);

        // modification
        List<ProjectPTM> ptms = new ArrayList<ProjectPTM>();
        ptms.addAll(DataConversionUtil.convertProjectPTMs(project, projectMetaData.getModifications()));
        project.setPtms(ptms);

        // quant method
        List<ProjectQuantificationMethodCvParam> quantificationMethods = new ArrayList<ProjectQuantificationMethodCvParam>();
        quantificationMethods.addAll(DataConversionUtil.convertProjectQuantificationMethodCvParams(project, projectMetaData.getQuantifications()));
        project.setQuantificationMethods(quantificationMethods);

        // instrument
        List<ProjectInstrumentCvParam> instruments = new ArrayList<ProjectInstrumentCvParam>();
        instruments.addAll(DataConversionUtil.convertProjectInstruments(project, projectMetaData.getInstruments()));
        project.setInstruments(instruments);

        for (Assay assay : assays) {
            //softwares
            Collection<ProjectSoftwareCvParam> projectSoftwares = new HashSet<ProjectSoftwareCvParam>();
            if (project.getSoftware() != null) {
                projectSoftwares.addAll(project.getSoftware());
            }

            for (Software software : assay.getSoftwares()) {
                ProjectSoftwareCvParam psCvParam = new ProjectSoftwareCvParam();
                CvParam softwareCvParam = new CvParam();
                softwareCvParam.setCvLabel(Constant.MS);
                softwareCvParam.setName(Constant.MS_SOFTWARE_NAME);
                softwareCvParam.setAccession(Constant.MS_SOFTWARE_AC);
                psCvParam.setCvParam(softwareCvParam);

                StringBuilder sb = new StringBuilder();
                sb.append(software.getName());
                if (software.getVersion() != null) {
                    sb.append(' ').append(software.getVersion());
                }
                psCvParam.setValue(sb.toString().trim());
                psCvParam.setProject(project);
                projectSoftwares.add(psCvParam);
            }
            project.setSoftware(projectSoftwares);

            // modifications
            Collection<AssayPTM> assayPtms = assay.getPtms();
            Set<ProjectPTM> projectPtms = new HashSet<ProjectPTM>();
            if (project.getPtms() != null) {
                projectPtms.addAll(project.getPtms());
            }

            for (AssayPTM ptm : assayPtms) {
                projectPtms.add(DataConversionUtil.convertAssayPTMtoProjectPTM(project, ptm));
            }

            projectPtms.addAll(project.getPtms());
            project.setPtms(projectPtms);
        }

        return project;
    }

    /**
     * Generate a list of project files for a given submission
     *
     * @param submission submission object
     * @return a map of assay accession to project files
     */
    public Map<ProjectFile, String> makeFiles(final Submission submission) throws IOException {
        Map<ProjectFile, String> projectFiles = new HashMap<ProjectFile, String>();

        List<DataFile> dataFiles = submission.getDataFiles();

        for (DataFile dataFile : dataFiles) {
            ProjectFile projectFile = new ProjectFile();

            // file type
            ProjectFileType fileType = dataFile.getFileType();
            projectFile.setFileType(fileType);

            // file size
            File file = dataFile.getFile();
            File realFile = fileFinder.find(file);
            projectFile.setFileSize(realFile.length());

            // file name
            projectFile.setFileName(file.getName());

            // file source
            projectFile.setFileSource(ProjectFileSource.SUBMITTED);

            String assayAccession = getAssayAccession(dataFile, dataFiles);
            projectFiles.put(projectFile, assayAccession);

        }

        return projectFiles;
    }

    private String getAssayAccession(DataFile dataFile, List<DataFile> dataFiles) {
        String assayAccession = null;

        if (dataFile.getFileType().equals(ProjectFileType.RESULT)) {
            assayAccession = dataFile.getAssayAccession();
        } else {
            for (DataFile data : dataFiles) {
                if (data.getFileMappings().contains(dataFile)
                        && data.getFileType().equals(ProjectFileType.RESULT)) {
                    assayAccession = data.getAssayAccession();
                }
            }
        }

        if ("".equals(assayAccession)) {
            assayAccession = null;
        }

        return assayAccession;
    }
}
