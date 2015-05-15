// Compiled by ClojureScript 0.0-3269 {}
goog.provide('cljs.core.async');
goog.require('cljs.core');
goog.require('cljs.core.async.impl.channels');
goog.require('cljs.core.async.impl.dispatch');
goog.require('cljs.core.async.impl.ioc_helpers');
goog.require('cljs.core.async.impl.protocols');
goog.require('cljs.core.async.impl.buffers');
goog.require('cljs.core.async.impl.timers');
cljs.core.async.fn_handler = (function cljs$core$async$fn_handler(f){
if(typeof cljs.core.async.t20980 !== 'undefined'){
} else {

/**
* @constructor
*/
cljs.core.async.t20980 = (function (fn_handler,f,meta20981){
this.fn_handler = fn_handler;
this.f = f;
this.meta20981 = meta20981;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
})
cljs.core.async.t20980.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_20982,meta20981__$1){
var self__ = this;
var _20982__$1 = this;
return (new cljs.core.async.t20980(self__.fn_handler,self__.f,meta20981__$1));
});

cljs.core.async.t20980.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_20982){
var self__ = this;
var _20982__$1 = this;
return self__.meta20981;
});

cljs.core.async.t20980.prototype.cljs$core$async$impl$protocols$Handler$ = true;

cljs.core.async.t20980.prototype.cljs$core$async$impl$protocols$Handler$active_QMARK_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return true;
});

cljs.core.async.t20980.prototype.cljs$core$async$impl$protocols$Handler$commit$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return self__.f;
});

cljs.core.async.t20980.getBasis = (function (){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"fn-handler","fn-handler",648785851,null),new cljs.core.Symbol(null,"f","f",43394975,null),new cljs.core.Symbol(null,"meta20981","meta20981",396708505,null)], null);
});

cljs.core.async.t20980.cljs$lang$type = true;

cljs.core.async.t20980.cljs$lang$ctorStr = "cljs.core.async/t20980";

cljs.core.async.t20980.cljs$lang$ctorPrWriter = (function (this__18652__auto__,writer__18653__auto__,opt__18654__auto__){
return cljs.core._write.call(null,writer__18653__auto__,"cljs.core.async/t20980");
});

cljs.core.async.__GT_t20980 = (function cljs$core$async$fn_handler_$___GT_t20980(fn_handler__$1,f__$1,meta20981){
return (new cljs.core.async.t20980(fn_handler__$1,f__$1,meta20981));
});

}

return (new cljs.core.async.t20980(cljs$core$async$fn_handler,f,cljs.core.PersistentArrayMap.EMPTY));
});
/**
 * Returns a fixed buffer of size n. When full, puts will block/park.
 */
cljs.core.async.buffer = (function cljs$core$async$buffer(n){
return cljs.core.async.impl.buffers.fixed_buffer.call(null,n);
});
/**
 * Returns a buffer of size n. When full, puts will complete but
 * val will be dropped (no transfer).
 */
cljs.core.async.dropping_buffer = (function cljs$core$async$dropping_buffer(n){
return cljs.core.async.impl.buffers.dropping_buffer.call(null,n);
});
/**
 * Returns a buffer of size n. When full, puts will complete, and be
 * buffered, but oldest elements in buffer will be dropped (not
 * transferred).
 */
cljs.core.async.sliding_buffer = (function cljs$core$async$sliding_buffer(n){
return cljs.core.async.impl.buffers.sliding_buffer.call(null,n);
});
/**
 * Returns true if a channel created with buff will never block. That is to say,
 * puts into this buffer will never cause the buffer to be full.
 */
cljs.core.async.unblocking_buffer_QMARK_ = (function cljs$core$async$unblocking_buffer_QMARK_(buff){
var G__20984 = buff;
if(G__20984){
var bit__18747__auto__ = null;
if(cljs.core.truth_((function (){var or__18073__auto__ = bit__18747__auto__;
if(cljs.core.truth_(or__18073__auto__)){
return or__18073__auto__;
} else {
return G__20984.cljs$core$async$impl$protocols$UnblockingBuffer$;
}
})())){
return true;
} else {
if((!G__20984.cljs$lang$protocol_mask$partition$)){
return cljs.core.native_satisfies_QMARK_.call(null,cljs.core.async.impl.protocols.UnblockingBuffer,G__20984);
} else {
return false;
}
}
} else {
return cljs.core.native_satisfies_QMARK_.call(null,cljs.core.async.impl.protocols.UnblockingBuffer,G__20984);
}
});
/**
 * Creates a channel with an optional buffer, an optional transducer (like (map f),
 * (filter p) etc or a composition thereof), and an optional exception handler.
 * If buf-or-n is a number, will create and use a fixed buffer of that size. If a
 * transducer is supplied a buffer must be specified. ex-handler must be a
 * fn of one argument - if an exception occurs during transformation it will be called
 * with the thrown value as an argument, and any non-nil return value will be placed
 * in the channel.
 */
cljs.core.async.chan = (function cljs$core$async$chan(){
var G__20986 = arguments.length;
switch (G__20986) {
case 0:
return cljs.core.async.chan.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return cljs.core.async.chan.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return cljs.core.async.chan.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

cljs.core.async.chan.cljs$core$IFn$_invoke$arity$0 = (function (){
return cljs.core.async.chan.call(null,null);
});

cljs.core.async.chan.cljs$core$IFn$_invoke$arity$1 = (function (buf_or_n){
return cljs.core.async.chan.call(null,buf_or_n,null,null);
});

cljs.core.async.chan.cljs$core$IFn$_invoke$arity$2 = (function (buf_or_n,xform){
return cljs.core.async.chan.call(null,buf_or_n,xform,null);
});

cljs.core.async.chan.cljs$core$IFn$_invoke$arity$3 = (function (buf_or_n,xform,ex_handler){
var buf_or_n__$1 = ((cljs.core._EQ_.call(null,buf_or_n,(0)))?null:buf_or_n);
if(cljs.core.truth_(xform)){
if(cljs.core.truth_(buf_or_n__$1)){
} else {
throw (new Error([cljs.core.str("Assert failed: "),cljs.core.str("buffer must be supplied when transducer is"),cljs.core.str("\n"),cljs.core.str(cljs.core.pr_str.call(null,new cljs.core.Symbol(null,"buf-or-n","buf-or-n",-1646815050,null)))].join('')));
}
} else {
}

return cljs.core.async.impl.channels.chan.call(null,((typeof buf_or_n__$1 === 'number')?cljs.core.async.buffer.call(null,buf_or_n__$1):buf_or_n__$1),xform,ex_handler);
});

cljs.core.async.chan.cljs$lang$maxFixedArity = 3;
/**
 * Returns a channel that will close after msecs
 */
cljs.core.async.timeout = (function cljs$core$async$timeout(msecs){
return cljs.core.async.impl.timers.timeout.call(null,msecs);
});
/**
 * takes a val from port. Must be called inside a (go ...) block. Will
 * return nil if closed. Will park if nothing is available.
 * Returns true unless port is already closed
 */
cljs.core.async._LT__BANG_ = (function cljs$core$async$_LT__BANG_(port){
throw (new Error("<! used not in (go ...) block"));
});
/**
 * Asynchronously takes a val from port, passing to fn1. Will pass nil
 * if closed. If on-caller? (default true) is true, and value is
 * immediately available, will call fn1 on calling thread.
 * Returns nil.
 */
cljs.core.async.take_BANG_ = (function cljs$core$async$take_BANG_(){
var G__20989 = arguments.length;
switch (G__20989) {
case 2:
return cljs.core.async.take_BANG_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return cljs.core.async.take_BANG_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

cljs.core.async.take_BANG_.cljs$core$IFn$_invoke$arity$2 = (function (port,fn1){
return cljs.core.async.take_BANG_.call(null,port,fn1,true);
});

cljs.core.async.take_BANG_.cljs$core$IFn$_invoke$arity$3 = (function (port,fn1,on_caller_QMARK_){
var ret = cljs.core.async.impl.protocols.take_BANG_.call(null,port,cljs.core.async.fn_handler.call(null,fn1));
if(cljs.core.truth_(ret)){
var val_20991 = cljs.core.deref.call(null,ret);
if(cljs.core.truth_(on_caller_QMARK_)){
fn1.call(null,val_20991);
} else {
cljs.core.async.impl.dispatch.run.call(null,((function (val_20991,ret){
return (function (){
return fn1.call(null,val_20991);
});})(val_20991,ret))
);
}
} else {
}

return null;
});

cljs.core.async.take_BANG_.cljs$lang$maxFixedArity = 3;
cljs.core.async.nop = (function cljs$core$async$nop(_){
return null;
});
cljs.core.async.fhnop = cljs.core.async.fn_handler.call(null,cljs.core.async.nop);
/**
 * puts a val into port. nil values are not allowed. Must be called
 * inside a (go ...) block. Will park if no buffer space is available.
 * Returns true unless port is already closed.
 */
cljs.core.async._GT__BANG_ = (function cljs$core$async$_GT__BANG_(port,val){
throw (new Error(">! used not in (go ...) block"));
});
/**
 * Asynchronously puts a val into port, calling fn0 (if supplied) when
 * complete. nil values are not allowed. Will throw if closed. If
 * on-caller? (default true) is true, and the put is immediately
 * accepted, will call fn0 on calling thread.  Returns nil.
 */
cljs.core.async.put_BANG_ = (function cljs$core$async$put_BANG_(){
var G__20993 = arguments.length;
switch (G__20993) {
case 2:
return cljs.core.async.put_BANG_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return cljs.core.async.put_BANG_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return cljs.core.async.put_BANG_.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

cljs.core.async.put_BANG_.cljs$core$IFn$_invoke$arity$2 = (function (port,val){
var temp__4421__auto__ = cljs.core.async.impl.protocols.put_BANG_.call(null,port,val,cljs.core.async.fhnop);
if(cljs.core.truth_(temp__4421__auto__)){
var ret = temp__4421__auto__;
return cljs.core.deref.call(null,ret);
} else {
return true;
}
});

cljs.core.async.put_BANG_.cljs$core$IFn$_invoke$arity$3 = (function (port,val,fn1){
return cljs.core.async.put_BANG_.call(null,port,val,fn1,true);
});

cljs.core.async.put_BANG_.cljs$core$IFn$_invoke$arity$4 = (function (port,val,fn1,on_caller_QMARK_){
var temp__4421__auto__ = cljs.core.async.impl.protocols.put_BANG_.call(null,port,val,cljs.core.async.fn_handler.call(null,fn1));
if(cljs.core.truth_(temp__4421__auto__)){
var retb = temp__4421__auto__;
var ret = cljs.core.deref.call(null,retb);
if(cljs.core.truth_(on_caller_QMARK_)){
fn1.call(null,ret);
} else {
cljs.core.async.impl.dispatch.run.call(null,((function (ret,retb,temp__4421__auto__){
return (function (){
return fn1.call(null,ret);
});})(ret,retb,temp__4421__auto__))
);
}

return ret;
} else {
return true;
}
});

cljs.core.async.put_BANG_.cljs$lang$maxFixedArity = 4;
cljs.core.async.close_BANG_ = (function cljs$core$async$close_BANG_(port){
return cljs.core.async.impl.protocols.close_BANG_.call(null,port);
});
cljs.core.async.random_array = (function cljs$core$async$random_array(n){
var a = (new Array(n));
var n__18958__auto___20995 = n;
var x_20996 = (0);
while(true){
if((x_20996 < n__18958__auto___20995)){
(a[x_20996] = (0));

var G__20997 = (x_20996 + (1));
x_20996 = G__20997;
continue;
} else {
}
break;
}

var i = (1);
while(true){
if(cljs.core._EQ_.call(null,i,n)){
return a;
} else {
var j = cljs.core.rand_int.call(null,i);
(a[i] = (a[j]));

(a[j] = i);

var G__20998 = (i + (1));
i = G__20998;
continue;
}
break;
}
});
cljs.core.async.alt_flag = (function cljs$core$async$alt_flag(){
var flag = cljs.core.atom.call(null,true);
if(typeof cljs.core.async.t21002 !== 'undefined'){
} else {

/**
* @constructor
*/
cljs.core.async.t21002 = (function (alt_flag,flag,meta21003){
this.alt_flag = alt_flag;
this.flag = flag;
this.meta21003 = meta21003;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
})
cljs.core.async.t21002.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = ((function (flag){
return (function (_21004,meta21003__$1){
var self__ = this;
var _21004__$1 = this;
return (new cljs.core.async.t21002(self__.alt_flag,self__.flag,meta21003__$1));
});})(flag))
;

cljs.core.async.t21002.prototype.cljs$core$IMeta$_meta$arity$1 = ((function (flag){
return (function (_21004){
var self__ = this;
var _21004__$1 = this;
return self__.meta21003;
});})(flag))
;

cljs.core.async.t21002.prototype.cljs$core$async$impl$protocols$Handler$ = true;

cljs.core.async.t21002.prototype.cljs$core$async$impl$protocols$Handler$active_QMARK_$arity$1 = ((function (flag){
return (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.deref.call(null,self__.flag);
});})(flag))
;

cljs.core.async.t21002.prototype.cljs$core$async$impl$protocols$Handler$commit$arity$1 = ((function (flag){
return (function (_){
var self__ = this;
var ___$1 = this;
cljs.core.reset_BANG_.call(null,self__.flag,null);

return true;
});})(flag))
;

cljs.core.async.t21002.getBasis = ((function (flag){
return (function (){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"alt-flag","alt-flag",-1794972754,null),new cljs.core.Symbol(null,"flag","flag",-1565787888,null),new cljs.core.Symbol(null,"meta21003","meta21003",-1646534807,null)], null);
});})(flag))
;

cljs.core.async.t21002.cljs$lang$type = true;

cljs.core.async.t21002.cljs$lang$ctorStr = "cljs.core.async/t21002";

cljs.core.async.t21002.cljs$lang$ctorPrWriter = ((function (flag){
return (function (this__18652__auto__,writer__18653__auto__,opt__18654__auto__){
return cljs.core._write.call(null,writer__18653__auto__,"cljs.core.async/t21002");
});})(flag))
;

cljs.core.async.__GT_t21002 = ((function (flag){
return (function cljs$core$async$alt_flag_$___GT_t21002(alt_flag__$1,flag__$1,meta21003){
return (new cljs.core.async.t21002(alt_flag__$1,flag__$1,meta21003));
});})(flag))
;

}

return (new cljs.core.async.t21002(cljs$core$async$alt_flag,flag,cljs.core.PersistentArrayMap.EMPTY));
});
cljs.core.async.alt_handler = (function cljs$core$async$alt_handler(flag,cb){
if(typeof cljs.core.async.t21008 !== 'undefined'){
} else {

/**
* @constructor
*/
cljs.core.async.t21008 = (function (alt_handler,flag,cb,meta21009){
this.alt_handler = alt_handler;
this.flag = flag;
this.cb = cb;
this.meta21009 = meta21009;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
})
cljs.core.async.t21008.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_21010,meta21009__$1){
var self__ = this;
var _21010__$1 = this;
return (new cljs.core.async.t21008(self__.alt_handler,self__.flag,self__.cb,meta21009__$1));
});

cljs.core.async.t21008.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_21010){
var self__ = this;
var _21010__$1 = this;
return self__.meta21009;
});

cljs.core.async.t21008.prototype.cljs$core$async$impl$protocols$Handler$ = true;

cljs.core.async.t21008.prototype.cljs$core$async$impl$protocols$Handler$active_QMARK_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.async.impl.protocols.active_QMARK_.call(null,self__.flag);
});

cljs.core.async.t21008.prototype.cljs$core$async$impl$protocols$Handler$commit$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
cljs.core.async.impl.protocols.commit.call(null,self__.flag);

return self__.cb;
});

cljs.core.async.t21008.getBasis = (function (){
return new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"alt-handler","alt-handler",963786170,null),new cljs.core.Symbol(null,"flag","flag",-1565787888,null),new cljs.core.Symbol(null,"cb","cb",-2064487928,null),new cljs.core.Symbol(null,"meta21009","meta21009",-1789459470,null)], null);
});

cljs.core.async.t21008.cljs$lang$type = true;

cljs.core.async.t21008.cljs$lang$ctorStr = "cljs.core.async/t21008";

cljs.core.async.t21008.cljs$lang$ctorPrWriter = (function (this__18652__auto__,writer__18653__auto__,opt__18654__auto__){
return cljs.core._write.call(null,writer__18653__auto__,"cljs.core.async/t21008");
});

cljs.core.async.__GT_t21008 = (function cljs$core$async$alt_handler_$___GT_t21008(alt_handler__$1,flag__$1,cb__$1,meta21009){
return (new cljs.core.async.t21008(alt_handler__$1,flag__$1,cb__$1,meta21009));
});

}

return (new cljs.core.async.t21008(cljs$core$async$alt_handler,flag,cb,cljs.core.PersistentArrayMap.EMPTY));
});
/**
 * returns derefable [val port] if immediate, nil if enqueued
 */
cljs.core.async.do_alts = (function cljs$core$async$do_alts(fret,ports,opts){
var flag = cljs.core.async.alt_flag.call(null);
var n = cljs.core.count.call(null,ports);
var idxs = cljs.core.async.random_array.call(null,n);
var priority = new cljs.core.Keyword(null,"priority","priority",1431093715).cljs$core$IFn$_invoke$arity$1(opts);
var ret = (function (){var i = (0);
while(true){
if((i < n)){
var idx = (cljs.core.truth_(priority)?i:(idxs[i]));
var port = cljs.core.nth.call(null,ports,idx);
var wport = ((cljs.core.vector_QMARK_.call(null,port))?port.call(null,(0)):null);
var vbox = (cljs.core.truth_(wport)?(function (){var val = port.call(null,(1));
return cljs.core.async.impl.protocols.put_BANG_.call(null,wport,val,cljs.core.async.alt_handler.call(null,flag,((function (i,val,idx,port,wport,flag,n,idxs,priority){
return (function (p1__21011_SHARP_){
return fret.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [p1__21011_SHARP_,wport], null));
});})(i,val,idx,port,wport,flag,n,idxs,priority))
));
})():cljs.core.async.impl.protocols.take_BANG_.call(null,port,cljs.core.async.alt_handler.call(null,flag,((function (i,idx,port,wport,flag,n,idxs,priority){
return (function (p1__21012_SHARP_){
return fret.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [p1__21012_SHARP_,port], null));
});})(i,idx,port,wport,flag,n,idxs,priority))
)));
if(cljs.core.truth_(vbox)){
return cljs.core.async.impl.channels.box.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.deref.call(null,vbox),(function (){var or__18073__auto__ = wport;
if(cljs.core.truth_(or__18073__auto__)){
return or__18073__auto__;
} else {
return port;
}
})()], null));
} else {
var G__21013 = (i + (1));
i = G__21013;
continue;
}
} else {
return null;
}
break;
}
})();
var or__18073__auto__ = ret;
if(cljs.core.truth_(or__18073__auto__)){
return or__18073__auto__;
} else {
if(cljs.core.contains_QMARK_.call(null,opts,new cljs.core.Keyword(null,"default","default",-1987822328))){
var temp__4423__auto__ = (function (){var and__18061__auto__ = cljs.core.async.impl.protocols.active_QMARK_.call(null,flag);
if(cljs.core.truth_(and__18061__auto__)){
return cljs.core.async.impl.protocols.commit.call(null,flag);
} else {
return and__18061__auto__;
}
})();
if(cljs.core.truth_(temp__4423__auto__)){
var got = temp__4423__auto__;
return cljs.core.async.impl.channels.box.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"default","default",-1987822328).cljs$core$IFn$_invoke$arity$1(opts),new cljs.core.Keyword(null,"default","default",-1987822328)], null));
} else {
return null;
}
} else {
return null;
}
}
});
/**
 * Completes at most one of several channel operations. Must be called
 * inside a (go ...) block. ports is a vector of channel endpoints,
 * which can be either a channel to take from or a vector of
 * [channel-to-put-to val-to-put], in any combination. Takes will be
 * made as if by <!, and puts will be made as if by >!. Unless
 * the :priority option is true, if more than one port operation is
 * ready a non-deterministic choice will be made. If no operation is
 * ready and a :default value is supplied, [default-val :default] will
 * be returned, otherwise alts! will park until the first operation to
 * become ready completes. Returns [val port] of the completed
 * operation, where val is the value taken for takes, and a
 * boolean (true unless already closed, as per put!) for puts.
 * 
 * opts are passed as :key val ... Supported options:
 * 
 * :default val - the value to use if none of the operations are immediately ready
 * :priority true - (default nil) when true, the operations will be tried in order.
 * 
 * Note: there is no guarantee that the port exps or val exprs will be
 * used, nor in what order should they be, so they should not be
 * depended upon for side effects.
 */
cljs.core.async.alts_BANG_ = (function cljs$core$async$alts_BANG_(){
var argseq__19113__auto__ = ((((1) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(1)),(0))):null);
return cljs.core.async.alts_BANG_.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__19113__auto__);
});

cljs.core.async.alts_BANG_.cljs$core$IFn$_invoke$arity$variadic = (function (ports,p__21016){
var map__21017 = p__21016;
var map__21017__$1 = ((cljs.core.seq_QMARK_.call(null,map__21017))?cljs.core.apply.call(null,cljs.core.hash_map,map__21017):map__21017);
var opts = map__21017__$1;
throw (new Error("alts! used not in (go ...) block"));
});

cljs.core.async.alts_BANG_.cljs$lang$maxFixedArity = (1);

cljs.core.async.alts_BANG_.cljs$lang$applyTo = (function (seq21014){
var G__21015 = cljs.core.first.call(null,seq21014);
var seq21014__$1 = cljs.core.next.call(null,seq21014);
return cljs.core.async.alts_BANG_.cljs$core$IFn$_invoke$arity$variadic(G__21015,seq21014__$1);
});
/**
 * Takes elements from the from channel and supplies them to the to
 * channel. By default, the to channel will be closed when the from
 * channel closes, but can be determined by the close?  parameter. Will
 * stop consuming the from channel if the to channel closes
 */
