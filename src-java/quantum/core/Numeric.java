package quantum.core;
// Parts of `ztellman/primitive-math` were adapted for this

// Note from java.lang.Math:
// Code generators are encouraged to use platform-specific native libraries or
// microprocessor instructions, where available, to provide higher-performance
// implementations of Math methods. Such higher-performance implementations still
// must conform to the specification for Math.

public class Numeric {

    public static final byte  byte0  = (byte) 0;
    public static final short short0 = (short)0;
    public static final char  char0  = (char) 0;
    public static final int   int0   = 0;
    public static final byte  byte1  = (byte) 1;
    public static final short short1 = (short)1;
    public static final char  char1  = (char) 1;
    public static final int   int1   = 1;

    // ================================= Boolean Operations ===================================== //

    public static boolean isTrue  (final boolean a                 ) { return a == true; }
    public static boolean isFalse (final boolean a                 ) { return a == false; }
    public static boolean isNil   (final Object  a                 ) { return a == null; }
    public static boolean not     (final boolean a                 ) { return !a; }
    public static boolean and     (final boolean a, final boolean b) { return a && b; }
    public static boolean or      (final boolean a, final boolean b) { return a || b; }

    // =================================== Bit Operations ======================================= //

    // ---------------------------- bitNot : ! (implicitly checked) ---------------------------- //

    public static boolean bitNot (final boolean x) { return        !x; }
    public static byte    bitNot (final byte    x) { return (byte) ~x; }
    public static short   bitNot (final short   x) { return (short)~x; }
    public static char    bitNot (final char    x) { return (char) ~x; }
    public static int     bitNot (final int     x) { return        ~x; }
    public static long    bitNot (final long    x) { return        ~x; }
    public static float   bitNot (final float   x) {
      return Float.intBitsToFloat(~Float.floatToIntBits(x));
    }
    public static double  bitNot (final double  x) {
      return Double.longBitsToDouble(~Double.doubleToLongBits(x));
    }

    // ---------------------------- bitAnd : & (implicitly checked) ---------------------------- //
    // Returns the smallest safe type; decimals are "infectious"

