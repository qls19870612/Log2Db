package infos;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;

/**
 *
 * 创建人  liangsong
 * 创建时间 2018/11/17 20:38
 */
public class TableStruct {
    private static final Logger logger = LoggerFactory.getLogger(TableStruct.class);
    public final String prepareSql;
    public String tableName;
    public final TableField[] fields;
    private final char[] fieldStrBytes;

    public TableStruct(Element item) {
        NodeList childNodes = item.getChildNodes();
        int iLen = childNodes.getLength();
        ArrayList<TableField> arrayList = new ArrayList<>();
        StringBuilder fieldStr = new StringBuilder("` (");
        for (int i = 0; i < iLen; i++) {
            Node entry = childNodes.item(i);
            if (entry.getNodeName().equals("entry")) {
                TableField tableField = new TableField((Element) entry);
                arrayList.add(tableField);
                fieldStr.append(tableField.fieldName);
                fieldStr.append(",");
            }
        }
        fieldStr.setLength(fieldStr.length() - 1);
        fieldStr.append(") values(");
        fieldStrBytes = fieldStr.toString().toCharArray();
        fields = arrayList.toArray(TableField.EMPTY);

        tableName = item.getAttributes().getNamedItem("name").getNodeValue().toLowerCase();
        this.prepareSql = getPrepare().toString();

    }

    public StringBuilder formatToInsertStr(String log, PlatInfo platInfo) {
        log = StringUtils.trim(log);
        String[] split = log.split("\\|");
        StringBuilder builder = new StringBuilder(32);

        builder.append("insert into `");

        builder.append(tableName);
        builder.append(fieldStrBytes);

        int count = 0;
        for (TableField field : fields) {
            count++;

            if (count < split.length) {
                builder.append("'");
                builder.append(split[count]);
                builder.append("',");
            } else {
                builder.append("'");
                builder.append("',");

            }

        }
        builder.setLength(builder.length() - 1);
        //        builder.append(");\n");
        builder.append(")");
        return builder;
    }

    private StringBuilder getPrepare() {

        StringBuilder builder = new StringBuilder(32);

        builder.append("insert into `");

        builder.append(tableName);
        builder.append(fieldStrBytes);

        for (TableField ignored : fields) {
            builder.append("?,");
        }
        builder.setLength(builder.length() - 1);
        builder.append(")");
        return builder;
    }

    public String getCreateTableSql() {
        StringBuilder stringBuilder = new StringBuilder("CREATE TABLE IF NOT EXISTS `");
        stringBuilder.append(tableName);
        stringBuilder.append("` (");
        for (TableField field : fields) {
            stringBuilder.append("\n`");
            stringBuilder.append(field.fieldName);
            stringBuilder.append("` ");
            int size = field.size;
            switch (field.type) {
                case "string":
                    if (size <= 0) {
                        size = 20;
                    }
                    stringBuilder.append("varchar(");
                    stringBuilder.append(size);
                    stringBuilder.append(") ");
                    break;
                case "uint":
                    stringBuilder.append("int(11) ");
                    break;
                case "bigint":
                    stringBuilder.append("bigint(20) ");
                    break;
                case "datetime":
                    stringBuilder.append("datetime ");
                    break;
                case "utinyint":
                    if (size == 0) {
                        size = 11;
                    }
                    stringBuilder.append("tinyint(");
                    stringBuilder.append(size);
                    stringBuilder.append(") ");
                    break;
                default:
                    throw new RuntimeException("xml中配置了未知数据类型：" + field.type);

            }
            stringBuilder.append("COMMENT '");
            stringBuilder.append(field.desc);
            stringBuilder.append("',");
        }
        stringBuilder.setLength(stringBuilder.length() - 1);
        stringBuilder.append("\n) ENGINE=InnoDB DEFAULT CHARSET=utf8;");
        String string = stringBuilder.toString();
        //        try {
        //            string= new String(string.getBytes(),"utf-8");
        //        } catch (UnsupportedEncodingException e) {
        //            e.printStackTrace();
        //        }
        return string;
    }
}
