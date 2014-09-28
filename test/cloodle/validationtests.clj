(ns cloodle.validationtests
  (:use clojure.test
        midje.sweet)
  (:require [cloodle.validations :as v]))
  
(def event  {:name "Movie night", 
             :description "Lets drink beers and watch some Arnold movie!", 
             :options [{:id 1, :name "Terminator 2. The Judgment Day"}
                       {:id 2, :name "The Running Man"}
                       {:id 3, :name "Commando"}], 
             :participants [{:id 1, :name "Jarkko",
                             :selections [{:optionId 1, :value 45} 
                                          {:optionId 2, :value 11} 
                                          {:optionId 3, :value 85}]} 
                            {:id 2, :name "Janne", 
                             :selections [{:optionId 1, :value 60} 
                                          {:optionId 2, :value 33} 
                                          {:optionId 3, :value 100}]}]})

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

(fact "When updating the event there must be participants included!" 
      (v/validate-participants event) => {}
      (v/validate-participants (dissoc event :participants)) => {:participants #{"Must include participants"}})

(def participant {:participants [{:id 1, :name "Jarkko",
                             :selections [{:optionId 1, :value 45} 
                                          {:optionId 2, :value 11} 
                                          {:optionId 3, :value 85}]} 
                            {:id 2, :name "Janne", 
                             :selections [{:optionId 1, :value 60} 
                                          {:optionId 2, :value 33} 
                                          {:optionId 3, :value 100}]}]})
(def malformed-participants {:participants [{:id 1, 
                                        :selections [{:optionId 1, :value 45} 
                                                     {:optionId 2, :value 11} 
                                                     {:optionId 3, :value 85}]} 
                                       { :name "Janne"}]})

(fact "Particpant must have id and name and selections" 
      (v/validate-participant-data participant) => '({}{})
      (v/validate-participant-data malformed-participants) => '({[1 :name] #{"Participant must have name"}} {[nil :id] #{"Participant must have id" "can't be blank"}, [nil :selections] #{"Participant must have the selections"}}))

(def selections  {:selections [{:optionId 1, :value 60} 
                                             {:optionId 2, :value 0}
                                             {:optionId 3, :value 100}]})

(def malformed-selections  {:selections [{} {:optionId "aa", :value -1}
                                            {:optionId 3, :value "moi"}
                                            {:optionId 4, :value 101}]})

(fact "selections must have optionId and value, which must be integers" 
      (v/validate-selection-data selections)  => '({}{}{})
      (v/validate-selection-data malformed-selections)  => ' ({[nil :optionId] #{"Participant must have id" "can't be blank"}, 
                                                               [nil :value] #{"Participant must have name" "can't be blank"}} 
                                                               {["aa" :optionId] #{"should be a number" "should be an integer"}, 
                                                                ["aa" :value] #{"should be greater than or equal to 0"}} 
                                                               {[3 :value] #{"should be a number" "should be an integer"}}
                                                               {[4 :value] #{"should be less than or equal to 100"}}))

(fact "Participants cannot be more than 20 people"  
      (v/validate-participants-length  {:participants []}) => {}
      (v/validate-participants-length  {}) => {}
      (v/validate-participants-length  {:participants (vec (repeat 5 {:name "Jarski"} ) )}) => {}
      (v/validate-participants-length  {:participants (vec (repeat 20 {:name "Jarski"} ) )}) => {}
      (v/validate-participants-length  {:participants (vec (repeat 21 {:name "Jarski"} ) )})=> {:participantslength #{"1. too many participants"}})

(def event-with-too-many-selections  {:name "Movie night", 
                                      :description "Lets drink beers and watch some Arnold movie!", 
                                      :options [{:id 1, :name "Terminator 2. The Judgment Day"}
                                                {:id 2, :name "The Running Man"}
                                                {:id 3, :name "Commando"}], 
                                      :participants [{:id 1, :name "Jarkko",
                                                      :selections [{:optionId 1, :value 45} 
                                                                   {:optionId 2, :value 11} 
                                                                   {:optionId 3, :value 85}]} 
                                                     {:id 2, :name "Janne", 
                                                      :selections (vec (repeat 11 {:optionId 2, :value "joo"}))} ;;here is the beef
                                                     {:id 1, :name "Jarkko",
                                                      :selections (vec (repeat 9 {:optionId 2, :value "joo"}))} 
                                                     {:id 2, :name "Janne", 
                                                      :selections [{:optionId 1, :value 60} 
                                                                   {:optionId 2, :value 33} 
                                                                   {:optionId 3, :value 100}
                                                                   {:optionId 3, :value 100}
                                                                   {:optionId 3, :value 100}]}]})

(fact "You cant have more than 10 selections per participant"
       (v/selections-valid-length event) => {}
       (v/selections-valid-length event-with-too-many-selections) =>{:selectionslength "too many selections "})
