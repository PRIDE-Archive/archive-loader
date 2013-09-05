package uk.ac.ebi.pride.prider.loader;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;
import uk.ac.ebi.pride.data.io.SubmissionFileParser;
import uk.ac.ebi.pride.data.model.Submission;
import uk.ac.ebi.pride.prider.dataprovider.person.Title;
import uk.ac.ebi.pride.prider.dataprovider.project.SubmissionType;
import uk.ac.ebi.pride.prider.loader.file.Pride3FileFinder;
import uk.ac.ebi.pride.prider.repo.assay.Assay;
import uk.ac.ebi.pride.prider.repo.assay.Contact;
import uk.ac.ebi.pride.prider.repo.file.ProjectFile;
import uk.ac.ebi.pride.prider.repo.project.*;
import uk.ac.ebi.pride.prider.repo.user.User;

import java.io.File;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created with IntelliJ IDEA.
 * User: rcote
 * Date: 04/02/13
 * Time: 14:43
 */
public class ProjectLoaderCompleteMzIdentMLSubmissionTest extends AbstractLoaderTest {

    @Test
    public void submissionPersistenceIsComplete() throws Exception {
        Submission submission = SubmissionFileParser.parse(submissionFile.getFile());

        SubmissionLoader loader = new SubmissionLoader(projectDao, assayDao, projectFileDao, cvParamDao, transactionManager);

        String filePath = submissionFile.getFile().getAbsolutePath().replace(submissionFile.getFilename(), "");
        File rootPath = new File(filePath);
        SubmissionMaker maker = new SubmissionMaker(new Pride3FileFinder(rootPath));
        List<Assay> assaysToPersist = maker.makeAssays(submission);

        User submitterToPersist = userDao.findByEmail("john.smith@dummy.ebi.com");
        Project project = maker.makeProject("88888", "88888", submitterToPersist, submission, assaysToPersist);

        Map<ProjectFile,String> projectFiles = maker.makeFiles(submission);

        loader.persistSubmission(project, assaysToPersist, projectFiles);


        Project loadedProject = checkProject();

        checkAssays(loadedProject);

        checkFiles(loadedProject);

    }

    private Project checkProject() {
        Project loadedProject = projectDao.findByAccession("88888");

        // submitter
        User submitter = loadedProject.getSubmitter();
        assertEquals(Title.Mr, submitter.getTitle());
        assertEquals("john.smith@dummy.ebi.com", submitter.getEmail());
        assertEquals("john", submitter.getFirstName());
        assertEquals("smith", submitter.getLastName());
        assertEquals("EBI", submitter.getAffiliation());

        // doi
        assertEquals("88888", loadedProject.getDoi());

        // lab head
        LabHead labHead = loadedProject.getLabHeads().iterator().next();
        assertEquals(Title.Dr, labHead.getTitle());
        assertEquals("alice.wonderland@cam.edu", labHead.getEmail());
        assertEquals("Alice", labHead.getFirstName());
        assertEquals("Wonderland", labHead.getLastName());
        assertEquals("University of Newcastle", labHead.getAffiliation());

        // project title
        assertEquals("mzidentml test", loadedProject.getTitle());

        // project description
        assertEquals("mzidentml test test description", loadedProject.getProjectDescription());

        // data processing protocol
        assertEquals("mzidentml test test data protocol", loadedProject.getDataProcessingProtocol());

        // sample processing protocol
        assertEquals("mzidentml test test sample protocol", loadedProject.getSampleProcessingProtocol());

        // project tags
        Collection<ProjectTag> projectTags = loadedProject.getProjectTags();
        assertEquals("Human proteome project", projectTags.iterator().next().getTag());

        // keywords
        assertEquals("mzidentml test", loadedProject.getKeywords());

        // number of assays
        assertEquals(1, loadedProject.getNumAssays());

        // submission type
        assertEquals(SubmissionType.COMPLETE, loadedProject.getSubmissionType());

        // project status
        assertEquals(false, loadedProject.isPublicProject());

        // other omics link
        assertEquals("http://www.ebi.ac.uk", loadedProject.getOtherOmicsLink());

        // experiment type
        Collection<ProjectExperimentType> experimentTypes = loadedProject.getExperimentTypes();
        assertEquals("SRM/MRM", experimentTypes.iterator().next().getName());

        // species, tissue, cell type, diesease
        Collection<ProjectSampleCvParam> samples = loadedProject.getSamples();
        List<String> accessions = Arrays.asList("9913", "BTO:0000142", "CL:0000081", "DOID:1319");
        assertEquals(4, samples.size());
        Iterator<ProjectSampleCvParam> sampleIterator = samples.iterator();
        assertTrue(accessions.contains(sampleIterator.next().getAccession()));
        assertTrue(accessions.contains(sampleIterator.next().getAccession()));
        assertTrue(accessions.contains(sampleIterator.next().getAccession()));
        assertTrue(accessions.contains(sampleIterator.next().getAccession()));

        // instrument
        assertEquals(1, loadedProject.getInstruments().size());
        assertEquals("LTQ", loadedProject.getInstruments().iterator().next().getName());

        // modifications
        assertEquals(1, loadedProject.getPtms().size());

        // reference
        Collection<Reference> references = loadedProject.getReferences();
        List<Integer> pubmedIds = Arrays.asList(23203882, 22121220);
        assertEquals(2, references.size());
        Iterator<Reference> referenceIterator = references.iterator();
        assertTrue(pubmedIds.contains(referenceIterator.next().getPubmedId()));
        assertTrue(pubmedIds.contains(referenceIterator.next().getPubmedId()));

        // reanalysis
        assertEquals("PXD000005,PXD000004", loadedProject.getReanalysis());

        // additional
        assertEquals(1, loadedProject.getProjectGroupUserParams().size());
        assertEquals("additional param value", loadedProject.getProjectGroupUserParams().iterator().next().getValue());
        assertEquals(0, loadedProject.getProjectGroupCvParams().size());

        // quantification
        assertEquals(1, loadedProject.getQuantificationMethods().size());
        assertEquals("iTRAQ", loadedProject.getQuantificationMethods().iterator().next().getName());

        // software
        assertEquals(2, loadedProject.getSoftware().size());
        assertEquals("iTRAQ", loadedProject.getQuantificationMethods().iterator().next().getName());
        return loadedProject;
    }

