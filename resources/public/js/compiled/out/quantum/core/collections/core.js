// Compiled by ClojureScript 0.0-3269 {}
goog.provide('quantum.core.collections.core');
goog.require('cljs.core');
goog.require('quantum.core.data.set');
goog.require('quantum.core.logic');
goog.require('quantum.core.macros');
goog.require('quantum.core.type');
goog.require('quantum.core.function$');
goog.require('quantum.core.data.vector');
goog.require('quantum.core.ns');
goog.require('quantum.core.reducers');
goog.require('quantum.core.string');
goog.require('quantum.core.error');
/**
 * Last index of a coll.
 */
quantum.core.collections.core.lasti = (function quantum$core$collections$core$lasti(coll){
return (cljs.core.count.call(null,coll) - (1));
});

quantum.core.collections.core.CollCount = (function (){var obj24813 = {};
return obj24813;
})();

quantum.core.collections.core.count_PLUS_ = (function quantum$core$collections$core$count_PLUS_(coll){
if((function (){var and__18061__auto__ = coll;
if(and__18061__auto__){
return coll.quantum$core$collections$core$CollCount$count_PLUS_$arity$1;
} else {
return and__18061__auto__;
}
})()){
return coll.quantum$core$collections$core$CollCount$count_PLUS_$arity$1(coll);
} else {
var x__18709__auto__ = (((coll == null))?null:coll);
return (function (){var or__18073__auto__ = (quantum.core.collections.core.count_PLUS_[goog.typeOf(x__18709__auto__)]);
if(or__18073__auto__){
return or__18073__auto__;
} else {
var or__18073__auto____$1 = (quantum.core.collections.core.count_PLUS_["_"]);
if(or__18073__auto____$1){
return or__18073__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"CollCount.count+",coll);
}
}
})().call(null,coll);
}
});

(quantum.core.collections.core.CollCount["null"] = true);

(quantum.core.collections.core.count_PLUS_["null"] = (function (coll){
return (0);
}));

(quantum.core.collections.core.CollCount["_"] = true);

(quantum.core.collections.core.count_PLUS_["_"] = (function (coll){
return cljs.core.count.call(null,coll);
}));

quantum.core.collections.core.CollRetrieve = (function (){var obj24815 = {};
return obj24815;
})();

/**
 * Get range
 */
