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
