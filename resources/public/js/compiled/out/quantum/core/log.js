// Compiled by ClojureScript 0.0-3269 {}
goog.provide('quantum.core.log');
goog.require('cljs.core');
goog.require('quantum.core.ns');
goog.require('quantum.core.time.core');
goog.require('quantum.core.string');
goog.require('cljs.core.async');
quantum.core.log._STAR_prs_STAR_ = cljs.core.atom.call(null,new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"warn","warn",-436710552),null,new cljs.core.Keyword(null,"user","user",1532431356),null], null), null));
quantum.core.log.log = cljs.core.atom.call(null,cljs.core.PersistentVector.EMPTY);
quantum.core.log.vars = cljs.core.atom.call(null,cljs.core.PersistentArrayMap.EMPTY);
quantum.core.log.cache_BANG_ = (function quantum$core$log$cache_BANG_(k,v){
return cljs.core.swap_BANG_.call(null,quantum.core.log.vars,cljs.core.assoc,k,v);
});
quantum.core.log.statuses = cljs.core.atom.call(null,cljs.core.async.chan.call(null));
quantum.core.log.errors = cljs.core.atom.call(null,cljs.core.PersistentVector.EMPTY);
quantum.core.log.error = (function quantum$core$log$error(throw_context){
return cljs.core.swap_BANG_.call(null,quantum.core.log.errors,cljs.core.conj,cljs.core.update.call(null,throw_context,new cljs.core.Keyword(null,"stack-trace","stack-trace",-1998072032),cljs.core.vec));
});

/**
* @constructor
* @param {*} time_stamp
* @param {*} type
* @param {*} ns_source
* @param {*} message
* @param {*} __meta
* @param {*} __extmap
* @param {*} __hash
* @param {*=} __meta 
* @param {*=} __extmap
* @param {number|null} __hash
*/
quantum.core.log.LogEntry = (function (time_stamp,type,ns_source,message,__meta,__extmap,__hash){
this.time_stamp = time_stamp;
this.type = type;
this.ns_source = ns_source;
this.message = message;
this.__meta = __meta;
this.__extmap = __extmap;
this.__hash = __hash;
this.cljs$lang$protocol_mask$partition0$ = 2229667594;
this.cljs$lang$protocol_mask$partition1$ = 8192;
})
quantum.core.log.LogEntry.prototype.cljs$core$ILookup$_lookup$arity$2 = (function (this__18668__auto__,k__18669__auto__){
var self__ = this;
var this__18668__auto____$1 = this;
return cljs.core._lookup.call(null,this__18668__auto____$1,k__18669__auto__,null);
});

quantum.core.log.LogEntry.prototype.cljs$core$ILookup$_lookup$arity$3 = (function (this__18670__auto__,k25835,else__18671__auto__){
var self__ = this;
var this__18670__auto____$1 = this;
var G__25837 = (((k25835 instanceof cljs.core.Keyword))?k25835.fqn:null);
switch (G__25837) {
case "time-stamp":
return self__.time_stamp;

break;
case "type":
return self__.type;

break;
case "ns-source":
return self__.ns_source;

break;
case "message":
return self__.message;

break;
default:
return cljs.core.get.call(null,self__.__extmap,k25835,else__18671__auto__);

}
});

quantum.core.log.LogEntry.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this__18682__auto__,writer__18683__auto__,opts__18684__auto__){
var self__ = this;
var this__18682__auto____$1 = this;
var pr_pair__18685__auto__ = ((function (this__18682__auto____$1){
return (function (keyval__18686__auto__){
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,cljs.core.pr_writer,""," ","",opts__18684__auto__,keyval__18686__auto__);
});})(this__18682__auto____$1))
;
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,pr_pair__18685__auto__,"#quantum.core.log.LogEntry{",", ","}",opts__18684__auto__,cljs.core.concat.call(null,new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Keyword(null,"time-stamp","time-stamp",-1161230692),self__.time_stamp],null)),(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Keyword(null,"type","type",1174270348),self__.type],null)),(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Keyword(null,"ns-source","ns-source",692633294),self__.ns_source],null)),(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Keyword(null,"message","message",-406056002),self__.message],null))], null),self__.__extmap));
});

quantum.core.log.LogEntry.prototype.cljs$core$IMeta$_meta$arity$1 = (function (this__18666__auto__){
var self__ = this;
var this__18666__auto____$1 = this;
return self__.__meta;
});

quantum.core.log.LogEntry.prototype.cljs$core$ICloneable$_clone$arity$1 = (function (this__18662__auto__){
var self__ = this;
var this__18662__auto____$1 = this;
return (new quantum.core.log.LogEntry(self__.time_stamp,self__.type,self__.ns_source,self__.message,self__.__meta,self__.__extmap,self__.__hash));
});

quantum.core.log.LogEntry.prototype.cljs$core$ICounted$_count$arity$1 = (function (this__18672__auto__){
var self__ = this;
var this__18672__auto____$1 = this;
return (4 + cljs.core.count.call(null,self__.__extmap));
});

