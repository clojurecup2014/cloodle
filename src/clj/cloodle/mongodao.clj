(ns cloodle.mongodao
  (:require [monger.collection :as mc]
            [monger.conversion :refer [from-db-object]])
  (:use [monger.core :only [connect get-db disconnect authenticate]])
  (:require [cloodle.validations :as v])
  (:require [crypto.random :as crypto])
  (:require [ring.util.response :as ring]))

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
  (let [eventhash (get-event-hash)
        errors (v/validate-event params)]
  (if (empty? errors)
    (let []
      (mc/insert @database collection (merge {:eventhash eventhash} params))
      (ring/response eventhash))
  {:status 500 :body errors})))

(defn strip-mongo-id[event]
  "Strip mongo object ids from the stuff that comes out of mongo db. It does not serialize to json"
  (dissoc event :_id))

(defn find-one-by-ehash[ehash]
  (mc/find-one-as-map @database collection {:eventhash ehash}))

(defn get-by-eventhash[ehash]
  "Get event from database by the eventhash"
  (let [event (find-one-by-ehash ehash)]
    (strip-mongo-id event)))

(defn update-event[event]
   (prn "updating event and validating" event)
   (let [errors (v/validate-event-update event)]
   (if (empty? errors)
     (let [event-from-db (find-one-by-ehash (:eventhash event))
           doc-id (:_id event-from-db)
           new-event (assoc event :_id doc-id)]
         ;; finds and updates a document by _id because it is present
         (strip-mongo-id (mc/save-and-return @database collection new-event)))
   {:status 500 :body errors })))


(defn init[]
  "Initialize mongodb connection and database!"
  (prn "MongoDb init running")
  (let [uri (get-db-uri)
          {:keys [conn db]} (monger.core/connect-via-uri uri)]
    (reset! database db)))

