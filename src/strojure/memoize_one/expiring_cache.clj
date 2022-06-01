(ns strojure.memoize-one.expiring-cache
  "Memoize single value in the automatically evicting cache."
  (:require [strojure.memoize-one.core :refer [LoadingCache MemoizedRef evict get-ref]])
  (:import (clojure.lang IDeref IPending)))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defprotocol ExpiringRef
  "Memoized reference which can expire."
  (expired? [ref]))

(defn- new-ref
  "Returns delay wrapped with the MemoizedRef and ExpiringRef protocol
  implementation."
  [f, cache!, test-fn]
  (let [expired-fn! (volatile! nil)
        value-delay (delay (doto (f)
                             (as-> value (vreset! expired-fn! (test-fn value)))))]
    (reify
      IDeref (deref [this] (try (.deref ^IDeref value-delay)
                                (catch Throwable e (evict this)
                                                   (throw e))))
      IPending (isRealized [_] (.isRealized ^IPending value-delay))
      MemoizedRef (evict [this]
                    (compare-and-set! cache! this (new-ref f cache! test-fn))
                    nil)
      ExpiringRef (expired? [_] (and (.isRealized value-delay)
                                     (when-some [f (.deref ^IDeref expired-fn!)]
                                       (f)))))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn memoize-with
  "Returns loading cache for the 0-arity function `f`. The value is evicted
  automatically using `test-fn`.

  The `test-fn` is a function on cached x which returns 0-arity function
  answering if x is expired and should be evicted:

  `(test-fn x)` -> `(fn [] ^Boolean result)`

  "
  [test-fn, f]
  (let [cache! (atom nil)]
    (reset! cache! (new-ref f cache! test-fn))
    (reify LoadingCache
      (get-ref [_] (let [current-ref (.deref ^IDeref cache!)]
                     (if (expired? current-ref)
                       (do (evict current-ref)
                           (.deref ^IDeref cache!))
                       current-ref))))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn ttl-test-fn
  "TTL millis test-fn."
  [ttl]
  (fn [_x] (let [expire (+ ttl (System/currentTimeMillis))]
             (fn []
               (< expire (System/currentTimeMillis))))))

(defn memoize-ttl
  "Returns loading cache for the 0-arity function `f`. The value is evicted
  automatically after `ttl` millis."
  [ttl f]
  (memoize-with (ttl-test-fn ttl) f))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(comment
  "Combine multiple test-fns in one:"

  (defn odd-test-fn
    [x]
    (fn [] (odd? x)))

  (defn combined-test-fn
    [ttl]
    (fn [x] (let [p1 ((ttl-test-fn ttl) x)
                  p2 (odd-test-fn x)]
              (fn []
                (or (p1) (p2)))))))

(comment
  (def f (fn [] (let [v (System/currentTimeMillis)]
                  (println "Create val" v (rand-int 100))
                  (Thread/sleep 100)
                  #_(when (pos? (rand-int 2))
                      (throw (Exception. "Oops")))
                  v)))
  (def c (memoize-ttl 20000 f))
  (def c (memoize-with odd-test-fn f))
  (def c (memoize-with (combined-test-fn 200) f))
  (get-ref c)
  (deref (get-ref c)) #_["20 ns" (memoize-ttl 20000 f) (memoize-with odd-test-fn f)
                         "30 ns" (combined-test-fn 20000)]
  (evict (get-ref c))

  (dotimes [i 10]
    (future
      (Thread/sleep (* i 50))
      [i (deref (get-ref c))]))
  )

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
