// Compiled by ClojureScript 0.0-3269 {}
goog.provide('figwheel.client.heads_up');
goog.require('cljs.core');
goog.require('clojure.string');
goog.require('figwheel.client.socket');
goog.require('cljs.core.async');

figwheel.client.heads_up.node = (function figwheel$client$heads_up$node(){
var argseq__19113__auto__ = ((((2) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(2)),(0))):null);
return figwheel.client.heads_up.node.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),argseq__19113__auto__);
});

figwheel.client.heads_up.node.cljs$core$IFn$_invoke$arity$variadic = (function (t,attrs,children){
var e = document.createElement(cljs.core.name.call(null,t));
var seq__31019_31027 = cljs.core.seq.call(null,cljs.core.keys.call(null,attrs));
var chunk__31020_31028 = null;
var count__31021_31029 = (0);
var i__31022_31030 = (0);
while(true){
if((i__31022_31030 < count__31021_31029)){
var k_31031 = cljs.core._nth.call(null,chunk__31020_31028,i__31022_31030);
e.setAttribute(cljs.core.name.call(null,k_31031),cljs.core.get.call(null,attrs,k_31031));

var G__31032 = seq__31019_31027;
var G__31033 = chunk__31020_31028;
var G__31034 = count__31021_31029;
var G__31035 = (i__31022_31030 + (1));
seq__31019_31027 = G__31032;
chunk__31020_31028 = G__31033;
count__31021_31029 = G__31034;
i__31022_31030 = G__31035;
continue;
} else {
var temp__4423__auto___31036 = cljs.core.seq.call(null,seq__31019_31027);
if(temp__4423__auto___31036){
var seq__31019_31037__$1 = temp__4423__auto___31036;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__31019_31037__$1)){
var c__18858__auto___31038 = cljs.core.chunk_first.call(null,seq__31019_31037__$1);
var G__31039 = cljs.core.chunk_rest.call(null,seq__31019_31037__$1);
var G__31040 = c__18858__auto___31038;
var G__31041 = cljs.core.count.call(null,c__18858__auto___31038);
var G__31042 = (0);
seq__31019_31027 = G__31039;
chunk__31020_31028 = G__31040;
count__31021_31029 = G__31041;
i__31022_31030 = G__31042;
continue;
} else {
var k_31043 = cljs.core.first.call(null,seq__31019_31037__$1);
e.setAttribute(cljs.core.name.call(null,k_31043),cljs.core.get.call(null,attrs,k_31043));

var G__31044 = cljs.core.next.call(null,seq__31019_31037__$1);
var G__31045 = null;
var G__31046 = (0);
var G__31047 = (0);
seq__31019_31027 = G__31044;
chunk__31020_31028 = G__31045;
count__31021_31029 = G__31046;
i__31022_31030 = G__31047;
continue;
}
} else {
}
}
break;
}

var seq__31023_31048 = cljs.core.seq.call(null,children);
var chunk__31024_31049 = null;
var count__31025_31050 = (0);
var i__31026_31051 = (0);
while(true){
if((i__31026_31051 < count__31025_31050)){
var ch_31052 = cljs.core._nth.call(null,chunk__31024_31049,i__31026_31051);
e.appendChild(ch_31052);

var G__31053 = seq__31023_31048;
var G__31054 = chunk__31024_31049;
var G__31055 = count__31025_31050;
var G__31056 = (i__31026_31051 + (1));
seq__31023_31048 = G__31053;
chunk__31024_31049 = G__31054;
count__31025_31050 = G__31055;
i__31026_31051 = G__31056;
continue;
} else {
var temp__4423__auto___31057 = cljs.core.seq.call(null,seq__31023_31048);
if(temp__4423__auto___31057){
var seq__31023_31058__$1 = temp__4423__auto___31057;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__31023_31058__$1)){
var c__18858__auto___31059 = cljs.core.chunk_first.call(null,seq__31023_31058__$1);
var G__31060 = cljs.core.chunk_rest.call(null,seq__31023_31058__$1);
var G__31061 = c__18858__auto___31059;
var G__31062 = cljs.core.count.call(null,c__18858__auto___31059);
var G__31063 = (0);
seq__31023_31048 = G__31060;
chunk__31024_31049 = G__31061;
count__31025_31050 = G__31062;
i__31026_31051 = G__31063;
continue;
} else {
var ch_31064 = cljs.core.first.call(null,seq__31023_31058__$1);
e.appendChild(ch_31064);

var G__31065 = cljs.core.next.call(null,seq__31023_31058__$1);
var G__31066 = null;
var G__31067 = (0);
var G__31068 = (0);
seq__31023_31048 = G__31065;
chunk__31024_31049 = G__31066;
count__31025_31050 = G__31067;
i__31026_31051 = G__31068;
continue;
}
} else {
}
}
break;
}

return e;
});

figwheel.client.heads_up.node.cljs$lang$maxFixedArity = (2);

