(ns strojure.memoize-one.manual-cache
  "Memoize single value in the manually evicting cache."
  (:refer-clojure :exclude [memoize])
  (:require [strojure.memoize-one.core :refer [LoadingCache MemoizedRef evict get-ref]])
  (:import (clojure.lang IDeref IPending)))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- new-ref
  "Returns delay wrapped with the MemoizedRef protocol implementation."
  [f, cache!]
  (let [value-delay (delay (f))]
    (reify
      IDeref (deref [this] (try (.deref ^IDeref value-delay)
                                (catch Throwable e (evict this)
                                                   (throw e))))
      IPending (isRealized [_] (.isRealized ^IPending value-delay))
      MemoizedRef (evict [this]
                    (compare-and-set! cache! this (new-ref f cache!))
                    nil))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn memoize
  "Returns loading cache for the 0-arity function `f`. The value can be evicted
  manually using `(evict ref)`."
  [f]
  (let [cache! (atom nil)]
    (reset! cache! (new-ref f cache!))
    (reify LoadingCache
      (get-ref [_] (.deref ^IDeref cache!)))))

(comment
  (defn test-cache [side-effect]
    (memoize (fn [] (let [v (System/currentTimeMillis)]
                      (println "Create val" v (rand-int 100))
                      (side-effect)
                      v))))

  (let [c (test-cache #(Thread/sleep 100))]
    (dotimes [i 10]
      (future
        (Thread/sleep (* i 50))
        (let [v (get-ref c)]
          @v
          (evict v)))))

  (def c (test-cache #(Thread/sleep 100)))
  (def c (test-cache #(when (pos? (rand-int 2)) (throw (Exception. "Oops")))))

  (get-ref c)
  (time (deref (get-ref c)))
  (deref (get-ref c)) #_"10 ns"
  (doto (get-ref c) (deref) (evict))
  )

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
