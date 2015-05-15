// Compiled by ClojureScript 0.0-3269 {}
goog.provide('quantum.core.numeric');
goog.require('cljs.core');
goog.require('quantum.core.ns');
goog.require('quantum.core.logic');
goog.require('quantum.core.type');
quantum.core.numeric.sign = (function quantum$core$numeric$sign(n){
if((n < (0))){
return (-1);
} else {
return (1);
}
});
quantum.core.numeric.nneg_QMARK_ = quantum.core.logic.fn_not.call(null,cljs.core.neg_QMARK_);
quantum.core.numeric.pos_int_QMARK_ = quantum.core.logic.fn_and.call(null,cljs.core.integer_QMARK_,cljs.core.pos_QMARK_);
quantum.core.numeric.nneg_int_QMARK_ = quantum.core.logic.fn_and.call(null,cljs.core.integer_QMARK_,quantum.core.numeric.nneg_QMARK_);
quantum.core.numeric.neg = cljs.core.partial.call(null,cljs.core._STAR_,(-1));
quantum.core.numeric.abs = (function quantum$core$numeric$abs(arg__22915__auto__){
var obj_f__22903__auto__ = arg__22915__auto__;
if((obj_f__22903__auto__ < (0))){
return quantum.core.numeric.neg.call(null,obj_f__22903__auto__);
} else {
return obj_f__22903__auto__;
}
});
quantum.core.numeric.int_nil = (function quantum$core$numeric$int_nil(arg__22915__auto__){
var obj_f__22903__auto__ = arg__22915__auto__;
if((obj_f__22903__auto__ == null)){
return cljs.core.constantly.call(null,(0)).call(null,obj_f__22903__auto__);
} else {
return obj_f__22903__auto__;
}
});
quantum.core.numeric.floor = (function quantum$core$numeric$floor(x){
return Math.floor(x);
});
quantum.core.numeric.ceil = (function quantum$core$numeric$ceil(x){
return Math.ceil(x);
});
quantum.core.numeric.safe_PLUS_ = (function quantum$core$numeric$safe_PLUS_(){
var G__22962 = arguments.length;
switch (G__22962) {
case 1:
return quantum.core.numeric.safe_PLUS_.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return quantum.core.numeric.safe_PLUS_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return quantum.core.numeric.safe_PLUS_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
var argseq__19124__auto__ = (new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(3)),(0)));
return quantum.core.numeric.safe_PLUS_.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),argseq__19124__auto__);

}
});

quantum.core.numeric.safe_PLUS_.cljs$core$IFn$_invoke$arity$1 = (function (a){
return quantum.core.numeric.int_nil.call(null,a);
});

quantum.core.numeric.safe_PLUS_.cljs$core$IFn$_invoke$arity$2 = (function (a,b){
return (quantum.core.numeric.int_nil.call(null,a) + quantum.core.numeric.int_nil.call(null,b));
});

quantum.core.numeric.safe_PLUS_.cljs$core$IFn$_invoke$arity$3 = (function (a,b,c){
return ((quantum.core.numeric.int_nil.call(null,a) + quantum.core.numeric.int_nil.call(null,b)) + quantum.core.numeric.int_nil.call(null,c));
});

quantum.core.numeric.safe_PLUS_.cljs$core$IFn$_invoke$arity$variadic = (function (a,b,c,args){
return cljs.core.apply.call(null,cljs.core._PLUS_,cljs.core.map.call(null,quantum.core.numeric.int_nil,cljs.core.conj.call(null,args,c,b,a)));
});

quantum.core.numeric.safe_PLUS_.cljs$lang$applyTo = (function (seq22957){
var G__22958 = cljs.core.first.call(null,seq22957);
var seq22957__$1 = cljs.core.next.call(null,seq22957);
var G__22959 = cljs.core.first.call(null,seq22957__$1);
var seq22957__$2 = cljs.core.next.call(null,seq22957__$1);
var G__22960 = cljs.core.first.call(null,seq22957__$2);
var seq22957__$3 = cljs.core.next.call(null,seq22957__$2);
return quantum.core.numeric.safe_PLUS_.cljs$core$IFn$_invoke$arity$variadic(G__22958,G__22959,G__22960,seq22957__$3);
});

quantum.core.numeric.safe_PLUS_.cljs$lang$maxFixedArity = (3);
quantum.core.numeric.safe_STAR_ = (function quantum$core$numeric$safe_STAR_(){
var G__22969 = arguments.length;
switch (G__22969) {
case 1:
return quantum.core.numeric.safe_STAR_.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return quantum.core.numeric.safe_STAR_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return quantum.core.numeric.safe_STAR_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
var argseq__19124__auto__ = (new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(3)),(0)));
return quantum.core.numeric.safe_STAR_.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),argseq__19124__auto__);

}
});

