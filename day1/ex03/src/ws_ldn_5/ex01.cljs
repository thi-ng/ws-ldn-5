(ns ws-ldn-5.ex01
  (:require-macros
   [thi.ng.math.macros :as mm])
  (:require
   [thi.ng.math.core :as m :refer [PI HALF_PI TWO_PI]]
   [thi.ng.geom.webgl.core :as gl]
   [thi.ng.geom.webgl.constants :as glc]
   [thi.ng.geom.webgl.animator :as anim]
   [thi.ng.geom.webgl.buffers :as buf]
   [thi.ng.geom.webgl.shaders :as sh]
   [thi.ng.geom.webgl.utils :as glu]
   [thi.ng.geom.webgl.glmesh :as glm]
   [thi.ng.geom.webgl.camera :as cam]
   [thi.ng.geom.core :as g]
   [thi.ng.geom.vector :as v :refer [vec2 vec3]]
   [thi.ng.geom.matrix :as mat :refer [M44]]
   [thi.ng.geom.aabb :as a]
   [thi.ng.geom.attribs :as attr]
   [thi.ng.typedarrays.core :as arrays]
   [thi.ng.color.core :as col]
   [thi.ng.geom.webgl.shaders.basic :as basic]
   [thi.ng.geom.circle :as c]
   [thi.ng.geom.polygon :as poly]))

(defn init-stats
  []
  (let [stats (js/Stats.)
        sdom  (.call (aget stats "getDomElement") stats)]
    (.appendChild (.-body js/document) sdom)
    (.setAttribute sdom "class" "stats")
    stats))

(defn update-stats
  [stats]
  (.call (aget stats "update") stats))

(def shader-spec
  {:vs       "void main() { gl_Position=proj * view * model * vec4(position, 0.0, 1.0); }"
   :fs       "void main() { gl_FragColor = color; }"
   :uniforms {:proj  :mat4
              :model [:mat4 M44]
              :view  [:mat4 M44]
              :color [:vec4 [0 0 0 1]]}
   :attribs  {:position :vec2}})

(defn ^:export demo
  []
  (enable-console-print!)
  (let [teeth     20
        gl        (gl/gl-context "main")
        view-rect (gl/get-viewport-rect gl)
        model     (-> (poly/cog 0.5 teeth [0.9 1 1 0.9])
                      (gl/as-webgl-buffer-spec {:normals false})
                      (gl/make-buffers-in-spec gl glc/static-draw)
                      (assoc-in [:uniforms :proj] (gl/ortho view-rect))
                      (assoc :shader (sh/make-shader-from-spec gl shader-spec)))
        stats     (init-stats)]
    (anim/animate
     (fn [t frame]
       (doto gl
         (gl/set-viewport view-rect)
         (gl/clear-color-and-depth-buffer 1 0.98 0.95 1 1)
         ;; draw left polygon
         (gl/draw-with-shader
          (update model :uniforms merge
                  {:model (-> M44 (g/translate -0.48 0 0) (g/rotate t))
                   :color [0 1 1 1]}))
         ;; draw right polygon
         (gl/draw-with-shader
          (update model :uniforms merge
                  {:model (-> M44 (g/translate 0.48 0 0) (g/rotate (- (+ t (/ HALF_PI teeth)))))
                   :color [1 0 0 1]})))
       (update-stats stats)
       true))))
