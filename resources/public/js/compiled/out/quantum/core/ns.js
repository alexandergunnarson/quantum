// Compiled by ClojureScript 0.0-3269 {}
goog.provide('quantum.core.ns');
goog.require('cljs.core');
goog.require('clojure.core.rrb_vector');
goog.require('cljs.core');
quantum.core.ns.ANil = null;
quantum.core.ns.AKey = cljs.core.Keyword;
quantum.core.ns.ANum = Number;
quantum.core.ns.AExactNum = Number;
quantum.core.ns.AInt = Number;
quantum.core.ns.ADouble = Number;
quantum.core.ns.ADecimal = Number;
quantum.core.ns.ASet = cljs.core.PersistentHashSet;
quantum.core.ns.ABool = Boolean;
quantum.core.ns.AArrList = cljs.core.ArrayList;
quantum.core.ns.ATreeMap = cljs.core.PersistentTreeMap;
quantum.core.ns.ALSeq = cljs.core.LazySeq;
quantum.core.ns.AVec = cljs.core.PersistentVector;
quantum.core.ns.AMEntry = cljs.core.Vec;
quantum.core.ns.ARegex = RegExp;
quantum.core.ns.AEditable = cljs.core.IEditableCollection;
quantum.core.ns.ATransient = cljs.core.ITransientCollection;
quantum.core.ns.AQueue = cljs.core.PersistentQueue;
quantum.core.ns.AMap = cljs.core.IMap;
quantum.core.ns.AError = Error;

/**
* @constructor
* @param {*} __meta
* @param {*} __extmap
* @param {*} __hash
* @param {*=} __meta 
* @param {*=} __extmap
* @param {number|null} __hash
*/
quantum.core.ns.Nil = (function (__meta,__extmap,__hash){
this.__meta = __meta;
this.__extmap = __extmap;
this.__hash = __hash;
this.cljs$lang$protocol_mask$partition0$ = 2229667594;
this.cljs$lang$protocol_mask$partition1$ = 8192;
})
quantum.core.ns.Nil.prototype.cljs$core$ILookup$_lookup$arity$2 = (function (this__18668__auto__,k__18669__auto__){
var self__ = this;
var this__18668__auto____$1 = this;
return cljs.core._lookup.call(null,this__18668__auto____$1,k__18669__auto__,null);
});

quantum.core.ns.Nil.prototype.cljs$core$ILookup$_lookup$arity$3 = (function (this__18670__auto__,k19181,else__18671__auto__){
var self__ = this;
var this__18670__auto____$1 = this;
var G__19183 = k19181;
switch (G__19183) {
default:
return cljs.core.get.call(null,self__.__extmap,k19181,else__18671__auto__);

}
});

quantum.core.ns.Nil.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this__18682__auto__,writer__18683__auto__,opts__18684__auto__){
var self__ = this;
var this__18682__auto____$1 = this;
var pr_pair__18685__auto__ = ((function (this__18682__auto____$1){
return (function (keyval__18686__auto__){
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,cljs.core.pr_writer,""," ","",opts__18684__auto__,keyval__18686__auto__);
});})(this__18682__auto____$1))
;
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,pr_pair__18685__auto__,"#quantum.core.ns.Nil{",", ","}",opts__18684__auto__,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.Nil.prototype.cljs$core$IMeta$_meta$arity$1 = (function (this__18666__auto__){
var self__ = this;
var this__18666__auto____$1 = this;
return self__.__meta;
});

quantum.core.ns.Nil.prototype.cljs$core$ICloneable$_clone$arity$1 = (function (this__18662__auto__){
var self__ = this;
var this__18662__auto____$1 = this;
return (new quantum.core.ns.Nil(self__.__meta,self__.__extmap,self__.__hash));
});

quantum.core.ns.Nil.prototype.cljs$core$ICounted$_count$arity$1 = (function (this__18672__auto__){
var self__ = this;
var this__18672__auto____$1 = this;
return (0 + cljs.core.count.call(null,self__.__extmap));
});

