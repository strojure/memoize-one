(ns strojure.memoize-one.core
  "Memoization of the single value.")

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defprotocol LoadingCache
  "Functions for the cached computation."

  (get-ref
    ^clojure.lang.IDeref [cache]
    "Returns cached value reference to `deref`."))

(defprotocol MemoizedRef
  "Cache related functions for the wrapped cached reference."

  (evict
    [ref]
    "Invalidates this reference in the cache. Returns nil."))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn get-value
  "Returns result of `(deref (get-ref cache))`."
  {:inline (fn [cache] `(.deref (get-ref ~cache)))
   :added "1.1"}
  [cache]
  (.deref (get-ref cache)))

(defn get-another-ref
  "Invalidates memoized ref and returns actual (probably new) ref from the cache."
  [cache ref]
  (evict ref)
  (get-ref cache))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
