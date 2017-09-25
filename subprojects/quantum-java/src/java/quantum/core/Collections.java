package quantum.core;

import java.util.Map;
import java.util.HashMap;
import static quantum.core.Core.throw_;
import quantum.core.Numeric;

public class Collections {
  // ----- ASSOC ----- //
    public static <K, V> Map<K, V> assocM (Map<K, V> m, K k0, V v0, Object... kvs) {
      if (! Numeric.isEven(kvs.length)) {
        throw_(new IllegalArgumentException("HashMap must have an even number of arguments (keys and values)."));
        return null;
      } else {
        m.put(k0, v0);

        for (int i = 0; i < kvs.length; i += 2) {
            K k = (K) kvs[i];
            V v = (V) kvs[i+1];
            m.put(k, v);
        }

        return m;
      }
    }

  // ----- HASH-MAP ----- //

  // "hashMapMS" — "M" because mutable, "S" because single-threaded
  public static <K, V> HashMap<K, V> hashMapMS(K k0, V v0, Object... kvs) {
      return (HashMap<K, V>)assocM(new HashMap<K, V>(), k0, v0, kvs);
  }
}
