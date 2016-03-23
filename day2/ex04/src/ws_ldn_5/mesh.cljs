(ns ws-ldn-5.mesh
  (:require
   [thi.ng.math.core :as m :refer [PI HALF_PI TWO_PI]]
   [thi.ng.geom.core :as g]
   [thi.ng.geom.vector :as v :refer [vec3]]
   [thi.ng.geom.attribs :as attr]
   [thi.ng.geom.aabb :as a]
   [thi.ng.geom.circle :refer [circle]]
   [thi.ng.geom.webgl.glmesh :refer [gl-mesh]]
   [thi.ng.geom.ptf :as ptf]))

(defn cinquefoil
  [t]
  (let [t  (* t TWO_PI)
        pt (* 2.0 t)
        qt (* 5.0 t)
        qc (+ 3.0 (Math/cos qt))]
    (v/vec3 (* qc (Math/cos pt)) (* qc (Math/sin pt)) (Math/sin qt))))

(def path-points
  "Evaluated points of cinquefoil knot"
  (mapv cinquefoil (m/norm-range 400)))

(def path-frames
  "Precompute Parallel Transport Frames for each path point"
  (-> path-points ptf/compute-frames ptf/align-frames))

(defn solidify-segment
  [res seg]
  (let [off   (- (count seg) 2)
        front (loop [acc [], i 0, j off]
                (if (< i (dec res))
                  (let [[averts aattr] (nth seg i)
                        [bverts battr] (nth seg j)
                        auv            (:uv aattr)
                        buv            (:uv battr)
                        f1             [[(nth averts 1) (nth averts 0) (nth bverts 1) (nth bverts 0)]
                                        {:uv [(nth auv 1) (nth auv 0) (nth buv 1) (nth buv 0)]}]]
                    (recur (conj acc f1) (inc i) (dec j)))
                  acc))]
    (concat seg front)))

(defn knot-simple
  []
  (let [res     7
        profile (concat (reverse (g/vertices (circle 0.5) res))
                        (g/vertices (circle 0.55) res))
        attribs {:uv attr/uv-tube}
        opts    {:loop? true :close? true}]
    (->> path-frames
         (ptf/sweep-profile profile attribs opts)
         (partition (* res 2))
         (take-nth 2)
         (mapcat #(solidify-segment 10 %))
         (g/into (gl-mesh 16800 #{:fnorm :uv})))))

(defn player
  []
  (-> (a/aabb 0.1)
      (g/as-mesh {:mesh    (gl-mesh 12 #{:fnorm :uv})
                  :attribs {:uv attr/uv-tube}})))
