(ns cloodle.mongodao
  (:require [cloodle.util :as util])
  (:require [monger.collection :as mc]
            [monger.conversion :refer [from-db-object]]
            [monger.operators :refer :all])
  (:use [monger.core :only [connect get-db disconnect authenticate]])
  (:require monger.json)
  (:require [cloodle.validations :as v])
  (:require [crypto.random :as crypto])
  (:require [ring.util.response :as ring])
  (:import org.bson.types.ObjectId))

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

(defn get-event-hash []
  "Create a uniq key to be used as an identifier for the event."
  (crypto/url-part key-size))


(defn generate-identifiers [event-data]

  (let [with-cloodle-code (merge {:cloodle-code (get-event-hash)} event-data)
        with-event-identifiers (assoc-in
                                with-cloodle-code [:options]
                                (map #(assoc % :optionId (util/uuid)) (:options with-cloodle-code)))]

    with-event-identifiers))

(defn with-identifier [participant]
  (assoc participant :id (util/uuid)))


(defn create-event [event-data]
  "Create a new event and return generated eventhash"
  (let [eventhash (get-event-hash)
        errors (v/validate-event event-data)]
  (if (empty? errors)
    (let [event-with-identifiers (generate-identifiers event-data)]

      (mc/insert @database collection event-with-identifiers)
      (ring/response event-with-identifiers))
  {:status 500 :body errors})))

(defn add-participant [{:keys [event-id participant]}]
  "Add the given participant and selections to an existing event identified by event-id"

  (let [validation-errors (v/validate-new-participant participant)]

    (println event-id)
    (println participant)

    (if (empty? validation-errors)
      (let [participant-with-identifier (with-identifier participant)]
        (mc/update @database collection {:_id (ObjectId. event-id)} {$push {:participants participant-with-identifier}})
        (ring/response participant-with-identifier))
      {:status 500 :body validation-errors})))



(defn find-one-by-ehash[ehash]
  (mc/find-one-as-map @database collection {:cloodle-code ehash}))

(defn get-by-eventhash[ehash]
  "Get event from database by the eventhash"
  (let [event (find-one-by-ehash ehash)]
    event))

(defn update-event[event]
   (prn "updating event and validating" event)
   (let [errors (v/validate-event-update event)
         exists (find-one-by-ehash (:cloodle-code event))]
         (cond (not-empty errors) {:status 500 :body errors }
               (nil? exists) {:status 500 :body "event not found in database"}
               :else  (let [doc-id (:_id exists)
                            new-event (assoc event :_id doc-id)]
                          ;; finds and updates a document by _id because it is present
                          (mc/save-and-return @database collection new-event)))))

(defn init[]
  "Initialize mongodb connection and database!"
  (prn "MongoDb init running")
  (let [uri (get-db-uri)
          {:keys [conn db]} (monger.core/connect-via-uri uri)]
    (reset! database db)))
