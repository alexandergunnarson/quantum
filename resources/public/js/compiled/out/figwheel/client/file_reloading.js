// Compiled by ClojureScript 0.0-3269 {}
goog.provide('figwheel.client.file_reloading');
goog.require('cljs.core');
goog.require('goog.Uri');
goog.require('goog.string');
goog.require('goog.net.jsloader');
goog.require('cljs.core.async');
goog.require('clojure.set');
goog.require('clojure.string');
goog.require('figwheel.client.utils');

figwheel.client.file_reloading.all_QMARK_ = (function figwheel$client$file_reloading$all_QMARK_(pred,coll){
return cljs.core.reduce.call(null,(function (p1__31324_SHARP_,p2__31325_SHARP_){
var and__18061__auto__ = p1__31324_SHARP_;
if(cljs.core.truth_(and__18061__auto__)){
return p2__31325_SHARP_;
} else {
return and__18061__auto__;
}
}),true,cljs.core.map.call(null,pred,coll));
});
figwheel.client.file_reloading.namespace_file_map_QMARK_ = (function figwheel$client$file_reloading$namespace_file_map_QMARK_(m){
var or__18073__auto__ = (cljs.core.map_QMARK_.call(null,m)) && (typeof new cljs.core.Keyword(null,"namespace","namespace",-377510372).cljs$core$IFn$_invoke$arity$1(m) === 'string') && (typeof new cljs.core.Keyword(null,"file","file",-1269645878).cljs$core$IFn$_invoke$arity$1(m) === 'string') && (cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"type","type",1174270348).cljs$core$IFn$_invoke$arity$1(m),new cljs.core.Keyword(null,"namespace","namespace",-377510372)));
if(or__18073__auto__){
return or__18073__auto__;
} else {
cljs.core.println.call(null,"Error not namespace-file-map",cljs.core.pr_str.call(null,m));

return false;
}
});
figwheel.client.file_reloading.add_cache_buster = (function figwheel$client$file_reloading$add_cache_buster(url){

return goog.Uri.parse(url).makeUnique();
});
figwheel.client.file_reloading.ns_to_js_file = (function figwheel$client$file_reloading$ns_to_js_file(ns){

return [cljs.core.str(clojure.string.replace.call(null,ns,".","/")),cljs.core.str(".js")].join('');
});
figwheel.client.file_reloading.resolve_ns = (function figwheel$client$file_reloading$resolve_ns(ns){

return [cljs.core.str(figwheel.client.utils.base_url_path.call(null)),cljs.core.str(figwheel.client.file_reloading.ns_to_js_file.call(null,ns))].join('');
});
figwheel.client.file_reloading.patch_goog_base = (function figwheel$client$file_reloading$patch_goog_base(){
goog.isProvided = (function (x){
return false;
});

if(((cljs.core._STAR_loaded_libs_STAR_ == null)) || (cljs.core.empty_QMARK_.call(null,cljs.core._STAR_loaded_libs_STAR_))){
cljs.core._STAR_loaded_libs_STAR_ = (function (){var gntp = goog.dependencies_.nameToPath;
return cljs.core.into.call(null,cljs.core.PersistentHashSet.EMPTY,cljs.core.filter.call(null,((function (gntp){
return (function (name){
return (goog.dependencies_.visited[(gntp[name])]);
});})(gntp))
,cljs.core.js_keys.call(null,gntp)));
})();
} else {
}

goog.require = (function (name,reload){
if(cljs.core.truth_((function (){var or__18073__auto__ = !(cljs.core.contains_QMARK_.call(null,cljs.core._STAR_loaded_libs_STAR_,name));
if(or__18073__auto__){
return or__18073__auto__;
} else {
return reload;
}
})())){
cljs.core._STAR_loaded_libs_STAR_ = cljs.core.conj.call(null,(function (){var or__18073__auto__ = cljs.core._STAR_loaded_libs_STAR_;
if(cljs.core.truth_(or__18073__auto__)){
return or__18073__auto__;
} else {
return cljs.core.PersistentHashSet.EMPTY;
}
})(),name);

return figwheel.client.file_reloading.reload_file_STAR_.call(null,figwheel.client.file_reloading.resolve_ns.call(null,name));
} else {
return null;
}
});

goog.provide = goog.exportPath_;

return goog.global.CLOSURE_IMPORT_SCRIPT = figwheel.client.file_reloading.reload_file_STAR_;
});
if(typeof figwheel.client.file_reloading.resolve_url !== 'undefined'){
} else {
figwheel.client.file_reloading.resolve_url = (function (){var method_table__18968__auto__ = cljs.core.atom.call(null,cljs.core.PersistentArrayMap.EMPTY);
var prefer_table__18969__auto__ = cljs.core.atom.call(null,cljs.core.PersistentArrayMap.EMPTY);
var method_cache__18970__auto__ = cljs.core.atom.call(null,cljs.core.PersistentArrayMap.EMPTY);
var cached_hierarchy__18971__auto__ = cljs.core.atom.call(null,cljs.core.PersistentArrayMap.EMPTY);
var hierarchy__18972__auto__ = cljs.core.get.call(null,cljs.core.PersistentArrayMap.EMPTY,new cljs.core.Keyword(null,"hierarchy","hierarchy",-1053470341),cljs.core.get_global_hierarchy.call(null));
return (new cljs.core.MultiFn(cljs.core.symbol.call(null,"figwheel.client.file-reloading","resolve-url"),new cljs.core.Keyword(null,"type","type",1174270348),new cljs.core.Keyword(null,"default","default",-1987822328),hierarchy__18972__auto__,method_table__18968__auto__,prefer_table__18969__auto__,method_cache__18970__auto__,cached_hierarchy__18971__auto__));
})();
}
cljs.core._add_method.call(null,figwheel.client.file_reloading.resolve_url,new cljs.core.Keyword(null,"default","default",-1987822328),(function (p__31326){
var map__31327 = p__31326;
var map__31327__$1 = ((cljs.core.seq_QMARK_.call(null,map__31327))?cljs.core.apply.call(null,cljs.core.hash_map,map__31327):map__31327);
var file = cljs.core.get.call(null,map__31327__$1,new cljs.core.Keyword(null,"file","file",-1269645878));
return file;
}));
cljs.core._add_method.call(null,figwheel.client.file_reloading.resolve_url,new cljs.core.Keyword(null,"namespace","namespace",-377510372),(function (p__31328){
var map__31329 = p__31328;
var map__31329__$1 = ((cljs.core.seq_QMARK_.call(null,map__31329))?cljs.core.apply.call(null,cljs.core.hash_map,map__31329):map__31329);
var namespace = cljs.core.get.call(null,map__31329__$1,new cljs.core.Keyword(null,"namespace","namespace",-377510372));

return figwheel.client.file_reloading.resolve_ns.call(null,namespace);
}));
if(typeof figwheel.client.file_reloading.reload_base !== 'undefined'){
} else {
figwheel.client.file_reloading.reload_base = (function (){var method_table__18968__auto__ = cljs.core.atom.call(null,cljs.core.PersistentArrayMap.EMPTY);
var prefer_table__18969__auto__ = cljs.core.atom.call(null,cljs.core.PersistentArrayMap.EMPTY);
var method_cache__18970__auto__ = cljs.core.atom.call(null,cljs.core.PersistentArrayMap.EMPTY);
var cached_hierarchy__18971__auto__ = cljs.core.atom.call(null,cljs.core.PersistentArrayMap.EMPTY);
var hierarchy__18972__auto__ = cljs.core.get.call(null,cljs.core.PersistentArrayMap.EMPTY,new cljs.core.Keyword(null,"hierarchy","hierarchy",-1053470341),cljs.core.get_global_hierarchy.call(null));
return (new cljs.core.MultiFn(cljs.core.symbol.call(null,"figwheel.client.file-reloading","reload-base"),figwheel.client.utils.host_env_QMARK_,new cljs.core.Keyword(null,"default","default",-1987822328),hierarchy__18972__auto__,method_table__18968__auto__,prefer_table__18969__auto__,method_cache__18970__auto__,cached_hierarchy__18971__auto__));
})();
}
cljs.core._add_method.call(null,figwheel.client.file_reloading.reload_base,new cljs.core.Keyword(null,"node","node",581201198),(function (request_url,callback){

var root = clojure.string.join.call(null,"/",cljs.core.reverse.call(null,cljs.core.drop.call(null,(2),cljs.core.reverse.call(null,clojure.string.split.call(null,__dirname,"/")))));
var path = [cljs.core.str(root),cljs.core.str("/"),cljs.core.str(request_url)].join('');
(require.cache[path] = null);

return callback.call(null,(function (){try{return require(path);
}catch (e31330){if((e31330 instanceof Error)){
var e = e31330;
figwheel.client.utils.log.call(null,new cljs.core.Keyword(null,"error","error",-978969032),[cljs.core.str("Figwheel: Error loading file "),cljs.core.str(path)].join(''));

figwheel.client.utils.log.call(null,new cljs.core.Keyword(null,"error","error",-978969032),e.stack);

return false;
} else {
throw e31330;

}
}})());
}));
cljs.core._add_method.call(null,figwheel.client.file_reloading.reload_base,new cljs.core.Keyword(null,"html","html",-998796897),(function (request_url,callback){

var deferred = goog.net.jsloader.load(figwheel.client.file_reloading.add_cache_buster.call(null,request_url),{"cleanupWhenDone": true});
deferred.addCallback(((function (deferred){
return (function (){
return cljs.core.apply.call(null,callback,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [true], null));
});})(deferred))
);

return deferred.addErrback(((function (deferred){
return (function (){
return cljs.core.apply.call(null,callback,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [false], null));
});})(deferred))
);
}));
figwheel.client.file_reloading.reload_file_STAR_ = (function figwheel$client$file_reloading$reload_file_STAR_(){
var G__31332 = arguments.length;
switch (G__31332) {
case 2:
return figwheel.client.file_reloading.reload_file_STAR_.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 1:
return figwheel.client.file_reloading.reload_file_STAR_.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

figwheel.client.file_reloading.reload_file_STAR_.cljs$core$IFn$_invoke$arity$2 = (function (request_url,callback){
return figwheel.client.file_reloading.reload_base.call(null,request_url,callback);
});

figwheel.client.file_reloading.reload_file_STAR_.cljs$core$IFn$_invoke$arity$1 = (function (request_url){
return figwheel.client.file_reloading.reload_file_STAR_.call(null,request_url,cljs.core.identity);
});

figwheel.client.file_reloading.reload_file_STAR_.cljs$lang$maxFixedArity = 2;
figwheel.client.file_reloading.reload_file = (function figwheel$client$file_reloading$reload_file(p__31334,callback){
var map__31336 = p__31334;
var map__31336__$1 = ((cljs.core.seq_QMARK_.call(null,map__31336))?cljs.core.apply.call(null,cljs.core.hash_map,map__31336):map__31336);
var file_msg = map__31336__$1;
var request_url = cljs.core.get.call(null,map__31336__$1,new cljs.core.Keyword(null,"request-url","request-url",2100346596));

figwheel.client.utils.debug_prn.call(null,[cljs.core.str("FigWheel: Attempting to load "),cljs.core.str(request_url)].join(''));

return figwheel.client.file_reloading.reload_file_STAR_.call(null,request_url,((function (map__31336,map__31336__$1,file_msg,request_url){
return (function (success_QMARK_){
if(cljs.core.truth_(success_QMARK_)){
figwheel.client.utils.debug_prn.call(null,[cljs.core.str("FigWheel: Successfullly loaded "),cljs.core.str(request_url)].join(''));

return cljs.core.apply.call(null,callback,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.assoc.call(null,file_msg,new cljs.core.Keyword(null,"loaded-file","loaded-file",-168399375),true)], null));
} else {
figwheel.client.utils.log.call(null,new cljs.core.Keyword(null,"error","error",-978969032),[cljs.core.str("Figwheel: Error loading file "),cljs.core.str(request_url)].join(''));

return cljs.core.apply.call(null,callback,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [file_msg], null));
}
});})(map__31336,map__31336__$1,file_msg,request_url))
);
});
figwheel.client.file_reloading.reload_file_QMARK_ = (function figwheel$client$file_reloading$reload_file_QMARK_(p__31337){
var map__31339 = p__31337;
var map__31339__$1 = ((cljs.core.seq_QMARK_.call(null,map__31339))?cljs.core.apply.call(null,cljs.core.hash_map,map__31339):map__31339);
var file_msg = map__31339__$1;
var namespace = cljs.core.get.call(null,map__31339__$1,new cljs.core.Keyword(null,"namespace","namespace",-377510372));
var meta_data = cljs.core.get.call(null,map__31339__$1,new cljs.core.Keyword(null,"meta-data","meta-data",-1613399157));

var meta_data__$1 = (function (){var or__18073__auto__ = meta_data;
if(cljs.core.truth_(or__18073__auto__)){
return or__18073__auto__;
} else {
return cljs.core.PersistentArrayMap.EMPTY;
}
})();
var and__18061__auto__ = cljs.core.not.call(null,new cljs.core.Keyword(null,"figwheel-no-load","figwheel-no-load",-555840179).cljs$core$IFn$_invoke$arity$1(meta_data__$1));
if(and__18061__auto__){
var or__18073__auto__ = new cljs.core.Keyword(null,"figwheel-always","figwheel-always",799819691).cljs$core$IFn$_invoke$arity$1(meta_data__$1);
if(cljs.core.truth_(or__18073__auto__)){
return or__18073__auto__;
} else {
var or__18073__auto____$1 = new cljs.core.Keyword(null,"figwheel-load","figwheel-load",1316089175).cljs$core$IFn$_invoke$arity$1(meta_data__$1);
if(cljs.core.truth_(or__18073__auto____$1)){
return or__18073__auto____$1;
} else {
var and__18061__auto____$1 = cljs.core.contains_QMARK_.call(null,cljs.core._STAR_loaded_libs_STAR_,namespace);
if(and__18061__auto____$1){
var or__18073__auto____$2 = !(cljs.core.contains_QMARK_.call(null,meta_data__$1,new cljs.core.Keyword(null,"file-changed-on-disk","file-changed-on-disk",1086171932)));
if(or__18073__auto____$2){
return or__18073__auto____$2;
} else {
return new cljs.core.Keyword(null,"file-changed-on-disk","file-changed-on-disk",1086171932).cljs$core$IFn$_invoke$arity$1(meta_data__$1);
}
} else {
return and__18061__auto____$1;
}
}
}
} else {
return and__18061__auto__;
}
});
figwheel.client.file_reloading.js_reload = (function figwheel$client$file_reloading$js_reload(p__31340,callback){
var map__31342 = p__31340;
var map__31342__$1 = ((cljs.core.seq_QMARK_.call(null,map__31342))?cljs.core.apply.call(null,cljs.core.hash_map,map__31342):map__31342);
var file_msg = map__31342__$1;
var request_url = cljs.core.get.call(null,map__31342__$1,new cljs.core.Keyword(null,"request-url","request-url",2100346596));
var namespace = cljs.core.get.call(null,map__31342__$1,new cljs.core.Keyword(null,"namespace","namespace",-377510372));

if(cljs.core.truth_(figwheel.client.file_reloading.reload_file_QMARK_.call(null,file_msg))){
return figwheel.client.file_reloading.reload_file.call(null,file_msg,callback);
} else {
figwheel.client.utils.debug_prn.call(null,[cljs.core.str("Figwheel: Not trying to load file "),cljs.core.str(request_url)].join(''));

return cljs.core.apply.call(null,callback,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [file_msg], null));
}
});
figwheel.client.file_reloading.reload_js_file = (function figwheel$client$file_reloading$reload_js_file(file_msg){
var out = cljs.core.async.chan.call(null);
setTimeout(((function (out){
return (function (){
return figwheel.client.file_reloading.js_reload.call(null,file_msg,((function (out){
return (function (url){
figwheel.client.file_reloading.patch_goog_base.call(null);

cljs.core.async.put_BANG_.call(null,out,url);

return cljs.core.async.close_BANG_.call(null,out);
});})(out))
);
});})(out))
,(0));

return out;
});
/**
 * Returns a chanel with one collection of loaded filenames on it.
 */
