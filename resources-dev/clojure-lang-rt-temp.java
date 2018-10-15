package clojure.lang;

import java.net.MalformedURLException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Callable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.net.URL;
import java.net.JarURLConnection;
import java.nio.charset.Charset;
import java.net.URLConnection;

public class RT{

static public final Object[] EMPTY_ARRAY = new Object[]{};

static AtomicInteger id = new AtomicInteger(1);

static public void addURL(Object url) throws MalformedURLException{
	URL u = (url instanceof String) ? (new URL((String) url)) : (URL) url;
	ClassLoader ccl = Thread.currentThread().getContextClassLoader();
	if(ccl instanceof DynamicClassLoader)
		((DynamicClassLoader)ccl).addURL(u);
	else
		throw new IllegalAccessError("Context classloader is not a DynamicClassLoader");
}

public static boolean checkSpecAsserts = Boolean.getBoolean("clojure.spec.check-asserts");
public static boolean instrumentMacros = ! Boolean.getBoolean("clojure.spec.skip-macros");
static volatile boolean CHECK_SPECS = false;


static public int nextID(){
	return id.getAndIncrement();
}

////////////// Collections support /////////////////////////////////

static public boolean canSeq(Object coll){
    return coll instanceof ISeq
            || coll instanceof Seqable
            || coll == null
            || coll instanceof Iterable
            || coll.getClass().isArray()
            || coll instanceof CharSequence
            || coll instanceof Map;
}

static public Iterator iter(Object coll){
	if(coll instanceof Iterable)
		return ((Iterable)coll).iterator();
	else if(coll == null)
		return new Iterator(){
			public boolean hasNext(){
				return false;
			}

			public Object next(){
				throw new NoSuchElementException();
			}

			public void remove(){
				throw new UnsupportedOperationException();
			}
		};
	else if(coll instanceof Map){
		return ((Map)coll).entrySet().iterator();
	}
	else if(coll instanceof String){
		final String s = (String) coll;
		return new Iterator(){
			int i = 0;

			public boolean hasNext(){
				return i < s.length();
			}

			public Object next(){
				return s.charAt(i++);
			}

			public void remove(){
				throw new UnsupportedOperationException();
			}
		};
	}
  else if(coll.getClass().isArray()){
    return ArrayIter.createFromObject(coll);
  }
	else
		return iter(seq(coll));
}

static public ISeq keys(Object coll){
	if(coll instanceof IPersistentMap)
		return APersistentMap.KeySeq.createFromMap((IPersistentMap)coll);
	else
		return APersistentMap.KeySeq.create(seq(coll));
}

static public ISeq vals(Object coll){
	if(coll instanceof IPersistentMap)
		return APersistentMap.ValSeq.createFromMap((IPersistentMap)coll);
	else
		return APersistentMap.ValSeq.create(seq(coll));
}

static public IPersistentCollection conj(IPersistentCollection coll, Object x){
	if(coll == null)
		return new PersistentList(x);
	return coll.cons(x);
}

static public ISeq cons(Object x, Object coll){
	//ISeq y = seq(coll);
	if(coll == null)
		return new PersistentList(x);
	else if(coll instanceof ISeq)
		return new Cons(x, (ISeq) coll);
	else
		return new Cons(x, seq(coll));
}

static public Object first(Object x){
	if(x instanceof ISeq)
		return ((ISeq) x).first();
	ISeq seq = seq(x);
	if(seq == null)
		return null;
	return seq.first();
}

static public ISeq next(Object x){
	if(x instanceof ISeq)
		return ((ISeq) x).next();
	ISeq seq = seq(x);
	if(seq == null)
		return null;
	return seq.next();
}

static public ISeq more(Object x){
	if(x instanceof ISeq)
		return ((ISeq) x).more();
	ISeq seq = seq(x);
	if(seq == null)
		return PersistentList.EMPTY;
	return seq.more();
}

//static public Seqable more(Object x){
//    Seqable ret = null;
//	if(x instanceof ISeq)
//		ret = ((ISeq) x).more();
//    else
//        {
//	    ISeq seq = seq(x);
//	    if(seq == null)
//		    ret = PersistentList.EMPTY;
//	    else
//            ret = seq.more();
//        }
//    if(ret == null)
//        ret = PersistentList.EMPTY;
//    return ret;
//}

static public Object peek(Object x){
	if(x == null)
		return null;
	return ((IPersistentStack) x).peek();
}

static public Object pop(Object x){
	if(x == null)
		return null;
	return ((IPersistentStack) x).pop();
}

static public Object get(Object coll, Object key){
	if(coll instanceof ILookup)
		return ((ILookup) coll).valAt(key);
	return getFrom(coll, key);
}

static Object getFrom(Object coll, Object key){
	if(coll == null)
		return null;
	else if(coll instanceof Map) {
		Map m = (Map) coll;
		return m.get(key);
	}
	else if(coll instanceof IPersistentSet) {
		IPersistentSet set = (IPersistentSet) coll;
		return set.get(key);
	}
	else if(key instanceof Number && (coll instanceof String || coll.getClass().isArray())) {
		int n = ((Number) key).intValue();
		if(n >= 0 && n < count(coll))
			return nth(coll, n);
		return null;
	}
	else if(coll instanceof ITransientSet) {
		ITransientSet set = (ITransientSet) coll;
		return set.get(key);
	}

	return null;
}

static public Object get(Object coll, Object key, Object notFound){
	if(coll instanceof ILookup)
		return ((ILookup) coll).valAt(key, notFound);
	return getFrom(coll, key, notFound);
}

static Object getFrom(Object coll, Object key, Object notFound){
	if(coll == null)
		return notFound;
	else if(coll instanceof Map) {
		Map m = (Map) coll;
		if(m.containsKey(key))
			return m.get(key);
		return notFound;
	}
	else if(coll instanceof IPersistentSet) {
		IPersistentSet set = (IPersistentSet) coll;
		if(set.contains(key))
			return set.get(key);
		return notFound;
	}
	else if(key instanceof Number && (coll instanceof String || coll.getClass().isArray())) {
		int n = ((Number) key).intValue();
		return n >= 0 && n < count(coll) ? nth(coll, n) : notFound;
	}
	else if(coll instanceof ITransientSet) {
		ITransientSet set = (ITransientSet) coll;
		if(set.contains(key))
			return set.get(key);
		return notFound;
	}
	return notFound;

}

static public Associative assoc(Object coll, Object key, Object val){
	if(coll == null)
		return new PersistentArrayMap(new Object[]{key, val});
	return ((Associative) coll).assoc(key, val);
}

static public Object contains(Object coll, Object key){
	if(coll == null)
		return F;
	else if(coll instanceof Associative)
		return ((Associative) coll).containsKey(key) ? T : F;
	else if(coll instanceof IPersistentSet)
		return ((IPersistentSet) coll).contains(key) ? T : F;
	else if(coll instanceof Map) {
		Map m = (Map) coll;
		return m.containsKey(key) ? T : F;
	}
	else if(coll instanceof Set) {
		Set s = (Set) coll;
		return s.contains(key) ? T : F;
	}
	else if(key instanceof Number && (coll instanceof String || coll.getClass().isArray())) {
		int n = ((Number) key).intValue();
		return n >= 0 && n < count(coll);
	}
	else if(coll instanceof ITransientSet)
		return ((ITransientSet)coll).contains(key) ? T : F;
	else if(coll instanceof ITransientAssociative2)
		return (((ITransientAssociative2)coll).containsKey(key)) ? T : F;
	throw new IllegalArgumentException("contains? not supported on type: " + coll.getClass().getName());
}

static public Object find(Object coll, Object key){
	if(coll == null)
		return null;
	else if(coll instanceof Associative)
		return ((Associative) coll).entryAt(key);
	else if(coll instanceof Map) {
		Map m = (Map) coll;
		if(m.containsKey(key))
			return MapEntry.create(key, m.get(key));
		return null;
	}
	else if(coll instanceof ITransientAssociative2) {
		return ((ITransientAssociative2) coll).entryAt(key);
	}
	throw new IllegalArgumentException("find not supported on type: " + coll.getClass().getName());
}

//takes a seq of key,val,key,val

//returns tail starting at val of matching key if found, else null
static public ISeq findKey(Keyword key, ISeq keyvals) {
	while(keyvals != null) {
		ISeq r = keyvals.next();
		if(r == null)
			throw Util.runtimeException("Malformed keyword argslist");
		if(keyvals.first() == key)
			return r;
		keyvals = r.next();
	}
	return null;
}

static public Object dissoc(Object coll, Object key) {
	if(coll == null)
		return null;
	return ((IPersistentMap) coll).without(key);
}

static public Object nth(Object coll, int n){
	if(coll instanceof Indexed)
		return ((Indexed) coll).nth(n);
	return nthFrom(Util.ret1(coll, coll = null), n);
}

static Object nthFrom(Object coll, int n){
	if(coll == null)
		return null;
	else if(coll instanceof CharSequence)
		return Character.valueOf(((CharSequence) coll).charAt(n));
	else if(coll.getClass().isArray())
		return Reflector.prepRet(coll.getClass().getComponentType(),Array.get(coll, n));
	else if(coll instanceof RandomAccess)
		return ((List) coll).get(n);
	else if(coll instanceof Matcher)
		return ((Matcher) coll).group(n);

	else if(coll instanceof Map.Entry) {
		Map.Entry e = (Map.Entry) coll;
		if(n == 0)
			return e.getKey();
		else if(n == 1)
			return e.getValue();
		throw new IndexOutOfBoundsException();
	}

	else if(coll instanceof Sequential) {
		ISeq seq = RT.seq(coll);
		coll = null;
		for(int i = 0; i <= n && seq != null; ++i, seq = seq.next()) {
			if(i == n)
				return seq.first();
		}
		throw new IndexOutOfBoundsException();
	}
	else
		throw new UnsupportedOperationException(
				"nth not supported on this type: " + coll.getClass().getSimpleName());
}

static public Object nth(Object coll, int n, Object notFound){
	if(coll instanceof Indexed) {
		Indexed v = (Indexed) coll;
			return v.nth(n, notFound);
	}
	return nthFrom(coll, n, notFound);
}

static Object nthFrom(Object coll, int n, Object notFound){
	if(coll == null)
		return notFound;
	else if(n < 0)
		return notFound;

	else if(coll instanceof CharSequence) {
		CharSequence s = (CharSequence) coll;
		if(n < s.length())
			return Character.valueOf(s.charAt(n));
		return notFound;
	}
	else if(coll.getClass().isArray()) {
		if(n < Array.getLength(coll))
			return Reflector.prepRet(coll.getClass().getComponentType(),Array.get(coll, n));
		return notFound;
	}
	else if(coll instanceof RandomAccess) {
		List list = (List) coll;
		if(n < list.size())
			return list.get(n);
		return notFound;
	}
	else if(coll instanceof Matcher) {
		Matcher m = (Matcher) coll;
		if(n < m.groupCount())
			return m.group(n);
		return notFound;
	}
	else if(coll instanceof Map.Entry) {
		Map.Entry e = (Map.Entry) coll;
		if(n == 0)
			return e.getKey();
		else if(n == 1)
			return e.getValue();
		return notFound;
	}
	else if(coll instanceof Sequential) {
		ISeq seq = RT.seq(coll);
		coll = null;
		for(int i = 0; i <= n && seq != null; ++i, seq = seq.next()) {
			if(i == n)
				return seq.first();
		}
		return notFound;
	}
	else
		throw new UnsupportedOperationException(
				"nth not supported on this type: " + coll.getClass().getSimpleName());
}

static public Object assocN(int n, Object val, Object coll){
	if(coll == null)
		return null;
	else if(coll instanceof IPersistentVector)
		return ((IPersistentVector) coll).assocN(n, val);
	else if(coll instanceof Object[]) {
		//hmm... this is not persistent
		Object[] array = ((Object[]) coll);
		array[n] = val;
		return array;
	}
	else
		return null;
}

static boolean hasTag(Object o, Object tag){
	return Util.equals(tag, RT.get(RT.meta(o), TAG_KEY));
}

/**
 * ********************* Boxing/casts ******************************
 */
static public char charCast(Object x){
	if(x instanceof Character)
		return ((Character) x).charValue();

	long n = ((Number) x).longValue();
	if(n < Character.MIN_VALUE || n > Character.MAX_VALUE)
		throw new IllegalArgumentException("Value out of range for char: " + x);

	return (char) n;
}

static public char charCast(byte x){
    char i = (char) x;
    if(i != x)
        throw new IllegalArgumentException("Value out of range for char: " + x);
    return i;
}

static public char charCast(short x){
    char i = (char) x;
    if(i != x)
        throw new IllegalArgumentException("Value out of range for char: " + x);
    return i;
}

static public char charCast(char x){
    return x;
}

static public char charCast(int x){
    char i = (char) x;
    if(i != x)
        throw new IllegalArgumentException("Value out of range for char: " + x);
    return i;
}

static public char charCast(long x){
    char i = (char) x;
    if(i != x)
        throw new IllegalArgumentException("Value out of range for char: " + x);
    return i;
}

static public char charCast(float x){
    if(x >= Character.MIN_VALUE && x <= Character.MAX_VALUE)
        return (char) x;
    throw new IllegalArgumentException("Value out of range for char: " + x);
}

static public char charCast(double x){
    if(x >= Character.MIN_VALUE && x <= Character.MAX_VALUE)
        return (char) x;
    throw new IllegalArgumentException("Value out of range for char: " + x);
}

static public boolean booleanCast(Object x){
	if(x instanceof Boolean)
		return ((Boolean) x).booleanValue();
	return x != null;
}

static public boolean booleanCast(boolean x){
	return x;
}

static public byte byteCast(Object x){
	if(x instanceof Byte)
		return ((Byte) x).byteValue();
	long n = longCast(x);
	if(n < Byte.MIN_VALUE || n > Byte.MAX_VALUE)
		throw new IllegalArgumentException("Value out of range for byte: " + x);

	return (byte) n;
}

static public byte byteCast(byte x){
    return x;
}

static public byte byteCast(short x){
    byte i = (byte) x;
    if(i != x)
        throw new IllegalArgumentException("Value out of range for byte: " + x);
    return i;
}

static public byte byteCast(int x){
    byte i = (byte) x;
    if(i != x)
        throw new IllegalArgumentException("Value out of range for byte: " + x);
    return i;
}

static public byte byteCast(long x){
    byte i = (byte) x;
    if(i != x)
        throw new IllegalArgumentException("Value out of range for byte: " + x);
    return i;
}

static public byte byteCast(float x){
    if(x >= Byte.MIN_VALUE && x <= Byte.MAX_VALUE)
        return (byte) x;
    throw new IllegalArgumentException("Value out of range for byte: " + x);
}

static public byte byteCast(double x){
    if(x >= Byte.MIN_VALUE && x <= Byte.MAX_VALUE)
        return (byte) x;
    throw new IllegalArgumentException("Value out of range for byte: " + x);
}

static public short shortCast(Object x){
	if(x instanceof Short)
		return ((Short) x).shortValue();
	long n = longCast(x);
	if(n < Short.MIN_VALUE || n > Short.MAX_VALUE)
		throw new IllegalArgumentException("Value out of range for short: " + x);

	return (short) n;
}

static public short shortCast(byte x){
	return x;
}

static public short shortCast(short x){
	return x;
}

static public short shortCast(int x){
    short i = (short) x;
    if(i != x)
        throw new IllegalArgumentException("Value out of range for short: " + x);
    return i;
}

static public short shortCast(long x){
    short i = (short) x;
    if(i != x)
        throw new IllegalArgumentException("Value out of range for short: " + x);
    return i;
}

static public short shortCast(float x){
    if(x >= Short.MIN_VALUE && x <= Short.MAX_VALUE)
        return (short) x;
    throw new IllegalArgumentException("Value out of range for short: " + x);
}

static public short shortCast(double x){
    if(x >= Short.MIN_VALUE && x <= Short.MAX_VALUE)
        return (short) x;
    throw new IllegalArgumentException("Value out of range for short: " + x);
}

static public int intCast(Object x){
	if(x instanceof Integer)
		return ((Integer)x).intValue();
	if(x instanceof Number)
		{
		long n = longCast(x);
		return intCast(n);
		}
	return ((Character) x).charValue();
}

static public int intCast(char x){
	return x;
}

static public int intCast(byte x){
	return x;
}

static public int intCast(short x){
	return x;
}

static public int intCast(int x){
	return x;
}

static public int intCast(float x){
	if(x < Integer.MIN_VALUE || x > Integer.MAX_VALUE)
		throw new IllegalArgumentException("Value out of range for int: " + x);
	return (int) x;
}

static public int intCast(long x){
	int i = (int) x;
	if(i != x)
		throw new IllegalArgumentException("Value out of range for int: " + x);
	return i;
}

static public int intCast(double x){
	if(x < Integer.MIN_VALUE || x > Integer.MAX_VALUE)
		throw new IllegalArgumentException("Value out of range for int: " + x);
	return (int) x;
}

static public long longCast(Object x){
	if(x instanceof Integer || x instanceof Long)
		return ((Number) x).longValue();
	else if (x instanceof BigInt)
		{
		BigInt bi = (BigInt) x;
		if(bi.bipart == null)
			return bi.lpart;
		else
			throw new IllegalArgumentException("Value out of range for long: " + x);
		}
	else if (x instanceof BigInteger)
		{
		BigInteger bi = (BigInteger) x;
		if(bi.bitLength() < 64)
			return bi.longValue();
		else
			throw new IllegalArgumentException("Value out of range for long: " + x);
		}
	else if (x instanceof Byte || x instanceof Short)
	    return ((Number) x).longValue();
	else if (x instanceof Ratio)
	    return longCast(((Ratio)x).bigIntegerValue());
	else if (x instanceof Character)
	    return longCast(((Character) x).charValue());
	else
	    return longCast(((Number)x).doubleValue());
}

static public long longCast(byte x){
    return x;
}

static public long longCast(short x){
    return x;
}

static public long longCast(int x){
	return x;
}

static public long longCast(float x){
	if(x < Long.MIN_VALUE || x > Long.MAX_VALUE)
		throw new IllegalArgumentException("Value out of range for long: " + x);
	return (long) x;
}

static public long longCast(long x){
	return x;
}

static public long longCast(double x){
	if(x < Long.MIN_VALUE || x > Long.MAX_VALUE)
		throw new IllegalArgumentException("Value out of range for long: " + x);
	return (long) x;
}

static public float floatCast(Object x){
	if(x instanceof Float)
		return ((Float) x).floatValue();

	double n = ((Number) x).doubleValue();
	if(n < -Float.MAX_VALUE || n > Float.MAX_VALUE)
		throw new IllegalArgumentException("Value out of range for float: " + x);

	return (float) n;

}

static public float floatCast(byte x){
    return x;
}

static public float floatCast(short x){
    return x;
}

static public float floatCast(int x){
	return x;
}

static public float floatCast(float x){
	return x;
}

static public float floatCast(long x){
	return x;
}

static public float floatCast(double x){
	if(x < -Float.MAX_VALUE || x > Float.MAX_VALUE)
		throw new IllegalArgumentException("Value out of range for float: " + x);

	return (float) x;
}

static public double doubleCast(Object x){
	return ((Number) x).doubleValue();
}

static public double doubleCast(byte x){
    return x;
}

static public double doubleCast(short x){
    return x;
}

static public double doubleCast(int x){
	return x;
}

static public double doubleCast(float x){
	return x;
}

static public double doubleCast(long x){
	return x;
}

static public double doubleCast(double x){
	return x;
}

static public byte uncheckedByteCast(Object x){
    return ((Number) x).byteValue();
}

static public byte uncheckedByteCast(byte x){
    return x;
}

static public byte uncheckedByteCast(short x){
    return (byte) x;
}

static public byte uncheckedByteCast(int x){
    return (byte) x;
}

static public byte uncheckedByteCast(long x){
    return (byte) x;
}

static public byte uncheckedByteCast(float x){
    return (byte) x;
}

static public byte uncheckedByteCast(double x){
    return (byte) x;
}

static public short uncheckedShortCast(Object x){
    return ((Number) x).shortValue();
}

static public short uncheckedShortCast(byte x){
    return x;
}

static public short uncheckedShortCast(short x){
    return x;
}

static public short uncheckedShortCast(int x){
    return (short) x;
}

static public short uncheckedShortCast(long x){
    return (short) x;
}

static public short uncheckedShortCast(float x){
    return (short) x;
}

static public short uncheckedShortCast(double x){
    return (short) x;
}

static public char uncheckedCharCast(Object x){
    if(x instanceof Character)
	return ((Character) x).charValue();
    return (char) ((Number) x).longValue();
}

static public char uncheckedCharCast(byte x){
    return (char) x;
}

static public char uncheckedCharCast(short x){
    return (char) x;
}

static public char uncheckedCharCast(char x){
    return x;
}

static public char uncheckedCharCast(int x){
    return (char) x;
}

static public char uncheckedCharCast(long x){
    return (char) x;
}

static public char uncheckedCharCast(float x){
    return (char) x;
}

static public char uncheckedCharCast(double x){
    return (char) x;
}

static public int uncheckedIntCast(Object x){
    if(x instanceof Number)
	return ((Number)x).intValue();
    return ((Character) x).charValue();
}

static public int uncheckedIntCast(byte x){
    return x;
}

static public int uncheckedIntCast(short x){
    return x;
}

static public int uncheckedIntCast(char x){
    return x;
}

static public int uncheckedIntCast(int x){
    return x;
}

static public int uncheckedIntCast(long x){
    return (int) x;
}

static public int uncheckedIntCast(float x){
    return (int) x;
}

static public int uncheckedIntCast(double x){
    return (int) x;
}

static public long uncheckedLongCast(Object x){
    return ((Number) x).longValue();
}

static public long uncheckedLongCast(byte x){
    return x;
}

static public long uncheckedLongCast(short x){
    return x;
}

static public long uncheckedLongCast(int x){
    return x;
}

static public long uncheckedLongCast(long x){
    return x;
}

static public long uncheckedLongCast(float x){
    return (long) x;
}

static public long uncheckedLongCast(double x){
    return (long) x;
}

static public float uncheckedFloatCast(Object x){
    return ((Number) x).floatValue();
}

static public float uncheckedFloatCast(byte x){
    return x;
}

static public float uncheckedFloatCast(short x){
    return x;
}

static public float uncheckedFloatCast(int x){
    return x;
}

static public float uncheckedFloatCast(long x){
    return x;
}

static public float uncheckedFloatCast(float x){
    return x;
}

static public float uncheckedFloatCast(double x){
    return (float) x;
}

static public double uncheckedDoubleCast(Object x){
    return ((Number) x).doubleValue();
}

static public double uncheckedDoubleCast(byte x){
    return x;
}

static public double uncheckedDoubleCast(short x){
    return x;
}

static public double uncheckedDoubleCast(int x){
    return x;
}

static public double uncheckedDoubleCast(long x){
    return x;
}

static public double uncheckedDoubleCast(float x){
    return x;
}

static public double uncheckedDoubleCast(double x){
    return x;
}

static public IPersistentMap map(Object... init){
	if(init == null || init.length == 0)
		return PersistentArrayMap.EMPTY;
	else if(init.length <= PersistentArrayMap.HASHTABLE_THRESHOLD)
		return PersistentArrayMap.createWithCheck(init);
	return PersistentHashMap.createWithCheck(init);
}

static public IPersistentMap mapUniqueKeys(Object... init){
	if(init == null)
		return PersistentArrayMap.EMPTY;
	else if(init.length <= PersistentArrayMap.HASHTABLE_THRESHOLD)
		return new PersistentArrayMap(init);
	return PersistentHashMap.create(init);
}

static public IPersistentSet set(Object... init){
	return PersistentHashSet.createWithCheck(init);
}

static public IPersistentVector vector(Object... init){
	return LazilyPersistentVector.createOwning(init);
}

static public IPersistentVector subvec(IPersistentVector v, int start, int end){
	if(end < start || start < 0 || end > v.count())
		throw new IndexOutOfBoundsException();
	if(start == end)
		return PersistentVector.EMPTY;
	return new APersistentVector.SubVector(null, v, start, end);
}

/**
 * **************************************** list support *******************************
 */


static public ISeq list(){
	return null;
}

static public ISeq list(Object arg1){
	return new PersistentList(arg1);
}

static public ISeq list(Object arg1, Object arg2){
	return listStar(arg1, arg2, null);
}

static public ISeq list(Object arg1, Object arg2, Object arg3){
	return listStar(arg1, arg2, arg3, null);
}

static public ISeq list(Object arg1, Object arg2, Object arg3, Object arg4){
	return listStar(arg1, arg2, arg3, arg4, null);
}

static public ISeq list(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5){
	return listStar(arg1, arg2, arg3, arg4, arg5, null);
}

static public ISeq listStar(Object arg1, ISeq rest){
	return (ISeq) cons(arg1, rest);
}

static public ISeq listStar(Object arg1, Object arg2, ISeq rest){
	return (ISeq) cons(arg1, cons(arg2, rest));
}

static public ISeq listStar(Object arg1, Object arg2, Object arg3, ISeq rest){
	return (ISeq) cons(arg1, cons(arg2, cons(arg3, rest)));
}

static public ISeq listStar(Object arg1, Object arg2, Object arg3, Object arg4, ISeq rest){
	return (ISeq) cons(arg1, cons(arg2, cons(arg3, cons(arg4, rest))));
}

static public ISeq listStar(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, ISeq rest){
	return (ISeq) cons(arg1, cons(arg2, cons(arg3, cons(arg4, cons(arg5, rest)))));
}

static public ISeq arrayToList(Object[] a) {
	ISeq ret = null;
	for(int i = a.length - 1; i >= 0; --i)
		ret = (ISeq) cons(a[i], ret);
	return ret;
}

static public Object[] object_array(Object sizeOrSeq){
	if(sizeOrSeq instanceof Number)
		return new Object[((Number) sizeOrSeq).intValue()];
	else
		{
		ISeq s = RT.seq(sizeOrSeq);
		int size = RT.count(s);
		Object[] ret = new Object[size];
		for(int i = 0; i < size && s != null; i++, s = s.next())
			ret[i] = s.first();
		return ret;
		}
}

static public Object[] toArray(Object coll) {
	if(coll == null)
		return EMPTY_ARRAY;
	else if(coll instanceof Object[])
		return (Object[]) coll;
	else if(coll instanceof Collection)
		return ((Collection) coll).toArray();
	else if(coll instanceof Iterable) {
		ArrayList ret = new ArrayList();
		for(Object o : (Iterable)coll)
			ret.add(o);
		return ret.toArray();
	} else if(coll instanceof Map)
		return ((Map) coll).entrySet().toArray();
	else if(coll instanceof String) {
		char[] chars = ((String) coll).toCharArray();
		Object[] ret = new Object[chars.length];
		for(int i = 0; i < chars.length; i++)
			ret[i] = chars[i];
		return ret;
	}
	else if(coll.getClass().isArray()) {
		ISeq s = (seq(coll));
		Object[] ret = new Object[count(s)];
		for(int i = 0; i < ret.length; i++, s = s.next())
			ret[i] = s.first();
		return ret;
	}
	else
		throw Util.runtimeException("Unable to convert: " + coll.getClass() + " to Object[]");
}

static public Object[] seqToArray(ISeq seq){
	int len = length(seq);
	Object[] ret = new Object[len];
	for(int i = 0; seq != null; ++i, seq = seq.next())
		ret[i] = seq.first();
	return ret;
}

