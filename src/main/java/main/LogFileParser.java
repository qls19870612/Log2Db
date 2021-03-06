package main;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import org.iq80.snappy.SnappyInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import infos.PlatInfo;
import utils.FileOperator;

/**
 * 对 每个文件进行解析
 * 创建人  liangsong
 * 创建时间 2018/11/17 18:57
 */
public class LogFileParser {
    //区服id
    private final int serverId;
    //文件
    public final File logFile;
    private static final Logger logger = LoggerFactory.getLogger(LogFileParser.class);
    public final HashMap<String, ArrayList<String>> tableSqlMap = new HashMap<>();

    public LogFileParser(int serverId, File logFile) {

        this.serverId = serverId;

        this.logFile = logFile;
    }

    /**
     * 解析文件中每一行日志
     * 按{table=[log,log,log,...]}重新组织
     * @param xmlTemplateParser
     * @param platInfo
     * @throws IOException
     */
    public void parser(XmlTemplateParser xmlTemplateParser, PlatInfo platInfo) throws IOException {

        String string;
        try{
            //snappy解压文件
            byte[] bytes = Files.toByteArray(logFile);

            //        logger.debug("parser fileName:{} bytes:{}", logFile.getName(), Arrays.copyOf(bytes, 10));

            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            SnappyInputStream snappyFramedInputStream = new SnappyInputStream(byteArrayInputStream, true);
            byte[] uncompress = ByteStreams.toByteArray(snappyFramedInputStream);
            byteArrayInputStream.close();
            snappyFramedInputStream.close();
            //        byte[] uncompress = Snappy.uncompress(bytes, 0, bytes.length);
            string = new String(uncompress);
        }
        catch (Exception e)
        {
            string = FileOperator.readFiles(logFile);
            string = string.replace("\r","");
        }
        String[] split = string.split("\n");
        for (String s : split) {
            int i = s.indexOf('|');
            String tableName = s.substring(0, i).toLowerCase();
            //            TableStruct tableStruct = xmlTemplateParser.getTableStruct(tableName);
            //            if (tableStruct != null) {
            //                StringBuilder s1 = tableStruct.formatToInsertStr(s, platInfo);
            //                ArrayList<String> arrayList = tableSqlMap.computeIfAbsent(tableName, k -> new ArrayList<>());
            //                arrayList.add(s1.toString());
            //            }


            ArrayList<String> arrayList = tableSqlMap.computeIfAbsent(tableName, k -> new ArrayList<>());
            arrayList.add(s.substring(i + 1));
        }

    }

    public ArrayList<String> getTableSqls(String tableName) {
        return tableSqlMap.get(tableName);
    }

    public void dispose() {
        tableSqlMap.clear();
    }
}
