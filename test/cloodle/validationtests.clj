(ns cloodle.validationtests
  (:use clojure.test
        midje.sweet)
  (:require [cloodle.mongodao :as dao]))
  
(def event-ok  {:name "Movie night", :description "Lets drink beers and watch some Arnold movie!", :options [{:id 1, :name "Terminator 2. The Judgment Day"} {:id 2, :name "The Running Man"} {:id 3, :name "Commando"}], :participants [{:id 1, :name "Jarkko", :selections [{:optionId 1, :value 45} {:optionId 2, :value 11} {:optionId 3, :value 85}]} {:id 2, :name "Janne", :selections [{:optionId 1, :value 60} {:optionId 2, :value 33} {:optionId 3, :value 100}]}]})
(facts "Event must have name, description and options"
        (dao/validate-event event-ok) => {})
       



