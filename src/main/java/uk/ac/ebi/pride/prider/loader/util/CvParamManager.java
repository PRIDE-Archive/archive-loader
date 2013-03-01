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

    public CvParamManager(CvParamRepository cvParamDao) {
        if (cvParamDao == null) {
            throw new IllegalStateException("CvParam DAO not set!");
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
     * If a param isn't already loaded from the database, it is new and will be stored. If it alraedy exists,
     * nothing happens.
     *
     * @return true if the param has been stored, false otherwise.
     */
    public boolean putCvParam(String cvLabel, String accession, String name) {
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

}
