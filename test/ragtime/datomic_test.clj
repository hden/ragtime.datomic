(ns ragtime.datomic-test
  (:require [clojure.test :refer :all]
            [ragtime.datomic :refer :all]
            [datomic.client.api :as datomic]
            [ragtime.core :as ragtime]))

(def client (datomic/client {:server-type :peer-server
                             :access-key "myaccesskey"
                             :secret "mysecret"
                             :endpoint "localhost:8998"}))

(def conn (datomic/connect client {:db-name "hello"}))

(deftest index-key
  (are [x] (= :ragtime.datomic/migration-id (:index-key x))
    (create-connection conn)
    (create-migration :id [{:db/ident :inv/sku
                            :db/valueType :db.type/string
                            :db/unique :db.unique/identity
                            :db/cardinality :db.cardinality/one}])))

(deftest e2e
  (let [connection (create-connection conn)
        migration (create-migration :id [{:db/ident :inv/sku
                                          :db/valueType :db.type/string
                                          :db/unique :db.unique/identity
                                          :db/cardinality :db.cardinality/one}])
        index (ragtime/into-index [migration])]
    (testing "applied-migrations"
      (ragtime/migrate-all connection index [migration])
      (is (= (seq [migration])
             (ragtime/applied-migrations connection index))))))
