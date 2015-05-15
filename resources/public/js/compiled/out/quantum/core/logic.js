// Compiled by ClojureScript 0.0-3269 {}
goog.provide('quantum.core.logic');
goog.require('cljs.core');
goog.require('quantum.core.ns');
goog.require('quantum.core.function$');
quantum.core.logic.nnil_QMARK_ = cljs.core.comp.call(null,cljs.core.not,cljs.core.nil_QMARK_);
quantum.core.logic.nempty_QMARK_ = cljs.core.comp.call(null,cljs.core.not,cljs.core.empty_QMARK_);
quantum.core.logic.nseq_QMARK_ = cljs.core.comp.call(null,cljs.core.not,cljs.core.seq_QMARK_);
quantum.core.logic.iff = (function quantum$core$logic$iff(pred,const$,else$){
if(cljs.core.truth_(pred.call(null,const$))){
return const$;
} else {
return else$;
}
});
quantum.core.logic.iffn = (function quantum$core$logic$iffn(pred,const$,else_fn){
if(cljs.core.truth_(pred.call(null,const$))){
return const$;
} else {
return else_fn.call(null,const$);
}
});
quantum.core.logic.eq_QMARK_ = quantum.core.function$.unary.call(null,cljs.core._EQ_);
quantum.core.logic.fn_EQ_ = quantum.core.logic.eq_QMARK_;
quantum.core.logic.fn_eq_QMARK_ = quantum.core.logic.eq_QMARK_;
quantum.core.logic.neq_QMARK_ = quantum.core.function$.unary.call(null,cljs.core.not_EQ_);
quantum.core.logic.fn_neq_QMARK_ = quantum.core.logic.neq_QMARK_;
quantum.core.logic.any_QMARK_ = cljs.core.some;
quantum.core.logic.apply_and = (function quantum$core$logic$apply_and(arg_list){
return cljs.core.every_QMARK_.call(null,cljs.core.identity,arg_list);
});
quantum.core.logic.apply_or = (function quantum$core$logic$apply_or(arg_list){
return quantum.core.logic.any_QMARK_.call(null,cljs.core.identity,arg_list);
});
quantum.core.logic.dor = (function quantum$core$logic$dor(){
var argseq__19113__auto__ = ((((0) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(0)),(0))):null);
return quantum.core.logic.dor.cljs$core$IFn$_invoke$arity$variadic(argseq__19113__auto__);
});

quantum.core.logic.dor.cljs$core$IFn$_invoke$arity$variadic = (function (args){
var and__18061__auto__ = quantum.core.logic.apply_or.call(null,args);
if(cljs.core.truth_(and__18061__auto__)){
return cljs.core.not.call(null,quantum.core.logic.apply_and.call(null,args));
} else {
return and__18061__auto__;
}
});

quantum.core.logic.dor.cljs$lang$maxFixedArity = (0);

quantum.core.logic.dor.cljs$lang$applyTo = (function (seq22785){
return quantum.core.logic.dor.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq22785));
});
quantum.core.logic.pred_or = (function quantum$core$logic$pred_or(pred,obj,args){
return quantum.core.logic.apply_or.call(null,cljs.core.map.call(null,pred.call(null,obj),args));
});
quantum.core.logic.pred_and = (function quantum$core$logic$pred_and(pred,obj,args){
return quantum.core.logic.apply_and.call(null,cljs.core.map.call(null,pred.call(null,obj),args));
});
quantum.core.logic.fn_and = cljs.core.every_pred;
quantum.core.logic.fn_or = cljs.core.some_fn;
quantum.core.logic.fn_not = cljs.core.complement;
quantum.core.logic.splice_or = (function quantum$core$logic$splice_or(){
var argseq__19113__auto__ = ((((2) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(2)),(0))):null);
return quantum.core.logic.splice_or.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),argseq__19113__auto__);
});

quantum.core.logic.splice_or.cljs$core$IFn$_invoke$arity$variadic = (function (obj,compare_fn,coll){
return quantum.core.logic.any_QMARK_.call(null,cljs.core.partial.call(null,compare_fn,obj),coll);
});

quantum.core.logic.splice_or.cljs$lang$maxFixedArity = (2);

