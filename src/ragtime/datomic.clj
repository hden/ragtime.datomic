(ns ragtime.datomic
  (:require [cognitect.anomalies :as anomalies]
            [datomic.client.api.protocols :as client-protocols]
            [datomic.client.api :as datomic]
            [diehard.core :as diehard]
            [ragtime.protocols :as ragtime-protocol]))

(def retryable-anomaly?
  "Set of retryable anomalies."
  #{::anomalies/busy
    ::anomalies/unavailable
    ::anomalies/interrupted})

(defn- retry? [_ e]
  (-> e ex-data ::anomalies/category retryable-anomaly?))

(def default-retry-policy
  {:backoff-ms    [10 1000]
   :jitter-factor 0.25
   :max-retries   5
   :retry-if      retry?})

(defn- create-schema [index-key]
  [{:db/ident       index-key
    :db/valueType   :db.type/keyword
    :db/unique      :db.unique/value
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
  (let [tuples (datomic/q '[:find ?id ?t
                            :in $ ?a
                            :where [_ ?a ?id ?t]]
                          db index-key)]
    (into []
          (map first)
          (sort-by last tuples))))

(defrecord Connection [conn index-key retry-policy]
  ragtime-protocol/DataStore
  (add-migration-id [_ _]
    (diehard/with-retry retry-policy
      (ensure-ragtime-schema conn index-key)))
  (remove-migration-id [_ _]
    (diehard/with-retry retry-policy
      (ensure-ragtime-schema conn index-key)))
  (applied-migration-ids [_]
    (diehard/with-retry retry-policy
      (ensure-ragtime-schema conn index-key)
      (let [db (datomic/db conn)]
        (find-migrations db index-key)))))

(defn create-connection
  ([conn] (create-connection conn ::migration-id))
  ([conn index-key] (create-connection conn index-key default-retry-policy))
  ([conn index-key retry-policy]
   {:pre [(and (satisfies? client-protocols/Connection conn) (keyword? index-key))]}
   (->Connection conn index-key retry-policy)))

(defrecord Migration [id txs index-key retry-policy]
  ragtime-protocol/Migration
  (id [_] id)
  (run-up! [_ {:keys [conn]}]
    (let [tx-data (conj txs [:db/add "datomic.tx" index-key id])]
      (diehard/with-retry retry-policy
        (datomic/transact conn {:tx-data tx-data}))))
  (run-down! [_ _]))

(defn create-migration
  ([id txs] (create-migration id txs ::migration-id))
  ([id txs index-key] (create-migration id txs index-key default-retry-policy))
  ([id txs index-key retry-policy]
   {:pre [(and (keyword? id)
               (vector? txs)
               (> (count txs) 0)
               (map? retry-policy))]}
   (->Migration id txs index-key retry-policy)))
