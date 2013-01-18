package uk.ac.ebi.pride.prider.loader.util;

import uk.ac.ebi.pride.data.core.InstrumentConfiguration;
import uk.ac.ebi.pride.data.core.Software;
import uk.ac.ebi.pride.data.model.CvParam;
import uk.ac.ebi.pride.data.model.Param;
import uk.ac.ebi.pride.data.util.MassSpecFileType;
import uk.ac.ebi.pride.data.util.SubmissionType;
import uk.ac.ebi.pride.prider.repo.file.ProjectFile;
import uk.ac.ebi.pride.prider.repo.instrument.Instrument;
import uk.ac.ebi.pride.prider.repo.project.Reference;

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
     * Convert a collection of cv params to prider cv params
     *
     * @param originalCvParams a collection of cv params
     * @return a collection of cv params
     */
    public static List<uk.ac.ebi.pride.prider.repo.param.CvParam> convertCvParams(Collection<CvParam> originalCvParams) {
        List<uk.ac.ebi.pride.prider.repo.param.CvParam> cvParams = new ArrayList<uk.ac.ebi.pride.prider.core.param.CvParam>();

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
    public static uk.ac.ebi.pride.prider.repo.param.CvParam convertCvParam(uk.ac.ebi.pride.data.core.CvParam originalCvParam) {
        uk.ac.ebi.pride.prider.repo.param.CvParam cvParam = new uk.ac.ebi.pride.prider.repo.param.CvParam();

        cvParam.setCvLabel(originalCvParam.getCvLookupID());
        cvParam.setAccession(originalCvParam.getAccession());
        cvParam.setName(originalCvParam.getName());
        cvParam.setValue(originalCvParam.getValue());

        return cvParam;
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


    /**
     * Convert instrument
     *
     * @param instrumentConfiguration instrument configuration
     */
    public static Instrument convertInstrument(InstrumentConfiguration instrumentConfiguration) {
        Instrument instrument = new Instrument();

        // todo: implement

        return instrument;
    }

    public static uk.ac.ebi.pride.prider.repo.assay.software.Software convertSoftware(Software originalSoftware) {
        uk.ac.ebi.pride.prider.repo.assay.software.Software software = new uk.ac.ebi.pride.prider.core.assay.Software();

        software.setName(originalSoftware.getName());
        software.setCustomization(originalSoftware.getCustomization());
        software.setVersion(originalSoftware.getVersion());
        List<Param> params = new ArrayList<Param>();
        params.addAll(convertCvParams(originalSoftware.getCvParams()));
        software.setParams(convertParams(originalSoftware.getCvParams()));

        return software;
    }

    public static uk.ac.ebi.pride.prider.dataprovider.project.SubmissionType convertSubmissionType(SubmissionType submissionType) {

        switch (submissionType) {

            case COMPLETE:
                return uk.ac.ebi.pride.prider.dataprovider.project.SubmissionType.COMPLETE;
                break;
            case PARTIAL:
                return uk.ac.ebi.pride.prider.dataprovider.project.SubmissionType.PARTIAL;
                break;
            case PRIDE:
            case RAW:
                throw new IllegalArgumentException("Unsupported submittion type for the PRIDE-R loader")
                break;


        }


    }
}
