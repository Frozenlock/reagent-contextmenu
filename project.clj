(defproject org.clojars.frozenlock/reagent-contextmenu "0.3.1"
  :description "Context menu for Reagent!"
  :url "https://github.com/Frozenlock/reagent-contextmenu"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :clojurescript? true
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [reagent "0.6.0-rc"]]
  :profiles {:dev
             {:dependencies [[org.clojure/clojurescript "0.0-2371" :scope "provided"]]
              :plugins [[lein-cljsbuild "1.0.3"]]}})
