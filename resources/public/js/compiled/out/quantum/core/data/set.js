// Compiled by ClojureScript 0.0-3269 {}
goog.provide('quantum.core.data.set');
goog.require('cljs.core');
goog.require('quantum.core.ns');
goog.require('clojure.set');
goog.require('clojure.data.avl');
quantum.core.data.set.union = clojure.set.union;
quantum.core.data.set.intersection = clojure.set.intersection;
quantum.core.data.set.difference = clojure.set.difference;
quantum.core.data.set.sorted_set_PLUS_ = clojure.data.avl.sorted_set;
quantum.core.data.set.sorted_set_by_PLUS_ = clojure.data.avl.sorted_set_by;
quantum.core.data.set.xset_QMARK_ = (function quantum$core$data$set$xset_QMARK_(fn_key,set1,set2){
var funcs = (function (){var G__23076 = (((fn_key instanceof cljs.core.Keyword))?fn_key.fqn:null);
switch (G__23076) {
case "sub":
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"eq","eq",-618539067),cljs.core._LT__EQ_,new cljs.core.Keyword(null,"fn","fn",-1175266204),((function (G__23076){
return (function (p1__23068_SHARP_,p2__23067_SHARP_){
return (new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[cljs.core.partial.call(null,cljs.core.contains_QMARK_,p2__23067_SHARP_),p1__23068_SHARP_],null));
});})(G__23076))
], null);

break;
case "super":
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"eq","eq",-618539067),cljs.core._GT__EQ_,new cljs.core.Keyword(null,"fn","fn",-1175266204),((function (G__23076){
return (function (p1__23069_SHARP_,p2__23070_SHARP_){
return (new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[cljs.core.partial.call(null,cljs.core.contains_QMARK_,p1__23069_SHARP_),p2__23070_SHARP_],null));
});})(G__23076))
], null);

break;
case "proper-sub":
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"eq","eq",-618539067),cljs.core._LT_,new cljs.core.Keyword(null,"fn","fn",-1175266204),((function (G__23076){
return (function (p1__23072_SHARP_,p2__23071_SHARP_){
return (new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[p2__23071_SHARP_,p1__23072_SHARP_],null));
});})(G__23076))
], null);

break;
case "proper-super":
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"eq","eq",-618539067),cljs.core._GT_,new cljs.core.Keyword(null,"fn","fn",-1175266204),((function (G__23076){
return (function (p1__23073_SHARP_,p2__23074_SHARP_){
return (new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[p1__23073_SHARP_,p2__23074_SHARP_],null));
});})(G__23076))
], null);

break;
default:
throw (new Error([cljs.core.str("No matching clause: "),cljs.core.str(fn_key)].join('')));

}
})();
var and__18061__auto__ = new cljs.core.Keyword(null,"eq","eq",-618539067).cljs$core$IFn$_invoke$arity$1(funcs).call(null,cljs.core.count.call(null,set1),cljs.core.count.call(null,set2));
if(cljs.core.truth_(and__18061__auto__)){
return cljs.core.apply.call(null,cljs.core.every_QMARK_,new cljs.core.Keyword(null,"fn","fn",-1175266204).cljs$core$IFn$_invoke$arity$1(funcs).call(null,set1,set2));
} else {
return and__18061__auto__;
}
});
quantum.core.data.set.subset_QMARK_ = (function quantum$core$data$set$subset_QMARK_(p1__23078_SHARP_,p2__23079_SHARP_){
return quantum.core.data.set.xset_QMARK_.call(null,new cljs.core.Keyword(null,"sub","sub",-2093760025),p1__23078_SHARP_,p2__23079_SHARP_);
});
quantum.core.data.set.superset_QMARK_ = (function quantum$core$data$set$superset_QMARK_(p1__23080_SHARP_,p2__23081_SHARP_){
return quantum.core.data.set.xset_QMARK_.call(null,new cljs.core.Keyword(null,"super","super",840752938),p1__23080_SHARP_,p2__23081_SHARP_);
});
quantum.core.data.set.proper_subset_QMARK_ = (function quantum$core$data$set$proper_subset_QMARK_(p1__23082_SHARP_,p2__23083_SHARP_){
return quantum.core.data.set.xset_QMARK_.call(null,new cljs.core.Keyword(null,"proper-sub","proper-sub",-904442091),p1__23082_SHARP_,p2__23083_SHARP_);
});
quantum.core.data.set.proper_superset_QMARK_ = (function quantum$core$data$set$proper_superset_QMARK_(p1__23084_SHARP_,p2__23085_SHARP_){
return quantum.core.data.set.xset_QMARK_.call(null,new cljs.core.Keyword(null,"proper-super","proper-super",-1070696755),p1__23084_SHARP_,p2__23085_SHARP_);
});

//# sourceMappingURL=set.js.map?rel=1431625568738