quantum.core.logic.splice_or.cljs$lang$applyTo = (function (seq22786){
var G__22787 = cljs.core.first.call(null,seq22786);
var seq22786__$1 = cljs.core.next.call(null,seq22786);
var G__22788 = cljs.core.first.call(null,seq22786__$1);
var seq22786__$2 = cljs.core.next.call(null,seq22786__$1);
return quantum.core.logic.splice_or.cljs$core$IFn$_invoke$arity$variadic(G__22787,G__22788,seq22786__$2);
});
quantum.core.logic.splice_and = (function quantum$core$logic$splice_and(){
var argseq__19113__auto__ = ((((2) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(2)),(0))):null);
return quantum.core.logic.splice_and.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),argseq__19113__auto__);
});

quantum.core.logic.splice_and.cljs$core$IFn$_invoke$arity$variadic = (function (obj,compare_fn,coll){
return cljs.core.every_QMARK_.call(null,cljs.core.partial.call(null,compare_fn,obj),coll);
});

quantum.core.logic.splice_and.cljs$lang$maxFixedArity = (2);

quantum.core.logic.splice_and.cljs$lang$applyTo = (function (seq22789){
var G__22790 = cljs.core.first.call(null,seq22789);
var seq22789__$1 = cljs.core.next.call(null,seq22789);
var G__22791 = cljs.core.first.call(null,seq22789__$1);
var seq22789__$2 = cljs.core.next.call(null,seq22789__$1);
return quantum.core.logic.splice_and.cljs$core$IFn$_invoke$arity$variadic(G__22790,G__22791,seq22789__$2);
});
quantum.core.logic.fn_pred_or = (function quantum$core$logic$fn_pred_or(pred_fn,args){
return cljs.core.apply.call(null,quantum.core.logic.fn_or,cljs.core.map.call(null,pred_fn,args));
});
quantum.core.logic.fn_pred_and = (function quantum$core$logic$fn_pred_and(pred_fn,args){
return cljs.core.apply.call(null,quantum.core.logic.fn_and,cljs.core.map.call(null,pred_fn,args));
});
quantum.core.logic.coll_or = (function quantum$core$logic$coll_or(){
var argseq__19113__auto__ = ((((0) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(0)),(0))):null);
return quantum.core.logic.coll_or.cljs$core$IFn$_invoke$arity$variadic(argseq__19113__auto__);
});

quantum.core.logic.coll_or.cljs$core$IFn$_invoke$arity$variadic = (function (elems){
return (function (bin_pred,obj){
return quantum.core.logic.fn_pred_or.call(null,quantum.core.function$.unary.call(null,bin_pred),elems).call(null,obj);
});
});

quantum.core.logic.coll_or.cljs$lang$maxFixedArity = (0);

quantum.core.logic.coll_or.cljs$lang$applyTo = (function (seq22792){
return quantum.core.logic.coll_or.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq22792));
});
quantum.core.logic.coll_and = (function quantum$core$logic$coll_and(){
var argseq__19113__auto__ = ((((0) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(0)),(0))):null);
return quantum.core.logic.coll_and.cljs$core$IFn$_invoke$arity$variadic(argseq__19113__auto__);
});

quantum.core.logic.coll_and.cljs$core$IFn$_invoke$arity$variadic = (function (elems){
return (function (bin_pred,obj){
return quantum.core.logic.fn_pred_and.call(null,quantum.core.function$.unary.call(null,bin_pred),elems).call(null,obj);
});
});

quantum.core.logic.coll_and.cljs$lang$maxFixedArity = (0);

quantum.core.logic.coll_and.cljs$lang$applyTo = (function (seq22793){
return quantum.core.logic.coll_and.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq22793));
});
quantum.core.logic.empty_PLUS__QMARK_ = quantum.core.logic.fn_or.call(null,quantum.core.logic.nseq_QMARK_,cljs.core.empty_QMARK_);
quantum.core.logic.bool = (function quantum$core$logic$bool(v){
if(cljs.core._EQ_.call(null,v,(0))){
return false;
} else {
if(cljs.core._EQ_.call(null,v,(1))){
return true;
} else {
throw (new quantum.core.ns.IllegalArgumentException([cljs.core.str("Value not booleanizable: "),cljs.core.str(v)].join(''),null,null,null));

}
}
});
/**
 * Reverse comparator.
 */
quantum.core.logic.rcompare = (function quantum$core$logic$rcompare(x,y){
return cljs.core.compare.call(null,y,x);
});
quantum.core.logic.is_QMARK_ = (function quantum$core$logic$is_QMARK_(p1__22804_SHARP_,p2__22805_SHARP_){
return p1__22804_SHARP_.call(null,p2__22805_SHARP_);
});

//# sourceMappingURL=logic.js.map?rel=1431625568097