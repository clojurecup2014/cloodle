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
     (cond 
       (not-empty basic) basic
       (not-empty options-l) options-l
       (not-empty (filter #(not-empty %)  options-data)) options-data ;'({}{}{}) check if it is empty
       :else {})))

(defn validate-participants[event]
  (let [v (validation-set
     (presence-of :participants :message "Must include participants"))] 
     (v event)))

(def participant-validator
  (validation-set 
    (presence-of :id :message "Participant must have id")
    (numericality-of :id :only-integer true )
    (presence-of :name :message "Participant must have name")
    (presence-of :selections :message "Participant must have the selections")))


(defn validate-participant-data [event]
  (for [option (seq(:participants event))]
    (nest (:id option)
           (participant-validator option))))

(def selection-validator
  (validation-set 
    (presence-of :optionId :message "Participant must have id")
    (numericality-of :optionId :only-integer true )
    (presence-of :value :message "Participant must have name")
    (numericality-of :value :only-integer true :gte 0 :lte 100 )))

(defn validate-selection-data [event]
  (for [selected (seq(:selections event))]
    (nest (:optionId selected)
           (selection-validator selected))))

(def max-participants 20)

(defn validate-participants-length[event]
  "validate the participants in the event"
  (let [participants (seq (:participants event))]
    (cond 
      (> (count participants) max-participants){:participantslength #{"1. too many participants"}}
      :else  {})))

(defn get-selection-lengths[event] 
   (map #(count (% :selections)) (:participants event)))

(defn selections-valid-length[event]
  (cond (not-empty (filter #(> % max-options) (get-selection-lengths event))) {:selectionslength "too many selections "}
        :else {}))

(defn validate-event-update[event] 
  (let [errors (validate-event event) ;ok
        p-errors (validate-participant-data event) ;ok
        s-errors (flatten (map #(validate-selection-data %) (:participants event)))  ;participants contain the selection data
        slen-errors (selections-valid-length event)]
    (cond 
       (not-empty errors) errors
       (not-empty (filter #(not-empty %)  p-errors)) p-errors
       (not-empty (filter #(not-empty %)  s-errors)) s-errors;'({}{}{}) check if it is empty
       (not-empty slen-errors) slen-errors
       :else {})))


