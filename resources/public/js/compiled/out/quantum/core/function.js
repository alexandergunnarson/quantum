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
var G__19374 = arguments.length;
switch (G__19374) {
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

quantum.core.function$.call.cljs$lang$applyTo = (function (seq19368){
var G__19369 = cljs.core.first.call(null,seq19368);
var seq19368__$1 = cljs.core.next.call(null,seq19368);
var G__19370 = cljs.core.first.call(null,seq19368__$1);
var seq19368__$2 = cljs.core.next.call(null,seq19368__$1);
var G__19371 = cljs.core.first.call(null,seq19368__$2);
var seq19368__$3 = cljs.core.next.call(null,seq19368__$2);
var G__19372 = cljs.core.first.call(null,seq19368__$3);
var seq19368__$4 = cljs.core.next.call(null,seq19368__$3);
return quantum.core.function$.call.cljs$core$IFn$_invoke$arity$variadic(G__19369,G__19370,G__19371,G__19372,seq19368__$4);
});

quantum.core.function$.call.cljs$lang$maxFixedArity = (4);
/**
 * Accepts any number of arguments and returns the first.
 */
quantum.core.function$.firsta = (function quantum$core$function$firsta(){
var G__19381 = arguments.length;
switch (G__19381) {
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

quantum.core.function$.firsta.cljs$lang$applyTo = (function (seq19376){
var G__19377 = cljs.core.first.call(null,seq19376);
var seq19376__$1 = cljs.core.next.call(null,seq19376);
var G__19378 = cljs.core.first.call(null,seq19376__$1);
var seq19376__$2 = cljs.core.next.call(null,seq19376__$1);
var G__19379 = cljs.core.first.call(null,seq19376__$2);
var seq19376__$3 = cljs.core.next.call(null,seq19376__$2);
return quantum.core.function$.firsta.cljs$core$IFn$_invoke$arity$variadic(G__19377,G__19378,G__19379,seq19376__$3);
});

quantum.core.function$.firsta.cljs$lang$maxFixedArity = (3);
/**
 * Accepts any number of arguments and returns the second.
 */
quantum.core.function$.seconda = (function quantum$core$function$seconda(){
var G__19388 = arguments.length;
switch (G__19388) {
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

quantum.core.function$.seconda.cljs$lang$applyTo = (function (seq19383){
var G__19384 = cljs.core.first.call(null,seq19383);
var seq19383__$1 = cljs.core.next.call(null,seq19383);
var G__19385 = cljs.core.first.call(null,seq19383__$1);
var seq19383__$2 = cljs.core.next.call(null,seq19383__$1);
var G__19386 = cljs.core.first.call(null,seq19383__$2);
var seq19383__$3 = cljs.core.next.call(null,seq19383__$2);
return quantum.core.function$.seconda.cljs$core$IFn$_invoke$arity$variadic(G__19384,G__19385,G__19386,seq19383__$3);
});

quantum.core.function$.seconda.cljs$lang$maxFixedArity = (3);
quantum.core.function$.do_curried = (function quantum$core$function$do_curried(name,doc,meta,args,body){
var cargs = cljs.core.vec.call(null,cljs.core.butlast.call(null,args));
return cljs.core.sequence.call(null,cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core._conj.call(null,cljs.core.List.EMPTY,new cljs.core.Symbol("quantum.core.function","defn","quantum.core.function/defn",1916476930,null)),cljs.core._conj.call(null,cljs.core.List.EMPTY,name),cljs.core._conj.call(null,cljs.core.List.EMPTY,doc),cljs.core._conj.call(null,cljs.core.List.EMPTY,meta),cljs.core._conj.call(null,cljs.core.List.EMPTY,cljs.core.sequence.call(null,cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core._conj.call(null,cljs.core.List.EMPTY,cargs),cljs.core._conj.call(null,cljs.core.List.EMPTY,cljs.core.sequence.call(null,cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core._conj.call(null,cljs.core.List.EMPTY,new cljs.core.Symbol("quantum.core.function","fn","quantum.core.function/fn",-1770437089,null)),cljs.core._conj.call(null,cljs.core.List.EMPTY,cljs.core.vec.call(null,cljs.core.sequence.call(null,cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core._conj.call(null,cljs.core.List.EMPTY,new cljs.core.Symbol(null,"x__19395__auto__","x__19395__auto__",-2060576050,null))))))),cljs.core._conj.call(null,cljs.core.List.EMPTY,cljs.core.sequence.call(null,cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core._conj.call(null,cljs.core.List.EMPTY,name),cargs,cljs.core._conj.call(null,cljs.core.List.EMPTY,new cljs.core.Symbol(null,"x__19395__auto__","x__19395__auto__",-2060576050,null)))))))))))))),cljs.core._conj.call(null,cljs.core.List.EMPTY,cljs.core.sequence.call(null,cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core._conj.call(null,cljs.core.List.EMPTY,args),body)))))));
});
quantum.core.function$.zeroid = (function quantum$core$function$zeroid(func,base){
return (function() {
var G__19396 = null;
var G__19396__0 = (function (){
return base;
});
var G__19396__2 = (function (arg1,arg2){
return func.call(null,arg1,arg2);
});
var G__19396__3 = (function (arg1,arg2,arg3){
return func.call(null,func.call(null,arg1,arg2),arg3);
});
var G__19396__4 = (function() { 
var G__19397__delegate = function (arg1,arg2,arg3,args){
return cljs.core.apply.call(null,func,func.call(null,func.call(null,arg1,arg2),arg3),args);
};
var G__19397 = function (arg1,arg2,arg3,var_args){
var args = null;
if (arguments.length > 3) {
var G__19398__i = 0, G__19398__a = new Array(arguments.length -  3);
while (G__19398__i < G__19398__a.length) {G__19398__a[G__19398__i] = arguments[G__19398__i + 3]; ++G__19398__i;}
  args = new cljs.core.IndexedSeq(G__19398__a,0);
} 
return G__19397__delegate.call(this,arg1,arg2,arg3,args);};
G__19397.cljs$lang$maxFixedArity = 3;
G__19397.cljs$lang$applyTo = (function (arglist__19399){
var arg1 = cljs.core.first(arglist__19399);
arglist__19399 = cljs.core.next(arglist__19399);
var arg2 = cljs.core.first(arglist__19399);
arglist__19399 = cljs.core.next(arglist__19399);
var arg3 = cljs.core.first(arglist__19399);
var args = cljs.core.rest(arglist__19399);
return G__19397__delegate(arg1,arg2,arg3,args);
});
G__19397.cljs$core$IFn$_invoke$arity$variadic = G__19397__delegate;
return G__19397;
})()
;
G__19396 = function(arg1,arg2,arg3,var_args){
var args = var_args;
switch(arguments.length){
case 0:
return G__19396__0.call(this);
case 2:
return G__19396__2.call(this,arg1,arg2);
case 3:
return G__19396__3.call(this,arg1,arg2,arg3);
default:
var G__19400 = null;
if (arguments.length > 3) {
var G__19401__i = 0, G__19401__a = new Array(arguments.length -  3);
while (G__19401__i < G__19401__a.length) {G__19401__a[G__19401__i] = arguments[G__19401__i + 3]; ++G__19401__i;}
G__19400 = new cljs.core.IndexedSeq(G__19401__a,0);
}
return G__19396__4.cljs$core$IFn$_invoke$arity$variadic(arg1,arg2,arg3, G__19400);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
G__19396.cljs$lang$maxFixedArity = 3;
G__19396.cljs$lang$applyTo = G__19396__4.cljs$lang$applyTo;
G__19396.cljs$core$IFn$_invoke$arity$0 = G__19396__0;
G__19396.cljs$core$IFn$_invoke$arity$2 = G__19396__2;
G__19396.cljs$core$IFn$_invoke$arity$3 = G__19396__3;
G__19396.cljs$core$IFn$_invoke$arity$variadic = G__19396__4.cljs$core$IFn$_invoke$arity$variadic;
return G__19396;
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

quantum.core.function$.compr.cljs$lang$applyTo = (function (seq19402){
return quantum.core.function$.compr.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq19402));
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

quantum.core.function$.fn_STAR_.cljs$lang$applyTo = (function (seq19403){
return quantum.core.function$.fn_STAR_.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq19403));
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

quantum.core.function$.f_STAR_n.cljs$lang$applyTo = (function (seq19404){
var G__19405 = cljs.core.first.call(null,seq19404);
var seq19404__$1 = cljs.core.next.call(null,seq19404);
return quantum.core.function$.f_STAR_n.cljs$core$IFn$_invoke$arity$variadic(G__19405,seq19404__$1);
});
quantum.core.function$.f_STAR__STAR_n = (function quantum$core$function$f_STAR__STAR_n(){
var argseq__19113__auto__ = ((((1) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(1)),(0))):null);
return quantum.core.function$.f_STAR__STAR_n.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__19113__auto__);
});

quantum.core.function$.f_STAR__STAR_n.cljs$core$IFn$_invoke$arity$variadic = (function (func,args){
return (function() { 
var G__19408__delegate = function (args_inner){
return cljs.core.apply.call(null,func,cljs.core.concat.call(null,args_inner,args));
};
var G__19408 = function (var_args){
var args_inner = null;
if (arguments.length > 0) {
var G__19409__i = 0, G__19409__a = new Array(arguments.length -  0);
while (G__19409__i < G__19409__a.length) {G__19409__a[G__19409__i] = arguments[G__19409__i + 0]; ++G__19409__i;}
  args_inner = new cljs.core.IndexedSeq(G__19409__a,0);
} 
return G__19408__delegate.call(this,args_inner);};
G__19408.cljs$lang$maxFixedArity = 0;
G__19408.cljs$lang$applyTo = (function (arglist__19410){
var args_inner = cljs.core.seq(arglist__19410);
return G__19408__delegate(args_inner);
});
G__19408.cljs$core$IFn$_invoke$arity$variadic = G__19408__delegate;
return G__19408;
})()
;
});

quantum.core.function$.f_STAR__STAR_n.cljs$lang$maxFixedArity = (1);

quantum.core.function$.f_STAR__STAR_n.cljs$lang$applyTo = (function (seq19406){
var G__19407 = cljs.core.first.call(null,seq19406);
var seq19406__$1 = cljs.core.next.call(null,seq19406);
return quantum.core.function$.f_STAR__STAR_n.cljs$core$IFn$_invoke$arity$variadic(G__19407,seq19406__$1);
});
quantum.core.function$._STAR_fn = (function quantum$core$function$_STAR_fn(){
var argseq__19113__auto__ = ((((0) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(0)),(0))):null);
return quantum.core.function$._STAR_fn.cljs$core$IFn$_invoke$arity$variadic(argseq__19113__auto__);
});

quantum.core.function$._STAR_fn.cljs$core$IFn$_invoke$arity$variadic = (function (args){
return quantum.core.function$.f_STAR_n.call(null,cljs.core.apply,args);
});

quantum.core.function$._STAR_fn.cljs$lang$maxFixedArity = (0);

quantum.core.function$._STAR_fn.cljs$lang$applyTo = (function (seq19411){
return quantum.core.function$._STAR_fn.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq19411));
});
quantum.core.function$.fn_bi = (function quantum$core$function$fn_bi(arg){
return (function (p1__19412_SHARP_,p2__19413_SHARP_){
return arg.call(null,p1__19412_SHARP_,p2__19413_SHARP_);
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

quantum.core.function$.call_fn_STAR_.cljs$lang$applyTo = (function (seq19416){
return quantum.core.function$.call_fn_STAR_.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq19416));
});
quantum.core.function$.call_f_STAR_n = (function quantum$core$function$call_f_STAR_n(){
var argseq__19113__auto__ = ((((0) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(0)),(0))):null);
return quantum.core.function$.call_f_STAR_n.cljs$core$IFn$_invoke$arity$variadic(argseq__19113__auto__);
});

quantum.core.function$.call_f_STAR_n.cljs$core$IFn$_invoke$arity$variadic = (function (args){
return cljs.core.apply.call(null,quantum.core.function$.f_STAR_n,cljs.core.butlast.call(null,args)).call(null,cljs.core.last.call(null,args));
});

quantum.core.function$.call_f_STAR_n.cljs$lang$maxFixedArity = (0);

quantum.core.function$.call_f_STAR_n.cljs$lang$applyTo = (function (seq19417){
return quantum.core.function$.call_f_STAR_n.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq19417));
});
quantum.core.function$.call__GT_ = (function quantum$core$function$call__GT_(){
var argseq__19113__auto__ = ((((1) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(1)),(0))):null);
return quantum.core.function$.call__GT_.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__19113__auto__);
});