    private void checkAssays(Project loadedProject) {
        List<Assay> assays = assayDao.findAllByProjectId(loadedProject.getId());
        Assay assay = assays.iterator().next();

        // number of assays
        assertEquals(1, assays.size());

        // short label
        assertEquals(null, assay.getShortLabel());

        // title
        assertEquals("Unknown experiment (mzIdentML)", assay.getTitle());

        // assay accession
        assertEquals("1234", assay.getAccession());

        // number of proteins
        assertEquals(7, assay.getProteinCount());

        // number of peptides
        assertEquals(11, assay.getPeptideCount());

        // number of unique peptides
        assertEquals(7, assay.getUniquePeptideCount());

//        todo - this fails
        // total spectrum count
        assertEquals(39, assay.getTotalSpectrumCount());

        // identified spectrum count
        assertEquals(11, assay.getIdentifiedSpectrumCount());

        // instrument size
        assertEquals(0, assay.getInstruments().size());

        // software size
        assertEquals(2, assay.getSoftwares().size());

        // ptms
        assertEquals(1, assay.getPtms().size());
        assertEquals("UNIMOD:35", assay.getPtms().iterator().next().getAccession());

        // contact
        Contact contact = assay.getContacts().iterator().next();
        assertEquals(1, assay.getContacts().size());
        assertEquals("John", contact.getFirstName());
        assertEquals("Doe", contact.getLastName());
        assertEquals("Matrix Science Limited", contact.getAffiliation());
        assertEquals("john.doe@nowhere.com", contact.getEmail());
    }

    private void checkFiles(Project loadedProject) {
        List<ProjectFile> projectFiles = projectFileDao.findAllByProjectId(loadedProject.getId());

        assertEquals(3, projectFiles.size());

        for (ProjectFile projectFile : projectFiles) {
            assertTrue(projectFile.getAssayId() != null);
        }
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

        url = ProjectLoaderCompleteMzIdentMLSubmissionTest.class.getClassLoader().getResource("px_files_mzidentml/55merge_raw.zip");
        file1 = new File(temporaryFolder.getRoot(), "55merge_raw.zip");
        FileUtils.copyFile(new File(url.toURI()), file1);
    }
}