quantum.core.collections.core.getr_PLUS_ = (function quantum$core$collections$core$getr_PLUS_(){
var G__24817 = arguments.length;
switch (G__24817) {
case 2:
return quantum.core.collections.core.getr_PLUS_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return quantum.core.collections.core.getr_PLUS_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.collections.core.getr_PLUS_.cljs$core$IFn$_invoke$arity$2 = (function (coll,a){
if((function (){var and__18061__auto__ = coll;
if(and__18061__auto__){
return coll.quantum$core$collections$core$CollRetrieve$getr_PLUS_$arity$2;
} else {
return and__18061__auto__;
}
})()){
return coll.quantum$core$collections$core$CollRetrieve$getr_PLUS_$arity$2(coll,a);
} else {
var x__18709__auto__ = (((coll == null))?null:coll);
return (function (){var or__18073__auto__ = (quantum.core.collections.core.getr_PLUS_[goog.typeOf(x__18709__auto__)]);
if(or__18073__auto__){
return or__18073__auto__;
} else {
var or__18073__auto____$1 = (quantum.core.collections.core.getr_PLUS_["_"]);
if(or__18073__auto____$1){
return or__18073__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"CollRetrieve.getr+",coll);
}
}
})().call(null,coll,a);
}
});

quantum.core.collections.core.getr_PLUS_.cljs$core$IFn$_invoke$arity$3 = (function (coll,a,b){
if((function (){var and__18061__auto__ = coll;
if(and__18061__auto__){
return coll.quantum$core$collections$core$CollRetrieve$getr_PLUS_$arity$3;
} else {
return and__18061__auto__;
}
})()){
return coll.quantum$core$collections$core$CollRetrieve$getr_PLUS_$arity$3(coll,a,b);
} else {
var x__18709__auto__ = (((coll == null))?null:coll);
return (function (){var or__18073__auto__ = (quantum.core.collections.core.getr_PLUS_[goog.typeOf(x__18709__auto__)]);
if(or__18073__auto__){
return or__18073__auto__;
} else {
var or__18073__auto____$1 = (quantum.core.collections.core.getr_PLUS_["_"]);
if(or__18073__auto____$1){
return or__18073__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"CollRetrieve.getr+",coll);
}
}
})().call(null,coll,a,b);
}
});

quantum.core.collections.core.getr_PLUS_.cljs$lang$maxFixedArity = 3;

quantum.core.collections.core.get_PLUS_ = (function quantum$core$collections$core$get_PLUS_(){
var G__24819 = arguments.length;
switch (G__24819) {
case 2:
return quantum.core.collections.core.get_PLUS_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return quantum.core.collections.core.get_PLUS_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.collections.core.get_PLUS_.cljs$core$IFn$_invoke$arity$2 = (function (coll,n){
if((function (){var and__18061__auto__ = coll;
if(and__18061__auto__){
return coll.quantum$core$collections$core$CollRetrieve$get_PLUS_$arity$2;
} else {
return and__18061__auto__;
}
})()){
return coll.quantum$core$collections$core$CollRetrieve$get_PLUS_$arity$2(coll,n);
} else {
var x__18709__auto__ = (((coll == null))?null:coll);
return (function (){var or__18073__auto__ = (quantum.core.collections.core.get_PLUS_[goog.typeOf(x__18709__auto__)]);
if(or__18073__auto__){
return or__18073__auto__;
} else {
var or__18073__auto____$1 = (quantum.core.collections.core.get_PLUS_["_"]);
if(or__18073__auto____$1){
return or__18073__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"CollRetrieve.get+",coll);
}
}
})().call(null,coll,n);
}
});

quantum.core.collections.core.get_PLUS_.cljs$core$IFn$_invoke$arity$3 = (function (coll,n,if_not_found){
if((function (){var and__18061__auto__ = coll;
if(and__18061__auto__){
return coll.quantum$core$collections$core$CollRetrieve$get_PLUS_$arity$3;
} else {
return and__18061__auto__;
}
})()){
return coll.quantum$core$collections$core$CollRetrieve$get_PLUS_$arity$3(coll,n,if_not_found);
} else {
var x__18709__auto__ = (((coll == null))?null:coll);
return (function (){var or__18073__auto__ = (quantum.core.collections.core.get_PLUS_[goog.typeOf(x__18709__auto__)]);
if(or__18073__auto__){
return or__18073__auto__;
} else {
var or__18073__auto____$1 = (quantum.core.collections.core.get_PLUS_["_"]);
if(or__18073__auto____$1){
return or__18073__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"CollRetrieve.get+",coll);
}
}
})().call(null,coll,n,if_not_found);
}
});

quantum.core.collections.core.get_PLUS_.cljs$lang$maxFixedArity = 3;

quantum.core.collections.core.first_PLUS_ = (function quantum$core$collections$core$first_PLUS_(coll){
if((function (){var and__18061__auto__ = coll;
if(and__18061__auto__){
return coll.quantum$core$collections$core$CollRetrieve$first_PLUS_$arity$1;
} else {
return and__18061__auto__;
}
})()){
return coll.quantum$core$collections$core$CollRetrieve$first_PLUS_$arity$1(coll);
} else {
var x__18709__auto__ = (((coll == null))?null:coll);
return (function (){var or__18073__auto__ = (quantum.core.collections.core.first_PLUS_[goog.typeOf(x__18709__auto__)]);
if(or__18073__auto__){
return or__18073__auto__;
} else {
var or__18073__auto____$1 = (quantum.core.collections.core.first_PLUS_["_"]);
if(or__18073__auto____$1){
return or__18073__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"CollRetrieve.first+",coll);
}
}
})().call(null,coll);
}
});

quantum.core.collections.core.second_PLUS_ = (function quantum$core$collections$core$second_PLUS_(coll){
if((function (){var and__18061__auto__ = coll;
if(and__18061__auto__){
return coll.quantum$core$collections$core$CollRetrieve$second_PLUS_$arity$1;
} else {
return and__18061__auto__;
}
})()){
return coll.quantum$core$collections$core$CollRetrieve$second_PLUS_$arity$1(coll);
} else {
var x__18709__auto__ = (((coll == null))?null:coll);
return (function (){var or__18073__auto__ = (quantum.core.collections.core.second_PLUS_[goog.typeOf(x__18709__auto__)]);
if(or__18073__auto__){
return or__18073__auto__;
} else {
var or__18073__auto____$1 = (quantum.core.collections.core.second_PLUS_["_"]);
if(or__18073__auto____$1){
return or__18073__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"CollRetrieve.second+",coll);
}
}
})().call(null,coll);
}
});

