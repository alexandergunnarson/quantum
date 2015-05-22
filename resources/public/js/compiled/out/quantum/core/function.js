// Compiled by ClojureScript 0.0-3269 {}
goog.provide('quantum.core.function$');
goog.require('cljs.core');
goog.require('quantum.core.ns');
goog.require('quantum.core.data.map');
goog.require('clojure.walk');
/**
 * Call function `f` with additional arguments.
 */
quantum.core.function$.call = (function quantum$core$function$call(){
var G__19375 = arguments.length;
switch (G__19375) {
case 1:
return quantum.core.function$.call.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return quantum.core.function$.call.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return quantum.core.function$.call.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return quantum.core.function$.call.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
var argseq__19124__auto__ = (new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(4)),(0)));
return quantum.core.function$.call.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),argseq__19124__auto__);

}
});

quantum.core.function$.call.cljs$core$IFn$_invoke$arity$1 = (function (f){
return f.call(null);
});

quantum.core.function$.call.cljs$core$IFn$_invoke$arity$2 = (function (f,x){
return f.call(null,x);
});

quantum.core.function$.call.cljs$core$IFn$_invoke$arity$3 = (function (f,x,y){
return f.call(null,x,y);
});

quantum.core.function$.call.cljs$core$IFn$_invoke$arity$4 = (function (f,x,y,z){
return f.call(null,x,y,z);
});

quantum.core.function$.call.cljs$core$IFn$_invoke$arity$variadic = (function (f,x,y,z,more){
return cljs.core.apply.call(null,f,x,y,z,more);
});

quantum.core.function$.call.cljs$lang$applyTo = (function (seq19369){
var G__19370 = cljs.core.first.call(null,seq19369);
var seq19369__$1 = cljs.core.next.call(null,seq19369);
var G__19371 = cljs.core.first.call(null,seq19369__$1);
var seq19369__$2 = cljs.core.next.call(null,seq19369__$1);
var G__19372 = cljs.core.first.call(null,seq19369__$2);
var seq19369__$3 = cljs.core.next.call(null,seq19369__$2);
var G__19373 = cljs.core.first.call(null,seq19369__$3);
var seq19369__$4 = cljs.core.next.call(null,seq19369__$3);
return quantum.core.function$.call.cljs$core$IFn$_invoke$arity$variadic(G__19370,G__19371,G__19372,G__19373,seq19369__$4);
});

quantum.core.function$.call.cljs$lang$maxFixedArity = (4);
/**
 * Accepts any number of arguments and returns the first.
 */
quantum.core.function$.firsta = (function quantum$core$function$firsta(){
var G__19382 = arguments.length;
switch (G__19382) {
case 1:
return quantum.core.function$.firsta.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return quantum.core.function$.firsta.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return quantum.core.function$.firsta.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
var argseq__19124__auto__ = (new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(3)),(0)));
return quantum.core.function$.firsta.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),argseq__19124__auto__);

}
});

quantum.core.function$.firsta.cljs$core$IFn$_invoke$arity$1 = (function (x){
return x;
});

quantum.core.function$.firsta.cljs$core$IFn$_invoke$arity$2 = (function (x,y){
return x;
});

quantum.core.function$.firsta.cljs$core$IFn$_invoke$arity$3 = (function (x,y,z){
return x;
});

quantum.core.function$.firsta.cljs$core$IFn$_invoke$arity$variadic = (function (x,y,z,more){
return x;
});

quantum.core.function$.firsta.cljs$lang$applyTo = (function (seq19377){
var G__19378 = cljs.core.first.call(null,seq19377);
var seq19377__$1 = cljs.core.next.call(null,seq19377);
var G__19379 = cljs.core.first.call(null,seq19377__$1);
var seq19377__$2 = cljs.core.next.call(null,seq19377__$1);
var G__19380 = cljs.core.first.call(null,seq19377__$2);
var seq19377__$3 = cljs.core.next.call(null,seq19377__$2);
return quantum.core.function$.firsta.cljs$core$IFn$_invoke$arity$variadic(G__19378,G__19379,G__19380,seq19377__$3);
});

