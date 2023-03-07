# memoize-one

Clojure library for memoization of the single value.

[![Clojars Project](https://img.shields.io/clojars/v/com.github.strojure/memoize-one.svg)](https://clojars.org/com.github.strojure/memoize-one)

## Design goals

* Simple utility for caching single value accessed from concurrent threads.
* Lazy initialization of the cached value on access.
* Performant access to the cached value.
* Optional cache expiration.
* Native implementation without external dependencies.

## Basic usage

```clojure
(ns readme.manual
  "Example of using of the manual cache."
  (:require [strojure.memoize-one.core :as memoize]
            [strojure.memoize-one.manual-cache :as cache]))

(defn- test-cache
  [side-effect]
  (cache/memoize (fn [] (let [v (System/currentTimeMillis)]
                          (println (str "Create val " v " " (rand-int 100)))
                          (side-effect)
                          v))))

(comment
  (let [c (test-cache #(Thread/sleep 100))]
    (dotimes [i 10]
      (future
        (Thread/sleep (* i 50))
        (let [v (memoize/get-ref c)]
          @v
          (memoize/evict v)))))

  (def c (test-cache #(Thread/sleep 100)))
  (def c (test-cache #(when (pos? (rand-int 2)) (throw (Exception. "Oops")))))

  (memoize/get-ref c)

  (deref (memoize/get-ref c))                     ; Execution time mean : 7.299272 ns
  #_1654093121258

  (.deref (memoize/get-ref c))                    ; Execution time mean : 6.074256 ns

  (memoize/get-value c)                           ; Execution time mean : 6.343759 ns

  (doto (memoize/get-ref c) (deref) (memoize/evict))
  )

```

```clojure
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
```
