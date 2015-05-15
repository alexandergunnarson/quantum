// Compiled by ClojureScript 0.0-3269 {}
goog.provide('quantum.core.string');
goog.require('cljs.core');
goog.require('quantum.core.logic');
goog.require('quantum.core.macros');
goog.require('quantum.core.type');
goog.require('quantum.core.numeric');
goog.require('quantum.core.reducers');
goog.require('cljs.core');
goog.require('quantum.core.function$');
goog.require('quantum.core.ns');
goog.require('quantum.core.loops');
goog.require('clojure.string');
quantum.core.string.replace = clojure.string.replace;
/**
 * Replace all.
 */
quantum.core.string.replace_with = (function quantum$core$string$replace_with(s,m){
return cljs.core.reduce.call(null,(function (ret,old_n,new_n){
return clojure.string.replace.call(null,ret,old_n,new_n);
}),s,m);
});
quantum.core.string.capitalize = clojure.string.capitalize;
quantum.core.string.split = clojure.string.split;
quantum.core.string.join = clojure.string.join;
quantum.core.string.upper_case = clojure.string.upper_case;
quantum.core.string.lower_case = clojure.string.lower_case;
quantum.core.string.triml = clojure.string.triml;
quantum.core.string.trimr = clojure.string.trimr;
quantum.core.string.re_find = cljs.core.re_find;
/**
 * Determines if an object @obj is a blank/empty string.
 */
quantum.core.string.blank_QMARK_ = (function quantum$core$string$blank_QMARK_(obj){
return quantum.core.logic.fn_and.call(null,cljs.core.string_QMARK_,cljs.core.empty_QMARK_).call(null,obj);
});
quantum.core.string.str_nil = (function quantum$core$string$str_nil(arg__22921__auto__){
var obj_f__22909__auto__ = arg__22921__auto__;
if((obj_f__22909__auto__ == null)){
return "";
} else {
return obj_f__22909__auto__;
}
});
quantum.core.string.upper_case_QMARK_ = (function quantum$core$string$upper_case_QMARK_(c){
return cljs.core._EQ_.call(null,c,c.toUpperCase());
});
quantum.core.string.lower_case_QMARK_ = (function quantum$core$string$lower_case_QMARK_(c){
return cljs.core._EQ_.call(null,c,c.toLowerCase());
});
quantum.core.string.char_PLUS_ = (function quantum$core$string$char_PLUS_(obj){
if(cljs.core.truth_(quantum.core.logic.fn_and.call(null,cljs.core.string_QMARK_,(function (x__22739__auto__){
return ((1) >= cljs.core.count.call(null,x__22739__auto__));
})).call(null,obj))){
return cljs.core.first.call(null,obj);
} else {
return cljs.core.char$.call(null,obj);
}
});
quantum.core.string.conv_regex_specials = (function quantum$core$string$conv_regex_specials(str_0){
return clojure.string.replace.call(null,clojure.string.replace.call(null,clojure.string.replace.call(null,clojure.string.replace.call(null,clojure.string.replace.call(null,clojure.string.replace.call(null,clojure.string.replace.call(null,clojure.string.replace.call(null,clojure.string.replace.call(null,clojure.string.replace.call(null,clojure.string.replace.call(null,str_0,"\\","\\\\"),"$","\\$"),"^","\\^"),".","\\."),"|","\\|"),"*","\\*"),"+","\\+"),"(","\\("),")","\\)"),"[","\\["),"{","\\{");
});
/**
 * Gives a consistent, flexible, cross-platform substring API with support for:
 * * Clamping of indexes beyond string limits.
 * * Negative indexes: [   0   |   1   |  ...  |  n-1  |   n   ) or
 * [  -n   | -n+1  |  ...  |  -1   |   0).
 * (start index inclusive, end index exclusive).
 * 
 * Note that `max-len` was chosen over `end-idx` since it's less ambiguous and
 * easier to reason about - esp. when accepting negative indexes.
 * From taoensso.encore.
 */
