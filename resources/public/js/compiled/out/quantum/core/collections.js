// Compiled by ClojureScript 0.0-3269 {}
goog.provide('quantum.core.collections');
goog.require('cljs.core');
goog.require('quantum.core.data.set');
goog.require('quantum.core.logic');
goog.require('quantum.core.log');
goog.require('quantum.core.data.map');
goog.require('quantum.core.type');
goog.require('quantum.core.numeric');
goog.require('cljs.core.async');
goog.require('quantum.core.function$');
goog.require('clojure.walk');
goog.require('quantum.core.collections.core');
goog.require('quantum.core.data.vector');
goog.require('quantum.core.ns');
goog.require('quantum.core.error');
goog.require('quantum.core.loops');
goog.require('quantum.core.reducers');
goog.require('quantum.core.string');
var orig_var__20471__auto___30612 = new cljs.core.Var(function(){return quantum.core.reducers.vec_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","vec+","quantum.core.reducers/vec+",820726505,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"vec+","vec+",-828728092,null),"src/cljc/quantum/core/reducers.cljc",10,1,605,605,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.reducers.vec_PLUS_)?quantum.core.reducers.vec_PLUS_.cljs$lang$test:null)]));
if(cljs.core.truth_(orig_var__20471__auto___30612.hasRoot())){
quantum.core.collections.vec = cljs.core.with_meta.call(null,cljs.core.deref.call(null,new cljs.core.Var(function(){return quantum.core.reducers.vec_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","vec+","quantum.core.reducers/vec+",820726505,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"vec+","vec+",-828728092,null),"src/cljc/quantum/core/reducers.cljc",10,1,605,605,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.reducers.vec_PLUS_)?quantum.core.reducers.vec_PLUS_.cljs$lang$test:null)]))),cljs.core.meta.call(null,new cljs.core.Var(function(){return quantum.core.reducers.vec_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","vec+","quantum.core.reducers/vec+",820726505,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"vec+","vec+",-828728092,null),"src/cljc/quantum/core/reducers.cljc",10,1,605,605,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.reducers.vec_PLUS_)?quantum.core.reducers.vec_PLUS_.cljs$lang$test:null)]))));

if(new cljs.core.Keyword(null,"macro","macro",-867863404).cljs$core$IFn$_invoke$arity$1(cljs.core.meta.call(null,orig_var__20471__auto___30612)) === true){
cljs.core.alter_meta_BANG_.call(null,new cljs.core.Var(function(){return quantum.core.collections.vec;},new cljs.core.Symbol("quantum.core.collections","vec","quantum.core.collections/vec",-1650511979,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"vec","vec",982683596,null),"src/cljc/quantum/core/collections.cljc",14,1,81,81,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.vec)?quantum.core.collections.vec.cljs$lang$test:null)])),cljs.core.assoc,new cljs.core.Keyword(null,"macro","macro",-867863404),true);
} else {
}
} else {
}

new cljs.core.Var(function(){return quantum.core.collections.vec;},new cljs.core.Symbol("quantum.core.collections","vec","quantum.core.collections/vec",-1650511979,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"vec","vec",982683596,null),"src/cljc/quantum/core/collections.cljc",14,1,81,81,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.vec)?quantum.core.collections.vec.cljs$lang$test:null)]));
var orig_var__20471__auto___30613 = new cljs.core.Var(function(){return quantum.core.reducers.into_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","into+","quantum.core.reducers/into+",382873519,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"top-fn","top-fn",-2056129173),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"todo","todo",-1046442570),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"into+","into+",283146666,null),"src/cljc/quantum/core/reducers.cljc",12,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"variadic","variadic",882626057),true,new cljs.core.Keyword(null,"max-fixed-arity","max-fixed-arity",-690205543),(2),new cljs.core.Keyword(null,"method-params","method-params",-980792179),cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"to","to",1832630534,null),new cljs.core.Symbol(null,"from","from",-839142725,null)], null)),new cljs.core.Keyword(null,"arglists","arglists",1661989754),cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"to","to",1832630534,null),new cljs.core.Symbol(null,"from","from",-839142725,null)], null),new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"to","to",1832630534,null),new cljs.core.Symbol(null,"from","from",-839142725,null),new cljs.core.Symbol(null,"&","&",-2144855648,null),new cljs.core.Symbol(null,"froms","froms",-351266080,null)], null)),new cljs.core.Keyword(null,"arglists-meta","arglists-meta",1944829838),cljs.core.list(null,null)], null),1,393,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Add extra arities"], null),393,cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"to","to",1832630534,null),new cljs.core.Symbol(null,"from","from",-839142725,null)], null),new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"to","to",1832630534,null),new cljs.core.Symbol(null,"from","from",-839142725,null),new cljs.core.Symbol(null,"&","&",-2144855648,null),new cljs.core.Symbol(null,"froms","froms",-351266080,null)], null)),null,(cljs.core.truth_(quantum.core.reducers.into_PLUS_)?quantum.core.reducers.into_PLUS_.cljs$lang$test:null)]));
if(cljs.core.truth_(orig_var__20471__auto___30613.hasRoot())){
quantum.core.collections.into = cljs.core.with_meta.call(null,cljs.core.deref.call(null,new cljs.core.Var(function(){return quantum.core.reducers.into_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","into+","quantum.core.reducers/into+",382873519,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"top-fn","top-fn",-2056129173),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"todo","todo",-1046442570),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"into+","into+",283146666,null),"src/cljc/quantum/core/reducers.cljc",12,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"variadic","variadic",882626057),true,new cljs.core.Keyword(null,"max-fixed-arity","max-fixed-arity",-690205543),(2),new cljs.core.Keyword(null,"method-params","method-params",-980792179),cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"to","to",1832630534,null),new cljs.core.Symbol(null,"from","from",-839142725,null)], null)),new cljs.core.Keyword(null,"arglists","arglists",1661989754),cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"to","to",1832630534,null),new cljs.core.Symbol(null,"from","from",-839142725,null)], null),new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"to","to",1832630534,null),new cljs.core.Symbol(null,"from","from",-839142725,null),new cljs.core.Symbol(null,"&","&",-2144855648,null),new cljs.core.Symbol(null,"froms","froms",-351266080,null)], null)),new cljs.core.Keyword(null,"arglists-meta","arglists-meta",1944829838),cljs.core.list(null,null)], null),1,393,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Add extra arities"], null),393,cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"to","to",1832630534,null),new cljs.core.Symbol(null,"from","from",-839142725,null)], null),new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"to","to",1832630534,null),new cljs.core.Symbol(null,"from","from",-839142725,null),new cljs.core.Symbol(null,"&","&",-2144855648,null),new cljs.core.Symbol(null,"froms","froms",-351266080,null)], null)),null,(cljs.core.truth_(quantum.core.reducers.into_PLUS_)?quantum.core.reducers.into_PLUS_.cljs$lang$test:null)]))),cljs.core.meta.call(null,new cljs.core.Var(function(){return quantum.core.reducers.into_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","into+","quantum.core.reducers/into+",382873519,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"top-fn","top-fn",-2056129173),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"todo","todo",-1046442570),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"into+","into+",283146666,null),"src/cljc/quantum/core/reducers.cljc",12,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"variadic","variadic",882626057),true,new cljs.core.Keyword(null,"max-fixed-arity","max-fixed-arity",-690205543),(2),new cljs.core.Keyword(null,"method-params","method-params",-980792179),cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"to","to",1832630534,null),new cljs.core.Symbol(null,"from","from",-839142725,null)], null)),new cljs.core.Keyword(null,"arglists","arglists",1661989754),cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"to","to",1832630534,null),new cljs.core.Symbol(null,"from","from",-839142725,null)], null),new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"to","to",1832630534,null),new cljs.core.Symbol(null,"from","from",-839142725,null),new cljs.core.Symbol(null,"&","&",-2144855648,null),new cljs.core.Symbol(null,"froms","froms",-351266080,null)], null)),new cljs.core.Keyword(null,"arglists-meta","arglists-meta",1944829838),cljs.core.list(null,null)], null),1,393,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Add extra arities"], null),393,cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"to","to",1832630534,null),new cljs.core.Symbol(null,"from","from",-839142725,null)], null),new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"to","to",1832630534,null),new cljs.core.Symbol(null,"from","from",-839142725,null),new cljs.core.Symbol(null,"&","&",-2144855648,null),new cljs.core.Symbol(null,"froms","froms",-351266080,null)], null)),null,(cljs.core.truth_(quantum.core.reducers.into_PLUS_)?quantum.core.reducers.into_PLUS_.cljs$lang$test:null)]))));

if(new cljs.core.Keyword(null,"macro","macro",-867863404).cljs$core$IFn$_invoke$arity$1(cljs.core.meta.call(null,orig_var__20471__auto___30613)) === true){
cljs.core.alter_meta_BANG_.call(null,new cljs.core.Var(function(){return quantum.core.collections.into;},new cljs.core.Symbol("quantum.core.collections","into","quantum.core.collections/into",-70823101,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"into","into",1489695498,null),"src/cljc/quantum/core/collections.cljc",15,1,82,82,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.into)?quantum.core.collections.into.cljs$lang$test:null)])),cljs.core.assoc,new cljs.core.Keyword(null,"macro","macro",-867863404),true);
} else {
}
} else {
}

new cljs.core.Var(function(){return quantum.core.collections.into;},new cljs.core.Symbol("quantum.core.collections","into","quantum.core.collections/into",-70823101,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"into","into",1489695498,null),"src/cljc/quantum/core/collections.cljc",15,1,82,82,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.into)?quantum.core.collections.into.cljs$lang$test:null)]));
var orig_var__20471__auto___30614 = new cljs.core.Var(function(){return quantum.core.reducers.reduce;},new cljs.core.Symbol("quantum.core.reducers","reduce","quantum.core.reducers/reduce",1454172239,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"top-fn","top-fn",-2056129173),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"attribution","attribution",1937239286),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"reduce","reduce",1358839360,null),"src/cljc/quantum/core/reducers.cljc",13,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"variadic","variadic",882626057),false,new cljs.core.Keyword(null,"max-fixed-arity","max-fixed-arity",-690205543),3,new cljs.core.Keyword(null,"method-params","method-params",-980792179),cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"f","f",43394975,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"f","f",43394975,null),new cljs.core.Symbol(null,"init","init",-234949907,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),new cljs.core.Keyword(null,"arglists","arglists",1661989754),cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"f","f",43394975,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"f","f",43394975,null),new cljs.core.Symbol(null,"init","init",-234949907,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),new cljs.core.Keyword(null,"arglists-meta","arglists-meta",1944829838),cljs.core.list(null,null)], null),1,374,"Alex Gunnarson",374,cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"f","f",43394975,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"f","f",43394975,null),new cljs.core.Symbol(null,"init","init",-234949907,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),"Like |core/reduce| except:\n   When init is not provided, (f) is used.\n   Maps are reduced with reduce-kv.\n\n   Entry point for internal reduce (in order to switch the args\n   around to dispatch on type).",(cljs.core.truth_(quantum.core.reducers.reduce)?quantum.core.reducers.reduce.cljs$lang$test:null)]));
if(cljs.core.truth_(orig_var__20471__auto___30614.hasRoot())){
quantum.core.collections.reduce = cljs.core.with_meta.call(null,cljs.core.deref.call(null,new cljs.core.Var(function(){return quantum.core.reducers.reduce;},new cljs.core.Symbol("quantum.core.reducers","reduce","quantum.core.reducers/reduce",1454172239,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"top-fn","top-fn",-2056129173),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"attribution","attribution",1937239286),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"reduce","reduce",1358839360,null),"src/cljc/quantum/core/reducers.cljc",13,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"variadic","variadic",882626057),false,new cljs.core.Keyword(null,"max-fixed-arity","max-fixed-arity",-690205543),3,new cljs.core.Keyword(null,"method-params","method-params",-980792179),cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"f","f",43394975,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"f","f",43394975,null),new cljs.core.Symbol(null,"init","init",-234949907,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),new cljs.core.Keyword(null,"arglists","arglists",1661989754),cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"f","f",43394975,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"f","f",43394975,null),new cljs.core.Symbol(null,"init","init",-234949907,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),new cljs.core.Keyword(null,"arglists-meta","arglists-meta",1944829838),cljs.core.list(null,null)], null),1,374,"Alex Gunnarson",374,cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"f","f",43394975,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"f","f",43394975,null),new cljs.core.Symbol(null,"init","init",-234949907,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),"Like |core/reduce| except:\n   When init is not provided, (f) is used.\n   Maps are reduced with reduce-kv.\n\n   Entry point for internal reduce (in order to switch the args\n   around to dispatch on type).",(cljs.core.truth_(quantum.core.reducers.reduce)?quantum.core.reducers.reduce.cljs$lang$test:null)]))),cljs.core.meta.call(null,new cljs.core.Var(function(){return quantum.core.reducers.reduce;},new cljs.core.Symbol("quantum.core.reducers","reduce","quantum.core.reducers/reduce",1454172239,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"top-fn","top-fn",-2056129173),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"attribution","attribution",1937239286),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"reduce","reduce",1358839360,null),"src/cljc/quantum/core/reducers.cljc",13,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"variadic","variadic",882626057),false,new cljs.core.Keyword(null,"max-fixed-arity","max-fixed-arity",-690205543),3,new cljs.core.Keyword(null,"method-params","method-params",-980792179),cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"f","f",43394975,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"f","f",43394975,null),new cljs.core.Symbol(null,"init","init",-234949907,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),new cljs.core.Keyword(null,"arglists","arglists",1661989754),cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"f","f",43394975,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"f","f",43394975,null),new cljs.core.Symbol(null,"init","init",-234949907,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),new cljs.core.Keyword(null,"arglists-meta","arglists-meta",1944829838),cljs.core.list(null,null)], null),1,374,"Alex Gunnarson",374,cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"f","f",43394975,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"f","f",43394975,null),new cljs.core.Symbol(null,"init","init",-234949907,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),"Like |core/reduce| except:\n   When init is not provided, (f) is used.\n   Maps are reduced with reduce-kv.\n\n   Entry point for internal reduce (in order to switch the args\n   around to dispatch on type).",(cljs.core.truth_(quantum.core.reducers.reduce)?quantum.core.reducers.reduce.cljs$lang$test:null)]))));

if(new cljs.core.Keyword(null,"macro","macro",-867863404).cljs$core$IFn$_invoke$arity$1(cljs.core.meta.call(null,orig_var__20471__auto___30614)) === true){
cljs.core.alter_meta_BANG_.call(null,new cljs.core.Var(function(){return quantum.core.collections.reduce;},new cljs.core.Symbol("quantum.core.collections","reduce","quantum.core.collections/reduce",-1941991,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"reduce","reduce",1358839360,null),"src/cljc/quantum/core/collections.cljc",17,1,83,83,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.reduce)?quantum.core.collections.reduce.cljs$lang$test:null)])),cljs.core.assoc,new cljs.core.Keyword(null,"macro","macro",-867863404),true);
} else {
}
} else {
}

new cljs.core.Var(function(){return quantum.core.collections.reduce;},new cljs.core.Symbol("quantum.core.collections","reduce","quantum.core.collections/reduce",-1941991,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"reduce","reduce",1358839360,null),"src/cljc/quantum/core/collections.cljc",17,1,83,83,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.reduce)?quantum.core.collections.reduce.cljs$lang$test:null)]));
var orig_var__20471__auto___30615 = new cljs.core.Var(function(){return quantum.core.reducers.fold_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","fold+","quantum.core.reducers/fold+",503964630,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"added","added",2057651688),new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"top-fn","top-fn",-2056129173),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"contributors","contributors",-1706452523),new cljs.core.Keyword(null,"attribution","attribution",1937239286),new cljs.core.Keyword(null,"todo","todo",-1046442570),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],["1.5",new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"fold+","fold+",272338387,null),"src/cljc/quantum/core/reducers.cljc",12,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"variadic","variadic",882626057),false,new cljs.core.Keyword(null,"max-fixed-arity","max-fixed-arity",-690205543),4,new cljs.core.Keyword(null,"method-params","method-params",-980792179),cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"obj","obj",-1672671807,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"combinef","combinef",2095886860,null),new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"n","n",-2092305744,null),new cljs.core.Symbol(null,"combinef","combinef",2095886860,null),new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),new cljs.core.Keyword(null,"arglists","arglists",1661989754),cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"obj","obj",-1672671807,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"combinef","combinef",2095886860,null),new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"n","n",-2092305744,null),new cljs.core.Symbol(null,"combinef","combinef",2095886860,null),new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),new cljs.core.Keyword(null,"arglists-meta","arglists-meta",1944829838),cljs.core.list(null,null,null,null)], null),1,612,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Alex Gunnarson"], null),"clojure.core.reducers",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Make more efficient.","So many fns created in this lead to inefficiency."], null),612,cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"obj","obj",-1672671807,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"combinef","combinef",2095886860,null),new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"n","n",-2092305744,null),new cljs.core.Symbol(null,"combinef","combinef",2095886860,null),new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),"Reduces a collection using a (potentially parallel) reduce-combine\n  strategy. The collection is partitioned into groups of approximately\n  n (default 512), each of which is reduced with reducef (with a seed\n  value obtained by calling (combinef) with no arguments). The results\n  of these reductions are then reduced with combinef (default\n  reducef).\n  @combinef must be associative. When called with no\n  arguments, (combinef) must produce its identity element.\n  These operations may be performed in parallel, but the results will preserve order.",(cljs.core.truth_(quantum.core.reducers.fold_PLUS_)?quantum.core.reducers.fold_PLUS_.cljs$lang$test:null)]));
if(cljs.core.truth_(orig_var__20471__auto___30615.hasRoot())){
quantum.core.collections.redv = cljs.core.with_meta.call(null,cljs.core.deref.call(null,new cljs.core.Var(function(){return quantum.core.reducers.fold_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","fold+","quantum.core.reducers/fold+",503964630,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"added","added",2057651688),new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"top-fn","top-fn",-2056129173),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"contributors","contributors",-1706452523),new cljs.core.Keyword(null,"attribution","attribution",1937239286),new cljs.core.Keyword(null,"todo","todo",-1046442570),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],["1.5",new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"fold+","fold+",272338387,null),"src/cljc/quantum/core/reducers.cljc",12,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"variadic","variadic",882626057),false,new cljs.core.Keyword(null,"max-fixed-arity","max-fixed-arity",-690205543),4,new cljs.core.Keyword(null,"method-params","method-params",-980792179),cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"obj","obj",-1672671807,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"combinef","combinef",2095886860,null),new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"n","n",-2092305744,null),new cljs.core.Symbol(null,"combinef","combinef",2095886860,null),new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),new cljs.core.Keyword(null,"arglists","arglists",1661989754),cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"obj","obj",-1672671807,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"combinef","combinef",2095886860,null),new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"n","n",-2092305744,null),new cljs.core.Symbol(null,"combinef","combinef",2095886860,null),new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),new cljs.core.Keyword(null,"arglists-meta","arglists-meta",1944829838),cljs.core.list(null,null,null,null)], null),1,612,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Alex Gunnarson"], null),"clojure.core.reducers",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Make more efficient.","So many fns created in this lead to inefficiency."], null),612,cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"obj","obj",-1672671807,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"combinef","combinef",2095886860,null),new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"n","n",-2092305744,null),new cljs.core.Symbol(null,"combinef","combinef",2095886860,null),new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),"Reduces a collection using a (potentially parallel) reduce-combine\n  strategy. The collection is partitioned into groups of approximately\n  n (default 512), each of which is reduced with reducef (with a seed\n  value obtained by calling (combinef) with no arguments). The results\n  of these reductions are then reduced with combinef (default\n  reducef).\n  @combinef must be associative. When called with no\n  arguments, (combinef) must produce its identity element.\n  These operations may be performed in parallel, but the results will preserve order.",(cljs.core.truth_(quantum.core.reducers.fold_PLUS_)?quantum.core.reducers.fold_PLUS_.cljs$lang$test:null)]))),cljs.core.meta.call(null,new cljs.core.Var(function(){return quantum.core.reducers.fold_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","fold+","quantum.core.reducers/fold+",503964630,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"added","added",2057651688),new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"top-fn","top-fn",-2056129173),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"contributors","contributors",-1706452523),new cljs.core.Keyword(null,"attribution","attribution",1937239286),new cljs.core.Keyword(null,"todo","todo",-1046442570),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],["1.5",new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"fold+","fold+",272338387,null),"src/cljc/quantum/core/reducers.cljc",12,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"variadic","variadic",882626057),false,new cljs.core.Keyword(null,"max-fixed-arity","max-fixed-arity",-690205543),4,new cljs.core.Keyword(null,"method-params","method-params",-980792179),cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"obj","obj",-1672671807,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"combinef","combinef",2095886860,null),new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"n","n",-2092305744,null),new cljs.core.Symbol(null,"combinef","combinef",2095886860,null),new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),new cljs.core.Keyword(null,"arglists","arglists",1661989754),cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"obj","obj",-1672671807,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"combinef","combinef",2095886860,null),new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"n","n",-2092305744,null),new cljs.core.Symbol(null,"combinef","combinef",2095886860,null),new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),new cljs.core.Keyword(null,"arglists-meta","arglists-meta",1944829838),cljs.core.list(null,null,null,null)], null),1,612,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Alex Gunnarson"], null),"clojure.core.reducers",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Make more efficient.","So many fns created in this lead to inefficiency."], null),612,cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"obj","obj",-1672671807,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"combinef","combinef",2095886860,null),new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"n","n",-2092305744,null),new cljs.core.Symbol(null,"combinef","combinef",2095886860,null),new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),"Reduces a collection using a (potentially parallel) reduce-combine\n  strategy. The collection is partitioned into groups of approximately\n  n (default 512), each of which is reduced with reducef (with a seed\n  value obtained by calling (combinef) with no arguments). The results\n  of these reductions are then reduced with combinef (default\n  reducef).\n  @combinef must be associative. When called with no\n  arguments, (combinef) must produce its identity element.\n  These operations may be performed in parallel, but the results will preserve order.",(cljs.core.truth_(quantum.core.reducers.fold_PLUS_)?quantum.core.reducers.fold_PLUS_.cljs$lang$test:null)]))));