quantum.core.ns.Nil.prototype.cljs$core$IHash$_hash$arity$1 = (function (this__18663__auto__){
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

quantum.core.ns.Nil.prototype.cljs$core$IEquiv$_equiv$arity$2 = (function (this__18664__auto__,other__18665__auto__){
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

quantum.core.ns.Nil.prototype.cljs$core$IMap$_dissoc$arity$2 = (function (this__18677__auto__,k__18678__auto__){
var self__ = this;
var this__18677__auto____$1 = this;
if(cljs.core.contains_QMARK_.call(null,cljs.core.PersistentHashSet.EMPTY,k__18678__auto__)){
return cljs.core.dissoc.call(null,cljs.core.with_meta.call(null,cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,this__18677__auto____$1),self__.__meta),k__18678__auto__);
} else {
return (new quantum.core.ns.Nil(self__.__meta,cljs.core.not_empty.call(null,cljs.core.dissoc.call(null,self__.__extmap,k__18678__auto__)),null));
}
});

quantum.core.ns.Nil.prototype.cljs$core$IAssociative$_assoc$arity$3 = (function (this__18675__auto__,k__18676__auto__,G__19180){
var self__ = this;
var this__18675__auto____$1 = this;
var pred__19184 = cljs.core.keyword_identical_QMARK_;
var expr__19185 = k__18676__auto__;
return (new quantum.core.ns.Nil(self__.__meta,cljs.core.assoc.call(null,self__.__extmap,k__18676__auto__,G__19180),null));
});

quantum.core.ns.Nil.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (this__18680__auto__){
var self__ = this;
var this__18680__auto____$1 = this;
return cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.Nil.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (this__18667__auto__,G__19180){
var self__ = this;
var this__18667__auto____$1 = this;
return (new quantum.core.ns.Nil(G__19180,self__.__extmap,self__.__hash));
});

quantum.core.ns.Nil.prototype.cljs$core$ICollection$_conj$arity$2 = (function (this__18673__auto__,entry__18674__auto__){
var self__ = this;
var this__18673__auto____$1 = this;
if(cljs.core.vector_QMARK_.call(null,entry__18674__auto__)){
return cljs.core._assoc.call(null,this__18673__auto____$1,cljs.core._nth.call(null,entry__18674__auto__,(0)),cljs.core._nth.call(null,entry__18674__auto__,(1)));
} else {
return cljs.core.reduce.call(null,cljs.core._conj,this__18673__auto____$1,entry__18674__auto__);
}
});

quantum.core.ns.Nil.getBasis = (function (){
return cljs.core.PersistentVector.EMPTY;
});

quantum.core.ns.Nil.cljs$lang$type = true;

quantum.core.ns.Nil.cljs$lang$ctorPrSeq = (function (this__18702__auto__){
return cljs.core._conj.call(null,cljs.core.List.EMPTY,"quantum.core.ns/Nil");
});

quantum.core.ns.Nil.cljs$lang$ctorPrWriter = (function (this__18702__auto__,writer__18703__auto__){
return cljs.core._write.call(null,writer__18703__auto__,"quantum.core.ns/Nil");
});

quantum.core.ns.__GT_Nil = (function quantum$core$ns$__GT_Nil(){
return (new quantum.core.ns.Nil(null,null,null));
});

quantum.core.ns.map__GT_Nil = (function quantum$core$ns$map__GT_Nil(G__19182){
return (new quantum.core.ns.Nil(null,cljs.core.dissoc.call(null,G__19182),null));
});


/**
* @constructor
* @param {*} __meta
* @param {*} __extmap
* @param {*} __hash
* @param {*=} __meta 
* @param {*=} __extmap
* @param {number|null} __hash
*/
quantum.core.ns.Key = (function (__meta,__extmap,__hash){
this.__meta = __meta;
this.__extmap = __extmap;
this.__hash = __hash;
this.cljs$lang$protocol_mask$partition0$ = 2229667594;
this.cljs$lang$protocol_mask$partition1$ = 8192;
})
quantum.core.ns.Key.prototype.cljs$core$ILookup$_lookup$arity$2 = (function (this__18668__auto__,k__18669__auto__){
var self__ = this;
var this__18668__auto____$1 = this;
return cljs.core._lookup.call(null,this__18668__auto____$1,k__18669__auto__,null);
});

quantum.core.ns.Key.prototype.cljs$core$ILookup$_lookup$arity$3 = (function (this__18670__auto__,k19189,else__18671__auto__){
var self__ = this;
var this__18670__auto____$1 = this;
var G__19191 = k19189;
switch (G__19191) {
default:
return cljs.core.get.call(null,self__.__extmap,k19189,else__18671__auto__);

}
});

quantum.core.ns.Key.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this__18682__auto__,writer__18683__auto__,opts__18684__auto__){
var self__ = this;
var this__18682__auto____$1 = this;
var pr_pair__18685__auto__ = ((function (this__18682__auto____$1){
return (function (keyval__18686__auto__){
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,cljs.core.pr_writer,""," ","",opts__18684__auto__,keyval__18686__auto__);
});})(this__18682__auto____$1))
;
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,pr_pair__18685__auto__,"#quantum.core.ns.Key{",", ","}",opts__18684__auto__,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.Key.prototype.cljs$core$IMeta$_meta$arity$1 = (function (this__18666__auto__){
var self__ = this;
var this__18666__auto____$1 = this;
return self__.__meta;
});

quantum.core.ns.Key.prototype.cljs$core$ICloneable$_clone$arity$1 = (function (this__18662__auto__){
var self__ = this;
var this__18662__auto____$1 = this;
return (new quantum.core.ns.Key(self__.__meta,self__.__extmap,self__.__hash));
});

quantum.core.ns.Key.prototype.cljs$core$ICounted$_count$arity$1 = (function (this__18672__auto__){
var self__ = this;
var this__18672__auto____$1 = this;
return (0 + cljs.core.count.call(null,self__.__extmap));
});

quantum.core.ns.Key.prototype.cljs$core$IHash$_hash$arity$1 = (function (this__18663__auto__){
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

quantum.core.ns.Key.prototype.cljs$core$IEquiv$_equiv$arity$2 = (function (this__18664__auto__,other__18665__auto__){
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

quantum.core.ns.Key.prototype.cljs$core$IMap$_dissoc$arity$2 = (function (this__18677__auto__,k__18678__auto__){
var self__ = this;
var this__18677__auto____$1 = this;
if(cljs.core.contains_QMARK_.call(null,cljs.core.PersistentHashSet.EMPTY,k__18678__auto__)){
return cljs.core.dissoc.call(null,cljs.core.with_meta.call(null,cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,this__18677__auto____$1),self__.__meta),k__18678__auto__);
} else {
return (new quantum.core.ns.Key(self__.__meta,cljs.core.not_empty.call(null,cljs.core.dissoc.call(null,self__.__extmap,k__18678__auto__)),null));
}
});

quantum.core.ns.Key.prototype.cljs$core$IAssociative$_assoc$arity$3 = (function (this__18675__auto__,k__18676__auto__,G__19188){
var self__ = this;
var this__18675__auto____$1 = this;
var pred__19192 = cljs.core.keyword_identical_QMARK_;
var expr__19193 = k__18676__auto__;
return (new quantum.core.ns.Key(self__.__meta,cljs.core.assoc.call(null,self__.__extmap,k__18676__auto__,G__19188),null));
});

quantum.core.ns.Key.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (this__18680__auto__){
var self__ = this;
var this__18680__auto____$1 = this;
return cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.Key.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (this__18667__auto__,G__19188){
var self__ = this;
var this__18667__auto____$1 = this;
return (new quantum.core.ns.Key(G__19188,self__.__extmap,self__.__hash));
});

quantum.core.ns.Key.prototype.cljs$core$ICollection$_conj$arity$2 = (function (this__18673__auto__,entry__18674__auto__){
var self__ = this;
var this__18673__auto____$1 = this;
if(cljs.core.vector_QMARK_.call(null,entry__18674__auto__)){
return cljs.core._assoc.call(null,this__18673__auto____$1,cljs.core._nth.call(null,entry__18674__auto__,(0)),cljs.core._nth.call(null,entry__18674__auto__,(1)));
} else {
return cljs.core.reduce.call(null,cljs.core._conj,this__18673__auto____$1,entry__18674__auto__);
}
});

quantum.core.ns.Key.getBasis = (function (){
return cljs.core.PersistentVector.EMPTY;
});

quantum.core.ns.Key.cljs$lang$type = true;

quantum.core.ns.Key.cljs$lang$ctorPrSeq = (function (this__18702__auto__){
return cljs.core._conj.call(null,cljs.core.List.EMPTY,"quantum.core.ns/Key");
});

quantum.core.ns.Key.cljs$lang$ctorPrWriter = (function (this__18702__auto__,writer__18703__auto__){
return cljs.core._write.call(null,writer__18703__auto__,"quantum.core.ns/Key");
});

quantum.core.ns.__GT_Key = (function quantum$core$ns$__GT_Key(){
return (new quantum.core.ns.Key(null,null,null));
});

quantum.core.ns.map__GT_Key = (function quantum$core$ns$map__GT_Key(G__19190){
return (new quantum.core.ns.Key(null,cljs.core.dissoc.call(null,G__19190),null));
});


/**
* @constructor
* @param {*} __meta
* @param {*} __extmap
* @param {*} __hash
* @param {*=} __meta 
* @param {*=} __extmap
* @param {number|null} __hash
*/
quantum.core.ns.Num = (function (__meta,__extmap,__hash){
this.__meta = __meta;
this.__extmap = __extmap;
this.__hash = __hash;
this.cljs$lang$protocol_mask$partition0$ = 2229667594;
this.cljs$lang$protocol_mask$partition1$ = 8192;
})
quantum.core.ns.Num.prototype.cljs$core$ILookup$_lookup$arity$2 = (function (this__18668__auto__,k__18669__auto__){
var self__ = this;
var this__18668__auto____$1 = this;
return cljs.core._lookup.call(null,this__18668__auto____$1,k__18669__auto__,null);
});

quantum.core.ns.Num.prototype.cljs$core$ILookup$_lookup$arity$3 = (function (this__18670__auto__,k19197,else__18671__auto__){
var self__ = this;
var this__18670__auto____$1 = this;
var G__19199 = k19197;
switch (G__19199) {
default:
return cljs.core.get.call(null,self__.__extmap,k19197,else__18671__auto__);

}
});

quantum.core.ns.Num.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this__18682__auto__,writer__18683__auto__,opts__18684__auto__){
var self__ = this;
var this__18682__auto____$1 = this;
var pr_pair__18685__auto__ = ((function (this__18682__auto____$1){
return (function (keyval__18686__auto__){
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,cljs.core.pr_writer,""," ","",opts__18684__auto__,keyval__18686__auto__);
});})(this__18682__auto____$1))
;
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,pr_pair__18685__auto__,"#quantum.core.ns.Num{",", ","}",opts__18684__auto__,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.Num.prototype.cljs$core$IMeta$_meta$arity$1 = (function (this__18666__auto__){
var self__ = this;
var this__18666__auto____$1 = this;
return self__.__meta;
});

quantum.core.ns.Num.prototype.cljs$core$ICloneable$_clone$arity$1 = (function (this__18662__auto__){
var self__ = this;
var this__18662__auto____$1 = this;
return (new quantum.core.ns.Num(self__.__meta,self__.__extmap,self__.__hash));
});

quantum.core.ns.Num.prototype.cljs$core$ICounted$_count$arity$1 = (function (this__18672__auto__){
var self__ = this;
var this__18672__auto____$1 = this;
return (0 + cljs.core.count.call(null,self__.__extmap));
});

quantum.core.ns.Num.prototype.cljs$core$IHash$_hash$arity$1 = (function (this__18663__auto__){
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

quantum.core.ns.Num.prototype.cljs$core$IEquiv$_equiv$arity$2 = (function (this__18664__auto__,other__18665__auto__){
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

quantum.core.ns.Num.prototype.cljs$core$IMap$_dissoc$arity$2 = (function (this__18677__auto__,k__18678__auto__){
var self__ = this;
var this__18677__auto____$1 = this;
if(cljs.core.contains_QMARK_.call(null,cljs.core.PersistentHashSet.EMPTY,k__18678__auto__)){
return cljs.core.dissoc.call(null,cljs.core.with_meta.call(null,cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,this__18677__auto____$1),self__.__meta),k__18678__auto__);
} else {
return (new quantum.core.ns.Num(self__.__meta,cljs.core.not_empty.call(null,cljs.core.dissoc.call(null,self__.__extmap,k__18678__auto__)),null));
}
});

quantum.core.ns.Num.prototype.cljs$core$IAssociative$_assoc$arity$3 = (function (this__18675__auto__,k__18676__auto__,G__19196){
var self__ = this;
var this__18675__auto____$1 = this;
var pred__19200 = cljs.core.keyword_identical_QMARK_;
var expr__19201 = k__18676__auto__;
return (new quantum.core.ns.Num(self__.__meta,cljs.core.assoc.call(null,self__.__extmap,k__18676__auto__,G__19196),null));
});

quantum.core.ns.Num.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (this__18680__auto__){
var self__ = this;
var this__18680__auto____$1 = this;
return cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.Num.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (this__18667__auto__,G__19196){
var self__ = this;
var this__18667__auto____$1 = this;
return (new quantum.core.ns.Num(G__19196,self__.__extmap,self__.__hash));
});

quantum.core.ns.Num.prototype.cljs$core$ICollection$_conj$arity$2 = (function (this__18673__auto__,entry__18674__auto__){
var self__ = this;
var this__18673__auto____$1 = this;
if(cljs.core.vector_QMARK_.call(null,entry__18674__auto__)){
return cljs.core._assoc.call(null,this__18673__auto____$1,cljs.core._nth.call(null,entry__18674__auto__,(0)),cljs.core._nth.call(null,entry__18674__auto__,(1)));
} else {
return cljs.core.reduce.call(null,cljs.core._conj,this__18673__auto____$1,entry__18674__auto__);
}
});

quantum.core.ns.Num.getBasis = (function (){
return cljs.core.PersistentVector.EMPTY;
});

quantum.core.ns.Num.cljs$lang$type = true;

quantum.core.ns.Num.cljs$lang$ctorPrSeq = (function (this__18702__auto__){
return cljs.core._conj.call(null,cljs.core.List.EMPTY,"quantum.core.ns/Num");
});

quantum.core.ns.Num.cljs$lang$ctorPrWriter = (function (this__18702__auto__,writer__18703__auto__){
return cljs.core._write.call(null,writer__18703__auto__,"quantum.core.ns/Num");
});

quantum.core.ns.__GT_Num = (function quantum$core$ns$__GT_Num(){
return (new quantum.core.ns.Num(null,null,null));
});

quantum.core.ns.map__GT_Num = (function quantum$core$ns$map__GT_Num(G__19198){
return (new quantum.core.ns.Num(null,cljs.core.dissoc.call(null,G__19198),null));
});


/**
* @constructor
* @param {*} __meta
* @param {*} __extmap
* @param {*} __hash
* @param {*=} __meta 
* @param {*=} __extmap
* @param {number|null} __hash
*/
quantum.core.ns.ExactNum = (function (__meta,__extmap,__hash){
this.__meta = __meta;
this.__extmap = __extmap;
this.__hash = __hash;
this.cljs$lang$protocol_mask$partition0$ = 2229667594;
this.cljs$lang$protocol_mask$partition1$ = 8192;
})
quantum.core.ns.ExactNum.prototype.cljs$core$ILookup$_lookup$arity$2 = (function (this__18668__auto__,k__18669__auto__){
var self__ = this;
var this__18668__auto____$1 = this;
return cljs.core._lookup.call(null,this__18668__auto____$1,k__18669__auto__,null);
});

quantum.core.ns.ExactNum.prototype.cljs$core$ILookup$_lookup$arity$3 = (function (this__18670__auto__,k19205,else__18671__auto__){
var self__ = this;
var this__18670__auto____$1 = this;
var G__19207 = k19205;
switch (G__19207) {
default:
return cljs.core.get.call(null,self__.__extmap,k19205,else__18671__auto__);

}
});

quantum.core.ns.ExactNum.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this__18682__auto__,writer__18683__auto__,opts__18684__auto__){
var self__ = this;
var this__18682__auto____$1 = this;
var pr_pair__18685__auto__ = ((function (this__18682__auto____$1){
return (function (keyval__18686__auto__){
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,cljs.core.pr_writer,""," ","",opts__18684__auto__,keyval__18686__auto__);
});})(this__18682__auto____$1))
;
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,pr_pair__18685__auto__,"#quantum.core.ns.ExactNum{",", ","}",opts__18684__auto__,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.ExactNum.prototype.cljs$core$IMeta$_meta$arity$1 = (function (this__18666__auto__){
var self__ = this;
var this__18666__auto____$1 = this;
return self__.__meta;
});

quantum.core.ns.ExactNum.prototype.cljs$core$ICloneable$_clone$arity$1 = (function (this__18662__auto__){
var self__ = this;
var this__18662__auto____$1 = this;
return (new quantum.core.ns.ExactNum(self__.__meta,self__.__extmap,self__.__hash));
});

quantum.core.ns.ExactNum.prototype.cljs$core$ICounted$_count$arity$1 = (function (this__18672__auto__){
var self__ = this;
var this__18672__auto____$1 = this;
return (0 + cljs.core.count.call(null,self__.__extmap));
});

quantum.core.ns.ExactNum.prototype.cljs$core$IHash$_hash$arity$1 = (function (this__18663__auto__){
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

quantum.core.ns.ExactNum.prototype.cljs$core$IEquiv$_equiv$arity$2 = (function (this__18664__auto__,other__18665__auto__){
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

quantum.core.ns.ExactNum.prototype.cljs$core$IMap$_dissoc$arity$2 = (function (this__18677__auto__,k__18678__auto__){
var self__ = this;
var this__18677__auto____$1 = this;
if(cljs.core.contains_QMARK_.call(null,cljs.core.PersistentHashSet.EMPTY,k__18678__auto__)){
return cljs.core.dissoc.call(null,cljs.core.with_meta.call(null,cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,this__18677__auto____$1),self__.__meta),k__18678__auto__);
} else {
return (new quantum.core.ns.ExactNum(self__.__meta,cljs.core.not_empty.call(null,cljs.core.dissoc.call(null,self__.__extmap,k__18678__auto__)),null));
}
});

quantum.core.ns.ExactNum.prototype.cljs$core$IAssociative$_assoc$arity$3 = (function (this__18675__auto__,k__18676__auto__,G__19204){
var self__ = this;
var this__18675__auto____$1 = this;
var pred__19208 = cljs.core.keyword_identical_QMARK_;
var expr__19209 = k__18676__auto__;
return (new quantum.core.ns.ExactNum(self__.__meta,cljs.core.assoc.call(null,self__.__extmap,k__18676__auto__,G__19204),null));
});

quantum.core.ns.ExactNum.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (this__18680__auto__){
var self__ = this;
var this__18680__auto____$1 = this;
return cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.ExactNum.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (this__18667__auto__,G__19204){
var self__ = this;
var this__18667__auto____$1 = this;
return (new quantum.core.ns.ExactNum(G__19204,self__.__extmap,self__.__hash));
});

quantum.core.ns.ExactNum.prototype.cljs$core$ICollection$_conj$arity$2 = (function (this__18673__auto__,entry__18674__auto__){
var self__ = this;
var this__18673__auto____$1 = this;
if(cljs.core.vector_QMARK_.call(null,entry__18674__auto__)){
return cljs.core._assoc.call(null,this__18673__auto____$1,cljs.core._nth.call(null,entry__18674__auto__,(0)),cljs.core._nth.call(null,entry__18674__auto__,(1)));
} else {
return cljs.core.reduce.call(null,cljs.core._conj,this__18673__auto____$1,entry__18674__auto__);
}
});

quantum.core.ns.ExactNum.getBasis = (function (){
return cljs.core.PersistentVector.EMPTY;
});

quantum.core.ns.ExactNum.cljs$lang$type = true;

quantum.core.ns.ExactNum.cljs$lang$ctorPrSeq = (function (this__18702__auto__){
return cljs.core._conj.call(null,cljs.core.List.EMPTY,"quantum.core.ns/ExactNum");
});

quantum.core.ns.ExactNum.cljs$lang$ctorPrWriter = (function (this__18702__auto__,writer__18703__auto__){
return cljs.core._write.call(null,writer__18703__auto__,"quantum.core.ns/ExactNum");
});

quantum.core.ns.__GT_ExactNum = (function quantum$core$ns$__GT_ExactNum(){
return (new quantum.core.ns.ExactNum(null,null,null));
});

quantum.core.ns.map__GT_ExactNum = (function quantum$core$ns$map__GT_ExactNum(G__19206){
return (new quantum.core.ns.ExactNum(null,cljs.core.dissoc.call(null,G__19206),null));
});


/**
* @constructor
* @param {*} __meta
* @param {*} __extmap
* @param {*} __hash
* @param {*=} __meta 
* @param {*=} __extmap
* @param {number|null} __hash
*/
quantum.core.ns.Int = (function (__meta,__extmap,__hash){
this.__meta = __meta;
this.__extmap = __extmap;
this.__hash = __hash;
this.cljs$lang$protocol_mask$partition0$ = 2229667594;
this.cljs$lang$protocol_mask$partition1$ = 8192;
})
quantum.core.ns.Int.prototype.cljs$core$ILookup$_lookup$arity$2 = (function (this__18668__auto__,k__18669__auto__){
var self__ = this;
var this__18668__auto____$1 = this;
return cljs.core._lookup.call(null,this__18668__auto____$1,k__18669__auto__,null);
});

quantum.core.ns.Int.prototype.cljs$core$ILookup$_lookup$arity$3 = (function (this__18670__auto__,k19213,else__18671__auto__){
var self__ = this;
var this__18670__auto____$1 = this;
var G__19215 = k19213;
switch (G__19215) {
default:
return cljs.core.get.call(null,self__.__extmap,k19213,else__18671__auto__);

}
});

quantum.core.ns.Int.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this__18682__auto__,writer__18683__auto__,opts__18684__auto__){
var self__ = this;
var this__18682__auto____$1 = this;
var pr_pair__18685__auto__ = ((function (this__18682__auto____$1){
return (function (keyval__18686__auto__){
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,cljs.core.pr_writer,""," ","",opts__18684__auto__,keyval__18686__auto__);
});})(this__18682__auto____$1))
;
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,pr_pair__18685__auto__,"#quantum.core.ns.Int{",", ","}",opts__18684__auto__,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.Int.prototype.cljs$core$IMeta$_meta$arity$1 = (function (this__18666__auto__){
var self__ = this;
var this__18666__auto____$1 = this;
return self__.__meta;
});

quantum.core.ns.Int.prototype.cljs$core$ICloneable$_clone$arity$1 = (function (this__18662__auto__){
var self__ = this;
var this__18662__auto____$1 = this;
return (new quantum.core.ns.Int(self__.__meta,self__.__extmap,self__.__hash));
});

quantum.core.ns.Int.prototype.cljs$core$ICounted$_count$arity$1 = (function (this__18672__auto__){
var self__ = this;
var this__18672__auto____$1 = this;
return (0 + cljs.core.count.call(null,self__.__extmap));
});

quantum.core.ns.Int.prototype.cljs$core$IHash$_hash$arity$1 = (function (this__18663__auto__){
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

quantum.core.ns.Int.prototype.cljs$core$IEquiv$_equiv$arity$2 = (function (this__18664__auto__,other__18665__auto__){
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

quantum.core.ns.Int.prototype.cljs$core$IMap$_dissoc$arity$2 = (function (this__18677__auto__,k__18678__auto__){
var self__ = this;
var this__18677__auto____$1 = this;
if(cljs.core.contains_QMARK_.call(null,cljs.core.PersistentHashSet.EMPTY,k__18678__auto__)){
return cljs.core.dissoc.call(null,cljs.core.with_meta.call(null,cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,this__18677__auto____$1),self__.__meta),k__18678__auto__);
} else {
return (new quantum.core.ns.Int(self__.__meta,cljs.core.not_empty.call(null,cljs.core.dissoc.call(null,self__.__extmap,k__18678__auto__)),null));
}
});

quantum.core.ns.Int.prototype.cljs$core$IAssociative$_assoc$arity$3 = (function (this__18675__auto__,k__18676__auto__,G__19212){
var self__ = this;
var this__18675__auto____$1 = this;
var pred__19216 = cljs.core.keyword_identical_QMARK_;
var expr__19217 = k__18676__auto__;
return (new quantum.core.ns.Int(self__.__meta,cljs.core.assoc.call(null,self__.__extmap,k__18676__auto__,G__19212),null));
});

quantum.core.ns.Int.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (this__18680__auto__){
var self__ = this;
var this__18680__auto____$1 = this;
return cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.Int.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (this__18667__auto__,G__19212){
var self__ = this;
var this__18667__auto____$1 = this;
return (new quantum.core.ns.Int(G__19212,self__.__extmap,self__.__hash));
});

quantum.core.ns.Int.prototype.cljs$core$ICollection$_conj$arity$2 = (function (this__18673__auto__,entry__18674__auto__){
var self__ = this;
var this__18673__auto____$1 = this;
if(cljs.core.vector_QMARK_.call(null,entry__18674__auto__)){
return cljs.core._assoc.call(null,this__18673__auto____$1,cljs.core._nth.call(null,entry__18674__auto__,(0)),cljs.core._nth.call(null,entry__18674__auto__,(1)));
} else {
return cljs.core.reduce.call(null,cljs.core._conj,this__18673__auto____$1,entry__18674__auto__);
}
});

quantum.core.ns.Int.getBasis = (function (){
return cljs.core.PersistentVector.EMPTY;
});

quantum.core.ns.Int.cljs$lang$type = true;

quantum.core.ns.Int.cljs$lang$ctorPrSeq = (function (this__18702__auto__){
return cljs.core._conj.call(null,cljs.core.List.EMPTY,"quantum.core.ns/Int");
});

quantum.core.ns.Int.cljs$lang$ctorPrWriter = (function (this__18702__auto__,writer__18703__auto__){
return cljs.core._write.call(null,writer__18703__auto__,"quantum.core.ns/Int");
});

quantum.core.ns.__GT_Int = (function quantum$core$ns$__GT_Int(){
return (new quantum.core.ns.Int(null,null,null));
});

quantum.core.ns.map__GT_Int = (function quantum$core$ns$map__GT_Int(G__19214){
return (new quantum.core.ns.Int(null,cljs.core.dissoc.call(null,G__19214),null));
});


/**
* @constructor
* @param {*} __meta
* @param {*} __extmap
* @param {*} __hash
* @param {*=} __meta 
* @param {*=} __extmap
* @param {number|null} __hash
*/
quantum.core.ns.Decimal = (function (__meta,__extmap,__hash){
this.__meta = __meta;
this.__extmap = __extmap;
this.__hash = __hash;
this.cljs$lang$protocol_mask$partition0$ = 2229667594;
this.cljs$lang$protocol_mask$partition1$ = 8192;
})
quantum.core.ns.Decimal.prototype.cljs$core$ILookup$_lookup$arity$2 = (function (this__18668__auto__,k__18669__auto__){
var self__ = this;
var this__18668__auto____$1 = this;
return cljs.core._lookup.call(null,this__18668__auto____$1,k__18669__auto__,null);
});

quantum.core.ns.Decimal.prototype.cljs$core$ILookup$_lookup$arity$3 = (function (this__18670__auto__,k19221,else__18671__auto__){
var self__ = this;
var this__18670__auto____$1 = this;
var G__19223 = k19221;
switch (G__19223) {
default:
return cljs.core.get.call(null,self__.__extmap,k19221,else__18671__auto__);

}
});

quantum.core.ns.Decimal.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this__18682__auto__,writer__18683__auto__,opts__18684__auto__){
var self__ = this;
var this__18682__auto____$1 = this;
var pr_pair__18685__auto__ = ((function (this__18682__auto____$1){
return (function (keyval__18686__auto__){
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,cljs.core.pr_writer,""," ","",opts__18684__auto__,keyval__18686__auto__);
});})(this__18682__auto____$1))
;
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,pr_pair__18685__auto__,"#quantum.core.ns.Decimal{",", ","}",opts__18684__auto__,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.Decimal.prototype.cljs$core$IMeta$_meta$arity$1 = (function (this__18666__auto__){
var self__ = this;
var this__18666__auto____$1 = this;
return self__.__meta;
});

quantum.core.ns.Decimal.prototype.cljs$core$ICloneable$_clone$arity$1 = (function (this__18662__auto__){
var self__ = this;
var this__18662__auto____$1 = this;
return (new quantum.core.ns.Decimal(self__.__meta,self__.__extmap,self__.__hash));
});

quantum.core.ns.Decimal.prototype.cljs$core$ICounted$_count$arity$1 = (function (this__18672__auto__){
var self__ = this;
var this__18672__auto____$1 = this;
return (0 + cljs.core.count.call(null,self__.__extmap));
});

quantum.core.ns.Decimal.prototype.cljs$core$IHash$_hash$arity$1 = (function (this__18663__auto__){
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

quantum.core.ns.Decimal.prototype.cljs$core$IEquiv$_equiv$arity$2 = (function (this__18664__auto__,other__18665__auto__){
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

quantum.core.ns.Decimal.prototype.cljs$core$IMap$_dissoc$arity$2 = (function (this__18677__auto__,k__18678__auto__){
var self__ = this;
var this__18677__auto____$1 = this;
if(cljs.core.contains_QMARK_.call(null,cljs.core.PersistentHashSet.EMPTY,k__18678__auto__)){
return cljs.core.dissoc.call(null,cljs.core.with_meta.call(null,cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,this__18677__auto____$1),self__.__meta),k__18678__auto__);
} else {
return (new quantum.core.ns.Decimal(self__.__meta,cljs.core.not_empty.call(null,cljs.core.dissoc.call(null,self__.__extmap,k__18678__auto__)),null));
}
});

quantum.core.ns.Decimal.prototype.cljs$core$IAssociative$_assoc$arity$3 = (function (this__18675__auto__,k__18676__auto__,G__19220){
var self__ = this;
var this__18675__auto____$1 = this;
var pred__19224 = cljs.core.keyword_identical_QMARK_;
var expr__19225 = k__18676__auto__;
return (new quantum.core.ns.Decimal(self__.__meta,cljs.core.assoc.call(null,self__.__extmap,k__18676__auto__,G__19220),null));
});

quantum.core.ns.Decimal.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (this__18680__auto__){
var self__ = this;
var this__18680__auto____$1 = this;
return cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.Decimal.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (this__18667__auto__,G__19220){
var self__ = this;
var this__18667__auto____$1 = this;
return (new quantum.core.ns.Decimal(G__19220,self__.__extmap,self__.__hash));
});

quantum.core.ns.Decimal.prototype.cljs$core$ICollection$_conj$arity$2 = (function (this__18673__auto__,entry__18674__auto__){
var self__ = this;
var this__18673__auto____$1 = this;
if(cljs.core.vector_QMARK_.call(null,entry__18674__auto__)){
return cljs.core._assoc.call(null,this__18673__auto____$1,cljs.core._nth.call(null,entry__18674__auto__,(0)),cljs.core._nth.call(null,entry__18674__auto__,(1)));
} else {
return cljs.core.reduce.call(null,cljs.core._conj,this__18673__auto____$1,entry__18674__auto__);
}
});

quantum.core.ns.Decimal.getBasis = (function (){
return cljs.core.PersistentVector.EMPTY;
});

quantum.core.ns.Decimal.cljs$lang$type = true;

quantum.core.ns.Decimal.cljs$lang$ctorPrSeq = (function (this__18702__auto__){
return cljs.core._conj.call(null,cljs.core.List.EMPTY,"quantum.core.ns/Decimal");
});

quantum.core.ns.Decimal.cljs$lang$ctorPrWriter = (function (this__18702__auto__,writer__18703__auto__){
return cljs.core._write.call(null,writer__18703__auto__,"quantum.core.ns/Decimal");
});

quantum.core.ns.__GT_Decimal = (function quantum$core$ns$__GT_Decimal(){
return (new quantum.core.ns.Decimal(null,null,null));
});

quantum.core.ns.map__GT_Decimal = (function quantum$core$ns$map__GT_Decimal(G__19222){
return (new quantum.core.ns.Decimal(null,cljs.core.dissoc.call(null,G__19222),null));
});


/**
* @constructor
* @param {*} __meta
* @param {*} __extmap
* @param {*} __hash
* @param {*=} __meta 
* @param {*=} __extmap
* @param {number|null} __hash
*/
quantum.core.ns.Set = (function (__meta,__extmap,__hash){
this.__meta = __meta;
this.__extmap = __extmap;
this.__hash = __hash;
this.cljs$lang$protocol_mask$partition0$ = 2229667594;
this.cljs$lang$protocol_mask$partition1$ = 8192;
})
quantum.core.ns.Set.prototype.cljs$core$ILookup$_lookup$arity$2 = (function (this__18668__auto__,k__18669__auto__){
var self__ = this;
var this__18668__auto____$1 = this;
return cljs.core._lookup.call(null,this__18668__auto____$1,k__18669__auto__,null);
});

quantum.core.ns.Set.prototype.cljs$core$ILookup$_lookup$arity$3 = (function (this__18670__auto__,k19229,else__18671__auto__){
var self__ = this;
var this__18670__auto____$1 = this;
var G__19231 = k19229;
switch (G__19231) {
default:
return cljs.core.get.call(null,self__.__extmap,k19229,else__18671__auto__);

}
});

quantum.core.ns.Set.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this__18682__auto__,writer__18683__auto__,opts__18684__auto__){
var self__ = this;
var this__18682__auto____$1 = this;
var pr_pair__18685__auto__ = ((function (this__18682__auto____$1){
return (function (keyval__18686__auto__){
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,cljs.core.pr_writer,""," ","",opts__18684__auto__,keyval__18686__auto__);
});})(this__18682__auto____$1))
;
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,pr_pair__18685__auto__,"#quantum.core.ns.Set{",", ","}",opts__18684__auto__,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.Set.prototype.cljs$core$IMeta$_meta$arity$1 = (function (this__18666__auto__){
var self__ = this;
var this__18666__auto____$1 = this;
return self__.__meta;
});

quantum.core.ns.Set.prototype.cljs$core$ICloneable$_clone$arity$1 = (function (this__18662__auto__){
var self__ = this;
var this__18662__auto____$1 = this;
return (new quantum.core.ns.Set(self__.__meta,self__.__extmap,self__.__hash));
});

quantum.core.ns.Set.prototype.cljs$core$ICounted$_count$arity$1 = (function (this__18672__auto__){
var self__ = this;
var this__18672__auto____$1 = this;
return (0 + cljs.core.count.call(null,self__.__extmap));
});

quantum.core.ns.Set.prototype.cljs$core$IHash$_hash$arity$1 = (function (this__18663__auto__){
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

quantum.core.ns.Set.prototype.cljs$core$IEquiv$_equiv$arity$2 = (function (this__18664__auto__,other__18665__auto__){
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

quantum.core.ns.Set.prototype.cljs$core$IMap$_dissoc$arity$2 = (function (this__18677__auto__,k__18678__auto__){
var self__ = this;
var this__18677__auto____$1 = this;
if(cljs.core.contains_QMARK_.call(null,cljs.core.PersistentHashSet.EMPTY,k__18678__auto__)){
return cljs.core.dissoc.call(null,cljs.core.with_meta.call(null,cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,this__18677__auto____$1),self__.__meta),k__18678__auto__);
} else {
return (new quantum.core.ns.Set(self__.__meta,cljs.core.not_empty.call(null,cljs.core.dissoc.call(null,self__.__extmap,k__18678__auto__)),null));
}
});

quantum.core.ns.Set.prototype.cljs$core$IAssociative$_assoc$arity$3 = (function (this__18675__auto__,k__18676__auto__,G__19228){
var self__ = this;
var this__18675__auto____$1 = this;
var pred__19232 = cljs.core.keyword_identical_QMARK_;
var expr__19233 = k__18676__auto__;
return (new quantum.core.ns.Set(self__.__meta,cljs.core.assoc.call(null,self__.__extmap,k__18676__auto__,G__19228),null));
});

quantum.core.ns.Set.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (this__18680__auto__){
var self__ = this;
var this__18680__auto____$1 = this;
return cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.Set.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (this__18667__auto__,G__19228){
var self__ = this;
var this__18667__auto____$1 = this;
return (new quantum.core.ns.Set(G__19228,self__.__extmap,self__.__hash));
});

quantum.core.ns.Set.prototype.cljs$core$ICollection$_conj$arity$2 = (function (this__18673__auto__,entry__18674__auto__){
var self__ = this;
var this__18673__auto____$1 = this;
if(cljs.core.vector_QMARK_.call(null,entry__18674__auto__)){
return cljs.core._assoc.call(null,this__18673__auto____$1,cljs.core._nth.call(null,entry__18674__auto__,(0)),cljs.core._nth.call(null,entry__18674__auto__,(1)));
} else {
return cljs.core.reduce.call(null,cljs.core._conj,this__18673__auto____$1,entry__18674__auto__);
}
});

quantum.core.ns.Set.getBasis = (function (){
return cljs.core.PersistentVector.EMPTY;
});

quantum.core.ns.Set.cljs$lang$type = true;

quantum.core.ns.Set.cljs$lang$ctorPrSeq = (function (this__18702__auto__){
return cljs.core._conj.call(null,cljs.core.List.EMPTY,"quantum.core.ns/Set");
});

quantum.core.ns.Set.cljs$lang$ctorPrWriter = (function (this__18702__auto__,writer__18703__auto__){
return cljs.core._write.call(null,writer__18703__auto__,"quantum.core.ns/Set");
});

quantum.core.ns.__GT_Set = (function quantum$core$ns$__GT_Set(){
return (new quantum.core.ns.Set(null,null,null));
});

quantum.core.ns.map__GT_Set = (function quantum$core$ns$map__GT_Set(G__19230){
return (new quantum.core.ns.Set(null,cljs.core.dissoc.call(null,G__19230),null));
});


/**
* @constructor
* @param {*} __meta
* @param {*} __extmap
* @param {*} __hash
* @param {*=} __meta 
* @param {*=} __extmap
* @param {number|null} __hash
*/
quantum.core.ns.Bool = (function (__meta,__extmap,__hash){
this.__meta = __meta;
this.__extmap = __extmap;
this.__hash = __hash;
this.cljs$lang$protocol_mask$partition0$ = 2229667594;
this.cljs$lang$protocol_mask$partition1$ = 8192;
})
quantum.core.ns.Bool.prototype.cljs$core$ILookup$_lookup$arity$2 = (function (this__18668__auto__,k__18669__auto__){
var self__ = this;
var this__18668__auto____$1 = this;
return cljs.core._lookup.call(null,this__18668__auto____$1,k__18669__auto__,null);
});

quantum.core.ns.Bool.prototype.cljs$core$ILookup$_lookup$arity$3 = (function (this__18670__auto__,k19237,else__18671__auto__){
var self__ = this;
var this__18670__auto____$1 = this;
var G__19239 = k19237;
switch (G__19239) {
default:
return cljs.core.get.call(null,self__.__extmap,k19237,else__18671__auto__);

}
});

quantum.core.ns.Bool.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this__18682__auto__,writer__18683__auto__,opts__18684__auto__){
var self__ = this;
var this__18682__auto____$1 = this;
var pr_pair__18685__auto__ = ((function (this__18682__auto____$1){
return (function (keyval__18686__auto__){
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,cljs.core.pr_writer,""," ","",opts__18684__auto__,keyval__18686__auto__);
});})(this__18682__auto____$1))
;
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,pr_pair__18685__auto__,"#quantum.core.ns.Bool{",", ","}",opts__18684__auto__,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.Bool.prototype.cljs$core$IMeta$_meta$arity$1 = (function (this__18666__auto__){
var self__ = this;
var this__18666__auto____$1 = this;
return self__.__meta;
});

quantum.core.ns.Bool.prototype.cljs$core$ICloneable$_clone$arity$1 = (function (this__18662__auto__){
var self__ = this;
var this__18662__auto____$1 = this;
return (new quantum.core.ns.Bool(self__.__meta,self__.__extmap,self__.__hash));
});

quantum.core.ns.Bool.prototype.cljs$core$ICounted$_count$arity$1 = (function (this__18672__auto__){
var self__ = this;
var this__18672__auto____$1 = this;
return (0 + cljs.core.count.call(null,self__.__extmap));
});

quantum.core.ns.Bool.prototype.cljs$core$IHash$_hash$arity$1 = (function (this__18663__auto__){
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

quantum.core.ns.Bool.prototype.cljs$core$IEquiv$_equiv$arity$2 = (function (this__18664__auto__,other__18665__auto__){
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

quantum.core.ns.Bool.prototype.cljs$core$IMap$_dissoc$arity$2 = (function (this__18677__auto__,k__18678__auto__){
var self__ = this;
var this__18677__auto____$1 = this;
if(cljs.core.contains_QMARK_.call(null,cljs.core.PersistentHashSet.EMPTY,k__18678__auto__)){
return cljs.core.dissoc.call(null,cljs.core.with_meta.call(null,cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,this__18677__auto____$1),self__.__meta),k__18678__auto__);
} else {
return (new quantum.core.ns.Bool(self__.__meta,cljs.core.not_empty.call(null,cljs.core.dissoc.call(null,self__.__extmap,k__18678__auto__)),null));
}
});

quantum.core.ns.Bool.prototype.cljs$core$IAssociative$_assoc$arity$3 = (function (this__18675__auto__,k__18676__auto__,G__19236){
var self__ = this;
var this__18675__auto____$1 = this;
var pred__19240 = cljs.core.keyword_identical_QMARK_;
var expr__19241 = k__18676__auto__;
return (new quantum.core.ns.Bool(self__.__meta,cljs.core.assoc.call(null,self__.__extmap,k__18676__auto__,G__19236),null));
});

quantum.core.ns.Bool.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (this__18680__auto__){
var self__ = this;
var this__18680__auto____$1 = this;
return cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.Bool.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (this__18667__auto__,G__19236){
var self__ = this;
var this__18667__auto____$1 = this;
return (new quantum.core.ns.Bool(G__19236,self__.__extmap,self__.__hash));
});

quantum.core.ns.Bool.prototype.cljs$core$ICollection$_conj$arity$2 = (function (this__18673__auto__,entry__18674__auto__){
var self__ = this;
var this__18673__auto____$1 = this;
if(cljs.core.vector_QMARK_.call(null,entry__18674__auto__)){
return cljs.core._assoc.call(null,this__18673__auto____$1,cljs.core._nth.call(null,entry__18674__auto__,(0)),cljs.core._nth.call(null,entry__18674__auto__,(1)));
} else {
return cljs.core.reduce.call(null,cljs.core._conj,this__18673__auto____$1,entry__18674__auto__);
}
});

quantum.core.ns.Bool.getBasis = (function (){
return cljs.core.PersistentVector.EMPTY;
});

quantum.core.ns.Bool.cljs$lang$type = true;

quantum.core.ns.Bool.cljs$lang$ctorPrSeq = (function (this__18702__auto__){
return cljs.core._conj.call(null,cljs.core.List.EMPTY,"quantum.core.ns/Bool");
});

quantum.core.ns.Bool.cljs$lang$ctorPrWriter = (function (this__18702__auto__,writer__18703__auto__){
return cljs.core._write.call(null,writer__18703__auto__,"quantum.core.ns/Bool");
});

quantum.core.ns.__GT_Bool = (function quantum$core$ns$__GT_Bool(){
return (new quantum.core.ns.Bool(null,null,null));
});

quantum.core.ns.map__GT_Bool = (function quantum$core$ns$map__GT_Bool(G__19238){
return (new quantum.core.ns.Bool(null,cljs.core.dissoc.call(null,G__19238),null));
});


/**
* @constructor
* @param {*} __meta
* @param {*} __extmap
* @param {*} __hash
* @param {*=} __meta 
* @param {*=} __extmap
* @param {number|null} __hash
*/
quantum.core.ns.ArrList = (function (__meta,__extmap,__hash){
this.__meta = __meta;
this.__extmap = __extmap;
this.__hash = __hash;
this.cljs$lang$protocol_mask$partition0$ = 2229667594;
this.cljs$lang$protocol_mask$partition1$ = 8192;
})
quantum.core.ns.ArrList.prototype.cljs$core$ILookup$_lookup$arity$2 = (function (this__18668__auto__,k__18669__auto__){
var self__ = this;
var this__18668__auto____$1 = this;
return cljs.core._lookup.call(null,this__18668__auto____$1,k__18669__auto__,null);
});

quantum.core.ns.ArrList.prototype.cljs$core$ILookup$_lookup$arity$3 = (function (this__18670__auto__,k19245,else__18671__auto__){
var self__ = this;
var this__18670__auto____$1 = this;
var G__19247 = k19245;
switch (G__19247) {
default:
return cljs.core.get.call(null,self__.__extmap,k19245,else__18671__auto__);

}
});

quantum.core.ns.ArrList.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this__18682__auto__,writer__18683__auto__,opts__18684__auto__){
var self__ = this;
var this__18682__auto____$1 = this;
var pr_pair__18685__auto__ = ((function (this__18682__auto____$1){
return (function (keyval__18686__auto__){
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,cljs.core.pr_writer,""," ","",opts__18684__auto__,keyval__18686__auto__);
});})(this__18682__auto____$1))
;
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,pr_pair__18685__auto__,"#quantum.core.ns.ArrList{",", ","}",opts__18684__auto__,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.ArrList.prototype.cljs$core$IMeta$_meta$arity$1 = (function (this__18666__auto__){
var self__ = this;
var this__18666__auto____$1 = this;
return self__.__meta;
});

quantum.core.ns.ArrList.prototype.cljs$core$ICloneable$_clone$arity$1 = (function (this__18662__auto__){
var self__ = this;
var this__18662__auto____$1 = this;
return (new quantum.core.ns.ArrList(self__.__meta,self__.__extmap,self__.__hash));
});

quantum.core.ns.ArrList.prototype.cljs$core$ICounted$_count$arity$1 = (function (this__18672__auto__){
var self__ = this;
var this__18672__auto____$1 = this;
return (0 + cljs.core.count.call(null,self__.__extmap));
});

quantum.core.ns.ArrList.prototype.cljs$core$IHash$_hash$arity$1 = (function (this__18663__auto__){
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

quantum.core.ns.ArrList.prototype.cljs$core$IEquiv$_equiv$arity$2 = (function (this__18664__auto__,other__18665__auto__){
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

quantum.core.ns.ArrList.prototype.cljs$core$IMap$_dissoc$arity$2 = (function (this__18677__auto__,k__18678__auto__){
var self__ = this;
var this__18677__auto____$1 = this;
if(cljs.core.contains_QMARK_.call(null,cljs.core.PersistentHashSet.EMPTY,k__18678__auto__)){
return cljs.core.dissoc.call(null,cljs.core.with_meta.call(null,cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,this__18677__auto____$1),self__.__meta),k__18678__auto__);
} else {
return (new quantum.core.ns.ArrList(self__.__meta,cljs.core.not_empty.call(null,cljs.core.dissoc.call(null,self__.__extmap,k__18678__auto__)),null));
}
});

quantum.core.ns.ArrList.prototype.cljs$core$IAssociative$_assoc$arity$3 = (function (this__18675__auto__,k__18676__auto__,G__19244){
var self__ = this;
var this__18675__auto____$1 = this;
var pred__19248 = cljs.core.keyword_identical_QMARK_;
var expr__19249 = k__18676__auto__;
return (new quantum.core.ns.ArrList(self__.__meta,cljs.core.assoc.call(null,self__.__extmap,k__18676__auto__,G__19244),null));
});

quantum.core.ns.ArrList.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (this__18680__auto__){
var self__ = this;
var this__18680__auto____$1 = this;
return cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.ArrList.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (this__18667__auto__,G__19244){
var self__ = this;
var this__18667__auto____$1 = this;
return (new quantum.core.ns.ArrList(G__19244,self__.__extmap,self__.__hash));
});

quantum.core.ns.ArrList.prototype.cljs$core$ICollection$_conj$arity$2 = (function (this__18673__auto__,entry__18674__auto__){
var self__ = this;
var this__18673__auto____$1 = this;
if(cljs.core.vector_QMARK_.call(null,entry__18674__auto__)){
return cljs.core._assoc.call(null,this__18673__auto____$1,cljs.core._nth.call(null,entry__18674__auto__,(0)),cljs.core._nth.call(null,entry__18674__auto__,(1)));
} else {
return cljs.core.reduce.call(null,cljs.core._conj,this__18673__auto____$1,entry__18674__auto__);
}
});

quantum.core.ns.ArrList.getBasis = (function (){
return cljs.core.PersistentVector.EMPTY;
});

quantum.core.ns.ArrList.cljs$lang$type = true;

quantum.core.ns.ArrList.cljs$lang$ctorPrSeq = (function (this__18702__auto__){
return cljs.core._conj.call(null,cljs.core.List.EMPTY,"quantum.core.ns/ArrList");
});

quantum.core.ns.ArrList.cljs$lang$ctorPrWriter = (function (this__18702__auto__,writer__18703__auto__){
return cljs.core._write.call(null,writer__18703__auto__,"quantum.core.ns/ArrList");
});

quantum.core.ns.__GT_ArrList = (function quantum$core$ns$__GT_ArrList(){
return (new quantum.core.ns.ArrList(null,null,null));
});

quantum.core.ns.map__GT_ArrList = (function quantum$core$ns$map__GT_ArrList(G__19246){
return (new quantum.core.ns.ArrList(null,cljs.core.dissoc.call(null,G__19246),null));
});


/**
* @constructor
* @param {*} __meta
* @param {*} __extmap
* @param {*} __hash
* @param {*=} __meta 
* @param {*=} __extmap
* @param {number|null} __hash
*/
quantum.core.ns.TreeMap = (function (__meta,__extmap,__hash){
this.__meta = __meta;
this.__extmap = __extmap;
this.__hash = __hash;
this.cljs$lang$protocol_mask$partition0$ = 2229667594;
this.cljs$lang$protocol_mask$partition1$ = 8192;
})
quantum.core.ns.TreeMap.prototype.cljs$core$ILookup$_lookup$arity$2 = (function (this__18668__auto__,k__18669__auto__){
var self__ = this;
var this__18668__auto____$1 = this;
return cljs.core._lookup.call(null,this__18668__auto____$1,k__18669__auto__,null);
});

quantum.core.ns.TreeMap.prototype.cljs$core$ILookup$_lookup$arity$3 = (function (this__18670__auto__,k19253,else__18671__auto__){
var self__ = this;
var this__18670__auto____$1 = this;
var G__19255 = k19253;
switch (G__19255) {
default:
return cljs.core.get.call(null,self__.__extmap,k19253,else__18671__auto__);

}
});

quantum.core.ns.TreeMap.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this__18682__auto__,writer__18683__auto__,opts__18684__auto__){
var self__ = this;
var this__18682__auto____$1 = this;
var pr_pair__18685__auto__ = ((function (this__18682__auto____$1){
return (function (keyval__18686__auto__){
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,cljs.core.pr_writer,""," ","",opts__18684__auto__,keyval__18686__auto__);
});})(this__18682__auto____$1))
;
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,pr_pair__18685__auto__,"#quantum.core.ns.TreeMap{",", ","}",opts__18684__auto__,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.TreeMap.prototype.cljs$core$IMeta$_meta$arity$1 = (function (this__18666__auto__){
var self__ = this;
var this__18666__auto____$1 = this;
return self__.__meta;
});

quantum.core.ns.TreeMap.prototype.cljs$core$ICloneable$_clone$arity$1 = (function (this__18662__auto__){
var self__ = this;
var this__18662__auto____$1 = this;
return (new quantum.core.ns.TreeMap(self__.__meta,self__.__extmap,self__.__hash));
});

quantum.core.ns.TreeMap.prototype.cljs$core$ICounted$_count$arity$1 = (function (this__18672__auto__){
var self__ = this;
var this__18672__auto____$1 = this;
return (0 + cljs.core.count.call(null,self__.__extmap));
});

quantum.core.ns.TreeMap.prototype.cljs$core$IHash$_hash$arity$1 = (function (this__18663__auto__){
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

quantum.core.ns.TreeMap.prototype.cljs$core$IEquiv$_equiv$arity$2 = (function (this__18664__auto__,other__18665__auto__){
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

quantum.core.ns.TreeMap.prototype.cljs$core$IMap$_dissoc$arity$2 = (function (this__18677__auto__,k__18678__auto__){
var self__ = this;
var this__18677__auto____$1 = this;
if(cljs.core.contains_QMARK_.call(null,cljs.core.PersistentHashSet.EMPTY,k__18678__auto__)){
return cljs.core.dissoc.call(null,cljs.core.with_meta.call(null,cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,this__18677__auto____$1),self__.__meta),k__18678__auto__);
} else {
return (new quantum.core.ns.TreeMap(self__.__meta,cljs.core.not_empty.call(null,cljs.core.dissoc.call(null,self__.__extmap,k__18678__auto__)),null));
}
});

quantum.core.ns.TreeMap.prototype.cljs$core$IAssociative$_assoc$arity$3 = (function (this__18675__auto__,k__18676__auto__,G__19252){
var self__ = this;
var this__18675__auto____$1 = this;
var pred__19256 = cljs.core.keyword_identical_QMARK_;
var expr__19257 = k__18676__auto__;
return (new quantum.core.ns.TreeMap(self__.__meta,cljs.core.assoc.call(null,self__.__extmap,k__18676__auto__,G__19252),null));
});

quantum.core.ns.TreeMap.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (this__18680__auto__){
var self__ = this;
var this__18680__auto____$1 = this;
return cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.TreeMap.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (this__18667__auto__,G__19252){
var self__ = this;
var this__18667__auto____$1 = this;
return (new quantum.core.ns.TreeMap(G__19252,self__.__extmap,self__.__hash));
});

quantum.core.ns.TreeMap.prototype.cljs$core$ICollection$_conj$arity$2 = (function (this__18673__auto__,entry__18674__auto__){
var self__ = this;
var this__18673__auto____$1 = this;
if(cljs.core.vector_QMARK_.call(null,entry__18674__auto__)){
return cljs.core._assoc.call(null,this__18673__auto____$1,cljs.core._nth.call(null,entry__18674__auto__,(0)),cljs.core._nth.call(null,entry__18674__auto__,(1)));
} else {
return cljs.core.reduce.call(null,cljs.core._conj,this__18673__auto____$1,entry__18674__auto__);
}
});

quantum.core.ns.TreeMap.getBasis = (function (){
return cljs.core.PersistentVector.EMPTY;
});

quantum.core.ns.TreeMap.cljs$lang$type = true;

quantum.core.ns.TreeMap.cljs$lang$ctorPrSeq = (function (this__18702__auto__){
return cljs.core._conj.call(null,cljs.core.List.EMPTY,"quantum.core.ns/TreeMap");
});

quantum.core.ns.TreeMap.cljs$lang$ctorPrWriter = (function (this__18702__auto__,writer__18703__auto__){
return cljs.core._write.call(null,writer__18703__auto__,"quantum.core.ns/TreeMap");
});

quantum.core.ns.__GT_TreeMap = (function quantum$core$ns$__GT_TreeMap(){
return (new quantum.core.ns.TreeMap(null,null,null));
});

quantum.core.ns.map__GT_TreeMap = (function quantum$core$ns$map__GT_TreeMap(G__19254){
return (new quantum.core.ns.TreeMap(null,cljs.core.dissoc.call(null,G__19254),null));
});


/**
* @constructor
* @param {*} __meta
* @param {*} __extmap
* @param {*} __hash
* @param {*=} __meta 
* @param {*=} __extmap
* @param {number|null} __hash
*/
quantum.core.ns.LSeq = (function (__meta,__extmap,__hash){
this.__meta = __meta;
this.__extmap = __extmap;
this.__hash = __hash;
this.cljs$lang$protocol_mask$partition0$ = 2229667594;
this.cljs$lang$protocol_mask$partition1$ = 8192;
})
quantum.core.ns.LSeq.prototype.cljs$core$ILookup$_lookup$arity$2 = (function (this__18668__auto__,k__18669__auto__){
var self__ = this;
var this__18668__auto____$1 = this;
return cljs.core._lookup.call(null,this__18668__auto____$1,k__18669__auto__,null);
});

quantum.core.ns.LSeq.prototype.cljs$core$ILookup$_lookup$arity$3 = (function (this__18670__auto__,k19261,else__18671__auto__){
var self__ = this;
var this__18670__auto____$1 = this;
var G__19263 = k19261;
switch (G__19263) {
default:
return cljs.core.get.call(null,self__.__extmap,k19261,else__18671__auto__);

}
});

quantum.core.ns.LSeq.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this__18682__auto__,writer__18683__auto__,opts__18684__auto__){
var self__ = this;
var this__18682__auto____$1 = this;
var pr_pair__18685__auto__ = ((function (this__18682__auto____$1){
return (function (keyval__18686__auto__){
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,cljs.core.pr_writer,""," ","",opts__18684__auto__,keyval__18686__auto__);
});})(this__18682__auto____$1))
;
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,pr_pair__18685__auto__,"#quantum.core.ns.LSeq{",", ","}",opts__18684__auto__,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.LSeq.prototype.cljs$core$IMeta$_meta$arity$1 = (function (this__18666__auto__){
var self__ = this;
var this__18666__auto____$1 = this;
return self__.__meta;
});

quantum.core.ns.LSeq.prototype.cljs$core$ICloneable$_clone$arity$1 = (function (this__18662__auto__){
var self__ = this;
var this__18662__auto____$1 = this;
return (new quantum.core.ns.LSeq(self__.__meta,self__.__extmap,self__.__hash));
});

quantum.core.ns.LSeq.prototype.cljs$core$ICounted$_count$arity$1 = (function (this__18672__auto__){
var self__ = this;
var this__18672__auto____$1 = this;
return (0 + cljs.core.count.call(null,self__.__extmap));
});

quantum.core.ns.LSeq.prototype.cljs$core$IHash$_hash$arity$1 = (function (this__18663__auto__){
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

quantum.core.ns.LSeq.prototype.cljs$core$IEquiv$_equiv$arity$2 = (function (this__18664__auto__,other__18665__auto__){
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

quantum.core.ns.LSeq.prototype.cljs$core$IMap$_dissoc$arity$2 = (function (this__18677__auto__,k__18678__auto__){
var self__ = this;
var this__18677__auto____$1 = this;
if(cljs.core.contains_QMARK_.call(null,cljs.core.PersistentHashSet.EMPTY,k__18678__auto__)){
return cljs.core.dissoc.call(null,cljs.core.with_meta.call(null,cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,this__18677__auto____$1),self__.__meta),k__18678__auto__);
} else {
return (new quantum.core.ns.LSeq(self__.__meta,cljs.core.not_empty.call(null,cljs.core.dissoc.call(null,self__.__extmap,k__18678__auto__)),null));
}
});

quantum.core.ns.LSeq.prototype.cljs$core$IAssociative$_assoc$arity$3 = (function (this__18675__auto__,k__18676__auto__,G__19260){
var self__ = this;
var this__18675__auto____$1 = this;
var pred__19264 = cljs.core.keyword_identical_QMARK_;
var expr__19265 = k__18676__auto__;
return (new quantum.core.ns.LSeq(self__.__meta,cljs.core.assoc.call(null,self__.__extmap,k__18676__auto__,G__19260),null));
});

quantum.core.ns.LSeq.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (this__18680__auto__){
var self__ = this;
var this__18680__auto____$1 = this;
return cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.LSeq.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (this__18667__auto__,G__19260){
var self__ = this;
var this__18667__auto____$1 = this;
return (new quantum.core.ns.LSeq(G__19260,self__.__extmap,self__.__hash));
});

quantum.core.ns.LSeq.prototype.cljs$core$ICollection$_conj$arity$2 = (function (this__18673__auto__,entry__18674__auto__){
var self__ = this;
var this__18673__auto____$1 = this;
if(cljs.core.vector_QMARK_.call(null,entry__18674__auto__)){
return cljs.core._assoc.call(null,this__18673__auto____$1,cljs.core._nth.call(null,entry__18674__auto__,(0)),cljs.core._nth.call(null,entry__18674__auto__,(1)));
} else {
return cljs.core.reduce.call(null,cljs.core._conj,this__18673__auto____$1,entry__18674__auto__);
}
});

quantum.core.ns.LSeq.getBasis = (function (){
return cljs.core.PersistentVector.EMPTY;
});

quantum.core.ns.LSeq.cljs$lang$type = true;

quantum.core.ns.LSeq.cljs$lang$ctorPrSeq = (function (this__18702__auto__){
return cljs.core._conj.call(null,cljs.core.List.EMPTY,"quantum.core.ns/LSeq");
});

quantum.core.ns.LSeq.cljs$lang$ctorPrWriter = (function (this__18702__auto__,writer__18703__auto__){
return cljs.core._write.call(null,writer__18703__auto__,"quantum.core.ns/LSeq");
});

quantum.core.ns.__GT_LSeq = (function quantum$core$ns$__GT_LSeq(){
return (new quantum.core.ns.LSeq(null,null,null));
});

quantum.core.ns.map__GT_LSeq = (function quantum$core$ns$map__GT_LSeq(G__19262){
return (new quantum.core.ns.LSeq(null,cljs.core.dissoc.call(null,G__19262),null));
});


/**
* @constructor
* @param {*} __meta
* @param {*} __extmap
* @param {*} __hash
* @param {*=} __meta 
* @param {*=} __extmap
* @param {number|null} __hash
*/
quantum.core.ns.Vec = (function (__meta,__extmap,__hash){
this.__meta = __meta;
this.__extmap = __extmap;
this.__hash = __hash;
this.cljs$lang$protocol_mask$partition0$ = 2229667594;
this.cljs$lang$protocol_mask$partition1$ = 8192;
})
quantum.core.ns.Vec.prototype.cljs$core$ILookup$_lookup$arity$2 = (function (this__18668__auto__,k__18669__auto__){
var self__ = this;
var this__18668__auto____$1 = this;
return cljs.core._lookup.call(null,this__18668__auto____$1,k__18669__auto__,null);
});

quantum.core.ns.Vec.prototype.cljs$core$ILookup$_lookup$arity$3 = (function (this__18670__auto__,k19269,else__18671__auto__){
var self__ = this;
var this__18670__auto____$1 = this;
var G__19271 = k19269;
switch (G__19271) {
default:
return cljs.core.get.call(null,self__.__extmap,k19269,else__18671__auto__);

}
});

quantum.core.ns.Vec.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this__18682__auto__,writer__18683__auto__,opts__18684__auto__){
var self__ = this;
var this__18682__auto____$1 = this;
var pr_pair__18685__auto__ = ((function (this__18682__auto____$1){
return (function (keyval__18686__auto__){
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,cljs.core.pr_writer,""," ","",opts__18684__auto__,keyval__18686__auto__);
});})(this__18682__auto____$1))
;
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,pr_pair__18685__auto__,"#quantum.core.ns.Vec{",", ","}",opts__18684__auto__,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.Vec.prototype.cljs$core$IMeta$_meta$arity$1 = (function (this__18666__auto__){
var self__ = this;
var this__18666__auto____$1 = this;
return self__.__meta;
});

quantum.core.ns.Vec.prototype.cljs$core$ICloneable$_clone$arity$1 = (function (this__18662__auto__){
var self__ = this;
var this__18662__auto____$1 = this;
return (new quantum.core.ns.Vec(self__.__meta,self__.__extmap,self__.__hash));
});

quantum.core.ns.Vec.prototype.cljs$core$ICounted$_count$arity$1 = (function (this__18672__auto__){
var self__ = this;
var this__18672__auto____$1 = this;
return (0 + cljs.core.count.call(null,self__.__extmap));
});

quantum.core.ns.Vec.prototype.cljs$core$IHash$_hash$arity$1 = (function (this__18663__auto__){
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

quantum.core.ns.Vec.prototype.cljs$core$IEquiv$_equiv$arity$2 = (function (this__18664__auto__,other__18665__auto__){
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

quantum.core.ns.Vec.prototype.cljs$core$IMap$_dissoc$arity$2 = (function (this__18677__auto__,k__18678__auto__){
var self__ = this;
var this__18677__auto____$1 = this;
if(cljs.core.contains_QMARK_.call(null,cljs.core.PersistentHashSet.EMPTY,k__18678__auto__)){
return cljs.core.dissoc.call(null,cljs.core.with_meta.call(null,cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,this__18677__auto____$1),self__.__meta),k__18678__auto__);
} else {
return (new quantum.core.ns.Vec(self__.__meta,cljs.core.not_empty.call(null,cljs.core.dissoc.call(null,self__.__extmap,k__18678__auto__)),null));
}
});

quantum.core.ns.Vec.prototype.cljs$core$IAssociative$_assoc$arity$3 = (function (this__18675__auto__,k__18676__auto__,G__19268){
var self__ = this;
var this__18675__auto____$1 = this;
var pred__19272 = cljs.core.keyword_identical_QMARK_;
var expr__19273 = k__18676__auto__;
return (new quantum.core.ns.Vec(self__.__meta,cljs.core.assoc.call(null,self__.__extmap,k__18676__auto__,G__19268),null));
});

quantum.core.ns.Vec.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (this__18680__auto__){
var self__ = this;
var this__18680__auto____$1 = this;
return cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.Vec.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (this__18667__auto__,G__19268){
var self__ = this;
var this__18667__auto____$1 = this;
return (new quantum.core.ns.Vec(G__19268,self__.__extmap,self__.__hash));
});

quantum.core.ns.Vec.prototype.cljs$core$ICollection$_conj$arity$2 = (function (this__18673__auto__,entry__18674__auto__){
var self__ = this;
var this__18673__auto____$1 = this;
if(cljs.core.vector_QMARK_.call(null,entry__18674__auto__)){
return cljs.core._assoc.call(null,this__18673__auto____$1,cljs.core._nth.call(null,entry__18674__auto__,(0)),cljs.core._nth.call(null,entry__18674__auto__,(1)));
} else {
return cljs.core.reduce.call(null,cljs.core._conj,this__18673__auto____$1,entry__18674__auto__);
}
});

quantum.core.ns.Vec.getBasis = (function (){
return cljs.core.PersistentVector.EMPTY;
});

quantum.core.ns.Vec.cljs$lang$type = true;

quantum.core.ns.Vec.cljs$lang$ctorPrSeq = (function (this__18702__auto__){
return cljs.core._conj.call(null,cljs.core.List.EMPTY,"quantum.core.ns/Vec");
});

quantum.core.ns.Vec.cljs$lang$ctorPrWriter = (function (this__18702__auto__,writer__18703__auto__){
return cljs.core._write.call(null,writer__18703__auto__,"quantum.core.ns/Vec");
});

quantum.core.ns.__GT_Vec = (function quantum$core$ns$__GT_Vec(){
return (new quantum.core.ns.Vec(null,null,null));
});

quantum.core.ns.map__GT_Vec = (function quantum$core$ns$map__GT_Vec(G__19270){
return (new quantum.core.ns.Vec(null,cljs.core.dissoc.call(null,G__19270),null));
});


/**
* @constructor
* @param {*} __meta
* @param {*} __extmap
* @param {*} __hash
* @param {*=} __meta 
* @param {*=} __extmap
* @param {number|null} __hash
*/
quantum.core.ns.Regex = (function (__meta,__extmap,__hash){
this.__meta = __meta;
this.__extmap = __extmap;
this.__hash = __hash;
this.cljs$lang$protocol_mask$partition0$ = 2229667594;
this.cljs$lang$protocol_mask$partition1$ = 8192;
})
quantum.core.ns.Regex.prototype.cljs$core$ILookup$_lookup$arity$2 = (function (this__18668__auto__,k__18669__auto__){
var self__ = this;
var this__18668__auto____$1 = this;
return cljs.core._lookup.call(null,this__18668__auto____$1,k__18669__auto__,null);
});

quantum.core.ns.Regex.prototype.cljs$core$ILookup$_lookup$arity$3 = (function (this__18670__auto__,k19277,else__18671__auto__){
var self__ = this;
var this__18670__auto____$1 = this;
var G__19279 = k19277;
switch (G__19279) {
default:
return cljs.core.get.call(null,self__.__extmap,k19277,else__18671__auto__);

}
});

quantum.core.ns.Regex.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this__18682__auto__,writer__18683__auto__,opts__18684__auto__){
var self__ = this;
var this__18682__auto____$1 = this;
var pr_pair__18685__auto__ = ((function (this__18682__auto____$1){
return (function (keyval__18686__auto__){
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,cljs.core.pr_writer,""," ","",opts__18684__auto__,keyval__18686__auto__);
});})(this__18682__auto____$1))
;
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,pr_pair__18685__auto__,"#quantum.core.ns.Regex{",", ","}",opts__18684__auto__,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.Regex.prototype.cljs$core$IMeta$_meta$arity$1 = (function (this__18666__auto__){
var self__ = this;
var this__18666__auto____$1 = this;
return self__.__meta;
});

quantum.core.ns.Regex.prototype.cljs$core$ICloneable$_clone$arity$1 = (function (this__18662__auto__){
var self__ = this;
var this__18662__auto____$1 = this;
return (new quantum.core.ns.Regex(self__.__meta,self__.__extmap,self__.__hash));
});

quantum.core.ns.Regex.prototype.cljs$core$ICounted$_count$arity$1 = (function (this__18672__auto__){
var self__ = this;
var this__18672__auto____$1 = this;
return (0 + cljs.core.count.call(null,self__.__extmap));
});

quantum.core.ns.Regex.prototype.cljs$core$IHash$_hash$arity$1 = (function (this__18663__auto__){
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

quantum.core.ns.Regex.prototype.cljs$core$IEquiv$_equiv$arity$2 = (function (this__18664__auto__,other__18665__auto__){
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

quantum.core.ns.Regex.prototype.cljs$core$IMap$_dissoc$arity$2 = (function (this__18677__auto__,k__18678__auto__){
var self__ = this;
var this__18677__auto____$1 = this;
if(cljs.core.contains_QMARK_.call(null,cljs.core.PersistentHashSet.EMPTY,k__18678__auto__)){
return cljs.core.dissoc.call(null,cljs.core.with_meta.call(null,cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,this__18677__auto____$1),self__.__meta),k__18678__auto__);
} else {
return (new quantum.core.ns.Regex(self__.__meta,cljs.core.not_empty.call(null,cljs.core.dissoc.call(null,self__.__extmap,k__18678__auto__)),null));
}
});

quantum.core.ns.Regex.prototype.cljs$core$IAssociative$_assoc$arity$3 = (function (this__18675__auto__,k__18676__auto__,G__19276){
var self__ = this;
var this__18675__auto____$1 = this;
var pred__19280 = cljs.core.keyword_identical_QMARK_;
var expr__19281 = k__18676__auto__;
return (new quantum.core.ns.Regex(self__.__meta,cljs.core.assoc.call(null,self__.__extmap,k__18676__auto__,G__19276),null));
});

quantum.core.ns.Regex.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (this__18680__auto__){
var self__ = this;
var this__18680__auto____$1 = this;
return cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.Regex.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (this__18667__auto__,G__19276){
var self__ = this;
var this__18667__auto____$1 = this;
return (new quantum.core.ns.Regex(G__19276,self__.__extmap,self__.__hash));
});

quantum.core.ns.Regex.prototype.cljs$core$ICollection$_conj$arity$2 = (function (this__18673__auto__,entry__18674__auto__){
var self__ = this;
var this__18673__auto____$1 = this;
if(cljs.core.vector_QMARK_.call(null,entry__18674__auto__)){
return cljs.core._assoc.call(null,this__18673__auto____$1,cljs.core._nth.call(null,entry__18674__auto__,(0)),cljs.core._nth.call(null,entry__18674__auto__,(1)));
} else {
return cljs.core.reduce.call(null,cljs.core._conj,this__18673__auto____$1,entry__18674__auto__);
}
});

quantum.core.ns.Regex.getBasis = (function (){
return cljs.core.PersistentVector.EMPTY;
});

quantum.core.ns.Regex.cljs$lang$type = true;

quantum.core.ns.Regex.cljs$lang$ctorPrSeq = (function (this__18702__auto__){
return cljs.core._conj.call(null,cljs.core.List.EMPTY,"quantum.core.ns/Regex");
});

quantum.core.ns.Regex.cljs$lang$ctorPrWriter = (function (this__18702__auto__,writer__18703__auto__){
return cljs.core._write.call(null,writer__18703__auto__,"quantum.core.ns/Regex");
});

quantum.core.ns.__GT_Regex = (function quantum$core$ns$__GT_Regex(){
return (new quantum.core.ns.Regex(null,null,null));
});

quantum.core.ns.map__GT_Regex = (function quantum$core$ns$map__GT_Regex(G__19278){
return (new quantum.core.ns.Regex(null,cljs.core.dissoc.call(null,G__19278),null));
});


/**
* @constructor
* @param {*} __meta
* @param {*} __extmap
* @param {*} __hash
* @param {*=} __meta 
* @param {*=} __extmap
* @param {number|null} __hash
*/
quantum.core.ns.Editable = (function (__meta,__extmap,__hash){
this.__meta = __meta;
this.__extmap = __extmap;
this.__hash = __hash;
this.cljs$lang$protocol_mask$partition0$ = 2229667594;
this.cljs$lang$protocol_mask$partition1$ = 8192;
})
quantum.core.ns.Editable.prototype.cljs$core$ILookup$_lookup$arity$2 = (function (this__18668__auto__,k__18669__auto__){
var self__ = this;
var this__18668__auto____$1 = this;
return cljs.core._lookup.call(null,this__18668__auto____$1,k__18669__auto__,null);
});

quantum.core.ns.Editable.prototype.cljs$core$ILookup$_lookup$arity$3 = (function (this__18670__auto__,k19285,else__18671__auto__){
var self__ = this;
var this__18670__auto____$1 = this;
var G__19287 = k19285;
switch (G__19287) {
default:
return cljs.core.get.call(null,self__.__extmap,k19285,else__18671__auto__);

}
});

quantum.core.ns.Editable.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this__18682__auto__,writer__18683__auto__,opts__18684__auto__){
var self__ = this;
var this__18682__auto____$1 = this;
var pr_pair__18685__auto__ = ((function (this__18682__auto____$1){
return (function (keyval__18686__auto__){
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,cljs.core.pr_writer,""," ","",opts__18684__auto__,keyval__18686__auto__);
});})(this__18682__auto____$1))
;
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,pr_pair__18685__auto__,"#quantum.core.ns.Editable{",", ","}",opts__18684__auto__,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.Editable.prototype.cljs$core$IMeta$_meta$arity$1 = (function (this__18666__auto__){
var self__ = this;
var this__18666__auto____$1 = this;
return self__.__meta;
});

quantum.core.ns.Editable.prototype.cljs$core$ICloneable$_clone$arity$1 = (function (this__18662__auto__){
var self__ = this;
var this__18662__auto____$1 = this;
return (new quantum.core.ns.Editable(self__.__meta,self__.__extmap,self__.__hash));
});

quantum.core.ns.Editable.prototype.cljs$core$ICounted$_count$arity$1 = (function (this__18672__auto__){
var self__ = this;
var this__18672__auto____$1 = this;
return (0 + cljs.core.count.call(null,self__.__extmap));
});

quantum.core.ns.Editable.prototype.cljs$core$IHash$_hash$arity$1 = (function (this__18663__auto__){
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

quantum.core.ns.Editable.prototype.cljs$core$IEquiv$_equiv$arity$2 = (function (this__18664__auto__,other__18665__auto__){
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

quantum.core.ns.Editable.prototype.cljs$core$IMap$_dissoc$arity$2 = (function (this__18677__auto__,k__18678__auto__){
var self__ = this;
var this__18677__auto____$1 = this;
if(cljs.core.contains_QMARK_.call(null,cljs.core.PersistentHashSet.EMPTY,k__18678__auto__)){
return cljs.core.dissoc.call(null,cljs.core.with_meta.call(null,cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,this__18677__auto____$1),self__.__meta),k__18678__auto__);
} else {
return (new quantum.core.ns.Editable(self__.__meta,cljs.core.not_empty.call(null,cljs.core.dissoc.call(null,self__.__extmap,k__18678__auto__)),null));
}
});

quantum.core.ns.Editable.prototype.cljs$core$IAssociative$_assoc$arity$3 = (function (this__18675__auto__,k__18676__auto__,G__19284){
var self__ = this;
var this__18675__auto____$1 = this;
var pred__19288 = cljs.core.keyword_identical_QMARK_;
var expr__19289 = k__18676__auto__;
return (new quantum.core.ns.Editable(self__.__meta,cljs.core.assoc.call(null,self__.__extmap,k__18676__auto__,G__19284),null));
});

quantum.core.ns.Editable.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (this__18680__auto__){
var self__ = this;
var this__18680__auto____$1 = this;
return cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.Editable.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (this__18667__auto__,G__19284){
var self__ = this;
var this__18667__auto____$1 = this;
return (new quantum.core.ns.Editable(G__19284,self__.__extmap,self__.__hash));
});

quantum.core.ns.Editable.prototype.cljs$core$ICollection$_conj$arity$2 = (function (this__18673__auto__,entry__18674__auto__){
var self__ = this;
var this__18673__auto____$1 = this;
if(cljs.core.vector_QMARK_.call(null,entry__18674__auto__)){
return cljs.core._assoc.call(null,this__18673__auto____$1,cljs.core._nth.call(null,entry__18674__auto__,(0)),cljs.core._nth.call(null,entry__18674__auto__,(1)));
} else {
return cljs.core.reduce.call(null,cljs.core._conj,this__18673__auto____$1,entry__18674__auto__);
}
});

quantum.core.ns.Editable.getBasis = (function (){
return cljs.core.PersistentVector.EMPTY;
});

quantum.core.ns.Editable.cljs$lang$type = true;

quantum.core.ns.Editable.cljs$lang$ctorPrSeq = (function (this__18702__auto__){
return cljs.core._conj.call(null,cljs.core.List.EMPTY,"quantum.core.ns/Editable");
});

quantum.core.ns.Editable.cljs$lang$ctorPrWriter = (function (this__18702__auto__,writer__18703__auto__){
return cljs.core._write.call(null,writer__18703__auto__,"quantum.core.ns/Editable");
});

quantum.core.ns.__GT_Editable = (function quantum$core$ns$__GT_Editable(){
return (new quantum.core.ns.Editable(null,null,null));
});

quantum.core.ns.map__GT_Editable = (function quantum$core$ns$map__GT_Editable(G__19286){
return (new quantum.core.ns.Editable(null,cljs.core.dissoc.call(null,G__19286),null));
});


/**
* @constructor
* @param {*} __meta
* @param {*} __extmap
* @param {*} __hash
* @param {*=} __meta 
* @param {*=} __extmap
* @param {number|null} __hash
*/
quantum.core.ns.Transient = (function (__meta,__extmap,__hash){
this.__meta = __meta;
this.__extmap = __extmap;
this.__hash = __hash;
this.cljs$lang$protocol_mask$partition0$ = 2229667594;
this.cljs$lang$protocol_mask$partition1$ = 8192;
})
quantum.core.ns.Transient.prototype.cljs$core$ILookup$_lookup$arity$2 = (function (this__18668__auto__,k__18669__auto__){
var self__ = this;
var this__18668__auto____$1 = this;
return cljs.core._lookup.call(null,this__18668__auto____$1,k__18669__auto__,null);
});

quantum.core.ns.Transient.prototype.cljs$core$ILookup$_lookup$arity$3 = (function (this__18670__auto__,k19293,else__18671__auto__){
var self__ = this;
var this__18670__auto____$1 = this;
var G__19295 = k19293;
switch (G__19295) {
default:
return cljs.core.get.call(null,self__.__extmap,k19293,else__18671__auto__);

}
});

quantum.core.ns.Transient.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this__18682__auto__,writer__18683__auto__,opts__18684__auto__){
var self__ = this;
var this__18682__auto____$1 = this;
var pr_pair__18685__auto__ = ((function (this__18682__auto____$1){
return (function (keyval__18686__auto__){
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,cljs.core.pr_writer,""," ","",opts__18684__auto__,keyval__18686__auto__);
});})(this__18682__auto____$1))
;
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,pr_pair__18685__auto__,"#quantum.core.ns.Transient{",", ","}",opts__18684__auto__,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.Transient.prototype.cljs$core$IMeta$_meta$arity$1 = (function (this__18666__auto__){
var self__ = this;
var this__18666__auto____$1 = this;
return self__.__meta;
});

quantum.core.ns.Transient.prototype.cljs$core$ICloneable$_clone$arity$1 = (function (this__18662__auto__){
var self__ = this;
var this__18662__auto____$1 = this;
return (new quantum.core.ns.Transient(self__.__meta,self__.__extmap,self__.__hash));
});

quantum.core.ns.Transient.prototype.cljs$core$ICounted$_count$arity$1 = (function (this__18672__auto__){
var self__ = this;
var this__18672__auto____$1 = this;
return (0 + cljs.core.count.call(null,self__.__extmap));
});

quantum.core.ns.Transient.prototype.cljs$core$IHash$_hash$arity$1 = (function (this__18663__auto__){
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

quantum.core.ns.Transient.prototype.cljs$core$IEquiv$_equiv$arity$2 = (function (this__18664__auto__,other__18665__auto__){
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

quantum.core.ns.Transient.prototype.cljs$core$IMap$_dissoc$arity$2 = (function (this__18677__auto__,k__18678__auto__){
var self__ = this;
var this__18677__auto____$1 = this;
if(cljs.core.contains_QMARK_.call(null,cljs.core.PersistentHashSet.EMPTY,k__18678__auto__)){
return cljs.core.dissoc.call(null,cljs.core.with_meta.call(null,cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,this__18677__auto____$1),self__.__meta),k__18678__auto__);
} else {
return (new quantum.core.ns.Transient(self__.__meta,cljs.core.not_empty.call(null,cljs.core.dissoc.call(null,self__.__extmap,k__18678__auto__)),null));
}
});

quantum.core.ns.Transient.prototype.cljs$core$IAssociative$_assoc$arity$3 = (function (this__18675__auto__,k__18676__auto__,G__19292){
var self__ = this;
var this__18675__auto____$1 = this;
var pred__19296 = cljs.core.keyword_identical_QMARK_;
var expr__19297 = k__18676__auto__;
return (new quantum.core.ns.Transient(self__.__meta,cljs.core.assoc.call(null,self__.__extmap,k__18676__auto__,G__19292),null));
});

quantum.core.ns.Transient.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (this__18680__auto__){
var self__ = this;
var this__18680__auto____$1 = this;
return cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.Transient.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (this__18667__auto__,G__19292){
var self__ = this;
var this__18667__auto____$1 = this;
return (new quantum.core.ns.Transient(G__19292,self__.__extmap,self__.__hash));
});

quantum.core.ns.Transient.prototype.cljs$core$ICollection$_conj$arity$2 = (function (this__18673__auto__,entry__18674__auto__){
var self__ = this;
var this__18673__auto____$1 = this;
if(cljs.core.vector_QMARK_.call(null,entry__18674__auto__)){
return cljs.core._assoc.call(null,this__18673__auto____$1,cljs.core._nth.call(null,entry__18674__auto__,(0)),cljs.core._nth.call(null,entry__18674__auto__,(1)));
} else {
return cljs.core.reduce.call(null,cljs.core._conj,this__18673__auto____$1,entry__18674__auto__);
}
});

quantum.core.ns.Transient.getBasis = (function (){
return cljs.core.PersistentVector.EMPTY;
});

quantum.core.ns.Transient.cljs$lang$type = true;

quantum.core.ns.Transient.cljs$lang$ctorPrSeq = (function (this__18702__auto__){
return cljs.core._conj.call(null,cljs.core.List.EMPTY,"quantum.core.ns/Transient");
});

quantum.core.ns.Transient.cljs$lang$ctorPrWriter = (function (this__18702__auto__,writer__18703__auto__){
return cljs.core._write.call(null,writer__18703__auto__,"quantum.core.ns/Transient");
});

quantum.core.ns.__GT_Transient = (function quantum$core$ns$__GT_Transient(){
return (new quantum.core.ns.Transient(null,null,null));
});

quantum.core.ns.map__GT_Transient = (function quantum$core$ns$map__GT_Transient(G__19294){
return (new quantum.core.ns.Transient(null,cljs.core.dissoc.call(null,G__19294),null));
});


/**
* @constructor
* @param {*} __meta
* @param {*} __extmap
* @param {*} __hash
* @param {*=} __meta 
* @param {*=} __extmap
* @param {number|null} __hash
*/
quantum.core.ns.Queue = (function (__meta,__extmap,__hash){
this.__meta = __meta;
this.__extmap = __extmap;
this.__hash = __hash;
this.cljs$lang$protocol_mask$partition0$ = 2229667594;
this.cljs$lang$protocol_mask$partition1$ = 8192;
})
quantum.core.ns.Queue.prototype.cljs$core$ILookup$_lookup$arity$2 = (function (this__18668__auto__,k__18669__auto__){
var self__ = this;
var this__18668__auto____$1 = this;
return cljs.core._lookup.call(null,this__18668__auto____$1,k__18669__auto__,null);
});

quantum.core.ns.Queue.prototype.cljs$core$ILookup$_lookup$arity$3 = (function (this__18670__auto__,k19301,else__18671__auto__){
var self__ = this;
var this__18670__auto____$1 = this;
var G__19303 = k19301;
switch (G__19303) {
default:
return cljs.core.get.call(null,self__.__extmap,k19301,else__18671__auto__);

}
});

quantum.core.ns.Queue.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this__18682__auto__,writer__18683__auto__,opts__18684__auto__){
var self__ = this;
var this__18682__auto____$1 = this;
var pr_pair__18685__auto__ = ((function (this__18682__auto____$1){
return (function (keyval__18686__auto__){
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,cljs.core.pr_writer,""," ","",opts__18684__auto__,keyval__18686__auto__);
});})(this__18682__auto____$1))
;
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,pr_pair__18685__auto__,"#quantum.core.ns.Queue{",", ","}",opts__18684__auto__,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.Queue.prototype.cljs$core$IMeta$_meta$arity$1 = (function (this__18666__auto__){
var self__ = this;
var this__18666__auto____$1 = this;
return self__.__meta;
});

quantum.core.ns.Queue.prototype.cljs$core$ICloneable$_clone$arity$1 = (function (this__18662__auto__){
var self__ = this;
var this__18662__auto____$1 = this;
return (new quantum.core.ns.Queue(self__.__meta,self__.__extmap,self__.__hash));
});

quantum.core.ns.Queue.prototype.cljs$core$ICounted$_count$arity$1 = (function (this__18672__auto__){
var self__ = this;
var this__18672__auto____$1 = this;
return (0 + cljs.core.count.call(null,self__.__extmap));
});

quantum.core.ns.Queue.prototype.cljs$core$IHash$_hash$arity$1 = (function (this__18663__auto__){
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

quantum.core.ns.Queue.prototype.cljs$core$IEquiv$_equiv$arity$2 = (function (this__18664__auto__,other__18665__auto__){
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

quantum.core.ns.Queue.prototype.cljs$core$IMap$_dissoc$arity$2 = (function (this__18677__auto__,k__18678__auto__){
var self__ = this;
var this__18677__auto____$1 = this;
if(cljs.core.contains_QMARK_.call(null,cljs.core.PersistentHashSet.EMPTY,k__18678__auto__)){
return cljs.core.dissoc.call(null,cljs.core.with_meta.call(null,cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,this__18677__auto____$1),self__.__meta),k__18678__auto__);
} else {
return (new quantum.core.ns.Queue(self__.__meta,cljs.core.not_empty.call(null,cljs.core.dissoc.call(null,self__.__extmap,k__18678__auto__)),null));
}
});

quantum.core.ns.Queue.prototype.cljs$core$IAssociative$_assoc$arity$3 = (function (this__18675__auto__,k__18676__auto__,G__19300){
var self__ = this;
var this__18675__auto____$1 = this;
var pred__19304 = cljs.core.keyword_identical_QMARK_;
var expr__19305 = k__18676__auto__;
return (new quantum.core.ns.Queue(self__.__meta,cljs.core.assoc.call(null,self__.__extmap,k__18676__auto__,G__19300),null));
});

quantum.core.ns.Queue.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (this__18680__auto__){
var self__ = this;
var this__18680__auto____$1 = this;
return cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.Queue.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (this__18667__auto__,G__19300){
var self__ = this;
var this__18667__auto____$1 = this;
return (new quantum.core.ns.Queue(G__19300,self__.__extmap,self__.__hash));
});

quantum.core.ns.Queue.prototype.cljs$core$ICollection$_conj$arity$2 = (function (this__18673__auto__,entry__18674__auto__){
var self__ = this;
var this__18673__auto____$1 = this;
if(cljs.core.vector_QMARK_.call(null,entry__18674__auto__)){
return cljs.core._assoc.call(null,this__18673__auto____$1,cljs.core._nth.call(null,entry__18674__auto__,(0)),cljs.core._nth.call(null,entry__18674__auto__,(1)));
} else {
return cljs.core.reduce.call(null,cljs.core._conj,this__18673__auto____$1,entry__18674__auto__);
}
});

quantum.core.ns.Queue.getBasis = (function (){
return cljs.core.PersistentVector.EMPTY;
});

quantum.core.ns.Queue.cljs$lang$type = true;

quantum.core.ns.Queue.cljs$lang$ctorPrSeq = (function (this__18702__auto__){
return cljs.core._conj.call(null,cljs.core.List.EMPTY,"quantum.core.ns/Queue");
});

quantum.core.ns.Queue.cljs$lang$ctorPrWriter = (function (this__18702__auto__,writer__18703__auto__){
return cljs.core._write.call(null,writer__18703__auto__,"quantum.core.ns/Queue");
});

quantum.core.ns.__GT_Queue = (function quantum$core$ns$__GT_Queue(){
return (new quantum.core.ns.Queue(null,null,null));
});

quantum.core.ns.map__GT_Queue = (function quantum$core$ns$map__GT_Queue(G__19302){
return (new quantum.core.ns.Queue(null,cljs.core.dissoc.call(null,G__19302),null));
});


/**
* @constructor
* @param {*} __meta
* @param {*} __extmap
* @param {*} __hash
* @param {*=} __meta 
* @param {*=} __extmap
* @param {number|null} __hash
*/
quantum.core.ns.Map = (function (__meta,__extmap,__hash){
this.__meta = __meta;
this.__extmap = __extmap;
this.__hash = __hash;
this.cljs$lang$protocol_mask$partition0$ = 2229667594;
this.cljs$lang$protocol_mask$partition1$ = 8192;
})
quantum.core.ns.Map.prototype.cljs$core$ILookup$_lookup$arity$2 = (function (this__18668__auto__,k__18669__auto__){
var self__ = this;
var this__18668__auto____$1 = this;
return cljs.core._lookup.call(null,this__18668__auto____$1,k__18669__auto__,null);
});

quantum.core.ns.Map.prototype.cljs$core$ILookup$_lookup$arity$3 = (function (this__18670__auto__,k19309,else__18671__auto__){
var self__ = this;
var this__18670__auto____$1 = this;
var G__19311 = k19309;
switch (G__19311) {
default:
return cljs.core.get.call(null,self__.__extmap,k19309,else__18671__auto__);

}
});

quantum.core.ns.Map.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this__18682__auto__,writer__18683__auto__,opts__18684__auto__){
var self__ = this;
var this__18682__auto____$1 = this;
var pr_pair__18685__auto__ = ((function (this__18682__auto____$1){
return (function (keyval__18686__auto__){
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,cljs.core.pr_writer,""," ","",opts__18684__auto__,keyval__18686__auto__);
});})(this__18682__auto____$1))
;
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,pr_pair__18685__auto__,"#quantum.core.ns.Map{",", ","}",opts__18684__auto__,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.Map.prototype.cljs$core$IMeta$_meta$arity$1 = (function (this__18666__auto__){
var self__ = this;
var this__18666__auto____$1 = this;
return self__.__meta;
});

quantum.core.ns.Map.prototype.cljs$core$ICloneable$_clone$arity$1 = (function (this__18662__auto__){
var self__ = this;
var this__18662__auto____$1 = this;
return (new quantum.core.ns.Map(self__.__meta,self__.__extmap,self__.__hash));
});

quantum.core.ns.Map.prototype.cljs$core$ICounted$_count$arity$1 = (function (this__18672__auto__){
var self__ = this;
var this__18672__auto____$1 = this;
return (0 + cljs.core.count.call(null,self__.__extmap));
});

quantum.core.ns.Map.prototype.cljs$core$IHash$_hash$arity$1 = (function (this__18663__auto__){
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

quantum.core.ns.Map.prototype.cljs$core$IEquiv$_equiv$arity$2 = (function (this__18664__auto__,other__18665__auto__){
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

quantum.core.ns.Map.prototype.cljs$core$IMap$_dissoc$arity$2 = (function (this__18677__auto__,k__18678__auto__){
var self__ = this;
var this__18677__auto____$1 = this;
if(cljs.core.contains_QMARK_.call(null,cljs.core.PersistentHashSet.EMPTY,k__18678__auto__)){
return cljs.core.dissoc.call(null,cljs.core.with_meta.call(null,cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,this__18677__auto____$1),self__.__meta),k__18678__auto__);
} else {
return (new quantum.core.ns.Map(self__.__meta,cljs.core.not_empty.call(null,cljs.core.dissoc.call(null,self__.__extmap,k__18678__auto__)),null));
}
});

quantum.core.ns.Map.prototype.cljs$core$IAssociative$_assoc$arity$3 = (function (this__18675__auto__,k__18676__auto__,G__19308){
var self__ = this;
var this__18675__auto____$1 = this;
var pred__19312 = cljs.core.keyword_identical_QMARK_;
var expr__19313 = k__18676__auto__;
return (new quantum.core.ns.Map(self__.__meta,cljs.core.assoc.call(null,self__.__extmap,k__18676__auto__,G__19308),null));
});

quantum.core.ns.Map.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (this__18680__auto__){
var self__ = this;
var this__18680__auto____$1 = this;
return cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.Map.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (this__18667__auto__,G__19308){
var self__ = this;
var this__18667__auto____$1 = this;
return (new quantum.core.ns.Map(G__19308,self__.__extmap,self__.__hash));
});

quantum.core.ns.Map.prototype.cljs$core$ICollection$_conj$arity$2 = (function (this__18673__auto__,entry__18674__auto__){
var self__ = this;
var this__18673__auto____$1 = this;
if(cljs.core.vector_QMARK_.call(null,entry__18674__auto__)){
return cljs.core._assoc.call(null,this__18673__auto____$1,cljs.core._nth.call(null,entry__18674__auto__,(0)),cljs.core._nth.call(null,entry__18674__auto__,(1)));
} else {
return cljs.core.reduce.call(null,cljs.core._conj,this__18673__auto____$1,entry__18674__auto__);
}
});

quantum.core.ns.Map.getBasis = (function (){
return cljs.core.PersistentVector.EMPTY;
});

quantum.core.ns.Map.cljs$lang$type = true;

quantum.core.ns.Map.cljs$lang$ctorPrSeq = (function (this__18702__auto__){
return cljs.core._conj.call(null,cljs.core.List.EMPTY,"quantum.core.ns/Map");
});

quantum.core.ns.Map.cljs$lang$ctorPrWriter = (function (this__18702__auto__,writer__18703__auto__){
return cljs.core._write.call(null,writer__18703__auto__,"quantum.core.ns/Map");
});

quantum.core.ns.__GT_Map = (function quantum$core$ns$__GT_Map(){
return (new quantum.core.ns.Map(null,null,null));
});

quantum.core.ns.map__GT_Map = (function quantum$core$ns$map__GT_Map(G__19310){
return (new quantum.core.ns.Map(null,cljs.core.dissoc.call(null,G__19310),null));
});


/**
* @constructor
* @param {*} __meta
* @param {*} __extmap
* @param {*} __hash
* @param {*=} __meta 
* @param {*=} __extmap
* @param {number|null} __hash
*/
quantum.core.ns.Seq = (function (__meta,__extmap,__hash){
this.__meta = __meta;
this.__extmap = __extmap;
this.__hash = __hash;
this.cljs$lang$protocol_mask$partition0$ = 2229667594;
this.cljs$lang$protocol_mask$partition1$ = 8192;
})
quantum.core.ns.Seq.prototype.cljs$core$ILookup$_lookup$arity$2 = (function (this__18668__auto__,k__18669__auto__){
var self__ = this;
var this__18668__auto____$1 = this;
return cljs.core._lookup.call(null,this__18668__auto____$1,k__18669__auto__,null);
});

quantum.core.ns.Seq.prototype.cljs$core$ILookup$_lookup$arity$3 = (function (this__18670__auto__,k19317,else__18671__auto__){
var self__ = this;
var this__18670__auto____$1 = this;
var G__19319 = k19317;
switch (G__19319) {
default:
return cljs.core.get.call(null,self__.__extmap,k19317,else__18671__auto__);

}
});

quantum.core.ns.Seq.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this__18682__auto__,writer__18683__auto__,opts__18684__auto__){
var self__ = this;
var this__18682__auto____$1 = this;
var pr_pair__18685__auto__ = ((function (this__18682__auto____$1){
return (function (keyval__18686__auto__){
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,cljs.core.pr_writer,""," ","",opts__18684__auto__,keyval__18686__auto__);
});})(this__18682__auto____$1))
;
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,pr_pair__18685__auto__,"#quantum.core.ns.Seq{",", ","}",opts__18684__auto__,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.Seq.prototype.cljs$core$IMeta$_meta$arity$1 = (function (this__18666__auto__){
var self__ = this;
var this__18666__auto____$1 = this;
return self__.__meta;
});

quantum.core.ns.Seq.prototype.cljs$core$ICloneable$_clone$arity$1 = (function (this__18662__auto__){
var self__ = this;
var this__18662__auto____$1 = this;
return (new quantum.core.ns.Seq(self__.__meta,self__.__extmap,self__.__hash));
});

quantum.core.ns.Seq.prototype.cljs$core$ICounted$_count$arity$1 = (function (this__18672__auto__){
var self__ = this;
var this__18672__auto____$1 = this;
return (0 + cljs.core.count.call(null,self__.__extmap));
});

quantum.core.ns.Seq.prototype.cljs$core$IHash$_hash$arity$1 = (function (this__18663__auto__){
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

quantum.core.ns.Seq.prototype.cljs$core$IEquiv$_equiv$arity$2 = (function (this__18664__auto__,other__18665__auto__){
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

quantum.core.ns.Seq.prototype.cljs$core$IMap$_dissoc$arity$2 = (function (this__18677__auto__,k__18678__auto__){
var self__ = this;
var this__18677__auto____$1 = this;
if(cljs.core.contains_QMARK_.call(null,cljs.core.PersistentHashSet.EMPTY,k__18678__auto__)){
return cljs.core.dissoc.call(null,cljs.core.with_meta.call(null,cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,this__18677__auto____$1),self__.__meta),k__18678__auto__);
} else {
return (new quantum.core.ns.Seq(self__.__meta,cljs.core.not_empty.call(null,cljs.core.dissoc.call(null,self__.__extmap,k__18678__auto__)),null));
}
});

quantum.core.ns.Seq.prototype.cljs$core$IAssociative$_assoc$arity$3 = (function (this__18675__auto__,k__18676__auto__,G__19316){
var self__ = this;
var this__18675__auto____$1 = this;
var pred__19320 = cljs.core.keyword_identical_QMARK_;
var expr__19321 = k__18676__auto__;
return (new quantum.core.ns.Seq(self__.__meta,cljs.core.assoc.call(null,self__.__extmap,k__18676__auto__,G__19316),null));
});

quantum.core.ns.Seq.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (this__18680__auto__){
var self__ = this;
var this__18680__auto____$1 = this;
return cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.Seq.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (this__18667__auto__,G__19316){
var self__ = this;
var this__18667__auto____$1 = this;
return (new quantum.core.ns.Seq(G__19316,self__.__extmap,self__.__hash));
});

quantum.core.ns.Seq.prototype.cljs$core$ICollection$_conj$arity$2 = (function (this__18673__auto__,entry__18674__auto__){
var self__ = this;
var this__18673__auto____$1 = this;
if(cljs.core.vector_QMARK_.call(null,entry__18674__auto__)){
return cljs.core._assoc.call(null,this__18673__auto____$1,cljs.core._nth.call(null,entry__18674__auto__,(0)),cljs.core._nth.call(null,entry__18674__auto__,(1)));
} else {
return cljs.core.reduce.call(null,cljs.core._conj,this__18673__auto____$1,entry__18674__auto__);
}
});

quantum.core.ns.Seq.getBasis = (function (){
return cljs.core.PersistentVector.EMPTY;
});

quantum.core.ns.Seq.cljs$lang$type = true;

quantum.core.ns.Seq.cljs$lang$ctorPrSeq = (function (this__18702__auto__){
return cljs.core._conj.call(null,cljs.core.List.EMPTY,"quantum.core.ns/Seq");
});

quantum.core.ns.Seq.cljs$lang$ctorPrWriter = (function (this__18702__auto__,writer__18703__auto__){
return cljs.core._write.call(null,writer__18703__auto__,"quantum.core.ns/Seq");
});

quantum.core.ns.__GT_Seq = (function quantum$core$ns$__GT_Seq(){
return (new quantum.core.ns.Seq(null,null,null));
});

quantum.core.ns.map__GT_Seq = (function quantum$core$ns$map__GT_Seq(G__19318){
return (new quantum.core.ns.Seq(null,cljs.core.dissoc.call(null,G__19318),null));
});


/**
* @constructor
* @param {*} __meta
* @param {*} __extmap
* @param {*} __hash
* @param {*=} __meta 
* @param {*=} __extmap
* @param {number|null} __hash
*/
quantum.core.ns.Record = (function (__meta,__extmap,__hash){
this.__meta = __meta;
this.__extmap = __extmap;
this.__hash = __hash;
this.cljs$lang$protocol_mask$partition0$ = 2229667594;
this.cljs$lang$protocol_mask$partition1$ = 8192;
})
quantum.core.ns.Record.prototype.cljs$core$ILookup$_lookup$arity$2 = (function (this__18668__auto__,k__18669__auto__){
var self__ = this;
var this__18668__auto____$1 = this;
return cljs.core._lookup.call(null,this__18668__auto____$1,k__18669__auto__,null);
});

quantum.core.ns.Record.prototype.cljs$core$ILookup$_lookup$arity$3 = (function (this__18670__auto__,k19325,else__18671__auto__){
var self__ = this;
var this__18670__auto____$1 = this;
var G__19327 = k19325;
switch (G__19327) {
default:
return cljs.core.get.call(null,self__.__extmap,k19325,else__18671__auto__);

}
});

quantum.core.ns.Record.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this__18682__auto__,writer__18683__auto__,opts__18684__auto__){
var self__ = this;
var this__18682__auto____$1 = this;
var pr_pair__18685__auto__ = ((function (this__18682__auto____$1){
return (function (keyval__18686__auto__){
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,cljs.core.pr_writer,""," ","",opts__18684__auto__,keyval__18686__auto__);
});})(this__18682__auto____$1))
;
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,pr_pair__18685__auto__,"#quantum.core.ns.Record{",", ","}",opts__18684__auto__,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.Record.prototype.cljs$core$IMeta$_meta$arity$1 = (function (this__18666__auto__){
var self__ = this;
var this__18666__auto____$1 = this;
return self__.__meta;
});

quantum.core.ns.Record.prototype.cljs$core$ICloneable$_clone$arity$1 = (function (this__18662__auto__){
var self__ = this;
var this__18662__auto____$1 = this;
return (new quantum.core.ns.Record(self__.__meta,self__.__extmap,self__.__hash));
});

quantum.core.ns.Record.prototype.cljs$core$ICounted$_count$arity$1 = (function (this__18672__auto__){
var self__ = this;
var this__18672__auto____$1 = this;
return (0 + cljs.core.count.call(null,self__.__extmap));
});

quantum.core.ns.Record.prototype.cljs$core$IHash$_hash$arity$1 = (function (this__18663__auto__){
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

quantum.core.ns.Record.prototype.cljs$core$IEquiv$_equiv$arity$2 = (function (this__18664__auto__,other__18665__auto__){
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

quantum.core.ns.Record.prototype.cljs$core$IMap$_dissoc$arity$2 = (function (this__18677__auto__,k__18678__auto__){
var self__ = this;
var this__18677__auto____$1 = this;
if(cljs.core.contains_QMARK_.call(null,cljs.core.PersistentHashSet.EMPTY,k__18678__auto__)){
return cljs.core.dissoc.call(null,cljs.core.with_meta.call(null,cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,this__18677__auto____$1),self__.__meta),k__18678__auto__);
} else {
return (new quantum.core.ns.Record(self__.__meta,cljs.core.not_empty.call(null,cljs.core.dissoc.call(null,self__.__extmap,k__18678__auto__)),null));
}
});

quantum.core.ns.Record.prototype.cljs$core$IAssociative$_assoc$arity$3 = (function (this__18675__auto__,k__18676__auto__,G__19324){
var self__ = this;
var this__18675__auto____$1 = this;
var pred__19328 = cljs.core.keyword_identical_QMARK_;
var expr__19329 = k__18676__auto__;
return (new quantum.core.ns.Record(self__.__meta,cljs.core.assoc.call(null,self__.__extmap,k__18676__auto__,G__19324),null));
});

quantum.core.ns.Record.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (this__18680__auto__){
var self__ = this;
var this__18680__auto____$1 = this;
return cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.Record.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (this__18667__auto__,G__19324){
var self__ = this;
var this__18667__auto____$1 = this;
return (new quantum.core.ns.Record(G__19324,self__.__extmap,self__.__hash));
});

quantum.core.ns.Record.prototype.cljs$core$ICollection$_conj$arity$2 = (function (this__18673__auto__,entry__18674__auto__){
var self__ = this;
var this__18673__auto____$1 = this;
if(cljs.core.vector_QMARK_.call(null,entry__18674__auto__)){
return cljs.core._assoc.call(null,this__18673__auto____$1,cljs.core._nth.call(null,entry__18674__auto__,(0)),cljs.core._nth.call(null,entry__18674__auto__,(1)));
} else {
return cljs.core.reduce.call(null,cljs.core._conj,this__18673__auto____$1,entry__18674__auto__);
}
});

quantum.core.ns.Record.getBasis = (function (){
return cljs.core.PersistentVector.EMPTY;
});

quantum.core.ns.Record.cljs$lang$type = true;

quantum.core.ns.Record.cljs$lang$ctorPrSeq = (function (this__18702__auto__){
return cljs.core._conj.call(null,cljs.core.List.EMPTY,"quantum.core.ns/Record");
});

quantum.core.ns.Record.cljs$lang$ctorPrWriter = (function (this__18702__auto__,writer__18703__auto__){
return cljs.core._write.call(null,writer__18703__auto__,"quantum.core.ns/Record");
});

quantum.core.ns.__GT_Record = (function quantum$core$ns$__GT_Record(){
return (new quantum.core.ns.Record(null,null,null));
});

quantum.core.ns.map__GT_Record = (function quantum$core$ns$map__GT_Record(G__19326){
return (new quantum.core.ns.Record(null,cljs.core.dissoc.call(null,G__19326),null));
});


/**
* @constructor
* @param {*} __meta
* @param {*} __extmap
* @param {*} __hash
* @param {*=} __meta 
* @param {*=} __extmap
* @param {number|null} __hash
*/
quantum.core.ns.JSObj = (function (__meta,__extmap,__hash){
this.__meta = __meta;
this.__extmap = __extmap;
this.__hash = __hash;
this.cljs$lang$protocol_mask$partition0$ = 2229667594;
this.cljs$lang$protocol_mask$partition1$ = 8192;
})
quantum.core.ns.JSObj.prototype.cljs$core$ILookup$_lookup$arity$2 = (function (this__18668__auto__,k__18669__auto__){
var self__ = this;
var this__18668__auto____$1 = this;
return cljs.core._lookup.call(null,this__18668__auto____$1,k__18669__auto__,null);
});

quantum.core.ns.JSObj.prototype.cljs$core$ILookup$_lookup$arity$3 = (function (this__18670__auto__,k19333,else__18671__auto__){
var self__ = this;
var this__18670__auto____$1 = this;
var G__19335 = k19333;
switch (G__19335) {
default:
return cljs.core.get.call(null,self__.__extmap,k19333,else__18671__auto__);

}
});

quantum.core.ns.JSObj.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this__18682__auto__,writer__18683__auto__,opts__18684__auto__){
var self__ = this;
var this__18682__auto____$1 = this;
var pr_pair__18685__auto__ = ((function (this__18682__auto____$1){
return (function (keyval__18686__auto__){
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,cljs.core.pr_writer,""," ","",opts__18684__auto__,keyval__18686__auto__);
});})(this__18682__auto____$1))
;
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,pr_pair__18685__auto__,"#quantum.core.ns.JSObj{",", ","}",opts__18684__auto__,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.JSObj.prototype.cljs$core$IMeta$_meta$arity$1 = (function (this__18666__auto__){
var self__ = this;
var this__18666__auto____$1 = this;
return self__.__meta;
});

quantum.core.ns.JSObj.prototype.cljs$core$ICloneable$_clone$arity$1 = (function (this__18662__auto__){
var self__ = this;
var this__18662__auto____$1 = this;
return (new quantum.core.ns.JSObj(self__.__meta,self__.__extmap,self__.__hash));
});

quantum.core.ns.JSObj.prototype.cljs$core$ICounted$_count$arity$1 = (function (this__18672__auto__){
var self__ = this;
var this__18672__auto____$1 = this;
return (0 + cljs.core.count.call(null,self__.__extmap));
});

quantum.core.ns.JSObj.prototype.cljs$core$IHash$_hash$arity$1 = (function (this__18663__auto__){
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

quantum.core.ns.JSObj.prototype.cljs$core$IEquiv$_equiv$arity$2 = (function (this__18664__auto__,other__18665__auto__){
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

quantum.core.ns.JSObj.prototype.cljs$core$IMap$_dissoc$arity$2 = (function (this__18677__auto__,k__18678__auto__){
var self__ = this;
var this__18677__auto____$1 = this;
if(cljs.core.contains_QMARK_.call(null,cljs.core.PersistentHashSet.EMPTY,k__18678__auto__)){
return cljs.core.dissoc.call(null,cljs.core.with_meta.call(null,cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,this__18677__auto____$1),self__.__meta),k__18678__auto__);
} else {
return (new quantum.core.ns.JSObj(self__.__meta,cljs.core.not_empty.call(null,cljs.core.dissoc.call(null,self__.__extmap,k__18678__auto__)),null));
}
});

quantum.core.ns.JSObj.prototype.cljs$core$IAssociative$_assoc$arity$3 = (function (this__18675__auto__,k__18676__auto__,G__19332){
var self__ = this;
var this__18675__auto____$1 = this;
var pred__19336 = cljs.core.keyword_identical_QMARK_;
var expr__19337 = k__18676__auto__;
return (new quantum.core.ns.JSObj(self__.__meta,cljs.core.assoc.call(null,self__.__extmap,k__18676__auto__,G__19332),null));
});

quantum.core.ns.JSObj.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (this__18680__auto__){
var self__ = this;
var this__18680__auto____$1 = this;
return cljs.core.seq.call(null,cljs.core.concat.call(null,cljs.core.PersistentVector.EMPTY,self__.__extmap));
});

quantum.core.ns.JSObj.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (this__18667__auto__,G__19332){
var self__ = this;
var this__18667__auto____$1 = this;
return (new quantum.core.ns.JSObj(G__19332,self__.__extmap,self__.__hash));
});

quantum.core.ns.JSObj.prototype.cljs$core$ICollection$_conj$arity$2 = (function (this__18673__auto__,entry__18674__auto__){
var self__ = this;
var this__18673__auto____$1 = this;
if(cljs.core.vector_QMARK_.call(null,entry__18674__auto__)){
return cljs.core._assoc.call(null,this__18673__auto____$1,cljs.core._nth.call(null,entry__18674__auto__,(0)),cljs.core._nth.call(null,entry__18674__auto__,(1)));
} else {
return cljs.core.reduce.call(null,cljs.core._conj,this__18673__auto____$1,entry__18674__auto__);
}
});

quantum.core.ns.JSObj.getBasis = (function (){
return cljs.core.PersistentVector.EMPTY;
});

quantum.core.ns.JSObj.cljs$lang$type = true;

quantum.core.ns.JSObj.cljs$lang$ctorPrSeq = (function (this__18702__auto__){
return cljs.core._conj.call(null,cljs.core.List.EMPTY,"quantum.core.ns/JSObj");
});

quantum.core.ns.JSObj.cljs$lang$ctorPrWriter = (function (this__18702__auto__,writer__18703__auto__){
return cljs.core._write.call(null,writer__18703__auto__,"quantum.core.ns/JSObj");
});

quantum.core.ns.__GT_JSObj = (function quantum$core$ns$__GT_JSObj(){
return (new quantum.core.ns.JSObj(null,null,null));
});

quantum.core.ns.map__GT_JSObj = (function quantum$core$ns$map__GT_JSObj(G__19334){
return (new quantum.core.ns.JSObj(null,cljs.core.dissoc.call(null,G__19334),null));
});


/**
* @constructor
* @param {*} msg
* @param {*} __meta
* @param {*} __extmap
* @param {*} __hash
* @param {*=} __meta 
* @param {*=} __extmap
* @param {number|null} __hash
*/
quantum.core.ns.Exception = (function (msg,__meta,__extmap,__hash){
this.msg = msg;
this.__meta = __meta;
this.__extmap = __extmap;
this.__hash = __hash;
this.cljs$lang$protocol_mask$partition0$ = 2229667594;
this.cljs$lang$protocol_mask$partition1$ = 8192;
})
quantum.core.ns.Exception.prototype.cljs$core$ILookup$_lookup$arity$2 = (function (this__18668__auto__,k__18669__auto__){
var self__ = this;
var this__18668__auto____$1 = this;
return cljs.core._lookup.call(null,this__18668__auto____$1,k__18669__auto__,null);
});

quantum.core.ns.Exception.prototype.cljs$core$ILookup$_lookup$arity$3 = (function (this__18670__auto__,k19341,else__18671__auto__){
var self__ = this;
var this__18670__auto____$1 = this;
var G__19343 = (((k19341 instanceof cljs.core.Keyword))?k19341.fqn:null);
switch (G__19343) {
case "msg":
return self__.msg;

break;
default:
return cljs.core.get.call(null,self__.__extmap,k19341,else__18671__auto__);

}
});

quantum.core.ns.Exception.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this__18682__auto__,writer__18683__auto__,opts__18684__auto__){
var self__ = this;
var this__18682__auto____$1 = this;
var pr_pair__18685__auto__ = ((function (this__18682__auto____$1){
return (function (keyval__18686__auto__){
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,cljs.core.pr_writer,""," ","",opts__18684__auto__,keyval__18686__auto__);
});})(this__18682__auto____$1))
;
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,pr_pair__18685__auto__,"#quantum.core.ns.Exception{",", ","}",opts__18684__auto__,cljs.core.concat.call(null,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Keyword(null,"msg","msg",-1386103444),self__.msg],null))], null),self__.__extmap));
});

quantum.core.ns.Exception.prototype.cljs$core$IMeta$_meta$arity$1 = (function (this__18666__auto__){
var self__ = this;
var this__18666__auto____$1 = this;
return self__.__meta;
});

quantum.core.ns.Exception.prototype.cljs$core$ICloneable$_clone$arity$1 = (function (this__18662__auto__){
var self__ = this;
var this__18662__auto____$1 = this;
return (new quantum.core.ns.Exception(self__.msg,self__.__meta,self__.__extmap,self__.__hash));
});

quantum.core.ns.Exception.prototype.cljs$core$ICounted$_count$arity$1 = (function (this__18672__auto__){
var self__ = this;
var this__18672__auto____$1 = this;
return (1 + cljs.core.count.call(null,self__.__extmap));
});

quantum.core.ns.Exception.prototype.cljs$core$IHash$_hash$arity$1 = (function (this__18663__auto__){
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

quantum.core.ns.Exception.prototype.cljs$core$IEquiv$_equiv$arity$2 = (function (this__18664__auto__,other__18665__auto__){
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

quantum.core.ns.Exception.prototype.cljs$core$IMap$_dissoc$arity$2 = (function (this__18677__auto__,k__18678__auto__){
var self__ = this;
var this__18677__auto____$1 = this;
if(cljs.core.contains_QMARK_.call(null,new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"msg","msg",-1386103444),null], null), null),k__18678__auto__)){
return cljs.core.dissoc.call(null,cljs.core.with_meta.call(null,cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,this__18677__auto____$1),self__.__meta),k__18678__auto__);
} else {
return (new quantum.core.ns.Exception(self__.msg,self__.__meta,cljs.core.not_empty.call(null,cljs.core.dissoc.call(null,self__.__extmap,k__18678__auto__)),null));
}
});

quantum.core.ns.Exception.prototype.cljs$core$IAssociative$_assoc$arity$3 = (function (this__18675__auto__,k__18676__auto__,G__19340){
var self__ = this;
var this__18675__auto____$1 = this;
var pred__19344 = cljs.core.keyword_identical_QMARK_;
var expr__19345 = k__18676__auto__;
if(cljs.core.truth_(pred__19344.call(null,new cljs.core.Keyword(null,"msg","msg",-1386103444),expr__19345))){
return (new quantum.core.ns.Exception(G__19340,self__.__meta,self__.__extmap,null));
} else {
return (new quantum.core.ns.Exception(self__.msg,self__.__meta,cljs.core.assoc.call(null,self__.__extmap,k__18676__auto__,G__19340),null));
}
});

quantum.core.ns.Exception.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (this__18680__auto__){
var self__ = this;
var this__18680__auto____$1 = this;
return cljs.core.seq.call(null,cljs.core.concat.call(null,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Keyword(null,"msg","msg",-1386103444),self__.msg],null))], null),self__.__extmap));
});

quantum.core.ns.Exception.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (this__18667__auto__,G__19340){
var self__ = this;
var this__18667__auto____$1 = this;
return (new quantum.core.ns.Exception(self__.msg,G__19340,self__.__extmap,self__.__hash));
});

quantum.core.ns.Exception.prototype.cljs$core$ICollection$_conj$arity$2 = (function (this__18673__auto__,entry__18674__auto__){
var self__ = this;
var this__18673__auto____$1 = this;
if(cljs.core.vector_QMARK_.call(null,entry__18674__auto__)){
return cljs.core._assoc.call(null,this__18673__auto____$1,cljs.core._nth.call(null,entry__18674__auto__,(0)),cljs.core._nth.call(null,entry__18674__auto__,(1)));
} else {
return cljs.core.reduce.call(null,cljs.core._conj,this__18673__auto____$1,entry__18674__auto__);
}
});

quantum.core.ns.Exception.getBasis = (function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"msg","msg",254428083,null)], null);
});

quantum.core.ns.Exception.cljs$lang$type = true;

quantum.core.ns.Exception.cljs$lang$ctorPrSeq = (function (this__18702__auto__){
return cljs.core._conj.call(null,cljs.core.List.EMPTY,"quantum.core.ns/Exception");
});

quantum.core.ns.Exception.cljs$lang$ctorPrWriter = (function (this__18702__auto__,writer__18703__auto__){
return cljs.core._write.call(null,writer__18703__auto__,"quantum.core.ns/Exception");
});

quantum.core.ns.__GT_Exception = (function quantum$core$ns$__GT_Exception(msg){
return (new quantum.core.ns.Exception(msg,null,null,null));
});

quantum.core.ns.map__GT_Exception = (function quantum$core$ns$map__GT_Exception(G__19342){
return (new quantum.core.ns.Exception(new cljs.core.Keyword(null,"msg","msg",-1386103444).cljs$core$IFn$_invoke$arity$1(G__19342),null,cljs.core.dissoc.call(null,G__19342,new cljs.core.Keyword(null,"msg","msg",-1386103444)),null));
});


/**
* @constructor
* @param {*} msg
* @param {*} __meta
* @param {*} __extmap
* @param {*} __hash
* @param {*=} __meta 
* @param {*=} __extmap
* @param {number|null} __hash
*/
quantum.core.ns.IllegalArgumentException = (function (msg,__meta,__extmap,__hash){
this.msg = msg;
this.__meta = __meta;
this.__extmap = __extmap;
this.__hash = __hash;
this.cljs$lang$protocol_mask$partition0$ = 2229667594;
this.cljs$lang$protocol_mask$partition1$ = 8192;
})
quantum.core.ns.IllegalArgumentException.prototype.cljs$core$ILookup$_lookup$arity$2 = (function (this__18668__auto__,k__18669__auto__){
var self__ = this;
var this__18668__auto____$1 = this;
return cljs.core._lookup.call(null,this__18668__auto____$1,k__18669__auto__,null);
});

quantum.core.ns.IllegalArgumentException.prototype.cljs$core$ILookup$_lookup$arity$3 = (function (this__18670__auto__,k19349,else__18671__auto__){
var self__ = this;
var this__18670__auto____$1 = this;
var G__19351 = (((k19349 instanceof cljs.core.Keyword))?k19349.fqn:null);
switch (G__19351) {
case "msg":
return self__.msg;

break;
default:
return cljs.core.get.call(null,self__.__extmap,k19349,else__18671__auto__);

}
});

quantum.core.ns.IllegalArgumentException.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this__18682__auto__,writer__18683__auto__,opts__18684__auto__){
var self__ = this;
var this__18682__auto____$1 = this;
var pr_pair__18685__auto__ = ((function (this__18682__auto____$1){
return (function (keyval__18686__auto__){
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,cljs.core.pr_writer,""," ","",opts__18684__auto__,keyval__18686__auto__);
});})(this__18682__auto____$1))
;
return cljs.core.pr_sequential_writer.call(null,writer__18683__auto__,pr_pair__18685__auto__,"#quantum.core.ns.IllegalArgumentException{",", ","}",opts__18684__auto__,cljs.core.concat.call(null,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Keyword(null,"msg","msg",-1386103444),self__.msg],null))], null),self__.__extmap));
});

quantum.core.ns.IllegalArgumentException.prototype.cljs$core$IMeta$_meta$arity$1 = (function (this__18666__auto__){
var self__ = this;
var this__18666__auto____$1 = this;
return self__.__meta;
});

quantum.core.ns.IllegalArgumentException.prototype.cljs$core$ICloneable$_clone$arity$1 = (function (this__18662__auto__){
var self__ = this;
var this__18662__auto____$1 = this;
return (new quantum.core.ns.IllegalArgumentException(self__.msg,self__.__meta,self__.__extmap,self__.__hash));
});

quantum.core.ns.IllegalArgumentException.prototype.cljs$core$ICounted$_count$arity$1 = (function (this__18672__auto__){
var self__ = this;
var this__18672__auto____$1 = this;
return (1 + cljs.core.count.call(null,self__.__extmap));
});

quantum.core.ns.IllegalArgumentException.prototype.cljs$core$IHash$_hash$arity$1 = (function (this__18663__auto__){
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

quantum.core.ns.IllegalArgumentException.prototype.cljs$core$IEquiv$_equiv$arity$2 = (function (this__18664__auto__,other__18665__auto__){
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

quantum.core.ns.IllegalArgumentException.prototype.cljs$core$IMap$_dissoc$arity$2 = (function (this__18677__auto__,k__18678__auto__){
var self__ = this;
var this__18677__auto____$1 = this;
if(cljs.core.contains_QMARK_.call(null,new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"msg","msg",-1386103444),null], null), null),k__18678__auto__)){
return cljs.core.dissoc.call(null,cljs.core.with_meta.call(null,cljs.core.into.call(null,cljs.core.PersistentArrayMap.EMPTY,this__18677__auto____$1),self__.__meta),k__18678__auto__);
} else {
return (new quantum.core.ns.IllegalArgumentException(self__.msg,self__.__meta,cljs.core.not_empty.call(null,cljs.core.dissoc.call(null,self__.__extmap,k__18678__auto__)),null));
}
});

quantum.core.ns.IllegalArgumentException.prototype.cljs$core$IAssociative$_assoc$arity$3 = (function (this__18675__auto__,k__18676__auto__,G__19348){
var self__ = this;
var this__18675__auto____$1 = this;
var pred__19352 = cljs.core.keyword_identical_QMARK_;
var expr__19353 = k__18676__auto__;
if(cljs.core.truth_(pred__19352.call(null,new cljs.core.Keyword(null,"msg","msg",-1386103444),expr__19353))){
return (new quantum.core.ns.IllegalArgumentException(G__19348,self__.__meta,self__.__extmap,null));
} else {
return (new quantum.core.ns.IllegalArgumentException(self__.msg,self__.__meta,cljs.core.assoc.call(null,self__.__extmap,k__18676__auto__,G__19348),null));
}
});

quantum.core.ns.IllegalArgumentException.prototype.cljs$core$ISeqable$_seq$arity$1 = (function (this__18680__auto__){
var self__ = this;
var this__18680__auto____$1 = this;
return cljs.core.seq.call(null,cljs.core.concat.call(null,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Keyword(null,"msg","msg",-1386103444),self__.msg],null))], null),self__.__extmap));
});

quantum.core.ns.IllegalArgumentException.prototype.cljs$core$IWithMeta$_with_meta$arity$2 = (function (this__18667__auto__,G__19348){
var self__ = this;
var this__18667__auto____$1 = this;
return (new quantum.core.ns.IllegalArgumentException(self__.msg,G__19348,self__.__extmap,self__.__hash));
});

quantum.core.ns.IllegalArgumentException.prototype.cljs$core$ICollection$_conj$arity$2 = (function (this__18673__auto__,entry__18674__auto__){
var self__ = this;
var this__18673__auto____$1 = this;
if(cljs.core.vector_QMARK_.call(null,entry__18674__auto__)){
return cljs.core._assoc.call(null,this__18673__auto____$1,cljs.core._nth.call(null,entry__18674__auto__,(0)),cljs.core._nth.call(null,entry__18674__auto__,(1)));
} else {
return cljs.core.reduce.call(null,cljs.core._conj,this__18673__auto____$1,entry__18674__auto__);
}
});

quantum.core.ns.IllegalArgumentException.getBasis = (function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"msg","msg",254428083,null)], null);
});

quantum.core.ns.IllegalArgumentException.cljs$lang$type = true;

quantum.core.ns.IllegalArgumentException.cljs$lang$ctorPrSeq = (function (this__18702__auto__){
return cljs.core._conj.call(null,cljs.core.List.EMPTY,"quantum.core.ns/IllegalArgumentException");
});

quantum.core.ns.IllegalArgumentException.cljs$lang$ctorPrWriter = (function (this__18702__auto__,writer__18703__auto__){
return cljs.core._write.call(null,writer__18703__auto__,"quantum.core.ns/IllegalArgumentException");
});

quantum.core.ns.__GT_IllegalArgumentException = (function quantum$core$ns$__GT_IllegalArgumentException(msg){
return (new quantum.core.ns.IllegalArgumentException(msg,null,null,null));
});

quantum.core.ns.map__GT_IllegalArgumentException = (function quantum$core$ns$map__GT_IllegalArgumentException(G__19350){
return (new quantum.core.ns.IllegalArgumentException(new cljs.core.Keyword(null,"msg","msg",-1386103444).cljs$core$IFn$_invoke$arity$1(G__19350),null,cljs.core.dissoc.call(null,G__19350,new cljs.core.Keyword(null,"msg","msg",-1386103444)),null));
});


//# sourceMappingURL=ns.js.map?rel=1431625564051