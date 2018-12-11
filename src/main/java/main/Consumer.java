package main;

import com.lmax.disruptor.WorkHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

/**
 *
 * 创建人  liangsong
 * 创建时间 2018/12/11 11:49
 */
public class Consumer implements WorkHandler<LogEvent> {
    private static final Logger logger = LoggerFactory.getLogger(Consumer.class);
    private final CountDownLatch countDownLatch;
    private final LogParser logParser;

    public Consumer(CountDownLatch countDownLatch, LogParser logParser) {

        this.countDownLatch = countDownLatch;
        this.logParser = logParser;
    }

    @Override
    public void onEvent(LogEvent logEvent) throws Exception {
        logEvent.logFileParser.parser(logParser.xmlTemplateParser, logParser.platInfo);
        logParser.logToDb(logEvent.logFileParser);
        
        logEvent.clear();
        countDownLatch.countDown();
    }
}