    public static boolean bitAnd (final boolean a, final boolean b) { return         a & b ; }
    public static byte    bitAnd (final byte    a, final byte    b) { return (byte) (a & b); }
    public static short   bitAnd (final byte    a, final short   b) { return (short)(a & b); }
    public static int     bitAnd (final byte    a, final char    b) { return         a & b ; }
    public static int     bitAnd (final byte    a, final int     b) { return         a & b ; }
    public static long    bitAnd (final byte    a, final long    b) { return         a & b ; }
    public static float   bitAnd (final byte    a, final float   b) {
      return Float.intBitsToFloat(a & Float.floatToIntBits(b));
    }
    public static double  bitAnd (final byte    a, final double  b) {
      return Double.longBitsToDouble(a & Double.doubleToLongBits(b));
    }
    public static short   bitAnd (final short   a, final byte    b) { return (short)(a & b); }
    public static short   bitAnd (final short   a, final short   b) { return (short)(a & b); }
    public static int     bitAnd (final short   a, final char    b) { return         a & b ; }
    public static int     bitAnd (final short   a, final int     b) { return         a & b ; }
    public static long    bitAnd (final short   a, final long    b) { return         a & b ; }
    public static float   bitAnd (final short   a, final float   b) {
      return Float.intBitsToFloat(a & Float.floatToIntBits(b));
    }
    public static double  bitAnd (final short   a, final double  b) {
      return Double.longBitsToDouble(a & Double.doubleToLongBits(b));
    }
    public static int     bitAnd (final char    a, final byte    b) { return         a & b ; }
    public static int     bitAnd (final char    a, final short   b) { return         a & b ; }
    public static char    bitAnd (final char    a, final char    b) { return (char) (a & b); }
    public static int     bitAnd (final char    a, final int     b) { return         a & b ; }
    public static long    bitAnd (final char    a, final long    b) { return         a & b ; }
    public static float   bitAnd (final char    a, final float   b) {
      return Float.intBitsToFloat(a & Float.floatToIntBits(b));
    }
    public static double  bitAnd (final char    a, final double  b) {
      return Double.longBitsToDouble(a & Double.doubleToLongBits(b));
    }
    public static int     bitAnd (final int     a, final byte    b) { return         a & b ; }
    public static int     bitAnd (final int     a, final short   b) { return         a & b ; }
    public static int     bitAnd (final int     a, final char    b) { return         a & b ; }
    public static int     bitAnd (final int     a, final int     b) { return         a & b ; }
    public static long    bitAnd (final int     a, final long    b) { return         a & b ; }
    public static float   bitAnd (final int     a, final float   b) {
      return Float.intBitsToFloat(a & Float.floatToIntBits(b));
    }
    public static double  bitAnd (final int     a, final double  b) {
      return Double.longBitsToDouble(a & Double.doubleToLongBits(b));
    }
    public static long    bitAnd (final long    a, final byte    b) { return         a & b ; }
    public static long    bitAnd (final long    a, final short   b) { return         a & b ; }
    public static long    bitAnd (final long    a, final char    b) { return         a & b ; }
    public static long    bitAnd (final long    a, final int     b) { return         a & b ; }
    public static long    bitAnd (final long    a, final long    b) { return         a & b ; }
    public static double  bitAnd (final long    a, final float   b) {
      return Double.longBitsToDouble(a & Float.floatToIntBits(b));
    }
    public static double  bitAnd (final long    a, final double  b) {
      return Double.longBitsToDouble(a & Double.doubleToLongBits(b));
    }
    public static float   bitAnd (final float   a, final byte    b) {
      return Float.intBitsToFloat(Float.floatToIntBits(a) & b);
    }
    public static float   bitAnd (final float   a, final short   b) {
      return Float.intBitsToFloat(Float.floatToIntBits(a) & b);
    }
    public static float   bitAnd (final float   a, final char    b) {
      return Float.intBitsToFloat(Float.floatToIntBits(a) & b);
    }
    public static float   bitAnd (final float   a, final int     b) {
      return Float.intBitsToFloat(Float.floatToIntBits(a) & b);
    }
    public static double  bitAnd (final float   a, final long    b) {
      return Double.longBitsToDouble(Float.floatToIntBits(a) & b);
    }
    public static double  bitAnd (final float   a, final float   b) {
      return Float.intBitsToFloat(Float.floatToIntBits(a) & Float.floatToIntBits(b));
    }
    public static double  bitAnd (final float   a, final double  b) {
      return Double.longBitsToDouble(Float.floatToIntBits(a) & Double.doubleToLongBits(b));
    }
    public static double  bitAnd (final double  a, final byte    b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) & b);
    }
    public static double  bitAnd (final double  a, final short   b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) & b);
    }
    public static double  bitAnd (final double  a, final char    b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) & b);
    }
    public static double  bitAnd (final double  a, final int     b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) & b);
    }
    public static double  bitAnd (final double  a, final long    b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) & b);
    }
    public static double  bitAnd (final double  a, final float   b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) & Float.floatToIntBits(b));
    }
    public static double  bitAnd (final double  a, final double  b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) & Double.doubleToLongBits(b));
    }

    // ----------------------------- bitOr : | (implicitly checked) ----------------------------- //
    // Returns the smallest safe type; decimals are "infectious"

    public static boolean bitOr (final boolean a, final boolean b) { return         a | b ; }
    public static byte    bitOr (final byte    a, final byte    b) { return (byte) (a | b); }
    public static short   bitOr (final byte    a, final short   b) { return (short)(a | b); }
    public static int     bitOr (final byte    a, final char    b) { return         a | b ; }
    public static int     bitOr (final byte    a, final int     b) { return         a | b ; }
    public static long    bitOr (final byte    a, final long    b) { return         a | b ; }
    public static float   bitOr (final byte    a, final float   b) {
      return Float.intBitsToFloat(a | Float.floatToIntBits(b));
    }
    public static double  bitOr (final byte    a, final double  b) {
      return Double.longBitsToDouble(a | Double.doubleToLongBits(b));
    }
    public static short   bitOr (final short   a, final byte    b) { return (short)(a | b); }
    public static short   bitOr (final short   a, final short   b) { return (short)(a | b); }
    public static int     bitOr (final short   a, final char    b) { return         a | b ; }
    public static int     bitOr (final short   a, final int     b) { return         a | b ; }
    public static long    bitOr (final short   a, final long    b) { return         a | b ; }
    public static float   bitOr (final short   a, final float   b) {
      return Float.intBitsToFloat(a | Float.floatToIntBits(b));
    }
    public static double  bitOr (final short   a, final double  b) {
      return Double.longBitsToDouble(a | Double.doubleToLongBits(b));
    }
    public static int     bitOr (final char    a, final byte    b) { return         a | b ; }
    public static int     bitOr (final char    a, final short   b) { return         a | b ; }
    public static char    bitOr (final char    a, final char    b) { return (char) (a | b); }
    public static int     bitOr (final char    a, final int     b) { return         a | b ; }
    public static long    bitOr (final char    a, final long    b) { return         a | b ; }
    public static float   bitOr (final char    a, final float   b) {
      return Float.intBitsToFloat(a | Float.floatToIntBits(b));
    }
    public static double  bitOr (final char    a, final double  b) {
      return Double.longBitsToDouble(a | Double.doubleToLongBits(b));
    }
    public static int     bitOr (final int     a, final byte    b) { return         a | b ; }
    public static int     bitOr (final int     a, final short   b) { return         a | b ; }
    public static int     bitOr (final int     a, final char    b) { return         a | b ; }
    public static int     bitOr (final int     a, final int     b) { return         a | b ; }
    public static long    bitOr (final int     a, final long    b) { return         a | b ; }
    public static float   bitOr (final int     a, final float   b) {
      return Float.intBitsToFloat(a | Float.floatToIntBits(b));
    }
    public static double  bitOr (final int     a, final double  b) {
      return Double.longBitsToDouble(a | Double.doubleToLongBits(b));
    }
    public static long    bitOr (final long    a, final byte    b) { return         a | b ; }
    public static long    bitOr (final long    a, final char    b) { return         a | b ; }
    public static long    bitOr (final long    a, final short   b) { return         a | b ; }
    public static long    bitOr (final long    a, final int     b) { return         a | b ; }
    public static long    bitOr (final long    a, final long    b) { return         a | b ; }
    public static double  bitOr (final long    a, final float   b) {
      return Double.longBitsToDouble(a | Float.floatToIntBits(b));
    }
    public static double  bitOr (final long    a, final double  b) {
      return Double.longBitsToDouble(a | Double.doubleToLongBits(b));
    }
    public static float   bitOr (final float   a, final byte    b) {
      return Float.intBitsToFloat(Float.floatToIntBits(a) | b);
    }
    public static float   bitOr (final float   a, final short   b) {
      return Float.intBitsToFloat(Float.floatToIntBits(a) | b);
    }
    public static float   bitOr (final float   a, final char    b) {
      return Float.intBitsToFloat(Float.floatToIntBits(a) | b);
    }
    public static float   bitOr (final float   a, final int     b) {
      return Float.intBitsToFloat(Float.floatToIntBits(a) | b);
    }
    public static double  bitOr (final float   a, final long    b) {
      return Double.longBitsToDouble(Float.floatToIntBits(a) | b);
    }
    public static double  bitOr (final float   a, final float   b) {
      return Float.intBitsToFloat(Float.floatToIntBits(a) | Float.floatToIntBits(b));
    }
    public static double  bitOr (final float   a, final double  b) {
      return Double.longBitsToDouble(Float.floatToIntBits(a) | Double.doubleToLongBits(b));
    }
    public static double  bitOr (final double  a, final byte    b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) | b);
    }
    public static double  bitOr (final double  a, final short   b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) | b);
    }
    public static double  bitOr (final double  a, final char    b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) | b);
    }
    public static double  bitOr (final double  a, final int     b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) | b);
    }
    public static double  bitOr (final double  a, final long    b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) | b);
    }
    public static double  bitOr (final double  a, final float   b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) | Float.floatToIntBits(b));
    }
    public static double  bitOr (final double  a, final double  b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) | Double.doubleToLongBits(b));
    }

    // ------------------------------ bitXOr (implicitly checked) ------------------------------ //
    // Returns the smallest safe type; decimals are "infectious"

    public static boolean bitXOr (final boolean a, final boolean b) { return         a ^ b ; }
    public static byte    bitXOr (final byte    a, final byte    b) { return (byte) (a ^ b); }
    public static short   bitXOr (final byte    a, final short   b) { return (short)(a ^ b); }
    public static int     bitXOr (final byte    a, final char    b) { return         a ^ b ; }
    public static int     bitXOr (final byte    a, final int     b) { return         a ^ b ; }
    public static long    bitXOr (final byte    a, final long    b) { return         a ^ b ; }
    public static float   bitXOr (final byte    a, final float   b) {
      return Float.intBitsToFloat(a ^ Float.floatToIntBits(b));
    }
    public static double  bitXOr (final byte    a, final double  b) {
      return Double.longBitsToDouble(a ^ Double.doubleToLongBits(b));
    }
    public static short   bitXOr (final short   a, final byte    b) { return (short)(a ^ b); }
    public static short   bitXOr (final short   a, final short   b) { return (short)(a ^ b); }
    public static int     bitXOr (final short   a, final char    b) { return         a ^ b ; }
    public static int     bitXOr (final short   a, final int     b) { return         a ^ b ; }
    public static long    bitXOr (final short   a, final long    b) { return         a ^ b ; }
    public static float   bitXOr (final short   a, final float   b) {
      return Float.intBitsToFloat(a ^ Float.floatToIntBits(b));
    }
    public static double  bitXOr (final short   a, final double  b) {
      return Double.longBitsToDouble(a ^ Double.doubleToLongBits(b));
    }
    public static int     bitXOr (final char    a, final byte    b) { return         a ^ b ; }
    public static int     bitXOr (final char    a, final short   b) { return         a ^ b ; }
    public static char    bitXOr (final char    a, final char    b) { return (char) (a ^ b); }
    public static int     bitXOr (final char    a, final int     b) { return         a ^ b ; }
    public static long    bitXOr (final char    a, final long    b) { return         a ^ b ; }
    public static float   bitXOr (final char    a, final float   b) {
      return Float.intBitsToFloat(a ^ Float.floatToIntBits(b));
    }
    public static double  bitXOr (final char    a, final double  b) {
      return Double.longBitsToDouble(a ^ Double.doubleToLongBits(b));
    }
    public static int     bitXOr (final int     a, final byte    b) { return         a ^ b ; }
    public static int     bitXOr (final int     a, final short   b) { return         a ^ b ; }
    public static int     bitXOr (final int     a, final char    b) { return         a ^ b ; }
    public static int     bitXOr (final int     a, final int     b) { return         a ^ b ; }
    public static long    bitXOr (final int     a, final long    b) { return         a ^ b ; }
    public static float   bitXOr (final int     a, final float   b) {
      return Float.intBitsToFloat(a ^ Float.floatToIntBits(b));
    }
    public static double  bitXOr (final int     a, final double  b) {
      return Double.longBitsToDouble(a ^ Double.doubleToLongBits(b));
    }
    public static long    bitXOr (final long    a, final byte    b) { return         a ^ b ; }
    public static long    bitXOr (final long    a, final char    b) { return         a ^ b ; }
    public static long    bitXOr (final long    a, final short   b) { return         a ^ b ; }
    public static long    bitXOr (final long    a, final int     b) { return         a ^ b ; }
    public static long    bitXOr (final long    a, final long    b) { return         a ^ b ; }
    public static double  bitXOr (final long    a, final float   b) {
      return Double.longBitsToDouble(a ^ Float.floatToIntBits(b));
    }
    public static double  bitXOr (final long    a, final double  b) {
      return Double.longBitsToDouble(a ^ Double.doubleToLongBits(b));
    }
    public static float   bitXOr (final float   a, final byte    b) {
      return Float.intBitsToFloat(Float.floatToIntBits(a) ^ b);
    }
    public static float   bitXOr (final float   a, final short   b) {
      return Float.intBitsToFloat(Float.floatToIntBits(a) ^ b);
    }
    public static float   bitXOr (final float   a, final char    b) {
      return Float.intBitsToFloat(Float.floatToIntBits(a) ^ b);
    }
    public static float   bitXOr (final float   a, final int     b) {
      return Float.intBitsToFloat(Float.floatToIntBits(a) ^ b);
    }
    public static double  bitXOr (final float   a, final long    b) {
      return Double.longBitsToDouble(Float.floatToIntBits(a) ^ b);
    }
    public static double  bitXOr (final float   a, final float   b) {
      return Float.intBitsToFloat(Float.floatToIntBits(a) ^ Float.floatToIntBits(b));
    }
    public static double  bitXOr (final float   a, final double  b) {
      return Double.longBitsToDouble(Float.floatToIntBits(a) ^ Double.doubleToLongBits(b));
    }
    public static double  bitXOr (final double  a, final byte    b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) ^ b);
    }
    public static double  bitXOr (final double  a, final short   b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) ^ b);
    }
    public static double  bitXOr (final double  a, final char    b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) ^ b);
    }
    public static double  bitXOr (final double  a, final int     b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) ^ b);
    }
    public static double  bitXOr (final double  a, final long    b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) ^ b);
    }
    public static double  bitXOr (final double  a, final float   b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) ^ Float.floatToIntBits(b));
    }
    public static double  bitXOr (final double  a, final double  b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) ^ Double.doubleToLongBits(b));
    }

    // -------------------------- shiftLeft : << (implicitly checked) -------------------------- //
    // Returns the smallest safe type; decimals are "infectious"

    // Though technically `1 << 1` = 2, not 1
    public static boolean shiftLeft (final boolean a, final boolean b) { return a; }
    public static byte    shiftLeft (final byte    a, final byte    b) { return (byte) (a << b); }
    public static short   shiftLeft (final byte    a, final short   b) { return (short)(a << b); }
    public static int     shiftLeft (final byte    a, final char    b) { return         a << b ; }
    public static int     shiftLeft (final byte    a, final int     b) { return         a << b ; }
    public static long    shiftLeft (final byte    a, final long    b) { return         a << b ; }
    public static float   shiftLeft (final byte    a, final float   b) {
      return Float.intBitsToFloat(a << Float.floatToIntBits(b));
    }
    public static double  shiftLeft (final byte    a, final double  b) {
      return Double.longBitsToDouble(a << Double.doubleToLongBits(b));
    }
    public static short   shiftLeft (final short   a, final byte    b) { return (short)(a << b); }
    public static short   shiftLeft (final short   a, final short   b) { return (short)(a << b); }
    public static int     shiftLeft (final short   a, final char    b) { return         a << b ; }
    public static int     shiftLeft (final short   a, final int     b) { return         a << b ; }
    public static long    shiftLeft (final short   a, final long    b) { return         a << b ; }
    public static float   shiftLeft (final short   a, final float   b) {
      return Float.intBitsToFloat(a << Float.floatToIntBits(b));
    }
    public static double  shiftLeft (final short   a, final double  b) {
      return Double.longBitsToDouble(a << Double.doubleToLongBits(b));
    }
    public static int     shiftLeft (final char    a, final byte    b) { return         a << b ; }
    public static int     shiftLeft (final char    a, final short   b) { return         a << b ; }
    public static char    shiftLeft (final char    a, final char    b) { return (char) (a << b); }
    public static int     shiftLeft (final char    a, final int     b) { return         a << b ; }
    public static long    shiftLeft (final char    a, final long    b) { return         a << b ; }
    public static float   shiftLeft (final char    a, final float   b) {
      return Float.intBitsToFloat(a << Float.floatToIntBits(b));
    }
    public static double  shiftLeft (final char    a, final double  b) {
      return Double.longBitsToDouble(a << Double.doubleToLongBits(b));
    }
    public static int     shiftLeft (final int     a, final byte    b) { return         a << b ; }
    public static int     shiftLeft (final int     a, final short   b) { return         a << b ; }
    public static int     shiftLeft (final int     a, final char    b) { return         a << b ; }
    public static int     shiftLeft (final int     a, final int     b) { return         a << b ; }
    public static long    shiftLeft (final int     a, final long    b) { return         a << b ; }
    public static float   shiftLeft (final int     a, final float   b) {
      return Float.intBitsToFloat(a << Float.floatToIntBits(b));
    }
    public static double  shiftLeft (final int     a, final double  b) {
      return Double.longBitsToDouble(a << Double.doubleToLongBits(b));
    }
    public static long    shiftLeft (final long    a, final byte    b) { return         a << b ; }
    public static long    shiftLeft (final long    a, final char    b) { return         a << b ; }
    public static long    shiftLeft (final long    a, final short   b) { return         a << b ; }
    public static long    shiftLeft (final long    a, final int     b) { return         a << b ; }
    public static long    shiftLeft (final long    a, final long    b) { return         a << b ; }
    public static double  shiftLeft (final long    a, final float   b) {
      return Double.longBitsToDouble(a << Float.floatToIntBits(b));
    }
    public static double  shiftLeft (final long    a, final double  b) {
      return Double.longBitsToDouble(a << Double.doubleToLongBits(b));
    }
    public static float   shiftLeft (final float   a, final byte    b) {
      return Float.intBitsToFloat(Float.floatToIntBits(a) << b);
    }
    public static float   shiftLeft (final float   a, final short   b) {
      return Float.intBitsToFloat(Float.floatToIntBits(a) << b);
    }
    public static float   shiftLeft (final float   a, final char    b) {
      return Float.intBitsToFloat(Float.floatToIntBits(a) << b);
    }
    public static float   shiftLeft (final float   a, final int     b) {
      return Float.intBitsToFloat(Float.floatToIntBits(a) << b);
    }
    public static double  shiftLeft (final float   a, final long    b) {
      return Double.longBitsToDouble(Float.floatToIntBits(a) << b);
    }
    public static double  shiftLeft (final float   a, final float   b) {
      return Float.intBitsToFloat(Float.floatToIntBits(a) << Float.floatToIntBits(b));
    }
    public static double  shiftLeft (final float   a, final double  b) {
      return Double.longBitsToDouble(Float.floatToIntBits(a) << Double.doubleToLongBits(b));
    }
    public static double  shiftLeft (final double  a, final byte    b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) << b);
    }
    public static double  shiftLeft (final double  a, final short   b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) << b);
    }
    public static double  shiftLeft (final double  a, final char    b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) << b);
    }
    public static double  shiftLeft (final double  a, final int     b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) << b);
    }
    public static double  shiftLeft (final double  a, final long    b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) << b);
    }
    public static double  shiftLeft (final double  a, final float   b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) << Float.floatToIntBits(b));
    }
    public static double  shiftLeft (final double  a, final double  b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) << Double.doubleToLongBits(b));
    }

    // -------------------------- shiftRight : >> (implicitly checked) -------------------------- //
    // Returns the smallest safe type; decimals are "infectious"

    public static boolean shiftRight (final boolean a, final boolean b) { return a && !b; }
    public static byte    shiftRight (final byte    a, final byte    b) { return (byte) (a >> b); }
    public static short   shiftRight (final byte    a, final short   b) { return (short)(a >> b); }
    public static int     shiftRight (final byte    a, final char    b) { return         a >> b ; }
    public static int     shiftRight (final byte    a, final int     b) { return         a >> b ; }
    public static long    shiftRight (final byte    a, final long    b) { return         a >> b ; }
    public static float   shiftRight (final byte    a, final float   b) {
      return Float.intBitsToFloat(a >> Float.floatToIntBits(b));
    }
    public static double  shiftRight (final byte    a, final double  b) {
      return Double.longBitsToDouble(a >> Double.doubleToLongBits(b));
    }
    public static short   shiftRight (final short   a, final byte    b) { return (short)(a >> b); }
    public static short   shiftRight (final short   a, final short   b) { return (short)(a >> b); }
    public static int     shiftRight (final short   a, final char    b) { return         a >> b ; }
    public static int     shiftRight (final short   a, final int     b) { return         a >> b ; }
    public static long    shiftRight (final short   a, final long    b) { return         a >> b ; }
    public static float   shiftRight (final short   a, final float   b) {
      return Float.intBitsToFloat(a >> Float.floatToIntBits(b));
    }
    public static double  shiftRight (final short   a, final double  b) {
      return Double.longBitsToDouble(a >> Double.doubleToLongBits(b));
    }
    public static int     shiftRight (final char    a, final byte    b) { return         a >> b ; }
    public static int     shiftRight (final char    a, final short   b) { return         a >> b ; }
    public static char    shiftRight (final char    a, final char    b) { return (char) (a >> b); }
    public static int     shiftRight (final char    a, final int     b) { return         a >> b ; }
    public static long    shiftRight (final char    a, final long    b) { return         a >> b ; }
    public static float   shiftRight (final char    a, final float   b) {
      return Float.intBitsToFloat(a >> Float.floatToIntBits(b));
    }
    public static double  shiftRight (final char    a, final double  b) {
      return Double.longBitsToDouble(a >> Double.doubleToLongBits(b));
    }
    public static int     shiftRight (final int     a, final byte    b) { return         a >> b ; }
    public static int     shiftRight (final int     a, final short   b) { return         a >> b ; }
    public static int     shiftRight (final int     a, final char    b) { return         a >> b ; }
    public static int     shiftRight (final int     a, final int     b) { return         a >> b ; }
    public static long    shiftRight (final int     a, final long    b) { return         a >> b ; }
    public static float   shiftRight (final int     a, final float   b) {
      return Float.intBitsToFloat(a >> Float.floatToIntBits(b));
    }
    public static double  shiftRight (final int     a, final double  b) {
      return Double.longBitsToDouble(a >> Double.doubleToLongBits(b));
    }
    public static long    shiftRight (final long    a, final byte    b) { return         a >> b ; }
    public static long    shiftRight (final long    a, final char    b) { return         a >> b ; }
    public static long    shiftRight (final long    a, final short   b) { return         a >> b ; }
    public static long    shiftRight (final long    a, final int     b) { return         a >> b ; }
    public static long    shiftRight (final long    a, final long    b) { return         a >> b ; }
    public static double  shiftRight (final long    a, final float   b) {
      return Double.longBitsToDouble(a >> Float.floatToIntBits(b));
    }
    public static double  shiftRight (final long    a, final double  b) {
      return Double.longBitsToDouble(a >> Double.doubleToLongBits(b));
    }
    public static float   shiftRight (final float   a, final byte    b) {
      return Float.intBitsToFloat(Float.floatToIntBits(a) >> b);
    }
    public static float   shiftRight (final float   a, final short   b) {
      return Float.intBitsToFloat(Float.floatToIntBits(a) >> b);
    }
    public static float   shiftRight (final float   a, final char    b) {
      return Float.intBitsToFloat(Float.floatToIntBits(a) >> b);
    }
    public static float   shiftRight (final float   a, final int     b) {
      return Float.intBitsToFloat(Float.floatToIntBits(a) >> b);
    }
    public static double  shiftRight (final float   a, final long    b) {
      return Double.longBitsToDouble(Float.floatToIntBits(a) >> b);
    }
    public static double  shiftRight (final float   a, final float   b) {
      return Float.intBitsToFloat(Float.floatToIntBits(a) >> Float.floatToIntBits(b));
    }
    public static double  shiftRight (final float   a, final double  b) {
      return Double.longBitsToDouble(Float.floatToIntBits(a) >> Double.doubleToLongBits(b));
    }
    public static double  shiftRight (final double  a, final byte    b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) >> b);
    }
    public static double  shiftRight (final double  a, final short   b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) >> b);
    }
    public static double  shiftRight (final double  a, final char    b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) >> b);
    }
    public static double  shiftRight (final double  a, final int     b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) >> b);
    }
    public static double  shiftRight (final double  a, final long    b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) >> b);
    }
    public static double  shiftRight (final double  a, final float   b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) >> Float.floatToIntBits(b));
    }
    public static double  shiftRight (final double  a, final double  b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) >> Double.doubleToLongBits(b));
    }

    // -------------------------------- unsignedShiftRight : >>> -------------------------------- //
    // Returns the smallest safe type; decimals are "infectious"

    public static boolean uShiftRight (final boolean a, final boolean b) { return a && !b; }
    public static byte    uShiftRight (final byte    a, final byte    b) { return (byte) (a >>> b);}
    public static short   uShiftRight (final byte    a, final short   b) { return (short)(a >>> b);}
    public static int     uShiftRight (final byte    a, final char    b) { return         a >>> b ;}
    public static int     uShiftRight (final byte    a, final int     b) { return         a >>> b ;}
    public static long    uShiftRight (final byte    a, final long    b) { return         a >>> b ;}
    public static float   uShiftRight (final byte    a, final float   b) {
      return Float.intBitsToFloat(a >>> Float.floatToIntBits(b));
    }
    public static double  uShiftRight (final byte    a, final double  b) {
      return Double.longBitsToDouble(a >>> Double.doubleToLongBits(b));
    }
    public static short   uShiftRight (final short   a, final byte    b) { return (short)(a >>> b);}
    public static short   uShiftRight (final short   a, final short   b) { return (short)(a >>> b);}
    public static int     uShiftRight (final short   a, final char    b) { return         a >>> b ;}
    public static int     uShiftRight (final short   a, final int     b) { return         a >>> b ;}
    public static long    uShiftRight (final short   a, final long    b) { return         a >>> b ;}
    public static float   uShiftRight (final short   a, final float   b) {
      return Float.intBitsToFloat(a >>> Float.floatToIntBits(b));
    }
    public static double  uShiftRight (final short   a, final double  b) {
      return Double.longBitsToDouble(a >>> Double.doubleToLongBits(b));
    }
    public static int     uShiftRight (final char    a, final byte    b) { return         a >>> b ;}
    public static int     uShiftRight (final char    a, final short   b) { return         a >>> b ;}
    public static char    uShiftRight (final char    a, final char    b) { return (char) (a >>> b);}
    public static int     uShiftRight (final char    a, final int     b) { return         a >>> b ;}
    public static long    uShiftRight (final char    a, final long    b) { return         a >>> b ;}
    public static float   uShiftRight (final char    a, final float   b) {
      return Float.intBitsToFloat(a >>> Float.floatToIntBits(b));
    }
    public static double  uShiftRight (final char    a, final double  b) {
      return Double.longBitsToDouble(a >>> Double.doubleToLongBits(b));
    }
    public static int     uShiftRight (final int     a, final byte    b) { return         a >>> b ;}
    public static int     uShiftRight (final int     a, final short   b) { return         a >>> b ;}
    public static int     uShiftRight (final int     a, final char    b) { return         a >>> b ;}
    public static int     uShiftRight (final int     a, final int     b) { return         a >>> b ;}
    public static long    uShiftRight (final int     a, final long    b) { return         a >>> b ;}
    public static float   uShiftRight (final int     a, final float   b) {
      return Float.intBitsToFloat(a >>> Float.floatToIntBits(b));
    }
    public static double  uShiftRight (final int     a, final double  b) {
      return Double.longBitsToDouble(a >>> Double.doubleToLongBits(b));
    }
    public static long    uShiftRight (final long    a, final byte    b) { return         a >>> b ;}
    public static long    uShiftRight (final long    a, final char    b) { return         a >>> b ;}
    public static long    uShiftRight (final long    a, final short   b) { return         a >>> b ;}
    public static long    uShiftRight (final long    a, final int     b) { return         a >>> b ;}
    public static long    uShiftRight (final long    a, final long    b) { return         a >>> b ;}
    public static double  uShiftRight (final long    a, final float   b) {
      return Double.longBitsToDouble(a >>> Float.floatToIntBits(b));
    }
    public static double  uShiftRight (final long    a, final double  b) {
      return Double.longBitsToDouble(a >>> Double.doubleToLongBits(b));
    }
    public static float   uShiftRight (final float   a, final byte    b) {
      return Float.intBitsToFloat(Float.floatToIntBits(a) >>> b);
    }
    public static float   uShiftRight (final float   a, final short   b) {
      return Float.intBitsToFloat(Float.floatToIntBits(a) >>> b);
    }
    public static float   uShiftRight (final float   a, final char    b) {
      return Float.intBitsToFloat(Float.floatToIntBits(a) >>> b);
    }
    public static float   uShiftRight (final float   a, final int     b) {
      return Float.intBitsToFloat(Float.floatToIntBits(a) >>> b);
    }
    public static double  uShiftRight (final float   a, final long    b) {
      return Double.longBitsToDouble(Float.floatToIntBits(a) >>> b);
    }
    public static double  uShiftRight (final float   a, final float   b) {
      return Float.intBitsToFloat(Float.floatToIntBits(a) >>> Float.floatToIntBits(b));
    }
    public static double  uShiftRight (final float   a, final double  b) {
      return Double.longBitsToDouble(Float.floatToIntBits(a) >>> Double.doubleToLongBits(b));
    }
    public static double  uShiftRight (final double  a, final byte    b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) >>> b);
    }
    public static double  uShiftRight (final double  a, final short   b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) >>> b);
    }
    public static double  uShiftRight (final double  a, final char    b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) >>> b);
    }
    public static double  uShiftRight (final double  a, final int     b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) >>> b);
    }
    public static double  uShiftRight (final double  a, final long    b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) >>> b);
    }
    public static double  uShiftRight (final double  a, final float   b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) >>> Float.floatToIntBits(b));
    }
    public static double  uShiftRight (final double  a, final double  b) {
      return Double.longBitsToDouble(Double.doubleToLongBits(a) >>> Double.doubleToLongBits(b));
    }

    // ---------------------------------- bitClear (unchecked) ---------------------------------- //

    public static byte   bitClear (final byte   x, final long n) { return (byte) (x & ~(1L << n)); }
    public static short  bitClear (final short  x, final long n) { return (short)(x & ~(1L << n)); }
    public static char   bitClear (final char   x, final long n) { return (char) (x & ~(1L << n)); }
    public static int    bitClear (final int    x, final long n) { return (int)  (x & ~(1L << n)); }
    public static long   bitClear (final long   x, final long n) { return         x & ~(1L << n) ; }
    public static float  bitClear (final float  x, final long n) {
      return Float.intBitsToFloat((int)(Float.floatToIntBits(x) & ~(1L << n)));
    }
    public static double bitClear (final double x, final long n) {
      return Double.longBitsToDouble(Double.doubleToLongBits(x) & ~(1L << n));
    }

    // ---------------------------------- bitFlip (unchecked) ---------------------------------- //

    public static byte   bitFlip (final byte   x, final long n) { return (byte) (x ^ (1L << n)); }
    public static short  bitFlip (final short  x, final long n) { return (short)(x ^ (1L << n)); }
    public static char   bitFlip (final char   x, final long n) { return (char) (x ^ (1L << n)); }
    public static int    bitFlip (final int    x, final long n) { return (int)  (x ^ (1L << n)); }
    public static long   bitFlip (final long   x, final long n) { return         x ^ (1L << n) ; }
    public static float  bitFlip (final float  x, final long n) {
      return Float.intBitsToFloat((int)(Float.floatToIntBits(x) ^ (1L << n)));
    }
    public static double bitFlip (final double x, final long n) {
      return Double.longBitsToDouble(Double.doubleToLongBits(x) ^ (1L << n));
    }

    // ----------------------------------- bitSet (unchecked) ----------------------------------- //
    // Returns the smallest safe type

    public static byte   bitSet (final byte   x, final long n) { return (byte) (x | (1L << n)); }
    public static short  bitSet (final short  x, final long n) { return (short)(x | (1L << n)); }
    public static char   bitSet (final char   x, final long n) { return (char) (x | (1L << n)); }
    public static int    bitSet (final int    x, final long n) { return (int)  (x | (1L << n)); }
    public static long   bitSet (final long   x, final long n) { return         x | (1L << n) ; }
    public static float  bitSet (final float  x, final long n) {
      return Float.intBitsToFloat((int)(Float.floatToIntBits(x) | (1L << n)));
    }
    public static double bitSet (final double x, final long n) {
      return Double.longBitsToDouble(Double.doubleToLongBits(x) | (1L << n));
    }

    // ---------------------------------- bitTest (unchecked) ---------------------------------- //

    public static boolean bitTest (final byte   x, final long n) { return (x & (1L << n)) != 0L; }
    public static boolean bitTest (final short  x, final long n) { return (x & (1L << n)) != 0L; }
    public static boolean bitTest (final char   x, final long n) { return (x & (1L << n)) != 0L; }
    public static boolean bitTest (final int    x, final long n) { return (x & (1L << n)) != 0L; }
    public static boolean bitTest (final long   x, final long n) { return (x & (1L << n)) != 0L; }
    public static boolean bitTest (final float  x, final long n) {
      return (Float.floatToIntBits(x) & (1L << n)) != 0L;
    }
    public static boolean bitTest (final double x, final long n) {
      return (Double.doubleToLongBits(x) & (1L << n)) != 0L;
    }

    // ======================================= lt : < =========================================== //

    public static boolean lt (final byte   a, final byte   b) { return a < b; }
    public static boolean lt (final byte   a, final short  b) { return a < b; }
    public static boolean lt (final byte   a, final char   b) { return a < b; }
    public static boolean lt (final byte   a, final int    b) { return a < b; }
    public static boolean lt (final byte   a, final long   b) { return a < b; }
    public static boolean lt (final byte   a, final float  b) { return a < b; }
    public static boolean lt (final byte   a, final double b) { return a < b; }
    public static boolean lt (final short  a, final byte   b) { return a < b; }
    public static boolean lt (final short  a, final short  b) { return a < b; }
    public static boolean lt (final short  a, final char   b) { return a < b; }
    public static boolean lt (final short  a, final int    b) { return a < b; }
    public static boolean lt (final short  a, final long   b) { return a < b; }
    public static boolean lt (final short  a, final float  b) { return a < b; }
    public static boolean lt (final short  a, final double b) { return a < b; }
    public static boolean lt (final char   a, final byte   b) { return a < b; }
    public static boolean lt (final char   a, final char   b) { return a < b; }
    public static boolean lt (final char   a, final short  b) { return a < b; }
    public static boolean lt (final char   a, final int    b) { return a < b; }
    public static boolean lt (final char   a, final long   b) { return a < b; }
    public static boolean lt (final char   a, final float  b) { return a < b; }
    public static boolean lt (final char   a, final double b) { return a < b; }
    public static boolean lt (final int    a, final byte   b) { return a < b; }
    public static boolean lt (final int    a, final short  b) { return a < b; }
    public static boolean lt (final int    a, final char   b) { return a < b; }
    public static boolean lt (final int    a, final int    b) { return a < b; }
    public static boolean lt (final int    a, final long   b) { return a < b; }
    public static boolean lt (final int    a, final float  b) { return a < b; }
    public static boolean lt (final int    a, final double b) { return a < b; }
    public static boolean lt (final long   a, final byte   b) { return a < b; }
    public static boolean lt (final long   a, final short  b) { return a < b; }
    public static boolean lt (final long   a, final char   b) { return a < b; }
    public static boolean lt (final long   a, final int    b) { return a < b; }
    public static boolean lt (final long   a, final long   b) { return a < b; }
    public static boolean lt (final long   a, final float  b) { return a < b; }
    public static boolean lt (final long   a, final double b) { return a < b; }
    public static boolean lt (final float  a, final byte   b) { return a < b; }
    public static boolean lt (final float  a, final short  b) { return a < b; }
    public static boolean lt (final float  a, final char   b) { return a < b; }
    public static boolean lt (final float  a, final int    b) { return a < b; }
    public static boolean lt (final float  a, final long   b) { return a < b; }
    public static boolean lt (final float  a, final float  b) { return a < b; }
    public static boolean lt (final float  a, final double b) { return a < b; }
    public static boolean lt (final double a, final byte   b) { return a < b; }
    public static boolean lt (final double a, final short  b) { return a < b; }
    public static boolean lt (final double a, final char   b) { return a < b; }
    public static boolean lt (final double a, final int    b) { return a < b; }
    public static boolean lt (final double a, final long   b) { return a < b; }
    public static boolean lt (final double a, final float  b) { return a < b; }
    public static boolean lt (final double a, final double b) { return a < b; }

    // ====================================== lte : <= ========================================== //

    public static boolean lte (final byte   a, final byte   b) { return a <= b; }
    public static boolean lte (final byte   a, final short  b) { return a <= b; }
    public static boolean lte (final byte   a, final char   b) { return a <= b; }
    public static boolean lte (final byte   a, final int    b) { return a <= b; }
    public static boolean lte (final byte   a, final long   b) { return a <= b; }
    public static boolean lte (final byte   a, final float  b) { return a <= b; }
    public static boolean lte (final byte   a, final double b) { return a <= b; }
    public static boolean lte (final short  a, final byte   b) { return a <= b; }
    public static boolean lte (final short  a, final short  b) { return a <= b; }
    public static boolean lte (final short  a, final char   b) { return a <= b; }
    public static boolean lte (final short  a, final int    b) { return a <= b; }
    public static boolean lte (final short  a, final long   b) { return a <= b; }
    public static boolean lte (final short  a, final float  b) { return a <= b; }
    public static boolean lte (final short  a, final double b) { return a <= b; }
    public static boolean lte (final char   a, final byte   b) { return a <= b; }
    public static boolean lte (final char   a, final short  b) { return a <= b; }
    public static boolean lte (final char   a, final char   b) { return a <= b; }
    public static boolean lte (final char   a, final int    b) { return a <= b; }
    public static boolean lte (final char   a, final long   b) { return a <= b; }
    public static boolean lte (final char   a, final float  b) { return a <= b; }
    public static boolean lte (final char   a, final double b) { return a <= b; }
    public static boolean lte (final int    a, final byte   b) { return a <= b; }
    public static boolean lte (final int    a, final short  b) { return a <= b; }
    public static boolean lte (final int    a, final char   b) { return a <= b; }
    public static boolean lte (final int    a, final int    b) { return a <= b; }
    public static boolean lte (final int    a, final long   b) { return a <= b; }
    public static boolean lte (final int    a, final float  b) { return a <= b; }
    public static boolean lte (final int    a, final double b) { return a <= b; }
    public static boolean lte (final long   a, final byte   b) { return a <= b; }
    public static boolean lte (final long   a, final short  b) { return a <= b; }
    public static boolean lte (final long   a, final char   b) { return a <= b; }
    public static boolean lte (final long   a, final int    b) { return a <= b; }
    public static boolean lte (final long   a, final long   b) { return a <= b; }
    public static boolean lte (final long   a, final float  b) { return a <= b; }
    public static boolean lte (final long   a, final double b) { return a <= b; }
    public static boolean lte (final float  a, final byte   b) { return a <= b; }
    public static boolean lte (final float  a, final short  b) { return a <= b; }
    public static boolean lte (final float  a, final char   b) { return a <= b; }
    public static boolean lte (final float  a, final int    b) { return a <= b; }
    public static boolean lte (final float  a, final long   b) { return a <= b; }
    public static boolean lte (final float  a, final float  b) { return a <= b; }
    public static boolean lte (final float  a, final double b) { return a <= b; }
    public static boolean lte (final double a, final byte   b) { return a <= b; }
    public static boolean lte (final double a, final short  b) { return a <= b; }
    public static boolean lte (final double a, final char   b) { return a <= b; }
    public static boolean lte (final double a, final int    b) { return a <= b; }
    public static boolean lte (final double a, final long   b) { return a <= b; }
    public static boolean lte (final double a, final float  b) { return a <= b; }
    public static boolean lte (final double a, final double b) { return a <= b; }

    // ======================================= gt : > =========================================== //

    public static boolean gt (final byte   a, final byte   b) { return a > b; }
    public static boolean gt (final byte   a, final short  b) { return a > b; }
    public static boolean gt (final byte   a, final char   b) { return a > b; }
    public static boolean gt (final byte   a, final int    b) { return a > b; }
    public static boolean gt (final byte   a, final long   b) { return a > b; }
    public static boolean gt (final byte   a, final float  b) { return a > b; }
    public static boolean gt (final byte   a, final double b) { return a > b; }
    public static boolean gt (final short  a, final byte   b) { return a > b; }
    public static boolean gt (final short  a, final short  b) { return a > b; }
    public static boolean gt (final short  a, final char   b) { return a > b; }
    public static boolean gt (final short  a, final int    b) { return a > b; }
    public static boolean gt (final short  a, final long   b) { return a > b; }
    public static boolean gt (final short  a, final float  b) { return a > b; }
    public static boolean gt (final short  a, final double b) { return a > b; }
    public static boolean gt (final char   a, final byte   b) { return a > b; }
    public static boolean gt (final char   a, final short  b) { return a > b; }
    public static boolean gt (final char   a, final char   b) { return a > b; }
    public static boolean gt (final char   a, final int    b) { return a > b; }
    public static boolean gt (final char   a, final long   b) { return a > b; }
    public static boolean gt (final char   a, final float  b) { return a > b; }
    public static boolean gt (final char   a, final double b) { return a > b; }
    public static boolean gt (final int    a, final byte   b) { return a > b; }
    public static boolean gt (final int    a, final short  b) { return a > b; }
    public static boolean gt (final int    a, final char   b) { return a > b; }
    public static boolean gt (final int    a, final int    b) { return a > b; }
    public static boolean gt (final int    a, final long   b) { return a > b; }
    public static boolean gt (final int    a, final float  b) { return a > b; }
    public static boolean gt (final int    a, final double b) { return a > b; }
    public static boolean gt (final long   a, final byte   b) { return a > b; }
    public static boolean gt (final long   a, final short  b) { return a > b; }
    public static boolean gt (final long   a, final char   b) { return a > b; }
    public static boolean gt (final long   a, final int    b) { return a > b; }
    public static boolean gt (final long   a, final long   b) { return a > b; }
    public static boolean gt (final long   a, final float  b) { return a > b; }
    public static boolean gt (final long   a, final double b) { return a > b; }
    public static boolean gt (final float  a, final byte   b) { return a > b; }
    public static boolean gt (final float  a, final short  b) { return a > b; }
    public static boolean gt (final float  a, final char   b) { return a > b; }
    public static boolean gt (final float  a, final int    b) { return a > b; }
    public static boolean gt (final float  a, final long   b) { return a > b; }
    public static boolean gt (final float  a, final float  b) { return a > b; }
    public static boolean gt (final float  a, final double b) { return a > b; }
    public static boolean gt (final double a, final byte   b) { return a > b; }
    public static boolean gt (final double a, final short  b) { return a > b; }
    public static boolean gt (final double a, final char   b) { return a > b; }
    public static boolean gt (final double a, final int    b) { return a > b; }
    public static boolean gt (final double a, final long   b) { return a > b; }
    public static boolean gt (final double a, final float  b) { return a > b; }
    public static boolean gt (final double a, final double b) { return a > b; }

    // ====================================== gte : >= ========================================== //

    public static boolean gte (final byte   a, final byte   b) { return a >= b; }
    public static boolean gte (final byte   a, final short  b) { return a >= b; }
    public static boolean gte (final byte   a, final char   b) { return a >= b; }
    public static boolean gte (final byte   a, final int    b) { return a >= b; }
    public static boolean gte (final byte   a, final long   b) { return a >= b; }
    public static boolean gte (final byte   a, final float  b) { return a >= b; }
    public static boolean gte (final byte   a, final double b) { return a >= b; }
    public static boolean gte (final short  a, final byte   b) { return a >= b; }
    public static boolean gte (final short  a, final short  b) { return a >= b; }
    public static boolean gte (final short  a, final char   b) { return a >= b; }
    public static boolean gte (final short  a, final int    b) { return a >= b; }
    public static boolean gte (final short  a, final long   b) { return a >= b; }
    public static boolean gte (final short  a, final float  b) { return a >= b; }
    public static boolean gte (final short  a, final double b) { return a >= b; }
    public static boolean gte (final char   a, final byte   b) { return a >= b; }
    public static boolean gte (final char   a, final short  b) { return a >= b; }
    public static boolean gte (final char   a, final char   b) { return a >= b; }
    public static boolean gte (final char   a, final int    b) { return a >= b; }
    public static boolean gte (final char   a, final long   b) { return a >= b; }
    public static boolean gte (final char   a, final float  b) { return a >= b; }
    public static boolean gte (final char   a, final double b) { return a >= b; }
    public static boolean gte (final int    a, final byte   b) { return a >= b; }
    public static boolean gte (final int    a, final short  b) { return a >= b; }
    public static boolean gte (final int    a, final char   b) { return a >= b; }
    public static boolean gte (final int    a, final int    b) { return a >= b; }
    public static boolean gte (final int    a, final long   b) { return a >= b; }
    public static boolean gte (final int    a, final float  b) { return a >= b; }
    public static boolean gte (final int    a, final double b) { return a >= b; }
    public static boolean gte (final long   a, final byte   b) { return a >= b; }
    public static boolean gte (final long   a, final short  b) { return a >= b; }
    public static boolean gte (final long   a, final char   b) { return a >= b; }
    public static boolean gte (final long   a, final int    b) { return a >= b; }
    public static boolean gte (final long   a, final long   b) { return a >= b; }
    public static boolean gte (final long   a, final float  b) { return a >= b; }
    public static boolean gte (final long   a, final double b) { return a >= b; }
    public static boolean gte (final float  a, final byte   b) { return a >= b; }
    public static boolean gte (final float  a, final short  b) { return a >= b; }
    public static boolean gte (final float  a, final char   b) { return a >= b; }
    public static boolean gte (final float  a, final int    b) { return a >= b; }
    public static boolean gte (final float  a, final long   b) { return a >= b; }
    public static boolean gte (final float  a, final float  b) { return a >= b; }
    public static boolean gte (final float  a, final double b) { return a >= b; }
    public static boolean gte (final double a, final byte   b) { return a >= b; }
    public static boolean gte (final double a, final short  b) { return a >= b; }
    public static boolean gte (final double a, final char   b) { return a >= b; }
    public static boolean gte (final double a, final int    b) { return a >= b; }
    public static boolean gte (final double a, final long   b) { return a >= b; }
    public static boolean gte (final double a, final float  b) { return a >= b; }
    public static boolean gte (final double a, final double b) { return a >= b; }

    // ====================================== eq : == ========================================== //

    public static boolean eq  (final boolean a, final boolean b) { return a == b; }
    public static boolean eq  (final byte    a, final byte    b) { return a == b; }
    public static boolean eq  (final byte    a, final short   b) { return a == b; }
    public static boolean eq  (final byte    a, final char    b) { return a == b; }
    public static boolean eq  (final byte    a, final int     b) { return a == b; }
    public static boolean eq  (final byte    a, final long    b) { return a == b; }
    public static boolean eq  (final byte    a, final float   b) { return a == b; }
    public static boolean eq  (final byte    a, final double  b) { return a == b; }
    public static boolean eq  (final short   a, final byte    b) { return a == b; }
    public static boolean eq  (final short   a, final short   b) { return a == b; }
    public static boolean eq  (final short   a, final char    b) { return a == b; }
    public static boolean eq  (final short   a, final int     b) { return a == b; }
    public static boolean eq  (final short   a, final long    b) { return a == b; }
    public static boolean eq  (final short   a, final float   b) { return a == b; }
    public static boolean eq  (final short   a, final double  b) { return a == b; }
    public static boolean eq  (final char    a, final byte    b) { return a == b; }
    public static boolean eq  (final char    a, final short   b) { return a == b; }
    public static boolean eq  (final char    a, final char    b) { return a == b; }
    public static boolean eq  (final char    a, final int     b) { return a == b; }
    public static boolean eq  (final char    a, final long    b) { return a == b; }
    public static boolean eq  (final char    a, final float   b) { return a == b; }
    public static boolean eq  (final char    a, final double  b) { return a == b; }
    public static boolean eq  (final int     a, final byte    b) { return a == b; }
    public static boolean eq  (final int     a, final short   b) { return a == b; }
    public static boolean eq  (final int     a, final char    b) { return a == b; }
    public static boolean eq  (final int     a, final int     b) { return a == b; }
    public static boolean eq  (final int     a, final long    b) { return a == b; }
    public static boolean eq  (final int     a, final float   b) { return a == b; }
    public static boolean eq  (final int     a, final double  b) { return a == b; }
    public static boolean eq  (final long    a, final byte    b) { return a == b; }
    public static boolean eq  (final long    a, final short   b) { return a == b; }
    public static boolean eq  (final long    a, final char    b) { return a == b; }
    public static boolean eq  (final long    a, final int     b) { return a == b; }
    public static boolean eq  (final long    a, final long    b) { return a == b; }
    public static boolean eq  (final long    a, final float   b) { return a == b; }
    public static boolean eq  (final long    a, final double  b) { return a == b; }
    public static boolean eq  (final float   a, final byte    b) { return a == b; }
    public static boolean eq  (final float   a, final short   b) { return a == b; }
    public static boolean eq  (final float   a, final char    b) { return a == b; }
    public static boolean eq  (final float   a, final int     b) { return a == b; }
    public static boolean eq  (final float   a, final long    b) { return a == b; }
    public static boolean eq  (final float   a, final float   b) { return a == b; }
    public static boolean eq  (final float   a, final double  b) { return a == b; }
    public static boolean eq  (final double  a, final byte    b) { return a == b; }
    public static boolean eq  (final double  a, final short   b) { return a == b; }
    public static boolean eq  (final double  a, final char    b) { return a == b; }
    public static boolean eq  (final double  a, final int     b) { return a == b; }
    public static boolean eq  (final double  a, final long    b) { return a == b; }
    public static boolean eq  (final double  a, final float   b) { return a == b; }
    public static boolean eq  (final double  a, final double  b) { return a == b; }

    // =========================== neq : != (implicitly checked) =============================== //

    public static boolean neq (final boolean a, final boolean b) { return a != b; }
    public static boolean neq (final byte    a, final byte    b) { return a != b; }
    public static boolean neq (final byte    a, final short   b) { return a != b; }
    public static boolean neq (final byte    a, final char    b) { return a != b; }
    public static boolean neq (final byte    a, final int     b) { return a != b; }
    public static boolean neq (final byte    a, final long    b) { return a != b; }
    public static boolean neq (final byte    a, final float   b) { return a != b; }
    public static boolean neq (final byte    a, final double  b) { return a != b; }
    public static boolean neq (final short   a, final byte    b) { return a != b; }
    public static boolean neq (final short   a, final short   b) { return a != b; }
    public static boolean neq (final short   a, final char    b) { return a != b; }
    public static boolean neq (final short   a, final int     b) { return a != b; }
    public static boolean neq (final short   a, final long    b) { return a != b; }
    public static boolean neq (final short   a, final float   b) { return a != b; }
    public static boolean neq (final short   a, final double  b) { return a != b; }
    public static boolean neq (final char    a, final byte    b) { return a != b; }
    public static boolean neq (final char    a, final short   b) { return a != b; }
    public static boolean neq (final char    a, final char    b) { return a != b; }
    public static boolean neq (final char    a, final int     b) { return a != b; }
    public static boolean neq (final char    a, final long    b) { return a != b; }
    public static boolean neq (final char    a, final float   b) { return a != b; }
    public static boolean neq (final char    a, final double  b) { return a != b; }
    public static boolean neq (final int     a, final byte    b) { return a != b; }
    public static boolean neq (final int     a, final short   b) { return a != b; }
    public static boolean neq (final int     a, final char    b) { return a != b; }
    public static boolean neq (final int     a, final int     b) { return a != b; }
    public static boolean neq (final int     a, final long    b) { return a != b; }
    public static boolean neq (final int     a, final float   b) { return a != b; }
    public static boolean neq (final int     a, final double  b) { return a != b; }
    public static boolean neq (final long    a, final byte    b) { return a != b; }
    public static boolean neq (final long    a, final short   b) { return a != b; }
    public static boolean neq (final long    a, final char    b) { return a != b; }
    public static boolean neq (final long    a, final int     b) { return a != b; }
    public static boolean neq (final long    a, final long    b) { return a != b; }
    public static boolean neq (final long    a, final float   b) { return a != b; }
    public static boolean neq (final long    a, final double  b) { return a != b; }
    public static boolean neq (final float   a, final byte    b) { return a != b; }
    public static boolean neq (final float   a, final short   b) { return a != b; }
    public static boolean neq (final float   a, final char    b) { return a != b; }
    public static boolean neq (final float   a, final int     b) { return a != b; }
    public static boolean neq (final float   a, final long    b) { return a != b; }
    public static boolean neq (final float   a, final float   b) { return a != b; }
    public static boolean neq (final float   a, final double  b) { return a != b; }
    public static boolean neq (final double  a, final byte    b) { return a != b; }
    public static boolean neq (final double  a, final short   b) { return a != b; }
    public static boolean neq (final double  a, final char    b) { return a != b; }
    public static boolean neq (final double  a, final int     b) { return a != b; }
    public static boolean neq (final double  a, final long    b) { return a != b; }
    public static boolean neq (final double  a, final float   b) { return a != b; }
    public static boolean neq (final double  a, final double  b) { return a != b; }

    // =============================== inc / dec (unchecked) =================================== //

    public static byte   inc (final byte   a) { return (byte )(a + byte1 ); }
    public static short  inc (final short  a) { return (short)(a + short1); }
    public static char   inc (final char   a) { return (char )(a + char1 ); }
    public static int    inc (final int    a) { return a + 1;      }
    public static long   inc (final long   a) { return a + 1L;     }
    public static float  inc (final float  a) { return a + 1.0f;   }
    public static double inc (final double a) { return a + 1.0d;   }

    public static byte   dec (final byte   a) { return (byte )(a - byte1 ); }
    public static short  dec (final short  a) { return (short)(a - short1); }
    public static char   dec (final char   a) { return (char )(a - char1 ); }
    public static int    dec (final int    a) { return a - 1;   }
    public static long   dec (final long   a) { return a - 1L;     }
    public static float  dec (final float  a) { return a - 1.0f;   }
    public static double dec (final double a) { return a - 1.0d;   }

    // ============================ isZero (implicitly checked) ================================ //

    public static boolean isZero (final byte   a) { return a == byte0;  }
    public static boolean isZero (final short  a) { return a == short0; }
    public static boolean isZero (final char   a) { return a == char0;  }
    public static boolean isZero (final int    a) { return a == int0;   }
    public static boolean isZero (final long   a) { return a == 0L;     }
    public static boolean isZero (final float  a) { return a == 0.0f;   }
    public static boolean isZero (final double a) { return a == 0.0d;   }

    // ============================ isNeg (implicitly checked)  ================================ //

    public static boolean isNeg (final byte   a) { return a < byte0;  }
    public static boolean isNeg (final short  a) { return a < short0; }
    public static boolean isNeg (final char   a) { return a < char0;  }
    public static boolean isNeg (final int    a) { return a < int0;   }
    public static boolean isNeg (final long   a) { return a < 0L;     }
    public static boolean isNeg (final float  a) { return a < 0.0f;   }
    public static boolean isNeg (final double a) { return a < 0.0d;   }

    // ============================ isPos (implicitly checked)  ================================ //

    public static boolean isPos (final byte   a) { return a > byte0;  }
    public static boolean isPos (final short  a) { return a > short0; }
    public static boolean isPos (final char   a) { return a > char0;  }
    public static boolean isPos (final int    a) { return a > int0;   }
    public static boolean isPos (final long   a) { return a > 0L;     }
    public static boolean isPos (final float  a) { return a > 0.0f;   }
    public static boolean isPos (final double a) { return a > 0.0d;   }

    // ===================== add : + (unchecked unless otherwise noted) ========================= //
    // "Infectious": uses a promotion of the largest data type passed

    // Implicitly checked
    public static short  add (final byte   a, final byte   b) { return (short)(a + b); }
    // Implicitly checked
    public static int    add (final byte   a, final short  b) { return         a + b;  }
    // Implicitly checked
    public static int    add (final byte   a, final char   b) { return         a + b;  }
    // Implicitly checked
    public static long   add (final byte   a, final int    b) { return         a + b;  }
    public static long   add (final byte   a, final long   b) { return         a + b;  }
    // Implicitly checked
    public static double add (final byte   a, final float  b) { return         a + b;  }
    public static double add (final byte   a, final double b) { return         a + b;  }
    // Implicitly checked
    public static int    add (final short  a, final byte   b) { return         a + b;  }
    // Implicitly checked
    public static int    add (final short  a, final short  b) { return         a + b;  }
    // Implicitly checked
    public static int    add (final short  a, final char   b) { return         a + b;  }
    // Implicitly checked
    public static long   add (final short  a, final int    b) { return         a + b;  }
    public static long   add (final short  a, final long   b) { return         a + b;  }
    // Implicitly checked
    public static double add (final short  a, final float  b) { return         a + b;  }
    public static double add (final short  a, final double b) { return         a + b;  }
    // Implicitly checked
    public static int    add (final char   a, final byte   b) { return         a + b;  }
    // Implicitly checked
    public static int    add (final char   a, final short  b) { return         a + b;  }
    // Implicitly checked
    public static int    add (final char   a, final char   b) { return         a + b;  }
    // Implicitly checked
    public static long   add (final char   a, final int    b) { return         a + b;  }
    public static long   add (final char   a, final long   b) { return         a + b;  }
    // Implicitly checked
    public static double add (final char   a, final float  b) { return         a + b;  }
    public static double add (final char   a, final double b) { return         a + b;  }
    // Implicitly checked
    public static long   add (final int    a, final byte   b) { return         a + b;  }
    // Implicitly checked
    public static long   add (final int    a, final short  b) { return         a + b;  }
    // Implicitly checked
    public static long   add (final int    a, final char   b) { return         a + b;  }
    // Implicitly checked
    public static long   add (final int    a, final int    b) { return         a + b;  }
    public static long   add (final int    a, final long   b) { return         a + b;  }
    // Implicitly checked
    public static double add (final int    a, final float  b) { return         a + b;  }
    public static double add (final int    a, final double b) { return         a + b;  }
    public static long   add (final long   a, final byte   b) { return         a + b;  }
    public static long   add (final long   a, final short  b) { return         a + b;  }
    public static long   add (final long   a, final char   b) { return         a + b;  }
    public static long   add (final long   a, final int    b) { return         a + b;  }
    public static long   add (final long   a, final long   b) { return         a + b;  }
    public static double add (final long   a, final float  b) { return         a + b;  }
    public static double add (final long   a, final double b) { return         a + b;  }
    // Implicitly checked
    public static double add (final float  a, final byte   b) { return         a + b;  }
    // Implicitly checked
    public static double add (final float  a, final short  b) { return         a + b;  }
    // Implicitly checked
    public static double add (final float  a, final char   b) { return         a + b;  }
    // Implicitly checked
    public static double add (final float  a, final int    b) { return         a + b;  }
    public static double add (final float  a, final long   b) { return         a + b;  }
    // Implicitly checked
    public static double add (final float  a, final float  b) { return         a + b;  }
    public static double add (final float  a, final double b) { return         a + b;  }
    public static double add (final double a, final byte   b) { return         a + b;  }
    public static double add (final double a, final short  b) { return         a + b;  }
    public static double add (final double a, final char   b) { return         a + b;  }
    public static double add (final double a, final int    b) { return         a + b;  }
    public static double add (final double a, final long   b) { return         a + b;  }
    public static double add (final double a, final float  b) { return         a + b;  }
    public static double add (final double a, final double b) { return         a + b;  }

    // ================== subtract : - (unchecked unless otherwise noted) ====================== //
    // "Infectious": uses the largest data type passed

    // Implicitly checked
    public static short  subtract (final byte   a, final byte   b) { return (short)(a - b); }
    // Implicitly checked
    public static int    subtract (final byte   a, final short  b) { return         a - b;  }
    // Implicitly checked
    public static int    subtract (final byte   a, final char   b) { return         a - b;  }
    // Implicitly checked
    public static long   subtract (final byte   a, final int    b) { return         a - b;  }
    public static long   subtract (final byte   a, final long   b) { return         a - b;  }
    // Implicitly checked
    public static double subtract (final byte   a, final float  b) { return         a - b;  }
    public static double subtract (final byte   a, final double b) { return         a - b;  }
    // Implicitly checked
    public static int    subtract (final short  a, final byte   b) { return         a - b;  }
    // Implicitly checked
    public static int    subtract (final short  a, final short  b) { return         a - b;  }
    // Implicitly checked
    public static int    subtract (final short  a, final char   b) { return         a - b;  }
    // Implicitly checked
    public static long   subtract (final short  a, final int    b) { return         a - b;  }
    public static long   subtract (final short  a, final long   b) { return         a - b;  }
    // Implicitly checked
    public static double subtract (final short  a, final float  b) { return         a - b;  }
    public static double subtract (final short  a, final double b) { return         a - b;  }
    // Implicitly checked
    public static int    subtract (final char   a, final byte   b) { return         a - b;  }
    // Implicitly checked
    public static int    subtract (final char   a, final short  b) { return         a - b;  }
    // Implicitly checked
    public static int    subtract (final char   a, final char   b) { return         a - b;  }
    // Implicitly checked
    public static long   subtract (final char   a, final int    b) { return         a - b;  }
    public static long   subtract (final char   a, final long   b) { return         a - b;  }
    // Implicitly checked
    public static double subtract (final char   a, final float  b) { return         a - b;  }
    public static double subtract (final char   a, final double b) { return         a - b;  }
    // Implicitly checked
    public static long   subtract (final int    a, final byte   b) { return         a - b;  }
    // Implicitly checked
    public static long   subtract (final int    a, final short  b) { return         a - b;  }
    // Implicitly checked
    public static long   subtract (final int    a, final char   b) { return         a - b;  }
    // Implicitly checked
    public static long   subtract (final int    a, final int    b) { return         a - b;  }
    public static long   subtract (final int    a, final long   b) { return         a - b;  }
    // Implicitly checked
    public static double subtract (final int    a, final float  b) { return         a - b;  }
    public static double subtract (final int    a, final double b) { return         a - b;  }
    public static long   subtract (final long   a, final byte   b) { return         a - b;  }
    public static long   subtract (final long   a, final short  b) { return         a - b;  }
    public static long   subtract (final long   a, final char   b) { return         a - b;  }
    public static long   subtract (final long   a, final int    b) { return         a - b;  }
    public static long   subtract (final long   a, final long   b) { return         a - b;  }
    public static double subtract (final long   a, final float  b) { return         a - b;  }
    public static double subtract (final long   a, final double b) { return         a - b;  }
    // Implicitly checked
    public static double subtract (final float  a, final byte   b) { return         a - b;  }
    // Implicitly checked
    public static double subtract (final float  a, final short  b) { return         a - b;  }
    // Implicitly checked
    public static double subtract (final float  a, final char   b) { return         a - b;  }
    // Implicitly checked
    public static double subtract (final float  a, final int    b) { return         a - b;  }
    public static double subtract (final float  a, final long   b) { return         a - b;  }
    // Implicitly checked
    public static double subtract (final float  a, final float  b) { return         a - b;  }
    public static double subtract (final float  a, final double b) { return         a - b;  }
    public static double subtract (final double a, final byte   b) { return         a - b;  }
    public static double subtract (final double a, final short  b) { return         a - b;  }
    public static double subtract (final double a, final char   b) { return         a - b;  }
    public static double subtract (final double a, final int    b) { return         a - b;  }
    public static double subtract (final double a, final long   b) { return         a - b;  }
    public static double subtract (final double a, final float  b) { return         a - b;  }
    public static double subtract (final double a, final double b) { return         a - b;  }

    // =============================== negate : - (unchecked) =================================== //

    public static byte   negate   (final byte   a) { return (byte )-a; }
    public static short  negate   (final short  a) { return (short)-a; }
    public static char   negate   (final char   a) { return (char )-a; }
    public static int    negate   (final int    a) { return -a; }
    public static long   negate   (final long   a) { return -a; }
    public static float  negate   (final float  a) { return -a; }
    public static double negate   (final double a) { return -a; }

    // ================== multiply : * (unchecked unless otherwise noted) ====================== //
    // "Infectious": uses the largest data type passed

    // Implicitly checked
    public static short  multiply (final byte   a, final byte   b) { return (short)(a * b); }
    // Implicitly checked
    public static int    multiply (final byte   a, final short  b) { return a * b; }
    // Implicitly checked
    public static int    multiply (final byte   a, final char   b) { return a * b; }
    // Implicitly checked
    public static long   multiply (final byte   a, final int    b) { return a * b; }
    public static long   multiply (final byte   a, final long   b) { return a * b; } // ->BigInteger
    public static double multiply (final byte   a, final float  b) { return a * b; }
    public static double multiply (final byte   a, final double b) { return a * b; }
    public static long   multiply (final short  a, final byte   b) { return a * b; }
    public static long   multiply (final short  a, final short  b) { return a * b; }
    public static long   multiply (final short  a, final char   b) { return a * b; }
    public static long   multiply (final short  a, final int    b) { return a * b; }
    public static long   multiply (final short  a, final long   b) { return a * b; }
    public static double multiply (final short  a, final float  b) { return a * b; }
    public static double multiply (final short  a, final double b) { return a * b; }
    public static long   multiply (final char   a, final byte   b) { return a * b; }
    public static long   multiply (final char   a, final short  b) { return a * b; }
    public static long   multiply (final char   a, final char   b) { return a * b; }
    public static long   multiply (final char   a, final int    b) { return a * b; }
    public static long   multiply (final char   a, final long   b) { return a * b; }
    public static double multiply (final char   a, final float  b) { return a * b; }
    public static double multiply (final char   a, final double b) { return a * b; }
    public static long   multiply (final int    a, final byte   b) { return a * b; }
    public static long   multiply (final int    a, final short  b) { return a * b; }
    public static long   multiply (final int    a, final char   b) { return a * b; }
    public static long   multiply (final int    a, final int    b) { return a * b; }
    public static long   multiply (final int    a, final long   b) { return a * b; }
    public static double multiply (final int    a, final float  b) { return a * b; }
    public static double multiply (final int    a, final double b) { return a * b; }
    public static long   multiply (final long   a, final byte   b) { return a * b; }
    public static long   multiply (final long   a, final short  b) { return a * b; }
    public static long   multiply (final long   a, final char   b) { return a * b; }
    public static long   multiply (final long   a, final int    b) { return a * b; }
    public static long   multiply (final long   a, final long   b) { return a * b; }
    public static double multiply (final long   a, final float  b) { return a * b; }
    public static double multiply (final long   a, final double b) { return a * b; }
    public static double multiply (final float  a, final byte   b) { return a * b; }
    public static double multiply (final float  a, final short  b) { return a * b; }
    public static double multiply (final float  a, final char   b) { return a * b; }
    public static double multiply (final float  a, final int    b) { return a * b; }
    public static double multiply (final float  a, final long   b) { return a * b; }
    public static double multiply (final float  a, final float  b) { return a * b; }
    public static double multiply (final float  a, final double b) { return a * b; }
    public static double multiply (final double a, final byte   b) { return a * b; }
    public static double multiply (final double a, final short  b) { return a * b; }
    public static double multiply (final double a, final char   b) { return a * b; }
    public static double multiply (final double a, final int    b) { return a * b; }
    public static double multiply (final double a, final long   b) { return a * b; }
    public static double multiply (final double a, final float  b) { return a * b; }
    public static double multiply (final double a, final double b) { return a * b; }

    // =============================== divde : / (unchecked) =================================== //
    // "Infectious": uses the largest data type passed
    // TODO need to deal with int truncation here... sometimes it's intentional...

    public static double divide   (final byte   a, final byte   b) { return a / b; }
    public static double divide   (final byte   a, final short  b) { return a / b; }
    public static double divide   (final byte   a, final char   b) { return a / b; }
    public static double divide   (final byte   a, final int    b) { return a / b; }
    public static double divide   (final byte   a, final long   b) { return a / b; }
    public static double divide   (final byte   a, final float  b) { return a / b; }
    public static double divide   (final byte   a, final double b) { return a / b; }
    public static double divide   (final short  a, final byte   b) { return a / b; }
    public static double divide   (final short  a, final short  b) { return a / b; }
    public static double divide   (final short  a, final char   b) { return a / b; }
    public static double divide   (final short  a, final int    b) { return a / b; }
    public static double divide   (final short  a, final long   b) { return a / b; }
    public static double divide   (final short  a, final float  b) { return a / b; }
    public static double divide   (final short  a, final double b) { return a / b; }
    public static double divide   (final char   a, final byte   b) { return a / b; }
    public static double divide   (final char   a, final short  b) { return a / b; }
    public static double divide   (final char   a, final char   b) { return a / b; }
    public static double divide   (final char   a, final int    b) { return a / b; }
    public static double divide   (final char   a, final long   b) { return a / b; }
    public static double divide   (final char   a, final float  b) { return a / b; }
    public static double divide   (final char   a, final double b) { return a / b; }
    public static double divide   (final int    a, final byte   b) { return a / b; }
    public static double divide   (final int    a, final short  b) { return a / b; }
    public static double divide   (final int    a, final char   b) { return a / b; }
    public static double divide   (final int    a, final int    b) { return a / b; }
    public static double divide   (final int    a, final long   b) { return a / b; }
    public static double divide   (final int    a, final float  b) { return a / b; }
    public static double divide   (final int    a, final double b) { return a / b; }
    public static double divide   (final long   a, final byte   b) { return a / b; }
    public static double divide   (final long   a, final short  b) { return a / b; }
    public static double divide   (final long   a, final char   b) { return a / b; }
    public static double divide   (final long   a, final int    b) { return a / b; }
    public static double divide   (final long   a, final long   b) { return a / b; }
    public static double divide   (final long   a, final float  b) { return a / b; }
    public static double divide   (final long   a, final double b) { return a / b; }
    public static double divide   (final float  a, final byte   b) { return a / b; }
    public static double divide   (final float  a, final short  b) { return a / b; }
    public static double divide   (final float  a, final char   b) { return a / b; }
    public static double divide   (final float  a, final int    b) { return a / b; }
    public static double divide   (final float  a, final long   b) { return a / b; }
    public static double divide   (final float  a, final float  b) { return a / b; }
    public static double divide   (final float  a, final double b) { return a / b; }
    public static double divide   (final double a, final byte   b) { return a / b; }
    public static double divide   (final double a, final short  b) { return a / b; }
    public static double divide   (final double a, final char   b) { return a / b; }
    public static double divide   (final double a, final int    b) { return a / b; }
    public static double divide   (final double a, final long   b) { return a / b; }
    public static double divide   (final double a, final float  b) { return a / b; }
    public static double divide   (final double a, final double b) { return a / b; }

    // ============================== max (implicitly checked) ================================== //
    // "Infectious": uses the largest data type passed

    public static byte   max (final byte   a, final byte   b) { return (a < b) ? b : a; }
    public static short  max (final byte   a, final short  b) { return (a < b) ? b : a; }
    public static char   max (final byte   a, final char   b) { return (char)((a < b) ? b : a); }
    public static int    max (final byte   a, final int    b) { return (a < b) ? b : a; }
    public static long   max (final byte   a, final long   b) { return (a < b) ? b : a; }
    public static float  max (final byte   a, final float  b) { return (a < b) ? b : a; }
    public static double max (final byte   a, final double b) { return (a < b) ? b : a; }
    public static short  max (final short  a, final byte   b) { return (a < b) ? b : a; }
    public static short  max (final short  a, final short  b) { return (a < b) ? b : a; }
    public static short  max (final short  a, final char   b) { return (short)((a < b) ? b : a); }
    public static int    max (final short  a, final int    b) { return (a < b) ? b : a; }
    public static long   max (final short  a, final long   b) { return (a < b) ? b : a; }
    public static float  max (final short  a, final float  b) { return (a < b) ? b : a; }
    public static double max (final short  a, final double b) { return (a < b) ? b : a; }
    public static char   max (final char   a, final byte   b) { return (char)((a < b) ? b : a); }
    public static short  max (final char   a, final short  b) { return (short)((a < b) ? b : a); }
    public static char   max (final char   a, final char   b) { return (a < b) ? b : a; }
    public static int    max (final char   a, final int    b) { return (a < b) ? b : a; }
    public static long   max (final char   a, final long   b) { return (a < b) ? b : a; }
    public static float  max (final char   a, final float  b) { return (a < b) ? b : a; }
    public static double max (final char   a, final double b) { return (a < b) ? b : a; }
    public static int    max (final int    a, final byte   b) { return (a < b) ? b : a; }
    public static int    max (final int    a, final short  b) { return (a < b) ? b : a; }
    public static int    max (final int    a, final char   b) { return (a < b) ? b : a; }
    public static int    max (final int    a, final int    b) { return Math.max(a, b);  }
    public static long   max (final int    a, final long   b) { return (a < b) ? b : a; }
    public static float  max (final int    a, final float  b) { return (a < b) ? b : a; }
    public static double max (final int    a, final double b) { return (a < b) ? b : a; }
    public static long   max (final long   a, final byte   b) { return (a < b) ? b : a; }
    public static long   max (final long   a, final short  b) { return (a < b) ? b : a; }
    public static long   max (final long   a, final char   b) { return (a < b) ? b : a; }
    public static long   max (final long   a, final int    b) { return (a < b) ? b : a; }
    public static long   max (final long   a, final long   b) { return Math.max(a, b);  }
    public static float  max (final long   a, final float  b) { return (a < b) ? b : a; }
    public static double max (final long   a, final double b) { return (a < b) ? b : a; }
    public static float  max (final float  a, final byte   b) { return (a < b) ? b : a; }
    public static float  max (final float  a, final short  b) { return (a < b) ? b : a; }
    public static float  max (final float  a, final char   b) { return (a < b) ? b : a; }
    public static float  max (final float  a, final int    b) { return (a < b) ? b : a; }
    public static float  max (final float  a, final long   b) { return (a < b) ? b : a; }
    public static float  max (final float  a, final float  b) { return Math.max(a, b);  }
    public static double max (final float  a, final double b) { return (a < b) ? b : a; }
    public static double max (final double a, final byte   b) { return (a < b) ? b : a; }
    public static double max (final double a, final short  b) { return (a < b) ? b : a; }
    public static double max (final double a, final char   b) { return (a < b) ? b : a; }
    public static double max (final double a, final int    b) { return (a < b) ? b : a; }
    public static double max (final double a, final long   b) { return (a < b) ? b : a; }
    public static double max (final double a, final float  b) { return (a < b) ? b : a; }
    public static double max (final double a, final double b) { return Math.max(a, b);  }

    // ============================== min (implicitly checked) ================================== //
    // "Infectious": uses the largest data type passed

    public static byte   min (final byte   a, final byte   b) { return (a > b) ? b : a; }
    public static short  min (final byte   a, final short  b) { return (a > b) ? b : a; }
    public static char   min (final byte   a, final char   b) { return (char)((a > b) ? b : a); }
    public static int    min (final byte   a, final int    b) { return (a > b) ? b : a; }
    public static long   min (final byte   a, final long   b) { return (a > b) ? b : a; }
    public static float  min (final byte   a, final float  b) { return (a > b) ? b : a; }
    public static double min (final byte   a, final double b) { return (a > b) ? b : a; }
    public static short  min (final short  a, final byte   b) { return (a > b) ? b : a; }
    public static short  min (final short  a, final short  b) { return (a > b) ? b : a; }
    public static short  min (final short  a, final char   b) { return (short)((a > b) ? b : a); }
    public static int    min (final short  a, final int    b) { return (a > b) ? b : a; }
    public static long   min (final short  a, final long   b) { return (a > b) ? b : a; }
    public static float  min (final short  a, final float  b) { return (a > b) ? b : a; }
    public static double min (final short  a, final double b) { return (a > b) ? b : a; }
    public static char   min (final char   a, final byte   b) { return (char)((a > b) ? b : a); }
    public static short  min (final char   a, final short  b) { return (short)((a > b) ? b : a); }
    public static char   min (final char   a, final char   b) { return (a > b) ? b : a; }
    public static int    min (final char   a, final int    b) { return (a > b) ? b : a; }
    public static long   min (final char   a, final long   b) { return (a > b) ? b : a; }
    public static float  min (final char   a, final float  b) { return (a > b) ? b : a; }
    public static double min (final char   a, final double b) { return (a > b) ? b : a; }
    public static int    min (final int    a, final byte   b) { return (a > b) ? b : a; }
    public static int    min (final int    a, final short  b) { return (a > b) ? b : a; }
    public static int    min (final int    a, final char   b) { return (a > b) ? b : a; }
    // Intrinsic; maybe the others could be acclerated in the same way?
    // TODO maybe use if-optimization?
    public static int    min (final int    a, final int    b) { return Math.min(a, b);  }
    public static long   min (final int    a, final long   b) { return (a > b) ? b : a; }
    public static float  min (final int    a, final float  b) { return (a > b) ? b : a; }
    public static double min (final int    a, final double b) { return (a > b) ? b : a; }
    public static long   min (final long   a, final byte   b) { return (a > b) ? b : a; }
    public static long   min (final long   a, final short  b) { return (a > b) ? b : a; }
    public static long   min (final long   a, final char   b) { return (a > b) ? b : a; }
    public static long   min (final long   a, final int    b) { return (a > b) ? b : a; }
    public static long   min (final long   a, final long   b) { return Math.min(a, b);  }
    public static float  min (final long   a, final float  b) { return (a > b) ? b : a; }
    public static double min (final long   a, final double b) { return (a > b) ? b : a; }
    public static float  min (final float  a, final byte   b) { return (a > b) ? b : a; }
    public static float  min (final float  a, final short  b) { return (a > b) ? b : a; }
    public static float  min (final float  a, final char   b) { return (a > b) ? b : a; }
    public static float  min (final float  a, final int    b) { return (a > b) ? b : a; }
    public static float  min (final float  a, final long   b) { return (a > b) ? b : a; }
    public static float  min (final float  a, final float  b) { return Math.min(a, b);  }
    public static double min (final float  a, final double b) { return (a > b) ? b : a; }
    public static double min (final double a, final byte   b) { return (a > b) ? b : a; }
    public static double min (final double a, final short  b) { return (a > b) ? b : a; }
    public static double min (final double a, final char   b) { return (a > b) ? b : a; }
    public static double min (final double a, final int    b) { return (a > b) ? b : a; }
    public static double min (final double a, final long   b) { return (a > b) ? b : a; }
    public static double min (final double a, final float  b) { return (a > b) ? b : a; }
    public static double min (final double a, final double b) { return Math.min(a, b);  }

    // ================================== rem (unchecked) ====================================== //

    // TODO other overloads
    public static long   rem (final long   a, final long   b) { return a % b; }

    // ======================================= even? =========================================== //

    // NOTE that this is here for the Java quantum.core.Collections
    public static boolean isEven (final long x) {
        return isZero(bitAnd(x, 1));
    }
}
