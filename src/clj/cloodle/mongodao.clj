(ns cloodle.mongodao
  (:require [monger.collection :as mc])
  (:use [monger.core :only [connect get-db disconnect authenticate]])
  (:use [crypto.random :as crypto])
  )

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
  (prn params)
  (let [eventhash (get-event-hash) ]
  (mc/insert @database collection (merge {:eventhash eventhash} params))
  eventhash))

(defn init[]
  "Initialize mongodb connection and database!" 
  (prn "MongoDb init running")
  (let [uri (get-db-uri)
          {:keys [conn db]} (monger.core/connect-via-uri uri)]
    (reset! database db)))
