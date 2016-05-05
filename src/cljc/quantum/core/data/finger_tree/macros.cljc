(ns ^{:doc "Persistent collections based on 2-3 finger trees."
      :author "Chris Houser"}
  quantum.core.data.finger-tree.macros
  (:require [quantum.core.macros.deftype
              :refer [pfn ?Seqable ?Counted ?Indexed ?Sequential ?Seq
                      ?Stack ?Collection]]
   #?(:cljs [cljs.core
              :refer [ISeqable ISequential ISeq ISet ILookup
                      IStack ICollection IAssociative
                      ISorted IReversible IIndexed ICounted IHash
                      INext IEmptyableCollection IEquiv
                      IMeta IWithMeta]]))
  ;#?(:cljs (:require-macros [quantum.core.cljs.deps.macros :refer [quote+]]))
  #?(:clj  (:import (clojure.lang Seqable Sequential ISeq IPersistentSet ILookup
                                  IPersistentStack IPersistentCollection Associative
                                  Sorted Reversible Indexed Counted IHashEq
                                  IObj))))

#?(:clj
(defmacro defdigit
  {:compatibility #{:clj :cljs}}
  [lang & items]
  (let [i (gensym "i_")
        p (gensym "p_")
        o (gensym "o_")
        typename (symbol (str "Digit" (count items)))
        this-items (map #(list (keyword %) o) items)
        digit*        'quantum.core.data.finger-tree/digit
        newEmptyTree* 'quantum.core.data.finger-tree/newEmptyTree
        first*        `(~(pfn 'first    lang) [_#] ~(first items))
        next*         `(~(pfn 'next     lang) [_#] ~(when (> (count items) 1)
                         `(~digit* ~'meter-obj ~@(next items))))]
   `(deftype ~typename [~@items ~'meter-obj ~'measure-ref]
      ~(?Seqable lang)
        (~(pfn 'seq   lang) [_#] ~(reduce #(list `cons %2 %1) nil (reverse items)))
      ~(?Counted lang)
        (~(pfn 'count lang) [_#] ~(count items)) ; not needed?
      ~(?Indexed lang)
        (~(pfn 'nth   lang) [_# ~i notfound#]
          (cond ~@(mapcat (fn [sym n] [`(== ~i (int ~n)) sym])
                          items
                          (range (count items)))
            :else notfound#))
      ~(?Sequential lang)
      ~@(condp = lang
         :clj
          `[~(?Seq lang)
              ~first*
              (~(pfn 'more lang) [_#] ~(if (> (count items) 1)
                                          `(~digit* ~'meter-obj ~@(next items))
                                          `(~newEmptyTree* ~'meter-obj)))
              ~next*]
         :cljs
          `[~(?Seq lang)
              ~first*
              (~(pfn 'rest lang) [_#] ~(if (> (count items) 1)
                                          `(~digit* ~'meter-obj ~@(next items))
                                          `(~newEmptyTree* ~'meter-obj)))
            cljs.core/INext
              ~next*])
      ~(?Stack lang)
        (~(pfn 'peek     lang) [_#] ~(last items))
        (~(pfn 'pop      lang) [_#] ~(if (> (count items) 1)
                                        `(~digit* ~'meter-obj ~@(drop-last items))
                                        `(~newEmptyTree* ~'meter-obj)))
      ~@(condp = lang
          :clj
           `[~(?Collection lang)
              (~(pfn 'empty lang) [_#]) ; TBD ; not needed?
              (~(pfn 'equiv lang) [_# x#] false) ; TBD
              (~(pfn 'cons  lang) [_# x#] (~digit* ~'meter-obj ~@items x#))]
          :cljs
           `[cljs.core/IEmptyableCollection
               (~(pfn 'empty lang) [_#])
             cljs.core/IEquiv
               (~(pfn 'equiv lang) [_# x#] false) ; TBD
             ~(?Collection lang)
               (~(pfn 'conj  lang) [_# x#] (~digit* ~'meter-obj ~@items x#))])
      ~'ConjL
        (~'conjl    [_# x#] (~digit* ~'meter-obj x# ~@items))
      ~'Measured
        (~'measured [_#] @~'measure-ref)
        (~'getMeter [_#] ~'meter-obj) ; not needed?
      ~'Splittable ; allow to fail if op is nil:
        (~'split    [_# ~p ~i]
          ~(letfn [(step [ips [ix & ixs]]
                      (if (empty? ixs)
                        [(when ips `(~digit* ~'meter-obj ~@ips))
                         ix
                         nil]
                        `(let [~i ((~'opfn ~'meter-obj)
                                     ~i
                                     (~'measure ~'meter-obj ~ix))]
                           (if (~p ~i)
                             [~(when ips
                                 `(~digit* ~'meter-obj ~@ips))
                              ~ix
                              (~digit* ~'meter-obj ~@ixs)]
                             ~(step (concat ips [ix]) ixs)))))]
             (step nil items)))))))

#?(:clj
(defmacro make-digit [meter-obj & items]
  (let [typename (symbol (str "Digit" (count items)))]
    `(let [~'mobj ~meter-obj
           ~'op (quantum.core.data.finger-tree/opfn ~'mobj)]
       (new ~typename ~@items ~'mobj
            (when ~'op
              (delay ~(reduce #(list 'op %1 %2)
                              (map #(list 'quantum.core.data.finger-tree/measure 'mobj %) items)))))))))

;#?(:clj
;(defmacro meter [measure idElem op]
;  `(reify quantum.core.data.finger-tree/ObjMeter
;      (quantum.core.data.finger-tree/measure [_# a#] (~measure a#))
;      (quantum.core.data.finger-tree/idElem  [_#]    ~idElem)
;      (quantum.core.data.finger-tree/opfn    [_#]    ~op))))

#?(:clj
(defmacro delay-ft [tree-expr mval]
  `(quantum.core.data.finger_tree.DelayedTree. (delay ~tree-expr) ~mval)))

#?(:clj
(defmacro meter [measure idElem op]
  `(reify quantum.core.data.finger-tree/ObjMeter
     (quantum.core.data.finger-tree/measure [_# a#] (~measure a#))
     (quantum.core.data.finger-tree/idElem  [_#]    ~idElem)
     (quantum.core.data.finger-tree/opfn    [_#]    ~op))))