quantum.core.string.subs_PLUS_ = (function quantum$core$string$subs_PLUS_(){
var argseq__19113__auto__ = ((((2) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(2)),(0))):null);
return quantum.core.string.subs_PLUS_.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),argseq__19113__auto__);
});

quantum.core.string.subs_PLUS_.cljs$core$IFn$_invoke$arity$variadic = (function (s,start_idx,p__24774){
var vec__24775 = p__24774;
var max_len = cljs.core.nth.call(null,vec__24775,(0),null);
if(cljs.core.truth_((function (){var or__18073__auto__ = (max_len == null);
if(or__18073__auto__){
return or__18073__auto__;
} else {
return quantum.core.numeric.nneg_int_QMARK_.call(null,max_len);
}
})())){
} else {
throw (new Error([cljs.core.str("Assert failed: "),cljs.core.str(cljs.core.pr_str.call(null,cljs.core.list(new cljs.core.Symbol(null,"or","or",1876275696,null),cljs.core.list(new cljs.core.Symbol(null,"nil?","nil?",1612038930,null),new cljs.core.Symbol(null,"max-len","max-len",1621685511,null)),cljs.core.list(new cljs.core.Symbol("num","nneg-int?","num/nneg-int?",803621830,null),new cljs.core.Symbol(null,"max-len","max-len",1621685511,null)))))].join('')));
}

var slen = cljs.core.count.call(null,s);
var start_idx_STAR_ = (((start_idx >= (0)))?(function (){var x__18392__auto__ = start_idx;
var y__18393__auto__ = slen;
return ((x__18392__auto__ < y__18393__auto__) ? x__18392__auto__ : y__18393__auto__);
})():(function (){var x__18385__auto__ = (0);
var y__18386__auto__ = ((slen + start_idx) - (1));
return ((x__18385__auto__ > y__18386__auto__) ? x__18385__auto__ : y__18386__auto__);
})());
var end_idx_STAR_ = ((cljs.core.not.call(null,max_len))?slen:(function (){var x__18392__auto__ = (start_idx_STAR_ + max_len);
var y__18393__auto__ = slen;
return ((x__18392__auto__ < y__18393__auto__) ? x__18392__auto__ : y__18393__auto__);
})());
return s.substring(start_idx_STAR_,end_idx_STAR_);
});

quantum.core.string.subs_PLUS_.cljs$lang$maxFixedArity = (2);

quantum.core.string.subs_PLUS_.cljs$lang$applyTo = (function (seq24771){
var G__24772 = cljs.core.first.call(null,seq24771);
var seq24771__$1 = cljs.core.next.call(null,seq24771);
var G__24773 = cljs.core.first.call(null,seq24771__$1);
var seq24771__$2 = cljs.core.next.call(null,seq24771__$1);
return quantum.core.string.subs_PLUS_.cljs$core$IFn$_invoke$arity$variadic(G__24772,G__24773,seq24771__$2);
});
quantum.core.string.starts_with_QMARK_ = (function quantum$core$string$starts_with_QMARK_(super$,sub){
if(typeof super$ === 'string'){
return (super$.indexOf(sub) === (0));
} else {
if((super$ instanceof cljs.core.Keyword)){
return quantum$core$string$starts_with_QMARK_.call(null,cljs.core.name.call(null,super$),sub);
} else {
return null;
}
}
});
quantum.core.string.ends_with_QMARK_ = (function quantum$core$string$ends_with_QMARK_(super$,sub){
if(typeof super$ === 'string'){
return super$.endsWith(sub);
} else {
if((super$ instanceof cljs.core.Keyword)){
return quantum$core$string$ends_with_QMARK_.call(null,cljs.core.name.call(null,super$),sub);
} else {
return null;
}
}
});
quantum.core.string.remove_all = (function quantum$core$string$remove_all(str_0,to_remove){
return cljs.core.reduce.call(null,(function (p1__24776_SHARP_,p2__24777_SHARP_){
return quantum.core.string.replace.call(null,p1__24776_SHARP_,p2__24777_SHARP_,"");
}),str_0,to_remove);
});
/**
 * Like /clojure.string/join/ but ensures no double separators.
 */
