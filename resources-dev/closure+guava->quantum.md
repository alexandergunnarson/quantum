TODO java.util.concurrent.atomic.AtomicIntegerArray

#1: Make the type system :)
#2: Go through Java core packages
#3: Go through CLJ(S) core packages

GOOGLE CLOSURE LIBRARY  v20170218
; (structures = syntactic; data = semantic)
; format = textual/structural representation <-> textual representation
; parse  = textual representation -> structural representation
; TODO rename quantum.*.string.regex.* -> quantum.*.string.pattern*
; TODO rename quantum.core.print -> quantum.core.format
; TODO clojure.parallel, clojure.set, clojure.inspector, clojure.string, etc.


Note that `quantum` is made with the assumption in mind that neither Java-based UIs nor other user-dependent utilities will be built with it.
Not desired:

- GOOGLE CLOSURE LIBRARY:
- goog.async.freelist
- goog.base (transitive dependency)
- goog.bootstrap.nodejs
- goog.bootstrap.webworkers *(though this may come in handy with ReactNative WebWorkers)*
- goog.db.error (transitive dependency)
- goog.deps (transitive dependency)
- goog.debug.devcss
- goog.debug.devcss.devcssrunner
- goog.debug.console
- goog.debug.errorhandlerweakdep
- goog.debug.logbuffer (prefer circularbuffer)
- goog.disposable.disposable
- goog.disposable.idisposable
- goog.dom.animationframe.polyfill (transitive dependency)
- goog.dom.browserrange (transitive dependency)
- goog.dom.browserrange.abstractrange (transitive dependency)
- goog.dom.browserrange.geckorange (transitive dependency)
- goog.dom.browserrange.ierange (transitive dependency)
- goog.dom.browserrange.operarange (transitive dependency)
- goog.dom.browserrange.w3crange (transitive dependency)
- goog.dom.browserrange.webkitrange (transitive dependency)
- goog.dom.abstractmultirange (transitive dependency)
- goog.dom.abstractrange (transitive dependency)
- goog.dom.pattern
- goog.dom.pattern.abstractpattern
- goog.dom.pattern.allchildren
- goog.dom.pattern.callback
- goog.dom.pattern.callback.counter
- goog.dom.pattern.childmatches
- goog.dom.pattern.endtag
- goog.dom.pattern.fulltag
- goog.dom.pattern.nodetype
- goog.dom.pattern.repeat
- goog.dom.pattern.sequence
- goog.dom.pattern.starttag
- goog.dom.pattern.tag
- goog.dom.pattern.text
- goog.dom.bufferedviewportsizemonitor
- goog.dom.classes (prefer goog.dom.classlist)
- goog.dom.controlrange (transitive dependency)
- goog.fx.abstractdragdrop
- goog.fx.animationqueue
- goog.fx.fx
- goog.fx.transition
- goog.fx.transitionbase
- goog.graphics.abstractgraphics
- goog.html.flash
- goog.html.legacyconversions
- goog.html.sanitizer.attributewhitelist
- goog.html.sanitizer.tagblacklist
- goog.html.sanitizer.tagwhitelist
- goog.html.sanitizer.unsafe
- goog.html.silverlight
- goog.html.testing
- goog.html.uncheckedconversions
- goog.images.*
- goog.json.processor
- goog.messaging.abstractchannel (transitive dependency)
- goog.messaging.messagechannel (transitive dependency)
- goog.messaging.portnetwork (transitive dependency)
- goog.module (transitive dependency)
- goog.module.abstractmoduleloader (transitive dependency)
- goog.module.basemodule (transitive dependency)
- goog.module.loader (transitive dependency)
- goog.module.moduleinfo (transitive dependency)
- goog.module.moduleloader
- goog.module.moduleloadcallback (transitive dependency)
- goog.module.modulemanager (transitive dependency)
- goog.net.browsertestchannel
- goog.net.streams.streamparser
- goog.net.streams.utils
- goog.net.tmpnetwork
- goog.osapi
- goog.positioning.abstractposition
- goog.promise.resolver (transitive dependency)
- goog.pubsub.topicid (transitive dependency)
- goog.soy
- goog.soy.data
- goog.soy.renderer
- goog.structs.collection (transitive dependency)
- goog.thenable (transitive dependency)
- goog.transpile (transitive dependency)
- goog.reflect
- goog.result.dependentresult
- goog.result.result_interface
- goog.storage.mechanism.errorcode
- goog.storage.mechanism.errorhandlingmechanism
- goog.string.parser
- goog.string.stringifier
- goog.string.typedstring

