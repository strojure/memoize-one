(ns strojure.memoize-one.core
  "Memoization of the single value.")

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defprotocol LoadingCache
  "Functions for the cached computation."

  (get-ref [cache]
    "Returns cached value reference to `deref`."))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defprotocol MemoizedRef
  "Cache related functions for the wrapped cached reference."

  (evict [ref]
    "Invalidates this reference in the cache. Returns nil."))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn get-another-ref
  "Invalidates memoized ref and returns actual (probably new) ref from the cache."
  [cache ref]
  (evict ref)
  (get-ref cache))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
