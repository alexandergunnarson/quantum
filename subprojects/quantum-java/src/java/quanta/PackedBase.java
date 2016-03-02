package quanta;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

abstract class PackedBase
{

    public static Charset US_ASCII = Charset.forName("US-ASCII");
    protected static int get( final byte[] ar, final int index )
    {
        return index < ar.length ? ar[ index ] : 0;
    }
 
    protected abstract ByteBuffer toByteBuffer();
 
    protected String toString( final ByteBuffer bbuf )
    {
        final byte[] ar = bbuf.array();
        //skip zero bytes at the tail of the string
        int last = ar.length - 1;
        while ( last > 0 && ar[ last ] == 0 )
            --last;
        return new String( ar, 0, last + 1, US_ASCII );
    }
 
    @Override
    public String toString()
    {
        return toString( toByteBuffer());
    }

    private class Packed12 extends PackedBase
{
    private final int f1;
    private final int f2;
    private final int f3;
 
    public Packed12( final byte[] ar )
    { // should be the same logic as in java.util.Bits.getInt, because ByteBuffer.putInt use it
        f1 = get( ar, 3  ) | get( ar, 2  ) << 8 | get( ar, 1 ) << 16 | get( ar, 0 ) << 24;
        f2 = get( ar, 7  ) | get( ar, 6  ) << 8 | get( ar, 5 ) << 16 | get( ar, 4 ) << 24;
        f3 = get( ar, 11 ) | get( ar, 10 ) << 8 | get( ar, 9 ) << 16 | get( ar, 8 ) << 24;
    }
 
    protected ByteBuffer toByteBuffer() {
        final ByteBuffer bbuf = ByteBuffer.allocate( 12 );
        bbuf.putInt( f1 );
        bbuf.putInt( f2 );
        bbuf.putInt( f3 );
        return bbuf;
    }
 
    @Override
    public boolean equals(Object o) {
        if ( this == o ) return true;
        if ( o == null || getClass() != o.getClass() ) return false;
        Packed12 packed12 = ( Packed12 ) o;
        return f1 == packed12.f1 && f2 == packed12.f2 && f3 == packed12.f3;
    }
 
    @Override
    public int hashCode() {
        int result = f1;
        result = (31 * result) + f2;
        result = (31 * result) + f3;
        return result;
    }
}
}