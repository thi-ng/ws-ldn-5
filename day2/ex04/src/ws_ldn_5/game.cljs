(ns ws-ldn-5.game
  (:require-macros
   [thi.ng.math.macros :as mm]
   [cljs-log.core :refer [debug info warn severe]])
  (:require
   [ws-ldn-5.camera :as wscam]
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
   [reagent.core :as reagent]))

(defonce app (atom {}))

(defn update-player-pos!
  [x]
  (swap! app assoc-in [:player :target-pos] (/ x (g/width (:view @app)))))


(defn compute-player-worldpos
  [[path _ norms binorms] x t]
  (let [player-theta (m/map-interval x 1 0 (m/radians 60) (m/radians 300))
        n            (count path)
        t            (mod t 1.0)
        t*n          (* t n)
        i            (int t*n)
        j            (mod (inc i) n)
        fract        (- t*n i)
        pa           (nth path i)
        na           (nth norms i)
        ba           (nth binorms i)
        pb           (nth path j)
        nb           (nth norms j)
        bb           (nth binorms j)
        a            (->> (vec2 0.4 player-theta)
                          g/as-cartesian
                          (ptf/sweep-point pa na ba))
        b            (->> (vec2 0.4 player-theta)
                          g/as-cartesian
                          (ptf/sweep-point pb nb bb))
        pos          (m/mix a b fract)
        fwd          (m/normalize (m/- b a))
        up           (m/normalize (m/mix na nb fract))
        right        (m/normalize (m/mix ba bb fract))]
    (-> M44
        (g/translate pos)
        (m/* (mat/matrix44 ;; TODO add as mat/rotation-matrix-from-axes
              (right 0) (right 1) (right 2) 0
              (up 0) (up 1) (up 2) 0
              (fwd 0) (fwd 1) (fwd 2) 0
              0 0 0 1))
        (g/rotate-z
         (m/map-interval x 0 1 (m/radians -90) (m/radians 90))))))

(defn init-game
  [this]
  (let [gl         (gl/gl-context (reagent/dom-node this))
        view-rect  (gl/get-viewport-rect gl)
        tunnel-tex (wstex/gradient-texture gl 32 1024 {:wrap [glc/clamp-to-edge glc/repeat]})
        logo       (buf/load-texture
                    gl {:callback (fn [tex img] (swap! app assoc-in [:flags :logo-ready] true))
                        :src      "img/sjo512.png"
                        :format   glc/rgba
                        :flip     false})]
    (reset! app
            {:player {:speed      0.00035
                      :pos        0.5
                      :target-pos 0.5
                      :track-pos  0
                      :laps       0}
             :cam    (wscam/camera-path wsmesh/path-points wsmesh/path-frames)
             :gl     gl
             :view   view-rect
             :scene  {:tunnel     (-> (wsmesh/knot-simple)
                                      (gl/as-webgl-buffer-spec {})
                                      (assoc :shader (sh/make-shader-from-spec gl wsshader/tunnel-shader))
                                      (assoc-in [:shader :state :tex] tunnel-tex)
                                      (gl/make-buffers-in-spec gl glc/static-draw)
                                      (time))
                      :player     (-> (wsmesh/player)
                                      (gl/as-webgl-buffer-spec {})
                                      (assoc :shader (sh/make-shader-from-spec gl wsshader/tunnel-shader))
                                      (gl/make-buffers-in-spec gl glc/static-draw))
                      :tunnel-tex tunnel-tex
                      :logo       (img/make-shader-spec
                                   gl {:view-port view-rect
                                       :pos       [(/ (- (g/width view-rect) 512) 2)
                                                   (/ (- (g/height view-rect) 512) 2)]
                                       :width     512
                                       :height    512
                                       :state     {:tex logo}})}
             :flags  {:active     true
                      :logo-ready false}})))

(defn update-game-state!
  []
  (swap!
   app
   (fn [app]
     (let [{:keys [pos target-pos track-pos speed laps]} (:player app)
           tp   (+ track-pos speed)
           laps (if (>= tp 1.0) (inc laps) laps)
           tp   (if (>= tp 1.0) (dec tp) tp)
           xp   (m/mix* pos target-pos 0.1)
           cam  (wscam/camera-at-path-pos (:cam app) (- tp 0.015) 0.02 (:view app))]
       (-> app
           (update :player merge
                   {:pos       xp
                    :track-pos tp
                    :laps      laps
                    :tx        (compute-player-worldpos wsmesh/path-frames xp tp)})
           (assoc-in [:scene :cam] cam))))))

(defn game-loop
  [this]
  (fn [t frame]
    (update-game-state!)
    (let [{:keys [gl view player scene flags]} @app
          cam           (:cam scene)
          lum           (m/map-interval (Math/sin (+ PI (* t 0.2))) -1 1 0.1 0.5)
          [bgr bgg bgb] @(col/as-rgba (col/hsla 0.6666 1 lum))]
      #_(gl/bind (:tunnel-tex scene) 0)
      (doto gl
        (gl/set-viewport view)
        (gl/clear-color-and-depth-buffer bgr bgg bgb 1 1)
        (gl/draw-with-shader
         (-> (:tunnel scene)
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
         (-> (:player scene)
             (cam/apply cam)
             (update :uniforms assoc
                     :time 0
                     :lightPos (:eye cam)
                     :m 0.2
                     :s 0.9
                     :model (:tx player))
             (gl/inject-normal-matrix :model :view :normalMat))))
      (when (:logo-ready flags)
        (img/draw gl (:logo scene)))
      (:active (reagent/state this)))))
