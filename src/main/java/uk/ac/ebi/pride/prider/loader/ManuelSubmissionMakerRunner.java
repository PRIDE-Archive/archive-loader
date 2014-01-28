package uk.ac.ebi.pride.prider.loader;

import uk.ac.ebi.pride.data.exception.SubmissionFileException;
import uk.ac.ebi.pride.data.io.SubmissionFileParser;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.model.Submission;
import uk.ac.ebi.pride.prider.dataprovider.file.ProjectFileType;
import uk.ac.ebi.pride.prider.loader.assay.AssayFileScanner;
import uk.ac.ebi.pride.prider.loader.assay.AssayFileSummary;
import uk.ac.ebi.pride.prider.loader.file.Pride3FileFinder;
import uk.ac.ebi.pride.prider.repo.assay.Assay;

import java.io.File;
import java.io.IOException;

/**
 * @author Rui Wang
 * @version $Id$
 */
public class ManuelSubmissionMakerRunner {

    public static void main(String[] args) throws SubmissionFileException, IOException {
        File submissionDirectory = new File(args[0]);

        long start = System.currentTimeMillis();

        // scan for assay statistics
        Pride3FileFinder fileFinder = new Pride3FileFinder(submissionDirectory.getAbsoluteFile());
        SubmissionMaker submissionMaker = new SubmissionMaker(fileFinder);

        // read submission
        Submission submission = SubmissionFileParser.parse(new File(args[0] + "/submission.px"));

        for (DataFile dataFile : submission.getDataFiles()) {
            if (dataFile.getFileType().equals(ProjectFileType.RESULT)) {
                AssayFileScanner assayFileScanner = new AssayFileScanner(fileFinder);
                AssayFileSummary assayFileSummary = assayFileScanner.scan(dataFile);
                Assay assay = submissionMaker.makeAssay(assayFileSummary);
                System.out.println("Protein count: " + assay.getProteinCount());
                System.out.println("Peptide count: " + assay.getPeptideCount());
            }
        }

        long stop = System.currentTimeMillis();
        System.out.println(stop - start);
    }
}
