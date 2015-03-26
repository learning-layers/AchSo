package fi.aalto.legroup.achso.authoring;

import android.net.Uri;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import fi.aalto.legroup.achso.app.App;

/**
 * FIXME: This class needs to be cleaned up and tested.
 * Separating functionality that is inherently coupled with certain activities or fragments into
 * static methods in a separate class causes more problems than it solves. Better to keep related
 * code together if it isn't reused.
 */
public class VideoHelper {

    public static void moveFile(Uri inputPath, String outputPath) {

        InputStream in = null;
        OutputStream out = null;
        try {

            //create output directory if it doesn't exist
            File dir = new File(outputPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }


            String inputFile = new File(inputPath.getPath()).getName();
            in = new FileInputStream(inputPath.getPath());
            out = new FileOutputStream(outputPath + inputFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;

            // write the output file
            out.flush();
            out.close();
            out = null;

            // delete the original file
            new File(inputPath + inputFile).delete();


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static UUID unpackAchsoFile(String zipname) {
        InputStream is;
        ZipInputStream zis;
        UUID uuid = null;

        try {
            String filename;
            is = new FileInputStream(App.localStorageDirectory.getPath() + "/" + zipname);
            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry ze;
            byte[] buffer = new byte[1024];
            int count;


            while ((ze = zis.getNextEntry()) != null) {
                // zapis do souboru
                filename = ze.getName();
                if (filename.contains(".json")) {
                    uuid = UUID.fromString(filename.replace(".json", ""));
                }

                // Need to create directories if not exists, or
                // it will generate an Exception...
                if (ze.isDirectory()) {
                    File fmd = new File(App.localStorageDirectory.getPath() + "/" + filename);
                    fmd.mkdirs();
                    continue;
                }

                FileOutputStream fout = new FileOutputStream(App.localStorageDirectory.getPath() + "/" + filename);

                // cteni zipu a zapis
                while ((count = zis.read(buffer)) != -1) {
                    fout.write(buffer, 0, count);
                }

                fout.close();
                zis.closeEntry();
            }

            zis.close();

            new File(App.localStorageDirectory + "/" + zipname).delete();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return uuid;
    }

}