quantum.core.function$.firsta.cljs$lang$maxFixedArity = (3);
/**
 * Accepts any number of arguments and returns the second.
 */
quantum.core.function$.seconda = (function quantum$core$function$seconda(){
var G__19389 = arguments.length;
switch (G__19389) {
case 2:
return quantum.core.function$.seconda.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return quantum.core.function$.seconda.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
var argseq__19124__auto__ = (new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(3)),(0)));
return quantum.core.function$.seconda.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),argseq__19124__auto__);

}
});

quantum.core.function$.seconda.cljs$core$IFn$_invoke$arity$2 = (function (x,y){
return y;
});

quantum.core.function$.seconda.cljs$core$IFn$_invoke$arity$3 = (function (x,y,z){
return y;
});

quantum.core.function$.seconda.cljs$core$IFn$_invoke$arity$variadic = (function (x,y,z,more){
return y;
});

quantum.core.function$.seconda.cljs$lang$applyTo = (function (seq19384){
var G__19385 = cljs.core.first.call(null,seq19384);
var seq19384__$1 = cljs.core.next.call(null,seq19384);
var G__19386 = cljs.core.first.call(null,seq19384__$1);
var seq19384__$2 = cljs.core.next.call(null,seq19384__$1);
var G__19387 = cljs.core.first.call(null,seq19384__$2);
var seq19384__$3 = cljs.core.next.call(null,seq19384__$2);
return quantum.core.function$.seconda.cljs$core$IFn$_invoke$arity$variadic(G__19385,G__19386,G__19387,seq19384__$3);
});

