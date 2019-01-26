package infos;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import java.util.Objects;

/**
 *
 * 创建人  liangsong
 * 创建时间 2018/11/17 20:42
 */
public class TableField {
    public static final TableField[] EMPTY = new TableField[0];
    public final String fieldName;
    public String type;
    public int size;
    public final String desc;

    public TableField(Element entry) {
        NamedNodeMap attributes = entry.getAttributes();
        fieldName = attributes.getNamedItem("name").getNodeValue();
        type = attributes.getNamedItem("type").getNodeValue();
        if (type.equals("string")) {
            String size = attributes.getNamedItem("size").getNodeValue();
            if (size.length() > 0) {
                this.size = Integer.parseInt(size);
            } else {
                this.size = 0;
            }
        } else {
            size = 0;
        }
        convertTypeAndSize();
        type = type.toUpperCase();
        desc = attributes.getNamedItem("desc").getNodeValue();
    }

    private void convertTypeAndSize() {
        switch (type.toLowerCase()) {
            case "string":
                if (size <= 0) {
                    size = 20;
                }
                type = "varchar";
                break;
            case "uint":
                type = "int";
                size = 10;
                break;
            case "bigint":

                size = 19;
                break;
            case "datetime":
                size = 19;
                break;
            case "utinyint":
                if (size == 0) {
                    size = 3;
                }
                type = "tinyint";
                break;
            case "varchar":

                break;
            default:
                throw new RuntimeException("xml中配置了未知数据类型：" + type);

        }
    }

    public TableField(String fieldName, String type, int size, String desc) {

        this.fieldName = fieldName;
        this.type = type;
        this.size = size;
        this.desc = desc;
    }

    public TableField(boolean convertType, String fieldName, String type, int size, String desc) {

        this.fieldName = fieldName;
        this.type = type;
        this.size = size;
        this.desc = desc;
        convertTypeAndSize();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TableField that = (TableField) o;
        boolean b = Objects.equals(fieldName, that.fieldName) && type.equalsIgnoreCase(that.type) && Objects.equals(desc, that.desc);
        if (!b) {
            return false;
        }
        if (type.equalsIgnoreCase("DATETIME")) {
            return true;
        }
        return size == that.size;
    }

    @Override
    public int hashCode() {

        return Objects.hash(fieldName, type, size, desc);
    }

    @Override
    public String toString() {
        return "TableField{" + "fieldName='" + fieldName + '\'' + ", type='" + type + '\'' + ", size=" + size + ", desc='" + desc + '\'' + '}';
    }

    public boolean isDate() {
        return type.equalsIgnoreCase("datetime");
    }
}
