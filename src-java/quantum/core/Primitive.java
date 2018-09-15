package quantum.core;

public class Primitive {

    public static byte   uncheckedByteCast   (final byte   x) { return        x; }
    public static byte   uncheckedByteCast   (final char   x) { return (byte) x; }
    public static byte   uncheckedByteCast   (final short  x) { return (byte) x; }
    public static byte   uncheckedByteCast   (final int    x) { return (byte) x; }
    public static byte   uncheckedByteCast   (final long   x) { return (byte) x; }
    public static byte   uncheckedByteCast   (final float  x) { return (byte) x; }
    public static byte   uncheckedByteCast   (final double x) { return (byte) x; }

    public static char   uncheckedCharCast   (final byte   x) { return (char) x; }
    public static char   uncheckedCharCast   (final char   x) { return        x; }
    public static char   uncheckedCharCast   (final short  x) { return (char) x; }
    public static char   uncheckedCharCast   (final int    x) { return (char) x; }
    public static char   uncheckedCharCast   (final long   x) { return (char) x; }
    public static char   uncheckedCharCast   (final float  x) { return (char) x; }
    public static char   uncheckedCharCast   (final double x) { return (char) x; }

    public static short  uncheckedShortCast  (final byte   x) { return        x; }
    public static short  uncheckedShortCast  (final char   x) { return (short)x; }
    public static short  uncheckedShortCast  (final short  x) { return        x; }
    public static short  uncheckedShortCast  (final int    x) { return (short)x; }
    public static short  uncheckedShortCast  (final long   x) { return (short)x; }
    public static short  uncheckedShortCast  (final float  x) { return (short)x; }
    public static short  uncheckedShortCast  (final double x) { return (short)x; }

    public static int    uncheckedIntCast    (final byte   x) { return        x; }
    public static int    uncheckedIntCast    (final char   x) { return        x; }
    public static int    uncheckedIntCast    (final short  x) { return        x; }
    public static int    uncheckedIntCast    (final int    x) { return        x; }
    public static int    uncheckedIntCast    (final long   x) { return (int)  x; }
    public static int    uncheckedIntCast    (final float  x) { return (int)  x; }
    public static int    uncheckedIntCast    (final double x) { return (int)  x; }

    public static long   uncheckedLongCast   (final byte   x) { return        x; }
    public static long   uncheckedLongCast   (final char   x) { return        x; }
    public static long   uncheckedLongCast   (final short  x) { return        x; }
    public static long   uncheckedLongCast   (final int    x) { return        x; }
    public static long   uncheckedLongCast   (final long   x) { return        x; }
    public static long   uncheckedLongCast   (final float  x) { return (long) x; }
    public static long   uncheckedLongCast   (final double x) { return (long) x; }

    public static float  uncheckedFloatCast  (final byte   x) { return        x; }
    public static float  uncheckedFloatCast  (final char   x) { return        x; }
    public static float  uncheckedFloatCast  (final short  x) { return        x; }
    public static float  uncheckedFloatCast  (final int    x) { return        x; }
    public static float  uncheckedFloatCast  (final long   x) { return        x; }
    public static float  uncheckedFloatCast  (final float  x) { return        x; }
    public static float  uncheckedFloatCast  (final double x) { return (float)x; }

    public static double uncheckedDoubleCast (final byte   x) { return        x; }
    public static double uncheckedDoubleCast (final char   x) { return        x; }
    public static double uncheckedDoubleCast (final short  x) { return        x; }
    public static double uncheckedDoubleCast (final int    x) { return        x; }
    public static double uncheckedDoubleCast (final long   x) { return        x; }
    public static double uncheckedDoubleCast (final float  x) { return        x; }
    public static double uncheckedDoubleCast (final double x) { return        x; }

}
