// Compiled by ClojureScript 0.0-3269 {}
goog.provide('quantum.core.macros');
goog.require('cljs.core');
goog.require('quantum.core.ns');
goog.require('quantum.core.type');
quantum.core.macros.emit_comprehension = (function quantum$core$macros$emit_comprehension(_AMPERSAND_form,p__23038,seq_exprs,body_expr){
var map__23042 = p__23038;
var map__23042__$1 = ((cljs.core.seq_QMARK_.call(null,map__23042))?cljs.core.apply.call(null,cljs.core.hash_map,map__23042):map__23042);
var emit_other = cljs.core.get.call(null,map__23042__$1,new cljs.core.Keyword(null,"emit-other","emit-other",-892550235));
var emit_inner = cljs.core.get.call(null,map__23042__$1,new cljs.core.Keyword(null,"emit-inner","emit-inner",1188985349));





var groups = cljs.core.reduce.call(null,((function (map__23042,map__23042__$1,emit_other,emit_inner){
return (function (groups,p__23043){
var vec__23044 = p__23043;
var k = cljs.core.nth.call(null,vec__23044,(0),null);
var v = cljs.core.nth.call(null,vec__23044,(1),null);
if((k instanceof cljs.core.Keyword)){
return cljs.core.conj.call(null,cljs.core.pop.call(null,groups),cljs.core.conj.call(null,cljs.core.peek.call(null,groups),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [k,v], null)));
} else {
return cljs.core.conj.call(null,groups,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [k,v], null));
}
});})(map__23042,map__23042__$1,emit_other,emit_inner))
,cljs.core.PersistentVector.EMPTY,cljs.core.partition.call(null,(2),seq_exprs));
var inner_group = cljs.core.peek.call(null,groups);
var other_groups = cljs.core.pop.call(null,groups);
return cljs.core.reduce.call(null,emit_other,emit_inner.call(null,body_expr,inner_group),other_groups);
});
quantum.core.macros.do_mod = (function quantum$core$macros$do_mod(){
var argseq__19113__auto__ = ((((2) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(2)),(0))):null);
return quantum.core.macros.do_mod.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),argseq__19113__auto__);
});

quantum.core.macros.do_mod.cljs$core$IFn$_invoke$arity$variadic = (function (mod_pairs,cont,p__23048){
var map__23049 = p__23048;
var map__23049__$1 = ((cljs.core.seq_QMARK_.call(null,map__23049))?cljs.core.apply.call(null,cljs.core.hash_map,map__23049):map__23049);
var skip = cljs.core.get.call(null,map__23049__$1,new cljs.core.Keyword(null,"skip","skip",602715391));
var stop = cljs.core.get.call(null,map__23049__$1,new cljs.core.Keyword(null,"stop","stop",-2140911342));
var err = ((function (map__23049,map__23049__$1,skip,stop){
return (function() { 
var G__23052__delegate = function (msg){
throw (new quantum.core.ns.IllegalArgumentException(cljs.core.apply.call(null,cljs.core.str,msg),null,null,null));
};
var G__23052 = function (var_args){
var msg = null;
if (arguments.length > 0) {
var G__23053__i = 0, G__23053__a = new Array(arguments.length -  0);
while (G__23053__i < G__23053__a.length) {G__23053__a[G__23053__i] = arguments[G__23053__i + 0]; ++G__23053__i;}
  msg = new cljs.core.IndexedSeq(G__23053__a,0);
} 
return G__23052__delegate.call(this,msg);};
G__23052.cljs$lang$maxFixedArity = 0;
G__23052.cljs$lang$applyTo = (function (arglist__23054){
var msg = cljs.core.seq(arglist__23054);
return G__23052__delegate(msg);
});
G__23052.cljs$core$IFn$_invoke$arity$variadic = G__23052__delegate;
return G__23052;
})()
;})(map__23049,map__23049__$1,skip,stop))
;
return cljs.core.reduce.call(null,((function (err,map__23049,map__23049__$1,skip,stop){
return (function (cont__$1,p__23050){
var vec__23051 = p__23050;
var k = cljs.core.nth.call(null,vec__23051,(0),null);
var v = cljs.core.nth.call(null,vec__23051,(1),null);
if(cljs.core._EQ_.call(null,k,new cljs.core.Keyword(null,"let","let",-1282412701))){
return cljs.core.sequence.call(null,cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core._conj.call(null,cljs.core.List.EMPTY,new cljs.core.Symbol("quantum.core.macros","let","quantum.core.macros/let",-733509463,null)),cljs.core._conj.call(null,cljs.core.List.EMPTY,v),cljs.core._conj.call(null,cljs.core.List.EMPTY,cont__$1))));
} else {
if(cljs.core._EQ_.call(null,k,new cljs.core.Keyword(null,"while","while",963117786))){
return cljs.core.sequence.call(null,cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core._conj.call(null,cljs.core.List.EMPTY,new cljs.core.Symbol(null,"if","if",1181717262,null)),cljs.core._conj.call(null,cljs.core.List.EMPTY,v),cljs.core._conj.call(null,cljs.core.List.EMPTY,cont__$1),cljs.core._conj.call(null,cljs.core.List.EMPTY,stop))));
} else {
if(cljs.core._EQ_.call(null,k,new cljs.core.Keyword(null,"when","when",-576417306))){
return cljs.core.sequence.call(null,cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core._conj.call(null,cljs.core.List.EMPTY,new cljs.core.Symbol(null,"if","if",1181717262,null)),cljs.core._conj.call(null,cljs.core.List.EMPTY,v),cljs.core._conj.call(null,cljs.core.List.EMPTY,cont__$1),cljs.core._conj.call(null,cljs.core.List.EMPTY,skip))));
} else {
return err.call(null,"Invalid 'for' keyword ",k);

}
}
}
});})(err,map__23049,map__23049__$1,skip,stop))
,cont,cljs.core.reverse.call(null,mod_pairs));
});

quantum.core.macros.do_mod.cljs$lang$maxFixedArity = (2);

quantum.core.macros.do_mod.cljs$lang$applyTo = (function (seq23045){
var G__23046 = cljs.core.first.call(null,seq23045);
var seq23045__$1 = cljs.core.next.call(null,seq23045);
var G__23047 = cljs.core.first.call(null,seq23045__$1);
var seq23045__$2 = cljs.core.next.call(null,seq23045__$1);
return quantum.core.macros.do_mod.cljs$core$IFn$_invoke$arity$variadic(G__23046,G__23047,seq23045__$2);
});

//# sourceMappingURL=macros.js.map?rel=1431625568623