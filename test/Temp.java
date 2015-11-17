package org.apache.commons.convert;

import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Date/time <code>Converter</code> classes. */
public class DateTimeConverters implements ConverterLoader {

    /**
     * Calendar format string: <code>EEE MMM dd HH:mm:ss.SSS z yyyy</code>. 
     */
    public static final String CALENDAR_FORMAT = "EEE MMM dd HH:mm:ss.SSS z yyyy";
    /**
     * JDBC DATE format string: <code>yyyy-MM-dd</code>. 
     */
    public static final String JDBC_DATE_FORMAT = "yyyy-MM-dd";
    /**
     * JDBC TIME format string: <code>HH:mm:ss</code>. 
     */
    public static final String JDBC_TIME_FORMAT = "HH:mm:ss";

    /**
     * Returns an initialized DateFormat object.
     * 
     * @param tz
     * @return DateFormat object
     */
    protected static DateFormat toDateFormat(TimeZone tz) {
        DateFormat df = new SimpleDateFormat(JDBC_DATE_FORMAT);
        df.setTimeZone(tz);
        return df;
    }

    /**
     * Returns an initialized DateFormat object.
     * 
     * @param dateTimeFormat
     *            optional format string
     * @param tz
     * @param locale
     *            can be null if dateTimeFormat is not null
     * @return DateFormat object
     */
    protected static DateFormat toDateTimeFormat(String dateTimeFormat, TimeZone tz, Locale locale) {
        DateFormat df = null;
        if (Util.isEmpty(dateTimeFormat)) {
            df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale);
        } else {
            df = new SimpleDateFormat(dateTimeFormat);
        }
        df.setTimeZone(tz);
        return df;
    }

    /**
     * Returns an initialized DateFormat object.
     * 
     * @param tz
     * @return DateFormat object
     */
    protected static DateFormat toTimeFormat(TimeZone tz) {
        DateFormat df = new SimpleDateFormat(JDBC_TIME_FORMAT);
        df.setTimeZone(tz);
        return df;
    }

    public void loadConverters() {
        Converters.loadContainedConverters(DateTimeConverters.class);
        Converters.registerConverter(new GenericDateToLong<java.util.Date>(java.util.Date.class));
        Converters.registerConverter(new GenericDateToLong<java.sql.Date>(java.sql.Date.class));
        Converters.registerConverter(new GenericDateToLong<java.sql.Time>(java.sql.Time.class));
        Converters.registerConverter(new GenericDateToLong<java.sql.Timestamp>(java.sql.Timestamp.class));
    }

    /**
     * An object that converts a <code>Calendar</code> to a <code>Date</code>.
     */
    public static class CalendarToDate extends AbstractConverter<Calendar, Date> {
        public CalendarToDate() {
            super(Calendar.class, Date.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return Util.instanceOf(sourceClass, this.getSourceClass()) && Date.class.equals(targetClass);
        }

        public Date convert(Calendar obj) throws ConversionException {
            return obj.getTime();
        }
    }

    /**
     * An object that converts a <code>Calendar</code> to a <code>Long</code>.
     */
    public static class CalendarToLong extends AbstractConverter<Calendar, Long> {
        public CalendarToLong() {
            super(Calendar.class, Long.class);
        }

        /**
         * Returns the millisecond value of <code>obj</code>.
         */
        public Long convert(Calendar obj) throws ConversionException {
            return obj.getTimeInMillis();
        }
    }

    /**
     * An object that converts a <code>Calendar</code> to a <code>String</code>.
     */
    public static class CalendarToString extends AbstractLocalizedConverter<Calendar, String> {
        public CalendarToString() {
            super(Calendar.class, String.class);
        }

        /**
         * Converts <code>obj</code> to a <code>String</code> formatted as
         * {@link DateTimeConverters#CALENDAR_FORMAT}. The returned string is
         * referenced to the default time zone.
         */
        public String convert(Calendar obj) throws ConversionException {
            DateFormat df = new SimpleDateFormat(CALENDAR_FORMAT);
            df.setCalendar(obj);
            return df.format(obj.getTime());
        }

        /**
         * Converts <code>obj</code> to a <code>String</code> using the supplied
         * locale, time zone, and format string. If <code>formatString</code> is
         * <code>null</code>, the string is formatted as
         * {@link DateTimeConverters#CALENDAR_FORMAT}.
         */
        public String convert(Calendar obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            DateFormat df = toDateTimeFormat(formatString == null ? CALENDAR_FORMAT : formatString, timeZone, locale);
            df.setCalendar(obj);
            return df.format(obj.getTime());
        }
    }

    /**
     * An object that converts a <code>Calendar</code> to a <code>Timestamp</code>.
     */
    public static class CalendarToTimestamp extends AbstractConverter<Calendar, Timestamp> {
        public CalendarToTimestamp() {
            super(Calendar.class, Timestamp.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return Util.instanceOf(sourceClass, this.getSourceClass()) && Timestamp.class.equals(targetClass);
        }

        public Timestamp convert(Calendar obj) throws ConversionException {
            return new Timestamp(obj.getTimeInMillis());
        }
    }


    /**
     * An object that converts a <code>Date</code> to a <code>Calendar</code>.
     */
    public static class DateToCalendar extends GenericLocalizedConverter<Date, Calendar> {
        public DateToCalendar() {
            super(Date.class, Calendar.class);
        }

        public Calendar convert(Date obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            Calendar cal = Calendar.getInstance(timeZone, locale);
            cal.setTime(obj);
            return cal;
        }
    }

    /**
     * An object that converts a <code>java.util.Date</code> to a
     * <code>java.sql.Date</code>.
     */
    public static class DateToSqlDate extends AbstractConverter<Date, java.sql.Date> {
        public DateToSqlDate() {
            super(Date.class, java.sql.Date.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return (java.util.Date.class.equals(sourceClass) || java.sql.Timestamp.class.equals(sourceClass)) && java.sql.Date.class.equals(targetClass);
        }

        /**
         * Returns <code>obj</code> converted to a <code>java.sql.Date</code>.
         */
        @SuppressWarnings("deprecation")
        public java.sql.Date convert(Date obj) throws ConversionException {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(obj.getTime());
            return new java.sql.Date(cal.get(Calendar.YEAR) - 1900, cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        }
    }

    /**
     * An object that converts a <code>java.util.Date</code> to a
     * <code>java.sql.Time</code>.
     */
    public static class DateToSqlTime extends AbstractConverter<java.util.Date, java.sql.Time> {
        public DateToSqlTime() {
            super(Date.class, java.sql.Time.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return sourceClass == this.getSourceClass() && targetClass == this.getTargetClass();
        }

        @SuppressWarnings("deprecation")
        public java.sql.Time convert(Date obj) throws ConversionException {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(obj.getTime());
            return new java.sql.Time(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
        }
    }

    /**
     * An object that converts a <code>java.util.Date</code> to a
     * <code>String</code>.
     */
    public static class DateToString extends AbstractLocalizedConverter<Date, String> {
        public DateToString() {
            super(Date.class, String.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return Date.class.equals(sourceClass) && String.class.equals(targetClass);
        }

        /**
         * Converts <code>obj</code> to a <code>String</code> formatted as
         * {@link DateTimeConverters#CALENDAR_FORMAT}. The returned string is
         * referenced to the default time zone.
         */
        public String convert(Date obj) throws ConversionException {
            DateFormat df = new SimpleDateFormat(CALENDAR_FORMAT);
            return df.format(obj);
        }

        /**
         * Converts <code>obj</code> to a <code>String</code> using the supplied
         * locale, time zone, and format string. If <code>formatString</code> is
         * <code>null</code>, the string is formatted as
         * {@link DateTimeConverters#CALENDAR_FORMAT}.
         */
        public String convert(Date obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            DateFormat df = toDateTimeFormat(formatString == null ? CALENDAR_FORMAT : formatString, timeZone, locale);
            return df.format(obj);
        }
    }

    /**
     * An object that converts a <code>java.util.Date</code> to a
     * <code>java.sql.Timestamp</code>.
     */
    public static class DateToTimestamp extends AbstractConverter<Date, java.sql.Timestamp> {
        public DateToTimestamp() {
            super(Date.class, java.sql.Timestamp.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return (java.util.Date.class.equals(sourceClass) || java.sql.Timestamp.class.equals(sourceClass)) && java.sql.Timestamp.class.equals(targetClass);
        }

        /**
         * Returns <code>obj</code> converted to a <code>java.sql.Timestamp</code>.
         */
        public java.sql.Timestamp convert(Date obj) throws ConversionException {
            return new java.sql.Timestamp(obj.getTime());
        }
    }

    /**
     * An object that converts a <code>java.util.Date</code> (and its subclasses) to a
     * <code>Long</code>.
     */
    public static class GenericDateToLong<S extends Date> extends AbstractConverter<S, Long> {
        public GenericDateToLong(Class<S> source) {
            super(source, Long.class);
        }

        /**
         * Returns the millisecond value of <code>obj</code>.
         */
        public Long convert(S obj) throws ConversionException {
            return obj.getTime();
        }
    }

    public static abstract class GenericLocalizedConverter<S, T> extends AbstractLocalizedConverter<S, T> {
        protected GenericLocalizedConverter(Class<S> sourceClass, Class<T> targetClass) {
            super(sourceClass, targetClass);
        }

        public T convert(S obj) throws ConversionException {
            return convert(obj, Locale.getDefault(), TimeZone.getDefault(), null);
        }

        public T convert(S obj, Locale locale, TimeZone timeZone) throws ConversionException {
            return convert(obj, locale, timeZone, null);
        }
    }

    /**
     * An object that converts a <code>Long</code> to a
     * <code>Calendar</code>.
     */
    public static class LongToCalendar extends AbstractLocalizedConverter<Long, Calendar> {
        public LongToCalendar() {
            super(Long.class, Calendar.class);
        }

        /**
         * Returns <code>obj</code> converted to a <code>Calendar</code>,
         * initialized with the default locale and time zone.
         */
        public Calendar convert(Long obj) throws ConversionException {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(obj);
            return cal;
        }

        /**
         * Returns <code>obj</code> converted to a <code>Calendar</code>,
         * initialized with the specified locale and time zone. The
         * <code>formatString</code> parameter is ignored.
         */
        public Calendar convert(Long obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            Calendar cal = Calendar.getInstance(timeZone, locale);
            cal.setTimeInMillis(obj);
            return cal;
        }
    }

    /**
     * An object that converts a <code>Long</code> to a
     * <code>java.util.Date</code>.
     */
    public static class LongToDate extends AbstractConverter<Long, Date> {
        public LongToDate() {
            super(Long.class, Date.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return Long.class.equals(sourceClass) && Date.class.equals(targetClass);
        }

        /**
         * Returns <code>obj</code> converted to a <code>java.util.Date</code>.
         */
        public Date convert(Long obj) throws ConversionException {
            return new Date(obj.longValue());
        }
    }

    /**
     * An object that converts a <code>Long</code> to a
     * <code>java.sql.Date</code>.
     */
    public static class LongToSqlDate extends AbstractConverter<Long, java.sql.Date> {
        public LongToSqlDate() {
            super(Long.class, java.sql.Date.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return Long.class.equals(sourceClass) && java.sql.Date.class.equals(targetClass);
        }

        /**
         * Returns <code>obj</code> converted to a <code>java.sql.Date</code>.
         */
        public java.sql.Date convert(Long obj) throws ConversionException {
            return new java.sql.Date(obj.longValue());
        }
    }

    /**
     * An object that converts a <code>Long</code> to a
     * <code>java.sql.Time</code>.
     */
    public static class LongToSqlTime extends AbstractConverter<Long, java.sql.Time> {
        public LongToSqlTime() {
            super(Long.class, java.sql.Time.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return Long.class.equals(sourceClass) && java.sql.Time.class.equals(targetClass);
        }

        /**
         * Returns <code>obj</code> converted to a <code>java.sql.Time</code>.
         */
        public java.sql.Time convert(Long obj) throws ConversionException {
            return new java.sql.Time(obj.longValue());
        }
    }

    /**
     * An object that converts a <code>Long</code> to a
     * <code>java.sql.Timestamp</code>.
     */
    public static class LongToTimestamp extends AbstractConverter<Long, java.sql.Timestamp> {
        public LongToTimestamp() {
            super(Long.class, java.sql.Timestamp.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return Long.class.equals(sourceClass) && java.sql.Timestamp.class.equals(targetClass);
        }

        /**
         * Returns <code>obj</code> converted to a <code>java.sql.Timestamp</code>.
         */
        public java.sql.Timestamp convert(Long obj) throws ConversionException {
            return new java.sql.Timestamp(obj.longValue());
        }
    }

    /**
     * An object that converts a <code>java.sql.Date</code> to a
     * <code>java.util.Date</code>.
     */
    public static class SqlDateToDate extends AbstractConverter<java.sql.Date, Date> {
        public SqlDateToDate() {
            super(java.sql.Date.class, Date.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return java.sql.Date.class.equals(sourceClass) && java.util.Date.class.equals(targetClass);
        }

        /**
         * Returns <code>obj</code> converted to a <code>java.util.Date</code>.
         */
        public Date convert(java.sql.Date obj) throws ConversionException {
            return new Date(obj.getTime());
        }
    }

    /**
     * An object that converts a <code>java.sql.Date</code> to a
     * <code>String</code>.
     */
    public static class SqlDateToString extends AbstractLocalizedConverter<java.sql.Date, String> {
        public SqlDateToString() {
            super(java.sql.Date.class, String.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return java.sql.Date.class.equals(sourceClass) && String.class.equals(targetClass);
        }

        public String convert(java.sql.Date obj) throws ConversionException {
            return obj.toString();
        }

        /**
         * Converts <code>obj</code> to a <code>String</code> using the supplied
         * time zone. The <code>formatString</code> parameter is
         * ignored. The returned string is formatted as
         * {@link DateTimeConverters#JDBC_DATE_FORMAT}.
         */
        public String convert(java.sql.Date obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            DateFormat df = toDateFormat(timeZone);
            return df.format(obj);
        }
    }

    /**
     * An object that converts a <code>java.sql.Date</code> to a
     * <code>java.sql.Timestamp</code>.
     */
    public static class SqlDateToTimestamp extends AbstractConverter<java.sql.Date, java.sql.Timestamp> {
        public SqlDateToTimestamp() {
            super(java.sql.Date.class, java.sql.Timestamp.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return java.sql.Date.class.equals(sourceClass) && java.sql.Timestamp.class.equals(targetClass);
        }

        /**
         * Returns <code>obj</code> converted to a <code>java.sql.Timestamp</code>.
         */
        public java.sql.Timestamp convert(java.sql.Date obj) throws ConversionException {
            return new java.sql.Timestamp(obj.getTime());
        }
    }

    /**
     * An object that converts a <code>java.sql.Time</code> to a
     * <code>String</code>.
     */
    public static class SqlTimeToString extends AbstractLocalizedConverter<java.sql.Time, String> {
        public SqlTimeToString() {
            super(java.sql.Time.class, String.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return java.sql.Time.class.equals(sourceClass) && String.class.equals(targetClass);
        }

        public String convert(java.sql.Time obj) throws ConversionException {
            return obj.toString();
        }

        /**
         * Converts <code>obj</code> to a <code>String</code> using the supplied
         * time zone. The <code>formatString</code> parameter is
         * ignored. The returned string is formatted as
         * {@link DateTimeConverters#JDBC_TIME_FORMAT}.
         */
        public String convert(java.sql.Time obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            DateFormat df = toTimeFormat(timeZone);
            return df.format(obj);
        }
    }

    /**
     * An object that converts a <code>String</code> to a
     * <code>java.util.Calendar</code>.
     */
    public static class StringToCalendar extends AbstractLocalizedConverter<String, Calendar> {
        public StringToCalendar() {
            super(String.class, Calendar.class);
        }

        /**
         * Converts <code>obj</code> to a <code>java.util.Calendar</code> initialized to
         * the default locale and time zone. The string must be formatted as
         * {@link DateTimeConverters#CALENDAR_FORMAT}.
         */
        public Calendar convert(String obj) throws ConversionException {
            try {
                DateFormat df = new SimpleDateFormat(CALENDAR_FORMAT);
                Calendar cal = Calendar.getInstance();
                cal.setTime(df.parse(obj));
                return cal;
            } catch (ParseException e) {
                throw new ConversionException(e);
            }
        }

        /**
         * Converts <code>obj</code> to a <code>java.util.Calendar</code> initialized to
         * the supplied locale and time zone. If <code>formatString</code> is
         * <code>null</code>, the string is formatted as
         * {@link DateTimeConverters#CALENDAR_FORMAT}.
         */
        public Calendar convert(String obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            DateFormat df = toDateTimeFormat(formatString == null ? CALENDAR_FORMAT : formatString, timeZone, locale);
            try {
                Date date = df.parse(obj);
                Calendar cal = Calendar.getInstance(timeZone, locale);
                cal.setTimeInMillis(date.getTime());
                return cal;
            } catch (ParseException e) {
                throw new ConversionException(e);
            }
        }
    }

    /**
     * An object that converts a <code>String</code> to a
     * <code>java.util.Date</code>.
     */
    public static class StringToDate extends AbstractLocalizedConverter<String, Date> {
        public StringToDate() {
            super(String.class, Date.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return String.class.equals(sourceClass) && Date.class.equals(targetClass);
        }

        /**
         * Converts <code>obj</code> to a <code>java.util.Date</code>.
         * The string must be formatted as
         * {@link DateTimeConverters#CALENDAR_FORMAT}.
         */
        public Date convert(String obj) throws ConversionException {
            try {
                DateFormat df = new SimpleDateFormat(CALENDAR_FORMAT);
                return df.parse(obj);
            } catch (ParseException e) {
                throw new ConversionException(e);
            }
        }

        /**
         * Converts <code>obj</code> to a <code>java.util.Date</code>. If
         * <code>formatString</code> is <code>null</code>, the string is formatted as
         * {@link DateTimeConverters#CALENDAR_FORMAT}.
         */
        public Date convert(String obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            DateFormat df = toDateTimeFormat(formatString == null ? CALENDAR_FORMAT : formatString, timeZone, locale);
            try {
                return df.parse(obj);
            } catch (ParseException e) {
                throw new ConversionException(e);
            }
        }
    }

    /**
     * An object that converts a <code>String</code> to a
     * <code>java.sql.Date</code>.
     */
    public static class StringToSqlDate extends AbstractLocalizedConverter<String, java.sql.Date> {
        public StringToSqlDate() {
            super(String.class, java.sql.Date.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return String.class.equals(sourceClass) && java.sql.Date.class.equals(targetClass);
        }

        public java.sql.Date convert(String obj) throws ConversionException {
            return java.sql.Date.valueOf(obj);
        }

        /**
         * Converts <code>obj</code> to a <code>java.sql.Date</code> using the supplied
         * time zone. The <code>locale</code> and <code>formatString</code> parameters are
         * ignored. The string must be formatted as
         * {@link DateTimeConverters#JDBC_DATE_FORMAT}.
         */
        public java.sql.Date convert(String obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            DateFormat df = toDateFormat(timeZone);
            try {
                return new java.sql.Date(df.parse(obj).getTime());
            } catch (ParseException e) {
                throw new ConversionException(e);
            }
        }
    }

    /**
     * An object that converts a <code>String</code> to a
     * <code>java.sql.Time</code>.
     */
    public static class StringToSqlTime extends AbstractLocalizedConverter<String, java.sql.Time> {
        public StringToSqlTime() {
            super(String.class, java.sql.Time.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return String.class.equals(sourceClass) && java.sql.Time.class.equals(targetClass);
        }

        public java.sql.Time convert(String obj) throws ConversionException {
            return java.sql.Time.valueOf(obj);
        }

        /**
         * Converts <code>obj</code> to a <code>java.sql.Time</code> using the supplied
         * time zone. The <code>locale</code> and <code>formatString</code> parameters are
         * ignored. The string must be formatted as
         * {@link DateTimeConverters#JDBC_TIME_FORMAT}.
         */
        public Time convert(String obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            DateFormat df = toTimeFormat(timeZone);
            try {
                return new java.sql.Time(df.parse(obj).getTime());
            } catch (ParseException e) {
                throw new ConversionException(e);
            }
        }
    }

    /**
     * An object that converts a <code>String</code> to a
     * <code>java.sql.Timestamp</code>.
     */
    public static class StringToTimestamp extends AbstractLocalizedConverter<String, java.sql.Timestamp> {
        public StringToTimestamp() {
            super(String.class, java.sql.Timestamp.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return String.class.equals(sourceClass) && java.sql.Timestamp.class.equals(targetClass);
        }

        public Timestamp convert(String obj) throws ConversionException {
            return java.sql.Timestamp.valueOf(obj);
        }

        /**
         * Converts <code>obj</code> to a <code>java.sql.Timestamp</code>.
         * <p>Note that the string representation is referenced to the <code>timeZone</code>
         * argument, not UTC. The <code>Timestamp</code> that is returned is adjusted to UTC.
         * This behavior is intended to accommodate user-entered timestamps, where users are
         * accustomed to using their own time zone.</p>
         * </p>
         */
        public java.sql.Timestamp convert(String obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            try {
                // The String is referenced to the time zone represented by the timeZone
                // argument, but the parsing code assumes a reference to UTC. So, we need
                // to "adjust" the parsed Timestamp's value.
                Timestamp parsedStamp = Timestamp.valueOf(obj);
                Calendar cal = Calendar.getInstance(timeZone, locale);
                cal.setTime(parsedStamp);
                cal.add(Calendar.MILLISECOND, 0 - timeZone.getOffset(parsedStamp.getTime()));
                Timestamp result = new Timestamp(cal.getTimeInMillis());
                result.setNanos(parsedStamp.getNanos());
                return result;
            } catch (Exception e) {
                throw new ConversionException(e);
            }
        }
    }

    /**
     * An object that converts a <code>String</code> ID to a
     * <code>java.util.TimeZone</code>.
     */
    public static class StringToTimeZone extends AbstractConverter<String, TimeZone> {
        public StringToTimeZone() {
            super(String.class, TimeZone.class);
        }

        public TimeZone convert(String obj) throws ConversionException {
            return TimeZone.getTimeZone(obj);
        }
    }

    /**
     * An object that converts a <code>java.sql.Timestamp</code> to a
     * <code>java.util.Date</code>.
     */
    public static class TimestampToDate extends AbstractConverter<java.sql.Timestamp, Date> {
        public TimestampToDate() {
            super(java.sql.Timestamp.class, Date.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return java.sql.Timestamp.class.equals(sourceClass) && java.util.Date.class.equals(targetClass);
        }

        public Date convert(java.sql.Timestamp obj) throws ConversionException {
            return new Timestamp(obj.getTime());
        }
    }

    /**
     * An object that converts a <code>java.sql.Timestamp</code> to a
     * <code>java.sql.Date</code>.
     */
    public static class TimestampToSqlDate extends AbstractConverter<java.sql.Timestamp, java.sql.Date> {
        public TimestampToSqlDate() {
            super(java.sql.Timestamp.class, java.sql.Date.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return java.sql.Timestamp.class.equals(sourceClass) && java.sql.Date.class.equals(targetClass);
        }

        public java.sql.Date convert(java.sql.Timestamp obj) throws ConversionException {
            return new java.sql.Date(obj.getTime());
        }
    }

    /**
     * An object that converts a <code>java.sql.Timestamp</code> to a
     * <code>java.sql.Time</code>.
     */
    public static class TimestampToSqlTime extends AbstractConverter<java.sql.Timestamp, java.sql.Time> {
        public TimestampToSqlTime() {
            super(java.sql.Timestamp.class, java.sql.Time.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return java.sql.Timestamp.class.equals(sourceClass) && java.sql.Time.class.equals(targetClass);
        }

        public java.sql.Time convert(java.sql.Timestamp obj) throws ConversionException {
            return new java.sql.Time(obj.getTime());
        }
    }

    /**
     * An object that converts a <code>java.sql.Timestamp</code> to a
     * <code>String</code>.
     */
    public static class TimestampToString extends AbstractLocalizedConverter<java.sql.Timestamp, String> {
        public TimestampToString() {
            super(java.sql.Timestamp.class, String.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return java.sql.Timestamp.class.equals(sourceClass) && String.class.equals(targetClass);
        }

        public String convert(java.sql.Timestamp obj) throws ConversionException {
            return obj.toString();
        }

        /**
         * Converts <code>obj</code> to a <code>String</code> using the supplied
         * time zone.
         * <p>Note that the string representation is referenced to the <code>timeZone</code>
         * argument, not UTC. The <code>Timestamp</code> is adjusted to the specified
         * time zone before conversion. This behavior is intended to accommodate user interfaces,
         * where users are accustomed to viewing timestamps in their own time zone.</p>
         * </p>
         */
        public String convert(Timestamp obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            try {
                // The Timestamp is referenced to UTC, but the String result needs to be
                // referenced to the time zone represented by the timeZone argument.
                // So, we need to "adjust" the Timestamp's value before conversion.
                Calendar cal = Calendar.getInstance(timeZone, locale);
                cal.setTime(obj);
                cal.add(Calendar.MILLISECOND, timeZone.getOffset(obj.getTime()));
                Timestamp result = new Timestamp(cal.getTimeInMillis());
                result.setNanos(obj.getNanos());
                return result.toString();
            } catch (Exception e) {
                throw new ConversionException(e);
            }
        }
    }

    /**
     * An object that converts a <code>java.util.TimeZone</code> to a
     * <code>String</code> ID.
     */
    public static class TimeZoneToString extends AbstractConverter<TimeZone, String> {
        public TimeZoneToString() {
            super(TimeZone.class, String.class);
        }

        public String convert(TimeZone obj) throws ConversionException {
            return obj.getID();
        }
    }
}