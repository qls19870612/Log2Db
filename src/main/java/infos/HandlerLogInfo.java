package infos;

import java.util.Comparator;

/**
 *
 * 创建人  liangsong
 * 创建时间 2018/11/20 21:35
 */
public class HandlerLogInfo {
    public static final Comparator<HandlerLogInfo> compare = new Comparator<HandlerLogInfo>() {
        @Override
        public int compare(HandlerLogInfo o1, HandlerLogInfo o2) {
            if (o1.handlerTime > o1.handlerTime) {
                return 1;
            } else if (o1.handlerTime < o2.handlerTime) {
                return -1;
            }
            return o1.fileName.compareTo(o2.fileName);
        }
    };
    public final String fileName;
    public static final HandlerLogInfo[] EMPTY = new HandlerLogInfo[0];
    public final long handlerTime;

    public HandlerLogInfo(String fileName, long handlerTime) {
        this.fileName = fileName;
        this.handlerTime = handlerTime;
    }
}