quantum.core.collections.core.rest_PLUS_ = (function quantum$core$collections$core$rest_PLUS_(coll){
if((function (){var and__18061__auto__ = coll;
if(and__18061__auto__){
return coll.quantum$core$collections$core$CollRetrieve$rest_PLUS_$arity$1;
} else {
return and__18061__auto__;
}
})()){
return coll.quantum$core$collections$core$CollRetrieve$rest_PLUS_$arity$1(coll);
} else {
var x__18709__auto__ = (((coll == null))?null:coll);
return (function (){var or__18073__auto__ = (quantum.core.collections.core.rest_PLUS_[goog.typeOf(x__18709__auto__)]);
if(or__18073__auto__){
return or__18073__auto__;
} else {
var or__18073__auto____$1 = (quantum.core.collections.core.rest_PLUS_["_"]);
if(or__18073__auto____$1){
return or__18073__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"CollRetrieve.rest+",coll);
}
}
})().call(null,coll);
}
});

quantum.core.collections.core.butlast_PLUS_ = (function quantum$core$collections$core$butlast_PLUS_(coll){
if((function (){var and__18061__auto__ = coll;
if(and__18061__auto__){
return coll.quantum$core$collections$core$CollRetrieve$butlast_PLUS_$arity$1;
} else {
return and__18061__auto__;
}
})()){
return coll.quantum$core$collections$core$CollRetrieve$butlast_PLUS_$arity$1(coll);
} else {
var x__18709__auto__ = (((coll == null))?null:coll);
return (function (){var or__18073__auto__ = (quantum.core.collections.core.butlast_PLUS_[goog.typeOf(x__18709__auto__)]);
if(or__18073__auto__){
return or__18073__auto__;
} else {
var or__18073__auto____$1 = (quantum.core.collections.core.butlast_PLUS_["_"]);
if(or__18073__auto____$1){
return or__18073__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"CollRetrieve.butlast+",coll);
}
}
})().call(null,coll);
}
});

quantum.core.collections.core.last_PLUS_ = (function quantum$core$collections$core$last_PLUS_(coll){
if((function (){var and__18061__auto__ = coll;
if(and__18061__auto__){
return coll.quantum$core$collections$core$CollRetrieve$last_PLUS_$arity$1;
} else {
return and__18061__auto__;
}
})()){
return coll.quantum$core$collections$core$CollRetrieve$last_PLUS_$arity$1(coll);
} else {
var x__18709__auto__ = (((coll == null))?null:coll);
return (function (){var or__18073__auto__ = (quantum.core.collections.core.last_PLUS_[goog.typeOf(x__18709__auto__)]);
if(or__18073__auto__){
return or__18073__auto__;
} else {
var or__18073__auto____$1 = (quantum.core.collections.core.last_PLUS_["_"]);
if(or__18073__auto____$1){
return or__18073__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"CollRetrieve.last+",coll);
}
}
})().call(null,coll);
}
});

cljs.core.Keyword.prototype.quantum$core$collections$core$CollRetrieve$ = true;

cljs.core.Keyword.prototype.quantum$core$collections$core$CollRetrieve$rest_PLUS_$arity$1 = (function (k){
var k__$1 = this;
return quantum.core.collections.core.rest_PLUS_.call(null,cljs.core.name.call(null,k__$1));
});

(quantum.core.collections.core.CollRetrieve["string"] = true);

(quantum.core.collections.core.first_PLUS_["string"] = (function (coll){
return cljs.core.subs.call(null,coll,(0),(1));
}));

