// Compiled by ClojureScript 0.0-3269 {}
goog.provide('quantum.cljs_test');
goog.require('cljs.core');
goog.require('quantum.core.ns');
cljs.core.enable_console_print_BANG_.call(null);
quantum.cljs_test.init_BANG_ = (function quantum$cljs_test$init_BANG_(){
cljs.core.println.call(null,"Edits to this text should show up in your developer console!! YAY :D");

return cljs.core.println.call(null,(function (){var seq__30797 = cljs.core.seq.call(null,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [(4),(3),(5)], null));
var chunk__30798 = null;
var count__30799 = (0);
var i__30800 = (0);
while(true){
if((i__30800 < count__30799)){
var a = cljs.core._nth.call(null,chunk__30798,i__30800);
cljs.core.println.call(null,a);

var G__30801 = seq__30797;
var G__30802 = chunk__30798;
var G__30803 = count__30799;
var G__30804 = (i__30800 + (1));
seq__30797 = G__30801;
chunk__30798 = G__30802;
count__30799 = G__30803;
i__30800 = G__30804;
continue;
} else {
var temp__4423__auto__ = cljs.core.seq.call(null,seq__30797);
if(temp__4423__auto__){
var seq__30797__$1 = temp__4423__auto__;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__30797__$1)){
var c__18858__auto__ = cljs.core.chunk_first.call(null,seq__30797__$1);
var G__30805 = cljs.core.chunk_rest.call(null,seq__30797__$1);
var G__30806 = c__18858__auto__;
var G__30807 = cljs.core.count.call(null,c__18858__auto__);
var G__30808 = (0);
seq__30797 = G__30805;
chunk__30798 = G__30806;
count__30799 = G__30807;
i__30800 = G__30808;
continue;
} else {
var a = cljs.core.first.call(null,seq__30797__$1);
cljs.core.println.call(null,a);

var G__30809 = cljs.core.next.call(null,seq__30797__$1);
var G__30810 = null;
var G__30811 = (0);
var G__30812 = (0);
seq__30797 = G__30809;
chunk__30798 = G__30810;
count__30799 = G__30811;
i__30800 = G__30812;
continue;
}
} else {
return null;
}
}
break;
}
})());
});
quantum.cljs_test.init_BANG_.call(null);
if(typeof quantum.cljs_test.app_state !== 'undefined'){
} else {
quantum.cljs_test.app_state = cljs.core.atom.call(null,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"text","text",-1790561697),"Hello world!"], null));
}

//# sourceMappingURL=cljs_test.js.map?rel=1431625736195