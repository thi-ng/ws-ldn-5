(ns ws-ldn-5.naive
  (:require
   [ws-ldn-5.utils :as utils]
   [thi.ng.geom.webgl.animator :as anim]
   [thi.ng.domus.core :as dom]
   [thi.ng.strf.core :as f]))

(defn sum-neighbors
  [grid x y]
  (let [-x (dec x)
        +x (inc x)
        -y (dec y)
        +y (inc y)]
    (->> [[-y -x] [-y x] [-y +x]
          [y -x]         [y +x]
          [+y -x] [+y x] [+y +x]]
         (map #(get-in grid %))
         (reduce +))))

(defn sum-neighbors-transduce
  [grid x y]
  (let [-x (dec x)
        +x (inc x)
        -y (dec y)
        +y (inc y)]
    (transduce
     (map #(get-in grid %))
     +
     [[-y -x] [-y x] [-y +x]
      [y -x]         [y +x]
      [+y -x] [+y x] [+y +x]])))

(defn sum-neighbors-no-reduce
  [grid x y]
  (let [-x (dec x)
        +x (inc x)
        -y (dec y)
        +y (inc y)]
    (+ (+ (+ (+ (+ (+ (+ (get-in grid [-y -x])
                         (get-in grid [-y x]))
                      (get-in grid [-y +x]))
                   (get-in grid [y -x]))
                (get-in grid [y +x]))
             (get-in grid [+y -x]))
          (get-in grid [+y x]))
       (get-in grid [+y +x]))))

(defn sum-neighbors-nth
  [grid x y]
  (let [-x (dec x)
        +x (inc x)
        -y (nth grid (dec y))
        +y (nth grid (inc y))
        y  (nth grid y)]
    (+ (+ (+ (+ (+ (+ (+ (nth -y -x)
                         (nth -y x))
                      (nth -y +x))
                   (nth y -x))
                (nth y +x))
             (nth +y -x))
          (nth +y x))
       (nth +y +x))))

(defn life-step
  [grid x y cell]
  (let [;;neighbors (sum-neighbors grid x y) ;; 594ms
        ;;neighbors (sum-neighbors-transduce grid x y) ;; 780ms -> 680 ms
        ;;neighbors (sum-neighbors-no-reduce grid x y) ;; 270ms
        neighbors (sum-neighbors-nth grid x y) ;; 128ms
        ]
    (if (pos? cell)
      (if (or (== neighbors 2) (== neighbors 3)) 1 0)
      (if (== 3 neighbors) 1 0))))

(defn life
  [w h grid]
  (let [w' (- w 1)
        h' (- h 2)]
    (loop [grid' grid, y 1, x 1]
      (if (< x w')
        (recur (update-in grid' [y x] #(life-step grid x y %)) y (inc x))
        (if (< y h')
          (recur grid' (inc y) 1)
          grid')))))

(defn draw
  [canvas ctx w h grid]
  (let [w' (- w 1)
        h' (- h 2)]
    (set! (.-fillStyle ctx) "#000")
    (.fillRect ctx 0 0 w h)
    (set! (.-fillStyle ctx) "#f00")
    (loop [y 1, x 1]
      (if (< x w)
        (do (when (pos? (get-in grid [y x]))
              (.fillRect ctx x y 1 1))
            (recur y (inc x)))
        (if (< y h')
          (recur (inc y) 1)
          grid)))))

(defn main
  [canvas ctx width height]
  (let [grid    (->> #(if (< (rand) 0.25) 1 0)
                     (repeatedly (* width height))
                     (partition width)
                     (mapv vec)
                     volatile!)
        samples (volatile! [])]
    (anim/animate
     (fn [_ _]
       (let [t   (utils/timed
                  (fn []
                    (vswap! grid
                            #(->> %
                                  (life width height)
                                  (draw canvas ctx width height)))))
             s   (vswap! samples utils/conj-max 30 t)
             avg (/ (reduce + s) (count s))]
         (dom/set-text! (dom/by-id "stats") (f/format [(f/float 3) " ms"] avg))
         true)))))