(quantum.core.collections.core.second_PLUS_["string"] = (function (coll){
return cljs.core.subs.call(null,coll,(1),(2));
}));

(quantum.core.collections.core.rest_PLUS_["string"] = (function (coll){
return cljs.core.subs.call(null,coll,(1),quantum.core.collections.core.count_PLUS_.call(null,coll));
}));

(quantum.core.collections.core.butlast_PLUS_["string"] = (function (coll){
return cljs.core.subs.call(null,coll,(0),(quantum.core.collections.core.count_PLUS_.call(null,coll) - (1)));
}));

(quantum.core.collections.core.last_PLUS_["string"] = (function (coll){
return cljs.core.subs.call(null,coll,(quantum.core.collections.core.count_PLUS_.call(null,coll) - (1)));
}));

(quantum.core.collections.core.getr_PLUS_["string"] = (function (coll,a,b){
return quantum.core.string.subs_PLUS_.call(null,coll,a,((b - a) + (1)));
}));
cljs.core.Delay.prototype.quantum$core$collections$core$CollRetrieve$ = true;

cljs.core.Delay.prototype.quantum$core$collections$core$CollRetrieve$getr_PLUS_$arity$3 = (function (coll,a,b){
var coll__$1 = this;
return quantum.core.reducers.drop_PLUS_.call(null,a,quantum.core.reducers.take_PLUS_.call(null,b,coll__$1));
});

cljs.core.Delay.prototype.quantum$core$collections$core$CollRetrieve$first_PLUS_$arity$1 = (function (coll){
var coll__$1 = this;
return quantum.core.reducers.take_PLUS_.call(null,(1),coll__$1);
});

cljs.core.Delay.prototype.quantum$core$collections$core$CollRetrieve$rest_PLUS_$arity$1 = (function (coll){
var coll__$1 = this;
return quantum.core.reducers.drop_PLUS_.call(null,(1),coll__$1);
});
cljs.core.EmptyList.prototype.quantum$core$collections$core$CollRetrieve$ = true;

cljs.core.EmptyList.prototype.quantum$core$collections$core$CollRetrieve$first_PLUS_$arity$1 = (function (coll){
var coll__$1 = this;
return null;
});
(quantum.core.collections.core.CollRetrieve["_"] = true);

(quantum.core.collections.core.get_PLUS_["_"] = (function() {
var G__24825 = null;
var G__24825__2 = (function (obj,n){
return quantum.core.collections.core.get_PLUS_.call(null,obj,n,null);
});
var G__24825__3 = (function (obj,n,if_not_found){
return cljs.core.get.call(null,obj,n);
});
G__24825 = function(obj,n,if_not_found){
switch(arguments.length){
case 2:
return G__24825__2.call(this,obj,n);
case 3:
return G__24825__3.call(this,obj,n,if_not_found);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
G__24825.cljs$core$IFn$_invoke$arity$2 = G__24825__2;
G__24825.cljs$core$IFn$_invoke$arity$3 = G__24825__3;
return G__24825;
})()
);

(quantum.core.collections.core.first_PLUS_["_"] = (function (obj){
return obj;
}));

(quantum.core.collections.core.last_PLUS_["_"] = (function (obj){
return obj;
}));

(quantum.core.collections.core.butlast_PLUS_["_"] = (function (obj){
return obj;
}));

(quantum.core.collections.core.CollRetrieve["null"] = true);

(quantum.core.collections.core.get_PLUS_["null"] = (function (obj,n){
return obj;
}));

(quantum.core.collections.core.first_PLUS_["null"] = (function (obj){
return obj;
}));

(quantum.core.collections.core.last_PLUS_["null"] = (function (obj){
return obj;
}));

(quantum.core.collections.core.butlast_PLUS_["null"] = (function (obj){
return obj;
}));
quantum.core.collections.core.gets_PLUS_ = (function quantum$core$collections$core$gets_PLUS_(){
var argseq__19113__auto__ = ((((1) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(1)),(0))):null);
return quantum.core.collections.core.gets_PLUS_.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__19113__auto__);
});