quantum.core.string.join_once = (function quantum$core$string$join_once(){
var argseq__19113__auto__ = ((((1) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(1)),(0))):null);
return quantum.core.string.join_once.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__19113__auto__);
});

quantum.core.string.join_once.cljs$core$IFn$_invoke$arity$variadic = (function (separator,coll){
return cljs.core.reduce.call(null,(function (s1,s2){
var s1__$1 = [cljs.core.str(s1)].join('');
var s2__$1 = [cljs.core.str(s2)].join('');
if(cljs.core.truth_(quantum.core.string.ends_with_QMARK_.call(null,s1__$1,separator))){
if(cljs.core.truth_(quantum.core.string.starts_with_QMARK_.call(null,s2__$1,separator))){
return [cljs.core.str(s1__$1),cljs.core.str(s2__$1.substring((1)))].join('');
} else {
return [cljs.core.str(s1__$1),cljs.core.str(s2__$1)].join('');
}
} else {
if(cljs.core.truth_(quantum.core.string.starts_with_QMARK_.call(null,s2__$1,separator))){
return [cljs.core.str(s1__$1),cljs.core.str(s2__$1)].join('');
} else {
if((cljs.core._EQ_.call(null,s1__$1,"")) || (cljs.core._EQ_.call(null,s2__$1,""))){
return [cljs.core.str(s1__$1),cljs.core.str(s2__$1)].join('');
} else {
return [cljs.core.str(s1__$1),cljs.core.str(separator),cljs.core.str(s2__$1)].join('');
}
}
}
}),null,coll);
});

quantum.core.string.join_once.cljs$lang$maxFixedArity = (1);

quantum.core.string.join_once.cljs$lang$applyTo = (function (seq24778){
var G__24779 = cljs.core.first.call(null,seq24778);
var seq24778__$1 = cljs.core.next.call(null,seq24778);
return quantum.core.string.join_once.cljs$core$IFn$_invoke$arity$variadic(G__24779,seq24778__$1);
});
quantum.core.string.remove_from_end = (function quantum$core$string$remove_from_end(string,end){
if(cljs.core.truth_(string.endsWith(end))){
return string.substring((0),(cljs.core.count.call(null,string) - cljs.core.count.call(null,end)));
} else {
return string;
}
});
quantum.core.string.remove_extra_whitespace = (function quantum$core$string$remove_extra_whitespace(string_0){
var string_n = string_0;
while(true){
if(cljs.core._EQ_.call(null,string_n,clojure.string.replace.call(null,string_n,"  "," "))){
return string_n;
} else {
var G__24780 = clojure.string.replace.call(null,string_n,"  "," ");
string_n = G__24780;
continue;
}
break;
}
});
quantum.core.string.capitalize_each_word = (function quantum$core$string$capitalize_each_word(string){
return clojure.string.join.call(null," ",cljs.core.map.call(null,clojure.string.capitalize,clojure.string.split.call(null,string,/ /)));
});
quantum.core.string.properize_keyword = (function quantum$core$string$properize_keyword(x__22733__auto__){
return quantum.core.string.capitalize_each_word.call(null,quantum.core.string.replace.call(null,(function (){var obj_f__22879__auto__ = x__22733__auto__;
if((obj_f__22879__auto__ == null)){
return quantum.core.string.str_nil.call(null,obj_f__22879__auto__);
} else {
return cljs.core.name.call(null,obj_f__22879__auto__);
}
})(),/\-/," "));
});
quantum.core.string.keywordize = (function quantum$core$string$keywordize(kw){
return cljs.core.keyword.call(null,quantum.core.string.lower_case.call(null,quantum.core.string.replace.call(null,kw," ","-")));
});
quantum.core.string.camelcase = (function quantum$core$string$camelcase(){
var argseq__19113__auto__ = ((((1) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(1)),(0))):null);
return quantum.core.string.camelcase.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__19113__auto__);
});

