# ragtime.datomic [![CircleCI](https://circleci.com/gh/hden/ragtime.datomic/tree/master.svg?style=svg)](https://circleci.com/gh/hden/ragtime.datomic/tree/master) [![codecov](https://codecov.io/gh/hden/ragtime.datomic/branch/master/graph/badge.svg?token=H3B9JL6DQX)](https://codecov.io/gh/hden/ragtime.datomic)

Manage datomic schema with [ragtime](https://github.com/weavejester/ragtime).

## Warnings

In Datomic, changing an existing schema attribute is similar to accumulate a new schema attribute. Since its not possible to rollback and redo a transaction, it is recommended to set the migration strategy to `ragtime.strategy/raise-error`.

https://docs.datomic.com/cloud/schema/schema-change.html


## Usage

First, add the following dependency to your project:

`[ragtime.datomic "0.1.0-SNAPSHOT"]`

Once you have at least one migration, you can set up Ragtime. You'll need to build a configuration map that will tell Ragtime how to connect to your database, and where the migrations are. In the example below, we'll put the configuration in the user namespace:

```clojure
(ns user
  (:require [datomic.client.api :as datomic]
            [ragtime.datomic :as rd]
            [ragtime.repl :as repl]))

(def client (datomic/client {:server-type :peer-server
                             :access-key "myaccesskey"
                             :secret "mysecret"
                             :endpoint "localhost:8998"}))

(def conn (datomic/connect client {:db-name "hello"}))

(def migration (rd/create-migration :id [:db/ident :inv/sku
                                         :db/valueType :db.type/string
                                         :db/unique :db.unique/identity
                                         :db/cardinality :db.cardinality/one]))

(def config
  {:datastore  (rd/create-connection conn)
   :migrations [migration])

(repl/migrate config)
```


This library will install an extra schema inn your database.

```clojure
{:db/ident       :ragtime.datomic/migration-id
 :db/valueType   :db.type/keyword
 :db/unique      :db.unique/identity
 :db/cardinality :db.cardinality/one}
```

The following datum will be appended to each of the schema transactions.

```
[:db/add "datomic.tx" :ragtime.datomic/migration-id migration-id]
```

## License

Copyright © 2019 Haokang Den

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.
