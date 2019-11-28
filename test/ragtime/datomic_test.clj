(ns ragtime.datomic-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer [pprint]]
            [ragtime.datomic :refer :all]
            [shrubbery.core :as shrubbery]
            [datomic.client.api :as datomic]
            [datomic.client.api.protocols :as client-protocols]
            [datomic.client.api.impl :as client-impl]
            [ragtime.core :as ragtime]
            [ragtime.protocols :as ragtime-protocols]))

(defn create-mocks []
  (let [db (shrubbery/mock client-protocols/Db client-impl/Queryable)
        conn (shrubbery/spy (reify client-protocols/Connection
                              (db [_] db)
                              (transact [_ arg-map])))
        store (create-connection conn)]
    {:db db
     :conn conn
     :store store}))

(deftest test-applied-migration-ids
  (let [{:keys [db conn store]} (create-mocks)]
    (ragtime-protocols/applied-migration-ids store)
    (is (shrubbery/received? db client-impl/q))))

(deftest test-run-up
  (let [{:keys [db conn store]} (create-mocks)
        schema [{:db/ident :inv/sku
                 :db/valueType :db.type/string
                 :db/unique :db.unique/identity
                 :db/cardinality :db.cardinality/one}]
        migration (create-migration :id schema)
        _ (ragtime-protocols/run-up! migration {:conn conn})]
    (is (shrubbery/received? conn client-protocols/transact))
    (let [tx-data (-> (shrubbery/calls conn)
                      (get client-protocols/transact)
                      first
                      first
                      :tx-data)]
      (is (= (first tx-data)
             [:db/add "datomic.tx" :ragtime.datomic/migration-id :id])))))

(deftest test-migrate-all
  (let [{:keys [db conn store]} (create-mocks)
        migration (create-migration :id [{:db/ident :inv/sku
                                          :db/valueType :db.type/string
                                          :db/unique :db.unique/identity
                                          :db/cardinality :db.cardinality/one}])
        index (ragtime/into-index [migration])]
    (ragtime/migrate-all store index [migration])
    (is (shrubbery/received? db client-impl/q))
    (is (shrubbery/received? conn client-protocols/transact))))
