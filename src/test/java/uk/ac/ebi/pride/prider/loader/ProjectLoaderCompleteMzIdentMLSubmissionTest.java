package uk.ac.ebi.pride.prider.loader;

import junit.framework.Assert;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import uk.ac.ebi.pride.data.io.SubmissionFileParser;
import uk.ac.ebi.pride.data.io.SubmissionFileWriter;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.model.Submission;
import uk.ac.ebi.pride.prider.dataprovider.project.SubmissionType;
import uk.ac.ebi.pride.prider.loader.util.CvParamManager;
import uk.ac.ebi.pride.prider.repo.assay.Assay;
import uk.ac.ebi.pride.prider.repo.assay.Contact;
import uk.ac.ebi.pride.prider.repo.project.Project;

import java.io.File;
import java.net.URL;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: rcote
 * Date: 04/02/13
 * Time: 14:43
 */
public class ProjectLoaderCompleteMzIdentMLSubmissionTest extends AbstractLoaderTest {

    @Test
    public void LoaderTest() throws Exception {

        ProjectLoader loader = new ProjectLoader(userDao, projectDao, assayDao, projectFileDao, transactionManager);
        CvParamManager paramManager = CvParamManager.getInstance();
        paramManager.setCvParamDao(cvParamDao);
        loader.load("88888", "88888", submissionFile.getPath());

        Project loadedProject = projectDao.findByAccession("88888");
        Assert.assertEquals("john.smith@dummy.ebi.com", loadedProject.getSubmitter().getEmail());
        Assert.assertEquals("88888", loadedProject.getDoi());
        Assert.assertEquals("mzidentml test", loadedProject.getTitle());
        Assert.assertEquals("mzidentml test", loadedProject.getKeywords());
        Assert.assertEquals(1, loadedProject.getNumAssays());
        Assert.assertEquals(SubmissionType.COMPLETE, loadedProject.getSubmissionType());
        Assert.assertEquals(false, loadedProject.isPublicProject());
        Assert.assertEquals(1, loadedProject.getPtms().size());
        Assert.assertEquals(0, loadedProject.getReferences().size());
        Assert.assertEquals("SRM/MRM", loadedProject.getExperimentTypes().iterator().next().getName());
        Assert.assertEquals(0, loadedProject.getProjectGroupUserParams().size());
        Assert.assertEquals(0, loadedProject.getProjectGroupCvParams().size());
        Assert.assertEquals(1, loadedProject.getQuantificationMethods().size());
        Assert.assertEquals("iTRAQ", loadedProject.getQuantificationMethods().iterator().next().getName());
        Assert.assertEquals(1, loadedProject.getInstruments().size());
        Assert.assertEquals("LTQ", loadedProject.getInstruments().iterator().next().getName());
        Assert.assertEquals(2, loadedProject.getSoftware().size());
        Assert.assertEquals("mzidentml test test data protocol", loadedProject.getDataProcessingProtocol());
        Assert.assertEquals("mzidentml test test sample protocol", loadedProject.getSampleProcessingProtocol());

        List<Assay> assays = assayDao.findAllByProjectId(loadedProject.getId());
        Assay assay = assays.iterator().next();
        Assert.assertEquals(1, assays.size());
        Assert.assertEquals(null, assay.getShortLabel());
        Assert.assertEquals("Unknown experiment (mzIdentML)", assay.getTitle());
        Assert.assertEquals("1234", assay.getAccession());
        Assert.assertEquals(7, assay.getProteinCount());
        Assert.assertEquals(11, assay.getPeptideCount());
        Assert.assertEquals(7, assay.getUniquePeptideCount());
//        todo - this fails
        Assert.assertEquals(39, assay.getTotalSpectrumCount());
        Assert.assertEquals(11, assay.getIdentifiedSpectrumCount());
        Assert.assertEquals(0, assay.getInstruments().size());
        Assert.assertEquals(2, assay.getSoftwares().size());
        Assert.assertEquals(1, assay.getPtms().size());
        Assert.assertEquals("UNIMOD:35", assay.getPtms().iterator().next().getAccession());
        Assert.assertEquals(1, assay.getContacts().size());

        Contact contact = assay.getContacts().iterator().next();
        Assert.assertEquals("John", contact.getFirstName());
        Assert.assertEquals("Doe", contact.getLastName());
        Assert.assertEquals("Matrix Science Limited", contact.getAffiliation());
        Assert.assertEquals("john.doe@nowhere.com", contact.getEmail());

    }

    @Before
    public void setUp() throws Exception {

        URL url = ProjectLoaderCompleteMzIdentMLSubmissionTest.class.getClassLoader().getResource("px_files_mzidentml/submission.px");
        //copy submission file
        File file1 = new File(temporaryFolder.getRoot(), "submission.px");
        FileUtils.copyFile(new File(url.toURI()), file1);
        //use copy of original file
        submissionFile = new FileSystemResource(file1);

        //copy prideXML file as well to temporary folder
        url = ProjectLoaderCompleteMzIdentMLSubmissionTest.class.getClassLoader().getResource("px_files_mzidentml/F001261.mzid");
        file1 = new File(temporaryFolder.getRoot(), "F001261.mzid");
        FileUtils.copyFile(new File(url.toURI()), file1);
        url = ProjectLoaderCompleteMzIdentMLSubmissionTest.class.getClassLoader().getResource("px_files_mzidentml/data-no-pitc-34-filter.mgf");
        file1 = new File(temporaryFolder.getRoot(), "data-no-pitc-34-filter.mgf");
        FileUtils.copyFile(new File(url.toURI()), file1);

        //and update path in submission,px file to point to pride.xml file in tmp folder
        updatePrideXMLFilePath(submissionFile);

    }

    private void updatePrideXMLFilePath(Resource submissionFile) throws Exception {
        //check if submission file has new accession
        Submission submission = SubmissionFileParser.parse(submissionFile.getFile());
        List<DataFile> dataFiles = submission.getDataFiles();
        //update the prideXML file path to point to the "real" file in the tmp folder
        for (DataFile dataFile : dataFiles) {
            String filename = dataFile.getFile().getName();
            dataFile.setFile(new File(temporaryFolder.getRoot(), filename));
        }
        SubmissionFileWriter.write(submission, submissionFile.getFile());
    }


}
