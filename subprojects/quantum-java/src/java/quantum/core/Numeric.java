package quantum.core;
// TAKEN FROM ztellman/primitive-math AND EXPANDED

// Note from java.lang.Math:
// Code generators are encouraged to use platform-specific native libraries or
// microprocessor instructions, where available, to provide higher-performance
// implementations of Math methods. Such higher-performance implementations still
// must conform to the specification for Math.

public class Numeric {

    public static final byte  byte0  = (byte) 0;
    public static final char  char0  = (char) 0;
    public static final short short0 = (short)0;
    public static final int   int0   = 0;
    public static final byte  byte1  = (byte) 1;
    public static final char  char1  = (char) 1;
    public static final short short1 = (short)1;
    public static final int   int1   = 1;

    // ============================ BOOLEAN OPERATIONS ================================ //

    public static boolean isTrue  (final boolean a                 ) { return a == true; }
    public static boolean isFalse (final boolean a                 ) { return a == false; }
    public static boolean isNil   (final Object  a                 ) { return a == null; }
    public static boolean and     (final boolean a, final boolean b) { return a && b; }
    public static boolean or      (final boolean a, final boolean b) { return a || b; }
    public static boolean not     (final boolean a                 ) { return !a; }
    public static boolean xor     (final boolean a, final boolean b) { return (a || b) && !(a && b); }

    // ============================ BIT OPERATIONS ================================ //
    // Apparently '&' is fundamentally an int operation

    public static byte  bitAnd (final byte   a, final byte   b) { return (byte) (a & b); }
    public static char  bitAnd (final byte   a, final char   b) { return (char) (a & b); }
    public static short bitAnd (final byte   a, final short  b) { return (short)(a & b); }
    public static int   bitAnd (final byte   a, final int    b) { return         a & b; }
    public static long  bitAnd (final byte   a, final long   b) { return         a & b; }
    public static char  bitAnd (final char   a, final byte   b) { return (char) (a & b); }
    public static char  bitAnd (final char   a, final char   b) { return (char) (a & b); }
    public static short bitAnd (final char   a, final short  b) { return (short)(a & b); }
    public static int   bitAnd (final char   a, final int    b) { return         a & b; }
    public static long  bitAnd (final char   a, final long   b) { return         a & b; }
    public static short bitAnd (final short  a, final byte   b) { return (short)(a & b); }
    public static short bitAnd (final short  a, final char   b) { return (short)(a & b); }
    public static short bitAnd (final short  a, final short  b) { return (short)(a & b); }
    public static int   bitAnd (final short  a, final int    b) { return         a & b; }
    public static long  bitAnd (final short  a, final long   b) { return         a & b; }
    public static int   bitAnd (final int    a, final byte   b) { return         a & b; }
    public static int   bitAnd (final int    a, final char   b) { return         a & b; }
    public static int   bitAnd (final int    a, final short  b) { return         a & b; }
    public static int   bitAnd (final int    a, final int    b) { return         a & b; }
    public static long  bitAnd (final int    a, final long   b) { return         a & b; }
    public static long  bitAnd (final long   a, final byte   b) { return         a & b; }
    public static long  bitAnd (final long   a, final char   b) { return         a & b; }
    public static long  bitAnd (final long   a, final short  b) { return         a & b; }
    public static long  bitAnd (final long   a, final int    b) { return         a & b; }
    public static long  bitAnd (final long   a, final long   b) { return         a & b; }
    
    public static long  bitOr              (final long a, final long b) { return a |   b; } // Implicitly checked
    public static long  bitXor             (final long a, final long b) { return a ^   b; } // Implicitly checked
    public static long  bitNot             (final long a              ) { return ~a;      } // Implicitly checked
    public static long  shiftLeft          (final long a, final long b) { return a <<  b; } // Implicitly checked
    public static long  shiftRight         (final long a, final long b) { return a >>  b; } // Implicitly checked
    public static long  unsignedShiftRight (final long a, final long b) { return a >>> b; }
    public static long  unsignedShiftRight (final int  a, final long b) { return a >>> b; }

    // TODO flipbit, testbit, setbit, clearbit

    // Because "more than one matching method"
    public static short reverseShort(final short x) {
        return (short) ((x << 8)
                        | ((char) x >>> 8));
    }

    public static int   reverseInt  (final int   x) {
        return (  (x << 24)
                | ((x & 0x0000ff00) <<  8)
                | ((x & 0x00ff0000) >>> 8)
                | (x >>> 24));
    }

    public static long  reverseLong (final long  x) {
        return (  ((long) reverseInt((int)x) << 32)
                | ((long) reverseInt((int)(x >>> 32)) & 0xffffffffL));
    }

    // ============================ LT : < ================================ //

    public static boolean lt (final byte   a, final byte   b) { return a < b; }
    public static boolean lt (final byte   a, final char   b) { return a < b; }
    public static boolean lt (final byte   a, final short  b) { return a < b; }
    public static boolean lt (final byte   a, final int    b) { return a < b; }
    public static boolean lt (final byte   a, final long   b) { return a < b; }
    public static boolean lt (final byte   a, final float  b) { return a < b; }
    public static boolean lt (final byte   a, final double b) { return a < b; }
    public static boolean lt (final char   a, final byte   b) { return a < b; }
    public static boolean lt (final char   a, final char   b) { return a < b; }
    public static boolean lt (final char   a, final short  b) { return a < b; }
    public static boolean lt (final char   a, final int    b) { return a < b; }
    public static boolean lt (final char   a, final long   b) { return a < b; }
    public static boolean lt (final char   a, final float  b) { return a < b; }
    public static boolean lt (final char   a, final double b) { return a < b; }
    public static boolean lt (final short  a, final byte   b) { return a < b; }
    public static boolean lt (final short  a, final char   b) { return a < b; }
    public static boolean lt (final short  a, final short  b) { return a < b; }
    public static boolean lt (final short  a, final int    b) { return a < b; }
    public static boolean lt (final short  a, final long   b) { return a < b; }
    public static boolean lt (final short  a, final float  b) { return a < b; }
    public static boolean lt (final short  a, final double b) { return a < b; }
    public static boolean lt (final int    a, final byte   b) { return a < b; }
    public static boolean lt (final int    a, final char   b) { return a < b; }
    public static boolean lt (final int    a, final short  b) { return a < b; }
    public static boolean lt (final int    a, final int    b) { return a < b; }
    public static boolean lt (final int    a, final long   b) { return a < b; }
    public static boolean lt (final int    a, final float  b) { return a < b; }
    public static boolean lt (final int    a, final double b) { return a < b; }
    public static boolean lt (final long   a, final byte   b) { return a < b; }
    public static boolean lt (final long   a, final char   b) { return a < b; }
    public static boolean lt (final long   a, final short  b) { return a < b; }
    public static boolean lt (final long   a, final int    b) { return a < b; }
    public static boolean lt (final long   a, final long   b) { return a < b; }
    public static boolean lt (final long   a, final float  b) { return a < b; }
    public static boolean lt (final long   a, final double b) { return a < b; }
    public static boolean lt (final float  a, final byte   b) { return a < b; }
    public static boolean lt (final float  a, final char   b) { return a < b; }
    public static boolean lt (final float  a, final short  b) { return a < b; }
    public static boolean lt (final float  a, final int    b) { return a < b; }
    public static boolean lt (final float  a, final long   b) { return a < b; }
    public static boolean lt (final float  a, final float  b) { return a < b; }
    public static boolean lt (final float  a, final double b) { return a < b; }
    public static boolean lt (final double a, final byte   b) { return a < b; }
    public static boolean lt (final double a, final char   b) { return a < b; }
    public static boolean lt (final double a, final short  b) { return a < b; }
    public static boolean lt (final double a, final int    b) { return a < b; }
    public static boolean lt (final double a, final long   b) { return a < b; }
    public static boolean lt (final double a, final float  b) { return a < b; }
    public static boolean lt (final double a, final double b) { return a < b; }

