package main;

import com.lmax.disruptor.EventFactory;

/**
 *
 * 创建人  liangsong
 * 创建时间 2018/12/11 11:08
 */
public class LogEvent {
    public static final EventFactory<LogEvent> FACTORY = new EventFactory<LogEvent>() {
        @Override
        public LogEvent newInstance() {
            return new LogEvent();
        }
    };
    public LogFileParser logFileParser;

    public void clear() {
        logFileParser.dispose();
        logFileParser = null;
    }
}