quantum.core.function$.call__GT_.cljs$core$IFn$_invoke$arity$variadic = (function (arg,p__19420){
var vec__19421 = p__19420;
var func = cljs.core.nth.call(null,vec__19421,(0),null);
var args = cljs.core.nthnext.call(null,vec__19421,(1));
return cljs.core.apply.call(null,func,args).call(null,arg);
});

quantum.core.function$.call__GT_.cljs$lang$maxFixedArity = (1);

quantum.core.function$.call__GT_.cljs$lang$applyTo = (function (seq19418){
var G__19419 = cljs.core.first.call(null,seq19418);
var seq19418__$1 = cljs.core.next.call(null,seq19418);
return quantum.core.function$.call__GT_.cljs$core$IFn$_invoke$arity$variadic(G__19419,seq19418__$1);
});
quantum.core.function$.call__GT__GT_ = (function quantum$core$function$call__GT__GT_(){
var argseq__19113__auto__ = ((((0) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(0)),(0))):null);
return quantum.core.function$.call__GT__GT_.cljs$core$IFn$_invoke$arity$variadic(argseq__19113__auto__);
});

quantum.core.function$.call__GT__GT_.cljs$core$IFn$_invoke$arity$variadic = (function (p__19423){
var vec__19424 = p__19423;
var func = cljs.core.nth.call(null,vec__19424,(0),null);
var args = cljs.core.nthnext.call(null,vec__19424,(1));
return cljs.core.apply.call(null,func,cljs.core.butlast.call(null,args)).call(null,cljs.core.last.call(null,args));
});

