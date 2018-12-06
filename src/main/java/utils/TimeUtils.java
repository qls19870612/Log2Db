package utils;

import org.joda.time.DateTimeConstants;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.chrono.ISOChronology;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.TimeZone;

/**
 * @author Liwei
 *
 */
public class TimeUtils {

    public static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("yy-MM-dd HH:mm:ss");

    private static final ISOChronology chronology = ISOChronology.getInstance();

    public static final long MILLIS_PER_DAY = DateTimeConstants.MINUTES_PER_DAY;

    public static boolean isSameDayOfMillis(final long ms1, final long ms2) {
        final long interval = ms1 - ms2;
        return interval < MILLIS_PER_DAY && interval > -1L * MILLIS_PER_DAY && toDay(ms1) == toDay(ms2);
    }

    public static String printTime(long time) {
        return FORMATTER.print(time);
    }

    private static long toDay(long millis) {
        return (millis + TimeZone.getDefault().getOffset(millis)) / MILLIS_PER_DAY;
    }

    public static int toHourOfDay(long instant) {
        return chronology.hourOfDay().get(instant);
    }

    /**
     * 返回多少号，取值[1-31]
     * @param instant
     * @return
     */
    public static int toDayOfMonth(long instant) {
        return chronology.dayOfMonth().get(instant);
    }

    /**
     * 返回多少号，取值[1-12]
     * @param instant
     * @return
     */
    public static int toMonthOfYear(long instant) {
        return chronology.monthOfYear().get(instant);
    }

    public static int toYear(long instant) {
        return chronology.year().get(instant);
    }

    /**
     * 判断某一个操作，自上次执行的时间点lastOpTime之后，当前时间点currentTime经过了 多少次重置时间点resetTime
     *
     * <p>
     */
    public static int getDoDailyResetCount(long currentTime, long lastOpTime, LocalTime resetTime) {
        if (currentTime <= lastOpTime) {
            return 0;
        }

        long resetMillisOfDay = resetTime.getMillisOfDay();
        return Days.daysBetween(new LocalDate(lastOpTime - resetMillisOfDay), new LocalDate(currentTime - resetMillisOfDay)).getDays();
    }

    public static int getDoDailyResetCount(long currentTime, long lastOpTime) {
        return getDoDailyResetCount(currentTime, lastOpTime, LocalTime.MIDNIGHT);
    }


    /**
     * 传入 23:00 格式数据返回 相对于零点的分钟数
     */
    public static int getMinuteOfDay(String timeStr) {
        try {
            int hour, min;

            String[] time = timeStr.split(":");
            if (time.length != 2) {
                throw new IllegalArgumentException("getMinuteOfDay时, 参数格式错误:" + timeStr);
            }

            hour = Integer.parseInt(time[0]);
            min = Integer.parseInt(time[1]);

            if (hour < 0 || hour >= 24) {
                throw new IllegalArgumentException("getMinuteOfDay时, 小时参数格式错误:" + time[0]);
            }
            if (min < 0 || min >= 60) {
                throw new IllegalArgumentException("getMinuteOfDay时, 分钟参数格式错误:" + time[1]);
            }

            return hour * 60 + min;
        } catch (Throwable throwable) {
            throw throwable;
        }
    }

    public static int millsToSecond(long mills) {
        return (int) (mills / 1000);
    }

    public static String printTimeDuration(long mills) {
        int second = millsToSecond(mills);
        int remainSecond = second % 60;
        int minute = second / 60;
        int remainMinute = minute % 60;

        int hour = minute / 60;
        int remainHour = hour % 24;
        int day = hour / 24;
        StringBuilder stringBuilder = new StringBuilder();
        if (day > 0) {
            stringBuilder.append(day);
            stringBuilder.append("天");
        }
        if (stringBuilder.length() > 0 || remainHour > 0) {
            stringBuilder.append(remainHour);
            stringBuilder.append("小时");
        }
        if (stringBuilder.length() > 0 || remainMinute > 0) {
            stringBuilder.append(remainMinute);
            stringBuilder.append("分钟");
        }

        stringBuilder.append(remainSecond);
        stringBuilder.append("秒");
        return stringBuilder.toString();

    }

    private TimeUtils() {
    }
}
