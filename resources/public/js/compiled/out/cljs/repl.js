// Compiled by ClojureScript 0.0-3269 {}
goog.provide('cljs.repl');
goog.require('cljs.core');
cljs.repl.print_doc = (function cljs$repl$print_doc(m){
cljs.core.println.call(null,"-------------------------");

cljs.core.println.call(null,[cljs.core.str((function (){var temp__4423__auto__ = new cljs.core.Keyword(null,"ns","ns",441598760).cljs$core$IFn$_invoke$arity$1(m);
if(cljs.core.truth_(temp__4423__auto__)){
var ns = temp__4423__auto__;
return [cljs.core.str(ns),cljs.core.str("/")].join('');
} else {
return null;
}
})()),cljs.core.str(new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(m))].join(''));

if(cljs.core.truth_(new cljs.core.Keyword(null,"protocol","protocol",652470118).cljs$core$IFn$_invoke$arity$1(m))){
cljs.core.println.call(null,"Protocol");
} else {
}

if(cljs.core.truth_(new cljs.core.Keyword(null,"forms","forms",2045992350).cljs$core$IFn$_invoke$arity$1(m))){
var seq__31262_31274 = cljs.core.seq.call(null,new cljs.core.Keyword(null,"forms","forms",2045992350).cljs$core$IFn$_invoke$arity$1(m));
var chunk__31263_31275 = null;
var count__31264_31276 = (0);
var i__31265_31277 = (0);
while(true){
if((i__31265_31277 < count__31264_31276)){
var f_31278 = cljs.core._nth.call(null,chunk__31263_31275,i__31265_31277);
cljs.core.println.call(null,"  ",f_31278);

var G__31279 = seq__31262_31274;
var G__31280 = chunk__31263_31275;
var G__31281 = count__31264_31276;
var G__31282 = (i__31265_31277 + (1));
seq__31262_31274 = G__31279;
chunk__31263_31275 = G__31280;
count__31264_31276 = G__31281;
i__31265_31277 = G__31282;
continue;
} else {
var temp__4423__auto___31283 = cljs.core.seq.call(null,seq__31262_31274);
if(temp__4423__auto___31283){
var seq__31262_31284__$1 = temp__4423__auto___31283;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__31262_31284__$1)){
var c__18858__auto___31285 = cljs.core.chunk_first.call(null,seq__31262_31284__$1);
var G__31286 = cljs.core.chunk_rest.call(null,seq__31262_31284__$1);
var G__31287 = c__18858__auto___31285;
var G__31288 = cljs.core.count.call(null,c__18858__auto___31285);
var G__31289 = (0);
seq__31262_31274 = G__31286;
chunk__31263_31275 = G__31287;
count__31264_31276 = G__31288;
i__31265_31277 = G__31289;
continue;
} else {
var f_31290 = cljs.core.first.call(null,seq__31262_31284__$1);
cljs.core.println.call(null,"  ",f_31290);

var G__31291 = cljs.core.next.call(null,seq__31262_31284__$1);
var G__31292 = null;
var G__31293 = (0);
var G__31294 = (0);
seq__31262_31274 = G__31291;
chunk__31263_31275 = G__31292;
count__31264_31276 = G__31293;
i__31265_31277 = G__31294;
continue;
}
} else {
}
}
break;
}
} else {
if(cljs.core.truth_(new cljs.core.Keyword(null,"arglists","arglists",1661989754).cljs$core$IFn$_invoke$arity$1(m))){
var arglists_31295 = new cljs.core.Keyword(null,"arglists","arglists",1661989754).cljs$core$IFn$_invoke$arity$1(m);
if(cljs.core.truth_((function (){var or__18073__auto__ = new cljs.core.Keyword(null,"macro","macro",-867863404).cljs$core$IFn$_invoke$arity$1(m);
if(cljs.core.truth_(or__18073__auto__)){
return or__18073__auto__;
} else {
return new cljs.core.Keyword(null,"repl-special-function","repl-special-function",1262603725).cljs$core$IFn$_invoke$arity$1(m);
}
})())){
cljs.core.prn.call(null,arglists_31295);
} else {
cljs.core.prn.call(null,((cljs.core._EQ_.call(null,new cljs.core.Symbol(null,"quote","quote",1377916282,null),cljs.core.first.call(null,arglists_31295)))?cljs.core.second.call(null,arglists_31295):arglists_31295));
}
} else {
}
}