quantum.core.collections.core.gets_PLUS_.cljs$core$IFn$_invoke$arity$variadic = (function (coll,indices){
if(cljs.core.truth_((function (){var and__18061__auto__ = quantum.core.type.editable_QMARK_.call(null,indices);
if(cljs.core.truth_(and__18061__auto__)){
return (cljs.core.counted_QMARK_.call(null,indices)) && ((cljs.core.count.call(null,indices) > quantum.core.type.transient_threshold));
} else {
return and__18061__auto__;
}
})())){
return cljs.core.persistent_BANG_.call(null,cljs.core.reduce.call(null,(function (ret,ind){
return cljs.core.conj_BANG_.call(null,ret,quantum.core.collections.core.get_PLUS_.call(null,coll,ind));
}),cljs.core.transient$.call(null,cljs.core.PersistentVector.EMPTY),indices));
} else {
return cljs.core.reduce.call(null,(function (ret,ind){
return cljs.core.conj.call(null,ret,quantum.core.collections.core.get_PLUS_.call(null,coll,ind));
}),cljs.core.PersistentVector.EMPTY,indices);
}
});

quantum.core.collections.core.gets_PLUS_.cljs$lang$maxFixedArity = (1);

quantum.core.collections.core.gets_PLUS_.cljs$lang$applyTo = (function (seq24826){
var G__24827 = cljs.core.first.call(null,seq24826);
var seq24826__$1 = cljs.core.next.call(null,seq24826);
return quantum.core.collections.core.gets_PLUS_.cljs$core$IFn$_invoke$arity$variadic(G__24827,seq24826__$1);
});
quantum.core.collections.core.pop_PLUS_ = quantum.core.collections.core.butlast_PLUS_;
quantum.core.collections.core.popr_PLUS_ = quantum.core.collections.core.butlast_PLUS_;
quantum.core.collections.core.popl_PLUS_ = quantum.core.collections.core.rest_PLUS_;
quantum.core.collections.core.peek_PLUS_ = quantum.core.collections.core.last_PLUS_;
quantum.core.collections.core.getf_PLUS_ = (function quantum$core$collections$core$getf_PLUS_(n){
return quantum.core.function$.f_STAR_n.call(null,quantum.core.collections.core.get_PLUS_,n);
});

quantum.core.collections.core.CollSearch = (function (){var obj24829 = {};
return obj24829;
})();

quantum.core.collections.core.index_of_PLUS_ = (function quantum$core$collections$core$index_of_PLUS_(coll,elem){
if((function (){var and__18061__auto__ = coll;
if(and__18061__auto__){
return coll.quantum$core$collections$core$CollSearch$index_of_PLUS_$arity$2;
} else {
return and__18061__auto__;
}
})()){
return coll.quantum$core$collections$core$CollSearch$index_of_PLUS_$arity$2(coll,elem);
} else {
var x__18709__auto__ = (((coll == null))?null:coll);
return (function (){var or__18073__auto__ = (quantum.core.collections.core.index_of_PLUS_[goog.typeOf(x__18709__auto__)]);
if(or__18073__auto__){
return or__18073__auto__;
} else {
var or__18073__auto____$1 = (quantum.core.collections.core.index_of_PLUS_["_"]);
if(or__18073__auto____$1){
return or__18073__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"CollSearch.index-of+",coll);
}
}
})().call(null,coll,elem);
}
});

quantum.core.collections.core.last_index_of_PLUS_ = (function quantum$core$collections$core$last_index_of_PLUS_(coll,elem){
if((function (){var and__18061__auto__ = coll;
if(and__18061__auto__){
return coll.quantum$core$collections$core$CollSearch$last_index_of_PLUS_$arity$2;
} else {
return and__18061__auto__;
}
})()){
return coll.quantum$core$collections$core$CollSearch$last_index_of_PLUS_$arity$2(coll,elem);
} else {
var x__18709__auto__ = (((coll == null))?null:coll);
return (function (){var or__18073__auto__ = (quantum.core.collections.core.last_index_of_PLUS_[goog.typeOf(x__18709__auto__)]);
if(or__18073__auto__){
return or__18073__auto__;
} else {
var or__18073__auto____$1 = (quantum.core.collections.core.last_index_of_PLUS_["_"]);
if(or__18073__auto____$1){
return or__18073__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"CollSearch.last-index-of+",coll);
}
}
})().call(null,coll,elem);
}
});

