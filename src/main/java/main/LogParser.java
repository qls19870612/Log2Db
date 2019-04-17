package main;

import com.google.common.base.Preconditions;
import com.google.common.io.Files;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.IgnoreExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WorkerPool;
import com.lmax.disruptor.dsl.ProducerType;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import config.ConfigProperty;
import db.TomcatJdbcPool;
import infos.HandlerLogInfo;
import infos.PlatInfo;
import infos.TableField;
import infos.TableStruct;
import utils.TimeUtils;

/**
 *
 * 创建人  liangsong
 * 创建时间 2018/11/17 17:29
 */
public class LogParser {
    public static String NEW_LINE = System.getProperty("line.separator");
    private static final Logger logger = LoggerFactory.getLogger(LogParser.class);
    public static final DateTimeFormatter MILLIS = DateTimeFormat.forPattern("yyyyMMddHHmmssSSS");

    public final PlatInfo platInfo;
    private final ConfigProperty properties;
    private final String logDbName;
    private final Long startTime;
    private final Long endTime;
    private String configDbName;


    private HashMap<String, HandlerLogInfo> fileProcessorMap = new HashMap<>();//key:文件名 value:解析日期
    private TomcatJdbcPool logDbPool;
    public XmlTemplateParser xmlTemplateParser;
    private final File platFolder;

    private AtomicInteger totalSuccessHandlerFileCount = new AtomicInteger(0);
    public static final DateTimeFormatter DAY = DateTimeFormat.forPattern("yyyyMMdd");

    private final long currDate = new Date().getTime();
    private final long oneDayMS = TimeUnit.DAYS.toMillis(1);
    private long startDate;
    public long endDate;
    private ThreadPoolExecutor threadPoolExecutor;
    int threadCount = Runtime.getRuntime().availableProcessors() * 2;

    private RingBuffer<LogEvent> ringBuffer;
    private HandlerLogInfo[] handlerLogInfos;
    private WorkerPool<LogEvent> workerPool;


    public LogParser(String[] args, ConfigProperty properties) throws Exception {
        this.properties = properties;
        long runStartTime = System.currentTimeMillis();
        if (args.length <= 1) {
            throw new RuntimeException("必需要有游戏名，平台名");
        }
        int count = 0;
        String gameName = args[count++];
        String platName = args[count++];
        platInfo = new PlatInfo(gameName, platName);
        configDbName = "db" + gameName + "conf";
        logDbName = "db" + gameName + platName + "log";
        logger.debug("LogParser configDbName:{},logDbName:{}", configDbName, logDbName);


        if (args.length > count) {

            startDate = getDay(args[count], "开始日期");
        } else {
            startDate = getDay("0", "开始日期");
        }
        count++;
        if (args.length > count) {
            endDate = getDay(args[count], "结束日期");
        } else {
            endDate = startDate + oneDayMS;
        }
        Preconditions.checkArgument(endDate > startDate, "结束日期必需大于等于开始日期startDate:%s, endDate:%s", TimeUtils.printTime(startDate),
                TimeUtils.printTime(endDate));
        logger.debug("LogParser TimeUtils.printTime(startDate):{}", TimeUtils.printTime(startDate));
        logger.debug("LogParser TimeUtils.printTime(endDate):{}", TimeUtils.printTime(endDate));

        startTime = Long.valueOf(MILLIS.print(startDate));
        endTime = Long.valueOf(MILLIS.print(endDate));

        loadPlatInfoFromDb();

        String logFolderPath = properties.logs_dir.endsWith("/") ? properties.logs_dir : properties.logs_dir + "/";
        platFolder = new File(logFolderPath + platInfo.getPlatId());
        logger.debug("LogParser platFolder.getPath():{}", platFolder.getPath());
        if (!platFolder.exists() || !platFolder.isDirectory()) {
            throw new RuntimeException("平台对应的目录不存在!");
        }
        parserParserHistoryConfig(platFolder);

        xmlTemplateParser = new XmlTemplateParser();

        tryCreateLogDb(xmlTemplateParser);


        ArrayList<LogFileParser> logFileParsers = prepareParserList(platFolder);

        startParser(logFileParsers);
//        startParserByDisruptor(logFileParsers);
        logger.debug("LogParser 处理文件个数:{},入库文件个数:{}, 耗时:{}", logFileParsers.size(), totalSuccessHandlerFileCount.get(),
                System.currentTimeMillis() - runStartTime);
    }

