// Compiled by ClojureScript 0.0-3269 {}
goog.provide('quantum.core.type');
goog.require('cljs.core');
goog.require('quantum.core.data.set');
goog.require('quantum.core.logic');
goog.require('cljs.core');
goog.require('quantum.core.function$');
goog.require('quantum.core.ns');
quantum.core.type.class$ = cljs.core.type;
quantum.core.type.instance_PLUS__QMARK_ = (function quantum$core$type$instance_PLUS__QMARK_(class_0,obj){
try{return (obj instanceof class_0);
}catch (e22673){if((e22673 instanceof TypeError)){
var _ = e22673;
var G__22674 = obj;
if(G__22674){
var bit__18747__auto__ = null;
if(cljs.core.truth_((function (){var or__18073__auto__ = bit__18747__auto__;
if(cljs.core.truth_(or__18073__auto__)){
return or__18073__auto__;
} else {
return G__22674.quantum$core$type$class_0$;
}
})())){
return true;
} else {
if((!G__22674.cljs$lang$protocol_mask$partition$)){
return cljs.core.native_satisfies_QMARK_.call(null,class_0,G__22674);
} else {
return false;
}
}
} else {
return cljs.core.native_satisfies_QMARK_.call(null,class_0,G__22674);
}
} else {
throw e22673;

}
}});
quantum.core.type.name_from_class = (function quantum$core$type$name_from_class(class_0){
var class_str = [cljs.core.str(class_0)].join('');
return cljs.core.symbol.call(null,cljs.core.subs.call(null,class_str,(class_str.indexOf(" ") + (1))));
});
quantum.core.type.transient_threshold = (3);
quantum.core.type.double_QMARK_ = quantum.core.logic.fn_and.call(null,cljs.core.number_QMARK_,(function (x__22470__auto__){
return cljs.core.not_EQ_.call(null,[cljs.core.str(x__22470__auto__)].join('').indexOf("."),(-1));
}));
quantum.core.type.boolean_QMARK_ = quantum.core.logic.fn_or.call(null,cljs.core.true_QMARK_,cljs.core.false_QMARK_);
quantum.core.type.map_entry_QMARK_ = quantum.core.logic.fn_and.call(null,quantum.core.type.vector_QMARK_,(function (x__22470__auto__){
return cljs.core._EQ_.call(null,cljs.core.count.call(null,x__22470__auto__),(2));
}));
quantum.core.type.editable_QMARK_ = cljs.core.partial.call(null,quantum.core.type.instance_PLUS__QMARK_,quantum.core.ns.AEditable);
quantum.core.type.transient_QMARK_ = cljs.core.partial.call(null,quantum.core.type.instance_PLUS__QMARK_,quantum.core.ns.ATransient);
quantum.core.type.error_QMARK_ = cljs.core.partial.call(null,quantum.core.type.instance_PLUS__QMARK_,quantum.core.ns.AError);
quantum.core.type.types = (function (){var hash_map_types = cljs.core.PersistentHashSet.fromArray([cljs.core.PersistentHashMap,cljs.core.TransientHashMap], true);
var array_map_types = cljs.core.PersistentHashSet.fromArray([cljs.core.PersistentArrayMap,cljs.core.TransientArrayMap], true);
var tree_map_types = cljs.core.PersistentHashSet.fromArray([quantum.core.ns.ATreeMap], true);
var map_types = quantum.core.data.set.union.call(null,cljs.core.PersistentHashSet.EMPTY,hash_map_types,array_map_types,tree_map_types);
var array_list_types = cljs.core.PersistentHashSet.fromArray([quantum.core.ns.AArrList], true);
var number_types = cljs.core.PersistentHashSet.EMPTY;
var set_types = cljs.core.PersistentHashSet.fromArray([cljs.core.TransientHashSet,cljs.core.PersistentHashSet,cljs.core.PersistentTreeSet], true);
var vec_types = cljs.core.PersistentHashSet.fromArray([cljs.core.TransientVector,cljs.core.PersistentVector], true);
var list_types = cljs.core.PersistentHashSet.fromArray([cljs.core.List], true);
var queue_types = cljs.core.PersistentHashSet.fromArray([quantum.core.ns.AQueue], true);
var regex_types = cljs.core.PersistentHashSet.fromArray([quantum.core.ns.ARegex], true);
var associative_types = quantum.core.data.set.union.call(null,map_types,set_types,vec_types);
var cons_types = cljs.core.PersistentHashSet.fromArray([cljs.core.Cons], true);
var lseq_types = cljs.core.PersistentHashSet.fromArray([quantum.core.ns.ALSeq], true);
var seq_types = quantum.core.data.set.union.call(null,list_types,queue_types,lseq_types,cljs.core.PersistentHashSet.fromArray([cljs.core.IndexedSeq,cljs.core.ValSeq,cljs.core.KeySeq,cljs.core.ChunkedSeq], true));
var listy_types = seq_types;
var coll_types = quantum.core.data.set.union.call(null,seq_types,associative_types,array_list_types);
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Symbol(null,"queue?","queue?",-880510795,null),new cljs.core.Symbol(null,"cons?","cons?",1120494060,null),new cljs.core.Symbol(null,"nil?","nil?",1612038930,null),new cljs.core.Symbol(null,"array-list?","array-list?",-1781966448,null),new cljs.core.Symbol(null,"sorted-map?","sorted-map?",1119963269,null),new cljs.core.Symbol(null,"map?","map?",-1780568534,null),new cljs.core.Symbol(null,"vector?","vector?",-61367869,null),new cljs.core.Keyword(null,"default","default",-1987822328),new cljs.core.Symbol(null,"associative?","associative?",-141666771,null),new cljs.core.Symbol(null,"tree-map?","tree-map?",104645508,null),new cljs.core.Symbol(null,"char?","char?",-1072221244,null),new cljs.core.Symbol(null,"list?","list?",-1494629,null),new cljs.core.Symbol(null,"coll?","coll?",-1874821441,null),new cljs.core.Symbol(null,"keyword?","keyword?",1917797069,null),new cljs.core.Symbol(null,"lseq?","lseq?",-1797189347,null),new cljs.core.Symbol(null,"vec?","vec?",1920925294,null),new cljs.core.Symbol(null,"string?","string?",-1129175764,null),new cljs.core.Symbol(null,"seq?","seq?",-1951934719,null),new cljs.core.Symbol(null,"set?","set?",1636014792,null),new cljs.core.Symbol(null,"regex?","regex?",661137998,null),new cljs.core.Symbol(null,"pattern?","pattern?",519487951,null),new cljs.core.Symbol(null,"listy?","listy?",-483531253,null)],[queue_types,cons_types,new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 1, [null,null], null), null),array_list_types,tree_map_types,map_types,vec_types,new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"default","default",-1987822328),null], null), null),associative_types,tree_map_types,cljs.core.PersistentHashSet.fromArray([quantum.core.type.Character], true),list_types,coll_types,cljs.core.PersistentHashSet.fromArray([cljs.core.Keyword], true),lseq_types,vec_types,cljs.core.PersistentHashSet.fromArray([quantum.core.type.string], true),seq_types,set_types,regex_types,regex_types,listy_types]);
})();
cljs.core.doall.call(null,cljs.core.map.call(null,(function (p__22675){
var vec__22676 = p__22675;
var type_pred = cljs.core.nth.call(null,vec__22676,(0),null);
var types = cljs.core.nth.call(null,vec__22676,(1),null);
if((type_pred instanceof cljs.core.Symbol)){
return quantum.core.type.intern.call(null,quantum.core.type._STAR_ns_STAR_,type_pred,((function (vec__22676,type_pred,types){
return (function (x__22476__auto__){
return cljs.core.contains_QMARK_.call(null,types,quantum.core.type.class$.call(null,x__22476__auto__));
});})(vec__22676,type_pred,types))
);
} else {
return null;
}
}),quantum.core.type.types));

//# sourceMappingURL=type.js.map?rel=1431987042695