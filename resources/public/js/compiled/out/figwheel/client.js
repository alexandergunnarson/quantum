// Compiled by ClojureScript 0.0-3269 {}
goog.provide('figwheel.client');
goog.require('cljs.core');
goog.require('goog.Uri');
goog.require('cljs.core.async');
goog.require('figwheel.client.socket');
goog.require('figwheel.client.file_reloading');
goog.require('clojure.string');
goog.require('figwheel.client.utils');
goog.require('cljs.repl');
goog.require('figwheel.client.heads_up');
figwheel.client.figwheel_repl_print = (function figwheel$client$figwheel_repl_print(args){
figwheel.client.socket.send_BANG_.call(null,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"figwheel-event","figwheel-event",519570592),"callback",new cljs.core.Keyword(null,"callback-name","callback-name",336964714),"figwheel-repl-print",new cljs.core.Keyword(null,"content","content",15833224),args], null));

return args;
});
figwheel.client.console_print = (function figwheel$client$console_print(args){
console.log.apply(console,cljs.core.into_array.call(null,args));

return args;
});
figwheel.client.enable_repl_print_BANG_ = (function figwheel$client$enable_repl_print_BANG_(){
cljs.core._STAR_print_newline_STAR_ = false;

return cljs.core._STAR_print_fn_STAR_ = (function() { 
var G__30251__delegate = function (args){
return figwheel.client.figwheel_repl_print.call(null,figwheel.client.console_print.call(null,args));
};
var G__30251 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__30252__i = 0, G__30252__a = new Array(arguments.length -  0);
while (G__30252__i < G__30252__a.length) {G__30252__a[G__30252__i] = arguments[G__30252__i + 0]; ++G__30252__i;}
  args = new cljs.core.IndexedSeq(G__30252__a,0);
} 
return G__30251__delegate.call(this,args);};
G__30251.cljs$lang$maxFixedArity = 0;
G__30251.cljs$lang$applyTo = (function (arglist__30253){
var args = cljs.core.seq(arglist__30253);
return G__30251__delegate(args);
});
G__30251.cljs$core$IFn$_invoke$arity$variadic = G__30251__delegate;
return G__30251;
})()
;
});
figwheel.client.get_essential_messages = (function figwheel$client$get_essential_messages(ed){
if(cljs.core.truth_(ed)){
return cljs.core.cons.call(null,cljs.core.select_keys.call(null,ed,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"message","message",-406056002),new cljs.core.Keyword(null,"class","class",-2030961996)], null)),figwheel$client$get_essential_messages.call(null,new cljs.core.Keyword(null,"cause","cause",231901252).cljs$core$IFn$_invoke$arity$1(ed)));
} else {
return null;
}
});
figwheel.client.error_msg_format = (function figwheel$client$error_msg_format(p__30254){
var map__30256 = p__30254;
var map__30256__$1 = ((cljs.core.seq_QMARK_.call(null,map__30256))?cljs.core.apply.call(null,cljs.core.hash_map,map__30256):map__30256);
var message = cljs.core.get.call(null,map__30256__$1,new cljs.core.Keyword(null,"message","message",-406056002));
var class$ = cljs.core.get.call(null,map__30256__$1,new cljs.core.Keyword(null,"class","class",-2030961996));
return [cljs.core.str(class$),cljs.core.str(" : "),cljs.core.str(message)].join('');
});
figwheel.client.format_messages = cljs.core.comp.call(null,cljs.core.partial.call(null,cljs.core.map,figwheel.client.error_msg_format),figwheel.client.get_essential_messages);
figwheel.client.focus_msgs = (function figwheel$client$focus_msgs(name_set,msg_hist){
return cljs.core.cons.call(null,cljs.core.first.call(null,msg_hist),cljs.core.filter.call(null,cljs.core.comp.call(null,name_set,new cljs.core.Keyword(null,"msg-name","msg-name",-353709863)),cljs.core.rest.call(null,msg_hist)));
});
figwheel.client.reload_file_QMARK__STAR_ = (function figwheel$client$reload_file_QMARK__STAR_(msg_name,opts){
var or__18073__auto__ = new cljs.core.Keyword(null,"load-warninged-code","load-warninged-code",-2030345223).cljs$core$IFn$_invoke$arity$1(opts);
if(cljs.core.truth_(or__18073__auto__)){
return or__18073__auto__;
} else {
return cljs.core.not_EQ_.call(null,msg_name,new cljs.core.Keyword(null,"compile-warning","compile-warning",43425356));
}
});
figwheel.client.reload_file_state_QMARK_ = (function figwheel$client$reload_file_state_QMARK_(msg_names,opts){
var and__18061__auto__ = cljs.core._EQ_.call(null,cljs.core.first.call(null,msg_names),new cljs.core.Keyword(null,"files-changed","files-changed",-1418200563));
if(and__18061__auto__){
return figwheel.client.reload_file_QMARK__STAR_.call(null,cljs.core.second.call(null,msg_names),opts);
} else {
return and__18061__auto__;
}
});
figwheel.client.block_reload_file_state_QMARK_ = (function figwheel$client$block_reload_file_state_QMARK_(msg_names,opts){
return (cljs.core._EQ_.call(null,cljs.core.first.call(null,msg_names),new cljs.core.Keyword(null,"files-changed","files-changed",-1418200563))) && (cljs.core.not.call(null,figwheel.client.reload_file_QMARK__STAR_.call(null,cljs.core.second.call(null,msg_names),opts)));
});
figwheel.client.warning_append_state_QMARK_ = (function figwheel$client$warning_append_state_QMARK_(msg_names){
return cljs.core._EQ_.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"compile-warning","compile-warning",43425356),new cljs.core.Keyword(null,"compile-warning","compile-warning",43425356)], null),cljs.core.take.call(null,(2),msg_names));
});
figwheel.client.warning_state_QMARK_ = (function figwheel$client$warning_state_QMARK_(msg_names){
return cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"compile-warning","compile-warning",43425356),cljs.core.first.call(null,msg_names));
});
figwheel.client.rewarning_state_QMARK_ = (function figwheel$client$rewarning_state_QMARK_(msg_names){
return cljs.core._EQ_.call(null,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"compile-warning","compile-warning",43425356),new cljs.core.Keyword(null,"files-changed","files-changed",-1418200563),new cljs.core.Keyword(null,"compile-warning","compile-warning",43425356)], null),cljs.core.take.call(null,(3),msg_names));
});
figwheel.client.compile_fail_state_QMARK_ = (function figwheel$client$compile_fail_state_QMARK_(msg_names){
return cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"compile-failed","compile-failed",-477639289),cljs.core.first.call(null,msg_names));
});
figwheel.client.compile_refail_state_QMARK_ = (function figwheel$client$compile_refail_state_QMARK_(msg_names){
return cljs.core._EQ_.call(null,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"compile-failed","compile-failed",-477639289),new cljs.core.Keyword(null,"compile-failed","compile-failed",-477639289)], null),cljs.core.take.call(null,(2),msg_names));
});
figwheel.client.css_loaded_state_QMARK_ = (function figwheel$client$css_loaded_state_QMARK_(msg_names){
return cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"css-files-changed","css-files-changed",720773874),cljs.core.first.call(null,msg_names));
});
figwheel.client.file_reloader_plugin = (function figwheel$client$file_reloader_plugin(opts){
var ch = cljs.core.async.chan.call(null);
var c__20932__auto___30385 = cljs.core.async.chan.call(null,(1));
cljs.core.async.impl.dispatch.run.call(null,((function (c__20932__auto___30385,ch){
return (function (){
var f__20933__auto__ = (function (){var switch__20870__auto__ = ((function (c__20932__auto___30385,ch){
return (function (state_30359){
var state_val_30360 = (state_30359[(1)]);
if((state_val_30360 === (7))){
var inst_30355 = (state_30359[(2)]);
var state_30359__$1 = state_30359;
var statearr_30361_30386 = state_30359__$1;
(statearr_30361_30386[(2)] = inst_30355);

(statearr_30361_30386[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30360 === (1))){
var state_30359__$1 = state_30359;
var statearr_30362_30387 = state_30359__$1;
(statearr_30362_30387[(2)] = null);

(statearr_30362_30387[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30360 === (4))){
var inst_30323 = (state_30359[(7)]);
var inst_30323__$1 = (state_30359[(2)]);
var state_30359__$1 = (function (){var statearr_30363 = state_30359;
(statearr_30363[(7)] = inst_30323__$1);

return statearr_30363;
})();
if(cljs.core.truth_(inst_30323__$1)){
var statearr_30364_30388 = state_30359__$1;
(statearr_30364_30388[(1)] = (5));

} else {
var statearr_30365_30389 = state_30359__$1;
(statearr_30365_30389[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30360 === (13))){
var state_30359__$1 = state_30359;
var statearr_30366_30390 = state_30359__$1;
(statearr_30366_30390[(2)] = null);

(statearr_30366_30390[(1)] = (14));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30360 === (6))){
var state_30359__$1 = state_30359;
var statearr_30367_30391 = state_30359__$1;
(statearr_30367_30391[(2)] = null);

(statearr_30367_30391[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30360 === (3))){
var inst_30357 = (state_30359[(2)]);
var state_30359__$1 = state_30359;
return cljs.core.async.impl.ioc_helpers.return_chan.call(null,state_30359__$1,inst_30357);
} else {
if((state_val_30360 === (12))){
var inst_30330 = (state_30359[(8)]);
var inst_30343 = new cljs.core.Keyword(null,"files","files",-472457450).cljs$core$IFn$_invoke$arity$1(inst_30330);
var inst_30344 = cljs.core.first.call(null,inst_30343);
var inst_30345 = new cljs.core.Keyword(null,"file","file",-1269645878).cljs$core$IFn$_invoke$arity$1(inst_30344);
var inst_30346 = console.warn("Figwheel: Not loading code with warnings - ",inst_30345);
var state_30359__$1 = state_30359;
var statearr_30368_30392 = state_30359__$1;
(statearr_30368_30392[(2)] = inst_30346);

(statearr_30368_30392[(1)] = (14));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30360 === (2))){
var state_30359__$1 = state_30359;
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_30359__$1,(4),ch);
} else {
if((state_val_30360 === (11))){
var inst_30339 = (state_30359[(2)]);
var state_30359__$1 = state_30359;
var statearr_30369_30393 = state_30359__$1;
(statearr_30369_30393[(2)] = inst_30339);

(statearr_30369_30393[(1)] = (10));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30360 === (9))){
var inst_30329 = (state_30359[(9)]);
var inst_30341 = figwheel.client.block_reload_file_state_QMARK_.call(null,inst_30329,opts);
var state_30359__$1 = state_30359;
if(cljs.core.truth_(inst_30341)){
var statearr_30370_30394 = state_30359__$1;
(statearr_30370_30394[(1)] = (12));

} else {
var statearr_30371_30395 = state_30359__$1;
(statearr_30371_30395[(1)] = (13));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30360 === (5))){
var inst_30323 = (state_30359[(7)]);
var inst_30329 = (state_30359[(9)]);
var inst_30325 = [new cljs.core.Keyword(null,"compile-warning","compile-warning",43425356),null,new cljs.core.Keyword(null,"files-changed","files-changed",-1418200563),null];
var inst_30326 = (new cljs.core.PersistentArrayMap(null,2,inst_30325,null));
var inst_30327 = (new cljs.core.PersistentHashSet(null,inst_30326,null));
var inst_30328 = figwheel.client.focus_msgs.call(null,inst_30327,inst_30323);
var inst_30329__$1 = cljs.core.map.call(null,new cljs.core.Keyword(null,"msg-name","msg-name",-353709863),inst_30328);
var inst_30330 = cljs.core.first.call(null,inst_30328);
var inst_30331 = figwheel.client.reload_file_state_QMARK_.call(null,inst_30329__$1,opts);
var state_30359__$1 = (function (){var statearr_30372 = state_30359;
(statearr_30372[(8)] = inst_30330);

(statearr_30372[(9)] = inst_30329__$1);

return statearr_30372;
})();
if(cljs.core.truth_(inst_30331)){
var statearr_30373_30396 = state_30359__$1;
(statearr_30373_30396[(1)] = (8));

} else {
var statearr_30374_30397 = state_30359__$1;
(statearr_30374_30397[(1)] = (9));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30360 === (14))){
var inst_30349 = (state_30359[(2)]);
var state_30359__$1 = state_30359;
var statearr_30375_30398 = state_30359__$1;
(statearr_30375_30398[(2)] = inst_30349);

(statearr_30375_30398[(1)] = (10));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30360 === (10))){
var inst_30351 = (state_30359[(2)]);
var state_30359__$1 = (function (){var statearr_30376 = state_30359;
(statearr_30376[(10)] = inst_30351);

return statearr_30376;
})();
var statearr_30377_30399 = state_30359__$1;
(statearr_30377_30399[(2)] = null);

(statearr_30377_30399[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30360 === (8))){
var inst_30330 = (state_30359[(8)]);
var inst_30333 = cljs.core.PersistentVector.EMPTY_NODE;
var inst_30334 = figwheel.client.file_reloading.reload_js_files.call(null,opts,inst_30330);
var inst_30335 = cljs.core.async.timeout.call(null,(1000));
var inst_30336 = [inst_30334,inst_30335];
var inst_30337 = (new cljs.core.PersistentVector(null,2,(5),inst_30333,inst_30336,null));
var state_30359__$1 = state_30359;
return cljs.core.async.ioc_alts_BANG_.call(null,state_30359__$1,(11),inst_30337);
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
});})(c__20932__auto___30385,ch))
;
return ((function (switch__20870__auto__,c__20932__auto___30385,ch){
return (function() {
var figwheel$client$file_reloader_plugin_$_state_machine__20871__auto__ = null;
var figwheel$client$file_reloader_plugin_$_state_machine__20871__auto____0 = (function (){
var statearr_30381 = [null,null,null,null,null,null,null,null,null,null,null];
(statearr_30381[(0)] = figwheel$client$file_reloader_plugin_$_state_machine__20871__auto__);

(statearr_30381[(1)] = (1));

return statearr_30381;
});
var figwheel$client$file_reloader_plugin_$_state_machine__20871__auto____1 = (function (state_30359){
while(true){
var ret_value__20872__auto__ = (function (){try{while(true){
var result__20873__auto__ = switch__20870__auto__.call(null,state_30359);
if(cljs.core.keyword_identical_QMARK_.call(null,result__20873__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__20873__auto__;
}
break;
}
}catch (e30382){if((e30382 instanceof Object)){
var ex__20874__auto__ = e30382;
var statearr_30383_30400 = state_30359;
(statearr_30383_30400[(5)] = ex__20874__auto__);


cljs.core.async.impl.ioc_helpers.process_exception.call(null,state_30359);

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
throw e30382;

}
}})();
if(cljs.core.keyword_identical_QMARK_.call(null,ret_value__20872__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__30401 = state_30359;
state_30359 = G__30401;
continue;
} else {
return ret_value__20872__auto__;
}
break;
}
});
figwheel$client$file_reloader_plugin_$_state_machine__20871__auto__ = function(state_30359){
switch(arguments.length){
case 0:
return figwheel$client$file_reloader_plugin_$_state_machine__20871__auto____0.call(this);
case 1:
return figwheel$client$file_reloader_plugin_$_state_machine__20871__auto____1.call(this,state_30359);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
figwheel$client$file_reloader_plugin_$_state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$0 = figwheel$client$file_reloader_plugin_$_state_machine__20871__auto____0;
figwheel$client$file_reloader_plugin_$_state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$1 = figwheel$client$file_reloader_plugin_$_state_machine__20871__auto____1;
return figwheel$client$file_reloader_plugin_$_state_machine__20871__auto__;
})()
;})(switch__20870__auto__,c__20932__auto___30385,ch))
})();
var state__20934__auto__ = (function (){var statearr_30384 = f__20933__auto__.call(null);
(statearr_30384[cljs.core.async.impl.ioc_helpers.USER_START_IDX] = c__20932__auto___30385);

return statearr_30384;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped.call(null,state__20934__auto__);
});})(c__20932__auto___30385,ch))
);


return ((function (ch){
return (function (msg_hist){
cljs.core.async.put_BANG_.call(null,ch,msg_hist);

return msg_hist;
});
;})(ch))
});
figwheel.client.truncate_stack_trace = (function figwheel$client$truncate_stack_trace(stack_str){
return cljs.core.take_while.call(null,(function (p1__30402_SHARP_){
return cljs.core.not.call(null,cljs.core.re_matches.call(null,/.*eval_javascript_STAR__STAR_.*/,p1__30402_SHARP_));
}),clojure.string.split_lines.call(null,stack_str));
});
var base_path_30409 = figwheel.client.utils.base_url_path.call(null);
figwheel.client.eval_javascript_STAR__STAR_ = ((function (base_path_30409){
return (function figwheel$client$eval_javascript_STAR__STAR_(code,result_handler){
try{var _STAR_print_fn_STAR_30407 = cljs.core._STAR_print_fn_STAR_;
var _STAR_print_newline_STAR_30408 = cljs.core._STAR_print_newline_STAR_;
cljs.core._STAR_print_fn_STAR_ = ((function (_STAR_print_fn_STAR_30407,_STAR_print_newline_STAR_30408,base_path_30409){
return (function() { 
var G__30410__delegate = function (args){
return figwheel.client.figwheel_repl_print.call(null,figwheel.client.console_print.call(null,args));
};
var G__30410 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__30411__i = 0, G__30411__a = new Array(arguments.length -  0);
while (G__30411__i < G__30411__a.length) {G__30411__a[G__30411__i] = arguments[G__30411__i + 0]; ++G__30411__i;}
  args = new cljs.core.IndexedSeq(G__30411__a,0);
} 
return G__30410__delegate.call(this,args);};
G__30410.cljs$lang$maxFixedArity = 0;
G__30410.cljs$lang$applyTo = (function (arglist__30412){
var args = cljs.core.seq(arglist__30412);
return G__30410__delegate(args);
});
G__30410.cljs$core$IFn$_invoke$arity$variadic = G__30410__delegate;
return G__30410;
})()
;})(_STAR_print_fn_STAR_30407,_STAR_print_newline_STAR_30408,base_path_30409))
;

cljs.core._STAR_print_newline_STAR_ = false;

try{return result_handler.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"status","status",-1997798413),new cljs.core.Keyword(null,"success","success",1890645906),new cljs.core.Keyword(null,"value","value",305978217),[cljs.core.str(eval(code))].join('')], null));
}finally {cljs.core._STAR_print_newline_STAR_ = _STAR_print_newline_STAR_30408;

cljs.core._STAR_print_fn_STAR_ = _STAR_print_fn_STAR_30407;
}}catch (e30406){if((e30406 instanceof Error)){
var e = e30406;
return result_handler.call(null,new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"status","status",-1997798413),new cljs.core.Keyword(null,"exception","exception",-335277064),new cljs.core.Keyword(null,"value","value",305978217),cljs.core.pr_str.call(null,e),new cljs.core.Keyword(null,"stacktrace","stacktrace",-95588394),clojure.string.join.call(null,"\n",figwheel.client.truncate_stack_trace.call(null,e.stack)),new cljs.core.Keyword(null,"base-path","base-path",495760020),base_path_30409], null));
} else {
var e = e30406;
return result_handler.call(null,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"status","status",-1997798413),new cljs.core.Keyword(null,"exception","exception",-335277064),new cljs.core.Keyword(null,"value","value",305978217),cljs.core.pr_str.call(null,e),new cljs.core.Keyword(null,"stacktrace","stacktrace",-95588394),"No stacktrace available."], null));

}
}});})(base_path_30409))
;
/**
 * The REPL can disconnect and reconnect lets ensure cljs.user exists at least.
 */
