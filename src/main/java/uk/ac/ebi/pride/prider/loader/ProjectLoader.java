package uk.ac.ebi.pride.prider.loader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;
import uk.ac.ebi.pride.data.controller.DataAccessException;
import uk.ac.ebi.pride.data.io.SubmissionFileParser;
import uk.ac.ebi.pride.data.model.CvParam;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.model.ProjectMetaData;
import uk.ac.ebi.pride.data.model.Submission;
import uk.ac.ebi.pride.prider.dataprovider.file.ProjectFileType;
import uk.ac.ebi.pride.prider.dataprovider.project.SubmissionType;
import uk.ac.ebi.pride.prider.loader.assay.AssayFactory;
import uk.ac.ebi.pride.prider.loader.exception.ProjectLoaderException;
import uk.ac.ebi.pride.prider.loader.util.Constant;
import uk.ac.ebi.pride.prider.loader.util.CvParamManager;
import uk.ac.ebi.pride.prider.loader.util.DataConversionUtil;
import uk.ac.ebi.pride.prider.repo.assay.*;
import uk.ac.ebi.pride.prider.repo.file.ProjectFile;
import uk.ac.ebi.pride.prider.repo.file.ProjectFileRepository;
import uk.ac.ebi.pride.prider.repo.instrument.Instrument;
import uk.ac.ebi.pride.prider.repo.instrument.InstrumentModel;
import uk.ac.ebi.pride.prider.repo.project.*;
import uk.ac.ebi.pride.prider.repo.user.User;
import uk.ac.ebi.pride.prider.repo.user.UserRepository;

import javax.sql.DataSource;
import java.io.File;
import java.util.*;

/**
 * @author Rui Wang
 * @version $Id$
 */
public class ProjectLoader {

    public static final Logger logger = LoggerFactory.getLogger(ProjectLoader.class);

    private TransactionTemplate transactionTemplate;

    @Autowired
    private UserRepository userDao;

    @Autowired
    private ProjectRepository projectDao;

    @Autowired
    private AssayRepository assayDao;

    @Autowired
    private ProjectFileRepository projectFileDao;

    @Autowired
    private DataSource datasource;