    // ============================ LTE : <= ================================ //

    public static boolean lte (final byte   a, final byte   b) { return a <= b; }
    public static boolean lte (final byte   a, final char   b) { return a <= b; }
    public static boolean lte (final byte   a, final short  b) { return a <= b; }
    public static boolean lte (final byte   a, final int    b) { return a <= b; }
    public static boolean lte (final byte   a, final long   b) { return a <= b; }
    public static boolean lte (final byte   a, final float  b) { return a <= b; }
    public static boolean lte (final byte   a, final double b) { return a <= b; }
    public static boolean lte (final char   a, final byte   b) { return a <= b; }
    public static boolean lte (final char   a, final char   b) { return a <= b; }
    public static boolean lte (final char   a, final short  b) { return a <= b; }
    public static boolean lte (final char   a, final int    b) { return a <= b; }
    public static boolean lte (final char   a, final long   b) { return a <= b; }
    public static boolean lte (final char   a, final float  b) { return a <= b; }
    public static boolean lte (final char   a, final double b) { return a <= b; }
    public static boolean lte (final short  a, final byte   b) { return a <= b; }
    public static boolean lte (final short  a, final char   b) { return a <= b; }
    public static boolean lte (final short  a, final short  b) { return a <= b; }
    public static boolean lte (final short  a, final int    b) { return a <= b; }
    public static boolean lte (final short  a, final long   b) { return a <= b; }
    public static boolean lte (final short  a, final float  b) { return a <= b; }
    public static boolean lte (final short  a, final double b) { return a <= b; }
    public static boolean lte (final int    a, final byte   b) { return a <= b; }
    public static boolean lte (final int    a, final char   b) { return a <= b; }
    public static boolean lte (final int    a, final short  b) { return a <= b; }
    public static boolean lte (final int    a, final int    b) { return a <= b; }
    public static boolean lte (final int    a, final long   b) { return a <= b; }
    public static boolean lte (final int    a, final float  b) { return a <= b; }
    public static boolean lte (final int    a, final double b) { return a <= b; }
    public static boolean lte (final long   a, final byte   b) { return a <= b; }
    public static boolean lte (final long   a, final char   b) { return a <= b; }
    public static boolean lte (final long   a, final short  b) { return a <= b; }
    public static boolean lte (final long   a, final int    b) { return a <= b; }
    public static boolean lte (final long   a, final long   b) { return a <= b; }
    public static boolean lte (final long   a, final float  b) { return a <= b; }
    public static boolean lte (final long   a, final double b) { return a <= b; }
    public static boolean lte (final float  a, final byte   b) { return a <= b; }
    public static boolean lte (final float  a, final char   b) { return a <= b; }
    public static boolean lte (final float  a, final short  b) { return a <= b; }
    public static boolean lte (final float  a, final int    b) { return a <= b; }
    public static boolean lte (final float  a, final long   b) { return a <= b; }
    public static boolean lte (final float  a, final float  b) { return a <= b; }
    public static boolean lte (final float  a, final double b) { return a <= b; }
    public static boolean lte (final double a, final byte   b) { return a <= b; }
    public static boolean lte (final double a, final char   b) { return a <= b; }
    public static boolean lte (final double a, final short  b) { return a <= b; }
    public static boolean lte (final double a, final int    b) { return a <= b; }
    public static boolean lte (final double a, final long   b) { return a <= b; }
    public static boolean lte (final double a, final float  b) { return a <= b; }
    public static boolean lte (final double a, final double b) { return a <= b; }

    // ============================ GT : > ================================ //

