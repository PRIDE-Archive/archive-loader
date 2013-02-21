package uk.ac.ebi.pride.prider.loader;

import junit.framework.Assert;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;
import uk.ac.ebi.pride.prider.dataprovider.project.SubmissionType;
import uk.ac.ebi.pride.prider.loader.util.CvParamManager;
import uk.ac.ebi.pride.prider.repo.project.Project;

import java.io.File;
import java.net.URL;

/**
 * Created with IntelliJ IDEA.
 * User: rcote
 * Date: 04/02/13
 * Time: 14:43
 */
public class ProjectLoaderPartialSubmissionTest extends AbstractLoaderTest {

    @Test
    public void LoaderTest() throws Exception {

        ProjectLoader loader = new ProjectLoader(userDao, projectDao, assayDao, projectFileDao, transactionManager);
        CvParamManager paramManager = CvParamManager.getInstance();
        paramManager.setCvParamDao(cvParamDao);
        loader.load("456798", null, submissionFile.getPath());

        Project loadedProject = projectDao.findByAccession("456798");
        Assert.assertEquals("john.smith@dummy.ebi.com", loadedProject.getSubmitter().getEmail());
        Assert.assertEquals(null, loadedProject.getDoi());
        Assert.assertEquals("partial submission title", loadedProject.getTitle());
        Assert.assertEquals("partial submission keywords", loadedProject.getKeywords());
        Assert.assertEquals("partial submission processing protocol", loadedProject.getDataProcessingProtocol());
        Assert.assertEquals("partial submission sample protocol", loadedProject.getSampleProcessingProtocol());
        Assert.assertEquals(0, loadedProject.getNumAssays());
        Assert.assertEquals(SubmissionType.PARTIAL, loadedProject.getSubmissionType());
        Assert.assertEquals(false, loadedProject.isPublicProject());
        Assert.assertEquals(1, loadedProject.getPtms().size());
        Assert.assertEquals(0, loadedProject.getReferences().size());
        Assert.assertEquals("Shotgun proteomics", loadedProject.getExperimentTypes().iterator().next().getName());
        Assert.assertEquals(1, loadedProject.getProjectGroupUserParams().size());
        Assert.assertEquals("partial submission experiment comment", loadedProject.getProjectGroupUserParams().iterator().next().getValue());
        Assert.assertEquals(1, loadedProject.getSamples().size());
        Assert.assertEquals("7460", loadedProject.getSamples().iterator().next().getAccession());
        Assert.assertEquals(1, loadedProject.getExperimentTypes().size());
        Assert.assertEquals("Shotgun proteomics", loadedProject.getExperimentTypes().iterator().next().getName());
        Assert.assertEquals(1, loadedProject.getInstruments().size());
        Assert.assertEquals("LTQ Orbitrap", loadedProject.getInstruments().iterator().next().getName());
        Assert.assertEquals(1, loadedProject.getPtms().size());
        Assert.assertEquals("phosphorylated residue", loadedProject.getPtms().iterator().next().getName());
        Assert.assertEquals(0, loadedProject.getSoftware().size());

    }

    @Before
    public void setUp() throws Exception {

        URL url = ProjectLoaderPartialSubmissionTest.class.getClassLoader().getResource("px_files_partial/submission.px");
        //copy submission file
        File file1 = new File(temporaryFolder.getRoot(), "submission.px");
        FileUtils.copyFile(new File(url.toURI()), file1);
        //use copy of original file
        submissionFile = new FileSystemResource(file1);

    }

}