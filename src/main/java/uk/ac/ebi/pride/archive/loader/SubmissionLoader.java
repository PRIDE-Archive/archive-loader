package uk.ac.ebi.pride.archive.loader;

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
import uk.ac.ebi.pride.archive.repo.assay.Assay;
import uk.ac.ebi.pride.archive.repo.assay.AssayRepository;
import uk.ac.ebi.pride.archive.repo.file.ProjectFile;
import uk.ac.ebi.pride.archive.repo.file.ProjectFileRepository;
import uk.ac.ebi.pride.archive.repo.param.CvParam;
import uk.ac.ebi.pride.archive.repo.param.CvParamRepository;
import uk.ac.ebi.pride.archive.repo.project.Project;
import uk.ac.ebi.pride.archive.repo.project.ProjectRepository;

import java.util.Collection;
import java.util.Map;

/**
 * @author Rui Wang
 * @version $Id$
 */
public class SubmissionLoader {

    public static final Logger logger = LoggerFactory.getLogger(SubmissionLoader.class);

    private final TransactionTemplate transactionTemplate;
    private final ProjectRepository projectDao;
    private final AssayRepository assayDao;
    private final ProjectFileRepository projectFileDao;
    private final CvParamManager cvParamManager;


    public SubmissionLoader(ProjectRepository projectDao,
                            AssayRepository assayDao,
                            ProjectFileRepository projectFileDao,
                            CvParamRepository cvParamDao,
                            PlatformTransactionManager transactionManager) {
        this.projectDao = projectDao;
        this.assayDao = assayDao;
        this.projectFileDao = projectFileDao;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.cvParamManager = new CvParamManager(cvParamDao);
    }


    public void persistSubmission(final Project project,
                                  final Collection<Assay> assays,
                                  final Map<ProjectFile, String> projectFiles) {
        // create a new transaction call object
        ProjectLoaderTransactionCallback<Boolean> transactionCallback = new ProjectLoaderTransactionCallback<Boolean>() {

            @Override
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
                               final Collection<Assay> assays) {
        for (Assay assay : assays) {
            Long id = project.getId();
            assay.setProjectId(id);

            AssayCvParamFinder assayCvParamFinder = new AssayCvParamFinder();
            Collection<CvParam> cvParams = assayCvParamFinder.find(assay);
            cvParamManager.persistCvParams(cvParams);

            assayDao.save(assay);
        }
    }

    /**
     * This method persist project to the repository, and assumes that project object contains all the metadata
     *
     * @param project project object
     */
    private void persistProject(final Project project) {
        ProjectCvParamFinder projectCvParamFinder = new ProjectCvParamFinder();
        Collection<CvParam> cvParams = projectCvParamFinder.find(project);
        cvParamManager.persistCvParams(cvParams);

        projectDao.save(project);
    }

    private void persistFiles(final Project project,
                              final Collection<Assay> assays,
                              final Map<ProjectFile, String> projectFiles) {

        for (Map.Entry<ProjectFile, String> projectFileEntry : projectFiles.entrySet()) {
            String assayAccession = projectFileEntry.getValue();
            ProjectFile projectFile = projectFileEntry.getKey();

            // set project id
            projectFile.setProjectId(project.getId());

            Long assayId = getAssayId(assayAccession, assays);
            projectFile.setAssayId(assayId);

            projectFileDao.save(projectFile);

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