figwheel.client.heads_up.node.cljs$lang$applyTo = (function (seq31016){
var G__31017 = cljs.core.first.call(null,seq31016);
var seq31016__$1 = cljs.core.next.call(null,seq31016);
var G__31018 = cljs.core.first.call(null,seq31016__$1);
var seq31016__$2 = cljs.core.next.call(null,seq31016__$1);
return figwheel.client.heads_up.node.cljs$core$IFn$_invoke$arity$variadic(G__31017,G__31018,seq31016__$2);
});
if(typeof figwheel.client.heads_up.heads_up_event_dispatch !== 'undefined'){
} else {
figwheel.client.heads_up.heads_up_event_dispatch = (function (){var method_table__18968__auto__ = cljs.core.atom.call(null,cljs.core.PersistentArrayMap.EMPTY);
var prefer_table__18969__auto__ = cljs.core.atom.call(null,cljs.core.PersistentArrayMap.EMPTY);
var method_cache__18970__auto__ = cljs.core.atom.call(null,cljs.core.PersistentArrayMap.EMPTY);
var cached_hierarchy__18971__auto__ = cljs.core.atom.call(null,cljs.core.PersistentArrayMap.EMPTY);
var hierarchy__18972__auto__ = cljs.core.get.call(null,cljs.core.PersistentArrayMap.EMPTY,new cljs.core.Keyword(null,"hierarchy","hierarchy",-1053470341),cljs.core.get_global_hierarchy.call(null));
return (new cljs.core.MultiFn(cljs.core.symbol.call(null,"figwheel.client.heads-up","heads-up-event-dispatch"),((function (method_table__18968__auto__,prefer_table__18969__auto__,method_cache__18970__auto__,cached_hierarchy__18971__auto__,hierarchy__18972__auto__){
return (function (dataset){
return dataset.figwheelEvent;
});})(method_table__18968__auto__,prefer_table__18969__auto__,method_cache__18970__auto__,cached_hierarchy__18971__auto__,hierarchy__18972__auto__))
,new cljs.core.Keyword(null,"default","default",-1987822328),hierarchy__18972__auto__,method_table__18968__auto__,prefer_table__18969__auto__,method_cache__18970__auto__,cached_hierarchy__18971__auto__));
})();
}
cljs.core._add_method.call(null,figwheel.client.heads_up.heads_up_event_dispatch,new cljs.core.Keyword(null,"default","default",-1987822328),(function (_){
return cljs.core.PersistentArrayMap.EMPTY;
}));
cljs.core._add_method.call(null,figwheel.client.heads_up.heads_up_event_dispatch,"file-selected",(function (dataset){
return figwheel.client.socket.send_BANG_.call(null,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"figwheel-event","figwheel-event",519570592),"file-selected",new cljs.core.Keyword(null,"file-name","file-name",-1654217259),dataset.fileName,new cljs.core.Keyword(null,"file-line","file-line",-1228823138),dataset.fileLine], null));
}));
cljs.core._add_method.call(null,figwheel.client.heads_up.heads_up_event_dispatch,"close-heads-up",(function (dataset){
return figwheel.client.heads_up.clear.call(null);
}));
figwheel.client.heads_up.ancestor_nodes = (function figwheel$client$heads_up$ancestor_nodes(el){
return cljs.core.iterate.call(null,(function (e){
return e.parentNode;
}),el);
});
figwheel.client.heads_up.get_dataset = (function figwheel$client$heads_up$get_dataset(el){
return cljs.core.first.call(null,cljs.core.keep.call(null,(function (x){
if(cljs.core.truth_(x.dataset.figwheelEvent)){
return x.dataset;
} else {
return null;
}
}),cljs.core.take.call(null,(4),figwheel.client.heads_up.ancestor_nodes.call(null,el))));
});
figwheel.client.heads_up.heads_up_onclick_handler = (function figwheel$client$heads_up$heads_up_onclick_handler(event){
var dataset = figwheel.client.heads_up.get_dataset.call(null,event.target);
event.preventDefault();

if(cljs.core.truth_(dataset)){
return figwheel.client.heads_up.heads_up_event_dispatch.call(null,dataset);
} else {
return null;
}
});
figwheel.client.heads_up.ensure_container = (function figwheel$client$heads_up$ensure_container(){
var cont_id = "figwheel-heads-up-container";
var content_id = "figwheel-heads-up-content-area";
if(cljs.core.not.call(null,document.querySelector([cljs.core.str("#"),cljs.core.str(cont_id)].join('')))){
var el_31069 = figwheel.client.heads_up.node.call(null,new cljs.core.Keyword(null,"div","div",1057191632),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"id","id",-1388402092),cont_id,new cljs.core.Keyword(null,"style","style",-496642736),[cljs.core.str("-webkit-transition: all 0.2s ease-in-out;"),cljs.core.str("-moz-transition: all 0.2s ease-in-out;"),cljs.core.str("-o-transition: all 0.2s ease-in-out;"),cljs.core.str("transition: all 0.2s ease-in-out;"),cljs.core.str("font-size: 13px;"),cljs.core.str("border-top: 1px solid #f5f5f5;"),cljs.core.str("box-shadow: 0px 0px 1px #aaaaaa;"),cljs.core.str("line-height: 18px;"),cljs.core.str("color: #333;"),cljs.core.str("font-family: monospace;"),cljs.core.str("padding: 0px 10px 0px 70px;"),cljs.core.str("position: fixed;"),cljs.core.str("bottom: 0px;"),cljs.core.str("left: 0px;"),cljs.core.str("height: 0px;"),cljs.core.str("opacity: 0.0;"),cljs.core.str("box-sizing: border-box;"),cljs.core.str("z-index: 10000;")].join('')], null));
el_31069.onclick = figwheel.client.heads_up.heads_up_onclick_handler;