if(new cljs.core.Keyword(null,"macro","macro",-867863404).cljs$core$IFn$_invoke$arity$1(cljs.core.meta.call(null,orig_var__20471__auto___30615)) === true){
cljs.core.alter_meta_BANG_.call(null,new cljs.core.Var(function(){return quantum.core.collections.redv;},new cljs.core.Symbol("quantum.core.collections","redv","quantum.core.collections/redv",-463592330,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"redv","redv",1230668989,null),"src/cljc/quantum/core/collections.cljc",15,1,85,85,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.redv)?quantum.core.collections.redv.cljs$lang$test:null)])),cljs.core.assoc,new cljs.core.Keyword(null,"macro","macro",-867863404),true);
} else {
}
} else {
}

new cljs.core.Var(function(){return quantum.core.collections.redv;},new cljs.core.Symbol("quantum.core.collections","redv","quantum.core.collections/redv",-463592330,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"redv","redv",1230668989,null),"src/cljc/quantum/core/collections.cljc",15,1,85,85,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.redv)?quantum.core.collections.redv.cljs$lang$test:null)]));
var orig_var__20471__auto___30616 = new cljs.core.Var(function(){return quantum.core.reducers.reducem_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","reducem+","quantum.core.reducers/reducem+",10770984,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"performance","performance",1987578184),new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"attribution","attribution",1937239286),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],["9.94 ms vs. 17.02 ms for 10000 calls to (into+ {}) for small collections ;\n           This is because the |transient| function deals a performance hit.",new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"reducem+","reducem+",446303279,null),"src/cljc/quantum/core/reducers.cljc",15,1,402,"Alex Gunnarson",402,cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),"Requires only one argument for preceding functions in its call chain.",(cljs.core.truth_(quantum.core.reducers.reducem_PLUS_)?quantum.core.reducers.reducem_PLUS_.cljs$lang$test:null)]));
if(cljs.core.truth_(orig_var__20471__auto___30616.hasRoot())){
quantum.core.collections.redm = cljs.core.with_meta.call(null,cljs.core.deref.call(null,new cljs.core.Var(function(){return quantum.core.reducers.reducem_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","reducem+","quantum.core.reducers/reducem+",10770984,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"performance","performance",1987578184),new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"attribution","attribution",1937239286),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],["9.94 ms vs. 17.02 ms for 10000 calls to (into+ {}) for small collections ;\n           This is because the |transient| function deals a performance hit.",new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"reducem+","reducem+",446303279,null),"src/cljc/quantum/core/reducers.cljc",15,1,402,"Alex Gunnarson",402,cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),"Requires only one argument for preceding functions in its call chain.",(cljs.core.truth_(quantum.core.reducers.reducem_PLUS_)?quantum.core.reducers.reducem_PLUS_.cljs$lang$test:null)]))),cljs.core.meta.call(null,new cljs.core.Var(function(){return quantum.core.reducers.reducem_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","reducem+","quantum.core.reducers/reducem+",10770984,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"performance","performance",1987578184),new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"attribution","attribution",1937239286),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],["9.94 ms vs. 17.02 ms for 10000 calls to (into+ {}) for small collections ;\n           This is because the |transient| function deals a performance hit.",new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"reducem+","reducem+",446303279,null),"src/cljc/quantum/core/reducers.cljc",15,1,402,"Alex Gunnarson",402,cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),"Requires only one argument for preceding functions in its call chain.",(cljs.core.truth_(quantum.core.reducers.reducem_PLUS_)?quantum.core.reducers.reducem_PLUS_.cljs$lang$test:null)]))));

if(new cljs.core.Keyword(null,"macro","macro",-867863404).cljs$core$IFn$_invoke$arity$1(cljs.core.meta.call(null,orig_var__20471__auto___30616)) === true){
cljs.core.alter_meta_BANG_.call(null,new cljs.core.Var(function(){return quantum.core.collections.redm;},new cljs.core.Symbol("quantum.core.collections","redm","quantum.core.collections/redm",362250541,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"redm","redm",1788027350,null),"src/cljc/quantum/core/collections.cljc",15,1,86,86,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.redm)?quantum.core.collections.redm.cljs$lang$test:null)])),cljs.core.assoc,new cljs.core.Keyword(null,"macro","macro",-867863404),true);
} else {
}
} else {
}

new cljs.core.Var(function(){return quantum.core.collections.redm;},new cljs.core.Symbol("quantum.core.collections","redm","quantum.core.collections/redm",362250541,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"redm","redm",1788027350,null),"src/cljc/quantum/core/collections.cljc",15,1,86,86,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.redm)?quantum.core.collections.redm.cljs$lang$test:null)]));
var orig_var__20471__auto___30617 = new cljs.core.Var(function(){return quantum.core.reducers.fold_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","fold+","quantum.core.reducers/fold+",503964630,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"added","added",2057651688),new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"top-fn","top-fn",-2056129173),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"contributors","contributors",-1706452523),new cljs.core.Keyword(null,"attribution","attribution",1937239286),new cljs.core.Keyword(null,"todo","todo",-1046442570),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],["1.5",new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"fold+","fold+",272338387,null),"src/cljc/quantum/core/reducers.cljc",12,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"variadic","variadic",882626057),false,new cljs.core.Keyword(null,"max-fixed-arity","max-fixed-arity",-690205543),4,new cljs.core.Keyword(null,"method-params","method-params",-980792179),cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"obj","obj",-1672671807,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"combinef","combinef",2095886860,null),new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"n","n",-2092305744,null),new cljs.core.Symbol(null,"combinef","combinef",2095886860,null),new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),new cljs.core.Keyword(null,"arglists","arglists",1661989754),cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"obj","obj",-1672671807,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"combinef","combinef",2095886860,null),new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"n","n",-2092305744,null),new cljs.core.Symbol(null,"combinef","combinef",2095886860,null),new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),new cljs.core.Keyword(null,"arglists-meta","arglists-meta",1944829838),cljs.core.list(null,null,null,null)], null),1,612,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Alex Gunnarson"], null),"clojure.core.reducers",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Make more efficient.","So many fns created in this lead to inefficiency."], null),612,cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"obj","obj",-1672671807,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"combinef","combinef",2095886860,null),new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"n","n",-2092305744,null),new cljs.core.Symbol(null,"combinef","combinef",2095886860,null),new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),"Reduces a collection using a (potentially parallel) reduce-combine\n  strategy. The collection is partitioned into groups of approximately\n  n (default 512), each of which is reduced with reducef (with a seed\n  value obtained by calling (combinef) with no arguments). The results\n  of these reductions are then reduced with combinef (default\n  reducef).\n  @combinef must be associative. When called with no\n  arguments, (combinef) must produce its identity element.\n  These operations may be performed in parallel, but the results will preserve order.",(cljs.core.truth_(quantum.core.reducers.fold_PLUS_)?quantum.core.reducers.fold_PLUS_.cljs$lang$test:null)]));
if(cljs.core.truth_(orig_var__20471__auto___30617.hasRoot())){
quantum.core.collections.fold = cljs.core.with_meta.call(null,cljs.core.deref.call(null,new cljs.core.Var(function(){return quantum.core.reducers.fold_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","fold+","quantum.core.reducers/fold+",503964630,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"added","added",2057651688),new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"top-fn","top-fn",-2056129173),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"contributors","contributors",-1706452523),new cljs.core.Keyword(null,"attribution","attribution",1937239286),new cljs.core.Keyword(null,"todo","todo",-1046442570),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],["1.5",new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"fold+","fold+",272338387,null),"src/cljc/quantum/core/reducers.cljc",12,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"variadic","variadic",882626057),false,new cljs.core.Keyword(null,"max-fixed-arity","max-fixed-arity",-690205543),4,new cljs.core.Keyword(null,"method-params","method-params",-980792179),cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"obj","obj",-1672671807,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"combinef","combinef",2095886860,null),new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"n","n",-2092305744,null),new cljs.core.Symbol(null,"combinef","combinef",2095886860,null),new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),new cljs.core.Keyword(null,"arglists","arglists",1661989754),cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"obj","obj",-1672671807,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"combinef","combinef",2095886860,null),new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"n","n",-2092305744,null),new cljs.core.Symbol(null,"combinef","combinef",2095886860,null),new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),new cljs.core.Keyword(null,"arglists-meta","arglists-meta",1944829838),cljs.core.list(null,null,null,null)], null),1,612,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Alex Gunnarson"], null),"clojure.core.reducers",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Make more efficient.","So many fns created in this lead to inefficiency."], null),612,cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"obj","obj",-1672671807,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"combinef","combinef",2095886860,null),new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"n","n",-2092305744,null),new cljs.core.Symbol(null,"combinef","combinef",2095886860,null),new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),"Reduces a collection using a (potentially parallel) reduce-combine\n  strategy. The collection is partitioned into groups of approximately\n  n (default 512), each of which is reduced with reducef (with a seed\n  value obtained by calling (combinef) with no arguments). The results\n  of these reductions are then reduced with combinef (default\n  reducef).\n  @combinef must be associative. When called with no\n  arguments, (combinef) must produce its identity element.\n  These operations may be performed in parallel, but the results will preserve order.",(cljs.core.truth_(quantum.core.reducers.fold_PLUS_)?quantum.core.reducers.fold_PLUS_.cljs$lang$test:null)]))),cljs.core.meta.call(null,new cljs.core.Var(function(){return quantum.core.reducers.fold_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","fold+","quantum.core.reducers/fold+",503964630,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"added","added",2057651688),new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"top-fn","top-fn",-2056129173),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"contributors","contributors",-1706452523),new cljs.core.Keyword(null,"attribution","attribution",1937239286),new cljs.core.Keyword(null,"todo","todo",-1046442570),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],["1.5",new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"fold+","fold+",272338387,null),"src/cljc/quantum/core/reducers.cljc",12,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"variadic","variadic",882626057),false,new cljs.core.Keyword(null,"max-fixed-arity","max-fixed-arity",-690205543),4,new cljs.core.Keyword(null,"method-params","method-params",-980792179),cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"obj","obj",-1672671807,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"combinef","combinef",2095886860,null),new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"n","n",-2092305744,null),new cljs.core.Symbol(null,"combinef","combinef",2095886860,null),new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),new cljs.core.Keyword(null,"arglists","arglists",1661989754),cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"obj","obj",-1672671807,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"combinef","combinef",2095886860,null),new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"n","n",-2092305744,null),new cljs.core.Symbol(null,"combinef","combinef",2095886860,null),new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),new cljs.core.Keyword(null,"arglists-meta","arglists-meta",1944829838),cljs.core.list(null,null,null,null)], null),1,612,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Alex Gunnarson"], null),"clojure.core.reducers",new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Make more efficient.","So many fns created in this lead to inefficiency."], null),612,cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"obj","obj",-1672671807,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"combinef","combinef",2095886860,null),new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null),new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"n","n",-2092305744,null),new cljs.core.Symbol(null,"combinef","combinef",2095886860,null),new cljs.core.Symbol(null,"reducef","reducef",1835338990,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),"Reduces a collection using a (potentially parallel) reduce-combine\n  strategy. The collection is partitioned into groups of approximately\n  n (default 512), each of which is reduced with reducef (with a seed\n  value obtained by calling (combinef) with no arguments). The results\n  of these reductions are then reduced with combinef (default\n  reducef).\n  @combinef must be associative. When called with no\n  arguments, (combinef) must produce its identity element.\n  These operations may be performed in parallel, but the results will preserve order.",(cljs.core.truth_(quantum.core.reducers.fold_PLUS_)?quantum.core.reducers.fold_PLUS_.cljs$lang$test:null)]))));

if(new cljs.core.Keyword(null,"macro","macro",-867863404).cljs$core$IFn$_invoke$arity$1(cljs.core.meta.call(null,orig_var__20471__auto___30617)) === true){
cljs.core.alter_meta_BANG_.call(null,new cljs.core.Var(function(){return quantum.core.collections.fold;},new cljs.core.Symbol("quantum.core.collections","fold","quantum.core.collections/fold",-1041936196,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"fold","fold",753070195,null),"src/cljc/quantum/core/collections.cljc",15,1,87,87,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.fold)?quantum.core.collections.fold.cljs$lang$test:null)])),cljs.core.assoc,new cljs.core.Keyword(null,"macro","macro",-867863404),true);
} else {
}
} else {
}

new cljs.core.Var(function(){return quantum.core.collections.fold;},new cljs.core.Symbol("quantum.core.collections","fold","quantum.core.collections/fold",-1041936196,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"fold","fold",753070195,null),"src/cljc/quantum/core/collections.cljc",15,1,87,87,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.fold)?quantum.core.collections.fold.cljs$lang$test:null)]));
var orig_var__20471__auto___30618 = new cljs.core.Var(function(){return quantum.core.reducers.foldp_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","foldp+","quantum.core.reducers/foldp+",338540753,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"todo","todo",-1046442570),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"foldp+","foldp+",509502124,null),"src/cljc/quantum/core/reducers.cljc",13,1,644,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Detect whether there can be a speed improvement achieved or not"], null),644,cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"obj","obj",-1672671807,null)], null)),null,(cljs.core.truth_(quantum.core.reducers.foldp_PLUS_)?quantum.core.reducers.foldp_PLUS_.cljs$lang$test:null)]));
if(cljs.core.truth_(orig_var__20471__auto___30618.hasRoot())){
quantum.core.collections.foldv = cljs.core.with_meta.call(null,cljs.core.deref.call(null,new cljs.core.Var(function(){return quantum.core.reducers.foldp_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","foldp+","quantum.core.reducers/foldp+",338540753,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"todo","todo",-1046442570),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"foldp+","foldp+",509502124,null),"src/cljc/quantum/core/reducers.cljc",13,1,644,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Detect whether there can be a speed improvement achieved or not"], null),644,cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"obj","obj",-1672671807,null)], null)),null,(cljs.core.truth_(quantum.core.reducers.foldp_PLUS_)?quantum.core.reducers.foldp_PLUS_.cljs$lang$test:null)]))),cljs.core.meta.call(null,new cljs.core.Var(function(){return quantum.core.reducers.foldp_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","foldp+","quantum.core.reducers/foldp+",338540753,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"todo","todo",-1046442570),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"foldp+","foldp+",509502124,null),"src/cljc/quantum/core/reducers.cljc",13,1,644,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, ["Detect whether there can be a speed improvement achieved or not"], null),644,cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"obj","obj",-1672671807,null)], null)),null,(cljs.core.truth_(quantum.core.reducers.foldp_PLUS_)?quantum.core.reducers.foldp_PLUS_.cljs$lang$test:null)]))));

if(new cljs.core.Keyword(null,"macro","macro",-867863404).cljs$core$IFn$_invoke$arity$1(cljs.core.meta.call(null,orig_var__20471__auto___30618)) === true){
cljs.core.alter_meta_BANG_.call(null,new cljs.core.Var(function(){return quantum.core.collections.foldv;},new cljs.core.Symbol("quantum.core.collections","foldv","quantum.core.collections/foldv",1662950993,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"foldv","foldv",98762646,null),"src/cljc/quantum/core/collections.cljc",16,1,88,88,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.foldv)?quantum.core.collections.foldv.cljs$lang$test:null)])),cljs.core.assoc,new cljs.core.Keyword(null,"macro","macro",-867863404),true);
} else {
}
} else {
}

new cljs.core.Var(function(){return quantum.core.collections.foldv;},new cljs.core.Symbol("quantum.core.collections","foldv","quantum.core.collections/foldv",1662950993,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"foldv","foldv",98762646,null),"src/cljc/quantum/core/collections.cljc",16,1,88,88,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.foldv)?quantum.core.collections.foldv.cljs$lang$test:null)]));
var orig_var__20471__auto___30619 = new cljs.core.Var(function(){return quantum.core.reducers.foldm_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","foldm+","quantum.core.reducers/foldm+",-1740451440,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"foldm+","foldm+",-2110702701,null),"src/cljc/quantum/core/reducers.cljc",13,1,654,654,cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"obj","obj",-1672671807,null)], null)),null,(cljs.core.truth_(quantum.core.reducers.foldm_PLUS_)?quantum.core.reducers.foldm_PLUS_.cljs$lang$test:null)]));
if(cljs.core.truth_(orig_var__20471__auto___30619.hasRoot())){
quantum.core.collections.foldm = cljs.core.with_meta.call(null,cljs.core.deref.call(null,new cljs.core.Var(function(){return quantum.core.reducers.foldm_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","foldm+","quantum.core.reducers/foldm+",-1740451440,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"foldm+","foldm+",-2110702701,null),"src/cljc/quantum/core/reducers.cljc",13,1,654,654,cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"obj","obj",-1672671807,null)], null)),null,(cljs.core.truth_(quantum.core.reducers.foldm_PLUS_)?quantum.core.reducers.foldm_PLUS_.cljs$lang$test:null)]))),cljs.core.meta.call(null,new cljs.core.Var(function(){return quantum.core.reducers.foldm_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","foldm+","quantum.core.reducers/foldm+",-1740451440,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"foldm+","foldm+",-2110702701,null),"src/cljc/quantum/core/reducers.cljc",13,1,654,654,cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"obj","obj",-1672671807,null)], null)),null,(cljs.core.truth_(quantum.core.reducers.foldm_PLUS_)?quantum.core.reducers.foldm_PLUS_.cljs$lang$test:null)]))));

if(new cljs.core.Keyword(null,"macro","macro",-867863404).cljs$core$IFn$_invoke$arity$1(cljs.core.meta.call(null,orig_var__20471__auto___30619)) === true){
cljs.core.alter_meta_BANG_.call(null,new cljs.core.Var(function(){return quantum.core.collections.foldm;},new cljs.core.Symbol("quantum.core.collections","foldm","quantum.core.collections/foldm",510552853,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"foldm","foldm",-1184199842,null),"src/cljc/quantum/core/collections.cljc",16,1,89,89,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.foldm)?quantum.core.collections.foldm.cljs$lang$test:null)])),cljs.core.assoc,new cljs.core.Keyword(null,"macro","macro",-867863404),true);
} else {
}
} else {
}

new cljs.core.Var(function(){return quantum.core.collections.foldm;},new cljs.core.Symbol("quantum.core.collections","foldm","quantum.core.collections/foldm",510552853,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"foldm","foldm",-1184199842,null),"src/cljc/quantum/core/collections.cljc",16,1,89,89,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.foldm)?quantum.core.collections.foldm.cljs$lang$test:null)]));
var orig_var__20471__auto___30620 = new cljs.core.Var(function(){return quantum.core.reducers.map_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","map+","quantum.core.reducers/map+",392282139,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"map+","map+",288585244,null),"src/cljc/quantum/core/reducers.cljc",11,1,804,804,cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"func","func",1401825487,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),null,(cljs.core.truth_(quantum.core.reducers.map_PLUS_)?quantum.core.reducers.map_PLUS_.cljs$lang$test:null)]));
if(cljs.core.truth_(orig_var__20471__auto___30620.hasRoot())){
quantum.core.collections.map_PLUS_ = cljs.core.with_meta.call(null,cljs.core.deref.call(null,new cljs.core.Var(function(){return quantum.core.reducers.map_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","map+","quantum.core.reducers/map+",392282139,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"map+","map+",288585244,null),"src/cljc/quantum/core/reducers.cljc",11,1,804,804,cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"func","func",1401825487,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),null,(cljs.core.truth_(quantum.core.reducers.map_PLUS_)?quantum.core.reducers.map_PLUS_.cljs$lang$test:null)]))),cljs.core.meta.call(null,new cljs.core.Var(function(){return quantum.core.reducers.map_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","map+","quantum.core.reducers/map+",392282139,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"map+","map+",288585244,null),"src/cljc/quantum/core/reducers.cljc",11,1,804,804,cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"func","func",1401825487,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),null,(cljs.core.truth_(quantum.core.reducers.map_PLUS_)?quantum.core.reducers.map_PLUS_.cljs$lang$test:null)]))));

if(new cljs.core.Keyword(null,"macro","macro",-867863404).cljs$core$IFn$_invoke$arity$1(cljs.core.meta.call(null,orig_var__20471__auto___30620)) === true){
cljs.core.alter_meta_BANG_.call(null,new cljs.core.Var(function(){return quantum.core.collections.map_PLUS_;},new cljs.core.Symbol("quantum.core.collections","map+","quantum.core.collections/map+",-1103718827,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"map+","map+",288585244,null),"src/cljc/quantum/core/collections.cljc",15,1,90,90,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.map_PLUS_)?quantum.core.collections.map_PLUS_.cljs$lang$test:null)])),cljs.core.assoc,new cljs.core.Keyword(null,"macro","macro",-867863404),true);
} else {
}
} else {
}