if(cljs.core.truth_(new cljs.core.Keyword(null,"special-form","special-form",-1326536374).cljs$core$IFn$_invoke$arity$1(m))){
cljs.core.println.call(null,"Special Form");

cljs.core.println.call(null," ",new cljs.core.Keyword(null,"doc","doc",1913296891).cljs$core$IFn$_invoke$arity$1(m));

if(cljs.core.contains_QMARK_.call(null,m,new cljs.core.Keyword(null,"url","url",276297046))){
if(cljs.core.truth_(new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(m))){
return cljs.core.println.call(null,[cljs.core.str("\n  Please see http://clojure.org/"),cljs.core.str(new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(m))].join(''));
} else {
return null;
}
} else {
return cljs.core.println.call(null,[cljs.core.str("\n  Please see http://clojure.org/special_forms#"),cljs.core.str(new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(m))].join(''));
}
} else {
if(cljs.core.truth_(new cljs.core.Keyword(null,"macro","macro",-867863404).cljs$core$IFn$_invoke$arity$1(m))){
cljs.core.println.call(null,"Macro");
} else {
}

if(cljs.core.truth_(new cljs.core.Keyword(null,"repl-special-function","repl-special-function",1262603725).cljs$core$IFn$_invoke$arity$1(m))){
cljs.core.println.call(null,"REPL Special Function");
} else {
}

cljs.core.println.call(null," ",new cljs.core.Keyword(null,"doc","doc",1913296891).cljs$core$IFn$_invoke$arity$1(m));

if(cljs.core.truth_(new cljs.core.Keyword(null,"protocol","protocol",652470118).cljs$core$IFn$_invoke$arity$1(m))){
var seq__31266 = cljs.core.seq.call(null,new cljs.core.Keyword(null,"methods","methods",453930866).cljs$core$IFn$_invoke$arity$1(m));
var chunk__31267 = null;
var count__31268 = (0);
var i__31269 = (0);
while(true){
if((i__31269 < count__31268)){
var vec__31270 = cljs.core._nth.call(null,chunk__31267,i__31269);
var name = cljs.core.nth.call(null,vec__31270,(0),null);
var map__31271 = cljs.core.nth.call(null,vec__31270,(1),null);
var map__31271__$1 = ((cljs.core.seq_QMARK_.call(null,map__31271))?cljs.core.apply.call(null,cljs.core.hash_map,map__31271):map__31271);
var doc = cljs.core.get.call(null,map__31271__$1,new cljs.core.Keyword(null,"doc","doc",1913296891));
var arglists = cljs.core.get.call(null,map__31271__$1,new cljs.core.Keyword(null,"arglists","arglists",1661989754));
cljs.core.println.call(null);

cljs.core.println.call(null," ",name);

cljs.core.println.call(null," ",arglists);

if(cljs.core.truth_(doc)){
cljs.core.println.call(null," ",doc);
} else {
}

var G__31296 = seq__31266;
var G__31297 = chunk__31267;
var G__31298 = count__31268;
var G__31299 = (i__31269 + (1));
seq__31266 = G__31296;
chunk__31267 = G__31297;
count__31268 = G__31298;
i__31269 = G__31299;
continue;
} else {
var temp__4423__auto__ = cljs.core.seq.call(null,seq__31266);
if(temp__4423__auto__){
var seq__31266__$1 = temp__4423__auto__;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__31266__$1)){
var c__18858__auto__ = cljs.core.chunk_first.call(null,seq__31266__$1);
var G__31300 = cljs.core.chunk_rest.call(null,seq__31266__$1);
var G__31301 = c__18858__auto__;
var G__31302 = cljs.core.count.call(null,c__18858__auto__);
var G__31303 = (0);
seq__31266 = G__31300;
chunk__31267 = G__31301;
count__31268 = G__31302;
i__31269 = G__31303;
continue;
} else {
var vec__31272 = cljs.core.first.call(null,seq__31266__$1);
var name = cljs.core.nth.call(null,vec__31272,(0),null);
var map__31273 = cljs.core.nth.call(null,vec__31272,(1),null);
var map__31273__$1 = ((cljs.core.seq_QMARK_.call(null,map__31273))?cljs.core.apply.call(null,cljs.core.hash_map,map__31273):map__31273);
var doc = cljs.core.get.call(null,map__31273__$1,new cljs.core.Keyword(null,"doc","doc",1913296891));
var arglists = cljs.core.get.call(null,map__31273__$1,new cljs.core.Keyword(null,"arglists","arglists",1661989754));
cljs.core.println.call(null);

cljs.core.println.call(null," ",name);

cljs.core.println.call(null," ",arglists);

if(cljs.core.truth_(doc)){
cljs.core.println.call(null," ",doc);
} else {
}

var G__31304 = cljs.core.next.call(null,seq__31266__$1);
var G__31305 = null;
var G__31306 = (0);
var G__31307 = (0);
seq__31266 = G__31304;
chunk__31267 = G__31305;
count__31268 = G__31306;
i__31269 = G__31307;
continue;
}
} else {
return null;
}
}
break;
}
} else {
return null;
}
}
});

//# sourceMappingURL=repl.js.map?rel=1431620936723