(quantum.core.collections.core.CollSearch["string"] = true);

(quantum.core.collections.core.index_of_PLUS_["string"] = (function (coll,elem){
return coll.indexOf(elem);
}));

(quantum.core.collections.core.last_index_of_PLUS_["string"] = (function (coll,elem){
return coll.lastIndexOf(elem);
}));
quantum.core.collections.core.third = (function quantum$core$collections$core$third(coll){
return quantum.core.collections.core.first_PLUS_.call(null,quantum.core.collections.core.rest_PLUS_.call(null,quantum.core.collections.core.rest_PLUS_.call(null,coll)));
});

quantum.core.collections.core.CollMod = (function (){var obj24834 = {};
return obj24834;
})();

quantum.core.collections.core.conjl_ = (function quantum$core$collections$core$conjl_(coll,args){
if((function (){var and__18061__auto__ = coll;
if(and__18061__auto__){
return coll.quantum$core$collections$core$CollMod$conjl_$arity$2;
} else {
return and__18061__auto__;
}
})()){
return coll.quantum$core$collections$core$CollMod$conjl_$arity$2(coll,args);
} else {
var x__18709__auto__ = (((coll == null))?null:coll);
return (function (){var or__18073__auto__ = (quantum.core.collections.core.conjl_[goog.typeOf(x__18709__auto__)]);
if(or__18073__auto__){
return or__18073__auto__;
} else {
var or__18073__auto____$1 = (quantum.core.collections.core.conjl_["_"]);
if(or__18073__auto____$1){
return or__18073__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"CollMod.conjl-",coll);
}
}
})().call(null,coll,args);
}
});

quantum.core.collections.core.conjr_ = (function quantum$core$collections$core$conjr_(coll,args){
if((function (){var and__18061__auto__ = coll;
if(and__18061__auto__){
return coll.quantum$core$collections$core$CollMod$conjr_$arity$2;
} else {
return and__18061__auto__;
}
})()){
return coll.quantum$core$collections$core$CollMod$conjr_$arity$2(coll,args);
} else {
var x__18709__auto__ = (((coll == null))?null:coll);
return (function (){var or__18073__auto__ = (quantum.core.collections.core.conjr_[goog.typeOf(x__18709__auto__)]);
if(or__18073__auto__){
return or__18073__auto__;
} else {
var or__18073__auto____$1 = (quantum.core.collections.core.conjr_["_"]);
if(or__18073__auto____$1){
return or__18073__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"CollMod.conjr-",coll);
}
}
})().call(null,coll,args);
}
});

