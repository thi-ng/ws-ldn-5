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
   [thi.ng.geom.webgl.buffers :as buf]
   [thi.ng.geom.webgl.shaders :as sh]
   [thi.ng.geom.webgl.shaders.image :as img]
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
   {:player {:speed 0
             :pos   [0 0]}}))

(defn update-player-pos-x!
  [x]
  (swap! app assoc-in [:player :pos 0] (/ x 1280))) ;; FIXME

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

(defn naive-cam-at-pos
  [{:keys [pos pos-index]} t delta view-rect]
  (let [t      (mod t 1.0)
        t'     (mod (+ t delta) 1.0)
        eye    (gu/point-at t pos pos-index)
        target (gu/point-at t' pos pos-index)
        up     (vec3 0 1 0)]
    (cam/perspective-camera
     {:eye    eye
      :target target
      :up     up
      :fov    90
      :aspect view-rect
      :far    10})))

(defn compute-player-worldpos
  [frames t player-state]
  (let [player-theta (m/map-interval
                      (get-in player-state [:pos 0])
                      1 0 (m/radians 60) (m/radians 300))
        n            (count (first frames))
        t            (mod t 1.0)
        t*n          (* t n)
        i            (int t*n)
        j            (mod (inc i) n)
        fract        (- t*n i)

        frame-pa     (nth (frames 0) i)
        frame-na     (nth (frames 2) i)
        frame-ba     (nth (frames 3) i)

        frame-pb     (nth (frames 0) j)
        frame-nb     (nth (frames 2) j)
        frame-bb     (nth (frames 3) j)
        a            (->> (vec2 0.3 player-theta)
                          g/as-cartesian
                          (ptf/sweep-point frame-pa frame-na frame-ba))
        b            (->> (vec2 0.3 player-theta)
                          g/as-cartesian
                          (ptf/sweep-point frame-pb frame-nb frame-bb))]
    (m/mix a b fract)))

(defn gl-component
  [props]
  (reagent/create-class
   {:component-did-mount
    (fn [this]
      (let [gl         (gl/gl-context (reagent/dom-node this))
            view-rect  (gl/get-viewport-rect gl)
            model      (-> (wsmesh/knot-simple)
                           (gl/as-webgl-buffer-spec {})
                           (assoc :shader (sh/make-shader-from-spec gl wsshader/tunnel-shader))
                           (gl/make-buffers-in-spec gl glc/static-draw)
                           (time))
            player     (-> (wsmesh/player)
                           (gl/as-webgl-buffer-spec {})
                           (assoc :shader (sh/make-shader-from-spec gl wsshader/tunnel-shader))
                           (gl/make-buffers-in-spec gl glc/static-draw))
            tex        (wstex/gradient-texture gl 32 1024 {:wrap [glc/clamp-to-edge glc/repeat]})
            logo-ready (volatile! false)
            logo       (buf/load-texture
                        gl {:callback (fn [tex img] (vreset! logo-ready true))
                            :src      "img/sjo512.png"
                            :format   glc/rgba
                            :flip     false})
            logo-ov    (img/make-shader-spec
                        gl {:view-port view-rect
                            :pos       [384 104]
                            :width     512
                            :height    512
                            :state     {:tex logo}})
            cam        (camera-path wsmesh/path-points wsmesh/path-frames)]
        (reagent/set-state this {:active true})
        (anim/animate
         (fn [t frame]
           (let [cam           (camera-at-path-pos cam (* t 0.025) 0.02 view-rect)
                 ;;cam           (naive-cam-at-pos cam (* t 0.025) 0.02 view-rect)
                 tsin          (Math/sin (+ PI (* t 0.2)))
                 hue           0.666 ;(- (* 0.25 tsin) 0.1)
                 lum           (m/map-interval tsin -1 1 0.1 0.5)
                 [bgr bgg bgb] @(col/as-rgba (col/hsla hue 1 lum))
                 player-pos    (compute-player-worldpos
                                wsmesh/path-frames
                                (+ (* t 0.025) 0.02)
                                (:player @app))
                 player-tx     (-> M44 (g/translate player-pos))]
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
                    (gl/inject-normal-matrix :model :view :normalMat)))
               ;; draw player
               (gl/draw-with-shader
                (-> player
                    (cam/apply cam)
                    (update :uniforms assoc
                            :time 0
                            :lightPos (:eye cam)
                            :model player-tx)
                    (gl/inject-normal-matrix :model :view :normalMat))))
             #_(when @logo-ready
                 (img/draw gl logo-ov)))
           (:active (reagent/state this))))))
    :component-will-unmount
    (fn [this]
      (debug "unmount GL")
      (reagent/set-state this {:active false}))
    :reagent-render
    (fn [_] [:canvas
            (merge
             {:width 1280
              :height 720
              :on-mouse-move (fn [e] (update-player-pos-x! (.-clientX e)))}
             props)])}))

(defn main
  []
  (let [root [gl-component {}]]
    (reagent/render-component root (dom/by-id "app"))))

(main)
