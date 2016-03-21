(ns ws-ldn-5.utils
  (:require
   [thi.ng.strf.core :as f]))

(defn timed
  [f] (let [t0 (f/now)] (f) (- (f/now) t0)))

(defn conj-max
  [vec limit x]
  (let [n (count vec)]
    (if (>= n limit)
      (conj (subvec vec (inc (- n limit))) x)
      (conj vec x))))

(defn run-with-timer
  [samples window f]
  (let [samples (vswap! samples conj-max window (timed f))]
    (/ (reduce + samples) (count samples))))
