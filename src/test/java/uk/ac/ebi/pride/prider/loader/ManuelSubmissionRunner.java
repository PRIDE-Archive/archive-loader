package uk.ac.ebi.pride.prider.loader;

import org.junit.Before;
import org.junit.Test;

/**
 * To run this class, you need to modify the test-context.xml file
 *
 * Comment out the hsql data source and uncomment the oracle data source
 *
 * Also, comment out the hsql database from the entity manager factory and uncomment
 * the oracle database
 *
 * @author Rui Wang
 * @version $Id$
 */
public class ManuelSubmissionRunner extends AbstractLoaderTest {

    private SubmissionLoader loader;

    @Before
    public void setUp() throws Exception {
        this.loader = new SubmissionLoader(projectDao, assayDao, projectFileDao, cvParamDao, transactionManager);
    }

    @Test
    public void submit() throws Exception {

//        File submissionFile = new File("/Data_Examples/Test/PRD000020/submission.px");
//        Submission submission = SubmissionFileParser.parse(submissionFile);
//
//        String filePath = submissionFile.getAbsolutePath().replace(submissionFile.getName(), "");
//        File rootPath = new File(filePath);
//        SubmissionMaker maker = new SubmissionMaker(new Pride3FileFinder(rootPath));
//        List<Assay> assaysToPersist = maker.makeAssays(submission);
//
//        String userName = submission.getProjectMetaData().getSubmitterContact().getUserName();
//        User submitterToPersist = userDao.findByEmail(userName);
//
//        String resubmissionPxAccession = submission.getProjectMetaData().getResubmissionPxAccession();
//        Project project = maker.makeProject(resubmissionPxAccession, null, submitterToPersist, submission, assaysToPersist);
//
//        Map<ProjectFile,String> projectFiles = maker.makeFiles(submission);
//
//        loader.persistSubmission(project, assaysToPersist, projectFiles);
    }
}