cljs.core.async.pipe = (function cljs$core$async$pipe(){
var G__21019 = arguments.length;
switch (G__21019) {
case 2:
return cljs.core.async.pipe.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return cljs.core.async.pipe.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

cljs.core.async.pipe.cljs$core$IFn$_invoke$arity$2 = (function (from,to){
return cljs.core.async.pipe.call(null,from,to,true);
});

cljs.core.async.pipe.cljs$core$IFn$_invoke$arity$3 = (function (from,to,close_QMARK_){
var c__20932__auto___21068 = cljs.core.async.chan.call(null,(1));
cljs.core.async.impl.dispatch.run.call(null,((function (c__20932__auto___21068){
return (function (){
var f__20933__auto__ = (function (){var switch__20870__auto__ = ((function (c__20932__auto___21068){
return (function (state_21043){
var state_val_21044 = (state_21043[(1)]);
if((state_val_21044 === (7))){
var inst_21039 = (state_21043[(2)]);
var state_21043__$1 = state_21043;
var statearr_21045_21069 = state_21043__$1;
(statearr_21045_21069[(2)] = inst_21039);

(statearr_21045_21069[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21044 === (1))){
var state_21043__$1 = state_21043;
var statearr_21046_21070 = state_21043__$1;
(statearr_21046_21070[(2)] = null);

(statearr_21046_21070[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21044 === (4))){
var inst_21022 = (state_21043[(7)]);
var inst_21022__$1 = (state_21043[(2)]);
var inst_21023 = (inst_21022__$1 == null);
var state_21043__$1 = (function (){var statearr_21047 = state_21043;
(statearr_21047[(7)] = inst_21022__$1);

return statearr_21047;
})();
if(cljs.core.truth_(inst_21023)){
var statearr_21048_21071 = state_21043__$1;
(statearr_21048_21071[(1)] = (5));

} else {
var statearr_21049_21072 = state_21043__$1;
(statearr_21049_21072[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21044 === (13))){
var state_21043__$1 = state_21043;
var statearr_21050_21073 = state_21043__$1;
(statearr_21050_21073[(2)] = null);

(statearr_21050_21073[(1)] = (14));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21044 === (6))){
var inst_21022 = (state_21043[(7)]);
var state_21043__$1 = state_21043;
return cljs.core.async.impl.ioc_helpers.put_BANG_.call(null,state_21043__$1,(11),to,inst_21022);
} else {
if((state_val_21044 === (3))){
var inst_21041 = (state_21043[(2)]);
var state_21043__$1 = state_21043;
return cljs.core.async.impl.ioc_helpers.return_chan.call(null,state_21043__$1,inst_21041);
} else {
if((state_val_21044 === (12))){
var state_21043__$1 = state_21043;
var statearr_21051_21074 = state_21043__$1;
(statearr_21051_21074[(2)] = null);

(statearr_21051_21074[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21044 === (2))){
var state_21043__$1 = state_21043;
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_21043__$1,(4),from);
} else {
if((state_val_21044 === (11))){
var inst_21032 = (state_21043[(2)]);
var state_21043__$1 = state_21043;
if(cljs.core.truth_(inst_21032)){
var statearr_21052_21075 = state_21043__$1;
(statearr_21052_21075[(1)] = (12));

} else {
var statearr_21053_21076 = state_21043__$1;
(statearr_21053_21076[(1)] = (13));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21044 === (9))){
var state_21043__$1 = state_21043;
var statearr_21054_21077 = state_21043__$1;
(statearr_21054_21077[(2)] = null);

(statearr_21054_21077[(1)] = (10));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21044 === (5))){
var state_21043__$1 = state_21043;
if(cljs.core.truth_(close_QMARK_)){
var statearr_21055_21078 = state_21043__$1;
(statearr_21055_21078[(1)] = (8));

} else {
var statearr_21056_21079 = state_21043__$1;
(statearr_21056_21079[(1)] = (9));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21044 === (14))){
var inst_21037 = (state_21043[(2)]);
var state_21043__$1 = state_21043;
var statearr_21057_21080 = state_21043__$1;
(statearr_21057_21080[(2)] = inst_21037);

(statearr_21057_21080[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21044 === (10))){
var inst_21029 = (state_21043[(2)]);
var state_21043__$1 = state_21043;
var statearr_21058_21081 = state_21043__$1;
(statearr_21058_21081[(2)] = inst_21029);

(statearr_21058_21081[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21044 === (8))){
var inst_21026 = cljs.core.async.close_BANG_.call(null,to);
var state_21043__$1 = state_21043;
var statearr_21059_21082 = state_21043__$1;
(statearr_21059_21082[(2)] = inst_21026);

(statearr_21059_21082[(1)] = (10));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
return null;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
});})(c__20932__auto___21068))
;
return ((function (switch__20870__auto__,c__20932__auto___21068){
return (function() {
var cljs$core$async$state_machine__20871__auto__ = null;
var cljs$core$async$state_machine__20871__auto____0 = (function (){
var statearr_21063 = [null,null,null,null,null,null,null,null];
(statearr_21063[(0)] = cljs$core$async$state_machine__20871__auto__);

(statearr_21063[(1)] = (1));

return statearr_21063;
});
var cljs$core$async$state_machine__20871__auto____1 = (function (state_21043){
while(true){
var ret_value__20872__auto__ = (function (){try{while(true){
var result__20873__auto__ = switch__20870__auto__.call(null,state_21043);
if(cljs.core.keyword_identical_QMARK_.call(null,result__20873__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__20873__auto__;
}
break;
}
}catch (e21064){if((e21064 instanceof Object)){
var ex__20874__auto__ = e21064;
var statearr_21065_21083 = state_21043;
(statearr_21065_21083[(5)] = ex__20874__auto__);


cljs.core.async.impl.ioc_helpers.process_exception.call(null,state_21043);

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
throw e21064;

}
}})();
if(cljs.core.keyword_identical_QMARK_.call(null,ret_value__20872__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__21084 = state_21043;
state_21043 = G__21084;
continue;
} else {
return ret_value__20872__auto__;
}
break;
}
});
cljs$core$async$state_machine__20871__auto__ = function(state_21043){
switch(arguments.length){
case 0:
return cljs$core$async$state_machine__20871__auto____0.call(this);
case 1:
return cljs$core$async$state_machine__20871__auto____1.call(this,state_21043);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$state_machine__20871__auto____0;
cljs$core$async$state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$state_machine__20871__auto____1;
return cljs$core$async$state_machine__20871__auto__;
})()
;})(switch__20870__auto__,c__20932__auto___21068))
})();
var state__20934__auto__ = (function (){var statearr_21066 = f__20933__auto__.call(null);
(statearr_21066[cljs.core.async.impl.ioc_helpers.USER_START_IDX] = c__20932__auto___21068);

return statearr_21066;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped.call(null,state__20934__auto__);
});})(c__20932__auto___21068))
);


return to;
});

cljs.core.async.pipe.cljs$lang$maxFixedArity = 3;
cljs.core.async.pipeline_STAR_ = (function cljs$core$async$pipeline_STAR_(n,to,xf,from,close_QMARK_,ex_handler,type){
if((n > (0))){
} else {
throw (new Error([cljs.core.str("Assert failed: "),cljs.core.str(cljs.core.pr_str.call(null,cljs.core.list(new cljs.core.Symbol(null,"pos?","pos?",-244377722,null),new cljs.core.Symbol(null,"n","n",-2092305744,null))))].join('')));
}

var jobs = cljs.core.async.chan.call(null,n);
var results = cljs.core.async.chan.call(null,n);
var process = ((function (jobs,results){
return (function (p__21268){
var vec__21269 = p__21268;
var v = cljs.core.nth.call(null,vec__21269,(0),null);
var p = cljs.core.nth.call(null,vec__21269,(1),null);
var job = vec__21269;
if((job == null)){
cljs.core.async.close_BANG_.call(null,results);

return null;
} else {
var res = cljs.core.async.chan.call(null,(1),xf,ex_handler);
var c__20932__auto___21451 = cljs.core.async.chan.call(null,(1));
cljs.core.async.impl.dispatch.run.call(null,((function (c__20932__auto___21451,res,vec__21269,v,p,job,jobs,results){
return (function (){
var f__20933__auto__ = (function (){var switch__20870__auto__ = ((function (c__20932__auto___21451,res,vec__21269,v,p,job,jobs,results){
return (function (state_21274){
var state_val_21275 = (state_21274[(1)]);
if((state_val_21275 === (1))){
var state_21274__$1 = state_21274;
return cljs.core.async.impl.ioc_helpers.put_BANG_.call(null,state_21274__$1,(2),res,v);
} else {
if((state_val_21275 === (2))){
var inst_21271 = (state_21274[(2)]);
var inst_21272 = cljs.core.async.close_BANG_.call(null,res);
var state_21274__$1 = (function (){var statearr_21276 = state_21274;
(statearr_21276[(7)] = inst_21271);

return statearr_21276;
})();
return cljs.core.async.impl.ioc_helpers.return_chan.call(null,state_21274__$1,inst_21272);
} else {
return null;
}
}
});})(c__20932__auto___21451,res,vec__21269,v,p,job,jobs,results))
;
return ((function (switch__20870__auto__,c__20932__auto___21451,res,vec__21269,v,p,job,jobs,results){
return (function() {
var cljs$core$async$pipeline_STAR__$_state_machine__20871__auto__ = null;
var cljs$core$async$pipeline_STAR__$_state_machine__20871__auto____0 = (function (){
var statearr_21280 = [null,null,null,null,null,null,null,null];
(statearr_21280[(0)] = cljs$core$async$pipeline_STAR__$_state_machine__20871__auto__);

(statearr_21280[(1)] = (1));

return statearr_21280;
});
var cljs$core$async$pipeline_STAR__$_state_machine__20871__auto____1 = (function (state_21274){
while(true){
var ret_value__20872__auto__ = (function (){try{while(true){
var result__20873__auto__ = switch__20870__auto__.call(null,state_21274);
if(cljs.core.keyword_identical_QMARK_.call(null,result__20873__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__20873__auto__;
}
break;
}
}catch (e21281){if((e21281 instanceof Object)){
var ex__20874__auto__ = e21281;
var statearr_21282_21452 = state_21274;
(statearr_21282_21452[(5)] = ex__20874__auto__);


cljs.core.async.impl.ioc_helpers.process_exception.call(null,state_21274);

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
throw e21281;

}
}})();
if(cljs.core.keyword_identical_QMARK_.call(null,ret_value__20872__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__21453 = state_21274;
state_21274 = G__21453;
continue;
} else {
return ret_value__20872__auto__;
}
break;
}
});
cljs$core$async$pipeline_STAR__$_state_machine__20871__auto__ = function(state_21274){
switch(arguments.length){
case 0:
return cljs$core$async$pipeline_STAR__$_state_machine__20871__auto____0.call(this);
case 1:
return cljs$core$async$pipeline_STAR__$_state_machine__20871__auto____1.call(this,state_21274);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$pipeline_STAR__$_state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$pipeline_STAR__$_state_machine__20871__auto____0;
cljs$core$async$pipeline_STAR__$_state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$pipeline_STAR__$_state_machine__20871__auto____1;
return cljs$core$async$pipeline_STAR__$_state_machine__20871__auto__;
})()
;})(switch__20870__auto__,c__20932__auto___21451,res,vec__21269,v,p,job,jobs,results))
})();
var state__20934__auto__ = (function (){var statearr_21283 = f__20933__auto__.call(null);
(statearr_21283[cljs.core.async.impl.ioc_helpers.USER_START_IDX] = c__20932__auto___21451);

return statearr_21283;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped.call(null,state__20934__auto__);
});})(c__20932__auto___21451,res,vec__21269,v,p,job,jobs,results))
);


cljs.core.async.put_BANG_.call(null,p,res);

return true;
}
});})(jobs,results))
;
var async = ((function (jobs,results,process){
return (function (p__21284){
var vec__21285 = p__21284;
var v = cljs.core.nth.call(null,vec__21285,(0),null);
var p = cljs.core.nth.call(null,vec__21285,(1),null);
var job = vec__21285;
if((job == null)){
cljs.core.async.close_BANG_.call(null,results);

return null;
} else {
var res = cljs.core.async.chan.call(null,(1));
xf.call(null,v,res);

cljs.core.async.put_BANG_.call(null,p,res);

return true;
}
});})(jobs,results,process))
;
var n__18958__auto___21454 = n;
var __21455 = (0);
while(true){
if((__21455 < n__18958__auto___21454)){
var G__21286_21456 = (((type instanceof cljs.core.Keyword))?type.fqn:null);
switch (G__21286_21456) {
case "compute":
var c__20932__auto___21458 = cljs.core.async.chan.call(null,(1));
cljs.core.async.impl.dispatch.run.call(null,((function (__21455,c__20932__auto___21458,G__21286_21456,n__18958__auto___21454,jobs,results,process,async){
return (function (){
var f__20933__auto__ = (function (){var switch__20870__auto__ = ((function (__21455,c__20932__auto___21458,G__21286_21456,n__18958__auto___21454,jobs,results,process,async){
return (function (state_21299){
var state_val_21300 = (state_21299[(1)]);
if((state_val_21300 === (1))){
var state_21299__$1 = state_21299;
var statearr_21301_21459 = state_21299__$1;
(statearr_21301_21459[(2)] = null);

(statearr_21301_21459[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21300 === (2))){
var state_21299__$1 = state_21299;
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_21299__$1,(4),jobs);
} else {
if((state_val_21300 === (3))){
var inst_21297 = (state_21299[(2)]);
var state_21299__$1 = state_21299;
return cljs.core.async.impl.ioc_helpers.return_chan.call(null,state_21299__$1,inst_21297);
} else {
if((state_val_21300 === (4))){
var inst_21289 = (state_21299[(2)]);
var inst_21290 = process.call(null,inst_21289);
var state_21299__$1 = state_21299;
if(cljs.core.truth_(inst_21290)){
var statearr_21302_21460 = state_21299__$1;
(statearr_21302_21460[(1)] = (5));

} else {
var statearr_21303_21461 = state_21299__$1;
(statearr_21303_21461[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21300 === (5))){
var state_21299__$1 = state_21299;
var statearr_21304_21462 = state_21299__$1;
(statearr_21304_21462[(2)] = null);

(statearr_21304_21462[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21300 === (6))){
var state_21299__$1 = state_21299;
var statearr_21305_21463 = state_21299__$1;
(statearr_21305_21463[(2)] = null);

(statearr_21305_21463[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21300 === (7))){
var inst_21295 = (state_21299[(2)]);
var state_21299__$1 = state_21299;
var statearr_21306_21464 = state_21299__$1;
(statearr_21306_21464[(2)] = inst_21295);

(statearr_21306_21464[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
return null;
}
}
}
}
}
}
}
});})(__21455,c__20932__auto___21458,G__21286_21456,n__18958__auto___21454,jobs,results,process,async))
;
return ((function (__21455,switch__20870__auto__,c__20932__auto___21458,G__21286_21456,n__18958__auto___21454,jobs,results,process,async){
return (function() {
var cljs$core$async$pipeline_STAR__$_state_machine__20871__auto__ = null;
var cljs$core$async$pipeline_STAR__$_state_machine__20871__auto____0 = (function (){
var statearr_21310 = [null,null,null,null,null,null,null];
(statearr_21310[(0)] = cljs$core$async$pipeline_STAR__$_state_machine__20871__auto__);

(statearr_21310[(1)] = (1));

return statearr_21310;
});
var cljs$core$async$pipeline_STAR__$_state_machine__20871__auto____1 = (function (state_21299){
while(true){
var ret_value__20872__auto__ = (function (){try{while(true){
var result__20873__auto__ = switch__20870__auto__.call(null,state_21299);
if(cljs.core.keyword_identical_QMARK_.call(null,result__20873__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__20873__auto__;
}
break;
}
}catch (e21311){if((e21311 instanceof Object)){
var ex__20874__auto__ = e21311;
var statearr_21312_21465 = state_21299;
(statearr_21312_21465[(5)] = ex__20874__auto__);


cljs.core.async.impl.ioc_helpers.process_exception.call(null,state_21299);

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
throw e21311;

}
}})();
if(cljs.core.keyword_identical_QMARK_.call(null,ret_value__20872__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__21466 = state_21299;
state_21299 = G__21466;
continue;
} else {
return ret_value__20872__auto__;
}
break;
}
});
cljs$core$async$pipeline_STAR__$_state_machine__20871__auto__ = function(state_21299){
switch(arguments.length){
case 0:
return cljs$core$async$pipeline_STAR__$_state_machine__20871__auto____0.call(this);
case 1:
return cljs$core$async$pipeline_STAR__$_state_machine__20871__auto____1.call(this,state_21299);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$pipeline_STAR__$_state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$pipeline_STAR__$_state_machine__20871__auto____0;
cljs$core$async$pipeline_STAR__$_state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$pipeline_STAR__$_state_machine__20871__auto____1;
return cljs$core$async$pipeline_STAR__$_state_machine__20871__auto__;
})()
;})(__21455,switch__20870__auto__,c__20932__auto___21458,G__21286_21456,n__18958__auto___21454,jobs,results,process,async))
})();
var state__20934__auto__ = (function (){var statearr_21313 = f__20933__auto__.call(null);
(statearr_21313[cljs.core.async.impl.ioc_helpers.USER_START_IDX] = c__20932__auto___21458);

return statearr_21313;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped.call(null,state__20934__auto__);
});})(__21455,c__20932__auto___21458,G__21286_21456,n__18958__auto___21454,jobs,results,process,async))
);


break;
case "async":
var c__20932__auto___21467 = cljs.core.async.chan.call(null,(1));
cljs.core.async.impl.dispatch.run.call(null,((function (__21455,c__20932__auto___21467,G__21286_21456,n__18958__auto___21454,jobs,results,process,async){
return (function (){
var f__20933__auto__ = (function (){var switch__20870__auto__ = ((function (__21455,c__20932__auto___21467,G__21286_21456,n__18958__auto___21454,jobs,results,process,async){
return (function (state_21326){
var state_val_21327 = (state_21326[(1)]);
if((state_val_21327 === (1))){
var state_21326__$1 = state_21326;
var statearr_21328_21468 = state_21326__$1;
(statearr_21328_21468[(2)] = null);

(statearr_21328_21468[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21327 === (2))){
var state_21326__$1 = state_21326;
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_21326__$1,(4),jobs);
} else {
if((state_val_21327 === (3))){
var inst_21324 = (state_21326[(2)]);
var state_21326__$1 = state_21326;
return cljs.core.async.impl.ioc_helpers.return_chan.call(null,state_21326__$1,inst_21324);
} else {
if((state_val_21327 === (4))){
var inst_21316 = (state_21326[(2)]);
var inst_21317 = async.call(null,inst_21316);
var state_21326__$1 = state_21326;
if(cljs.core.truth_(inst_21317)){
var statearr_21329_21469 = state_21326__$1;
(statearr_21329_21469[(1)] = (5));

} else {
var statearr_21330_21470 = state_21326__$1;
(statearr_21330_21470[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21327 === (5))){
var state_21326__$1 = state_21326;
var statearr_21331_21471 = state_21326__$1;
(statearr_21331_21471[(2)] = null);

(statearr_21331_21471[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21327 === (6))){
var state_21326__$1 = state_21326;
var statearr_21332_21472 = state_21326__$1;
(statearr_21332_21472[(2)] = null);

(statearr_21332_21472[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21327 === (7))){
var inst_21322 = (state_21326[(2)]);
var state_21326__$1 = state_21326;
var statearr_21333_21473 = state_21326__$1;
(statearr_21333_21473[(2)] = inst_21322);

(statearr_21333_21473[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
return null;
}
}
}
}
}
}
}
});})(__21455,c__20932__auto___21467,G__21286_21456,n__18958__auto___21454,jobs,results,process,async))
;
return ((function (__21455,switch__20870__auto__,c__20932__auto___21467,G__21286_21456,n__18958__auto___21454,jobs,results,process,async){
return (function() {
var cljs$core$async$pipeline_STAR__$_state_machine__20871__auto__ = null;
var cljs$core$async$pipeline_STAR__$_state_machine__20871__auto____0 = (function (){
var statearr_21337 = [null,null,null,null,null,null,null];
(statearr_21337[(0)] = cljs$core$async$pipeline_STAR__$_state_machine__20871__auto__);

(statearr_21337[(1)] = (1));

return statearr_21337;
});
var cljs$core$async$pipeline_STAR__$_state_machine__20871__auto____1 = (function (state_21326){
while(true){
var ret_value__20872__auto__ = (function (){try{while(true){
var result__20873__auto__ = switch__20870__auto__.call(null,state_21326);
if(cljs.core.keyword_identical_QMARK_.call(null,result__20873__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__20873__auto__;
}
break;
}
}catch (e21338){if((e21338 instanceof Object)){
var ex__20874__auto__ = e21338;
var statearr_21339_21474 = state_21326;
(statearr_21339_21474[(5)] = ex__20874__auto__);


cljs.core.async.impl.ioc_helpers.process_exception.call(null,state_21326);

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
throw e21338;

}
}})();
if(cljs.core.keyword_identical_QMARK_.call(null,ret_value__20872__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__21475 = state_21326;
state_21326 = G__21475;
continue;
} else {
return ret_value__20872__auto__;
}
break;
}
});
cljs$core$async$pipeline_STAR__$_state_machine__20871__auto__ = function(state_21326){
switch(arguments.length){
case 0:
return cljs$core$async$pipeline_STAR__$_state_machine__20871__auto____0.call(this);
case 1:
return cljs$core$async$pipeline_STAR__$_state_machine__20871__auto____1.call(this,state_21326);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$pipeline_STAR__$_state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$pipeline_STAR__$_state_machine__20871__auto____0;
cljs$core$async$pipeline_STAR__$_state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$pipeline_STAR__$_state_machine__20871__auto____1;
return cljs$core$async$pipeline_STAR__$_state_machine__20871__auto__;
})()
;})(__21455,switch__20870__auto__,c__20932__auto___21467,G__21286_21456,n__18958__auto___21454,jobs,results,process,async))
})();
var state__20934__auto__ = (function (){var statearr_21340 = f__20933__auto__.call(null);
(statearr_21340[cljs.core.async.impl.ioc_helpers.USER_START_IDX] = c__20932__auto___21467);

return statearr_21340;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped.call(null,state__20934__auto__);
});})(__21455,c__20932__auto___21467,G__21286_21456,n__18958__auto___21454,jobs,results,process,async))
);


break;
default:
throw (new Error([cljs.core.str("No matching clause: "),cljs.core.str(type)].join('')));

}

var G__21476 = (__21455 + (1));
__21455 = G__21476;
continue;
} else {
}
break;
}

var c__20932__auto___21477 = cljs.core.async.chan.call(null,(1));
cljs.core.async.impl.dispatch.run.call(null,((function (c__20932__auto___21477,jobs,results,process,async){
return (function (){
var f__20933__auto__ = (function (){var switch__20870__auto__ = ((function (c__20932__auto___21477,jobs,results,process,async){
return (function (state_21362){
var state_val_21363 = (state_21362[(1)]);
if((state_val_21363 === (1))){
var state_21362__$1 = state_21362;
var statearr_21364_21478 = state_21362__$1;
(statearr_21364_21478[(2)] = null);

(statearr_21364_21478[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21363 === (2))){
var state_21362__$1 = state_21362;
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_21362__$1,(4),from);
} else {
if((state_val_21363 === (3))){
var inst_21360 = (state_21362[(2)]);
var state_21362__$1 = state_21362;
return cljs.core.async.impl.ioc_helpers.return_chan.call(null,state_21362__$1,inst_21360);
} else {
if((state_val_21363 === (4))){
var inst_21343 = (state_21362[(7)]);
var inst_21343__$1 = (state_21362[(2)]);
var inst_21344 = (inst_21343__$1 == null);
var state_21362__$1 = (function (){var statearr_21365 = state_21362;
(statearr_21365[(7)] = inst_21343__$1);

return statearr_21365;
})();
if(cljs.core.truth_(inst_21344)){
var statearr_21366_21479 = state_21362__$1;
(statearr_21366_21479[(1)] = (5));

} else {
var statearr_21367_21480 = state_21362__$1;
(statearr_21367_21480[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21363 === (5))){
var inst_21346 = cljs.core.async.close_BANG_.call(null,jobs);
var state_21362__$1 = state_21362;
var statearr_21368_21481 = state_21362__$1;
(statearr_21368_21481[(2)] = inst_21346);

(statearr_21368_21481[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21363 === (6))){
var inst_21343 = (state_21362[(7)]);
var inst_21348 = (state_21362[(8)]);
var inst_21348__$1 = cljs.core.async.chan.call(null,(1));
var inst_21349 = cljs.core.PersistentVector.EMPTY_NODE;
var inst_21350 = [inst_21343,inst_21348__$1];
var inst_21351 = (new cljs.core.PersistentVector(null,2,(5),inst_21349,inst_21350,null));
var state_21362__$1 = (function (){var statearr_21369 = state_21362;
(statearr_21369[(8)] = inst_21348__$1);

return statearr_21369;
})();
return cljs.core.async.impl.ioc_helpers.put_BANG_.call(null,state_21362__$1,(8),jobs,inst_21351);
} else {
if((state_val_21363 === (7))){
var inst_21358 = (state_21362[(2)]);
var state_21362__$1 = state_21362;
var statearr_21370_21482 = state_21362__$1;
(statearr_21370_21482[(2)] = inst_21358);

(statearr_21370_21482[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21363 === (8))){
var inst_21348 = (state_21362[(8)]);
var inst_21353 = (state_21362[(2)]);
var state_21362__$1 = (function (){var statearr_21371 = state_21362;
(statearr_21371[(9)] = inst_21353);

return statearr_21371;
})();
return cljs.core.async.impl.ioc_helpers.put_BANG_.call(null,state_21362__$1,(9),results,inst_21348);
} else {
if((state_val_21363 === (9))){
var inst_21355 = (state_21362[(2)]);
var state_21362__$1 = (function (){var statearr_21372 = state_21362;
(statearr_21372[(10)] = inst_21355);

return statearr_21372;
})();
var statearr_21373_21483 = state_21362__$1;
(statearr_21373_21483[(2)] = null);

(statearr_21373_21483[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
return null;
}
}
}
}
}
}
}
}
}
});})(c__20932__auto___21477,jobs,results,process,async))
;
return ((function (switch__20870__auto__,c__20932__auto___21477,jobs,results,process,async){
return (function() {
var cljs$core$async$pipeline_STAR__$_state_machine__20871__auto__ = null;
var cljs$core$async$pipeline_STAR__$_state_machine__20871__auto____0 = (function (){
var statearr_21377 = [null,null,null,null,null,null,null,null,null,null,null];
(statearr_21377[(0)] = cljs$core$async$pipeline_STAR__$_state_machine__20871__auto__);

(statearr_21377[(1)] = (1));

return statearr_21377;
});
var cljs$core$async$pipeline_STAR__$_state_machine__20871__auto____1 = (function (state_21362){
while(true){
var ret_value__20872__auto__ = (function (){try{while(true){
var result__20873__auto__ = switch__20870__auto__.call(null,state_21362);
if(cljs.core.keyword_identical_QMARK_.call(null,result__20873__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__20873__auto__;
}
break;
}
}catch (e21378){if((e21378 instanceof Object)){
var ex__20874__auto__ = e21378;
var statearr_21379_21484 = state_21362;
(statearr_21379_21484[(5)] = ex__20874__auto__);


cljs.core.async.impl.ioc_helpers.process_exception.call(null,state_21362);

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
throw e21378;

}
}})();
if(cljs.core.keyword_identical_QMARK_.call(null,ret_value__20872__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__21485 = state_21362;
state_21362 = G__21485;
continue;
} else {
return ret_value__20872__auto__;
}
break;
}
});
cljs$core$async$pipeline_STAR__$_state_machine__20871__auto__ = function(state_21362){
switch(arguments.length){
case 0:
return cljs$core$async$pipeline_STAR__$_state_machine__20871__auto____0.call(this);
case 1:
return cljs$core$async$pipeline_STAR__$_state_machine__20871__auto____1.call(this,state_21362);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$pipeline_STAR__$_state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$pipeline_STAR__$_state_machine__20871__auto____0;
cljs$core$async$pipeline_STAR__$_state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$pipeline_STAR__$_state_machine__20871__auto____1;
return cljs$core$async$pipeline_STAR__$_state_machine__20871__auto__;
})()
;})(switch__20870__auto__,c__20932__auto___21477,jobs,results,process,async))
})();
var state__20934__auto__ = (function (){var statearr_21380 = f__20933__auto__.call(null);
(statearr_21380[cljs.core.async.impl.ioc_helpers.USER_START_IDX] = c__20932__auto___21477);

return statearr_21380;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped.call(null,state__20934__auto__);
});})(c__20932__auto___21477,jobs,results,process,async))
);


var c__20932__auto__ = cljs.core.async.chan.call(null,(1));
cljs.core.async.impl.dispatch.run.call(null,((function (c__20932__auto__,jobs,results,process,async){
return (function (){
var f__20933__auto__ = (function (){var switch__20870__auto__ = ((function (c__20932__auto__,jobs,results,process,async){
return (function (state_21418){
var state_val_21419 = (state_21418[(1)]);
if((state_val_21419 === (7))){
var inst_21414 = (state_21418[(2)]);
var state_21418__$1 = state_21418;
var statearr_21420_21486 = state_21418__$1;
(statearr_21420_21486[(2)] = inst_21414);

(statearr_21420_21486[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21419 === (20))){
var state_21418__$1 = state_21418;
var statearr_21421_21487 = state_21418__$1;
(statearr_21421_21487[(2)] = null);

(statearr_21421_21487[(1)] = (21));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21419 === (1))){
var state_21418__$1 = state_21418;
var statearr_21422_21488 = state_21418__$1;
(statearr_21422_21488[(2)] = null);

(statearr_21422_21488[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21419 === (4))){
var inst_21383 = (state_21418[(7)]);
var inst_21383__$1 = (state_21418[(2)]);
var inst_21384 = (inst_21383__$1 == null);
var state_21418__$1 = (function (){var statearr_21423 = state_21418;
(statearr_21423[(7)] = inst_21383__$1);

return statearr_21423;
})();
if(cljs.core.truth_(inst_21384)){
var statearr_21424_21489 = state_21418__$1;
(statearr_21424_21489[(1)] = (5));

} else {
var statearr_21425_21490 = state_21418__$1;
(statearr_21425_21490[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21419 === (15))){
var inst_21396 = (state_21418[(8)]);
var state_21418__$1 = state_21418;
return cljs.core.async.impl.ioc_helpers.put_BANG_.call(null,state_21418__$1,(18),to,inst_21396);
} else {
if((state_val_21419 === (21))){
var inst_21409 = (state_21418[(2)]);
var state_21418__$1 = state_21418;
var statearr_21426_21491 = state_21418__$1;
(statearr_21426_21491[(2)] = inst_21409);

(statearr_21426_21491[(1)] = (13));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21419 === (13))){
var inst_21411 = (state_21418[(2)]);
var state_21418__$1 = (function (){var statearr_21427 = state_21418;
(statearr_21427[(9)] = inst_21411);

return statearr_21427;
})();
var statearr_21428_21492 = state_21418__$1;
(statearr_21428_21492[(2)] = null);

(statearr_21428_21492[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21419 === (6))){
var inst_21383 = (state_21418[(7)]);
var state_21418__$1 = state_21418;
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_21418__$1,(11),inst_21383);
} else {
if((state_val_21419 === (17))){
var inst_21404 = (state_21418[(2)]);
var state_21418__$1 = state_21418;
if(cljs.core.truth_(inst_21404)){
var statearr_21429_21493 = state_21418__$1;
(statearr_21429_21493[(1)] = (19));

} else {
var statearr_21430_21494 = state_21418__$1;
(statearr_21430_21494[(1)] = (20));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21419 === (3))){
var inst_21416 = (state_21418[(2)]);
var state_21418__$1 = state_21418;
return cljs.core.async.impl.ioc_helpers.return_chan.call(null,state_21418__$1,inst_21416);
} else {
if((state_val_21419 === (12))){
var inst_21393 = (state_21418[(10)]);
var state_21418__$1 = state_21418;
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_21418__$1,(14),inst_21393);
} else {
if((state_val_21419 === (2))){
var state_21418__$1 = state_21418;
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_21418__$1,(4),results);
} else {
if((state_val_21419 === (19))){
var state_21418__$1 = state_21418;
var statearr_21431_21495 = state_21418__$1;
(statearr_21431_21495[(2)] = null);

(statearr_21431_21495[(1)] = (12));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21419 === (11))){
var inst_21393 = (state_21418[(2)]);
var state_21418__$1 = (function (){var statearr_21432 = state_21418;
(statearr_21432[(10)] = inst_21393);

return statearr_21432;
})();
var statearr_21433_21496 = state_21418__$1;
(statearr_21433_21496[(2)] = null);

(statearr_21433_21496[(1)] = (12));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21419 === (9))){
var state_21418__$1 = state_21418;
var statearr_21434_21497 = state_21418__$1;
(statearr_21434_21497[(2)] = null);

(statearr_21434_21497[(1)] = (10));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21419 === (5))){
var state_21418__$1 = state_21418;
if(cljs.core.truth_(close_QMARK_)){
var statearr_21435_21498 = state_21418__$1;
(statearr_21435_21498[(1)] = (8));

} else {
var statearr_21436_21499 = state_21418__$1;
(statearr_21436_21499[(1)] = (9));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21419 === (14))){
var inst_21398 = (state_21418[(11)]);
var inst_21396 = (state_21418[(8)]);
var inst_21396__$1 = (state_21418[(2)]);
var inst_21397 = (inst_21396__$1 == null);
var inst_21398__$1 = cljs.core.not.call(null,inst_21397);
var state_21418__$1 = (function (){var statearr_21437 = state_21418;
(statearr_21437[(11)] = inst_21398__$1);

(statearr_21437[(8)] = inst_21396__$1);

return statearr_21437;
})();
if(inst_21398__$1){
var statearr_21438_21500 = state_21418__$1;
(statearr_21438_21500[(1)] = (15));

} else {
var statearr_21439_21501 = state_21418__$1;
(statearr_21439_21501[(1)] = (16));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21419 === (16))){
var inst_21398 = (state_21418[(11)]);
var state_21418__$1 = state_21418;
var statearr_21440_21502 = state_21418__$1;
(statearr_21440_21502[(2)] = inst_21398);

(statearr_21440_21502[(1)] = (17));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21419 === (10))){
var inst_21390 = (state_21418[(2)]);
var state_21418__$1 = state_21418;
var statearr_21441_21503 = state_21418__$1;
(statearr_21441_21503[(2)] = inst_21390);

(statearr_21441_21503[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21419 === (18))){
var inst_21401 = (state_21418[(2)]);
var state_21418__$1 = state_21418;
var statearr_21442_21504 = state_21418__$1;
(statearr_21442_21504[(2)] = inst_21401);

(statearr_21442_21504[(1)] = (17));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21419 === (8))){
var inst_21387 = cljs.core.async.close_BANG_.call(null,to);
var state_21418__$1 = state_21418;
var statearr_21443_21505 = state_21418__$1;
(statearr_21443_21505[(2)] = inst_21387);

(statearr_21443_21505[(1)] = (10));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
return null;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
});})(c__20932__auto__,jobs,results,process,async))
;
return ((function (switch__20870__auto__,c__20932__auto__,jobs,results,process,async){
return (function() {
var cljs$core$async$pipeline_STAR__$_state_machine__20871__auto__ = null;
var cljs$core$async$pipeline_STAR__$_state_machine__20871__auto____0 = (function (){
var statearr_21447 = [null,null,null,null,null,null,null,null,null,null,null,null];
(statearr_21447[(0)] = cljs$core$async$pipeline_STAR__$_state_machine__20871__auto__);

(statearr_21447[(1)] = (1));

return statearr_21447;
});
var cljs$core$async$pipeline_STAR__$_state_machine__20871__auto____1 = (function (state_21418){
while(true){
var ret_value__20872__auto__ = (function (){try{while(true){
var result__20873__auto__ = switch__20870__auto__.call(null,state_21418);
if(cljs.core.keyword_identical_QMARK_.call(null,result__20873__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__20873__auto__;
}
break;
}
}catch (e21448){if((e21448 instanceof Object)){
var ex__20874__auto__ = e21448;
var statearr_21449_21506 = state_21418;
(statearr_21449_21506[(5)] = ex__20874__auto__);


cljs.core.async.impl.ioc_helpers.process_exception.call(null,state_21418);

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
throw e21448;

}
}})();
if(cljs.core.keyword_identical_QMARK_.call(null,ret_value__20872__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__21507 = state_21418;
state_21418 = G__21507;
continue;
} else {
return ret_value__20872__auto__;
}
break;
}
});
cljs$core$async$pipeline_STAR__$_state_machine__20871__auto__ = function(state_21418){
switch(arguments.length){
case 0:
return cljs$core$async$pipeline_STAR__$_state_machine__20871__auto____0.call(this);
case 1:
return cljs$core$async$pipeline_STAR__$_state_machine__20871__auto____1.call(this,state_21418);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$pipeline_STAR__$_state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$pipeline_STAR__$_state_machine__20871__auto____0;
cljs$core$async$pipeline_STAR__$_state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$pipeline_STAR__$_state_machine__20871__auto____1;
return cljs$core$async$pipeline_STAR__$_state_machine__20871__auto__;
})()
;})(switch__20870__auto__,c__20932__auto__,jobs,results,process,async))
})();
var state__20934__auto__ = (function (){var statearr_21450 = f__20933__auto__.call(null);
(statearr_21450[cljs.core.async.impl.ioc_helpers.USER_START_IDX] = c__20932__auto__);

return statearr_21450;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped.call(null,state__20934__auto__);
});})(c__20932__auto__,jobs,results,process,async))
);

return c__20932__auto__;
});
/**
 * Takes elements from the from channel and supplies them to the to
 * channel, subject to the async function af, with parallelism n. af
 * must be a function of two arguments, the first an input value and
 * the second a channel on which to place the result(s). af must close!
 * the channel before returning.  The presumption is that af will
 * return immediately, having launched some asynchronous operation
 * whose completion/callback will manipulate the result channel. Outputs
 * will be returned in order relative to  the inputs. By default, the to
 * channel will be closed when the from channel closes, but can be
 * determined by the close?  parameter. Will stop consuming the from
 * channel if the to channel closes.
 */
cljs.core.async.pipeline_async = (function cljs$core$async$pipeline_async(){
var G__21509 = arguments.length;
switch (G__21509) {
case 4:
return cljs.core.async.pipeline_async.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
case 5:
return cljs.core.async.pipeline_async.cljs$core$IFn$_invoke$arity$5((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

cljs.core.async.pipeline_async.cljs$core$IFn$_invoke$arity$4 = (function (n,to,af,from){
return cljs.core.async.pipeline_async.call(null,n,to,af,from,true);
});

cljs.core.async.pipeline_async.cljs$core$IFn$_invoke$arity$5 = (function (n,to,af,from,close_QMARK_){
return cljs.core.async.pipeline_STAR_.call(null,n,to,af,from,close_QMARK_,null,new cljs.core.Keyword(null,"async","async",1050769601));
});

cljs.core.async.pipeline_async.cljs$lang$maxFixedArity = 5;
/**
 * Takes elements from the from channel and supplies them to the to
 * channel, subject to the transducer xf, with parallelism n. Because
 * it is parallel, the transducer will be applied independently to each
 * element, not across elements, and may produce zero or more outputs
 * per input.  Outputs will be returned in order relative to the
 * inputs. By default, the to channel will be closed when the from
 * channel closes, but can be determined by the close?  parameter. Will
 * stop consuming the from channel if the to channel closes.
 * 
 * Note this is supplied for API compatibility with the Clojure version.
 * Values of N > 1 will not result in actual concurrency in a
 * single-threaded runtime.
 */
cljs.core.async.pipeline = (function cljs$core$async$pipeline(){
var G__21512 = arguments.length;
switch (G__21512) {
case 4:
return cljs.core.async.pipeline.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
case 5:
return cljs.core.async.pipeline.cljs$core$IFn$_invoke$arity$5((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]));

break;
case 6:
return cljs.core.async.pipeline.cljs$core$IFn$_invoke$arity$6((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]),(arguments[(4)]),(arguments[(5)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

cljs.core.async.pipeline.cljs$core$IFn$_invoke$arity$4 = (function (n,to,xf,from){
return cljs.core.async.pipeline.call(null,n,to,xf,from,true);
});

cljs.core.async.pipeline.cljs$core$IFn$_invoke$arity$5 = (function (n,to,xf,from,close_QMARK_){
return cljs.core.async.pipeline.call(null,n,to,xf,from,close_QMARK_,null);
});

cljs.core.async.pipeline.cljs$core$IFn$_invoke$arity$6 = (function (n,to,xf,from,close_QMARK_,ex_handler){
return cljs.core.async.pipeline_STAR_.call(null,n,to,xf,from,close_QMARK_,ex_handler,new cljs.core.Keyword(null,"compute","compute",1555393130));
});

cljs.core.async.pipeline.cljs$lang$maxFixedArity = 6;
/**
 * Takes a predicate and a source channel and returns a vector of two
 * channels, the first of which will contain the values for which the
 * predicate returned true, the second those for which it returned
 * false.
 * 
 * The out channels will be unbuffered by default, or two buf-or-ns can
 * be supplied. The channels will close after the source channel has
 * closed.
 */
cljs.core.async.split = (function cljs$core$async$split(){
var G__21515 = arguments.length;
switch (G__21515) {
case 2:
return cljs.core.async.split.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 4:
return cljs.core.async.split.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

cljs.core.async.split.cljs$core$IFn$_invoke$arity$2 = (function (p,ch){
return cljs.core.async.split.call(null,p,ch,null,null);
});

cljs.core.async.split.cljs$core$IFn$_invoke$arity$4 = (function (p,ch,t_buf_or_n,f_buf_or_n){
var tc = cljs.core.async.chan.call(null,t_buf_or_n);
var fc = cljs.core.async.chan.call(null,f_buf_or_n);
var c__20932__auto___21567 = cljs.core.async.chan.call(null,(1));
cljs.core.async.impl.dispatch.run.call(null,((function (c__20932__auto___21567,tc,fc){
return (function (){
var f__20933__auto__ = (function (){var switch__20870__auto__ = ((function (c__20932__auto___21567,tc,fc){
return (function (state_21541){
var state_val_21542 = (state_21541[(1)]);
if((state_val_21542 === (7))){
var inst_21537 = (state_21541[(2)]);
var state_21541__$1 = state_21541;
var statearr_21543_21568 = state_21541__$1;
(statearr_21543_21568[(2)] = inst_21537);

(statearr_21543_21568[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21542 === (1))){
var state_21541__$1 = state_21541;
var statearr_21544_21569 = state_21541__$1;
(statearr_21544_21569[(2)] = null);

(statearr_21544_21569[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21542 === (4))){
var inst_21518 = (state_21541[(7)]);
var inst_21518__$1 = (state_21541[(2)]);
var inst_21519 = (inst_21518__$1 == null);
var state_21541__$1 = (function (){var statearr_21545 = state_21541;
(statearr_21545[(7)] = inst_21518__$1);

return statearr_21545;
})();
if(cljs.core.truth_(inst_21519)){
var statearr_21546_21570 = state_21541__$1;
(statearr_21546_21570[(1)] = (5));

} else {
var statearr_21547_21571 = state_21541__$1;
(statearr_21547_21571[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21542 === (13))){
var state_21541__$1 = state_21541;
var statearr_21548_21572 = state_21541__$1;
(statearr_21548_21572[(2)] = null);

(statearr_21548_21572[(1)] = (14));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21542 === (6))){
var inst_21518 = (state_21541[(7)]);
var inst_21524 = p.call(null,inst_21518);
var state_21541__$1 = state_21541;
if(cljs.core.truth_(inst_21524)){
var statearr_21549_21573 = state_21541__$1;
(statearr_21549_21573[(1)] = (9));

} else {
var statearr_21550_21574 = state_21541__$1;
(statearr_21550_21574[(1)] = (10));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21542 === (3))){
var inst_21539 = (state_21541[(2)]);
var state_21541__$1 = state_21541;
return cljs.core.async.impl.ioc_helpers.return_chan.call(null,state_21541__$1,inst_21539);
} else {
if((state_val_21542 === (12))){
var state_21541__$1 = state_21541;
var statearr_21551_21575 = state_21541__$1;
(statearr_21551_21575[(2)] = null);

(statearr_21551_21575[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21542 === (2))){
var state_21541__$1 = state_21541;
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_21541__$1,(4),ch);
} else {
if((state_val_21542 === (11))){
var inst_21518 = (state_21541[(7)]);
var inst_21528 = (state_21541[(2)]);
var state_21541__$1 = state_21541;
return cljs.core.async.impl.ioc_helpers.put_BANG_.call(null,state_21541__$1,(8),inst_21528,inst_21518);
} else {
if((state_val_21542 === (9))){
var state_21541__$1 = state_21541;
var statearr_21552_21576 = state_21541__$1;
(statearr_21552_21576[(2)] = tc);

(statearr_21552_21576[(1)] = (11));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21542 === (5))){
var inst_21521 = cljs.core.async.close_BANG_.call(null,tc);
var inst_21522 = cljs.core.async.close_BANG_.call(null,fc);
var state_21541__$1 = (function (){var statearr_21553 = state_21541;
(statearr_21553[(8)] = inst_21521);

return statearr_21553;
})();
var statearr_21554_21577 = state_21541__$1;
(statearr_21554_21577[(2)] = inst_21522);

(statearr_21554_21577[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21542 === (14))){
var inst_21535 = (state_21541[(2)]);
var state_21541__$1 = state_21541;
var statearr_21555_21578 = state_21541__$1;
(statearr_21555_21578[(2)] = inst_21535);

(statearr_21555_21578[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21542 === (10))){
var state_21541__$1 = state_21541;
var statearr_21556_21579 = state_21541__$1;
(statearr_21556_21579[(2)] = fc);

(statearr_21556_21579[(1)] = (11));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21542 === (8))){
var inst_21530 = (state_21541[(2)]);
var state_21541__$1 = state_21541;
if(cljs.core.truth_(inst_21530)){
var statearr_21557_21580 = state_21541__$1;
(statearr_21557_21580[(1)] = (12));

} else {
var statearr_21558_21581 = state_21541__$1;
(statearr_21558_21581[(1)] = (13));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
return null;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
});})(c__20932__auto___21567,tc,fc))
;
return ((function (switch__20870__auto__,c__20932__auto___21567,tc,fc){
return (function() {
var cljs$core$async$state_machine__20871__auto__ = null;
var cljs$core$async$state_machine__20871__auto____0 = (function (){
var statearr_21562 = [null,null,null,null,null,null,null,null,null];
(statearr_21562[(0)] = cljs$core$async$state_machine__20871__auto__);

(statearr_21562[(1)] = (1));

return statearr_21562;
});
var cljs$core$async$state_machine__20871__auto____1 = (function (state_21541){
while(true){
var ret_value__20872__auto__ = (function (){try{while(true){
var result__20873__auto__ = switch__20870__auto__.call(null,state_21541);
if(cljs.core.keyword_identical_QMARK_.call(null,result__20873__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__20873__auto__;
}
break;
}
}catch (e21563){if((e21563 instanceof Object)){
var ex__20874__auto__ = e21563;
var statearr_21564_21582 = state_21541;
(statearr_21564_21582[(5)] = ex__20874__auto__);


cljs.core.async.impl.ioc_helpers.process_exception.call(null,state_21541);

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
throw e21563;

}
}})();
if(cljs.core.keyword_identical_QMARK_.call(null,ret_value__20872__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__21583 = state_21541;
state_21541 = G__21583;
continue;
} else {
return ret_value__20872__auto__;
}
break;
}
});
cljs$core$async$state_machine__20871__auto__ = function(state_21541){
switch(arguments.length){
case 0:
return cljs$core$async$state_machine__20871__auto____0.call(this);
case 1:
return cljs$core$async$state_machine__20871__auto____1.call(this,state_21541);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$state_machine__20871__auto____0;
cljs$core$async$state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$state_machine__20871__auto____1;
return cljs$core$async$state_machine__20871__auto__;
})()
;})(switch__20870__auto__,c__20932__auto___21567,tc,fc))
})();
var state__20934__auto__ = (function (){var statearr_21565 = f__20933__auto__.call(null);
(statearr_21565[cljs.core.async.impl.ioc_helpers.USER_START_IDX] = c__20932__auto___21567);

return statearr_21565;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped.call(null,state__20934__auto__);
});})(c__20932__auto___21567,tc,fc))
);


return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [tc,fc], null);
});

cljs.core.async.split.cljs$lang$maxFixedArity = 4;
/**
 * f should be a function of 2 arguments. Returns a channel containing
 * the single result of applying f to init and the first item from the
 * channel, then applying f to that result and the 2nd item, etc. If
 * the channel closes without yielding items, returns init and f is not
 * called. ch must close before reduce produces a result.
 */
cljs.core.async.reduce = (function cljs$core$async$reduce(f,init,ch){
var c__20932__auto__ = cljs.core.async.chan.call(null,(1));
cljs.core.async.impl.dispatch.run.call(null,((function (c__20932__auto__){
return (function (){
var f__20933__auto__ = (function (){var switch__20870__auto__ = ((function (c__20932__auto__){
return (function (state_21630){
var state_val_21631 = (state_21630[(1)]);
if((state_val_21631 === (1))){
var inst_21616 = init;
var state_21630__$1 = (function (){var statearr_21632 = state_21630;
(statearr_21632[(7)] = inst_21616);

return statearr_21632;
})();
var statearr_21633_21648 = state_21630__$1;
(statearr_21633_21648[(2)] = null);

(statearr_21633_21648[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21631 === (2))){
var state_21630__$1 = state_21630;
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_21630__$1,(4),ch);
} else {
if((state_val_21631 === (3))){
var inst_21628 = (state_21630[(2)]);
var state_21630__$1 = state_21630;
return cljs.core.async.impl.ioc_helpers.return_chan.call(null,state_21630__$1,inst_21628);
} else {
if((state_val_21631 === (4))){
var inst_21619 = (state_21630[(8)]);
var inst_21619__$1 = (state_21630[(2)]);
var inst_21620 = (inst_21619__$1 == null);
var state_21630__$1 = (function (){var statearr_21634 = state_21630;
(statearr_21634[(8)] = inst_21619__$1);

return statearr_21634;
})();
if(cljs.core.truth_(inst_21620)){
var statearr_21635_21649 = state_21630__$1;
(statearr_21635_21649[(1)] = (5));

} else {
var statearr_21636_21650 = state_21630__$1;
(statearr_21636_21650[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21631 === (5))){
var inst_21616 = (state_21630[(7)]);
var state_21630__$1 = state_21630;
var statearr_21637_21651 = state_21630__$1;
(statearr_21637_21651[(2)] = inst_21616);

(statearr_21637_21651[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21631 === (6))){
var inst_21616 = (state_21630[(7)]);
var inst_21619 = (state_21630[(8)]);
var inst_21623 = f.call(null,inst_21616,inst_21619);
var inst_21616__$1 = inst_21623;
var state_21630__$1 = (function (){var statearr_21638 = state_21630;
(statearr_21638[(7)] = inst_21616__$1);

return statearr_21638;
})();
var statearr_21639_21652 = state_21630__$1;
(statearr_21639_21652[(2)] = null);

(statearr_21639_21652[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21631 === (7))){
var inst_21626 = (state_21630[(2)]);
var state_21630__$1 = state_21630;
var statearr_21640_21653 = state_21630__$1;
(statearr_21640_21653[(2)] = inst_21626);

(statearr_21640_21653[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
return null;
}
}
}
}
}
}
}
});})(c__20932__auto__))
;
return ((function (switch__20870__auto__,c__20932__auto__){
return (function() {
var cljs$core$async$reduce_$_state_machine__20871__auto__ = null;
var cljs$core$async$reduce_$_state_machine__20871__auto____0 = (function (){
var statearr_21644 = [null,null,null,null,null,null,null,null,null];
(statearr_21644[(0)] = cljs$core$async$reduce_$_state_machine__20871__auto__);

(statearr_21644[(1)] = (1));

return statearr_21644;
});
var cljs$core$async$reduce_$_state_machine__20871__auto____1 = (function (state_21630){
while(true){
var ret_value__20872__auto__ = (function (){try{while(true){
var result__20873__auto__ = switch__20870__auto__.call(null,state_21630);
if(cljs.core.keyword_identical_QMARK_.call(null,result__20873__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__20873__auto__;
}
break;
}
}catch (e21645){if((e21645 instanceof Object)){
var ex__20874__auto__ = e21645;
var statearr_21646_21654 = state_21630;
(statearr_21646_21654[(5)] = ex__20874__auto__);


cljs.core.async.impl.ioc_helpers.process_exception.call(null,state_21630);

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
throw e21645;

}
}})();
if(cljs.core.keyword_identical_QMARK_.call(null,ret_value__20872__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__21655 = state_21630;
state_21630 = G__21655;
continue;
} else {
return ret_value__20872__auto__;
}
break;
}
});
cljs$core$async$reduce_$_state_machine__20871__auto__ = function(state_21630){
switch(arguments.length){
case 0:
return cljs$core$async$reduce_$_state_machine__20871__auto____0.call(this);
case 1:
return cljs$core$async$reduce_$_state_machine__20871__auto____1.call(this,state_21630);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$reduce_$_state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$reduce_$_state_machine__20871__auto____0;
cljs$core$async$reduce_$_state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$reduce_$_state_machine__20871__auto____1;
return cljs$core$async$reduce_$_state_machine__20871__auto__;
})()
;})(switch__20870__auto__,c__20932__auto__))
})();
var state__20934__auto__ = (function (){var statearr_21647 = f__20933__auto__.call(null);
(statearr_21647[cljs.core.async.impl.ioc_helpers.USER_START_IDX] = c__20932__auto__);

return statearr_21647;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped.call(null,state__20934__auto__);
});})(c__20932__auto__))
);

return c__20932__auto__;
});
/**
 * Puts the contents of coll into the supplied channel.
 * 
 * By default the channel will be closed after the items are copied,
 * but can be determined by the close? parameter.
 * 
 * Returns a channel which will close after the items are copied.
 */
cljs.core.async.onto_chan = (function cljs$core$async$onto_chan(){
var G__21657 = arguments.length;
switch (G__21657) {
case 2:
return cljs.core.async.onto_chan.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return cljs.core.async.onto_chan.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

cljs.core.async.onto_chan.cljs$core$IFn$_invoke$arity$2 = (function (ch,coll){
return cljs.core.async.onto_chan.call(null,ch,coll,true);
});

cljs.core.async.onto_chan.cljs$core$IFn$_invoke$arity$3 = (function (ch,coll,close_QMARK_){
var c__20932__auto__ = cljs.core.async.chan.call(null,(1));
cljs.core.async.impl.dispatch.run.call(null,((function (c__20932__auto__){
return (function (){
var f__20933__auto__ = (function (){var switch__20870__auto__ = ((function (c__20932__auto__){
return (function (state_21682){
var state_val_21683 = (state_21682[(1)]);
if((state_val_21683 === (7))){
var inst_21664 = (state_21682[(2)]);
var state_21682__$1 = state_21682;
var statearr_21684_21708 = state_21682__$1;
(statearr_21684_21708[(2)] = inst_21664);

(statearr_21684_21708[(1)] = (6));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21683 === (1))){
var inst_21658 = cljs.core.seq.call(null,coll);
var inst_21659 = inst_21658;
var state_21682__$1 = (function (){var statearr_21685 = state_21682;
(statearr_21685[(7)] = inst_21659);

return statearr_21685;
})();
var statearr_21686_21709 = state_21682__$1;
(statearr_21686_21709[(2)] = null);

(statearr_21686_21709[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21683 === (4))){
var inst_21659 = (state_21682[(7)]);
var inst_21662 = cljs.core.first.call(null,inst_21659);
var state_21682__$1 = state_21682;
return cljs.core.async.impl.ioc_helpers.put_BANG_.call(null,state_21682__$1,(7),ch,inst_21662);
} else {
if((state_val_21683 === (13))){
var inst_21676 = (state_21682[(2)]);
var state_21682__$1 = state_21682;
var statearr_21687_21710 = state_21682__$1;
(statearr_21687_21710[(2)] = inst_21676);

(statearr_21687_21710[(1)] = (10));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21683 === (6))){
var inst_21667 = (state_21682[(2)]);
var state_21682__$1 = state_21682;
if(cljs.core.truth_(inst_21667)){
var statearr_21688_21711 = state_21682__$1;
(statearr_21688_21711[(1)] = (8));

} else {
var statearr_21689_21712 = state_21682__$1;
(statearr_21689_21712[(1)] = (9));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21683 === (3))){
var inst_21680 = (state_21682[(2)]);
var state_21682__$1 = state_21682;
return cljs.core.async.impl.ioc_helpers.return_chan.call(null,state_21682__$1,inst_21680);
} else {
if((state_val_21683 === (12))){
var state_21682__$1 = state_21682;
var statearr_21690_21713 = state_21682__$1;
(statearr_21690_21713[(2)] = null);

(statearr_21690_21713[(1)] = (13));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21683 === (2))){
var inst_21659 = (state_21682[(7)]);
var state_21682__$1 = state_21682;
if(cljs.core.truth_(inst_21659)){
var statearr_21691_21714 = state_21682__$1;
(statearr_21691_21714[(1)] = (4));

} else {
var statearr_21692_21715 = state_21682__$1;
(statearr_21692_21715[(1)] = (5));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21683 === (11))){
var inst_21673 = cljs.core.async.close_BANG_.call(null,ch);
var state_21682__$1 = state_21682;
var statearr_21693_21716 = state_21682__$1;
(statearr_21693_21716[(2)] = inst_21673);

(statearr_21693_21716[(1)] = (13));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21683 === (9))){
var state_21682__$1 = state_21682;
if(cljs.core.truth_(close_QMARK_)){
var statearr_21694_21717 = state_21682__$1;
(statearr_21694_21717[(1)] = (11));

} else {
var statearr_21695_21718 = state_21682__$1;
(statearr_21695_21718[(1)] = (12));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21683 === (5))){
var inst_21659 = (state_21682[(7)]);
var state_21682__$1 = state_21682;
var statearr_21696_21719 = state_21682__$1;
(statearr_21696_21719[(2)] = inst_21659);

(statearr_21696_21719[(1)] = (6));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21683 === (10))){
var inst_21678 = (state_21682[(2)]);
var state_21682__$1 = state_21682;
var statearr_21697_21720 = state_21682__$1;
(statearr_21697_21720[(2)] = inst_21678);

(statearr_21697_21720[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_21683 === (8))){
var inst_21659 = (state_21682[(7)]);
var inst_21669 = cljs.core.next.call(null,inst_21659);
var inst_21659__$1 = inst_21669;
var state_21682__$1 = (function (){var statearr_21698 = state_21682;
(statearr_21698[(7)] = inst_21659__$1);

return statearr_21698;
})();
var statearr_21699_21721 = state_21682__$1;
(statearr_21699_21721[(2)] = null);

(statearr_21699_21721[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
return null;
}
}
}
}
}
}
}
}
}
}
}
}
}
});})(c__20932__auto__))
;
return ((function (switch__20870__auto__,c__20932__auto__){
return (function() {
var cljs$core$async$state_machine__20871__auto__ = null;
var cljs$core$async$state_machine__20871__auto____0 = (function (){
var statearr_21703 = [null,null,null,null,null,null,null,null];
(statearr_21703[(0)] = cljs$core$async$state_machine__20871__auto__);

(statearr_21703[(1)] = (1));

return statearr_21703;
});
var cljs$core$async$state_machine__20871__auto____1 = (function (state_21682){
while(true){
var ret_value__20872__auto__ = (function (){try{while(true){
var result__20873__auto__ = switch__20870__auto__.call(null,state_21682);
if(cljs.core.keyword_identical_QMARK_.call(null,result__20873__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__20873__auto__;
}
break;
}
}catch (e21704){if((e21704 instanceof Object)){
var ex__20874__auto__ = e21704;
var statearr_21705_21722 = state_21682;
(statearr_21705_21722[(5)] = ex__20874__auto__);


cljs.core.async.impl.ioc_helpers.process_exception.call(null,state_21682);

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
throw e21704;

}
}})();
if(cljs.core.keyword_identical_QMARK_.call(null,ret_value__20872__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__21723 = state_21682;
state_21682 = G__21723;
continue;
} else {
return ret_value__20872__auto__;
}
break;
}
});
cljs$core$async$state_machine__20871__auto__ = function(state_21682){
switch(arguments.length){
case 0:
return cljs$core$async$state_machine__20871__auto____0.call(this);
case 1:
return cljs$core$async$state_machine__20871__auto____1.call(this,state_21682);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$state_machine__20871__auto____0;
cljs$core$async$state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$state_machine__20871__auto____1;
return cljs$core$async$state_machine__20871__auto__;
})()
;})(switch__20870__auto__,c__20932__auto__))
})();
var state__20934__auto__ = (function (){var statearr_21706 = f__20933__auto__.call(null);
(statearr_21706[cljs.core.async.impl.ioc_helpers.USER_START_IDX] = c__20932__auto__);

return statearr_21706;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped.call(null,state__20934__auto__);
});})(c__20932__auto__))
);

return c__20932__auto__;
});

cljs.core.async.onto_chan.cljs$lang$maxFixedArity = 3;
/**
 * Creates and returns a channel which contains the contents of coll,
 * closing when exhausted.
 */
cljs.core.async.to_chan = (function cljs$core$async$to_chan(coll){
var ch = cljs.core.async.chan.call(null,cljs.core.bounded_count.call(null,(100),coll));
cljs.core.async.onto_chan.call(null,ch,coll);

return ch;
});

cljs.core.async.Mux = (function (){var obj21725 = {};
return obj21725;
})();

cljs.core.async.muxch_STAR_ = (function cljs$core$async$muxch_STAR_(_){
if((function (){var and__18061__auto__ = _;
if(and__18061__auto__){
return _.cljs$core$async$Mux$muxch_STAR_$arity$1;
} else {
return and__18061__auto__;
}
})()){
return _.cljs$core$async$Mux$muxch_STAR_$arity$1(_);
} else {
var x__18709__auto__ = (((_ == null))?null:_);
return (function (){var or__18073__auto__ = (cljs.core.async.muxch_STAR_[goog.typeOf(x__18709__auto__)]);
if(or__18073__auto__){
return or__18073__auto__;
} else {
var or__18073__auto____$1 = (cljs.core.async.muxch_STAR_["_"]);
if(or__18073__auto____$1){
return or__18073__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"Mux.muxch*",_);
}
}
})().call(null,_);
}
});


cljs.core.async.Mult = (function (){var obj21727 = {};
return obj21727;
})();

cljs.core.async.tap_STAR_ = (function cljs$core$async$tap_STAR_(m,ch,close_QMARK_){
if((function (){var and__18061__auto__ = m;
if(and__18061__auto__){
return m.cljs$core$async$Mult$tap_STAR_$arity$3;
} else {
return and__18061__auto__;
}
})()){
return m.cljs$core$async$Mult$tap_STAR_$arity$3(m,ch,close_QMARK_);
} else {
var x__18709__auto__ = (((m == null))?null:m);
return (function (){var or__18073__auto__ = (cljs.core.async.tap_STAR_[goog.typeOf(x__18709__auto__)]);
if(or__18073__auto__){
return or__18073__auto__;
} else {
var or__18073__auto____$1 = (cljs.core.async.tap_STAR_["_"]);
if(or__18073__auto____$1){
return or__18073__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"Mult.tap*",m);
}
}
})().call(null,m,ch,close_QMARK_);
}
});

cljs.core.async.untap_STAR_ = (function cljs$core$async$untap_STAR_(m,ch){
if((function (){var and__18061__auto__ = m;
if(and__18061__auto__){
return m.cljs$core$async$Mult$untap_STAR_$arity$2;
} else {
return and__18061__auto__;
}
})()){
return m.cljs$core$async$Mult$untap_STAR_$arity$2(m,ch);
} else {
var x__18709__auto__ = (((m == null))?null:m);
return (function (){var or__18073__auto__ = (cljs.core.async.untap_STAR_[goog.typeOf(x__18709__auto__)]);
if(or__18073__auto__){
return or__18073__auto__;
} else {
var or__18073__auto____$1 = (cljs.core.async.untap_STAR_["_"]);
if(or__18073__auto____$1){
return or__18073__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"Mult.untap*",m);
}
}
})().call(null,m,ch);
}
});

cljs.core.async.untap_all_STAR_ = (function cljs$core$async$untap_all_STAR_(m){
if((function (){var and__18061__auto__ = m;
if(and__18061__auto__){
return m.cljs$core$async$Mult$untap_all_STAR_$arity$1;
} else {
return and__18061__auto__;
}
})()){
return m.cljs$core$async$Mult$untap_all_STAR_$arity$1(m);
} else {
var x__18709__auto__ = (((m == null))?null:m);
return (function (){var or__18073__auto__ = (cljs.core.async.untap_all_STAR_[goog.typeOf(x__18709__auto__)]);
if(or__18073__auto__){
return or__18073__auto__;
} else {
var or__18073__auto____$1 = (cljs.core.async.untap_all_STAR_["_"]);
if(or__18073__auto____$1){
return or__18073__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"Mult.untap-all*",m);
}
}
})().call(null,m);
}
});

/**
 * Creates and returns a mult(iple) of the supplied channel. Channels
 * containing copies of the channel can be created with 'tap', and
 * detached with 'untap'.
 * 
 * Each item is distributed to all taps in parallel and synchronously,
 * i.e. each tap must accept before the next item is distributed. Use
 * buffering/windowing to prevent slow taps from holding up the mult.
 * 
 * Items received when there are no taps get dropped.
 * 
 * If a tap puts to a closed channel, it will be removed from the mult.
 */
cljs.core.async.mult = (function cljs$core$async$mult(ch){
var cs = cljs.core.atom.call(null,cljs.core.PersistentArrayMap.EMPTY);
var m = (function (){
if(typeof cljs.core.async.t21949 !== 'undefined'){
} else {

/**
* @constructor
*/
cljs.core.async.t21949 = (function (mult,ch,cs,meta21950){
this.mult = mult;
this.ch = ch;
this.cs = cs;
this.meta21950 = meta21950;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
})
cljs.core.async.t21949.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = ((function (cs){
return (function (_21951,meta21950__$1){
var self__ = this;
var _21951__$1 = this;
return (new cljs.core.async.t21949(self__.mult,self__.ch,self__.cs,meta21950__$1));
});})(cs))
;

cljs.core.async.t21949.prototype.cljs$core$IMeta$_meta$arity$1 = ((function (cs){
return (function (_21951){
var self__ = this;
var _21951__$1 = this;
return self__.meta21950;
});})(cs))
;

cljs.core.async.t21949.prototype.cljs$core$async$Mux$ = true;

cljs.core.async.t21949.prototype.cljs$core$async$Mux$muxch_STAR_$arity$1 = ((function (cs){
return (function (_){
var self__ = this;
var ___$1 = this;
return self__.ch;
});})(cs))
;

cljs.core.async.t21949.prototype.cljs$core$async$Mult$ = true;

cljs.core.async.t21949.prototype.cljs$core$async$Mult$tap_STAR_$arity$3 = ((function (cs){
return (function (_,ch__$1,close_QMARK_){
var self__ = this;
var ___$1 = this;
cljs.core.swap_BANG_.call(null,self__.cs,cljs.core.assoc,ch__$1,close_QMARK_);

return null;
});})(cs))
;

cljs.core.async.t21949.prototype.cljs$core$async$Mult$untap_STAR_$arity$2 = ((function (cs){
return (function (_,ch__$1){
var self__ = this;
var ___$1 = this;
cljs.core.swap_BANG_.call(null,self__.cs,cljs.core.dissoc,ch__$1);

return null;
});})(cs))
;

cljs.core.async.t21949.prototype.cljs$core$async$Mult$untap_all_STAR_$arity$1 = ((function (cs){
return (function (_){
var self__ = this;
var ___$1 = this;
cljs.core.reset_BANG_.call(null,self__.cs,cljs.core.PersistentArrayMap.EMPTY);

return null;
});})(cs))
;

cljs.core.async.t21949.getBasis = ((function (cs){
return (function (){
return new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"mult","mult",-1187640995,null),new cljs.core.Symbol(null,"ch","ch",1085813622,null),new cljs.core.Symbol(null,"cs","cs",-117024463,null),new cljs.core.Symbol(null,"meta21950","meta21950",993875612,null)], null);
});})(cs))
;

cljs.core.async.t21949.cljs$lang$type = true;

cljs.core.async.t21949.cljs$lang$ctorStr = "cljs.core.async/t21949";

cljs.core.async.t21949.cljs$lang$ctorPrWriter = ((function (cs){
return (function (this__18652__auto__,writer__18653__auto__,opt__18654__auto__){
return cljs.core._write.call(null,writer__18653__auto__,"cljs.core.async/t21949");
});})(cs))
;

cljs.core.async.__GT_t21949 = ((function (cs){
return (function cljs$core$async$mult_$___GT_t21949(mult__$1,ch__$1,cs__$1,meta21950){
return (new cljs.core.async.t21949(mult__$1,ch__$1,cs__$1,meta21950));
});})(cs))
;

}

return (new cljs.core.async.t21949(cljs$core$async$mult,ch,cs,cljs.core.PersistentArrayMap.EMPTY));
})()
;
var dchan = cljs.core.async.chan.call(null,(1));
var dctr = cljs.core.atom.call(null,null);
var done = ((function (cs,m,dchan,dctr){
return (function (_){
if((cljs.core.swap_BANG_.call(null,dctr,cljs.core.dec) === (0))){
return cljs.core.async.put_BANG_.call(null,dchan,true);
} else {
return null;
}
});})(cs,m,dchan,dctr))
;
var c__20932__auto___22170 = cljs.core.async.chan.call(null,(1));
cljs.core.async.impl.dispatch.run.call(null,((function (c__20932__auto___22170,cs,m,dchan,dctr,done){
return (function (){
var f__20933__auto__ = (function (){var switch__20870__auto__ = ((function (c__20932__auto___22170,cs,m,dchan,dctr,done){
return (function (state_22082){
var state_val_22083 = (state_22082[(1)]);
if((state_val_22083 === (7))){
var inst_22078 = (state_22082[(2)]);
var state_22082__$1 = state_22082;
var statearr_22084_22171 = state_22082__$1;
(statearr_22084_22171[(2)] = inst_22078);

(statearr_22084_22171[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (20))){
var inst_21983 = (state_22082[(7)]);
var inst_21993 = cljs.core.first.call(null,inst_21983);
var inst_21994 = cljs.core.nth.call(null,inst_21993,(0),null);
var inst_21995 = cljs.core.nth.call(null,inst_21993,(1),null);
var state_22082__$1 = (function (){var statearr_22085 = state_22082;
(statearr_22085[(8)] = inst_21994);

return statearr_22085;
})();
if(cljs.core.truth_(inst_21995)){
var statearr_22086_22172 = state_22082__$1;
(statearr_22086_22172[(1)] = (22));

} else {
var statearr_22087_22173 = state_22082__$1;
(statearr_22087_22173[(1)] = (23));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (27))){
var inst_21954 = (state_22082[(9)]);
var inst_22023 = (state_22082[(10)]);
var inst_22030 = (state_22082[(11)]);
var inst_22025 = (state_22082[(12)]);
var inst_22030__$1 = cljs.core._nth.call(null,inst_22023,inst_22025);
var inst_22031 = cljs.core.async.put_BANG_.call(null,inst_22030__$1,inst_21954,done);
var state_22082__$1 = (function (){var statearr_22088 = state_22082;
(statearr_22088[(11)] = inst_22030__$1);

return statearr_22088;
})();
if(cljs.core.truth_(inst_22031)){
var statearr_22089_22174 = state_22082__$1;
(statearr_22089_22174[(1)] = (30));

} else {
var statearr_22090_22175 = state_22082__$1;
(statearr_22090_22175[(1)] = (31));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (1))){
var state_22082__$1 = state_22082;
var statearr_22091_22176 = state_22082__$1;
(statearr_22091_22176[(2)] = null);

(statearr_22091_22176[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (24))){
var inst_21983 = (state_22082[(7)]);
var inst_22000 = (state_22082[(2)]);
var inst_22001 = cljs.core.next.call(null,inst_21983);
var inst_21963 = inst_22001;
var inst_21964 = null;
var inst_21965 = (0);
var inst_21966 = (0);
var state_22082__$1 = (function (){var statearr_22092 = state_22082;
(statearr_22092[(13)] = inst_22000);

(statearr_22092[(14)] = inst_21964);

(statearr_22092[(15)] = inst_21965);

(statearr_22092[(16)] = inst_21963);

(statearr_22092[(17)] = inst_21966);

return statearr_22092;
})();
var statearr_22093_22177 = state_22082__$1;
(statearr_22093_22177[(2)] = null);

(statearr_22093_22177[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (39))){
var state_22082__$1 = state_22082;
var statearr_22097_22178 = state_22082__$1;
(statearr_22097_22178[(2)] = null);

(statearr_22097_22178[(1)] = (41));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (4))){
var inst_21954 = (state_22082[(9)]);
var inst_21954__$1 = (state_22082[(2)]);
var inst_21955 = (inst_21954__$1 == null);
var state_22082__$1 = (function (){var statearr_22098 = state_22082;
(statearr_22098[(9)] = inst_21954__$1);

return statearr_22098;
})();
if(cljs.core.truth_(inst_21955)){
var statearr_22099_22179 = state_22082__$1;
(statearr_22099_22179[(1)] = (5));

} else {
var statearr_22100_22180 = state_22082__$1;
(statearr_22100_22180[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (15))){
var inst_21964 = (state_22082[(14)]);
var inst_21965 = (state_22082[(15)]);
var inst_21963 = (state_22082[(16)]);
var inst_21966 = (state_22082[(17)]);
var inst_21979 = (state_22082[(2)]);
var inst_21980 = (inst_21966 + (1));
var tmp22094 = inst_21964;
var tmp22095 = inst_21965;
var tmp22096 = inst_21963;
var inst_21963__$1 = tmp22096;
var inst_21964__$1 = tmp22094;
var inst_21965__$1 = tmp22095;
var inst_21966__$1 = inst_21980;
var state_22082__$1 = (function (){var statearr_22101 = state_22082;
(statearr_22101[(18)] = inst_21979);

(statearr_22101[(14)] = inst_21964__$1);

(statearr_22101[(15)] = inst_21965__$1);

(statearr_22101[(16)] = inst_21963__$1);

(statearr_22101[(17)] = inst_21966__$1);

return statearr_22101;
})();
var statearr_22102_22181 = state_22082__$1;
(statearr_22102_22181[(2)] = null);

(statearr_22102_22181[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (21))){
var inst_22004 = (state_22082[(2)]);
var state_22082__$1 = state_22082;
var statearr_22106_22182 = state_22082__$1;
(statearr_22106_22182[(2)] = inst_22004);

(statearr_22106_22182[(1)] = (18));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (31))){
var inst_22030 = (state_22082[(11)]);
var inst_22034 = done.call(null,null);
var inst_22035 = cljs.core.async.untap_STAR_.call(null,m,inst_22030);
var state_22082__$1 = (function (){var statearr_22107 = state_22082;
(statearr_22107[(19)] = inst_22034);

return statearr_22107;
})();
var statearr_22108_22183 = state_22082__$1;
(statearr_22108_22183[(2)] = inst_22035);

(statearr_22108_22183[(1)] = (32));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (32))){
var inst_22023 = (state_22082[(10)]);
var inst_22024 = (state_22082[(20)]);
var inst_22025 = (state_22082[(12)]);
var inst_22022 = (state_22082[(21)]);
var inst_22037 = (state_22082[(2)]);
var inst_22038 = (inst_22025 + (1));
var tmp22103 = inst_22023;
var tmp22104 = inst_22024;
var tmp22105 = inst_22022;
var inst_22022__$1 = tmp22105;
var inst_22023__$1 = tmp22103;
var inst_22024__$1 = tmp22104;
var inst_22025__$1 = inst_22038;
var state_22082__$1 = (function (){var statearr_22109 = state_22082;
(statearr_22109[(22)] = inst_22037);

(statearr_22109[(10)] = inst_22023__$1);

(statearr_22109[(20)] = inst_22024__$1);

(statearr_22109[(12)] = inst_22025__$1);

(statearr_22109[(21)] = inst_22022__$1);

return statearr_22109;
})();
var statearr_22110_22184 = state_22082__$1;
(statearr_22110_22184[(2)] = null);

(statearr_22110_22184[(1)] = (25));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (40))){
var inst_22050 = (state_22082[(23)]);
var inst_22054 = done.call(null,null);
var inst_22055 = cljs.core.async.untap_STAR_.call(null,m,inst_22050);
var state_22082__$1 = (function (){var statearr_22111 = state_22082;
(statearr_22111[(24)] = inst_22054);

return statearr_22111;
})();
var statearr_22112_22185 = state_22082__$1;
(statearr_22112_22185[(2)] = inst_22055);

(statearr_22112_22185[(1)] = (41));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (33))){
var inst_22041 = (state_22082[(25)]);
var inst_22043 = cljs.core.chunked_seq_QMARK_.call(null,inst_22041);
var state_22082__$1 = state_22082;
if(inst_22043){
var statearr_22113_22186 = state_22082__$1;
(statearr_22113_22186[(1)] = (36));

} else {
var statearr_22114_22187 = state_22082__$1;
(statearr_22114_22187[(1)] = (37));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (13))){
var inst_21973 = (state_22082[(26)]);
var inst_21976 = cljs.core.async.close_BANG_.call(null,inst_21973);
var state_22082__$1 = state_22082;
var statearr_22115_22188 = state_22082__$1;
(statearr_22115_22188[(2)] = inst_21976);

(statearr_22115_22188[(1)] = (15));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (22))){
var inst_21994 = (state_22082[(8)]);
var inst_21997 = cljs.core.async.close_BANG_.call(null,inst_21994);
var state_22082__$1 = state_22082;
var statearr_22116_22189 = state_22082__$1;
(statearr_22116_22189[(2)] = inst_21997);

(statearr_22116_22189[(1)] = (24));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (36))){
var inst_22041 = (state_22082[(25)]);
var inst_22045 = cljs.core.chunk_first.call(null,inst_22041);
var inst_22046 = cljs.core.chunk_rest.call(null,inst_22041);
var inst_22047 = cljs.core.count.call(null,inst_22045);
var inst_22022 = inst_22046;
var inst_22023 = inst_22045;
var inst_22024 = inst_22047;
var inst_22025 = (0);
var state_22082__$1 = (function (){var statearr_22117 = state_22082;
(statearr_22117[(10)] = inst_22023);

(statearr_22117[(20)] = inst_22024);

(statearr_22117[(12)] = inst_22025);

(statearr_22117[(21)] = inst_22022);

return statearr_22117;
})();
var statearr_22118_22190 = state_22082__$1;
(statearr_22118_22190[(2)] = null);

(statearr_22118_22190[(1)] = (25));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (41))){
var inst_22041 = (state_22082[(25)]);
var inst_22057 = (state_22082[(2)]);
var inst_22058 = cljs.core.next.call(null,inst_22041);
var inst_22022 = inst_22058;
var inst_22023 = null;
var inst_22024 = (0);
var inst_22025 = (0);
var state_22082__$1 = (function (){var statearr_22119 = state_22082;
(statearr_22119[(10)] = inst_22023);

(statearr_22119[(20)] = inst_22024);

(statearr_22119[(12)] = inst_22025);

(statearr_22119[(21)] = inst_22022);

(statearr_22119[(27)] = inst_22057);

return statearr_22119;
})();
var statearr_22120_22191 = state_22082__$1;
(statearr_22120_22191[(2)] = null);

(statearr_22120_22191[(1)] = (25));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (43))){
var state_22082__$1 = state_22082;
var statearr_22121_22192 = state_22082__$1;
(statearr_22121_22192[(2)] = null);

(statearr_22121_22192[(1)] = (44));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (29))){
var inst_22066 = (state_22082[(2)]);
var state_22082__$1 = state_22082;
var statearr_22122_22193 = state_22082__$1;
(statearr_22122_22193[(2)] = inst_22066);

(statearr_22122_22193[(1)] = (26));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (44))){
var inst_22075 = (state_22082[(2)]);
var state_22082__$1 = (function (){var statearr_22123 = state_22082;
(statearr_22123[(28)] = inst_22075);

return statearr_22123;
})();
var statearr_22124_22194 = state_22082__$1;
(statearr_22124_22194[(2)] = null);

(statearr_22124_22194[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (6))){
var inst_22014 = (state_22082[(29)]);
var inst_22013 = cljs.core.deref.call(null,cs);
var inst_22014__$1 = cljs.core.keys.call(null,inst_22013);
var inst_22015 = cljs.core.count.call(null,inst_22014__$1);
var inst_22016 = cljs.core.reset_BANG_.call(null,dctr,inst_22015);
var inst_22021 = cljs.core.seq.call(null,inst_22014__$1);
var inst_22022 = inst_22021;
var inst_22023 = null;
var inst_22024 = (0);
var inst_22025 = (0);
var state_22082__$1 = (function (){var statearr_22125 = state_22082;
(statearr_22125[(10)] = inst_22023);

(statearr_22125[(20)] = inst_22024);

(statearr_22125[(30)] = inst_22016);

(statearr_22125[(12)] = inst_22025);

(statearr_22125[(21)] = inst_22022);

(statearr_22125[(29)] = inst_22014__$1);

return statearr_22125;
})();
var statearr_22126_22195 = state_22082__$1;
(statearr_22126_22195[(2)] = null);

(statearr_22126_22195[(1)] = (25));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (28))){
var inst_22041 = (state_22082[(25)]);
var inst_22022 = (state_22082[(21)]);
var inst_22041__$1 = cljs.core.seq.call(null,inst_22022);
var state_22082__$1 = (function (){var statearr_22127 = state_22082;
(statearr_22127[(25)] = inst_22041__$1);

return statearr_22127;
})();
if(inst_22041__$1){
var statearr_22128_22196 = state_22082__$1;
(statearr_22128_22196[(1)] = (33));

} else {
var statearr_22129_22197 = state_22082__$1;
(statearr_22129_22197[(1)] = (34));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (25))){
var inst_22024 = (state_22082[(20)]);
var inst_22025 = (state_22082[(12)]);
var inst_22027 = (inst_22025 < inst_22024);
var inst_22028 = inst_22027;
var state_22082__$1 = state_22082;
if(cljs.core.truth_(inst_22028)){
var statearr_22130_22198 = state_22082__$1;
(statearr_22130_22198[(1)] = (27));

} else {
var statearr_22131_22199 = state_22082__$1;
(statearr_22131_22199[(1)] = (28));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (34))){
var state_22082__$1 = state_22082;
var statearr_22132_22200 = state_22082__$1;
(statearr_22132_22200[(2)] = null);

(statearr_22132_22200[(1)] = (35));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (17))){
var state_22082__$1 = state_22082;
var statearr_22133_22201 = state_22082__$1;
(statearr_22133_22201[(2)] = null);

(statearr_22133_22201[(1)] = (18));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (3))){
var inst_22080 = (state_22082[(2)]);
var state_22082__$1 = state_22082;
return cljs.core.async.impl.ioc_helpers.return_chan.call(null,state_22082__$1,inst_22080);
} else {
if((state_val_22083 === (12))){
var inst_22009 = (state_22082[(2)]);
var state_22082__$1 = state_22082;
var statearr_22134_22202 = state_22082__$1;
(statearr_22134_22202[(2)] = inst_22009);

(statearr_22134_22202[(1)] = (9));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (2))){
var state_22082__$1 = state_22082;
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_22082__$1,(4),ch);
} else {
if((state_val_22083 === (23))){
var state_22082__$1 = state_22082;
var statearr_22135_22203 = state_22082__$1;
(statearr_22135_22203[(2)] = null);

(statearr_22135_22203[(1)] = (24));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (35))){
var inst_22064 = (state_22082[(2)]);
var state_22082__$1 = state_22082;
var statearr_22136_22204 = state_22082__$1;
(statearr_22136_22204[(2)] = inst_22064);

(statearr_22136_22204[(1)] = (29));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (19))){
var inst_21983 = (state_22082[(7)]);
var inst_21987 = cljs.core.chunk_first.call(null,inst_21983);
var inst_21988 = cljs.core.chunk_rest.call(null,inst_21983);
var inst_21989 = cljs.core.count.call(null,inst_21987);
var inst_21963 = inst_21988;
var inst_21964 = inst_21987;
var inst_21965 = inst_21989;
var inst_21966 = (0);
var state_22082__$1 = (function (){var statearr_22137 = state_22082;
(statearr_22137[(14)] = inst_21964);

(statearr_22137[(15)] = inst_21965);

(statearr_22137[(16)] = inst_21963);

(statearr_22137[(17)] = inst_21966);

return statearr_22137;
})();
var statearr_22138_22205 = state_22082__$1;
(statearr_22138_22205[(2)] = null);

(statearr_22138_22205[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (11))){
var inst_21983 = (state_22082[(7)]);
var inst_21963 = (state_22082[(16)]);
var inst_21983__$1 = cljs.core.seq.call(null,inst_21963);
var state_22082__$1 = (function (){var statearr_22139 = state_22082;
(statearr_22139[(7)] = inst_21983__$1);

return statearr_22139;
})();
if(inst_21983__$1){
var statearr_22140_22206 = state_22082__$1;
(statearr_22140_22206[(1)] = (16));

} else {
var statearr_22141_22207 = state_22082__$1;
(statearr_22141_22207[(1)] = (17));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (9))){
var inst_22011 = (state_22082[(2)]);
var state_22082__$1 = state_22082;
var statearr_22142_22208 = state_22082__$1;
(statearr_22142_22208[(2)] = inst_22011);

(statearr_22142_22208[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (5))){
var inst_21961 = cljs.core.deref.call(null,cs);
var inst_21962 = cljs.core.seq.call(null,inst_21961);
var inst_21963 = inst_21962;
var inst_21964 = null;
var inst_21965 = (0);
var inst_21966 = (0);
var state_22082__$1 = (function (){var statearr_22143 = state_22082;
(statearr_22143[(14)] = inst_21964);

(statearr_22143[(15)] = inst_21965);

(statearr_22143[(16)] = inst_21963);

(statearr_22143[(17)] = inst_21966);

return statearr_22143;
})();
var statearr_22144_22209 = state_22082__$1;
(statearr_22144_22209[(2)] = null);

(statearr_22144_22209[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (14))){
var state_22082__$1 = state_22082;
var statearr_22145_22210 = state_22082__$1;
(statearr_22145_22210[(2)] = null);

(statearr_22145_22210[(1)] = (15));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (45))){
var inst_22072 = (state_22082[(2)]);
var state_22082__$1 = state_22082;
var statearr_22146_22211 = state_22082__$1;
(statearr_22146_22211[(2)] = inst_22072);

(statearr_22146_22211[(1)] = (44));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (26))){
var inst_22014 = (state_22082[(29)]);
var inst_22068 = (state_22082[(2)]);
var inst_22069 = cljs.core.seq.call(null,inst_22014);
var state_22082__$1 = (function (){var statearr_22147 = state_22082;
(statearr_22147[(31)] = inst_22068);

return statearr_22147;
})();
if(inst_22069){
var statearr_22148_22212 = state_22082__$1;
(statearr_22148_22212[(1)] = (42));

} else {
var statearr_22149_22213 = state_22082__$1;
(statearr_22149_22213[(1)] = (43));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (16))){
var inst_21983 = (state_22082[(7)]);
var inst_21985 = cljs.core.chunked_seq_QMARK_.call(null,inst_21983);
var state_22082__$1 = state_22082;
if(inst_21985){
var statearr_22150_22214 = state_22082__$1;
(statearr_22150_22214[(1)] = (19));

} else {
var statearr_22151_22215 = state_22082__$1;
(statearr_22151_22215[(1)] = (20));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (38))){
var inst_22061 = (state_22082[(2)]);
var state_22082__$1 = state_22082;
var statearr_22152_22216 = state_22082__$1;
(statearr_22152_22216[(2)] = inst_22061);

(statearr_22152_22216[(1)] = (35));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (30))){
var state_22082__$1 = state_22082;
var statearr_22153_22217 = state_22082__$1;
(statearr_22153_22217[(2)] = null);

(statearr_22153_22217[(1)] = (32));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (10))){
var inst_21964 = (state_22082[(14)]);
var inst_21966 = (state_22082[(17)]);
var inst_21972 = cljs.core._nth.call(null,inst_21964,inst_21966);
var inst_21973 = cljs.core.nth.call(null,inst_21972,(0),null);
var inst_21974 = cljs.core.nth.call(null,inst_21972,(1),null);
var state_22082__$1 = (function (){var statearr_22154 = state_22082;
(statearr_22154[(26)] = inst_21973);

return statearr_22154;
})();
if(cljs.core.truth_(inst_21974)){
var statearr_22155_22218 = state_22082__$1;
(statearr_22155_22218[(1)] = (13));

} else {
var statearr_22156_22219 = state_22082__$1;
(statearr_22156_22219[(1)] = (14));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (18))){
var inst_22007 = (state_22082[(2)]);
var state_22082__$1 = state_22082;
var statearr_22157_22220 = state_22082__$1;
(statearr_22157_22220[(2)] = inst_22007);

(statearr_22157_22220[(1)] = (12));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (42))){
var state_22082__$1 = state_22082;
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_22082__$1,(45),dchan);
} else {
if((state_val_22083 === (37))){
var inst_22041 = (state_22082[(25)]);
var inst_21954 = (state_22082[(9)]);
var inst_22050 = (state_22082[(23)]);
var inst_22050__$1 = cljs.core.first.call(null,inst_22041);
var inst_22051 = cljs.core.async.put_BANG_.call(null,inst_22050__$1,inst_21954,done);
var state_22082__$1 = (function (){var statearr_22158 = state_22082;
(statearr_22158[(23)] = inst_22050__$1);

return statearr_22158;
})();
if(cljs.core.truth_(inst_22051)){
var statearr_22159_22221 = state_22082__$1;
(statearr_22159_22221[(1)] = (39));

} else {
var statearr_22160_22222 = state_22082__$1;
(statearr_22160_22222[(1)] = (40));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22083 === (8))){
var inst_21965 = (state_22082[(15)]);
var inst_21966 = (state_22082[(17)]);
var inst_21968 = (inst_21966 < inst_21965);
var inst_21969 = inst_21968;
var state_22082__$1 = state_22082;
if(cljs.core.truth_(inst_21969)){
var statearr_22161_22223 = state_22082__$1;
(statearr_22161_22223[(1)] = (10));

} else {
var statearr_22162_22224 = state_22082__$1;
(statearr_22162_22224[(1)] = (11));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
return null;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
});})(c__20932__auto___22170,cs,m,dchan,dctr,done))
;
return ((function (switch__20870__auto__,c__20932__auto___22170,cs,m,dchan,dctr,done){
return (function() {
var cljs$core$async$mult_$_state_machine__20871__auto__ = null;
var cljs$core$async$mult_$_state_machine__20871__auto____0 = (function (){
var statearr_22166 = [null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null];
(statearr_22166[(0)] = cljs$core$async$mult_$_state_machine__20871__auto__);

(statearr_22166[(1)] = (1));

return statearr_22166;
});
var cljs$core$async$mult_$_state_machine__20871__auto____1 = (function (state_22082){
while(true){
var ret_value__20872__auto__ = (function (){try{while(true){
var result__20873__auto__ = switch__20870__auto__.call(null,state_22082);
if(cljs.core.keyword_identical_QMARK_.call(null,result__20873__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__20873__auto__;
}
break;
}
}catch (e22167){if((e22167 instanceof Object)){
var ex__20874__auto__ = e22167;
var statearr_22168_22225 = state_22082;
(statearr_22168_22225[(5)] = ex__20874__auto__);


cljs.core.async.impl.ioc_helpers.process_exception.call(null,state_22082);

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
throw e22167;

}
}})();
if(cljs.core.keyword_identical_QMARK_.call(null,ret_value__20872__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__22226 = state_22082;
state_22082 = G__22226;
continue;
} else {
return ret_value__20872__auto__;
}
break;
}
});
cljs$core$async$mult_$_state_machine__20871__auto__ = function(state_22082){
switch(arguments.length){
case 0:
return cljs$core$async$mult_$_state_machine__20871__auto____0.call(this);
case 1:
return cljs$core$async$mult_$_state_machine__20871__auto____1.call(this,state_22082);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$mult_$_state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$mult_$_state_machine__20871__auto____0;
cljs$core$async$mult_$_state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$mult_$_state_machine__20871__auto____1;
return cljs$core$async$mult_$_state_machine__20871__auto__;
})()
;})(switch__20870__auto__,c__20932__auto___22170,cs,m,dchan,dctr,done))
})();
var state__20934__auto__ = (function (){var statearr_22169 = f__20933__auto__.call(null);
(statearr_22169[cljs.core.async.impl.ioc_helpers.USER_START_IDX] = c__20932__auto___22170);

return statearr_22169;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped.call(null,state__20934__auto__);
});})(c__20932__auto___22170,cs,m,dchan,dctr,done))
);


return m;
});
/**
 * Copies the mult source onto the supplied channel.
 * 
 * By default the channel will be closed when the source closes,
 * but can be determined by the close? parameter.
 */
cljs.core.async.tap = (function cljs$core$async$tap(){
var G__22228 = arguments.length;
switch (G__22228) {
case 2:
return cljs.core.async.tap.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return cljs.core.async.tap.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

cljs.core.async.tap.cljs$core$IFn$_invoke$arity$2 = (function (mult,ch){
return cljs.core.async.tap.call(null,mult,ch,true);
});

cljs.core.async.tap.cljs$core$IFn$_invoke$arity$3 = (function (mult,ch,close_QMARK_){
cljs.core.async.tap_STAR_.call(null,mult,ch,close_QMARK_);

return ch;
});

cljs.core.async.tap.cljs$lang$maxFixedArity = 3;
/**
 * Disconnects a target channel from a mult
 */
cljs.core.async.untap = (function cljs$core$async$untap(mult,ch){
return cljs.core.async.untap_STAR_.call(null,mult,ch);
});
/**
 * Disconnects all target channels from a mult
 */
cljs.core.async.untap_all = (function cljs$core$async$untap_all(mult){
return cljs.core.async.untap_all_STAR_.call(null,mult);
});

cljs.core.async.Mix = (function (){var obj22231 = {};
return obj22231;
})();

cljs.core.async.admix_STAR_ = (function cljs$core$async$admix_STAR_(m,ch){
if((function (){var and__18061__auto__ = m;
if(and__18061__auto__){
return m.cljs$core$async$Mix$admix_STAR_$arity$2;
} else {
return and__18061__auto__;
}
})()){
return m.cljs$core$async$Mix$admix_STAR_$arity$2(m,ch);
} else {
var x__18709__auto__ = (((m == null))?null:m);
return (function (){var or__18073__auto__ = (cljs.core.async.admix_STAR_[goog.typeOf(x__18709__auto__)]);
if(or__18073__auto__){
return or__18073__auto__;
} else {
var or__18073__auto____$1 = (cljs.core.async.admix_STAR_["_"]);
if(or__18073__auto____$1){
return or__18073__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"Mix.admix*",m);
}
}
})().call(null,m,ch);
}
});

cljs.core.async.unmix_STAR_ = (function cljs$core$async$unmix_STAR_(m,ch){
if((function (){var and__18061__auto__ = m;
if(and__18061__auto__){
return m.cljs$core$async$Mix$unmix_STAR_$arity$2;
} else {
return and__18061__auto__;
}
})()){
return m.cljs$core$async$Mix$unmix_STAR_$arity$2(m,ch);
} else {
var x__18709__auto__ = (((m == null))?null:m);
return (function (){var or__18073__auto__ = (cljs.core.async.unmix_STAR_[goog.typeOf(x__18709__auto__)]);
if(or__18073__auto__){
return or__18073__auto__;
} else {
var or__18073__auto____$1 = (cljs.core.async.unmix_STAR_["_"]);
if(or__18073__auto____$1){
return or__18073__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"Mix.unmix*",m);
}
}
})().call(null,m,ch);
}
});

cljs.core.async.unmix_all_STAR_ = (function cljs$core$async$unmix_all_STAR_(m){
if((function (){var and__18061__auto__ = m;
if(and__18061__auto__){
return m.cljs$core$async$Mix$unmix_all_STAR_$arity$1;
} else {
return and__18061__auto__;
}
})()){
return m.cljs$core$async$Mix$unmix_all_STAR_$arity$1(m);
} else {
var x__18709__auto__ = (((m == null))?null:m);
return (function (){var or__18073__auto__ = (cljs.core.async.unmix_all_STAR_[goog.typeOf(x__18709__auto__)]);
if(or__18073__auto__){
return or__18073__auto__;
} else {
var or__18073__auto____$1 = (cljs.core.async.unmix_all_STAR_["_"]);
if(or__18073__auto____$1){
return or__18073__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"Mix.unmix-all*",m);
}
}
})().call(null,m);
}
});

cljs.core.async.toggle_STAR_ = (function cljs$core$async$toggle_STAR_(m,state_map){
if((function (){var and__18061__auto__ = m;
if(and__18061__auto__){
return m.cljs$core$async$Mix$toggle_STAR_$arity$2;
} else {
return and__18061__auto__;
}
})()){
return m.cljs$core$async$Mix$toggle_STAR_$arity$2(m,state_map);
} else {
var x__18709__auto__ = (((m == null))?null:m);
return (function (){var or__18073__auto__ = (cljs.core.async.toggle_STAR_[goog.typeOf(x__18709__auto__)]);
if(or__18073__auto__){
return or__18073__auto__;
} else {
var or__18073__auto____$1 = (cljs.core.async.toggle_STAR_["_"]);
if(or__18073__auto____$1){
return or__18073__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"Mix.toggle*",m);
}
}
})().call(null,m,state_map);
}
});

cljs.core.async.solo_mode_STAR_ = (function cljs$core$async$solo_mode_STAR_(m,mode){
if((function (){var and__18061__auto__ = m;
if(and__18061__auto__){
return m.cljs$core$async$Mix$solo_mode_STAR_$arity$2;
} else {
return and__18061__auto__;
}
})()){
return m.cljs$core$async$Mix$solo_mode_STAR_$arity$2(m,mode);
} else {
var x__18709__auto__ = (((m == null))?null:m);
return (function (){var or__18073__auto__ = (cljs.core.async.solo_mode_STAR_[goog.typeOf(x__18709__auto__)]);
if(or__18073__auto__){
return or__18073__auto__;
} else {
var or__18073__auto____$1 = (cljs.core.async.solo_mode_STAR_["_"]);
if(or__18073__auto____$1){
return or__18073__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"Mix.solo-mode*",m);
}
}
})().call(null,m,mode);
}
});

cljs.core.async.ioc_alts_BANG_ = (function cljs$core$async$ioc_alts_BANG_(){
var argseq__19113__auto__ = ((((3) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(3)),(0))):null);
return cljs.core.async.ioc_alts_BANG_.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),argseq__19113__auto__);
});

cljs.core.async.ioc_alts_BANG_.cljs$core$IFn$_invoke$arity$variadic = (function (state,cont_block,ports,p__22236){
var map__22237 = p__22236;
var map__22237__$1 = ((cljs.core.seq_QMARK_.call(null,map__22237))?cljs.core.apply.call(null,cljs.core.hash_map,map__22237):map__22237);
var opts = map__22237__$1;
var statearr_22238_22241 = state;
(statearr_22238_22241[cljs.core.async.impl.ioc_helpers.STATE_IDX] = cont_block);


var temp__4423__auto__ = cljs.core.async.do_alts.call(null,((function (map__22237,map__22237__$1,opts){
return (function (val){
var statearr_22239_22242 = state;
(statearr_22239_22242[cljs.core.async.impl.ioc_helpers.VALUE_IDX] = val);


return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped.call(null,state);
});})(map__22237,map__22237__$1,opts))
,ports,opts);
if(cljs.core.truth_(temp__4423__auto__)){
var cb = temp__4423__auto__;
var statearr_22240_22243 = state;
(statearr_22240_22243[cljs.core.async.impl.ioc_helpers.VALUE_IDX] = cljs.core.deref.call(null,cb));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
return null;
}
});

cljs.core.async.ioc_alts_BANG_.cljs$lang$maxFixedArity = (3);

cljs.core.async.ioc_alts_BANG_.cljs$lang$applyTo = (function (seq22232){
var G__22233 = cljs.core.first.call(null,seq22232);
var seq22232__$1 = cljs.core.next.call(null,seq22232);
var G__22234 = cljs.core.first.call(null,seq22232__$1);
var seq22232__$2 = cljs.core.next.call(null,seq22232__$1);
var G__22235 = cljs.core.first.call(null,seq22232__$2);
var seq22232__$3 = cljs.core.next.call(null,seq22232__$2);
return cljs.core.async.ioc_alts_BANG_.cljs$core$IFn$_invoke$arity$variadic(G__22233,G__22234,G__22235,seq22232__$3);
});
/**
 * Creates and returns a mix of one or more input channels which will
 * be put on the supplied out channel. Input sources can be added to
 * the mix with 'admix', and removed with 'unmix'. A mix supports
 * soloing, muting and pausing multiple inputs atomically using
 * 'toggle', and can solo using either muting or pausing as determined
 * by 'solo-mode'.
 * 
 * Each channel can have zero or more boolean modes set via 'toggle':
 * 
 * :solo - when true, only this (ond other soloed) channel(s) will appear
 * in the mix output channel. :mute and :pause states of soloed
 * channels are ignored. If solo-mode is :mute, non-soloed
 * channels are muted, if :pause, non-soloed channels are
 * paused.
 * 
 * :mute - muted channels will have their contents consumed but not included in the mix
 * :pause - paused channels will not have their contents consumed (and thus also not included in the mix)
 */
cljs.core.async.mix = (function cljs$core$async$mix(out){
var cs = cljs.core.atom.call(null,cljs.core.PersistentArrayMap.EMPTY);
var solo_modes = new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"pause","pause",-2095325672),null,new cljs.core.Keyword(null,"mute","mute",1151223646),null], null), null);
var attrs = cljs.core.conj.call(null,solo_modes,new cljs.core.Keyword(null,"solo","solo",-316350075));
var solo_mode = cljs.core.atom.call(null,new cljs.core.Keyword(null,"mute","mute",1151223646));
var change = cljs.core.async.chan.call(null);
var changed = ((function (cs,solo_modes,attrs,solo_mode,change){
return (function (){
return cljs.core.async.put_BANG_.call(null,change,true);
});})(cs,solo_modes,attrs,solo_mode,change))
;
var pick = ((function (cs,solo_modes,attrs,solo_mode,change,changed){
return (function (attr,chs){
return cljs.core.reduce_kv.call(null,((function (cs,solo_modes,attrs,solo_mode,change,changed){
return (function (ret,c,v){
if(cljs.core.truth_(attr.call(null,v))){
return cljs.core.conj.call(null,ret,c);
} else {
return ret;
}
});})(cs,solo_modes,attrs,solo_mode,change,changed))
,cljs.core.PersistentHashSet.EMPTY,chs);
});})(cs,solo_modes,attrs,solo_mode,change,changed))
;
var calc_state = ((function (cs,solo_modes,attrs,solo_mode,change,changed,pick){
return (function (){
var chs = cljs.core.deref.call(null,cs);
var mode = cljs.core.deref.call(null,solo_mode);
var solos = pick.call(null,new cljs.core.Keyword(null,"solo","solo",-316350075),chs);
var pauses = pick.call(null,new cljs.core.Keyword(null,"pause","pause",-2095325672),chs);
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"solos","solos",1441458643),solos,new cljs.core.Keyword(null,"mutes","mutes",1068806309),pick.call(null,new cljs.core.Keyword(null,"mute","mute",1151223646),chs),new cljs.core.Keyword(null,"reads","reads",-1215067361),cljs.core.conj.call(null,(((cljs.core._EQ_.call(null,mode,new cljs.core.Keyword(null,"pause","pause",-2095325672))) && (!(cljs.core.empty_QMARK_.call(null,solos))))?cljs.core.vec.call(null,solos):cljs.core.vec.call(null,cljs.core.remove.call(null,pauses,cljs.core.keys.call(null,chs)))),change)], null);
});})(cs,solo_modes,attrs,solo_mode,change,changed,pick))
;
var m = (function (){
if(typeof cljs.core.async.t22363 !== 'undefined'){
} else {

/**
* @constructor
*/
cljs.core.async.t22363 = (function (change,mix,solo_mode,pick,cs,calc_state,out,changed,solo_modes,attrs,meta22364){
this.change = change;
this.mix = mix;
this.solo_mode = solo_mode;
this.pick = pick;
this.cs = cs;
this.calc_state = calc_state;
this.out = out;
this.changed = changed;
this.solo_modes = solo_modes;
this.attrs = attrs;
this.meta22364 = meta22364;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
})
cljs.core.async.t22363.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = ((function (cs,solo_modes,attrs,solo_mode,change,changed,pick,calc_state){
return (function (_22365,meta22364__$1){
var self__ = this;
var _22365__$1 = this;
return (new cljs.core.async.t22363(self__.change,self__.mix,self__.solo_mode,self__.pick,self__.cs,self__.calc_state,self__.out,self__.changed,self__.solo_modes,self__.attrs,meta22364__$1));
});})(cs,solo_modes,attrs,solo_mode,change,changed,pick,calc_state))
;

cljs.core.async.t22363.prototype.cljs$core$IMeta$_meta$arity$1 = ((function (cs,solo_modes,attrs,solo_mode,change,changed,pick,calc_state){
return (function (_22365){
var self__ = this;
var _22365__$1 = this;
return self__.meta22364;
});})(cs,solo_modes,attrs,solo_mode,change,changed,pick,calc_state))
;

cljs.core.async.t22363.prototype.cljs$core$async$Mux$ = true;

cljs.core.async.t22363.prototype.cljs$core$async$Mux$muxch_STAR_$arity$1 = ((function (cs,solo_modes,attrs,solo_mode,change,changed,pick,calc_state){
return (function (_){
var self__ = this;
var ___$1 = this;
return self__.out;
});})(cs,solo_modes,attrs,solo_mode,change,changed,pick,calc_state))
;

cljs.core.async.t22363.prototype.cljs$core$async$Mix$ = true;

cljs.core.async.t22363.prototype.cljs$core$async$Mix$admix_STAR_$arity$2 = ((function (cs,solo_modes,attrs,solo_mode,change,changed,pick,calc_state){
return (function (_,ch){
var self__ = this;
var ___$1 = this;
cljs.core.swap_BANG_.call(null,self__.cs,cljs.core.assoc,ch,cljs.core.PersistentArrayMap.EMPTY);

return self__.changed.call(null);
});})(cs,solo_modes,attrs,solo_mode,change,changed,pick,calc_state))
;

cljs.core.async.t22363.prototype.cljs$core$async$Mix$unmix_STAR_$arity$2 = ((function (cs,solo_modes,attrs,solo_mode,change,changed,pick,calc_state){
return (function (_,ch){
var self__ = this;
var ___$1 = this;
cljs.core.swap_BANG_.call(null,self__.cs,cljs.core.dissoc,ch);

return self__.changed.call(null);
});})(cs,solo_modes,attrs,solo_mode,change,changed,pick,calc_state))
;

cljs.core.async.t22363.prototype.cljs$core$async$Mix$unmix_all_STAR_$arity$1 = ((function (cs,solo_modes,attrs,solo_mode,change,changed,pick,calc_state){
return (function (_){
var self__ = this;
var ___$1 = this;
cljs.core.reset_BANG_.call(null,self__.cs,cljs.core.PersistentArrayMap.EMPTY);

return self__.changed.call(null);
});})(cs,solo_modes,attrs,solo_mode,change,changed,pick,calc_state))
;

cljs.core.async.t22363.prototype.cljs$core$async$Mix$toggle_STAR_$arity$2 = ((function (cs,solo_modes,attrs,solo_mode,change,changed,pick,calc_state){
return (function (_,state_map){
var self__ = this;
var ___$1 = this;
cljs.core.swap_BANG_.call(null,self__.cs,cljs.core.partial.call(null,cljs.core.merge_with,cljs.core.merge),state_map);

return self__.changed.call(null);
});})(cs,solo_modes,attrs,solo_mode,change,changed,pick,calc_state))
;

cljs.core.async.t22363.prototype.cljs$core$async$Mix$solo_mode_STAR_$arity$2 = ((function (cs,solo_modes,attrs,solo_mode,change,changed,pick,calc_state){
return (function (_,mode){
var self__ = this;
var ___$1 = this;
if(cljs.core.truth_(self__.solo_modes.call(null,mode))){
} else {
throw (new Error([cljs.core.str("Assert failed: "),cljs.core.str([cljs.core.str("mode must be one of: "),cljs.core.str(self__.solo_modes)].join('')),cljs.core.str("\n"),cljs.core.str(cljs.core.pr_str.call(null,cljs.core.list(new cljs.core.Symbol(null,"solo-modes","solo-modes",882180540,null),new cljs.core.Symbol(null,"mode","mode",-2000032078,null))))].join('')));
}

cljs.core.reset_BANG_.call(null,self__.solo_mode,mode);

return self__.changed.call(null);
});})(cs,solo_modes,attrs,solo_mode,change,changed,pick,calc_state))
;

cljs.core.async.t22363.getBasis = ((function (cs,solo_modes,attrs,solo_mode,change,changed,pick,calc_state){
return (function (){
return new cljs.core.PersistentVector(null, 11, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"change","change",477485025,null),new cljs.core.Symbol(null,"mix","mix",2121373763,null),new cljs.core.Symbol(null,"solo-mode","solo-mode",2031788074,null),new cljs.core.Symbol(null,"pick","pick",1300068175,null),new cljs.core.Symbol(null,"cs","cs",-117024463,null),new cljs.core.Symbol(null,"calc-state","calc-state",-349968968,null),new cljs.core.Symbol(null,"out","out",729986010,null),new cljs.core.Symbol(null,"changed","changed",-2083710852,null),new cljs.core.Symbol(null,"solo-modes","solo-modes",882180540,null),new cljs.core.Symbol(null,"attrs","attrs",-450137186,null),new cljs.core.Symbol(null,"meta22364","meta22364",420958948,null)], null);
});})(cs,solo_modes,attrs,solo_mode,change,changed,pick,calc_state))
;

cljs.core.async.t22363.cljs$lang$type = true;

cljs.core.async.t22363.cljs$lang$ctorStr = "cljs.core.async/t22363";

cljs.core.async.t22363.cljs$lang$ctorPrWriter = ((function (cs,solo_modes,attrs,solo_mode,change,changed,pick,calc_state){
return (function (this__18652__auto__,writer__18653__auto__,opt__18654__auto__){
return cljs.core._write.call(null,writer__18653__auto__,"cljs.core.async/t22363");
});})(cs,solo_modes,attrs,solo_mode,change,changed,pick,calc_state))
;

cljs.core.async.__GT_t22363 = ((function (cs,solo_modes,attrs,solo_mode,change,changed,pick,calc_state){
return (function cljs$core$async$mix_$___GT_t22363(change__$1,mix__$1,solo_mode__$1,pick__$1,cs__$1,calc_state__$1,out__$1,changed__$1,solo_modes__$1,attrs__$1,meta22364){
return (new cljs.core.async.t22363(change__$1,mix__$1,solo_mode__$1,pick__$1,cs__$1,calc_state__$1,out__$1,changed__$1,solo_modes__$1,attrs__$1,meta22364));
});})(cs,solo_modes,attrs,solo_mode,change,changed,pick,calc_state))
;

}

return (new cljs.core.async.t22363(change,cljs$core$async$mix,solo_mode,pick,cs,calc_state,out,changed,solo_modes,attrs,cljs.core.PersistentArrayMap.EMPTY));
})()
;
var c__20932__auto___22482 = cljs.core.async.chan.call(null,(1));
cljs.core.async.impl.dispatch.run.call(null,((function (c__20932__auto___22482,cs,solo_modes,attrs,solo_mode,change,changed,pick,calc_state,m){
return (function (){
var f__20933__auto__ = (function (){var switch__20870__auto__ = ((function (c__20932__auto___22482,cs,solo_modes,attrs,solo_mode,change,changed,pick,calc_state,m){
return (function (state_22435){
var state_val_22436 = (state_22435[(1)]);
if((state_val_22436 === (7))){
var inst_22379 = (state_22435[(7)]);
var inst_22384 = cljs.core.apply.call(null,cljs.core.hash_map,inst_22379);
var state_22435__$1 = state_22435;
var statearr_22437_22483 = state_22435__$1;
(statearr_22437_22483[(2)] = inst_22384);

(statearr_22437_22483[(1)] = (9));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22436 === (20))){
var inst_22394 = (state_22435[(8)]);
var state_22435__$1 = state_22435;
return cljs.core.async.impl.ioc_helpers.put_BANG_.call(null,state_22435__$1,(23),out,inst_22394);
} else {
if((state_val_22436 === (1))){
var inst_22369 = (state_22435[(9)]);
var inst_22369__$1 = calc_state.call(null);
var inst_22370 = cljs.core.seq_QMARK_.call(null,inst_22369__$1);
var state_22435__$1 = (function (){var statearr_22438 = state_22435;
(statearr_22438[(9)] = inst_22369__$1);

return statearr_22438;
})();
if(inst_22370){
var statearr_22439_22484 = state_22435__$1;
(statearr_22439_22484[(1)] = (2));

} else {
var statearr_22440_22485 = state_22435__$1;
(statearr_22440_22485[(1)] = (3));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22436 === (24))){
var inst_22387 = (state_22435[(10)]);
var inst_22379 = inst_22387;
var state_22435__$1 = (function (){var statearr_22441 = state_22435;
(statearr_22441[(7)] = inst_22379);

return statearr_22441;
})();
var statearr_22442_22486 = state_22435__$1;
(statearr_22442_22486[(2)] = null);

(statearr_22442_22486[(1)] = (5));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22436 === (4))){
var inst_22369 = (state_22435[(9)]);
var inst_22375 = (state_22435[(2)]);
var inst_22376 = cljs.core.get.call(null,inst_22375,new cljs.core.Keyword(null,"solos","solos",1441458643));
var inst_22377 = cljs.core.get.call(null,inst_22375,new cljs.core.Keyword(null,"mutes","mutes",1068806309));
var inst_22378 = cljs.core.get.call(null,inst_22375,new cljs.core.Keyword(null,"reads","reads",-1215067361));
var inst_22379 = inst_22369;
var state_22435__$1 = (function (){var statearr_22443 = state_22435;
(statearr_22443[(11)] = inst_22377);

(statearr_22443[(12)] = inst_22376);

(statearr_22443[(7)] = inst_22379);

(statearr_22443[(13)] = inst_22378);

return statearr_22443;
})();
var statearr_22444_22487 = state_22435__$1;
(statearr_22444_22487[(2)] = null);

(statearr_22444_22487[(1)] = (5));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22436 === (15))){
var state_22435__$1 = state_22435;
var statearr_22445_22488 = state_22435__$1;
(statearr_22445_22488[(2)] = null);

(statearr_22445_22488[(1)] = (16));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22436 === (21))){
var inst_22387 = (state_22435[(10)]);
var inst_22379 = inst_22387;
var state_22435__$1 = (function (){var statearr_22446 = state_22435;
(statearr_22446[(7)] = inst_22379);

return statearr_22446;
})();
var statearr_22447_22489 = state_22435__$1;
(statearr_22447_22489[(2)] = null);

(statearr_22447_22489[(1)] = (5));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22436 === (13))){
var inst_22431 = (state_22435[(2)]);
var state_22435__$1 = state_22435;
var statearr_22448_22490 = state_22435__$1;
(statearr_22448_22490[(2)] = inst_22431);

(statearr_22448_22490[(1)] = (6));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22436 === (22))){
var inst_22429 = (state_22435[(2)]);
var state_22435__$1 = state_22435;
var statearr_22449_22491 = state_22435__$1;
(statearr_22449_22491[(2)] = inst_22429);

(statearr_22449_22491[(1)] = (13));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22436 === (6))){
var inst_22433 = (state_22435[(2)]);
var state_22435__$1 = state_22435;
return cljs.core.async.impl.ioc_helpers.return_chan.call(null,state_22435__$1,inst_22433);
} else {
if((state_val_22436 === (25))){
var state_22435__$1 = state_22435;
var statearr_22450_22492 = state_22435__$1;
(statearr_22450_22492[(2)] = null);

(statearr_22450_22492[(1)] = (26));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22436 === (17))){
var inst_22409 = (state_22435[(14)]);
var state_22435__$1 = state_22435;
var statearr_22451_22493 = state_22435__$1;
(statearr_22451_22493[(2)] = inst_22409);

(statearr_22451_22493[(1)] = (19));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22436 === (3))){
var inst_22369 = (state_22435[(9)]);
var state_22435__$1 = state_22435;
var statearr_22452_22494 = state_22435__$1;
(statearr_22452_22494[(2)] = inst_22369);

(statearr_22452_22494[(1)] = (4));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22436 === (12))){
var inst_22388 = (state_22435[(15)]);
var inst_22395 = (state_22435[(16)]);
var inst_22409 = (state_22435[(14)]);
var inst_22409__$1 = inst_22388.call(null,inst_22395);
var state_22435__$1 = (function (){var statearr_22453 = state_22435;
(statearr_22453[(14)] = inst_22409__$1);

return statearr_22453;
})();
if(cljs.core.truth_(inst_22409__$1)){
var statearr_22454_22495 = state_22435__$1;
(statearr_22454_22495[(1)] = (17));

} else {
var statearr_22455_22496 = state_22435__$1;
(statearr_22455_22496[(1)] = (18));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22436 === (2))){
var inst_22369 = (state_22435[(9)]);
var inst_22372 = cljs.core.apply.call(null,cljs.core.hash_map,inst_22369);
var state_22435__$1 = state_22435;
var statearr_22456_22497 = state_22435__$1;
(statearr_22456_22497[(2)] = inst_22372);

(statearr_22456_22497[(1)] = (4));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22436 === (23))){
var inst_22420 = (state_22435[(2)]);
var state_22435__$1 = state_22435;
if(cljs.core.truth_(inst_22420)){
var statearr_22457_22498 = state_22435__$1;
(statearr_22457_22498[(1)] = (24));

} else {
var statearr_22458_22499 = state_22435__$1;
(statearr_22458_22499[(1)] = (25));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22436 === (19))){
var inst_22417 = (state_22435[(2)]);
var state_22435__$1 = state_22435;
if(cljs.core.truth_(inst_22417)){
var statearr_22459_22500 = state_22435__$1;
(statearr_22459_22500[(1)] = (20));

} else {
var statearr_22460_22501 = state_22435__$1;
(statearr_22460_22501[(1)] = (21));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22436 === (11))){
var inst_22394 = (state_22435[(8)]);
var inst_22400 = (inst_22394 == null);
var state_22435__$1 = state_22435;
if(cljs.core.truth_(inst_22400)){
var statearr_22461_22502 = state_22435__$1;
(statearr_22461_22502[(1)] = (14));

} else {
var statearr_22462_22503 = state_22435__$1;
(statearr_22462_22503[(1)] = (15));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22436 === (9))){
var inst_22387 = (state_22435[(10)]);
var inst_22387__$1 = (state_22435[(2)]);
var inst_22388 = cljs.core.get.call(null,inst_22387__$1,new cljs.core.Keyword(null,"solos","solos",1441458643));
var inst_22389 = cljs.core.get.call(null,inst_22387__$1,new cljs.core.Keyword(null,"mutes","mutes",1068806309));
var inst_22390 = cljs.core.get.call(null,inst_22387__$1,new cljs.core.Keyword(null,"reads","reads",-1215067361));
var state_22435__$1 = (function (){var statearr_22463 = state_22435;
(statearr_22463[(17)] = inst_22389);

(statearr_22463[(15)] = inst_22388);

(statearr_22463[(10)] = inst_22387__$1);

return statearr_22463;
})();
return cljs.core.async.ioc_alts_BANG_.call(null,state_22435__$1,(10),inst_22390);
} else {
if((state_val_22436 === (5))){
var inst_22379 = (state_22435[(7)]);
var inst_22382 = cljs.core.seq_QMARK_.call(null,inst_22379);
var state_22435__$1 = state_22435;
if(inst_22382){
var statearr_22464_22504 = state_22435__$1;
(statearr_22464_22504[(1)] = (7));

} else {
var statearr_22465_22505 = state_22435__$1;
(statearr_22465_22505[(1)] = (8));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22436 === (14))){
var inst_22395 = (state_22435[(16)]);
var inst_22402 = cljs.core.swap_BANG_.call(null,cs,cljs.core.dissoc,inst_22395);
var state_22435__$1 = state_22435;
var statearr_22466_22506 = state_22435__$1;
(statearr_22466_22506[(2)] = inst_22402);

(statearr_22466_22506[(1)] = (16));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22436 === (26))){
var inst_22425 = (state_22435[(2)]);
var state_22435__$1 = state_22435;
var statearr_22467_22507 = state_22435__$1;
(statearr_22467_22507[(2)] = inst_22425);

(statearr_22467_22507[(1)] = (22));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22436 === (16))){
var inst_22405 = (state_22435[(2)]);
var inst_22406 = calc_state.call(null);
var inst_22379 = inst_22406;
var state_22435__$1 = (function (){var statearr_22468 = state_22435;
(statearr_22468[(18)] = inst_22405);

(statearr_22468[(7)] = inst_22379);

return statearr_22468;
})();
var statearr_22469_22508 = state_22435__$1;
(statearr_22469_22508[(2)] = null);

(statearr_22469_22508[(1)] = (5));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22436 === (10))){
var inst_22394 = (state_22435[(8)]);
var inst_22395 = (state_22435[(16)]);
var inst_22393 = (state_22435[(2)]);
var inst_22394__$1 = cljs.core.nth.call(null,inst_22393,(0),null);
var inst_22395__$1 = cljs.core.nth.call(null,inst_22393,(1),null);
var inst_22396 = (inst_22394__$1 == null);
var inst_22397 = cljs.core._EQ_.call(null,inst_22395__$1,change);
var inst_22398 = (inst_22396) || (inst_22397);
var state_22435__$1 = (function (){var statearr_22470 = state_22435;
(statearr_22470[(8)] = inst_22394__$1);

(statearr_22470[(16)] = inst_22395__$1);

return statearr_22470;
})();
if(cljs.core.truth_(inst_22398)){
var statearr_22471_22509 = state_22435__$1;
(statearr_22471_22509[(1)] = (11));

} else {
var statearr_22472_22510 = state_22435__$1;
(statearr_22472_22510[(1)] = (12));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22436 === (18))){
var inst_22389 = (state_22435[(17)]);
var inst_22388 = (state_22435[(15)]);
var inst_22395 = (state_22435[(16)]);
var inst_22412 = cljs.core.empty_QMARK_.call(null,inst_22388);
var inst_22413 = inst_22389.call(null,inst_22395);
var inst_22414 = cljs.core.not.call(null,inst_22413);
var inst_22415 = (inst_22412) && (inst_22414);
var state_22435__$1 = state_22435;
var statearr_22473_22511 = state_22435__$1;
(statearr_22473_22511[(2)] = inst_22415);

(statearr_22473_22511[(1)] = (19));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22436 === (8))){
var inst_22379 = (state_22435[(7)]);
var state_22435__$1 = state_22435;
var statearr_22474_22512 = state_22435__$1;
(statearr_22474_22512[(2)] = inst_22379);

(statearr_22474_22512[(1)] = (9));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
return null;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
});})(c__20932__auto___22482,cs,solo_modes,attrs,solo_mode,change,changed,pick,calc_state,m))
;
return ((function (switch__20870__auto__,c__20932__auto___22482,cs,solo_modes,attrs,solo_mode,change,changed,pick,calc_state,m){
return (function() {
var cljs$core$async$mix_$_state_machine__20871__auto__ = null;
var cljs$core$async$mix_$_state_machine__20871__auto____0 = (function (){
var statearr_22478 = [null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null];
(statearr_22478[(0)] = cljs$core$async$mix_$_state_machine__20871__auto__);

(statearr_22478[(1)] = (1));

return statearr_22478;
});
var cljs$core$async$mix_$_state_machine__20871__auto____1 = (function (state_22435){
while(true){
var ret_value__20872__auto__ = (function (){try{while(true){
var result__20873__auto__ = switch__20870__auto__.call(null,state_22435);
if(cljs.core.keyword_identical_QMARK_.call(null,result__20873__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__20873__auto__;
}
break;
}
}catch (e22479){if((e22479 instanceof Object)){
var ex__20874__auto__ = e22479;
var statearr_22480_22513 = state_22435;
(statearr_22480_22513[(5)] = ex__20874__auto__);


cljs.core.async.impl.ioc_helpers.process_exception.call(null,state_22435);

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
throw e22479;

}
}})();
if(cljs.core.keyword_identical_QMARK_.call(null,ret_value__20872__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__22514 = state_22435;
state_22435 = G__22514;
continue;
} else {
return ret_value__20872__auto__;
}
break;
}
});
cljs$core$async$mix_$_state_machine__20871__auto__ = function(state_22435){
switch(arguments.length){
case 0:
return cljs$core$async$mix_$_state_machine__20871__auto____0.call(this);
case 1:
return cljs$core$async$mix_$_state_machine__20871__auto____1.call(this,state_22435);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$mix_$_state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$mix_$_state_machine__20871__auto____0;
cljs$core$async$mix_$_state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$mix_$_state_machine__20871__auto____1;
return cljs$core$async$mix_$_state_machine__20871__auto__;
})()
;})(switch__20870__auto__,c__20932__auto___22482,cs,solo_modes,attrs,solo_mode,change,changed,pick,calc_state,m))
})();
var state__20934__auto__ = (function (){var statearr_22481 = f__20933__auto__.call(null);
(statearr_22481[cljs.core.async.impl.ioc_helpers.USER_START_IDX] = c__20932__auto___22482);

return statearr_22481;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped.call(null,state__20934__auto__);
});})(c__20932__auto___22482,cs,solo_modes,attrs,solo_mode,change,changed,pick,calc_state,m))
);


return m;
});
/**
 * Adds ch as an input to the mix
 */
cljs.core.async.admix = (function cljs$core$async$admix(mix,ch){
return cljs.core.async.admix_STAR_.call(null,mix,ch);
});
/**
 * Removes ch as an input to the mix
 */
cljs.core.async.unmix = (function cljs$core$async$unmix(mix,ch){
return cljs.core.async.unmix_STAR_.call(null,mix,ch);
});
/**
 * removes all inputs from the mix
 */
cljs.core.async.unmix_all = (function cljs$core$async$unmix_all(mix){
return cljs.core.async.unmix_all_STAR_.call(null,mix);
});
/**
 * Atomically sets the state(s) of one or more channels in a mix. The
 * state map is a map of channels -> channel-state-map. A
 * channel-state-map is a map of attrs -> boolean, where attr is one or
 * more of :mute, :pause or :solo. Any states supplied are merged with
 * the current state.
 * 
 * Note that channels can be added to a mix via toggle, which can be
 * used to add channels in a particular (e.g. paused) state.
 */
cljs.core.async.toggle = (function cljs$core$async$toggle(mix,state_map){
return cljs.core.async.toggle_STAR_.call(null,mix,state_map);
});
/**
 * Sets the solo mode of the mix. mode must be one of :mute or :pause
 */
cljs.core.async.solo_mode = (function cljs$core$async$solo_mode(mix,mode){
return cljs.core.async.solo_mode_STAR_.call(null,mix,mode);
});

cljs.core.async.Pub = (function (){var obj22516 = {};
return obj22516;
})();

cljs.core.async.sub_STAR_ = (function cljs$core$async$sub_STAR_(p,v,ch,close_QMARK_){
if((function (){var and__18061__auto__ = p;
if(and__18061__auto__){
return p.cljs$core$async$Pub$sub_STAR_$arity$4;
} else {
return and__18061__auto__;
}
})()){
return p.cljs$core$async$Pub$sub_STAR_$arity$4(p,v,ch,close_QMARK_);
} else {
var x__18709__auto__ = (((p == null))?null:p);
return (function (){var or__18073__auto__ = (cljs.core.async.sub_STAR_[goog.typeOf(x__18709__auto__)]);
if(or__18073__auto__){
return or__18073__auto__;
} else {
var or__18073__auto____$1 = (cljs.core.async.sub_STAR_["_"]);
if(or__18073__auto____$1){
return or__18073__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"Pub.sub*",p);
}
}
})().call(null,p,v,ch,close_QMARK_);
}
});

cljs.core.async.unsub_STAR_ = (function cljs$core$async$unsub_STAR_(p,v,ch){
if((function (){var and__18061__auto__ = p;
if(and__18061__auto__){
return p.cljs$core$async$Pub$unsub_STAR_$arity$3;
} else {
return and__18061__auto__;
}
})()){
return p.cljs$core$async$Pub$unsub_STAR_$arity$3(p,v,ch);
} else {
var x__18709__auto__ = (((p == null))?null:p);
return (function (){var or__18073__auto__ = (cljs.core.async.unsub_STAR_[goog.typeOf(x__18709__auto__)]);
if(or__18073__auto__){
return or__18073__auto__;
} else {
var or__18073__auto____$1 = (cljs.core.async.unsub_STAR_["_"]);
if(or__18073__auto____$1){
return or__18073__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"Pub.unsub*",p);
}
}
})().call(null,p,v,ch);
}
});

cljs.core.async.unsub_all_STAR_ = (function cljs$core$async$unsub_all_STAR_(){
var G__22518 = arguments.length;
switch (G__22518) {
case 1:
return cljs.core.async.unsub_all_STAR_.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return cljs.core.async.unsub_all_STAR_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

cljs.core.async.unsub_all_STAR_.cljs$core$IFn$_invoke$arity$1 = (function (p){
if((function (){var and__18061__auto__ = p;
if(and__18061__auto__){
return p.cljs$core$async$Pub$unsub_all_STAR_$arity$1;
} else {
return and__18061__auto__;
}
})()){
return p.cljs$core$async$Pub$unsub_all_STAR_$arity$1(p);
} else {
var x__18709__auto__ = (((p == null))?null:p);
return (function (){var or__18073__auto__ = (cljs.core.async.unsub_all_STAR_[goog.typeOf(x__18709__auto__)]);
if(or__18073__auto__){
return or__18073__auto__;
} else {
var or__18073__auto____$1 = (cljs.core.async.unsub_all_STAR_["_"]);
if(or__18073__auto____$1){
return or__18073__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"Pub.unsub-all*",p);
}
}
})().call(null,p);
}
});

cljs.core.async.unsub_all_STAR_.cljs$core$IFn$_invoke$arity$2 = (function (p,v){
if((function (){var and__18061__auto__ = p;
if(and__18061__auto__){
return p.cljs$core$async$Pub$unsub_all_STAR_$arity$2;
} else {
return and__18061__auto__;
}
})()){
return p.cljs$core$async$Pub$unsub_all_STAR_$arity$2(p,v);
} else {
var x__18709__auto__ = (((p == null))?null:p);
return (function (){var or__18073__auto__ = (cljs.core.async.unsub_all_STAR_[goog.typeOf(x__18709__auto__)]);
if(or__18073__auto__){
return or__18073__auto__;
} else {
var or__18073__auto____$1 = (cljs.core.async.unsub_all_STAR_["_"]);
if(or__18073__auto____$1){
return or__18073__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"Pub.unsub-all*",p);
}
}
})().call(null,p,v);
}
});

cljs.core.async.unsub_all_STAR_.cljs$lang$maxFixedArity = 2;

/**
 * Creates and returns a pub(lication) of the supplied channel,
 * partitioned into topics by the topic-fn. topic-fn will be applied to
 * each value on the channel and the result will determine the 'topic'
 * on which that value will be put. Channels can be subscribed to
 * receive copies of topics using 'sub', and unsubscribed using
 * 'unsub'. Each topic will be handled by an internal mult on a
 * dedicated channel. By default these internal channels are
 * unbuffered, but a buf-fn can be supplied which, given a topic,
 * creates a buffer with desired properties.
 * 
 * Each item is distributed to all subs in parallel and synchronously,
 * i.e. each sub must accept before the next item is distributed. Use
 * buffering/windowing to prevent slow subs from holding up the pub.
 * 
 * Items received when there are no matching subs get dropped.
 * 
 * Note that if buf-fns are used then each topic is handled
 * asynchronously, i.e. if a channel is subscribed to more than one
 * topic it should not expect them to be interleaved identically with
 * the source.
 */
cljs.core.async.pub = (function cljs$core$async$pub(){
var G__22522 = arguments.length;
switch (G__22522) {
case 2:
return cljs.core.async.pub.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return cljs.core.async.pub.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

cljs.core.async.pub.cljs$core$IFn$_invoke$arity$2 = (function (ch,topic_fn){
return cljs.core.async.pub.call(null,ch,topic_fn,cljs.core.constantly.call(null,null));
});

cljs.core.async.pub.cljs$core$IFn$_invoke$arity$3 = (function (ch,topic_fn,buf_fn){
var mults = cljs.core.atom.call(null,cljs.core.PersistentArrayMap.EMPTY);
var ensure_mult = ((function (mults){
return (function (topic){
var or__18073__auto__ = cljs.core.get.call(null,cljs.core.deref.call(null,mults),topic);
if(cljs.core.truth_(or__18073__auto__)){
return or__18073__auto__;
} else {
return cljs.core.get.call(null,cljs.core.swap_BANG_.call(null,mults,((function (or__18073__auto__,mults){
return (function (p1__22520_SHARP_){
if(cljs.core.truth_(p1__22520_SHARP_.call(null,topic))){
return p1__22520_SHARP_;
} else {
return cljs.core.assoc.call(null,p1__22520_SHARP_,topic,cljs.core.async.mult.call(null,cljs.core.async.chan.call(null,buf_fn.call(null,topic))));
}
});})(or__18073__auto__,mults))
),topic);
}
});})(mults))
;
var p = (function (){
if(typeof cljs.core.async.t22523 !== 'undefined'){
} else {

/**
* @constructor
*/
cljs.core.async.t22523 = (function (ch,topic_fn,buf_fn,mults,ensure_mult,meta22524){
this.ch = ch;
this.topic_fn = topic_fn;
this.buf_fn = buf_fn;
this.mults = mults;
this.ensure_mult = ensure_mult;
this.meta22524 = meta22524;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
})
cljs.core.async.t22523.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = ((function (mults,ensure_mult){
return (function (_22525,meta22524__$1){
var self__ = this;
var _22525__$1 = this;
return (new cljs.core.async.t22523(self__.ch,self__.topic_fn,self__.buf_fn,self__.mults,self__.ensure_mult,meta22524__$1));
});})(mults,ensure_mult))
;

cljs.core.async.t22523.prototype.cljs$core$IMeta$_meta$arity$1 = ((function (mults,ensure_mult){
return (function (_22525){
var self__ = this;
var _22525__$1 = this;
return self__.meta22524;
});})(mults,ensure_mult))
;

cljs.core.async.t22523.prototype.cljs$core$async$Mux$ = true;

cljs.core.async.t22523.prototype.cljs$core$async$Mux$muxch_STAR_$arity$1 = ((function (mults,ensure_mult){
return (function (_){
var self__ = this;
var ___$1 = this;
return self__.ch;
});})(mults,ensure_mult))
;

cljs.core.async.t22523.prototype.cljs$core$async$Pub$ = true;

cljs.core.async.t22523.prototype.cljs$core$async$Pub$sub_STAR_$arity$4 = ((function (mults,ensure_mult){
return (function (p,topic,ch__$1,close_QMARK_){
var self__ = this;
var p__$1 = this;
var m = self__.ensure_mult.call(null,topic);
return cljs.core.async.tap.call(null,m,ch__$1,close_QMARK_);
});})(mults,ensure_mult))
;

cljs.core.async.t22523.prototype.cljs$core$async$Pub$unsub_STAR_$arity$3 = ((function (mults,ensure_mult){
return (function (p,topic,ch__$1){
var self__ = this;
var p__$1 = this;
var temp__4423__auto__ = cljs.core.get.call(null,cljs.core.deref.call(null,self__.mults),topic);
if(cljs.core.truth_(temp__4423__auto__)){
var m = temp__4423__auto__;
return cljs.core.async.untap.call(null,m,ch__$1);
} else {
return null;
}
});})(mults,ensure_mult))
;

cljs.core.async.t22523.prototype.cljs$core$async$Pub$unsub_all_STAR_$arity$1 = ((function (mults,ensure_mult){
return (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.reset_BANG_.call(null,self__.mults,cljs.core.PersistentArrayMap.EMPTY);
});})(mults,ensure_mult))
;

cljs.core.async.t22523.prototype.cljs$core$async$Pub$unsub_all_STAR_$arity$2 = ((function (mults,ensure_mult){
return (function (_,topic){
var self__ = this;
var ___$1 = this;
return cljs.core.swap_BANG_.call(null,self__.mults,cljs.core.dissoc,topic);
});})(mults,ensure_mult))
;

cljs.core.async.t22523.getBasis = ((function (mults,ensure_mult){
return (function (){
return new cljs.core.PersistentVector(null, 6, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"ch","ch",1085813622,null),new cljs.core.Symbol(null,"topic-fn","topic-fn",-862449736,null),new cljs.core.Symbol(null,"buf-fn","buf-fn",-1200281591,null),new cljs.core.Symbol(null,"mults","mults",-461114485,null),new cljs.core.Symbol(null,"ensure-mult","ensure-mult",1796584816,null),new cljs.core.Symbol(null,"meta22524","meta22524",-176543773,null)], null);
});})(mults,ensure_mult))
;

cljs.core.async.t22523.cljs$lang$type = true;

cljs.core.async.t22523.cljs$lang$ctorStr = "cljs.core.async/t22523";

cljs.core.async.t22523.cljs$lang$ctorPrWriter = ((function (mults,ensure_mult){
return (function (this__18652__auto__,writer__18653__auto__,opt__18654__auto__){
return cljs.core._write.call(null,writer__18653__auto__,"cljs.core.async/t22523");
});})(mults,ensure_mult))
;

cljs.core.async.__GT_t22523 = ((function (mults,ensure_mult){
return (function cljs$core$async$__GT_t22523(ch__$1,topic_fn__$1,buf_fn__$1,mults__$1,ensure_mult__$1,meta22524){
return (new cljs.core.async.t22523(ch__$1,topic_fn__$1,buf_fn__$1,mults__$1,ensure_mult__$1,meta22524));
});})(mults,ensure_mult))
;

}

return (new cljs.core.async.t22523(ch,topic_fn,buf_fn,mults,ensure_mult,cljs.core.PersistentArrayMap.EMPTY));
})()
;
var c__20932__auto___22646 = cljs.core.async.chan.call(null,(1));
cljs.core.async.impl.dispatch.run.call(null,((function (c__20932__auto___22646,mults,ensure_mult,p){
return (function (){
var f__20933__auto__ = (function (){var switch__20870__auto__ = ((function (c__20932__auto___22646,mults,ensure_mult,p){
return (function (state_22597){
var state_val_22598 = (state_22597[(1)]);
if((state_val_22598 === (7))){
var inst_22593 = (state_22597[(2)]);
var state_22597__$1 = state_22597;
var statearr_22599_22647 = state_22597__$1;
(statearr_22599_22647[(2)] = inst_22593);

(statearr_22599_22647[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22598 === (20))){
var state_22597__$1 = state_22597;
var statearr_22600_22648 = state_22597__$1;
(statearr_22600_22648[(2)] = null);

(statearr_22600_22648[(1)] = (21));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22598 === (1))){
var state_22597__$1 = state_22597;
var statearr_22601_22649 = state_22597__$1;
(statearr_22601_22649[(2)] = null);

(statearr_22601_22649[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22598 === (24))){
var inst_22576 = (state_22597[(7)]);
var inst_22585 = cljs.core.swap_BANG_.call(null,mults,cljs.core.dissoc,inst_22576);
var state_22597__$1 = state_22597;
var statearr_22602_22650 = state_22597__$1;
(statearr_22602_22650[(2)] = inst_22585);

(statearr_22602_22650[(1)] = (25));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22598 === (4))){
var inst_22528 = (state_22597[(8)]);
var inst_22528__$1 = (state_22597[(2)]);
var inst_22529 = (inst_22528__$1 == null);
var state_22597__$1 = (function (){var statearr_22603 = state_22597;
(statearr_22603[(8)] = inst_22528__$1);

return statearr_22603;
})();
if(cljs.core.truth_(inst_22529)){
var statearr_22604_22651 = state_22597__$1;
(statearr_22604_22651[(1)] = (5));

} else {
var statearr_22605_22652 = state_22597__$1;
(statearr_22605_22652[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22598 === (15))){
var inst_22570 = (state_22597[(2)]);
var state_22597__$1 = state_22597;
var statearr_22606_22653 = state_22597__$1;
(statearr_22606_22653[(2)] = inst_22570);

(statearr_22606_22653[(1)] = (12));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22598 === (21))){
var inst_22590 = (state_22597[(2)]);
var state_22597__$1 = (function (){var statearr_22607 = state_22597;
(statearr_22607[(9)] = inst_22590);

return statearr_22607;
})();
var statearr_22608_22654 = state_22597__$1;
(statearr_22608_22654[(2)] = null);

(statearr_22608_22654[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22598 === (13))){
var inst_22552 = (state_22597[(10)]);
var inst_22554 = cljs.core.chunked_seq_QMARK_.call(null,inst_22552);
var state_22597__$1 = state_22597;
if(inst_22554){
var statearr_22609_22655 = state_22597__$1;
(statearr_22609_22655[(1)] = (16));

} else {
var statearr_22610_22656 = state_22597__$1;
(statearr_22610_22656[(1)] = (17));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22598 === (22))){
var inst_22582 = (state_22597[(2)]);
var state_22597__$1 = state_22597;
if(cljs.core.truth_(inst_22582)){
var statearr_22611_22657 = state_22597__$1;
(statearr_22611_22657[(1)] = (23));

} else {
var statearr_22612_22658 = state_22597__$1;
(statearr_22612_22658[(1)] = (24));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22598 === (6))){
var inst_22578 = (state_22597[(11)]);
var inst_22576 = (state_22597[(7)]);
var inst_22528 = (state_22597[(8)]);
var inst_22576__$1 = topic_fn.call(null,inst_22528);
var inst_22577 = cljs.core.deref.call(null,mults);
var inst_22578__$1 = cljs.core.get.call(null,inst_22577,inst_22576__$1);
var state_22597__$1 = (function (){var statearr_22613 = state_22597;
(statearr_22613[(11)] = inst_22578__$1);

(statearr_22613[(7)] = inst_22576__$1);

return statearr_22613;
})();
if(cljs.core.truth_(inst_22578__$1)){
var statearr_22614_22659 = state_22597__$1;
(statearr_22614_22659[(1)] = (19));

} else {
var statearr_22615_22660 = state_22597__$1;
(statearr_22615_22660[(1)] = (20));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22598 === (25))){
var inst_22587 = (state_22597[(2)]);
var state_22597__$1 = state_22597;
var statearr_22616_22661 = state_22597__$1;
(statearr_22616_22661[(2)] = inst_22587);

(statearr_22616_22661[(1)] = (21));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22598 === (17))){
var inst_22552 = (state_22597[(10)]);
var inst_22561 = cljs.core.first.call(null,inst_22552);
var inst_22562 = cljs.core.async.muxch_STAR_.call(null,inst_22561);
var inst_22563 = cljs.core.async.close_BANG_.call(null,inst_22562);
var inst_22564 = cljs.core.next.call(null,inst_22552);
var inst_22538 = inst_22564;
var inst_22539 = null;
var inst_22540 = (0);
var inst_22541 = (0);
var state_22597__$1 = (function (){var statearr_22617 = state_22597;
(statearr_22617[(12)] = inst_22563);

(statearr_22617[(13)] = inst_22539);

(statearr_22617[(14)] = inst_22541);

(statearr_22617[(15)] = inst_22540);

(statearr_22617[(16)] = inst_22538);

return statearr_22617;
})();
var statearr_22618_22662 = state_22597__$1;
(statearr_22618_22662[(2)] = null);

(statearr_22618_22662[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22598 === (3))){
var inst_22595 = (state_22597[(2)]);
var state_22597__$1 = state_22597;
return cljs.core.async.impl.ioc_helpers.return_chan.call(null,state_22597__$1,inst_22595);
} else {
if((state_val_22598 === (12))){
var inst_22572 = (state_22597[(2)]);
var state_22597__$1 = state_22597;
var statearr_22619_22663 = state_22597__$1;
(statearr_22619_22663[(2)] = inst_22572);

(statearr_22619_22663[(1)] = (9));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22598 === (2))){
var state_22597__$1 = state_22597;
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_22597__$1,(4),ch);
} else {
if((state_val_22598 === (23))){
var state_22597__$1 = state_22597;
var statearr_22620_22664 = state_22597__$1;
(statearr_22620_22664[(2)] = null);

(statearr_22620_22664[(1)] = (25));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22598 === (19))){
var inst_22578 = (state_22597[(11)]);
var inst_22528 = (state_22597[(8)]);
var inst_22580 = cljs.core.async.muxch_STAR_.call(null,inst_22578);
var state_22597__$1 = state_22597;
return cljs.core.async.impl.ioc_helpers.put_BANG_.call(null,state_22597__$1,(22),inst_22580,inst_22528);
} else {
if((state_val_22598 === (11))){
var inst_22552 = (state_22597[(10)]);
var inst_22538 = (state_22597[(16)]);
var inst_22552__$1 = cljs.core.seq.call(null,inst_22538);
var state_22597__$1 = (function (){var statearr_22621 = state_22597;
(statearr_22621[(10)] = inst_22552__$1);

return statearr_22621;
})();
if(inst_22552__$1){
var statearr_22622_22665 = state_22597__$1;
(statearr_22622_22665[(1)] = (13));

} else {
var statearr_22623_22666 = state_22597__$1;
(statearr_22623_22666[(1)] = (14));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22598 === (9))){
var inst_22574 = (state_22597[(2)]);
var state_22597__$1 = state_22597;
var statearr_22624_22667 = state_22597__$1;
(statearr_22624_22667[(2)] = inst_22574);

(statearr_22624_22667[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22598 === (5))){
var inst_22535 = cljs.core.deref.call(null,mults);
var inst_22536 = cljs.core.vals.call(null,inst_22535);
var inst_22537 = cljs.core.seq.call(null,inst_22536);
var inst_22538 = inst_22537;
var inst_22539 = null;
var inst_22540 = (0);
var inst_22541 = (0);
var state_22597__$1 = (function (){var statearr_22625 = state_22597;
(statearr_22625[(13)] = inst_22539);

(statearr_22625[(14)] = inst_22541);

(statearr_22625[(15)] = inst_22540);

(statearr_22625[(16)] = inst_22538);

return statearr_22625;
})();
var statearr_22626_22668 = state_22597__$1;
(statearr_22626_22668[(2)] = null);

(statearr_22626_22668[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22598 === (14))){
var state_22597__$1 = state_22597;
var statearr_22630_22669 = state_22597__$1;
(statearr_22630_22669[(2)] = null);

(statearr_22630_22669[(1)] = (15));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22598 === (16))){
var inst_22552 = (state_22597[(10)]);
var inst_22556 = cljs.core.chunk_first.call(null,inst_22552);
var inst_22557 = cljs.core.chunk_rest.call(null,inst_22552);
var inst_22558 = cljs.core.count.call(null,inst_22556);
var inst_22538 = inst_22557;
var inst_22539 = inst_22556;
var inst_22540 = inst_22558;
var inst_22541 = (0);
var state_22597__$1 = (function (){var statearr_22631 = state_22597;
(statearr_22631[(13)] = inst_22539);

(statearr_22631[(14)] = inst_22541);

(statearr_22631[(15)] = inst_22540);

(statearr_22631[(16)] = inst_22538);

return statearr_22631;
})();
var statearr_22632_22670 = state_22597__$1;
(statearr_22632_22670[(2)] = null);

(statearr_22632_22670[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22598 === (10))){
var inst_22539 = (state_22597[(13)]);
var inst_22541 = (state_22597[(14)]);
var inst_22540 = (state_22597[(15)]);
var inst_22538 = (state_22597[(16)]);
var inst_22546 = cljs.core._nth.call(null,inst_22539,inst_22541);
var inst_22547 = cljs.core.async.muxch_STAR_.call(null,inst_22546);
var inst_22548 = cljs.core.async.close_BANG_.call(null,inst_22547);
var inst_22549 = (inst_22541 + (1));
var tmp22627 = inst_22539;
var tmp22628 = inst_22540;
var tmp22629 = inst_22538;
var inst_22538__$1 = tmp22629;
var inst_22539__$1 = tmp22627;
var inst_22540__$1 = tmp22628;
var inst_22541__$1 = inst_22549;
var state_22597__$1 = (function (){var statearr_22633 = state_22597;
(statearr_22633[(13)] = inst_22539__$1);

(statearr_22633[(14)] = inst_22541__$1);

(statearr_22633[(15)] = inst_22540__$1);

(statearr_22633[(17)] = inst_22548);

(statearr_22633[(16)] = inst_22538__$1);

return statearr_22633;
})();
var statearr_22634_22671 = state_22597__$1;
(statearr_22634_22671[(2)] = null);

(statearr_22634_22671[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22598 === (18))){
var inst_22567 = (state_22597[(2)]);
var state_22597__$1 = state_22597;
var statearr_22635_22672 = state_22597__$1;
(statearr_22635_22672[(2)] = inst_22567);

(statearr_22635_22672[(1)] = (15));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22598 === (8))){
var inst_22541 = (state_22597[(14)]);
var inst_22540 = (state_22597[(15)]);
var inst_22543 = (inst_22541 < inst_22540);
var inst_22544 = inst_22543;
var state_22597__$1 = state_22597;
if(cljs.core.truth_(inst_22544)){
var statearr_22636_22673 = state_22597__$1;
(statearr_22636_22673[(1)] = (10));

} else {
var statearr_22637_22674 = state_22597__$1;
(statearr_22637_22674[(1)] = (11));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
return null;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
});})(c__20932__auto___22646,mults,ensure_mult,p))
;
return ((function (switch__20870__auto__,c__20932__auto___22646,mults,ensure_mult,p){
return (function() {
var cljs$core$async$state_machine__20871__auto__ = null;
var cljs$core$async$state_machine__20871__auto____0 = (function (){
var statearr_22641 = [null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null];
(statearr_22641[(0)] = cljs$core$async$state_machine__20871__auto__);

(statearr_22641[(1)] = (1));

return statearr_22641;
});
var cljs$core$async$state_machine__20871__auto____1 = (function (state_22597){
while(true){
var ret_value__20872__auto__ = (function (){try{while(true){
var result__20873__auto__ = switch__20870__auto__.call(null,state_22597);
if(cljs.core.keyword_identical_QMARK_.call(null,result__20873__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__20873__auto__;
}
break;
}
}catch (e22642){if((e22642 instanceof Object)){
var ex__20874__auto__ = e22642;
var statearr_22643_22675 = state_22597;
(statearr_22643_22675[(5)] = ex__20874__auto__);


cljs.core.async.impl.ioc_helpers.process_exception.call(null,state_22597);

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
throw e22642;

}
}})();
if(cljs.core.keyword_identical_QMARK_.call(null,ret_value__20872__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__22676 = state_22597;
state_22597 = G__22676;
continue;
} else {
return ret_value__20872__auto__;
}
break;
}
});
cljs$core$async$state_machine__20871__auto__ = function(state_22597){
switch(arguments.length){
case 0:
return cljs$core$async$state_machine__20871__auto____0.call(this);
case 1:
return cljs$core$async$state_machine__20871__auto____1.call(this,state_22597);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$state_machine__20871__auto____0;
cljs$core$async$state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$state_machine__20871__auto____1;
return cljs$core$async$state_machine__20871__auto__;
})()
;})(switch__20870__auto__,c__20932__auto___22646,mults,ensure_mult,p))
})();
var state__20934__auto__ = (function (){var statearr_22644 = f__20933__auto__.call(null);
(statearr_22644[cljs.core.async.impl.ioc_helpers.USER_START_IDX] = c__20932__auto___22646);

return statearr_22644;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped.call(null,state__20934__auto__);
});})(c__20932__auto___22646,mults,ensure_mult,p))
);


return p;
});

cljs.core.async.pub.cljs$lang$maxFixedArity = 3;
/**
 * Subscribes a channel to a topic of a pub.
 * 
 * By default the channel will be closed when the source closes,
 * but can be determined by the close? parameter.
 */
cljs.core.async.sub = (function cljs$core$async$sub(){
var G__22678 = arguments.length;
switch (G__22678) {
case 3:
return cljs.core.async.sub.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
case 4:
return cljs.core.async.sub.cljs$core$IFn$_invoke$arity$4((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),(arguments[(3)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

cljs.core.async.sub.cljs$core$IFn$_invoke$arity$3 = (function (p,topic,ch){
return cljs.core.async.sub.call(null,p,topic,ch,true);
});

cljs.core.async.sub.cljs$core$IFn$_invoke$arity$4 = (function (p,topic,ch,close_QMARK_){
return cljs.core.async.sub_STAR_.call(null,p,topic,ch,close_QMARK_);
});

cljs.core.async.sub.cljs$lang$maxFixedArity = 4;
/**
 * Unsubscribes a channel from a topic of a pub
 */
cljs.core.async.unsub = (function cljs$core$async$unsub(p,topic,ch){
return cljs.core.async.unsub_STAR_.call(null,p,topic,ch);
});
/**
 * Unsubscribes all channels from a pub, or a topic of a pub
 */
cljs.core.async.unsub_all = (function cljs$core$async$unsub_all(){
var G__22681 = arguments.length;
switch (G__22681) {
case 1:
return cljs.core.async.unsub_all.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return cljs.core.async.unsub_all.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

cljs.core.async.unsub_all.cljs$core$IFn$_invoke$arity$1 = (function (p){
return cljs.core.async.unsub_all_STAR_.call(null,p);
});

cljs.core.async.unsub_all.cljs$core$IFn$_invoke$arity$2 = (function (p,topic){
return cljs.core.async.unsub_all_STAR_.call(null,p,topic);
});

cljs.core.async.unsub_all.cljs$lang$maxFixedArity = 2;
/**
 * Takes a function and a collection of source channels, and returns a
 * channel which contains the values produced by applying f to the set
 * of first items taken from each source channel, followed by applying
 * f to the set of second items from each channel, until any one of the
 * channels is closed, at which point the output channel will be
 * closed. The returned channel will be unbuffered by default, or a
 * buf-or-n can be supplied
 */
cljs.core.async.map = (function cljs$core$async$map(){
var G__22684 = arguments.length;
switch (G__22684) {
case 2:
return cljs.core.async.map.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return cljs.core.async.map.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

cljs.core.async.map.cljs$core$IFn$_invoke$arity$2 = (function (f,chs){
return cljs.core.async.map.call(null,f,chs,null);
});

cljs.core.async.map.cljs$core$IFn$_invoke$arity$3 = (function (f,chs,buf_or_n){
var chs__$1 = cljs.core.vec.call(null,chs);
var out = cljs.core.async.chan.call(null,buf_or_n);
var cnt = cljs.core.count.call(null,chs__$1);
var rets = cljs.core.object_array.call(null,cnt);
var dchan = cljs.core.async.chan.call(null,(1));
var dctr = cljs.core.atom.call(null,null);
var done = cljs.core.mapv.call(null,((function (chs__$1,out,cnt,rets,dchan,dctr){
return (function (i){
return ((function (chs__$1,out,cnt,rets,dchan,dctr){
return (function (ret){
(rets[i] = ret);

if((cljs.core.swap_BANG_.call(null,dctr,cljs.core.dec) === (0))){
return cljs.core.async.put_BANG_.call(null,dchan,rets.slice((0)));
} else {
return null;
}
});
;})(chs__$1,out,cnt,rets,dchan,dctr))
});})(chs__$1,out,cnt,rets,dchan,dctr))
,cljs.core.range.call(null,cnt));
var c__20932__auto___22754 = cljs.core.async.chan.call(null,(1));
cljs.core.async.impl.dispatch.run.call(null,((function (c__20932__auto___22754,chs__$1,out,cnt,rets,dchan,dctr,done){
return (function (){
var f__20933__auto__ = (function (){var switch__20870__auto__ = ((function (c__20932__auto___22754,chs__$1,out,cnt,rets,dchan,dctr,done){
return (function (state_22723){
var state_val_22724 = (state_22723[(1)]);
if((state_val_22724 === (7))){
var state_22723__$1 = state_22723;
var statearr_22725_22755 = state_22723__$1;
(statearr_22725_22755[(2)] = null);

(statearr_22725_22755[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22724 === (1))){
var state_22723__$1 = state_22723;
var statearr_22726_22756 = state_22723__$1;
(statearr_22726_22756[(2)] = null);

(statearr_22726_22756[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22724 === (4))){
var inst_22687 = (state_22723[(7)]);
var inst_22689 = (inst_22687 < cnt);
var state_22723__$1 = state_22723;
if(cljs.core.truth_(inst_22689)){
var statearr_22727_22757 = state_22723__$1;
(statearr_22727_22757[(1)] = (6));

} else {
var statearr_22728_22758 = state_22723__$1;
(statearr_22728_22758[(1)] = (7));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22724 === (15))){
var inst_22719 = (state_22723[(2)]);
var state_22723__$1 = state_22723;
var statearr_22729_22759 = state_22723__$1;
(statearr_22729_22759[(2)] = inst_22719);

(statearr_22729_22759[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22724 === (13))){
var inst_22712 = cljs.core.async.close_BANG_.call(null,out);
var state_22723__$1 = state_22723;
var statearr_22730_22760 = state_22723__$1;
(statearr_22730_22760[(2)] = inst_22712);

(statearr_22730_22760[(1)] = (15));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22724 === (6))){
var state_22723__$1 = state_22723;
var statearr_22731_22761 = state_22723__$1;
(statearr_22731_22761[(2)] = null);

(statearr_22731_22761[(1)] = (11));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22724 === (3))){
var inst_22721 = (state_22723[(2)]);
var state_22723__$1 = state_22723;
return cljs.core.async.impl.ioc_helpers.return_chan.call(null,state_22723__$1,inst_22721);
} else {
if((state_val_22724 === (12))){
var inst_22709 = (state_22723[(8)]);
var inst_22709__$1 = (state_22723[(2)]);
var inst_22710 = cljs.core.some.call(null,cljs.core.nil_QMARK_,inst_22709__$1);
var state_22723__$1 = (function (){var statearr_22732 = state_22723;
(statearr_22732[(8)] = inst_22709__$1);

return statearr_22732;
})();
if(cljs.core.truth_(inst_22710)){
var statearr_22733_22762 = state_22723__$1;
(statearr_22733_22762[(1)] = (13));

} else {
var statearr_22734_22763 = state_22723__$1;
(statearr_22734_22763[(1)] = (14));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22724 === (2))){
var inst_22686 = cljs.core.reset_BANG_.call(null,dctr,cnt);
var inst_22687 = (0);
var state_22723__$1 = (function (){var statearr_22735 = state_22723;
(statearr_22735[(9)] = inst_22686);

(statearr_22735[(7)] = inst_22687);

return statearr_22735;
})();
var statearr_22736_22764 = state_22723__$1;
(statearr_22736_22764[(2)] = null);

(statearr_22736_22764[(1)] = (4));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22724 === (11))){
var inst_22687 = (state_22723[(7)]);
var _ = cljs.core.async.impl.ioc_helpers.add_exception_frame.call(null,state_22723,(10),Object,null,(9));
var inst_22696 = chs__$1.call(null,inst_22687);
var inst_22697 = done.call(null,inst_22687);
var inst_22698 = cljs.core.async.take_BANG_.call(null,inst_22696,inst_22697);
var state_22723__$1 = state_22723;
var statearr_22737_22765 = state_22723__$1;
(statearr_22737_22765[(2)] = inst_22698);


cljs.core.async.impl.ioc_helpers.process_exception.call(null,state_22723__$1);

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22724 === (9))){
var inst_22687 = (state_22723[(7)]);
var inst_22700 = (state_22723[(2)]);
var inst_22701 = (inst_22687 + (1));
var inst_22687__$1 = inst_22701;
var state_22723__$1 = (function (){var statearr_22738 = state_22723;
(statearr_22738[(7)] = inst_22687__$1);

(statearr_22738[(10)] = inst_22700);

return statearr_22738;
})();
var statearr_22739_22766 = state_22723__$1;
(statearr_22739_22766[(2)] = null);

(statearr_22739_22766[(1)] = (4));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22724 === (5))){
var inst_22707 = (state_22723[(2)]);
var state_22723__$1 = (function (){var statearr_22740 = state_22723;
(statearr_22740[(11)] = inst_22707);

return statearr_22740;
})();
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_22723__$1,(12),dchan);
} else {
if((state_val_22724 === (14))){
var inst_22709 = (state_22723[(8)]);
var inst_22714 = cljs.core.apply.call(null,f,inst_22709);
var state_22723__$1 = state_22723;
return cljs.core.async.impl.ioc_helpers.put_BANG_.call(null,state_22723__$1,(16),out,inst_22714);
} else {
if((state_val_22724 === (16))){
var inst_22716 = (state_22723[(2)]);
var state_22723__$1 = (function (){var statearr_22741 = state_22723;
(statearr_22741[(12)] = inst_22716);

return statearr_22741;
})();
var statearr_22742_22767 = state_22723__$1;
(statearr_22742_22767[(2)] = null);

(statearr_22742_22767[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22724 === (10))){
var inst_22691 = (state_22723[(2)]);
var inst_22692 = cljs.core.swap_BANG_.call(null,dctr,cljs.core.dec);
var state_22723__$1 = (function (){var statearr_22743 = state_22723;
(statearr_22743[(13)] = inst_22691);

return statearr_22743;
})();
var statearr_22744_22768 = state_22723__$1;
(statearr_22744_22768[(2)] = inst_22692);


cljs.core.async.impl.ioc_helpers.process_exception.call(null,state_22723__$1);

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22724 === (8))){
var inst_22705 = (state_22723[(2)]);
var state_22723__$1 = state_22723;
var statearr_22745_22769 = state_22723__$1;
(statearr_22745_22769[(2)] = inst_22705);

(statearr_22745_22769[(1)] = (5));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
return null;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
});})(c__20932__auto___22754,chs__$1,out,cnt,rets,dchan,dctr,done))
;
return ((function (switch__20870__auto__,c__20932__auto___22754,chs__$1,out,cnt,rets,dchan,dctr,done){
return (function() {
var cljs$core$async$state_machine__20871__auto__ = null;
var cljs$core$async$state_machine__20871__auto____0 = (function (){
var statearr_22749 = [null,null,null,null,null,null,null,null,null,null,null,null,null,null];
(statearr_22749[(0)] = cljs$core$async$state_machine__20871__auto__);

(statearr_22749[(1)] = (1));

return statearr_22749;
});
var cljs$core$async$state_machine__20871__auto____1 = (function (state_22723){
while(true){
var ret_value__20872__auto__ = (function (){try{while(true){
var result__20873__auto__ = switch__20870__auto__.call(null,state_22723);
if(cljs.core.keyword_identical_QMARK_.call(null,result__20873__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__20873__auto__;
}
break;
}
}catch (e22750){if((e22750 instanceof Object)){
var ex__20874__auto__ = e22750;
var statearr_22751_22770 = state_22723;
(statearr_22751_22770[(5)] = ex__20874__auto__);


cljs.core.async.impl.ioc_helpers.process_exception.call(null,state_22723);

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
throw e22750;

}
}})();
if(cljs.core.keyword_identical_QMARK_.call(null,ret_value__20872__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__22771 = state_22723;
state_22723 = G__22771;
continue;
} else {
return ret_value__20872__auto__;
}
break;
}
});
cljs$core$async$state_machine__20871__auto__ = function(state_22723){
switch(arguments.length){
case 0:
return cljs$core$async$state_machine__20871__auto____0.call(this);
case 1:
return cljs$core$async$state_machine__20871__auto____1.call(this,state_22723);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$state_machine__20871__auto____0;
cljs$core$async$state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$state_machine__20871__auto____1;
return cljs$core$async$state_machine__20871__auto__;
})()
;})(switch__20870__auto__,c__20932__auto___22754,chs__$1,out,cnt,rets,dchan,dctr,done))
})();
var state__20934__auto__ = (function (){var statearr_22752 = f__20933__auto__.call(null);
(statearr_22752[cljs.core.async.impl.ioc_helpers.USER_START_IDX] = c__20932__auto___22754);

return statearr_22752;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped.call(null,state__20934__auto__);
});})(c__20932__auto___22754,chs__$1,out,cnt,rets,dchan,dctr,done))
);


return out;
});

cljs.core.async.map.cljs$lang$maxFixedArity = 3;
/**
 * Takes a collection of source channels and returns a channel which
 * contains all values taken from them. The returned channel will be
 * unbuffered by default, or a buf-or-n can be supplied. The channel
 * will close after all the source channels have closed.
 */
cljs.core.async.merge = (function cljs$core$async$merge(){
var G__22774 = arguments.length;
switch (G__22774) {
case 1:
return cljs.core.async.merge.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return cljs.core.async.merge.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

cljs.core.async.merge.cljs$core$IFn$_invoke$arity$1 = (function (chs){
return cljs.core.async.merge.call(null,chs,null);
});

cljs.core.async.merge.cljs$core$IFn$_invoke$arity$2 = (function (chs,buf_or_n){
var out = cljs.core.async.chan.call(null,buf_or_n);
var c__20932__auto___22829 = cljs.core.async.chan.call(null,(1));
cljs.core.async.impl.dispatch.run.call(null,((function (c__20932__auto___22829,out){
return (function (){
var f__20933__auto__ = (function (){var switch__20870__auto__ = ((function (c__20932__auto___22829,out){
return (function (state_22804){
var state_val_22805 = (state_22804[(1)]);
if((state_val_22805 === (7))){
var inst_22783 = (state_22804[(7)]);
var inst_22784 = (state_22804[(8)]);
var inst_22783__$1 = (state_22804[(2)]);
var inst_22784__$1 = cljs.core.nth.call(null,inst_22783__$1,(0),null);
var inst_22785 = cljs.core.nth.call(null,inst_22783__$1,(1),null);
var inst_22786 = (inst_22784__$1 == null);
var state_22804__$1 = (function (){var statearr_22806 = state_22804;
(statearr_22806[(7)] = inst_22783__$1);

(statearr_22806[(8)] = inst_22784__$1);

(statearr_22806[(9)] = inst_22785);

return statearr_22806;
})();
if(cljs.core.truth_(inst_22786)){
var statearr_22807_22830 = state_22804__$1;
(statearr_22807_22830[(1)] = (8));

} else {
var statearr_22808_22831 = state_22804__$1;
(statearr_22808_22831[(1)] = (9));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22805 === (1))){
var inst_22775 = cljs.core.vec.call(null,chs);
var inst_22776 = inst_22775;
var state_22804__$1 = (function (){var statearr_22809 = state_22804;
(statearr_22809[(10)] = inst_22776);

return statearr_22809;
})();
var statearr_22810_22832 = state_22804__$1;
(statearr_22810_22832[(2)] = null);

(statearr_22810_22832[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22805 === (4))){
var inst_22776 = (state_22804[(10)]);
var state_22804__$1 = state_22804;
return cljs.core.async.ioc_alts_BANG_.call(null,state_22804__$1,(7),inst_22776);
} else {
if((state_val_22805 === (6))){
var inst_22800 = (state_22804[(2)]);
var state_22804__$1 = state_22804;
var statearr_22811_22833 = state_22804__$1;
(statearr_22811_22833[(2)] = inst_22800);

(statearr_22811_22833[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22805 === (3))){
var inst_22802 = (state_22804[(2)]);
var state_22804__$1 = state_22804;
return cljs.core.async.impl.ioc_helpers.return_chan.call(null,state_22804__$1,inst_22802);
} else {
if((state_val_22805 === (2))){
var inst_22776 = (state_22804[(10)]);
var inst_22778 = cljs.core.count.call(null,inst_22776);
var inst_22779 = (inst_22778 > (0));
var state_22804__$1 = state_22804;
if(cljs.core.truth_(inst_22779)){
var statearr_22813_22834 = state_22804__$1;
(statearr_22813_22834[(1)] = (4));

} else {
var statearr_22814_22835 = state_22804__$1;
(statearr_22814_22835[(1)] = (5));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22805 === (11))){
var inst_22776 = (state_22804[(10)]);
var inst_22793 = (state_22804[(2)]);
var tmp22812 = inst_22776;
var inst_22776__$1 = tmp22812;
var state_22804__$1 = (function (){var statearr_22815 = state_22804;
(statearr_22815[(11)] = inst_22793);

(statearr_22815[(10)] = inst_22776__$1);

return statearr_22815;
})();
var statearr_22816_22836 = state_22804__$1;
(statearr_22816_22836[(2)] = null);

(statearr_22816_22836[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22805 === (9))){
var inst_22784 = (state_22804[(8)]);
var state_22804__$1 = state_22804;
return cljs.core.async.impl.ioc_helpers.put_BANG_.call(null,state_22804__$1,(11),out,inst_22784);
} else {
if((state_val_22805 === (5))){
var inst_22798 = cljs.core.async.close_BANG_.call(null,out);
var state_22804__$1 = state_22804;
var statearr_22817_22837 = state_22804__$1;
(statearr_22817_22837[(2)] = inst_22798);

(statearr_22817_22837[(1)] = (6));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22805 === (10))){
var inst_22796 = (state_22804[(2)]);
var state_22804__$1 = state_22804;
var statearr_22818_22838 = state_22804__$1;
(statearr_22818_22838[(2)] = inst_22796);

(statearr_22818_22838[(1)] = (6));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22805 === (8))){
var inst_22783 = (state_22804[(7)]);
var inst_22784 = (state_22804[(8)]);
var inst_22776 = (state_22804[(10)]);
var inst_22785 = (state_22804[(9)]);
var inst_22788 = (function (){var cs = inst_22776;
var vec__22781 = inst_22783;
var v = inst_22784;
var c = inst_22785;
return ((function (cs,vec__22781,v,c,inst_22783,inst_22784,inst_22776,inst_22785,state_val_22805,c__20932__auto___22829,out){
return (function (p1__22772_SHARP_){
return cljs.core.not_EQ_.call(null,c,p1__22772_SHARP_);
});
;})(cs,vec__22781,v,c,inst_22783,inst_22784,inst_22776,inst_22785,state_val_22805,c__20932__auto___22829,out))
})();
var inst_22789 = cljs.core.filterv.call(null,inst_22788,inst_22776);
var inst_22776__$1 = inst_22789;
var state_22804__$1 = (function (){var statearr_22819 = state_22804;
(statearr_22819[(10)] = inst_22776__$1);

return statearr_22819;
})();
var statearr_22820_22839 = state_22804__$1;
(statearr_22820_22839[(2)] = null);

(statearr_22820_22839[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
return null;
}
}
}
}
}
}
}
}
}
}
}
});})(c__20932__auto___22829,out))
;
return ((function (switch__20870__auto__,c__20932__auto___22829,out){
return (function() {
var cljs$core$async$state_machine__20871__auto__ = null;
var cljs$core$async$state_machine__20871__auto____0 = (function (){
var statearr_22824 = [null,null,null,null,null,null,null,null,null,null,null,null];
(statearr_22824[(0)] = cljs$core$async$state_machine__20871__auto__);

(statearr_22824[(1)] = (1));

return statearr_22824;
});
var cljs$core$async$state_machine__20871__auto____1 = (function (state_22804){
while(true){
var ret_value__20872__auto__ = (function (){try{while(true){
var result__20873__auto__ = switch__20870__auto__.call(null,state_22804);
if(cljs.core.keyword_identical_QMARK_.call(null,result__20873__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__20873__auto__;
}
break;
}
}catch (e22825){if((e22825 instanceof Object)){
var ex__20874__auto__ = e22825;
var statearr_22826_22840 = state_22804;
(statearr_22826_22840[(5)] = ex__20874__auto__);


cljs.core.async.impl.ioc_helpers.process_exception.call(null,state_22804);

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
throw e22825;

}
}})();
if(cljs.core.keyword_identical_QMARK_.call(null,ret_value__20872__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__22841 = state_22804;
state_22804 = G__22841;
continue;
} else {
return ret_value__20872__auto__;
}
break;
}
});
cljs$core$async$state_machine__20871__auto__ = function(state_22804){
switch(arguments.length){
case 0:
return cljs$core$async$state_machine__20871__auto____0.call(this);
case 1:
return cljs$core$async$state_machine__20871__auto____1.call(this,state_22804);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$state_machine__20871__auto____0;
cljs$core$async$state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$state_machine__20871__auto____1;
return cljs$core$async$state_machine__20871__auto__;
})()
;})(switch__20870__auto__,c__20932__auto___22829,out))
})();
var state__20934__auto__ = (function (){var statearr_22827 = f__20933__auto__.call(null);
(statearr_22827[cljs.core.async.impl.ioc_helpers.USER_START_IDX] = c__20932__auto___22829);

return statearr_22827;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped.call(null,state__20934__auto__);
});})(c__20932__auto___22829,out))
);


return out;
});

cljs.core.async.merge.cljs$lang$maxFixedArity = 2;
/**
 * Returns a channel containing the single (collection) result of the
 * items taken from the channel conjoined to the supplied
 * collection. ch must close before into produces a result.
 */
cljs.core.async.into = (function cljs$core$async$into(coll,ch){
return cljs.core.async.reduce.call(null,cljs.core.conj,coll,ch);
});
/**
 * Returns a channel that will return, at most, n items from ch. After n items
 * have been returned, or ch has been closed, the return chanel will close.
 * 
 * The output channel is unbuffered by default, unless buf-or-n is given.
 */
cljs.core.async.take = (function cljs$core$async$take(){
var G__22843 = arguments.length;
switch (G__22843) {
case 2:
return cljs.core.async.take.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return cljs.core.async.take.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

cljs.core.async.take.cljs$core$IFn$_invoke$arity$2 = (function (n,ch){
return cljs.core.async.take.call(null,n,ch,null);
});

cljs.core.async.take.cljs$core$IFn$_invoke$arity$3 = (function (n,ch,buf_or_n){
var out = cljs.core.async.chan.call(null,buf_or_n);
var c__20932__auto___22891 = cljs.core.async.chan.call(null,(1));
cljs.core.async.impl.dispatch.run.call(null,((function (c__20932__auto___22891,out){
return (function (){
var f__20933__auto__ = (function (){var switch__20870__auto__ = ((function (c__20932__auto___22891,out){
return (function (state_22867){
var state_val_22868 = (state_22867[(1)]);
if((state_val_22868 === (7))){
var inst_22849 = (state_22867[(7)]);
var inst_22849__$1 = (state_22867[(2)]);
var inst_22850 = (inst_22849__$1 == null);
var inst_22851 = cljs.core.not.call(null,inst_22850);
var state_22867__$1 = (function (){var statearr_22869 = state_22867;
(statearr_22869[(7)] = inst_22849__$1);

return statearr_22869;
})();
if(inst_22851){
var statearr_22870_22892 = state_22867__$1;
(statearr_22870_22892[(1)] = (8));

} else {
var statearr_22871_22893 = state_22867__$1;
(statearr_22871_22893[(1)] = (9));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22868 === (1))){
var inst_22844 = (0);
var state_22867__$1 = (function (){var statearr_22872 = state_22867;
(statearr_22872[(8)] = inst_22844);

return statearr_22872;
})();
var statearr_22873_22894 = state_22867__$1;
(statearr_22873_22894[(2)] = null);

(statearr_22873_22894[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22868 === (4))){
var state_22867__$1 = state_22867;
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_22867__$1,(7),ch);
} else {
if((state_val_22868 === (6))){
var inst_22862 = (state_22867[(2)]);
var state_22867__$1 = state_22867;
var statearr_22874_22895 = state_22867__$1;
(statearr_22874_22895[(2)] = inst_22862);

(statearr_22874_22895[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22868 === (3))){
var inst_22864 = (state_22867[(2)]);
var inst_22865 = cljs.core.async.close_BANG_.call(null,out);
var state_22867__$1 = (function (){var statearr_22875 = state_22867;
(statearr_22875[(9)] = inst_22864);

return statearr_22875;
})();
return cljs.core.async.impl.ioc_helpers.return_chan.call(null,state_22867__$1,inst_22865);
} else {
if((state_val_22868 === (2))){
var inst_22844 = (state_22867[(8)]);
var inst_22846 = (inst_22844 < n);
var state_22867__$1 = state_22867;
if(cljs.core.truth_(inst_22846)){
var statearr_22876_22896 = state_22867__$1;
(statearr_22876_22896[(1)] = (4));

} else {
var statearr_22877_22897 = state_22867__$1;
(statearr_22877_22897[(1)] = (5));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22868 === (11))){
var inst_22844 = (state_22867[(8)]);
var inst_22854 = (state_22867[(2)]);
var inst_22855 = (inst_22844 + (1));
var inst_22844__$1 = inst_22855;
var state_22867__$1 = (function (){var statearr_22878 = state_22867;
(statearr_22878[(8)] = inst_22844__$1);

(statearr_22878[(10)] = inst_22854);

return statearr_22878;
})();
var statearr_22879_22898 = state_22867__$1;
(statearr_22879_22898[(2)] = null);

(statearr_22879_22898[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22868 === (9))){
var state_22867__$1 = state_22867;
var statearr_22880_22899 = state_22867__$1;
(statearr_22880_22899[(2)] = null);

(statearr_22880_22899[(1)] = (10));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22868 === (5))){
var state_22867__$1 = state_22867;
var statearr_22881_22900 = state_22867__$1;
(statearr_22881_22900[(2)] = null);

(statearr_22881_22900[(1)] = (6));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22868 === (10))){
var inst_22859 = (state_22867[(2)]);
var state_22867__$1 = state_22867;
var statearr_22882_22901 = state_22867__$1;
(statearr_22882_22901[(2)] = inst_22859);

(statearr_22882_22901[(1)] = (6));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22868 === (8))){
var inst_22849 = (state_22867[(7)]);
var state_22867__$1 = state_22867;
return cljs.core.async.impl.ioc_helpers.put_BANG_.call(null,state_22867__$1,(11),out,inst_22849);
} else {
return null;
}
}
}
}
}
}
}
}
}
}
}
});})(c__20932__auto___22891,out))
;
return ((function (switch__20870__auto__,c__20932__auto___22891,out){
return (function() {
var cljs$core$async$state_machine__20871__auto__ = null;
var cljs$core$async$state_machine__20871__auto____0 = (function (){
var statearr_22886 = [null,null,null,null,null,null,null,null,null,null,null];
(statearr_22886[(0)] = cljs$core$async$state_machine__20871__auto__);

(statearr_22886[(1)] = (1));

return statearr_22886;
});
var cljs$core$async$state_machine__20871__auto____1 = (function (state_22867){
while(true){
var ret_value__20872__auto__ = (function (){try{while(true){
var result__20873__auto__ = switch__20870__auto__.call(null,state_22867);
if(cljs.core.keyword_identical_QMARK_.call(null,result__20873__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__20873__auto__;
}
break;
}
}catch (e22887){if((e22887 instanceof Object)){
var ex__20874__auto__ = e22887;
var statearr_22888_22902 = state_22867;
(statearr_22888_22902[(5)] = ex__20874__auto__);


cljs.core.async.impl.ioc_helpers.process_exception.call(null,state_22867);

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
throw e22887;

}
}})();
if(cljs.core.keyword_identical_QMARK_.call(null,ret_value__20872__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__22903 = state_22867;
state_22867 = G__22903;
continue;
} else {
return ret_value__20872__auto__;
}
break;
}
});
cljs$core$async$state_machine__20871__auto__ = function(state_22867){
switch(arguments.length){
case 0:
return cljs$core$async$state_machine__20871__auto____0.call(this);
case 1:
return cljs$core$async$state_machine__20871__auto____1.call(this,state_22867);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$state_machine__20871__auto____0;
cljs$core$async$state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$state_machine__20871__auto____1;
return cljs$core$async$state_machine__20871__auto__;
})()
;})(switch__20870__auto__,c__20932__auto___22891,out))
})();
var state__20934__auto__ = (function (){var statearr_22889 = f__20933__auto__.call(null);
(statearr_22889[cljs.core.async.impl.ioc_helpers.USER_START_IDX] = c__20932__auto___22891);

return statearr_22889;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped.call(null,state__20934__auto__);
});})(c__20932__auto___22891,out))
);


return out;
});

cljs.core.async.take.cljs$lang$maxFixedArity = 3;
/**
 * Deprecated - this function will be removed. Use transducer instead
 */
cljs.core.async.map_LT_ = (function cljs$core$async$map_LT_(f,ch){
if(typeof cljs.core.async.t22911 !== 'undefined'){
} else {

/**
* @constructor
*/
cljs.core.async.t22911 = (function (map_LT_,f,ch,meta22912){
this.map_LT_ = map_LT_;
this.f = f;
this.ch = ch;
this.meta22912 = meta22912;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
})
cljs.core.async.t22911.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_22913,meta22912__$1){
var self__ = this;
var _22913__$1 = this;
return (new cljs.core.async.t22911(self__.map_LT_,self__.f,self__.ch,meta22912__$1));
});

cljs.core.async.t22911.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_22913){
var self__ = this;
var _22913__$1 = this;
return self__.meta22912;
});

cljs.core.async.t22911.prototype.cljs$core$async$impl$protocols$Channel$ = true;

cljs.core.async.t22911.prototype.cljs$core$async$impl$protocols$Channel$close_BANG_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.async.impl.protocols.close_BANG_.call(null,self__.ch);
});

cljs.core.async.t22911.prototype.cljs$core$async$impl$protocols$Channel$closed_QMARK_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.async.impl.protocols.closed_QMARK_.call(null,self__.ch);
});

cljs.core.async.t22911.prototype.cljs$core$async$impl$protocols$ReadPort$ = true;

cljs.core.async.t22911.prototype.cljs$core$async$impl$protocols$ReadPort$take_BANG_$arity$2 = (function (_,fn1){
var self__ = this;
var ___$1 = this;
var ret = cljs.core.async.impl.protocols.take_BANG_.call(null,self__.ch,(function (){
if(typeof cljs.core.async.t22914 !== 'undefined'){
} else {

/**
* @constructor
*/
cljs.core.async.t22914 = (function (map_LT_,f,ch,meta22912,_,fn1,meta22915){
this.map_LT_ = map_LT_;
this.f = f;
this.ch = ch;
this.meta22912 = meta22912;
this._ = _;
this.fn1 = fn1;
this.meta22915 = meta22915;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
})
cljs.core.async.t22914.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = ((function (___$1){
return (function (_22916,meta22915__$1){
var self__ = this;
var _22916__$1 = this;
return (new cljs.core.async.t22914(self__.map_LT_,self__.f,self__.ch,self__.meta22912,self__._,self__.fn1,meta22915__$1));
});})(___$1))
;

cljs.core.async.t22914.prototype.cljs$core$IMeta$_meta$arity$1 = ((function (___$1){
return (function (_22916){
var self__ = this;
var _22916__$1 = this;
return self__.meta22915;
});})(___$1))
;

cljs.core.async.t22914.prototype.cljs$core$async$impl$protocols$Handler$ = true;

cljs.core.async.t22914.prototype.cljs$core$async$impl$protocols$Handler$active_QMARK_$arity$1 = ((function (___$1){
return (function (___$1){
var self__ = this;
var ___$2 = this;
return cljs.core.async.impl.protocols.active_QMARK_.call(null,self__.fn1);
});})(___$1))
;

cljs.core.async.t22914.prototype.cljs$core$async$impl$protocols$Handler$commit$arity$1 = ((function (___$1){
return (function (___$1){
var self__ = this;
var ___$2 = this;
var f1 = cljs.core.async.impl.protocols.commit.call(null,self__.fn1);
return ((function (f1,___$2,___$1){
return (function (p1__22904_SHARP_){
return f1.call(null,(((p1__22904_SHARP_ == null))?null:self__.f.call(null,p1__22904_SHARP_)));
});
;})(f1,___$2,___$1))
});})(___$1))
;

cljs.core.async.t22914.getBasis = ((function (___$1){
return (function (){
return new cljs.core.PersistentVector(null, 7, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"map<","map<",-1235808357,null),new cljs.core.Symbol(null,"f","f",43394975,null),new cljs.core.Symbol(null,"ch","ch",1085813622,null),new cljs.core.Symbol(null,"meta22912","meta22912",2108843149,null),new cljs.core.Symbol(null,"_","_",-1201019570,null),new cljs.core.Symbol(null,"fn1","fn1",895834444,null),new cljs.core.Symbol(null,"meta22915","meta22915",1318750003,null)], null);
});})(___$1))
;

cljs.core.async.t22914.cljs$lang$type = true;

cljs.core.async.t22914.cljs$lang$ctorStr = "cljs.core.async/t22914";

cljs.core.async.t22914.cljs$lang$ctorPrWriter = ((function (___$1){
return (function (this__18652__auto__,writer__18653__auto__,opt__18654__auto__){
return cljs.core._write.call(null,writer__18653__auto__,"cljs.core.async/t22914");
});})(___$1))
;

cljs.core.async.__GT_t22914 = ((function (___$1){
return (function cljs$core$async$map_LT__$___GT_t22914(map_LT___$1,f__$1,ch__$1,meta22912__$1,___$2,fn1__$1,meta22915){
return (new cljs.core.async.t22914(map_LT___$1,f__$1,ch__$1,meta22912__$1,___$2,fn1__$1,meta22915));
});})(___$1))
;

}

return (new cljs.core.async.t22914(self__.map_LT_,self__.f,self__.ch,self__.meta22912,___$1,fn1,cljs.core.PersistentArrayMap.EMPTY));
})()
);
if(cljs.core.truth_((function (){var and__18061__auto__ = ret;
if(cljs.core.truth_(and__18061__auto__)){
return !((cljs.core.deref.call(null,ret) == null));
} else {
return and__18061__auto__;
}
})())){
return cljs.core.async.impl.channels.box.call(null,self__.f.call(null,cljs.core.deref.call(null,ret)));
} else {
return ret;
}
});

cljs.core.async.t22911.prototype.cljs$core$async$impl$protocols$WritePort$ = true;

cljs.core.async.t22911.prototype.cljs$core$async$impl$protocols$WritePort$put_BANG_$arity$3 = (function (_,val,fn1){
var self__ = this;
var ___$1 = this;
return cljs.core.async.impl.protocols.put_BANG_.call(null,self__.ch,val,fn1);
});

cljs.core.async.t22911.getBasis = (function (){
return new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"map<","map<",-1235808357,null),new cljs.core.Symbol(null,"f","f",43394975,null),new cljs.core.Symbol(null,"ch","ch",1085813622,null),new cljs.core.Symbol(null,"meta22912","meta22912",2108843149,null)], null);
});

cljs.core.async.t22911.cljs$lang$type = true;

cljs.core.async.t22911.cljs$lang$ctorStr = "cljs.core.async/t22911";

cljs.core.async.t22911.cljs$lang$ctorPrWriter = (function (this__18652__auto__,writer__18653__auto__,opt__18654__auto__){
return cljs.core._write.call(null,writer__18653__auto__,"cljs.core.async/t22911");
});

cljs.core.async.__GT_t22911 = (function cljs$core$async$map_LT__$___GT_t22911(map_LT___$1,f__$1,ch__$1,meta22912){
return (new cljs.core.async.t22911(map_LT___$1,f__$1,ch__$1,meta22912));
});

}

return (new cljs.core.async.t22911(cljs$core$async$map_LT_,f,ch,cljs.core.PersistentArrayMap.EMPTY));
});
/**
 * Deprecated - this function will be removed. Use transducer instead
 */
cljs.core.async.map_GT_ = (function cljs$core$async$map_GT_(f,ch){
if(typeof cljs.core.async.t22920 !== 'undefined'){
} else {

/**
* @constructor
*/
cljs.core.async.t22920 = (function (map_GT_,f,ch,meta22921){
this.map_GT_ = map_GT_;
this.f = f;
this.ch = ch;
this.meta22921 = meta22921;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
})
cljs.core.async.t22920.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_22922,meta22921__$1){
var self__ = this;
var _22922__$1 = this;
return (new cljs.core.async.t22920(self__.map_GT_,self__.f,self__.ch,meta22921__$1));
});

cljs.core.async.t22920.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_22922){
var self__ = this;
var _22922__$1 = this;
return self__.meta22921;
});

cljs.core.async.t22920.prototype.cljs$core$async$impl$protocols$Channel$ = true;

cljs.core.async.t22920.prototype.cljs$core$async$impl$protocols$Channel$close_BANG_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.async.impl.protocols.close_BANG_.call(null,self__.ch);
});

cljs.core.async.t22920.prototype.cljs$core$async$impl$protocols$ReadPort$ = true;

cljs.core.async.t22920.prototype.cljs$core$async$impl$protocols$ReadPort$take_BANG_$arity$2 = (function (_,fn1){
var self__ = this;
var ___$1 = this;
return cljs.core.async.impl.protocols.take_BANG_.call(null,self__.ch,fn1);
});

cljs.core.async.t22920.prototype.cljs$core$async$impl$protocols$WritePort$ = true;

cljs.core.async.t22920.prototype.cljs$core$async$impl$protocols$WritePort$put_BANG_$arity$3 = (function (_,val,fn1){
var self__ = this;
var ___$1 = this;
return cljs.core.async.impl.protocols.put_BANG_.call(null,self__.ch,self__.f.call(null,val),fn1);
});

cljs.core.async.t22920.getBasis = (function (){
return new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"map>","map>",1676369295,null),new cljs.core.Symbol(null,"f","f",43394975,null),new cljs.core.Symbol(null,"ch","ch",1085813622,null),new cljs.core.Symbol(null,"meta22921","meta22921",997674365,null)], null);
});

cljs.core.async.t22920.cljs$lang$type = true;

cljs.core.async.t22920.cljs$lang$ctorStr = "cljs.core.async/t22920";

cljs.core.async.t22920.cljs$lang$ctorPrWriter = (function (this__18652__auto__,writer__18653__auto__,opt__18654__auto__){
return cljs.core._write.call(null,writer__18653__auto__,"cljs.core.async/t22920");
});

cljs.core.async.__GT_t22920 = (function cljs$core$async$map_GT__$___GT_t22920(map_GT___$1,f__$1,ch__$1,meta22921){
return (new cljs.core.async.t22920(map_GT___$1,f__$1,ch__$1,meta22921));
});

}

return (new cljs.core.async.t22920(cljs$core$async$map_GT_,f,ch,cljs.core.PersistentArrayMap.EMPTY));
});
/**
 * Deprecated - this function will be removed. Use transducer instead
 */
cljs.core.async.filter_GT_ = (function cljs$core$async$filter_GT_(p,ch){
if(typeof cljs.core.async.t22926 !== 'undefined'){
} else {

/**
* @constructor
*/
cljs.core.async.t22926 = (function (filter_GT_,p,ch,meta22927){
this.filter_GT_ = filter_GT_;
this.p = p;
this.ch = ch;
this.meta22927 = meta22927;
this.cljs$lang$protocol_mask$partition0$ = 393216;
this.cljs$lang$protocol_mask$partition1$ = 0;
})
cljs.core.async.t22926.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (_22928,meta22927__$1){
var self__ = this;
var _22928__$1 = this;
return (new cljs.core.async.t22926(self__.filter_GT_,self__.p,self__.ch,meta22927__$1));
});

cljs.core.async.t22926.prototype.cljs$core$IMeta$_meta$arity$1 = (function (_22928){
var self__ = this;
var _22928__$1 = this;
return self__.meta22927;
});

cljs.core.async.t22926.prototype.cljs$core$async$impl$protocols$Channel$ = true;

cljs.core.async.t22926.prototype.cljs$core$async$impl$protocols$Channel$close_BANG_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.async.impl.protocols.close_BANG_.call(null,self__.ch);
});

cljs.core.async.t22926.prototype.cljs$core$async$impl$protocols$Channel$closed_QMARK_$arity$1 = (function (_){
var self__ = this;
var ___$1 = this;
return cljs.core.async.impl.protocols.closed_QMARK_.call(null,self__.ch);
});

cljs.core.async.t22926.prototype.cljs$core$async$impl$protocols$ReadPort$ = true;

cljs.core.async.t22926.prototype.cljs$core$async$impl$protocols$ReadPort$take_BANG_$arity$2 = (function (_,fn1){
var self__ = this;
var ___$1 = this;
return cljs.core.async.impl.protocols.take_BANG_.call(null,self__.ch,fn1);
});

cljs.core.async.t22926.prototype.cljs$core$async$impl$protocols$WritePort$ = true;

cljs.core.async.t22926.prototype.cljs$core$async$impl$protocols$WritePort$put_BANG_$arity$3 = (function (_,val,fn1){
var self__ = this;
var ___$1 = this;
if(cljs.core.truth_(self__.p.call(null,val))){
return cljs.core.async.impl.protocols.put_BANG_.call(null,self__.ch,val,fn1);
} else {
return cljs.core.async.impl.channels.box.call(null,cljs.core.not.call(null,cljs.core.async.impl.protocols.closed_QMARK_.call(null,self__.ch)));
}
});

cljs.core.async.t22926.getBasis = (function (){
return new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"filter>","filter>",-37644455,null),new cljs.core.Symbol(null,"p","p",1791580836,null),new cljs.core.Symbol(null,"ch","ch",1085813622,null),new cljs.core.Symbol(null,"meta22927","meta22927",-1992793083,null)], null);
});

cljs.core.async.t22926.cljs$lang$type = true;

cljs.core.async.t22926.cljs$lang$ctorStr = "cljs.core.async/t22926";

cljs.core.async.t22926.cljs$lang$ctorPrWriter = (function (this__18652__auto__,writer__18653__auto__,opt__18654__auto__){
return cljs.core._write.call(null,writer__18653__auto__,"cljs.core.async/t22926");
});

cljs.core.async.__GT_t22926 = (function cljs$core$async$filter_GT__$___GT_t22926(filter_GT___$1,p__$1,ch__$1,meta22927){
return (new cljs.core.async.t22926(filter_GT___$1,p__$1,ch__$1,meta22927));
});

}

return (new cljs.core.async.t22926(cljs$core$async$filter_GT_,p,ch,cljs.core.PersistentArrayMap.EMPTY));
});
/**
 * Deprecated - this function will be removed. Use transducer instead
 */
cljs.core.async.remove_GT_ = (function cljs$core$async$remove_GT_(p,ch){
return cljs.core.async.filter_GT_.call(null,cljs.core.complement.call(null,p),ch);
});
/**
 * Deprecated - this function will be removed. Use transducer instead
 */
cljs.core.async.filter_LT_ = (function cljs$core$async$filter_LT_(){
var G__22930 = arguments.length;
switch (G__22930) {
case 2:
return cljs.core.async.filter_LT_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return cljs.core.async.filter_LT_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

cljs.core.async.filter_LT_.cljs$core$IFn$_invoke$arity$2 = (function (p,ch){
return cljs.core.async.filter_LT_.call(null,p,ch,null);
});

cljs.core.async.filter_LT_.cljs$core$IFn$_invoke$arity$3 = (function (p,ch,buf_or_n){
var out = cljs.core.async.chan.call(null,buf_or_n);
var c__20932__auto___22973 = cljs.core.async.chan.call(null,(1));
cljs.core.async.impl.dispatch.run.call(null,((function (c__20932__auto___22973,out){
return (function (){
var f__20933__auto__ = (function (){var switch__20870__auto__ = ((function (c__20932__auto___22973,out){
return (function (state_22951){
var state_val_22952 = (state_22951[(1)]);
if((state_val_22952 === (7))){
var inst_22947 = (state_22951[(2)]);
var state_22951__$1 = state_22951;
var statearr_22953_22974 = state_22951__$1;
(statearr_22953_22974[(2)] = inst_22947);

(statearr_22953_22974[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22952 === (1))){
var state_22951__$1 = state_22951;
var statearr_22954_22975 = state_22951__$1;
(statearr_22954_22975[(2)] = null);

(statearr_22954_22975[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22952 === (4))){
var inst_22933 = (state_22951[(7)]);
var inst_22933__$1 = (state_22951[(2)]);
var inst_22934 = (inst_22933__$1 == null);
var state_22951__$1 = (function (){var statearr_22955 = state_22951;
(statearr_22955[(7)] = inst_22933__$1);

return statearr_22955;
})();
if(cljs.core.truth_(inst_22934)){
var statearr_22956_22976 = state_22951__$1;
(statearr_22956_22976[(1)] = (5));

} else {
var statearr_22957_22977 = state_22951__$1;
(statearr_22957_22977[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22952 === (6))){
var inst_22933 = (state_22951[(7)]);
var inst_22938 = p.call(null,inst_22933);
var state_22951__$1 = state_22951;
if(cljs.core.truth_(inst_22938)){
var statearr_22958_22978 = state_22951__$1;
(statearr_22958_22978[(1)] = (8));

} else {
var statearr_22959_22979 = state_22951__$1;
(statearr_22959_22979[(1)] = (9));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22952 === (3))){
var inst_22949 = (state_22951[(2)]);
var state_22951__$1 = state_22951;
return cljs.core.async.impl.ioc_helpers.return_chan.call(null,state_22951__$1,inst_22949);
} else {
if((state_val_22952 === (2))){
var state_22951__$1 = state_22951;
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_22951__$1,(4),ch);
} else {
if((state_val_22952 === (11))){
var inst_22941 = (state_22951[(2)]);
var state_22951__$1 = state_22951;
var statearr_22960_22980 = state_22951__$1;
(statearr_22960_22980[(2)] = inst_22941);

(statearr_22960_22980[(1)] = (10));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22952 === (9))){
var state_22951__$1 = state_22951;
var statearr_22961_22981 = state_22951__$1;
(statearr_22961_22981[(2)] = null);

(statearr_22961_22981[(1)] = (10));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22952 === (5))){
var inst_22936 = cljs.core.async.close_BANG_.call(null,out);
var state_22951__$1 = state_22951;
var statearr_22962_22982 = state_22951__$1;
(statearr_22962_22982[(2)] = inst_22936);

(statearr_22962_22982[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22952 === (10))){
var inst_22944 = (state_22951[(2)]);
var state_22951__$1 = (function (){var statearr_22963 = state_22951;
(statearr_22963[(8)] = inst_22944);

return statearr_22963;
})();
var statearr_22964_22983 = state_22951__$1;
(statearr_22964_22983[(2)] = null);

(statearr_22964_22983[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_22952 === (8))){
var inst_22933 = (state_22951[(7)]);
var state_22951__$1 = state_22951;
return cljs.core.async.impl.ioc_helpers.put_BANG_.call(null,state_22951__$1,(11),out,inst_22933);
} else {
return null;
}
}
}
}
}
}
}
}
}
}
}
});})(c__20932__auto___22973,out))
;
return ((function (switch__20870__auto__,c__20932__auto___22973,out){
return (function() {
var cljs$core$async$state_machine__20871__auto__ = null;
var cljs$core$async$state_machine__20871__auto____0 = (function (){
var statearr_22968 = [null,null,null,null,null,null,null,null,null];
(statearr_22968[(0)] = cljs$core$async$state_machine__20871__auto__);

(statearr_22968[(1)] = (1));

return statearr_22968;
});
var cljs$core$async$state_machine__20871__auto____1 = (function (state_22951){
while(true){
var ret_value__20872__auto__ = (function (){try{while(true){
var result__20873__auto__ = switch__20870__auto__.call(null,state_22951);
if(cljs.core.keyword_identical_QMARK_.call(null,result__20873__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__20873__auto__;
}
break;
}
}catch (e22969){if((e22969 instanceof Object)){
var ex__20874__auto__ = e22969;
var statearr_22970_22984 = state_22951;
(statearr_22970_22984[(5)] = ex__20874__auto__);


cljs.core.async.impl.ioc_helpers.process_exception.call(null,state_22951);

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
throw e22969;

}
}})();
if(cljs.core.keyword_identical_QMARK_.call(null,ret_value__20872__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__22985 = state_22951;
state_22951 = G__22985;
continue;
} else {
return ret_value__20872__auto__;
}
break;
}
});
cljs$core$async$state_machine__20871__auto__ = function(state_22951){
switch(arguments.length){
case 0:
return cljs$core$async$state_machine__20871__auto____0.call(this);
case 1:
return cljs$core$async$state_machine__20871__auto____1.call(this,state_22951);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$state_machine__20871__auto____0;
cljs$core$async$state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$state_machine__20871__auto____1;
return cljs$core$async$state_machine__20871__auto__;
})()
;})(switch__20870__auto__,c__20932__auto___22973,out))
})();
var state__20934__auto__ = (function (){var statearr_22971 = f__20933__auto__.call(null);
(statearr_22971[cljs.core.async.impl.ioc_helpers.USER_START_IDX] = c__20932__auto___22973);

return statearr_22971;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped.call(null,state__20934__auto__);
});})(c__20932__auto___22973,out))
);


return out;
});

cljs.core.async.filter_LT_.cljs$lang$maxFixedArity = 3;
/**
 * Deprecated - this function will be removed. Use transducer instead
 */
cljs.core.async.remove_LT_ = (function cljs$core$async$remove_LT_(){
var G__22987 = arguments.length;
switch (G__22987) {
case 2:
return cljs.core.async.remove_LT_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return cljs.core.async.remove_LT_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

cljs.core.async.remove_LT_.cljs$core$IFn$_invoke$arity$2 = (function (p,ch){
return cljs.core.async.remove_LT_.call(null,p,ch,null);
});

cljs.core.async.remove_LT_.cljs$core$IFn$_invoke$arity$3 = (function (p,ch,buf_or_n){
return cljs.core.async.filter_LT_.call(null,cljs.core.complement.call(null,p),ch,buf_or_n);
});

cljs.core.async.remove_LT_.cljs$lang$maxFixedArity = 3;
cljs.core.async.mapcat_STAR_ = (function cljs$core$async$mapcat_STAR_(f,in$,out){
var c__20932__auto__ = cljs.core.async.chan.call(null,(1));
cljs.core.async.impl.dispatch.run.call(null,((function (c__20932__auto__){
return (function (){
var f__20933__auto__ = (function (){var switch__20870__auto__ = ((function (c__20932__auto__){
return (function (state_23154){
var state_val_23155 = (state_23154[(1)]);
if((state_val_23155 === (7))){
var inst_23150 = (state_23154[(2)]);
var state_23154__$1 = state_23154;
var statearr_23156_23197 = state_23154__$1;
(statearr_23156_23197[(2)] = inst_23150);

(statearr_23156_23197[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23155 === (20))){
var inst_23120 = (state_23154[(7)]);
var inst_23131 = (state_23154[(2)]);
var inst_23132 = cljs.core.next.call(null,inst_23120);
var inst_23106 = inst_23132;
var inst_23107 = null;
var inst_23108 = (0);
var inst_23109 = (0);
var state_23154__$1 = (function (){var statearr_23157 = state_23154;
(statearr_23157[(8)] = inst_23107);

(statearr_23157[(9)] = inst_23108);

(statearr_23157[(10)] = inst_23106);

(statearr_23157[(11)] = inst_23109);

(statearr_23157[(12)] = inst_23131);

return statearr_23157;
})();
var statearr_23158_23198 = state_23154__$1;
(statearr_23158_23198[(2)] = null);

(statearr_23158_23198[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23155 === (1))){
var state_23154__$1 = state_23154;
var statearr_23159_23199 = state_23154__$1;
(statearr_23159_23199[(2)] = null);

(statearr_23159_23199[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23155 === (4))){
var inst_23095 = (state_23154[(13)]);
var inst_23095__$1 = (state_23154[(2)]);
var inst_23096 = (inst_23095__$1 == null);
var state_23154__$1 = (function (){var statearr_23160 = state_23154;
(statearr_23160[(13)] = inst_23095__$1);

return statearr_23160;
})();
if(cljs.core.truth_(inst_23096)){
var statearr_23161_23200 = state_23154__$1;
(statearr_23161_23200[(1)] = (5));

} else {
var statearr_23162_23201 = state_23154__$1;
(statearr_23162_23201[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23155 === (15))){
var state_23154__$1 = state_23154;
var statearr_23166_23202 = state_23154__$1;
(statearr_23166_23202[(2)] = null);

(statearr_23166_23202[(1)] = (16));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23155 === (21))){
var state_23154__$1 = state_23154;
var statearr_23167_23203 = state_23154__$1;
(statearr_23167_23203[(2)] = null);

(statearr_23167_23203[(1)] = (23));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23155 === (13))){
var inst_23107 = (state_23154[(8)]);
var inst_23108 = (state_23154[(9)]);
var inst_23106 = (state_23154[(10)]);
var inst_23109 = (state_23154[(11)]);
var inst_23116 = (state_23154[(2)]);
var inst_23117 = (inst_23109 + (1));
var tmp23163 = inst_23107;
var tmp23164 = inst_23108;
var tmp23165 = inst_23106;
var inst_23106__$1 = tmp23165;
var inst_23107__$1 = tmp23163;
var inst_23108__$1 = tmp23164;
var inst_23109__$1 = inst_23117;
var state_23154__$1 = (function (){var statearr_23168 = state_23154;
(statearr_23168[(8)] = inst_23107__$1);

(statearr_23168[(9)] = inst_23108__$1);

(statearr_23168[(10)] = inst_23106__$1);

(statearr_23168[(11)] = inst_23109__$1);

(statearr_23168[(14)] = inst_23116);

return statearr_23168;
})();
var statearr_23169_23204 = state_23154__$1;
(statearr_23169_23204[(2)] = null);

(statearr_23169_23204[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23155 === (22))){
var state_23154__$1 = state_23154;
var statearr_23170_23205 = state_23154__$1;
(statearr_23170_23205[(2)] = null);

(statearr_23170_23205[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23155 === (6))){
var inst_23095 = (state_23154[(13)]);
var inst_23104 = f.call(null,inst_23095);
var inst_23105 = cljs.core.seq.call(null,inst_23104);
var inst_23106 = inst_23105;
var inst_23107 = null;
var inst_23108 = (0);
var inst_23109 = (0);
var state_23154__$1 = (function (){var statearr_23171 = state_23154;
(statearr_23171[(8)] = inst_23107);

(statearr_23171[(9)] = inst_23108);

(statearr_23171[(10)] = inst_23106);

(statearr_23171[(11)] = inst_23109);

return statearr_23171;
})();
var statearr_23172_23206 = state_23154__$1;
(statearr_23172_23206[(2)] = null);

(statearr_23172_23206[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23155 === (17))){
var inst_23120 = (state_23154[(7)]);
var inst_23124 = cljs.core.chunk_first.call(null,inst_23120);
var inst_23125 = cljs.core.chunk_rest.call(null,inst_23120);
var inst_23126 = cljs.core.count.call(null,inst_23124);
var inst_23106 = inst_23125;
var inst_23107 = inst_23124;
var inst_23108 = inst_23126;
var inst_23109 = (0);
var state_23154__$1 = (function (){var statearr_23173 = state_23154;
(statearr_23173[(8)] = inst_23107);

(statearr_23173[(9)] = inst_23108);

(statearr_23173[(10)] = inst_23106);

(statearr_23173[(11)] = inst_23109);

return statearr_23173;
})();
var statearr_23174_23207 = state_23154__$1;
(statearr_23174_23207[(2)] = null);

(statearr_23174_23207[(1)] = (8));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23155 === (3))){
var inst_23152 = (state_23154[(2)]);
var state_23154__$1 = state_23154;
return cljs.core.async.impl.ioc_helpers.return_chan.call(null,state_23154__$1,inst_23152);
} else {
if((state_val_23155 === (12))){
var inst_23140 = (state_23154[(2)]);
var state_23154__$1 = state_23154;
var statearr_23175_23208 = state_23154__$1;
(statearr_23175_23208[(2)] = inst_23140);

(statearr_23175_23208[(1)] = (9));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23155 === (2))){
var state_23154__$1 = state_23154;
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_23154__$1,(4),in$);
} else {
if((state_val_23155 === (23))){
var inst_23148 = (state_23154[(2)]);
var state_23154__$1 = state_23154;
var statearr_23176_23209 = state_23154__$1;
(statearr_23176_23209[(2)] = inst_23148);

(statearr_23176_23209[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23155 === (19))){
var inst_23135 = (state_23154[(2)]);
var state_23154__$1 = state_23154;
var statearr_23177_23210 = state_23154__$1;
(statearr_23177_23210[(2)] = inst_23135);

(statearr_23177_23210[(1)] = (16));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23155 === (11))){
var inst_23106 = (state_23154[(10)]);
var inst_23120 = (state_23154[(7)]);
var inst_23120__$1 = cljs.core.seq.call(null,inst_23106);
var state_23154__$1 = (function (){var statearr_23178 = state_23154;
(statearr_23178[(7)] = inst_23120__$1);

return statearr_23178;
})();
if(inst_23120__$1){
var statearr_23179_23211 = state_23154__$1;
(statearr_23179_23211[(1)] = (14));

} else {
var statearr_23180_23212 = state_23154__$1;
(statearr_23180_23212[(1)] = (15));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23155 === (9))){
var inst_23142 = (state_23154[(2)]);
var inst_23143 = cljs.core.async.impl.protocols.closed_QMARK_.call(null,out);
var state_23154__$1 = (function (){var statearr_23181 = state_23154;
(statearr_23181[(15)] = inst_23142);

return statearr_23181;
})();
if(cljs.core.truth_(inst_23143)){
var statearr_23182_23213 = state_23154__$1;
(statearr_23182_23213[(1)] = (21));

} else {
var statearr_23183_23214 = state_23154__$1;
(statearr_23183_23214[(1)] = (22));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23155 === (5))){
var inst_23098 = cljs.core.async.close_BANG_.call(null,out);
var state_23154__$1 = state_23154;
var statearr_23184_23215 = state_23154__$1;
(statearr_23184_23215[(2)] = inst_23098);

(statearr_23184_23215[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23155 === (14))){
var inst_23120 = (state_23154[(7)]);
var inst_23122 = cljs.core.chunked_seq_QMARK_.call(null,inst_23120);
var state_23154__$1 = state_23154;
if(inst_23122){
var statearr_23185_23216 = state_23154__$1;
(statearr_23185_23216[(1)] = (17));

} else {
var statearr_23186_23217 = state_23154__$1;
(statearr_23186_23217[(1)] = (18));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23155 === (16))){
var inst_23138 = (state_23154[(2)]);
var state_23154__$1 = state_23154;
var statearr_23187_23218 = state_23154__$1;
(statearr_23187_23218[(2)] = inst_23138);

(statearr_23187_23218[(1)] = (12));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23155 === (10))){
var inst_23107 = (state_23154[(8)]);
var inst_23109 = (state_23154[(11)]);
var inst_23114 = cljs.core._nth.call(null,inst_23107,inst_23109);
var state_23154__$1 = state_23154;
return cljs.core.async.impl.ioc_helpers.put_BANG_.call(null,state_23154__$1,(13),out,inst_23114);
} else {
if((state_val_23155 === (18))){
var inst_23120 = (state_23154[(7)]);
var inst_23129 = cljs.core.first.call(null,inst_23120);
var state_23154__$1 = state_23154;
return cljs.core.async.impl.ioc_helpers.put_BANG_.call(null,state_23154__$1,(20),out,inst_23129);
} else {
if((state_val_23155 === (8))){
var inst_23108 = (state_23154[(9)]);
var inst_23109 = (state_23154[(11)]);
var inst_23111 = (inst_23109 < inst_23108);
var inst_23112 = inst_23111;
var state_23154__$1 = state_23154;
if(cljs.core.truth_(inst_23112)){
var statearr_23188_23219 = state_23154__$1;
(statearr_23188_23219[(1)] = (10));

} else {
var statearr_23189_23220 = state_23154__$1;
(statearr_23189_23220[(1)] = (11));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
return null;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
});})(c__20932__auto__))
;
return ((function (switch__20870__auto__,c__20932__auto__){
return (function() {
var cljs$core$async$mapcat_STAR__$_state_machine__20871__auto__ = null;
var cljs$core$async$mapcat_STAR__$_state_machine__20871__auto____0 = (function (){
var statearr_23193 = [null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null];
(statearr_23193[(0)] = cljs$core$async$mapcat_STAR__$_state_machine__20871__auto__);

(statearr_23193[(1)] = (1));

return statearr_23193;
});
var cljs$core$async$mapcat_STAR__$_state_machine__20871__auto____1 = (function (state_23154){
while(true){
var ret_value__20872__auto__ = (function (){try{while(true){
var result__20873__auto__ = switch__20870__auto__.call(null,state_23154);
if(cljs.core.keyword_identical_QMARK_.call(null,result__20873__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__20873__auto__;
}
break;
}
}catch (e23194){if((e23194 instanceof Object)){
var ex__20874__auto__ = e23194;
var statearr_23195_23221 = state_23154;
(statearr_23195_23221[(5)] = ex__20874__auto__);


cljs.core.async.impl.ioc_helpers.process_exception.call(null,state_23154);

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
throw e23194;

}
}})();
if(cljs.core.keyword_identical_QMARK_.call(null,ret_value__20872__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__23222 = state_23154;
state_23154 = G__23222;
continue;
} else {
return ret_value__20872__auto__;
}
break;
}
});
cljs$core$async$mapcat_STAR__$_state_machine__20871__auto__ = function(state_23154){
switch(arguments.length){
case 0:
return cljs$core$async$mapcat_STAR__$_state_machine__20871__auto____0.call(this);
case 1:
return cljs$core$async$mapcat_STAR__$_state_machine__20871__auto____1.call(this,state_23154);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$mapcat_STAR__$_state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$mapcat_STAR__$_state_machine__20871__auto____0;
cljs$core$async$mapcat_STAR__$_state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$mapcat_STAR__$_state_machine__20871__auto____1;
return cljs$core$async$mapcat_STAR__$_state_machine__20871__auto__;
})()
;})(switch__20870__auto__,c__20932__auto__))
})();
var state__20934__auto__ = (function (){var statearr_23196 = f__20933__auto__.call(null);
(statearr_23196[cljs.core.async.impl.ioc_helpers.USER_START_IDX] = c__20932__auto__);

return statearr_23196;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped.call(null,state__20934__auto__);
});})(c__20932__auto__))
);

return c__20932__auto__;
});
/**
 * Deprecated - this function will be removed. Use transducer instead
 */
cljs.core.async.mapcat_LT_ = (function cljs$core$async$mapcat_LT_(){
var G__23224 = arguments.length;
switch (G__23224) {
case 2:
return cljs.core.async.mapcat_LT_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return cljs.core.async.mapcat_LT_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

cljs.core.async.mapcat_LT_.cljs$core$IFn$_invoke$arity$2 = (function (f,in$){
return cljs.core.async.mapcat_LT_.call(null,f,in$,null);
});

cljs.core.async.mapcat_LT_.cljs$core$IFn$_invoke$arity$3 = (function (f,in$,buf_or_n){
var out = cljs.core.async.chan.call(null,buf_or_n);
cljs.core.async.mapcat_STAR_.call(null,f,in$,out);

return out;
});

cljs.core.async.mapcat_LT_.cljs$lang$maxFixedArity = 3;
/**
 * Deprecated - this function will be removed. Use transducer instead
 */
cljs.core.async.mapcat_GT_ = (function cljs$core$async$mapcat_GT_(){
var G__23227 = arguments.length;
switch (G__23227) {
case 2:
return cljs.core.async.mapcat_GT_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return cljs.core.async.mapcat_GT_.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

cljs.core.async.mapcat_GT_.cljs$core$IFn$_invoke$arity$2 = (function (f,out){
return cljs.core.async.mapcat_GT_.call(null,f,out,null);
});

cljs.core.async.mapcat_GT_.cljs$core$IFn$_invoke$arity$3 = (function (f,out,buf_or_n){
var in$ = cljs.core.async.chan.call(null,buf_or_n);
cljs.core.async.mapcat_STAR_.call(null,f,in$,out);

return in$;
});

cljs.core.async.mapcat_GT_.cljs$lang$maxFixedArity = 3;
/**
 * Deprecated - this function will be removed. Use transducer instead
 */
cljs.core.async.unique = (function cljs$core$async$unique(){
var G__23230 = arguments.length;
switch (G__23230) {
case 1:
return cljs.core.async.unique.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return cljs.core.async.unique.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

cljs.core.async.unique.cljs$core$IFn$_invoke$arity$1 = (function (ch){
return cljs.core.async.unique.call(null,ch,null);
});

cljs.core.async.unique.cljs$core$IFn$_invoke$arity$2 = (function (ch,buf_or_n){
var out = cljs.core.async.chan.call(null,buf_or_n);
var c__20932__auto___23280 = cljs.core.async.chan.call(null,(1));
cljs.core.async.impl.dispatch.run.call(null,((function (c__20932__auto___23280,out){
return (function (){
var f__20933__auto__ = (function (){var switch__20870__auto__ = ((function (c__20932__auto___23280,out){
return (function (state_23254){
var state_val_23255 = (state_23254[(1)]);
if((state_val_23255 === (7))){
var inst_23249 = (state_23254[(2)]);
var state_23254__$1 = state_23254;
var statearr_23256_23281 = state_23254__$1;
(statearr_23256_23281[(2)] = inst_23249);

(statearr_23256_23281[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23255 === (1))){
var inst_23231 = null;
var state_23254__$1 = (function (){var statearr_23257 = state_23254;
(statearr_23257[(7)] = inst_23231);

return statearr_23257;
})();
var statearr_23258_23282 = state_23254__$1;
(statearr_23258_23282[(2)] = null);

(statearr_23258_23282[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23255 === (4))){
var inst_23234 = (state_23254[(8)]);
var inst_23234__$1 = (state_23254[(2)]);
var inst_23235 = (inst_23234__$1 == null);
var inst_23236 = cljs.core.not.call(null,inst_23235);
var state_23254__$1 = (function (){var statearr_23259 = state_23254;
(statearr_23259[(8)] = inst_23234__$1);

return statearr_23259;
})();
if(inst_23236){
var statearr_23260_23283 = state_23254__$1;
(statearr_23260_23283[(1)] = (5));

} else {
var statearr_23261_23284 = state_23254__$1;
(statearr_23261_23284[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23255 === (6))){
var state_23254__$1 = state_23254;
var statearr_23262_23285 = state_23254__$1;
(statearr_23262_23285[(2)] = null);

(statearr_23262_23285[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23255 === (3))){
var inst_23251 = (state_23254[(2)]);
var inst_23252 = cljs.core.async.close_BANG_.call(null,out);
var state_23254__$1 = (function (){var statearr_23263 = state_23254;
(statearr_23263[(9)] = inst_23251);

return statearr_23263;
})();
return cljs.core.async.impl.ioc_helpers.return_chan.call(null,state_23254__$1,inst_23252);
} else {
if((state_val_23255 === (2))){
var state_23254__$1 = state_23254;
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_23254__$1,(4),ch);
} else {
if((state_val_23255 === (11))){
var inst_23234 = (state_23254[(8)]);
var inst_23243 = (state_23254[(2)]);
var inst_23231 = inst_23234;
var state_23254__$1 = (function (){var statearr_23264 = state_23254;
(statearr_23264[(7)] = inst_23231);

(statearr_23264[(10)] = inst_23243);

return statearr_23264;
})();
var statearr_23265_23286 = state_23254__$1;
(statearr_23265_23286[(2)] = null);

(statearr_23265_23286[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23255 === (9))){
var inst_23234 = (state_23254[(8)]);
var state_23254__$1 = state_23254;
return cljs.core.async.impl.ioc_helpers.put_BANG_.call(null,state_23254__$1,(11),out,inst_23234);
} else {
if((state_val_23255 === (5))){
var inst_23231 = (state_23254[(7)]);
var inst_23234 = (state_23254[(8)]);
var inst_23238 = cljs.core._EQ_.call(null,inst_23234,inst_23231);
var state_23254__$1 = state_23254;
if(inst_23238){
var statearr_23267_23287 = state_23254__$1;
(statearr_23267_23287[(1)] = (8));

} else {
var statearr_23268_23288 = state_23254__$1;
(statearr_23268_23288[(1)] = (9));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23255 === (10))){
var inst_23246 = (state_23254[(2)]);
var state_23254__$1 = state_23254;
var statearr_23269_23289 = state_23254__$1;
(statearr_23269_23289[(2)] = inst_23246);

(statearr_23269_23289[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23255 === (8))){
var inst_23231 = (state_23254[(7)]);
var tmp23266 = inst_23231;
var inst_23231__$1 = tmp23266;
var state_23254__$1 = (function (){var statearr_23270 = state_23254;
(statearr_23270[(7)] = inst_23231__$1);

return statearr_23270;
})();
var statearr_23271_23290 = state_23254__$1;
(statearr_23271_23290[(2)] = null);

(statearr_23271_23290[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
return null;
}
}
}
}
}
}
}
}
}
}
}
});})(c__20932__auto___23280,out))
;
return ((function (switch__20870__auto__,c__20932__auto___23280,out){
return (function() {
var cljs$core$async$state_machine__20871__auto__ = null;
var cljs$core$async$state_machine__20871__auto____0 = (function (){
var statearr_23275 = [null,null,null,null,null,null,null,null,null,null,null];
(statearr_23275[(0)] = cljs$core$async$state_machine__20871__auto__);

(statearr_23275[(1)] = (1));

return statearr_23275;
});
var cljs$core$async$state_machine__20871__auto____1 = (function (state_23254){
while(true){
var ret_value__20872__auto__ = (function (){try{while(true){
var result__20873__auto__ = switch__20870__auto__.call(null,state_23254);
if(cljs.core.keyword_identical_QMARK_.call(null,result__20873__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__20873__auto__;
}
break;
}
}catch (e23276){if((e23276 instanceof Object)){
var ex__20874__auto__ = e23276;
var statearr_23277_23291 = state_23254;
(statearr_23277_23291[(5)] = ex__20874__auto__);


cljs.core.async.impl.ioc_helpers.process_exception.call(null,state_23254);

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
throw e23276;

}
}})();
if(cljs.core.keyword_identical_QMARK_.call(null,ret_value__20872__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__23292 = state_23254;
state_23254 = G__23292;
continue;
} else {
return ret_value__20872__auto__;
}
break;
}
});
cljs$core$async$state_machine__20871__auto__ = function(state_23254){
switch(arguments.length){
case 0:
return cljs$core$async$state_machine__20871__auto____0.call(this);
case 1:
return cljs$core$async$state_machine__20871__auto____1.call(this,state_23254);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$state_machine__20871__auto____0;
cljs$core$async$state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$state_machine__20871__auto____1;
return cljs$core$async$state_machine__20871__auto__;
})()
;})(switch__20870__auto__,c__20932__auto___23280,out))
})();
var state__20934__auto__ = (function (){var statearr_23278 = f__20933__auto__.call(null);
(statearr_23278[cljs.core.async.impl.ioc_helpers.USER_START_IDX] = c__20932__auto___23280);

return statearr_23278;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped.call(null,state__20934__auto__);
});})(c__20932__auto___23280,out))
);


return out;
});

cljs.core.async.unique.cljs$lang$maxFixedArity = 2;
/**
 * Deprecated - this function will be removed. Use transducer instead
 */
cljs.core.async.partition = (function cljs$core$async$partition(){
var G__23294 = arguments.length;
switch (G__23294) {
case 2:
return cljs.core.async.partition.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return cljs.core.async.partition.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

cljs.core.async.partition.cljs$core$IFn$_invoke$arity$2 = (function (n,ch){
return cljs.core.async.partition.call(null,n,ch,null);
});

cljs.core.async.partition.cljs$core$IFn$_invoke$arity$3 = (function (n,ch,buf_or_n){
var out = cljs.core.async.chan.call(null,buf_or_n);
var c__20932__auto___23363 = cljs.core.async.chan.call(null,(1));
cljs.core.async.impl.dispatch.run.call(null,((function (c__20932__auto___23363,out){
return (function (){
var f__20933__auto__ = (function (){var switch__20870__auto__ = ((function (c__20932__auto___23363,out){
return (function (state_23332){
var state_val_23333 = (state_23332[(1)]);
if((state_val_23333 === (7))){
var inst_23328 = (state_23332[(2)]);
var state_23332__$1 = state_23332;
var statearr_23334_23364 = state_23332__$1;
(statearr_23334_23364[(2)] = inst_23328);

(statearr_23334_23364[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23333 === (1))){
var inst_23295 = (new Array(n));
var inst_23296 = inst_23295;
var inst_23297 = (0);
var state_23332__$1 = (function (){var statearr_23335 = state_23332;
(statearr_23335[(7)] = inst_23297);

(statearr_23335[(8)] = inst_23296);

return statearr_23335;
})();
var statearr_23336_23365 = state_23332__$1;
(statearr_23336_23365[(2)] = null);

(statearr_23336_23365[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23333 === (4))){
var inst_23300 = (state_23332[(9)]);
var inst_23300__$1 = (state_23332[(2)]);
var inst_23301 = (inst_23300__$1 == null);
var inst_23302 = cljs.core.not.call(null,inst_23301);
var state_23332__$1 = (function (){var statearr_23337 = state_23332;
(statearr_23337[(9)] = inst_23300__$1);

return statearr_23337;
})();
if(inst_23302){
var statearr_23338_23366 = state_23332__$1;
(statearr_23338_23366[(1)] = (5));

} else {
var statearr_23339_23367 = state_23332__$1;
(statearr_23339_23367[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23333 === (15))){
var inst_23322 = (state_23332[(2)]);
var state_23332__$1 = state_23332;
var statearr_23340_23368 = state_23332__$1;
(statearr_23340_23368[(2)] = inst_23322);

(statearr_23340_23368[(1)] = (14));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23333 === (13))){
var state_23332__$1 = state_23332;
var statearr_23341_23369 = state_23332__$1;
(statearr_23341_23369[(2)] = null);

(statearr_23341_23369[(1)] = (14));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23333 === (6))){
var inst_23297 = (state_23332[(7)]);
var inst_23318 = (inst_23297 > (0));
var state_23332__$1 = state_23332;
if(cljs.core.truth_(inst_23318)){
var statearr_23342_23370 = state_23332__$1;
(statearr_23342_23370[(1)] = (12));

} else {
var statearr_23343_23371 = state_23332__$1;
(statearr_23343_23371[(1)] = (13));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23333 === (3))){
var inst_23330 = (state_23332[(2)]);
var state_23332__$1 = state_23332;
return cljs.core.async.impl.ioc_helpers.return_chan.call(null,state_23332__$1,inst_23330);
} else {
if((state_val_23333 === (12))){
var inst_23296 = (state_23332[(8)]);
var inst_23320 = cljs.core.vec.call(null,inst_23296);
var state_23332__$1 = state_23332;
return cljs.core.async.impl.ioc_helpers.put_BANG_.call(null,state_23332__$1,(15),out,inst_23320);
} else {
if((state_val_23333 === (2))){
var state_23332__$1 = state_23332;
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_23332__$1,(4),ch);
} else {
if((state_val_23333 === (11))){
var inst_23312 = (state_23332[(2)]);
var inst_23313 = (new Array(n));
var inst_23296 = inst_23313;
var inst_23297 = (0);
var state_23332__$1 = (function (){var statearr_23344 = state_23332;
(statearr_23344[(10)] = inst_23312);

(statearr_23344[(7)] = inst_23297);

(statearr_23344[(8)] = inst_23296);

return statearr_23344;
})();
var statearr_23345_23372 = state_23332__$1;
(statearr_23345_23372[(2)] = null);

(statearr_23345_23372[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23333 === (9))){
var inst_23296 = (state_23332[(8)]);
var inst_23310 = cljs.core.vec.call(null,inst_23296);
var state_23332__$1 = state_23332;
return cljs.core.async.impl.ioc_helpers.put_BANG_.call(null,state_23332__$1,(11),out,inst_23310);
} else {
if((state_val_23333 === (5))){
var inst_23300 = (state_23332[(9)]);
var inst_23297 = (state_23332[(7)]);
var inst_23296 = (state_23332[(8)]);
var inst_23305 = (state_23332[(11)]);
var inst_23304 = (inst_23296[inst_23297] = inst_23300);
var inst_23305__$1 = (inst_23297 + (1));
var inst_23306 = (inst_23305__$1 < n);
var state_23332__$1 = (function (){var statearr_23346 = state_23332;
(statearr_23346[(12)] = inst_23304);

(statearr_23346[(11)] = inst_23305__$1);

return statearr_23346;
})();
if(cljs.core.truth_(inst_23306)){
var statearr_23347_23373 = state_23332__$1;
(statearr_23347_23373[(1)] = (8));

} else {
var statearr_23348_23374 = state_23332__$1;
(statearr_23348_23374[(1)] = (9));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23333 === (14))){
var inst_23325 = (state_23332[(2)]);
var inst_23326 = cljs.core.async.close_BANG_.call(null,out);
var state_23332__$1 = (function (){var statearr_23350 = state_23332;
(statearr_23350[(13)] = inst_23325);

return statearr_23350;
})();
var statearr_23351_23375 = state_23332__$1;
(statearr_23351_23375[(2)] = inst_23326);

(statearr_23351_23375[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23333 === (10))){
var inst_23316 = (state_23332[(2)]);
var state_23332__$1 = state_23332;
var statearr_23352_23376 = state_23332__$1;
(statearr_23352_23376[(2)] = inst_23316);

(statearr_23352_23376[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23333 === (8))){
var inst_23296 = (state_23332[(8)]);
var inst_23305 = (state_23332[(11)]);
var tmp23349 = inst_23296;
var inst_23296__$1 = tmp23349;
var inst_23297 = inst_23305;
var state_23332__$1 = (function (){var statearr_23353 = state_23332;
(statearr_23353[(7)] = inst_23297);

(statearr_23353[(8)] = inst_23296__$1);

return statearr_23353;
})();
var statearr_23354_23377 = state_23332__$1;
(statearr_23354_23377[(2)] = null);

(statearr_23354_23377[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
return null;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
});})(c__20932__auto___23363,out))
;
return ((function (switch__20870__auto__,c__20932__auto___23363,out){
return (function() {
var cljs$core$async$state_machine__20871__auto__ = null;
var cljs$core$async$state_machine__20871__auto____0 = (function (){
var statearr_23358 = [null,null,null,null,null,null,null,null,null,null,null,null,null,null];
(statearr_23358[(0)] = cljs$core$async$state_machine__20871__auto__);

(statearr_23358[(1)] = (1));

return statearr_23358;
});
var cljs$core$async$state_machine__20871__auto____1 = (function (state_23332){
while(true){
var ret_value__20872__auto__ = (function (){try{while(true){
var result__20873__auto__ = switch__20870__auto__.call(null,state_23332);
if(cljs.core.keyword_identical_QMARK_.call(null,result__20873__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__20873__auto__;
}
break;
}
}catch (e23359){if((e23359 instanceof Object)){
var ex__20874__auto__ = e23359;
var statearr_23360_23378 = state_23332;
(statearr_23360_23378[(5)] = ex__20874__auto__);


cljs.core.async.impl.ioc_helpers.process_exception.call(null,state_23332);

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
throw e23359;

}
}})();
if(cljs.core.keyword_identical_QMARK_.call(null,ret_value__20872__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__23379 = state_23332;
state_23332 = G__23379;
continue;
} else {
return ret_value__20872__auto__;
}
break;
}
});
cljs$core$async$state_machine__20871__auto__ = function(state_23332){
switch(arguments.length){
case 0:
return cljs$core$async$state_machine__20871__auto____0.call(this);
case 1:
return cljs$core$async$state_machine__20871__auto____1.call(this,state_23332);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$state_machine__20871__auto____0;
cljs$core$async$state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$state_machine__20871__auto____1;
return cljs$core$async$state_machine__20871__auto__;
})()
;})(switch__20870__auto__,c__20932__auto___23363,out))
})();
var state__20934__auto__ = (function (){var statearr_23361 = f__20933__auto__.call(null);
(statearr_23361[cljs.core.async.impl.ioc_helpers.USER_START_IDX] = c__20932__auto___23363);

return statearr_23361;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped.call(null,state__20934__auto__);
});})(c__20932__auto___23363,out))
);


return out;
});

cljs.core.async.partition.cljs$lang$maxFixedArity = 3;
/**
 * Deprecated - this function will be removed. Use transducer instead
 */
cljs.core.async.partition_by = (function cljs$core$async$partition_by(){
var G__23381 = arguments.length;
switch (G__23381) {
case 2:
return cljs.core.async.partition_by.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return cljs.core.async.partition_by.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

cljs.core.async.partition_by.cljs$core$IFn$_invoke$arity$2 = (function (f,ch){
return cljs.core.async.partition_by.call(null,f,ch,null);
});

cljs.core.async.partition_by.cljs$core$IFn$_invoke$arity$3 = (function (f,ch,buf_or_n){
var out = cljs.core.async.chan.call(null,buf_or_n);
var c__20932__auto___23454 = cljs.core.async.chan.call(null,(1));
cljs.core.async.impl.dispatch.run.call(null,((function (c__20932__auto___23454,out){
return (function (){
var f__20933__auto__ = (function (){var switch__20870__auto__ = ((function (c__20932__auto___23454,out){
return (function (state_23423){
var state_val_23424 = (state_23423[(1)]);
if((state_val_23424 === (7))){
var inst_23419 = (state_23423[(2)]);
var state_23423__$1 = state_23423;
var statearr_23425_23455 = state_23423__$1;
(statearr_23425_23455[(2)] = inst_23419);

(statearr_23425_23455[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23424 === (1))){
var inst_23382 = [];
var inst_23383 = inst_23382;
var inst_23384 = new cljs.core.Keyword("cljs.core.async","nothing","cljs.core.async/nothing",-69252123);
var state_23423__$1 = (function (){var statearr_23426 = state_23423;
(statearr_23426[(7)] = inst_23384);

(statearr_23426[(8)] = inst_23383);

return statearr_23426;
})();
var statearr_23427_23456 = state_23423__$1;
(statearr_23427_23456[(2)] = null);

(statearr_23427_23456[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23424 === (4))){
var inst_23387 = (state_23423[(9)]);
var inst_23387__$1 = (state_23423[(2)]);
var inst_23388 = (inst_23387__$1 == null);
var inst_23389 = cljs.core.not.call(null,inst_23388);
var state_23423__$1 = (function (){var statearr_23428 = state_23423;
(statearr_23428[(9)] = inst_23387__$1);

return statearr_23428;
})();
if(inst_23389){
var statearr_23429_23457 = state_23423__$1;
(statearr_23429_23457[(1)] = (5));

} else {
var statearr_23430_23458 = state_23423__$1;
(statearr_23430_23458[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23424 === (15))){
var inst_23413 = (state_23423[(2)]);
var state_23423__$1 = state_23423;
var statearr_23431_23459 = state_23423__$1;
(statearr_23431_23459[(2)] = inst_23413);

(statearr_23431_23459[(1)] = (14));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23424 === (13))){
var state_23423__$1 = state_23423;
var statearr_23432_23460 = state_23423__$1;
(statearr_23432_23460[(2)] = null);

(statearr_23432_23460[(1)] = (14));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23424 === (6))){
var inst_23383 = (state_23423[(8)]);
var inst_23408 = inst_23383.length;
var inst_23409 = (inst_23408 > (0));
var state_23423__$1 = state_23423;
if(cljs.core.truth_(inst_23409)){
var statearr_23433_23461 = state_23423__$1;
(statearr_23433_23461[(1)] = (12));

} else {
var statearr_23434_23462 = state_23423__$1;
(statearr_23434_23462[(1)] = (13));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23424 === (3))){
var inst_23421 = (state_23423[(2)]);
var state_23423__$1 = state_23423;
return cljs.core.async.impl.ioc_helpers.return_chan.call(null,state_23423__$1,inst_23421);
} else {
if((state_val_23424 === (12))){
var inst_23383 = (state_23423[(8)]);
var inst_23411 = cljs.core.vec.call(null,inst_23383);
var state_23423__$1 = state_23423;
return cljs.core.async.impl.ioc_helpers.put_BANG_.call(null,state_23423__$1,(15),out,inst_23411);
} else {
if((state_val_23424 === (2))){
var state_23423__$1 = state_23423;
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_23423__$1,(4),ch);
} else {
if((state_val_23424 === (11))){
var inst_23387 = (state_23423[(9)]);
var inst_23391 = (state_23423[(10)]);
var inst_23401 = (state_23423[(2)]);
var inst_23402 = [];
var inst_23403 = inst_23402.push(inst_23387);
var inst_23383 = inst_23402;
var inst_23384 = inst_23391;
var state_23423__$1 = (function (){var statearr_23435 = state_23423;
(statearr_23435[(11)] = inst_23401);

(statearr_23435[(12)] = inst_23403);

(statearr_23435[(7)] = inst_23384);

(statearr_23435[(8)] = inst_23383);

return statearr_23435;
})();
var statearr_23436_23463 = state_23423__$1;
(statearr_23436_23463[(2)] = null);

(statearr_23436_23463[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23424 === (9))){
var inst_23383 = (state_23423[(8)]);
var inst_23399 = cljs.core.vec.call(null,inst_23383);
var state_23423__$1 = state_23423;
return cljs.core.async.impl.ioc_helpers.put_BANG_.call(null,state_23423__$1,(11),out,inst_23399);
} else {
if((state_val_23424 === (5))){
var inst_23387 = (state_23423[(9)]);
var inst_23391 = (state_23423[(10)]);
var inst_23384 = (state_23423[(7)]);
var inst_23391__$1 = f.call(null,inst_23387);
var inst_23392 = cljs.core._EQ_.call(null,inst_23391__$1,inst_23384);
var inst_23393 = cljs.core.keyword_identical_QMARK_.call(null,inst_23384,new cljs.core.Keyword("cljs.core.async","nothing","cljs.core.async/nothing",-69252123));
var inst_23394 = (inst_23392) || (inst_23393);
var state_23423__$1 = (function (){var statearr_23437 = state_23423;
(statearr_23437[(10)] = inst_23391__$1);

return statearr_23437;
})();
if(cljs.core.truth_(inst_23394)){
var statearr_23438_23464 = state_23423__$1;
(statearr_23438_23464[(1)] = (8));

} else {
var statearr_23439_23465 = state_23423__$1;
(statearr_23439_23465[(1)] = (9));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23424 === (14))){
var inst_23416 = (state_23423[(2)]);
var inst_23417 = cljs.core.async.close_BANG_.call(null,out);
var state_23423__$1 = (function (){var statearr_23441 = state_23423;
(statearr_23441[(13)] = inst_23416);

return statearr_23441;
})();
var statearr_23442_23466 = state_23423__$1;
(statearr_23442_23466[(2)] = inst_23417);

(statearr_23442_23466[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23424 === (10))){
var inst_23406 = (state_23423[(2)]);
var state_23423__$1 = state_23423;
var statearr_23443_23467 = state_23423__$1;
(statearr_23443_23467[(2)] = inst_23406);

(statearr_23443_23467[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_23424 === (8))){
var inst_23387 = (state_23423[(9)]);
var inst_23391 = (state_23423[(10)]);
var inst_23383 = (state_23423[(8)]);
var inst_23396 = inst_23383.push(inst_23387);
var tmp23440 = inst_23383;
var inst_23383__$1 = tmp23440;
var inst_23384 = inst_23391;
var state_23423__$1 = (function (){var statearr_23444 = state_23423;
(statearr_23444[(7)] = inst_23384);

(statearr_23444[(8)] = inst_23383__$1);

(statearr_23444[(14)] = inst_23396);

return statearr_23444;
})();
var statearr_23445_23468 = state_23423__$1;
(statearr_23445_23468[(2)] = null);

(statearr_23445_23468[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
return null;
}
}
}
}
}
}
}
}
}
}
}
}
}
}
}
});})(c__20932__auto___23454,out))
;
return ((function (switch__20870__auto__,c__20932__auto___23454,out){
return (function() {
var cljs$core$async$state_machine__20871__auto__ = null;
var cljs$core$async$state_machine__20871__auto____0 = (function (){
var statearr_23449 = [null,null,null,null,null,null,null,null,null,null,null,null,null,null,null];
(statearr_23449[(0)] = cljs$core$async$state_machine__20871__auto__);

(statearr_23449[(1)] = (1));

return statearr_23449;
});
var cljs$core$async$state_machine__20871__auto____1 = (function (state_23423){
while(true){
var ret_value__20872__auto__ = (function (){try{while(true){
var result__20873__auto__ = switch__20870__auto__.call(null,state_23423);
if(cljs.core.keyword_identical_QMARK_.call(null,result__20873__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__20873__auto__;
}
break;
}
}catch (e23450){if((e23450 instanceof Object)){
var ex__20874__auto__ = e23450;
var statearr_23451_23469 = state_23423;
(statearr_23451_23469[(5)] = ex__20874__auto__);


cljs.core.async.impl.ioc_helpers.process_exception.call(null,state_23423);

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
throw e23450;

}
}})();
if(cljs.core.keyword_identical_QMARK_.call(null,ret_value__20872__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__23470 = state_23423;
state_23423 = G__23470;
continue;
} else {
return ret_value__20872__auto__;
}
break;
}
});
cljs$core$async$state_machine__20871__auto__ = function(state_23423){
switch(arguments.length){
case 0:
return cljs$core$async$state_machine__20871__auto____0.call(this);
case 1:
return cljs$core$async$state_machine__20871__auto____1.call(this,state_23423);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
cljs$core$async$state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$0 = cljs$core$async$state_machine__20871__auto____0;
cljs$core$async$state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$1 = cljs$core$async$state_machine__20871__auto____1;
return cljs$core$async$state_machine__20871__auto__;
})()
;})(switch__20870__auto__,c__20932__auto___23454,out))
})();
var state__20934__auto__ = (function (){var statearr_23452 = f__20933__auto__.call(null);
(statearr_23452[cljs.core.async.impl.ioc_helpers.USER_START_IDX] = c__20932__auto___23454);

return statearr_23452;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped.call(null,state__20934__auto__);
});})(c__20932__auto___23454,out))
);


return out;
});

cljs.core.async.partition_by.cljs$lang$maxFixedArity = 3;

//# sourceMappingURL=async.js.map?rel=1431620928679