new cljs.core.Var(function(){return quantum.core.collections.map_PLUS_;},new cljs.core.Symbol("quantum.core.collections","map+","quantum.core.collections/map+",-1103718827,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"map+","map+",288585244,null),"src/cljc/quantum/core/collections.cljc",15,1,90,90,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.map_PLUS_)?quantum.core.collections.map_PLUS_.cljs$lang$test:null)]));
var orig_var__20471__auto___30621 = new cljs.core.Var(function(){return quantum.core.reducers.filter_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","filter+","quantum.core.reducers/filter+",1344160246,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"filter+","filter+",1311518707,null),"src/cljc/quantum/core/reducers.cljc",14,1,891,891,cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"func","func",1401825487,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),null,(cljs.core.truth_(quantum.core.reducers.filter_PLUS_)?quantum.core.reducers.filter_PLUS_.cljs$lang$test:null)]));
if(cljs.core.truth_(orig_var__20471__auto___30621.hasRoot())){
quantum.core.collections.filter_PLUS_ = cljs.core.with_meta.call(null,cljs.core.deref.call(null,new cljs.core.Var(function(){return quantum.core.reducers.filter_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","filter+","quantum.core.reducers/filter+",1344160246,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"filter+","filter+",1311518707,null),"src/cljc/quantum/core/reducers.cljc",14,1,891,891,cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"func","func",1401825487,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),null,(cljs.core.truth_(quantum.core.reducers.filter_PLUS_)?quantum.core.reducers.filter_PLUS_.cljs$lang$test:null)]))),cljs.core.meta.call(null,new cljs.core.Var(function(){return quantum.core.reducers.filter_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","filter+","quantum.core.reducers/filter+",1344160246,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"filter+","filter+",1311518707,null),"src/cljc/quantum/core/reducers.cljc",14,1,891,891,cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"func","func",1401825487,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),null,(cljs.core.truth_(quantum.core.reducers.filter_PLUS_)?quantum.core.reducers.filter_PLUS_.cljs$lang$test:null)]))));

if(new cljs.core.Keyword(null,"macro","macro",-867863404).cljs$core$IFn$_invoke$arity$1(cljs.core.meta.call(null,orig_var__20471__auto___30621)) === true){
cljs.core.alter_meta_BANG_.call(null,new cljs.core.Var(function(){return quantum.core.collections.filter_PLUS_;},new cljs.core.Symbol("quantum.core.collections","filter+","quantum.core.collections/filter+",-383676868,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"filter+","filter+",1311518707,null),"src/cljc/quantum/core/collections.cljc",18,1,91,91,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.filter_PLUS_)?quantum.core.collections.filter_PLUS_.cljs$lang$test:null)])),cljs.core.assoc,new cljs.core.Keyword(null,"macro","macro",-867863404),true);
} else {
}
} else {
}

new cljs.core.Var(function(){return quantum.core.collections.filter_PLUS_;},new cljs.core.Symbol("quantum.core.collections","filter+","quantum.core.collections/filter+",-383676868,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"filter+","filter+",1311518707,null),"src/cljc/quantum/core/collections.cljc",18,1,91,91,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.filter_PLUS_)?quantum.core.collections.filter_PLUS_.cljs$lang$test:null)]));
var orig_var__20471__auto___30622 = new cljs.core.Var(function(){return cljs.core.filter;},new cljs.core.Symbol("cljs.core","filter","cljs.core/filter",-251894204,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"top-fn","top-fn",-2056129173),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"cljs.core","cljs.core",770546058,null),new cljs.core.Symbol(null,"filter","filter",691993593,null),"cljs/core.cljs",(13),new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"variadic","variadic",882626057),false,new cljs.core.Keyword(null,"max-fixed-arity","max-fixed-arity",-690205543),(2),new cljs.core.Keyword(null,"method-params","method-params",-980792179),cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),new cljs.core.Keyword(null,"arglists","arglists",1661989754),cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),new cljs.core.Keyword(null,"arglists-meta","arglists-meta",1944829838),cljs.core.list(null,null)], null),(1),(4256),(4256),cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),"Returns a lazy sequence of the items in coll for which\n  (pred item) returns true. pred must be free of side-effects.\n  Returns a transducer when no collection is provided.",(cljs.core.truth_(cljs.core.filter)?cljs.core.filter.cljs$lang$test:null)]));
if(cljs.core.truth_(orig_var__20471__auto___30622.hasRoot())){
quantum.core.collections.lfilter = cljs.core.with_meta.call(null,cljs.core.deref.call(null,new cljs.core.Var(function(){return cljs.core.filter;},new cljs.core.Symbol("cljs.core","filter","cljs.core/filter",-251894204,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"top-fn","top-fn",-2056129173),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"cljs.core","cljs.core",770546058,null),new cljs.core.Symbol(null,"filter","filter",691993593,null),"cljs/core.cljs",(13),new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"variadic","variadic",882626057),false,new cljs.core.Keyword(null,"max-fixed-arity","max-fixed-arity",-690205543),(2),new cljs.core.Keyword(null,"method-params","method-params",-980792179),cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),new cljs.core.Keyword(null,"arglists","arglists",1661989754),cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),new cljs.core.Keyword(null,"arglists-meta","arglists-meta",1944829838),cljs.core.list(null,null)], null),(1),(4256),(4256),cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),"Returns a lazy sequence of the items in coll for which\n  (pred item) returns true. pred must be free of side-effects.\n  Returns a transducer when no collection is provided.",(cljs.core.truth_(cljs.core.filter)?cljs.core.filter.cljs$lang$test:null)]))),cljs.core.meta.call(null,new cljs.core.Var(function(){return cljs.core.filter;},new cljs.core.Symbol("cljs.core","filter","cljs.core/filter",-251894204,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"top-fn","top-fn",-2056129173),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"cljs.core","cljs.core",770546058,null),new cljs.core.Symbol(null,"filter","filter",691993593,null),"cljs/core.cljs",(13),new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"variadic","variadic",882626057),false,new cljs.core.Keyword(null,"max-fixed-arity","max-fixed-arity",-690205543),(2),new cljs.core.Keyword(null,"method-params","method-params",-980792179),cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),new cljs.core.Keyword(null,"arglists","arglists",1661989754),cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),new cljs.core.Keyword(null,"arglists-meta","arglists-meta",1944829838),cljs.core.list(null,null)], null),(1),(4256),(4256),cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),"Returns a lazy sequence of the items in coll for which\n  (pred item) returns true. pred must be free of side-effects.\n  Returns a transducer when no collection is provided.",(cljs.core.truth_(cljs.core.filter)?cljs.core.filter.cljs$lang$test:null)]))));

if(new cljs.core.Keyword(null,"macro","macro",-867863404).cljs$core$IFn$_invoke$arity$1(cljs.core.meta.call(null,orig_var__20471__auto___30622)) === true){
cljs.core.alter_meta_BANG_.call(null,new cljs.core.Var(function(){return quantum.core.collections.lfilter;},new cljs.core.Symbol("quantum.core.collections","lfilter","quantum.core.collections/lfilter",-189268564,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"lfilter","lfilter",465263703,null),"src/cljc/quantum/core/collections.cljc",18,1,92,92,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.lfilter)?quantum.core.collections.lfilter.cljs$lang$test:null)])),cljs.core.assoc,new cljs.core.Keyword(null,"macro","macro",-867863404),true);
} else {
}
} else {
}

new cljs.core.Var(function(){return quantum.core.collections.lfilter;},new cljs.core.Symbol("quantum.core.collections","lfilter","quantum.core.collections/lfilter",-189268564,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"lfilter","lfilter",465263703,null),"src/cljc/quantum/core/collections.cljc",18,1,92,92,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.lfilter)?quantum.core.collections.lfilter.cljs$lang$test:null)]));
var orig_var__20471__auto___30623 = new cljs.core.Var(function(){return quantum.core.reducers.remove_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","remove+","quantum.core.reducers/remove+",-379680096,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"remove+","remove+",-754172755,null),"src/cljc/quantum/core/reducers.cljc",14,1,901,901,cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"func","func",1401825487,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),null,(cljs.core.truth_(quantum.core.reducers.remove_PLUS_)?quantum.core.reducers.remove_PLUS_.cljs$lang$test:null)]));
if(cljs.core.truth_(orig_var__20471__auto___30623.hasRoot())){
quantum.core.collections.remove_PLUS_ = cljs.core.with_meta.call(null,cljs.core.deref.call(null,new cljs.core.Var(function(){return quantum.core.reducers.remove_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","remove+","quantum.core.reducers/remove+",-379680096,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"remove+","remove+",-754172755,null),"src/cljc/quantum/core/reducers.cljc",14,1,901,901,cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"func","func",1401825487,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),null,(cljs.core.truth_(quantum.core.reducers.remove_PLUS_)?quantum.core.reducers.remove_PLUS_.cljs$lang$test:null)]))),cljs.core.meta.call(null,new cljs.core.Var(function(){return quantum.core.reducers.remove_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","remove+","quantum.core.reducers/remove+",-379680096,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"remove+","remove+",-754172755,null),"src/cljc/quantum/core/reducers.cljc",14,1,901,901,cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"func","func",1401825487,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),null,(cljs.core.truth_(quantum.core.reducers.remove_PLUS_)?quantum.core.reducers.remove_PLUS_.cljs$lang$test:null)]))));

if(new cljs.core.Keyword(null,"macro","macro",-867863404).cljs$core$IFn$_invoke$arity$1(cljs.core.meta.call(null,orig_var__20471__auto___30623)) === true){
cljs.core.alter_meta_BANG_.call(null,new cljs.core.Var(function(){return quantum.core.collections.remove_PLUS_;},new cljs.core.Symbol("quantum.core.collections","remove+","quantum.core.collections/remove+",805854570,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"remove+","remove+",-754172755,null),"src/cljc/quantum/core/collections.cljc",18,1,93,93,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.remove_PLUS_)?quantum.core.collections.remove_PLUS_.cljs$lang$test:null)])),cljs.core.assoc,new cljs.core.Keyword(null,"macro","macro",-867863404),true);
} else {
}
} else {
}

new cljs.core.Var(function(){return quantum.core.collections.remove_PLUS_;},new cljs.core.Symbol("quantum.core.collections","remove+","quantum.core.collections/remove+",805854570,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"remove+","remove+",-754172755,null),"src/cljc/quantum/core/collections.cljc",18,1,93,93,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.remove_PLUS_)?quantum.core.collections.remove_PLUS_.cljs$lang$test:null)]));
var orig_var__20471__auto___30624 = new cljs.core.Var(function(){return cljs.core.remove;},new cljs.core.Symbol("cljs.core","remove","cljs.core/remove",20102034,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"top-fn","top-fn",-2056129173),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"cljs.core","cljs.core",770546058,null),new cljs.core.Symbol(null,"remove","remove",1509103113,null),"cljs/core.cljs",(13),new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"variadic","variadic",882626057),false,new cljs.core.Keyword(null,"max-fixed-arity","max-fixed-arity",-690205543),(2),new cljs.core.Keyword(null,"method-params","method-params",-980792179),cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),new cljs.core.Keyword(null,"arglists","arglists",1661989754),cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),new cljs.core.Keyword(null,"arglists-meta","arglists-meta",1944829838),cljs.core.list(null,null)], null),(1),(4285),(4285),cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),"Returns a lazy sequence of the items in coll for which\n  (pred item) returns false. pred must be free of side-effects.\n  Returns a transducer when no collection is provided.",(cljs.core.truth_(cljs.core.remove)?cljs.core.remove.cljs$lang$test:null)]));
if(cljs.core.truth_(orig_var__20471__auto___30624.hasRoot())){
quantum.core.collections.lremove = cljs.core.with_meta.call(null,cljs.core.deref.call(null,new cljs.core.Var(function(){return cljs.core.remove;},new cljs.core.Symbol("cljs.core","remove","cljs.core/remove",20102034,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"top-fn","top-fn",-2056129173),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"cljs.core","cljs.core",770546058,null),new cljs.core.Symbol(null,"remove","remove",1509103113,null),"cljs/core.cljs",(13),new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"variadic","variadic",882626057),false,new cljs.core.Keyword(null,"max-fixed-arity","max-fixed-arity",-690205543),(2),new cljs.core.Keyword(null,"method-params","method-params",-980792179),cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),new cljs.core.Keyword(null,"arglists","arglists",1661989754),cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),new cljs.core.Keyword(null,"arglists-meta","arglists-meta",1944829838),cljs.core.list(null,null)], null),(1),(4285),(4285),cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),"Returns a lazy sequence of the items in coll for which\n  (pred item) returns false. pred must be free of side-effects.\n  Returns a transducer when no collection is provided.",(cljs.core.truth_(cljs.core.remove)?cljs.core.remove.cljs$lang$test:null)]))),cljs.core.meta.call(null,new cljs.core.Var(function(){return cljs.core.remove;},new cljs.core.Symbol("cljs.core","remove","cljs.core/remove",20102034,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"top-fn","top-fn",-2056129173),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"cljs.core","cljs.core",770546058,null),new cljs.core.Symbol(null,"remove","remove",1509103113,null),"cljs/core.cljs",(13),new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"variadic","variadic",882626057),false,new cljs.core.Keyword(null,"max-fixed-arity","max-fixed-arity",-690205543),(2),new cljs.core.Keyword(null,"method-params","method-params",-980792179),cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),new cljs.core.Keyword(null,"arglists","arglists",1661989754),cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),new cljs.core.Keyword(null,"arglists-meta","arglists-meta",1944829838),cljs.core.list(null,null)], null),(1),(4285),(4285),cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),"Returns a lazy sequence of the items in coll for which\n  (pred item) returns false. pred must be free of side-effects.\n  Returns a transducer when no collection is provided.",(cljs.core.truth_(cljs.core.remove)?cljs.core.remove.cljs$lang$test:null)]))));

if(new cljs.core.Keyword(null,"macro","macro",-867863404).cljs$core$IFn$_invoke$arity$1(cljs.core.meta.call(null,orig_var__20471__auto___30624)) === true){
cljs.core.alter_meta_BANG_.call(null,new cljs.core.Var(function(){return quantum.core.collections.lremove;},new cljs.core.Symbol("quantum.core.collections","lremove","quantum.core.collections/lremove",908226503,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"lremove","lremove",-1858203714,null),"src/cljc/quantum/core/collections.cljc",18,1,94,94,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.lremove)?quantum.core.collections.lremove.cljs$lang$test:null)])),cljs.core.assoc,new cljs.core.Keyword(null,"macro","macro",-867863404),true);
} else {
}
} else {
}

new cljs.core.Var(function(){return quantum.core.collections.lremove;},new cljs.core.Symbol("quantum.core.collections","lremove","quantum.core.collections/lremove",908226503,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"lremove","lremove",-1858203714,null),"src/cljc/quantum/core/collections.cljc",18,1,94,94,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.lremove)?quantum.core.collections.lremove.cljs$lang$test:null)]));
var orig_var__20471__auto___30625 = new cljs.core.Var(function(){return quantum.core.reducers.take_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","take+","quantum.core.reducers/take+",-2068817596,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"take+","take+",-1903972517,null),"src/cljc/quantum/core/reducers.cljc",13,1,1024,1024,cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"n","n",-2092305744,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),null,(cljs.core.truth_(quantum.core.reducers.take_PLUS_)?quantum.core.reducers.take_PLUS_.cljs$lang$test:null)]));
if(cljs.core.truth_(orig_var__20471__auto___30625.hasRoot())){
quantum.core.collections.take_PLUS_ = cljs.core.with_meta.call(null,cljs.core.deref.call(null,new cljs.core.Var(function(){return quantum.core.reducers.take_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","take+","quantum.core.reducers/take+",-2068817596,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"take+","take+",-1903972517,null),"src/cljc/quantum/core/reducers.cljc",13,1,1024,1024,cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"n","n",-2092305744,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),null,(cljs.core.truth_(quantum.core.reducers.take_PLUS_)?quantum.core.reducers.take_PLUS_.cljs$lang$test:null)]))),cljs.core.meta.call(null,new cljs.core.Var(function(){return quantum.core.reducers.take_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","take+","quantum.core.reducers/take+",-2068817596,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"take+","take+",-1903972517,null),"src/cljc/quantum/core/reducers.cljc",13,1,1024,1024,cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"n","n",-2092305744,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),null,(cljs.core.truth_(quantum.core.reducers.take_PLUS_)?quantum.core.reducers.take_PLUS_.cljs$lang$test:null)]))));

if(new cljs.core.Keyword(null,"macro","macro",-867863404).cljs$core$IFn$_invoke$arity$1(cljs.core.meta.call(null,orig_var__20471__auto___30625)) === true){
cljs.core.alter_meta_BANG_.call(null,new cljs.core.Var(function(){return quantum.core.collections.take_PLUS_;},new cljs.core.Symbol("quantum.core.collections","take+","quantum.core.collections/take+",1938755346,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"take+","take+",-1903972517,null),"src/cljc/quantum/core/collections.cljc",16,1,95,95,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.take_PLUS_)?quantum.core.collections.take_PLUS_.cljs$lang$test:null)])),cljs.core.assoc,new cljs.core.Keyword(null,"macro","macro",-867863404),true);
} else {
}
} else {
}

new cljs.core.Var(function(){return quantum.core.collections.take_PLUS_;},new cljs.core.Symbol("quantum.core.collections","take+","quantum.core.collections/take+",1938755346,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"take+","take+",-1903972517,null),"src/cljc/quantum/core/collections.cljc",16,1,95,95,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.take_PLUS_)?quantum.core.collections.take_PLUS_.cljs$lang$test:null)]));
var orig_var__20471__auto___30626 = new cljs.core.Var(function(){return quantum.core.reducers.take_while_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","take-while+","quantum.core.reducers/take-while+",-1200987563,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"take-while+","take-while+",1175993430,null),"src/cljc/quantum/core/reducers.cljc",19,1,1039,1039,cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),null,(cljs.core.truth_(quantum.core.reducers.take_while_PLUS_)?quantum.core.reducers.take_while_PLUS_.cljs$lang$test:null)]));
if(cljs.core.truth_(orig_var__20471__auto___30626.hasRoot())){
quantum.core.collections.take_while_PLUS_ = cljs.core.with_meta.call(null,cljs.core.deref.call(null,new cljs.core.Var(function(){return quantum.core.reducers.take_while_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","take-while+","quantum.core.reducers/take-while+",-1200987563,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"take-while+","take-while+",1175993430,null),"src/cljc/quantum/core/reducers.cljc",19,1,1039,1039,cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),null,(cljs.core.truth_(quantum.core.reducers.take_while_PLUS_)?quantum.core.reducers.take_while_PLUS_.cljs$lang$test:null)]))),cljs.core.meta.call(null,new cljs.core.Var(function(){return quantum.core.reducers.take_while_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","take-while+","quantum.core.reducers/take-while+",-1200987563,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"take-while+","take-while+",1175993430,null),"src/cljc/quantum/core/reducers.cljc",19,1,1039,1039,cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"pred","pred",-727012372,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),null,(cljs.core.truth_(quantum.core.reducers.take_while_PLUS_)?quantum.core.reducers.take_while_PLUS_.cljs$lang$test:null)]))));

if(new cljs.core.Keyword(null,"macro","macro",-867863404).cljs$core$IFn$_invoke$arity$1(cljs.core.meta.call(null,orig_var__20471__auto___30626)) === true){
cljs.core.alter_meta_BANG_.call(null,new cljs.core.Var(function(){return quantum.core.collections.take_while_PLUS_;},new cljs.core.Symbol("quantum.core.collections","take-while+","quantum.core.collections/take-while+",-518218337,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"take-while+","take-while+",1175993430,null),"src/cljc/quantum/core/collections.cljc",22,1,96,96,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.take_while_PLUS_)?quantum.core.collections.take_while_PLUS_.cljs$lang$test:null)])),cljs.core.assoc,new cljs.core.Keyword(null,"macro","macro",-867863404),true);
} else {
}
} else {
}

new cljs.core.Var(function(){return quantum.core.collections.take_while_PLUS_;},new cljs.core.Symbol("quantum.core.collections","take-while+","quantum.core.collections/take-while+",-518218337,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"take-while+","take-while+",1175993430,null),"src/cljc/quantum/core/collections.cljc",22,1,96,96,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.take_while_PLUS_)?quantum.core.collections.take_while_PLUS_.cljs$lang$test:null)]));
var orig_var__20471__auto___30627 = new cljs.core.Var(function(){return quantum.core.reducers.drop_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","drop+","quantum.core.reducers/drop+",-1565484505,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"tag","tag",-1290361223),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"drop+","drop+",-1520290780,null),"src/cljc/quantum/core/reducers.cljc",19,1,1083,1083,new cljs.core.Symbol(null,"Delay","Delay",-956795572,null),cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"n","n",-2092305744,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),null,(cljs.core.truth_(quantum.core.reducers.drop_PLUS_)?quantum.core.reducers.drop_PLUS_.cljs$lang$test:null)]));
if(cljs.core.truth_(orig_var__20471__auto___30627.hasRoot())){
quantum.core.collections.drop_PLUS_ = cljs.core.with_meta.call(null,cljs.core.deref.call(null,new cljs.core.Var(function(){return quantum.core.reducers.drop_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","drop+","quantum.core.reducers/drop+",-1565484505,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"tag","tag",-1290361223),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"drop+","drop+",-1520290780,null),"src/cljc/quantum/core/reducers.cljc",19,1,1083,1083,new cljs.core.Symbol(null,"Delay","Delay",-956795572,null),cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"n","n",-2092305744,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),null,(cljs.core.truth_(quantum.core.reducers.drop_PLUS_)?quantum.core.reducers.drop_PLUS_.cljs$lang$test:null)]))),cljs.core.meta.call(null,new cljs.core.Var(function(){return quantum.core.reducers.drop_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","drop+","quantum.core.reducers/drop+",-1565484505,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"tag","tag",-1290361223),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"drop+","drop+",-1520290780,null),"src/cljc/quantum/core/reducers.cljc",19,1,1083,1083,new cljs.core.Symbol(null,"Delay","Delay",-956795572,null),cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"n","n",-2092305744,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),null,(cljs.core.truth_(quantum.core.reducers.drop_PLUS_)?quantum.core.reducers.drop_PLUS_.cljs$lang$test:null)]))));

