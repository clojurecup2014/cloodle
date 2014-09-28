(ns cloodle.validationtests
  (:use clojure.test
        midje.sweet)
  (:require [cloodle.validations :as v]))
  
(def event  {:name "Movie night", :description "Lets drink beers and watch some Arnold movie!", :options [{:id 1, :name "Terminator 2. The Judgment Day"} {:id 2, :name "The Running Man"} {:id 3, :name "Commando"}], :participants [{:id 1, :name "Jarkko", :selections [{:optionId 1, :value 45} {:optionId 2, :value 11} {:optionId 3, :value 85}]} {:id 2, :name "Janne", :selections [{:optionId 1, :value 60} {:optionId 2, :value 33} {:optionId 3, :value 100}]}]})

(facts "Event must have name, description and options"
        (v/validate-event event) => {}
        (v/validate-event (dissoc event :name )) => {:name #{"Must have name"}}
        (v/validate-event (dissoc event :name :description)) => {:description #{"Must have description"} :name #{"Must have name"}}
        (v/validate-event (dissoc event :options)) => {:options #{"Options must be defined"}})

(facts "Option, movie selection option eg. must have id and name"
        (v/validate-options {:options [{:id 1, :name "Terminator 2"} {:id 2, :name "The Running Man"}]}) => '({} {})   
        (v/validate-options {:options [{:name "Terminator 2"} {:id 2, :name "The Running Man"}]}) => '({[nil :id] #{"can't be blank" "option must have id"}} {})
        (v/validate-options {:options [{:name "Terminator 2"} {:id 2}]}) => '({[nil :id] #{"can't be blank" "option must have id"}} {[2 :name] #{"option must have name"}})
        (v/validate-options {:options [{:id "ab", :name "Terminator 2"} {:id 2, :name "    "}]}) => '({["ab" :id] #{"should be a number" "should be an integer"}} {[2 :name] #{"option must have name"}}))


(fact "There must be atleast one option but not more than 10" 
   (v/validate-options-length event) => {}
   (v/validate-options-length {:options [{:id 1, :name "Terminator 2"} {:id 2, :name "The Running Man"} {:id 2, :name "The Running Man"}  {:id 2, :name "The Running Man"}   {:id 2, :name "The Running Man"}  {:id 2, :name "The Running Man"}  {:id 2, :name "The Running Man"}  {:id 2, :name "The Running Man"}  {:id 2, :name "The Running Man"}  {:id 2, :name "The Running Man"} {:id 2, :name "The Running Man"} ]}) => {:optionslength #{"4. Too many options"}}
   (v/validate-options-length {:options [{:id 1, :name "Terminator 2"} {:id 2, :name "The Running Man"} {:id 2, :name "The Running Man"}  {:id 2, :name "The Running Man"}   {:id 2, :name "The Running Man"}  {:id 2, :name "The Running Man"}  {:id 2, :name "The Running Man"}  {:id 2, :name "The Running Man"}  {:id 2, :name "The Running Man"}  {:id 2, :name "The Running Man"}]}) => {})