el_31069.innerHTML = [cljs.core.str(figwheel.client.heads_up.clojure_symbol_svg)].join('');

el_31069.appendChild(figwheel.client.heads_up.node.call(null,new cljs.core.Keyword(null,"div","div",1057191632),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"id","id",-1388402092),content_id], null)));

document.body.appendChild(el_31069);
} else {
}

return new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"container-el","container-el",109664205),document.getElementById(cont_id),new cljs.core.Keyword(null,"content-area-el","content-area-el",742757187),document.getElementById(content_id)], null);
});
figwheel.client.heads_up.set_style_BANG_ = (function figwheel$client$heads_up$set_style_BANG_(p__31070,st_map){
var map__31074 = p__31070;
var map__31074__$1 = ((cljs.core.seq_QMARK_.call(null,map__31074))?cljs.core.apply.call(null,cljs.core.hash_map,map__31074):map__31074);
var container_el = cljs.core.get.call(null,map__31074__$1,new cljs.core.Keyword(null,"container-el","container-el",109664205));
return cljs.core.mapv.call(null,((function (map__31074,map__31074__$1,container_el){
return (function (p__31075){
var vec__31076 = p__31075;
var k = cljs.core.nth.call(null,vec__31076,(0),null);
var v = cljs.core.nth.call(null,vec__31076,(1),null);
return (container_el.style[cljs.core.name.call(null,k)] = v);
});})(map__31074,map__31074__$1,container_el))
,st_map);
});
figwheel.client.heads_up.set_content_BANG_ = (function figwheel$client$heads_up$set_content_BANG_(p__31077,dom_str){
var map__31079 = p__31077;
var map__31079__$1 = ((cljs.core.seq_QMARK_.call(null,map__31079))?cljs.core.apply.call(null,cljs.core.hash_map,map__31079):map__31079);
var c = map__31079__$1;
var content_area_el = cljs.core.get.call(null,map__31079__$1,new cljs.core.Keyword(null,"content-area-el","content-area-el",742757187));
return content_area_el.innerHTML = dom_str;
});
figwheel.client.heads_up.get_content = (function figwheel$client$heads_up$get_content(p__31080){
var map__31082 = p__31080;
var map__31082__$1 = ((cljs.core.seq_QMARK_.call(null,map__31082))?cljs.core.apply.call(null,cljs.core.hash_map,map__31082):map__31082);
var content_area_el = cljs.core.get.call(null,map__31082__$1,new cljs.core.Keyword(null,"content-area-el","content-area-el",742757187));
return content_area_el.innerHTML;
});
figwheel.client.heads_up.close_link = (function figwheel$client$heads_up$close_link(){
return [cljs.core.str("<a style=\""),cljs.core.str("float: right;"),cljs.core.str("font-size: 18px;"),cljs.core.str("text-decoration: none;"),cljs.core.str("text-align: right;"),cljs.core.str("width: 30px;"),cljs.core.str("height: 30px;"),cljs.core.str("color: rgba(84,84,84, 0.5);"),cljs.core.str("\" href=\"#\"  data-figwheel-event=\"close-heads-up\">"),cljs.core.str("x"),cljs.core.str("</a>")].join('');
});
figwheel.client.heads_up.display_heads_up = (function figwheel$client$heads_up$display_heads_up(style,msg){
var c__20932__auto__ = cljs.core.async.chan.call(null,(1));
cljs.core.async.impl.dispatch.run.call(null,((function (c__20932__auto__){
return (function (){
var f__20933__auto__ = (function (){var switch__20870__auto__ = ((function (c__20932__auto__){
return (function (state_31124){
var state_val_31125 = (state_31124[(1)]);
if((state_val_31125 === (1))){
var inst_31109 = (state_31124[(7)]);
var inst_31109__$1 = figwheel.client.heads_up.ensure_container.call(null);
var inst_31110 = [new cljs.core.Keyword(null,"paddingTop","paddingTop",-1088692345),new cljs.core.Keyword(null,"paddingBottom","paddingBottom",-916694489),new cljs.core.Keyword(null,"width","width",-384071477),new cljs.core.Keyword(null,"minHeight","minHeight",-1635998980),new cljs.core.Keyword(null,"opacity","opacity",397153780)];
var inst_31111 = ["10px","10px","100%","68px","1.0"];
var inst_31112 = cljs.core.PersistentHashMap.fromArrays(inst_31110,inst_31111);
var inst_31113 = cljs.core.merge.call(null,inst_31112,style);
var inst_31114 = figwheel.client.heads_up.set_style_BANG_.call(null,inst_31109__$1,inst_31113);
var inst_31115 = figwheel.client.heads_up.set_content_BANG_.call(null,inst_31109__$1,msg);
var inst_31116 = cljs.core.async.timeout.call(null,(300));
var state_31124__$1 = (function (){var statearr_31126 = state_31124;
(statearr_31126[(8)] = inst_31115);

(statearr_31126[(7)] = inst_31109__$1);

(statearr_31126[(9)] = inst_31114);

return statearr_31126;
})();
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_31124__$1,(2),inst_31116);
} else {
if((state_val_31125 === (2))){
var inst_31109 = (state_31124[(7)]);
var inst_31118 = (state_31124[(2)]);
var inst_31119 = [new cljs.core.Keyword(null,"height","height",1025178622)];
var inst_31120 = ["auto"];
var inst_31121 = cljs.core.PersistentHashMap.fromArrays(inst_31119,inst_31120);
var inst_31122 = figwheel.client.heads_up.set_style_BANG_.call(null,inst_31109,inst_31121);
var state_31124__$1 = (function (){var statearr_31127 = state_31124;
(statearr_31127[(10)] = inst_31118);

return statearr_31127;
})();
return cljs.core.async.impl.ioc_helpers.return_chan.call(null,state_31124__$1,inst_31122);
} else {
return null;
}
}
});})(c__20932__auto__))
;
return ((function (switch__20870__auto__,c__20932__auto__){
return (function() {
var figwheel$client$heads_up$display_heads_up_$_state_machine__20871__auto__ = null;
var figwheel$client$heads_up$display_heads_up_$_state_machine__20871__auto____0 = (function (){
var statearr_31131 = [null,null,null,null,null,null,null,null,null,null,null];
(statearr_31131[(0)] = figwheel$client$heads_up$display_heads_up_$_state_machine__20871__auto__);

(statearr_31131[(1)] = (1));

return statearr_31131;
});
var figwheel$client$heads_up$display_heads_up_$_state_machine__20871__auto____1 = (function (state_31124){
while(true){
var ret_value__20872__auto__ = (function (){try{while(true){
var result__20873__auto__ = switch__20870__auto__.call(null,state_31124);
if(cljs.core.keyword_identical_QMARK_.call(null,result__20873__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__20873__auto__;
}
break;
}
}catch (e31132){if((e31132 instanceof Object)){
var ex__20874__auto__ = e31132;
var statearr_31133_31135 = state_31124;
(statearr_31133_31135[(5)] = ex__20874__auto__);


cljs.core.async.impl.ioc_helpers.process_exception.call(null,state_31124);

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
throw e31132;

}
}})();
if(cljs.core.keyword_identical_QMARK_.call(null,ret_value__20872__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__31136 = state_31124;
state_31124 = G__31136;
continue;
} else {
return ret_value__20872__auto__;
}
break;
}
});
figwheel$client$heads_up$display_heads_up_$_state_machine__20871__auto__ = function(state_31124){
switch(arguments.length){
case 0:
return figwheel$client$heads_up$display_heads_up_$_state_machine__20871__auto____0.call(this);
case 1:
return figwheel$client$heads_up$display_heads_up_$_state_machine__20871__auto____1.call(this,state_31124);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
figwheel$client$heads_up$display_heads_up_$_state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$0 = figwheel$client$heads_up$display_heads_up_$_state_machine__20871__auto____0;
figwheel$client$heads_up$display_heads_up_$_state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$1 = figwheel$client$heads_up$display_heads_up_$_state_machine__20871__auto____1;
return figwheel$client$heads_up$display_heads_up_$_state_machine__20871__auto__;
})()
;})(switch__20870__auto__,c__20932__auto__))
})();
var state__20934__auto__ = (function (){var statearr_31134 = f__20933__auto__.call(null);
(statearr_31134[cljs.core.async.impl.ioc_helpers.USER_START_IDX] = c__20932__auto__);

return statearr_31134;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped.call(null,state__20934__auto__);
});})(c__20932__auto__))
);

return c__20932__auto__;
});
figwheel.client.heads_up.heading = (function figwheel$client$heads_up$heading(s){
return [cljs.core.str("<div style=\""),cljs.core.str("font-size: 26px;"),cljs.core.str("line-height: 26px;"),cljs.core.str("margin-bottom: 2px;"),cljs.core.str("padding-top: 1px;"),cljs.core.str("\">"),cljs.core.str(s),cljs.core.str("</div>")].join('');
});
figwheel.client.heads_up.file_and_line_number = (function figwheel$client$heads_up$file_and_line_number(msg){
if(cljs.core.truth_(cljs.core.re_matches.call(null,/.*at\sline.*/,msg))){
return cljs.core.take.call(null,(2),cljs.core.reverse.call(null,clojure.string.split.call(null,msg," ")));
} else {
return null;
}
});
figwheel.client.heads_up.file_selector_div = (function figwheel$client$heads_up$file_selector_div(file_name,line_number,msg){
return [cljs.core.str("<div data-figwheel-event=\"file-selected\" data-file-name=\""),cljs.core.str(file_name),cljs.core.str("\" data-file-line=\""),cljs.core.str(line_number),cljs.core.str("\">"),cljs.core.str(msg),cljs.core.str("</div>")].join('');
});
figwheel.client.heads_up.format_line = (function figwheel$client$heads_up$format_line(msg){
var temp__4421__auto__ = figwheel.client.heads_up.file_and_line_number.call(null,msg);
if(cljs.core.truth_(temp__4421__auto__)){
var vec__31138 = temp__4421__auto__;
var f = cljs.core.nth.call(null,vec__31138,(0),null);
var ln = cljs.core.nth.call(null,vec__31138,(1),null);
return figwheel.client.heads_up.file_selector_div.call(null,f,ln,msg);
} else {
return [cljs.core.str("<div>"),cljs.core.str(msg),cljs.core.str("</div>")].join('');
}
});
figwheel.client.heads_up.display_error = (function figwheel$client$heads_up$display_error(formatted_messages,cause){
var vec__31141 = (cljs.core.truth_(cause)?new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"file","file",-1269645878).cljs$core$IFn$_invoke$arity$1(cause),new cljs.core.Keyword(null,"line","line",212345235).cljs$core$IFn$_invoke$arity$1(cause),new cljs.core.Keyword(null,"column","column",2078222095).cljs$core$IFn$_invoke$arity$1(cause)], null):cljs.core.first.call(null,cljs.core.keep.call(null,figwheel.client.heads_up.file_and_line_number,formatted_messages)));
var file_name = cljs.core.nth.call(null,vec__31141,(0),null);
var file_line = cljs.core.nth.call(null,vec__31141,(1),null);
var file_column = cljs.core.nth.call(null,vec__31141,(2),null);
var msg = cljs.core.apply.call(null,cljs.core.str,cljs.core.map.call(null,((function (vec__31141,file_name,file_line,file_column){
return (function (p1__31139_SHARP_){
return [cljs.core.str("<div>"),cljs.core.str(p1__31139_SHARP_),cljs.core.str("</div>")].join('');
});})(vec__31141,file_name,file_line,file_column))
,formatted_messages));
return figwheel.client.heads_up.display_heads_up.call(null,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"backgroundColor","backgroundColor",1738438491),"rgba(255, 161, 161, 0.95)"], null),[cljs.core.str(figwheel.client.heads_up.close_link.call(null)),cljs.core.str(figwheel.client.heads_up.heading.call(null,"Compile Error")),cljs.core.str(figwheel.client.heads_up.file_selector_div.call(null,file_name,(function (){var or__18073__auto__ = file_line;
if(cljs.core.truth_(or__18073__auto__)){
return or__18073__auto__;
} else {
var and__18061__auto__ = cause;
if(cljs.core.truth_(and__18061__auto__)){
return new cljs.core.Keyword(null,"line","line",212345235).cljs$core$IFn$_invoke$arity$1(cause);
} else {
return and__18061__auto__;
}
}
})(),[cljs.core.str(msg),cljs.core.str((cljs.core.truth_(cause)?[cljs.core.str("Error on file "),cljs.core.str(new cljs.core.Keyword(null,"file","file",-1269645878).cljs$core$IFn$_invoke$arity$1(cause)),cljs.core.str(", line "),cljs.core.str(new cljs.core.Keyword(null,"line","line",212345235).cljs$core$IFn$_invoke$arity$1(cause)),cljs.core.str(", column "),cljs.core.str(new cljs.core.Keyword(null,"column","column",2078222095).cljs$core$IFn$_invoke$arity$1(cause))].join(''):""))].join('')))].join(''));
});
figwheel.client.heads_up.display_system_warning = (function figwheel$client$heads_up$display_system_warning(header,msg){
return figwheel.client.heads_up.display_heads_up.call(null,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"backgroundColor","backgroundColor",1738438491),"rgba(255, 220, 110, 0.95)"], null),[cljs.core.str(figwheel.client.heads_up.close_link.call(null)),cljs.core.str(figwheel.client.heads_up.heading.call(null,header)),cljs.core.str(figwheel.client.heads_up.format_line.call(null,msg))].join(''));
});
figwheel.client.heads_up.display_warning = (function figwheel$client$heads_up$display_warning(msg){
return figwheel.client.heads_up.display_system_warning.call(null,"Compile Warning",msg);
});
figwheel.client.heads_up.append_message = (function figwheel$client$heads_up$append_message(message){
var map__31143 = figwheel.client.heads_up.ensure_container.call(null);
var map__31143__$1 = ((cljs.core.seq_QMARK_.call(null,map__31143))?cljs.core.apply.call(null,cljs.core.hash_map,map__31143):map__31143);
var content_area_el = cljs.core.get.call(null,map__31143__$1,new cljs.core.Keyword(null,"content-area-el","content-area-el",742757187));
var el = document.createElement("div");
el.innerHTML = figwheel.client.heads_up.format_line.call(null,message);

return content_area_el.appendChild(el);
});
figwheel.client.heads_up.clear = (function figwheel$client$heads_up$clear(){
var c__20932__auto__ = cljs.core.async.chan.call(null,(1));
cljs.core.async.impl.dispatch.run.call(null,((function (c__20932__auto__){
return (function (){
var f__20933__auto__ = (function (){var switch__20870__auto__ = ((function (c__20932__auto__){
return (function (state_31190){
var state_val_31191 = (state_31190[(1)]);
if((state_val_31191 === (1))){
var inst_31173 = (state_31190[(7)]);
var inst_31173__$1 = figwheel.client.heads_up.ensure_container.call(null);
var inst_31174 = [new cljs.core.Keyword(null,"opacity","opacity",397153780)];
var inst_31175 = ["0.0"];
var inst_31176 = cljs.core.PersistentHashMap.fromArrays(inst_31174,inst_31175);
var inst_31177 = figwheel.client.heads_up.set_style_BANG_.call(null,inst_31173__$1,inst_31176);
var inst_31178 = cljs.core.async.timeout.call(null,(300));
var state_31190__$1 = (function (){var statearr_31192 = state_31190;
(statearr_31192[(8)] = inst_31177);

(statearr_31192[(7)] = inst_31173__$1);

return statearr_31192;
})();
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_31190__$1,(2),inst_31178);
} else {
if((state_val_31191 === (2))){
var inst_31173 = (state_31190[(7)]);
var inst_31180 = (state_31190[(2)]);
var inst_31181 = [new cljs.core.Keyword(null,"width","width",-384071477),new cljs.core.Keyword(null,"height","height",1025178622),new cljs.core.Keyword(null,"minHeight","minHeight",-1635998980),new cljs.core.Keyword(null,"padding","padding",1660304693),new cljs.core.Keyword(null,"borderRadius","borderRadius",-1505621083),new cljs.core.Keyword(null,"backgroundColor","backgroundColor",1738438491)];
var inst_31182 = ["auto","0px","0px","0px 10px 0px 70px","0px","transparent"];
var inst_31183 = cljs.core.PersistentHashMap.fromArrays(inst_31181,inst_31182);
var inst_31184 = figwheel.client.heads_up.set_style_BANG_.call(null,inst_31173,inst_31183);
var inst_31185 = cljs.core.async.timeout.call(null,(200));
var state_31190__$1 = (function (){var statearr_31193 = state_31190;
(statearr_31193[(9)] = inst_31184);

(statearr_31193[(10)] = inst_31180);

return statearr_31193;
})();
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_31190__$1,(3),inst_31185);
} else {
if((state_val_31191 === (3))){
var inst_31173 = (state_31190[(7)]);
var inst_31187 = (state_31190[(2)]);
var inst_31188 = figwheel.client.heads_up.set_content_BANG_.call(null,inst_31173,"");
var state_31190__$1 = (function (){var statearr_31194 = state_31190;
(statearr_31194[(11)] = inst_31187);

return statearr_31194;
})();
return cljs.core.async.impl.ioc_helpers.return_chan.call(null,state_31190__$1,inst_31188);
} else {
return null;
}
}
}
});})(c__20932__auto__))
;
return ((function (switch__20870__auto__,c__20932__auto__){
return (function() {
var figwheel$client$heads_up$clear_$_state_machine__20871__auto__ = null;
var figwheel$client$heads_up$clear_$_state_machine__20871__auto____0 = (function (){
var statearr_31198 = [null,null,null,null,null,null,null,null,null,null,null,null];
(statearr_31198[(0)] = figwheel$client$heads_up$clear_$_state_machine__20871__auto__);

(statearr_31198[(1)] = (1));

return statearr_31198;
});
var figwheel$client$heads_up$clear_$_state_machine__20871__auto____1 = (function (state_31190){
while(true){
var ret_value__20872__auto__ = (function (){try{while(true){
var result__20873__auto__ = switch__20870__auto__.call(null,state_31190);
if(cljs.core.keyword_identical_QMARK_.call(null,result__20873__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__20873__auto__;
}
break;
}
}catch (e31199){if((e31199 instanceof Object)){
var ex__20874__auto__ = e31199;
var statearr_31200_31202 = state_31190;
(statearr_31200_31202[(5)] = ex__20874__auto__);


cljs.core.async.impl.ioc_helpers.process_exception.call(null,state_31190);

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
throw e31199;

}
}})();
if(cljs.core.keyword_identical_QMARK_.call(null,ret_value__20872__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__31203 = state_31190;
state_31190 = G__31203;
continue;
} else {
return ret_value__20872__auto__;
}
break;
}
});
figwheel$client$heads_up$clear_$_state_machine__20871__auto__ = function(state_31190){
switch(arguments.length){
case 0:
return figwheel$client$heads_up$clear_$_state_machine__20871__auto____0.call(this);
case 1:
return figwheel$client$heads_up$clear_$_state_machine__20871__auto____1.call(this,state_31190);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
figwheel$client$heads_up$clear_$_state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$0 = figwheel$client$heads_up$clear_$_state_machine__20871__auto____0;
figwheel$client$heads_up$clear_$_state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$1 = figwheel$client$heads_up$clear_$_state_machine__20871__auto____1;
return figwheel$client$heads_up$clear_$_state_machine__20871__auto__;
})()
;})(switch__20870__auto__,c__20932__auto__))
})();
var state__20934__auto__ = (function (){var statearr_31201 = f__20933__auto__.call(null);
(statearr_31201[cljs.core.async.impl.ioc_helpers.USER_START_IDX] = c__20932__auto__);

return statearr_31201;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped.call(null,state__20934__auto__);
});})(c__20932__auto__))
);

return c__20932__auto__;
});
figwheel.client.heads_up.display_loaded_start = (function figwheel$client$heads_up$display_loaded_start(){
return figwheel.client.heads_up.display_heads_up.call(null,new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null,"backgroundColor","backgroundColor",1738438491),"rgba(211,234,172,1.0)",new cljs.core.Keyword(null,"width","width",-384071477),"68px",new cljs.core.Keyword(null,"height","height",1025178622),"68px",new cljs.core.Keyword(null,"paddingLeft","paddingLeft",262720813),"0px",new cljs.core.Keyword(null,"paddingRight","paddingRight",-1642313463),"0px",new cljs.core.Keyword(null,"borderRadius","borderRadius",-1505621083),"35px"], null),"");
});
figwheel.client.heads_up.flash_loaded = (function figwheel$client$heads_up$flash_loaded(){
var c__20932__auto__ = cljs.core.async.chan.call(null,(1));
cljs.core.async.impl.dispatch.run.call(null,((function (c__20932__auto__){
return (function (){
var f__20933__auto__ = (function (){var switch__20870__auto__ = ((function (c__20932__auto__){
return (function (state_31235){
var state_val_31236 = (state_31235[(1)]);
if((state_val_31236 === (1))){
var inst_31225 = figwheel.client.heads_up.display_loaded_start.call(null);
var state_31235__$1 = state_31235;
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_31235__$1,(2),inst_31225);
} else {
if((state_val_31236 === (2))){
var inst_31227 = (state_31235[(2)]);
var inst_31228 = cljs.core.async.timeout.call(null,(400));
var state_31235__$1 = (function (){var statearr_31237 = state_31235;
(statearr_31237[(7)] = inst_31227);

return statearr_31237;
})();
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_31235__$1,(3),inst_31228);
} else {
if((state_val_31236 === (3))){
var inst_31230 = (state_31235[(2)]);
var inst_31231 = figwheel.client.heads_up.clear.call(null);
var state_31235__$1 = (function (){var statearr_31238 = state_31235;
(statearr_31238[(8)] = inst_31230);

return statearr_31238;
})();
return cljs.core.async.impl.ioc_helpers.take_BANG_.call(null,state_31235__$1,(4),inst_31231);
} else {
if((state_val_31236 === (4))){
var inst_31233 = (state_31235[(2)]);
var state_31235__$1 = state_31235;
return cljs.core.async.impl.ioc_helpers.return_chan.call(null,state_31235__$1,inst_31233);
} else {
return null;
}
}
}
}
});})(c__20932__auto__))
;
return ((function (switch__20870__auto__,c__20932__auto__){
return (function() {
var figwheel$client$heads_up$flash_loaded_$_state_machine__20871__auto__ = null;
var figwheel$client$heads_up$flash_loaded_$_state_machine__20871__auto____0 = (function (){
var statearr_31242 = [null,null,null,null,null,null,null,null,null];
(statearr_31242[(0)] = figwheel$client$heads_up$flash_loaded_$_state_machine__20871__auto__);

(statearr_31242[(1)] = (1));

return statearr_31242;
});
var figwheel$client$heads_up$flash_loaded_$_state_machine__20871__auto____1 = (function (state_31235){
while(true){
var ret_value__20872__auto__ = (function (){try{while(true){
var result__20873__auto__ = switch__20870__auto__.call(null,state_31235);
if(cljs.core.keyword_identical_QMARK_.call(null,result__20873__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
continue;
} else {
return result__20873__auto__;
}
break;
}
}catch (e31243){if((e31243 instanceof Object)){
var ex__20874__auto__ = e31243;
var statearr_31244_31246 = state_31235;
(statearr_31244_31246[(5)] = ex__20874__auto__);


cljs.core.async.impl.ioc_helpers.process_exception.call(null,state_31235);

return new cljs.core.Keyword(null,"recur","recur",-437573268);
} else {
throw e31243;

}
}})();
if(cljs.core.keyword_identical_QMARK_.call(null,ret_value__20872__auto__,new cljs.core.Keyword(null,"recur","recur",-437573268))){
var G__31247 = state_31235;
state_31235 = G__31247;
continue;
} else {
return ret_value__20872__auto__;
}
break;
}
});
figwheel$client$heads_up$flash_loaded_$_state_machine__20871__auto__ = function(state_31235){
switch(arguments.length){
case 0:
return figwheel$client$heads_up$flash_loaded_$_state_machine__20871__auto____0.call(this);
case 1:
return figwheel$client$heads_up$flash_loaded_$_state_machine__20871__auto____1.call(this,state_31235);
}
throw(new Error('Invalid arity: ' + arguments.length));
};
figwheel$client$heads_up$flash_loaded_$_state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$0 = figwheel$client$heads_up$flash_loaded_$_state_machine__20871__auto____0;
figwheel$client$heads_up$flash_loaded_$_state_machine__20871__auto__.cljs$core$IFn$_invoke$arity$1 = figwheel$client$heads_up$flash_loaded_$_state_machine__20871__auto____1;
return figwheel$client$heads_up$flash_loaded_$_state_machine__20871__auto__;
})()
;})(switch__20870__auto__,c__20932__auto__))
})();
var state__20934__auto__ = (function (){var statearr_31245 = f__20933__auto__.call(null);
(statearr_31245[cljs.core.async.impl.ioc_helpers.USER_START_IDX] = c__20932__auto__);

return statearr_31245;
})();
return cljs.core.async.impl.ioc_helpers.run_state_machine_wrapped.call(null,state__20934__auto__);
});})(c__20932__auto__))
);

return c__20932__auto__;
});
figwheel.client.heads_up.clojure_symbol_svg = "<?xml version='1.0' encoding='UTF-8' ?>\n<!DOCTYPE svg PUBLIC '-//W3C//DTD SVG 1.1//EN' 'http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd'>\n<svg width='49px' height='49px' viewBox='0 0 100 99' version='1.1' xmlns='http://www.w3.org/2000/svg' style='position:absolute; top:9px; left: 10px;'>\n<circle fill='rgba(255,255,255,0.5)' cx='49.75' cy='49.5' r='48.5'/>\n<path fill='#5881d8' d=' M 39.30 6.22 C 51.71 3.11 65.45 5.64 75.83 13.16 C 88.68 22.10 96.12 38.22 94.43 53.80 C 93.66 60.11 89.40 66.01 83.37 68.24 C 79.21 69.97 74.64 69.78 70.23 69.80 C 80.77 59.67 81.41 41.33 71.45 30.60 C 63.60 21.32 49.75 18.52 38.65 23.16 C 31.27 18.80 21.83 18.68 14.27 22.69 C 20.65 14.79 29.32 8.56 39.30 6.22 Z' />\n<path fill='#90b4fe' d=' M 42.93 26.99 C 48.49 25.50 54.55 25.62 59.79 28.14 C 68.71 32.19 74.61 42.14 73.41 51.94 C 72.85 58.64 68.92 64.53 63.81 68.69 C 59.57 66.71 57.53 62.30 55.66 58.30 C 50.76 48.12 50.23 36.02 42.93 26.99 Z' />\n<path fill='#63b132' d=' M 12.30 33.30 C 17.11 28.49 24.33 26.90 30.91 28.06 C 25.22 33.49 21.44 41.03 21.46 48.99 C 21.11 58.97 26.58 68.76 35.08 73.92 C 43.28 79.06 53.95 79.28 62.66 75.29 C 70.37 77.57 78.52 77.36 86.31 75.57 C 80.05 84.00 70.94 90.35 60.69 92.84 C 48.02 96.03 34.00 93.24 23.56 85.37 C 12.16 77.09 5.12 63.11 5.44 49.00 C 5.15 43.06 8.22 37.42 12.30 33.30 Z' />\n<path fill='#91dc47' d=' M 26.94 54.00 C 24.97 45.06 29.20 35.59 36.45 30.24 C 41.99 33.71 44.23 40.14 46.55 45.91 C 43.00 53.40 38.44 60.46 35.94 68.42 C 31.50 64.74 27.96 59.77 26.94 54.00 Z' />\n<path fill='#91dc47' d=' M 41.97 71.80 C 41.46 64.27 45.31 57.52 48.11 50.80 C 50.40 58.13 51.84 66.19 57.18 72.06 C 52.17 73.37 46.93 73.26 41.97 71.80 Z' />\n</svg>";

//# sourceMappingURL=heads_up.js.map?rel=1431620936616