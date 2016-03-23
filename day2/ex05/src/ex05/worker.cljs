(ns worker)

(.importScripts js/self "base.js")

(enable-console-print!)

(set! (.-onmessage js/self)
      (fn [msg]
        (prn "worker received: " (.-data msg))))

(js/setInterval
 (fn [] (prn "worker running..."))
 1000)