figwheel.client.file_reloading.load_all_js_files = (function figwheel$client$file_reloading$load_all_js_files(files){
var out = cljs.core.async.chan.call(null);
var c__20932__auto___31429 = cljs.core.async.chan.call(null,(1));
cljs.core.async.impl.dispatch.run.call(null,((function (c__20932__auto___31429,out){
return (function (){
var f__20933__auto__ = (function (){var switch__20870__auto__ = ((function (c__20932__auto___31429,out){
return (function (state_31411){
var state_val_31412 = (state_31411[(1)]);
if((state_val_31412 === (1))){
var inst_31389 = cljs.core.nth.call(null,files,(0),null);
var inst_31390 = cljs.core.nthnext.call(null,files,(1));
var inst_31391 = files;
var state_31411__$1 = (function (){var statearr_31413 = state_31411;
(statearr_31413[(7)] = inst_31390);

(statearr_31413[(8)] = inst_31391);

(statearr_31413[(9)] = inst_31389);

return statearr_31413;
})();
var statearr_31414_31430 = state_31411__$1;
(statearr_31414_31430[(2)] = null);

(statearr_31414_31430[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_31412 === (2))){
var inst_31394 = (state_31411[(10)]);
var inst_31391 = (state_31411[(8)]);
var inst_31394__$1 = cljs.core.nth.call(null,inst_31391,(0),null);
var inst_31395 = cljs.core.nthnext.call(null,inst_31391,(1));
var inst_31396 = (inst_31394__$1 == null);
var inst_31397 = cljs.core.not.call(null,inst_31396);
var state_31411__$1 = (function (){var statearr_31415 = state_31411;
(statearr_31415[(10)] = inst_31394__$1);

(statearr_31415[(11)] = inst_31395);

return statearr_31415;
})();
if(inst_31397){
var statearr_31416_31431 = state_31411__$1;
(statearr_31416_31431[(1)] = (4));

} else {
var statearr_31417_31432 = state_31411__$1;
(statearr_31417_31432[(1)] = (5));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_31412 === (3))){
var inst_31409 = (state_31411[(2)]);
var state_31411__$1 = state_31411;
return cljs.core.async.impl.ioc_helpers.return_chan.call(null,state_31411__$1,inst_31409);
} else {
if((state_val_31412 === (4))){
var inst_31394 = (state_31411[(10)]);
var inst_31399 = figwheel.client.file_reloading.reload_js_file.call(null,inst_31394);
var state_31411__$1 = state_31411;
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_31411__$1,(7),inst_31399);
} else {
if((state_val_31412 === (5))){
var inst_31405 = cljs.core.async.close_BANG_.call(null,out);
var state_31411__$1 = state_31411;
var statearr_31418_31433 = state_31411__$1;
(statearr_31418_31433[(2)] = inst_31405);

(statearr_31418_31433[(1)] = (6));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_31412 === (6))){
var inst_31407 = (state_31411[(2)]);
var state_31411__$1 = state_31411;
var statearr_31419_31434 = state_31411__$1;
(statearr_31419_31434[(2)] = inst_31407);

(statearr_31419_31434[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_31412 === (7))){
var inst_31395 = (state_31411[(11)]);
var inst_31401 = (state_31411[(2)]);
var inst_31402 = cljs.core.async.put_BANG_.call(null,out,inst_31401);
var inst_31391 = inst_31395;
var state_31411__$1 = (function (){var statearr_31420 = state_31411;
(statearr_31420[(8)] = inst_31391);

(statearr_31420[(12)] = inst_31402);

return statearr_31420;
})();
var statearr_31421_31435 = state_31411__$1;
(statearr_31421_31435[(2)] = null);

(statearr_31421_31435[(1)] = (2));


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
});})(c__20932__auto___31429,out))
;
return ((function (switch__20870__auto__,c__20932__auto___31429,out){
return (function() {
var figwheel$client$file_reloading$load_all_js_files_$_state_machine__20871__auto__ = null;
var figwheel$client$file_reloading$load_all_js_files_$_state_machine__20871__auto____0 = (function (){
var statearr_31425 = [null,null,null,null,null,null,null,null,null,null,null,null,null];
(statearr_31425[(0)] = figwheel$client$file_reloading$load_all_js_files_$_state_machine__20871__auto__);

(statearr_31425[(1)] = (1));

return statearr_31425;
});
var figwheel$client$file_reloading$load_all_js_files_$_state_machine__20871__auto____1 = (function (state_31411){
while(true){
var ret_value__20872__auto__ = (function (){try{while(true){
var result__20873__auto__ = switch__20870__auto__.call(null,state_31411);
if(cljs.core.keyword_identical_QMARK_.call(null,result__20873__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__20873__auto__;
}
break;
}
}catch (e31426){if((e31426 instanceof Object)){
var ex__20874__auto__ = e31426;
var statearr_31427_31436 = state_31411;
(statearr_31427_31436[(5)] = ex__20874__auto__);


cljs.core.async.impl.ioc_helpers.process_exception.call(null,state_31411);

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
throw e31426;

}
}})();
if(cljs.core.keyword_identical_QMARK_.call(null,ret_value__20872__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__31437 = state_31411;
state_31411 = G__31437;
continue;
} else {
return ret_value__20872__auto__;
}
break;
}
});
figwheel$client$file_reloading$load_all_js_files_$_state_machine__20871__auto__ = function(state_31411){
switch(arguments.length){
case 0:
return figwheel$client$file_reloading$load_all_js_files_$_state_machine__20871__auto____0.call(this);
case 1:
return figwheel$client$file_reloading$load_all_js_files_$_state_machine__20871__auto____1.call(this,state_31411);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
figwheel$client$file_reloading$load_all_js_files_$_state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$0 = figwheel$client$file_reloading$load_all_js_files_$_state_machine__20871__auto____0;
figwheel$client$file_reloading$load_all_js_files_$_state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$1 = figwheel$client$file_reloading$load_all_js_files_$_state_machine__20871__auto____1;
return figwheel$client$file_reloading$load_all_js_files_$_state_machine__20871__auto__;
})()
;})(switch__20870__auto__,c__20932__auto___31429,out))
})();
var state__20934__auto__ = (function (){var statearr_31428 = f__20933__auto__.call(null);
(statearr_31428[cljs.core.async.impl.ioc_helpers.USER_START_IDX] = c__20932__auto___31429);

return statearr_31428;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped.call(null,state__20934__auto__);
});})(c__20932__auto___31429,out))
);


return cljs.core.async.into.call(null,cljs.core.PersistentVector.EMPTY,out);
});
figwheel.client.file_reloading.add_request_url = (function figwheel$client$file_reloading$add_request_url(p__31438,p__31439){
var map__31442 = p__31438;
var map__31442__$1 = ((cljs.core.seq_QMARK_.call(null,map__31442))?cljs.core.apply.call(null,cljs.core.hash_map,map__31442):map__31442);
var opts = map__31442__$1;
var url_rewriter = cljs.core.get.call(null,map__31442__$1,new cljs.core.Keyword(null,"url-rewriter","url-rewriter",200543838));
var map__31443 = p__31439;
var map__31443__$1 = ((cljs.core.seq_QMARK_.call(null,map__31443))?cljs.core.apply.call(null,cljs.core.hash_map,map__31443):map__31443);
var file_msg = map__31443__$1;
var file = cljs.core.get.call(null,map__31443__$1,new cljs.core.Keyword(null,"file","file",-1269645878));
var resolved_path = figwheel.client.file_reloading.resolve_url.call(null,file_msg);
return cljs.core.assoc.call(null,file_msg,new cljs.core.Keyword(null,"request-url","request-url",2100346596),(cljs.core.truth_(url_rewriter)?url_rewriter.call(null,resolved_path):resolved_path));
});
figwheel.client.file_reloading.add_request_urls = (function figwheel$client$file_reloading$add_request_urls(opts,files){
return cljs.core.map.call(null,cljs.core.partial.call(null,figwheel.client.file_reloading.add_request_url,opts),files);
});
figwheel.client.file_reloading.eval_body = (function figwheel$client$file_reloading$eval_body(p__31444){
var map__31447 = p__31444;
var map__31447__$1 = ((cljs.core.seq_QMARK_.call(null,map__31447))?cljs.core.apply.call(null,cljs.core.hash_map,map__31447):map__31447);
var eval_body__$1 = cljs.core.get.call(null,map__31447__$1,new cljs.core.Keyword(null,"eval-body","eval-body",-907279883));
var file = cljs.core.get.call(null,map__31447__$1,new cljs.core.Keyword(null,"file","file",-1269645878));
if(cljs.core.truth_((function (){var and__18061__auto__ = eval_body__$1;
if(cljs.core.truth_(and__18061__auto__)){
return typeof eval_body__$1 === 'string';
} else {
return and__18061__auto__;
}
})())){
var code = eval_body__$1;
try{figwheel.client.utils.debug_prn.call(null,[cljs.core.str("Evaling file "),cljs.core.str(file)].join(''));

return eval(code);
}catch (e31448){var e = e31448;
return figwheel.client.utils.log.call(null,new cljs.core.Keyword(null,"error","error",-978969032),[cljs.core.str("Unable to evaluate "),cljs.core.str(file)].join(''));
}} else {
return null;
}
});
figwheel.client.file_reloading.reload_js_files = (function figwheel$client$file_reloading$reload_js_files(p__31453,p__31454){
var map__31655 = p__31453;
var map__31655__$1 = ((cljs.core.seq_QMARK_.call(null,map__31655))?cljs.core.apply.call(null,cljs.core.hash_map,map__31655):map__31655);
var opts = map__31655__$1;
var before_jsload = cljs.core.get.call(null,map__31655__$1,new cljs.core.Keyword(null,"before-jsload","before-jsload",-847513128));
var on_jsload = cljs.core.get.call(null,map__31655__$1,new cljs.core.Keyword(null,"on-jsload","on-jsload",-395756602));
var load_unchanged_files = cljs.core.get.call(null,map__31655__$1,new cljs.core.Keyword(null,"load-unchanged-files","load-unchanged-files",-1561468704));
var map__31656 = p__31454;
var map__31656__$1 = ((cljs.core.seq_QMARK_.call(null,map__31656))?cljs.core.apply.call(null,cljs.core.hash_map,map__31656):map__31656);
var msg = map__31656__$1;
var files = cljs.core.get.call(null,map__31656__$1,new cljs.core.Keyword(null,"files","files",-472457450));
var c__20932__auto__ = cljs.core.async.chan.call(null,(1));
cljs.core.async.impl.dispatch.run.call(null,((function (c__20932__auto__,map__31655,map__31655__$1,opts,before_jsload,on_jsload,load_unchanged_files,map__31656,map__31656__$1,msg,files){
return (function (){
var f__20933__auto__ = (function (){var switch__20870__auto__ = ((function (c__20932__auto__,map__31655,map__31655__$1,opts,before_jsload,on_jsload,load_unchanged_files,map__31656,map__31656__$1,msg,files){
return (function (state_31780){
var state_val_31781 = (state_31780[(1)]);
if((state_val_31781 === (7))){
var inst_31668 = (state_31780[(7)]);
var inst_31669 = (state_31780[(8)]);
var inst_31667 = (state_31780[(9)]);
var inst_31670 = (state_31780[(10)]);
var inst_31675 = cljs.core._nth.call(null,inst_31668,inst_31670);
var inst_31676 = figwheel.client.file_reloading.eval_body.call(null,inst_31675);
var inst_31677 = (inst_31670 + (1));
var tmp31782 = inst_31668;
var tmp31783 = inst_31669;
var tmp31784 = inst_31667;
var inst_31667__$1 = tmp31784;
var inst_31668__$1 = tmp31782;
var inst_31669__$1 = tmp31783;
var inst_31670__$1 = inst_31677;
var state_31780__$1 = (function (){var statearr_31785 = state_31780;
(statearr_31785[(7)] = inst_31668__$1);

(statearr_31785[(8)] = inst_31669__$1);

(statearr_31785[(9)] = inst_31667__$1);

(statearr_31785[(10)] = inst_31670__$1);

(statearr_31785[(11)] = inst_31676);

return statearr_31785;
})();
var statearr_31786_31855 = state_31780__$1;
(statearr_31786_31855[(2)] = null);

(statearr_31786_31855[(1)] = (5));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_31781 === (20))){
var inst_31716 = (state_31780[(12)]);
var inst_31717 = (state_31780[(13)]);
var inst_31713 = (state_31780[(14)]);
var inst_31712 = (state_31780[(15)]);
var inst_31719 = (state_31780[(16)]);
var inst_31722 = figwheel.client.utils.log.call(null,new cljs.core.Keyword(null,"debug","debug",-1608172596),"Figwheel: loaded these files");
var inst_31724 = (function (){var all_files = inst_31712;
var files_SINGLEQUOTE_ = inst_31713;
var res_SINGLEQUOTE_ = inst_31716;
var res = inst_31717;
var files_not_loaded = inst_31719;
return ((function (all_files,files_SINGLEQUOTE_,res_SINGLEQUOTE_,res,files_not_loaded,inst_31716,inst_31717,inst_31713,inst_31712,inst_31719,inst_31722,state_val_31781,c__20932__auto__,map__31655,map__31655__$1,opts,before_jsload,on_jsload,load_unchanged_files,map__31656,map__31656__$1,msg,files){
return (function (p__31723){
var map__31787 = p__31723;
var map__31787__$1 = ((cljs.core.seq_QMARK_.call(null,map__31787))?cljs.core.apply.call(null,cljs.core.hash_map,map__31787):map__31787);
var namespace = cljs.core.get.call(null,map__31787__$1,new cljs.core.Keyword(null,"namespace","namespace",-377510372));
var file = cljs.core.get.call(null,map__31787__$1,new cljs.core.Keyword(null,"file","file",-1269645878));
if(cljs.core.truth_(namespace)){
return figwheel.client.file_reloading.ns_to_js_file.call(null,namespace);
} else {
return file;
}
});
;})(all_files,files_SINGLEQUOTE_,res_SINGLEQUOTE_,res,files_not_loaded,inst_31716,inst_31717,inst_31713,inst_31712,inst_31719,inst_31722,state_val_31781,c__20932__auto__,map__31655,map__31655__$1,opts,before_jsload,on_jsload,load_unchanged_files,map__31656,map__31656__$1,msg,files))
})();
var inst_31725 = cljs.core.map.call(null,inst_31724,inst_31717);
var inst_31726 = cljs.core.pr_str.call(null,inst_31725);
var inst_31727 = figwheel.client.utils.log.call(null,inst_31726);
var inst_31728 = (function (){var all_files = inst_31712;
var files_SINGLEQUOTE_ = inst_31713;
var res_SINGLEQUOTE_ = inst_31716;
var res = inst_31717;
var files_not_loaded = inst_31719;
return ((function (all_files,files_SINGLEQUOTE_,res_SINGLEQUOTE_,res,files_not_loaded,inst_31716,inst_31717,inst_31713,inst_31712,inst_31719,inst_31722,inst_31724,inst_31725,inst_31726,inst_31727,state_val_31781,c__20932__auto__,map__31655,map__31655__$1,opts,before_jsload,on_jsload,load_unchanged_files,map__31656,map__31656__$1,msg,files){
return (function (){
return cljs.core.apply.call(null,on_jsload,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [res], null));
});
;})(all_files,files_SINGLEQUOTE_,res_SINGLEQUOTE_,res,files_not_loaded,inst_31716,inst_31717,inst_31713,inst_31712,inst_31719,inst_31722,inst_31724,inst_31725,inst_31726,inst_31727,state_val_31781,c__20932__auto__,map__31655,map__31655__$1,opts,before_jsload,on_jsload,load_unchanged_files,map__31656,map__31656__$1,msg,files))
})();
var inst_31729 = setTimeout(inst_31728,(10));
var state_31780__$1 = (function (){var statearr_31788 = state_31780;
(statearr_31788[(17)] = inst_31722);

(statearr_31788[(18)] = inst_31727);

return statearr_31788;
})();
var statearr_31789_31856 = state_31780__$1;
(statearr_31789_31856[(2)] = inst_31729);

(statearr_31789_31856[(1)] = (22));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_31781 === (27))){
var inst_31739 = (state_31780[(19)]);
var state_31780__$1 = state_31780;
var statearr_31790_31857 = state_31780__$1;
(statearr_31790_31857[(2)] = inst_31739);

(statearr_31790_31857[(1)] = (28));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_31781 === (1))){
var inst_31659 = (state_31780[(20)]);
var inst_31657 = before_jsload.call(null,files);
var inst_31658 = (function (){return ((function (inst_31659,inst_31657,state_val_31781,c__20932__auto__,map__31655,map__31655__$1,opts,before_jsload,on_jsload,load_unchanged_files,map__31656,map__31656__$1,msg,files){
return (function (p1__31449_SHARP_){
return new cljs.core.Keyword(null,"eval-body","eval-body",-907279883).cljs$core$IFn$_invoke$arity$1(p1__31449_SHARP_);
});
;})(inst_31659,inst_31657,state_val_31781,c__20932__auto__,map__31655,map__31655__$1,opts,before_jsload,on_jsload,load_unchanged_files,map__31656,map__31656__$1,msg,files))
})();
var inst_31659__$1 = cljs.core.filter.call(null,inst_31658,files);
var inst_31660 = cljs.core.not_empty.call(null,inst_31659__$1);
var state_31780__$1 = (function (){var statearr_31791 = state_31780;
(statearr_31791[(21)] = inst_31657);

(statearr_31791[(20)] = inst_31659__$1);

return statearr_31791;
})();
if(cljs.core.truth_(inst_31660)){
var statearr_31792_31858 = state_31780__$1;
(statearr_31792_31858[(1)] = (2));

} else {
var statearr_31793_31859 = state_31780__$1;
(statearr_31793_31859[(1)] = (3));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_31781 === (24))){
var state_31780__$1 = state_31780;
var statearr_31794_31860 = state_31780__$1;
(statearr_31794_31860[(2)] = null);

(statearr_31794_31860[(1)] = (25));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_31781 === (4))){
var inst_31704 = (state_31780[(2)]);
var inst_31705 = (function (){return ((function (inst_31704,state_val_31781,c__20932__auto__,map__31655,map__31655__$1,opts,before_jsload,on_jsload,load_unchanged_files,map__31656,map__31656__$1,msg,files){
return (function (p1__31450_SHARP_){
var and__18061__auto__ = new cljs.core.Keyword(null,"namespace","namespace",-377510372).cljs$core$IFn$_invoke$arity$1(p1__31450_SHARP_);
if(cljs.core.truth_(and__18061__auto__)){
return cljs.core.not.call(null,new cljs.core.Keyword(null,"eval-body","eval-body",-907279883).cljs$core$IFn$_invoke$arity$1(p1__31450_SHARP_));
} else {
return and__18061__auto__;
}
});
;})(inst_31704,state_val_31781,c__20932__auto__,map__31655,map__31655__$1,opts,before_jsload,on_jsload,load_unchanged_files,map__31656,map__31656__$1,msg,files))
})();
var inst_31706 = cljs.core.filter.call(null,inst_31705,files);
var state_31780__$1 = (function (){var statearr_31795 = state_31780;
(statearr_31795[(22)] = inst_31704);

(statearr_31795[(23)] = inst_31706);

return statearr_31795;
})();
if(cljs.core.truth_(load_unchanged_files)){
var statearr_31796_31861 = state_31780__$1;
(statearr_31796_31861[(1)] = (16));

} else {
var statearr_31797_31862 = state_31780__$1;
(statearr_31797_31862[(1)] = (17));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_31781 === (15))){
var inst_31694 = (state_31780[(2)]);
var state_31780__$1 = state_31780;
var statearr_31798_31863 = state_31780__$1;
(statearr_31798_31863[(2)] = inst_31694);

(statearr_31798_31863[(1)] = (12));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_31781 === (21))){
var state_31780__$1 = state_31780;
var statearr_31799_31864 = state_31780__$1;
(statearr_31799_31864[(2)] = null);

(statearr_31799_31864[(1)] = (22));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_31781 === (31))){
var inst_31747 = (state_31780[(24)]);
var inst_31757 = (state_31780[(2)]);
var inst_31758 = cljs.core.not_empty.call(null,inst_31747);
var state_31780__$1 = (function (){var statearr_31800 = state_31780;
(statearr_31800[(25)] = inst_31757);

return statearr_31800;
})();
if(cljs.core.truth_(inst_31758)){
var statearr_31801_31865 = state_31780__$1;
(statearr_31801_31865[(1)] = (32));

} else {
var statearr_31802_31866 = state_31780__$1;
(statearr_31802_31866[(1)] = (33));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_31781 === (32))){
var inst_31747 = (state_31780[(24)]);
var inst_31760 = cljs.core.map.call(null,new cljs.core.Keyword(null,"file","file",-1269645878),inst_31747);
var inst_31761 = cljs.core.pr_str.call(null,inst_31760);
var inst_31762 = [cljs.core.str("file didn't change: "),cljs.core.str(inst_31761)].join('');
var inst_31763 = figwheel.client.utils.log.call(null,inst_31762);
var state_31780__$1 = state_31780;
var statearr_31803_31867 = state_31780__$1;
(statearr_31803_31867[(2)] = inst_31763);

(statearr_31803_31867[(1)] = (34));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_31781 === (33))){
var state_31780__$1 = state_31780;
var statearr_31804_31868 = state_31780__$1;
(statearr_31804_31868[(2)] = null);

(statearr_31804_31868[(1)] = (34));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_31781 === (13))){
var inst_31680 = (state_31780[(26)]);
var inst_31684 = cljs.core.chunk_first.call(null,inst_31680);
var inst_31685 = cljs.core.chunk_rest.call(null,inst_31680);
var inst_31686 = cljs.core.count.call(null,inst_31684);
var inst_31667 = inst_31685;
var inst_31668 = inst_31684;
var inst_31669 = inst_31686;
var inst_31670 = (0);
var state_31780__$1 = (function (){var statearr_31805 = state_31780;
(statearr_31805[(7)] = inst_31668);

(statearr_31805[(8)] = inst_31669);

(statearr_31805[(9)] = inst_31667);

(statearr_31805[(10)] = inst_31670);

return statearr_31805;
})();
var statearr_31806_31869 = state_31780__$1;
(statearr_31806_31869[(2)] = null);

(statearr_31806_31869[(1)] = (5));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_31781 === (22))){
var inst_31719 = (state_31780[(16)]);
var inst_31732 = (state_31780[(2)]);
var inst_31733 = cljs.core.not_empty.call(null,inst_31719);
var state_31780__$1 = (function (){var statearr_31807 = state_31780;
(statearr_31807[(27)] = inst_31732);

return statearr_31807;
})();
if(cljs.core.truth_(inst_31733)){
var statearr_31808_31870 = state_31780__$1;
(statearr_31808_31870[(1)] = (23));

} else {
var statearr_31809_31871 = state_31780__$1;
(statearr_31809_31871[(1)] = (24));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_31781 === (36))){
var state_31780__$1 = state_31780;
var statearr_31810_31872 = state_31780__$1;
(statearr_31810_31872[(2)] = null);

(statearr_31810_31872[(1)] = (37));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_31781 === (29))){
var inst_31746 = (state_31780[(28)]);
var inst_31751 = cljs.core.map.call(null,new cljs.core.Keyword(null,"file","file",-1269645878),inst_31746);
var inst_31752 = cljs.core.pr_str.call(null,inst_31751);
var inst_31753 = [cljs.core.str("figwheel-no-load meta-data: "),cljs.core.str(inst_31752)].join('');
var inst_31754 = figwheel.client.utils.log.call(null,inst_31753);
var state_31780__$1 = state_31780;
var statearr_31811_31873 = state_31780__$1;
(statearr_31811_31873[(2)] = inst_31754);

(statearr_31811_31873[(1)] = (31));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_31781 === (6))){
var inst_31701 = (state_31780[(2)]);
var state_31780__$1 = state_31780;
var statearr_31812_31874 = state_31780__$1;
(statearr_31812_31874[(2)] = inst_31701);

(statearr_31812_31874[(1)] = (4));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_31781 === (28))){
var inst_31746 = (state_31780[(28)]);
var inst_31745 = (state_31780[(2)]);
var inst_31746__$1 = cljs.core.get.call(null,inst_31745,new cljs.core.Keyword(null,"figwheel-no-load","figwheel-no-load",-555840179));
var inst_31747 = cljs.core.get.call(null,inst_31745,new cljs.core.Keyword(null,"file-changed-on-disk","file-changed-on-disk",1086171932));
var inst_31748 = cljs.core.get.call(null,inst_31745,new cljs.core.Keyword(null,"not-required","not-required",-950359114));
var inst_31749 = cljs.core.not_empty.call(null,inst_31746__$1);
var state_31780__$1 = (function (){var statearr_31813 = state_31780;
(statearr_31813[(29)] = inst_31748);

(statearr_31813[(28)] = inst_31746__$1);

(statearr_31813[(24)] = inst_31747);

return statearr_31813;
})();
if(cljs.core.truth_(inst_31749)){
var statearr_31814_31875 = state_31780__$1;
(statearr_31814_31875[(1)] = (29));

} else {
var statearr_31815_31876 = state_31780__$1;
(statearr_31815_31876[(1)] = (30));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_31781 === (25))){
var inst_31778 = (state_31780[(2)]);
var state_31780__$1 = state_31780;
return cljs.core.async.impl.ioc_helpers.return_chan.call(null,state_31780__$1,inst_31778);
} else {
if((state_val_31781 === (34))){
var inst_31748 = (state_31780[(29)]);
var inst_31766 = (state_31780[(2)]);
var inst_31767 = cljs.core.not_empty.call(null,inst_31748);
var state_31780__$1 = (function (){var statearr_31816 = state_31780;
(statearr_31816[(30)] = inst_31766);

return statearr_31816;
})();
if(cljs.core.truth_(inst_31767)){
var statearr_31817_31877 = state_31780__$1;
(statearr_31817_31877[(1)] = (35));

} else {
var statearr_31818_31878 = state_31780__$1;
(statearr_31818_31878[(1)] = (36));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_31781 === (17))){
var inst_31706 = (state_31780[(23)]);
var state_31780__$1 = state_31780;
var statearr_31819_31879 = state_31780__$1;
(statearr_31819_31879[(2)] = inst_31706);

(statearr_31819_31879[(1)] = (18));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_31781 === (3))){
var state_31780__$1 = state_31780;
var statearr_31820_31880 = state_31780__$1;
(statearr_31820_31880[(2)] = null);

(statearr_31820_31880[(1)] = (4));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_31781 === (12))){
var inst_31697 = (state_31780[(2)]);
var state_31780__$1 = state_31780;
var statearr_31821_31881 = state_31780__$1;
(statearr_31821_31881[(2)] = inst_31697);

(statearr_31821_31881[(1)] = (9));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_31781 === (2))){
var inst_31659 = (state_31780[(20)]);
var inst_31666 = cljs.core.seq.call(null,inst_31659);
var inst_31667 = inst_31666;
var inst_31668 = null;
var inst_31669 = (0);
var inst_31670 = (0);
var state_31780__$1 = (function (){var statearr_31822 = state_31780;
(statearr_31822[(7)] = inst_31668);

(statearr_31822[(8)] = inst_31669);

(statearr_31822[(9)] = inst_31667);

(statearr_31822[(10)] = inst_31670);

return statearr_31822;
})();
var statearr_31823_31882 = state_31780__$1;
(statearr_31823_31882[(2)] = null);

(statearr_31823_31882[(1)] = (5));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_31781 === (23))){
var inst_31716 = (state_31780[(12)]);
var inst_31717 = (state_31780[(13)]);
var inst_31739 = (state_31780[(19)]);
var inst_31713 = (state_31780[(14)]);
var inst_31712 = (state_31780[(15)]);
var inst_31719 = (state_31780[(16)]);
var inst_31735 = figwheel.client.utils.log.call(null,new cljs.core.Keyword(null,"debug","debug",-1608172596),"Figwheel: NOT loading these files ");
var inst_31738 = (function (){var all_files = inst_31712;
var files_SINGLEQUOTE_ = inst_31713;
var res_SINGLEQUOTE_ = inst_31716;
var res = inst_31717;
var files_not_loaded = inst_31719;
return ((function (all_files,files_SINGLEQUOTE_,res_SINGLEQUOTE_,res,files_not_loaded,inst_31716,inst_31717,inst_31739,inst_31713,inst_31712,inst_31719,inst_31735,state_val_31781,c__20932__auto__,map__31655,map__31655__$1,opts,before_jsload,on_jsload,load_unchanged_files,map__31656,map__31656__$1,msg,files){
return (function (p__31737){
var map__31824 = p__31737;
var map__31824__$1 = ((cljs.core.seq_QMARK_.call(null,map__31824))?cljs.core.apply.call(null,cljs.core.hash_map,map__31824):map__31824);
var meta_data = cljs.core.get.call(null,map__31824__$1,new cljs.core.Keyword(null,"meta-data","meta-data",-1613399157));
if((meta_data == null)){
return new cljs.core.Keyword(null,"not-required","not-required",-950359114);
} else {
if(cljs.core.contains_QMARK_.call(null,meta_data,new cljs.core.Keyword(null,"figwheel-no-load","figwheel-no-load",-555840179))){
return new cljs.core.Keyword(null,"figwheel-no-load","figwheel-no-load",-555840179);
} else {
if((cljs.core.contains_QMARK_.call(null,meta_data,new cljs.core.Keyword(null,"file-changed-on-disk","file-changed-on-disk",1086171932))) && (cljs.core.not.call(null,new cljs.core.Keyword(null,"file-changed-on-disk","file-changed-on-disk",1086171932).cljs$core$IFn$_invoke$arity$1(meta_data)))){
return new cljs.core.Keyword(null,"file-changed-on-disk","file-changed-on-disk",1086171932);
} else {
return new cljs.core.Keyword(null,"not-required","not-required",-950359114);

}
}
}
});
;})(all_files,files_SINGLEQUOTE_,res_SINGLEQUOTE_,res,files_not_loaded,inst_31716,inst_31717,inst_31739,inst_31713,inst_31712,inst_31719,inst_31735,state_val_31781,c__20932__auto__,map__31655,map__31655__$1,opts,before_jsload,on_jsload,load_unchanged_files,map__31656,map__31656__$1,msg,files))
})();
var inst_31739__$1 = cljs.core.group_by.call(null,inst_31738,inst_31719);
var inst_31740 = cljs.core.seq_QMARK_.call(null,inst_31739__$1);
var state_31780__$1 = (function (){var statearr_31825 = state_31780;
(statearr_31825[(31)] = inst_31735);

(statearr_31825[(19)] = inst_31739__$1);

return statearr_31825;
})();
if(inst_31740){
var statearr_31826_31883 = state_31780__$1;
(statearr_31826_31883[(1)] = (26));

} else {
var statearr_31827_31884 = state_31780__$1;
(statearr_31827_31884[(1)] = (27));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_31781 === (35))){
var inst_31748 = (state_31780[(29)]);
var inst_31769 = cljs.core.map.call(null,new cljs.core.Keyword(null,"file","file",-1269645878),inst_31748);
var inst_31770 = cljs.core.pr_str.call(null,inst_31769);
var inst_31771 = [cljs.core.str("not required: "),cljs.core.str(inst_31770)].join('');
var inst_31772 = figwheel.client.utils.log.call(null,inst_31771);
var state_31780__$1 = state_31780;
var statearr_31828_31885 = state_31780__$1;
(statearr_31828_31885[(2)] = inst_31772);

(statearr_31828_31885[(1)] = (37));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_31781 === (19))){
var inst_31716 = (state_31780[(12)]);
var inst_31717 = (state_31780[(13)]);
var inst_31713 = (state_31780[(14)]);
var inst_31712 = (state_31780[(15)]);
var inst_31716__$1 = (state_31780[(2)]);
var inst_31717__$1 = cljs.core.filter.call(null,new cljs.core.Keyword(null,"loaded-file","loaded-file",-168399375),inst_31716__$1);
var inst_31718 = (function (){var all_files = inst_31712;
var files_SINGLEQUOTE_ = inst_31713;
var res_SINGLEQUOTE_ = inst_31716__$1;
var res = inst_31717__$1;
return ((function (all_files,files_SINGLEQUOTE_,res_SINGLEQUOTE_,res,inst_31716,inst_31717,inst_31713,inst_31712,inst_31716__$1,inst_31717__$1,state_val_31781,c__20932__auto__,map__31655,map__31655__$1,opts,before_jsload,on_jsload,load_unchanged_files,map__31656,map__31656__$1,msg,files){
return (function (p1__31452_SHARP_){
return cljs.core.not.call(null,new cljs.core.Keyword(null,"loaded-file","loaded-file",-168399375).cljs$core$IFn$_invoke$arity$1(p1__31452_SHARP_));
});
;})(all_files,files_SINGLEQUOTE_,res_SINGLEQUOTE_,res,inst_31716,inst_31717,inst_31713,inst_31712,inst_31716__$1,inst_31717__$1,state_val_31781,c__20932__auto__,map__31655,map__31655__$1,opts,before_jsload,on_jsload,load_unchanged_files,map__31656,map__31656__$1,msg,files))
})();
var inst_31719 = cljs.core.filter.call(null,inst_31718,inst_31716__$1);
var inst_31720 = cljs.core.not_empty.call(null,inst_31717__$1);
var state_31780__$1 = (function (){var statearr_31829 = state_31780;
(statearr_31829[(12)] = inst_31716__$1);

(statearr_31829[(13)] = inst_31717__$1);

(statearr_31829[(16)] = inst_31719);

return statearr_31829;
})();
if(cljs.core.truth_(inst_31720)){
var statearr_31830_31886 = state_31780__$1;
(statearr_31830_31886[(1)] = (20));

} else {
var statearr_31831_31887 = state_31780__$1;
(statearr_31831_31887[(1)] = (21));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_31781 === (11))){
var state_31780__$1 = state_31780;
var statearr_31832_31888 = state_31780__$1;
(statearr_31832_31888[(2)] = null);

(statearr_31832_31888[(1)] = (12));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_31781 === (9))){
var inst_31699 = (state_31780[(2)]);
var state_31780__$1 = state_31780;
var statearr_31833_31889 = state_31780__$1;
(statearr_31833_31889[(2)] = inst_31699);

(statearr_31833_31889[(1)] = (6));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_31781 === (5))){
var inst_31669 = (state_31780[(8)]);
var inst_31670 = (state_31780[(10)]);
var inst_31672 = (inst_31670 < inst_31669);
var inst_31673 = inst_31672;
var state_31780__$1 = state_31780;
if(cljs.core.truth_(inst_31673)){
var statearr_31834_31890 = state_31780__$1;
(statearr_31834_31890[(1)] = (7));

} else {
var statearr_31835_31891 = state_31780__$1;
(statearr_31835_31891[(1)] = (8));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_31781 === (14))){
var inst_31680 = (state_31780[(26)]);
var inst_31689 = cljs.core.first.call(null,inst_31680);
var inst_31690 = figwheel.client.file_reloading.eval_body.call(null,inst_31689);
var inst_31691 = cljs.core.next.call(null,inst_31680);
var inst_31667 = inst_31691;
var inst_31668 = null;
var inst_31669 = (0);
var inst_31670 = (0);
var state_31780__$1 = (function (){var statearr_31836 = state_31780;
(statearr_31836[(7)] = inst_31668);

(statearr_31836[(32)] = inst_31690);

(statearr_31836[(8)] = inst_31669);

(statearr_31836[(9)] = inst_31667);

(statearr_31836[(10)] = inst_31670);

return statearr_31836;
})();
var statearr_31837_31892 = state_31780__$1;
(statearr_31837_31892[(2)] = null);

(statearr_31837_31892[(1)] = (5));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_31781 === (26))){
var inst_31739 = (state_31780[(19)]);
var inst_31742 = cljs.core.apply.call(null,cljs.core.hash_map,inst_31739);
var state_31780__$1 = state_31780;
var statearr_31838_31893 = state_31780__$1;
(statearr_31838_31893[(2)] = inst_31742);

(statearr_31838_31893[(1)] = (28));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_31781 === (16))){
var inst_31706 = (state_31780[(23)]);
var inst_31708 = (function (){var all_files = inst_31706;
return ((function (all_files,inst_31706,state_val_31781,c__20932__auto__,map__31655,map__31655__$1,opts,before_jsload,on_jsload,load_unchanged_files,map__31656,map__31656__$1,msg,files){
return (function (p1__31451_SHARP_){
return cljs.core.update_in.call(null,p1__31451_SHARP_,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"meta-data","meta-data",-1613399157)], null),cljs.core.dissoc,new cljs.core.Keyword(null,"file-changed-on-disk","file-changed-on-disk",1086171932));
});
;})(all_files,inst_31706,state_val_31781,c__20932__auto__,map__31655,map__31655__$1,opts,before_jsload,on_jsload,load_unchanged_files,map__31656,map__31656__$1,msg,files))
})();
var inst_31709 = cljs.core.map.call(null,inst_31708,inst_31706);
var state_31780__$1 = state_31780;
var statearr_31839_31894 = state_31780__$1;
(statearr_31839_31894[(2)] = inst_31709);

(statearr_31839_31894[(1)] = (18));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_31781 === (30))){
var state_31780__$1 = state_31780;
var statearr_31840_31895 = state_31780__$1;
(statearr_31840_31895[(2)] = null);

(statearr_31840_31895[(1)] = (31));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_31781 === (10))){
var inst_31680 = (state_31780[(26)]);
var inst_31682 = cljs.core.chunked_seq_QMARK_.call(null,inst_31680);
var state_31780__$1 = state_31780;
if(inst_31682){
var statearr_31841_31896 = state_31780__$1;
(statearr_31841_31896[(1)] = (13));

} else {
var statearr_31842_31897 = state_31780__$1;
(statearr_31842_31897[(1)] = (14));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_31781 === (18))){
var inst_31713 = (state_31780[(14)]);
var inst_31712 = (state_31780[(15)]);
var inst_31712__$1 = (state_31780[(2)]);
var inst_31713__$1 = figwheel.client.file_reloading.add_request_urls.call(null,opts,inst_31712__$1);
var inst_31714 = figwheel.client.file_reloading.load_all_js_files.call(null,inst_31713__$1);
var state_31780__$1 = (function (){var statearr_31843 = state_31780;
(statearr_31843[(14)] = inst_31713__$1);

(statearr_31843[(15)] = inst_31712__$1);

return statearr_31843;
})();
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_31780__$1,(19),inst_31714);
} else {
if((state_val_31781 === (37))){
var inst_31775 = (state_31780[(2)]);
var state_31780__$1 = state_31780;
var statearr_31844_31898 = state_31780__$1;
(statearr_31844_31898[(2)] = inst_31775);

(statearr_31844_31898[(1)] = (25));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_31781 === (8))){
var inst_31667 = (state_31780[(9)]);
var inst_31680 = (state_31780[(26)]);
var inst_31680__$1 = cljs.core.seq.call(null,inst_31667);
var state_31780__$1 = (function (){var statearr_31845 = state_31780;
(statearr_31845[(26)] = inst_31680__$1);

return statearr_31845;
})();
if(inst_31680__$1){
var statearr_31846_31899 = state_31780__$1;
(statearr_31846_31899[(1)] = (10));

} else {
var statearr_31847_31900 = state_31780__$1;
(statearr_31847_31900[(1)] = (11));

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
});})(c__20932__auto__,map__31655,map__31655__$1,opts,before_jsload,on_jsload,load_unchanged_files,map__31656,map__31656__$1,msg,files))
;
return ((function (switch__20870__auto__,c__20932__auto__,map__31655,map__31655__$1,opts,before_jsload,on_jsload,load_unchanged_files,map__31656,map__31656__$1,msg,files){
return (function() {
var figwheel$client$file_reloading$reload_js_files_$_state_machine__20871__auto__ = null;
var figwheel$client$file_reloading$reload_js_files_$_state_machine__20871__auto____0 = (function (){
var statearr_31851 = [null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null];
(statearr_31851[(0)] = figwheel$client$file_reloading$reload_js_files_$_state_machine__20871__auto__);

(statearr_31851[(1)] = (1));

return statearr_31851;
});
var figwheel$client$file_reloading$reload_js_files_$_state_machine__20871__auto____1 = (function (state_31780){
while(true){
var ret_value__20872__auto__ = (function (){try{while(true){
var result__20873__auto__ = switch__20870__auto__.call(null,state_31780);
if(cljs.core.keyword_identical_QMARK_.call(null,result__20873__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__20873__auto__;
}
break;
}
}catch (e31852){if((e31852 instanceof Object)){
var ex__20874__auto__ = e31852;
var statearr_31853_31901 = state_31780;
(statearr_31853_31901[(5)] = ex__20874__auto__);


cljs.core.async.impl.ioc_helpers.process_exception.call(null,state_31780);

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
throw e31852;

}
}})();
if(cljs.core.keyword_identical_QMARK_.call(null,ret_value__20872__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__31902 = state_31780;
state_31780 = G__31902;
continue;
} else {
return ret_value__20872__auto__;
}
break;
}
});
figwheel$client$file_reloading$reload_js_files_$_state_machine__20871__auto__ = function(state_31780){
switch(arguments.length){
case 0:
return figwheel$client$file_reloading$reload_js_files_$_state_machine__20871__auto____0.call(this);
case 1:
return figwheel$client$file_reloading$reload_js_files_$_state_machine__20871__auto____1.call(this,state_31780);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
figwheel$client$file_reloading$reload_js_files_$_state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$0 = figwheel$client$file_reloading$reload_js_files_$_state_machine__20871__auto____0;
figwheel$client$file_reloading$reload_js_files_$_state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$1 = figwheel$client$file_reloading$reload_js_files_$_state_machine__20871__auto____1;
return figwheel$client$file_reloading$reload_js_files_$_state_machine__20871__auto__;
})()
;})(switch__20870__auto__,c__20932__auto__,map__31655,map__31655__$1,opts,before_jsload,on_jsload,load_unchanged_files,map__31656,map__31656__$1,msg,files))
})();
var state__20934__auto__ = (function (){var statearr_31854 = f__20933__auto__.call(null);
(statearr_31854[cljs.core.async.impl.ioc_helpers.USER_START_IDX] = c__20932__auto__);

return statearr_31854;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped.call(null,state__20934__auto__);
});})(c__20932__auto__,map__31655,map__31655__$1,opts,before_jsload,on_jsload,load_unchanged_files,map__31656,map__31656__$1,msg,files))
);