    public static boolean gt (final byte   a, final byte   b) { return a > b; }
    public static boolean gt (final byte   a, final char   b) { return a > b; }
    public static boolean gt (final byte   a, final short  b) { return a > b; }
    public static boolean gt (final byte   a, final int    b) { return a > b; }
    public static boolean gt (final byte   a, final long   b) { return a > b; }
    public static boolean gt (final byte   a, final float  b) { return a > b; }
    public static boolean gt (final byte   a, final double b) { return a > b; }
    public static boolean gt (final char   a, final byte   b) { return a > b; }
    public static boolean gt (final char   a, final char   b) { return a > b; }
    public static boolean gt (final char   a, final short  b) { return a > b; }
    public static boolean gt (final char   a, final int    b) { return a > b; }
    public static boolean gt (final char   a, final long   b) { return a > b; }
    public static boolean gt (final char   a, final float  b) { return a > b; }
    public static boolean gt (final char   a, final double b) { return a > b; }
    public static boolean gt (final short  a, final byte   b) { return a > b; }
    public static boolean gt (final short  a, final char   b) { return a > b; }
    public static boolean gt (final short  a, final short  b) { return a > b; }
    public static boolean gt (final short  a, final int    b) { return a > b; }
    public static boolean gt (final short  a, final long   b) { return a > b; }
    public static boolean gt (final short  a, final float  b) { return a > b; }
    public static boolean gt (final short  a, final double b) { return a > b; }
    public static boolean gt (final int    a, final byte   b) { return a > b; }
    public static boolean gt (final int    a, final char   b) { return a > b; }
    public static boolean gt (final int    a, final short  b) { return a > b; }
    public static boolean gt (final int    a, final int    b) { return a > b; }
    public static boolean gt (final int    a, final long   b) { return a > b; }
    public static boolean gt (final int    a, final float  b) { return a > b; }
    public static boolean gt (final int    a, final double b) { return a > b; }
    public static boolean gt (final long   a, final byte   b) { return a > b; }
    public static boolean gt (final long   a, final char   b) { return a > b; }
    public static boolean gt (final long   a, final short  b) { return a > b; }
    public static boolean gt (final long   a, final int    b) { return a > b; }
    public static boolean gt (final long   a, final long   b) { return a > b; }
    public static boolean gt (final long   a, final float  b) { return a > b; }
    public static boolean gt (final long   a, final double b) { return a > b; }
    public static boolean gt (final float  a, final byte   b) { return a > b; }
    public static boolean gt (final float  a, final char   b) { return a > b; }
    public static boolean gt (final float  a, final short  b) { return a > b; }
    public static boolean gt (final float  a, final int    b) { return a > b; }
    public static boolean gt (final float  a, final long   b) { return a > b; }
    public static boolean gt (final float  a, final float  b) { return a > b; }
    public static boolean gt (final float  a, final double b) { return a > b; }
    public static boolean gt (final double a, final byte   b) { return a > b; }
    public static boolean gt (final double a, final char   b) { return a > b; }
    public static boolean gt (final double a, final short  b) { return a > b; }
    public static boolean gt (final double a, final int    b) { return a > b; }
    public static boolean gt (final double a, final long   b) { return a > b; }
    public static boolean gt (final double a, final float  b) { return a > b; }
    public static boolean gt (final double a, final double b) { return a > b; }

    // ============================ GTE : >= ================================ //
    
    public static boolean gte (final byte   a, final byte   b) { return a >= b; }
    public static boolean gte (final byte   a, final char   b) { return a >= b; }
    public static boolean gte (final byte   a, final short  b) { return a >= b; }
    public static boolean gte (final byte   a, final int    b) { return a >= b; }
    public static boolean gte (final byte   a, final long   b) { return a >= b; }
    public static boolean gte (final byte   a, final float  b) { return a >= b; }
    public static boolean gte (final byte   a, final double b) { return a >= b; }
    public static boolean gte (final char   a, final byte   b) { return a >= b; }
    public static boolean gte (final char   a, final char   b) { return a >= b; }
    public static boolean gte (final char   a, final short  b) { return a >= b; }
    public static boolean gte (final char   a, final int    b) { return a >= b; }
    public static boolean gte (final char   a, final long   b) { return a >= b; }
    public static boolean gte (final char   a, final float  b) { return a >= b; }
    public static boolean gte (final char   a, final double b) { return a >= b; }
    public static boolean gte (final short  a, final byte   b) { return a >= b; }
    public static boolean gte (final short  a, final char   b) { return a >= b; }
    public static boolean gte (final short  a, final short  b) { return a >= b; }
    public static boolean gte (final short  a, final int    b) { return a >= b; }
    public static boolean gte (final short  a, final long   b) { return a >= b; }
    public static boolean gte (final short  a, final float  b) { return a >= b; }
    public static boolean gte (final short  a, final double b) { return a >= b; }
    public static boolean gte (final int    a, final byte   b) { return a >= b; }
    public static boolean gte (final int    a, final char   b) { return a >= b; }
    public static boolean gte (final int    a, final short  b) { return a >= b; }
    public static boolean gte (final int    a, final int    b) { return a >= b; }
    public static boolean gte (final int    a, final long   b) { return a >= b; }
    public static boolean gte (final int    a, final float  b) { return a >= b; }
    public static boolean gte (final int    a, final double b) { return a >= b; }
    public static boolean gte (final long   a, final byte   b) { return a >= b; }
    public static boolean gte (final long   a, final char   b) { return a >= b; }
    public static boolean gte (final long   a, final short  b) { return a >= b; }
    public static boolean gte (final long   a, final int    b) { return a >= b; }
    public static boolean gte (final long   a, final long   b) { return a >= b; }
    public static boolean gte (final long   a, final float  b) { return a >= b; }
    public static boolean gte (final long   a, final double b) { return a >= b; }
    public static boolean gte (final float  a, final byte   b) { return a >= b; }
    public static boolean gte (final float  a, final char   b) { return a >= b; }
    public static boolean gte (final float  a, final short  b) { return a >= b; }
    public static boolean gte (final float  a, final int    b) { return a >= b; }
    public static boolean gte (final float  a, final long   b) { return a >= b; }
    public static boolean gte (final float  a, final float  b) { return a >= b; }
    public static boolean gte (final float  a, final double b) { return a >= b; }
    public static boolean gte (final double a, final byte   b) { return a >= b; }
    public static boolean gte (final double a, final char   b) { return a >= b; }
    public static boolean gte (final double a, final short  b) { return a >= b; }
    public static boolean gte (final double a, final int    b) { return a >= b; }
    public static boolean gte (final double a, final long   b) { return a >= b; }
    public static boolean gte (final double a, final float  b) { return a >= b; }
    public static boolean gte (final double a, final double b) { return a >= b; }
    
    // ============================ EQ : == ================================ //
    
