package quantum.core;
// TAKEN FROM ztellman/primitive-math AND EXPANDED

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

    public static boolean isTrue (boolean a           ) { return a == true; }
    public static boolean isFalse(boolean a           ) { return a == false; }
    public static boolean isNil  (Object  a           ) { return a == null; }
    public static boolean and    (boolean a, boolean b) { return a && b; }
    public static boolean or     (boolean a, boolean b) { return a || b; }
    public static boolean not    (boolean a           ) { return !a; }
    public static boolean xor    (boolean a, boolean b) { return (a || b) && !(a && b); }

    // ============================ BIT OPERATIONS ================================ //
    // Apparently '&' is fundamentally an int operation

    public static byte  bitAnd (byte   a, byte   b) { return (byte) (a & b); }
    public static char  bitAnd (byte   a, char   b) { return (char) (a & b); }
    public static short bitAnd (byte   a, short  b) { return (short)(a & b); }
    public static int   bitAnd (byte   a, int    b) { return a & b; }
    public static long  bitAnd (byte   a, long   b) { return a & b; }
    public static char  bitAnd (char   a, byte   b) { return (char) (a & b); }
    public static char  bitAnd (char   a, char   b) { return (char) (a & b); }
    public static short bitAnd (char   a, short  b) { return (short)(a & b); }
    public static int   bitAnd (char   a, int    b) { return a & b; }
    public static long  bitAnd (char   a, long   b) { return a & b; }
    public static short bitAnd (short  a, byte   b) { return (short)(a & b); }
    public static short bitAnd (short  a, char   b) { return (short)(a & b); }
    public static short bitAnd (short  a, short  b) { return (short)(a & b); }
    public static int   bitAnd (short  a, int    b) { return a & b; }
    public static long  bitAnd (short  a, long   b) { return a & b; }
    public static int   bitAnd (int    a, byte   b) { return a & b; }
    public static int   bitAnd (int    a, char   b) { return a & b; }
    public static int   bitAnd (int    a, short  b) { return a & b; }
    public static int   bitAnd (int    a, int    b) { return a & b; }
    public static long  bitAnd (int    a, long   b) { return a & b; }
    public static long  bitAnd (long   a, byte   b) { return a & b; }
    public static long  bitAnd (long   a, char   b) { return a & b; }
    public static long  bitAnd (long   a, short  b) { return a & b; }
    public static long  bitAnd (long   a, int    b) { return a & b; }
    public static long  bitAnd (long   a, long   b) { return a & b; }
    
    public static long bitOr             (long a, long b) { return a |   b; }
    public static long bitXor            (long a, long b) { return a ^   b; }
    public static long bitNot            (long a        ) { return ~a; }
    public static long shiftLeft         (long a, long b) { return a <<  b; }
    public static long shiftRight        (long a, long b) { return a >>  b; }
    public static long unsignedShiftRight(long a, long b) { return a >>> b; }
    public static int  unsignedShiftRight(int  a, long b) { return a >>> b; }

    // Because "more than one matching method"
    public static short reverseShort(short x) {
        return (short) ((x << 8)
                        | ((char) x >>> 8));
    }

    public static int reverseInt(int x) {
        return (  (x << 24)
                | ((x & 0x0000ff00) <<  8)
                | ((x & 0x00ff0000) >>> 8)
                | (x >>> 24));
    }

    public static long reverseLong(long x) {
        return (((long) reverseInt((int)x) << 32)
                | ((long) reverseInt((int)(x >>> 32)) & 0xffffffffL));
    }

    // ============================ LT : < ================================ //

    public static boolean lt (byte   a, byte   b) { return a < b; }
    public static boolean lt (byte   a, char   b) { return a < b; }
    public static boolean lt (byte   a, short  b) { return a < b; }
    public static boolean lt (byte   a, int    b) { return a < b; }
    public static boolean lt (byte   a, long   b) { return a < b; }
    public static boolean lt (byte   a, float  b) { return a < b; }
    public static boolean lt (byte   a, double b) { return a < b; }
    public static boolean lt (char   a, byte   b) { return a < b; }
    public static boolean lt (char   a, char   b) { return a < b; }
    public static boolean lt (char   a, short  b) { return a < b; }
    public static boolean lt (char   a, int    b) { return a < b; }
    public static boolean lt (char   a, long   b) { return a < b; }
    public static boolean lt (char   a, float  b) { return a < b; }
    public static boolean lt (char   a, double b) { return a < b; }
    public static boolean lt (short  a, byte   b) { return a < b; }
    public static boolean lt (short  a, char   b) { return a < b; }
    public static boolean lt (short  a, short  b) { return a < b; }
    public static boolean lt (short  a, int    b) { return a < b; }
    public static boolean lt (short  a, long   b) { return a < b; }
    public static boolean lt (short  a, float  b) { return a < b; }
    public static boolean lt (short  a, double b) { return a < b; }
    public static boolean lt (int    a, byte   b) { return a < b; }
    public static boolean lt (int    a, char   b) { return a < b; }
    public static boolean lt (int    a, short  b) { return a < b; }
    public static boolean lt (int    a, int    b) { return a < b; }
    public static boolean lt (int    a, long   b) { return a < b; }
    public static boolean lt (int    a, float  b) { return a < b; }
    public static boolean lt (int    a, double b) { return a < b; }
    public static boolean lt (long   a, byte   b) { return a < b; }
    public static boolean lt (long   a, char   b) { return a < b; }
    public static boolean lt (long   a, short  b) { return a < b; }
    public static boolean lt (long   a, int    b) { return a < b; }
    public static boolean lt (long   a, long   b) { return a < b; }
    public static boolean lt (long   a, float  b) { return a < b; }
    public static boolean lt (long   a, double b) { return a < b; }
    public static boolean lt (float  a, byte   b) { return a < b; }
    public static boolean lt (float  a, char   b) { return a < b; }
    public static boolean lt (float  a, short  b) { return a < b; }
    public static boolean lt (float  a, int    b) { return a < b; }
    public static boolean lt (float  a, long   b) { return a < b; }
    public static boolean lt (float  a, float  b) { return a < b; }
    public static boolean lt (float  a, double b) { return a < b; }
    public static boolean lt (double a, byte   b) { return a < b; }
    public static boolean lt (double a, char   b) { return a < b; }
    public static boolean lt (double a, short  b) { return a < b; }
    public static boolean lt (double a, int    b) { return a < b; }
    public static boolean lt (double a, long   b) { return a < b; }
    public static boolean lt (double a, float  b) { return a < b; }
    public static boolean lt (double a, double b) { return a < b; }

    // ============================ LTE : <= ================================ //

    public static boolean lte (byte   a, byte   b) { return a <= b; }
    public static boolean lte (byte   a, char   b) { return a <= b; }
    public static boolean lte (byte   a, short  b) { return a <= b; }
    public static boolean lte (byte   a, int    b) { return a <= b; }
    public static boolean lte (byte   a, long   b) { return a <= b; }
    public static boolean lte (byte   a, float  b) { return a <= b; }
    public static boolean lte (byte   a, double b) { return a <= b; }
    public static boolean lte (char   a, byte   b) { return a <= b; }
    public static boolean lte (char   a, char   b) { return a <= b; }
    public static boolean lte (char   a, short  b) { return a <= b; }
    public static boolean lte (char   a, int    b) { return a <= b; }
    public static boolean lte (char   a, long   b) { return a <= b; }
    public static boolean lte (char   a, float  b) { return a <= b; }
    public static boolean lte (char   a, double b) { return a <= b; }
    public static boolean lte (short  a, byte   b) { return a <= b; }
    public static boolean lte (short  a, char   b) { return a <= b; }
    public static boolean lte (short  a, short  b) { return a <= b; }
    public static boolean lte (short  a, int    b) { return a <= b; }
    public static boolean lte (short  a, long   b) { return a <= b; }
    public static boolean lte (short  a, float  b) { return a <= b; }
    public static boolean lte (short  a, double b) { return a <= b; }
    public static boolean lte (int    a, byte   b) { return a <= b; }
    public static boolean lte (int    a, char   b) { return a <= b; }
    public static boolean lte (int    a, short  b) { return a <= b; }
    public static boolean lte (int    a, int    b) { return a <= b; }
    public static boolean lte (int    a, long   b) { return a <= b; }
    public static boolean lte (int    a, float  b) { return a <= b; }
    public static boolean lte (int    a, double b) { return a <= b; }
    public static boolean lte (long   a, byte   b) { return a <= b; }
    public static boolean lte (long   a, char   b) { return a <= b; }
    public static boolean lte (long   a, short  b) { return a <= b; }
    public static boolean lte (long   a, int    b) { return a <= b; }
    public static boolean lte (long   a, long   b) { return a <= b; }
    public static boolean lte (long   a, float  b) { return a <= b; }
    public static boolean lte (long   a, double b) { return a <= b; }
    public static boolean lte (float  a, byte   b) { return a <= b; }
    public static boolean lte (float  a, char   b) { return a <= b; }
    public static boolean lte (float  a, short  b) { return a <= b; }
    public static boolean lte (float  a, int    b) { return a <= b; }
    public static boolean lte (float  a, long   b) { return a <= b; }
    public static boolean lte (float  a, float  b) { return a <= b; }
    public static boolean lte (float  a, double b) { return a <= b; }
    public static boolean lte (double a, byte   b) { return a <= b; }
    public static boolean lte (double a, char   b) { return a <= b; }
    public static boolean lte (double a, short  b) { return a <= b; }
    public static boolean lte (double a, int    b) { return a <= b; }
    public static boolean lte (double a, long   b) { return a <= b; }
    public static boolean lte (double a, float  b) { return a <= b; }
    public static boolean lte (double a, double b) { return a <= b; }

    // ============================ GT : > ================================ //

    public static boolean gt (byte   a, byte   b) { return a > b; }
    public static boolean gt (byte   a, char   b) { return a > b; }
    public static boolean gt (byte   a, short  b) { return a > b; }
    public static boolean gt (byte   a, int    b) { return a > b; }
    public static boolean gt (byte   a, long   b) { return a > b; }
    public static boolean gt (byte   a, float  b) { return a > b; }
    public static boolean gt (byte   a, double b) { return a > b; }
    public static boolean gt (char   a, byte   b) { return a > b; }
    public static boolean gt (char   a, char   b) { return a > b; }
    public static boolean gt (char   a, short  b) { return a > b; }
    public static boolean gt (char   a, int    b) { return a > b; }
    public static boolean gt (char   a, long   b) { return a > b; }
    public static boolean gt (char   a, float  b) { return a > b; }
    public static boolean gt (char   a, double b) { return a > b; }
    public static boolean gt (short  a, byte   b) { return a > b; }
    public static boolean gt (short  a, char   b) { return a > b; }
    public static boolean gt (short  a, short  b) { return a > b; }
    public static boolean gt (short  a, int    b) { return a > b; }
    public static boolean gt (short  a, long   b) { return a > b; }
    public static boolean gt (short  a, float  b) { return a > b; }
    public static boolean gt (short  a, double b) { return a > b; }
    public static boolean gt (int    a, byte   b) { return a > b; }
    public static boolean gt (int    a, char   b) { return a > b; }
    public static boolean gt (int    a, short  b) { return a > b; }
    public static boolean gt (int    a, int    b) { return a > b; }
    public static boolean gt (int    a, long   b) { return a > b; }
    public static boolean gt (int    a, float  b) { return a > b; }
    public static boolean gt (int    a, double b) { return a > b; }
    public static boolean gt (long   a, byte   b) { return a > b; }
    public static boolean gt (long   a, char   b) { return a > b; }
    public static boolean gt (long   a, short  b) { return a > b; }
    public static boolean gt (long   a, int    b) { return a > b; }
    public static boolean gt (long   a, long   b) { return a > b; }
    public static boolean gt (long   a, float  b) { return a > b; }
    public static boolean gt (long   a, double b) { return a > b; }
    public static boolean gt (float  a, byte   b) { return a > b; }
    public static boolean gt (float  a, char   b) { return a > b; }
    public static boolean gt (float  a, short  b) { return a > b; }
    public static boolean gt (float  a, int    b) { return a > b; }
    public static boolean gt (float  a, long   b) { return a > b; }
    public static boolean gt (float  a, float  b) { return a > b; }
    public static boolean gt (float  a, double b) { return a > b; }
    public static boolean gt (double a, byte   b) { return a > b; }
    public static boolean gt (double a, char   b) { return a > b; }
    public static boolean gt (double a, short  b) { return a > b; }
    public static boolean gt (double a, int    b) { return a > b; }
    public static boolean gt (double a, long   b) { return a > b; }
    public static boolean gt (double a, float  b) { return a > b; }
    public static boolean gt (double a, double b) { return a > b; }

    // ============================ GTE : >= ================================ //
    
    public static boolean gte (byte   a, byte   b) { return a >= b; }
    public static boolean gte (byte   a, char   b) { return a >= b; }
    public static boolean gte (byte   a, short  b) { return a >= b; }
    public static boolean gte (byte   a, int    b) { return a >= b; }
    public static boolean gte (byte   a, long   b) { return a >= b; }
    public static boolean gte (byte   a, float  b) { return a >= b; }
    public static boolean gte (byte   a, double b) { return a >= b; }
    public static boolean gte (char   a, byte   b) { return a >= b; }
    public static boolean gte (char   a, char   b) { return a >= b; }
    public static boolean gte (char   a, short  b) { return a >= b; }
    public static boolean gte (char   a, int    b) { return a >= b; }
    public static boolean gte (char   a, long   b) { return a >= b; }
    public static boolean gte (char   a, float  b) { return a >= b; }
    public static boolean gte (char   a, double b) { return a >= b; }
    public static boolean gte (short  a, byte   b) { return a >= b; }
    public static boolean gte (short  a, char   b) { return a >= b; }
    public static boolean gte (short  a, short  b) { return a >= b; }
    public static boolean gte (short  a, int    b) { return a >= b; }
    public static boolean gte (short  a, long   b) { return a >= b; }
    public static boolean gte (short  a, float  b) { return a >= b; }
    public static boolean gte (short  a, double b) { return a >= b; }
    public static boolean gte (int    a, byte   b) { return a >= b; }
    public static boolean gte (int    a, char   b) { return a >= b; }
    public static boolean gte (int    a, short  b) { return a >= b; }
    public static boolean gte (int    a, int    b) { return a >= b; }
    public static boolean gte (int    a, long   b) { return a >= b; }
    public static boolean gte (int    a, float  b) { return a >= b; }
    public static boolean gte (int    a, double b) { return a >= b; }
    public static boolean gte (long   a, byte   b) { return a >= b; }
    public static boolean gte (long   a, char   b) { return a >= b; }
    public static boolean gte (long   a, short  b) { return a >= b; }
    public static boolean gte (long   a, int    b) { return a >= b; }
    public static boolean gte (long   a, long   b) { return a >= b; }
    public static boolean gte (long   a, float  b) { return a >= b; }
    public static boolean gte (long   a, double b) { return a >= b; }
    public static boolean gte (float  a, byte   b) { return a >= b; }
    public static boolean gte (float  a, char   b) { return a >= b; }
    public static boolean gte (float  a, short  b) { return a >= b; }
    public static boolean gte (float  a, int    b) { return a >= b; }
    public static boolean gte (float  a, long   b) { return a >= b; }
    public static boolean gte (float  a, float  b) { return a >= b; }
    public static boolean gte (float  a, double b) { return a >= b; }
    public static boolean gte (double a, byte   b) { return a >= b; }
    public static boolean gte (double a, char   b) { return a >= b; }
    public static boolean gte (double a, short  b) { return a >= b; }
    public static boolean gte (double a, int    b) { return a >= b; }
    public static boolean gte (double a, long   b) { return a >= b; }
    public static boolean gte (double a, float  b) { return a >= b; }
    public static boolean gte (double a, double b) { return a >= b; }
    
    // ============================ EQ : == ================================ //
    
    public static boolean eq (byte   a, byte   b) { return a == b; }
    public static boolean eq (byte   a, char   b) { return a == b; }
    public static boolean eq (byte   a, short  b) { return a == b; }
    public static boolean eq (byte   a, int    b) { return a == b; }
    public static boolean eq (byte   a, long   b) { return a == b; }
    public static boolean eq (byte   a, float  b) { return a == b; }
    public static boolean eq (byte   a, double b) { return a == b; }
    public static boolean eq (char   a, byte   b) { return a == b; }
    public static boolean eq (char   a, char   b) { return a == b; }
    public static boolean eq (char   a, short  b) { return a == b; }
    public static boolean eq (char   a, int    b) { return a == b; }
    public static boolean eq (char   a, long   b) { return a == b; }
    public static boolean eq (char   a, float  b) { return a == b; }
    public static boolean eq (char   a, double b) { return a == b; }
    public static boolean eq (short  a, byte   b) { return a == b; }
    public static boolean eq (short  a, char   b) { return a == b; }
    public static boolean eq (short  a, short  b) { return a == b; }
    public static boolean eq (short  a, int    b) { return a == b; }
    public static boolean eq (short  a, long   b) { return a == b; }
    public static boolean eq (short  a, float  b) { return a == b; }
    public static boolean eq (short  a, double b) { return a == b; }
    public static boolean eq (int    a, byte   b) { return a == b; }
    public static boolean eq (int    a, char   b) { return a == b; }
    public static boolean eq (int    a, short  b) { return a == b; }
    public static boolean eq (int    a, int    b) { return a == b; }
    public static boolean eq (int    a, long   b) { return a == b; }
    public static boolean eq (int    a, float  b) { return a == b; }
    public static boolean eq (int    a, double b) { return a == b; }
    public static boolean eq (long   a, byte   b) { return a == b; }
    public static boolean eq (long   a, char   b) { return a == b; }
    public static boolean eq (long   a, short  b) { return a == b; }
    public static boolean eq (long   a, int    b) { return a == b; }
    public static boolean eq (long   a, long   b) { return a == b; }
    public static boolean eq (long   a, float  b) { return a == b; }
    public static boolean eq (long   a, double b) { return a == b; }
    public static boolean eq (float  a, byte   b) { return a == b; }
    public static boolean eq (float  a, char   b) { return a == b; }
    public static boolean eq (float  a, short  b) { return a == b; }
    public static boolean eq (float  a, int    b) { return a == b; }
    public static boolean eq (float  a, long   b) { return a == b; }
    public static boolean eq (float  a, float  b) { return a == b; }
    public static boolean eq (float  a, double b) { return a == b; }
    public static boolean eq (double a, byte   b) { return a == b; }
    public static boolean eq (double a, char   b) { return a == b; }
    public static boolean eq (double a, short  b) { return a == b; }
    public static boolean eq (double a, int    b) { return a == b; }
    public static boolean eq (double a, long   b) { return a == b; }
    public static boolean eq (double a, float  b) { return a == b; }
    public static boolean eq (double a, double b) { return a == b; }
    
    // ============================ NEQ : != ================================ //
    
    public static boolean neq (byte   a, byte   b) { return a != b; }
    public static boolean neq (byte   a, char   b) { return a != b; }
    public static boolean neq (byte   a, short  b) { return a != b; }
    public static boolean neq (byte   a, int    b) { return a != b; }
    public static boolean neq (byte   a, long   b) { return a != b; }
    public static boolean neq (byte   a, float  b) { return a != b; }
    public static boolean neq (byte   a, double b) { return a != b; }
    public static boolean neq (char   a, byte   b) { return a != b; }
    public static boolean neq (char   a, char   b) { return a != b; }
    public static boolean neq (char   a, short  b) { return a != b; }
    public static boolean neq (char   a, int    b) { return a != b; }
    public static boolean neq (char   a, long   b) { return a != b; }
    public static boolean neq (char   a, float  b) { return a != b; }
    public static boolean neq (char   a, double b) { return a != b; }
    public static boolean neq (short  a, byte   b) { return a != b; }
    public static boolean neq (short  a, char   b) { return a != b; }
    public static boolean neq (short  a, short  b) { return a != b; }
    public static boolean neq (short  a, int    b) { return a != b; }
    public static boolean neq (short  a, long   b) { return a != b; }
    public static boolean neq (short  a, float  b) { return a != b; }
    public static boolean neq (short  a, double b) { return a != b; }
    public static boolean neq (int    a, byte   b) { return a != b; }
    public static boolean neq (int    a, char   b) { return a != b; }
    public static boolean neq (int    a, short  b) { return a != b; }
    public static boolean neq (int    a, int    b) { return a != b; }
    public static boolean neq (int    a, long   b) { return a != b; }
    public static boolean neq (int    a, float  b) { return a != b; }
    public static boolean neq (int    a, double b) { return a != b; }
    public static boolean neq (long   a, byte   b) { return a != b; }
    public static boolean neq (long   a, char   b) { return a != b; }
    public static boolean neq (long   a, short  b) { return a != b; }
    public static boolean neq (long   a, int    b) { return a != b; }
    public static boolean neq (long   a, long   b) { return a != b; }
    public static boolean neq (long   a, float  b) { return a != b; }
    public static boolean neq (long   a, double b) { return a != b; }
    public static boolean neq (float  a, byte   b) { return a != b; }
    public static boolean neq (float  a, char   b) { return a != b; }
    public static boolean neq (float  a, short  b) { return a != b; }
    public static boolean neq (float  a, int    b) { return a != b; }
    public static boolean neq (float  a, long   b) { return a != b; }
    public static boolean neq (float  a, float  b) { return a != b; }
    public static boolean neq (float  a, double b) { return a != b; }
    public static boolean neq (double a, byte   b) { return a != b; }
    public static boolean neq (double a, char   b) { return a != b; }
    public static boolean neq (double a, short  b) { return a != b; }
    public static boolean neq (double a, int    b) { return a != b; }
    public static boolean neq (double a, long   b) { return a != b; }
    public static boolean neq (double a, float  b) { return a != b; }
    public static boolean neq (double a, double b) { return a != b; }

    // ============================ INC / DEC ================================ //

    public static byte   inc (byte   a) { return (byte )(a + byte1 ); }
    public static char   inc (char   a) { return (char )(a + char1 ); }
    public static short  inc (short  a) { return (short)(a + short1); }
    public static int    inc (int    a) { return a + int1;   }
    public static long   inc (long   a) { return a + 1L;     }
    public static float  inc (float  a) { return a + 1.0f;   }
    public static double inc (double a) { return a + 1.0d;   }
     
    public static byte   dec (byte   a) { return (byte )(a - byte1 ); }
    public static char   dec (char   a) { return (char )(a - char1 ); }
    public static short  dec (short  a) { return (short)(a - short1); }
    public static int    dec (int    a) { return a - int1;   }
    public static long   dec (long   a) { return a - 1L;     }
    public static float  dec (float  a) { return a - 1.0f;   }
    public static double dec (double a) { return a - 1.0d;   }

    // ============================ ISZERO ================================ //
  
    public static boolean isZero (byte   a) { return a == byte0;  }
    public static boolean isZero (char   a) { return a == char0;  }
    public static boolean isZero (short  a) { return a == short0; }
    public static boolean isZero (int    a) { return a == int0;   }
    public static boolean isZero (long   a) { return a == 0L;     }
    public static boolean isZero (float  a) { return a == 0.0f;   }
    public static boolean isZero (double a) { return a == 0.0d;   }
  
    // ============================ ADD : + ================================ //
    // "Infectious": uses the largest data type passed
    // Apparently '+' is fundamentally an int operator. Maybe use bit things

    public static byte   add (byte   a, byte   b) { return (byte )(a + b); }
    public static char   add (byte   a, char   b) { return (char )(a + b); }
    public static short  add (byte   a, short  b) { return (short)(a + b); }
    public static int    add (byte   a, int    b) { return a + b; }
    public static long   add (byte   a, long   b) { return a + b; }
    public static float  add (byte   a, float  b) { return a + b; }
    public static double add (byte   a, double b) { return a + b; }
    public static char   add (char   a, byte   b) { return (char )(a + b); }
    public static char   add (char   a, char   b) { return (char )(a + b); }
    public static short  add (char   a, short  b) { return (short)(a + b); }
    public static int    add (char   a, int    b) { return a + b; }
    public static long   add (char   a, long   b) { return a + b; }
    public static float  add (char   a, float  b) { return a + b; }
    public static double add (char   a, double b) { return a + b; }
    public static short  add (short  a, byte   b) { return (short)(a + b); }
    public static short  add (short  a, char   b) { return (short)(a + b); }
    public static short  add (short  a, short  b) { return (short)(a + b); }
    public static int    add (short  a, int    b) { return a + b; }
    public static long   add (short  a, long   b) { return a + b; }
    public static float  add (short  a, float  b) { return a + b; }
    public static double add (short  a, double b) { return a + b; }
    public static int    add (int    a, byte   b) { return a + b; }
    public static int    add (int    a, char   b) { return a + b; }
    public static int    add (int    a, short  b) { return a + b; }
    public static int    add (int    a, int    b) { return a + b; }
    public static long   add (int    a, long   b) { return a + b; }
    public static float  add (int    a, float  b) { return a + b; }
    public static double add (int    a, double b) { return a + b; }
    public static long   add (long   a, byte   b) { return a + b; }
    public static long   add (long   a, char   b) { return a + b; }
    public static long   add (long   a, short  b) { return a + b; }
    public static long   add (long   a, int    b) { return a + b; }
    public static long   add (long   a, long   b) { return a + b; }
    public static float  add (long   a, float  b) { return a + b; }
    public static double add (long   a, double b) { return a + b; }
    public static float  add (float  a, byte   b) { return a + b; }
    public static float  add (float  a, char   b) { return a + b; }
    public static float  add (float  a, short  b) { return a + b; }
    public static float  add (float  a, int    b) { return a + b; }
    public static float  add (float  a, long   b) { return a + b; }
    public static float  add (float  a, float  b) { return a + b; }
    public static double add (float  a, double b) { return a + b; }
    public static double add (double a, byte   b) { return a + b; }
    public static double add (double a, char   b) { return a + b; }
    public static double add (double a, short  b) { return a + b; }
    public static double add (double a, int    b) { return a + b; }
    public static double add (double a, long   b) { return a + b; }
    public static double add (double a, float  b) { return a + b; }
    public static double add (double a, double b) { return a + b; }
 
    // ============================ SUBTRACT : - ================================ //
    // "Infectious": uses the largest data type passed

    public static byte   subtract (byte   a, byte   b) { return (byte )(a - b); }
    public static char   subtract (byte   a, char   b) { return (char )(a - b); }
    public static short  subtract (byte   a, short  b) { return (short)(a - b); }
    public static int    subtract (byte   a, int    b) { return a - b; }
    public static long   subtract (byte   a, long   b) { return a - b; }
    public static float  subtract (byte   a, float  b) { return a - b; }
    public static double subtract (byte   a, double b) { return a - b; }
    public static char   subtract (char   a, byte   b) { return (char )(a - b); }
    public static char   subtract (char   a, char   b) { return (char )(a - b); }
    public static short  subtract (char   a, short  b) { return (short)(a - b); }
    public static int    subtract (char   a, int    b) { return a - b; }
    public static long   subtract (char   a, long   b) { return a - b; }
    public static float  subtract (char   a, float  b) { return a - b; }
    public static double subtract (char   a, double b) { return a - b; }
    public static short  subtract (short  a, byte   b) { return (short)(a - b); }
    public static short  subtract (short  a, char   b) { return (short)(a - b); }
    public static short  subtract (short  a, short  b) { return (short)(a - b); }
    public static int    subtract (short  a, int    b) { return a - b; }
    public static long   subtract (short  a, long   b) { return a - b; }
    public static float  subtract (short  a, float  b) { return a - b; }
    public static double subtract (short  a, double b) { return a - b; }
    public static int    subtract (int    a, byte   b) { return a - b; }
    public static int    subtract (int    a, char   b) { return a - b; }
    public static int    subtract (int    a, short  b) { return a - b; }
    public static int    subtract (int    a, int    b) { return a - b; }
    public static long   subtract (int    a, long   b) { return a - b; }
    public static float  subtract (int    a, float  b) { return a - b; }
    public static double subtract (int    a, double b) { return a - b; }
    public static long   subtract (long   a, byte   b) { return a - b; }
    public static long   subtract (long   a, char   b) { return a - b; }
    public static long   subtract (long   a, short  b) { return a - b; }
    public static long   subtract (long   a, int    b) { return a - b; }
    public static long   subtract (long   a, long   b) { return a - b; }
    public static float  subtract (long   a, float  b) { return a - b; }
    public static double subtract (long   a, double b) { return a - b; }
    public static float  subtract (float  a, byte   b) { return a - b; }
    public static float  subtract (float  a, char   b) { return a - b; }
    public static float  subtract (float  a, short  b) { return a - b; }
    public static float  subtract (float  a, int    b) { return a - b; }
    public static float  subtract (float  a, long   b) { return a - b; }
    public static float  subtract (float  a, float  b) { return a - b; }
    public static double subtract (float  a, double b) { return a - b; }
    public static double subtract (double a, byte   b) { return a - b; }
    public static double subtract (double a, char   b) { return a - b; }
    public static double subtract (double a, short  b) { return a - b; }
    public static double subtract (double a, int    b) { return a - b; }
    public static double subtract (double a, long   b) { return a - b; }
    public static double subtract (double a, float  b) { return a - b; }
    public static double subtract (double a, double b) { return a - b; }
    
    // ============================ NEGATE : - ================================ //

    public static byte   negate (byte   a) { return (byte )-a; }
    public static char   negate (char   a) { return (char )-a; }
    public static short  negate (short  a) { return (short)-a; }
    public static int    negate (int    a) { return -a; }
    public static long   negate (long   a) { return -a; }
    public static float  negate (float  a) { return -a; }
    public static double negate (double a) { return -a; }

    // ============================ MULTIPLY : * ================================ //
    // "Infectious": uses the largest data type passed

    public static byte   multiply (byte   a, byte   b) { return (byte )(a * b); }
    public static char   multiply (byte   a, char   b) { return (char )(a * b); }
    public static short  multiply (byte   a, short  b) { return (short)(a * b); }
    public static int    multiply (byte   a, int    b) { return a * b; }
    public static long   multiply (byte   a, long   b) { return a * b; }
    public static float  multiply (byte   a, float  b) { return a * b; }
    public static double multiply (byte   a, double b) { return a * b; }
    public static char   multiply (char   a, byte   b) { return (char )(a * b); }
    public static char   multiply (char   a, char   b) { return (char )(a * b); }
    public static short  multiply (char   a, short  b) { return (short)(a * b); }
    public static int    multiply (char   a, int    b) { return a * b; }
    public static long   multiply (char   a, long   b) { return a * b; }
    public static float  multiply (char   a, float  b) { return a * b; }
    public static double multiply (char   a, double b) { return a * b; }
    public static short  multiply (short  a, byte   b) { return (short)(a * b); }
    public static short  multiply (short  a, char   b) { return (short)(a * b); }
    public static short  multiply (short  a, short  b) { return (short)(a * b); }
    public static int    multiply (short  a, int    b) { return a * b; }
    public static long   multiply (short  a, long   b) { return a * b; }
    public static float  multiply (short  a, float  b) { return a * b; }
    public static double multiply (short  a, double b) { return a * b; }
    public static int    multiply (int    a, byte   b) { return a * b; }
    public static int    multiply (int    a, char   b) { return a * b; }
    public static int    multiply (int    a, short  b) { return a * b; }
    public static int    multiply (int    a, int    b) { return a * b; }
    public static long   multiply (int    a, long   b) { return a * b; }
    public static float  multiply (int    a, float  b) { return a * b; }
    public static double multiply (int    a, double b) { return a * b; }
    public static long   multiply (long   a, byte   b) { return a * b; }
    public static long   multiply (long   a, char   b) { return a * b; }
    public static long   multiply (long   a, short  b) { return a * b; }
    public static long   multiply (long   a, int    b) { return a * b; }
    public static long   multiply (long   a, long   b) { return a * b; }
    public static float  multiply (long   a, float  b) { return a * b; }
    public static double multiply (long   a, double b) { return a * b; }
    public static float  multiply (float  a, byte   b) { return a * b; }
    public static float  multiply (float  a, char   b) { return a * b; }
    public static float  multiply (float  a, short  b) { return a * b; }
    public static float  multiply (float  a, int    b) { return a * b; }
    public static float  multiply (float  a, long   b) { return a * b; }
    public static float  multiply (float  a, float  b) { return a * b; }
    public static double multiply (float  a, double b) { return a * b; }
    public static double multiply (double a, byte   b) { return a * b; }
    public static double multiply (double a, char   b) { return a * b; }
    public static double multiply (double a, short  b) { return a * b; }
    public static double multiply (double a, int    b) { return a * b; }
    public static double multiply (double a, long   b) { return a * b; }
    public static double multiply (double a, float  b) { return a * b; }
    public static double multiply (double a, double b) { return a * b; }
 
    // ============================ DIVIDE : / ================================ //
    // "Infectious": uses the largest data type passed

    public static byte   divide (byte   a, byte   b) { return (byte )(a / b); }
    public static char   divide (byte   a, char   b) { return (char )(a / b); }
    public static short  divide (byte   a, short  b) { return (short)(a / b); }
    public static int    divide (byte   a, int    b) { return a / b; }
    public static long   divide (byte   a, long   b) { return a / b; }
    public static float  divide (byte   a, float  b) { return a / b; }
    public static double divide (byte   a, double b) { return a / b; }
    public static char   divide (char   a, byte   b) { return (char )(a / b); }
    public static char   divide (char   a, char   b) { return (char )(a / b); }
    public static short  divide (char   a, short  b) { return (short)(a / b); }
    public static int    divide (char   a, int    b) { return a / b; }
    public static long   divide (char   a, long   b) { return a / b; }
    public static float  divide (char   a, float  b) { return a / b; }
    public static double divide (char   a, double b) { return a / b; }
    public static short  divide (short  a, byte   b) { return (short)(a / b); }
    public static short  divide (short  a, char   b) { return (short)(a / b); }
    public static short  divide (short  a, short  b) { return (short)(a / b); }
    public static int    divide (short  a, int    b) { return a / b; }
    public static long   divide (short  a, long   b) { return a / b; }
    public static float  divide (short  a, float  b) { return a / b; }
    public static double divide (short  a, double b) { return a / b; }
    public static int    divide (int    a, byte   b) { return a / b; }
    public static int    divide (int    a, char   b) { return a / b; }
    public static int    divide (int    a, short  b) { return a / b; }
    public static int    divide (int    a, int    b) { return a / b; }
    public static long   divide (int    a, long   b) { return a / b; }
    public static float  divide (int    a, float  b) { return a / b; }
    public static double divide (int    a, double b) { return a / b; }
    public static long   divide (long   a, byte   b) { return a / b; }
    public static long   divide (long   a, char   b) { return a / b; }
    public static long   divide (long   a, short  b) { return a / b; }
    public static long   divide (long   a, int    b) { return a / b; }
    public static long   divide (long   a, long   b) { return a / b; }
    public static float  divide (long   a, float  b) { return a / b; }
    public static double divide (long   a, double b) { return a / b; }
    public static float  divide (float  a, byte   b) { return a / b; }
    public static float  divide (float  a, char   b) { return a / b; }
    public static float  divide (float  a, short  b) { return a / b; }
    public static float  divide (float  a, int    b) { return a / b; }
    public static float  divide (float  a, long   b) { return a / b; }
    public static float  divide (float  a, float  b) { return a / b; }
    public static double divide (float  a, double b) { return a / b; }
    public static double divide (double a, byte   b) { return a / b; }
    public static double divide (double a, char   b) { return a / b; }
    public static double divide (double a, short  b) { return a / b; }
    public static double divide (double a, int    b) { return a / b; }
    public static double divide (double a, long   b) { return a / b; }
    public static double divide (double a, float  b) { return a / b; }
    public static double divide (double a, double b) { return a / b; }
    
    // ============================ MAX ================================ //
    // "Infectious": uses the largest data type passed

    public static byte   max (byte   a, byte   b) { return (a < b) ? b : a; }
    public static char   max (byte   a, char   b) { return (char)((a < b) ? b : a); }
    public static short  max (byte   a, short  b) { return (a < b) ? b : a; }
    public static int    max (byte   a, int    b) { return (a < b) ? b : a; }
    public static long   max (byte   a, long   b) { return (a < b) ? b : a; }
    public static float  max (byte   a, float  b) { return (a < b) ? b : a; }
    public static double max (byte   a, double b) { return (a < b) ? b : a; }
    public static char   max (char   a, byte   b) { return (char)((a < b) ? b : a); }
    public static char   max (char   a, char   b) { return (a < b) ? b : a; }
    public static short  max (char   a, short  b) { return (short)((a < b) ? b : a); }
    public static int    max (char   a, int    b) { return (a < b) ? b : a; }
    public static long   max (char   a, long   b) { return (a < b) ? b : a; }
    public static float  max (char   a, float  b) { return (a < b) ? b : a; }
    public static double max (char   a, double b) { return (a < b) ? b : a; }
    public static short  max (short  a, byte   b) { return (a < b) ? b : a; }
    public static short  max (short  a, char   b) { return (short)((a < b) ? b : a); }
    public static short  max (short  a, short  b) { return (a < b) ? b : a; }
    public static int    max (short  a, int    b) { return (a < b) ? b : a; }
    public static long   max (short  a, long   b) { return (a < b) ? b : a; }
    public static float  max (short  a, float  b) { return (a < b) ? b : a; }
    public static double max (short  a, double b) { return (a < b) ? b : a; }
    public static int    max (int    a, byte   b) { return (a < b) ? b : a; }
    public static int    max (int    a, char   b) { return (a < b) ? b : a; }
    public static int    max (int    a, short  b) { return (a < b) ? b : a; }
    public static int    max (int    a, int    b) { return (a < b) ? b : a; }
    public static long   max (int    a, long   b) { return (a < b) ? b : a; }
    public static float  max (int    a, float  b) { return (a < b) ? b : a; }
    public static double max (int    a, double b) { return (a < b) ? b : a; }
    public static long   max (long   a, byte   b) { return (a < b) ? b : a; }
    public static long   max (long   a, char   b) { return (a < b) ? b : a; }
    public static long   max (long   a, short  b) { return (a < b) ? b : a; }
    public static long   max (long   a, int    b) { return (a < b) ? b : a; }
    public static long   max (long   a, long   b) { return (a < b) ? b : a; }
    public static float  max (long   a, float  b) { return (a < b) ? b : a; }
    public static double max (long   a, double b) { return (a < b) ? b : a; }
    public static float  max (float  a, byte   b) { return (a < b) ? b : a; }
    public static float  max (float  a, char   b) { return (a < b) ? b : a; }
    public static float  max (float  a, short  b) { return (a < b) ? b : a; }
    public static float  max (float  a, int    b) { return (a < b) ? b : a; }
    public static float  max (float  a, long   b) { return (a < b) ? b : a; }
    public static float  max (float  a, float  b) { return (a < b) ? b : a; }
    public static double max (float  a, double b) { return (a < b) ? b : a; }
    public static double max (double a, byte   b) { return (a < b) ? b : a; }
    public static double max (double a, char   b) { return (a < b) ? b : a; }
    public static double max (double a, short  b) { return (a < b) ? b : a; }
    public static double max (double a, int    b) { return (a < b) ? b : a; }
    public static double max (double a, long   b) { return (a < b) ? b : a; }
    public static double max (double a, float  b) { return (a < b) ? b : a; }
    public static double max (double a, double b) { return (a < b) ? b : a; }

    // ============================ MIN ================================ //
    // "Infectious": uses the largest data type passed

    public static byte   min (byte   a, byte   b) { return (a > b) ? b : a; }
    public static char   min (byte   a, char   b) { return (char)((a > b) ? b : a); }
    public static short  min (byte   a, short  b) { return (a > b) ? b : a; }
    public static int    min (byte   a, int    b) { return (a > b) ? b : a; }
    public static long   min (byte   a, long   b) { return (a > b) ? b : a; }
    public static float  min (byte   a, float  b) { return (a > b) ? b : a; }
    public static double min (byte   a, double b) { return (a > b) ? b : a; }
    public static char   min (char   a, byte   b) { return (char)((a > b) ? b : a); }
    public static char   min (char   a, char   b) { return (a > b) ? b : a; }
    public static short  min (char   a, short  b) { return (short)((a > b) ? b : a); }
    public static int    min (char   a, int    b) { return (a > b) ? b : a; }
    public static long   min (char   a, long   b) { return (a > b) ? b : a; }
    public static float  min (char   a, float  b) { return (a > b) ? b : a; }
    public static double min (char   a, double b) { return (a > b) ? b : a; }
    public static short  min (short  a, byte   b) { return (a > b) ? b : a; }
    public static short  min (short  a, char   b) { return (short)((a > b) ? b : a); }
    public static short  min (short  a, short  b) { return (a > b) ? b : a; }
    public static int    min (short  a, int    b) { return (a > b) ? b : a; }
    public static long   min (short  a, long   b) { return (a > b) ? b : a; }
    public static float  min (short  a, float  b) { return (a > b) ? b : a; }
    public static double min (short  a, double b) { return (a > b) ? b : a; }
    public static int    min (int    a, byte   b) { return (a > b) ? b : a; }
    public static int    min (int    a, char   b) { return (a > b) ? b : a; }
    public static int    min (int    a, short  b) { return (a > b) ? b : a; }
    public static int    min (int    a, int    b) { return (a > b) ? b : a; }
    public static long   min (int    a, long   b) { return (a > b) ? b : a; }
    public static float  min (int    a, float  b) { return (a > b) ? b : a; }
    public static double min (int    a, double b) { return (a > b) ? b : a; }
    public static long   min (long   a, byte   b) { return (a > b) ? b : a; }
    public static long   min (long   a, char   b) { return (a > b) ? b : a; }
    public static long   min (long   a, short  b) { return (a > b) ? b : a; }
    public static long   min (long   a, int    b) { return (a > b) ? b : a; }
    public static long   min (long   a, long   b) { return (a > b) ? b : a; }
    public static float  min (long   a, float  b) { return (a > b) ? b : a; }
    public static double min (long   a, double b) { return (a > b) ? b : a; }
    public static float  min (float  a, byte   b) { return (a > b) ? b : a; }
    public static float  min (float  a, char   b) { return (a > b) ? b : a; }
    public static float  min (float  a, short  b) { return (a > b) ? b : a; }
    public static float  min (float  a, int    b) { return (a > b) ? b : a; }
    public static float  min (float  a, long   b) { return (a > b) ? b : a; }
    public static float  min (float  a, float  b) { return (a > b) ? b : a; }
    public static double min (float  a, double b) { return (a > b) ? b : a; }
    public static double min (double a, byte   b) { return (a > b) ? b : a; }
    public static double min (double a, char   b) { return (a > b) ? b : a; }
    public static double min (double a, short  b) { return (a > b) ? b : a; }
    public static double min (double a, int    b) { return (a > b) ? b : a; }
    public static double min (double a, long   b) { return (a > b) ? b : a; }
    public static double min (double a, float  b) { return (a > b) ? b : a; }
    public static double min (double a, double b) { return (a > b) ? b : a; }

    // ============================ REM ================================ //

    public static long    rem     (long   n, long div) { return n % div; }

    public static long uncheckedLongCast (char x) { return (long)x; }
}
