package uk.ac.ebi.pride.prider.loader;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import uk.ac.ebi.pride.prider.loader.util.CvParamManager;
import uk.ac.ebi.pride.prider.repo.assay.AssayRepository;
import uk.ac.ebi.pride.prider.repo.file.ProjectFileRepository;
import uk.ac.ebi.pride.prider.repo.param.CvParamRepository;
import uk.ac.ebi.pride.prider.repo.project.ProjectRepository;
import uk.ac.ebi.pride.prider.repo.user.UserRepository;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.assertNotNull;

/**
 * Created with IntelliJ IDEA.
 * User: rcote
 * Date: 04/02/13
 * Time: 14:43
 */
@ContextConfiguration(locations = {"/test-context.xml"})
@RunWith(SpringJUnit4ClassRunner.class)
public class ProjectLoaderTest {

    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private UserRepository userDao;
    @Autowired
    private ProjectRepository projectDao;
    @Autowired
    private AssayRepository assayDao;
    @Autowired
    private ProjectFileRepository projectFileDao;
    @Autowired
    private CvParamRepository cvParamDao;

    @Test
    public void LoaderTest() throws Exception {

        ProjectLoader loader = new ProjectLoader(userDao, projectDao, assayDao, projectFileDao, transactionManager);
        CvParamManager paramManager = CvParamManager.getInstance();
        paramManager.setCvParamDao(cvParamDao);
        URL url = getClass().getClassLoader().getResource("px_files/submission.px");
        assertNotNull(url);
        loader.load("123456", "12345", new File(url.toURI()).getAbsolutePath());

    }


}