    public static boolean eq  (final boolean a, final boolean b) { return a == b; }
    public static boolean eq  (final byte    a, final byte    b) { return a == b; }
    public static boolean eq  (final byte    a, final char    b) { return a == b; }
    public static boolean eq  (final byte    a, final short   b) { return a == b; }
    public static boolean eq  (final byte    a, final int     b) { return a == b; }
    public static boolean eq  (final byte    a, final long    b) { return a == b; }
    public static boolean eq  (final byte    a, final float   b) { return a == b; }
    public static boolean eq  (final byte    a, final double  b) { return a == b; }
    public static boolean eq  (final char    a, final byte    b) { return a == b; }
    public static boolean eq  (final char    a, final char    b) { return a == b; }
    public static boolean eq  (final char    a, final short   b) { return a == b; }
    public static boolean eq  (final char    a, final int     b) { return a == b; }
    public static boolean eq  (final char    a, final long    b) { return a == b; }
    public static boolean eq  (final char    a, final float   b) { return a == b; }
    public static boolean eq  (final char    a, final double  b) { return a == b; }
    public static boolean eq  (final short   a, final byte    b) { return a == b; }
    public static boolean eq  (final short   a, final char    b) { return a == b; }
    public static boolean eq  (final short   a, final short   b) { return a == b; }
    public static boolean eq  (final short   a, final int     b) { return a == b; }
    public static boolean eq  (final short   a, final long    b) { return a == b; }
    public static boolean eq  (final short   a, final float   b) { return a == b; }
    public static boolean eq  (final short   a, final double  b) { return a == b; }
    public static boolean eq  (final int     a, final byte    b) { return a == b; }
    public static boolean eq  (final int     a, final char    b) { return a == b; }
    public static boolean eq  (final int     a, final short   b) { return a == b; }
    public static boolean eq  (final int     a, final int     b) { return a == b; }
    public static boolean eq  (final int     a, final long    b) { return a == b; }
    public static boolean eq  (final int     a, final float   b) { return a == b; }
    public static boolean eq  (final int     a, final double  b) { return a == b; }
    public static boolean eq  (final long    a, final byte    b) { return a == b; }
    public static boolean eq  (final long    a, final char    b) { return a == b; }
    public static boolean eq  (final long    a, final short   b) { return a == b; }
    public static boolean eq  (final long    a, final int     b) { return a == b; }
    public static boolean eq  (final long    a, final long    b) { return a == b; }
    public static boolean eq  (final long    a, final float   b) { return a == b; }
    public static boolean eq  (final long    a, final double  b) { return a == b; }
    public static boolean eq  (final float   a, final byte    b) { return a == b; }
    public static boolean eq  (final float   a, final char    b) { return a == b; }
    public static boolean eq  (final float   a, final short   b) { return a == b; }
    public static boolean eq  (final float   a, final int     b) { return a == b; }
    public static boolean eq  (final float   a, final long    b) { return a == b; }
    public static boolean eq  (final float   a, final float   b) { return a == b; }
    public static boolean eq  (final float   a, final double  b) { return a == b; }
    public static boolean eq  (final double  a, final byte    b) { return a == b; }
    public static boolean eq  (final double  a, final char    b) { return a == b; }
    public static boolean eq  (final double  a, final short   b) { return a == b; }
    public static boolean eq  (final double  a, final int     b) { return a == b; }
    public static boolean eq  (final double  a, final long    b) { return a == b; }
    public static boolean eq  (final double  a, final float   b) { return a == b; }
    public static boolean eq  (final double  a, final double  b) { return a == b; }
    
    // ============================ NEQ : != ================================ //
    
    public static boolean neq (final boolean a, final boolean b) { return a != b; }
    public static boolean neq (final byte    a, final byte    b) { return a != b; }
    public static boolean neq (final byte    a, final char    b) { return a != b; }
    public static boolean neq (final byte    a, final short   b) { return a != b; }
    public static boolean neq (final byte    a, final int     b) { return a != b; }
    public static boolean neq (final byte    a, final long    b) { return a != b; }
    public static boolean neq (final byte    a, final float   b) { return a != b; }
    public static boolean neq (final byte    a, final double  b) { return a != b; }
    public static boolean neq (final char    a, final byte    b) { return a != b; }
    public static boolean neq (final char    a, final char    b) { return a != b; }
    public static boolean neq (final char    a, final short   b) { return a != b; }
    public static boolean neq (final char    a, final int     b) { return a != b; }
    public static boolean neq (final char    a, final long    b) { return a != b; }
    public static boolean neq (final char    a, final float   b) { return a != b; }
    public static boolean neq (final char    a, final double  b) { return a != b; }
    public static boolean neq (final short   a, final byte    b) { return a != b; }
    public static boolean neq (final short   a, final char    b) { return a != b; }
    public static boolean neq (final short   a, final short   b) { return a != b; }
    public static boolean neq (final short   a, final int     b) { return a != b; }
    public static boolean neq (final short   a, final long    b) { return a != b; }
    public static boolean neq (final short   a, final float   b) { return a != b; }
    public static boolean neq (final short   a, final double  b) { return a != b; }
    public static boolean neq (final int     a, final byte    b) { return a != b; }
    public static boolean neq (final int     a, final char    b) { return a != b; }
    public static boolean neq (final int     a, final short   b) { return a != b; }
    public static boolean neq (final int     a, final int     b) { return a != b; }
    public static boolean neq (final int     a, final long    b) { return a != b; }
    public static boolean neq (final int     a, final float   b) { return a != b; }
    public static boolean neq (final int     a, final double  b) { return a != b; }
    public static boolean neq (final long    a, final byte    b) { return a != b; }
    public static boolean neq (final long    a, final char    b) { return a != b; }
    public static boolean neq (final long    a, final short   b) { return a != b; }
    public static boolean neq (final long    a, final int     b) { return a != b; }
    public static boolean neq (final long    a, final long    b) { return a != b; }
    public static boolean neq (final long    a, final float   b) { return a != b; }
    public static boolean neq (final long    a, final double  b) { return a != b; }
    public static boolean neq (final float   a, final byte    b) { return a != b; }
    public static boolean neq (final float   a, final char    b) { return a != b; }
    public static boolean neq (final float   a, final short   b) { return a != b; }
    public static boolean neq (final float   a, final int     b) { return a != b; }
    public static boolean neq (final float   a, final long    b) { return a != b; }
    public static boolean neq (final float   a, final float   b) { return a != b; }
    public static boolean neq (final float   a, final double  b) { return a != b; }
    public static boolean neq (final double  a, final byte    b) { return a != b; }
    public static boolean neq (final double  a, final char    b) { return a != b; }
    public static boolean neq (final double  a, final short   b) { return a != b; }
    public static boolean neq (final double  a, final int     b) { return a != b; }
    public static boolean neq (final double  a, final long    b) { return a != b; }
    public static boolean neq (final double  a, final float   b) { return a != b; }
    public static boolean neq (final double  a, final double  b) { return a != b; }

    // ============================ INC / DEC ================================ //

    public static byte   inc (final byte   a) { return (byte )(a + byte1 ); }
    public static char   inc (final char   a) { return (char )(a + char1 ); }
    public static short  inc (final short  a) { return (short)(a + short1); }
    public static int    inc (final int    a) { return a + 1;      }
    public static long   inc (final long   a) { return a + 1L;     }
    public static float  inc (final float  a) { return a + 1.0f;   }
    public static double inc (final double a) { return a + 1.0d;   }
     
