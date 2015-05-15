// Compiled by ClojureScript 0.0-3269 {}
goog.provide('quantum.core.loops');
goog.require('cljs.core');
goog.require('quantum.core.ns');
goog.require('quantum.core.data.map');
goog.require('quantum.core.macros');
goog.require('quantum.core.type');
/**
 * |reduce|s over 2 values in a collection with each pass.
 * 
 * Doesn't use CollReduce... so not as fast as |reduce|.
 */
quantum.core.loops.reduce_2 = (function quantum$core$loops$reduce_2(func,init,coll){
var ret = init;
var coll_n = coll;
while(true){
if(cljs.core.empty_QMARK_.call(null,coll_n)){
return ret;
} else {
var G__23299 = func.call(null,ret,cljs.core.first.call(null,coll_n),cljs.core.second.call(null,coll_n));
var G__23300 = cljs.core.rest.call(null,cljs.core.rest.call(null,coll_n));
ret = G__23299;
coll_n = G__23300;
continue;
}
break;
}
});
quantum.core.loops.while_recur = (function quantum$core$loops$while_recur(obj_0,pred,func){
var obj = obj_0;
while(true){
if(cljs.core.not.call(null,pred.call(null,obj))){
return obj;
} else {
var G__23301 = func.call(null,obj);
obj = G__23301;
continue;
}
break;
}
});

//# sourceMappingURL=loops.js.map?rel=1431625569480