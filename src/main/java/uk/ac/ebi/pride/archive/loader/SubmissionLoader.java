package uk.ac.ebi.pride.archive.loader;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import uk.ac.ebi.pride.archive.loader.exception.SubmissionLoaderException;
import uk.ac.ebi.pride.archive.loader.param.AssayCvParamFinder;
import uk.ac.ebi.pride.archive.loader.param.ProjectCvParamFinder;
import uk.ac.ebi.pride.archive.loader.util.CvParamManager;
import uk.ac.ebi.pride.archive.repo.client.AssayRepoClient;
import uk.ac.ebi.pride.archive.repo.client.CvParamRepoClient;
import uk.ac.ebi.pride.archive.repo.client.FileRepoClient;
import uk.ac.ebi.pride.archive.repo.client.ProjectRepoClient;
import uk.ac.ebi.pride.archive.repo.models.assay.Assay;
import uk.ac.ebi.pride.archive.repo.models.file.ProjectFile;
import uk.ac.ebi.pride.archive.repo.models.param.CvParam;
import uk.ac.ebi.pride.archive.repo.models.project.Project;

import java.util.Collection;
import java.util.Map;

/**
 * @author Rui Wang
 * @version $Id$
 */
public class SubmissionLoader {

    public static final Logger logger = LoggerFactory.getLogger(SubmissionLoader.class);

    private final TransactionTemplate transactionTemplate;
    private final ProjectRepoClient projectRepoClient;
    private final AssayRepoClient assayRepoClient;
    private final FileRepoClient fileRepoClient;
    private final CvParamManager cvParamManager;

    public SubmissionLoader(ProjectRepoClient projectRepoClient,
                            AssayRepoClient assayRepoClient,
                            FileRepoClient fileRepoClient,
                            CvParamRepoClient cvParamRepoClient,
                            PlatformTransactionManager transactionManager) {
        this.projectRepoClient = projectRepoClient;
        this.assayRepoClient = assayRepoClient;
        this.fileRepoClient = fileRepoClient;
        this.cvParamManager = new CvParamManager(cvParamRepoClient);
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }


    public void persistSubmission(final Project project,
                                  final Collection<Assay> assays,
                                  final Map<ProjectFile, String> projectFiles) {
        // create a new transaction call object
        ProjectLoaderTransactionCallback<Boolean> transactionCallback = new ProjectLoaderTransactionCallback<Boolean>() {

            public Boolean doInTransaction(TransactionStatus status) {
                boolean success = true;
                try {
                    //persist project
                    persistProject(project);

                    //persist assays
                    if (!assays.isEmpty()) {
                        persistAssays(project, assays);
                    }

                    // persist project related files
                    persistFiles(project, assays, projectFiles);

                } catch (Exception ex) {
                    status.setRollbackOnly();
                    this.setException(ex);
                    logger.error(ex.getMessage());
                    success = false;
                }

                return success;
            }
        };

        // start a new transaction for loading the data
        boolean succeedInProjectLoading = transactionTemplate.execute(transactionCallback);

        // check the loading result status
        if (!succeedInProjectLoading) {
            String msg = "Failed to load project: " + project.getAccession();
            logger.error(msg);
            throw new SubmissionLoaderException(msg, transactionCallback.getException());
        }
    }

    private void persistAssays(final Project project,
                               final Collection<Assay> assays) throws JsonProcessingException {
        for (Assay assay : assays) {
            Long id = project.getId();
            assay.setProjectId(id);

            AssayCvParamFinder assayCvParamFinder = new AssayCvParamFinder();
            Collection<CvParam> cvParams = assayCvParamFinder.find(assay);
            cvParamManager.persistCvParams(cvParams);
            logger.info("Saving assay: " + assay.getAccession());
            assayRepoClient.save(assay);
        }
    }

    /**
     * This method persist project to the repository, and assumes that project object contains all the metadata
     *
     * @param project project object
     */
    private void persistProject(final Project project) throws JsonProcessingException {
        ProjectCvParamFinder projectCvParamFinder = new ProjectCvParamFinder();
        Collection<CvParam> cvParams = projectCvParamFinder.find(project);
        cvParamManager.persistCvParams(cvParams);

        projectRepoClient.save(project);
    }

    private void persistFiles(final Project project,
                              final Collection<Assay> assays,
                              final Map<ProjectFile, String> projectFiles) throws JsonProcessingException {

        for (Map.Entry<ProjectFile, String> projectFileEntry : projectFiles.entrySet()) {
            String assayAccession = projectFileEntry.getValue();
            ProjectFile projectFile = projectFileEntry.getKey();

            // set project id
            projectFile.setProjectId(project.getId());

            Long assayId = getAssayId(assayAccession, assays);
            projectFile.setAssayId(assayId);
            logger.info("Saving project files : " + projectFile.getProjectId() + " assayID: " + projectFile.getAssayId() + " file: " +  projectFile.getFileName());
            fileRepoClient.save(projectFile);

        }
    }

    private Long getAssayId(String assayAccession, Collection<Assay> assays) {
        for (Assay assay : assays) {
            if (assay.getAccession().equals(assayAccession)) {
                return assay.getId();
            }
        }
        return null;
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
