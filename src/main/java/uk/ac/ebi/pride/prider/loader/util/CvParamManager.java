package uk.ac.ebi.pride.prider.loader.util;

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

    private CvParamRepository cvParamDao;

    private Map<String, CvParam> allParams = new HashMap<String, CvParam>();

    private static CvParamManager instance = new CvParamManager();

    public static CvParamManager getInstance() {
        return instance;
    }

    private CvParamManager() {
    }

    public void setCvParamDao(CvParamRepository cvParamDao) {
        this.cvParamDao = cvParamDao;
        cacheData();
    }

    private void cacheData() {
        checkDao();
        Iterator<CvParam> iterator = cvParamDao.findAll().iterator();
        while (iterator.hasNext()) {
            CvParam param = iterator.next();
            allParams.put(param.getAccession(), param);
        }
    }

    public CvParam getCvParam(String accession) {
        checkDao();
        return allParams.get(accession);
    }

    /**
     * If a param isn't already loaded from the database, it is new and will be stored. If it alraedy exists,
     * nothing happens.
     *
     * @return true if the param has been stored, false otherwise.
     */
    public boolean putCvParam(String cvLabel, String accession, String name) {
        checkDao();
        if (!allParams.containsKey(accession)) {
            CvParam param = new CvParam();
            param.setAccession(accession);
            param.setCvLabel(cvLabel);
            param.setName(name);
            cvParamDao.save(param);
            allParams.put(accession, param);
            return true;
        }
        return false;
    }

    private void checkDao() {
        if (cvParamDao == null) {
            throw new IllegalStateException("CvParam DAO not set!");
        }
    }

}
