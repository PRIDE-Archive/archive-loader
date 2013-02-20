package uk.ac.ebi.pride.prider.loader;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import uk.ac.ebi.pride.prider.repo.assay.AssayRepository;
import uk.ac.ebi.pride.prider.repo.file.ProjectFileRepository;
import uk.ac.ebi.pride.prider.repo.param.CvParamRepository;
import uk.ac.ebi.pride.prider.repo.project.ProjectRepository;
import uk.ac.ebi.pride.prider.repo.user.UserRepository;

/**
 * Created with IntelliJ IDEA.
 * User: rcote
 * Date: 20/02/13
 * Time: 16:22
 */
@ContextConfiguration(locations = {"/test-context.xml"})
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class AbstractLoaderTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Autowired
    protected PlatformTransactionManager transactionManager;
    @Autowired
    protected UserRepository userDao;
    @Autowired
    protected ProjectRepository projectDao;
    @Autowired
    protected AssayRepository assayDao;
    @Autowired
    protected ProjectFileRepository projectFileDao;
    @Autowired
    protected CvParamRepository cvParamDao;

    protected FileSystemResource submissionFile;

}
