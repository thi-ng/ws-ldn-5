(ns ws-ldn-5.core
  (:require
   [ws-ldn-5.naive :as naive]
   [ws-ldn-5.faster :as faster]
   [ws-ldn-5.fast :as fast]
   [thi.ng.domus.core :as dom]))

(enable-console-print!)

(def width 256)
(def height 256)

(defn main
  []
  (let [canvas (dom/create-dom!
                [:canvas {:width width :height height}]
                (dom/by-id "app"))
        ctx    (.getContext canvas "2d")]
    (naive/main canvas ctx width height)    ;; 594 ms
    ;;(faster/main canvas ctx width height) ;; 84ms
    ;;(fast/main canvas ctx width height)   ;; 2.3ms
    ))

(main)

