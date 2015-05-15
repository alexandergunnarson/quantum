// Compiled by ClojureScript 0.0-3269 {}
goog.provide('quantum.core.time.core');
goog.require('cljs.core');
goog.require('quantum.core.ns');
goog.require('cljs_time.core');
goog.require('cljs_time.periodic');
goog.require('quantum.core.reducers');
quantum.core.time.core.ymd = (function quantum$core$time$core$ymd(date){
return (new cljs.core.PersistentVector(null,3,(5),cljs.core.PersistentVector.EMPTY_NODE,[cljs_time.core.year.call(null,date),cljs_time.core.month.call(null,date),cljs_time.core.day.call(null,date)],null));
});
quantum.core.time.core.beg_of_day = (function quantum$core$time$core$beg_of_day(){
var G__24856 = arguments.length;
switch (G__24856) {
case 1:
return quantum.core.time.core.beg_of_day.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 3:
return quantum.core.time.core.beg_of_day.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.time.core.beg_of_day.cljs$core$IFn$_invoke$arity$1 = (function (date){
return cljs.core.apply.call(null,quantum.core.time.core.beg_of_day,quantum.core.time.core.ymd.call(null,date));
});

quantum.core.time.core.beg_of_day.cljs$core$IFn$_invoke$arity$3 = (function (y,m,d){
return cljs_time.core.date_time.call(null,y,m,d,(0),(0),(0),(0));
});

quantum.core.time.core.beg_of_day.cljs$lang$maxFixedArity = 3;
quantum.core.time.core.end_of_day = (function quantum$core$time$core$end_of_day(){
var G__24859 = arguments.length;
switch (G__24859) {
case 1:
return quantum.core.time.core.end_of_day.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 3:
return quantum.core.time.core.end_of_day.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.time.core.end_of_day.cljs$core$IFn$_invoke$arity$1 = (function (date){
return cljs.core.apply.call(null,quantum.core.time.core.end_of_day,quantum.core.time.core.ymd.call(null,date));
});

quantum.core.time.core.end_of_day.cljs$core$IFn$_invoke$arity$3 = (function (y,m,d){
return cljs_time.core.date_time.call(null,y,m,d,(23),(59),(59),(999));
});

quantum.core.time.core.end_of_day.cljs$lang$maxFixedArity = 3;
quantum.core.time.core.whole_day = (function quantum$core$time$core$whole_day(){
var G__24862 = arguments.length;
switch (G__24862) {
case 1:
return quantum.core.time.core.whole_day.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 3:
return quantum.core.time.core.whole_day.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.time.core.whole_day.cljs$core$IFn$_invoke$arity$1 = (function (date){
return cljs.core.apply.call(null,quantum.core.time.core.whole_day,quantum.core.time.core.ymd.call(null,date));
});

quantum.core.time.core.whole_day.cljs$core$IFn$_invoke$arity$3 = (function (y,m,d){
return cljs_time.core.interval.call(null,quantum.core.time.core.beg_of_day.call(null,y,m,d),quantum.core.time.core.end_of_day.call(null,y,m,d));
});

quantum.core.time.core.whole_day.cljs$lang$maxFixedArity = 3;
/**
 * Determines if date is on day.
 * Inclusive of intervals.
 */
quantum.core.time.core.on_QMARK_ = (function quantum$core$time$core$on_QMARK_(){
var G__24865 = arguments.length;
switch (G__24865) {
case 2:
return quantum.core.time.core.on_QMARK_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 4:
return quantum.core.time.core.on_QMARK_.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.time.core.on_QMARK_.cljs$core$IFn$_invoke$arity$2 = (function (date,on_date){
return cljs.core.apply.call(null,quantum.core.time.core.on_QMARK_,date,quantum.core.time.core.ymd.call(null,on_date));
});

quantum.core.time.core.on_QMARK_.cljs$core$IFn$_invoke$arity$4 = (function (date,y,m,d){
return cljs_time.core.within_QMARK_.call(null,quantum.core.time.core.whole_day.call(null,y,m,d),date);
});

quantum.core.time.core.on_QMARK_.cljs$lang$maxFixedArity = 4;
quantum.core.time.core.for_days_between = (function quantum$core$time$core$for_days_between(date_a,date_b,f){
var difference_in_days = cljs_time.core.in_days.call(null,cljs_time.core.interval.call(null,date_a,date_b));
var instants_on_beg_of_days = cljs_time.periodic.periodic_seq.call(null,date_a,cljs_time.core.days.call(null,(1)));
return quantum.core.reducers.folder_PLUS_.call(null,cljs.core.take.call(null,(difference_in_days + (1)),instants_on_beg_of_days),((function (difference_in_days,instants_on_beg_of_days){
return (function (f__24869){
return ((function (difference_in_days,instants_on_beg_of_days){
return (function() {
var G__24871 = null;
var G__24871__0 = (function (){
return f__24869.call(null);
});
var G__24871__2 = (function (ret__24870,day){
return f__24869.call(null,ret__24870,f.call(null,day));
});
var G__24871__3 = (function (ret__24870,k__24740__auto__,v__24741__auto__){
var day = quantum.core.data.map.map_entry.call(null,k__24740__auto__,v__24741__auto__);
return f__24869.call(null,ret__24870,f.call(null,day));
});
G__24871 = function(ret__24870,k__24740__auto__,v__24741__auto__){
switch(arguments.length){
case 0:
return G__24871__0.call(this);
case 2:
return G__24871__2.call(this,ret__24870,k__24740__auto__);
case 3:
return G__24871__3.call(this,ret__24870,k__24740__auto__,v__24741__auto__);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
G__24871.cljs$core$IFn$_invoke$arity$0 = G__24871__0;
G__24871.cljs$core$IFn$_invoke$arity$2 = G__24871__2;
G__24871.cljs$core$IFn$_invoke$arity$3 = G__24871__3;
return G__24871;
})()
;})(difference_in_days,instants_on_beg_of_days))
});})(difference_in_days,instants_on_beg_of_days))
);
});

//# sourceMappingURL=core.js.map?rel=1431625571052