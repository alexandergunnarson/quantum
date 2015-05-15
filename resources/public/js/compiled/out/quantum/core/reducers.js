// Compiled by ClojureScript 0.0-3269 {}
goog.provide('quantum.core.reducers');
goog.require('cljs.core');
goog.require('quantum.core.data.set');
goog.require('quantum.core.logic');
goog.require('quantum.core.data.map');
goog.require('quantum.core.macros');
goog.require('quantum.core.type');
goog.require('quantum.core.numeric');
goog.require('quantum.core.function$');
goog.require('clojure.walk');
goog.require('quantum.core.data.vector');
goog.require('quantum.core.ns');
quantum.core.reducers.reverse_conses = (function quantum$core$reducers$reverse_conses(){
var G__23150 = arguments.length;
switch (G__23150) {
case 2:
return quantum.core.reducers.reverse_conses.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return quantum.core.reducers.reverse_conses.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.reducers.reverse_conses.cljs$core$IFn$_invoke$arity$2 = (function (s,tail){
if((cljs.core.rest.call(null,s) === tail)){
return s;
} else {
return quantum.core.reducers.reverse_conses.call(null,s,tail,tail);
}
});

quantum.core.reducers.reverse_conses.cljs$core$IFn$_invoke$arity$3 = (function (s,from_tail,to_tail){
var f = s;
var b = to_tail;
while(true){
if((f === from_tail)){
return b;
} else {
var G__23152 = cljs.core.rest.call(null,f);
var G__23153 = cljs.core.cons.call(null,cljs.core.first.call(null,f),b);
f = G__23152;
b = G__23153;
continue;
}
break;
}
});

quantum.core.reducers.reverse_conses.cljs$lang$maxFixedArity = 3;
quantum.core.reducers.fjtask = (function quantum$core$reducers$fjtask(f){
return f;
});
quantum.core.reducers.fjinvoke = (function quantum$core$reducers$fjinvoke(f){
return f.call(null);
});
quantum.core.reducers.fjfork = (function quantum$core$reducers$fjfork(task){
return task;
});
quantum.core.reducers.fjjoin = (function quantum$core$reducers$fjjoin(task){
return task.call(null);
});
/**
 * Creates an implementation of CollReduce using the given reducer.
 * The two-argument implementation of reduce will call f1 with no args
 * to get an init value, and then forward on to your three-argument version.
 */
quantum.core.reducers.reduce_impl = (function quantum$core$reducers$reduce_impl(reducer_n){
return new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"-reduce","-reduce",-848900642),(function() {
var G__23157 = null;
var G__23157__2 = (function (coll,f1){
return reducer_n.call(null,coll,f1,f1.call(null));
});
var G__23157__3 = (function (coll,f1,init){
return reducer_n.call(null,coll,f1,init);
});
G__23157 = function(coll,f1,init){
switch(arguments.length){
case 2:
return G__23157__2.call(this,coll,f1);
case 3:
return G__23157__3.call(this,coll,f1,init);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
G__23157.cljs$core$IFn$_invoke$arity$2 = G__23157__2;
G__23157.cljs$core$IFn$_invoke$arity$3 = G__23157__3;
return G__23157;
})()
], null);
});

quantum.core.reducers.Reduce_PLUS_ = (function (){var obj23159 = {};
return obj23159;
})();

quantum.core.reducers.reduce_PLUS_ = (function quantum$core$reducers$reduce_PLUS_(){
var G__23161 = arguments.length;
switch (G__23161) {
case 2:
return quantum.core.reducers.reduce_PLUS_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return quantum.core.reducers.reduce_PLUS_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.reducers.reduce_PLUS_.cljs$core$IFn$_invoke$arity$2 = (function (coll,f){
if((function (){var and__18061__auto__ = coll;
if(and__18061__auto__){
return coll.quantum$core$reducers$Reduce_PLUS_$reduce_PLUS_$arity$2;
} else {
return and__18061__auto__;
}
})()){
return coll.quantum$core$reducers$Reduce_PLUS_$reduce_PLUS_$arity$2(coll,f);
} else {
var x__18709__auto__ = (((coll == null))?null:coll);
return (function (){var or__18073__auto__ = (quantum.core.reducers.reduce_PLUS_[goog.typeOf(x__18709__auto__)]);
if(or__18073__auto__){
return or__18073__auto__;
} else {
var or__18073__auto____$1 = (quantum.core.reducers.reduce_PLUS_["_"]);
if(or__18073__auto____$1){
return or__18073__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"Reduce+.reduce+",coll);
}
}
})().call(null,coll,f);
}
});

quantum.core.reducers.reduce_PLUS_.cljs$core$IFn$_invoke$arity$3 = (function (coll,f,init){
if((function (){var and__18061__auto__ = coll;
if(and__18061__auto__){
return coll.quantum$core$reducers$Reduce_PLUS_$reduce_PLUS_$arity$3;
} else {
return and__18061__auto__;
}
})()){
return coll.quantum$core$reducers$Reduce_PLUS_$reduce_PLUS_$arity$3(coll,f,init);
} else {
var x__18709__auto__ = (((coll == null))?null:coll);
return (function (){var or__18073__auto__ = (quantum.core.reducers.reduce_PLUS_[goog.typeOf(x__18709__auto__)]);
if(or__18073__auto__){
return or__18073__auto__;
} else {
var or__18073__auto____$1 = (quantum.core.reducers.reduce_PLUS_["_"]);
if(or__18073__auto____$1){
return or__18073__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"Reduce+.reduce+",coll);
}
}
})().call(null,coll,f,init);
}
});

quantum.core.reducers.reduce_PLUS_.cljs$lang$maxFixedArity = 3;

(quantum.core.reducers.Reduce_PLUS_["array"] = true);

(quantum.core.reducers.reduce_PLUS_["array"] = (function (arr,f,init){
return cljs.core.array_reduce.call(null,arr,f,init);
}));

(quantum.core.reducers.Reduce_PLUS_["string"] = true);

(quantum.core.reducers.reduce_PLUS_["string"] = (function (s,f,init){
var last_index = cljs.core.long$.call(null,(cljs.core.count.call(null,s) - (1)));
if((last_index > Number.MAX_VALUE())){
throw (new quantum.core.ns.Exception("String too large to reduce over (at least, efficiently).",null,null,null));
} else {
if(cljs.core._EQ_.call(null,last_index,(-1))){
return f.call(null,init,null);
} else {
var n = cljs.core.long$.call(null,(0));
var ret = init;
while(true){
if((n > last_index)){
return ret;
} else {
var G__23163 = (n + (1));
var G__23164 = f.call(null,ret,s.charAt(n));
n = G__23163;
ret = G__23164;
continue;
}
break;
}

}
}
}));

(quantum.core.reducers.Reduce_PLUS_["null"] = true);

(quantum.core.reducers.reduce_PLUS_["null"] = (function (obj,f,init){
return init;
}));

(quantum.core.reducers.Reduce_PLUS_["_"] = true);

(quantum.core.reducers.reduce_PLUS_["_"] = (function (coll,f,init){
return cljs.core._reduce.call(null,coll,f,init);
}));
/**
 * For some reason |reduce| is not implemented in ClojureScript for certain types.
 * This is a |loop|-|recur| replacement for it.
 */
quantum.core.reducers._reduce_seq = (function quantum$core$reducers$_reduce_seq(coll,f,init){
var coll_n = coll;
var ret = init;
while(true){
if(cljs.core.empty_QMARK_.call(null,coll_n)){
return ret;
} else {
var G__23165 = cljs.core.rest.call(null,coll_n);
var G__23166 = f.call(null,ret,cljs.core.first.call(null,coll_n));
coll_n = G__23165;
ret = G__23166;
continue;
}
break;
}
});
quantum.core.reducers._reduce_kv = (function quantum$core$reducers$_reduce_kv(coll,f,init){
try{return cljs.core._kv_reduce.call(null,coll,f,init);
}catch (e23168){if((e23168 instanceof Object)){
var e = e23168;
cljs.core.println.call(null,"There was an exception in kv-reduce!");

throw (new quantum.core.ns.Exception("Broke out of kv-reduce",null,null,null));
} else {
throw e23168;

}
}});
/**
 * Like |core/reduce| except:
 * When init is not provided, (f) is used.
 * Maps are reduced with reduce-kv.
 * 
 * Entry point for internal reduce (in order to switch the args
 * around to dispatch on type).
 */
quantum.core.reducers.reduce = (function quantum$core$reducers$reduce(){
var G__23170 = arguments.length;
switch (G__23170) {
case 2:
return quantum.core.reducers.reduce.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return quantum.core.reducers.reduce.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.reducers.reduce.cljs$core$IFn$_invoke$arity$2 = (function (f,coll){
return quantum.core.reducers.reduce.call(null,f,f.call(null),coll);
});

quantum.core.reducers.reduce.cljs$core$IFn$_invoke$arity$3 = (function (f,init,coll){
return quantum.core.reducers.reduce_PLUS_.call(null,coll,f,init);
});

quantum.core.reducers.reduce.cljs$lang$maxFixedArity = 3;
quantum.core.reducers.fold_pre = (function quantum$core$reducers$fold_pre(obj__22858__auto__){
var obj__23173 = obj__22858__auto__;
if((cljs.core._EQ_.call(null,cljs.core.fn_QMARK_,new cljs.core.Keyword(null,"else","else",-1508377146))) || (cljs.core.fn_QMARK_.call(null,obj__23173))){
return ((function (obj__23173){
return (function (x__22733__auto__){
var obj_f__22903__auto__ = quantum.core.function$.call.call(null,x__22733__auto__);
if(cljs.core.delay_QMARK_.call(null,obj_f__22903__auto__)){
return cljs.core.force.call(null,obj_f__22903__auto__);
} else {
return obj_f__22903__auto__;
}
});})(obj__23173))
.call(null,obj__23173);
} else {
if((cljs.core._EQ_.call(null,cljs.core.delay_QMARK_,new cljs.core.Keyword(null,"else","else",-1508377146))) || (cljs.core.delay_QMARK_.call(null,obj__23173))){
return ((function (obj__23173){
return (function (x__22733__auto__){
var obj_f__22903__auto__ = cljs.core.force.call(null,x__22733__auto__);
if(cljs.core.fn_QMARK_.call(null,obj_f__22903__auto__)){
return quantum.core.function$.call.call(null,obj_f__22903__auto__);
} else {
return obj_f__22903__auto__;
}
});})(obj__23173))
.call(null,obj__23173);
} else {
if(cljs.core.truth_((function (){var or__18073__auto__ = cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"else","else",-1508377146),new cljs.core.Keyword(null,"else","else",-1508377146));
if(or__18073__auto__){
return or__18073__auto__;
} else {
return new cljs.core.Keyword(null,"else","else",-1508377146).cljs$core$IFn$_invoke$arity$1(obj__23173);
}
})())){
return cljs.core.identity.call(null,obj__23173);
} else {
throw (new java.lang.IllegalArgumentException([cljs.core.str("No matching clause for "),cljs.core.str(obj__23173)].join('')));
}
}
}
});
quantum.core.reducers.into_PLUS_ = (function quantum$core$reducers$into_PLUS_(){
var G__23178 = arguments.length;
switch (G__23178) {
case 2:
return quantum.core.reducers.into_PLUS_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
var argseq__19124__auto__ = (new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(2)),(0)));
return quantum.core.reducers.into_PLUS_.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),argseq__19124__auto__);

}
});

