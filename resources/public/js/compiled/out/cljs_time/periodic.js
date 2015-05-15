// Compiled by ClojureScript 0.0-3269 {}
goog.provide('cljs_time.periodic');
goog.require('cljs.core');
goog.require('cljs_time.core');
goog.require('cljs_time.internal.core');
/**
 * Returns a sequence of date-time values growing over specific period.
 * The 2 argument function takes as input the starting value and the growing value,
 * returning a lazy infinite sequence.
 * The 3 argument function takes as input the starting value, the upper bound value,
 * and the growing value, return a lazy sequence.
 */
cljs_time.periodic.periodic_seq = (function cljs_time$periodic$periodic_seq(){
var G__19946 = arguments.length;
switch (G__19946) {
case 2:
return cljs_time.periodic.periodic_seq.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return cljs_time.periodic.periodic_seq.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

cljs_time.periodic.periodic_seq.cljs$core$IFn$_invoke$arity$2 = (function (start,period_like){
var period = cljs_time.core.__GT_period.call(null,period_like);
return cljs.core.map.call(null,((function (period){
return (function (i){
return cljs_time.core.plus.call(null,start,cljs_time.internal.core.multiplied_by.call(null,period,i));
});})(period))
,cljs.core.iterate.call(null,cljs.core.inc,(0)));
});

cljs_time.periodic.periodic_seq.cljs$core$IFn$_invoke$arity$3 = (function (start,end,period_like){
var period = cljs_time.core.__GT_period.call(null,period_like);
return cljs.core.take_while.call(null,((function (period){
return (function (next){
return cljs_time.core.before_QMARK_.call(null,next,end);
});})(period))
,cljs_time.periodic.periodic_seq.call(null,start,period_like));
});

cljs_time.periodic.periodic_seq.cljs$lang$maxFixedArity = 3;

//# sourceMappingURL=periodic.js.map?rel=1431620926261