if(new cljs.core.Keyword(null,"macro","macro",-867863404).cljs$core$IFn$_invoke$arity$1(cljs.core.meta.call(null,orig_var__20471__auto___30627)) === true){
cljs.core.alter_meta_BANG_.call(null,new cljs.core.Var(function(){return quantum.core.collections.drop_PLUS_;},new cljs.core.Symbol("quantum.core.collections","drop+","quantum.core.collections/drop+",1249891565,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"drop+","drop+",-1520290780,null),"src/cljc/quantum/core/collections.cljc",16,1,97,97,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.drop_PLUS_)?quantum.core.collections.drop_PLUS_.cljs$lang$test:null)])),cljs.core.assoc,new cljs.core.Keyword(null,"macro","macro",-867863404),true);
} else {
}
} else {
}

new cljs.core.Var(function(){return quantum.core.collections.drop_PLUS_;},new cljs.core.Symbol("quantum.core.collections","drop+","quantum.core.collections/drop+",1249891565,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"drop+","drop+",-1520290780,null),"src/cljc/quantum/core/collections.cljc",16,1,97,97,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.drop_PLUS_)?quantum.core.collections.drop_PLUS_.cljs$lang$test:null)]));
var orig_var__20471__auto___30628 = new cljs.core.Var(function(){return quantum.core.reducers.group_by_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","group-by+","quantum.core.reducers/group-by+",1070285042,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"usage","usage",-1583752910),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"out","out",-910545517),new cljs.core.Keyword(null,"attribution","attribution",1937239286),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"group-by+","group-by+",841291471,null),"src/cljc/quantum/core/reducers.cljc",16,1,cljs.core.list(new cljs.core.Symbol(null,"quote","quote",1377916282,null),cljs.core.list(new cljs.core.Symbol(null,"group-by","group-by",1261391725,null),new cljs.core.Symbol(null,"odd?","odd?",-1458588199,null),cljs.core.list(new cljs.core.Symbol(null,"range","range",-1014743483,null),(10)))),1154,cljs.core.list(new cljs.core.Symbol(null,"quote","quote",1377916282,null),new cljs.core.PersistentArrayMap(null, 2, [false,new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [(0),(2),(4),(6),(8)], null),true,new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [(1),(3),(5),(7),(9)], null)], null)),"Christophe Grand - http://grokbase.com/t/gg/clojure/12c3k7ztbz/group-by-vs-reducers",1154,cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"f","f",43394975,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),"Reducers version. Possibly slower than |core/group-by|",(cljs.core.truth_(quantum.core.reducers.group_by_PLUS_)?quantum.core.reducers.group_by_PLUS_.cljs$lang$test:null)]));
if(cljs.core.truth_(orig_var__20471__auto___30628.hasRoot())){
quantum.core.collections.group_by_PLUS_ = cljs.core.with_meta.call(null,cljs.core.deref.call(null,new cljs.core.Var(function(){return quantum.core.reducers.group_by_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","group-by+","quantum.core.reducers/group-by+",1070285042,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"usage","usage",-1583752910),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"out","out",-910545517),new cljs.core.Keyword(null,"attribution","attribution",1937239286),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"group-by+","group-by+",841291471,null),"src/cljc/quantum/core/reducers.cljc",16,1,cljs.core.list(new cljs.core.Symbol(null,"quote","quote",1377916282,null),cljs.core.list(new cljs.core.Symbol(null,"group-by","group-by",1261391725,null),new cljs.core.Symbol(null,"odd?","odd?",-1458588199,null),cljs.core.list(new cljs.core.Symbol(null,"range","range",-1014743483,null),(10)))),1154,cljs.core.list(new cljs.core.Symbol(null,"quote","quote",1377916282,null),new cljs.core.PersistentArrayMap(null, 2, [false,new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [(0),(2),(4),(6),(8)], null),true,new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [(1),(3),(5),(7),(9)], null)], null)),"Christophe Grand - http://grokbase.com/t/gg/clojure/12c3k7ztbz/group-by-vs-reducers",1154,cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"f","f",43394975,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),"Reducers version. Possibly slower than |core/group-by|",(cljs.core.truth_(quantum.core.reducers.group_by_PLUS_)?quantum.core.reducers.group_by_PLUS_.cljs$lang$test:null)]))),cljs.core.meta.call(null,new cljs.core.Var(function(){return quantum.core.reducers.group_by_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","group-by+","quantum.core.reducers/group-by+",1070285042,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"usage","usage",-1583752910),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"out","out",-910545517),new cljs.core.Keyword(null,"attribution","attribution",1937239286),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"group-by+","group-by+",841291471,null),"src/cljc/quantum/core/reducers.cljc",16,1,cljs.core.list(new cljs.core.Symbol(null,"quote","quote",1377916282,null),cljs.core.list(new cljs.core.Symbol(null,"group-by","group-by",1261391725,null),new cljs.core.Symbol(null,"odd?","odd?",-1458588199,null),cljs.core.list(new cljs.core.Symbol(null,"range","range",-1014743483,null),(10)))),1154,cljs.core.list(new cljs.core.Symbol(null,"quote","quote",1377916282,null),new cljs.core.PersistentArrayMap(null, 2, [false,new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [(0),(2),(4),(6),(8)], null),true,new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [(1),(3),(5),(7),(9)], null)], null)),"Christophe Grand - http://grokbase.com/t/gg/clojure/12c3k7ztbz/group-by-vs-reducers",1154,cljs.core.list(new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"f","f",43394975,null),new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),"Reducers version. Possibly slower than |core/group-by|",(cljs.core.truth_(quantum.core.reducers.group_by_PLUS_)?quantum.core.reducers.group_by_PLUS_.cljs$lang$test:null)]))));

if(new cljs.core.Keyword(null,"macro","macro",-867863404).cljs$core$IFn$_invoke$arity$1(cljs.core.meta.call(null,orig_var__20471__auto___30628)) === true){
cljs.core.alter_meta_BANG_.call(null,new cljs.core.Var(function(){return quantum.core.collections.group_by_PLUS_;},new cljs.core.Symbol("quantum.core.collections","group-by+","quantum.core.collections/group-by+",1461302788,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"group-by+","group-by+",841291471,null),"src/cljc/quantum/core/collections.cljc",20,1,98,98,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.group_by_PLUS_)?quantum.core.collections.group_by_PLUS_.cljs$lang$test:null)])),cljs.core.assoc,new cljs.core.Keyword(null,"macro","macro",-867863404),true);
} else {
}
} else {
}

new cljs.core.Var(function(){return quantum.core.collections.group_by_PLUS_;},new cljs.core.Symbol("quantum.core.collections","group-by+","quantum.core.collections/group-by+",1461302788,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"group-by+","group-by+",841291471,null),"src/cljc/quantum/core/collections.cljc",20,1,98,98,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.group_by_PLUS_)?quantum.core.collections.group_by_PLUS_.cljs$lang$test:null)]));
var orig_var__20471__auto___30629 = new cljs.core.Var(function(){return quantum.core.reducers.flatten_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","flatten+","quantum.core.reducers/flatten+",-1745209136,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"flatten+","flatten+",-1842486573,null),"src/cljc/quantum/core/reducers.cljc",15,1,925,925,cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),null,(cljs.core.truth_(quantum.core.reducers.flatten_PLUS_)?quantum.core.reducers.flatten_PLUS_.cljs$lang$test:null)]));
if(cljs.core.truth_(orig_var__20471__auto___30629.hasRoot())){
quantum.core.collections.flatten_PLUS_ = cljs.core.with_meta.call(null,cljs.core.deref.call(null,new cljs.core.Var(function(){return quantum.core.reducers.flatten_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","flatten+","quantum.core.reducers/flatten+",-1745209136,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"flatten+","flatten+",-1842486573,null),"src/cljc/quantum/core/reducers.cljc",15,1,925,925,cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),null,(cljs.core.truth_(quantum.core.reducers.flatten_PLUS_)?quantum.core.reducers.flatten_PLUS_.cljs$lang$test:null)]))),cljs.core.meta.call(null,new cljs.core.Var(function(){return quantum.core.reducers.flatten_PLUS_;},new cljs.core.Symbol("quantum.core.reducers","flatten+","quantum.core.reducers/flatten+",-1745209136,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.reducers","quantum.core.reducers",-681006924,null),new cljs.core.Symbol(null,"flatten+","flatten+",-1842486573,null),"src/cljc/quantum/core/reducers.cljc",15,1,925,925,cljs.core.list(new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"coll","coll",-1006698606,null)], null)),null,(cljs.core.truth_(quantum.core.reducers.flatten_PLUS_)?quantum.core.reducers.flatten_PLUS_.cljs$lang$test:null)]))));

if(new cljs.core.Keyword(null,"macro","macro",-867863404).cljs$core$IFn$_invoke$arity$1(cljs.core.meta.call(null,orig_var__20471__auto___30629)) === true){
cljs.core.alter_meta_BANG_.call(null,new cljs.core.Var(function(){return quantum.core.collections.flatten_PLUS_;},new cljs.core.Symbol("quantum.core.collections","flatten+","quantum.core.collections/flatten+",791249434,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"flatten+","flatten+",-1842486573,null),"src/cljc/quantum/core/collections.cljc",19,1,99,99,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.flatten_PLUS_)?quantum.core.collections.flatten_PLUS_.cljs$lang$test:null)])),cljs.core.assoc,new cljs.core.Keyword(null,"macro","macro",-867863404),true);
} else {
}
} else {
}

new cljs.core.Var(function(){return quantum.core.collections.flatten_PLUS_;},new cljs.core.Symbol("quantum.core.collections","flatten+","quantum.core.collections/flatten+",791249434,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"flatten+","flatten+",-1842486573,null),"src/cljc/quantum/core/collections.cljc",19,1,99,99,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.flatten_PLUS_)?quantum.core.collections.flatten_PLUS_.cljs$lang$test:null)]));
/**
 * For some reason ClojureScript reducers have an issue and it's terrible... so use it like so:
 * (map+ (compr kv+ <myfunc>) _)
 * |reduce| doesn't have this problem.
 */
quantum.core.collections.kv_PLUS_ = (function quantum$core$collections$kv_PLUS_(){
var G__30631 = arguments.length;
switch (G__30631) {
case 1:
return quantum.core.collections.kv_PLUS_.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return quantum.core.collections.kv_PLUS_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.collections.kv_PLUS_.cljs$core$IFn$_invoke$arity$1 = (function (obj){
return obj;
});

quantum.core.collections.kv_PLUS_.cljs$core$IFn$_invoke$arity$2 = (function (k,v){
return k;
});

quantum.core.collections.kv_PLUS_.cljs$lang$maxFixedArity = 2;
quantum.core.collections.lasti = quantum.core.collections.core.lasti;
quantum.core.collections.index_of = quantum.core.collections.core.index_of_PLUS_;
quantum.core.collections.last_index_of = quantum.core.collections.core.last_index_of_PLUS_;
quantum.core.collections.count = quantum.core.collections.core.count_PLUS_;
quantum.core.collections.getr = quantum.core.collections.core.getr_PLUS_;
quantum.core.collections.get = quantum.core.collections.core.get_PLUS_;
quantum.core.collections.gets = quantum.core.collections.core.gets_PLUS_;
quantum.core.collections.getf = quantum.core.collections.core.getf_PLUS_;
quantum.core.collections.pop = quantum.core.collections.core.pop_PLUS_;
quantum.core.collections.popr = quantum.core.collections.core.popr_PLUS_;
quantum.core.collections.popl = quantum.core.collections.core.popl_PLUS_;
quantum.core.collections.peek = quantum.core.collections.core.peek_PLUS_;
quantum.core.collections.first = quantum.core.collections.core.first_PLUS_;
quantum.core.collections.second = quantum.core.collections.core.second_PLUS_;
quantum.core.collections.third = quantum.core.collections.core.third;
quantum.core.collections.rest = quantum.core.collections.core.rest_PLUS_;
var orig_var__20471__auto___30633 = new cljs.core.Var(function(){return cljs.core.rest;},new cljs.core.Symbol("cljs","core.rest","cljs/core.rest",-1355121594,null),new cljs.core.PersistentArrayMap(null, 8, [new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Symbol(null,"cljs","cljs",-1162018140,null),new cljs.core.Keyword(null,"doc","doc",1913296891),null,new cljs.core.Keyword(null,"file","file",-1269645878),null,new cljs.core.Keyword(null,"line","line",212345235),null,new cljs.core.Keyword(null,"column","column",2078222095),null,new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Symbol(null,"core.rest","core.rest",-1343548620,null),new cljs.core.Keyword(null,"test","test",577538877),(cljs.core.truth_(cljs.core.rest)?cljs.core.rest.cljs$lang$test:null),new cljs.core.Keyword(null,"arglists","arglists",1661989754),cljs.core.List.EMPTY], null));
if(cljs.core.truth_(orig_var__20471__auto___30633.hasRoot())){
quantum.core.collections.lrest = cljs.core.with_meta.call(null,cljs.core.deref.call(null,new cljs.core.Var(function(){return cljs.core.rest;},new cljs.core.Symbol("cljs","core.rest","cljs/core.rest",-1355121594,null),new cljs.core.PersistentArrayMap(null, 8, [new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Symbol(null,"cljs","cljs",-1162018140,null),new cljs.core.Keyword(null,"doc","doc",1913296891),null,new cljs.core.Keyword(null,"file","file",-1269645878),null,new cljs.core.Keyword(null,"line","line",212345235),null,new cljs.core.Keyword(null,"column","column",2078222095),null,new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Symbol(null,"core.rest","core.rest",-1343548620,null),new cljs.core.Keyword(null,"test","test",577538877),(cljs.core.truth_(cljs.core.rest)?cljs.core.rest.cljs$lang$test:null),new cljs.core.Keyword(null,"arglists","arglists",1661989754),cljs.core.List.EMPTY], null))),cljs.core.meta.call(null,new cljs.core.Var(function(){return cljs.core.rest;},new cljs.core.Symbol("cljs","core.rest","cljs/core.rest",-1355121594,null),new cljs.core.PersistentArrayMap(null, 8, [new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Symbol(null,"cljs","cljs",-1162018140,null),new cljs.core.Keyword(null,"doc","doc",1913296891),null,new cljs.core.Keyword(null,"file","file",-1269645878),null,new cljs.core.Keyword(null,"line","line",212345235),null,new cljs.core.Keyword(null,"column","column",2078222095),null,new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Symbol(null,"core.rest","core.rest",-1343548620,null),new cljs.core.Keyword(null,"test","test",577538877),(cljs.core.truth_(cljs.core.rest)?cljs.core.rest.cljs$lang$test:null),new cljs.core.Keyword(null,"arglists","arglists",1661989754),cljs.core.List.EMPTY], null))));

if(new cljs.core.Keyword(null,"macro","macro",-867863404).cljs$core$IFn$_invoke$arity$1(cljs.core.meta.call(null,orig_var__20471__auto___30633)) === true){
cljs.core.alter_meta_BANG_.call(null,new cljs.core.Var(function(){return quantum.core.collections.lrest;},new cljs.core.Symbol("quantum.core.collections","lrest","quantum.core.collections/lrest",1343439104,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"lrest","lrest",1024926153,null),"src/cljc/quantum/core/collections.cljc",16,1,157,157,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.lrest)?quantum.core.collections.lrest.cljs$lang$test:null)])),cljs.core.assoc,new cljs.core.Keyword(null,"macro","macro",-867863404),true);
} else {
}
} else {
}

new cljs.core.Var(function(){return quantum.core.collections.lrest;},new cljs.core.Symbol("quantum.core.collections","lrest","quantum.core.collections/lrest",1343439104,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"lrest","lrest",1024926153,null),"src/cljc/quantum/core/collections.cljc",16,1,157,157,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.lrest)?quantum.core.collections.lrest.cljs$lang$test:null)]));
quantum.core.collections.butlast = quantum.core.collections.core.butlast_PLUS_;
quantum.core.collections.last = quantum.core.collections.core.last_PLUS_;
var orig_var__20471__auto___30634 = new cljs.core.Var(function(){return quantum.core.data.map.merge_PLUS_;},new cljs.core.Symbol("quantum.core.data.map","merge+","quantum.core.data.map/merge+",1894972433,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"performance","performance",1987578184),new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"top-fn","top-fn",-2056129173),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"attribution","attribution",1937239286),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],["782.922731 ms |merge+| vs. 1.133217 sec normal |merge| ; 1.5 times faster!",new cljs.core.Symbol(null,"quantum.core.data.map","quantum.core.data.map",172374462,null),new cljs.core.Symbol(null,"merge+","merge+",-1829307131,null),"src/cljc/quantum/core/data/map.cljc",13,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"variadic","variadic",882626057),true,new cljs.core.Keyword(null,"max-fixed-arity","max-fixed-arity",-690205543),(1),new cljs.core.Keyword(null,"method-params","method-params",-980792179),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.list(new cljs.core.Symbol(null,"map-0","map-0",1310108841,null),new cljs.core.Symbol(null,"maps","maps",-71029607,null))], null),new cljs.core.Keyword(null,"arglists","arglists",1661989754),cljs.core.list(new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"map-0","map-0",1310108841,null),new cljs.core.Symbol(null,"&","&",-2144855648,null),new cljs.core.Symbol(null,"maps","maps",-71029607,null)], null)),new cljs.core.Keyword(null,"arglists-meta","arglists-meta",1944829838),cljs.core.list(null)], null),1,38,"Alex Gunnarson",38,cljs.core.list(new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"map-0","map-0",1310108841,null),new cljs.core.Symbol(null,"&","&",-2144855648,null),new cljs.core.Symbol(null,"maps","maps",-71029607,null)], null)),"A performant drop-in replacemen for |clojure.core/merge|.",(cljs.core.truth_(quantum.core.data.map.merge_PLUS_)?quantum.core.data.map.merge_PLUS_.cljs$lang$test:null)]));
if(cljs.core.truth_(orig_var__20471__auto___30634.hasRoot())){
quantum.core.collections.merge = cljs.core.with_meta.call(null,cljs.core.deref.call(null,new cljs.core.Var(function(){return quantum.core.data.map.merge_PLUS_;},new cljs.core.Symbol("quantum.core.data.map","merge+","quantum.core.data.map/merge+",1894972433,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"performance","performance",1987578184),new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"top-fn","top-fn",-2056129173),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"attribution","attribution",1937239286),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],["782.922731 ms |merge+| vs. 1.133217 sec normal |merge| ; 1.5 times faster!",new cljs.core.Symbol(null,"quantum.core.data.map","quantum.core.data.map",172374462,null),new cljs.core.Symbol(null,"merge+","merge+",-1829307131,null),"src/cljc/quantum/core/data/map.cljc",13,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"variadic","variadic",882626057),true,new cljs.core.Keyword(null,"max-fixed-arity","max-fixed-arity",-690205543),(1),new cljs.core.Keyword(null,"method-params","method-params",-980792179),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.list(new cljs.core.Symbol(null,"map-0","map-0",1310108841,null),new cljs.core.Symbol(null,"maps","maps",-71029607,null))], null),new cljs.core.Keyword(null,"arglists","arglists",1661989754),cljs.core.list(new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"map-0","map-0",1310108841,null),new cljs.core.Symbol(null,"&","&",-2144855648,null),new cljs.core.Symbol(null,"maps","maps",-71029607,null)], null)),new cljs.core.Keyword(null,"arglists-meta","arglists-meta",1944829838),cljs.core.list(null)], null),1,38,"Alex Gunnarson",38,cljs.core.list(new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"map-0","map-0",1310108841,null),new cljs.core.Symbol(null,"&","&",-2144855648,null),new cljs.core.Symbol(null,"maps","maps",-71029607,null)], null)),"A performant drop-in replacemen for |clojure.core/merge|.",(cljs.core.truth_(quantum.core.data.map.merge_PLUS_)?quantum.core.data.map.merge_PLUS_.cljs$lang$test:null)]))),cljs.core.meta.call(null,new cljs.core.Var(function(){return quantum.core.data.map.merge_PLUS_;},new cljs.core.Symbol("quantum.core.data.map","merge+","quantum.core.data.map/merge+",1894972433,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"performance","performance",1987578184),new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"top-fn","top-fn",-2056129173),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"attribution","attribution",1937239286),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],["782.922731 ms |merge+| vs. 1.133217 sec normal |merge| ; 1.5 times faster!",new cljs.core.Symbol(null,"quantum.core.data.map","quantum.core.data.map",172374462,null),new cljs.core.Symbol(null,"merge+","merge+",-1829307131,null),"src/cljc/quantum/core/data/map.cljc",13,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"variadic","variadic",882626057),true,new cljs.core.Keyword(null,"max-fixed-arity","max-fixed-arity",-690205543),(1),new cljs.core.Keyword(null,"method-params","method-params",-980792179),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.list(new cljs.core.Symbol(null,"map-0","map-0",1310108841,null),new cljs.core.Symbol(null,"maps","maps",-71029607,null))], null),new cljs.core.Keyword(null,"arglists","arglists",1661989754),cljs.core.list(new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"map-0","map-0",1310108841,null),new cljs.core.Symbol(null,"&","&",-2144855648,null),new cljs.core.Symbol(null,"maps","maps",-71029607,null)], null)),new cljs.core.Keyword(null,"arglists-meta","arglists-meta",1944829838),cljs.core.list(null)], null),1,38,"Alex Gunnarson",38,cljs.core.list(new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"map-0","map-0",1310108841,null),new cljs.core.Symbol(null,"&","&",-2144855648,null),new cljs.core.Symbol(null,"maps","maps",-71029607,null)], null)),"A performant drop-in replacemen for |clojure.core/merge|.",(cljs.core.truth_(quantum.core.data.map.merge_PLUS_)?quantum.core.data.map.merge_PLUS_.cljs$lang$test:null)]))));

