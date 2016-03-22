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
   [thi.ng.color.core :as col]
   [thi.ng.color.gradients :as grad]
   [thi.ng.domus.core :as dom]
   [reagent.core :as reagent]))

(enable-console-print!)

(defonce app (reagent/atom {}))

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
                          (cam/apply (cam/perspective-camera {:eye (vec3 0 0 5) :fov 90 :aspect view-rect}))
                          (assoc :shader (sh/make-shader-from-spec gl wsshader/tunnel-shader))
                          (gl/make-buffers-in-spec gl glc/static-draw)
                          (time))
            tex       (wstex/gradient-texture gl 4 1024 {:wrap [glc/clamp-to-edge glc/repeat]})]
        (reagent/set-state this {:anim true})
        (anim/animate
         (fn [t frame]
           (gl/bind tex 0)
           (doto gl
             (gl/set-viewport view-rect)
             (gl/clear-color-and-depth-buffer 0.0 0.0 0.1 1 1)
             (gl/draw-with-shader
              (-> model
                  (update :uniforms assoc
                          :time t
                          :m (+ 0.21 (* 0.2 (Math/sin (* t 0.5))))
                          :model (-> M44 (g/rotate-x (* t 0.36)) (g/rotate-y t)))
                  (gl/inject-normal-matrix :model :view :normalMat))))
           (:anim (reagent/state this))))))
    :component-will-unmount
    (fn [this]
      (debug "unmount GL")
      (reagent/set-state this {:anim false}))
    :reagent-render
    (fn [_] [:canvas (merge {:width 1280 :height 720} props)])}))

(defn main
  []
  (let [root [gl-component {}]]
    (reagent/render-component root (dom/by-id "app"))))

(main)
