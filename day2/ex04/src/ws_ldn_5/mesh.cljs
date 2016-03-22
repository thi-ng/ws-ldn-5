(ns ws-ldn-5.mesh
  (:require
   [thi.ng.math.core :as m :refer [PI HALF_PI TWO_PI]]
   [thi.ng.geom.core :as g]
   [thi.ng.geom.vector :as v :refer [vec3]]
   [thi.ng.geom.attribs :as attr]
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

(def path-points (mapv cinquefoil (butlast (m/norm-range 400))))

(defn knot-simple
  []
  (-> path-points
      (ptf/sweep-mesh
       (g/vertices (circle 0.5) 7)
       {:mesh    (gl-mesh 16800 #{:fnorm :uv})
        :attribs {:uv attr/uv-tube}
        :align?  true
        :loop?   true
        :close? false})))