    public static byte   dec (final byte   a) { return (byte )(a - byte1 ); }
    public static char   dec (final char   a) { return (char )(a - char1 ); }
    public static short  dec (final short  a) { return (short)(a - short1); }
    public static int    dec (final int    a) { return a - 1;   }
    public static long   dec (final long   a) { return a - 1L;     }
    public static float  dec (final float  a) { return a - 1.0f;   }
    public static double dec (final double a) { return a - 1.0d;   }

    // ============================ ISZERO ================================ //
  
    public static boolean isZero (final byte   a) { return a == byte0;  }
    public static boolean isZero (final char   a) { return a == char0;  }
    public static boolean isZero (final short  a) { return a == short0; }
    public static boolean isZero (final int    a) { return a == int0;   }
    public static boolean isZero (final long   a) { return a == 0L;     }
    public static boolean isZero (final float  a) { return a == 0.0f;   }
    public static boolean isZero (final double a) { return a == 0.0d;   }

    // ============================ ISNEG ================================ //

    public static boolean isNeg (final byte   a) { return a < byte0;  } // Implicitly checked
    public static boolean isNeg (final char   a) { return a < char0;  } // Implicitly checked
    public static boolean isNeg (final short  a) { return a < short0; } // Implicitly checked
    public static boolean isNeg (final int    a) { return a < int0;   } // Implicitly checked
    public static boolean isNeg (final long   a) { return a < 0L;     } // Implicitly checked
    public static boolean isNeg (final float  a) { return a < 0.0f;   } // Implicitly checked
    public static boolean isNeg (final double a) { return a < 0.0d;   } // Implicitly checked

    // ============================ ISPOS ================================ //

    public static boolean isPos (final byte   a) { return a > byte0;  } // Implicitly checked
    public static boolean isPos (final char   a) { return a > char0;  } // Implicitly checked
    public static boolean isPos (final short  a) { return a > short0; } // Implicitly checked
    public static boolean isPos (final int    a) { return a > int0;   } // Implicitly checked
    public static boolean isPos (final long   a) { return a > 0L;     } // Implicitly checked
    public static boolean isPos (final float  a) { return a > 0.0f;   } // Implicitly checked
    public static boolean isPos (final double a) { return a > 0.0d;   } // Implicitly checked
  
    // ============================ ADD : + ================================ //
    // "Infectious": uses a promotion of the largest data type passed
    public static short  add (final byte   a, final byte   b) { return (short)(a + b); } // Implicitly checked
    public static int    add (final byte   a, final char   b) { return         a + b;  } // Implicitly checked
    public static int    add (final byte   a, final short  b) { return         a + b;  } // Implicitly checked
    public static long   add (final byte   a, final int    b) { return         a + b;  } // Implicitly checked
    public static long   add (final byte   a, final long   b) { return         a + b;  }
    public static double add (final byte   a, final float  b) { return         a + b;  } // Implicitly checked
    public static double add (final byte   a, final double b) { return         a + b;  }
    public static int    add (final char   a, final byte   b) { return         a + b;  } // Implicitly checked
    public static int    add (final char   a, final char   b) { return         a + b;  } // Implicitly checked
    public static int    add (final char   a, final short  b) { return         a + b;  } // Implicitly checked
    public static long   add (final char   a, final int    b) { return         a + b;  } // Implicitly checked
    public static long   add (final char   a, final long   b) { return         a + b;  }
    public static double add (final char   a, final float  b) { return         a + b;  } // Implicitly checked
    public static double add (final char   a, final double b) { return         a + b;  }
    public static int    add (final short  a, final byte   b) { return         a + b;  } // Implicitly checked
    public static int    add (final short  a, final char   b) { return         a + b;  } // Implicitly checked
    public static int    add (final short  a, final short  b) { return         a + b;  } // Implicitly checked
    public static long   add (final short  a, final int    b) { return         a + b;  } // Implicitly checked
    public static long   add (final short  a, final long   b) { return         a + b;  }
    public static double add (final short  a, final float  b) { return         a + b;  } // Implicitly checked
    public static double add (final short  a, final double b) { return         a + b;  }
    public static long   add (final int    a, final byte   b) { return         a + b;  } // Implicitly checked
    public static long   add (final int    a, final char   b) { return         a + b;  } // Implicitly checked
    public static long   add (final int    a, final short  b) { return         a + b;  } // Implicitly checked
    public static long   add (final int    a, final int    b) { return         a + b;  } // Implicitly checked
    public static long   add (final int    a, final long   b) { return         a + b;  }
    public static double add (final int    a, final float  b) { return         a + b;  } // Implicitly checked
    public static double add (final int    a, final double b) { return         a + b;  }
    public static long   add (final long   a, final byte   b) { return         a + b;  }
    public static long   add (final long   a, final char   b) { return         a + b;  }
    public static long   add (final long   a, final short  b) { return         a + b;  }
    public static long   add (final long   a, final int    b) { return         a + b;  }
    public static long   add (final long   a, final long   b) { return         a + b;  }
    public static double add (final long   a, final float  b) { return         a + b;  }
    public static double add (final long   a, final double b) { return         a + b;  }
    public static double add (final float  a, final byte   b) { return         a + b;  } // Implicitly checked
    public static double add (final float  a, final char   b) { return         a + b;  } // Implicitly checked
    public static double add (final float  a, final short  b) { return         a + b;  } // Implicitly checked
    public static double add (final float  a, final int    b) { return         a + b;  } // Implicitly checked
    public static double add (final float  a, final long   b) { return         a + b;  }
    public static double add (final float  a, final float  b) { return         a + b;  } // Implicitly checked
    public static double add (final float  a, final double b) { return         a + b;  }
    public static double add (final double a, final byte   b) { return         a + b;  }
    public static double add (final double a, final char   b) { return         a + b;  }
    public static double add (final double a, final short  b) { return         a + b;  }
    public static double add (final double a, final int    b) { return         a + b;  }
    public static double add (final double a, final long   b) { return         a + b;  }
    public static double add (final double a, final float  b) { return         a + b;  }
    public static double add (final double a, final double b) { return         a + b;  }
 
    // ============================ SUBTRACT : - ================================ //
    // "Infectious": uses the largest data type passed