quantum.core.collections.core.conjl_list = (function quantum$core$collections$core$conjl_list(){
var G__24836 = arguments.length;
switch (G__24836) {
case 2:
return quantum.core.collections.core.conjl_list.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return quantum.core.collections.core.conjl_list.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return quantum.core.collections.core.conjl_list.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
case 5:
return quantum.core.collections.core.conjl_list.cljs$core$IFn$_invoke$arity$5((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]));

break;
case 6:
return quantum.core.collections.core.conjl_list.cljs$core$IFn$_invoke$arity$6((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]),(arguments[(5)]));

break;
case 7:
return quantum.core.collections.core.conjl_list.cljs$core$IFn$_invoke$arity$7((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]),(arguments[(5)]),(arguments[(6)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.collections.core.conjl_list.cljs$core$IFn$_invoke$arity$2 = (function (coll,a){
return cljs.core.cons.call(null,a,coll);
});

quantum.core.collections.core.conjl_list.cljs$core$IFn$_invoke$arity$3 = (function (coll,a,b){
return cljs.core.cons.call(null,a,cljs.core.cons.call(null,b,coll));
});

quantum.core.collections.core.conjl_list.cljs$core$IFn$_invoke$arity$4 = (function (coll,a,b,c){
return cljs.core.cons.call(null,a,cljs.core.cons.call(null,b,cljs.core.cons.call(null,c,coll)));
});

quantum.core.collections.core.conjl_list.cljs$core$IFn$_invoke$arity$5 = (function (coll,a,b,c,d){
return cljs.core.cons.call(null,a,cljs.core.cons.call(null,b,cljs.core.cons.call(null,c,cljs.core.cons.call(null,d,coll))));
});

quantum.core.collections.core.conjl_list.cljs$core$IFn$_invoke$arity$6 = (function (coll,a,b,c,d,e){
return cljs.core.cons.call(null,a,cljs.core.cons.call(null,b,cljs.core.cons.call(null,c,cljs.core.cons.call(null,d,cljs.core.cons.call(null,e,coll)))));
});

quantum.core.collections.core.conjl_list.cljs$core$IFn$_invoke$arity$7 = (function (coll,a,b,c,d,e,f){
return cljs.core.cons.call(null,a,cljs.core.cons.call(null,b,cljs.core.cons.call(null,c,cljs.core.cons.call(null,d,cljs.core.cons.call(null,e,cljs.core.cons.call(null,f,coll))))));
});

quantum.core.collections.core.conjl_list.cljs$lang$maxFixedArity = 7;
quantum.core.collections.core.conjl_vec = (function quantum$core$collections$core$conjl_vec(){
var G__24847 = arguments.length;
switch (G__24847) {
case 2:
return quantum.core.collections.core.conjl_vec.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return quantum.core.collections.core.conjl_vec.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return quantum.core.collections.core.conjl_vec.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
case 5:
return quantum.core.collections.core.conjl_vec.cljs$core$IFn$_invoke$arity$5((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]));

break;
case 6:
return quantum.core.collections.core.conjl_vec.cljs$core$IFn$_invoke$arity$6((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]),(arguments[(5)]));

break;
case 7:
return quantum.core.collections.core.conjl_vec.cljs$core$IFn$_invoke$arity$7((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]),(arguments[(5)]),(arguments[(6)]));

break;
default:
var argseq__19124__auto__ = (new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(7)),(0)));
return quantum.core.collections.core.conjl_vec.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]),(arguments[(5)]),(arguments[(6)]),argseq__19124__auto__);

}
});

quantum.core.collections.core.conjl_vec.cljs$core$IFn$_invoke$arity$2 = (function (coll,a){
return quantum.core.data.vector.catvec.call(null,quantum.core.data.vector.vector_PLUS_.call(null,a),coll);
});

quantum.core.collections.core.conjl_vec.cljs$core$IFn$_invoke$arity$3 = (function (coll,a,b){
return quantum.core.data.vector.catvec.call(null,quantum.core.data.vector.vector_PLUS_.call(null,a,b),coll);
});

quantum.core.collections.core.conjl_vec.cljs$core$IFn$_invoke$arity$4 = (function (coll,a,b,c){
return quantum.core.data.vector.catvec.call(null,quantum.core.data.vector.vector_PLUS_.call(null,a,b,c),coll);
});

quantum.core.collections.core.conjl_vec.cljs$core$IFn$_invoke$arity$5 = (function (coll,a,b,c,d){
return quantum.core.data.vector.catvec.call(null,quantum.core.data.vector.vector_PLUS_.call(null,a,b,c,d),coll);
});

quantum.core.collections.core.conjl_vec.cljs$core$IFn$_invoke$arity$6 = (function (coll,a,b,c,d,e){
return quantum.core.data.vector.catvec.call(null,quantum.core.data.vector.vector_PLUS_.call(null,a,b,c,d,e),coll);
});

quantum.core.collections.core.conjl_vec.cljs$core$IFn$_invoke$arity$7 = (function (coll,a,b,c,d,e,f){
return quantum.core.data.vector.catvec.call(null,quantum.core.data.vector.vector_PLUS_.call(null,a,b,c,d,e,f),coll);
});

quantum.core.collections.core.conjl_vec.cljs$core$IFn$_invoke$arity$variadic = (function (coll,a,b,c,d,e,f,args){
return quantum.core.data.vector.catvec.call(null,cljs.core.apply.call(null,quantum.core.data.vector.vector_PLUS_,args),coll);
});