quantum.core.function$.seconda.cljs$lang$maxFixedArity = (3);
quantum.core.function$.do_curried = (function quantum$core$function$do_curried(name,doc,meta,args,body){
var cargs = cljs.core.vec.call(null,cljs.core.butlast.call(null,args));
return cljs.core.sequence.call(null,cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core._conj.call(null,cljs.core.List.EMPTY,new cljs.core.Symbol("quantum.core.function","defn","quantum.core.function/defn",1916476930,null)),cljs.core._conj.call(null,cljs.core.List.EMPTY,name),cljs.core._conj.call(null,cljs.core.List.EMPTY,doc),cljs.core._conj.call(null,cljs.core.List.EMPTY,meta),cljs.core._conj.call(null,cljs.core.List.EMPTY,cljs.core.sequence.call(null,cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core._conj.call(null,cljs.core.List.EMPTY,cargs),cljs.core._conj.call(null,cljs.core.List.EMPTY,cljs.core.sequence.call(null,cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core._conj.call(null,cljs.core.List.EMPTY,new cljs.core.Symbol("quantum.core.function","fn","quantum.core.function/fn",-1770437089,null)),cljs.core._conj.call(null,cljs.core.List.EMPTY,cljs.core.vec.call(null,cljs.core.sequence.call(null,cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core._conj.call(null,cljs.core.List.EMPTY,new cljs.core.Symbol(null,"x__19396__auto__","x__19396__auto__",552225976,null))))))),cljs.core._conj.call(null,cljs.core.List.EMPTY,cljs.core.sequence.call(null,cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core._conj.call(null,cljs.core.List.EMPTY,name),cargs,cljs.core._conj.call(null,cljs.core.List.EMPTY,new cljs.core.Symbol(null,"x__19396__auto__","x__19396__auto__",552225976,null)))))))))))))),cljs.core._conj.call(null,cljs.core.List.EMPTY,cljs.core.sequence.call(null,cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core._conj.call(null,cljs.core.List.EMPTY,args),body)))))));
});
quantum.core.function$.zeroid = (function quantum$core$function$zeroid(func,base){
return (function() {
var G__19397 = null;
var G__19397__0 = (function (){
return base;
});
var G__19397__2 = (function (arg1,arg2){
return func.call(null,arg1,arg2);
});
var G__19397__3 = (function (arg1,arg2,arg3){
return func.call(null,func.call(null,arg1,arg2),arg3);
});
var G__19397__4 = (function() { 
var G__19398__delegate = function (arg1,arg2,arg3,args){
return cljs.core.apply.call(null,func,func.call(null,func.call(null,arg1,arg2),arg3),args);
};
var G__19398 = function (arg1,arg2,arg3,var_args){
var args = null;
if (arguments.length > 3) {
var G__19399__i = 0, G__19399__a = new Array(arguments.length -  3);
while (G__19399__i < G__19399__a.length) {G__19399__a[G__19399__i] = arguments[G__19399__i + 3]; ++G__19399__i;}
  args = new cljs.core.IndexedSeq(G__19399__a,0);
} 
return G__19398__delegate.call(this,arg1,arg2,arg3,args);};
G__19398.cljs$lang$maxFixedArity = 3;
G__19398.cljs$lang$applyTo = (function (arglist__19400){
var arg1 = cljs.core.first(arglist__19400);
arglist__19400 = cljs.core.next(arglist__19400);
var arg2 = cljs.core.first(arglist__19400);
arglist__19400 = cljs.core.next(arglist__19400);
var arg3 = cljs.core.first(arglist__19400);
var args = cljs.core.rest(arglist__19400);
return G__19398__delegate(arg1,arg2,arg3,args);
});
G__19398.cljs$core$IFn$_invoke$arity$variadic = G__19398__delegate;
return G__19398;
})()
;
G__19397 = function(arg1,arg2,arg3,var_args){
var args = var_args;
switch(arguments.length){
case 0:
return G__19397__0.call(this);
case 2:
return G__19397__2.call(this,arg1,arg2);
case 3:
return G__19397__3.call(this,arg1,arg2,arg3);
default:
var G__19401 = null;
if (arguments.length > 3) {
var G__19402__i = 0, G__19402__a = new Array(arguments.length -  3);
while (G__19402__i < G__19402__a.length) {G__19402__a[G__19402__i] = arguments[G__19402__i + 3]; ++G__19402__i;}
G__19401 = new cljs.core.IndexedSeq(G__19402__a,0);
}
return G__19397__4.cljs$core$IFn$_invoke$arity$variadic(arg1,arg2,arg3, G__19401);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
G__19397.cljs$lang$maxFixedArity = 3;
G__19397.cljs$lang$applyTo = G__19397__4.cljs$lang$applyTo;
G__19397.cljs$core$IFn$_invoke$arity$0 = G__19397__0;
G__19397.cljs$core$IFn$_invoke$arity$2 = G__19397__2;
G__19397.cljs$core$IFn$_invoke$arity$3 = G__19397__3;
G__19397.cljs$core$IFn$_invoke$arity$variadic = G__19397__4.cljs$core$IFn$_invoke$arity$variadic;
return G__19397;
})()
});
/**
 * Builds a combining fn out of the supplied operator and identity
 * constructor. op must be associative and ctor called with no args
 * must return an identity value for it.
 */
quantum.core.function$.monoid = (function quantum$core$function$monoid(op,ctor){
return (function() {
var quantum$core$function$monoid_$_mon = null;
var quantum$core$function$monoid_$_mon__0 = (function (){
return ctor.call(null);
});
var quantum$core$function$monoid_$_mon__2 = (function (a,b){
return op.call(null,a,b);
});
quantum$core$function$monoid_$_mon = function(a,b){
switch(arguments.length){
case 0:
return quantum$core$function$monoid_$_mon__0.call(this);
case 2:
return quantum$core$function$monoid_$_mon__2.call(this,a,b);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
quantum$core$function$monoid_$_mon.cljs$core$IFn$_invoke$arity$0 = quantum$core$function$monoid_$_mon__0;
quantum$core$function$monoid_$_mon.cljs$core$IFn$_invoke$arity$2 = quantum$core$function$monoid_$_mon__2;
return quantum$core$function$monoid_$_mon;
})()
});
quantum.core.function$.compr = (function quantum$core$function$compr(){
var argseq__19113__auto__ = ((((0) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(0)),(0))):null);
return quantum.core.function$.compr.cljs$core$IFn$_invoke$arity$variadic(argseq__19113__auto__);
});

quantum.core.function$.compr.cljs$core$IFn$_invoke$arity$variadic = (function (args){
return cljs.core.apply.call(null,cljs.core.comp,cljs.core.reverse.call(null,args));
});

quantum.core.function$.compr.cljs$lang$maxFixedArity = (0);

quantum.core.function$.compr.cljs$lang$applyTo = (function (seq19403){
return quantum.core.function$.compr.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq19403));
});
/**
 * FOR SOME REASON '(fn* + 3)' [and the like] FAILS WITH THE FOLLOWING EXCEPTION:
 * 'CompilerException java.lang.ClassCastException: java.lang.Long cannot be cast to clojure.lang.ISeq'
 * 
 * Likewise, simply copying and pasting the code for |partial| from clojure.core doesn't work either...
 */
quantum.core.function$.fn_STAR_ = (function quantum$core$function$fn_STAR_(){
var argseq__19113__auto__ = ((((0) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(0)),(0))):null);
return quantum.core.function$.fn_STAR_.cljs$core$IFn$_invoke$arity$variadic(argseq__19113__auto__);
});