quantum.core.numeric.safe_STAR_.cljs$core$IFn$_invoke$arity$1 = (function (a){
return quantum.core.numeric.int_nil.call(null,a);
});

quantum.core.numeric.safe_STAR_.cljs$core$IFn$_invoke$arity$2 = (function (a,b){
return (quantum.core.numeric.int_nil.call(null,a) * quantum.core.numeric.int_nil.call(null,b));
});

quantum.core.numeric.safe_STAR_.cljs$core$IFn$_invoke$arity$3 = (function (a,b,c){
return ((quantum.core.numeric.int_nil.call(null,a) * quantum.core.numeric.int_nil.call(null,b)) * quantum.core.numeric.int_nil.call(null,c));
});

quantum.core.numeric.safe_STAR_.cljs$core$IFn$_invoke$arity$variadic = (function (a,b,c,args){
return cljs.core.apply.call(null,cljs.core._STAR_,cljs.core.map.call(null,quantum.core.numeric.int_nil,cljs.core.conj.call(null,args,c,b,a)));
});

quantum.core.numeric.safe_STAR_.cljs$lang$applyTo = (function (seq22964){
var G__22965 = cljs.core.first.call(null,seq22964);
var seq22964__$1 = cljs.core.next.call(null,seq22964);
var G__22966 = cljs.core.first.call(null,seq22964__$1);
var seq22964__$2 = cljs.core.next.call(null,seq22964__$1);
var G__22967 = cljs.core.first.call(null,seq22964__$2);
var seq22964__$3 = cljs.core.next.call(null,seq22964__$2);
return quantum.core.numeric.safe_STAR_.cljs$core$IFn$_invoke$arity$variadic(G__22965,G__22966,G__22967,seq22964__$3);
});

quantum.core.numeric.safe_STAR_.cljs$lang$maxFixedArity = (3);
quantum.core.numeric.safe_ = (function quantum$core$numeric$safe_(){
var G__22976 = arguments.length;
switch (G__22976) {
case 1:
return quantum.core.numeric.safe_.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return quantum.core.numeric.safe_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return quantum.core.numeric.safe_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
var argseq__19124__auto__ = (new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(3)),(0)));
return quantum.core.numeric.safe_.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),argseq__19124__auto__);

}
});

quantum.core.numeric.safe_.cljs$core$IFn$_invoke$arity$1 = (function (a){
return quantum.core.numeric.neg.call(null,quantum.core.numeric.int_nil.call(null,a));
});

quantum.core.numeric.safe_.cljs$core$IFn$_invoke$arity$2 = (function (a,b){
return (quantum.core.numeric.int_nil.call(null,a) - quantum.core.numeric.int_nil.call(null,b));
});

quantum.core.numeric.safe_.cljs$core$IFn$_invoke$arity$3 = (function (a,b,c){
return ((quantum.core.numeric.int_nil.call(null,a) - quantum.core.numeric.int_nil.call(null,b)) - quantum.core.numeric.int_nil.call(null,c));
});

quantum.core.numeric.safe_.cljs$core$IFn$_invoke$arity$variadic = (function (a,b,c,args){
return cljs.core.apply.call(null,cljs.core._,cljs.core.map.call(null,quantum.core.numeric.int_nil,cljs.core.conj.call(null,args,c,b,a)));
});

quantum.core.numeric.safe_.cljs$lang$applyTo = (function (seq22971){
var G__22972 = cljs.core.first.call(null,seq22971);
var seq22971__$1 = cljs.core.next.call(null,seq22971);
var G__22973 = cljs.core.first.call(null,seq22971__$1);
var seq22971__$2 = cljs.core.next.call(null,seq22971__$1);
var G__22974 = cljs.core.first.call(null,seq22971__$2);
var seq22971__$3 = cljs.core.next.call(null,seq22971__$2);
return quantum.core.numeric.safe_.cljs$core$IFn$_invoke$arity$variadic(G__22972,G__22973,G__22974,seq22971__$3);
});

quantum.core.numeric.safe_.cljs$lang$maxFixedArity = (3);
quantum.core.numeric.safediv = (function quantum$core$numeric$safediv(){
var G__22983 = arguments.length;
switch (G__22983) {
case 2:
return quantum.core.numeric.safediv.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return quantum.core.numeric.safediv.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
var argseq__19124__auto__ = (new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(3)),(0)));
return quantum.core.numeric.safediv.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),argseq__19124__auto__);

}
});