if(new cljs.core.Keyword(null,"macro","macro",-867863404).cljs$core$IFn$_invoke$arity$1(cljs.core.meta.call(null,orig_var__20471__auto___30634)) === true){
cljs.core.alter_meta_BANG_.call(null,new cljs.core.Var(function(){return quantum.core.collections.merge;},new cljs.core.Symbol("quantum.core.collections","merge","quantum.core.collections/merge",1262496735,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"merge","merge",-163787882,null),"src/cljc/quantum/core/collections.cljc",16,1,162,162,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.merge)?quantum.core.collections.merge.cljs$lang$test:null)])),cljs.core.assoc,new cljs.core.Keyword(null,"macro","macro",-867863404),true);
} else {
}
} else {
}

new cljs.core.Var(function(){return quantum.core.collections.merge;},new cljs.core.Symbol("quantum.core.collections","merge","quantum.core.collections/merge",1262496735,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"merge","merge",-163787882,null),"src/cljc/quantum/core/collections.cljc",16,1,162,162,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.merge)?quantum.core.collections.merge.cljs$lang$test:null)]));
quantum.core.collections.frest = (function quantum$core$collections$frest(x__22733__auto__){
return quantum.core.collections.first.call(null,quantum.core.collections.rest.call(null,x__22733__auto__));
});
quantum.core.collections.lrepeatedly = cljs.core.repeatedly;
quantum.core.collections.range_PLUS_ = quantum.core.reducers.range_PLUS_;
var orig_var__20471__auto___30639 = new cljs.core.Var(function(){return cljs.core.range;},new cljs.core.Symbol("cljs.core","range","cljs.core/range",-1421369894,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"top-fn","top-fn",-2056129173),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"cljs.core","cljs.core",770546058,null),new cljs.core.Symbol(null,"range","range",-1014743483,null),"cljs/core.cljs",(12),new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"variadic","variadic",882626057),false,new cljs.core.Keyword(null,"max-fixed-arity","max-fixed-arity",-690205543),(3),new cljs.core.Keyword(null,"method-params","method-params",-980792179),cljs.core.list(cljs.core.PersistentVector.EMPTY,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"end","end",1372345569,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"start","start",1285322546,null),new cljs.core.Symbol(null,"end","end",1372345569,null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"start","start",1285322546,null),new cljs.core.Symbol(null,"end","end",1372345569,null),new cljs.core.Symbol(null,"step","step",-1365547645,null)], null)),new cljs.core.Keyword(null,"arglists","arglists",1661989754),cljs.core.list(cljs.core.PersistentVector.EMPTY,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"end","end",1372345569,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"start","start",1285322546,null),new cljs.core.Symbol(null,"end","end",1372345569,null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"start","start",1285322546,null),new cljs.core.Symbol(null,"end","end",1372345569,null),new cljs.core.Symbol(null,"step","step",-1365547645,null)], null)),new cljs.core.Keyword(null,"arglists-meta","arglists-meta",1944829838),cljs.core.list(null,null,null,null)], null),(1),(8226),(8226),cljs.core.list(cljs.core.PersistentVector.EMPTY,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"end","end",1372345569,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"start","start",1285322546,null),new cljs.core.Symbol(null,"end","end",1372345569,null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"start","start",1285322546,null),new cljs.core.Symbol(null,"end","end",1372345569,null),new cljs.core.Symbol(null,"step","step",-1365547645,null)], null)),"Returns a lazy seq of nums from start (inclusive) to end\n   (exclusive), by step, where start defaults to 0, step to 1,\n   and end to infinity.",(cljs.core.truth_(cljs.core.range)?cljs.core.range.cljs$lang$test:null)]));
if(cljs.core.truth_(orig_var__20471__auto___30639.hasRoot())){
quantum.core.collections.lrange = cljs.core.with_meta.call(null,cljs.core.deref.call(null,new cljs.core.Var(function(){return cljs.core.range;},new cljs.core.Symbol("cljs.core","range","cljs.core/range",-1421369894,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"top-fn","top-fn",-2056129173),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"cljs.core","cljs.core",770546058,null),new cljs.core.Symbol(null,"range","range",-1014743483,null),"cljs/core.cljs",(12),new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"variadic","variadic",882626057),false,new cljs.core.Keyword(null,"max-fixed-arity","max-fixed-arity",-690205543),(3),new cljs.core.Keyword(null,"method-params","method-params",-980792179),cljs.core.list(cljs.core.PersistentVector.EMPTY,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"end","end",1372345569,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"start","start",1285322546,null),new cljs.core.Symbol(null,"end","end",1372345569,null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"start","start",1285322546,null),new cljs.core.Symbol(null,"end","end",1372345569,null),new cljs.core.Symbol(null,"step","step",-1365547645,null)], null)),new cljs.core.Keyword(null,"arglists","arglists",1661989754),cljs.core.list(cljs.core.PersistentVector.EMPTY,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"end","end",1372345569,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"start","start",1285322546,null),new cljs.core.Symbol(null,"end","end",1372345569,null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"start","start",1285322546,null),new cljs.core.Symbol(null,"end","end",1372345569,null),new cljs.core.Symbol(null,"step","step",-1365547645,null)], null)),new cljs.core.Keyword(null,"arglists-meta","arglists-meta",1944829838),cljs.core.list(null,null,null,null)], null),(1),(8226),(8226),cljs.core.list(cljs.core.PersistentVector.EMPTY,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"end","end",1372345569,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"start","start",1285322546,null),new cljs.core.Symbol(null,"end","end",1372345569,null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"start","start",1285322546,null),new cljs.core.Symbol(null,"end","end",1372345569,null),new cljs.core.Symbol(null,"step","step",-1365547645,null)], null)),"Returns a lazy seq of nums from start (inclusive) to end\n   (exclusive), by step, where start defaults to 0, step to 1,\n   and end to infinity.",(cljs.core.truth_(cljs.core.range)?cljs.core.range.cljs$lang$test:null)]))),cljs.core.meta.call(null,new cljs.core.Var(function(){return cljs.core.range;},new cljs.core.Symbol("cljs.core","range","cljs.core/range",-1421369894,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"top-fn","top-fn",-2056129173),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"cljs.core","cljs.core",770546058,null),new cljs.core.Symbol(null,"range","range",-1014743483,null),"cljs/core.cljs",(12),new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"variadic","variadic",882626057),false,new cljs.core.Keyword(null,"max-fixed-arity","max-fixed-arity",-690205543),(3),new cljs.core.Keyword(null,"method-params","method-params",-980792179),cljs.core.list(cljs.core.PersistentVector.EMPTY,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"end","end",1372345569,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"start","start",1285322546,null),new cljs.core.Symbol(null,"end","end",1372345569,null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"start","start",1285322546,null),new cljs.core.Symbol(null,"end","end",1372345569,null),new cljs.core.Symbol(null,"step","step",-1365547645,null)], null)),new cljs.core.Keyword(null,"arglists","arglists",1661989754),cljs.core.list(cljs.core.PersistentVector.EMPTY,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"end","end",1372345569,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"start","start",1285322546,null),new cljs.core.Symbol(null,"end","end",1372345569,null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"start","start",1285322546,null),new cljs.core.Symbol(null,"end","end",1372345569,null),new cljs.core.Symbol(null,"step","step",-1365547645,null)], null)),new cljs.core.Keyword(null,"arglists-meta","arglists-meta",1944829838),cljs.core.list(null,null,null,null)], null),(1),(8226),(8226),cljs.core.list(cljs.core.PersistentVector.EMPTY,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"end","end",1372345569,null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"start","start",1285322546,null),new cljs.core.Symbol(null,"end","end",1372345569,null)], null),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"start","start",1285322546,null),new cljs.core.Symbol(null,"end","end",1372345569,null),new cljs.core.Symbol(null,"step","step",-1365547645,null)], null)),"Returns a lazy seq of nums from start (inclusive) to end\n   (exclusive), by step, where start defaults to 0, step to 1,\n   and end to infinity.",(cljs.core.truth_(cljs.core.range)?cljs.core.range.cljs$lang$test:null)]))));

if(new cljs.core.Keyword(null,"macro","macro",-867863404).cljs$core$IFn$_invoke$arity$1(cljs.core.meta.call(null,orig_var__20471__auto___30639)) === true){
cljs.core.alter_meta_BANG_.call(null,new cljs.core.Var(function(){return quantum.core.collections.lrange;},new cljs.core.Symbol("quantum.core.collections","lrange","quantum.core.collections/lrange",1850669893,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"lrange","lrange",-850781556,null),"src/cljc/quantum/core/collections.cljc",17,1,199,199,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.lrange)?quantum.core.collections.lrange.cljs$lang$test:null)])),cljs.core.assoc,new cljs.core.Keyword(null,"macro","macro",-867863404),true);
} else {
}
} else {
}

