// Compiled by ClojureScript 0.0-3269 {}
goog.provide('quantum.core.data.vector');
goog.require('cljs.core');
goog.require('quantum.core.ns');
goog.require('quantum.core.type');
goog.require('clojure.core.rrb_vector');
goog.require('clojure.core.rrb_vector.rrbt');
quantum.core.data.vector.catvec = clojure.core.rrb_vector.catvec;
quantum.core.data.vector.vec_PLUS_ = clojure.core.rrb_vector.vec;
quantum.core.data.vector.vector_PLUS_ = clojure.core.rrb_vector.vector;
/**
 * Produces a new vector containing the appropriate subrange of the input vector in logarithmic time
 * (in contrast to clojure.core/subvec, which returns a reference to the input vector)
 * clojure.core/subvec is a constant-time operation that prevents the underlying vector
 * from becoming eligible for garbage collection
 */
quantum.core.data.vector.subvec_PLUS_ = (function quantum$core$data$vector$subvec_PLUS_(coll,a,b){
try{return clojure.core.rrb_vector.subvec.call(null,coll,a,b);
}catch (e23058){if((e23058 instanceof Error)){
var _ = e23058;
return cljs.core.subvec.call(null,coll,a,b);
} else {
throw e23058;

}
}});
quantum.core.data.vector.vector_PLUS__QMARK_ = (function quantum$core$data$vector$vector_PLUS__QMARK_(obj){
return quantum.core.type.instance_PLUS__QMARK_.call(null,clojure.core.rrb_vector.rrbt.Vector,obj);
});
quantum.core.data.vector.conjl = (function quantum$core$data$vector$conjl(){
var G__23063 = arguments.length;
switch (G__23063) {
case 2:
return quantum.core.data.vector.conjl.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
var argseq__19124__auto__ = (new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(2)),(0)));
return quantum.core.data.vector.conjl.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),argseq__19124__auto__);

}
});

quantum.core.data.vector.conjl.cljs$core$IFn$_invoke$arity$2 = (function (vec_0,elem){
return quantum.core.data.vector.catvec.call(null,quantum.core.data.vector.vector_PLUS_.call(null,elem),vec_0);
});

quantum.core.data.vector.conjl.cljs$core$IFn$_invoke$arity$variadic = (function (vec_0,elem,elems){
return cljs.core.reduce.call(null,quantum.core.data.vector.conjl,elem,elems);
});

quantum.core.data.vector.conjl.cljs$lang$applyTo = (function (seq23059){
var G__23060 = cljs.core.first.call(null,seq23059);
var seq23059__$1 = cljs.core.next.call(null,seq23059);
var G__23061 = cljs.core.first.call(null,seq23059__$1);
var seq23059__$2 = cljs.core.next.call(null,seq23059__$1);
return quantum.core.data.vector.conjl.cljs$core$IFn$_invoke$arity$variadic(G__23060,G__23061,seq23059__$2);
});

quantum.core.data.vector.conjl.cljs$lang$maxFixedArity = (2);

//# sourceMappingURL=vector.js.map?rel=1431625568677