    public static short  subtract (final byte   a, final byte   b) { return (short)(a - b); } // Implicitly checked
    public static int    subtract (final byte   a, final char   b) { return         a - b;  } // Implicitly checked
    public static int    subtract (final byte   a, final short  b) { return         a - b;  } // Implicitly checked
    public static long   subtract (final byte   a, final int    b) { return         a - b;  } // Implicitly checked
    public static long   subtract (final byte   a, final long   b) { return         a - b;  }
    public static double subtract (final byte   a, final float  b) { return         a - b;  } // Implicitly checked
    public static double subtract (final byte   a, final double b) { return         a - b;  }
    public static int    subtract (final char   a, final byte   b) { return         a - b;  } // Implicitly checked
    public static int    subtract (final char   a, final char   b) { return         a - b;  } // Implicitly checked
    public static int    subtract (final char   a, final short  b) { return         a - b;  } // Implicitly checked
    public static long   subtract (final char   a, final int    b) { return         a - b;  } // Implicitly checked
    public static long   subtract (final char   a, final long   b) { return         a - b;  }
    public static double subtract (final char   a, final float  b) { return         a - b;  } // Implicitly checked
    public static double subtract (final char   a, final double b) { return         a - b;  }
    public static int    subtract (final short  a, final byte   b) { return         a - b;  } // Implicitly checked
    public static int    subtract (final short  a, final char   b) { return         a - b;  } // Implicitly checked
    public static int    subtract (final short  a, final short  b) { return         a - b;  } // Implicitly checked
    public static long   subtract (final short  a, final int    b) { return         a - b;  } // Implicitly checked
    public static long   subtract (final short  a, final long   b) { return         a - b;  }
    public static double subtract (final short  a, final float  b) { return         a - b;  } // Implicitly checked
    public static double subtract (final short  a, final double b) { return         a - b;  }
    public static long   subtract (final int    a, final byte   b) { return         a - b;  } // Implicitly checked
    public static long   subtract (final int    a, final char   b) { return         a - b;  } // Implicitly checked
    public static long   subtract (final int    a, final short  b) { return         a - b;  } // Implicitly checked
    public static long   subtract (final int    a, final int    b) { return         a - b;  } // Implicitly checked
    public static long   subtract (final int    a, final long   b) { return         a - b;  }
    public static double subtract (final int    a, final float  b) { return         a - b;  } // Implicitly checked
    public static double subtract (final int    a, final double b) { return         a - b;  }
    public static long   subtract (final long   a, final byte   b) { return         a - b;  }
    public static long   subtract (final long   a, final char   b) { return         a - b;  }
    public static long   subtract (final long   a, final short  b) { return         a - b;  }
    public static long   subtract (final long   a, final int    b) { return         a - b;  }
    public static long   subtract (final long   a, final long   b) { return         a - b;  }
    public static double subtract (final long   a, final float  b) { return         a - b;  }
    public static double subtract (final long   a, final double b) { return         a - b;  }
    public static double subtract (final float  a, final byte   b) { return         a - b;  } // Implicitly checked
    public static double subtract (final float  a, final char   b) { return         a - b;  } // Implicitly checked
    public static double subtract (final float  a, final short  b) { return         a - b;  } // Implicitly checked
    public static double subtract (final float  a, final int    b) { return         a - b;  } // Implicitly checked
    public static double subtract (final float  a, final long   b) { return         a - b;  }
    public static double subtract (final float  a, final float  b) { return         a - b;  } // Implicitly checked
    public static double subtract (final float  a, final double b) { return         a - b;  }
    public static double subtract (final double a, final byte   b) { return         a - b;  }
    public static double subtract (final double a, final char   b) { return         a - b;  }
    public static double subtract (final double a, final short  b) { return         a - b;  }
    public static double subtract (final double a, final int    b) { return         a - b;  }
    public static double subtract (final double a, final long   b) { return         a - b;  }
    public static double subtract (final double a, final float  b) { return         a - b;  }
    public static double subtract (final double a, final double b) { return         a - b;  }
    
    // ============================ NEGATE : - ================================ //

    public static byte   negate   (final byte   a) { return (byte )-a; }
    public static char   negate   (final char   a) { return (char )-a; }
    public static short  negate   (final short  a) { return (short)-a; }
    public static int    negate   (final int    a) { return -a; }
    public static long   negate   (final long   a) { return -a; }
    public static float  negate   (final float  a) { return -a; }
    public static double negate   (final double a) { return -a; }

    // ============================ MULTIPLY : * ================================ //
    // "Infectious": uses the largest data type passed

    public static short  multiply (final byte   a, final byte   b) { return (short)(a * b); } // Implicitly checked
    public static int    multiply (final byte   a, final char   b) { return a * b; } // Implicitly checked
    public static int    multiply (final byte   a, final short  b) { return a * b; } // Implicitly checked
    public static long   multiply (final byte   a, final int    b) { return a * b; } // Implicitly checked
    public static long   multiply (final byte   a, final long   b) { return a * b; } // ->BigInteger
    public static double multiply (final byte   a, final float  b) { return a * b; } // 
    public static double multiply (final byte   a, final double b) { return a * b; }
    public static long   multiply (final char   a, final byte   b) { return a * b; }
    public static long   multiply (final char   a, final char   b) { return a * b; }
    public static long   multiply (final char   a, final short  b) { return a * b; }
    public static long   multiply (final char   a, final int    b) { return a * b; }
    public static long   multiply (final char   a, final long   b) { return a * b; }
    public static double multiply (final char   a, final float  b) { return a * b; }
    public static double multiply (final char   a, final double b) { return a * b; }
    public static long   multiply (final short  a, final byte   b) { return a * b; }
    public static long   multiply (final short  a, final char   b) { return a * b; }
    public static long   multiply (final short  a, final short  b) { return a * b; }
    public static long   multiply (final short  a, final int    b) { return a * b; }
    public static long   multiply (final short  a, final long   b) { return a * b; }
    public static double multiply (final short  a, final float  b) { return a * b; }
    public static double multiply (final short  a, final double b) { return a * b; }
    public static long   multiply (final int    a, final byte   b) { return a * b; }
    public static long   multiply (final int    a, final char   b) { return a * b; }
    public static long   multiply (final int    a, final short  b) { return a * b; }
    public static long   multiply (final int    a, final int    b) { return a * b; }
    public static long   multiply (final int    a, final long   b) { return a * b; }
    public static double multiply (final int    a, final float  b) { return a * b; }
    public static double multiply (final int    a, final double b) { return a * b; }
    public static long   multiply (final long   a, final byte   b) { return a * b; }
    public static long   multiply (final long   a, final char   b) { return a * b; }
    public static long   multiply (final long   a, final short  b) { return a * b; }
    public static long   multiply (final long   a, final int    b) { return a * b; }
    public static long   multiply (final long   a, final long   b) { return a * b; }
    public static double multiply (final long   a, final float  b) { return a * b; }
    public static double multiply (final long   a, final double b) { return a * b; }
    public static double multiply (final float  a, final byte   b) { return a * b; }
    public static double multiply (final float  a, final char   b) { return a * b; }
    public static double multiply (final float  a, final short  b) { return a * b; }
    public static double multiply (final float  a, final int    b) { return a * b; }
    public static double multiply (final float  a, final long   b) { return a * b; }
    public static double multiply (final float  a, final float  b) { return a * b; }
    public static double multiply (final float  a, final double b) { return a * b; }
    public static double multiply (final double a, final byte   b) { return a * b; }
    public static double multiply (final double a, final char   b) { return a * b; }
    public static double multiply (final double a, final short  b) { return a * b; }
    public static double multiply (final double a, final int    b) { return a * b; }
    public static double multiply (final double a, final long   b) { return a * b; }
    public static double multiply (final double a, final float  b) { return a * b; }
    public static double multiply (final double a, final double b) { return a * b; }
 
