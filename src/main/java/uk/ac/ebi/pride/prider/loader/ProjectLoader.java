package uk.ac.ebi.pride.prider.loader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;
import uk.ac.ebi.pride.data.controller.DataAccessException;
import uk.ac.ebi.pride.data.io.SubmissionFileParser;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.model.ProjectMetaData;
import uk.ac.ebi.pride.data.model.SampleMetaData;
import uk.ac.ebi.pride.data.model.Submission;
import uk.ac.ebi.pride.data.util.MassSpecFileType;
import uk.ac.ebi.pride.data.util.SubmissionType;
import uk.ac.ebi.pride.prider.loader.assay.AssayFactory;
import uk.ac.ebi.pride.prider.loader.exception.ProjectLoaderException;
import uk.ac.ebi.pride.prider.loader.util.Constant;
import uk.ac.ebi.pride.prider.loader.util.DataConversionUtil;
import uk.ac.ebi.pride.prider.repo.assay.Assay;
import uk.ac.ebi.pride.prider.repo.assay.AssayRepository;
import uk.ac.ebi.pride.prider.repo.file.ProjectFile;
import uk.ac.ebi.pride.prider.repo.file.ProjectFileRepository;
import uk.ac.ebi.pride.prider.repo.instrument.Instrument;
import uk.ac.ebi.pride.prider.repo.param.CvParam;
import uk.ac.ebi.pride.prider.repo.project.Project;
import uk.ac.ebi.pride.prider.repo.project.ProjectRepository;
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
    private UserRepository userDao;
    private ProjectRepository projectDao;
    private AssayRepository assayDao;
    private ProjectFileRepository projectFileDao;


    public ProjectLoader(DataSource dataSource,
                         UserRepository userDao,
                         ProjectRepository projectDao,
                         AssayRepository assayDao,
                         ProjectFileRepository projectFileDao) {
        Assert.notNull(dataSource, "Data source cannot be null");

        this.transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        this.userDao = userDao;
        this.projectDao = projectDao;
        this.assayDao = assayDao;
        this.projectFileDao = projectFileDao;
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
                    Map<String, Long> assayIdMappings = loadAssays(assays);

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
                if (dataFile.isFile() && dataFile.getFileType().equals(MassSpecFileType.RESULT)) {
                    assays.add(AssayFactory.makeAssay(dataFile, dataFile.getSampleMetaData()));
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
        project.setKeywords(projectMetaData.getOtherOmicsLink());

        // submission type
        project.setSubmissionType(DataConversionUtil.convertSubmissionType(projectMetaData.getSubmissionType()));

        // experiment type
        project.setExperimentTypes(DataConversionUtil.convertCvParams(projectMetaData.getMassSpecExperimentMethods()));

        // species
        List<CvParam> samples = new ArrayList<CvParam>();
        samples.addAll(DataConversionUtil.convertCvParams(projectMetaData.getSpecies()));
        project.setSamples(samples);

        // instrument
        List<Param> instruments = new ArrayList<Param>();
        instruments.addAll(DataConversionUtil.convertParams(projectMetaData.getInstruments()));
        project.setInstruments(instruments);

        // modification
        List<CvParam> ptms = new ArrayList<CvParam>();
        ptms.addAll(DataConversionUtil.convertCvParams(projectMetaData.getModifications()));
        project.setPtms(ptms);

        // additional
        List<Param> additionals = new ArrayList<Param>();
        additionals.addAll(DataConversionUtil.convertParams(projectMetaData.getAdditional()));
        project.setParams(additionals);

        // pubmed
        project.setReferences(DataConversionUtil.convertReferences(projectMetaData.getPubmedIds()));

        // reanalysis px
        project.setReanalysis(DataConversionUtil.combineToString(projectMetaData.getReanalysisAccessions(), Constant.COMMA));

        // load assay details
        mergeAssayDetails(project, assays);

        // load project
        projectDao.save(project);

        return null;
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
            Collection<CvParam> samples = assay.getSamples();
            Collection<CvParam> projectSamples = project.getSamples();
            for (CvParam sample : samples) {
                if (!projectSamples.contains(sample)) {
                    projectSamples.add(sample);
                }
            }

            // instrument
            Collection<Instrument> instruments = assay.getInstruments();
            Collection<Param> projectInstruments = project.getInstruments();
            for (Instrument instrument : instruments) {
                Param model = instrument.getModel();
                if (!projectInstruments.contains(model)) {
                    projectInstruments.add(model);
                }
            }

            // modifications
            Collection<CvParam> ptms = assay.getPtms();
            Collection<CvParam> projectPtms = project.getPtms();
            for (CvParam ptm : ptms) {
                if (!projectPtms.contains(ptm)) {
                    projectPtms.add(ptm);
                }
            }

            // quantifications
            Collection<CvParam> quants = assay.getQuantificationMethods();
            Collection<CvParam> projectQuants = project.getQuantificationMethods();
            for (CvParam quant : quants) {
                if (!projectQuants.contains(quant)) {
                    projectQuants.add(quant);
                }
            }
        }
    }


    /**
     * Load assays
     *
     * @param assays a collection of assays
     * @throws ProjectLoaderException exception indicates error when loading assays
     */
    private Map<String, Long> loadAssays(List<Assay> assays) throws ProjectLoaderException {
        Map<String, Long> assayAccToIdMappings = new HashMap<String, Long>();

        for (Assay assay : assays) {
            String accession = assay.getAccession();
            assayDao.save(assay);
            Long id = assay.getId();
            if (id == null) {
                logAndThrowException("Failed to persist assay: " + accession);
            }

            assayAccToIdMappings.put(accession, id);
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
            MassSpecFileType fileType = dataFile.getFileType();
            if (fileType.equals(MassSpecFileType.RESULT)) {
                String assayAccession = dataFile.getAssayAccession();
                Long assayId = assayIdMappings.get(assayAccession);
                if (assayId != null) {
                    projectFile.setAssayId(assayId);
                } else {
                    logAndThrowException("Assay id cannot be null: " + assayAccession);
                }
            }

            // file type
            projectFile.setFileType(DataConversionUtil.convertProjectFileType(fileType));

            // file size
            File file = dataFile.getFile();
            projectFile.setFileSize(file.length());

            // file name
            projectFile.setFileName(file.getName());

            // file path
            projectFile.setFilePath(file.getAbsolutePath());

            // persist
            projectFileDao.saveFile(projectFile);
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
