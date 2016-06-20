(defproject pgw "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.9.0-alpha6"]
                 [org.clojure/clojurescript "1.9.36" :scope "provided"]

                 [cheshire "5.6.1"]
                 [cljs-http "0.1.41"]
                 [com.cognitect/transit-clj "0.8.285"]
                 [com.cognitect/transit-cljs "0.8.237"]
                 [com.zaxxer/HikariCP "2.4.3"]
                 [io.forward/yaml "1.0.3"]
                 [garden "1.3.2"]
                 [hiccup "1.0.5"]
                 [honeysql "0.6.2"]
                 [http-kit "2.1.19"]
                 [org.clojure/core.async "0.2.382"]
                 [org.clojure/java.jdbc "0.5.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.postgresql/postgresql "9.4.1208.jre7"]
                 [reagent "0.5.1" :exclusions [org.clojure/tools.reader]]
                 [reagent-utils "0.1.8"]
                 [ring "1.5.0"]
                 [environ "1.0.1"]
                 [ring/ring-defaults "0.2.1"]
                 [route-map "0.0.3"]

                 [clj-antlr "0.2.2"]]

  :plugins [[lein-environ "1.0.2"]
            [lein-cljsbuild "1.1.1"]
            [lein-ancient "0.6.8"]]

  :ring {:handler pgw.core/app
         :uberwar-name "pgw.war"}

  :min-lein-version "2.5.0"

  :uberjar-name "pgw.jar"

  :main pgw.core

  :clean-targets ^{:protect false}
  [:target-path
   [:cljsbuild :builds :app :compiler :output-dir]
   [:cljsbuild :builds :app :compiler :output-to]]

  :source-paths ["src/clj" "src/cljc" "clj-pg/src" "clj-pg/test"]
  :resource-paths ["resources" "target/cljsbuild"]

  :cljsbuild {:builds {:min
                       {:source-paths ["src/cljs" "src/cljc" "env/prod/cljs"]
                        :compiler {:output-to "target/cljsbuild/public/js/app.js"
                                   :output-dir "target/uberjar"
                                   :optimizations :advanced
                                   :pretty-print  false}}
                       :app {:source-paths ["src/cljs" "src/cljc" "env/dev/cljs"]
                             :compiler
                             {:main "pgw.dev"
                              :asset-path "/js/out"
                              :output-to "target/cljsbuild/public/js/app.js"
                              :output-dir "target/cljsbuild/public/js/out"
                              :source-map true
                              :optimizations :none
                              :pretty-print  true}}
                       :test {:source-paths ["src/cljs" "src/cljc" "test/cljs"]
                              :compiler {:main pgw.doo-runner
                                         :asset-path "/js/out"
                                         :output-to "target/test.js"
                                         :output-dir "target/cljstest/public/js/out"
                                         :optimizations :whitespace
                                         :pretty-print true}}}}

  :figwheel {:http-server-root "public"
             :server-port 3449
             :nrepl-port 7002
             :nrepl-middleware ["cemerick.piggieback/wrap-cljs-repl"]
             :css-dirs ["resources/public/css"]
             :ring-handler pgw.core/app}

  :profiles {:dev {:dependencies [[ring/ring-mock "0.3.0"]
                                  [ring/ring-devel "1.5.0"]
                                  [prone "1.1.1"]
                                  [figwheel-sidecar "0.5.4-3"]
                                  [org.clojure/tools.nrepl "0.2.12"]
                                  [com.cemerick/piggieback "0.2.2-SNAPSHOT"]
                                  [lein-doo "0.1.6"]]

                   :source-paths ["env/dev/clj"]
                   :plugins [[lein-figwheel "0.5.4-3"]
                             [lein-doo "0.1.6"]
                             [org.clojure/tools.namespace "0.3.0-alpha2"]]

                   :env {:dev true}}

             :uberjar {:source-paths ["env/prod/clj"]
                       :prep-tasks   ["compile" ["cljsbuild" "once" "min"]]
                       :env {:production true}
                       :aot :all
                       :omit-source true}})