quantum.core.reducers.into_PLUS_.cljs$core$IFn$_invoke$arity$2 = (function (to,from){
if((cljs.core.vector_QMARK_.call(null,to)) && (cljs.core.vector_QMARK_.call(null,from))){
return quantum.core.data.vector.catvec.call(null,to,from);
} else {
return cljs.core.into.call(null,to,quantum.core.reducers.fold_pre.call(null,from));
}
});

quantum.core.reducers.into_PLUS_.cljs$core$IFn$_invoke$arity$variadic = (function (to,from,froms){
return quantum.core.reducers.reduce.call(null,quantum.core.reducers.into_PLUS_,quantum.core.reducers.into_PLUS_.call(null,to,quantum.core.reducers.fold_pre.call(null,from)),froms);
});

quantum.core.reducers.into_PLUS_.cljs$lang$applyTo = (function (seq23174){
var G__23175 = cljs.core.first.call(null,seq23174);
var seq23174__$1 = cljs.core.next.call(null,seq23174);
var G__23176 = cljs.core.first.call(null,seq23174__$1);
var seq23174__$2 = cljs.core.next.call(null,seq23174__$1);
return quantum.core.reducers.into_PLUS_.cljs$core$IFn$_invoke$arity$variadic(G__23175,G__23176,seq23174__$2);
});

quantum.core.reducers.into_PLUS_.cljs$lang$maxFixedArity = (2);
/**
 * Requires only one argument for preceding functions in its call chain.
 */
quantum.core.reducers.reducem_PLUS_ = (function quantum$core$reducers$reducem_PLUS_(coll){
return quantum.core.reducers.into_PLUS_.call(null,cljs.core.PersistentArrayMap.EMPTY,coll);
});
quantum.core.reducers.count_STAR_ = (function quantum$core$reducers$count_STAR_(coll){
return quantum.core.reducers.reduce.call(null,cljs.core.comp.call(null,cljs.core.inc,quantum.core.function$.firsta),(0),coll);
});
/**
 * Given a reducible collection, and a transformation function xf,
 * returns a reducible collection, where any supplied reducing
 * fn will be transformed by xf. xf is a function of reducing fn to
 * reducing fn.
 */
quantum.core.reducers.reducer_PLUS_ = (function quantum$core$reducers$reducer_PLUS_(coll,xf){
if(typeof quantum.core.reducers.t23183 !== 'undefined'){
} else {

/**
* @constructor
*/
quantum.core.reducers.t23183 = (function (reducer_PLUS_,coll,xf,meta23184){
this.reducer_PLUS_ = reducer_PLUS_;
this.coll = coll;
this.xf = xf;
this.meta23184 = meta23184;
this.cljs$lang$protocol_mask$partition0$ = 917504;
this.cljs$lang$protocol_mask$partition1$ = 0;
})
quantum.core.reducers.t23183.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_23185,meta23184__$1){
var self__ = this;
var _23185__$1 = this;
return (new quantum.core.reducers.t23183(self__.reducer_PLUS_,self__.coll,self__.xf,meta23184__$1));
});

quantum.core.reducers.t23183.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_23185){
var self__ = this;
var _23185__$1 = this;
return self__.meta23184;
});

quantum.core.reducers.t23183.prototype.cljs$core$IReduce$_reduce$arity$2 = (function (this$,f1){
var self__ = this;
var this$__$1 = this;
return cljs.core._reduce.call(null,this$__$1,f1,f1.call(null));
});

quantum.core.reducers.t23183.prototype.cljs$core$IReduce$_reduce$arity$3 = (function (_,f1,init){
var self__ = this;
var ___$1 = this;
return cljs.core._reduce.call(null,self__.coll,self__.xf.call(null,f1),init);
});

quantum.core.reducers.t23183.getBasis = (function (){
return new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"reducer+","reducer+",840360012,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null),new cljs.core.Symbol(null,"xf","xf",2042434515,null),new cljs.core.Symbol(null,"meta23184","meta23184",704355403,null)], null);
});

quantum.core.reducers.t23183.cljs$lang$type = true;

quantum.core.reducers.t23183.cljs$lang$ctorStr = "quantum.core.reducers/t23183";

quantum.core.reducers.t23183.cljs$lang$ctorPrWriter = (function (this__18652__auto__,writer__18653__auto__,opt__18654__auto__){
return cljs.core._write.call(null,writer__18653__auto__,"quantum.core.reducers/t23183");
});

quantum.core.reducers.__GT_t23183 = (function quantum$core$reducers$reducer_PLUS__$___GT_t23183(reducer_PLUS___$1,coll__$1,xf__$1,meta23184){
return (new quantum.core.reducers.t23183(reducer_PLUS___$1,coll__$1,xf__$1,meta23184));
});

}

return (new quantum.core.reducers.t23183(quantum$core$reducers$reducer_PLUS_,coll,xf,cljs.core.PersistentArrayMap.EMPTY));
});

quantum.core.reducers.CollFold = (function (){var obj23187 = {};
return obj23187;
})();

quantum.core.reducers.coll_fold = (function quantum$core$reducers$coll_fold(coll,n,combinef,reducef){
if((function (){var and__18061__auto__ = coll;
if(and__18061__auto__){
return coll.quantum$core$reducers$CollFold$coll_fold$arity$4;
} else {
return and__18061__auto__;
}
})()){
return coll.quantum$core$reducers$CollFold$coll_fold$arity$4(coll,n,combinef,reducef);
} else {
var x__18709__auto__ = (((coll == null))?null:coll);
return (function (){var or__18073__auto__ = (quantum.core.reducers.coll_fold[goog.typeOf(x__18709__auto__)]);
if(or__18073__auto__){
return or__18073__auto__;
} else {
var or__18073__auto____$1 = (quantum.core.reducers.coll_fold["_"]);
if(or__18073__auto____$1){
return or__18073__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"CollFold.coll-fold",coll);
}
}
})().call(null,coll,n,combinef,reducef);
}
});

/**
 * Folds the provided collection by halving it until it is smaller than the
 * requested size, and folding each subsection. halving-fn will be passed as
 * input a collection and its size (so you need not recompute the size); it
 * should return the left and right halves of the collection as a pair. Those
 * halves will normally be of the same type as the parent collection, but
 * anything foldable is sufficient.
 * 
 * Generalized from |foldvec| to work for anything you can split in half.
 */
quantum.core.reducers.fold_by_halves = (function quantum$core$reducers$fold_by_halves(halving_fn,coll,n,combinef,reducef){
var size = cljs.core.count.call(null,coll);
if((size === (0))){
return combinef.call(null);
} else {
if((size <= n)){
return quantum.core.reducers.reduce.call(null,reducef,combinef.call(null),coll);
} else {
var vec__23189 = halving_fn.call(null,coll,size);
var left = cljs.core.nth.call(null,vec__23189,(0),null);
var right = cljs.core.nth.call(null,vec__23189,(1),null);
var child_fn = ((function (vec__23189,left,right,size){
return (function (child){
return ((function (vec__23189,left,right,size){
return (function (){
return quantum.core.reducers.coll_fold.call(null,child,n,combinef,reducef);
});
;})(vec__23189,left,right,size))
});})(vec__23189,left,right,size))
;
return quantum.core.reducers.fjinvoke.call(null,((function (vec__23189,left,right,child_fn,size){
return (function (){
var f1 = child_fn.call(null,left);
var t2 = quantum.core.reducers.fjtask.call(null,child_fn.call(null,right));
quantum.core.reducers.fjfork.call(null,t2);

return combinef.call(null,f1.call(null),quantum.core.reducers.fjjoin.call(null,t2));
});})(vec__23189,left,right,child_fn,size))
);

}
}
});
(quantum.core.reducers.CollFold["null"] = true);

(quantum.core.reducers.coll_fold["null"] = (function (coll,n,combinef,reducef){
return combinef.call(null);
}));

(quantum.core.reducers.CollFold["object"] = true);

(quantum.core.reducers.coll_fold["object"] = (function (coll,n,combinef,reducef){
return quantum.core.reducers.reduce.call(null,reducef,combinef.call(null),coll);
}));

cljs.core.PersistentVector.prototype.quantum$core$reducers$CollFold$ = true;

cljs.core.PersistentVector.prototype.quantum$core$reducers$CollFold$coll_fold$arity$4 = (function (coll,n,combinef,reducef){
var coll__$1 = this;
return quantum.core.reducers.fold_by_halves.call(null,((function (coll__$1){
return (function (coll_0,ct){
var split_ind = cljs.core.quot.call(null,ct,(2));
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [quantum.core.data.vector.subvec_PLUS_.call(null,coll_0,(0),split_ind),quantum.core.data.vector.subvec_PLUS_.call(null,coll_0,split_ind,ct)], null);
});})(coll__$1))
,coll__$1,n,combinef,reducef);
});

clojure.data.avl.AVLMap.prototype.quantum$core$reducers$CollFold$ = true;

clojure.data.avl.AVLMap.prototype.quantum$core$reducers$CollFold$coll_fold$arity$4 = (function (coll,n,combinef,reducef){
var coll__$1 = this;
return quantum.core.reducers.fold_by_halves.call(null,((function (coll__$1){
return (function (coll_0,ct){
var split_ind = cljs.core.quot.call(null,ct,(2));
return quantum.core.data.map.split_at.call(null,split_ind,coll_0);
});})(coll__$1))
,coll__$1,n,combinef,reducef);
});
/**
 * Given a foldable collection, and a transformation function xf,
 * returns a foldable collection, where any supplied reducing
 * fn will be transformed by xf. xf is a function of reducing fn to
 * reducing fn.
 * 
 * Modifies reducers to not use Java methods but external extensions.
 * This is because the protocol methods is not a Java method of the reducer
 * object anymore and thus it can be reclaimed while the protocol method
 * is executing.
 */
quantum.core.reducers.folder_PLUS_ = (function quantum$core$reducers$folder_PLUS_(coll,xf){
if(typeof quantum.core.reducers.t23193 !== 'undefined'){
} else {

/**
* @constructor
*/
quantum.core.reducers.t23193 = (function (folder_PLUS_,coll,xf,meta23194){
this.folder_PLUS_ = folder_PLUS_;
this.coll = coll;
this.xf = xf;
this.meta23194 = meta23194;
this.cljs$lang$protocol_mask$partition0$ = 917504;
this.cljs$lang$protocol_mask$partition1$ = 0;
})
quantum.core.reducers.t23193.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_23195,meta23194__$1){
var self__ = this;
var _23195__$1 = this;
return (new quantum.core.reducers.t23193(self__.folder_PLUS_,self__.coll,self__.xf,meta23194__$1));
});

quantum.core.reducers.t23193.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_23195){
var self__ = this;
var _23195__$1 = this;
return self__.meta23194;
});

