package quantum.core;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import static clojure.java.api.Clojure.var;
import clojure.lang.APersistentMap;
import clojure.lang.ILookupThunk;
import clojure.lang.IMapEntry;
import clojure.lang.IObj;
import clojure.lang.IPersistentCollection;
import clojure.lang.IPersistentMap;
import clojure.lang.IPersistentSet;
import clojure.lang.ISeq;
import clojure.lang.Keyword;
import clojure.lang.MapEntry;
import clojure.lang.Numbers;
import clojure.lang.PersistentHashMap;
import clojure.lang.RT;
import clojure.lang.Tuple;
import clojure.lang.Util;
import static quantum.core.Collections.hashMapMS;
import static quantum.core.Core.call;
import static quantum.core.Core.classOf;
import static quantum.core.Core.keyword;
import static quantum.core.Core.throw_;

// Like `clojure.lang.ExceptionInfo`, but a record
// This is expanded from `(macroexpand-all '(defrecord Error [ident message data trace cause]))`, while extending `RuntimeException`
public class Error
   extends
     RuntimeException
   implements
       clojure.lang.IRecord
     , clojure.lang.IHashEq
     , IObj
     , clojure.lang.ILookup
     , clojure.lang.IKeywordLookup
     , IPersistentMap
     , java.util.Map
     , java.io.Serializable {
    // static {
    //   call("clojure.core/prefer-method", var("clojure.core/print-method"), clojure.lang.IRecord.class, java.lang.Throwable.class);
    // }

    public         final Object  ident;
    private static final Keyword ident_kw   = keyword("ident");
                              // `message` from superclass's `message`
    private static final Keyword message_kw = keyword("message");
    public         final Object  data;
    private static final Keyword data_kw    = keyword("data");
                              // `trace`   from superclass's `stackTrace`
    private static final Keyword trace_kw   = keyword("trace");
                              // `cause`   from superclass's `cause`
    private static final Keyword cause_kw   = keyword("cause");

    public final IPersistentMap __meta;
    public final IPersistentMap __extmap;
    public       int            __hash;
    public       int            __hasheq;

    public Set                   entrySet ()                   { return (Set)       call("clojure.core/set" , this); }
    public Collection            values   ()                   { return (Collection)call("clojure.core/vals", this); }
    public Object                keySeq   ()                   { return             call("clojure.core/set" , this); }
    public Set                   keySet   ()                   { return (Set)       call("clojure.core/set" , call("clojure.core/keys", this)); }
    public void                  clear    ()                   { throw new UnsupportedOperationException(); }
    public void                  putAll   (Map m)              { throw new UnsupportedOperationException(); }
    public Object                remove   (Object k)           { throw new UnsupportedOperationException(); }
    public Object                put      (Object k, Object v) { throw new UnsupportedOperationException(); }
    public Object                get      (Object k)           { return this.valAt(k); }
    public boolean               isEmpty  ()                   { return this.count() == 0; }
    public int                   count    ()                   { return 5 + RT.count(__extmap); }
    public int                   size     ()                   { return this.count(); }
    public IPersistentCollection cons     (Object e)           { return (IPersistentCollection)call("clojure.core/imap-cons", this, e); }
    public IPersistentCollection empty    ()                   { throw new UnsupportedOperationException("Can't create empty: quantum.core.error.Error"); }

    // CONTAINMENT

    public boolean containsKey (Object k) {
      return this != this.valAt(k, this);
    }

    public boolean containsValue (Object v) {
      return (boolean)call(  "clojure.core/some"
                           , RT.set(new Object[]{v})
                           , call("clojure.core/vals", this));
    }

    // RETRIEVAL

    public Object valAt (Object k, Object elsev) {
      if (k instanceof Keyword) {
        switch(((Keyword)k).getName()) {
          case "ident"  : return this.ident                                       ;
          case "message": return super.getLocalizedMessage()                      ;
          case "data"   : return this.data                                        ;
          case "trace"  : return super.getStackTrace()                            ;
          case "cause"  : return super.getCause()                                 ;
          default       : return call("clojure.core/get", this.__extmap, k, elsev);
        }
      } else {
        return call("clojure.core/get", this.__extmap, k, elsev);
      }
    }

    public Object valAt (Object k) { return this.valAt(k, null); }

    public ILookupThunk getLookupThunk (Keyword k) {
      switch(k.getName()) {
        case "ident"  : return new ILookupThunk () {
                                 public Object get (Object gtarget) {
                                   return (classOf(gtarget) == Error.class)
                                          ? ((Error)gtarget).ident
                                          : this;
                                 }
                               };
        case "message": return new ILookupThunk () {
                                 public Object get (Object gtarget) {
                                   return (classOf(gtarget) == Error.class)
                                          ? ((Error)gtarget).getLocalizedMessage()
                                          : this;
                                 }
                               };
        case "data"   : return new ILookupThunk () {
                                 public Object get (Object gtarget) {
                                   return (classOf(gtarget) == Error.class)
                                          ? ((Error)gtarget).data
                                          : this;
                                 }
                               };
        case "trace"  : return new ILookupThunk () {
                                 public Object get (Object gtarget) {
                                   return (classOf(gtarget) == Error.class)
                                          ? ((Error)gtarget).getStackTrace()
                                          : this;
                                 }
                               };
        case "cause"  : return new ILookupThunk () {
                                 public Object get (Object gtarget) {
                                   return (classOf(gtarget) == Error.class)
                                          ? ((Error)gtarget).getCause()
                                          : this;
                                 }
                               };
        default       : return null;
      }
    }

    private static Object sentinel = new Object();

    public IMapEntry entryAt (Object k) {
      Object v = this.valAt(k, sentinel);
      return (v == sentinel)
             ? null
             : MapEntry.create(k, v);
    }

    public IPersistentMap assoc (Object k, Object v) {
      return   (ident_kw   == k)
             ? new Error(v    , super.getLocalizedMessage(), this.data, super.getStackTrace() , super.getCause(), this.__meta, this.__extmap)
             : (message_kw == k)
             ? new Error(ident, (String)v                  , this.data, super.getStackTrace() , super.getCause(), this.__meta, this.__extmap)
             : (data_kw    == k)
             ? new Error(ident, super.getLocalizedMessage(), v        , super.getStackTrace() , super.getCause(), this.__meta, this.__extmap)
             : (trace_kw   == k)
             ? new Error(ident, super.getLocalizedMessage(), this.data, (StackTraceElement[])v, super.getCause(), this.__meta, this.__extmap)
             : (cause_kw   == k)
             ? new Error(ident, super.getLocalizedMessage(), this.data, super.getStackTrace() , (Throwable)v    , this.__meta, this.__extmap)
             : new Error(ident, super.getLocalizedMessage(), this.data, super.getStackTrace() , super.getCause(), this.__meta, (IPersistentMap)call("clojure.core/assoc", this.__extmap, k, v));
    }

    public IPersistentMap assocEx (Object k, Object v) { throw new UnsupportedOperationException(); }

    public static IPersistentSet requiredKeys = RT.set(new Object[]{keyword("cause"), keyword("trace"), keyword("ident"), keyword("message"), keyword("data")});

    public IPersistentMap without (Object k) {
      return ((boolean)call("clojure.core/contains?", requiredKeys, k))
             ? (IPersistentMap)call(  "clojure.core/dissoc"
                                    , call(  "clojure.core/with-meta"
                                           , call("clojure.core/into", PersistentHashMap.EMPTY, this)
                                           , this.__meta)
                                    , k)
             : new Error(  this.ident, super.getLocalizedMessage(), this.data, super.getStackTrace(), super.getCause(), this.__meta
                         , (IPersistentMap)call(  "clojure.core/not-empty"
                                                , call("clojure.core/dissoc", this.__extmap, k)));
    }

    public IPersistentMap meta () { return this.__meta; }

    public IObj withMeta (IPersistentMap meta) {
      return new Error(  this.ident, super.getLocalizedMessage(), this.data, super.getStackTrace(), super.getCause()
                       , meta, this.__extmap, this.__hash, this.__hasheq);
    }

    public Iterator iterator () {
      return new clojure.lang.RecordIterator(this, Tuple.create(ident_kw, message_kw, data_kw, trace_kw, cause_kw), RT.iter(this.__extmap));
    }

    public ISeq seq () {
      return (ISeq)call(  "clojure.core/seq"
                        , call(  "clojure.core/concat"
                               , Tuple.create(  MapEntry.create(ident_kw  , this.ident)
                                              , MapEntry.create(message_kw, super.getLocalizedMessage())
                                              , MapEntry.create(data_kw   , this.data)
                                              , MapEntry.create(trace_kw  , super.getStackTrace())
                                              , MapEntry.create(cause_kw  , super.getCause()))
                               , this.__extmap));
    }

    // HASHING

    public int hashCode () {
      if (this.__hash == 0) {
        this.__hash = APersistentMap.mapHash(this);
      }

      return this.__hash;
    }

    public int hasheq () {
      if (this.__hasheq == 0) {
        this.__hasheq = (int)Numbers.xor(1063013216, APersistentMap.mapHasheq(this));
      }

      return this.__hasheq;
    }

    // EQUALITY

    public boolean equals (Object that) { return APersistentMap.mapEquals(this, that); }

    public boolean equiv (Object that_) {
      if (this == that_) {
        return true;
      } else if (classOf(that_) == Error.class) {
        Error that = (Error)that_;
        return  (   Util.equiv(this.ident                 , that.ident)
                 && Util.equiv(super.getLocalizedMessage(), that.getLocalizedMessage())
                 && Util.equiv(this.data                  , that.data)
                 && Util.equiv(super.getStackTrace()      , that.getStackTrace())
                 && Util.equiv(super.getCause()           , that.getCause())
                 && Util.equiv(this.__extmap              , that.__extmap));
      } else {
        return false;
      }
    }

    // `trace_` is ignored
    public Error (  Object ident, String message, Object data, StackTraceElement[] trace, Throwable cause
                  , IPersistentMap __meta, IPersistentMap __extmap, int __hash, int __hasheq) {
      super(message, cause);
      if (trace != null) { super.setStackTrace(trace); }
      this.ident    = ident   ;
      this.data     = data    ;
      this.__meta   = __meta  ;
      this.__extmap = __extmap;
      this.__hash   = __hash  ;
      this.__hasheq = __hasheq;
    }

    // `trace_` is ignored
    public Error (  Object ident, String message, Object data, StackTraceElement[] trace, Throwable cause
                  , IPersistentMap __meta, IPersistentMap __extmap) {
      this(ident, message, data, trace, cause, __meta, __extmap, 0, 0);
    }

    // `trace_` is ignored
    public Error (Object ident, String message, Object data, StackTraceElement[] trace, Throwable cause) {
      this(ident, message, data, trace, cause, null, PersistentHashMap.EMPTY);
    }
}