figwheel.client.ensure_cljs_user = (function figwheel$client$ensure_cljs_user(){
if(cljs.core.truth_(cljs.user)){
return null;
} else {
return cljs.user = {};
}
});
figwheel.client.repl_plugin = (function figwheel$client$repl_plugin(p__30413){
var map__30418 = p__30413;
var map__30418__$1 = ((cljs.core.seq_QMARK_.call(null,map__30418))?cljs.core.apply.call(null,cljs.core.hash_map,map__30418):map__30418);
var opts = map__30418__$1;
var build_id = cljs.core.get.call(null,map__30418__$1,new cljs.core.Keyword(null,"build-id","build-id",1642831089));
return ((function (map__30418,map__30418__$1,opts,build_id){
return (function (p__30419){
var vec__30420 = p__30419;
var map__30421 = cljs.core.nth.call(null,vec__30420,(0),null);
var map__30421__$1 = ((cljs.core.seq_QMARK_.call(null,map__30421))?cljs.core.apply.call(null,cljs.core.hash_map,map__30421):map__30421);
var msg = map__30421__$1;
var msg_name = cljs.core.get.call(null,map__30421__$1,new cljs.core.Keyword(null,"msg-name","msg-name",-353709863));
var _ = cljs.core.nthnext.call(null,vec__30420,(1));
if(cljs.core._EQ_.call(null,new cljs.core.Keyword(null,"repl-eval","repl-eval",-1784727398),msg_name)){
figwheel.client.ensure_cljs_user.call(null);

return figwheel.client.eval_javascript_STAR__STAR_.call(null,new cljs.core.Keyword(null,"code","code",1586293142).cljs$core$IFn$_invoke$arity$1(msg),((function (vec__30420,map__30421,map__30421__$1,msg,msg_name,_,map__30418,map__30418__$1,opts,build_id){
return (function (res){
return figwheel.client.socket.send_BANG_.call(null,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"figwheel-event","figwheel-event",519570592),"callback",new cljs.core.Keyword(null,"callback-name","callback-name",336964714),new cljs.core.Keyword(null,"callback-name","callback-name",336964714).cljs$core$IFn$_invoke$arity$1(msg),new cljs.core.Keyword(null,"content","content",15833224),res], null));
});})(vec__30420,map__30421,map__30421__$1,msg,msg_name,_,map__30418,map__30418__$1,opts,build_id))
);
} else {
return null;
}
});
;})(map__30418,map__30418__$1,opts,build_id))
});
figwheel.client.css_reloader_plugin = (function figwheel$client$css_reloader_plugin(opts){
return (function (p__30425){
var vec__30426 = p__30425;
var map__30427 = cljs.core.nth.call(null,vec__30426,(0),null);
var map__30427__$1 = ((cljs.core.seq_QMARK_.call(null,map__30427))?cljs.core.apply.call(null,cljs.core.hash_map,map__30427):map__30427);
var msg = map__30427__$1;
var msg_name = cljs.core.get.call(null,map__30427__$1,new cljs.core.Keyword(null,"msg-name","msg-name",-353709863));
var _ = cljs.core.nthnext.call(null,vec__30426,(1));
if(cljs.core._EQ_.call(null,msg_name,new cljs.core.Keyword(null,"css-files-changed","css-files-changed",720773874))){
return figwheel.client.file_reloading.reload_css_files.call(null,opts,msg);
} else {
return null;
}
});
});
figwheel.client.compile_fail_warning_plugin = (function figwheel$client$compile_fail_warning_plugin(p__30428){
var map__30436 = p__30428;
var map__30436__$1 = ((cljs.core.seq_QMARK_.call(null,map__30436))?cljs.core.apply.call(null,cljs.core.hash_map,map__30436):map__30436);
var on_compile_warning = cljs.core.get.call(null,map__30436__$1,new cljs.core.Keyword(null,"on-compile-warning","on-compile-warning",-1195585947));
var on_compile_fail = cljs.core.get.call(null,map__30436__$1,new cljs.core.Keyword(null,"on-compile-fail","on-compile-fail",728013036));
return ((function (map__30436,map__30436__$1,on_compile_warning,on_compile_fail){
return (function (p__30437){
var vec__30438 = p__30437;
var map__30439 = cljs.core.nth.call(null,vec__30438,(0),null);
var map__30439__$1 = ((cljs.core.seq_QMARK_.call(null,map__30439))?cljs.core.apply.call(null,cljs.core.hash_map,map__30439):map__30439);
var msg = map__30439__$1;
var msg_name = cljs.core.get.call(null,map__30439__$1,new cljs.core.Keyword(null,"msg-name","msg-name",-353709863));
var _ = cljs.core.nthnext.call(null,vec__30438,(1));
var pred__30440 = cljs.core._EQ_;
var expr__30441 = msg_name;
if(cljs.core.truth_(pred__30440.call(null,new cljs.core.Keyword(null,"compile-warning","compile-warning",43425356),expr__30441))){
return on_compile_warning.call(null,msg);
} else {
if(cljs.core.truth_(pred__30440.call(null,new cljs.core.Keyword(null,"compile-failed","compile-failed",-477639289),expr__30441))){
return on_compile_fail.call(null,msg);
} else {
return null;
}
}
});
;})(map__30436,map__30436__$1,on_compile_warning,on_compile_fail))
});
figwheel.client.heads_up_plugin_msg_handler = (function figwheel$client$heads_up_plugin_msg_handler(opts,msg_hist_SINGLEQUOTE_){
var msg_hist = figwheel.client.focus_msgs.call(null,new cljs.core.PersistentHashSet(null, new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"compile-failed","compile-failed",-477639289),null,new cljs.core.Keyword(null,"compile-warning","compile-warning",43425356),null,new cljs.core.Keyword(null,"files-changed","files-changed",-1418200563),null], null), null),msg_hist_SINGLEQUOTE_);
var msg_names = cljs.core.map.call(null,new cljs.core.Keyword(null,"msg-name","msg-name",-353709863),msg_hist);
var msg = cljs.core.first.call(null,msg_hist);
var c__20932__auto__ = cljs.core.async.chan.call(null,(1));
cljs.core.async.impl.dispatch.run.call(null,((function (c__20932__auto__,msg_hist,msg_names,msg){
return (function (){
var f__20933__auto__ = (function (){var switch__20870__auto__ = ((function (c__20932__auto__,msg_hist,msg_names,msg){
return (function (state_30642){
var state_val_30643 = (state_30642[(1)]);
if((state_val_30643 === (7))){
var inst_30576 = (state_30642[(2)]);
var state_30642__$1 = state_30642;
var statearr_30644_30685 = state_30642__$1;
(statearr_30644_30685[(2)] = inst_30576);

(statearr_30644_30685[(1)] = (4));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30643 === (20))){
var inst_30604 = figwheel.client.rewarning_state_QMARK_.call(null,msg_names);
var state_30642__$1 = state_30642;
if(cljs.core.truth_(inst_30604)){
var statearr_30645_30686 = state_30642__$1;
(statearr_30645_30686[(1)] = (22));

} else {
var statearr_30646_30687 = state_30642__$1;
(statearr_30646_30687[(1)] = (23));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30643 === (27))){
var inst_30616 = new cljs.core.Keyword(null,"message","message",-406056002).cljs$core$IFn$_invoke$arity$1(msg);
var inst_30617 = figwheel.client.heads_up.display_warning.call(null,inst_30616);
var state_30642__$1 = state_30642;
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_30642__$1,(30),inst_30617);
} else {
if((state_val_30643 === (1))){
var inst_30564 = figwheel.client.reload_file_state_QMARK_.call(null,msg_names,opts);
var state_30642__$1 = state_30642;
if(cljs.core.truth_(inst_30564)){
var statearr_30647_30688 = state_30642__$1;
(statearr_30647_30688[(1)] = (2));

} else {
var statearr_30648_30689 = state_30642__$1;
(statearr_30648_30689[(1)] = (3));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30643 === (24))){
var inst_30632 = (state_30642[(2)]);
var state_30642__$1 = state_30642;
var statearr_30649_30690 = state_30642__$1;
(statearr_30649_30690[(2)] = inst_30632);

(statearr_30649_30690[(1)] = (21));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30643 === (4))){
var inst_30640 = (state_30642[(2)]);
var state_30642__$1 = state_30642;
return cljs.core.async.impl.ioc_helpers.return_chan.call(null,state_30642__$1,inst_30640);
} else {
if((state_val_30643 === (15))){
var inst_30592 = new cljs.core.Keyword(null,"exception-data","exception-data",-512474886).cljs$core$IFn$_invoke$arity$1(msg);
var inst_30593 = figwheel.client.format_messages.call(null,inst_30592);
var inst_30594 = new cljs.core.Keyword(null,"cause","cause",231901252).cljs$core$IFn$_invoke$arity$1(msg);
var inst_30595 = figwheel.client.heads_up.display_error.call(null,inst_30593,inst_30594);
var state_30642__$1 = state_30642;
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_30642__$1,(18),inst_30595);
} else {
if((state_val_30643 === (21))){
var inst_30634 = (state_30642[(2)]);
var state_30642__$1 = state_30642;
var statearr_30650_30691 = state_30642__$1;
(statearr_30650_30691[(2)] = inst_30634);

(statearr_30650_30691[(1)] = (17));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30643 === (31))){
var inst_30623 = figwheel.client.heads_up.flash_loaded.call(null);
var state_30642__$1 = state_30642;
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_30642__$1,(34),inst_30623);
} else {
if((state_val_30643 === (32))){
var state_30642__$1 = state_30642;
var statearr_30651_30692 = state_30642__$1;
(statearr_30651_30692[(2)] = null);

(statearr_30651_30692[(1)] = (33));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30643 === (33))){
var inst_30628 = (state_30642[(2)]);
var state_30642__$1 = state_30642;
var statearr_30652_30693 = state_30642__$1;
(statearr_30652_30693[(2)] = inst_30628);

(statearr_30652_30693[(1)] = (29));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30643 === (13))){
var inst_30582 = (state_30642[(2)]);
var inst_30583 = new cljs.core.Keyword(null,"exception-data","exception-data",-512474886).cljs$core$IFn$_invoke$arity$1(msg);
var inst_30584 = figwheel.client.format_messages.call(null,inst_30583);
var inst_30585 = new cljs.core.Keyword(null,"cause","cause",231901252).cljs$core$IFn$_invoke$arity$1(msg);
var inst_30586 = figwheel.client.heads_up.display_error.call(null,inst_30584,inst_30585);
var state_30642__$1 = (function (){var statearr_30653 = state_30642;
(statearr_30653[(7)] = inst_30582);

return statearr_30653;
})();
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_30642__$1,(14),inst_30586);
} else {
if((state_val_30643 === (22))){
var inst_30606 = figwheel.client.heads_up.clear.call(null);
var state_30642__$1 = state_30642;
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_30642__$1,(25),inst_30606);
} else {
if((state_val_30643 === (29))){
var inst_30630 = (state_30642[(2)]);
var state_30642__$1 = state_30642;
var statearr_30654_30694 = state_30642__$1;
(statearr_30654_30694[(2)] = inst_30630);

(statearr_30654_30694[(1)] = (24));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30643 === (6))){
var inst_30572 = figwheel.client.heads_up.clear.call(null);
var state_30642__$1 = state_30642;
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_30642__$1,(9),inst_30572);
} else {
if((state_val_30643 === (28))){
var inst_30621 = figwheel.client.css_loaded_state_QMARK_.call(null,msg_names);
var state_30642__$1 = state_30642;
if(cljs.core.truth_(inst_30621)){
var statearr_30655_30695 = state_30642__$1;
(statearr_30655_30695[(1)] = (31));

} else {
var statearr_30656_30696 = state_30642__$1;
(statearr_30656_30696[(1)] = (32));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30643 === (25))){
var inst_30608 = (state_30642[(2)]);
var inst_30609 = new cljs.core.Keyword(null,"message","message",-406056002).cljs$core$IFn$_invoke$arity$1(msg);
var inst_30610 = figwheel.client.heads_up.display_warning.call(null,inst_30609);
var state_30642__$1 = (function (){var statearr_30657 = state_30642;
(statearr_30657[(8)] = inst_30608);

return statearr_30657;
})();
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_30642__$1,(26),inst_30610);
} else {
if((state_val_30643 === (34))){
var inst_30625 = (state_30642[(2)]);
var state_30642__$1 = state_30642;
var statearr_30658_30697 = state_30642__$1;
(statearr_30658_30697[(2)] = inst_30625);

(statearr_30658_30697[(1)] = (33));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30643 === (17))){
var inst_30636 = (state_30642[(2)]);
var state_30642__$1 = state_30642;
var statearr_30659_30698 = state_30642__$1;
(statearr_30659_30698[(2)] = inst_30636);

(statearr_30659_30698[(1)] = (12));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30643 === (3))){
var inst_30578 = figwheel.client.compile_refail_state_QMARK_.call(null,msg_names);
var state_30642__$1 = state_30642;
if(cljs.core.truth_(inst_30578)){
var statearr_30660_30699 = state_30642__$1;
(statearr_30660_30699[(1)] = (10));

} else {
var statearr_30661_30700 = state_30642__$1;
(statearr_30661_30700[(1)] = (11));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30643 === (12))){
var inst_30638 = (state_30642[(2)]);
var state_30642__$1 = state_30642;
var statearr_30662_30701 = state_30642__$1;
(statearr_30662_30701[(2)] = inst_30638);

(statearr_30662_30701[(1)] = (4));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30643 === (2))){
var inst_30566 = new cljs.core.Keyword(null,"autoload","autoload",-354122500).cljs$core$IFn$_invoke$arity$1(opts);
var state_30642__$1 = state_30642;
if(cljs.core.truth_(inst_30566)){
var statearr_30663_30702 = state_30642__$1;
(statearr_30663_30702[(1)] = (5));

} else {
var statearr_30664_30703 = state_30642__$1;
(statearr_30664_30703[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30643 === (23))){
var inst_30614 = figwheel.client.warning_state_QMARK_.call(null,msg_names);
var state_30642__$1 = state_30642;
if(cljs.core.truth_(inst_30614)){
var statearr_30665_30704 = state_30642__$1;
(statearr_30665_30704[(1)] = (27));

} else {
var statearr_30666_30705 = state_30642__$1;
(statearr_30666_30705[(1)] = (28));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30643 === (19))){
var inst_30601 = new cljs.core.Keyword(null,"message","message",-406056002).cljs$core$IFn$_invoke$arity$1(msg);
var inst_30602 = figwheel.client.heads_up.append_message.call(null,inst_30601);
var state_30642__$1 = state_30642;
var statearr_30667_30706 = state_30642__$1;
(statearr_30667_30706[(2)] = inst_30602);

(statearr_30667_30706[(1)] = (21));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30643 === (11))){
var inst_30590 = figwheel.client.compile_fail_state_QMARK_.call(null,msg_names);
var state_30642__$1 = state_30642;
if(cljs.core.truth_(inst_30590)){
var statearr_30668_30707 = state_30642__$1;
(statearr_30668_30707[(1)] = (15));

} else {
var statearr_30669_30708 = state_30642__$1;
(statearr_30669_30708[(1)] = (16));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30643 === (9))){
var inst_30574 = (state_30642[(2)]);
var state_30642__$1 = state_30642;
var statearr_30670_30709 = state_30642__$1;
(statearr_30670_30709[(2)] = inst_30574);

(statearr_30670_30709[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30643 === (5))){
var inst_30568 = figwheel.client.heads_up.flash_loaded.call(null);
var state_30642__$1 = state_30642;
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_30642__$1,(8),inst_30568);
} else {
if((state_val_30643 === (14))){
var inst_30588 = (state_30642[(2)]);
var state_30642__$1 = state_30642;
var statearr_30671_30710 = state_30642__$1;
(statearr_30671_30710[(2)] = inst_30588);

(statearr_30671_30710[(1)] = (12));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30643 === (26))){
var inst_30612 = (state_30642[(2)]);
var state_30642__$1 = state_30642;
var statearr_30672_30711 = state_30642__$1;
(statearr_30672_30711[(2)] = inst_30612);

(statearr_30672_30711[(1)] = (24));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30643 === (16))){
var inst_30599 = figwheel.client.warning_append_state_QMARK_.call(null,msg_names);
var state_30642__$1 = state_30642;
if(cljs.core.truth_(inst_30599)){
var statearr_30673_30712 = state_30642__$1;
(statearr_30673_30712[(1)] = (19));

} else {
var statearr_30674_30713 = state_30642__$1;
(statearr_30674_30713[(1)] = (20));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30643 === (30))){
var inst_30619 = (state_30642[(2)]);
var state_30642__$1 = state_30642;
var statearr_30675_30714 = state_30642__$1;
(statearr_30675_30714[(2)] = inst_30619);

(statearr_30675_30714[(1)] = (29));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30643 === (10))){
var inst_30580 = figwheel.client.heads_up.clear.call(null);
var state_30642__$1 = state_30642;
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_30642__$1,(13),inst_30580);
} else {
if((state_val_30643 === (18))){
var inst_30597 = (state_30642[(2)]);
var state_30642__$1 = state_30642;
var statearr_30676_30715 = state_30642__$1;
(statearr_30676_30715[(2)] = inst_30597);

(statearr_30676_30715[(1)] = (17));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30643 === (8))){
var inst_30570 = (state_30642[(2)]);
var state_30642__$1 = state_30642;
var statearr_30677_30716 = state_30642__$1;
(statearr_30677_30716[(2)] = inst_30570);

(statearr_30677_30716[(1)] = (7));


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
});})(c__20932__auto__,msg_hist,msg_names,msg))
;
return ((function (switch__20870__auto__,c__20932__auto__,msg_hist,msg_names,msg){
return (function() {
var figwheel$client$heads_up_plugin_msg_handler_$_state_machine__20871__auto__ = null;
var figwheel$client$heads_up_plugin_msg_handler_$_state_machine__20871__auto____0 = (function (){
var statearr_30681 = [null,null,null,null,null,null,null,null,null];
(statearr_30681[(0)] = figwheel$client$heads_up_plugin_msg_handler_$_state_machine__20871__auto__);

(statearr_30681[(1)] = (1));

return statearr_30681;
});
var figwheel$client$heads_up_plugin_msg_handler_$_state_machine__20871__auto____1 = (function (state_30642){
while(true){
var ret_value__20872__auto__ = (function (){try{while(true){
var result__20873__auto__ = switch__20870__auto__.call(null,state_30642);
if(cljs.core.keyword_identical_QMARK_.call(null,result__20873__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__20873__auto__;
}
break;
}
}catch (e30682){if((e30682 instanceof Object)){
var ex__20874__auto__ = e30682;
var statearr_30683_30717 = state_30642;
(statearr_30683_30717[(5)] = ex__20874__auto__);


cljs.core.async.impl.ioc_helpers.process_exception.call(null,state_30642);

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
throw e30682;

}
}})();
if(cljs.core.keyword_identical_QMARK_.call(null,ret_value__20872__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__30718 = state_30642;
state_30642 = G__30718;
continue;
} else {
return ret_value__20872__auto__;
}
break;
}
});
figwheel$client$heads_up_plugin_msg_handler_$_state_machine__20871__auto__ = function(state_30642){
switch(arguments.length){
case 0:
return figwheel$client$heads_up_plugin_msg_handler_$_state_machine__20871__auto____0.call(this);
case 1:
return figwheel$client$heads_up_plugin_msg_handler_$_state_machine__20871__auto____1.call(this,state_30642);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
figwheel$client$heads_up_plugin_msg_handler_$_state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$0 = figwheel$client$heads_up_plugin_msg_handler_$_state_machine__20871__auto____0;
figwheel$client$heads_up_plugin_msg_handler_$_state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$1 = figwheel$client$heads_up_plugin_msg_handler_$_state_machine__20871__auto____1;
return figwheel$client$heads_up_plugin_msg_handler_$_state_machine__20871__auto__;
})()
;})(switch__20870__auto__,c__20932__auto__,msg_hist,msg_names,msg))
})();
var state__20934__auto__ = (function (){var statearr_30684 = f__20933__auto__.call(null);
(statearr_30684[cljs.core.async.impl.ioc_helpers.USER_START_IDX] = c__20932__auto__);

return statearr_30684;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped.call(null,state__20934__auto__);
});})(c__20932__auto__,msg_hist,msg_names,msg))
);