quantum.core.function$.fn_STAR_.cljs$core$IFn$_invoke$arity$variadic = (function (args){
return cljs.core.apply.call(null,cljs.core.partial,args);
});

quantum.core.function$.fn_STAR_.cljs$lang$maxFixedArity = (0);

quantum.core.function$.fn_STAR_.cljs$lang$applyTo = (function (seq19404){
return quantum.core.function$.fn_STAR_.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq19404));
});
quantum.core.function$.f_STAR_n = (function quantum$core$function$f_STAR_n(){
var argseq__19113__auto__ = ((((1) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(1)),(0))):null);
return quantum.core.function$.f_STAR_n.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__19113__auto__);
});

quantum.core.function$.f_STAR_n.cljs$core$IFn$_invoke$arity$variadic = (function (func,args){
return (function (arg_inner){
return cljs.core.apply.call(null,func,arg_inner,args);
});
});

quantum.core.function$.f_STAR_n.cljs$lang$maxFixedArity = (1);

quantum.core.function$.f_STAR_n.cljs$lang$applyTo = (function (seq19405){
var G__19406 = cljs.core.first.call(null,seq19405);
var seq19405__$1 = cljs.core.next.call(null,seq19405);
return quantum.core.function$.f_STAR_n.cljs$core$IFn$_invoke$arity$variadic(G__19406,seq19405__$1);
});
quantum.core.function$.f_STAR__STAR_n = (function quantum$core$function$f_STAR__STAR_n(){
var argseq__19113__auto__ = ((((1) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(1)),(0))):null);
return quantum.core.function$.f_STAR__STAR_n.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__19113__auto__);
});

quantum.core.function$.f_STAR__STAR_n.cljs$core$IFn$_invoke$arity$variadic = (function (func,args){
return (function() { 
var G__19409__delegate = function (args_inner){
return cljs.core.apply.call(null,func,cljs.core.concat.call(null,args_inner,args));
};
var G__19409 = function (var_args){
var args_inner = null;
if (arguments.length > 0) {
var G__19410__i = 0, G__19410__a = new Array(arguments.length -  0);
while (G__19410__i < G__19410__a.length) {G__19410__a[G__19410__i] = arguments[G__19410__i + 0]; ++G__19410__i;}
  args_inner = new cljs.core.IndexedSeq(G__19410__a,0);
} 
return G__19409__delegate.call(this,args_inner);};
G__19409.cljs$lang$maxFixedArity = 0;
G__19409.cljs$lang$applyTo = (function (arglist__19411){
var args_inner = cljs.core.seq(arglist__19411);
return G__19409__delegate(args_inner);
});
G__19409.cljs$core$IFn$_invoke$arity$variadic = G__19409__delegate;
return G__19409;
})()
;
});

quantum.core.function$.f_STAR__STAR_n.cljs$lang$maxFixedArity = (1);

