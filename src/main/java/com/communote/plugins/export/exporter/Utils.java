package com.communote.plugins.export.exporter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.communote.server.model.user.User;
import com.communote.server.model.user.UserStatus;

/**
 *
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 */
public class Utils {

    /**
     * Add the content of a directory recursively to the zip.
     *
     * @param zipOutputStream
     *            the stream to add the files to
     * @param dirToZip
     *            the directory to zip
     * @param basePath
     *            the base path for determining the name of the zip entry
     * @param fileToExclude
     *            an optional file to not add to the zip, for instance if the zip file is created in
     *            the directory to zip
     * @throws IOException
     *             in case reading the files to write or writing to the zip failed
     */
    private static void addDirectoryToZip(ZipOutputStream zipOutputStream, File dirToZip,
            String basePath, File fileToExclude) throws IOException {
        for (File file : dirToZip.listFiles()) {
            if (file.isDirectory()) {
                addDirectoryToZip(zipOutputStream, file, basePath, fileToExclude);
            } else if (!file.equals(fileToExclude)) {
                addFileToZip(zipOutputStream, file, basePath);
            }
        }
    }

    /**
     * Add a file to the zip. The name of the zip entry will be relative to the given basePath.
     *
     * @param zipOutputStream
     *            stream to add the file to
     * @param file
     *            the file to add. It is assumed that it is a file and not a directory.
     * @param basePath
     *            the base path for determining the name of the zip entry
     * @throws IOException
     *             in case reading the files to write or writing to the zip failed
     */
    private static void addFileToZip(ZipOutputStream zipOutputStream, File file, String basePath)
            throws IOException {
        // get relative file name starting after the base path, have to add 1 to length for
        // path-separator character
        String relativeFilename = file.getCanonicalPath().substring(basePath.length() + 1,
                file.getCanonicalPath().length());
        ZipEntry zipEntry = new ZipEntry(relativeFilename);
        zipOutputStream.putNextEntry(zipEntry);
        try (FileInputStream fileInputStream = new FileInputStream(file);) {
            byte[] inputBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(inputBuffer)) != -1) {
                zipOutputStream.write(inputBuffer, 0, bytesRead);
            }
        }
    }

    /**
     * Test whether a user is active, temporarily or permanently disabled.
     *
     * @param user
     *            the user to test
     * @return true if the is active, temporarily or permanently disabled.
     */
    public static boolean isUserActiveOrDisabled(User user) {
        if (user != null) {
            return UserStatus.ACTIVE.equals(user.getStatus())
                    || UserStatus.TEMPORARILY_DISABLED.equals(user.getStatus())
                    || UserStatus.PERMANENTLY_DISABLED.equals(user.getStatus());
        }
        return false;
    }

    /**
     * Zip the content of a directory. Sub-directories will be added recursively to the zip
     *
     * @param dirToZip
     *            the directory to zip
     * @param outputDir
     *            the directory to write the zip file to
     * @param fileName
     *            the name of the zip file
     * @return the created zip file
     * @throws IOException
     *             in case reading the files to write or writing to the zip failed
     * @throws FileNotFoundException
     *             in case the outputDir or the dirToZip do not exist
     */
    public static File zipDirectory(File dirToZip, File outputDir, String fileName)
            throws FileNotFoundException, IOException {
        if (!dirToZip.exists()) {
            throw new FileNotFoundException("Directory to zip " + dirToZip.getCanonicalPath()
                    + " does not exist");
        }
        if (!outputDir.exists()) {
            throw new FileNotFoundException("Output directory " + outputDir.getCanonicalPath()
                    + " does not exist");
        }
        File zipFile = new File(outputDir, fileName);
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile));) {
            addDirectoryToZip(zipOutputStream, dirToZip, dirToZip.getCanonicalPath(), zipFile);
        }
        return zipFile;
    }

    private Utils() {
        // helper
    }
}