quantum.core.log.LogEntry.prototype.cljs$core$IHash$_hash$arity$1 = (function (this__18663__auto__){
var self__ = this;
var this__18663__auto____$1 = this;
var h__18489__auto__ = self__.__hash;
if(!((h__18489__auto__ == null))){
return h__18489__auto__;
} else {
var h__18489__auto____$1 = cljs.core.hash_imap.call(null,this__18663__auto____$1);
self__.__hash = h__18489__auto____$1;

return h__18489__auto____$1;
}
});

quantum.core.log.LogEntry.prototype.cljs$core$IEquiv$_equiv$arity$2 = (function (this__18664__auto__,other__18665__auto__){
var self__ = this;
var this__18664__auto____$1 = this;
if(cljs.core.truth_((function (){var and__18061__auto__ = other__18665__auto__;
if(cljs.core.truth_(and__18061__auto__)){
var and__18061__auto____$1 = (this__18664__auto____$1.constructor === other__18665__auto__.constructor);
if(and__18061__auto____$1){
return cljs.core.equiv_map.call(null,this__18664__auto____$1,other__18665__auto__);
} else {
return and__18061__auto____$1;
}
} else {
return and__18061__auto__;
}
})())){
return true;
} else {
return false;
}
});

quantum.core.log.LogEntry.prototype.cljs$core$IMap$_dissoc$arity$2 = (function (this__18677__auto__,k__18678__auto__){
var self__ = this;
var this__18677__auto____$1 = this;
if(cljs.core.contains_QMARK_.call(null,new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"type","type",1174270348),null,new cljs.core.Keyword(null,"ns-source","ns-source",692633294),null,new cljs.core.Keyword(null,"time-stamp","time-stamp",-1161230692),null,new cljs.core.Keyword(null,"message","message",-406056002),null], null), null),k__18678__auto__)){
return cljs.core.dissoc.call(null,cljs.core.with_meta.call(null,cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,this__18677__auto____$1),self__.__meta),k__18678__auto__);
} else {
return (new quantum.core.log.LogEntry(self__.time_stamp,self__.type,self__.ns_source,self__.message,self__.__meta,cljs.core.not_empty.call(null,cljs.core.dissoc.call(null,self__.__extmap,k__18678__auto__)),null));
}
});

quantum.core.log.LogEntry.prototype.cljs$core$IAssociative$_assoc$arity$3 = (function (this__18675__auto__,k__18676__auto__,G__25834){
var self__ = this;
var this__18675__auto____$1 = this;
var pred__25838 = cljs.core.keyword_identical_QMARK_;
var expr__25839 = k__18676__auto__;
if(cljs.core.truth_(pred__25838.call(null,new cljs.core.Keyword(null,"time-stamp","time-stamp",-1161230692),expr__25839))){
return (new quantum.core.log.LogEntry(G__25834,self__.type,self__.ns_source,self__.message,self__.__meta,self__.__extmap,null));
} else {
if(cljs.core.truth_(pred__25838.call(null,new cljs.core.Keyword(null,"type","type",1174270348),expr__25839))){
return (new quantum.core.log.LogEntry(self__.time_stamp,G__25834,self__.ns_source,self__.message,self__.__meta,self__.__extmap,null));
} else {
if(cljs.core.truth_(pred__25838.call(null,new cljs.core.Keyword(null,"ns-source","ns-source",692633294),expr__25839))){
return (new quantum.core.log.LogEntry(self__.time_stamp,self__.type,G__25834,self__.message,self__.__meta,self__.__extmap,null));
} else {
if(cljs.core.truth_(pred__25838.call(null,new cljs.core.Keyword(null,"message","message",-406056002),expr__25839))){
return (new quantum.core.log.LogEntry(self__.time_stamp,self__.type,self__.ns_source,G__25834,self__.__meta,self__.__extmap,null));
} else {
return (new quantum.core.log.LogEntry(self__.time_stamp,self__.type,self__.ns_source,self__.message,self__.__meta,cljs.core.assoc.call(null,self__.__extmap,k__18676__auto__,G__25834),null));
}
}
}
}
});

quantum.core.log.LogEntry.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (this__18680__auto__){
var self__ = this;
var this__18680__auto____$1 = this;
return cljs.core.seq.call(null,cljs.core.concat.call(null,new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Keyword(null,"time-stamp","time-stamp",-1161230692),self__.time_stamp],null)),(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Keyword(null,"type","type",1174270348),self__.type],null)),(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Keyword(null,"ns-source","ns-source",692633294),self__.ns_source],null)),(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Keyword(null,"message","message",-406056002),self__.message],null))], null),self__.__extmap));
});

quantum.core.log.LogEntry.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (this__18667__auto__,G__25834){
var self__ = this;
var this__18667__auto____$1 = this;
return (new quantum.core.log.LogEntry(self__.time_stamp,self__.type,self__.ns_source,self__.message,G__25834,self__.__extmap,self__.__hash));
});