- GOOGLE GUAVA:
- com.google.common.annotations.Beta
- com.google.common.annotations.GwtCompatible
- com.google.common.annotations.GwtIncompatible
- com.google.common.annotations.VisibleForTesting
- com.google.common.base.AbstractIterator (transitive dependency)
- com.google.common.base.CommonMatcher (transitive dependency)
- com.google.common.base.CommonPattern (transitive dependency)
- com.google.common.base.ExtraObjectsMethodsForWeb
- com.google.common.base.FinalizablePhantomReference (transitive dependency)
- com.google.common.base.FinalizableReference (transitive dependency)
- com.google.common.base.FinalizableSoftReference (transitive dependency)
- com.google.common.base.FinalizableWeakReference (transitive dependency)
- com.google.common.base.Function
- com.google.common.base.FunctionalEquivalence
- com.google.common.base.JdkPattern
- com.google.common.base.Objects
- com.google.common.base.PairwiseEquivalence
- com.google.common.base.PatternCompiler (transitive dependency)
- com.google.common.base.Platform
- com.google.common.base.Predicate
- com.google.common.base.Supplier
- com.google.common.base.VerifyException (transitive dependency)
- com.google.common.cache.AbstractCache (transitive dependency)
- com.google.common.cache.AbstractLoadingCache (transitive dependency)
- com.google.common.cache.Cache (transitive dependency)
- com.google.common.cache.CacheLoader (transitive dependency)
- com.google.common.cache.ForwardingCache (transitive dependency)
- com.google.common.cache.ForwardingLoadingCache (transitive dependency)
- com.google.common.cache.LoadingCache (transitive dependency)
- com.google.common.cache.LongAddable (transitive dependency)
- com.google.common.cache.RemovalListener (transitive dependency)
- com.google.common.cache.Striped64 (transitive dependency)
- com.google.common.cache.Weigher (transitive dependency)
- com.google.common.collect.AbstractBiMap (transitive dependency)
- com.google.common.collect.AbstractIndexedListIterator (transitive dependency)
- com.google.common.collect.AbstractIterator (transitive dependency)
- com.google.common.collect.AbstractListMultimap (transitive dependency)
- com.google.common.collect.AbstractMapBasedMultimap (transitive dependency)
- com.google.common.collect.AbstractMapBasedMultiset (transitive dependency)
- com.google.common.collect.AbstractMapEntry (transitive dependency)
- com.google.common.collect.AbstractMultimap (transitive dependency)
- com.google.common.collect.AbstractMultiset (transitive dependency)
- com.google.common.collect.AbstractNavigableMap (transitive dependency)
- com.google.common.collect.AbstractRangeSet (transitive dependency)
- com.google.common.collect.AbstractSequentialIterator (transitive dependency)
- com.google.common.collect.AbstractSetMultimap (transitive dependency)
- com.google.common.collect.AbstractSortedKeySortedSetMultimap (transitive dependency)
- com.google.common.collect.AbstractSortedMultiset (transitive dependency)
- com.google.common.collect.AbstractSortedSetMultimap (transitive dependency)
- com.google.common.collect.AbstractTable (transitive dependency)
- com.google.common.collect.BiMap (transitive dependency)
- com.google.common.collect.BinaryTreeTraverser (transitive dependency)
- com.google.common.collect.ClassToInstanceMap (transitive dependency)
- com.google.common.collect.ComputationException (transitive dependency)
- com.google.common.collect.ConsumingQueueIterator (transitive dependency)
- com.google.common.collect.ContiguousSet (transitive dependency)
- com.google.common.collect.Count (transitive dependency)
- com.google.common.collect.Cut (transitive dependency)
- com.google.common.collect.DescendingMultiset (transitive dependency)
- com.google.common.collect.EmptyContiguousSet (transitive dependency)
- com.google.common.collect.EmptyImmutableListMultimap (transitive dependency)
- com.google.common.collect.EmptyImmutableSetMultimap (transitive dependency)
- com.google.common.collect.FilteredMultimap (transitive dependency)
- com.google.common.collect.FilteredSetMultimap (transitive dependency)
- com.google.common.collect.ForwardingBlockingDeque (transitive dependency)
- com.google.common.collect.ForwardingCollection (transitive dependency)
- com.google.common.collect.ForwardingConcurrentMap (transitive dependency)
- com.google.common.collect.ForwardingDeque (transitive dependency)
- com.google.common.collect.ForwardingImmutableCollection (transitive dependency)
- com.google.common.collect.ForwardingImmutableList (transitive dependency)
- com.google.common.collect.ForwardingImmutableMap (transitive dependency)
- com.google.common.collect.ForwardingImmutableSet (transitive dependency)
- com.google.common.collect.ForwardingIterator (transitive dependency)
- com.google.common.collect.ForwardingList (transitive dependency)
- com.google.common.collect.ForwardingListIterator (transitive dependency)
- com.google.common.collect.ForwardingListMultimap (transitive dependency)
- com.google.common.collect.ForwardingMap (transitive dependency)
- com.google.common.collect.ForwardingMapEntry (transitive dependency)
- com.google.common.collect.ForwardingMultimap (transitive dependency)
- com.google.common.collect.ForwardingMultiset (transitive dependency)
- com.google.common.collect.ForwardingNavigableMap (transitive dependency)
- com.google.common.collect.ForwardingNavigableSet (transitive dependency)
- com.google.common.collect.ForwardingObject (transitive dependency) # really, the decorator pattern
- com.google.common.collect.ForwardingQueue (transitive dependency)
- com.google.common.collect.ForwardingSet (transitive dependency)
- com.google.common.collect.ForwardingSetMultimap (transitive dependency)
- com.google.common.collect.ForwardingSortedMap (transitive dependency)
- com.google.common.collect.ForwardingSortedMultiset (transitive dependency)
- com.google.common.collect.ForwardingSortedSet (transitive dependency)
- com.google.common.collect.ForwardingSortedSetMultimap (transitive dependency)
- com.google.common.collect.ForwardingTable (transitive dependency)
- com.google.common.collect.GeneralRange (transitive dependency)
- com.google.common.collect.GwtTransient (transitive dependency)
- com.google.common.collect.Hashing (transitive dependency)
- com.google.common.collect.ImmutableAsList (transitive dependency)
- com.google.common.collect.ImmutableBiMap (transitive dependency)
- com.google.common.collect.ImmutableBiMapFauxverideShim (transitive dependency)
- com.google.common.collect.ImmutableCollection (transitive dependency)
- com.google.common.collect.ImmutableList (transitive dependency)
- com.google.common.collect.ImmutableMap (transitive dependency)
- com.google.common.collect.ImmutableMapEntrySet (transitive dependency)
- com.google.common.collect.ImmutableMultimap (transitive dependency)
- com.google.common.collect.ImmutableMultiset (transitive dependency)
- com.google.common.collect.ImmutableSet (transitive dependency)
- com.google.common.collect.ImmutableSortedAsList (transitive dependency)
- com.google.common.collect.ImmutableSortedMapFauxverideShim (transitive dependency)
- com.google.common.collect.ImmutableSortedMultiset (transitive dependency)
- com.google.common.collect.ImmutableSortedMultisetFauxverideShim (transitive dependency)
- com.google.common.collect.ImmutableSortedSet (transitive dependency)
- com.google.common.collect.ImmutableSortedSetFauxverideShim (transitive dependency)
- com.google.common.collect.ImmutableTable (transitive dependency)
- com.google.common.collect.Interner (transitive dependency)
- com.google.common.collect.ListMultimap (transitive dependency)
- com.google.common.collect.MapDifference (transitive dependency)
- com.google.common.collect.Multimap (transitive dependency)
- com.google.common.collect.MultimapBuilder (transitive dependency)
- com.google.common.collect.Multiset (transitive dependency)
- com.google.common.collect.MultitransformedIterator (transitive dependency)
- com.google.common.collect.Ordering (transitive dependency)
- com.google.common.collect.PeekingIterator (transitive dependency)
- com.google.common.collect.Platform (transitive dependency)
- com.google.common.collect.RangeMap (transitive dependency)
- com.google.common.collect.RangeSet (transitive dependency)
- com.google.common.collect.RowSortedTable (transitive dependency)
- com.google.common.collect.SetMultimap (transitive dependency)
- com.google.common.collect.SingletonImmutableBiMap (transitive dependency)
- com.google.common.collect.SingletonImmutableList (transitive dependency)
- com.google.common.collect.SingletonImmutableSet (transitive dependency)
- com.google.common.collect.SingletonImmutableTable (transitive dependency)
- com.google.common.collect.SortedIterable (transitive dependency)
- com.google.common.collect.SortedMapDifference (transitive dependency)
- com.google.common.collect.SortedMultiset (transitive dependency)
- com.google.common.collect.SortedMultisetBridge (transitive dependency)
- com.google.common.collect.SortedSetMultimap (transitive dependency)
- com.google.common.collect.Table (transitive dependency)
- com.google.common.collect.TransformedIterator (transitive dependency)
- com.google.common.collect.TransformedListIterator (transitive dependency)
- com.google.common.collect.TreeTraverser (transitive dependency)
- com.google.common.collect.UnmodifiableIterator (transitive dependency)
- com.google.common.collect.UnmodifiableListIterator (transitive dependency)
- com.google.common.collect.UnmodifiableSortedMultiset (transitive dependency)
- com.google.common.escape.ArrayBasedCharEscaper (transitive dependency)
- com.google.common.escape.ArrayBasedEscaperMap
- com.google.common.escape.ArrayBasedUnicodeEscaper (transitive dependency)
- com.google.common.escape.CharEscaper (transitive dependency)
- com.google.common.escape.Escaper (transitive dependency)
- com.google.common.escape.Platform
- com.google.common.escape.UnicodeEscaper (transitive dependency)
- com.google.common.eventbus.AllowConcurrentEvents
- com.google.common.eventbus.Dispatcher
- com.google.common.eventbus.Subscribe
- com.google.common.eventbus.SubscriberExceptionContext
- com.google.common.eventbus.SubscriberExceptionHandler
- com.google.common.hash.AbstractByteHasher (transitive dependency)
- com.google.common.hash.AbstractCompositeHashFunction (transitive dependency)
- com.google.common.hash.AbstractHasher (transitive dependency)
- com.google.common.hash.AbstractNonStreamingHashFunction (transitive dependency)
- com.google.common.hash.AbstractStreamingHashFunction (transitive dependency)
- com.google.common.io.AndroidIncompatible
- com.google.common.io.ByteArrayDataInput (transitive dependency)
- com.google.common.io.ByteArrayDataOutput (transitive dependency)
- com.google.common.io.ByteProcessor (transitive dependency)
- com.google.common.io.ByteSink (transitive dependency)
- com.google.common.io.ByteSource (transitive dependency)
- com.google.common.io.CharSink (transitive dependency)
- com.google.common.io.CharSource (transitive dependency)
- com.google.common.io.FileWriteMode (transitive dependency)
- com.google.common.io.InsecureRecursiveDeleteException (transitive dependency)
- com.google.common.io.LineBuffer (transitive dependency)
- com.google.common.io.LineProcessor (transitive dependency)
- com.google.common.io.PatternFilenameFilter
- com.google.common.io.RecursiveDeleteOption (transitive dependency)
- com.google.common.reflect.AbstractInvocationHandler (transitive dependency)
- com.google.common.reflect.Invokable (transitive dependency)
- com.google.common.reflect.TypeCapture (transitive dependency)
- com.google.common.reflect.TypeParameter (transitive dependency)
- com.google.common.reflect.TypeToInstanceMap (transitive dependency)
- com.google.common.reflect.TypeToken (transitive dependency)
- com.google.common.reflect.TypeVisitor (transitive dependency)
- com.google.common.util.concurrent.AbstractCatchingFuture (transitive dependency)
- com.google.common.util.concurrent.AbstractCheckedFuture (transitive dependency)
- com.google.common.util.concurrent.AbstractExecutionThreadService (transitive dependency)
- com.google.common.util.concurrent.AbstractFuture (transitive dependency)
- com.google.common.util.concurrent.AbstractIdleService (transitive dependency)
- com.google.common.util.concurrent.AbstractListeningExecutorService (transitive dependency)
- com.google.common.util.concurrent.AbstractScheduledService (transitive dependency)
- com.google.common.util.concurrent.AbstractService (transitive dependency)
- com.google.common.util.concurrent.AbstractTransformFuture (transitive dependency)
- com.google.common.util.concurrent.AggregateFuture (transitive dependency)
- com.google.common.util.concurrent.AggregateFutureState (transitive dependency)
- com.google.common.util.concurrent.AsyncCallable (transitive dependency)
- com.google.common.util.concurrent.AsyncFunction (transitive dependency)
- com.google.common.util.concurrent.CheckedFuture (transitive dependency)
- com.google.common.util.concurrent.CollectionFuture (transitive dependency)
- com.google.common.util.concurrent.ExecutionError (transitive dependency)
- com.google.common.util.concurrent.ForwardingBlockingDeque (transitive dependency)
- com.google.common.util.concurrent.ForwardingCheckedFuture (transitive dependency)
- com.google.common.util.concurrent.ForwardingExecutorService (transitive dependency)
- com.google.common.util.concurrent.ForwardingFuture (transitive dependency)
- com.google.common.util.concurrent.ForwardingListenableFuture (transitive dependency)
- com.google.common.util.concurrent.ForwardingListeningExecutorService (transitive dependency)
- com.google.common.util.concurrent.FutureCallback (transitive dependency)
- com.google.common.util.concurrent.GwtFuturesCatchingSpecialization (transitive dependency)
- com.google.common.util.concurrent.ImmediateFuture (transitive dependency)
- com.google.common.util.concurrent.InterruptibleTask (transitive dependency)
- com.google.common.util.concurrent.ListenableFuture (transitive dependency)
- com.google.common.util.concurrent.ListenableScheduledFuture (transitive dependency)
- com.google.common.util.concurrent.ListeningExecutorService (transitive dependency)
- com.google.common.util.concurrent.ListeningScheduledExecutorService (transitive dependency)
- com.google.common.util.concurrent.Partially (transitive dependency)
- com.google.common.util.concurrent.Platform (transitive dependency)
- com.google.common.util.concurrent.RateLimiter (transitive dependency)
- com.google.common.util.concurrent.Service (transitive dependency)
- com.google.common.util.concurrent.SmoothRateLimiter (transitive dependency)
- com.google.common.util.concurrent.Striped (transitive dependency)
- com.google.common.util.concurrent.TimeLimiter (transitive dependency)
- com.google.common.util.concurrent.UncheckedExecutionException (transitive dependency)
- com.google.common.util.concurrent.UncheckedTimeoutException (transitive dependency)
- com.google.common.util.concurrent.WrappingExecutorService (transitive dependency)
- com.google.common.util.concurrent.WrappingScheduledExecutorService (transitive dependency)
- com.google.thirdparty.publicsuffix.PublicSuffixPatterns (transitive dependency)
- com.google.thirdparty.publicsuffix.PublicSuffixType (transitive dependency)
- com.google.thirdparty.publicsuffix.TrieParser

