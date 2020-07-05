package uk.ac.ebi.pride.archive.loader.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.archive.repo.client.CvParamRepoClient;
import uk.ac.ebi.pride.archive.repo.models.param.CvParam;

import java.io.IOException;
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

    private final CvParamRepoClient cvParamRepoClient;

    private Map<String, CvParam> allParams = new HashMap<String, CvParam>();

    public CvParamManager(CvParamRepoClient cvParamRepoClient) {
        this.cvParamRepoClient = cvParamRepoClient;
        try {
            cacheData();
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private void cacheData() throws IOException {
        for (CvParam param : cvParamRepoClient.findAll()) {
            allParams.put(param.getAccession(), param);
        }
    }

    public CvParam getCvParam(String accession) {
        return allParams.get(accession);
    }

    public void persistCvParams(Collection<CvParam> cvParams) throws JsonProcessingException {
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
    public void putCvParam(CvParam cvParam) throws JsonProcessingException {

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
                cvParamRepoClient.save(cvParam);
                logger.warn("Storing cv param: " + accession);
                allParams.put(accession, cvParam);
            } catch (RuntimeException e) {
                logger.error("Error saving param: " + e.getMessage(), e);
                throw e;
            }
        }
    }

}
