package infos;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.UnsupportedEncodingException;

/**
 *
 * 创建人  liangsong
 * 创建时间 2018/11/17 20:42
 */
public class TableField {
    public static final TableField[] EMPTY = new TableField[0];
    public final String fieldName;
    public final String type;
    public final int size;
    public  String desc;

    public TableField(Element entry) {
        NamedNodeMap attributes = entry.getAttributes();
        fieldName = attributes.getNamedItem("name").getNodeValue();
        type = attributes.getNamedItem("type").getNodeValue();
        if (type.equals("string")) {
            String size = attributes.getNamedItem("size").getNodeValue();
            if (size.length() > 0) {
                this.size = Integer.parseInt(size);
            }
            else {
                this.size = 0;
            }
        }
        else {
            size = 0;
        }
        desc = attributes.getNamedItem("desc").getNodeValue();
//        try {
//            desc = new String(desc.getBytes(),"utf-8");
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
    }
}
