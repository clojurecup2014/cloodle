(ns cloodle.mongodao
  (:require [monger.collection :as mc])
  (:use [monger.core :only [connect get-db disconnect authenticate]]))

(defn test-uri []
  (prn "IN TEST MODE!!")
  "mongodb://testcloodle:MikNak4498abe88@ds063779.mongolab.com:63779/test-cloodle")
(defn prod-uri []
  (prn "IN PRODUCTION MODE!!")
  "mongodb://prodcloodle:rekVakNeg1132bok12@ds063879.mongolab.com:63879/cloodle")

(defn get-db-uri [] 
  (let [mode (java.lang.System/getProperty "MODE")]
        (if (or (= mode "prod") (= mode "PROD")) 
          (prod-uri)
          (test-uri))))                                      

(defn test-stuff[]
  (let [uri (get-db-uri)
          {:keys [conn db]} (monger.core/connect-via-uri uri)]
        (mc/insert db "cloodle-events" {:first_name "Jo2hn"  :last_name "Len2non"})
        (mc/insert db "cloodle-events" {:first_name "Rin2go" :last_name "Sta2rr"})))
