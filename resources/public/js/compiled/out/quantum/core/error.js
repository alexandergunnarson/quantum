// Compiled by ClojureScript 0.0-3269 {}
goog.provide('quantum.core.error');
goog.require('cljs.core');
goog.require('quantum.core.ns');
quantum.core.error.throw_PLUS_ = (function quantum$core$error$throw_PLUS_(err){
throw err;
});
/**
 * Throws an exception with the given message @message if
 * @expr evaluates to false.
 * 
 * Specifically for use with :pre and :post conditions.
 */
quantum.core.error.with_throw = (function quantum$core$error$with_throw(expr,throw_content){
return cljs.core.sequence.call(null,cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core._conj.call(null,cljs.core.List.EMPTY,new cljs.core.Symbol(null,"if","if",1181717262,null)),cljs.core._conj.call(null,cljs.core.List.EMPTY,expr),cljs.core._conj.call(null,cljs.core.List.EMPTY,expr),cljs.core._conj.call(null,cljs.core.List.EMPTY,cljs.core.sequence.call(null,cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core._conj.call(null,cljs.core.List.EMPTY,new cljs.core.Symbol("quantum.core.error","throw+","quantum.core.error/throw+",1584937823,null)),cljs.core._conj.call(null,cljs.core.List.EMPTY,throw_content))))))));
});
quantum.core.error.unk_dispatch = (function quantum$core$error$unk_dispatch(dispatch){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"unk-dispatch","unk-dispatch",-334580425),new cljs.core.Keyword(null,"message","message",-406056002),[cljs.core.str("Unknown dispatch function '"),cljs.core.str(cljs.core.name.call(null,dispatch)),cljs.core.str("' requested.")].join('')], null);
});
quantum.core.error.unk_key = (function quantum$core$error$unk_key(k){
return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"unk-key","unk-key",311787746),new cljs.core.Keyword(null,"message","message",-406056002),[cljs.core.str("Unknown dispatch key '"),cljs.core.str(cljs.core.name.call(null,k)),cljs.core.str("' requested.")].join('')], null);
});

//# sourceMappingURL=error.js.map?rel=1431625570731