// Compiled by ClojureScript 0.0-3269 {}
goog.provide('quantum.ui.component');
goog.require('cljs.core');
quantum.ui.component.components = cljs.core.atom.call(null,cljs.core.PersistentHashSet.EMPTY);
quantum.ui.component.register_component_BANG_ = (function quantum$ui$component$register_component_BANG_(var_0){
return cljs.core.swap_BANG_.call(null,quantum.ui.component.components,cljs.core.conj,var_0);
});
quantum.ui.component.state = cljs.core.atom.call(null,cljs.core.PersistentArrayMap.EMPTY);
quantum.ui.component._STAR_component_hook_STAR_ = (function quantum$ui$component$_STAR_component_hook_STAR_(html){
css.update_css_once_BANG_.call(null,html,cljs.core.deref.call(null,quantum.ui.component.state));

return css.style.call(null,html);
});
quantum.ui.component.throw_args = (function quantum$ui$component$throw_args(args){
throw cljs.core.ex_info.call(null,"Arguments to |defcomponent| must be a vector:",args);
});

//# sourceMappingURL=component.js.map?rel=1431620827139