quantum.core.collections.core.conjl_vec.cljs$lang$applyTo = (function (seq24838){
var G__24839 = cljs.core.first.call(null,seq24838);
var seq24838__$1 = cljs.core.next.call(null,seq24838);
var G__24840 = cljs.core.first.call(null,seq24838__$1);
var seq24838__$2 = cljs.core.next.call(null,seq24838__$1);
var G__24841 = cljs.core.first.call(null,seq24838__$2);
var seq24838__$3 = cljs.core.next.call(null,seq24838__$2);
var G__24842 = cljs.core.first.call(null,seq24838__$3);
var seq24838__$4 = cljs.core.next.call(null,seq24838__$3);
var G__24843 = cljs.core.first.call(null,seq24838__$4);
var seq24838__$5 = cljs.core.next.call(null,seq24838__$4);
var G__24844 = cljs.core.first.call(null,seq24838__$5);
var seq24838__$6 = cljs.core.next.call(null,seq24838__$5);
var G__24845 = cljs.core.first.call(null,seq24838__$6);
var seq24838__$7 = cljs.core.next.call(null,seq24838__$6);
return quantum.core.collections.core.conjl_vec.cljs$core$IFn$_invoke$arity$variadic(G__24839,G__24840,G__24841,G__24842,G__24843,G__24844,G__24845,seq24838__$7);
});

quantum.core.collections.core.conjl_vec.cljs$lang$maxFixedArity = (7);
/**
 * @param {...*} var_args
 */
quantum.core.collections.core.conjl = (function() { 
var quantum$core$collections$core$conjl__delegate = function (coll,args){
return quantum.core.collections.core.conjr_.call(null,coll,args);
};
var quantum$core$collections$core$conjl = function (coll,var_args){
var args = null;
if (arguments.length > 1) {
var G__24849__i = 0, G__24849__a = new Array(arguments.length -  1);
while (G__24849__i < G__24849__a.length) {G__24849__a[G__24849__i] = arguments[G__24849__i + 1]; ++G__24849__i;}
  args = new cljs.core.IndexedSeq(G__24849__a,0);
} 
return quantum$core$collections$core$conjl__delegate.call(this,coll,args);};
quantum$core$collections$core$conjl.cljs$lang$maxFixedArity = 1;
quantum$core$collections$core$conjl.cljs$lang$applyTo = (function (arglist__24850){
var coll = cljs.core.first(arglist__24850);
var args = cljs.core.rest(arglist__24850);
return quantum$core$collections$core$conjl__delegate(coll,args);
});
quantum$core$collections$core$conjl.cljs$core$IFn$_invoke$arity$variadic = quantum$core$collections$core$conjl__delegate;
return quantum$core$collections$core$conjl;
})()
;
/**
 * @param {...*} var_args
 */
quantum.core.collections.core.conjr = (function() { 
var quantum$core$collections$core$conjr__delegate = function (coll,args){
return quantum.core.collections.core.conjl_.call(null,coll,args);
};
var quantum$core$collections$core$conjr = function (coll,var_args){
var args = null;
if (arguments.length > 1) {
var G__24851__i = 0, G__24851__a = new Array(arguments.length -  1);
while (G__24851__i < G__24851__a.length) {G__24851__a[G__24851__i] = arguments[G__24851__i + 1]; ++G__24851__i;}
  args = new cljs.core.IndexedSeq(G__24851__a,0);
} 
return quantum$core$collections$core$conjr__delegate.call(this,coll,args);};
quantum$core$collections$core$conjr.cljs$lang$maxFixedArity = 1;
quantum$core$collections$core$conjr.cljs$lang$applyTo = (function (arglist__24852){
var coll = cljs.core.first(arglist__24852);
var args = cljs.core.rest(arglist__24852);
return quantum$core$collections$core$conjr__delegate(coll,args);
});
quantum$core$collections$core$conjr.cljs$core$IFn$_invoke$arity$variadic = quantum$core$collections$core$conjr__delegate;
return quantum$core$collections$core$conjr;
})()
;
quantum.core.collections.core.doto_BANG_ = cljs.core.swap_BANG_;

//# sourceMappingURL=core.js.map?rel=1431625570931