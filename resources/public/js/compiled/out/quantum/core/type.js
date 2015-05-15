// Compiled by ClojureScript 0.0-3269 {}
goog.provide('quantum.core.type');
goog.require('cljs.core');
goog.require('quantum.core.ns');
goog.require('quantum.core.logic');
goog.require('quantum.core.function$');
quantum.core.type.class$ = cljs.core.type;
quantum.core.type.instance_PLUS__QMARK_ = (function quantum$core$type$instance_PLUS__QMARK_(class_0,obj){
try{return (obj instanceof class_0);
}catch (e22951){if((e22951 instanceof TypeError)){
var _ = e22951;
var G__22952 = obj;
if(G__22952){
var bit__18747__auto__ = null;
if(cljs.core.truth_((function (){var or__18073__auto__ = bit__18747__auto__;
if(cljs.core.truth_(or__18073__auto__)){
return or__18073__auto__;
} else {
return G__22952.quantum$core$type$class_0$;
}
})())){
return true;
} else {
if((!G__22952.cljs$lang$protocol_mask$partition$)){
return cljs.core.native_satisfies_QMARK_.call(null,class_0,G__22952);
} else {
return false;
}
}
} else {
return cljs.core.native_satisfies_QMARK_.call(null,class_0,G__22952);
}
} else {
throw e22951;

}
}});
quantum.core.type.double_QMARK_ = quantum.core.logic.fn_and.call(null,cljs.core.number_QMARK_,(function (x__22733__auto__){
return cljs.core.not_EQ_.call(null,[cljs.core.str(x__22733__auto__)].join('').indexOf("."),(-1));
}));
quantum.core.type.boolean_QMARK_ = quantum.core.logic.fn_or.call(null,cljs.core.true_QMARK_,cljs.core.false_QMARK_);
quantum.core.type.array_list_QMARK_ = quantum.core.function$.f_STAR_n.call(null,quantum.core.logic.splice_or,(function (p1__22954_SHARP_,p2__22953_SHARP_){
return quantum.core.type.instance_PLUS__QMARK_.call(null,p2__22953_SHARP_,p1__22954_SHARP_);
}),quantum.core.ns.AArrList);
quantum.core.type.map_entry_QMARK_ = quantum.core.logic.fn_and.call(null,cljs.core.vector_QMARK_,(function (x__22733__auto__){
return cljs.core._EQ_.call(null,cljs.core.count.call(null,x__22733__auto__),(2));
}));
quantum.core.type.sorted_map_QMARK_ = cljs.core.partial.call(null,quantum.core.type.instance_PLUS__QMARK_,quantum.core.ns.ATreeMap);
quantum.core.type.queue_QMARK_ = cljs.core.partial.call(null,quantum.core.type.instance_PLUS__QMARK_,quantum.core.ns.AQueue);
quantum.core.type.lseq_QMARK_ = cljs.core.partial.call(null,quantum.core.type.instance_PLUS__QMARK_,quantum.core.ns.ALSeq);
quantum.core.type.coll_PLUS__QMARK_ = quantum.core.logic.fn_or.call(null,cljs.core.coll_QMARK_,quantum.core.type.array_list_QMARK_);
quantum.core.type.pattern_QMARK_ = cljs.core.partial.call(null,quantum.core.type.instance_PLUS__QMARK_,quantum.core.ns.ARegex);
quantum.core.type.regex_QMARK_ = quantum.core.type.pattern_QMARK_;
quantum.core.type.editable_QMARK_ = cljs.core.partial.call(null,quantum.core.type.instance_PLUS__QMARK_,quantum.core.ns.AEditable);
quantum.core.type.transient_QMARK_ = cljs.core.partial.call(null,quantum.core.type.instance_PLUS__QMARK_,quantum.core.ns.ATransient);
quantum.core.type.error_QMARK_ = cljs.core.partial.call(null,quantum.core.type.instance_PLUS__QMARK_,quantum.core.ns.AError);
quantum.core.type.name_from_class = (function quantum$core$type$name_from_class(class_0){
var class_str = [cljs.core.str(class_0)].join('');
return cljs.core.symbol.call(null,cljs.core.subs.call(null,class_str,(class_str.indexOf(" ") + (1))));
});
quantum.core.type.transient_threshold = (3);
quantum.core.type.types = new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"map","map",1371690461),cljs.core.PersistentHashSet.fromArray([cljs.core.PersistentHashMap,cljs.core.PersistentArrayMap,cljs.core.TransientHashMap,cljs.core.TransientArrayMap,cljs.core.PersistentTreeMap], true),new cljs.core.Keyword(null,"set","set",304602554),cljs.core.PersistentHashSet.fromArray([cljs.core.TransientHashSet,cljs.core.PersistentHashSet,cljs.core.PersistentTreeSet], true),new cljs.core.Keyword(null,"vec","vec",-657847931),cljs.core.PersistentHashSet.fromArray([cljs.core.TransientVector,cljs.core.PersistentVector], true),new cljs.core.Keyword(null,"iseq","iseq",589292089),cljs.core.PersistentHashSet.fromArray([cljs.core.PersistentQueue,cljs.core.IndexedSeq,cljs.core.LazySeq,cljs.core.ValSeq,cljs.core.List,cljs.core.KeySeq,cljs.core.ChunkedSeq,cljs.core.ArrayList], true)], null);

//# sourceMappingURL=type.js.map?rel=1431625568275