new cljs.core.Var(function(){return quantum.core.collections.lrange;},new cljs.core.Symbol("quantum.core.collections","lrange","quantum.core.collections/lrange",1850669893,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760),new cljs.core.Keyword(null,"name","name",1843675177),new cljs.core.Keyword(null,"file","file",-1269645878),new cljs.core.Keyword(null,"end-column","end-column",1425389514),new cljs.core.Keyword(null,"column","column",2078222095),new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.Keyword(null,"end-line","end-line",1837326455),new cljs.core.Keyword(null,"arglists","arglists",1661989754),new cljs.core.Keyword(null,"doc","doc",1913296891),new cljs.core.Keyword(null,"test","test",577538877)],[new cljs.core.Symbol(null,"quantum.core.collections","quantum.core.collections",-1715813248,null),new cljs.core.Symbol(null,"lrange","lrange",-850781556,null),"src/cljc/quantum/core/collections.cljc",17,1,199,199,cljs.core.List.EMPTY,null,(cljs.core.truth_(quantum.core.collections.lrange)?quantum.core.collections.lrange.cljs$lang$test:null)]));
quantum.core.collections.range = (function quantum$core$collections$range(){
var G__30641 = arguments.length;
switch (G__30641) {
case 0:
return quantum.core.collections.range.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return quantum.core.collections.range.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return quantum.core.collections.range.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.collections.range.cljs$core$IFn$_invoke$arity$0 = (function (){
return quantum.core.collections.lrange.call(null);
});

quantum.core.collections.range.cljs$core$IFn$_invoke$arity$1 = (function (a){
return quantum.core.collections.redv.call(null,quantum.core.collections.range_PLUS_.call(null,a));
});

quantum.core.collections.range.cljs$core$IFn$_invoke$arity$2 = (function (a,b){
return quantum.core.collections.redv.call(null,quantum.core.collections.range_PLUS_.call(null,a,b));
});

quantum.core.collections.range.cljs$lang$maxFixedArity = 2;
quantum.core.collections.repeat = (function quantum$core$collections$repeat(){
var G__30644 = arguments.length;
switch (G__30644) {
case 1:
return quantum.core.collections.repeat.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return quantum.core.collections.repeat.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.collections.repeat.cljs$core$IFn$_invoke$arity$1 = (function (obj){
return cljs.core.repeat.call(null,obj);
});

quantum.core.collections.repeat.cljs$core$IFn$_invoke$arity$2 = (function (n,obj){
return cljs.core.reduce.call(null,(function (ret__23424__auto__,i){
return cljs.core.conj.call(null,ret__23424__auto__,obj);
}),cljs.core.PersistentVector.EMPTY,quantum.core.collections.range.call(null,n));
});

quantum.core.collections.repeat.cljs$lang$maxFixedArity = 2;
/**
 * Returns the absolute difference between a and b.
 * That is, (a diff b) union (b diff a).
 */
quantum.core.collections.abs_difference = (function quantum$core$collections$abs_difference(a,b){
return quantum.core.data.set.union.call(null,quantum.core.data.set.difference.call(null,a,b),quantum.core.data.set.difference.call(null,b,a));
});
/**
 * Like |reduce|, but reduces over two items in a collection at a time.
 * 
 * Its function @func must take three arguments:
 * 1) The accumulated return value of the reduction function
 * 2) The                next item in the collection being reduced over
 * 3) The item after the next item in the collection being reduced over
 */
quantum.core.collections.reduce_2 = (function quantum$core$collections$reduce_2(func,init,coll){
var ret = init;
var coll_n = coll;
while(true){
if(cljs.core.empty_QMARK_.call(null,coll_n)){
return ret;
} else {
var G__30646 = func.call(null,ret,quantum.core.collections.first.call(null,coll_n),quantum.core.collections.second.call(null,coll_n));
var G__30647 = quantum.core.collections.rest.call(null,quantum.core.collections.rest.call(null,coll_n));
ret = G__30646;
coll_n = G__30647;
continue;
}
break;
}
});
/**
 * Returns the first result of a |filter| operation.
 * Uses lazy |filter| so as to do it in the fastest possible way.
 */
quantum.core.collections.ffilter = (function quantum$core$collections$ffilter(filter_fn,coll){
return quantum.core.collections.first.call(null,cljs.core.filter.call(null,filter_fn,coll));
});
quantum.core.collections.ffilter_PLUS_ = (function quantum$core$collections$ffilter_PLUS_(pred,coll){
return quantum.core.collections.reduce.call(null,(function (ret,elem_n){
if(cljs.core.truth_(pred.call(null,elem_n))){
return cljs.core.reduced.call(null,elem_n);
} else {
return null;
}
}),null,coll);
});
quantum.core.collections.ffilteri_PLUS_ = (function quantum$core$collections$ffilteri_PLUS_(pred,coll){
var n__23387__auto__ = cljs.core.volatile_BANG_.call(null,cljs.core.long$.call(null,(-1)));
return cljs.core.reduce.call(null,((function (n__23387__auto__){
return (function (ret_n__23388__auto__,elem__23389__auto__){
cljs.core._vreset_BANG_.call(null,n__23387__auto__,(cljs.core._deref.call(null,n__23387__auto__) + (1)));

return ((function (n__23387__auto__){
return (function (ret,elem_n,index_n){
if(cljs.core.truth_(pred.call(null,elem_n))){
return cljs.core.reduced.call(null,quantum.core.data.map.map_entry.call(null,index_n,elem_n));
} else {
if(cljs.core._EQ_.call(null,index_n,quantum.core.collections.lasti.call(null,coll))){
return quantum.core.data.map.map_entry.call(null,(-1),null);
} else {
return quantum.core.data.map.map_entry.call(null,(index_n + (1)),null);
}
}
});})(n__23387__auto__))
.call(null,ret_n__23388__auto__,elem__23389__auto__,cljs.core.deref.call(null,n__23387__auto__));
});})(n__23387__auto__))
,quantum.core.data.map.map_entry.call(null,(0),null),coll);
});
quantum.core.collections.filteri_PLUS_ = (function quantum$core$collections$filteri_PLUS_(pred,coll){
if(cljs.core.truth_((function (){var and__18061__auto__ = quantum.core.type.editable_QMARK_.call(null,coll);
if(cljs.core.truth_(and__18061__auto__)){
return (cljs.core.counted_QMARK_.call(null,coll)) && ((cljs.core.count.call(null,coll) > quantum.core.type.transient_threshold));
} else {
return and__18061__auto__;
}
})())){
return cljs.core.persistent_BANG_.call(null,(function (){var n__23387__auto__ = cljs.core.volatile_BANG_.call(null,cljs.core.long$.call(null,(-1)));
return cljs.core.reduce.call(null,((function (n__23387__auto__){
return (function (ret_n__23388__auto__,elem__23389__auto__){
cljs.core._vreset_BANG_.call(null,n__23387__auto__,(cljs.core._deref.call(null,n__23387__auto__) + (1)));

return ((function (n__23387__auto__){
return (function (ret,elem_n,n){
if(cljs.core.truth_(pred.call(null,elem_n))){
return cljs.core.conj_BANG_.call(null,ret,quantum.core.data.map.map_entry.call(null,n,elem_n));
} else {
return ret;
}
});})(n__23387__auto__))
.call(null,ret_n__23388__auto__,elem__23389__auto__,cljs.core.deref.call(null,n__23387__auto__));
});})(n__23387__auto__))
,cljs.core.transient$.call(null,cljs.core.PersistentVector.EMPTY),coll);
})());
} else {
var n__23387__auto__ = cljs.core.volatile_BANG_.call(null,cljs.core.long$.call(null,(-1)));
return cljs.core.reduce.call(null,((function (n__23387__auto__){
return (function (ret_n__23388__auto__,elem__23389__auto__){
cljs.core._vreset_BANG_.call(null,n__23387__auto__,(cljs.core._deref.call(null,n__23387__auto__) + (1)));

return ((function (n__23387__auto__){
return (function (ret,elem_n,n){
if(cljs.core.truth_(pred.call(null,elem_n))){
return cljs.core.conj.call(null,ret,quantum.core.data.map.map_entry.call(null,n,elem_n));
} else {
return ret;
}
});})(n__23387__auto__))
.call(null,ret_n__23388__auto__,elem__23389__auto__,cljs.core.deref.call(null,n__23387__auto__));
});})(n__23387__auto__))
,cljs.core.PersistentVector.EMPTY,coll);
}
});
quantum.core.collections.indices_of_PLUS_ = (function quantum$core$collections$indices_of_PLUS_(coll,elem_0){
if(cljs.core.truth_((function (){var and__18061__auto__ = quantum.core.type.editable_QMARK_.call(null,coll);
if(cljs.core.truth_(and__18061__auto__)){
return (cljs.core.counted_QMARK_.call(null,coll)) && ((cljs.core.count.call(null,coll) > quantum.core.type.transient_threshold));
} else {
return and__18061__auto__;
}
})())){
return cljs.core.persistent_BANG_.call(null,(function (){var n__23387__auto__ = cljs.core.volatile_BANG_.call(null,cljs.core.long$.call(null,(-1)));
return cljs.core.reduce.call(null,((function (n__23387__auto__){
return (function (ret_n__23388__auto__,elem__23389__auto__){
cljs.core._vreset_BANG_.call(null,n__23387__auto__,(cljs.core._deref.call(null,n__23387__auto__) + (1)));

return ((function (n__23387__auto__){
return (function (ret,elem_n,n){
if(cljs.core._EQ_.call(null,elem_0,elem_n)){
return cljs.core.conj_BANG_.call(null,ret,n);
} else {
return ret;
}
});})(n__23387__auto__))
.call(null,ret_n__23388__auto__,elem__23389__auto__,cljs.core.deref.call(null,n__23387__auto__));
});})(n__23387__auto__))
,cljs.core.transient$.call(null,cljs.core.PersistentVector.EMPTY),coll);
})());
} else {
var n__23387__auto__ = cljs.core.volatile_BANG_.call(null,cljs.core.long$.call(null,(-1)));
return cljs.core.reduce.call(null,((function (n__23387__auto__){
return (function (ret_n__23388__auto__,elem__23389__auto__){
cljs.core._vreset_BANG_.call(null,n__23387__auto__,(cljs.core._deref.call(null,n__23387__auto__) + (1)));

return ((function (n__23387__auto__){
return (function (ret,elem_n,n){
if(cljs.core._EQ_.call(null,elem_0,elem_n)){
return cljs.core.conj.call(null,ret,n);
} else {
return ret;
}
});})(n__23387__auto__))
.call(null,ret_n__23388__auto__,elem__23389__auto__,cljs.core.deref.call(null,n__23387__auto__));
});})(n__23387__auto__))
,cljs.core.PersistentVector.EMPTY,coll);
}
});
quantum.core.collections.takel_PLUS_ = (function quantum$core$collections$takel_PLUS_(coll,n){
return quantum.core.collections.getr.call(null,coll,(0),n);
});
/**
 * Take starting at and including index n.
 */
quantum.core.collections.take_from_PLUS_ = (function quantum$core$collections$take_from_PLUS_(obj,n){
return quantum.core.collections.getr.call(null,obj,n,quantum.core.collections.count.call(null,obj));
});
quantum.core.collections.take_fromi_PLUS_ = (function quantum$core$collections$take_fromi_PLUS_(obj,sub_obj){
return quantum.core.collections.take_from_PLUS_.call(null,obj,quantum.core.collections.index_of.call(null,obj,sub_obj));
});
quantum.core.collections.take_afteri_PLUS_ = (function quantum$core$collections$take_afteri_PLUS_(obj,sub_obj){
return quantum.core.collections.take_from_PLUS_.call(null,obj,(quantum.core.collections.index_of.call(null,obj,sub_obj) + quantum.core.collections.count.call(null,sub_obj)));
});
quantum.core.collections.take_untili_PLUS_ = (function quantum$core$collections$take_untili_PLUS_(obj,sub_obj){
return quantum.core.collections.getr.call(null,obj,(0),quantum.core.collections.index_of.call(null,obj,sub_obj));
});
/**
 * Take up to and including right index of.
 */
quantum.core.collections.takeri_PLUS_ = (function quantum$core$collections$takeri_PLUS_(super$,sub){
var index_r_0 = quantum.core.collections.last_index_of.call(null,super$,sub);
var index_r = ((cljs.core._EQ_.call(null,(-1),index_r_0))?(function(){throw [cljs.core.str("Index of"),cljs.core.str(quantum.core.string.squote.call(null,sub)),cljs.core.str("not found.")].join('')})():index_r_0);
return quantum.core.collections.getr.call(null,super$,index_r,quantum.core.collections.lasti.call(null,super$));
});
/**
 * Until right index of.
 */
quantum.core.collections.taker_untili_PLUS_ = (function quantum$core$collections$taker_untili_PLUS_(super$,sub){
var index_r_30648 = (function (){var obj_f__22909__auto__ = quantum.core.collections.last_index_of.call(null,super$,sub);
if(cljs.core.truth_(quantum.core.logic.fn_EQ_.call(null,(-1)).call(null,obj_f__22909__auto__))){
throw [cljs.core.str("Index of"),cljs.core.str(quantum.core.string.squote.call(null,sub)),cljs.core.str("not found.")].join('');
} else {
return obj_f__22909__auto__;
}
})();

return quantum.core.collections.getr.call(null,super$,(quantum.core.collections.last_index_of.call(null,super$,sub) + (1)),quantum.core.collections.lasti.call(null,super$));
});
quantum.core.collections.dropl_PLUS_ = (function quantum$core$collections$dropl_PLUS_(obj,n){
return quantum.core.collections.getr.call(null,obj,n,quantum.core.collections.count.call(null,obj));
});
quantum.core.collections.dropr_PLUS_ = (function quantum$core$collections$dropr_PLUS_(obj,n){
return quantum.core.collections.getr.call(null,obj,(0),(quantum.core.collections.lasti.call(null,obj) - n));
});
quantum.core.collections.merge_keep_left = (function quantum$core$collections$merge_keep_left(a,b){
return quantum.core.collections.merge.call(null,b,a);
});
quantum.core.collections.split_remove_PLUS_ = (function quantum$core$collections$split_remove_PLUS_(coll,split_at_obj){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [quantum.core.collections.take_untili_PLUS_.call(null,coll,split_at_obj),quantum.core.collections.take_afteri_PLUS_.call(null,coll,split_at_obj)], null);
});
/**
 * Applies a list of functions, @fns, separately to an object, @coll.
 * A good use case is returning values from an associative structure with keys as @fns.
 * Returns a vector of the results.
 */
quantum.core.collections.select = (function quantum$core$collections$select(){
var argseq__19113__auto__ = ((((1) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(1)),(0))):null);
return quantum.core.collections.select.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__19113__auto__);
});

quantum.core.collections.select.cljs$core$IFn$_invoke$arity$variadic = (function (coll,fns){
return cljs.core.apply.call(null,cljs.core.juxt,fns).call(null,coll);
});

quantum.core.collections.select.cljs$lang$maxFixedArity = (1);

quantum.core.collections.select.cljs$lang$applyTo = (function (seq30649){
var G__30650 = cljs.core.first.call(null,seq30649);
var seq30649__$1 = cljs.core.next.call(null,seq30649);
return quantum.core.collections.select.cljs$core$IFn$_invoke$arity$variadic(G__30650,seq30649__$1);
});
/**
 * For compare-fns that don't have enough arity to do, say,
 * |(apply time/latest [date1 date2 date3])|.
 * 
 * Gets the most "extreme" element in collection @coll,
 * "extreme" being defined on the @compare-fn.
 * 
 * In the case of |time/latest|, it would return the latest
 * DateTime in a collection.
 * 
 * In the case of |>| (greater than), it would return the
 * greatest element in the collection:
 * 
 * (comparator-extreme-of [1 2 3] (fn [a b] (if (> a b) a b)) )
 * :: 3
 * 
 * |(fn [a b] (if (> a b) a b))| is the same thing as
 * |(choice-comparator >)|.
 */
quantum.core.collections.comparator_extreme_of = (function quantum$core$collections$comparator_extreme_of(coll,compare_fn){
var n__23387__auto__ = cljs.core.volatile_BANG_.call(null,cljs.core.long$.call(null,(-1)));
return cljs.core.reduce.call(null,((function (n__23387__auto__){
return (function (ret_n__23388__auto__,elem__23389__auto__){
cljs.core._vreset_BANG_.call(null,n__23387__auto__,(cljs.core._deref.call(null,n__23387__auto__) + (1)));

return ((function (n__23387__auto__){
return (function (ret,elem,n){
if(cljs.core._EQ_.call(null,n,(0))){
return elem;
} else {
return compare_fn.call(null,ret,elem);
}
});})(n__23387__auto__))
.call(null,ret_n__23388__auto__,elem__23389__auto__,cljs.core.deref.call(null,n__23387__auto__));
});})(n__23387__auto__))
,null,coll);
});
quantum.core.collections.coll_if = (function quantum$core$collections$coll_if(obj){
var obj_f__22903__auto__ = obj;
if(cljs.core.truth_(quantum.core.logic.fn_not.call(null,cljs.core.coll_QMARK_).call(null,obj_f__22903__auto__))){
return (new cljs.core.PersistentVector(null,1,(5),cljs.core.PersistentVector.EMPTY_NODE,[obj_f__22903__auto__],null));
} else {
return obj_f__22903__auto__;
}
});
quantum.core.collections.seq_if = (function quantum$core$collections$seq_if(obj){
var obj__30652 = obj;
if(cljs.core.truth_((function (){var or__18073__auto__ = cljs.core._EQ_.call(null,quantum.core.logic.fn_or.call(null,cljs.core.seq_QMARK_,cljs.core.nil_QMARK_),new cljs.core.Keyword(null,"else","else",-1508377146));
if(or__18073__auto__){
return or__18073__auto__;
} else {
return quantum.core.logic.fn_or.call(null,cljs.core.seq_QMARK_,cljs.core.nil_QMARK_).call(null,obj__30652);
}
})())){
return cljs.core.identity.call(null,obj__30652);
} else {
if((cljs.core._EQ_.call(null,cljs.core.coll_QMARK_,new cljs.core.Keyword(null,"else","else",-1508377146))) || (cljs.core.coll_QMARK_.call(null,obj__30652))){
return cljs.core.seq.call(null,obj__30652);
} else {
if(cljs.core.truth_((function (){var or__18073__auto__ = cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"else","else",-1508377146),new cljs.core.Keyword(null,"else","else",-1508377146));
if(or__18073__auto__){
return or__18073__auto__;
} else {
return new cljs.core.Keyword(null,"else","else",-1508377146).cljs$core$IFn$_invoke$arity$1(obj__30652);
}
})())){
return cljs.core._conj.call(null,cljs.core.List.EMPTY,obj__30652);
} else {
throw (new java.lang.IllegalArgumentException([cljs.core.str("No matching clause for "),cljs.core.str(obj__30652)].join('')));
}
}
}
});
/**
 * Takes a seqable and returns a lazy sequence that
 * is maximally lazy and doesn't realize elements due to either
 * chunking or apply.
 * 
 * Useful when you don't want chunking, for instance,
 * (first awesome-website? (map slurp <a-bunch-of-urls>))
 * may slurp up to 31 unneed webpages, whereas
 * (first awesome-website? (map slurp (unchunk <a-bunch-of-urls>)))
 * is guaranteed to stop slurping after the first awesome website.
 * 
 * Taken from http://stackoverflow.com/questions/3407876/how-do-i-avoid-clojures-chunking-behavior-for-lazy-seqs-that-i-want-to-short-ci
 */
quantum.core.collections.unchunk = (function quantum$core$collections$unchunk(s){
if(cljs.core.seq.call(null,s)){
return cljs.core.cons.call(null,quantum.core.collections.first.call(null,s),quantum.core.collections.lseq.call(null,s.call(null,quantum.core.collections.rest,quantum$core$collections$unchunk)));
} else {
return null;
}
});
/**
 * Like |key| but more robust.
 */
quantum.core.collections.key_PLUS_ = (function quantum$core$collections$key_PLUS_(){
var G__30655 = arguments.length;
switch (G__30655) {
case 1:
return quantum.core.collections.key_PLUS_.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return quantum.core.collections.key_PLUS_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.collections.key_PLUS_.cljs$core$IFn$_invoke$arity$1 = (function (obj){
try{var obj_f__22879__auto__ = obj;
if(cljs.core.vector_QMARK_.call(null,obj_f__22879__auto__)){
return quantum.core.collections.first.call(null,obj_f__22879__auto__);
} else {
return cljs.core.key.call(null,obj_f__22879__auto__);
}
}catch (e30656){if((e30656 instanceof Error)){
var _ = e30656;
cljs.core.println.call(null,"Error in key+ with obj:",quantum.core.type.class$.call(null,obj));

return null;
} else {
throw e30656;

}
}});

quantum.core.collections.key_PLUS_.cljs$core$IFn$_invoke$arity$2 = (function (k,v){
return k;
});

quantum.core.collections.key_PLUS_.cljs$lang$maxFixedArity = 2;
/**
 * Like |val| but more robust.
 */
quantum.core.collections.val_PLUS_ = (function quantum$core$collections$val_PLUS_(){
var G__30659 = arguments.length;
switch (G__30659) {
case 1:
return quantum.core.collections.val_PLUS_.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return quantum.core.collections.val_PLUS_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.collections.val_PLUS_.cljs$core$IFn$_invoke$arity$1 = (function (obj){
try{var obj_f__22879__auto__ = obj;
if(cljs.core.vector_QMARK_.call(null,obj_f__22879__auto__)){
return quantum.core.collections.second.call(null,obj_f__22879__auto__);
} else {
return cljs.core.key.call(null,obj_f__22879__auto__);
}
}catch (e30660){if((e30660 instanceof Error)){
var _ = e30660;
return null;
} else {
throw e30660;

}
}});

quantum.core.collections.val_PLUS_.cljs$core$IFn$_invoke$arity$2 = (function (k,v){
return v;
});

quantum.core.collections.val_PLUS_.cljs$lang$maxFixedArity = 2;
quantum.core.collections.fkey_PLUS_ = (function quantum$core$collections$fkey_PLUS_(m){
return quantum.core.collections.key_PLUS_.call(null,quantum.core.collections.first.call(null,m));
});
quantum.core.collections.fval_PLUS_ = (function quantum$core$collections$fval_PLUS_(m){
return quantum.core.collections.val_PLUS_.call(null,quantum.core.collections.first.call(null,m));
});
quantum.core.collections.up_val = (function quantum$core$collections$up_val(m,k){
return cljs.core.PersistentHashMap.fromArrays([quantum.core.collections.get.call(null,m,k)],[cljs.core.dissoc.call(null,m,k)]);
});
quantum.core.collections.rename_keys = (function quantum$core$collections$rename_keys(m_0,rename_m){
return quantum.core.collections.reduce.call(null,(function (ret,k_0,k_f){
return cljs.core.dissoc.call(null,cljs.core.assoc.call(null,ret,k_f,quantum.core.collections.get.call(null,ret,k_0)),k_0);
}),m_0,rename_m);
});
quantum.core.collections.get_in_PLUS_ = (function quantum$core$collections$get_in_PLUS_(coll,p__30662){
var vec__30664 = p__30662;
var iden = cljs.core.nth.call(null,vec__30664,(0),null);
var keys_0 = vec__30664;
if(cljs.core._EQ_.call(null,iden,cljs.core.identity)){
return coll;
} else {
return cljs.core.get_in.call(null,coll,keys_0);
}
});
quantum.core.collections.reverse_PLUS_ = (function quantum$core$collections$reverse_PLUS_(coll){
var obj_f__22879__auto__ = coll;
if(cljs.core.reversible_QMARK_.call(null,obj_f__22879__auto__)){
return cljs.core.rseq.call(null,obj_f__22879__auto__);
} else {
return cljs.core.reverse.call(null,obj_f__22879__auto__);
}
});
/**
 * Does coll have only one element?
 */
quantum.core.collections.single_QMARK_ = quantum.core.logic.fn_and.call(null,cljs.core.seq,quantum.core.logic.fn_not.call(null,cljs.core.next));
/**
 * Like merge-with, but the merging function takes the key being merged
 * as the first argument
 */
quantum.core.collections.merge_with_PLUS_ = (function quantum$core$collections$merge_with_PLUS_(){
var argseq__19113__auto__ = ((((1) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(1)),(0))):null);
return quantum.core.collections.merge_with_PLUS_.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__19113__auto__);
});

quantum.core.collections.merge_with_PLUS_.cljs$core$IFn$_invoke$arity$variadic = (function (f,maps){
if(cljs.core.truth_(quantum.core.logic.any_QMARK_.call(null,cljs.core.identity,maps))){
var merge_entry = (function (m,e){
var k = cljs.core.key.call(null,e);
var v = cljs.core.val.call(null,e);
if(cljs.core.truth_(quantum.core.collections.contains_QMARK_.call(null,m,k))){
return cljs.core.assoc.call(null,m,k,f.call(null,k,quantum.core.collections.get.call(null,m,k),v));
} else {
return cljs.core.assoc.call(null,m,k,v);
}
});
var merge2 = ((function (merge_entry){
return (function() {
var G__30667 = null;
var G__30667__0 = (function (){
return cljs.core.PersistentArrayMap.EMPTY;
});
var G__30667__2 = (function (m1,m2){
return quantum.core.collections.reduce.call(null,merge_entry,(function (){var or__18073__auto__ = m1;
if(cljs.core.truth_(or__18073__auto__)){
return or__18073__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})(),cljs.core.seq.call(null,m2));
});
G__30667 = function(m1,m2){
switch(arguments.length){
case 0:
return G__30667__0.call(this);
case 2:
return G__30667__2.call(this,m1,m2);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
G__30667.cljs$core$IFn$_invoke$arity$0 = G__30667__0;
G__30667.cljs$core$IFn$_invoke$arity$2 = G__30667__2;
return G__30667;
})()
;})(merge_entry))
;
return quantum.core.collections.reduce.call(null,merge2,maps);
} else {
return null;
}
});

quantum.core.collections.merge_with_PLUS_.cljs$lang$maxFixedArity = (1);

quantum.core.collections.merge_with_PLUS_.cljs$lang$applyTo = (function (seq30665){
var G__30666 = cljs.core.first.call(null,seq30665);
var seq30665__$1 = cljs.core.next.call(null,seq30665);
return quantum.core.collections.merge_with_PLUS_.cljs$core$IFn$_invoke$arity$variadic(G__30666,seq30665__$1);
});
/**
 * Merges into the left map all elements of the right map whose
 * keys are found in the left map.
 * 
 * Combines using @f, a |merge-with| function.
 */
quantum.core.collections.merge_vals_left = (function quantum$core$collections$merge_vals_left(left,right,f){
return cljs.core.persistent_BANG_.call(null,quantum.core.collections.reduce.call(null,(function (left_f,k_right,v_right){
var v_left = quantum.core.collections.get.call(null,left,k_right);
if((v_left == null)){
return left_f;
} else {
var merged_vs = quantum.core.collections.merge_with_PLUS_.call(null,f,v_left,v_right);
return cljs.core.assoc_BANG_.call(null,left_f,k_right,merged_vs);
}
}),cljs.core.transient$.call(null,left),right));
});
quantum.core.collections.concat_PLUS__PLUS_ = (function quantum$core$collections$concat_PLUS__PLUS_(){
var G__30671 = arguments.length;
switch (G__30671) {
case 1:
return quantum.core.collections.concat_PLUS__PLUS_.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
default:
var argseq__19124__auto__ = (new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(1)),(0)));
return quantum.core.collections.concat_PLUS__PLUS_.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__19124__auto__);

}
});

quantum.core.collections.concat_PLUS__PLUS_.cljs$core$IFn$_invoke$arity$1 = (function (coll){
try{return quantum.core.collections.reduce.call(null,quantum.core.data.vector.catvec,coll);
}catch (e30672){if((e30672 instanceof quantum.core.ns.Exception)){
var e = e30672;
return quantum.core.collections.reduce.call(null,quantum.core.function$.zeroid.call(null,quantum.core.collections.into,cljs.core.PersistentVector.EMPTY),coll);
} else {
throw e30672;

}
}});

quantum.core.collections.concat_PLUS__PLUS_.cljs$core$IFn$_invoke$arity$variadic = (function (coll,colls){
try{return cljs.core.apply.call(null,quantum.core.data.vector.catvec,coll,colls);
}catch (e30673){if((e30673 instanceof quantum.core.ns.Exception)){
var e = e30673;
return quantum.core.collections.into.call(null,cljs.core.PersistentVector.EMPTY,coll,colls);
} else {
throw e30673;

}
}});

quantum.core.collections.concat_PLUS__PLUS_.cljs$lang$applyTo = (function (seq30668){
var G__30669 = cljs.core.first.call(null,seq30668);
var seq30668__$1 = cljs.core.next.call(null,seq30668);
return quantum.core.collections.concat_PLUS__PLUS_.cljs$core$IFn$_invoke$arity$variadic(G__30669,seq30668__$1);
});

quantum.core.collections.concat_PLUS__PLUS_.cljs$lang$maxFixedArity = (1);

quantum.core.collections.Contains_QMARK_ = (function (){var obj30676 = {};
return obj30676;
})();

quantum.core.collections.contains_QMARK_ = (function quantum$core$collections$contains_QMARK_(coll,elem){
if((function (){var and__18061__auto__ = coll;
if(and__18061__auto__){
return coll.quantum$core$collections$Contains_QMARK_$contains_QMARK_$arity$2;
} else {
return and__18061__auto__;
}
})()){
return coll.quantum$core$collections$Contains_QMARK_$contains_QMARK_$arity$2(coll,elem);
} else {
var x__18709__auto__ = (((coll == null))?null:coll);
return (function (){var or__18073__auto__ = (quantum.core.collections.contains_QMARK_[goog.typeOf(x__18709__auto__)]);
if(or__18073__auto__){
return or__18073__auto__;
} else {
var or__18073__auto____$1 = (quantum.core.collections.contains_QMARK_["_"]);
if(or__18073__auto____$1){
return or__18073__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"Contains?.contains?",coll);
}
}
})().call(null,coll,elem);
}
});

(quantum.core.collections.Contains_QMARK_["string"] = true);

(quantum.core.collections.contains_QMARK_["string"] = (function (coll,elem){
return cljs.core.not_EQ_.call(null,(-1),coll.indexOf(elem));
}));

quantum.core.ns.Regex.prototype.quantum$core$collections$Contains_QMARK_$ = true;

quantum.core.ns.Regex.prototype.quantum$core$collections$Contains_QMARK_$contains_QMARK_$arity$2 = (function (coll,elem){
var coll__$1 = this;
return quantum.core.logic.nnil_QMARK_.call(null,quantum.core.string.re_find_PLUS_.call(null,elem,coll__$1));
});

(quantum.core.collections.Contains_QMARK_["_"] = true);

(quantum.core.collections.contains_QMARK_["_"] = (function (coll,elem){
return quantum.core.logic.any_QMARK_.call(null,quantum.core.logic.fn_eq_QMARK_.call(null,elem),coll);
}));
quantum.core.collections.doseq.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.type,new cljs.core.Keyword(null,"map","map",1371690461).cljs$core$IFn$_invoke$arity$1(quantum.core.type.types)], null),quantum.core.collections.extend.call(null,cljs.core.type,quantum.core.collections.Contains_QMARK_,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"contains?","contains?",977623193),(function (coll,k){
return cljs.core.contains_QMARK_.call(null,coll,k);
})], null)));
/**
 * The inverse of |contains?|
 */
quantum.core.collections.in_QMARK_ = (function quantum$core$collections$in_QMARK_(elem,coll){
return quantum.core.collections.contains_QMARK_.call(null,coll,elem);
});
quantum.core.collections.subs_QMARK_ = quantum.core.collections.in_QMARK_;
/**
 * A transient and reducing version of clojure.core's |select-keys|.
 */
quantum.core.collections.select_keys_large = (function quantum$core$collections$select_keys_large(keyseq,m){
return cljs.core.with_meta.call(null,cljs.core.persistent_BANG_.call(null,quantum.core.collections.reduce.call(null,cljs.core.transient$.call(null,cljs.core.PersistentArrayMap.EMPTY),(function (ret,k){
var entry = clojure.lang.RT.find(m,k);
if(cljs.core.truth_(entry)){
return cljs.core.conj_BANG_.call(null,ret,entry);
} else {
return ret;
}
}),cljs.core.seq.call(null,keyseq))),cljs.core.meta.call(null,m));
});
/**
 * A transient version of clojure.core's |select-keys|.
 * 
 * Note: using a reducer here incurs the overhead of creating a
 * function on the fly (can't extern it because of a closure).
 * This is better for small set of keys.
 */
quantum.core.collections.select_keys_small = (function quantum$core$collections$select_keys_small(keyseq,m){
var ret = cljs.core.transient$.call(null,cljs.core.PersistentArrayMap.EMPTY);
var keys = cljs.core.seq.call(null,keyseq);
while(true){
if(keys){
var entry = clojure.lang.RT.find(m,quantum.core.collections.first.call(null,keys));
var G__30677 = (cljs.core.truth_(entry)?cljs.core.conj_BANG_.call(null,ret,entry):ret);
var G__30678 = cljs.core.next.call(null,keys);
ret = G__30677;
keys = G__30678;
continue;
} else {
return cljs.core.with_meta.call(null,cljs.core.persistent_BANG_.call(null,ret),cljs.core.meta.call(null,m));
}
break;
}
});
/**
 * Not as fast as select-keys with transients.
 */
