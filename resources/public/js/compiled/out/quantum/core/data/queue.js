// Compiled by ClojureScript 0.0-3269 {}
goog.provide('quantum.core.data.queue');
goog.require('cljs.core');
goog.require('quantum.core.collections');
goog.require('clojure.core.rrb_vector');
goog.require('quantum.core.numeric');
goog.require('quantum.core.ns');
goog.require('clojure.core.rrb_vector.rrbt');
goog.require('quantum.core.loops');
/**
 * Creates an empty persistent queue, or one populated with a collection.
 */
quantum.core.data.queue.queue = (function quantum$core$data$queue$queue(){
var G__26031 = arguments.length;
switch (G__26031) {
case 0:
return quantum.core.data.queue.queue.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return quantum.core.data.queue.queue.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

quantum.core.data.queue.queue.cljs$core$IFn$_invoke$arity$0 = (function (){
return cljs.core.PersistentQueue.EMPTY;
});

quantum.core.data.queue.queue.cljs$core$IFn$_invoke$arity$1 = (function (coll){
return quantum.core.collections.into.call(null,quantum.core.data.queue.queue.call(null),coll);
});

quantum.core.data.queue.queue.cljs$lang$maxFixedArity = 1;

//# sourceMappingURL=queue.js.map?rel=1431625572943