quantum.core.function$.call__GT__GT_.cljs$lang$maxFixedArity = (0);

quantum.core.function$.call__GT__GT_.cljs$lang$applyTo = (function (seq19422){
return quantum.core.function$.call__GT__GT_.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq19422));
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

quantum.core.function$.juxtm.cljs$lang$applyTo = (function (seq19425){
return quantum.core.function$.juxtm.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq19425));
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

quantum.core.function$.juxt_sm.cljs$lang$applyTo = (function (seq19426){
return quantum.core.function$.juxt_sm.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq19426));
});
quantum.core.function$.with_pr = (function quantum$core$function$with_pr(obj){
cljs.core.println.call(null,obj);

return obj;
});
quantum.core.function$.with_msg = (function quantum$core$function$with_msg(msg,obj){
cljs.core.println.call(null,msg);

return obj;
});
quantum.core.function$.with$ = (function quantum$core$function$with(expr,obj){

return obj;
});
quantum.core.function$.withf = (function quantum$core$function$withf(func,obj){
func.call(null,obj);

return obj;
});
quantum.core.function$.do_rfn = (function quantum$core$function$do_rfn(f1,k,fkv){
return cljs.core.sequence.call(null,cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core._conj.call(null,cljs.core.List.EMPTY,new cljs.core.Symbol("quantum.core.function","fn","quantum.core.function/fn",-1770437089,null)),cljs.core._conj.call(null,cljs.core.List.EMPTY,cljs.core.sequence.call(null,cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core._conj.call(null,cljs.core.List.EMPTY,cljs.core.vec.call(null,cljs.core.sequence.call(null,cljs.core.seq.call(null,cljs.core.concat.call(null))))),cljs.core._conj.call(null,cljs.core.List.EMPTY,cljs.core.sequence.call(null,cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core._conj.call(null,cljs.core.List.EMPTY,f1))))))))),cljs.core._conj.call(null,cljs.core.List.EMPTY,clojure.walk.postwalk.call(null,(function (p1__19427_SHARP_){
if(cljs.core.sequential_QMARK_.call(null,p1__19427_SHARP_)){
return ((cljs.core.vector_QMARK_.call(null,p1__19427_SHARP_))?cljs.core.vec:cljs.core.identity).call(null,cljs.core.remove.call(null,cljs.core.PersistentHashSet.fromArray([k], true),p1__19427_SHARP_));
} else {
return p1__19427_SHARP_;
}
}),fkv)),cljs.core._conj.call(null,cljs.core.List.EMPTY,fkv))));
});

//# sourceMappingURL=function.js.map?rel=1431625564873