quantum.core.reducers.t23193.prototype.cljs$core$IReduce$_reduce$arity$2 = (function (_,f1){
var self__ = this;
var ___$1 = this;
return cljs.core._reduce.call(null,self__.coll,self__.xf.call(null,f1),f1.call(null));
});

quantum.core.reducers.t23193.prototype.cljs$core$IReduce$_reduce$arity$3 = (function (_,f1,init){
var self__ = this;
var ___$1 = this;
return cljs.core._reduce.call(null,self__.coll,self__.xf.call(null,f1),init);
});

quantum.core.reducers.t23193.prototype.quantum$core$reducers$CollFold$ = true;

quantum.core.reducers.t23193.prototype.quantum$core$reducers$CollFold$coll_fold$arity$4 = (function (_,n,combinef,reducef){
var self__ = this;
var ___$1 = this;
return quantum.core.reducers.coll_fold.call(null,self__.coll,n,combinef,self__.xf.call(null,reducef));
});

quantum.core.reducers.t23193.getBasis = (function (){
return new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"folder+","folder+",-2100652736,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null),new cljs.core.Symbol(null,"xf","xf",2042434515,null),new cljs.core.Symbol(null,"meta23194","meta23194",1995356351,null)], null);
});

quantum.core.reducers.t23193.cljs$lang$type = true;

quantum.core.reducers.t23193.cljs$lang$ctorStr = "quantum.core.reducers/t23193";

quantum.core.reducers.t23193.cljs$lang$ctorPrWriter = (function (this__18652__auto__,writer__18653__auto__,opt__18654__auto__){
return cljs.core._write.call(null,writer__18653__auto__,"quantum.core.reducers/t23193");
});

quantum.core.reducers.__GT_t23193 = (function quantum$core$reducers$folder_PLUS__$___GT_t23193(folder_PLUS___$1,coll__$1,xf__$1,meta23194){
return (new quantum.core.reducers.t23193(folder_PLUS___$1,coll__$1,xf__$1,meta23194));
});

}

return (new quantum.core.reducers.t23193(quantum$core$reducers$folder_PLUS_,coll,xf,cljs.core.PersistentArrayMap.EMPTY));
});
quantum.core.reducers.reducer_QMARK_ = quantum.core.function$.compr.call(null,quantum.core.type.class$,quantum.core.logic.fn_or.call(null,quantum.core.function$.f_STAR_n.call(null,quantum.core.type.instance_PLUS__QMARK_,cljs.core.IReduce),quantum.core.function$.f_STAR_n.call(null,cljs.core.isa_QMARK_,quantum.core.reducers.Folder)));
quantum.core.reducers.fold_size = cljs.core.count;
quantum.core.reducers.orig_folder_coll = (function quantum$core$reducers$orig_folder_coll(coll_0){
quantum.core.reducers.coll_n = cljs.core.atom.call(null,new cljs.core.Keyword(null,"coll","coll",1647737163).cljs$core$IFn$_invoke$arity$1(coll_0));

while(true){
if(cljs.core.truth_(quantum.core.reducers.reducer_QMARK_.call(null,cljs.core.deref.call(null,quantum.core.reducers.coll_n)))){
cljs.core.swap_BANG_.call(null,quantum.core.reducers.coll_n,new cljs.core.Keyword(null,"coll","coll",1647737163));

continue;
} else {
}
break;
}

return cljs.core.deref.call(null,quantum.core.reducers.coll_n);
});
quantum.core.reducers.vec_PLUS_ = (function quantum$core$reducers$vec_PLUS_(obj__22858__auto__){
var obj__23197 = obj__22858__auto__;
if((cljs.core._EQ_.call(null,cljs.core.vector_QMARK_,new cljs.core.Keyword(null,"else","else",-1508377146))) || (cljs.core.vector_QMARK_.call(null,obj__23197))){
return cljs.core.identity.call(null,obj__23197);
} else {
if(cljs.core.truth_((function (){var or__18073__auto__ = cljs.core._EQ_.call(null,quantum.core.logic.fn_or.call(null,cljs.core.coll_QMARK_,quantum.core.reducers.reducer_QMARK_,quantum.core.type.array_list_QMARK_),new cljs.core.Keyword(null,"else","else",-1508377146));
if(or__18073__auto__){
return or__18073__auto__;
} else {
return quantum.core.logic.fn_or.call(null,cljs.core.coll_QMARK_,quantum.core.reducers.reducer_QMARK_,quantum.core.type.array_list_QMARK_).call(null,obj__23197);
}
})())){
return cljs.core.partial.call(null,quantum.core.reducers.into_PLUS_,cljs.core.PersistentVector.EMPTY).call(null,obj__23197);
} else {
if((cljs.core._EQ_.call(null,cljs.core.nil_QMARK_,new cljs.core.Keyword(null,"else","else",-1508377146))) || ((obj__23197 == null))){
return cljs.core.constantly.call(null,cljs.core.PersistentVector.EMPTY).call(null,obj__23197);
} else {
if(cljs.core.truth_((function (){var or__18073__auto__ = cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"else","else",-1508377146),new cljs.core.Keyword(null,"else","else",-1508377146));
if(or__18073__auto__){
return or__18073__auto__;
} else {
return new cljs.core.Keyword(null,"else","else",-1508377146).cljs$core$IFn$_invoke$arity$1(obj__23197);
}
})())){
return (new cljs.core.PersistentVector(null,1,(5),cljs.core.PersistentVector.EMPTY_NODE,[obj__23197],null));
} else {
throw (new java.lang.IllegalArgumentException([cljs.core.str("No matching clause for "),cljs.core.str(obj__23197)].join('')));
}
}
}
}
});
/**
 * Reduces a collection using a (potentially parallel) reduce-combine
 * strategy. The collection is partitioned into groups of approximately
 * n (default 512), each of which is reduced with reducef (with a seed
 * value obtained by calling (combinef) with no arguments). The results
 * of these reductions are then reduced with combinef (default
 * reducef).
 * @combinef must be associative. When called with no
 * arguments, (combinef) must produce its identity element.
 * These operations may be performed in parallel, but the results will preserve order.
 */
