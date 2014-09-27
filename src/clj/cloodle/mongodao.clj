(ns cloodle.mongodao
  (:require [monger.collection :as mc]
            [monger.conversion :refer [from-db-object]])
  (:use [monger.core :only [connect get-db disconnect authenticate]])
  (:require [crypto.random :as crypto]))

(def key-size 16)
(def database (atom nil))
(def collection "cloodle")

(def test-uri
;  (prn "IN TEST MODE!!")
  "mongodb://testcloodle:MikNak4498abe88@ds063779.mongolab.com:63779/test-cloodle")
(def prod-uri
;  (prn "IN PRODUCTION MODE!!")
  "mongodb://prodcloodle:rekVakNeg1132bok12@ds063879.mongolab.com:63879/cloodle")

(defn get-db-uri []
  (let [mode (java.lang.System/getProperty "MODE")]
        (if (or (= mode "prod") (= mode "PROD"))
          prod-uri
          test-uri)))

(defn get-event-hash[]
  "Create a uniq key to be used as an identifier for the event."
  (crypto/url-part key-size))

(defn create-event[params]
  "Create a new event and return generated eventhash"
  (let [eventhash (get-event-hash) ]
  (mc/insert @database collection (merge {:eventhash eventhash} params))
  eventhash))

(defn strip-mongo-id[event]
  "Strip mongo object ids from the stuff that comes out of mongo db. It does not serialize to json"
  (dissoc event :_id))

(defn get-by-eventhash[ehash]
  "Get event from database by the eventhash"
  (let [event (mc/find-one-as-map @database collection {:eventhash ehash})]
;    (prn "found event " event)
    (strip-mongo-id event)))

(defn init[]
  "Initialize mongodb connection and database!"
  (prn "MongoDb init running")
  (let [uri (get-db-uri)
          {:keys [conn db]} (monger.core/connect-via-uri uri)]
    (reset! database db)))