quantum.core.log.LogEntry.prototype.cljs$core$ICollection$_conj$arity$2 = (function (this__18673__auto__,entry__18674__auto__){
var self__ = this;
var this__18673__auto____$1 = this;
if(cljs.core.vector_QMARK_.call(null,entry__18674__auto__)){
return cljs.core._assoc.call(null,this__18673__auto____$1,cljs.core._nth.call(null,entry__18674__auto__,(0)),cljs.core._nth.call(null,entry__18674__auto__,(1)));
} else {
return cljs.core.reduce.call(null,cljs.core._conj,this__18673__auto____$1,entry__18674__auto__);
}
});

quantum.core.log.LogEntry.getBasis = (function (){
return new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"time-stamp","time-stamp",479300835,null),new cljs.core.Symbol(null,"type","type",-1480165421,null),new cljs.core.Symbol(null,"ns-source","ns-source",-1961802475,null),new cljs.core.Symbol(null,"message","message",1234475525,null)], null);
});

quantum.core.log.LogEntry.cljs$lang$type = true;

quantum.core.log.LogEntry.cljs$lang$ctorPrSeq = (function (this__18702__auto__){
return cljs.core._conj.call(null,cljs.core.List.EMPTY,"quantum.core.log/LogEntry");
});

quantum.core.log.LogEntry.cljs$lang$ctorPrWriter = (function (this__18702__auto__,writer__18703__auto__){
return cljs.core._write.call(null,writer__18703__auto__,"quantum.core.log/LogEntry");
});

quantum.core.log.__GT_LogEntry = (function quantum$core$log$__GT_LogEntry(time_stamp,type,ns_source,message){
return (new quantum.core.log.LogEntry(time_stamp,type,ns_source,message,null,null,null));
});

quantum.core.log.map__GT_LogEntry = (function quantum$core$log$map__GT_LogEntry(G__25836){
return (new quantum.core.log.LogEntry(new cljs.core.Keyword(null,"time-stamp","time-stamp",-1161230692).cljs$core$IFn$_invoke$arity$1(G__25836),new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(G__25836),new cljs.core.Keyword(null,"ns-source","ns-source",692633294).cljs$core$IFn$_invoke$arity$1(G__25836),new cljs.core.Keyword(null,"message","message",-406056002).cljs$core$IFn$_invoke$arity$1(G__25836),null,cljs.core.dissoc.call(null,G__25836,new cljs.core.Keyword(null,"time-stamp","time-stamp",-1161230692),new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"ns-source","ns-source",692633294),new cljs.core.Keyword(null,"message","message",-406056002)),null));
});

quantum.core.log.disable_BANG_ = (function quantum$core$log$disable_BANG_(){
var G__25845 = arguments.length;
switch (G__25845) {
case 1:
return quantum.core.log.disable_BANG_.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
default:
var argseq__19124__auto__ = (new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(1)),(0)));
return quantum.core.log.disable_BANG_.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__19124__auto__);

}
});

quantum.core.log.disable_BANG_.cljs$core$IFn$_invoke$arity$1 = (function (pr_type){
return cljs.core.swap_BANG_.call(null,quantum.core.log._STAR_prs_STAR_,cljs.core.disj,pr_type);
});

quantum.core.log.disable_BANG_.cljs$core$IFn$_invoke$arity$variadic = (function (pr_type,pr_types){
return cljs.core.apply.call(null,cljs.core.swap_BANG_,quantum.core.log._STAR_prs_STAR_,cljs.core.disj,pr_type,pr_types);
});

quantum.core.log.disable_BANG_.cljs$lang$applyTo = (function (seq25842){
var G__25843 = cljs.core.first.call(null,seq25842);
var seq25842__$1 = cljs.core.next.call(null,seq25842);
return quantum.core.log.disable_BANG_.cljs$core$IFn$_invoke$arity$variadic(G__25843,seq25842__$1);
});

quantum.core.log.disable_BANG_.cljs$lang$maxFixedArity = (1);
quantum.core.log.enable_BANG_ = (function quantum$core$log$enable_BANG_(){
var G__25850 = arguments.length;
switch (G__25850) {
case 1:
return quantum.core.log.enable_BANG_.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
default:
var argseq__19124__auto__ = (new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(1)),(0)));
return quantum.core.log.enable_BANG_.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__19124__auto__);

}
});

quantum.core.log.enable_BANG_.cljs$core$IFn$_invoke$arity$1 = (function (pr_type){
return cljs.core.swap_BANG_.call(null,quantum.core.log._STAR_prs_STAR_,cljs.core.conj,pr_type);
});

quantum.core.log.enable_BANG_.cljs$core$IFn$_invoke$arity$variadic = (function (pr_type,pr_types){
return cljs.core.apply.call(null,cljs.core.swap_BANG_,quantum.core.log._STAR_prs_STAR_,cljs.core.conj,pr_type,pr_types);
});

quantum.core.log.enable_BANG_.cljs$lang$applyTo = (function (seq25847){
var G__25848 = cljs.core.first.call(null,seq25847);
var seq25847__$1 = cljs.core.next.call(null,seq25847);
return quantum.core.log.enable_BANG_.cljs$core$IFn$_invoke$arity$variadic(G__25848,seq25847__$1);
});

quantum.core.log.enable_BANG_.cljs$lang$maxFixedArity = (1);

//# sourceMappingURL=log.js.map?rel=1431625571714