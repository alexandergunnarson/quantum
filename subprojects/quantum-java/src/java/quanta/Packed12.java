package quanta;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

// SOURCE http://java-performance.info/string-packing-converting-characters-to-bytes/
//        http://java-performance.info/string-packing-converting-strings-to-any-other-objects/

public class Packed12 extends PackedBase
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
        result = 31 * result + f2;
        result = 31 * result + f3;
        return result;
    }
}