quantum.core.function$.f_STAR__STAR_n.cljs$lang$applyTo = (function (seq19407){
var G__19408 = cljs.core.first.call(null,seq19407);
var seq19407__$1 = cljs.core.next.call(null,seq19407);
return quantum.core.function$.f_STAR__STAR_n.cljs$core$IFn$_invoke$arity$variadic(G__19408,seq19407__$1);
});
quantum.core.function$._STAR_fn = (function quantum$core$function$_STAR_fn(){
var argseq__19113__auto__ = ((((0) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(0)),(0))):null);
return quantum.core.function$._STAR_fn.cljs$core$IFn$_invoke$arity$variadic(argseq__19113__auto__);
});

quantum.core.function$._STAR_fn.cljs$core$IFn$_invoke$arity$variadic = (function (args){
return quantum.core.function$.f_STAR_n.call(null,cljs.core.apply,args);
});

quantum.core.function$._STAR_fn.cljs$lang$maxFixedArity = (0);

quantum.core.function$._STAR_fn.cljs$lang$applyTo = (function (seq19412){
return quantum.core.function$._STAR_fn.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq19412));
});
quantum.core.function$.fn_bi = (function quantum$core$function$fn_bi(arg){
return (function (p1__19413_SHARP_,p2__19414_SHARP_){
return arg.call(null,p1__19413_SHARP_,p2__19414_SHARP_);
});
});
quantum.core.function$.unary = (function quantum$core$function$unary(pred){
return cljs.core.partial.call(null,quantum.core.function$.f_STAR_n,pred);
});
quantum.core.function$.call_fn_STAR_ = (function quantum$core$function$call_fn_STAR_(){
var argseq__19113__auto__ = ((((0) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(0)),(0))):null);
return quantum.core.function$.call_fn_STAR_.cljs$core$IFn$_invoke$arity$variadic(argseq__19113__auto__);
});

quantum.core.function$.call_fn_STAR_.cljs$core$IFn$_invoke$arity$variadic = (function (args){
return cljs.core.apply.call(null,cljs.core.partial,cljs.core.butlast.call(null,args)).call(null,cljs.core.last.call(null,args));
});

quantum.core.function$.call_fn_STAR_.cljs$lang$maxFixedArity = (0);

quantum.core.function$.call_fn_STAR_.cljs$lang$applyTo = (function (seq19418){
return quantum.core.function$.call_fn_STAR_.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq19418));
});
quantum.core.function$.call_f_STAR_n = (function quantum$core$function$call_f_STAR_n(){
var argseq__19113__auto__ = ((((0) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(0)),(0))):null);
return quantum.core.function$.call_f_STAR_n.cljs$core$IFn$_invoke$arity$variadic(argseq__19113__auto__);
});

quantum.core.function$.call_f_STAR_n.cljs$core$IFn$_invoke$arity$variadic = (function (args){
return cljs.core.apply.call(null,quantum.core.function$.f_STAR_n,cljs.core.butlast.call(null,args)).call(null,cljs.core.last.call(null,args));
});

quantum.core.function$.call_f_STAR_n.cljs$lang$maxFixedArity = (0);

quantum.core.function$.call_f_STAR_n.cljs$lang$applyTo = (function (seq19419){
return quantum.core.function$.call_f_STAR_n.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq19419));
});
quantum.core.function$.call__GT_ = (function quantum$core$function$call__GT_(){
var argseq__19113__auto__ = ((((1) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(1)),(0))):null);
return quantum.core.function$.call__GT_.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__19113__auto__);
});

quantum.core.function$.call__GT_.cljs$core$IFn$_invoke$arity$variadic = (function (arg,p__19422){
var vec__19423 = p__19422;
var func = cljs.core.nth.call(null,vec__19423,(0),null);
var args = cljs.core.nthnext.call(null,vec__19423,(1));
return cljs.core.apply.call(null,func,args).call(null,arg);
});

quantum.core.function$.call__GT_.cljs$lang$maxFixedArity = (1);

quantum.core.function$.call__GT_.cljs$lang$applyTo = (function (seq19420){
var G__19421 = cljs.core.first.call(null,seq19420);
var seq19420__$1 = cljs.core.next.call(null,seq19420);
return quantum.core.function$.call__GT_.cljs$core$IFn$_invoke$arity$variadic(G__19421,seq19420__$1);
});
quantum.core.function$.call__GT__GT_ = (function quantum$core$function$call__GT__GT_(){
var argseq__19113__auto__ = ((((0) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(0)),(0))):null);
return quantum.core.function$.call__GT__GT_.cljs$core$IFn$_invoke$arity$variadic(argseq__19113__auto__);
});