    public ProjectLoader() {
        Assert.notNull(datasource, "Data source cannot be null");
        this.transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(datasource));
    }

    public void load(final String projectAccession, final String doi, final String submissionSummaryFile) throws ProjectLoaderException {
        // create a new transaction call object
        ProjectLoaderTransactionCallback<Boolean> transactionCallback = new ProjectLoaderTransactionCallback<Boolean>() {

            @Override
            public Boolean doInTransaction(TransactionStatus status) {
                boolean success = true;
                try {
                    // load submission
                    Submission submission = SubmissionFileParser.parse(new File(submissionSummaryFile));

                    // login as a PRIDE-R user
                    User user = userDao.findByEmail(submission.getProjectMetaData().getContact().getEmail());
                    if (user == null) {
                        logAndThrowException("Failed to identify a existing user: " + submission.getProjectMetaData().getContact().getEmail());
                    }

                    // make sure project accession doesn't exist
                    if (projectDao.findByAccession(projectAccession) != null) {
                        logAndThrowException("Project accession already exists: " + projectAccession);
                    }

                    // make sure all the assay accessions don't exist
                    if (submission.getProjectMetaData().getSubmissionType().equals(SubmissionType.COMPLETE)) {
                        List<DataFile> dataFiles = submission.getDataFiles();
                        for (DataFile dataFile : dataFiles) {
                            String assayAccession = dataFile.getAssayAccession();
                            if (assayAccession != null && assayDao.findByAccession(assayAccession) != null) {
                                logAndThrowException("Assay accession already exists: " + assayAccession);
                            }
                        }
                    }

                    // collect assay details
                    List<Assay> assays = collectAssayDetails(submission);

                    // load project metadata
                    Long projectId = loadProjectMetaData(submission, projectAccession, doi, user, assays);

                    // load assay entry
                    Map<String, Long> assayIdMappings = loadAssays(assays, projectId);

                    // load file mappings
                    loadFileMappings(submission, projectId, assayIdMappings);

                } catch (Exception ex) {
                    status.setRollbackOnly();
                    this.setException(ex);
                    success = false;
                }

                return success;
            }
        };

        // start a new transaction for loading the data
        boolean succeedInProjectLoading = transactionTemplate.execute(transactionCallback);

        // check the loading result status
        if (!succeedInProjectLoading) {
            logAndThrowException("Failed to load project: " + projectAccession);
        }
    }


    /**
     * Create a collection of assay details
     *
     * @param submission submission summary
     * @return a collection of assays
     */
    private List<Assay> collectAssayDetails(Submission submission) throws DataAccessException, ProjectLoaderException {
        List<Assay> assays = new ArrayList<Assay>();

        if (submission.getProjectMetaData().getSubmissionType().equals(SubmissionType.COMPLETE)) {
            List<DataFile> dataFiles = submission.getDataFiles();
            for (DataFile dataFile : dataFiles) {
                if (dataFile.isFile() && dataFile.getFileType().equals(ProjectFileType.RESULT)) {
                    assays.add(AssayFactory.makeAssay(dataFile));
                }
            }
        }

        return assays;
    }

    /**
     * Load project level metadata
     *
     * @param submission       submission summary
     * @param projectAccession project accession
     * @param submitter        user
     * @param assays           a collection of assay details
     * @throws ProjectLoaderException exception indicates error when loading project data
     */
    private Long loadProjectMetaData(Submission submission, String projectAccession, String doi, User submitter, List<Assay> assays) throws ProjectLoaderException {
        Project project = new Project();

        // project accession
        project.setAccession(projectAccession);

        // doi
        project.setDoi(doi);

        // submitter id
        project.setSubmitter(submitter);

        // project title
        ProjectMetaData projectMetaData = submission.getProjectMetaData();
        project.setTitle(projectMetaData.getTitle());

        // project description
        project.setProjectDescription(projectMetaData.getProjectDescription());

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

        // pubmed
        project.setReferences(DataConversionUtil.convertReferences(projectMetaData.getPubmedIds()));

        // reanalysis px
        project.setReanalysis(DataConversionUtil.combineToString(projectMetaData.getReanalysisAccessions(), Constant.COMMA));

        // set to private
        project.setPublicProject(false);

        // set number of assays
        project.setNumAssays(assays.size());

        // load assay details - only done for complete submissions
        if (submission.getProjectMetaData().getSubmissionType().equals(SubmissionType.COMPLETE)) {

            mergeAssayDetails(project, assays);

        } else {

            //for partial submisions, these must be done here

            // sample
            List<ProjectSample> samples = new ArrayList<ProjectSample>();
            samples.addAll(DataConversionUtil.convertProjectSampleCvParams(project, projectMetaData.getSpecies()));
            project.setSamples(samples);

            // instrument
            List<Instrument> instruments = new ArrayList<Instrument>();
            Set<CvParam> instrumentModels = projectMetaData.getInstruments();
            for (CvParam cvParam : instrumentModels) {
                //check to see if the instrument param is already in PRIDE-R
                uk.ac.ebi.pride.prider.repo.param.CvParam repoParam = CvParamManager.getInstance().getCvParam(cvParam.getAccession());
                //if param isn't already seen in db, store it
                if (repoParam == null) {
                    CvParamManager.getInstance().putCvParam(cvParam.getCvLabel(), cvParam.getAccession(), cvParam.getName());
                    repoParam = CvParamManager.getInstance().getCvParam(cvParam.getAccession());
                }
                Instrument instrument = new Instrument();
                InstrumentModel model = new InstrumentModel();
                //instrument model is a wrapper around a cv param
                model.setId(repoParam.getId());
                model.setCvLabel(repoParam.getCvLabel());
                model.setAccession(repoParam.getAccession());
                model.setName(repoParam.getName());
                model.setValue(repoParam.getValue());
                instrument.setModel(model);
                //store assay link
                //todo - this might cause data duplication in case of identical instruments
                //todo - across multiple assays in the scope of a single project
                //todo - needs testing
                instrument.setProjects(Collections.singleton(project));
                instruments.add(instrument);
            }
            project.setInstrument(instruments);

            // modification
            List<ProjectPTM> ptms = new ArrayList<ProjectPTM>();
            ptms.addAll(DataConversionUtil.convertProjectPTMs(project, projectMetaData.getModifications()));
            project.setPtms(ptms);

            // quant method
            List<ProjectQuantificationMethod> quantificationMethods = new ArrayList<ProjectQuantificationMethod>();
            quantificationMethods.addAll(DataConversionUtil.convertProjectQuantificationMethodCvParams(project, projectMetaData.getQuantifications()));
            project.setQuantificationMethods(quantificationMethods);

        }

        // load project
        projectDao.save(project);

        //will return project PK after being persisted
        return project.getId();

    }

    /**
     * Merge a list of assays to project metadata
     *
     * @param project project object
     * @param assays  a collection of assays
     */
    private void mergeAssayDetails(Project project, List<Assay> assays) {

        for (Assay assay : assays) {
            // species
            Collection<AssaySample> samples = assay.getSamples();
            Set<ProjectSample> projectSamples = new HashSet<ProjectSample>();
            projectSamples.addAll(project.getSamples());
            for (AssaySample sample : samples) {
                projectSamples.add(DataConversionUtil.convertAssaySampleToProjectSample(project, sample));
            }
            project.setSamples(projectSamples);

            // instrument
            Set<Instrument> projectInstruments = new HashSet<Instrument>();
            for (Instrument instrument : assay.getInstruments()) {
                //todo - this might cause data duplication in case of identical instruments
                //todo - across multiple assays in the scope of a single project
                //todo - needs testing
                instrument.setProjects(Collections.singleton(project));
                projectInstruments.add(instrument);
            }
            project.setInstrument(projectInstruments);

            // modifications
            Collection<AssayPTM> ptms = assay.getPtms();
            Set<ProjectPTM> projectPtms = new HashSet<ProjectPTM>();
            projectPtms.addAll(project.getPtms());
            for (AssayPTM ptm : ptms) {
                projectPtms.add(DataConversionUtil.convertAssayPTMtoProjectPTM(project, ptm));
            }
            project.setPtms(projectPtms);

            // quantifications
            Collection<AssayQuantificationMethod> quants = assay.getQuantificationMethods();
            Collection<ProjectQuantificationMethod> projectQuants = new HashSet<ProjectQuantificationMethod>();
            projectQuants.addAll(project.getQuantificationMethods());
            for (AssayQuantificationMethod param : quants) {
                projectQuants.add(DataConversionUtil.convertAssayQuantitationMethodToProjectQuantitationMethod(project, param));
            }
            project.setQuantificationMethods(projectQuants);
        }
    }

    /**
     * Load assays
     *
     * @param assays a collection of assays
     * @throws ProjectLoaderException exception indicates error when loading assays
     */
    private Map<String, Long> loadAssays(List<Assay> assays, Long projectId) throws ProjectLoaderException {
        Map<String, Long> assayAccToIdMappings = new HashMap<String, Long>();

        for (Assay assay : assays) {

            //create link to project
            assay.setProjectId(projectId);
            //store to db
            assayDao.save(assay);
            //update mapping
            assayAccToIdMappings.put(assay.getAccession(), assay.getId());
        }

        return assayAccToIdMappings;
    }

    /**
     * Load file mappings
     *
     * @param submission      submission summary
     * @param projectId       project id
     * @param assayIdMappings assay id mappings
     * @throws ProjectLoaderException exception indicates error when loading file mappings
     */
    private void loadFileMappings(Submission submission, Long projectId, Map<String, Long> assayIdMappings) throws ProjectLoaderException {

        List<DataFile> dataFiles = submission.getDataFiles();

        for (DataFile dataFile : dataFiles) {

            ProjectFile projectFile = new ProjectFile();

            // project id
            projectFile.setProjectId(projectId);

            // assay id
            ProjectFileType fileType = dataFile.getFileType();
            if (fileType.equals(ProjectFileType.RESULT)) {
                String assayAccession = dataFile.getAssayAccession();
                Long assayId = assayIdMappings.get(assayAccession);
                if (assayId != null) {
                    projectFile.setAssayId(assayId);
                } else {
                    logAndThrowException("Assay id cannot be null: " + assayAccession);
                }
            }

            // file type
            projectFile.setFileType(fileType);

            // file size
            File file = dataFile.getFile();
            projectFile.setFileSize(file.length());

            // file name
            projectFile.setFileName(file.getName());

            // file path
            projectFile.setFilePath(file.getAbsolutePath());

            // persist
            projectFileDao.save(projectFile);
        }
    }

    /**
     * Log and throw an {@code ProjectLoaderException}
     *
     * @param msg error message
     * @throws ProjectLoaderException
     */
    private void logAndThrowException(String msg) throws ProjectLoaderException {
        logger.error(msg);
        throw new ProjectLoaderException(msg);
    }

    /**
     * {@code ProjectLoaderTransactionCallback} provides logging of exception causing the transaction failed
     *
     * @param <T>
     */
    private abstract class ProjectLoaderTransactionCallback<T> implements TransactionCallback<T> {

        private Exception exception;

        public Exception getException() {
            return exception;
        }

        public void setException(Exception exception) {
            this.exception = exception;
        }
    }

}
