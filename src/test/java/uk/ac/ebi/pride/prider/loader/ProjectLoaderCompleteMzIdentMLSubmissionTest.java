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
import uk.ac.ebi.pride.prider.repo.assay.instrument.Instrument;
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
        Assert.assertEquals("Test PX data set", loadedProject.getTitle());
        Assert.assertEquals("liver, human, bruker LTQ", loadedProject.getKeywords());
        Assert.assertEquals(4, loadedProject.getNumAssays());
        Assert.assertEquals(SubmissionType.COMPLETE, loadedProject.getSubmissionType());
        Assert.assertEquals(false, loadedProject.isPublicProject());
        Assert.assertEquals(2, loadedProject.getPtms().size());
        Assert.assertEquals(1, loadedProject.getReferences().size());
        Assert.assertEquals("10.1074/mcp.M800008-MCP200", loadedProject.getReferences().iterator().next().getDoi());
        Assert.assertEquals("Shotgun proteomics", loadedProject.getExperimentTypes().iterator().next().getName());
        Assert.assertEquals(1, loadedProject.getProjectGroupUserParams().size());
        Assert.assertEquals(1, loadedProject.getProjectGroupCvParams().size());
        Assert.assertEquals(1, loadedProject.getInstruments().size());
        Assert.assertEquals("LTQ FT", loadedProject.getInstruments().iterator().next().getName());
        Assert.assertEquals(1, loadedProject.getSoftware().size());
        Assert.assertEquals("Matrix Science Mascot 2.2.04", loadedProject.getSoftware().iterator().next().getValue());

        List<Assay> assays = assayDao.findAllByProjectId(loadedProject.getId());
        Assert.assertEquals(4, assays.size());

        int proteinCount = 0, uniquePeptideCount = 0, identifiedSpectrumCount = 0, totalSpectrumCount = 0, peptideCount = 0, assayPTMs = 0;
        int instrumentCount = 0, sourceCount = 0, detectorCount = 0, analyzerCount = 0;
        int contactCount = 0;
        for (Assay assay : assays) {
            proteinCount += assay.getProteinCount();
            uniquePeptideCount += assay.getUniquePeptideCount();
            identifiedSpectrumCount += assay.getIdentifiedSpectrumCount();
            totalSpectrumCount += assay.getTotalSpectrumCount();
            peptideCount += assay.getPeptideCount();
            assayPTMs += assay.getPtms().size();
            instrumentCount += assay.getInstruments().size();
            for (Instrument instrument : assay.getInstruments()) {
                sourceCount += instrument.getSources().size();
                detectorCount += instrument.getDetectors().size();
                analyzerCount += instrument.getAnalyzers().size();
            }
            contactCount += assay.getContacts().size();
        }
        Assert.assertEquals(6, proteinCount);
        Assert.assertEquals(4, identifiedSpectrumCount);
        Assert.assertEquals(4, totalSpectrumCount);
        Assert.assertEquals(118, peptideCount);
        Assert.assertEquals(103, uniquePeptideCount);
        Assert.assertEquals(6, assayPTMs);
        Assert.assertEquals(4, instrumentCount);
        Assert.assertEquals(4, sourceCount);
        Assert.assertEquals(4, detectorCount);
        Assert.assertEquals(4, analyzerCount);
        Assert.assertEquals(4, contactCount);

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
        url = ProjectLoaderCompleteMzIdentMLSubmissionTest.class.getClassLoader().getResource("px_files_mzidentml/55merge.mzid");
        file1 = new File(temporaryFolder.getRoot(), "55merge.mzid");
        FileUtils.copyFile(new File(url.toURI()), file1);
        url = ProjectLoaderCompleteMzIdentMLSubmissionTest.class.getClassLoader().getResource("px_files_mzidentml/55merge.mgf");
        file1 = new File(temporaryFolder.getRoot(), "55merge.mgf");
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
