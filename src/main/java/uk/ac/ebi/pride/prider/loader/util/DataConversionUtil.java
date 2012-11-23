package uk.ac.ebi.pride.prider.loader.util;

import uk.ac.ebi.pride.data.model.CvParam;
import uk.ac.ebi.pride.data.model.Param;
import uk.ac.ebi.pride.data.util.MassSpecFileType;
import uk.ac.ebi.pride.data.util.SubmissionType;
import uk.ac.ebi.pride.prider.core.file.ProjectFile;
import uk.ac.ebi.pride.prider.core.project.Reference;
import uk.ac.ebi.pride.prider.loader.exception.ProjectLoaderException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Convert various data model
 *
 * @author Rui Wang
 * @version $Id$
 */
public final class DataConversionUtil {
    /**
     * Combine a list of strings to a single string
     *
     * @param strToCombine strings to be combined
     * @param separator    string separator
     * @return
     */
    public static String combineToString(List<String> strToCombine, String separator) {
        StringBuilder builder = new StringBuilder();

        for (String s : strToCombine) {
            builder.append(s);
            builder.append(separator);
        }

        // remove the last the separator
        String str = builder.toString();
        if (strToCombine.size() > 0) {
            str = str.substring(0, str.length() - separator.length());
        }
        return str;
    }


    /**
     * Convert pubmed ids to prider reference objects
     *
     * @param pubmedIds pubmed ids
     * @return a list of prider references
     */
    public static List<Reference> convertReferences(List<String> pubmedIds) {
        List<Reference> references = new ArrayList<Reference>();

        for (String pubmedId : pubmedIds) {
            Reference reference = new Reference();
            reference.setPubmedId(Integer.parseInt(pubmedId));
            references.add(reference);
        }

        return references;
    }

    /**
     * Convert a collection params
     *
     * @param originalParams original params
     */
    public static List<uk.ac.ebi.pride.prider.core.param.Param> convertParams(List<Param> originalParams) {
        List<uk.ac.ebi.pride.prider.core.param.Param> params = new ArrayList<uk.ac.ebi.pride.prider.core.param.Param>();

        for (Param originalParam : originalParams) {
            params.add(convertParam(originalParam));
        }

        return params;
    }

    /**
     * Convert submission type from px submission core format to prider core format
     *
     * @param submissionType px submission core format submission type
     * @return submission type in prider core format
     */
    public static uk.ac.ebi.pride.prider.core.project.SubmissionType convertSubmissionType(SubmissionType submissionType) throws ProjectLoaderException {
        switch (submissionType) {
            case COMPLETE:
                return uk.ac.ebi.pride.prider.core.project.SubmissionType.COMPLETE;
            case PARTIAL:
                return uk.ac.ebi.pride.prider.core.project.SubmissionType.PARTIAL;

        }
        return null;
    }

    /**
     * Convert a collection of cv params to prider cv params
     *
     * @param originalCvParams a collection of cv params
     * @return a collection of cv params
     */
    public static List<uk.ac.ebi.pride.prider.core.param.CvParam> convertCvParams(Collection<CvParam> originalCvParams) {
        List<uk.ac.ebi.pride.prider.core.param.CvParam> cvParams = new ArrayList<uk.ac.ebi.pride.prider.core.param.CvParam>();

        for (CvParam originalCvParam : originalCvParams) {
            cvParams.add(convertCvParam(originalCvParam));
        }

        return cvParams;
    }

    /**
     * Convert a cv param to prider cv param
     *
     * @param originalCvParam original cv param
     * @return prider cv param
     */
    public static uk.ac.ebi.pride.prider.core.param.CvParam convertCvParam(CvParam originalCvParam) {
        uk.ac.ebi.pride.prider.core.param.CvParam cvParam = new uk.ac.ebi.pride.prider.core.param.CvParam();

        cvParam.setCvLabel(originalCvParam.getCvLabel());
        cvParam.setAccession(originalCvParam.getAccession());
        cvParam.setName(originalCvParam.getName());
        cvParam.setValue(originalCvParam.getValue());

        return cvParam;
    }

    /**
     * Convert a param to prider param
     *
     * @param originalParam original param
     * @return prider param
     */
    public static uk.ac.ebi.pride.prider.core.param.Param convertParam(Param originalParam) {
        if (originalParam instanceof CvParam) {
            return convertCvParam((CvParam) originalParam);
        } else {
            return new uk.ac.ebi.pride.prider.core.param.Param(originalParam.getName(), originalParam.getValue());
        }
    }

    /**
     * Convert project file type
     *
     * @param fileType mass spec file type
     * @return project file type
     */
    public static int convertProjectFileType(MassSpecFileType fileType) {
        switch (fileType) {
            case RESULT:
                return ProjectFile.RESULT_FILE_TYPE;
            case SEARCH:
                return ProjectFile.SEARCH_FILE_TYPE;
            case PEAK:
                return ProjectFile.PEAK_FILE_TYPE;
            case RAW:
                return ProjectFile.RAW_FILE_TYPE;
            case QUANT:
                return ProjectFile.QUANTIFICATION_FILE_TYPE;
            case GEL:
                return ProjectFile.GEL_FILE_TYPE;
            case OTHER:
                return ProjectFile.OTHER_FILE_TYPE;

        }
        return -1;
    }
}
