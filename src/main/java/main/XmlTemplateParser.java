package main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import infos.TableStruct;

/**
 *
 * 创建人  liangsong
 * 创建时间 2018/11/17 20:23
 */
public class XmlTemplateParser {
    public HashMap<String, TableStruct> tableStructHashMap = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(XmlTemplateParser.class);

    public XmlTemplateParser() throws ParserConfigurationException, IOException, SAXException, URISyntaxException {
        //        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        //        Enumeration<URL> resources = classLoader.getResources("");
        //        while (resources.hasMoreElements()) {
        //            URL url = resources.nextElement();
        //            logger.debug("XmlTemplateParser url.getPath():{}", url.getPath());
        //            if (url.getProtocol().equals("file"))
        //            {
        //                File file = new File(url.getPath());
        //                File[] files = file.listFiles();
        //                for (File file1 : files) {
        //                    logger.debug("XmlTemplateParser file1.getPath():{}", file1.getPath());
        //                }
        //            }
        //        }
        //
        //        URL path = this.getClass().getClassLoader().getResource("tlog.xml");
        File configFile = new File("tlog.xml");
        //        File configFile = new File(path.toURI());
        if (!configFile.exists()) {
            throw new RuntimeException("tlog.xml 不存在或是空的!");
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(configFile);
        NodeList struct = doc.getElementsByTagName("struct");
        int iLen = struct.getLength();
        for (int i = 0; i < iLen; i++) {
            Node item = struct.item(i);
            TableStruct tableStruct = new TableStruct((Element) item);
            tableStructHashMap.put(tableStruct.tableName, tableStruct);
        }


    }

    public TableStruct getTableStruct(String tabelName) {
        return tableStructHashMap.get(tabelName);
    }
}
