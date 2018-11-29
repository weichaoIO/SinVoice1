package io.weichao.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUtil {
    public static void writeLog(String path, String str) {
        try {
            final File file = new File(path);
            if (!file.exists()) {
                file.createNewFile();
            }
            final FileOutputStream out = new FileOutputStream(file);
            out.write(str.getBytes("utf-8"));
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}