    // ============================ DIVIDE : / ================================ //
    // "Infectious": uses the largest data type passed

    public static double divide   (final byte   a, final byte   b) { return a / b; }
    public static double divide   (final byte   a, final char   b) { return a / b; }
    public static double divide   (final byte   a, final short  b) { return a / b; }
    public static double divide   (final byte   a, final int    b) { return a / b; }
    public static double divide   (final byte   a, final long   b) { return a / b; }
    public static double divide   (final byte   a, final float  b) { return a / b; }
    public static double divide   (final byte   a, final double b) { return a / b; }
    public static double divide   (final char   a, final byte   b) { return a / b; }
    public static double divide   (final char   a, final char   b) { return a / b; }
    public static double divide   (final char   a, final short  b) { return a / b; }
    public static double divide   (final char   a, final int    b) { return a / b; }
    public static double divide   (final char   a, final long   b) { return a / b; }
    public static double divide   (final char   a, final float  b) { return a / b; }
    public static double divide   (final char   a, final double b) { return a / b; }
    public static double divide   (final short  a, final byte   b) { return a / b; }
    public static double divide   (final short  a, final char   b) { return a / b; }
    public static double divide   (final short  a, final short  b) { return a / b; }
    public static double divide   (final short  a, final int    b) { return a / b; }
    public static double divide   (final short  a, final long   b) { return a / b; }
    public static double divide   (final short  a, final float  b) { return a / b; }
    public static double divide   (final short  a, final double b) { return a / b; }
    public static double divide   (final int    a, final byte   b) { return a / b; }
    public static double divide   (final int    a, final char   b) { return a / b; }
    public static double divide   (final int    a, final short  b) { return a / b; }
    public static double divide   (final int    a, final int    b) { return a / b; }
    public static double divide   (final int    a, final long   b) { return a / b; }
    public static double divide   (final int    a, final float  b) { return a / b; }
    public static double divide   (final int    a, final double b) { return a / b; }
    public static double divide   (final long   a, final byte   b) { return a / b; }
    public static double divide   (final long   a, final char   b) { return a / b; }
    public static double divide   (final long   a, final short  b) { return a / b; }
    public static double divide   (final long   a, final int    b) { return a / b; }
    public static double divide   (final long   a, final long   b) { return a / b; }
    public static double divide   (final long   a, final float  b) { return a / b; }
    public static double divide   (final long   a, final double b) { return a / b; }
    public static double divide   (final float  a, final byte   b) { return a / b; }
    public static double divide   (final float  a, final char   b) { return a / b; }
    public static double divide   (final float  a, final short  b) { return a / b; }
    public static double divide   (final float  a, final int    b) { return a / b; }
    public static double divide   (final float  a, final long   b) { return a / b; }
    public static double divide   (final float  a, final float  b) { return a / b; }
    public static double divide   (final float  a, final double b) { return a / b; }
    public static double divide   (final double a, final byte   b) { return a / b; }
    public static double divide   (final double a, final char   b) { return a / b; }
    public static double divide   (final double a, final short  b) { return a / b; }
    public static double divide   (final double a, final int    b) { return a / b; }
    public static double divide   (final double a, final long   b) { return a / b; }
    public static double divide   (final double a, final float  b) { return a / b; }
    public static double divide   (final double a, final double b) { return a / b; }
    
    // ============================ MAX ================================ //
    // "Infectious": uses the largest data type passed

