(defproject ragtime.datomic "0.3.0"
  :description "Ragtime migrations for Datomic"
  :url "https://github.com/hden/ragtime.datomic"
  :license {:name "EPL-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.12.1"]
                 [com.cognitect/anomalies "0.1.12"]
                 [diehard "0.12.0"]
                 [ragtime/core "0.8.1"]]
  :plugins [[lein-cloverage "1.2.4"]]
  :repl-options {:init-ns ragtime.datomic}
  :profiles
  {:dev {:dependencies [[com.datomic/client-cloud "1.0.130"]
                        [com.gearswithingears/shrubbery "0.4.1"]]}})
