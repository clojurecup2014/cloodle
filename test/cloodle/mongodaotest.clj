(ns cloodle.mongodaotest

  (:use clojure.test)
  (:require [cloodle.mongodao :as dao])
  (:require [cloodle.util :as util]))


(deftest generate-identifiers

  (let [;; Pre-seeded rnd to get expected optionIds
        rnd (java.util.Random. 42)

        event-data {:name "Test event"
                    :options [ {:name "Option 1"}
                               {:name "Option 2"}]}]

    ;; Redef uuid and get-event-hash to get expected cloodle-code and optionIds
    (with-redefs-fn {#'util/uuid (fn [] (.nextInt rnd 10))
                     #'dao/get-event-hash (fn [] "gen-cloodle-code")}

      #(is (= (dao/generate-identifiers event-data)

              {:name "Test event" :cloodle-code "gen-cloodle-code"
               :options [ {:name "Option 1" :optionId 0}
                          {:name "Option 2" :optionId 3}]})))))

