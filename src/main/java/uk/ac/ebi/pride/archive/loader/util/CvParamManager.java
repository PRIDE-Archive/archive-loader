package uk.ac.ebi.pride.archive.loader.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.archive.loader.exception.SubmissionLoaderException;
import uk.ac.ebi.pride.archive.repo.param.CvParam;
import uk.ac.ebi.pride.archive.repo.param.CvParamRepository;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: rcote
 * Date: 22/01/13
 * Time: 22:13
 */
public class CvParamManager {

    private static final Logger logger = LoggerFactory.getLogger(CvParamManager.class);

    private CvParamRepository cvParamDao;

    private Map<String, CvParam> allParams = new HashMap<String, CvParam>();

    public CvParamManager(CvParamRepository cvParamDao) {
        if (cvParamDao == null) {
            throw new SubmissionLoaderException("CvParam DAO not set!");
        }
        this.cvParamDao = cvParamDao;
        cacheData();
    }

    private void cacheData() {
        for (CvParam param : cvParamDao.findAll()) {
            allParams.put(param.getAccession(), param);
        }
    }

    public CvParam getCvParam(String accession) {
        return allParams.get(accession);
    }

    public void persistCvParams(Collection<CvParam> cvParams) {
        for (CvParam cvParam : cvParams) {
            CvParam persistedCvParam = getCvParam(cvParam.getAccession());
            if (persistedCvParam != null) {
                cvParam.setId(persistedCvParam.getId());
            } else {
                putCvParam(cvParam);
            }
        }
    }

    /**
     * If a param isn't already loaded from the database, it is new and will be stored. If it already exists,
     * nothing happens.
     *
     * @return true if the param has been stored, false otherwise.
     */
    public void putCvParam(CvParam cvParam) {

        String accession = cvParam.getAccession();
        String name = cvParam.getName();
        String cvLabel = cvParam.getCvLabel();

        if (cvLabel == null || "".equals(cvLabel.trim())) {
            throw new IllegalArgumentException("CV LABEL cannot be null to store cv param");
        }
        if (accession == null || "".equals(accession.trim())) {
            throw new IllegalArgumentException("ACCESSION cannot be null to store cv param");
        }
        if (name == null || "".equals(name.trim())) {
            throw new IllegalArgumentException("NAME cannot be null to store cv param");
        }

        if (!allParams.containsKey(accession)) {
            try {
                cvParamDao.save(cvParam);
                logger.warn("Storing cv param: " + accession);
                allParams.put(accession, cvParam);
            } catch (RuntimeException e) {
                logger.error("Error saving param: " + e.getMessage(), e);
                throw e;
            }
        }
    }

}
