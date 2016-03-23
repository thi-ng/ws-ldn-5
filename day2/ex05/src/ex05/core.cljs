(ns ex05.core)

(enable-console-print!)

(defn main
  []
  (let [worker (js/Worker. "js/worker.js")]
    (.postMessage worker "Get on with it!")))

(main)
