(defproject ws-ldn-5-ex04 "0.1.0-SNAPSHOT"
  :description  "thi.ng Clojurescript workshop WS-LDN-5"
  :url          "http://workshop.thi.ng"
  :license      {:name "Eclipse Public License"
                 :url "http://www.eclipse.org/legal/epl-v10.html"}

  :min-lein-version "2.5.3"

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [org.clojure/core.async "0.2.374"
                  :exclusions [org.clojure/tools.reader]]
                 [thi.ng/geom "0.0.1046"]
                 [thi.ng/domus "0.3.0-SNAPSHOT"]
                 [reagent "0.5.1"]
                 [cljsjs/localforage "1.3.1-0"]]

  :plugins      [[lein-figwheel "0.5.0-6"]
                 [lein-cljsbuild "1.1.3" :exclusions [[org.clojure/clojure]]]]

  :source-paths ["src"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :cljsbuild {:builds
              [{:id           "dev1"
                :source-paths ["src"]
                :figwheel     true ;; {:on-jsload "ws-ldn-5.core/on-js-reload"}
                :compiler     {:main                 ws-ldn-5.core
                               :asset-path           "js/compiled/out"
                               :output-to            "resources/public/js/compiled/app.js"
                               :output-dir           "resources/public/js/compiled/out"
                               :source-map-timestamp true}}
               {:id           "min"
                :source-paths ["src"]
                :compiler     {:output-to     "resources/public/js/compiled/app.js"
                               :optimizations :advanced
                               :pretty-print  false}}]}

  :figwheel {:css-dirs ["resources/public/css"]
             ;; :ring-handler hello_world.server/handler
             })