- JAVA 8 STANDARD LIBRARY:
- java.applet.*
- java.awt.datatransfer.*
- java.awt.dnd.*
- java.awt.event.*
- java.awt.font.*
- java.awt.im.*
- java.awt.print.* (not supporting Java UI)
- java.beans.* (too Object-Oriented)
- java.lang.annotation.*
- java.nio.charset.spi.* (abstract)
- java.nio.file.spi.* (abstract)
- java.sql.* (use a more user-friendly package; also don't use SQL)
- javax.sql.* (use a more user-friendly package; also don't use SQL)
- java.text.spi.* (abstract)
- java.util.function.* (too specific)
- java.util.prefs.*
- java.util.spi.* (abstract)
- javax.accessibility.*
- javax.annotation.*
- javax.crypto.interfaces.*
- javax.naming.event.*
- javax.rmi.CORBA.* (ancient)
- javax.security.auth.spi.* (abstract)
- javax.sound.midi.spi.* (abstract)
- javax.sound.sampled.spi.* (abstract)
- javax.sql.rowset.spi.* (abstract)
- javax.swing.* (not supporting Java UI)
- javax.swing.border.* (not supporting Java UI)
- javax.swing.colorchooser.* (not supporting Java UI)
- javax.swing.event.* (not supporting Java UI)
- javax.swing.filechooser.* (not supporting Java UI)
- javax.swing.plaf.* (not supporting Java UI)
- javax.swing.table.*
- javax.swing.text.*
- javax.swing.text.html*
- javax.swing.text.rtf*
- javax.swing.tree.*
- javax.swing.undo.*
- javax.transaction.*
- javax.xml.* (one level deep)
- javax.xml.soap.* (ancient)
- javax.xml.stream.events.*
- javax.xml.stream.util.*
- org.omg.* (ancient)
- javax.naming.spi.*
- javax.print.*

# INTERESTING THINGS:
* javax.print is used to query about printer info and sending print jobs.
* java.awt.print is used to print the actual documents.


quantum.core.system

https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/base/StandardSystemProperty.java

quantum.core.fn

https://github.com/google/closure-library/blob/v20170218/closure/goog/functions/functions.js
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/base/Functions.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/base/Predicates.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/base/Suppliers.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/util/concurrent/Callables.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/util/concurrent/Runnables.java


quantum.core.memoize

https://github.com/google/closure-library/blob/v20170218/closure/goog/memoize/memoize.js

quantum.channels

; should include core.async channels
https://github.com/google/closure-library/blob/v20170218/closure/goog/messaging/messaging.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/messaging/bufferedchannel.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/messaging/deferredchannel.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/messaging/multichannel.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/messaging/portcaller.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/messaging/portchannel.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/messaging/portoperator.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/messaging/respondingchannel.js

quantum.channels.pub-sub

; should include e.g. kafka
https://github.com/google/closure-library/blob/v20170218/closure/goog/pubsub/pubsub.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/pubsub/typedpubsub.js
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/eventbus/AsyncEventBus.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/eventbus/DeadEvent.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/eventbus/EventBus.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/eventbus/Subscriber.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/eventbus/SubscriberRegistry.java

quantum.channels.net

https://github.com/google/closure-library/blob/v20170218/closure/goog/net/browserchannel.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/xpc/crosspagechannel.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/xpc/crosspagechannelrole.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/xpc/directtransport.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/xpc/frameelementmethodtransport.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/xpc/iframepollingtransport.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/xpc/iframerelaytransport.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/xpc/nativemessagingtransport.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/xpc/nixtransport.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/xpc/relay.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/channeldebug.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/channelrequest.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/xpc/xpc.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/xpc/transport.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/eventtype.js

quantum.core.resources

https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/io/Closer.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/io/Closeables.java

quantum.core.io.files ; uri-referenced binary entity

https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/io/Files.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/io/MoreFiles.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/io/Resources.java
https://github.com/google/closure-library/blob/v20170218/closure/goog/fs/entry.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/fs/error.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/fs/filereader.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/fs/filesaver.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/fs/filesystem.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/fs/filesystemimpl.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/fs/filewriter.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/fs/fs.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/fs/progressevent.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/fs/url.js

quantum.core.io.streams

https://github.com/google/closure-library/blob/v20170218/closure/goog/net/streams/base64streamdecoder.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/streams/nodereadablestream.js
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/io/FileBackedOutputStream.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/io/Flushables.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/io/LineReader.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/io/LittleEndianDataInputStream.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/io/LittleEndianDataOutputStream.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/io/MultiInputStream.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/io/MultiReader.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/io/ReaderInputStream.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/io/CountingOutputStream.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/io/CharStreams.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/io/CharSequenceReader.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/io/ByteStreams.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/io/AppendableWriter.java

quantum.graphics

https://github.com/google/closure-library/blob/v20170218/closure/goog/graphics/graphics.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/graphics/element.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/graphics/ext/element.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/graphics/ellipseelement.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/graphics/ext/ellipse.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/graphics/ext/ext.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/graphics/font.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/graphics/fill.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/graphics/solidfill.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/graphics/ext/graphics.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/graphics/groupelement.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/graphics/ext/group.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/graphics/imageelement.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/graphics/ext/image.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/graphics/lineargradient.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/graphics/path.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/graphics/pathelement.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/graphics/paths.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/graphics/ext/path.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/graphics/rectelement.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/graphics/ext/rectangle.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/graphics/ext/shape.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/graphics/stroke.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/graphics/strokeandfillelement.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/graphics/ext/strokeandfillelement.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/graphics/textelement.js

quantum.graphics.svg

https://github.com/google/closure-library/blob/v20170218/closure/goog/graphics/svggraphics.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/graphics/svgelement.js

quantum.graphics.vml

https://github.com/google/closure-library/blob/v20170218/closure/goog/graphics/vmlgraphics.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/graphics/vmlelement.js

quantum.graphics.html.canvas

https://github.com/google/closure-library/blob/v20170218/closure/goog/graphics/canvaselement.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/graphics/canvasgraphics.js

quantum.graphics.color

https://github.com/google/closure-library/blob/v20170218/closure/goog/color/alpha.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/color/color.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/color/names.js

quantum.graphics.animation

https://github.com/google/closure-library/blob/v20170218/closure/goog/fx/anim/anim.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/fx/animation.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/fx/easing.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/fx/dom.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/style/transition.js

quantum.graphics.animation.css

https://github.com/google/closure-library/blob/v20170218/closure/goog/fx/css3/fx.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/fx/css3/transition.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/fx/cssspriteanimation.js

quantum.graphics.webgl

https://github.com/google/closure-library/blob/v20170218/closure/goog/webgl/webgl.js

quantum.db.indexed-db

https://github.com/google/closure-library/blob/v20170218/closure/goog/db/db.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/db/cursor.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/db/index.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/db/indexeddb.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/db/keyrange.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/db/objectstore.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/db/transaction.js

quantum.validate.email + quantum.format.email

https://github.com/google/closure-library/blob/v20170218/closure/goog/format/emailaddress.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/format/internationalizedemailaddress.js

quantum.ui.a11y-aria

https://github.com/google/closure-library/blob/v20170218/closure/goog/a11y/aria/announcer.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/a11y/aria/aria.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/a11y/aria/attributes.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/a11y/aria/datatables.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/a11y/aria/roles.js

quantum.ui.async

https://github.com/google/closure-library/blob/v20170218/closure/goog/async/animationdelay.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/dom/animationframe/animationframe.js

quantum.ui

https://github.com/google/closure-library/tree/v20170218/closure/goog/css — maybe helpful for creating components?
https://github.com/google/closure-library/blob/v20170218/closure/goog/style/cursor.js

quantum.ui.dom

https://github.com/google/closure-library/blob/v20170218/closure/goog/dom/safe.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/dom/dom.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/dom/iter.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/dom/nodeiterator.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/dom/tagiterator.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/dom/textrangeiterator.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/dom/nodeoffset.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/dom/nodetype.js

quantum.ui.dom.query

https://github.com/google/closure-library/blob/v20170218/third_party/closure/goog/dojo/dom/query.js

quantum.ui.dom.modules.window

https://github.com/google/closure-library/blob/v20170218/closure/goog/window/window.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/dom/fullscreen.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/dom/iframe.js

quantum.ui.dom.modules.range

https://github.com/google/closure-library/blob/v20170218/closure/goog/dom/multirange.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/dom/range.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/dom/rangeendpoint.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/dom/savedcaretrange.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/dom/savedrange.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/dom/selection.js

quantum.ui.dom.tags

https://github.com/google/closure-library/blob/v20170218/closure/goog/dom/inputtype.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/dom/tagname.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/dom/tags.js

quantum.ui.dom.vendor

https://github.com/google/closure-library/blob/v20170218/closure/goog/dom/vendor.js

quantum.ui.dom.attributes

https://github.com/google/closure-library/blob/v20170218/closure/goog/dom/attr.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/dom/dataset.js

quantum.ui.dom.features

https://github.com/google/closure-library/blob/v20170218/closure/goog/dom/browserfeature.js

quantum.ui.dom.classes

https://github.com/google/closure-library/blob/v20170218/closure/goog/dom/classlist.js

quantum.ui.dom.style

https://github.com/google/closure-library/blob/v20170218/closure/goog/style/style.js

quantum.ui.transform

https://github.com/google/closure-library/blob/v20170218/closure/goog/style/transform.js

quantum.ui.dom.svg

https://github.com/google/closure-library/blob/v20170218/third_party/closure/goog/svgpan/svgpan.js

quantum.ui.components.debug

https://github.com/google/closure-library/blob/v20170218/closure/goog/debug/divconsole.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/debug/fancywindow.js

quantum.ui.components.annotate

https://github.com/google/closure-library/blob/v20170218/closure/goog/dom/annotate.js

quantum.ui.components.forms

https://github.com/google/closure-library/blob/v20170218/closure/goog/dom/forms.js

quantum.ui.components.drag-drop

https://github.com/google/closure-library/blob/v20170218/closure/goog/fx/dragdrop.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/fx/dragdropgroup.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/fx/dragger.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/fx/draglistgroup.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/fx/dragscrollsupport.js

quantum.net.http

https://github.com/google/closure-library/blob/v20170218/closure/goog/net/httpstatus.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/httpstatusname.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/bulkloader.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/corsxmlhttpfactory.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/fetchxmlhttpfactory.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/wrapperxmlhttpfactory.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/xmlhttpfactory.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/xhrio.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/xhrlike.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/xhrmanager.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/xhriopool.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/xmlhttp.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/filedownloader.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/imageloader.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/iframeio.js  (?)
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/iframeloadmonitor.js (?)
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/multiiframeloadmonitor.js (?
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/networkstatusmonitor.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/networktester.js)
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/jsloader.js (?)
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/jsonp.js

quantum.net.http.cookies

https://github.com/google/closure-library/blob/v20170218/closure/goog/net/cookies.js

quantum.net.http.rpc

https://github.com/google/closure-library/blob/v20170218/closure/goog/net/crossdomainrpc.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/rpc/httpcors.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/errorcode.js

quantum.net.websocket

https://github.com/google/closure-library/blob/v20170218/closure/goog/net/websocket.js

quantum.core.async

https://github.com/google/closure-library/blob/v20170218/closure/goog/async/throttle.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/async/run.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/async/nexttick.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/async/conditionaldelay.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/async/debouncer.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/async/delay.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/timer/timer.js
https://github.com/google/closure-library/blob/v20170218/third_party/closure/goog/mochikit/async/deferredlist.js
https://github.com/google/closure-library/blob/v20170218/third_party/closure/goog/mochikit/async/deferred.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/promise/promise.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/result/deferredadaptor.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/result/resultutil.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/result/simpleresult.js
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/util/concurrent/CombinedFuture.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/util/concurrent/ExecutionList.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/util/concurrent/Futures.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/util/concurrent/FuturesGetChecked.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/util/concurrent/JdkFutureAdapters.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/util/concurrent/ListenableFutureTask.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/util/concurrent/Monitor.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/util/concurrent/MoreExecutors.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/util/concurrent/SerializingExecutor.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/util/concurrent/SettableFuture.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/util/concurrent/SimpleTimeLimiter.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/util/concurrent/ThreadFactoryBuilder.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/util/concurrent/TimeoutFuture.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/util/concurrent/TrustedListenableFutureTask.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/util/concurrent/Uninterruptibles.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/util/concurrent/ServiceManager.java

quantum.core.data.array

https://github.com/google/closure-library/blob/v20170218/closure/goog/array/array.js
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/ObjectArrays.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/hash/LittleEndianByteArray.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/util/concurrent/AtomicDoubleArray.java
farray-table`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/ArrayTable.java
`!table`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/StandardTable.java
`!row-sorted-table`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/StandardRowSortedTable.java
`&dense-table`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/DenseImmutableTable.java
`&sparse-table`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/SparseImmutableTable.java
`&table`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/RegularImmutableTable.java
`!hash-based-table`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/HashBasedTable.java
`!tree-based-table`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/TreeBasedTable.java

quantum.core.data.queue

`!sliding-buffer` = `!cyclic-buffer` = `!ring-buffer` = `!circular-buffer` = `!evicting-queue`
https://github.com/google/closure-library/blob/v20170218/closure/goog/structs/circularbuffer.js
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/EvictingQueue.java
`!priority-sliding-buffer`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/TopKSelector.java
`!queue`
https://github.com/google/closure-library/blob/v20170218/closure/goog/structs/queue.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/async/workqueue.js
`!finalizable-ref-queue`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/base/FinalizableReferenceQueue.java
`!priority-queue` = `!sorted-queue`
https://github.com/google/closure-library/blob/v20170218/closure/goog/structs/priorityqueue.js
`!min-max-priority-queue` = `!sorted-deque`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/MinMaxPriorityQueue.java

quantum.location

https://github.com/google/closure-library/blob/v20170218/closure/goog/locale/countries.js

quantum.localization

https://github.com/google/closure-library/blob/v20170218/closure/goog/locale/defaultlocalenameconstants.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/locale/genericfontnames.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/locale/genericfontnamesdata.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/locale/locale.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/locale/nativenameconstants.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/locale/scriptToLanguages.js

quantum.core.collections

https://github.com/google/closure-library/blob/v20170218/closure/goog/structs/structs.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/structs/weak/weak.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/iter/iter.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/object/object.js
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/Range.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/CollectCollectors.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/MoreCollectors.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/Collections2.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/Iterables.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/Iterators.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/Lists.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/Maps.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/Multimaps.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/Multisets.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/Queues.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/Sets.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/SortedIterables.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/SortedLists.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/SortedMultisets.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/Synchronized.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/Tables.java

quantum.core.reducers

https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/CollectSpliterators.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/FluentIterable.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/Streams.java

quantum.core.data.list

`cartesian-list`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/CartesianList.java
`as-list`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/RegularImmutableAsList.java
`list`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/RegularImmutableList.java

quantum.core.data.map

https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/MapMaker.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/MapMakerInternalMap.java
`map-entry`
https://github.com/google/closure-library/blob/v20170218/closure/goog/structs/node.js
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/ImmutableEntry.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/ImmutableMapEntry.java
`!hash-map` — where keys are not simple (not just numbers or strings)
https://github.com/google/closure-library/blob/v20170218/closure/goog/structs/map.js
`!hash-multimap`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/HashMultiMap.java
`!tree-multimap`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/TreeMultiMap.java
`!hash-bi-map`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/HashBiMap.java
`&bi-map`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/RegularImmutableBiMap.java
`!inversion-map`
https://github.com/google/closure-library/blob/v20170218/closure/goog/structs/inversionmap.js
`&map-key-set`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/ImmutableMapKeySet.java
`&map-values`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/ImmutableMapValues.java
`&range-map`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/ImmutableRangeMap.java
`!tree-range-map`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/TreeRangeMap.java
`!linked-map` / `!ordered-map`
https://github.com/google/closure-library/blob/v20170218/closure/goog/structs/linkedmap.js
`!sorted-map`
https://github.com/google/closure-library/blob/v20170218/closure/goog/structs/avltree.js
`&sorted-map`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/ImmutableSortedMap.java
`atomic-map:long`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/util/concurrent/AtomicLongMap.java
`!heap`
https://github.com/google/closure-library/blob/v20170218/closure/goog/structs/heap.js
`&enum-map`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/ImmutableEnumMap.java
`!enum-map-entries`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/WellBehavedMap.java
`!enum-bi-map`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/EnumBiMap.java
`!enum-hash-bi-map`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/EnumHashBiMap.java
`&class->instance-map`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/ImmutableClassToInstanceMap.java
`!class->instance-map`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/MutableClassToInstanceMap.java
`&type->instance-map`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/reflect/ImmutableTypeToInstanceMap.java
`!type->instance-map`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/reflect/MutableTypeToInstanceMap.java
`!array-list-multimap`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/ArrayListMultimap.java
`!linked-list-multimap`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/LinkedListMultimap.java
`!linked-hash-multimap` = `!ordered-multimap`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/LinkedHashMultimap.java
`&list-multimap`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/ImmutableListMultimap.java
`&set-multimap`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/ImmutableSetMultimap.java
`!filtered-entry-multimap`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/FilteredEntryMultimap.java
`!filtered-entry-set-multimap`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/FilteredEntrySetMultimap.java
`!filtered-key-multimap`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/FilteredKeyMultimap.java
`!filtered-key-list-multimap`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/FilteredKeyListMultimap.java
`!filtered-key-set-multimap`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/FilteredKeySetMultimap.java
`!filtered-multimap-values`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/FilteredMultimapValues.java

quantum.core.refs

https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/util/concurrent/Atomics.java
`atom:double`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/util/concurrent/AtomicDouble.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/cache/LongAddables.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/cache/LongAdder.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/Interners.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/base/internal/Finalizer.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/util/concurrent/CycleDetectingLockFactory.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/util/concurrent/ListenerCallQueue.java

quantum.core.structures.set

`&set`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/RegularImmutableSet.java
`!hash-set`
https://github.com/google/closure-library/blob/v20170218/closure/goog/structs/set.js
`!hash-multiset`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/HashMultiset.java
`!tree-multiset`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/TreeMultiset.java
`!linked-hash-multiset` = `!ordered-multiset`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/LinkedHashMultiset.java
`&multiset`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/RegularImmutableMultiset.java
`!!hash-multiset`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/ConcurrentHashMultiset.java
`&enum-set`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/ImmutableEnumSet.java
`!enum-multiset`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/EnumMultiset.java
`&contiguous-set`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/RegularContiguousSet.java
`&sorted-set`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/RegularImmutableSortedSet.java
`&range-set`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/ImmutableRangeSet.java
`!tree-range-set`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/TreeRangeSet.java
`&desc-sorted-multiset`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/DescendingImmutableSortedMultiset.java
`&sorted-multiset`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/RegularImmutableSortedMultiset.java
`&desc-sorted-set`
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/DescendingImmutableSortedSet.java
`!string-set`
https://github.com/google/closure-library/blob/v20170218/closure/goog/structs/stringset.js

quantum.core.structures.tree

`!trie`
https://github.com/google/closure-library/blob/v20170218/closure/goog/structs/trie.js
`!quad-tree`
https://github.com/google/closure-library/blob/v20170218/closure/goog/structs/quadtree.js
`!tree-node`
https://github.com/google/closure-library/blob/v20170218/closure/goog/structs/treenode.js

quantum.data.text

https://github.com/google/closure-library/blob/v20170218/closure/goog/i18n/charpickerdata.js

quantum.data.text.format ; formatting unstructured text to other unstructured text

https://github.com/google/closure-library/blob/v20170218/closure/goog/format/format.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/style/bidi.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/i18n/bidi.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/string/linkify.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/i18n/bidiformatter.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/i18n/charlistdecompressor.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/i18n/collation.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/i18n/graphemebreak.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/i18n/messageformat.js
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/net/PercentEscaper.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/escape/CharEscaperBuilder.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/escape/Escapers.java

quantum.nlp.spell

https://github.com/google/closure-library/blob/v20170218/closure/goog/spell/spellcheck.js

quantum.nlp.pluralization

https://github.com/google/closure-library/blob/v20170218/closure/goog/i18n/pluralrules.js

quantum.data.text.time.format

https://github.com/google/closure-library/blob/v20170218/closure/goog/i18n/dateintervalformat.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/i18n/dateintervalpatterns.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/i18n/dateintervalpatternsext.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/i18n/dateintervalsymbols.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/i18n/dateintervalsymbolsext.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/i18n/datetimeformat.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/i18n/datetimeparse.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/i18n/datetimepatterns.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/i18n/datetimepatternsext.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/i18n/datetimesymbols.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/i18n/timezone.js

quantum.data.text.currency.format

https://github.com/google/closure-library/blob/v20170218/closure/goog/i18n/currency.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/i18n/currencycodemap.js

quantum.data.text.numeric.format

https://github.com/google/closure-library/blob/v20170218/closure/goog/i18n/compactnumberformatsymbols.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/i18n/compactnumberformatsymbolsext.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/i18n/numberformat.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/i18n/numberformatsymbols.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/i18n/numberformatsymbolsext.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/i18n/ordinalrules.js

quantum.data.generate

https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/DiscreteDomain.java

quantum.data.generate.text

https://github.com/google/closure-library/blob/v20170218/third_party/closure/goog/loremipsum/text/loremipsum.js

quantum.data.proto-buffer

https://github.com/google/closure-library/blob/v20170218/closure/goog/proto/proto.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/proto/serializer.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/proto2/descriptor.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/proto2/fielddescriptor.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/proto2/lazydeserializer.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/proto2/message.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/proto2/objectserializer.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/proto2/pbliteserializer.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/proto2/serializer.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/proto2/textformatserializer.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/proto2/util.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/streams/base64pbstreamparser.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/streams/pbjsonstreamparser.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/streams/pbstreamparser.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/streams/streamfactory.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/streams/xhrnodereadablestream.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/streams/xhrstreamreader.js

quantum.data.json

https://github.com/google/closure-library/blob/v20170218/closure/goog/json/evaljsonprocessor.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/json/hybrid.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/json/hybridjsonprocessor.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/json/json.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/json/nativejsonprocessor.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/streams/jsonstreamparser.js

quantum.data.json.format

https://github.com/google/closure-library/blob/v20170218/closure/goog/format/jsonprettyprinter.js

quantum.data.javascript.format

https://github.com/google/closure-library/blob/v20170218/closure/goog/html/safescript.js

quantum.data.css.format

https://github.com/google/closure-library/blob/v20170218/closure/goog/html/sanitizer/csssanitizer.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/html/safestyle.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/html/safestylesheet.js

quantum.data.xml

https://github.com/google/closure-library/blob/v20170218/closure/goog/dom/xml.js

quantum.data.xml.format

https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/xml/XmlEscapers.java

quantum.data.xml.html

https://github.com/google/closure-library/blob/v20170218/closure/goog/html/utils.js

quantum.data.xml.html.format

https://github.com/google/closure-library/blob/v20170218/closure/goog/format/htmlprettyprinter.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/html/sanitizer/htmlsanitizer.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/html/safehtmlformatter.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/html/safehtml.js
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/html/HtmlEscapers.java

quantum.data.xml.html.parse

https://github.com/google/closure-library/blob/v20170218/third_party/closure/goog/caja/string/html/htmlparser.js
https://github.com/google/closure-library/blob/v20170218/third_party/closure/goog/caja/string/html/htmlsanitizer.js

quantum.data.xml.html.pattern

https://github.com/google/closure-library/blob/v20170218/closure/goog/dom/pattern/matcher.js

quantum.security.cryptography

https://github.com/google/closure-library/blob/v20170218/closure/goog/crypt/aes.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/crypt/arc4.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/crypt/blockcipher.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/crypt/cbc.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/crypt/crypt.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/crypt/ctr.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/crypt/hmac.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/crypt/pbkdf2.js

quantum.core.error

# ensures that errors in JS actually have the stack; reports on whether they need to be sent to the server
https://github.com/google/closure-library/blob/v20170218/closure/goog/debug/error.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/debug/errorhandler.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/debug/errorreporter.js
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/base/Throwables.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/util/concurrent/UncaughtExceptionHandlers.java

quantum.core.error.assert

https://github.com/google/closure-library/blob/v20170218/closure/goog/asserts/asserts.js
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/base/Preconditions.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/base/Verify.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/CollectPreconditions.java

quantum.core.log

https://github.com/google/closure-library/blob/v20170218/closure/goog/debug/debug.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/debug/debugwindow.js
# for allowing functions e.g. to have trace logging
https://github.com/google/closure-library/blob/v20170218/closure/goog/debug/entrypointregistry.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/debug/formatter.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/debug/fpsdisplay.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/debug/logger.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/debug/logrecord.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/debug/logrecordserializer.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/debug/tracer.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/testing/stacktrace.js

quantum.telemetry

https://github.com/google/closure-library/blob/v20170218/closure/goog/stats/basicstat.js
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/base/Stopwatch.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/base/Ticker.java

quantum.telemetry.log

https://github.com/google/closure-library/blob/v20170218/closure/goog/messaging/loggerclient.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/messaging/loggerserver.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/log/log.js

quantum.core.time

https://github.com/google/closure-library/blob/v20170218/closure/goog/debug/relativetimeprovider.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/locale/timezonedetection.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/locale/timezonefingerprint.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/locale/timezonelist.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/date/date.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/date/datelike.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/date/daterange.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/date/duration.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/date/relative.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/date/relativewithplurals.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/date/utcdatetime.js

quantum.data.uri

https://github.com/google/closure-library/blob/v20170218/closure/goog/string/path.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/net/ipaddress.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/uri/uri.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/uri/utils.js
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/net/HostSpecifier.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/net/HostAndPort.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/net/InetAddresses.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/net/InternetDomainName.java

quantum.data.uri.format

https://github.com/google/closure-library/blob/v20170218/closure/goog/html/safeurl.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/html/trustedresourceurl.js
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/net/UrlEscapers.java

quantum.data.mime-type

https://github.com/google/closure-library/blob/v20170218/closure/goog/i18n/mime.js
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/net/MediaType.java

quantum.data.http

https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/net/HttpHeaders.java

quantum.core.data.primitive

https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/primitives/Booleans.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/primitives/Bytes.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/primitives/SignedBytes.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/primitives/UnsignedBytes.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/primitives/Chars.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/primitives/Doubles.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/primitives/Floats.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/primitives/Shorts.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/primitives/Ints.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/primitives/UnsignedInteger.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/primitives/UnsignedInts.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/primitives/Longs.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/primitives/UnsignedLong.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/primitives/UnsignedLongs.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/primitives/ParseRequest.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/primitives/Primitives.java

quantum.core.numeric

https://github.com/google/closure-library/blob/v20170218/closure/goog/math/math.js
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/math/MathPreconditions.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/math/BigIntegerMath.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/math/DoubleMath.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/math/DoubleUtils.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/math/IntMath.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/math/LongMath.java

quantum.math.sets ; TODO rename based on better knowledge of e.g. groups and semigroups and such

https://github.com/google/closure-library/blob/v20170218/closure/goog/math/box.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/math/coordinate.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/math/coordinate3.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/graphics/ext/coordinates.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/math/irect.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/math/rect.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/math/size.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/math/line.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/math/path.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/math/range.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/math/rangeset.js

quantum.numeric.transforms

https://github.com/google/closure-library/blob/v20170218/closure/goog/math/affinetransform.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/graphics/affinetransform.js

quantum.numeric.interpolation

https://github.com/google/closure-library/blob/v20170218/closure/goog/math/interpolator/interpolator1.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/math/interpolator/linear1.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/math/interpolator/pchip1.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/math/interpolator/spline1.js

quantum.numeric.optimization

https://github.com/google/closure-library/blob/v20170218/closure/goog/math/tdma.js

quantum.numeric.curves

https://github.com/google/closure-library/blob/v20170218/closure/goog/math/bezier.js

quantum.numeric.statistics.core

https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/math/Stats.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/math/PairedStats.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/math/StatsAccumulator.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/math/PairedStatsAccumulator.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/math/Quantiles.java

quantum.core.numeric.data

https://github.com/google/closure-library/blob/v20170218/closure/goog/math/integer.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/math/long.js
https://github.com/MikeMcl/big.js/

quantum.numeric.tensors

https://github.com/google/closure-library/blob/v20170218/closure/goog/vec/vec.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/vec/float32array.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/vec/float64array.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/math/vec2.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/vec/vec2.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/vec/vec2f.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/vec/vec2d.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/math/vec3.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/vec/vec3.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/vec/vec3f.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/vec/vec3d.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/vec/vec4.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/vec/vec4f.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/vec/vec4d.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/vec/quaternion.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/vec/ray.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/vec/mat3.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/vec/mat3f.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/vec/mat3d.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/vec/mat4.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/vec/mat4f.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/vec/mat4d.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/math/matrix.js


quantum.numeric.linear

https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/math/LinearTransformation.java

quantum.core.graph

https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/AbstractDirectedNetworkConnections.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/AbstractGraph.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/AbstractGraphBuilder.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/AbstractNetwork.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/AbstractUndirectedNetworkConnections.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/AbstractValueGraph.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/ConfigurableMutableGraph.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/ConfigurableMutableNetwork.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/ConfigurableMutableValueGraph.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/ConfigurableNetwork.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/ConfigurableValueGraph.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/DirectedGraphConnections.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/DirectedMultiNetworkConnections.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/DirectedNetworkConnections.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/EdgesConnecting.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/ElementOrder.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/EndpointPair.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/EndpointPairIterator.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/ForwardingGraph.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/Graph.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/GraphBuilder.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/GraphConnections.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/GraphConstants.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/Graphs.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/ImmutableGraph.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/ImmutableNetwork.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/ImmutableValueGraph.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/MapIteratorCache.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/MapRetrievalCache.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/MultiEdgesConnecting.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/MutableGraph.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/MutableNetwork.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/MutableValueGraph.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/Network.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/NetworkBuilder.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/NetworkConnections.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/UndirectedGraphConnections.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/UndirectedMultiNetworkConnections.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/UndirectedNetworkConnections.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/ValueGraph.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/graph/ValueGraphBuilder.java

quantum.hash

https://github.com/google/closure-library/blob/v20170218/closure/goog/crypt/blobhasher.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/crypt/hash.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/crypt/hash32.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/crypt/md5.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/crypt/sha1.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/crypt/sha2.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/crypt/sha224.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/crypt/sha256.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/crypt/sha2_64bit.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/crypt/sha384.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/crypt/sha512.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/crypt/sha512_256.js
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/hash/BloomFilter.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/hash/BloomFilterStrategies.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/hash/ChecksumHashFunction.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/hash/Crc32cHashFunction.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/hash/FarmHashFingerprint64.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/hash/Funnel.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/hash/Funnels.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/hash/HashCode.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/hash/HashFunction.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/hash/Hasher.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/hash/Hashing.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/hash/HashingInputStream.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/hash/HashingOutputStream.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/hash/MacHashFunction.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/hash/MessageDigestHashFunction.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/hash/Murmur3_128HashFunction.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/hash/Murmur3_32HashFunction.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/hash/PrimitiveSink.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/hash/SipHashFunction.java

quantum.core.string

https://github.com/google/closure-library/blob/v20170218/closure/goog/string/const.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/string/newlines.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/string/string.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/string/stringbuffer.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/string/stringformat.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/i18n/uchar.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/i18n/ucharnames.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/i18n/uchar/localnamefetcher.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/i18n/uchar/namefetcher.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/i18n/uchar/remotenamefetcher.js
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/base/CaseFormat.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/base/CharMatcher.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/base/SmallCharMatcher.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/base/Strings.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/base/Joiner.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/base/Splitter.java

quantum.core.string.encode

https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/base/Charsets.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/base/Ascii.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/base/Utf8.java

quantum.core.convert

https://github.com/google/closure-library/blob/v20170218/closure/goog/crypt/base64.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/crypt/basen.js
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/base/Converter.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/io/BaseEncoding.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/Serialization.java

quantum.core.cache

https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/cache/CacheBuilder.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/cache/CacheBuilderSpec.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/cache/CacheStats.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/cache/LocalCache.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/cache/RemovalCause.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/cache/RemovalListeners.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/cache/RemovalNotification.java

quantum.core.reflect

https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/reflect/Reflection.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/reflect/ClassPath.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/reflect/Element.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/reflect/Parameter.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/reflect/TypeResolver.java

quantum.core.type

https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/base/Absent.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/base/Defaults.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/base/Optional.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/base/Present.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/reflect/Types.java

quantum.core.enum

https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/base/Enums.java

quantum.core.compare

https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/base/Equivalence.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/AllEqualOrdering.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/ByFunctionOrdering.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/ComparatorOrdering.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/ExplicitOrdering.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/Comparators.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/ComparisonChain.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/CompoundOrdering.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/LexicographicalOrdering.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/NaturalOrdering.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/NullsFirstOrdering.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/NullsLastOrdering.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/ReverseNaturalOrdering.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/ReverseOrdering.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/UsingToStringOrdering.java

quantum.core.core

https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/base/MoreObjects.java
https://github.com/google/guava/blob/v21.0/guava/src/com/google/common/collect/BoundType.java

????

https://github.com/google/closure-library/blob/v20170218/closure/goog/math/exponentialbackoff.js

quantum.user-agent ; TODO meld with quantum.core.system

https://github.com/google/closure-library/blob/v20170218/closure/goog/useragent/adobereader.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/useragent/flash.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/useragent/iphoto.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/useragent/jscript.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/useragent/keyboard.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/useragent/platform.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/useragent/product.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/useragent/product_isversion.js
https://github.com/google/closure-library/blob/v20170218/closure/goog/useragent/useragent.js
