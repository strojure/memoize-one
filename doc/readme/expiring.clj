(ns readme.expiring
  "Example of using of the expiring cache."
  (:require [strojure.memoize-one.core :as memoize]
            [strojure.memoize-one.expiring-cache :as cache]))

(def ^:private test-fn
  (fn []
    (let [v (System/currentTimeMillis)]
      (println (str "Create val " v " " (rand-int 100)))
      (Thread/sleep 100)
      #_(when (pos? (rand-int 2))
          (throw (Exception. "Oops")))
      v)))

(def ttl-cache
  "TTL cache for 20 seconds."
  (cache/memoize-ttl 20000 test-fn))

(comment
  (deref (memoize/get-ref ttl-cache))             ; Execution time mean : 17,081336 ns
  ;Create val 1654094164999 12
  #_1654094164999
  (memoize/evict (memoize/get-ref ttl-cache))
  #_nil
  )

(defn- odd-test-fn
  [x]
  (fn [] (odd? x)))

(def odd-cache
  "Cache expiring using `odd-test-fn`."
  (cache/memoize-with odd-test-fn test-fn))

(comment
  (deref (memoize/get-ref odd-cache))             ; Execution time mean : 16,548488 ns
  ;Create val 1654094271591 0
  #_1654094271591
  (memoize/evict (memoize/get-ref odd-cache))
  #_nil
  )

(defn- combined-test-fn
  "Expiration test for TTL or `odd`."
  [ttl]
  (fn [x] (let [p1 ((cache/ttl-test-fn ttl) x)
                p2 (odd-test-fn x)]
            (fn []
              (or (p1) (p2))))))

(def combined-cache
  "Cache expiring using `combined-test-fn`."
  (cache/memoize-with (combined-test-fn 20000) test-fn))

(comment
  (deref (memoize/get-ref combined-cache))        ; Execution time mean : 26,168754 ns
  ;Create val 1654094360990 6
  #_1654094360990
  (memoize/evict (memoize/get-ref combined-cache))
  #_nil
  )