return c__20932__auto__;
});
figwheel.client.file_reloading.current_links = (function figwheel$client$file_reloading$current_links(){
return Array.prototype.slice.call(document.getElementsByTagName("link"));
});
figwheel.client.file_reloading.truncate_url = (function figwheel$client$file_reloading$truncate_url(url){
return clojure.string.replace_first.call(null,clojure.string.replace_first.call(null,clojure.string.replace_first.call(null,clojure.string.replace_first.call(null,cljs.core.first.call(null,clojure.string.split.call(null,url,/\?/)),[cljs.core.str(location.protocol),cljs.core.str("//")].join(''),""),".*://",""),/^\/\//,""),/[^\\/]*/,"");
});
figwheel.client.file_reloading.matches_file_QMARK_ = (function figwheel$client$file_reloading$matches_file_QMARK_(p__31905,link){
var map__31907 = p__31905;
var map__31907__$1 = ((cljs.core.seq_QMARK_.call(null,map__31907))?cljs.core.apply.call(null,cljs.core.hash_map,map__31907):map__31907);
var file = cljs.core.get.call(null,map__31907__$1,new cljs.core.Keyword(null,"file","file",-1269645878));
var temp__4423__auto__ = link.href;
if(cljs.core.truth_(temp__4423__auto__)){
var link_href = temp__4423__auto__;
var match = clojure.string.join.call(null,"/",cljs.core.take_while.call(null,cljs.core.identity,cljs.core.map.call(null,((function (link_href,temp__4423__auto__,map__31907,map__31907__$1,file){
return (function (p1__31903_SHARP_,p2__31904_SHARP_){
if(cljs.core._EQ_.call(null,p1__31903_SHARP_,p2__31904_SHARP_)){
return p1__31903_SHARP_;
} else {
return false;
}
});})(link_href,temp__4423__auto__,map__31907,map__31907__$1,file))
,cljs.core.reverse.call(null,clojure.string.split.call(null,file,"/")),cljs.core.reverse.call(null,clojure.string.split.call(null,figwheel.client.file_reloading.truncate_url.call(null,link_href),"/")))));
var match_length = cljs.core.count.call(null,match);
var file_name_length = cljs.core.count.call(null,cljs.core.last.call(null,clojure.string.split.call(null,file,"/")));
if((match_length >= file_name_length)){
return new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"link","link",-1769163468),link,new cljs.core.Keyword(null,"link-href","link-href",-250644450),link_href,new cljs.core.Keyword(null,"match-length","match-length",1101537310),match_length,new cljs.core.Keyword(null,"current-url-length","current-url-length",380404083),cljs.core.count.call(null,figwheel.client.file_reloading.truncate_url.call(null,link_href))], null);
} else {
return null;
}
} else {
return null;
}
});
figwheel.client.file_reloading.get_correct_link = (function figwheel$client$file_reloading$get_correct_link(f_data){
var temp__4423__auto__ = cljs.core.first.call(null,cljs.core.sort_by.call(null,(function (p__31911){
var map__31912 = p__31911;
var map__31912__$1 = ((cljs.core.seq_QMARK_.call(null,map__31912))?cljs.core.apply.call(null,cljs.core.hash_map,map__31912):map__31912);
var match_length = cljs.core.get.call(null,map__31912__$1,new cljs.core.Keyword(null,"match-length","match-length",1101537310));
var current_url_length = cljs.core.get.call(null,map__31912__$1,new cljs.core.Keyword(null,"current-url-length","current-url-length",380404083));
return (current_url_length - match_length);
}),cljs.core.keep.call(null,(function (p1__31908_SHARP_){
return figwheel.client.file_reloading.matches_file_QMARK_.call(null,f_data,p1__31908_SHARP_);
}),figwheel.client.file_reloading.current_links.call(null))));
if(cljs.core.truth_(temp__4423__auto__)){
var res = temp__4423__auto__;
return new cljs.core.Keyword(null,"link","link",-1769163468).cljs$core$IFn$_invoke$arity$1(res);
} else {
return null;
}
});
figwheel.client.file_reloading.clone_link = (function figwheel$client$file_reloading$clone_link(link,url){
var clone = document.createElement("link");
clone.rel = "stylesheet";

clone.media = link.media;

clone.disabled = link.disabled;

clone.href = figwheel.client.file_reloading.add_cache_buster.call(null,url);

return clone;
});
figwheel.client.file_reloading.create_link = (function figwheel$client$file_reloading$create_link(url){
var link = document.createElement("link");
link.rel = "stylesheet";

link.href = figwheel.client.file_reloading.add_cache_buster.call(null,url);

return link;
});
figwheel.client.file_reloading.add_link_to_doc = (function figwheel$client$file_reloading$add_link_to_doc(){
var G__31914 = arguments.length;
switch (G__31914) {
case 1:
return figwheel.client.file_reloading.add_link_to_doc.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 2:
return figwheel.client.file_reloading.add_link_to_doc.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

figwheel.client.file_reloading.add_link_to_doc.cljs$core$IFn$_invoke$arity$1 = (function (new_link){
return (document.getElementsByTagName("head")[(0)]).appendChild(new_link);
});

figwheel.client.file_reloading.add_link_to_doc.cljs$core$IFn$_invoke$arity$2 = (function (orig_link,klone){
var parent = orig_link.parentNode;
if(cljs.core._EQ_.call(null,orig_link,parent.lastChild)){
parent.appendChild(klone);
} else {
parent.insertBefore(klone,orig_link.nextSibling);
}

return setTimeout(((function (parent){
return (function (){
return parent.removeChild(orig_link);
});})(parent))
,(300));
});

figwheel.client.file_reloading.add_link_to_doc.cljs$lang$maxFixedArity = 2;
figwheel.client.file_reloading.reload_css_file = (function figwheel$client$file_reloading$reload_css_file(p__31916){
var map__31918 = p__31916;
var map__31918__$1 = ((cljs.core.seq_QMARK_.call(null,map__31918))?cljs.core.apply.call(null,cljs.core.hash_map,map__31918):map__31918);
var f_data = map__31918__$1;
var file = cljs.core.get.call(null,map__31918__$1,new cljs.core.Keyword(null,"file","file",-1269645878));
var request_url = cljs.core.get.call(null,map__31918__$1,new cljs.core.Keyword(null,"request-url","request-url",2100346596));
var temp__4421__auto__ = figwheel.client.file_reloading.get_correct_link.call(null,f_data);
if(cljs.core.truth_(temp__4421__auto__)){
var link = temp__4421__auto__;
return figwheel.client.file_reloading.add_link_to_doc.call(null,link,figwheel.client.file_reloading.clone_link.call(null,link,link.href));
} else {
return figwheel.client.file_reloading.add_link_to_doc.call(null,figwheel.client.file_reloading.create_link.call(null,(function (){var or__18073__auto__ = request_url;
if(cljs.core.truth_(or__18073__auto__)){
return or__18073__auto__;
} else {
return file;
}
})()));
}
});
figwheel.client.file_reloading.reload_css_files = (function figwheel$client$file_reloading$reload_css_files(p__31919,files_msg){
var map__31941 = p__31919;
var map__31941__$1 = ((cljs.core.seq_QMARK_.call(null,map__31941))?cljs.core.apply.call(null,cljs.core.hash_map,map__31941):map__31941);
var opts = map__31941__$1;
var on_cssload = cljs.core.get.call(null,map__31941__$1,new cljs.core.Keyword(null,"on-cssload","on-cssload",1825432318));
if(cljs.core.truth_(figwheel.client.utils.html_env_QMARK_.call(null))){
var seq__31942_31962 = cljs.core.seq.call(null,figwheel.client.file_reloading.add_request_urls.call(null,opts,new cljs.core.Keyword(null,"files","files",-472457450).cljs$core$IFn$_invoke$arity$1(files_msg)));
var chunk__31943_31963 = null;
var count__31944_31964 = (0);
var i__31945_31965 = (0);
while(true){
if((i__31945_31965 < count__31944_31964)){
var f_31966 = cljs.core._nth.call(null,chunk__31943_31963,i__31945_31965);
figwheel.client.file_reloading.reload_css_file.call(null,f_31966);

var G__31967 = seq__31942_31962;
var G__31968 = chunk__31943_31963;
var G__31969 = count__31944_31964;
var G__31970 = (i__31945_31965 + (1));
seq__31942_31962 = G__31967;
chunk__31943_31963 = G__31968;
count__31944_31964 = G__31969;
i__31945_31965 = G__31970;
continue;
} else {
var temp__4423__auto___31971 = cljs.core.seq.call(null,seq__31942_31962);
if(temp__4423__auto___31971){
var seq__31942_31972__$1 = temp__4423__auto___31971;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__31942_31972__$1)){
var c__18858__auto___31973 = cljs.core.chunk_first.call(null,seq__31942_31972__$1);
var G__31974 = cljs.core.chunk_rest.call(null,seq__31942_31972__$1);
var G__31975 = c__18858__auto___31973;
var G__31976 = cljs.core.count.call(null,c__18858__auto___31973);
var G__31977 = (0);
seq__31942_31962 = G__31974;
chunk__31943_31963 = G__31975;
count__31944_31964 = G__31976;
i__31945_31965 = G__31977;
continue;
} else {
var f_31978 = cljs.core.first.call(null,seq__31942_31972__$1);
figwheel.client.file_reloading.reload_css_file.call(null,f_31978);

var G__31979 = cljs.core.next.call(null,seq__31942_31972__$1);
var G__31980 = null;
var G__31981 = (0);
var G__31982 = (0);
seq__31942_31962 = G__31979;
chunk__31943_31963 = G__31980;
count__31944_31964 = G__31981;
i__31945_31965 = G__31982;
continue;
}
} else {
}
}
break;
}

var c__20932__auto__ = cljs.core.async.chan.call(null,(1));
cljs.core.async.impl.dispatch.run.call(null,((function (c__20932__auto__,map__31941,map__31941__$1,opts,on_cssload){
return (function (){
var f__20933__auto__ = (function (){var switch__20870__auto__ = ((function (c__20932__auto__,map__31941,map__31941__$1,opts,on_cssload){
return (function (state_31952){
var state_val_31953 = (state_31952[(1)]);
if((state_val_31953 === (1))){
var inst_31946 = cljs.core.async.timeout.call(null,(100));
var state_31952__$1 = state_31952;
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_31952__$1,(2),inst_31946);
} else {
if((state_val_31953 === (2))){
var inst_31948 = (state_31952[(2)]);
var inst_31949 = new cljs.core.Keyword(null,"files","files",-472457450).cljs$core$IFn$_invoke$arity$1(files_msg);
var inst_31950 = on_cssload.call(null,inst_31949);
var state_31952__$1 = (function (){var statearr_31954 = state_31952;
(statearr_31954[(7)] = inst_31948);

return statearr_31954;
})();
return cljs.core.async.impl.ioc_helpers.return_chan.call(null,state_31952__$1,inst_31950);
} else {
return null;
}
}
});})(c__20932__auto__,map__31941,map__31941__$1,opts,on_cssload))
;
return ((function (switch__20870__auto__,c__20932__auto__,map__31941,map__31941__$1,opts,on_cssload){
return (function() {
var figwheel$client$file_reloading$reload_css_files_$_state_machine__20871__auto__ = null;
var figwheel$client$file_reloading$reload_css_files_$_state_machine__20871__auto____0 = (function (){
var statearr_31958 = [null,null,null,null,null,null,null,null];
(statearr_31958[(0)] = figwheel$client$file_reloading$reload_css_files_$_state_machine__20871__auto__);

(statearr_31958[(1)] = (1));

return statearr_31958;
});
var figwheel$client$file_reloading$reload_css_files_$_state_machine__20871__auto____1 = (function (state_31952){
while(true){
var ret_value__20872__auto__ = (function (){try{while(true){
var result__20873__auto__ = switch__20870__auto__.call(null,state_31952);
if(cljs.core.keyword_identical_QMARK_.call(null,result__20873__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__20873__auto__;
}
break;
}
}catch (e31959){if((e31959 instanceof Object)){
var ex__20874__auto__ = e31959;
var statearr_31960_31983 = state_31952;
(statearr_31960_31983[(5)] = ex__20874__auto__);


cljs.core.async.impl.ioc_helpers.process_exception.call(null,state_31952);

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
throw e31959;

}
}})();
if(cljs.core.keyword_identical_QMARK_.call(null,ret_value__20872__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__31984 = state_31952;
state_31952 = G__31984;
continue;
} else {
return ret_value__20872__auto__;
}
break;
}
});
figwheel$client$file_reloading$reload_css_files_$_state_machine__20871__auto__ = function(state_31952){
switch(arguments.length){
case 0:
return figwheel$client$file_reloading$reload_css_files_$_state_machine__20871__auto____0.call(this);
case 1:
return figwheel$client$file_reloading$reload_css_files_$_state_machine__20871__auto____1.call(this,state_31952);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
figwheel$client$file_reloading$reload_css_files_$_state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$0 = figwheel$client$file_reloading$reload_css_files_$_state_machine__20871__auto____0;
figwheel$client$file_reloading$reload_css_files_$_state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$1 = figwheel$client$file_reloading$reload_css_files_$_state_machine__20871__auto____1;
return figwheel$client$file_reloading$reload_css_files_$_state_machine__20871__auto__;
})()
;})(switch__20870__auto__,c__20932__auto__,map__31941,map__31941__$1,opts,on_cssload))
})();
var state__20934__auto__ = (function (){var statearr_31961 = f__20933__auto__.call(null);
(statearr_31961[cljs.core.async.impl.ioc_helpers.USER_START_IDX] = c__20932__auto__);

return statearr_31961;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped.call(null,state__20934__auto__);
});})(c__20932__auto__,map__31941,map__31941__$1,opts,on_cssload))
);

return c__20932__auto__;
} else {
return null;
}
});

//# sourceMappingURL=file_reloading.js.map?rel=1431620937118