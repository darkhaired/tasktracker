package tasktracker.backend.controller;

import com.google.common.collect.Lists;
import tasktracker.backend.controller.exception.ApiException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class DateTimePatterns {
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private static final String YEAR_MONTH_FORMAT = "yyyy-MM";
    private static final String YEAR_FORMAT = "yyyy";

    public static SimpleDateFormat getDefault() {
        return new SimpleDateFormat(DATE_FORMAT);
    }

    public static SimpleDateFormat getDateTimeFormat() {
        return new SimpleDateFormat(DATETIME_FORMAT);
    }

    public static Date getDateFromFormattedDateString(String inputDate) {
        if (Objects.isNull(inputDate)) {
            return null;
        }

        final List<SimpleDateFormat> knownPatterns = Lists.newArrayListWithExpectedSize(2);
        knownPatterns.add(new SimpleDateFormat(DATETIME_FORMAT));
        knownPatterns.add(new SimpleDateFormat(DATE_FORMAT));
        knownPatterns.add(new SimpleDateFormat(YEAR_MONTH_FORMAT));

        for (SimpleDateFormat df : knownPatterns) {
            df.setLenient(false);
            try {
                return df.parse(inputDate);
            } catch (ParseException pe) {
                // does nothing
            }
        }

        throw new ApiException.InvalidInputData(inputDate);
    }

    public static Date getDateFromFormattedDateStringOrNull(String inputDate) {
        try {
            return getDateFromFormattedDateString(inputDate);
        } catch (Exception e) {
            return null;
        }
    }

    public static Date getDateTimeFromFormattedString(String inputDate) {
        if (Objects.isNull(inputDate)) {
            return null;
        }

        final SimpleDateFormat df = new SimpleDateFormat(DATETIME_FORMAT);
        df.setLenient(false);

        try {
            return df.parse(inputDate);
        } catch (ParseException ex) {
            throw new ApiException.InvalidInputData(inputDate);
        }
    }

    public static String getFormattedDateTimeString(final Date date) {
        final SimpleDateFormat df = new SimpleDateFormat(DATETIME_FORMAT);
        df.setLenient(false);

        return df.format(date);
    }

    public static boolean ltOrEq(Date first, Date second, boolean tillSeconds) {
        return eq(first, second, tillSeconds) || lt(first, second);
    }

    public static boolean gtOrEq(Date first, Date second, boolean tillSeconds) {
        return eq(first, second, tillSeconds) || gt(first, second);
    }

    public static boolean lt(Date first, Date second) {
        return first.before(second);
    }

    public static boolean gt(Date first, Date second) {
        return first.after(second);
    }

    public static boolean eq(Date first, Date second, boolean tillSeconds) {
        Calendar firstCalendar = Calendar.getInstance();
        Calendar secondCalendar = Calendar.getInstance();
        firstCalendar.setTime(first);
        secondCalendar.setTime(second);

        if (tillSeconds) {
            return firstCalendar.get(Calendar.YEAR) == secondCalendar.get(Calendar.YEAR)
                    && firstCalendar.get(Calendar.MONTH) == secondCalendar.get(Calendar.MONTH)
                    && firstCalendar.get(Calendar.DAY_OF_MONTH) == secondCalendar.get(Calendar.DAY_OF_MONTH)
                    && firstCalendar.get(Calendar.HOUR_OF_DAY) == secondCalendar.get(Calendar.HOUR_OF_DAY)
                    && firstCalendar.get(Calendar.MINUTE) == secondCalendar.get(Calendar.MINUTE)
                    && firstCalendar.get(Calendar.SECOND) == secondCalendar.get(Calendar.SECOND);
        } else {
            return firstCalendar.get(Calendar.YEAR) == secondCalendar.get(Calendar.YEAR)
                    && firstCalendar.get(Calendar.MONTH) == secondCalendar.get(Calendar.MONTH)
                    && firstCalendar.get(Calendar.DAY_OF_MONTH) == secondCalendar.get(Calendar.DAY_OF_MONTH);
        }
    }

    public static Date addDays(Date date, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DATE, days);
        return calendar.getTime();
    }

    public static String toYyyyMmDd(final Date date) {
        return new SimpleDateFormat(DATE_FORMAT).format(date);
    }
}