quantum.core.string.camelcase.cljs$core$IFn$_invoke$arity$variadic = (function (str_0,p__24784){
var vec__24785 = p__24784;
var method_QMARK_ = cljs.core.nth.call(null,vec__24785,(0),null);
return ((function (vec__24785,method_QMARK_){
return (function (p1__24781_SHARP_){
if(cljs.core.not.call(null,method_QMARK_)){
return cljs.core.apply.call(null,cljs.core.str,quantum.core.string.upper_case.call(null,cljs.core.first.call(null,p1__24781_SHARP_)),cljs.core.rest.call(null,p1__24781_SHARP_));
} else {
return p1__24781_SHARP_;
}
});})(vec__24785,method_QMARK_))
.call(null,quantum.core.string.replace.call(null,str_0,/[-_](\w)/,quantum.core.function$.compr.call(null,cljs.core.second,quantum.core.string.upper_case)));
});

quantum.core.string.camelcase.cljs$lang$maxFixedArity = (1);

quantum.core.string.camelcase.cljs$lang$applyTo = (function (seq24782){
var G__24783 = cljs.core.first.call(null,seq24782);
var seq24782__$1 = cljs.core.next.call(null,seq24782);
return quantum.core.string.camelcase.cljs$core$IFn$_invoke$arity$variadic(G__24783,seq24782__$1);
});
quantum.core.string.un_camelcase = (function quantum$core$string$un_camelcase(sym){
var str_0 = [cljs.core.str(sym)].join('');
var matches = cljs.core.distinct.call(null,cljs.core.re_seq.call(null,/[a-z0-1][A-Z]/,str_0));
return quantum.core.string.lower_case.call(null,quantum.core.reducers.reduce_PLUS_.call(null,((function (str_0,matches){
return (function (ret,p__24788){
var vec__24789 = p__24788;
var char1 = cljs.core.nth.call(null,vec__24789,(0),null);
var char2 = cljs.core.nth.call(null,vec__24789,(1),null);
var match = vec__24789;
return quantum.core.string.replace.call(null,ret,match,[cljs.core.str(char1),cljs.core.str("-"),cljs.core.str(quantum.core.string.lower_case.call(null,char2))].join(''));
});})(str_0,matches))
,str_0,matches));
});
/**
 * Wraps a given string in single quotes.
 */
quantum.core.string.squote = (function quantum$core$string$squote(str_0){
if((str_0 == null)){
return [cljs.core.str("'"),cljs.core.str("nil"),cljs.core.str("'")].join('');
} else {
return [cljs.core.str("'"),cljs.core.str(str_0),cljs.core.str("'")].join('');
}
});
/**
 * Wraps a given string in parentheses.
 */
quantum.core.string.paren = (function quantum$core$string$paren(str_0){
if((str_0 == null)){
return [cljs.core.str("("),cljs.core.str("nil"),cljs.core.str(")")].join('');
} else {
return [cljs.core.str("("),cljs.core.str(str_0),cljs.core.str(")")].join('');
}
});
/**
 * Like |str|, but adds spaces between the arguments.
 */
quantum.core.string.sp = (function quantum$core$string$sp(){
var argseq__19113__auto__ = ((((0) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(0)),(0))):null);
return quantum.core.string.sp.cljs$core$IFn$_invoke$arity$variadic(argseq__19113__auto__);
});

