The aim of `mod-deploy!` is not only to create modular artifact distributions as in `lein-repack`, but modular *repositories*.

Take the example of `quantum`. Say `quantum` has only three candidates for sub-libraries, namely `quantum.collections`, `quantum.defnt`, and `quantum.validate`. Let's further say that we decide, in the name of stars, to name each of the sub-libraries more attractively. So `quantum.collections` becomes `collect`, `quantum.defnt` becomes plain old `defnt`, `quantum.validate` becomes `transpec` (all of which are WIP names, of course). Then, like so:

```clojure
(mod-deploy! ["alexandergunnarson" "collect"  #{"./src/cljc/quantum/core/collections.cljc"}]
             ["alexandergunnarson" "defnt"    #{"./src/cljc/quantum/core/macros/defnt.cljc"}]
             ["alexandergunnarson" "transpec" #{"./src/cljc/quantum/core/data/validated.cljc"}])
```

Voilà! Now, effortlessly, you've created three new repositories: `alexandergunnarson/collect`, `alexandergunnarson/defnt`, and `alexandergunnarson/transpec`. Note that the function will do nothing if called again — it will detect that the repositories in question already exist. It uses git to detect whether the files in question are "dirty". Note that if you call `mod-deploy!` from the `master` branch in `quantum`, it will push to the same branch (`master`) in the sub-repos; likewise, if you call it from branch `abcde`, it will push to the `abcde` branch (creating it if necessary) on the sub-repos.

Monorepos are great (don't believe me? [ask Google why they've done it since the very beginning, now going on >86TB strong of repo data](http://cacm.acm.org/magazines/2016/7/204032-why-google-stores-billions-of-lines-of-code-in-a-single-repository/fulltext). The only downside of this technique, of course, is that for consistency's sake, 1) all edits of subrepos must be coordinated, preferably all at once whenever the parent repo is changed (in case subrepos depend on each other) and 2) all edits of subrepos must only ever be done from the parent repo and merely propagated to these subrepos. This means, in effect, that the subrepos become read-only with respect to all but the parent repo. But read-only subrepos are a modern marvel in comparison to the madness that is coordinating multidirectional changes among hundreds of independent repos. A small price to pay.