    // supports java Collection.toArray(T[])
    static public Object[] seqToPassedArray(ISeq seq, Object[] passed){
        Object[] dest = passed;
        int len = count(seq);
        if (len > dest.length) {
            dest = (Object[]) Array.newInstance(passed.getClass().getComponentType(), len);
        }
        for(int i = 0; seq != null; ++i, seq = seq.next())
            dest[i] = seq.first();
        if (len < passed.length) {
            dest[len] = null;
        }
        return dest;
    }

static public Object seqToTypedArray(ISeq seq) {
	Class type = (seq != null && seq.first() != null) ? seq.first().getClass() : Object.class;
	return seqToTypedArray(type, seq);
}

static public Object seqToTypedArray(Class type, ISeq seq) {
    Object ret = Array.newInstance(type, length(seq));
    if(type == Integer.TYPE){
        for(int i = 0; seq != null; ++i, seq=seq.next()){
            Array.set(ret, i, intCast(seq.first()));
        }
    } else if(type == Byte.TYPE) {
        for(int i = 0; seq != null; ++i, seq=seq.next()){
            Array.set(ret, i, byteCast(seq.first()));
        }
    } else if(type == Float.TYPE) {
        for(int i = 0; seq != null; ++i, seq=seq.next()){
            Array.set(ret, i, floatCast(seq.first()));
        }
    } else if(type == Short.TYPE) {
        for(int i = 0; seq != null; ++i, seq=seq.next()){
            Array.set(ret, i, shortCast(seq.first()));
        }
    } else if(type == Character.TYPE) {
        for(int i = 0; seq != null; ++i, seq=seq.next()){
            Array.set(ret, i, charCast(seq.first()));
        }
    } else {
        for(int i = 0; seq != null; ++i, seq=seq.next()){
            Array.set(ret, i, seq.first());
        }
    }
	return ret;
}

///////////////////////////////// reader support ////////////////////////////////

static Character readRet(int ret){
	if(ret == -1)
		return null;
	return box((char) ret);
}

static public Character readChar(Reader r) throws IOException{
	int ret = r.read();
	return readRet(ret);
}

static public Character peekChar(Reader r) throws IOException{
	int ret;
	if(r instanceof PushbackReader) {
		ret = r.read();
		((PushbackReader) r).unread(ret);
	}
	else {
		r.mark(1);
		ret = r.read();
		r.reset();
	}

	return readRet(ret);
}

static public int getLineNumber(Reader r){
	if(r instanceof LineNumberingPushbackReader)
		return ((LineNumberingPushbackReader) r).getLineNumber();
	return 0;
}

static public int getColumnNumber(Reader r){
	if(r instanceof LineNumberingPushbackReader)
		return ((LineNumberingPushbackReader) r).getColumnNumber();
	return 0;
}

static public LineNumberingPushbackReader getLineNumberingReader(Reader r){
	if(isLineNumberingReader(r))
		return (LineNumberingPushbackReader) r;
	return new LineNumberingPushbackReader(r);
}

static public boolean isLineNumberingReader(Reader r){
	return r instanceof LineNumberingPushbackReader;
}

static public String resolveClassNameInContext(String className){
	//todo - look up in context var
	return className;
}

static public boolean suppressRead(){
	return booleanCast(SUPPRESS_READ.deref());
}

static public String printString(Object x){
	try {
		StringWriter sw = new StringWriter();
		print(x, sw);
		return sw.toString();
	}
	catch(Exception e) {
		throw Util.sneakyThrow(e);
	}
}

static public Object readString(String s){
	return readString(s, null);
}

static public Object readString(String s, Object opts) {
	PushbackReader r = new PushbackReader(new StringReader(s));
	return LispReader.read(r, opts);
}

static public void print(Object x, Writer w) throws IOException{
	//call multimethod
	if(PRINT_INITIALIZED.isBound() && RT.booleanCast(PRINT_INITIALIZED.deref()))
		PR_ON.invoke(x, w);
//*
	else {
		boolean readably = booleanCast(PRINT_READABLY.deref());
		if(x instanceof Obj) {
			Obj o = (Obj) x;
			if(RT.count(o.meta()) > 0 &&
			   ((readably && booleanCast(PRINT_META.deref()))
			    || booleanCast(PRINT_DUP.deref()))) {
				IPersistentMap meta = o.meta();
				w.write("#^");
				if(meta.count() == 1 && meta.containsKey(TAG_KEY))
					print(meta.valAt(TAG_KEY), w);
				else
					print(meta, w);
				w.write(' ');
			}
		}
		if(x == null)
			w.write("nil");
		else if(x instanceof ISeq || x instanceof IPersistentList) {
			w.write('(');
			printInnerSeq(seq(x), w);
			w.write(')');
		}
		else if(x instanceof String) {
			String s = (String) x;
			if(!readably)
				w.write(s);
			else {
				w.write('"');
				//w.write(x.toString());
				for(int i = 0; i < s.length(); i++) {
					char c = s.charAt(i);
					switch(c) {
						case '\n':
							w.write("\\n");
							break;
						case '\t':
							w.write("\\t");
							break;
						case '\r':
							w.write("\\r");
							break;
						case '"':
							w.write("\\\"");
							break;
						case '\\':
							w.write("\\\\");
							break;
						case '\f':
							w.write("\\f");
							break;
						case '\b':
							w.write("\\b");
							break;
						default:
							w.write(c);
					}
				}
				w.write('"');
			}
		}
		else if(x instanceof IPersistentMap) {
			w.write('{');
			for(ISeq s = seq(x); s != null; s = s.next()) {
				IMapEntry e = (IMapEntry) s.first();
				print(e.key(), w);
				w.write(' ');
				print(e.val(), w);
				if(s.next() != null)
					w.write(", ");
			}
			w.write('}');
		}
		else if(x instanceof IPersistentVector) {
			IPersistentVector a = (IPersistentVector) x;
			w.write('[');
			for(int i = 0; i < a.count(); i++) {
				print(a.nth(i), w);
				if(i < a.count() - 1)
					w.write(' ');
			}
			w.write(']');
		}
		else if(x instanceof IPersistentSet) {
			w.write("#{");
			for(ISeq s = seq(x); s != null; s = s.next()) {
				print(s.first(), w);
				if(s.next() != null)
					w.write(" ");
			}
			w.write('}');
		}
		else if(x instanceof Character) {
			char c = ((Character) x).charValue();
			if(!readably)
				w.write(c);
			else {
				w.write('\\');
				switch(c) {
					case '\n':
						w.write("newline");
						break;
					case '\t':
						w.write("tab");
						break;
					case ' ':
						w.write("space");
						break;
					case '\b':
						w.write("backspace");
						break;
					case '\f':
						w.write("formfeed");
						break;
					case '\r':
						w.write("return");
						break;
					default:
						w.write(c);
				}
			}
		}
		else if(x instanceof Class) {
			w.write("#=");
			w.write(((Class) x).getName());
		}
		else if(x instanceof BigDecimal && readably) {
			w.write(x.toString());
			w.write('M');
		}
		else if(x instanceof BigInt && readably) {
			w.write(x.toString());
			w.write('N');
		}
		else if(x instanceof BigInteger && readably) {
			w.write(x.toString());
			w.write("BIGINT");
		}
		else if(x instanceof Var) {
			Var v = (Var) x;
			w.write("#=(var " + v.ns.name + "/" + v.sym + ")");
		}
		else if(x instanceof Pattern) {
			Pattern p = (Pattern) x;
			w.write("#\"" + p.pattern() + "\"");
		}
		else w.write(x.toString());
	}
	//*/
}

private static void printInnerSeq(ISeq x, Writer w) throws IOException{
	for(ISeq s = x; s != null; s = s.next()) {
		print(s.first(), w);
		if(s.next() != null)
			w.write(' ');
	}
}

static public void formatAesthetic(Writer w, Object obj) throws IOException{
	if(obj == null)
		w.write("null");
	else
		w.write(obj.toString());
}

static public void formatStandard(Writer w, Object obj) throws IOException{
	if(obj == null)
		w.write("null");
	else if(obj instanceof String) {
		w.write('"');
		w.write((String) obj);
		w.write('"');
	}
	else if(obj instanceof Character) {
		w.write('\\');
		char c = ((Character) obj).charValue();
		switch(c) {
			case '\n':
				w.write("newline");
				break;
			case '\t':
				w.write("tab");
				break;
			case ' ':
				w.write("space");
				break;
			case '\b':
				w.write("backspace");
				break;
			case '\f':
				w.write("formfeed");
				break;
			default:
				w.write(c);
		}
	}
	else
		w.write(obj.toString());
}

static public Object format(Object o, String s, Object... args) throws IOException{
	Writer w;
	if(o == null)
		w = new StringWriter();
	else if(Util.equals(o, T))
		w = (Writer) OUT.deref();
	else
		w = (Writer) o;
	doFormat(w, s, ArraySeq.create(args));
	if(o == null)
		return w.toString();
	return null;
}

static public ISeq doFormat(Writer w, String s, ISeq args) throws IOException{
	for(int i = 0; i < s.length();) {
		char c = s.charAt(i++);
		switch(Character.toLowerCase(c)) {
			case '~':
				char d = s.charAt(i++);
				switch(Character.toLowerCase(d)) {
					case '%':
						w.write('\n');
						break;
					case 't':
						w.write('\t');
						break;
					case 'a':
						if(args == null)
							throw new IllegalArgumentException("Missing argument");
						RT.formatAesthetic(w, RT.first(args));
						args = RT.next(args);
						break;
					case 's':
						if(args == null)
							throw new IllegalArgumentException("Missing argument");
						RT.formatStandard(w, RT.first(args));
						args = RT.next(args);
						break;
					case '{':
						int j = s.indexOf("~}", i);    //note - does not nest
						if(j == -1)
							throw new IllegalArgumentException("Missing ~}");
						String subs = s.substring(i, j);
						for(ISeq sargs = RT.seq(RT.first(args)); sargs != null;)
							sargs = doFormat(w, subs, sargs);
						args = RT.next(args);
						i = j + 2; //skip ~}
						break;
					case '^':
						if(args == null)
							return null;
						break;
					case '~':
						w.write('~');
						break;
					default:
						throw new IllegalArgumentException("Unsupported ~ directive: " + d);
				}
				break;
			default:
				w.write(c);
		}
	}
	return args;
}
///////////////////////////////// values //////////////////////////


static public ClassLoader makeClassLoader(){
	return (ClassLoader) AccessController.doPrivileged(new PrivilegedAction(){
		public Object run(){
            try{
            Var.pushThreadBindings(RT.map(USE_CONTEXT_CLASSLOADER, RT.T));
//			getRootClassLoader();
			return new DynamicClassLoader(baseLoader());
            }
                finally{
            Var.popThreadBindings();
            }
		}
	});
}

static public ClassLoader baseLoader(){
	if(Compiler.LOADER.isBound())
		return (ClassLoader) Compiler.LOADER.deref();
	else if(booleanCast(USE_CONTEXT_CLASSLOADER.deref()))
		return Thread.currentThread().getContextClassLoader();
	return Compiler.class.getClassLoader();
}

static public InputStream resourceAsStream(ClassLoader loader, String name){
    if (loader == null) {
        return ClassLoader.getSystemResourceAsStream(name);
    } else {
        return loader.getResourceAsStream(name);
    }
}

static public URL getResource(ClassLoader loader, String name){
    if (loader == null) {
        return ClassLoader.getSystemResource(name);
    } else {
        return loader.getResource(name);
    }
}

static public Class classForName(String name, boolean load, ClassLoader loader) {

	try
		{
		Class c = null;
		if (!(loader instanceof DynamicClassLoader))
			c = DynamicClassLoader.findInMemoryClass(name);
		if (c != null)
			return c;
		return Class.forName(name, load, loader);
		}
	catch(ClassNotFoundException e)
		{
		throw Util.sneakyThrow(e);
		}
}

static public Class classForName(String name) {
	return classForName(name, true, baseLoader());
}

static public Class classForNameNonLoading(String name) {
	return classForName(name, false, baseLoader());
}

static public Class loadClassForName(String name) {
	try
		{
		classForNameNonLoading(name);
		}
	catch(Exception e)
		{
		if (e instanceof ClassNotFoundException)
			return null;
		else
			throw Util.sneakyThrow(e);
		}
	return classForName(name);
}

static public float aget(float[] xs, int i){
	return xs[i];
}

static public float aset(float[] xs, int i, float v){
	xs[i] = v;
	return v;
}

static public int alength(float[] xs){
	return xs.length;
}

static public float[] aclone(float[] xs){
	return xs.clone();
}

static public double aget(double[] xs, int i){
	return xs[i];
}

static public double aset(double[] xs, int i, double v){
	xs[i] = v;
	return v;
}

static public int alength(double[] xs){
	return xs.length;
}

static public double[] aclone(double[] xs){
	return xs.clone();
}

static public int aget(int[] xs, int i){
	return xs[i];
}

static public int aset(int[] xs, int i, int v){
	xs[i] = v;
	return v;
}

static public int alength(int[] xs){
	return xs.length;
}

static public int[] aclone(int[] xs){
	return xs.clone();
}

static public long aget(long[] xs, int i){
	return xs[i];
}

static public long aset(long[] xs, int i, long v){
	xs[i] = v;
	return v;
}

static public int alength(long[] xs){
	return xs.length;
}

static public long[] aclone(long[] xs){
	return xs.clone();
}

static public char aget(char[] xs, int i){
	return xs[i];
}

static public char aset(char[] xs, int i, char v){
	xs[i] = v;
	return v;
}

static public int alength(char[] xs){
	return xs.length;
}

static public char[] aclone(char[] xs){
	return xs.clone();
}

static public byte aget(byte[] xs, int i){
	return xs[i];
}

static public byte aset(byte[] xs, int i, byte v){
	xs[i] = v;
	return v;
}

static public int alength(byte[] xs){
	return xs.length;
}

static public byte[] aclone(byte[] xs){
	return xs.clone();
}

static public short aget(short[] xs, int i){
	return xs[i];
}

static public short aset(short[] xs, int i, short v){
	xs[i] = v;
	return v;
}

static public int alength(short[] xs){
	return xs.length;
}

static public short[] aclone(short[] xs){
	return xs.clone();
}

static public boolean aget(boolean[] xs, int i){
	return xs[i];
}

static public boolean aset(boolean[] xs, int i, boolean v){
	xs[i] = v;
	return v;
}

static public int alength(boolean[] xs){
	return xs.length;
}

static public boolean[] aclone(boolean[] xs){
	return xs.clone();
}

static public Object aget(Object[] xs, int i){
	return xs[i];
}

static public Object aset(Object[] xs, int i, Object v){
	xs[i] = v;
	return v;
}

static public int alength(Object[] xs){
	return xs.length;
}

static public Object[] aclone(Object[] xs){
	return xs.clone();
}


}
