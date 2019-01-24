package infos;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
    public final int dtEventTimeIndex;
    private Map<String, TableField> fieldMap;

    public TableStruct(String tableName, TableField[] fields) {
        this.tableName = tableName;
        this.fields = fields;
        this.prepareSql = null;
        this.fieldStrBytes = null;
        this.dtEventTimeIndex = -1;
    }

    public final Map<String, TableField> getFieldMap() {
        if (fieldMap == null) {
            fieldMap = new HashMap<>();
            for (TableField field : fields) {
                fieldMap.put(field.fieldName, field);
            }
        }
        return fieldMap;
    }

    public TableStruct(Element item) {
        NodeList childNodes = item.getChildNodes();
        int iLen = childNodes.getLength();
        ArrayList<TableField> arrayList = new ArrayList<>();
        StringBuilder fieldStr = new StringBuilder("` (");
        int tmpDtEventTimeIndex = -1;
        int count = -1;
        for (int i = 0; i < iLen; i++) {
            Node entry = childNodes.item(i);
            if (entry.getNodeName().equals("entry")) {
                count++;
                TableField tableField = new TableField((Element) entry);
                arrayList.add(tableField);
                fieldStr.append(tableField.fieldName);
                fieldStr.append(",");
                if (tmpDtEventTimeIndex == -1 && tableField.fieldName.equals("dtEventTime")) {
                    tmpDtEventTimeIndex = count;
                }
            }
        }
        if (tmpDtEventTimeIndex == -1) {
            throw new RuntimeException("每条日志必需有 dtEventTime 字段");
        }
        dtEventTimeIndex = tmpDtEventTimeIndex;
        TableField field = new TableField(true, "dt", "string", 20, "记录时间日期");
        arrayList.add(field);
        fieldStr.append(field.fieldName);

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
        for (TableField ignored : fields) {
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
            stringBuilder.append("\n");
            stringBuilder.append("`");
            stringBuilder.append(field.fieldName);
            stringBuilder.append("` ");
            appendFiledTypeAndComment(stringBuilder, field);
            stringBuilder.append(",");

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

    public static void appendFiledTypeAndComment(StringBuilder stringBuilder, TableField field) {

        stringBuilder.append(field.type);
        if (!field.isDate()) {

            stringBuilder.append('(');
            stringBuilder.append(field.size);
            stringBuilder.append(')');
        }
        stringBuilder.append(' ');

        stringBuilder.append("COMMENT '");
        stringBuilder.append(field.desc);
        stringBuilder.append("'");

    }
}
