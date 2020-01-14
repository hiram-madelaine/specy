(ns specy.protocols
  (:require [#?(:clj  clojure.spec.gen.alpha
                :cljs cljs.spec.gen.alpha) :as gen]
            [#?(:clj  clojure.spec.alpha
                :cljs cljs.spec.alpha) :as s]
            [clojure.test.check.generators :as check-gen]
            [tick.alpha.api :as t]
            [specy.time :as time]
            [specy.uuid :as uuid]
            )
  ;; (:refer-clojure :exclude [range iterate format max min]) ;; for java-time to exclude conflicting fn name
  )

(defprotocol EventBus
  "A simple pub/sub interface for publishing event indexed with key k (the key value must be extracted from the event and is up to the implementation, this ensure homogeneity of the key extraction from the event)"
  (subscribe
    [_ f]
    [_ k f] "Subscribe to events, optionaly filtered with the key k, then callback the function f, the return of the subscribe function is implementation dependant")
  (publish! [_ event] "Publish the event to that bus, the key will be extracted depending on the implementation (usually through a function given at construction or hardcoded)")
  (stop [_] "Lifecycle operation as event bus is likely in need of some cleanup after use (subscribers for instance...)"))


(defprotocol Valueable
  "Protocol for common value operations"
  (to-string [this] "Returns a standard string representation of that value that can be parsed with value-of function"))


(defprotocol Localizable
  "Protocol for handling i18n, l12n"
  (localized-string [this locale]
                    [this locale f]
                    "Returns a string localized with the provided locale and the optionnal format argument"))
   
(defprotocol BuildingBlocksRepository
  "Protocol for building blocks access"
  (store! [repo block])
  (get-entity [repo name])
  (get-value  [repo name])
  (get-command [repo name])
  (get-event [repo name])
  (get-query [repo name])
  (find-value-by-class [repo x]))