    private void startParserByDisruptor(ArrayList<LogFileParser> logFileParsers) throws Exception {

        //        CountDownLatch countDownLatch = new CountDownLatch(logFileParsers.size());
        int totalHandlerFileCount = logFileParsers.size();
        AtomicInteger nowHandlerCount = new AtomicInteger(0);
        int PER_TIME_HANDLER_COUNT = 512;
        int HALF_PER_TIME_HANDLER_COUNT = PER_TIME_HANDLER_COUNT / 2;


        ExecutorService executor = initDisruptor(nowHandlerCount, PER_TIME_HANDLER_COUNT * 2);

        prepareToDb();
        handlerLogInfos = new HandlerLogInfo[logFileParsers.size()];
        //        for (LogFileParser logFileParser : logFileParsers) {
        //            long next = ringBuffer.next();
        //            LogEvent logEvent = ringBuffer.get(next);
        //            logEvent.logFileParser = logFileParser;
        //            ringBuffer.publish(next);
        //        }
        logger.debug("startParserByDisruptor 文件总数:{}", logFileParsers.size());
        int headIndex = 0;
        while (true) {
            if (nowHandlerCount.get() >= totalHandlerFileCount) {
                break;
            }


            if (headIndex < totalHandlerFileCount) {


                if (headIndex - nowHandlerCount.get() < HALF_PER_TIME_HANDLER_COUNT) {
                    logger.debug("startParserByDisruptor 现处理个数:{}", nowHandlerCount.get());
                    //ringbuffer 池子有可用一半的空间时，再加满
                    int iLen = Math.min(totalHandlerFileCount, headIndex + PER_TIME_HANDLER_COUNT);
                    for (int i = headIndex; i < iLen; i++) {
                        long next = ringBuffer.next();
                        LogEvent logEvent = ringBuffer.get(next);
                        LogFileParser logFileParser = logFileParsers.get(i);
                        if (logFileParser == null) {
                            throw new RuntimeException("怎么可能" + i);
                        }
                        logEvent.logFileParser = logFileParser;
                        ringBuffer.publish(next);
                        headIndex++;
                    }
                }
            }
            Thread.sleep(30);


        }
        logger.debug("startParserByDisruptor 完成");

        //        countDownLatch.await();
        logDbPool.close();
        writeParseHistory(handlerLogInfos);
        workerPool.halt();
        executor.shutdown();

    }

    private void writeParseHistory(HandlerLogInfo[] handlerLogInfos) throws Exception {
        StringBuilder stringBuilder = new StringBuilder();
        for (HandlerLogInfo handlerLogInfo : handlerLogInfos) {
            if (handlerLogInfo == null) {
                break;
            }
            stringBuilder.append(handlerLogInfo.fileName);
            stringBuilder.append("=");
            stringBuilder.append(handlerLogInfo.handlerTime);
            stringBuilder.append(NEW_LINE);
        }
        RandomAccessFile randomAccessFile = new RandomAccessFile(new File(getPlatLogFile()), "rw");
        if (properties.useCache) {
            randomAccessFile.seek(randomAccessFile.length());
        } else {
            randomAccessFile.seek(0);
        }
        randomAccessFile.seek(randomAccessFile.length());
        randomAccessFile.write(stringBuilder.toString().getBytes("UTF-8"));

        randomAccessFile.close();
    }


    private void prepareToDb() throws Exception {
        createLogDbPool();


        if (!properties.useCache) {
            long startDateMs = startDate / 1000;
            long endDateMs = endDate / 1000;
            //不使用缓存的时候，要先把日志库内，指定日期的的数据删除后，再添加 ，防止重复添加
            Connection connection = logDbPool.getConnection();
            for (TableStruct tableStruct : xmlTemplateParser.tableStructHashMap.values()) {
                String conditions =
                        tableStruct.tableName + " where unix_timestamp(dtEventTime) > " + startDateMs + " and unix_timestamp(dtEventTime) <= " +
                                endDateMs;
                String deleteSql = "delete from " + conditions;
                //                logger.debug("handParseResult deleteSql:{}", deleteSql);
                Statement statement = connection.createStatement();
                statement.execute(deleteSql);
                statement.close();
            }


        }
    }

