package utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.DirectoryStream;
import java.util.ArrayList;

/**
 * Created by liangsong on 2018/4/14
 */
public class FileOperator {
    public static final String NEX_LINE = System.getProperty("line.separator");

    public static ArrayList<File> getAllFiles(File root, String extName) {
        ArrayList<File> ret = new ArrayList<File>();
        readToList(root, ret, extName);

        return ret;
    }
    public static ArrayList<File> getAllFiles(File root,DirectoryStream.Filter<File> filter) {
        ArrayList<File> ret = new ArrayList<File>();
        try {
            readToList(root, ret, filter);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ret;
    }
    public static String readFiles(File file) {
        String code = null;
        try {
            code = get_charset(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String ret = null;
        BufferedReader reader = null;
        StringBuffer stringBuffer = null;
        FileInputStream fileInput = null;
        InputStreamReader input = null;
        try {
            fileInput = new FileInputStream(file.getPath());
            input = new InputStreamReader(fileInput, code);
            reader = new BufferedReader(input);
            String tempStr = null;
            stringBuffer = new StringBuffer();
            while ((tempStr = reader.readLine()) != null) {
                stringBuffer.append(tempStr).append(NEX_LINE);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileInput != null) {
                    fileInput.close();
                }
                if (input != null) {
                    input.close();
                }
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (stringBuffer != null) {
            ret = stringBuffer.toString();
            int lastIndex = ret.lastIndexOf(NEX_LINE);
            if (lastIndex != -1) {
                ret = ret.substring(0, lastIndex);
            }
        }


        return ret;
    }

    public static Boolean writeFile(File file, String contenxt) {
        Boolean ret = false;
        BufferedWriter bufferedWriter = null;
        FileOutputStream writerStream = null;
        OutputStreamWriter outputStream = null;
        try {
            writerStream = new FileOutputStream(file);
            outputStream = new OutputStreamWriter(writerStream, "utf-8");
            bufferedWriter = new BufferedWriter(outputStream);
            bufferedWriter.write(contenxt);
            bufferedWriter.flush();
            ret = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writerStream != null) {
                    writerStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
                if (bufferedWriter != null) {
                    bufferedWriter.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return ret;
    }

    private static void readToList(File root, ArrayList<File> ret, String extName) {
        if (root.isDirectory()) {
            File[] subFiles = root.listFiles();
            if (subFiles != null) {
                for (File file : subFiles) {
                    readToList(file, ret, extName);
                }
            }
        } else {
            if (root.getName().endsWith(extName)) {
                ret.add(root);
            }
        }
    }
    private static void readToList(File root, ArrayList<File> ret,DirectoryStream.Filter<File> filter) throws IOException {

        if (root.isDirectory()) {
            File[] subFiles = root.listFiles();
            if (subFiles != null) {
                for (File file : subFiles) {
                    readToList(file, ret, filter);
                }
            }
        } else {
            if (filter.accept(root)) {
                ret.add(root);
            }
        }
    }
    public static String getExtensionName(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot > -1) && (dot < (filename.length() - 1))) {
                return filename.substring(dot + 1);
            }
        }
        return filename;
    }

    public static String getFileNameNoEx(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot > -1) && (dot < (filename.length()))) {
                return filename.substring(0, dot);
            }
        }
        return filename;
    }

    public static String get_charset(File file) {
        String charset = "GBK";
        byte[] first3Bytes = new byte[3];//首先3个字节
        try {
            boolean checked = false;
            ;
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            bis.mark(0);
            int read = bis.read(first3Bytes, 0, 3);
            if (read == -1) {
                return charset;
            }
            if (first3Bytes[0] == (byte) 0xFF && first3Bytes[1] == (byte) 0xFE) {
                charset = "UTF-16LE";
                checked = true;
            } else if (first3Bytes[0] == (byte) 0xFE && first3Bytes[1] == (byte) 0xFF) {
                charset = "UTF-16BE";
                checked = true;
            } else if (first3Bytes[0] == (byte) 0xEF && first3Bytes[1] == (byte) 0xBB && first3Bytes[2] == (byte) 0xBF) {
                charset = "UTF-8";
                checked = true;
            }
            bis.reset();
            if (!checked) {
                // int len = 0;
                int loc = 0;

                while ((read = bis.read()) != -1) {
                    loc++;
                    if (read >= 0xF0) {
                        break;
                    }
                    if (0x80 <= read && read <= 0xBF) // 单独出现BF以下的，也算是GBK
                    {
                        break;
                    }
                    if (0xC0 <= read && read <= 0xDF) {
                        read = bis.read();
                        if (0x80 <= read && read <= 0xBF) // 双字节 (0xC0 - 0xDF)
                        // (0x80
                        // - 0xBF),也可能在GB编码内
                        {
                            continue;
                        } else {
                            break;
                        }
                    } else if (0xE0 <= read && read <= 0xEF) {// 也有可能出错，但是几率较小
                        read = bis.read();
                        if (0x80 <= read && read <= 0xBF) {
                            read = bis.read();
                            if (0x80 <= read && read <= 0xBF) {
                                charset = "UTF-8";
                                break;
                            } else {
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                }

            }

            bis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return charset;
    }


}
