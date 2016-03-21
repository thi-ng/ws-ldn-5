(ns ws-ldn-5.fastest
  (:require-macros
   [thi.ng.math.macros :as mm])
  (:require
   [ws-ldn-5.utils :as utils]
   [thi.ng.typedarrays.core :as ta]
   [thi.ng.geom.webgl.animator :as anim]
   [thi.ng.domus.core :as dom]
   [thi.ng.strf.core :as f]))

(defn sum-neighbors
  [grid idx stride]
  (let [t (- idx stride)
        b (+ idx stride)]
    (mm/add
     (aget grid (- t 1))
     (aget grid t)
     (aget grid (+ t 1))
     (aget grid (- idx 1))
     (aget grid (+ idx 1))
     (aget grid (- b 1))
     (aget grid b)
     (aget grid (+ b 1)))))

(defn life-step
  [grid idx stride]
  (let [neighbors (sum-neighbors grid idx stride)]
    (if (pos? (aget grid idx))
      (if (or (== neighbors 2) (== neighbors 3)) 1 0)
      (if (== 3 neighbors) 1 0))))

(defn life
  [w h [old new]]
  (let [w' (- w 1)
        h' (- h 2)]
    (loop [idx (+ w 1), x 1, y 1]
      (if (< x w')
        (do
          (aset new idx (life-step old idx w))
          (recur (inc idx) (inc x) y))
        (if (< y h')
          (recur (+ idx 2) 1 (inc y))
          [new old])))))

(defn draw
  [ctx img len [grid :as state]]
  (let [pixels (.-data img)]
    (loop [i 0, idx 4]
      (if (< i len)
        (do (aset pixels idx (* (aget grid i) 0xff))
            (recur (inc i) (+ idx 4)))
        (do (.putImageData ctx img 0 0)
            state)))))

(defn prepare-alpha
  [img len]
  (let [pixels (.-data img)
        len    (* 4 len)]
    (loop [i 3]
      (if (< i len)
        (do (aset pixels i 0xff) (recur (+ i 4)))
        img))))

(defn main
  [canvas ctx width height]
  (let [num     (* width height)
        grid    (->> #(if (< (rand) 0.5) 1 0)
                     (repeatedly num)
                     ta/uint8)
        grid2   (ta/uint8 num)
        img     (prepare-alpha (.createImageData ctx width height) num)
        state   (volatile! [grid grid2])
        samples (volatile! [])]
    (anim/animate
     (fn [_ _]
       (let [t   (utils/timed
                  (fn []
                    (vswap! state
                            #(->> %
                                  (life width height)
                                  (draw ctx img num)))))
             s   (vswap! samples utils/conj-max 30 t)
             avg (/ (reduce + s) (count s))]
         (dom/set-text! (dom/by-id "stats") (f/format [(f/float 3) " ms"] avg))
         true)))))