quantum.core.collections.select_keys_delay = (function quantum$core$collections$select_keys_delay(ks,m){
var ks_set = quantum.core.collections.into.call(null,cljs.core.PersistentHashSet.EMPTY,ks);
return quantum.core.collections.filter_PLUS_.call(null,quantum.core.function$.compr.call(null,quantum.core.collections.key_PLUS_,quantum.core.function$.f_STAR_n.call(null,quantum.core.collections.in_QMARK_,ks_set)),m);
});
quantum.core.collections.select_keys_PLUS_ = (function quantum$core$collections$select_keys_PLUS_(m,ks){
if((quantum.core.collections.count.call(null,ks) > (10))){
return quantum.core.collections.select_keys_small.call(null,m,ks);
} else {
return quantum.core.collections.select_keys_large.call(null,m,ks);
}
});
quantum.core.collections.get_keys = (function quantum$core$collections$get_keys(m,obj){
return cljs.core.persistent_BANG_.call(null,quantum.core.collections.reduce.call(null,(function (ret,k,v){
if((obj === v)){
return cljs.core.conj_BANG_.call(null,ret,k);
} else {
return ret;
}
}),cljs.core.transient$.call(null,cljs.core.PersistentVector.EMPTY),m));
});
quantum.core.collections.get_key = (function quantum$core$collections$get_key(m,obj){
return quantum.core.collections.first.call(null,quantum.core.collections.get_keys.call(null,m,obj));
});
quantum.core.collections.filter_keys_PLUS_ = (function quantum$core$collections$filter_keys_PLUS_(pred,coll){
return quantum.core.collections.filter_PLUS_.call(null,quantum.core.function$.compr.call(null,quantum.core.collections.key_PLUS_,pred),coll);
});
quantum.core.collections.remove_keys_PLUS_ = (function quantum$core$collections$remove_keys_PLUS_(pred,coll){
return quantum.core.collections.remove_PLUS_.call(null,quantum.core.function$.compr.call(null,quantum.core.collections.key_PLUS_,pred),coll);
});
quantum.core.collections.filter_vals_PLUS_ = (function quantum$core$collections$filter_vals_PLUS_(pred,coll){
return quantum.core.collections.filter_PLUS_.call(null,quantum.core.function$.compr.call(null,quantum.core.collections.val_PLUS_,pred),coll);
});
quantum.core.collections.remove_vals_PLUS_ = (function quantum$core$collections$remove_vals_PLUS_(pred,coll){
return quantum.core.collections.remove_PLUS_.call(null,quantum.core.function$.compr.call(null,quantum.core.collections.key_PLUS_,pred),coll);
});
quantum.core.collections.vals_PLUS_ = (function quantum$core$collections$vals_PLUS_(m){
return quantum.core.collections.redv.call(null,quantum.core.collections.map_PLUS_.call(null,quantum.core.collections.val_PLUS_,m));
});
quantum.core.collections.keys_PLUS_ = (function quantum$core$collections$keys_PLUS_(m){
return quantum.core.collections.redv.call(null,quantum.core.collections.map_PLUS_.call(null,quantum.core.collections.key_PLUS_,m));
});
/**
 * Divide coll into n approximately equal slices.
 * Like partition.
 */
quantum.core.collections.slice = (function quantum$core$collections$slice(n_0,coll){
var n_n = n_0;
var slices = cljs.core.PersistentVector.EMPTY;
var items = quantum.core.collections.vec.call(null,coll);
while(true){
if(cljs.core.empty_QMARK_.call(null,items)){
return slices;
} else {
var size = quantum.core.numeric.ceil.call(null,(quantum.core.collections.count.call(null,items) / n_n));
var G__30679 = (n_n - (1));
var G__30680 = cljs.core.conj.call(null,slices,quantum.core.data.vector.subvec_PLUS_.call(null,items,(0),size));
var G__30681 = quantum.core.data.vector.subvec_PLUS_.call(null,items,size);
n_n = G__30679;
slices = G__30680;
items = G__30681;
continue;
}
break;
}
});
quantum.core.collections.split = (function quantum$core$collections$split(ind,coll_0){
if(cljs.core.vector_QMARK_.call(null,coll_0)){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [quantum.core.data.vector.subvec_PLUS_.call(null,coll_0,(0),ind),quantum.core.data.vector.subvec_PLUS_.call(null,coll_0,ind,quantum.core.collections.count.call(null,coll_0))], null);
} else {
return cljs.core.split_at.call(null,coll_0,ind);
}
});
quantum.core.collections.split_with_v_PLUS_ = (function quantum$core$collections$split_with_v_PLUS_(pred,coll_0){
return quantum.core.collections.map_PLUS_.call(null,quantum.core.collections.vec,cljs.core.split_with.call(null,pred,coll_0));
});
/**
 * Extends an associative structure (for now, only vector) to a given index.
 */
quantum.core.collections.extend_coll_to = (function quantum$core$collections$extend_coll_to(coll_0,k){
if((cljs.core.vector_QMARK_.call(null,coll_0)) && (typeof k === 'number') && (((quantum.core.collections.count.call(null,coll_0) - (1)) < k))){
var trans_QMARK_ = quantum.core.type.transient_QMARK_.call(null,coll_0);
var trans_fn = (cljs.core.truth_(trans_QMARK_)?cljs.core.identity:cljs.core.transient$);
var pers_fn = (cljs.core.truth_(trans_QMARK_)?cljs.core.identity:cljs.core.persistent_BANG_);
return pers_fn.call(null,quantum.core.collections.reduce.call(null,((function (trans_QMARK_,trans_fn,pers_fn){
return (function (coll_n,_){
return cljs.core.conj_BANG_.call(null,coll_n,null);
});})(trans_QMARK_,trans_fn,pers_fn))
,trans_fn.call(null,coll_0),quantum.core.collections.range.call(null,quantum.core.collections.count.call(null,coll_0),(k + (1)))));
} else {
return coll_0;
}
});
quantum.core.collections.assoc_PLUS_ = (function quantum$core$collections$assoc_PLUS_(){
var G__30687 = arguments.length;
switch (G__30687) {
case 3:
return quantum.core.collections.assoc_PLUS_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
var argseq__19124__auto__ = (new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(3)),(0)));
return quantum.core.collections.assoc_PLUS_.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),argseq__19124__auto__);

}
});

quantum.core.collections.assoc_PLUS_.cljs$core$IFn$_invoke$arity$3 = (function (coll_0,k,v){
return cljs.core.assoc.call(null,quantum.core.collections.extend_coll_to.call(null,coll_0,k),k,v);
});

quantum.core.collections.assoc_PLUS_.cljs$core$IFn$_invoke$arity$variadic = (function (coll_0,k,v,kvs_0){
var edit_QMARK_ = quantum.core.type.editable_QMARK_.call(null,coll_0);
var trans_fn = (cljs.core.truth_(edit_QMARK_)?cljs.core.transient$:cljs.core.identity);
var pers_fn = (cljs.core.truth_(edit_QMARK_)?cljs.core.persistent_BANG_:cljs.core.identity);
var assoc_fn = (cljs.core.truth_(edit_QMARK_)?cljs.core.assoc_BANG_:cljs.core.assoc);
var kvs_n = kvs_0;
var coll_f = assoc_fn.call(null,quantum.core.collections.extend_coll_to.call(null,trans_fn.call(null,coll_0),k),k,v);
while(true){
if(cljs.core.empty_QMARK_.call(null,kvs_n)){
return pers_fn.call(null,coll_f);
} else {
var G__30689 = quantum.core.collections.rest.call(null,quantum.core.collections.rest.call(null,kvs_n));
var G__30690 = (function (){var k_n = quantum.core.collections.first.call(null,kvs_n);
return assoc_fn.call(null,quantum.core.collections.extend_coll_to.call(null,coll_f,k_n),k_n,quantum.core.collections.second.call(null,kvs_n));
})();
kvs_n = G__30689;
coll_f = G__30690;
continue;
}
break;
}
});

quantum.core.collections.assoc_PLUS_.cljs$lang$applyTo = (function (seq30682){
var G__30683 = cljs.core.first.call(null,seq30682);
var seq30682__$1 = cljs.core.next.call(null,seq30682);
var G__30684 = cljs.core.first.call(null,seq30682__$1);
var seq30682__$2 = cljs.core.next.call(null,seq30682__$1);
var G__30685 = cljs.core.first.call(null,seq30682__$2);
var seq30682__$3 = cljs.core.next.call(null,seq30682__$2);
return quantum.core.collections.assoc_PLUS_.cljs$core$IFn$_invoke$arity$variadic(G__30683,G__30684,G__30685,seq30682__$3);
});

quantum.core.collections.assoc_PLUS_.cljs$lang$maxFixedArity = (3);
/**
 * Updates the value in an associative data structure @coll associated with key @k
 * by applying the function @f to the existing value.
 */
quantum.core.collections.update_PLUS_ = (function quantum$core$collections$update_PLUS_(){
var G__30692 = arguments.length;
switch (G__30692) {
case 3:
return quantum.core.collections.update_PLUS_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return quantum.core.collections.update_PLUS_.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.collections.update_PLUS_.cljs$core$IFn$_invoke$arity$3 = (function (coll,k,f){
return quantum.core.collections.assoc_PLUS_.call(null,coll,k,f.call(null,quantum.core.collections.get.call(null,coll,k)));
});

quantum.core.collections.update_PLUS_.cljs$core$IFn$_invoke$arity$4 = (function (coll,k,f,args){
return quantum.core.collections.assoc_PLUS_.call(null,coll,k,cljs.core.apply.call(null,f,quantum.core.collections.get.call(null,coll,k),args));
});

quantum.core.collections.update_PLUS_.cljs$lang$maxFixedArity = 4;
/**
 * For each key-function pair in @kfs,
 * updates value in an associative data structure @coll associated with key
 * by applying the function @f to the existing value.
 */
quantum.core.collections.updates_PLUS_ = (function quantum$core$collections$updates_PLUS_(){
var argseq__19113__auto__ = ((((1) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(1)),(0))):null);
return quantum.core.collections.updates_PLUS_.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__19113__auto__);
});

quantum.core.collections.updates_PLUS_.cljs$core$IFn$_invoke$arity$variadic = (function (coll,kfs){
return quantum.core.collections.reduce_2.call(null,(function (ret,k,f){
return quantum.core.collections.update_PLUS_.call(null,ret,k,f);
}),coll,kfs);
});

quantum.core.collections.updates_PLUS_.cljs$lang$maxFixedArity = (1);

quantum.core.collections.updates_PLUS_.cljs$lang$applyTo = (function (seq30694){
var G__30695 = cljs.core.first.call(null,seq30694);
var seq30694__$1 = cljs.core.next.call(null,seq30694);
return quantum.core.collections.updates_PLUS_.cljs$core$IFn$_invoke$arity$variadic(G__30695,seq30694__$1);
});
quantum.core.collections.update_key_PLUS_ = (function quantum$core$collections$update_key_PLUS_(f){
return (function() {
var G__30696 = null;
var G__30696__1 = (function (kv){
return quantum.core.collections.assoc_PLUS_.call(null,kv,(0),f.call(null,quantum.core.collections.get.call(null,kv,(0))));
});
var G__30696__2 = (function (k,v){
return quantum.core.data.map.map_entry.call(null,f.call(null,k),v);
});
G__30696 = function(k,v){
switch(arguments.length){
case 1:
return G__30696__1.call(this,k);
case 2:
return G__30696__2.call(this,k,v);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
G__30696.cljs$core$IFn$_invoke$arity$1 = G__30696__1;
G__30696.cljs$core$IFn$_invoke$arity$2 = G__30696__2;
return G__30696;
})()
});
quantum.core.collections.update_val_PLUS_ = (function quantum$core$collections$update_val_PLUS_(f){
return (function() {
var G__30697 = null;
var G__30697__1 = (function (kv){
return quantum.core.collections.assoc_PLUS_.call(null,kv,(1),f.call(null,quantum.core.collections.get.call(null,kv,(1))));
});
var G__30697__2 = (function (k,v){
return quantum.core.data.map.map_entry.call(null,k,f.call(null,v));
});
G__30697 = function(k,v){
switch(arguments.length){
case 1:
return G__30697__1.call(this,k);
case 2:
return G__30697__2.call(this,k,v);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
G__30697.cljs$core$IFn$_invoke$arity$1 = G__30697__1;
G__30697.cljs$core$IFn$_invoke$arity$2 = G__30697__2;
return G__30697;
})()
});
quantum.core.collections.mapmux = (function quantum$core$collections$mapmux(){
var G__30699 = arguments.length;
switch (G__30699) {
case 1:
return quantum.core.collections.mapmux.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return quantum.core.collections.mapmux.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.collections.mapmux.cljs$core$IFn$_invoke$arity$1 = (function (kv){
return kv;
});

quantum.core.collections.mapmux.cljs$core$IFn$_invoke$arity$2 = (function (k,v){
return quantum.core.data.map.map_entry.call(null,k,v);
});

quantum.core.collections.mapmux.cljs$lang$maxFixedArity = 2;
quantum.core.collections.record__GT_map = (function quantum$core$collections$record__GT_map(rec){
return quantum.core.collections.into.call(null,cljs.core.PersistentArrayMap.EMPTY,rec);
});
/**
 * 'Updates' a value in a nested associative structure, where ks is a sequence of keys and
 * f is a function that will take the old value and any supplied args and return the new
 * value, and returns a new nested structure. The associative structure can have transients
 * in it, but if any levels do not exist, non-transient hash-maps will be created.
 */
quantum.core.collections.update_in_BANG_ = (function quantum$core$collections$update_in_BANG_(){
var argseq__19113__auto__ = ((((3) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(3)),(0))):null);
return quantum.core.collections.update_in_BANG_.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),argseq__19113__auto__);
});

quantum.core.collections.update_in_BANG_.cljs$core$IFn$_invoke$arity$variadic = (function (m,p__30705,f,args){
var vec__30706 = p__30705;
var k = cljs.core.nth.call(null,vec__30706,(0),null);
var ks = cljs.core.nthnext.call(null,vec__30706,(1));
var assoc_fn = (cljs.core.truth_(quantum.core.type.transient_QMARK_.call(null,m))?cljs.core.assoc_BANG_:cljs.core.assoc);
var val = quantum.core.collections.get.call(null,m,k);
return assoc_fn.call(null,m,k,(cljs.core.truth_(ks)?cljs.core.apply.call(null,quantum.core.collections.update_in_BANG_,val,ks,f,args):cljs.core.apply.call(null,f,val,args)));
});

quantum.core.collections.update_in_BANG_.cljs$lang$maxFixedArity = (3);

quantum.core.collections.update_in_BANG_.cljs$lang$applyTo = (function (seq30701){
var G__30702 = cljs.core.first.call(null,seq30701);
var seq30701__$1 = cljs.core.next.call(null,seq30701);
var G__30703 = cljs.core.first.call(null,seq30701__$1);
var seq30701__$2 = cljs.core.next.call(null,seq30701__$1);
var G__30704 = cljs.core.first.call(null,seq30701__$2);
var seq30701__$3 = cljs.core.next.call(null,seq30701__$2);
return quantum.core.collections.update_in_BANG_.cljs$core$IFn$_invoke$arity$variadic(G__30702,G__30703,G__30704,seq30701__$3);
});
/**
 * Created so vectors would also automatically be grown like maps,
 * given indices not present in the vector.
 */
quantum.core.collections.update_in_PLUS_ = (function quantum$core$collections$update_in_PLUS_(coll_0,p__30707,v0){
var vec__30709 = p__30707;
var k0 = cljs.core.nth.call(null,vec__30709,(0),null);
var keys_0 = cljs.core.nthnext.call(null,vec__30709,(1));
var value = quantum.core.collections.get.call(null,coll_0,k0,((typeof quantum.core.collections.first.call(null,keys_0) === 'number')?cljs.core.PersistentVector.EMPTY:null));
var coll_f = quantum.core.collections.extend_coll_to.call(null,coll_0,k0);
var val_f = (cljs.core.truth_(keys_0)?quantum$core$collections$update_in_PLUS_.call(null,value,keys_0,v0):v0);
return cljs.core.assoc.call(null,coll_f,k0,(function (){var obj_f__22903__auto__ = val_f;
if(cljs.core.fn_QMARK_.call(null,obj_f__22903__auto__)){
return quantum.core.function$._STAR_fn.call(null,quantum.core.collections.get.call(null,coll_f,k0)).call(null,obj_f__22903__auto__);
} else {
return obj_f__22903__auto__;
}
})());
});
quantum.core.collections.assoc_in_PLUS_ = (function quantum$core$collections$assoc_in_PLUS_(coll,ks,v){
return quantum.core.collections.update_in_PLUS_.call(null,coll,ks,cljs.core.constantly.call(null,v));
});
/**
 * Associates a value in a nested associative structure, where ks is a sequence of keys
 * and v is the new value and returns a new nested structure. The associative structure
 * can have transients in it, but if any levels do not exist, non-transient hash-maps will
 * be created.
 */
quantum.core.collections.assoc_in_BANG_ = (function quantum$core$collections$assoc_in_BANG_(m,ks,v){
return quantum.core.collections.update_in_BANG_.call(null,m,ks,cljs.core.constantly.call(null,v));
});
quantum.core.collections.assocs_in_PLUS_ = (function quantum$core$collections$assocs_in_PLUS_(){
var argseq__19113__auto__ = ((((1) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(1)),(0))):null);
return quantum.core.collections.assocs_in_PLUS_.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__19113__auto__);
});

quantum.core.collections.assocs_in_PLUS_.cljs$core$IFn$_invoke$arity$variadic = (function (coll,kvs){
return quantum.core.collections.reduce_2.call(null,(function (ret,k,v){
return quantum.core.collections.assoc_in_PLUS_.call(null,ret,k,v);
}),coll,kvs);
});

quantum.core.collections.assocs_in_PLUS_.cljs$lang$maxFixedArity = (1);

quantum.core.collections.assocs_in_PLUS_.cljs$lang$applyTo = (function (seq30710){
var G__30711 = cljs.core.first.call(null,seq30710);
var seq30710__$1 = cljs.core.next.call(null,seq30710);
return quantum.core.collections.assocs_in_PLUS_.cljs$core$IFn$_invoke$arity$variadic(G__30711,seq30710__$1);
});
quantum.core.collections.dissoc_PLUS_ = (function quantum$core$collections$dissoc_PLUS_(){
var G__30716 = arguments.length;
switch (G__30716) {
case 2:
return quantum.core.collections.dissoc_PLUS_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
var argseq__19124__auto__ = (new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(2)),(0)));
return quantum.core.collections.dissoc_PLUS_.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),argseq__19124__auto__);

}
});

quantum.core.collections.dissoc_PLUS_.cljs$core$IFn$_invoke$arity$2 = (function (coll,key_0){
try{if(cljs.core.vector_QMARK_.call(null,coll)){
return quantum.core.data.vector.catvec.call(null,quantum.core.data.vector.subvec_PLUS_.call(null,coll,(0),key_0),quantum.core.data.vector.subvec_PLUS_.call(null,coll,(key_0 + (1)),quantum.core.collections.count.call(null,coll)));
} else {
if(cljs.core.truth_(quantum.core.type.editable_QMARK_.call(null,coll))){
return cljs.core.persistent_BANG_.call(null,cljs.core.dissoc_BANG_.call(null,cljs.core.transient$.call(null,coll),coll,key_0));
} else {
return cljs.core.dissoc.call(null,coll,key_0);

}
}
}catch (e30717){if((e30717 instanceof quantum.core.collections.ClassCastException)){
var e = e30717;
return cljs.core.dissoc.call(null,coll,key_0);
} else {
throw e30717;

}
}});

quantum.core.collections.dissoc_PLUS_.cljs$core$IFn$_invoke$arity$variadic = (function (coll,key_0,keys_0){
return quantum.core.collections.reduce.call(null,quantum.core.collections.dissoc_PLUS_,coll,cljs.core.cons.call(null,key_0,keys_0));
});

quantum.core.collections.dissoc_PLUS_.cljs$lang$applyTo = (function (seq30712){
var G__30713 = cljs.core.first.call(null,seq30712);
var seq30712__$1 = cljs.core.next.call(null,seq30712);
var G__30714 = cljs.core.first.call(null,seq30712__$1);
var seq30712__$2 = cljs.core.next.call(null,seq30712__$1);
return quantum.core.collections.dissoc_PLUS_.cljs$core$IFn$_invoke$arity$variadic(G__30713,G__30714,seq30712__$2);
});

quantum.core.collections.dissoc_PLUS_.cljs$lang$maxFixedArity = (2);
quantum.core.collections.dissocs_PLUS_ = (function quantum$core$collections$dissocs_PLUS_(){
var argseq__19113__auto__ = ((((1) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(1)),(0))):null);
return quantum.core.collections.dissocs_PLUS_.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__19113__auto__);
});

quantum.core.collections.dissocs_PLUS_.cljs$core$IFn$_invoke$arity$variadic = (function (coll,ks){
return quantum.core.collections.reduce.call(null,(function (ret,k){
return quantum.core.collections.dissoc_PLUS_.call(null,ret,k);
}),coll,ks);
});

quantum.core.collections.dissocs_PLUS_.cljs$lang$maxFixedArity = (1);

quantum.core.collections.dissocs_PLUS_.cljs$lang$applyTo = (function (seq30719){
var G__30720 = cljs.core.first.call(null,seq30719);
var seq30719__$1 = cljs.core.next.call(null,seq30719);
return quantum.core.collections.dissocs_PLUS_.cljs$core$IFn$_invoke$arity$variadic(G__30720,seq30719__$1);
});
quantum.core.collections.dissoc_if_PLUS_ = (function quantum$core$collections$dissoc_if_PLUS_(coll,pred,k){
var obj_f__22903__auto__ = coll;
if(cljs.core.truth_(((function (obj_f__22903__auto__){
return (function (x__22733__auto__){
return pred.call(null,quantum.core.collections.get.call(null,x__22733__auto__,k));
});})(obj_f__22903__auto__))
.call(null,obj_f__22903__auto__))){
return quantum.core.function$.f_STAR_n.call(null,quantum.core.collections.dissoc_PLUS_,k).call(null,obj_f__22903__auto__);
} else {
return obj_f__22903__auto__;
}
});
/**
 * Dissociate a value in a nested assocative structure, identified by a sequence
 * of keys. Any collections left empty by the operation will be dissociated from
 * their containing structures.
 * This implementation was adapted from clojure.core.contrib
 */