quantum.core.string.sp.cljs$core$IFn$_invoke$arity$variadic = (function (args){
var n__23387__auto__ = cljs.core.volatile_BANG_.call(null,cljs.core.long$.call(null,(-1)));
return cljs.core.reduce.call(null,((function (n__23387__auto__){
return (function (ret_n__23388__auto__,elem__23389__auto__){
cljs.core._vreset_BANG_.call(null,n__23387__auto__,(cljs.core._deref.call(null,n__23387__auto__) + (1)));

return ((function (n__23387__auto__){
return (function (ret,elem,n){
if(cljs.core._EQ_.call(null,n,(cljs.core.count.call(null,args) - (1)))){
return [cljs.core.str(ret),cljs.core.str(elem)].join('');
} else {
return [cljs.core.str(ret),cljs.core.str(elem),cljs.core.str(" ")].join('');
}
});})(n__23387__auto__))
.call(null,ret_n__23388__auto__,elem__23389__auto__,cljs.core.deref.call(null,n__23387__auto__));
});})(n__23387__auto__))
,"",args);
});

quantum.core.string.sp.cljs$lang$maxFixedArity = (0);

quantum.core.string.sp.cljs$lang$applyTo = (function (seq24790){
return quantum.core.string.sp.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq24790));
});
/**
 * Like |sp|, but adds commas and spaces between the arguments.
 */
quantum.core.string.sp_comma = (function quantum$core$string$sp_comma(){
var argseq__19113__auto__ = ((((0) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(0)),(0))):null);
return quantum.core.string.sp_comma.cljs$core$IFn$_invoke$arity$variadic(argseq__19113__auto__);
});

quantum.core.string.sp_comma.cljs$core$IFn$_invoke$arity$variadic = (function (args){
var n__23387__auto__ = cljs.core.volatile_BANG_.call(null,cljs.core.long$.call(null,(-1)));
return cljs.core.reduce.call(null,((function (n__23387__auto__){
return (function (ret_n__23388__auto__,elem__23389__auto__){
cljs.core._vreset_BANG_.call(null,n__23387__auto__,(cljs.core._deref.call(null,n__23387__auto__) + (1)));

return ((function (n__23387__auto__){
return (function (ret,elem,n){
if(cljs.core._EQ_.call(null,n,(cljs.core.count.call(null,args) - (1)))){
return [cljs.core.str(ret),cljs.core.str(elem)].join('');
} else {
return [cljs.core.str(ret),cljs.core.str(elem),cljs.core.str(", ")].join('');
}
});})(n__23387__auto__))
.call(null,ret_n__23388__auto__,elem__23389__auto__,cljs.core.deref.call(null,n__23387__auto__));
});})(n__23387__auto__))
,"",args);
});

quantum.core.string.sp_comma.cljs$lang$maxFixedArity = (0);

quantum.core.string.sp_comma.cljs$lang$applyTo = (function (seq24791){
return quantum.core.string.sp_comma.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq24791));
});
quantum.core.string.re_get = (function quantum$core$string$re_get(regex,string){

new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"attribution","attribution",1937239286),"thebusby.bagotricks"], null);

