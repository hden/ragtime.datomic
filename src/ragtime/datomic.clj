(ns ragtime.datomic
  (:require [ragtime.protocols :as ragtime-protocol]
            [datomic.client.api :as datomic]))

(defn- create-schema [index-key]
  [{:db/ident       index-key
    :db/valueType   :db.type/keyword
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}])

(defn- has-ident? [db ident]
  (contains? (datomic/pull db {:eid ident :selector [:db/ident]})
             :db/ident))

(defn- schema-loaded? [db index-key]
  (has-ident? db index-key))

(defn- ensure-ragtime-schema [conn index-key]
  (let [db (datomic/db conn)
        schema (create-schema index-key)]
    (when-not (schema-loaded? db index-key)
      (datomic/transact conn {:tx-data schema}))))

(defn- find-migrations [db index-key]
  (let [tuples (datomic/q '[:find ?id
                            :in $ ?a
                            :where [_ ?a ?id]]
                          db index-key)]
    (map first tuples)))

(defrecord Connection [conn index-key]
  ragtime-protocol/DataStore
  (add-migration-id [_ id])
  (remove-migration-id [_ id])
  (applied-migration-ids [_]
    (ensure-ragtime-schema conn index-key)
    (let [db (datomic/db conn)]
      (find-migrations db index-key))))

(defn create-connection
  ([conn] (create-connection conn ::migration-id))
  ([conn index-key]
   (->Connection conn index-key)))

(defrecord Migration [id txs index-key]
  ragtime-protocol/Migration
  (id [_] id)
  (run-up! [_ {:keys [conn]}]
    (ensure-ragtime-schema conn index-key)
    (let [tx-data (into [[:db/add "datomic.tx" index-key id]]
                        txs)]
      (datomic/transact conn {:tx-data tx-data})))
  (run-down! [_ conn]))

(defn create-migration
  ([id txs] (create-migration id txs ::migration-id))
  ([id txs index-key]
   {:pre [(and (keyword? id)
               (sequential? txs)
               (> (count txs) 0))]}
   (->Migration id txs index-key)))