quantum.core.numeric.safediv.cljs$core$IFn$_invoke$arity$2 = (function (a,b){
return (quantum.core.numeric.int_nil.call(null,a) / quantum.core.numeric.int_nil.call(null,b));
});

quantum.core.numeric.safediv.cljs$core$IFn$_invoke$arity$3 = (function (a,b,c){
return ((quantum.core.numeric.int_nil.call(null,a) / quantum.core.numeric.int_nil.call(null,b)) / quantum.core.numeric.int_nil.call(null,c));
});

quantum.core.numeric.safediv.cljs$core$IFn$_invoke$arity$variadic = (function (a,b,c,args){
return cljs.core.apply.call(null,cljs.core._SLASH_,cljs.core.map.call(null,quantum.core.numeric.int_nil,cljs.core.conj.call(null,args,c,b,a)));
});

quantum.core.numeric.safediv.cljs$lang$applyTo = (function (seq22978){
var G__22979 = cljs.core.first.call(null,seq22978);
var seq22978__$1 = cljs.core.next.call(null,seq22978);
var G__22980 = cljs.core.first.call(null,seq22978__$1);
var seq22978__$2 = cljs.core.next.call(null,seq22978__$1);
var G__22981 = cljs.core.first.call(null,seq22978__$2);
var seq22978__$3 = cljs.core.next.call(null,seq22978__$2);
return quantum.core.numeric.safediv.cljs$core$IFn$_invoke$arity$variadic(G__22979,G__22980,G__22981,seq22978__$3);
});

quantum.core.numeric.safediv.cljs$lang$maxFixedArity = (3);
/**
 * Reverse comparator.
 */
quantum.core.numeric.rcompare = (function quantum$core$numeric$rcompare(x,y){
return cljs.core.compare.call(null,y,x);
});
/**
 * Returns the 'greatest' element in coll in O(n) time.
 */
quantum.core.numeric.greatest = (function quantum$core$numeric$greatest(){
var argseq__19113__auto__ = ((((1) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(1)),(0))):null);
return quantum.core.numeric.greatest.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__19113__auto__);
});

quantum.core.numeric.greatest.cljs$core$IFn$_invoke$arity$variadic = (function (coll,p__22989){
var vec__22990 = p__22989;
var _QMARK_comparator = cljs.core.nth.call(null,vec__22990,(0),null);
var comparator = (function (){var or__18073__auto__ = _QMARK_comparator;
if(cljs.core.truth_(or__18073__auto__)){
return or__18073__auto__;
} else {
return quantum.core.numeric.rcompare;
}
})();
return cljs.core.reduce.call(null,((function (comparator,vec__22990,_QMARK_comparator){
return (function (p1__22985_SHARP_,p2__22986_SHARP_){
if((comparator.call(null,p1__22985_SHARP_,p2__22986_SHARP_) > (0))){
return p2__22986_SHARP_;
} else {
return p1__22985_SHARP_;
}
});})(comparator,vec__22990,_QMARK_comparator))
,coll);
});

quantum.core.numeric.greatest.cljs$lang$maxFixedArity = (1);

quantum.core.numeric.greatest.cljs$lang$applyTo = (function (seq22987){
var G__22988 = cljs.core.first.call(null,seq22987);
var seq22987__$1 = cljs.core.next.call(null,seq22987);
return quantum.core.numeric.greatest.cljs$core$IFn$_invoke$arity$variadic(G__22988,seq22987__$1);
});
/**
 * Returns the 'least' element in coll in O(n) time.
 */
quantum.core.numeric.least = (function quantum$core$numeric$least(){
var argseq__19113__auto__ = ((((1) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(1)),(0))):null);
return quantum.core.numeric.least.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__19113__auto__);
});

quantum.core.numeric.least.cljs$core$IFn$_invoke$arity$variadic = (function (coll,p__22995){
var vec__22996 = p__22995;
var _QMARK_comparator = cljs.core.nth.call(null,vec__22996,(0),null);
var comparator = (function (){var or__18073__auto__ = _QMARK_comparator;
if(cljs.core.truth_(or__18073__auto__)){
return or__18073__auto__;
} else {
return quantum.core.numeric.rcompare;
}
})();
return cljs.core.reduce.call(null,((function (comparator,vec__22996,_QMARK_comparator){
return (function (p1__22991_SHARP_,p2__22992_SHARP_){
if((comparator.call(null,p1__22991_SHARP_,p2__22992_SHARP_) < (0))){
return p2__22992_SHARP_;
} else {
return p1__22991_SHARP_;
}
});})(comparator,vec__22996,_QMARK_comparator))
,coll);
});