    public static byte   max (final byte   a, final byte   b) { return (a < b) ? b : a; }
    public static char   max (final byte   a, final char   b) { return (char)((a < b) ? b : a); }
    public static short  max (final byte   a, final short  b) { return (a < b) ? b : a; }
    public static int    max (final byte   a, final int    b) { return (a < b) ? b : a; }
    public static long   max (final byte   a, final long   b) { return (a < b) ? b : a; }
    public static float  max (final byte   a, final float  b) { return (a < b) ? b : a; }
    public static double max (final byte   a, final double b) { return (a < b) ? b : a; }
    public static char   max (final char   a, final byte   b) { return (char)((a < b) ? b : a); }
    public static char   max (final char   a, final char   b) { return (a < b) ? b : a; }
    public static short  max (final char   a, final short  b) { return (short)((a < b) ? b : a); }
    public static int    max (final char   a, final int    b) { return (a < b) ? b : a; }
    public static long   max (final char   a, final long   b) { return (a < b) ? b : a; }
    public static float  max (final char   a, final float  b) { return (a < b) ? b : a; }
    public static double max (final char   a, final double b) { return (a < b) ? b : a; }
    public static short  max (final short  a, final byte   b) { return (a < b) ? b : a; }
    public static short  max (final short  a, final char   b) { return (short)((a < b) ? b : a); }
    public static short  max (final short  a, final short  b) { return (a < b) ? b : a; }
    public static int    max (final short  a, final int    b) { return (a < b) ? b : a; }
    public static long   max (final short  a, final long   b) { return (a < b) ? b : a; }
    public static float  max (final short  a, final float  b) { return (a < b) ? b : a; }
    public static double max (final short  a, final double b) { return (a < b) ? b : a; }
    public static int    max (final int    a, final byte   b) { return (a < b) ? b : a; }
    public static int    max (final int    a, final char   b) { return (a < b) ? b : a; }
    public static int    max (final int    a, final short  b) { return (a < b) ? b : a; }
    public static int    max (final int    a, final int    b) { return Math.max(a, b);  }
    public static long   max (final int    a, final long   b) { return (a < b) ? b : a; }
    public static float  max (final int    a, final float  b) { return (a < b) ? b : a; }
    public static double max (final int    a, final double b) { return (a < b) ? b : a; }
    public static long   max (final long   a, final byte   b) { return (a < b) ? b : a; }
    public static long   max (final long   a, final char   b) { return (a < b) ? b : a; }
    public static long   max (final long   a, final short  b) { return (a < b) ? b : a; }
    public static long   max (final long   a, final int    b) { return (a < b) ? b : a; }
    public static long   max (final long   a, final long   b) { return Math.max(a, b);  }
    public static float  max (final long   a, final float  b) { return (a < b) ? b : a; }
    public static double max (final long   a, final double b) { return (a < b) ? b : a; }
    public static float  max (final float  a, final byte   b) { return (a < b) ? b : a; }
    public static float  max (final float  a, final char   b) { return (a < b) ? b : a; }
    public static float  max (final float  a, final short  b) { return (a < b) ? b : a; }
    public static float  max (final float  a, final int    b) { return (a < b) ? b : a; }
    public static float  max (final float  a, final long   b) { return (a < b) ? b : a; }
    public static float  max (final float  a, final float  b) { return Math.max(a, b);  }
    public static double max (final float  a, final double b) { return (a < b) ? b : a; }
    public static double max (final double a, final byte   b) { return (a < b) ? b : a; }
    public static double max (final double a, final char   b) { return (a < b) ? b : a; }
    public static double max (final double a, final short  b) { return (a < b) ? b : a; }
    public static double max (final double a, final int    b) { return (a < b) ? b : a; }
    public static double max (final double a, final long   b) { return (a < b) ? b : a; }
    public static double max (final double a, final float  b) { return (a < b) ? b : a; }
    public static double max (final double a, final double b) { return Math.max(a, b);  }

    // ============================ MIN ================================ //
    // "Infectious": uses the largest data type passed

    public static byte   min (final byte   a, final byte   b) { return (a > b) ? b : a; }
    public static char   min (final byte   a, final char   b) { return (char)((a > b) ? b : a); }
    public static short  min (final byte   a, final short  b) { return (a > b) ? b : a; }
    public static int    min (final byte   a, final int    b) { return (a > b) ? b : a; }
    public static long   min (final byte   a, final long   b) { return (a > b) ? b : a; }
    public static float  min (final byte   a, final float  b) { return (a > b) ? b : a; }
    public static double min (final byte   a, final double b) { return (a > b) ? b : a; }
    public static char   min (final char   a, final byte   b) { return (char)((a > b) ? b : a); }
    public static char   min (final char   a, final char   b) { return (a > b) ? b : a; }
    public static short  min (final char   a, final short  b) { return (short)((a > b) ? b : a); }
    public static int    min (final char   a, final int    b) { return (a > b) ? b : a; }
    public static long   min (final char   a, final long   b) { return (a > b) ? b : a; }
    public static float  min (final char   a, final float  b) { return (a > b) ? b : a; }
    public static double min (final char   a, final double b) { return (a > b) ? b : a; }
    public static short  min (final short  a, final byte   b) { return (a > b) ? b : a; }
    public static short  min (final short  a, final char   b) { return (short)((a > b) ? b : a); }
    public static short  min (final short  a, final short  b) { return (a > b) ? b : a; }
    public static int    min (final short  a, final int    b) { return (a > b) ? b : a; }
    public static long   min (final short  a, final long   b) { return (a > b) ? b : a; }
    public static float  min (final short  a, final float  b) { return (a > b) ? b : a; }
    public static double min (final short  a, final double b) { return (a > b) ? b : a; }
    public static int    min (final int    a, final byte   b) { return (a > b) ? b : a; }
    public static int    min (final int    a, final char   b) { return (a > b) ? b : a; }
    public static int    min (final int    a, final short  b) { return (a > b) ? b : a; }
    // Intrinsic; maybe the others could be acclerated in the same way?
    // TODO maybe use if-optimization?
    public static int    min (final int    a, final int    b) { return Math.min(a, b);  }
    public static long   min (final int    a, final long   b) { return (a > b) ? b : a; }
    public static float  min (final int    a, final float  b) { return (a > b) ? b : a; }
    public static double min (final int    a, final double b) { return (a > b) ? b : a; }
    public static long   min (final long   a, final byte   b) { return (a > b) ? b : a; }
    public static long   min (final long   a, final char   b) { return (a > b) ? b : a; }
    public static long   min (final long   a, final short  b) { return (a > b) ? b : a; }
    public static long   min (final long   a, final int    b) { return (a > b) ? b : a; }
    public static long   min (final long   a, final long   b) { return Math.min(a, b);  }
    public static float  min (final long   a, final float  b) { return (a > b) ? b : a; }
    public static double min (final long   a, final double b) { return (a > b) ? b : a; }
    public static float  min (final float  a, final byte   b) { return (a > b) ? b : a; }
    public static float  min (final float  a, final char   b) { return (a > b) ? b : a; }
    public static float  min (final float  a, final short  b) { return (a > b) ? b : a; }
    public static float  min (final float  a, final int    b) { return (a > b) ? b : a; }
    public static float  min (final float  a, final long   b) { return (a > b) ? b : a; }
    public static float  min (final float  a, final float  b) { return Math.min(a, b);  }
    public static double min (final float  a, final double b) { return (a > b) ? b : a; }
    public static double min (final double a, final byte   b) { return (a > b) ? b : a; }
    public static double min (final double a, final char   b) { return (a > b) ? b : a; }
    public static double min (final double a, final short  b) { return (a > b) ? b : a; }
    public static double min (final double a, final int    b) { return (a > b) ? b : a; }
    public static double min (final double a, final long   b) { return (a > b) ? b : a; }
    public static double min (final double a, final float  b) { return (a > b) ? b : a; }
    public static double min (final double a, final double b) { return Math.min(a, b);  }

    // ============================ REM ================================ //

    public static long   rem (final long   a, final long   b) { return a % b; }

    public static long uncheckedLongCast (final char x) { return (long)x; }
}
