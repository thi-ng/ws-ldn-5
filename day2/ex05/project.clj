(defproject ex05 "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.7.228"]]

  :plugins      [[lein-figwheel "0.5.0-6"]
                 [lein-cljsbuild "1.1.3" :exclusions [[org.clojure/clojure]]]]

  :clean-targets ^{:protect false} ["resources/public/js" "target"]
  
  :cljsbuild {:builds
              [{:source-paths ["src"]
                :id "dev"
                :compiler {:optimizations :simple
                           :pretty-print  true
                           ;;:pseudo-names  true
                           :output-to     "resources/public/js/main.js"
                           :modules       {:cljs-base {:output-to "resources/public/js/base.js"}
                                           :app       {:output-to "resources/public/js/app.js"
                                                       :entries #{"ex05.core"}}
                                           :worker    {:output-to "resources/public/js/worker.js"
                                                       :entries #{"worker"}}}}}]})