quantum.core.numeric.least.cljs$lang$maxFixedArity = (1);

quantum.core.numeric.least.cljs$lang$applyTo = (function (seq22993){
var G__22994 = cljs.core.first.call(null,seq22993);
var seq22993__$1 = cljs.core.next.call(null,seq22993);
return quantum.core.numeric.least.cljs$core$IFn$_invoke$arity$variadic(G__22994,seq22993__$1);
});
quantum.core.numeric.greatest_or = (function quantum$core$numeric$greatest_or(a,b,else$){
if((a > b)){
return a;
} else {
if((b > a)){
return b;
} else {
return else$;

}
}
});
quantum.core.numeric.least_or = (function quantum$core$numeric$least_or(a,b,else$){
if((a < b)){
return a;
} else {
if((b < a)){
return b;
} else {
return else$;

}
}
});
quantum.core.numeric.approx_QMARK_ = (function quantum$core$numeric$approx_QMARK_(tolerance,a,b){
return (quantum.core.numeric.abs.call(null,(quantum.core.numeric.int_nil.call(null,a) - quantum.core.numeric.int_nil.call(null,b))) < tolerance);
});
quantum.core.numeric.sin = (function quantum$core$numeric$sin(n){
return Math.sin(n);
});

quantum.core.numeric.ToInt = (function (){var obj22998 = {};
return obj22998;
})();

/**
 * A simple function to coerce numbers, and strings, etc; to an int.
 * Note: nil input returns nil.
 */
quantum.core.numeric.int_PLUS_ = (function quantum$core$numeric$int_PLUS_(i){
if((function (){var and__18061__auto__ = i;
if(and__18061__auto__){
return i.quantum$core$numeric$ToInt$int_PLUS_$arity$1;
} else {
return and__18061__auto__;
}
})()){
return i.quantum$core$numeric$ToInt$int_PLUS_$arity$1(i);
} else {
var x__18709__auto__ = (((i == null))?null:i);
return (function (){var or__18073__auto__ = (quantum.core.numeric.int_PLUS_[goog.typeOf(x__18709__auto__)]);
if(or__18073__auto__){
return or__18073__auto__;
} else {
var or__18073__auto____$1 = (quantum.core.numeric.int_PLUS_["_"]);
if(or__18073__auto____$1){
return or__18073__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"ToInt.int+",i);
}
}
})().call(null,i);
}
});

(quantum.core.numeric.ToInt["number"] = true);

(quantum.core.numeric.int_PLUS_["number"] = (function (i){
return i;
}));

(quantum.core.numeric.ToInt["null"] = true);

(quantum.core.numeric.int_PLUS_["null"] = (function (_){
return null;
}));

(quantum.core.numeric.ToInt["string"] = true);

(quantum.core.numeric.int_PLUS_["string"] = (function (i){
return parseInt(i);
}));

quantum.core.numeric.ToLong = (function (){var obj23000 = {};
return obj23000;
})();

/**
 * A simple function to coerce numbers, and strings, etc; to a long.
 * Note: nil input returns nil.
 */
quantum.core.numeric.long_PLUS_ = (function quantum$core$numeric$long_PLUS_(i){
if((function (){var and__18061__auto__ = i;
if(and__18061__auto__){
return i.quantum$core$numeric$ToLong$long_PLUS_$arity$1;
} else {
return and__18061__auto__;
}
})()){
return i.quantum$core$numeric$ToLong$long_PLUS_$arity$1(i);
} else {
var x__18709__auto__ = (((i == null))?null:i);
return (function (){var or__18073__auto__ = (quantum.core.numeric.long_PLUS_[goog.typeOf(x__18709__auto__)]);
if(or__18073__auto__){
return or__18073__auto__;
} else {
var or__18073__auto____$1 = (quantum.core.numeric.long_PLUS_["_"]);
if(or__18073__auto____$1){
return or__18073__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"ToLong.long+",i);
}
}
})().call(null,i);
}
});

(quantum.core.numeric.ToLong["number"] = true);

(quantum.core.numeric.long_PLUS_["number"] = (function (l){
return cljs.core.long$.call(null,l);
}));

(quantum.core.numeric.ToLong["null"] = true);

(quantum.core.numeric.long_PLUS_["null"] = (function (_){
return null;
}));

(quantum.core.numeric.ToLong["string"] = true);

(quantum.core.numeric.long_PLUS_["string"] = (function (l){
return cljs.core.long$.call(null,parseInt(l));
}));

//# sourceMappingURL=numeric.js.map?rel=1431625568462