    private long getDay(String dayStr, String who) {
        long date = 0;
        Preconditions.checkArgument(dayStr.length() <= 6, "开始%s，必需小于6个字符，可以有负号", who);
        int day = Integer.parseInt(dayStr);
        if (dayStr.length() == 6) {
            Preconditions.checkArgument(day > 20180000 && day < 20380000, "%s果是6位数，取值范围需要 startDay > 20180000&& startDay < 20380000", who);
            date = DAY.parseDateTime(dayStr).getMillis();
        } else {
            Preconditions.checkArgument(day <= 365, "如果%s<6个字符时，必需是<=365", who);
            Date d = new Date(currDate);
            Date d2 = new Date(d.getYear(), d.getMonth(), d.getDate());

            date = d2.getTime() + (day) * oneDayMS;
        }
        return date;
    }

    private void loadPlatInfoFromDb() throws SQLException {
        Connection connection = getConnection(properties.db_url + configDbName, properties.db_user, properties.db_passwd);
        assert connection != null;
        try (Statement statement = connection.createStatement()) {
            String sql = "select * from tbplt where vPname = '" + platInfo.getPlatName() + "'";
            //            logger.debug("loadPlatInfoFromDb sql:{}", sql);
            ResultSet resultSet = statement.executeQuery(sql);

            int platId = 0;

            boolean next = resultSet.next();
            if (next) {
                platId = resultSet.getInt("iPid");
                logger.debug("loadPlatInfoFromDb platId:{}", platId);
            } else {
                logger.debug("loadPlatInfoFromDb :{}", "没有找到");
            }
            if (platId < 0) {
                throw new RuntimeException(configDbName + "找不到平台配置 platName:" + platInfo.getPlatName());
            } else {
                platInfo.setPlatId(platId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.debug("loadPlatInfoFromDb e.getMessage():{}", e.getMessage());
        } finally {
            connection.close();
        }
    }

    /**
     * 处理文件逻辑
     * 多线程分组文件结构{table:[log,log,...]}
     * 然后入库等操作
     * @param logFileParsers
     * @throws Exception
     */
    private void startParser(ArrayList<LogFileParser> logFileParsers) throws Exception {
        if (logFileParsers.size() > 0) {
            final CountDownLatch latch = new CountDownLatch(logFileParsers.size());
            threadPoolExecutor = new ThreadPoolExecutor(threadCount, threadCount, 60L, TimeUnit.MINUTES, new LinkedBlockingQueue<>(), new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r);
                }
            });

            for (LogFileParser parser : logFileParsers) {
                threadPoolExecutor.execute(() -> {
                    try {
                        parser.parser(xmlTemplateParser, platInfo);
                    } catch (IOException e) {
                        logger.debug("解析出错: logFileParser.logFile.getPath():{}", parser.logFile.getPath());
                        logger.debug("Error e:{}", e);
                    }
                    latch.countDown();
                });
            }

            latch.await();

            handParseResult(logFileParsers);
            threadPoolExecutor.shutdown();
        } else {
            logger.debug("startParser :没有文件需要处理");
        }
    }

    private ExecutorService initDisruptor(AtomicInteger countDownLatch, int PER_TIME_HANDLER_COUNT) {
        logger.debug("initDisructor threadCount:{}", threadCount);

        AtomicInteger threadNum = new AtomicInteger(0);
        ExecutorService exector = Executors.newFixedThreadPool(threadCount, r -> {
            int count = threadNum.incrementAndGet();
            return new Thread(r, "logthread:" + count);
        });

        ringBuffer = RingBuffer.create(ProducerType.MULTI, LogEvent.FACTORY, PER_TIME_HANDLER_COUNT, new BlockingWaitStrategy());
        Consumer[] consumers = new Consumer[threadCount];
        int iLen = consumers.length;
        for (int i = 0; i < iLen; i++) {
            consumers[i] = new Consumer(countDownLatch, this);
        }
        workerPool = new WorkerPool<LogEvent>(ringBuffer, ringBuffer.newBarrier(), new IgnoreExceptionHandler(), consumers);
        workerPool.start(exector);

        return exector;


    }


    private void tryCreateLogDb(XmlTemplateParser xmlTemplateParser) throws Exception {
        String sql = "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name=\"" + logDbName + "\"";

        Connection connection = getConnection(properties.db_url, properties.db_user, properties.db_passwd);

        Statement statement = connection.createStatement();
        ResultSet execute = statement.executeQuery(sql);
        boolean hasDb = false;
        while (execute.next()) {
            int anInt = execute.getInt(1);
            if (anInt > 0) {
                hasDb = true;
                break;
            }
        }
        //                if (hasDb) {
        //                    hasDb = false;
        //                    statement.execute("drop database IF EXISTS " + logDbName);
        //                }


        if (!hasDb) {

            boolean execute1 = statement.execute("create database " + logDbName);
            logger.debug("tryCreateLogDb execute1:{}", execute1);

        }
        statement.close();
        connection.close();
        if (!hasDb || properties.check_add_table) {

            createLogDbPool();
            connection = logDbPool.getConnection();
            statement = connection.createStatement();


            HashSet<String> newCreateTables = new HashSet();
            for (Entry<String, TableStruct> entry : xmlTemplateParser.tableStructHashMap.entrySet()) {
                try {
                    TableStruct xmlTableStruct = entry.getValue();
                    String createTableSql = xmlTableStruct.getCreateTableSql();

                    if (!statement.execute(createTableSql)) {//没成功，代表已经有表，检查表结构
                        newCreateTables.add(xmlTableStruct.tableName);


                    }


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            StringBuilder alertSql = new StringBuilder();

            HashMap<String, TableStruct> dbStruct = getTableField(connection);
            for (String tableName : newCreateTables) {
                TableStruct xmlTableStruct = xmlTemplateParser.tableStructHashMap.get(tableName);

                TableStruct dbTableStruct = dbStruct.get(tableName);

                Map<String, TableField> dbTableFields = dbTableStruct.getFieldMap();
                Map<String, TableField> xmlTableFileds = xmlTableStruct.getFieldMap();
                alertSql.setLength(0);
                for (Entry<String, TableField> tf : xmlTableFileds.entrySet()) {
                    String fieldName = tf.getKey();
                    TableField xmlTableField = tf.getValue();
                    if (dbTableFields.containsKey(fieldName)) {
                        if (!dbTableFields.get(fieldName).equals(xmlTableField)) {
                            logger.debug("tryCreateLogDb tf.getValue():{}, dbTableFields.get(tf.getKey()):{}", xmlTableField,
                                    dbTableFields.get(fieldName));
                            alertSql.append("ALTER TABLE ");
                            alertSql.append(tableName);
                            alertSql.append(" CHANGE ");
                            alertSql.append(xmlTableField.fieldName);
                            alertSql.append(" ");
                            alertSql.append(xmlTableField.fieldName);
                            alertSql.append(" ");


                            TableStruct.appendFiledTypeAndComment(alertSql, xmlTableField);


                            alertSql.append(';');
                        }

                    } else {
                        logger.debug("tryCreateLogDb tf.getKey():{}", fieldName);
                        alertSql.append("ALTER TABLE ");
                        alertSql.append(tableName);
                        alertSql.append(" ADD ");
                        alertSql.append(xmlTableField.fieldName);
                        alertSql.append(' ');
                        TableStruct.appendFiledTypeAndComment(alertSql, xmlTableField);
                        alertSql.append(';');

                    }
                }
                if (alertSql.length() > 0) {
                    logger.debug("tryCreateLogDb alertSql.toString():{}", alertSql.toString());
                    int update = statement.executeUpdate(alertSql.toString());
                    logger.debug("tryCreateLogDb update:{}", update);
                }

            }

            statement.close();
            connection.close();
        }


    }

    private HashMap<String, TableStruct> getTableField(Connection logDbCon) throws Exception {

        HashMap<String, TableStruct> ret = new HashMap<>();
        DatabaseMetaData metaData = logDbCon.getMetaData();
        ResultSet tableRet = metaData.getTables(null, "%", "%", new String[]{"TABLE"});
        /*其中"%"就是表示*的意思，也就是任意所有的意思。其中m_TableName就是要获取的数据表的名字，如果想获取所有的表的名字，就可以使用"%"来作为参数了。*/

        //3. 提取表的名字。
        ArrayList<TableField> fields = new ArrayList();
        while (tableRet.next()) {

            String table_name = tableRet.getString("TABLE_NAME");


            String columnName;
            String columnType;
            ResultSet colRet = metaData.getColumns(null, "%", table_name, "%");
            fields.clear();
            while (colRet.next()) {
                columnName = colRet.getString("COLUMN_NAME");
                columnType = colRet.getString("TYPE_NAME");
                int dataSize = colRet.getInt("COLUMN_SIZE");
                String remarks = colRet.getString("REMARKS");

                fields.add(new TableField(columnName, columnType, dataSize, remarks));
            }
            TableField[] tableFields = fields.toArray(TableField.EMPTY);
            TableStruct tableStruct = new TableStruct(table_name, tableFields);
            ret.put(table_name, tableStruct);
        }
        tableRet.close();
        return ret;
    }

    private Connection getConnection(String url, String user, String password) {

        //        String driverName = "com.mysql.jdbc.Driver";
        //        try {
        //            Class.forName(driverName);
        //        } catch (ClassNotFoundException e) {
        //            e.printStackTrace();
        //        }
        try {
            return DriverManager.getConnection("jdbc:mysql://" + url + "", user, password);
            //?useUnicode=true&characterEncoding=UTF-8
            //&characterEncoding=UTF-8
            //useUnicode=true
        } catch (SQLException e) {
            logger.debug("getConnection e.getMessage():{}", e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private void createLogDbPool() {
        if (logDbPool == null) {
            logDbPool = new TomcatJdbcPool(properties.db_url + logDbName, properties.db_user, properties.db_passwd);
        }
    }

    /**
     * 多线程日志入库
     * 及后续处理
     * @param logFileParsers
     * @throws SQLException
     * @throws IOException
     * @throws InterruptedException
     */
    private void handParseResult(ArrayList<LogFileParser> logFileParsers) throws SQLException, IOException, InterruptedException {
        createLogDbPool();
        long time = new Date().getTime();

        if (!properties.useCache) {
            long startDateMs = startDate / 1000;
            long endDateMs = endDate / 1000;
            //不使用缓存的时候，要先把日志库内，指定日期的的数据删除后，再添加 ，防止重复添加
            HashSet<String> allTables = new HashSet<String>();

            for (LogFileParser logFileParser : logFileParsers) {
                for (Entry<String, ArrayList<String>> stringStringBuilderEntry : logFileParser.tableSqlMap.entrySet()) {
                    allTables.add(stringStringBuilderEntry.getKey());
                }
            }
            Connection connection = logDbPool.getConnection();
            for (String table : allTables) {
                String conditions =
                        table + " where unix_timestamp(dtEventTime) > " + startDateMs + " and unix_timestamp(dtEventTime) <= " + endDateMs;
                String deleteSql = "delete from " + conditions;
                //                logger.debug("handParseResult deleteSql:{}", deleteSql);
                Statement statement = connection.createStatement();
                statement.execute(deleteSql);
                statement.close();
            }
        }


        ArrayList<HandlerLogInfo> parseLog = new ArrayList<>();
        int perThreadHandCount = logFileParsers.size() / threadCount;

        //取模剩余的文件数
        int remainCount = logFileParsers.size() % threadCount;

        int maxCountThreadCount = Math.min(threadCount, logFileParsers.size());
        int startIndex = 0;
        int handCount; //每个线程处理的文件数
        final CountDownLatch latch = new CountDownLatch(maxCountThreadCount);
        for (int i = 0; i < maxCountThreadCount; i++) {
//            int handCount = i >= remainCount ? perThreadHandCount : perThreadHandCount + 1;
            if (i == 0){
                handCount = perThreadHandCount + remainCount;
            }else {
                handCount = perThreadHandCount;
            }
            List<LogFileParser> handList = logFileParsers.subList(startIndex, Math.min(logFileParsers.size(), startIndex + handCount));
            startIndex += handCount;
            threadPoolExecutor.execute(new Runnable() {

                @Override
                public void run() {
                    try {
                        Connection connection = logDbPool.getConnection();
                        connection.setAutoCommit(false);

                        int totalCount = 0;
                        int fileCount = 0;
                        int tableCount = 0;
                        for (LogFileParser logFileParser : handList) {
                            try {
                                fileCount = 0;
                                //一个文件内日志入库操作
                                for (Entry<String, ArrayList<String>> stringStringBuilderEntry : logFileParser.tableSqlMap.entrySet()) {
                                    ArrayList<String> logItems = stringStringBuilderEntry.getValue();
                                    tableCount = logItems.size();
                                    TableStruct tableStruct = xmlTemplateParser.getTableStruct(stringStringBuilderEntry.getKey());
                                    PreparedStatement preparedStatement = connection.prepareStatement(tableStruct.prepareSql);

                                    fileCount += logItems.size();
                                    for (String fieldStrs : logItems) {
                                        String[] fields = fieldStrs.split("\\|");
                                        int c = 1;
                                        for (String filed : fields) {
                                            preparedStatement.setString(c++, filed);
                                        }

                                        String dt = fields[tableStruct.dtEventTimeIndex].split(" ")[0];
                                        preparedStatement.setString(c, dt);
                                        preparedStatement.addBatch();
                                    }
                                    preparedStatement.executeBatch();
                                    connection.commit();
                                    preparedStatement.close();
                                }
                                totalSuccessHandlerFileCount.incrementAndGet();
                                //记录下已处理的文件和时间
                                parseLog.add(new HandlerLogInfo(logFileParser.logFile.getName(), time));
                            } catch (Exception e) {
                                logger.debug("handParseResult sqls.size():{},fileCount:{},totalCount:{}", tableCount, fileCount, totalCount);
                                e.printStackTrace();
                                connection.rollback();
                                //                                logger.debug("handParseResult stringBuilder.toString():{}", stringBuilder.toString());
                                logger.debug("handParseResult logFileParser.fileName:{}", logFileParser.logFile.getName());
                            }

                            totalCount += fileCount;


                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    latch.countDown();
                }
            });
        }

        latch.await();
        logDbPool.close();
        //记录已处理文件
        writeParseHistory(parseLog);


    }

    /**
     * 记录已处理日志信息到parseLog.txt
     * @param processMap
     * @throws IOException
     */
    private void writeParseHistory(ArrayList<HandlerLogInfo> processMap) throws IOException {

        StringBuilder stringBuilder = new StringBuilder();
        for (HandlerLogInfo handlerLogInfo : processMap) {
            if (handlerLogInfo == null) {
                logger.debug("writeParseHistory :{}", processMap);
                break;
            }
            stringBuilder.append(handlerLogInfo.fileName);
            stringBuilder.append("=");
            stringBuilder.append(handlerLogInfo.handlerTime);
            stringBuilder.append(NEW_LINE);
        }

        //打开记录文件,记录在最后
        RandomAccessFile randomAccessFile = new RandomAccessFile(new File(getPlatLogFile()), "rw");
        if (properties.useCache) {
            randomAccessFile.seek(randomAccessFile.length());
        } else {
            randomAccessFile.seek(0);
        }
        randomAccessFile.seek(randomAccessFile.length());
        randomAccessFile.write(stringBuilder.toString().getBytes("UTF-8"));

        randomAccessFile.close();
    }

    private String getPlatLogFile() {
        return platFolder.getPath() + "/parseLog.txt";
    }

    /**
     * 生成之前处理过的日志信息map表
     * @param platFolder
     * @throws IOException
     */
    private void parserParserHistoryConfig(File platFolder) throws IOException {
        if (!properties.useCache) {
            return;
        }
        File file = new File(getPlatLogFile());
        if (!file.exists()) {
            return;
        }
        List<String> strings = Files.readLines(file, Charset.defaultCharset());
        logger.debug("parserParserHistoryConfig strings.size():{}", strings.size());
        for (String string : strings) {
            String[] split = string.split("=");
            fileProcessorMap.put(split[0].toLowerCase(), new HandlerLogInfo(split[0], Long.valueOf(split[1])));
        }

    }

    /**
     * 备好待处理的格式化日志List
     * @param platFolder
     * @return
     */
    private ArrayList<LogFileParser> prepareParserList(File platFolder) {
        //获取日志文件数组
        File[] logFiles = platFolder.listFiles(pathname -> pathname.getName().endsWith(".log"));

        ArrayList<LogFileParser> needProcessFiles = new ArrayList<LogFileParser>();
        assert logFiles != null;
        for (File logFile : logFiles) {
            String[] split = logFile.getName().replace(".log", "").split("_");
            long time = Long.parseLong(split[split.length - 1]);
            //跟处理过的文件列表对比
            boolean b = fileProcessorMap.containsKey(logFile.getName().toLowerCase());
            if (time >= startTime && time < endTime && !b) {
                int serverId = Integer.parseInt(split[split.length - 2]);
                //加入待处理文件
                needProcessFiles.add(new LogFileParser(serverId, logFile));

            }
        }
        return needProcessFiles;


    }

    public void logToDb(LogFileParser logFileParser) throws SQLException {
        Connection connection = logDbPool.getConnection();
        connection.setAutoCommit(false);

        int tableCount = 0;
        //        StringBuilder info = new StringBuilder(1024);
        try {
            int index = 0;
            for (Entry<String, ArrayList<String>> stringStringBuilderEntry : logFileParser.tableSqlMap.entrySet()) {
                index++;
                ArrayList<String> logItems = stringStringBuilderEntry.getValue();
                tableCount = logItems.size();
                TableStruct tableStruct = xmlTemplateParser.getTableStruct(stringStringBuilderEntry.getKey());
                PreparedStatement preparedStatement = connection.prepareStatement(tableStruct.prepareSql);

                //                info.append(tableCount);
                //                info.append("\n");
                //                info.append(tableStruct.prepareSql);
                //                info.append("\n");
                //                info.append(tableStruct.tableName);
                //                info.append("\n");
                boolean isAdd = false;
                for (String sql : logItems) {

                    //                    info.append(sql);
                    //                    info.append("\n");
                    //                    info.append(tableStruct.fields.length);
                    //                    info.append("\n");
                    String[] fields = sql.split("\\|");
                    int c = 1;
                    for (String field : fields) {
                        preparedStatement.setString(c++, field);
                    }
                    int length = tableStruct.fields.length;
                    while (c < length) {
                        preparedStatement.setString(c++, "");
                    }
                    String dt = fields[tableStruct.dtEventTimeIndex].split(" ")[0];
                    preparedStatement.setString(c, dt);
                    preparedStatement.addBatch();
                    isAdd = true;
                }
                if (isAdd) {
                    try {

                        preparedStatement.executeBatch();
                    } catch (SQLException e) {
                        e.printStackTrace();
                        logger.debug("logToDb tableName:{},logFileParser:{}", stringStringBuilderEntry.getKey(), logFileParser.logFile.getPath());
                        //                        logger.debug("logToDb info.toString():{}", info.toString());
                        throw e;
                    }
                }
                preparedStatement.close();
            }
            connection.commit();
            int count = totalSuccessHandlerFileCount.getAndIncrement();
            handlerLogInfos[count] = new HandlerLogInfo(logFileParser.logFile.getName(), new Date().getTime());

        } catch (Exception e) {
            logger.debug("handParseResult totalCount:{}", tableCount);
            //            logger.debug("logToDb info.toString():{}", info.toString());
            e.printStackTrace();
            connection.rollback();
            //                                logger.debug("handParseResult stringBuilder.toString():{}", stringBuilder.toString());
            logger.debug("handParseResult logFileParser.fileName:{}", logFileParser.logFile.getName());
        } finally {
            connection.close();
        }


    }
}
