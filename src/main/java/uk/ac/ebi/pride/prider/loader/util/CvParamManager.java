package uk.ac.ebi.pride.prider.loader.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.prider.loader.exception.ProjectLoaderException;
import uk.ac.ebi.pride.prider.repo.param.CvParam;
import uk.ac.ebi.pride.prider.repo.param.CvParamRepository;

import java.util.HashMap;
import java.util.Iterator;
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
            throw new ProjectLoaderException("CvParam DAO not set!");
        }
        this.cvParamDao = cvParamDao;
        cacheData();
    }

    private void cacheData() {
        Iterator<CvParam> iterator = cvParamDao.findAll().iterator();
        while (iterator.hasNext()) {
            CvParam param = iterator.next();
            allParams.put(param.getAccession(), param);
        }
        loadDefaultParams();
    }

    private void loadDefaultParams() {

        //these params are used in the prider-loader
        //ensure that they're already in the database
        if (getCvParam(Constant.MS_INSTRUMENT_MODEL_AC) == null) {
            putCvParam(Constant.MS, Constant.MS_INSTRUMENT_MODEL_AC, "instrument model");
        }
        if (getCvParam(Constant.MS_SOFTWARE_AC) == null) {
            putCvParam(Constant.MS, Constant.MS_SOFTWARE_AC, "software");
        }

    }

    public CvParam getCvParam(String accession) {
        return allParams.get(accession);
    }

    /**
     * If a param isn't already loaded from the database, it is new and will be stored. If it already exists,
     * nothing happens.
     *
     * @return true if the param has been stored, false otherwise.
     */
    public boolean putCvParam(String cvLabel, String accession, String name) {

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
                CvParam param = new CvParam();
                param.setAccession(accession);
                param.setCvLabel(cvLabel);
                param.setName(name);
                cvParamDao.save(param);
                logger.warn("Storing cv param: " + accession);
                allParams.put(accession, param);
                return true;
            } catch (RuntimeException e) {
                logger.error("Error saving param: " + e.getMessage(), e);
                throw e;
            }
        }
        return false;
    }

}