var vec__24793 = quantum.core.string.re_find.call(null,regex,string);
var _ = cljs.core.nth.call(null,vec__24793,(0),null);
var xs = cljs.core.nthnext.call(null,vec__24793,(1));
return xs;
});
quantum.core.string.re_find_PLUS_ = (function quantum$core$string$re_find_PLUS_(pat,in_str){
try{return quantum.core.string.re_find.call(null,cljs.core.re_pattern.call(null,pat),in_str);
}catch (e24795){if((e24795 instanceof TypeError)){
var _ = e24795;
return null;
} else {
throw e24795;

}
}});
quantum.core.string.alphabet = quantum.core.reducers.fold_PLUS_.call(null,quantum.core.reducers.map_PLUS_.call(null,(function (x__22733__auto__){
return [cljs.core.str(cljs.core.char$.call(null,x__22733__auto__))].join('');
}),quantum.core.reducers.range_PLUS_.call(null,(65),((90) + (1)))));
quantum.core.string.rand_str = (function quantum$core$string$rand_str(len){
return quantum.core.reducers.reduce_PLUS_.call(null,cljs.core.str,quantum.core.reducers.folder_PLUS_.call(null,cljs.core.range.call(null,(0),len),(function (f__24798){
return (function() {
var G__24800 = null;
var G__24800__0 = (function (){
return f__24798.call(null);
});
var G__24800__2 = (function (ret__24799,n){
return f__24798.call(null,ret__24799,[cljs.core.str(cljs.core.char$.call(null,((65) + cljs.core.rand_int.call(null,(((90) + (1)) - (65))))))].join(''));
});
var G__24800__3 = (function (ret__24799,k__24740__auto__,v__24741__auto__){
var n = quantum.core.data.map.map_entry.call(null,k__24740__auto__,v__24741__auto__);
return f__24798.call(null,ret__24799,[cljs.core.str(cljs.core.char$.call(null,((65) + cljs.core.rand_int.call(null,(((90) + (1)) - (65))))))].join(''));
});
G__24800 = function(ret__24799,k__24740__auto__,v__24741__auto__){
switch(arguments.length){
case 0:
return G__24800__0.call(this);
case 2:
return G__24800__2.call(this,ret__24799,k__24740__auto__);
case 3:
return G__24800__3.call(this,ret__24799,k__24740__auto__,v__24741__auto__);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
G__24800.cljs$core$IFn$_invoke$arity$0 = G__24800__0;
G__24800.cljs$core$IFn$_invoke$arity$2 = G__24800__2;
G__24800.cljs$core$IFn$_invoke$arity$3 = G__24800__3;
return G__24800;
})()
})));
});
quantum.core.string.val = (function quantum$core$string$val(obj){
if(typeof obj === 'string'){
var obj_f__22909__auto__ = Number(obj);
if(cljs.core.truth_(isNaN(obj_f__22909__auto__))){
return obj;
} else {
return obj_f__22909__auto__;
}
} else {
return obj;
}
});
quantum.core.string.vowels = new cljs.core.PersistentVector(null, 10, 5, cljs.core.PersistentVector.EMPTY_NODE, ["a","e","i","o","u","A","E","I","O","U"], null);
/**
 * Like |str| but for keywords.
 */
quantum.core.string.keyword_PLUS_ = (function quantum$core$string$keyword_PLUS_(){
var G__24804 = arguments.length;
switch (G__24804) {
case 1:
return quantum.core.string.keyword_PLUS_.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
default:
var argseq__19124__auto__ = (new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(1)),(0)));
return quantum.core.string.keyword_PLUS_.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__19124__auto__);

}
});

quantum.core.string.keyword_PLUS_.cljs$core$IFn$_invoke$arity$1 = (function (obj){
if((obj instanceof cljs.core.Keyword)){
return obj;
} else {
if(typeof obj === 'string'){
return cljs.core.keyword.call(null,obj);
} else {
return null;
}
}
});

quantum.core.string.keyword_PLUS_.cljs$core$IFn$_invoke$arity$variadic = (function (obj,objs){
return cljs.core.keyword.call(null,quantum.core.reducers.reduce_PLUS_.call(null,(function (ret,elem){
return [cljs.core.str(ret),cljs.core.str(cljs.core.name.call(null,elem))].join('');
}),"",cljs.core.cons.call(null,obj,objs)));
});

quantum.core.string.keyword_PLUS_.cljs$lang$applyTo = (function (seq24801){
var G__24802 = cljs.core.first.call(null,seq24801);
var seq24801__$1 = cljs.core.next.call(null,seq24801);
return quantum.core.string.keyword_PLUS_.cljs$core$IFn$_invoke$arity$variadic(G__24802,seq24801__$1);
});

quantum.core.string.keyword_PLUS_.cljs$lang$maxFixedArity = (1);

//# sourceMappingURL=string.js.map?rel=1431625570690