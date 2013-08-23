package uk.ac.ebi.pride.prider.loader.file;

import uk.ac.ebi.pride.data.util.FileUtil;

import java.io.File;
import java.io.IOException;

/**
 * File finder for PRIDE 3 file system
 *
 * @author Rui Wang
 * @version $Id$
 */
public class Pride3FileFinder implements FileFinder {
    private final File rootPath;

    public Pride3FileFinder(File rootPath) {
        if (!rootPath.isDirectory()) {
            throw new IllegalArgumentException("Root path must be a valid path: " + rootPath.getAbsolutePath());
        }

        this.rootPath = rootPath;
    }

    @Override
    public File find(File file) throws IOException {
        if (FileUtil.isGzipped(file) || FileUtil.isZipped(file)) {
            String decompressedFileName = FileUtil.getDecompressedFileName(file);
            return findFileByName(decompressedFileName);
        } else {
            return findFileByName(file.getName());
        }
    }

    private File findFileByName(String fileName) {
        File[] files = rootPath.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    File[] nestedFiles = file.listFiles();
                    if (nestedFiles != null) {
                        for (File nestedFile : nestedFiles) {
                            if (nestedFile.getName().equals(fileName)) {
                                return nestedFile;
                            }
                        }
                    }
                } else {
                    if (file.getName().equals(fileName)) {
                        return file;
                    }
                }
            }
        }

        return null;
    }
}