quantum.core.function$.call__GT__GT_.cljs$core$IFn$_invoke$arity$variadic = (function (p__19425){
var vec__19426 = p__19425;
var func = cljs.core.nth.call(null,vec__19426,(0),null);
var args = cljs.core.nthnext.call(null,vec__19426,(1));
return cljs.core.apply.call(null,func,cljs.core.butlast.call(null,args)).call(null,cljs.core.last.call(null,args));
});

quantum.core.function$.call__GT__GT_.cljs$lang$maxFixedArity = (0);

quantum.core.function$.call__GT__GT_.cljs$lang$applyTo = (function (seq19424){
return quantum.core.function$.call__GT__GT_.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq19424));
});
quantum.core.function$.juxtm_STAR_ = (function quantum$core$function$juxtm_STAR_(map_type,args){
if(cljs.core.even_QMARK_.call(null,cljs.core.count.call(null,args))){
return (function (arg){
return cljs.core.apply.call(null,map_type,cljs.core.apply.call(null,cljs.core.juxt,args).call(null,arg));
});
} else {
throw (new quantum.core.ns.IllegalArgumentException("juxtm requires an even number of arguments",null,null,null));
}
});
/**
 * Like /juxt/, but applies a hash-map instead of a vector.
 * Requires an even number of arguments.
 */
quantum.core.function$.juxtm = (function quantum$core$function$juxtm(){
var argseq__19113__auto__ = ((((0) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(0)),(0))):null);
return quantum.core.function$.juxtm.cljs$core$IFn$_invoke$arity$variadic(argseq__19113__auto__);
});

quantum.core.function$.juxtm.cljs$core$IFn$_invoke$arity$variadic = (function (args){
return quantum.core.function$.juxtm_STAR_.call(null,cljs.core.hash_map,args);
});

quantum.core.function$.juxtm.cljs$lang$maxFixedArity = (0);

quantum.core.function$.juxtm.cljs$lang$applyTo = (function (seq19427){
return quantum.core.function$.juxtm.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq19427));
});
/**
 * Like /juxt/, but applies a sorted-map+ instead of a vector.
 * Requires an even number of arguments.
 */
quantum.core.function$.juxt_sm = (function quantum$core$function$juxt_sm(){
var argseq__19113__auto__ = ((((0) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(0)),(0))):null);
return quantum.core.function$.juxt_sm.cljs$core$IFn$_invoke$arity$variadic(argseq__19113__auto__);
});

quantum.core.function$.juxt_sm.cljs$core$IFn$_invoke$arity$variadic = (function (args){
return quantum.core.function$.juxtm_STAR_.call(null,quantum.core.data.map.sorted_map_PLUS_,args);
});

quantum.core.function$.juxt_sm.cljs$lang$maxFixedArity = (0);

quantum.core.function$.juxt_sm.cljs$lang$applyTo = (function (seq19428){
return quantum.core.function$.juxt_sm.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq19428));
});
quantum.core.function$.with_pr__GT__GT_ = (function quantum$core$function$with_pr__GT__GT_(obj){
cljs.core.println.call(null,obj);

return obj;
});
quantum.core.function$.with_msg__GT__GT_ = (function quantum$core$function$with_msg__GT__GT_(msg,obj){
cljs.core.println.call(null,msg);

return obj;
});
quantum.core.function$.with__GT__GT_ = (function quantum$core$function$with__GT__GT_(expr,obj){

return obj;
});
quantum.core.function$.withf__GT__GT_ = (function quantum$core$function$withf__GT__GT_(f,obj){
f.call(null,obj);

return obj;
});
quantum.core.function$.withf = (function quantum$core$function$withf(obj,f){
f.call(null,obj);

return obj;
});
quantum.core.function$.withfs = (function quantum$core$function$withfs(){
var argseq__19113__auto__ = ((((1) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(1)),(0))):null);
return quantum.core.function$.withfs.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__19113__auto__);
});