return c__20932__auto__;
});
figwheel.client.heads_up_plugin = (function figwheel$client$heads_up_plugin(opts){
var ch = cljs.core.async.chan.call(null);
figwheel.client.heads_up_config_options_STAR__STAR_ = opts;

var c__20932__auto___30781 = cljs.core.async.chan.call(null,(1));
cljs.core.async.impl.dispatch.run.call(null,((function (c__20932__auto___30781,ch){
return (function (){
var f__20933__auto__ = (function (){var switch__20870__auto__ = ((function (c__20932__auto___30781,ch){
return (function (state_30764){
var state_val_30765 = (state_30764[(1)]);
if((state_val_30765 === (1))){
var state_30764__$1 = state_30764;
var statearr_30766_30782 = state_30764__$1;
(statearr_30766_30782[(2)] = null);

(statearr_30766_30782[(1)] = (2));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30765 === (2))){
var state_30764__$1 = state_30764;
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_30764__$1,(4),ch);
} else {
if((state_val_30765 === (3))){
var inst_30762 = (state_30764[(2)]);
var state_30764__$1 = state_30764;
return cljs.core.async.impl.ioc_helpers.return_chan.call(null,state_30764__$1,inst_30762);
} else {
if((state_val_30765 === (4))){
var inst_30752 = (state_30764[(7)]);
var inst_30752__$1 = (state_30764[(2)]);
var state_30764__$1 = (function (){var statearr_30767 = state_30764;
(statearr_30767[(7)] = inst_30752__$1);

return statearr_30767;
})();
if(cljs.core.truth_(inst_30752__$1)){
var statearr_30768_30783 = state_30764__$1;
(statearr_30768_30783[(1)] = (5));

} else {
var statearr_30769_30784 = state_30764__$1;
(statearr_30769_30784[(1)] = (6));

}

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30765 === (5))){
var inst_30752 = (state_30764[(7)]);
var inst_30754 = figwheel.client.heads_up_plugin_msg_handler.call(null,opts,inst_30752);
var state_30764__$1 = state_30764;
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_30764__$1,(8),inst_30754);
} else {
if((state_val_30765 === (6))){
var state_30764__$1 = state_30764;
var statearr_30770_30785 = state_30764__$1;
(statearr_30770_30785[(2)] = null);

(statearr_30770_30785[(1)] = (7));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30765 === (7))){
var inst_30760 = (state_30764[(2)]);
var state_30764__$1 = state_30764;
var statearr_30771_30786 = state_30764__$1;
(statearr_30771_30786[(2)] = inst_30760);

(statearr_30771_30786[(1)] = (3));


return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
if((state_val_30765 === (8))){
var inst_30756 = (state_30764[(2)]);
var state_30764__$1 = (function (){var statearr_30772 = state_30764;
(statearr_30772[(8)] = inst_30756);

return statearr_30772;
})();
var statearr_30773_30787 = state_30764__$1;
(statearr_30773_30787[(2)] = null);

(statearr_30773_30787[(1)] = (2));


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
});})(c__20932__auto___30781,ch))
;
return ((function (switch__20870__auto__,c__20932__auto___30781,ch){
return (function() {
var figwheel$client$heads_up_plugin_$_state_machine__20871__auto__ = null;
var figwheel$client$heads_up_plugin_$_state_machine__20871__auto____0 = (function (){
var statearr_30777 = [null,null,null,null,null,null,null,null,null];
(statearr_30777[(0)] = figwheel$client$heads_up_plugin_$_state_machine__20871__auto__);

(statearr_30777[(1)] = (1));

return statearr_30777;
});
var figwheel$client$heads_up_plugin_$_state_machine__20871__auto____1 = (function (state_30764){
while(true){
var ret_value__20872__auto__ = (function (){try{while(true){
var result__20873__auto__ = switch__20870__auto__.call(null,state_30764);
if(cljs.core.keyword_identical_QMARK_.call(null,result__20873__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__20873__auto__;
}
break;
}
}catch (e30778){if((e30778 instanceof Object)){
var ex__20874__auto__ = e30778;
var statearr_30779_30788 = state_30764;
(statearr_30779_30788[(5)] = ex__20874__auto__);


cljs.core.async.impl.ioc_helpers.process_exception.call(null,state_30764);

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
throw e30778;

}
}})();
if(cljs.core.keyword_identical_QMARK_.call(null,ret_value__20872__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__30789 = state_30764;
state_30764 = G__30789;
continue;
} else {
return ret_value__20872__auto__;
}
break;
}
});
figwheel$client$heads_up_plugin_$_state_machine__20871__auto__ = function(state_30764){
switch(arguments.length){
case 0:
return figwheel$client$heads_up_plugin_$_state_machine__20871__auto____0.call(this);
case 1:
return figwheel$client$heads_up_plugin_$_state_machine__20871__auto____1.call(this,state_30764);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
figwheel$client$heads_up_plugin_$_state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$0 = figwheel$client$heads_up_plugin_$_state_machine__20871__auto____0;
figwheel$client$heads_up_plugin_$_state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$1 = figwheel$client$heads_up_plugin_$_state_machine__20871__auto____1;
return figwheel$client$heads_up_plugin_$_state_machine__20871__auto__;
})()
;})(switch__20870__auto__,c__20932__auto___30781,ch))
})();
var state__20934__auto__ = (function (){var statearr_30780 = f__20933__auto__.call(null);
(statearr_30780[cljs.core.async.impl.ioc_helpers.USER_START_IDX] = c__20932__auto___30781);

return statearr_30780;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped.call(null,state__20934__auto__);
});})(c__20932__auto___30781,ch))
);


figwheel.client.heads_up.ensure_container.call(null);

return ((function (ch){
return (function (msg_hist){
cljs.core.async.put_BANG_.call(null,ch,msg_hist);

return msg_hist;
});
;})(ch))
});
figwheel.client.enforce_project_plugin = (function figwheel$client$enforce_project_plugin(opts){
return (function (msg_hist){
if(((1) < cljs.core.count.call(null,cljs.core.set.call(null,cljs.core.keep.call(null,new cljs.core.Keyword(null,"project-id","project-id",206449307),cljs.core.take.call(null,(5),msg_hist)))))){
figwheel.client.socket.close_BANG_.call(null);

console.error("Figwheel: message received from different project. Shutting socket down.");

if(cljs.core.truth_(new cljs.core.Keyword(null,"heads-up-display","heads-up-display",-896577202).cljs$core$IFn$_invoke$arity$1(opts))){
var c__20932__auto__ = cljs.core.async.chan.call(null,(1));
cljs.core.async.impl.dispatch.run.call(null,((function (c__20932__auto__){
return (function (){
var f__20933__auto__ = (function (){var switch__20870__auto__ = ((function (c__20932__auto__){
return (function (state_30810){
var state_val_30811 = (state_30810[(1)]);
if((state_val_30811 === (1))){
var inst_30805 = cljs.core.async.timeout.call(null,(3000));
var state_30810__$1 = state_30810;
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_30810__$1,(2),inst_30805);
} else {
if((state_val_30811 === (2))){
var inst_30807 = (state_30810[(2)]);
var inst_30808 = figwheel.client.heads_up.display_system_warning.call(null,"Connection from different project","Shutting connection down!!!!!");
var state_30810__$1 = (function (){var statearr_30812 = state_30810;
(statearr_30812[(7)] = inst_30807);

return statearr_30812;
})();
return cljs.core.async.impl.ioc_helpers.return_chan.call(null,state_30810__$1,inst_30808);
} else {
return null;
}
}
});})(c__20932__auto__))
;
return ((function (switch__20870__auto__,c__20932__auto__){
return (function() {
var figwheel$client$enforce_project_plugin_$_state_machine__20871__auto__ = null;
var figwheel$client$enforce_project_plugin_$_state_machine__20871__auto____0 = (function (){
var statearr_30816 = [null,null,null,null,null,null,null,null];
(statearr_30816[(0)] = figwheel$client$enforce_project_plugin_$_state_machine__20871__auto__);

(statearr_30816[(1)] = (1));

return statearr_30816;
});
var figwheel$client$enforce_project_plugin_$_state_machine__20871__auto____1 = (function (state_30810){
while(true){
var ret_value__20872__auto__ = (function (){try{while(true){
var result__20873__auto__ = switch__20870__auto__.call(null,state_30810);
if(cljs.core.keyword_identical_QMARK_.call(null,result__20873__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__20873__auto__;
}
break;
}
}catch (e30817){if((e30817 instanceof Object)){
var ex__20874__auto__ = e30817;
var statearr_30818_30820 = state_30810;
(statearr_30818_30820[(5)] = ex__20874__auto__);


cljs.core.async.impl.ioc_helpers.process_exception.call(null,state_30810);

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
throw e30817;

}
}})();
if(cljs.core.keyword_identical_QMARK_.call(null,ret_value__20872__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__30821 = state_30810;
state_30810 = G__30821;
continue;
} else {
return ret_value__20872__auto__;
}
break;
}
});
figwheel$client$enforce_project_plugin_$_state_machine__20871__auto__ = function(state_30810){
switch(arguments.length){
case 0:
return figwheel$client$enforce_project_plugin_$_state_machine__20871__auto____0.call(this);
case 1:
return figwheel$client$enforce_project_plugin_$_state_machine__20871__auto____1.call(this,state_30810);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
figwheel$client$enforce_project_plugin_$_state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$0 = figwheel$client$enforce_project_plugin_$_state_machine__20871__auto____0;
figwheel$client$enforce_project_plugin_$_state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$1 = figwheel$client$enforce_project_plugin_$_state_machine__20871__auto____1;
return figwheel$client$enforce_project_plugin_$_state_machine__20871__auto__;
})()
;})(switch__20870__auto__,c__20932__auto__))
})();
var state__20934__auto__ = (function (){var statearr_30819 = f__20933__auto__.call(null);
(statearr_30819[cljs.core.async.impl.ioc_helpers.USER_START_IDX] = c__20932__auto__);

return statearr_30819;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped.call(null,state__20934__auto__);
});})(c__20932__auto__))
);

return c__20932__auto__;
} else {
return null;
}
} else {
return null;
}
});
});
figwheel.client.default_on_jsload = (function figwheel$client$default_on_jsload(url){
if(cljs.core.truth_((function (){var and__18061__auto__ = figwheel.client.utils.html_env_QMARK_.call(null);
if(cljs.core.truth_(and__18061__auto__)){
return ("CustomEvent" in window);
} else {
return and__18061__auto__;
}
})())){
return document.body.dispatchEvent((new CustomEvent("figwheel.js-reload",(function (){var obj30825 = {"detail":url};
return obj30825;
})())));
} else {
return null;
}
});
figwheel.client.default_on_compile_fail = (function figwheel$client$default_on_compile_fail(p__30826){
var map__30832 = p__30826;
var map__30832__$1 = ((cljs.core.seq_QMARK_.call(null,map__30832))?cljs.core.apply.call(null,cljs.core.hash_map,map__30832):map__30832);
var ed = map__30832__$1;
var formatted_exception = cljs.core.get.call(null,map__30832__$1,new cljs.core.Keyword(null,"formatted-exception","formatted-exception",-116489026));
var exception_data = cljs.core.get.call(null,map__30832__$1,new cljs.core.Keyword(null,"exception-data","exception-data",-512474886));
var cause = cljs.core.get.call(null,map__30832__$1,new cljs.core.Keyword(null,"cause","cause",231901252));
figwheel.client.utils.log.call(null,new cljs.core.Keyword(null,"debug","debug",-1608172596),"Figwheel: Compile Exception");

var seq__30833_30837 = cljs.core.seq.call(null,figwheel.client.format_messages.call(null,exception_data));
var chunk__30834_30838 = null;
var count__30835_30839 = (0);
var i__30836_30840 = (0);
while(true){
if((i__30836_30840 < count__30835_30839)){
var msg_30841 = cljs.core._nth.call(null,chunk__30834_30838,i__30836_30840);
figwheel.client.utils.log.call(null,new cljs.core.Keyword(null,"info","info",-317069002),msg_30841);

var G__30842 = seq__30833_30837;
var G__30843 = chunk__30834_30838;
var G__30844 = count__30835_30839;
var G__30845 = (i__30836_30840 + (1));
seq__30833_30837 = G__30842;
chunk__30834_30838 = G__30843;
count__30835_30839 = G__30844;
i__30836_30840 = G__30845;
continue;
} else {
var temp__4423__auto___30846 = cljs.core.seq.call(null,seq__30833_30837);
if(temp__4423__auto___30846){
var seq__30833_30847__$1 = temp__4423__auto___30846;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__30833_30847__$1)){
var c__18858__auto___30848 = cljs.core.chunk_first.call(null,seq__30833_30847__$1);
var G__30849 = cljs.core.chunk_rest.call(null,seq__30833_30847__$1);
var G__30850 = c__18858__auto___30848;
var G__30851 = cljs.core.count.call(null,c__18858__auto___30848);
var G__30852 = (0);
seq__30833_30837 = G__30849;
chunk__30834_30838 = G__30850;
count__30835_30839 = G__30851;
i__30836_30840 = G__30852;
continue;
} else {
var msg_30853 = cljs.core.first.call(null,seq__30833_30847__$1);
figwheel.client.utils.log.call(null,new cljs.core.Keyword(null,"info","info",-317069002),msg_30853);

var G__30854 = cljs.core.next.call(null,seq__30833_30847__$1);
var G__30855 = null;
var G__30856 = (0);
var G__30857 = (0);
seq__30833_30837 = G__30854;
chunk__30834_30838 = G__30855;
count__30835_30839 = G__30856;
i__30836_30840 = G__30857;
continue;
}
} else {
}
}
break;
}

if(cljs.core.truth_(cause)){
figwheel.client.utils.log.call(null,new cljs.core.Keyword(null,"info","info",-317069002),[cljs.core.str("Error on file "),cljs.core.str(new cljs.core.Keyword(null,"file","file",-1269645878).cljs$core$IFn$_invoke$arity$1(cause)),cljs.core.str(", line "),cljs.core.str(new cljs.core.Keyword(null,"line","line",212345235).cljs$core$IFn$_invoke$arity$1(cause)),cljs.core.str(", column "),cljs.core.str(new cljs.core.Keyword(null,"column","column",2078222095).cljs$core$IFn$_invoke$arity$1(cause))].join(''));
} else {
}

return ed;
});
figwheel.client.default_on_compile_warning = (function figwheel$client$default_on_compile_warning(p__30858){
var map__30860 = p__30858;
var map__30860__$1 = ((cljs.core.seq_QMARK_.call(null,map__30860))?cljs.core.apply.call(null,cljs.core.hash_map,map__30860):map__30860);
var w = map__30860__$1;
var message = cljs.core.get.call(null,map__30860__$1,new cljs.core.Keyword(null,"message","message",-406056002));
figwheel.client.utils.log.call(null,new cljs.core.Keyword(null,"warn","warn",-436710552),[cljs.core.str("Figwheel: Compile Warning - "),cljs.core.str(message)].join(''));

return w;
});
figwheel.client.default_before_load = (function figwheel$client$default_before_load(files){
figwheel.client.utils.log.call(null,new cljs.core.Keyword(null,"debug","debug",-1608172596),"Figwheel: notified of file changes");

return files;
});
figwheel.client.default_on_cssload = (function figwheel$client$default_on_cssload(files){
figwheel.client.utils.log.call(null,new cljs.core.Keyword(null,"debug","debug",-1608172596),"Figwheel: loaded CSS files");

figwheel.client.utils.log.call(null,new cljs.core.Keyword(null,"info","info",-317069002),cljs.core.pr_str.call(null,cljs.core.map.call(null,new cljs.core.Keyword(null,"file","file",-1269645878),files)));

return files;
});
if(typeof figwheel.client.config_defaults !== 'undefined'){
} else {
figwheel.client.config_defaults = cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"load-unchanged-files","load-unchanged-files",-1561468704),new cljs.core.Keyword(null,"on-compile-warning","on-compile-warning",-1195585947),new cljs.core.Keyword(null,"on-jsload","on-jsload",-395756602),new cljs.core.Keyword(null,"on-compile-fail","on-compile-fail",728013036),new cljs.core.Keyword(null,"debug","debug",-1608172596),new cljs.core.Keyword(null,"heads-up-display","heads-up-display",-896577202),new cljs.core.Keyword(null,"websocket-url","websocket-url",-490444938),new cljs.core.Keyword(null,"before-jsload","before-jsload",-847513128),new cljs.core.Keyword(null,"load-warninged-code","load-warninged-code",-2030345223),new cljs.core.Keyword(null,"retry-count","retry-count",1936122875),new cljs.core.Keyword(null,"autoload","autoload",-354122500),new cljs.core.Keyword(null,"url-rewriter","url-rewriter",200543838),new cljs.core.Keyword(null,"on-cssload","on-cssload",1825432318)],[true,figwheel.client.default_on_compile_warning,figwheel.client.default_on_jsload,figwheel.client.default_on_compile_fail,false,true,[cljs.core.str("ws://"),cljs.core.str((cljs.core.truth_(figwheel.client.utils.html_env_QMARK_.call(null))?location.host:"localhost:3449")),cljs.core.str("/figwheel-ws")].join(''),figwheel.client.default_before_load,false,(100),true,false,figwheel.client.default_on_cssload]);
}
figwheel.client.handle_deprecated_jsload_callback = (function figwheel$client$handle_deprecated_jsload_callback(config){
if(cljs.core.truth_(new cljs.core.Keyword(null,"jsload-callback","jsload-callback",-1949628369).cljs$core$IFn$_invoke$arity$1(config))){
return cljs.core.dissoc.call(null,cljs.core.assoc.call(null,config,new cljs.core.Keyword(null,"on-jsload","on-jsload",-395756602),new cljs.core.Keyword(null,"jsload-callback","jsload-callback",-1949628369).cljs$core$IFn$_invoke$arity$1(config)),new cljs.core.Keyword(null,"jsload-callback","jsload-callback",-1949628369));
} else {
return config;
}
});
figwheel.client.base_plugins = (function figwheel$client$base_plugins(system_options){
var base = new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"enforce-project-plugin","enforce-project-plugin",959402899),figwheel.client.enforce_project_plugin,new cljs.core.Keyword(null,"file-reloader-plugin","file-reloader-plugin",-1792964733),figwheel.client.file_reloader_plugin,new cljs.core.Keyword(null,"comp-fail-warning-plugin","comp-fail-warning-plugin",634311),figwheel.client.compile_fail_warning_plugin,new cljs.core.Keyword(null,"css-reloader-plugin","css-reloader-plugin",2002032904),figwheel.client.css_reloader_plugin,new cljs.core.Keyword(null,"repl-plugin","repl-plugin",-1138952371),figwheel.client.repl_plugin], null);
var base__$1 = ((cljs.core.not.call(null,figwheel.client.utils.html_env_QMARK_.call(null)))?cljs.core.select_keys.call(null,base,new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"file-reloader-plugin","file-reloader-plugin",-1792964733),new cljs.core.Keyword(null,"comp-fail-warning-plugin","comp-fail-warning-plugin",634311),new cljs.core.Keyword(null,"repl-plugin","repl-plugin",-1138952371)], null)):base);
var base__$2 = ((new cljs.core.Keyword(null,"autoload","autoload",-354122500).cljs$core$IFn$_invoke$arity$1(system_options) === false)?cljs.core.dissoc.call(null,base__$1,new cljs.core.Keyword(null,"file-reloader-plugin","file-reloader-plugin",-1792964733)):base__$1);
if(cljs.core.truth_((function (){var and__18061__auto__ = new cljs.core.Keyword(null,"heads-up-display","heads-up-display",-896577202).cljs$core$IFn$_invoke$arity$1(system_options);
if(cljs.core.truth_(and__18061__auto__)){
return figwheel.client.utils.html_env_QMARK_.call(null);
} else {
return and__18061__auto__;
}
})())){
return cljs.core.assoc.call(null,base__$2,new cljs.core.Keyword(null,"heads-up-display-plugin","heads-up-display-plugin",1745207501),figwheel.client.heads_up_plugin);
} else {
return base__$2;
}
});
figwheel.client.add_plugins = (function figwheel$client$add_plugins(plugins,system_options){
var seq__30867 = cljs.core.seq.call(null,plugins);
var chunk__30868 = null;
var count__30869 = (0);
var i__30870 = (0);
while(true){
if((i__30870 < count__30869)){
var vec__30871 = cljs.core._nth.call(null,chunk__30868,i__30870);
var k = cljs.core.nth.call(null,vec__30871,(0),null);
var plugin = cljs.core.nth.call(null,vec__30871,(1),null);
if(cljs.core.truth_(plugin)){
var pl_30873 = plugin.call(null,system_options);
cljs.core.add_watch.call(null,figwheel.client.socket.message_history_atom,k,((function (seq__30867,chunk__30868,count__30869,i__30870,pl_30873,vec__30871,k,plugin){
return (function (_,___$1,___$2,msg_hist){
return pl_30873.call(null,msg_hist);
});})(seq__30867,chunk__30868,count__30869,i__30870,pl_30873,vec__30871,k,plugin))
);
} else {
}

var G__30874 = seq__30867;
var G__30875 = chunk__30868;
var G__30876 = count__30869;
var G__30877 = (i__30870 + (1));
seq__30867 = G__30874;
chunk__30868 = G__30875;
count__30869 = G__30876;
i__30870 = G__30877;
continue;
} else {
var temp__4423__auto__ = cljs.core.seq.call(null,seq__30867);
if(temp__4423__auto__){
var seq__30867__$1 = temp__4423__auto__;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__30867__$1)){
var c__18858__auto__ = cljs.core.chunk_first.call(null,seq__30867__$1);
var G__30878 = cljs.core.chunk_rest.call(null,seq__30867__$1);
var G__30879 = c__18858__auto__;
var G__30880 = cljs.core.count.call(null,c__18858__auto__);
var G__30881 = (0);
seq__30867 = G__30878;
chunk__30868 = G__30879;
count__30869 = G__30880;
i__30870 = G__30881;
continue;
} else {
var vec__30872 = cljs.core.first.call(null,seq__30867__$1);
var k = cljs.core.nth.call(null,vec__30872,(0),null);
var plugin = cljs.core.nth.call(null,vec__30872,(1),null);
if(cljs.core.truth_(plugin)){
var pl_30882 = plugin.call(null,system_options);
cljs.core.add_watch.call(null,figwheel.client.socket.message_history_atom,k,((function (seq__30867,chunk__30868,count__30869,i__30870,pl_30882,vec__30872,k,plugin,seq__30867__$1,temp__4423__auto__){
return (function (_,___$1,___$2,msg_hist){
return pl_30882.call(null,msg_hist);
});})(seq__30867,chunk__30868,count__30869,i__30870,pl_30882,vec__30872,k,plugin,seq__30867__$1,temp__4423__auto__))
);
} else {
}

var G__30883 = cljs.core.next.call(null,seq__30867__$1);
var G__30884 = null;
var G__30885 = (0);
var G__30886 = (0);
seq__30867 = G__30883;
chunk__30868 = G__30884;
count__30869 = G__30885;
i__30870 = G__30886;
continue;
}
} else {
return null;
}
}
break;
}
});
figwheel.client.start = (function figwheel$client$start(){
var G__30888 = arguments.length;
switch (G__30888) {
case 1:
return figwheel.client.start.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
case 0:
return figwheel.client.start.cljs$core$IFn$_invoke$arity$0();

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

figwheel.client.start.cljs$core$IFn$_invoke$arity$1 = (function (opts){
if((goog.dependencies_ == null)){
return null;
} else {
if(typeof figwheel.client.__figwheel_start_once__ !== 'undefined'){
return null;
} else {
figwheel.client.__figwheel_start_once__ = setTimeout((function (){
var plugins_SINGLEQUOTE_ = new cljs.core.Keyword(null,"plugins","plugins",1900073717).cljs$core$IFn$_invoke$arity$1(opts);
var merge_plugins = new cljs.core.Keyword(null,"merge-plugins","merge-plugins",-1193912370).cljs$core$IFn$_invoke$arity$1(opts);
var system_options = figwheel.client.handle_deprecated_jsload_callback.call(null,cljs.core.merge.call(null,figwheel.client.config_defaults,cljs.core.dissoc.call(null,opts,new cljs.core.Keyword(null,"plugins","plugins",1900073717),new cljs.core.Keyword(null,"merge-plugins","merge-plugins",-1193912370))));
var plugins = (cljs.core.truth_(plugins_SINGLEQUOTE_)?plugins_SINGLEQUOTE_:cljs.core.merge.call(null,figwheel.client.base_plugins.call(null,system_options),merge_plugins));
figwheel.client.utils._STAR_print_debug_STAR_ = new cljs.core.Keyword(null,"debug","debug",-1608172596).cljs$core$IFn$_invoke$arity$1(opts);

figwheel.client.add_plugins.call(null,plugins,system_options);

figwheel.client.file_reloading.patch_goog_base.call(null);

return figwheel.client.socket.open.call(null,system_options);
}));
}
}
});

figwheel.client.start.cljs$core$IFn$_invoke$arity$0 = (function (){
return figwheel.client.start.call(null,cljs.core.PersistentArrayMap.EMPTY);
});

figwheel.client.start.cljs$lang$maxFixedArity = 1;
figwheel.client.watch_and_reload_with_opts = figwheel.client.start;
figwheel.client.watch_and_reload = (function figwheel$client$watch_and_reload(){
var argseq__19113__auto__ = ((((0) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(0)),(0))):null);
return figwheel.client.watch_and_reload.cljs$core$IFn$_invoke$arity$variadic(argseq__19113__auto__);
});

figwheel.client.watch_and_reload.cljs$core$IFn$_invoke$arity$variadic = (function (p__30891){
var map__30892 = p__30891;
var map__30892__$1 = ((cljs.core.seq_QMARK_.call(null,map__30892))?cljs.core.apply.call(null,cljs.core.hash_map,map__30892):map__30892);
var opts = map__30892__$1;
return figwheel.client.start.call(null,opts);
});

figwheel.client.watch_and_reload.cljs$lang$maxFixedArity = (0);

figwheel.client.watch_and_reload.cljs$lang$applyTo = (function (seq30890){
return figwheel.client.watch_and_reload.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq30890));
});

//# sourceMappingURL=client.js.map?rel=1431620936046