quantum.core.collections.dissoc_in_PLUS_ = (function quantum$core$collections$dissoc_in_PLUS_(m,ks){
var temp__4421__auto__ = cljs.core.seq.call(null,ks);
if(temp__4421__auto__){
var vec__30722 = temp__4421__auto__;
var k = cljs.core.nth.call(null,vec__30722,(0),null);
var ks__$1 = cljs.core.nthnext.call(null,vec__30722,(1));
if(cljs.core.seq.call(null,ks__$1)){
var new_n = quantum$core$collections$dissoc_in_PLUS_.call(null,quantum.core.collections.get.call(null,m,k),ks__$1);
if(cljs.core.empty_QMARK_.call(null,new_n)){
return cljs.core.dissoc.call(null,m,k);
} else {
return cljs.core.assoc.call(null,m,k,new_n);
}
} else {
return cljs.core.dissoc.call(null,m,k);
}
} else {
return m;
}
});
quantum.core.collections.updates_in_PLUS_ = (function quantum$core$collections$updates_in_PLUS_(){
var argseq__19113__auto__ = ((((1) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(1)),(0))):null);
return quantum.core.collections.updates_in_PLUS_.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__19113__auto__);
});

quantum.core.collections.updates_in_PLUS_.cljs$core$IFn$_invoke$arity$variadic = (function (coll,kfs){
return quantum.core.collections.reduce_2.call(null,(function (ret,k_n,f_n){
return quantum.core.collections.update_in_PLUS_.call(null,ret,k_n,f_n);
}),coll,kfs);
});

quantum.core.collections.updates_in_PLUS_.cljs$lang$maxFixedArity = (1);

quantum.core.collections.updates_in_PLUS_.cljs$lang$applyTo = (function (seq30723){
var G__30724 = cljs.core.first.call(null,seq30723);
var seq30723__$1 = cljs.core.next.call(null,seq30723);
return quantum.core.collections.updates_in_PLUS_.cljs$core$IFn$_invoke$arity$variadic(G__30724,seq30723__$1);
});
quantum.core.collections.re_assoc_PLUS_ = (function quantum$core$collections$re_assoc_PLUS_(coll,k_0,k_f){
if(cljs.core.truth_(quantum.core.collections.contains_QMARK_.call(null,coll,k_0))){
return quantum.core.collections.dissoc_PLUS_.call(null,quantum.core.collections.assoc_PLUS_.call(null,coll,k_f,quantum.core.collections.get.call(null,coll,k_0)),k_0);
} else {
return coll;
}
});
quantum.core.collections.re_assocs_PLUS_ = (function quantum$core$collections$re_assocs_PLUS_(){
var argseq__19113__auto__ = ((((1) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(1)),(0))):null);
return quantum.core.collections.re_assocs_PLUS_.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__19113__auto__);
});

quantum.core.collections.re_assocs_PLUS_.cljs$core$IFn$_invoke$arity$variadic = (function (coll,kfs){
return quantum.core.collections.reduce_2.call(null,(function (ret,k_n,f_n){
return quantum.core.collections.re_assoc_PLUS_.call(null,ret,k_n,f_n);
}),coll,kfs);
});

quantum.core.collections.re_assocs_PLUS_.cljs$lang$maxFixedArity = (1);

quantum.core.collections.re_assocs_PLUS_.cljs$lang$applyTo = (function (seq30725){
var G__30726 = cljs.core.first.call(null,seq30725);
var seq30725__$1 = cljs.core.next.call(null,seq30725);
return quantum.core.collections.re_assocs_PLUS_.cljs$core$IFn$_invoke$arity$variadic(G__30726,seq30725__$1);
});
quantum.core.collections.select_as_PLUS_ = (function quantum$core$collections$select_as_PLUS_(){
var G__30732 = arguments.length;
switch (G__30732) {
case 2:
return quantum.core.collections.select_as_PLUS_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
var argseq__19124__auto__ = (new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(3)),(0)));
return quantum.core.collections.select_as_PLUS_.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),argseq__19124__auto__);

}
});

quantum.core.collections.select_as_PLUS_.cljs$core$IFn$_invoke$arity$2 = (function (coll,kfs){
return quantum.core.collections.reduce.call(null,(function (ret,k,f){
return quantum.core.collections.assoc_PLUS_.call(null,ret,k,f.call(null,coll));
}),cljs.core.PersistentArrayMap.EMPTY,kfs);
});

quantum.core.collections.select_as_PLUS_.cljs$core$IFn$_invoke$arity$variadic = (function (coll,k1,f1,p__30733){
var map__30734 = p__30733;
var map__30734__$1 = ((cljs.core.seq_QMARK_.call(null,map__30734))?cljs.core.apply.call(null,cljs.core.hash_map,map__30734):map__30734);
var kfs = map__30734__$1;
return quantum.core.collections.select_as_PLUS_.call(null,coll,quantum.core.collections.assoc_PLUS_.call(null,kfs,k1,f1));
});

quantum.core.collections.select_as_PLUS_.cljs$lang$applyTo = (function (seq30727){
var G__30728 = cljs.core.first.call(null,seq30727);
var seq30727__$1 = cljs.core.next.call(null,seq30727);
var G__30729 = cljs.core.first.call(null,seq30727__$1);
var seq30727__$2 = cljs.core.next.call(null,seq30727__$1);
var G__30730 = cljs.core.first.call(null,seq30727__$2);
var seq30727__$3 = cljs.core.next.call(null,seq30727__$2);
return quantum.core.collections.select_as_PLUS_.cljs$core$IFn$_invoke$arity$variadic(G__30728,G__30729,G__30730,seq30727__$3);
});

quantum.core.collections.select_as_PLUS_.cljs$lang$maxFixedArity = (3);

quantum.core.collections.Interpose = (function (){var obj30737 = {};
return obj30737;
})();

quantum.core.collections.interpose_PLUS__ = (function quantum$core$collections$interpose_PLUS__(coll,elem){
if((function (){var and__18061__auto__ = coll;
if(and__18061__auto__){
return coll.quantum$core$collections$Interpose$interpose_PLUS__$arity$2;
} else {
return and__18061__auto__;
}
})()){
return coll.quantum$core$collections$Interpose$interpose_PLUS__$arity$2(coll,elem);
} else {
var x__18709__auto__ = (((coll == null))?null:coll);
return (function (){var or__18073__auto__ = (quantum.core.collections.interpose_PLUS__[goog.typeOf(x__18709__auto__)]);
if(or__18073__auto__){
return or__18073__auto__;
} else {
var or__18073__auto____$1 = (quantum.core.collections.interpose_PLUS__["_"]);
if(or__18073__auto____$1){
return or__18073__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"Interpose.interpose+-",coll);
}
}
})().call(null,coll,elem);
}
});

(quantum.core.collections.Interpose["string"] = true);

(quantum.core.collections.interpose_PLUS__["string"] = (function (coll,elem){
return quantum.core.string.join.call(null,elem,coll);
}));

(quantum.core.collections.Interpose["_"] = true);

(quantum.core.collections.interpose_PLUS__["_"] = (function (coll,elem){
return cljs.core.interpose.call(null,elem,coll);
}));
quantum.core.collections.interpose_PLUS_ = (function quantum$core$collections$interpose_PLUS_(elem,coll){
return quantum.core.collections.interpose_PLUS__.call(null,coll,elem);
});
/**
 * Analogy: partition:partition-all :: interleave:interleave-all
 */
quantum.core.collections.linterleave_all = (function quantum$core$collections$linterleave_all(){
var argseq__19113__auto__ = ((((0) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(0)),(0))):null);
return quantum.core.collections.linterleave_all.cljs$core$IFn$_invoke$arity$variadic(argseq__19113__auto__);
});

quantum.core.collections.linterleave_all.cljs$core$IFn$_invoke$arity$variadic = (function (colls){
return (new cljs.core.LazySeq(null,(function (){
return (function quantum$core$collections$helper(seqs){
if(cljs.core.seq.call(null,seqs)){
return cljs.core.concat.call(null,cljs.core.map.call(null,quantum.core.collections.first,seqs),(new cljs.core.LazySeq(null,(function (){
return quantum$core$collections$helper.call(null,cljs.core.keep.call(null,cljs.core.next,seqs));
}),null,null)));
} else {
return null;
}
}).call(null,cljs.core.keep.call(null,cljs.core.seq,colls));
}),null,null));
});

quantum.core.collections.linterleave_all.cljs$lang$maxFixedArity = (0);

quantum.core.collections.linterleave_all.cljs$lang$applyTo = (function (seq30738){
return quantum.core.collections.linterleave_all.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq30738));
});
quantum.core.collections.group_merge_with_PLUS_ = (function quantum$core$collections$group_merge_with_PLUS_(group_by_f,merge_with_f,coll){
var merge_like_elems = (function (grouped_elems){
if(cljs.core.truth_(quantum.core.collections.single_QMARK_.call(null,grouped_elems))){
return grouped_elems;
} else {
return quantum.core.collections.reduce.call(null,(function (ret,elem){
return quantum.core.collections.merge_with_PLUS_.call(null,merge_with_f,ret,elem);
}),quantum.core.collections.first.call(null,grouped_elems),quantum.core.collections.rest.call(null,grouped_elems));
}
});
return quantum.core.collections.flatten_PLUS_.call(null,quantum.core.collections.map_PLUS_.call(null,merge_like_elems,quantum.core.collections.map_PLUS_.call(null,quantum.core.collections.val_PLUS_,quantum.core.collections.group_by_PLUS_.call(null,group_by_f,coll))));
});
quantum.core.collections.merge_left = (function quantum$core$collections$merge_left(){
var G__30740 = arguments.length;
switch (G__30740) {
case 1:
return quantum.core.collections.merge_left.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 3:
return quantum.core.collections.merge_left.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.collections.merge_left.cljs$core$IFn$_invoke$arity$1 = (function (alert_level){
return (function (k,v1,v2){
if(cljs.core.not_EQ_.call(null,v1,v2)){
quantum.core.log.pr.call(null,alert_level,"Values do not match for merge key",[cljs.core.str(quantum.core.string.squote.call(null,k)),cljs.core.str(":")].join(''),quantum.core.string.squote.call(null,v1),"|",quantum.core.string.squote.call(null,v2));
} else {
}

return v1;
});
});

quantum.core.collections.merge_left.cljs$core$IFn$_invoke$arity$3 = (function (k,v1,v2){
return v1;
});

quantum.core.collections.merge_left.cljs$lang$maxFixedArity = 3;
quantum.core.collections.merge_right = (function quantum$core$collections$merge_right(){
var G__30743 = arguments.length;
switch (G__30743) {
case 1:
return quantum.core.collections.merge_right.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 3:
return quantum.core.collections.merge_right.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.collections.merge_right.cljs$core$IFn$_invoke$arity$1 = (function (alert_level){
return (function (k,v1,v2){
if(cljs.core.not_EQ_.call(null,v1,v2)){
quantum.core.log.pr.call(null,alert_level,"Values do not match for merge key",[cljs.core.str(quantum.core.string.squote.call(null,k)),cljs.core.str(":")].join(''),quantum.core.string.squote.call(null,v1),"|",quantum.core.string.squote.call(null,v2));
} else {
}

return v1;
});
});

quantum.core.collections.merge_right.cljs$core$IFn$_invoke$arity$3 = (function (k,v1,v2){
return v2;
});

quantum.core.collections.merge_right.cljs$lang$maxFixedArity = 3;
quantum.core.collections.first_uniques_by_PLUS_ = (function quantum$core$collections$first_uniques_by_PLUS_(k,coll){
return quantum.core.collections.map_PLUS_.call(null,quantum.core.collections.update_val_PLUS_.call(null,quantum.core.collections.first),quantum.core.collections.group_by_PLUS_.call(null,k,coll));
});

quantum.core.collections.Walkable = (function (){var obj30746 = {};
return obj30746;
})();

/**
 * If coll is a collection, applies f to each element of the collection
 * and returns a collection of the results, of the same type and order
 * as coll. If coll is not a collection, returns it unchanged. "Same
 * type" means a type with the same behavior. For example, a hash-map
 * may be returned as an array-map, but a a sorted-map will be returned
 * as a sorted-map with the same comparator.
 */
quantum.core.collections.walk2 = (function quantum$core$collections$walk2(coll,f){
if((function (){var and__18061__auto__ = coll;
if(and__18061__auto__){
return coll.quantum$core$collections$Walkable$walk2$arity$2;
} else {
return and__18061__auto__;
}
})()){
return coll.quantum$core$collections$Walkable$walk2$arity$2(coll,f);
} else {
var x__18709__auto__ = (((coll == null))?null:coll);
return (function (){var or__18073__auto__ = (quantum.core.collections.walk2[goog.typeOf(x__18709__auto__)]);
if(or__18073__auto__){
return or__18073__auto__;
} else {
var or__18073__auto____$1 = (quantum.core.collections.walk2["_"]);
if(or__18073__auto____$1){
return or__18073__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"Walkable.walk2",coll);
}
}
})().call(null,coll,f);
}
});

quantum.core.collections.walk2_transient = (function quantum$core$collections$walk2_transient(coll,f){
return cljs.core.persistent_BANG_.call(null,quantum.core.collections.reduce.call(null,(function (r,x){
return cljs.core.conj_BANG_.call(null,r,f.call(null,x));
}),cljs.core.transient$.call(null,cljs.core.empty.call(null,coll)),coll));
});
quantum.core.collections.walk2_default = (function quantum$core$collections$walk2_default(coll,f){
return quantum.core.collections.reduce.call(null,(function (r,x){
return cljs.core.conj.call(null,r,f.call(null,x));
}),cljs.core.empty.call(null,coll),coll);
});
/**
 * Traverses form, an arbitrary data structure.  inner and outer are
 * functions.  Applies inner to each element of form, building up a
 * data structure of the same type, then applies outer to the result.
 * Recognizes all Clojure data structures. Consumes seqs as with doall.
 */
quantum.core.collections.walk = (function quantum$core$collections$walk(inner,outer,form){
return outer.call(null,quantum.core.collections.walk2.call(null,form,inner));
});
/**
 * Performs a depth-first, post-order traversal of form.  Calls f on
 * each sub-form, uses f's return value in place of the original.
 * Recognizes all Clojure data structures. Consumes seqs as with doall.
 */
quantum.core.collections.postwalk = (function quantum$core$collections$postwalk(f,form){
return quantum.core.collections.walk.call(null,cljs.core.partial.call(null,quantum$core$collections$postwalk,f),f,form);
});
/**
 * Like postwalk, but does pre-order traversal.
 */
quantum.core.collections.prewalk = (function quantum$core$collections$prewalk(f,form){
return quantum.core.collections.walk.call(null,cljs.core.partial.call(null,quantum$core$collections$prewalk,f),cljs.core.identity,f.call(null,form));
});
/**
 * Recursively transforms all map keys from keywords to strings.
 */
quantum.core.collections.keywordify_keys = (function quantum$core$collections$keywordify_keys(m){
var stringify_key = (function (p__30749){
var vec__30750 = p__30749;
var k = cljs.core.nth.call(null,vec__30750,(0),null);
var v = cljs.core.nth.call(null,vec__30750,(1),null);
if(typeof k === 'string'){
return quantum.core.data.map.map_entry.call(null,(k instanceof cljs.core.Keyword),v);
} else {
return quantum.core.data.map.map_entry.call(null,k,v);
}
});
return quantum.core.collections.postwalk.call(null,((function (stringify_key){
return (function (arg__22915__auto__){
var obj_f__22903__auto__ = arg__22915__auto__;
if(cljs.core.map_QMARK_.call(null,obj_f__22903__auto__)){
return ((function (obj_f__22903__auto__,stringify_key){
return (function (x__22739__auto__){
return quantum.core.collections.redm.call(null,quantum.core.collections.map_PLUS_.call(null,stringify_key,x__22739__auto__));
});})(obj_f__22903__auto__,stringify_key))
.call(null,obj_f__22903__auto__);
} else {
return obj_f__22903__auto__;
}
});})(stringify_key))
,m);
});
/**
 * Recursively transforms all map keys from keywords to strings.
 */
quantum.core.collections.stringify_keys = (function quantum$core$collections$stringify_keys(m){
var stringify_key = (function (p__30753){
var vec__30754 = p__30753;
var k = cljs.core.nth.call(null,vec__30754,(0),null);
var v = cljs.core.nth.call(null,vec__30754,(1),null);
if((k instanceof cljs.core.Keyword)){
return quantum.core.data.map.map_entry.call(null,cljs.core.name.call(null,k),v);
} else {
return quantum.core.data.map.map_entry.call(null,k,v);
}
});
return quantum.core.collections.postwalk.call(null,((function (stringify_key){
return (function (arg__22915__auto__){
var obj_f__22903__auto__ = arg__22915__auto__;
if(cljs.core.map_QMARK_.call(null,obj_f__22903__auto__)){
return ((function (obj_f__22903__auto__,stringify_key){
return (function (x__22739__auto__){
return quantum.core.collections.redm.call(null,quantum.core.collections.map_PLUS_.call(null,stringify_key,x__22739__auto__));
});})(obj_f__22903__auto__,stringify_key))
.call(null,obj_f__22903__auto__);
} else {
return obj_f__22903__auto__;
}
});})(stringify_key))
,m);
});
/**
 * Recursively transforms form by replacing keys in smap with their
 * values.  Like clojure/replace but works on any data structure.  Does
 * replacement at the root of the tree first.
 */
quantum.core.collections.prewalk_replace = (function quantum$core$collections$prewalk_replace(smap,form){
return quantum.core.collections.prewalk.call(null,(function (arg__22915__auto__){
var obj_f__22903__auto__ = arg__22915__auto__;
if(cljs.core.truth_(quantum.core.function$.f_STAR_n.call(null,quantum.core.collections.in_QMARK_,smap).call(null,obj_f__22903__auto__))){
return smap.call(null,obj_f__22903__auto__);
} else {
return obj_f__22903__auto__;
}
}),form);
});
/**
 * Recursively transforms form by replacing keys in smap with their
 * values.  Like clojure/replace but works on any data structure.  Does
 * replacement at the leaves of the tree first.
 */
quantum.core.collections.postwalk_replace = (function quantum$core$collections$postwalk_replace(smap,form){
return quantum.core.collections.postwalk.call(null,(function (arg__22915__auto__){
var obj_f__22903__auto__ = arg__22915__auto__;
if(cljs.core.truth_(quantum.core.function$.f_STAR_n.call(null,quantum.core.collections.in_QMARK_,smap).call(null,obj_f__22903__auto__))){
return smap.call(null,obj_f__22903__auto__);
} else {
return obj_f__22903__auto__;
}
}),form);
});
/**
 * Like |filter|, but performs a |postwalk| on a treelike structure @tree, putting in a new vector
 * only the elements for which @pred is true.
 */
quantum.core.collections.tree_filter = (function quantum$core$collections$tree_filter(pred,tree){
var results = cljs.core.transient$.call(null,cljs.core.PersistentVector.EMPTY);
quantum.core.collections.postwalk.call(null,((function (results){
return (function (arg__22915__auto__){
var obj_f__22903__auto__ = arg__22915__auto__;
if(cljs.core.truth_(pred.call(null,obj_f__22903__auto__))){
return ((function (obj_f__22903__auto__,results){
return (function (x__22739__auto__){
cljs.core.conj_BANG_.call(null,cljs.core.butlast.call(null,results.call(null,x__22739__auto__)),cljs.core.last.call(null,results.call(null,x__22739__auto__)));

return cljs.core.last.call(null,results.call(null,x__22739__auto__));
});})(obj_f__22903__auto__,results))
.call(null,obj_f__22903__auto__);
} else {
return obj_f__22903__auto__;
}
});})(results))
,tree);

return cljs.core.persistent_BANG_.call(null,results);
});
/**
 * Lazy, tail-recursive, incremental quicksort. Works against
 * and creates partitions based on the pivot, defined as 'work'.
 */
quantum.core.collections.sort_parts = (function quantum$core$collections$sort_parts(work){
return (new cljs.core.LazySeq(null,(function (){
var G__30763 = work;
var vec__30764 = G__30763;
var part = cljs.core.nth.call(null,vec__30764,(0),null);
var parts = cljs.core.nthnext.call(null,vec__30764,(1));
var G__30763__$1 = G__30763;
while(true){
var vec__30765 = G__30763__$1;
var part__$1 = cljs.core.nth.call(null,vec__30765,(0),null);
var parts__$1 = cljs.core.nthnext.call(null,vec__30765,(1));
var temp__4421__auto__ = cljs.core.seq.call(null,part__$1);
if(temp__4421__auto__){
var vec__30766 = temp__4421__auto__;
var pivot = cljs.core.nth.call(null,vec__30766,(0),null);
var xs = cljs.core.nthnext.call(null,vec__30766,(1));
var smaller_QMARK_ = ((function (G__30763__$1,vec__30766,pivot,xs,temp__4421__auto__,vec__30765,part__$1,parts__$1,G__30763,vec__30764,part,parts){
return (function (p1__30755_SHARP_){
return (p1__30755_SHARP_ < pivot);
});})(G__30763__$1,vec__30766,pivot,xs,temp__4421__auto__,vec__30765,part__$1,parts__$1,G__30763,vec__30764,part,parts))
;
var G__30768 = cljs.core.list_STAR_.call(null,cljs.core.filter.call(null,smaller_QMARK_,xs),pivot,cljs.core.remove.call(null,smaller_QMARK_,xs),parts__$1);
G__30763__$1 = G__30768;
continue;
} else {
var temp__4423__auto__ = parts__$1;
if(cljs.core.truth_(temp__4423__auto__)){
var vec__30767 = temp__4423__auto__;
var x = cljs.core.nth.call(null,vec__30767,(0),null);
var parts__$2 = cljs.core.nthnext.call(null,vec__30767,(1));
return cljs.core.cons.call(null,x,quantum$core$collections$sort_parts.call(null,parts__$2));
} else {
return null;
}
}
break;
}
}),null,null));
});
/**
 * Lazy 'quick'-sorting
 */
quantum.core.collections.lsort = (function quantum$core$collections$lsort(elems){
return quantum.core.collections.sort_parts.call(null,cljs.core._conj.call(null,cljs.core.List.EMPTY,elems));
});

//# sourceMappingURL=collections.js.map?rel=1431625680413