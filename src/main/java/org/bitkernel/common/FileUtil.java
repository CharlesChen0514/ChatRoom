package org.bitkernel.common;

import com.sun.istack.internal.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

@Slf4j
public class FileUtil {
    @NotNull
    public static Set<String> getAllFileNameSet(@NotNull String fileDir) {
        File f = new File(fileDir);
        Set<String> set = new LinkedHashSet<>();
        if (!f.exists()) {
            logger.error(fileDir + " not exists");
            return set;
        }
        File[] fa = f.listFiles();
        assert fa != null;
        for (File fs : fa) {
            set.add(fs.getName());
        }
        return set;
    }

    @NotNull
    public static String getAllFileNameString(@NotNull String fileDir) {
        Set<String> allFileNameSet = getAllFileNameSet(fileDir);
        StringBuilder sb = new StringBuilder();
        allFileNameSet.forEach(name -> sb.append(name).append(", "));
        return sb.toString();
    }

    public static boolean createFolder(@NotNull String dir) {
        File file = new File(dir);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                logger.error("create folder error: {}", dir);
                return false;
            }
            logger.debug("Create folder success: {}", dir);
        } else {
            logger.debug("Folder already exist: {}", dir);
        }
        return true;
    }

    public static boolean existInFolder(@NotNull String dir,
                                        @NotNull String name) {
        Set<String> allFileNameSet = getAllFileNameSet(dir);
        return allFileNameSet.contains(name);
    }

    public static boolean exist(@NotNull String filePath) {
        File file = new File(filePath);
        return file.exists();
    }

    public static void main(String[] args) {
        String file = System.getProperty("user.dir") + File.separator
                + "file";
        String file2 = System.getProperty("user.dir") + File.separator;
        String file3 = System.getProperty("user.dir") + File.separator
                + "file" + File.separator + "chen";
//        Set<String> allFileNames = getAllFileNameSet(file2);
//        allFileNames.forEach(System.out::println);
//        boolean folder = createFolder(file3);
        StopWatch watch = new StopWatch();
        watch.start();
        watch.stop();
        System.out.println(watch.shortSummary());
    }
}