quantum.core.function$.withfs.cljs$core$IFn$_invoke$arity$variadic = (function (obj,fs){
var seq__19431_19435 = cljs.core.seq.call(null,fs);
var chunk__19432_19436 = null;
var count__19433_19437 = (0);
var i__19434_19438 = (0);
while(true){
if((i__19434_19438 < count__19433_19437)){
var f_19439 = cljs.core._nth.call(null,chunk__19432_19436,i__19434_19438);
f_19439.call(null,obj);

var G__19440 = seq__19431_19435;
var G__19441 = chunk__19432_19436;
var G__19442 = count__19433_19437;
var G__19443 = (i__19434_19438 + (1));
seq__19431_19435 = G__19440;
chunk__19432_19436 = G__19441;
count__19433_19437 = G__19442;
i__19434_19438 = G__19443;
continue;
} else {
var temp__4423__auto___19444 = cljs.core.seq.call(null,seq__19431_19435);
if(temp__4423__auto___19444){
var seq__19431_19445__$1 = temp__4423__auto___19444;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__19431_19445__$1)){
var c__18858__auto___19446 = cljs.core.chunk_first.call(null,seq__19431_19445__$1);
var G__19447 = cljs.core.chunk_rest.call(null,seq__19431_19445__$1);
var G__19448 = c__18858__auto___19446;
var G__19449 = cljs.core.count.call(null,c__18858__auto___19446);
var G__19450 = (0);
seq__19431_19435 = G__19447;
chunk__19432_19436 = G__19448;
count__19433_19437 = G__19449;
i__19434_19438 = G__19450;
continue;
} else {
var f_19451 = cljs.core.first.call(null,seq__19431_19445__$1);
f_19451.call(null,obj);

var G__19452 = cljs.core.next.call(null,seq__19431_19445__$1);
var G__19453 = null;
var G__19454 = (0);
var G__19455 = (0);
seq__19431_19435 = G__19452;
chunk__19432_19436 = G__19453;
count__19433_19437 = G__19454;
i__19434_19438 = G__19455;
continue;
}
} else {
}
}
break;
}

return obj;
});

quantum.core.function$.withfs.cljs$lang$maxFixedArity = (1);

quantum.core.function$.withfs.cljs$lang$applyTo = (function (seq19429){
var G__19430 = cljs.core.first.call(null,seq19429);
var seq19429__$1 = cljs.core.next.call(null,seq19429);
return quantum.core.function$.withfs.cljs$core$IFn$_invoke$arity$variadic(G__19430,seq19429__$1);
});
quantum.core.function$.do_rfn = (function quantum$core$function$do_rfn(f1,k,fkv){
return cljs.core.sequence.call(null,cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core._conj.call(null,cljs.core.List.EMPTY,new cljs.core.Symbol("quantum.core.function","fn","quantum.core.function/fn",-1770437089,null)),cljs.core._conj.call(null,cljs.core.List.EMPTY,cljs.core.sequence.call(null,cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core._conj.call(null,cljs.core.List.EMPTY,cljs.core.vec.call(null,cljs.core.sequence.call(null,cljs.core.seq.call(null,cljs.core.concat.call(null))))),cljs.core._conj.call(null,cljs.core.List.EMPTY,cljs.core.sequence.call(null,cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core._conj.call(null,cljs.core.List.EMPTY,f1))))))))),cljs.core._conj.call(null,cljs.core.List.EMPTY,clojure.walk.postwalk.call(null,(function (p1__19456_SHARP_){
if(cljs.core.sequential_QMARK_.call(null,p1__19456_SHARP_)){
return ((cljs.core.vector_QMARK_.call(null,p1__19456_SHARP_))?cljs.core.vec:cljs.core.identity).call(null,cljs.core.remove.call(null,cljs.core.PersistentHashSet.fromArray([k], true),p1__19456_SHARP_));
} else {
return p1__19456_SHARP_;
}
}),fkv)),cljs.core._conj.call(null,cljs.core.List.EMPTY,fkv))));
});

//# sourceMappingURL=function.js.map?rel=1431986895546