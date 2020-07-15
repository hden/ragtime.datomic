(defproject ragtime.datomic "0.1.0-SNAPSHOT"
  :description "Ragtime migrations for Datomic"
  :url "https://github.com/hden/ragtime.datomic"
  :license {:name "EPL-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [ragtime/core "0.8.0"]]
  :repl-options {:init-ns ragtime.datomic}
  :profiles
  {:dev {:dependencies [[com.datomic/client-cloud "0.8.100"]
                        [com.gearswithingears/shrubbery "0.4.1"]]}})
