(ns ws-ldn-5.core
  (:require-macros
   [thi.ng.math.macros :as mm]
   [reagent.ratom :refer [reaction]]
   [cljs-log.core :refer [debug info warn severe]])
  (:require
   [ws-ldn-5.shaders :as wsshader]
   [ws-ldn-5.mesh :as wsmesh]
   [ws-ldn-5.texture :as wstex]
   [thi.ng.math.core :as m :refer [PI HALF_PI TWO_PI]]
   [thi.ng.geom.webgl.core :as gl]
   [thi.ng.geom.webgl.constants :as glc]
   [thi.ng.geom.webgl.animator :as anim]
   [thi.ng.geom.webgl.shaders :as sh]
   [thi.ng.geom.webgl.utils :as glu]
   [thi.ng.geom.webgl.camera :as cam]
   [thi.ng.geom.core :as g]
   [thi.ng.geom.vector :as v :refer [vec2 vec3]]
   [thi.ng.geom.matrix :as mat :refer [M44]]
   [thi.ng.geom.ptf :as ptf]
   [thi.ng.geom.utils :as gu]
   [thi.ng.color.core :as col]
   [thi.ng.color.gradients :as grad]
   [thi.ng.domus.core :as dom]
   [reagent.core :as reagent]))

(enable-console-print!)

(defonce app
  (reagent/atom
   {:player {:speed 0}}))

(defn camera-path
  [points frames]
  (let [up (nth frames 2)]
    {:pos       points
     :pos-index (gu/arc-length-index points)
     :up        up
     :up-index  (gu/arc-length-index up)}))

(defn camera-at-path-pos
  [{:keys [pos pos-index up up-index]} t delta view-rect]
  (let [t      (mod t 1.0)
        t'     (mod (+ t delta) 1.0)
        eye    (gu/point-at t pos pos-index)
        target (gu/point-at t' pos pos-index)
        up     (m/normalize (gu/point-at t up up-index))]
    (cam/perspective-camera
     {:eye    eye
      :target target
      :up     up
      :fov    90
      :aspect view-rect
      :far    10})))

(defn gl-component
  [props]
  (reagent/create-class
   {:component-did-mount
    (fn [this]
      (let [teeth     20
            gl        (gl/gl-context (reagent/dom-node this))
            view-rect (gl/get-viewport-rect gl)
            model     (-> (wsmesh/knot-simple)
                          (gl/as-webgl-buffer-spec {})
                          #_(cam/apply (cam/perspective-camera {:eye (vec3 0 0 5) :fov 90 :aspect view-rect}))
                          (assoc :shader (sh/make-shader-from-spec gl wsshader/tunnel-shader))
                          (gl/make-buffers-in-spec gl glc/static-draw)
                          (time))
            tex       (wstex/gradient-texture gl 4 1024 {:wrap [glc/clamp-to-edge glc/repeat]})
            cam       (camera-path wsmesh/path-points wsmesh/path-frames)]
        (reagent/set-state this {:active true})
        (anim/animate
         (fn [t frame]
           (let [cam           (camera-at-path-pos cam (* t 0.025) 0.02 view-rect)
                 tsin          (Math/sin (* t 0.2))
                 hue           (- (* 0.25 tsin) 0.1)
                 lum           (m/map-interval tsin -1 1 0.1 0.5)
                 [bgr bgg bgb] @(col/as-rgba (col/hsla hue 1 lum))]
             (gl/bind tex 0)
             (doto gl
               (gl/set-viewport view-rect)
               (gl/clear-color-and-depth-buffer bgr bgg bgb 1 1)
               (gl/draw-with-shader
                (-> model
                    (cam/apply cam)
                    (update :uniforms assoc
                            :time t
                            :Ka [bgr bgg bgb]
                            :Kf [bgr bgg bgb]
                            :lightPos (:eye cam)
                            :model M44)
                    (gl/inject-normal-matrix :model :view :normalMat)))))
           (:active (reagent/state this))))))
    :component-will-unmount
    (fn [this]
      (debug "unmount GL")
      (reagent/set-state this {:active false}))
    :reagent-render
    (fn [_] [:canvas (merge {:width 1280 :height 720} props)])}))

(defn main
  []
  (let [root [gl-component {}]]
    (reagent/render-component root (dom/by-id "app"))))

(main)