quantum.core.reducers.fold_PLUS_ = (function quantum$core$reducers$fold_PLUS_(){
var G__23199 = arguments.length;
switch (G__23199) {
case 1:
return quantum.core.reducers.fold_PLUS_.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return quantum.core.reducers.fold_PLUS_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return quantum.core.reducers.fold_PLUS_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return quantum.core.reducers.fold_PLUS_.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.reducers.fold_PLUS_.cljs$core$IFn$_invoke$arity$1 = (function (obj){
return quantum.core.reducers.into_PLUS_.call(null,cljs.core.PersistentVector.EMPTY,quantum.core.reducers.fold_pre.call(null,obj));
});

quantum.core.reducers.fold_PLUS_.cljs$core$IFn$_invoke$arity$2 = (function (reducef,coll){
return quantum.core.reducers.fold_PLUS_.call(null,reducef,reducef,coll);
});

quantum.core.reducers.fold_PLUS_.cljs$core$IFn$_invoke$arity$3 = (function (combinef,reducef,coll){
return quantum.core.reducers.fold_PLUS_.call(null,(function (){var obj__23200 = quantum.core.reducers.fold_pre.call(null,coll);
if(cljs.core.truth_((function (){var or__18073__auto__ = cljs.core._EQ_.call(null,quantum.core.logic.fn_and.call(null,quantum.core.logic.fn_not.call(null,quantum.core.reducers.reducer_QMARK_),cljs.core.counted_QMARK_),new cljs.core.Keyword(null,"else","else",-1508377146));
if(or__18073__auto__){
return or__18073__auto__;
} else {
return quantum.core.logic.fn_and.call(null,quantum.core.logic.fn_not.call(null,quantum.core.reducers.reducer_QMARK_),cljs.core.counted_QMARK_).call(null,obj__23200);
}
})())){
return quantum.core.reducers.fold_size.call(null,obj__23200);
} else {
if(cljs.core.truth_((function (){var or__18073__auto__ = cljs.core._EQ_.call(null,quantum.core.function$.compr.call(null,quantum.core.reducers.orig_folder_coll,cljs.core.counted_QMARK_),new cljs.core.Keyword(null,"else","else",-1508377146));
if(or__18073__auto__){
return or__18073__auto__;
} else {
return quantum.core.function$.compr.call(null,quantum.core.reducers.orig_folder_coll,cljs.core.counted_QMARK_).call(null,obj__23200);
}
})())){
return quantum.core.function$.compr.call(null,quantum.core.reducers.orig_folder_coll,quantum.core.reducers.fold_size).call(null,obj__23200);
} else {
if(cljs.core.truth_((function (){var or__18073__auto__ = cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"else","else",-1508377146),new cljs.core.Keyword(null,"else","else",-1508377146));
if(or__18073__auto__){
return or__18073__auto__;
} else {
return new cljs.core.Keyword(null,"else","else",-1508377146).cljs$core$IFn$_invoke$arity$1(obj__23200);
}
})())){
return cljs.core.constantly.call(null,(512)).call(null,obj__23200);
} else {
throw (new java.lang.IllegalArgumentException([cljs.core.str("No matching clause for "),cljs.core.str(obj__23200)].join('')));
}
}
}
})(),combinef,reducef,coll);
});

quantum.core.reducers.fold_PLUS_.cljs$core$IFn$_invoke$arity$4 = (function (n,combinef,reducef,coll){
return quantum.core.reducers.coll_fold.call(null,quantum.core.reducers.fold_pre.call(null,coll),n,combinef,reducef);
});

quantum.core.reducers.fold_PLUS_.cljs$lang$maxFixedArity = 4;
quantum.core.reducers.foldp_max_PLUS_ = (function quantum$core$reducers$foldp_max_PLUS_(obj){
return quantum.core.reducers.fold_PLUS_.call(null,(1),quantum.core.function$.monoid.call(null,quantum.core.reducers.into_PLUS_,cljs.core.vector),cljs.core.conj,obj);
});
quantum.core.reducers.foldp_PLUS_ = (function quantum$core$reducers$foldp_PLUS_(obj){
return quantum.core.reducers.fold_PLUS_.call(null,quantum.core.function$.monoid.call(null,quantum.core.reducers.into_PLUS_,cljs.core.vector),cljs.core.conj,obj);
});
quantum.core.reducers.foldm_STAR_ = (function quantum$core$reducers$foldm_STAR_(map_fn,obj){
return quantum.core.reducers.fold_PLUS_.call(null,(function() {
var G__23202 = null;
var G__23202__0 = (function (){
return map_fn.call(null);
});
var G__23202__2 = (function (ret,m){
return quantum.core.data.map.merge_PLUS_.call(null,ret,m);
});
var G__23202__3 = (function (ret,k,v){
return cljs.core.assoc.call(null,ret,k,v);
});
G__23202 = function(ret,k,v){
switch(arguments.length){
case 0:
return G__23202__0.call(this);
case 2:
return G__23202__2.call(this,ret,k);
case 3:
return G__23202__3.call(this,ret,k,v);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
G__23202.cljs$core$IFn$_invoke$arity$0 = G__23202__0;
G__23202.cljs$core$IFn$_invoke$arity$2 = G__23202__2;
G__23202.cljs$core$IFn$_invoke$arity$3 = G__23202__3;
return G__23202;
})()
,obj);
});
quantum.core.reducers.foldm_PLUS_ = (function quantum$core$reducers$foldm_PLUS_(obj){
return quantum.core.reducers.foldm_STAR_.call(null,cljs.core.hash_map,obj);
});
quantum.core.reducers.fold_am_PLUS_ = (function quantum$core$reducers$fold_am_PLUS_(obj){
return quantum.core.reducers.foldm_STAR_.call(null,cljs.core.array_map,obj);
});
quantum.core.reducers.fold_sm_PLUS_ = (function quantum$core$reducers$fold_sm_PLUS_(obj){
return quantum.core.reducers.foldm_STAR_.call(null,quantum.core.data.map.sorted_map_PLUS_,obj);
});
/**
 * Single-threaded to get around a weird 'ClassCastException' which
 * occurs presumably because of thread overload.
 */
quantum.core.reducers.foldm_s_PLUS_ = (function quantum$core$reducers$foldm_s_PLUS_(obj){
return quantum.core.reducers.into_PLUS_.call(null,cljs.core.PersistentArrayMap.EMPTY,quantum.core.reducers.fold_PLUS_.call(null,obj));
});
/**
 * Fold into hash-set.
 */
quantum.core.reducers.fold_s_PLUS_ = (function quantum$core$reducers$fold_s_PLUS_(coll){
return quantum.core.reducers.fold_PLUS_.call(null,(function() {
var G__23203 = null;
var G__23203__0 = (function (){
return cljs.core.PersistentHashSet.EMPTY;
});
var G__23203__2 = (function (ret,elem){
return quantum.core.data.set.union.call(null,ret,((!(cljs.core.set_QMARK_.call(null,elem)))?cljs.core.PersistentHashSet.fromArray([elem],true):elem));
});
G__23203 = function(ret,elem){
switch(arguments.length){
case 0:
return G__23203__0.call(this);
case 2:
return G__23203__2.call(this,ret,elem);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
G__23203.cljs$core$IFn$_invoke$arity$0 = G__23203__0;
G__23203.cljs$core$IFn$_invoke$arity$2 = G__23203__2;
return G__23203;
})()
,coll);
});
/**
 * Reduce into hash-set.
 */
quantum.core.reducers.reduce_s_PLUS_ = (function quantum$core$reducers$reduce_s_PLUS_(coll){
return quantum.core.reducers.reduce.call(null,(function() {
var G__23204 = null;
var G__23204__2 = (function (ret,elem){
return cljs.core.conj.call(null,ret,elem);
});
var G__23204__3 = (function (ret,k,v){
return cljs.core.conj.call(null,ret,quantum.core.data.map.map_entry.call(null,k,v));
});
G__23204 = function(ret,k,v){
switch(arguments.length){
case 2:
return G__23204__2.call(this,ret,k);
case 3:
return G__23204__3.call(this,ret,k,v);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
G__23204.cljs$core$IFn$_invoke$arity$2 = G__23204__2;
G__23204.cljs$core$IFn$_invoke$arity$3 = G__23204__3;
return G__23204;
})()
,cljs.core.PersistentHashSet.EMPTY,cljs.core.force.call(null,coll));
});
/**
 * Like map, but threads a state through the sequence of transformations.
 * For each x in coll, f is applied to [state x] and should return [state' x'].
 * The first invocation of f uses init as the state.
 */
quantum.core.reducers.map_state = (function quantum$core$reducers$map_state(){
var G__23206 = arguments.length;
switch (G__23206) {
case 2:
return quantum.core.reducers.map_state.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return quantum.core.reducers.map_state.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.reducers.map_state.cljs$core$IFn$_invoke$arity$2 = (function (f,init){
return (function (x__22705__auto__){
return quantum.core.reducers.map_state.call(null,f,init,x__22705__auto__);
});
});

quantum.core.reducers.map_state.cljs$core$IFn$_invoke$arity$3 = (function (f,init,coll){
return quantum.core.reducers.reducer_PLUS_.call(null,quantum.core.reducers.fold_pre.call(null,coll),(function (f1){
var state = cljs.core.atom.call(null,init);
return ((function (state){
return (function (acc,x){
var vec__23207 = f.call(null,cljs.core.deref.call(null,state),x);
var state_STAR_ = cljs.core.nth.call(null,vec__23207,(0),null);
var x_STAR_ = cljs.core.nth.call(null,vec__23207,(1),null);
cljs.core.reset_BANG_.call(null,state,state_STAR_);

return f1.call(null,acc,x_STAR_);
});
;})(state))
}));
});

quantum.core.reducers.map_state.cljs$lang$maxFixedArity = 3;
/**
 * Like mapcat, but threads a state through the sequence of transformations. ; so basically like /reductions/?
 * For each x in coll, f is applied to [state x] and should return [state' xs].
 * The result is the concatenation of each returned xs.
 */
quantum.core.reducers.mapcat_state = (function quantum$core$reducers$mapcat_state(){
var G__23210 = arguments.length;
switch (G__23210) {
case 2:
return quantum.core.reducers.mapcat_state.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return quantum.core.reducers.mapcat_state.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.reducers.mapcat_state.cljs$core$IFn$_invoke$arity$2 = (function (f,init){
return (function (x__22705__auto__){
return quantum.core.reducers.mapcat_state.call(null,f,init,x__22705__auto__);
});
});

quantum.core.reducers.mapcat_state.cljs$core$IFn$_invoke$arity$3 = (function (f,init,coll){
return quantum.core.reducers.reducer_PLUS_.call(null,coll,(function (f1){
var state = cljs.core.atom.call(null,init);
return ((function (state){
return (function (acc,x){
var vec__23211 = f.call(null,cljs.core.deref.call(null,state),x);
var state_STAR_ = cljs.core.nth.call(null,vec__23211,(0),null);
var xs = cljs.core.nth.call(null,vec__23211,(1),null);
cljs.core.reset_BANG_.call(null,state,state_STAR_);

if(cljs.core.seq.call(null,xs)){
return quantum.core.reducers.reduce.call(null,f1,acc,xs);
} else {
return acc;
}
});
;})(state))
}));
});

quantum.core.reducers.mapcat_state.cljs$lang$maxFixedArity = 3;

/**
* @constructor
*/
quantum.core.reducers.Cat = (function (cnt,left,right){
this.cnt = cnt;
this.left = left;
this.right = right;
this.cljs$lang$protocol_mask$partition0$ = 8912898;
this.cljs$lang$protocol_mask$partition1$ = 0;
})
quantum.core.reducers.Cat.prototype.cljs$core$ICounted$_count$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.cnt;
});

quantum.core.reducers.Cat.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.concat.call(null,cljs.core.seq.call(null,self__.left),cljs.core.seq.call(null,self__.right));
});

quantum.core.reducers.Cat.prototype.cljs$core$IReduce$_reduce$arity$2 = (function (this$,f1){
var self__ = this;
var this$__$1 = this;
return cljs.core._reduce.call(null,this$__$1,f1,f1.call(null));
});

quantum.core.reducers.Cat.prototype.cljs$core$IReduce$_reduce$arity$3 = (function (_,f1,init){
var self__ = this;
var ___$1 = this;
return cljs.core._reduce.call(null,self__.right,f1,cljs.core._reduce.call(null,self__.left,f1,init));
});

quantum.core.reducers.Cat.prototype.quantum$core$reducers$CollFold$ = true;

quantum.core.reducers.Cat.prototype.quantum$core$reducers$CollFold$coll_fold$arity$4 = (function (this$,n,combinef,reducef){
var self__ = this;
var this$__$1 = this;
return cljs.core._reduce.call(null,this$__$1,reducef);
});

quantum.core.reducers.Cat.getBasis = (function (){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"cnt","cnt",1924510325,null),new cljs.core.Symbol(null,"left","left",1241415590,null),new cljs.core.Symbol(null,"right","right",1187949694,null)], null);
});

quantum.core.reducers.Cat.cljs$lang$type = true;

quantum.core.reducers.Cat.cljs$lang$ctorStr = "quantum.core.reducers/Cat";

quantum.core.reducers.Cat.cljs$lang$ctorPrWriter = (function (this__18652__auto__,writer__18653__auto__,opt__18654__auto__){
return cljs.core._write.call(null,writer__18653__auto__,"quantum.core.reducers/Cat");
});

quantum.core.reducers.__GT_Cat = (function quantum$core$reducers$__GT_Cat(cnt,left,right){
return (new quantum.core.reducers.Cat(cnt,left,right));
});

/**
 * A high-performance combining fn that yields the catenation of the
 * reduced values. The result is reducible, foldable, seqable and
 * counted, providing the identity collections are reducible, seqable
 * and counted. The single argument version will build a combining fn
 * with the supplied identity constructor. Tests for identity
 * with (zero? (count x)). See also foldcat.
 */
quantum.core.reducers.cat_PLUS_ = (function quantum$core$reducers$cat_PLUS_(){
var G__23214 = arguments.length;
switch (G__23214) {
case 0:
return quantum.core.reducers.cat_PLUS_.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return quantum.core.reducers.cat_PLUS_.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return quantum.core.reducers.cat_PLUS_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.reducers.cat_PLUS_.cljs$core$IFn$_invoke$arity$0 = (function (){
return [];
});

quantum.core.reducers.cat_PLUS_.cljs$core$IFn$_invoke$arity$1 = (function (ctor){
return (function() {
var G__23216 = null;
var G__23216__0 = (function (){
return ctor.call(null);
});
var G__23216__2 = (function (left,right){
return quantum.core.reducers.cat_PLUS_.call(null,left,right);
});
G__23216 = function(left,right){
switch(arguments.length){
case 0:
return G__23216__0.call(this);
case 2:
return G__23216__2.call(this,left,right);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
G__23216.cljs$core$IFn$_invoke$arity$0 = G__23216__0;
G__23216.cljs$core$IFn$_invoke$arity$2 = G__23216__2;
return G__23216;
})()
});

quantum.core.reducers.cat_PLUS_.cljs$core$IFn$_invoke$arity$2 = (function (left_0,right_0){
var left = quantum.core.reducers.fold_pre.call(null,left_0);
var right = quantum.core.reducers.fold_pre.call(null,right_0);
if((cljs.core.count.call(null,left) === (0))){
return right;
} else {
if((cljs.core.count.call(null,right) === (0))){
return left;
} else {
return (new quantum.core.reducers.Cat((cljs.core.count.call(null,left) + cljs.core.count.call(null,right)),left,right));

}
}
});

quantum.core.reducers.cat_PLUS_.cljs$lang$maxFixedArity = 2;
/**
 * .adds x to acc and returns acc
 */
quantum.core.reducers.append_BANG_ = (function quantum$core$reducers$append_BANG_(acc,x){
var G__23218 = acc;
G__23218.push(x);

return G__23218;
});
/**
 * Equivalent to (fold cat append! coll)
 */
quantum.core.reducers.foldcat_PLUS_ = (function quantum$core$reducers$foldcat_PLUS_(coll){
return quantum.core.reducers.fold_PLUS_.call(null,quantum.core.reducers.cat_PLUS_,quantum.core.reducers.append_BANG_,coll);
});
/**
 * Applies f to every value in the reduction of coll. Foldable.
 */
quantum.core.reducers.map_STAR_ = (function quantum$core$reducers$map_STAR_(){
var G__23220 = arguments.length;
switch (G__23220) {
case 1:
return quantum.core.reducers.map_STAR_.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return quantum.core.reducers.map_STAR_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.reducers.map_STAR_.cljs$core$IFn$_invoke$arity$1 = (function (f){
return (function (x__22705__auto__){
return quantum.core.reducers.map_STAR_.call(null,f,x__22705__auto__);
});
});

quantum.core.reducers.map_STAR_.cljs$core$IFn$_invoke$arity$2 = (function (f,coll){
return quantum.core.reducers.folder_PLUS_.call(null,coll,(function (f1){
return (function() {
var G__23222 = null;
var G__23222__0 = (function (){
return f1.call(null);
});
var G__23222__2 = (function (ret,v){
return f1.call(null,ret,f.call(null,v));
});
var G__23222__3 = (function (ret,k,v){
return f1.call(null,ret,f.call(null,k,v));
});
G__23222 = function(ret,k,v){
switch(arguments.length){
case 0:
return G__23222__0.call(this);
case 2:
return G__23222__2.call(this,ret,k);
case 3:
return G__23222__3.call(this,ret,k,v);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
G__23222.cljs$core$IFn$_invoke$arity$0 = G__23222__0;
G__23222.cljs$core$IFn$_invoke$arity$2 = G__23222__2;
G__23222.cljs$core$IFn$_invoke$arity$3 = G__23222__3;
return G__23222;
})()
}));
});

quantum.core.reducers.map_STAR_.cljs$lang$maxFixedArity = 2;
quantum.core.reducers.map_PLUS_ = (function quantum$core$reducers$map_PLUS_(func,coll){
return (new cljs.core.Delay((function (){
return quantum.core.reducers.map_STAR_.call(null,func,quantum.core.reducers.fold_pre.call(null,coll));
}),null));
});
/**
 * Reducers version of /map-indexed/.
 */
quantum.core.reducers.map_indexed_PLUS_ = (function quantum$core$reducers$map_indexed_PLUS_(f,coll){
return quantum.core.reducers.map_state.call(null,(function (n,x){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(n + (1)),f.call(null,n,x)], null);
}),(0),coll);
});
/**
 * Returns an ordered sequence of vectors `[index item]`, where item is a
 * value in coll, and index its position starting from zero.
 */
quantum.core.reducers.indexed_PLUS_ = (function quantum$core$reducers$indexed_PLUS_(coll){
return quantum.core.reducers.map_indexed_PLUS_.call(null,cljs.core.vector,coll);
});
/**
 * Applies f to every value in the reduction of coll, concatenating the result
 * colls of (f val). Foldable.
 */
quantum.core.reducers.mapcat_STAR_ = (function quantum$core$reducers$mapcat_STAR_(){
var G__23224 = arguments.length;
switch (G__23224) {
case 1:
return quantum.core.reducers.mapcat_STAR_.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return quantum.core.reducers.mapcat_STAR_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.reducers.mapcat_STAR_.cljs$core$IFn$_invoke$arity$1 = (function (f){
return (function (x__22705__auto__){
return quantum.core.reducers.mapcat_STAR_.call(null,f,x__22705__auto__);
});
});

quantum.core.reducers.mapcat_STAR_.cljs$core$IFn$_invoke$arity$2 = (function (f,coll){
return quantum.core.reducers.folder_PLUS_.call(null,coll,(function (f1_0){
var f1 = (function() {
var G__23226 = null;
var G__23226__0 = (function (){
var x = f1_0.call(null);
if(cljs.core.reduced_QMARK_.call(null,x)){
return cljs.core.reduced.call(null,x);
} else {
return x;
}
});
var G__23226__1 = (function (ret){
var x = f1_0.call(null,ret);
if(cljs.core.reduced_QMARK_.call(null,x)){
return cljs.core.reduced.call(null,x);
} else {
return x;
}
});
var G__23226__2 = (function (ret,v){
var x = f1_0.call(null,ret,v);
if(cljs.core.reduced_QMARK_.call(null,x)){
return cljs.core.reduced.call(null,x);
} else {
return x;
}
});
var G__23226__3 = (function (ret,k,v){
var x = f1_0.call(null,ret,k,v);
if(cljs.core.reduced_QMARK_.call(null,x)){
return cljs.core.reduced.call(null,x);
} else {
return x;
}
});
G__23226 = function(ret,k,v){
switch(arguments.length){
case 0:
return G__23226__0.call(this);
case 1:
return G__23226__1.call(this,ret);
case 2:
return G__23226__2.call(this,ret,k);
case 3:
return G__23226__3.call(this,ret,k,v);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
G__23226.cljs$core$IFn$_invoke$arity$0 = G__23226__0;
G__23226.cljs$core$IFn$_invoke$arity$1 = G__23226__1;
G__23226.cljs$core$IFn$_invoke$arity$2 = G__23226__2;
G__23226.cljs$core$IFn$_invoke$arity$3 = G__23226__3;
return G__23226;
})()
;
return ((function (f1){
return (function() {
var G__23227 = null;
var G__23227__0 = (function (){
return f1.call(null);
});
var G__23227__2 = (function (ret,v){
return quantum.core.reducers.reduce.call(null,f1,ret,f.call(null,v));
});
var G__23227__3 = (function (ret,k,v){
return quantum.core.reducers.reduce.call(null,f1,ret,f.call(null,k,v));
});
G__23227 = function(ret,k,v){
switch(arguments.length){
case 0:
return G__23227__0.call(this);
case 2:
return G__23227__2.call(this,ret,k);
case 3:
return G__23227__3.call(this,ret,k,v);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
G__23227.cljs$core$IFn$_invoke$arity$0 = G__23227__0;
G__23227.cljs$core$IFn$_invoke$arity$2 = G__23227__2;
G__23227.cljs$core$IFn$_invoke$arity$3 = G__23227__3;
return G__23227;
})()
;})(f1))
}));
});

quantum.core.reducers.mapcat_STAR_.cljs$lang$maxFixedArity = 2;
quantum.core.reducers.mapcat_PLUS_ = (function quantum$core$reducers$mapcat_PLUS_(func,coll){
return (new cljs.core.Delay((function (){
return quantum.core.reducers.mapcat_STAR_.call(null,func,quantum.core.reducers.fold_pre.call(null,coll));
}),null));
});
quantum.core.reducers.concat_PLUS_ = (function quantum$core$reducers$concat_PLUS_(){
var argseq__19113__auto__ = ((((0) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(0)),(0))):null);
return quantum.core.reducers.concat_PLUS_.cljs$core$IFn$_invoke$arity$variadic(argseq__19113__auto__);
});

quantum.core.reducers.concat_PLUS_.cljs$core$IFn$_invoke$arity$variadic = (function (args){
return quantum.core.reducers.reduce.call(null,quantum.core.reducers.cat_PLUS_,args);
});

quantum.core.reducers.concat_PLUS_.cljs$lang$maxFixedArity = (0);

quantum.core.reducers.concat_PLUS_.cljs$lang$applyTo = (function (seq23228){
return quantum.core.reducers.concat_PLUS_.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq23228));
});
/**
 * Reducers version of /reductions/.
 * Returns a reducer of the intermediate values of the reduction (as per reduce) of coll by f.
 * 
 */
quantum.core.reducers.reductions_PLUS_ = (function quantum$core$reducers$reductions_PLUS_(){
var G__23230 = arguments.length;
switch (G__23230) {
case 2:
return quantum.core.reducers.reductions_PLUS_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return quantum.core.reducers.reductions_PLUS_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.reducers.reductions_PLUS_.cljs$core$IFn$_invoke$arity$2 = (function (f,coll){
return quantum.core.reducers.reductions_PLUS_.call(null,f,f.call(null),coll);
});

quantum.core.reducers.reductions_PLUS_.cljs$core$IFn$_invoke$arity$3 = (function (f,init,coll){
var sentinel = [];
return (new cljs.core.Delay(((function (sentinel){
return (function (){
return quantum.core.reducers.map_state.call(null,((function (sentinel){
return (function (acc,x){
if((sentinel === x)){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [null,acc], null);
} else {
var acc_SINGLEQUOTE_ = f.call(null,acc,x);
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [acc_SINGLEQUOTE_,acc], null);
}
});})(sentinel))
,init,quantum.core.reducers.mapcat_PLUS_.call(null,cljs.core.identity,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [coll,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [sentinel], null)], null)));
});})(sentinel))
,null));
});

quantum.core.reducers.reductions_PLUS_.cljs$lang$maxFixedArity = 3;
/**
 * Retains values in the reduction of coll for which (pred val)
 * returns logical true. Foldable.
 */
quantum.core.reducers.filter_STAR_ = (function quantum$core$reducers$filter_STAR_(){
var G__23233 = arguments.length;
switch (G__23233) {
case 1:
return quantum.core.reducers.filter_STAR_.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return quantum.core.reducers.filter_STAR_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.reducers.filter_STAR_.cljs$core$IFn$_invoke$arity$1 = (function (pred){
return (function (x__22705__auto__){
return quantum.core.reducers.filter_STAR_.call(null,pred,x__22705__auto__);
});
});

quantum.core.reducers.filter_STAR_.cljs$core$IFn$_invoke$arity$2 = (function (pred,coll){
return quantum.core.reducers.folder_PLUS_.call(null,coll,(function (f1){
return (function() {
var G__23235 = null;
var G__23235__0 = (function (){
return f1.call(null);
});
var G__23235__2 = (function (ret,v){
if(cljs.core.truth_(pred.call(null,v))){
return f1.call(null,ret,v);
} else {
return ret;
}
});
var G__23235__3 = (function (ret,k,v){
if(cljs.core.truth_(pred.call(null,k,v))){
return f1.call(null,ret,k,v);
} else {
return ret;
}
});
G__23235 = function(ret,k,v){
switch(arguments.length){
case 0:
return G__23235__0.call(this);
case 2:
return G__23235__2.call(this,ret,k);
case 3:
return G__23235__3.call(this,ret,k,v);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
G__23235.cljs$core$IFn$_invoke$arity$0 = G__23235__0;
G__23235.cljs$core$IFn$_invoke$arity$2 = G__23235__2;
G__23235.cljs$core$IFn$_invoke$arity$3 = G__23235__3;
return G__23235;
})()
}));
});

quantum.core.reducers.filter_STAR_.cljs$lang$maxFixedArity = 2;
quantum.core.reducers.filter_PLUS_ = (function quantum$core$reducers$filter_PLUS_(func,coll){
return (new cljs.core.Delay((function (){
return quantum.core.reducers.filter_STAR_.call(null,func,quantum.core.reducers.fold_pre.call(null,coll));
}),null));
});
/**
 * Removes values in the reduction of coll for which (pred val)
 * returns logical true. Foldable.
 */
quantum.core.reducers.remove_STAR_ = (function quantum$core$reducers$remove_STAR_(){
var G__23237 = arguments.length;
switch (G__23237) {
case 1:
return quantum.core.reducers.remove_STAR_.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return quantum.core.reducers.remove_STAR_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.reducers.remove_STAR_.cljs$core$IFn$_invoke$arity$1 = (function (pred){
return (function (x__22705__auto__){
return quantum.core.reducers.remove_STAR_.call(null,pred,x__22705__auto__);
});
});

quantum.core.reducers.remove_STAR_.cljs$core$IFn$_invoke$arity$2 = (function (pred,coll){
return quantum.core.reducers.filter_PLUS_.call(null,quantum.core.logic.fn_not.call(null,pred),coll);
});

quantum.core.reducers.remove_STAR_.cljs$lang$maxFixedArity = 2;
quantum.core.reducers.remove_PLUS_ = (function quantum$core$reducers$remove_PLUS_(func,coll){
return quantum.core.reducers.remove_STAR_.call(null,func,quantum.core.reducers.fold_pre.call(null,coll));
});
quantum.core.reducers.keep_PLUS_ = quantum.core.function$.compr.call(null,quantum.core.reducers.map_PLUS_,cljs.core.partial.call(null,quantum.core.reducers.remove_PLUS_,cljs.core.nil_QMARK_));
/**
 * Takes any nested combination of sequential things (lists, vectors,
 * etc.) and returns their contents as a single, flat foldable
 * collection.
 */
quantum.core.reducers.flatten_STAR_ = (function quantum$core$reducers$flatten_STAR_(){
var G__23240 = arguments.length;
switch (G__23240) {
case 0:
return quantum.core.reducers.flatten_STAR_.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return quantum.core.reducers.flatten_STAR_.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.reducers.flatten_STAR_.cljs$core$IFn$_invoke$arity$0 = (function (){
return (function (x__22705__auto__){
return quantum.core.reducers.flatten_STAR_.call(null,x__22705__auto__);
});
});

quantum.core.reducers.flatten_STAR_.cljs$core$IFn$_invoke$arity$1 = (function (coll){
return quantum.core.reducers.folder_PLUS_.call(null,coll,(function (f1){
return (function() {
var G__23242 = null;
var G__23242__0 = (function (){
return f1.call(null);
});
var G__23242__2 = (function (ret,v){
if(cljs.core.sequential_QMARK_.call(null,v)){
return cljs.core._reduce.call(null,quantum.core.reducers.flatten_STAR_.call(null,v),f1,ret);
} else {
return f1.call(null,ret,v);
}
});
G__23242 = function(ret,v){
switch(arguments.length){
case 0:
return G__23242__0.call(this);
case 2:
return G__23242__2.call(this,ret,v);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
G__23242.cljs$core$IFn$_invoke$arity$0 = G__23242__0;
G__23242.cljs$core$IFn$_invoke$arity$2 = G__23242__2;
return G__23242;
})()
}));
});

quantum.core.reducers.flatten_STAR_.cljs$lang$maxFixedArity = 1;
quantum.core.reducers.flatten_PLUS_ = (function quantum$core$reducers$flatten_PLUS_(coll){
return (new cljs.core.Delay((function (){
return quantum.core.reducers.flatten_STAR_.call(null,quantum.core.reducers.fold_pre.call(null,coll));
}),null));
});
quantum.core.reducers.flatten_1_PLUS_ = cljs.core.partial.call(null,quantum.core.reducers.mapcat_PLUS_,cljs.core.identity);
/**
 * A reducible collection of [seed, (f seed), (f (f seed)), ...]
 */
quantum.core.reducers.iterate_STAR_ = (function quantum$core$reducers$iterate_STAR_(){
var G__23244 = arguments.length;
switch (G__23244) {
case 1:
return quantum.core.reducers.iterate_STAR_.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return quantum.core.reducers.iterate_STAR_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.reducers.iterate_STAR_.cljs$core$IFn$_invoke$arity$1 = (function (f){
return (function (x__22705__auto__){
return quantum.core.reducers.iterate_STAR_.call(null,f,x__22705__auto__);
});
});

quantum.core.reducers.iterate_STAR_.cljs$core$IFn$_invoke$arity$2 = (function (f,seed){
if(typeof quantum.core.reducers.t23245 !== 'undefined'){
} else {

/**
* @constructor
*/
quantum.core.reducers.t23245 = (function (f,seed,meta23246){
this.f = f;
this.seed = seed;
this.meta23246 = meta23246;
this.cljs$lang$protocol_mask$partition0$ = 9306112;
this.cljs$lang$protocol_mask$partition1$ = 0;
})
quantum.core.reducers.t23245.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_23247,meta23246__$1){
var self__ = this;
var _23247__$1 = this;
return (new quantum.core.reducers.t23245(self__.f,self__.seed,meta23246__$1));
});

quantum.core.reducers.t23245.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_23247){
var self__ = this;
var _23247__$1 = this;
return self__.meta23246;
});

quantum.core.reducers.t23245.prototype.cljs$core$IReduce$_reduce$arity$2 = (function (this$,f1){
var self__ = this;
var this$__$1 = this;
return cljs.core._reduce.call(null,this$__$1,f1,f1.call(null));
});

quantum.core.reducers.t23245.prototype.cljs$core$IReduce$_reduce$arity$3 = (function (this$,f1,init){
var self__ = this;
var this$__$1 = this;
var ret = f1.call(null,init,self__.seed);
var seed__$1 = self__.seed;
while(true){
if(cljs.core.reduced_QMARK_.call(null,ret)){
return cljs.core.deref.call(null,ret);
} else {
var next_n = self__.f.call(null,seed__$1);
var G__23249 = f1.call(null,ret,next_n);
var G__23250 = next_n;
ret = G__23249;
seed__$1 = G__23250;
continue;
}
break;
}
});

quantum.core.reducers.t23245.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return cljs.core.seq.call(null,cljs.core.iterate.call(null,self__.f,self__.seed));
});

quantum.core.reducers.t23245.getBasis = (function (){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"f","f",43394975,null),new cljs.core.Symbol(null,"seed","seed",1709144854,null),new cljs.core.Symbol(null,"meta23246","meta23246",1363757078,null)], null);
});

quantum.core.reducers.t23245.cljs$lang$type = true;

quantum.core.reducers.t23245.cljs$lang$ctorStr = "quantum.core.reducers/t23245";

quantum.core.reducers.t23245.cljs$lang$ctorPrWriter = (function (this__18652__auto__,writer__18653__auto__,opt__18654__auto__){
return cljs.core._write.call(null,writer__18653__auto__,"quantum.core.reducers/t23245");
});

quantum.core.reducers.__GT_t23245 = (function quantum$core$reducers$__GT_t23245(f__$1,seed__$1,meta23246){
return (new quantum.core.reducers.t23245(f__$1,seed__$1,meta23246));
});

}

return (new quantum.core.reducers.t23245(f,seed,cljs.core.PersistentArrayMap.EMPTY));
});

quantum.core.reducers.iterate_STAR_.cljs$lang$maxFixedArity = 2;
quantum.core.reducers.iterate_PLUS_ = (function quantum$core$reducers$iterate_PLUS_(func,seed){
return (new cljs.core.Delay((function (){
return quantum.core.reducers.iterate_STAR_.call(null,func,quantum.core.reducers.fold_pre.call(null,seed));
}),null));
});

/**
* @constructor
*/
quantum.core.reducers.Range = (function (start,end,step){
this.start = start;
this.end = end;
this.step = step;
this.cljs$lang$protocol_mask$partition0$ = 8912898;
this.cljs$lang$protocol_mask$partition1$ = 0;
})
quantum.core.reducers.Range.prototype.cljs$core$ICounted$_count$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return (quantum.core.numeric.ceil.call(null,((self__.end - self__.start) / self__.step)) | (0));
});

quantum.core.reducers.Range.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return cljs.core.seq.call(null,cljs.core.range.call(null,self__.start,self__.end,self__.step));
});

quantum.core.reducers.Range.prototype.cljs$core$IReduce$_reduce$arity$2 = (function (this$,f1){
var self__ = this;
var this$__$1 = this;
return cljs.core._reduce.call(null,this$__$1,f1,f1.call(null));
});

quantum.core.reducers.Range.prototype.cljs$core$IReduce$_reduce$arity$3 = (function (this$,f1,init){
var self__ = this;
var this$__$1 = this;
var cmp = (((self__.step > (0)))?cljs.core._LT_:cljs.core._GT_);
var ret = init;
var i = self__.start;
while(true){
if(cljs.core.reduced_QMARK_.call(null,ret)){
return cljs.core.deref.call(null,ret);
} else {
if(cljs.core.truth_(cmp.call(null,i,self__.end))){
var G__23251 = f1.call(null,ret,i);
var G__23252 = (i + self__.step);
ret = G__23251;
i = G__23252;
continue;
} else {
return ret;
}
}
break;
}
});

quantum.core.reducers.Range.prototype.quantum$core$reducers$CollFold$ = true;

quantum.core.reducers.Range.prototype.quantum$core$reducers$CollFold$coll_fold$arity$4 = (function (this$,n,combinef,reducef){
var self__ = this;
var this$__$1 = this;
return quantum.core.reducers.fold_by_halves.call(null,((function (this$__$1){
return (function (_,size){
var split = ((cljs.core.quot.call(null,size,(2)) * self__.step) + self__.start);
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [(new quantum.core.reducers.Range(self__.start,split,self__.step)),(new quantum.core.reducers.Range(split,self__.end,self__.step))], null);
});})(this$__$1))
,this$__$1,n,combinef,reducef);
});

quantum.core.reducers.Range.getBasis = (function (){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"start","start",1285322546,null),new cljs.core.Symbol(null,"end","end",1372345569,null),new cljs.core.Symbol(null,"step","step",-1365547645,null)], null);
});

quantum.core.reducers.Range.cljs$lang$type = true;

quantum.core.reducers.Range.cljs$lang$ctorStr = "quantum.core.reducers/Range";

quantum.core.reducers.Range.cljs$lang$ctorPrWriter = (function (this__18652__auto__,writer__18653__auto__,opt__18654__auto__){
return cljs.core._write.call(null,writer__18653__auto__,"quantum.core.reducers/Range");
});

quantum.core.reducers.__GT_Range = (function quantum$core$reducers$__GT_Range(start,end,step){
return (new quantum.core.reducers.Range(start,end,step));
});

/**
 * Returns a reducible collection of nums from start (inclusive) to end
 * (exclusive), by step, where start defaults to 0, step to 1, and end
 * to infinity.
 */
quantum.core.reducers.range_PLUS_ = (function quantum$core$reducers$range_PLUS_(){
var G__23254 = arguments.length;
switch (G__23254) {
case 0:
return quantum.core.reducers.range_PLUS_.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return quantum.core.reducers.range_PLUS_.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return quantum.core.reducers.range_PLUS_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return quantum.core.reducers.range_PLUS_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.reducers.range_PLUS_.cljs$core$IFn$_invoke$arity$0 = (function (){
return quantum.core.reducers.iterate_PLUS_.call(null,cljs.core.inc,(0));
});

quantum.core.reducers.range_PLUS_.cljs$core$IFn$_invoke$arity$1 = (function (end){
return (new quantum.core.reducers.Range((0),end,(1)));
});

quantum.core.reducers.range_PLUS_.cljs$core$IFn$_invoke$arity$2 = (function (start,end){
return (new quantum.core.reducers.Range(start,end,(1)));
});

quantum.core.reducers.range_PLUS_.cljs$core$IFn$_invoke$arity$3 = (function (start,end,step){
return (new quantum.core.reducers.Range(start,end,step));
});

quantum.core.reducers.range_PLUS_.cljs$lang$maxFixedArity = 3;
/**
 * Ends the reduction of coll after consuming n values.
 */
quantum.core.reducers.take_STAR_ = (function quantum$core$reducers$take_STAR_(){
var G__23257 = arguments.length;
switch (G__23257) {
case 1:
return quantum.core.reducers.take_STAR_.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return quantum.core.reducers.take_STAR_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.reducers.take_STAR_.cljs$core$IFn$_invoke$arity$1 = (function (n){
return (function (x__22705__auto__){
return quantum.core.reducers.take_STAR_.call(null,n,x__22705__auto__);
});
});

quantum.core.reducers.take_STAR_.cljs$core$IFn$_invoke$arity$2 = (function (n,coll){
return quantum.core.reducers.reducer_PLUS_.call(null,coll,(function (f1){
var cnt = cljs.core.atom.call(null,n);
return ((function (cnt){
return (function() {
var G__23259 = null;
var G__23259__0 = (function (){
return f1.call(null);
});
var G__23259__2 = (function (ret,v){
cljs.core.swap_BANG_.call(null,cnt,cljs.core.dec);

if((cljs.core.deref.call(null,cnt) < (0))){
return cljs.core.reduced.call(null,ret);
} else {
return f1.call(null,ret,v);
}
});
var G__23259__3 = (function (ret,k,v){
cljs.core.swap_BANG_.call(null,cnt,cljs.core.dec);

if((cljs.core.deref.call(null,cnt) < (0))){
return cljs.core.reduced.call(null,ret);
} else {
return f1.call(null,ret,k,v);
}
});
G__23259 = function(ret,k,v){
switch(arguments.length){
case 0:
return G__23259__0.call(this);
case 2:
return G__23259__2.call(this,ret,k);
case 3:
return G__23259__3.call(this,ret,k,v);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
G__23259.cljs$core$IFn$_invoke$arity$0 = G__23259__0;
G__23259.cljs$core$IFn$_invoke$arity$2 = G__23259__2;
G__23259.cljs$core$IFn$_invoke$arity$3 = G__23259__3;
return G__23259;
})()
;})(cnt))
}));
});

quantum.core.reducers.take_STAR_.cljs$lang$maxFixedArity = 2;
quantum.core.reducers.take_PLUS_ = (function quantum$core$reducers$take_PLUS_(n,coll){
return (new cljs.core.Delay((function (){
return quantum.core.reducers.take_STAR_.call(null,n,quantum.core.reducers.fold_pre.call(null,coll));
}),null));
});
/**
 * Ends the reduction of coll when (pred val) returns logical false.
 */
quantum.core.reducers.take_while_STAR_ = (function quantum$core$reducers$take_while_STAR_(){
var G__23261 = arguments.length;
switch (G__23261) {
case 1:
return quantum.core.reducers.take_while_STAR_.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return quantum.core.reducers.take_while_STAR_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.reducers.take_while_STAR_.cljs$core$IFn$_invoke$arity$1 = (function (pred){
return (function (x__22705__auto__){
return quantum.core.reducers.take_while_STAR_.call(null,pred,x__22705__auto__);
});
});

quantum.core.reducers.take_while_STAR_.cljs$core$IFn$_invoke$arity$2 = (function (pred,coll){
return quantum.core.reducers.reducer_PLUS_.call(null,coll,(function (f1){
return (function() {
var G__23263 = null;
var G__23263__0 = (function (){
return f1.call(null);
});
var G__23263__2 = (function (ret,v){
if(cljs.core.truth_(pred.call(null,v))){
return f1.call(null,ret,v);
} else {
return cljs.core.reduced.call(null,ret);
}
});
var G__23263__3 = (function (ret,k,v){
if(cljs.core.truth_(pred.call(null,k,v))){
return f1.call(null,ret,k,v);
} else {
return cljs.core.reduced.call(null,ret);
}
});
G__23263 = function(ret,k,v){
switch(arguments.length){
case 0:
return G__23263__0.call(this);
case 2:
return G__23263__2.call(this,ret,k);
case 3:
return G__23263__3.call(this,ret,k,v);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
G__23263.cljs$core$IFn$_invoke$arity$0 = G__23263__0;
G__23263.cljs$core$IFn$_invoke$arity$2 = G__23263__2;
G__23263.cljs$core$IFn$_invoke$arity$3 = G__23263__3;
return G__23263;
})()
}));
});

quantum.core.reducers.take_while_STAR_.cljs$lang$maxFixedArity = 2;
quantum.core.reducers.take_while_PLUS_ = (function quantum$core$reducers$take_while_PLUS_(pred,coll){
return (new cljs.core.Delay((function (){
return quantum.core.reducers.take_while_STAR_.call(null,pred,quantum.core.reducers.fold_pre.call(null,coll));
}),null));
});
/**
 * Elides the first n values from the reduction of coll.
 */
quantum.core.reducers.drop_STAR_ = (function quantum$core$reducers$drop_STAR_(){
var G__23265 = arguments.length;
switch (G__23265) {
case 1:
return quantum.core.reducers.drop_STAR_.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return quantum.core.reducers.drop_STAR_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.reducers.drop_STAR_.cljs$core$IFn$_invoke$arity$1 = (function (n){
return (function (x__22705__auto__){
return quantum.core.reducers.drop_STAR_.call(null,n,x__22705__auto__);
});
});

quantum.core.reducers.drop_STAR_.cljs$core$IFn$_invoke$arity$2 = (function (n,coll){
return quantum.core.reducers.reducer_PLUS_.call(null,coll,(function (f1){
var cnt = cljs.core.atom.call(null,n);
return ((function (cnt){
return (function() {
var G__23267 = null;
var G__23267__0 = (function (){
return f1.call(null);
});
var G__23267__2 = (function (ret,v){
cljs.core.swap_BANG_.call(null,cnt,cljs.core.dec);

if((cljs.core.deref.call(null,cnt) < (0))){
return f1.call(null,ret,v);
} else {
return ret;
}
});
var G__23267__3 = (function (ret,k,v){
cljs.core.swap_BANG_.call(null,cnt,cljs.core.dec);

if((cljs.core.deref.call(null,cnt) < (0))){
return f1.call(null,ret,k,v);
} else {
return ret;
}
});
G__23267 = function(ret,k,v){
switch(arguments.length){
case 0:
return G__23267__0.call(this);
case 2:
return G__23267__2.call(this,ret,k);
case 3:
return G__23267__3.call(this,ret,k,v);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
G__23267.cljs$core$IFn$_invoke$arity$0 = G__23267__0;
G__23267.cljs$core$IFn$_invoke$arity$2 = G__23267__2;
G__23267.cljs$core$IFn$_invoke$arity$3 = G__23267__3;
return G__23267;
})()
;})(cnt))
}));
});

quantum.core.reducers.drop_STAR_.cljs$lang$maxFixedArity = 2;
quantum.core.reducers.drop_PLUS_ = (function quantum$core$reducers$drop_PLUS_(n,coll){
return (new cljs.core.Delay((function (){
return quantum.core.reducers.drop_STAR_.call(null,n,quantum.core.reducers.fold_pre.call(null,coll));
}),null));
});
/**
 * Skips values from the reduction of coll while (pred val) returns logical true.
 */
quantum.core.reducers.drop_while_STAR_ = (function quantum$core$reducers$drop_while_STAR_(){
var G__23269 = arguments.length;
switch (G__23269) {
case 1:
return quantum.core.reducers.drop_while_STAR_.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return quantum.core.reducers.drop_while_STAR_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.reducers.drop_while_STAR_.cljs$core$IFn$_invoke$arity$1 = (function (pred){
return (function (x__22705__auto__){
return quantum.core.reducers.drop_while_STAR_.call(null,pred,x__22705__auto__);
});
});

quantum.core.reducers.drop_while_STAR_.cljs$core$IFn$_invoke$arity$2 = (function (pred,coll){
return quantum.core.reducers.reducer_PLUS_.call(null,coll,(function (f1){
var keeping_QMARK_ = cljs.core.atom.call(null,false);
return ((function (keeping_QMARK_){
return (function() {
var G__23271 = null;
var G__23271__0 = (function (){
return f1.call(null);
});
var G__23271__2 = (function (ret,v){
if(cljs.core.truth_((function (){var or__18073__auto__ = cljs.core.deref.call(null,keeping_QMARK_);
if(cljs.core.truth_(or__18073__auto__)){
return or__18073__auto__;
} else {
return cljs.core.reset_BANG_.call(null,keeping_QMARK_,cljs.core.not.call(null,pred.call(null,v)));
}
})())){
return f1.call(null,ret,v);
} else {
return ret;
}
});
var G__23271__3 = (function (ret,k,v){
if(cljs.core.truth_((function (){var or__18073__auto__ = cljs.core.deref.call(null,keeping_QMARK_);
if(cljs.core.truth_(or__18073__auto__)){
return or__18073__auto__;
} else {
return cljs.core.reset_BANG_.call(null,keeping_QMARK_,cljs.core.not.call(null,pred.call(null,k,v)));
}
})())){
return f1.call(null,ret,k,v);
} else {
return ret;
}
});
G__23271 = function(ret,k,v){
switch(arguments.length){
case 0:
return G__23271__0.call(this);
case 2:
return G__23271__2.call(this,ret,k);
case 3:
return G__23271__3.call(this,ret,k,v);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
G__23271.cljs$core$IFn$_invoke$arity$0 = G__23271__0;
G__23271.cljs$core$IFn$_invoke$arity$2 = G__23271__2;
G__23271.cljs$core$IFn$_invoke$arity$3 = G__23271__3;
return G__23271;
})()
;})(keeping_QMARK_))
}));
});

quantum.core.reducers.drop_while_STAR_.cljs$lang$maxFixedArity = 2;
quantum.core.reducers.drop_while_PLUS_ = (function quantum$core$reducers$drop_while_PLUS_(pred,coll){
return (new cljs.core.Delay((function (){
return quantum.core.reducers.drop_while_STAR_.call(null,pred,quantum.core.reducers.fold_pre.call(null,coll));
}),null));
});
/**
 * Partition `coll` with `keyfn` as per /partition-by/, then reduce
 * each partition with `f` and optional initial value `init` as per
 * /reduce/.
 */
quantum.core.reducers.reduce_by_PLUS_ = (function quantum$core$reducers$reduce_by_PLUS_(){
var G__23273 = arguments.length;
switch (G__23273) {
case 3:
return quantum.core.reducers.reduce_by_PLUS_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return quantum.core.reducers.reduce_by_PLUS_.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.reducers.reduce_by_PLUS_.cljs$core$IFn$_invoke$arity$3 = (function (keyfn,f,coll){
var sentinel = [];
return quantum.core.reducers.remove_PLUS_.call(null,cljs.core.partial.call(null,cljs.core.identical_QMARK_,sentinel),quantum.core.reducers.map_state.call(null,((function (sentinel){
return (function (p__23274,x){
var vec__23275 = p__23274;
var k = cljs.core.nth.call(null,vec__23275,(0),null);
var acc = cljs.core.nth.call(null,vec__23275,(1),null);
if((sentinel === x)){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [null,acc], null);
} else {
var k_SINGLEQUOTE_ = keyfn.call(null,x);
if((cljs.core._EQ_.call(null,k,k_SINGLEQUOTE_)) || ((sentinel === k))){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [k_SINGLEQUOTE_,f.call(null,acc,x)], null),sentinel], null);
} else {
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [k_SINGLEQUOTE_,f.call(null,f.call(null),x)], null),acc], null);
}
}
});})(sentinel))
,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [sentinel,f.call(null)], null),quantum.core.reducers.mapcat_PLUS_.call(null,cljs.core.identity,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [coll,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [sentinel], null)], null))));
});

quantum.core.reducers.reduce_by_PLUS_.cljs$core$IFn$_invoke$arity$4 = (function (keyfn,f,init,coll){
var f__$1 = (function() {
var G__23277 = null;
var G__23277__0 = (function (){
return init;
});
var G__23277__2 = (function (acc,x){
return f.call(null,acc,x);
});
G__23277 = function(acc,x){
switch(arguments.length){
case 0:
return G__23277__0.call(this);
case 2:
return G__23277__2.call(this,acc,x);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
G__23277.cljs$core$IFn$_invoke$arity$0 = G__23277__0;
G__23277.cljs$core$IFn$_invoke$arity$2 = G__23277__2;
return G__23277;
})()
;
return quantum.core.reducers.reduce_by_PLUS_.call(null,keyfn,f__$1,coll);
});

quantum.core.reducers.reduce_by_PLUS_.cljs$lang$maxFixedArity = 4;
/**
 * Reducers version. Possibly slower than |core/group-by|
 */
quantum.core.reducers.group_by_PLUS_ = (function quantum$core$reducers$group_by_PLUS_(f,coll){
return quantum.core.reducers.fold_PLUS_.call(null,cljs.core.partial.call(null,cljs.core.merge_with,(function (v1,v2){
return quantum.core.reducers.fold_PLUS_.call(null,quantum.core.reducers.concat_PLUS_.call(null,v1,v2));
})),(function (groups,a){
var k = f.call(null,a);
return cljs.core.assoc.call(null,groups,k,cljs.core.conj.call(null,cljs.core.get.call(null,groups,k,cljs.core.PersistentVector.EMPTY),a));
}),coll);
});
/**
 * Remove adjacent duplicate values of (@f x) for each x in @coll.
 * CAVEAT: Requires @coll to be sorted to work correctly.
 */
quantum.core.reducers.distinct_by_PLUS_ = (function quantum$core$reducers$distinct_by_PLUS_(f,coll){
var sentinel = [];
return quantum.core.reducers.remove_PLUS_.call(null,cljs.core.partial.call(null,cljs.core.identical_QMARK_,sentinel),quantum.core.reducers.map_state.call(null,((function (sentinel){
return (function (x,x_SINGLEQUOTE_){
var xf = (function (){var obj_f__22879__auto__ = x;
if(cljs.core.truth_(cljs.core.partial.call(null,cljs.core.identical_QMARK_,sentinel).call(null,obj_f__22879__auto__))){
return cljs.core.identity.call(null,obj_f__22879__auto__);
} else {
return f.call(null,obj_f__22879__auto__);
}
})();
var xf_SINGLEQUOTE_ = (function (){var obj_f__22879__auto__ = x_SINGLEQUOTE_;
if(cljs.core.truth_(cljs.core.partial.call(null,cljs.core.identical_QMARK_,sentinel).call(null,obj_f__22879__auto__))){
return cljs.core.identity.call(null,obj_f__22879__auto__);
} else {
return f.call(null,obj_f__22879__auto__);
}
})();
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [x_SINGLEQUOTE_,((cljs.core._EQ_.call(null,xf,xf_SINGLEQUOTE_))?sentinel:x_SINGLEQUOTE_)], null);
});})(sentinel))
,sentinel,cljs.core.apply.call(null,quantum.core.reducers.concat_PLUS_,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [quantum.core.reducers.fold_pre.call(null,coll),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [sentinel], null)], null))));
});
/**
 * Remove adjacent duplicate values from @coll.
 * CAVEAT: Requires @coll to be sorted to work correctly.
 */
quantum.core.reducers.distinct_PLUS_ = (function quantum$core$reducers$distinct_PLUS_(coll){
return quantum.core.reducers.distinct_by_PLUS_.call(null,cljs.core.identity,quantum.core.reducers.fold_pre.call(null,coll));
});
/**
 * Zipvec. Needs a better implementation.
 * Must start out with pre-catvec'd colls.
 */
quantum.core.reducers.zipvec_STAR_ = (function quantum$core$reducers$zipvec_STAR_(){
var G__23279 = arguments.length;
switch (G__23279) {
case 0:
return quantum.core.reducers.zipvec_STAR_.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return quantum.core.reducers.zipvec_STAR_.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.reducers.zipvec_STAR_.cljs$core$IFn$_invoke$arity$0 = (function (){
return (function (x__22705__auto__){
return quantum.core.reducers.zipvec_STAR_.call(null,x__22705__auto__);
});
});

quantum.core.reducers.zipvec_STAR_.cljs$core$IFn$_invoke$arity$1 = (function (coll){
var ind_n = cljs.core.atom.call(null,(0));
var coll_ct = cljs.core.long$.call(null,(cljs.core.count.call(null,coll) / (2)));
return quantum.core.reducers.folder_PLUS_.call(null,coll,((function (ind_n,coll_ct){
return (function (f1){
return ((function (ind_n,coll_ct){
return (function() {
var G__23281 = null;
var G__23281__0 = (function (){
return f1.call(null);
});
var G__23281__2 = (function (ret,v){
if((cljs.core.count.call(null,ret) < coll_ct)){
return f1.call(null,ret,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [v,null], null));
} else {
cljs.core.swap_BANG_.call(null,ind_n,cljs.core.inc);

return f1.call(null,cljs.core.assoc_BANG_.call(null,ret,(cljs.core.deref.call(null,ind_n) - (1)),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.get.call(null,cljs.core.get.call(null,ret,(cljs.core.deref.call(null,ind_n) - (1))),(0)),v], null)),null);
}
});
var G__23281__3 = (function (ret,k,v){
if((cljs.core.count.call(null,ret) < coll_ct)){
return f1.call(null,ret,k,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [v,null], null));
} else {
cljs.core.swap_BANG_.call(null,ind_n,cljs.core.inc);

return f1.call(null,cljs.core.assoc_BANG_.call(null,ret,(cljs.core.deref.call(null,ind_n) - (1)),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.get.call(null,cljs.core.get.call(null,ret,(cljs.core.deref.call(null,ind_n) - (1))),(0)),v], null)),k,null);
}
});
G__23281 = function(ret,k,v){
switch(arguments.length){
case 0:
return G__23281__0.call(this);
case 2:
return G__23281__2.call(this,ret,k);
case 3:
return G__23281__3.call(this,ret,k,v);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
G__23281.cljs$core$IFn$_invoke$arity$0 = G__23281__0;
G__23281.cljs$core$IFn$_invoke$arity$2 = G__23281__2;
G__23281.cljs$core$IFn$_invoke$arity$3 = G__23281__3;
return G__23281;
})()
;})(ind_n,coll_ct))
});})(ind_n,coll_ct))
);
});

quantum.core.reducers.zipvec_STAR_.cljs$lang$maxFixedArity = 1;
quantum.core.reducers.zipvec_PLUS_ = (function quantum$core$reducers$zipvec_PLUS_(){
var G__23285 = arguments.length;
switch (G__23285) {
case 1:
return quantum.core.reducers.zipvec_PLUS_.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
default:
var argseq__19124__auto__ = (new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(1)),(0)));
return quantum.core.reducers.zipvec_PLUS_.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__19124__auto__);

}
});

quantum.core.reducers.zipvec_PLUS_.cljs$core$IFn$_invoke$arity$1 = (function (vec_0){
return quantum.core.reducers.take_PLUS_.call(null,(cljs.core.count.call(null,vec_0) / (2)),quantum.core.reducers.zipvec_STAR_.call(null,quantum.core.reducers.fold_pre.call(null,vec_0)));
});

quantum.core.reducers.zipvec_PLUS_.cljs$core$IFn$_invoke$arity$variadic = (function (vec_0,vecs){
return quantum.core.reducers.fold_PLUS_.call(null,cljs.core.apply.call(null,cljs.core.map,cljs.core.vector,vec_0,vecs));
});

quantum.core.reducers.zipvec_PLUS_.cljs$lang$applyTo = (function (seq23282){
var G__23283 = cljs.core.first.call(null,seq23282);
var seq23282__$1 = cljs.core.next.call(null,seq23282);
return quantum.core.reducers.zipvec_PLUS_.cljs$core$IFn$_invoke$arity$variadic(G__23283,seq23282__$1);
});

quantum.core.reducers.zipvec_PLUS_.cljs$lang$maxFixedArity = (1);
/**
 * Applies f to each item in coll, returns nil
 */
quantum.core.reducers.each = (function quantum$core$reducers$each(){
var G__23292 = arguments.length;
switch (G__23292) {
case 1:
return quantum.core.reducers.each.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return quantum.core.reducers.each.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.reducers.each.cljs$core$IFn$_invoke$arity$1 = (function (f){
return (function (x__22705__auto__){
return quantum.core.reducers.each.call(null,f,x__22705__auto__);
});
});

quantum.core.reducers.each.cljs$core$IFn$_invoke$arity$2 = (function (f,coll){
return quantum.core.reducers.reduce.call(null,(function (_,x){
f.call(null,x);

return null;
}),null,coll);
});

quantum.core.reducers.each.cljs$lang$maxFixedArity = 2;

//# sourceMappingURL=reducers.js.map?rel=1431625569355