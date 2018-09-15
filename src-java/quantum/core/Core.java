package quantum.core;

import clojure.lang.Keyword;
import clojure.lang.Util;
import static clojure.java.api.Clojure.var;

public class Core {
  public static Object call (String varName)                                                        { return var(varName).invoke();                   }
  public static Object call (String varName, Object a0)                                             { return var(varName).invoke(a0);                 }
  public static Object call (String varName, Object a0, Object a1)                                  { return var(varName).invoke(a0, a1);             }
  public static Object call (String varName, Object a0, Object a1, Object a2)                       { return var(varName).invoke(a0, a1, a2);         }
  public static Object call (String varName, Object a0, Object a1, Object a2, Object a3)            { return var(varName).invoke(a0, a1, a2, a3);     }
  public static Object call (String varName, Object a0, Object a1, Object a2, Object a3, Object a4) { return var(varName).invoke(a0, a1, a2, a3, a4); }

  public static Keyword keyword (String name) { return Keyword.intern(name); }

  public static Object throw_ (Throwable t) { Util.sneakyThrow(t); return null; }

  public static Class classOf (Object x) { return (x == null) ? null : x.getClass(); }
}
