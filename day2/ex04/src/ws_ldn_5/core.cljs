(ns ws-ldn-5.core
  (:require-macros
   [reagent.ratom :refer [reaction]]
   [cljs-log.core :refer [debug info warn severe]])
  (:require
   [ws-ldn-5.game :as game]
   [thi.ng.geom.webgl.animator :as anim]
   [thi.ng.domus.core :as dom]
   [reagent.core :as reagent]))

(enable-console-print!)

(defn gl-component
  [props]
  (reagent/create-class
   {:component-did-mount
    (fn [this]
      (reagent/set-state this {:active true})
      ((:init props) this)
      (anim/animate ((:loop props) this)))
    :component-will-unmount
    (fn [this]
      (debug "unmount GL")
      (reagent/set-state this {:active false}))
    :reagent-render
    (fn [_]
      [:canvas
       (merge
        {:width (.-innerWidth js/window)
         :height (.-innerHeight js/window)
         :on-mouse-move (fn [e] (game/update-player-pos! (.-clientX e)))}
        props)])}))

(defn main
  []
  (let [root [gl-component {:init game/init-game :loop game/game-loop}]]
    (reagent/render-component root (dom/by-id "app"))))

(main)
