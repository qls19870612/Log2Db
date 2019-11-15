package main;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import org.iq80.snappy.SnappyInputStream;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import utils.FileOperator;

/**
 *
 * 创建人  liangsong
 * 创建时间 2019/07/17 14:15
 */
public class ParseLog {

    public static void main(String[] args) throws IOException {
        args = new String[]{"D:\\workspace\\svr_log\\1"};
        File dir = new File(args[0]);
        File newDir = new File(dir.getPath() + "/uncompress");
        if(!newDir.exists())
        {
            newDir.mkdir();
        }
        for (File logFile : dir.listFiles()) {
            if (logFile.getName().endsWith(".log")) {
                uncompress(newDir, logFile);
            }
        }

    }

    private static void uncompress(File newDir, File logFile) throws IOException {
        byte[] bytes = Files.toByteArray(logFile);

        //        logger.debug("parser fileName:{} bytes:{}", logFile.getName(), Arrays.copyOf(bytes, 10));

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        SnappyInputStream snappyFramedInputStream = new SnappyInputStream(byteArrayInputStream, true);
        byte[] uncompress = ByteStreams.toByteArray(snappyFramedInputStream);
        byteArrayInputStream.close();
        snappyFramedInputStream.close();
        //        byte[] uncompress = Snappy.uncompress(bytes, 0, bytes.length);
        String string = new String(uncompress);
        File file = new File(newDir.getPath() + "/" + logFile.getName());

        FileOperator.writeFile(file,string);
    }
}
