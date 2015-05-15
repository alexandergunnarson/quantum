// Compiled by ClojureScript 0.0-3269 {}
goog.provide('quantum.core.data.map');
goog.require('cljs.core');
goog.require('clojure.data.avl');
goog.require('quantum.core.ns');
/**
 * A performant replacement for creating 2-tuples (vectors), e.g., as return values
 * in a |kv-reduce| function.
 */
quantum.core.data.map.map_entry = (function quantum$core$data$map$map_entry(k,v){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [k,v], null);
});
/**
 * A performant drop-in replacemen for |clojure.core/merge|.
 */
quantum.core.data.map.merge_PLUS_ = (function quantum$core$data$map$merge_PLUS_(){
var argseq__19113__auto__ = ((((1) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(1)),(0))):null);
return quantum.core.data.map.merge_PLUS_.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__19113__auto__);
});

quantum.core.data.map.merge_PLUS_.cljs$core$IFn$_invoke$arity$variadic = (function (map_0,maps){
if((function (){var G__19360 = map_0;
if(G__19360){
var bit__18747__auto__ = null;
if(cljs.core.truth_((function (){var or__18073__auto__ = bit__18747__auto__;
if(cljs.core.truth_(or__18073__auto__)){
return or__18073__auto__;
} else {
return G__19360.quantum$core$ns$Editable$;
}
})())){
return true;
} else {
if((!G__19360.cljs$lang$protocol_mask$partition$)){
return cljs.core.native_satisfies_QMARK_.call(null,quantum.core.ns.Editable,G__19360);
} else {
return false;
}
}
} else {
return cljs.core.native_satisfies_QMARK_.call(null,quantum.core.ns.Editable,G__19360);
}
})()){
return cljs.core.persistent_BANG_.call(null,cljs.core.reduce.call(null,cljs.core.conj_BANG_,cljs.core.transient$.call(null,map_0),maps));
} else {
return cljs.core.apply.call(null,cljs.core.merge,map_0,maps);
}
});

quantum.core.data.map.merge_PLUS_.cljs$lang$maxFixedArity = (1);

quantum.core.data.map.merge_PLUS_.cljs$lang$applyTo = (function (seq19358){
var G__19359 = cljs.core.first.call(null,seq19358);
var seq19358__$1 = cljs.core.next.call(null,seq19358);
return quantum.core.data.map.merge_PLUS_.cljs$core$IFn$_invoke$arity$variadic(G__19359,seq19358__$1);
});
/**
 * Like `merge-with` but merges maps recursively, applying the given fn
 * only when there's a non-map at a particular level.
 * 
 * (merge-deep-with + {:a {:b {:c 1 :d {:x 1 :y 2}} :e 3} :f 4}
 * {:a {:b {:c 2 :d {:z 9} :z 3} :e 100}})
 * => {:a {:b {:z 3, :c 3, :d {:z 9, :x 1, :y 2}}, :e 103}, :f 4}
 */
quantum.core.data.map.merge_deep_with = (function quantum$core$data$map$merge_deep_with(){
var argseq__19113__auto__ = ((((1) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(1)),(0))):null);
return quantum.core.data.map.merge_deep_with.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__19113__auto__);
});

quantum.core.data.map.merge_deep_with.cljs$core$IFn$_invoke$arity$variadic = (function (f,maps){
return cljs.core.apply.call(null,(function() { 
var quantum$core$data$map$m__delegate = function (maps__$1){
if(cljs.core.every_QMARK_.call(null,cljs.core.map_QMARK_,maps__$1)){
return cljs.core.apply.call(null,cljs.core.merge_with,quantum$core$data$map$m,maps__$1);
} else {
return cljs.core.apply.call(null,f,maps__$1);
}
};
var quantum$core$data$map$m = function (var_args){
var maps__$1 = null;
if (arguments.length > 0) {
var G__19363__i = 0, G__19363__a = new Array(arguments.length -  0);
while (G__19363__i < G__19363__a.length) {G__19363__a[G__19363__i] = arguments[G__19363__i + 0]; ++G__19363__i;}
  maps__$1 = new cljs.core.IndexedSeq(G__19363__a,0);
} 
return quantum$core$data$map$m__delegate.call(this,maps__$1);};
quantum$core$data$map$m.cljs$lang$maxFixedArity = 0;
quantum$core$data$map$m.cljs$lang$applyTo = (function (arglist__19364){
var maps__$1 = cljs.core.seq(arglist__19364);
return quantum$core$data$map$m__delegate(maps__$1);
});
quantum$core$data$map$m.cljs$core$IFn$_invoke$arity$variadic = quantum$core$data$map$m__delegate;
return quantum$core$data$map$m;
})()
,maps);
});

quantum.core.data.map.merge_deep_with.cljs$lang$maxFixedArity = (1);

quantum.core.data.map.merge_deep_with.cljs$lang$applyTo = (function (seq19361){
var G__19362 = cljs.core.first.call(null,seq19361);
var seq19361__$1 = cljs.core.next.call(null,seq19361);
return quantum.core.data.map.merge_deep_with.cljs$core$IFn$_invoke$arity$variadic(G__19362,seq19361__$1);
});
quantum.core.data.map.merge_deep = cljs.core.partial.call(null,quantum.core.data.map.merge_deep_with,cljs.core.second);
quantum.core.data.map.sorted_map_PLUS_ = clojure.data.avl.sorted_map;
quantum.core.data.map.sorted_map_by_PLUS_ = clojure.data.avl.sorted_map_by;
quantum.core.data.map.split_at = clojure.data.avl.split_at;

//# sourceMappingURL=map.js.map?rel=1431625564519