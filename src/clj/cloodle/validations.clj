(ns cloodle.validations
    (:require [validateur.validation :refer :all]))


(defn validate-event-basic-info[event] 
  (let [v (validation-set
     (presence-of :name :message "Must have name") 
     (presence-of :description :message "Must have description") 
     (presence-of :options :message "Options must be defined"))]
     (v event))) 

(def min-options 1)
(def max-options 10)

(defn validate-options-length[event]
  "validate the options in the event"
  (let [options (seq (:options event))]
    (cond 
      (nil? options) {:optionslength #{"1. Options cannot be empty"}}
      (empty? options)  {:optionslength #{"2. Options cannot be empty"}}
      (< (count options) min-options) {:optionslength #{"3. Must have atleast one option"}}
      (> (count options) max-options){:optionslength #{"4. Too many options"}}
      :else  {})))


(def option-validator
  (validation-set 
    (presence-of :id :message "option must have id")
    (numericality-of :id :only-integer true )
    (presence-of :name :message "option must have name")))

(defn validate-options [event]
  (for [option (seq(:options event))]
    (nest (:id option)
           (option-validator option))))

(defn validate-event[event] 
   (let [basic (validate-event-basic-info event)
         options-l (validate-options-length event)
         options-data (validate-options event)]
     (prn "basic " basic) 
     (prn "options-l " options-l)
     (prn "options-data " options-data)
     (cond 
       (not-empty basic) basic
       (not-empty options-l) options-l
       (not-empty (filter #(not-empty %)  options-data)) options-data ;'({}{}{}) check if it is empty
       :else {})))



