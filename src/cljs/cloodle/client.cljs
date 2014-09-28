(ns cloodle.client
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [cljs.core.async :refer [put! chan <!]]
            [clojure.string :as string]
            [cljs-http.client :as http]
            )
  (:use [jayq.core :only [$ css html]]))





(enable-console-print!)

(defn get-current-url []
  (.-href (.-location js/window)))

(defn get-cloodle-code []

  (let [url (get-current-url)
        event-id-matching (re-find #"event=(.+)" url)]

    (if-let [cloodle-code
             (when (coll? event-id-matching)
               (second event-id-matching))]

      cloodle-code)))

(defn build-initial-state []

  (let [cloodle-code (get-cloodle-code)]

    (if cloodle-code
      (atom
       {:name "Existing movie night"
        :description "Good times will be had with one of these movies"
        :cloodle-code cloodle-code
        :options [
                  {:id 1 :name "Terminator 2"}
                  {:id 2 :name "Commando"}
                  {:id 3 :name "Conan The Barbarian"}
                  {:id 4 :name "Junior"}]

        :participants [

                       {:id 1
                        :name "Jarkko"
                        :selections [

                                     {:optionId 1
                                      :value 45}

                                     {:optionId 2
                                      :value 11}

                                     {:optionId 3
                                      :value 90}

                                     {:optionId 4
                                      :value 70}


                                     ]

                        }


                       ]

        :saved true})

      (atom
       {
        :name "Movie Night"
        :description "Let's drink beer and watch an Arnie movie"
        :options [
                  {:id 1 :name "Terminator 2"}
                  {:id 2 :name "Commando"}
                  {:id 3 :name "Conan The Barbarian"}
                  {:id 4 :name "Junior"}]
        :cloodle-code ""
        :saved false})
      )))

(def app-state (build-initial-state))

(defn display [show]
  (if show
    #js {}
    #js {:display "none"}))


(defcomponent title-and-description [form-data owner]

  (render-state [this {:keys [visible]}]

                (if visible
                  (dom/div nil
                           (dom/h1 nil (:name form-data))
                           (dom/p nil (:description form-data))))))

(defcomponent option-container [option-data owner]
  (render-state [this {:keys [delete-chan]}]

          (dom/div #js {:style #js {:margin-bottom "5px"}}
                   (dom/div #js {:className "pull-left"} (:name option-data))
                   (dom/div #js {:className "pull-right"}

                            (dom/button #js {:type "button" :className "btn btn-danger btn-xs"
                                             :onClick (fn [e] (do (put! delete-chan @option-data)
                                                                            false))}

                                        (dom/span #js {:className "glyphicon glyphicon-trash"} nil)))

                   (dom/br #js {:className "clearfix"} nil))


  ))

(defn max-id [maps-with-ids]
  (:id (last (sort-by :id maps-with-ids))))

(defn add-option [form-data owner]
  (let [new-option (-> (om/get-node owner "new-option")
                       .-value)]
    (when (not (string/blank? new-option))
      (let [next-id (inc (max-id (:options @form-data)))]
        (om/transact! form-data :options #(conj % {:name new-option :id next-id}))))
        (om/set-state! owner :new-option-text "")))


(defn handle-new-option-change [e owner {:keys [new-option-text]}]
  (om/set-state! owner :new-option-text (.. e -target -value)))

(defn handle-value-change [e owner form-data ref-name state-key]
  (let [new-value (-> (om/get-node owner ref-name)
                     .-value)]
    (om/transact! form-data state-key (fn [_] new-value))))


(defn to-link [code]
    (dom/a #js {:href (str "event/" code)} code))


;; CLOODLE CODE COMPONENT
(defcomponent cloodle-code [code owner]


  (render-state [this state]

          (if (:saved state)
            (dom/div #js {:className "center-block bg-info cloodle-code-box"}
                     (dom/h3 nil

                             (dom/span nil "This event's ")
                             (dom/span #js {:className "cloodle-name"} "Cloodle")
                             (dom/span nil " link is"))

                     (dom/div #js {:className "alert alert-success center-block" :role "alert"}

                              (dom/h2 #js {:className "center-block"} (to-link code)))

                     (dom/h3 nil "Share it with your friends and have them vote on the options!")))))








(defcomponent new-option-component [form-data owner]

  (render-state [this state]

          (dom/div #js {:className "form-group"}
                   (dom/div #js {:className "col-sm-2"} nil)
                   (dom/div #js {:className "col-sm-2"}

                            (dom/input #js {:id "new-option-name"
                                            :className "form-control"
                                            :type "text"
                                            :ref "new-option"
                                            :value (:new-option-text state)
                                            :onChange #(handle-new-option-change % owner state)}))

                   (dom/div #js {:className "col-sm-6"}
                            (dom/button #js {:type "button"
                                             :className "btn btn-success"
                                             :onClick #(add-option form-data owner)}
                                        "Add option")))))


(defn state->save-payload [state]
  "Transform the snapshot of the state to the form that the save interface accepts"

  (select-keys state (disj (set (keys state)) :cloodle-code)))





;; MAIN FORM COMPONENT
(defcomponent cloodle-form [form-data owner]

  (init-state [_]
              {:delete-option-chan (chan)
               :save-event-chan (chan)
               :save-result-chan (chan)
               :new-option-text ""})

  (will-mount [_]
              (let [delete (om/get-state owner :delete-option-chan)
                    save (om/get-state owner :save-event-chan)]


                ;; DELETE OPTION
                (go (loop []
                      (let [option (<! delete)]


                        (om/transact! form-data :options
                                      (fn [xs] (vec (remove #(= option %) xs))))

                        (recur))))

                ;; SAVE EVENT
                (go (loop []
                      (let [save-event (<! save)
                            payload (state->save-payload save-event)]



                        (go
                         (let [response (<! (http/post "api/event" {:json-params payload}))
                               status (:status response)]

                            (if-let [cloodle-code (if (= status 200) (:body response))]

                              (do
                                (om/transact! form-data :cloodle-code (fn [_] cloodle-code))
                                (om/transact! form-data :saved (fn [_] true)))


                              ;; TODO: Display errors? Or rather prevent sending this stuff via UI
                              (print (str "Save failed: " (:body response))))





                           ))

                        (recur))))))

  (render-state [this state]

          (dom/div #js {:className "form-group"}
                   (dom/form #js {:className "form-horizontal" :role "form"
                                  :style (display (not (:saved state)))}

                             ;; TODO: Label / Input pairs as components?
                             ;; NAME
                             (dom/div #js {:className "form-group"}

                                      ;; TODO: Label / Input pairs as components?
                                      (dom/label #js {:htmlFor "name"
                                                      :className "col-sm-2 control-label"}
                                                 "Event name")
                                      (dom/div #js {:className "col-sm-6"}
                                               (dom/input #js {:id "name"
                                                               :className "form-control"
                                                               :type "text"
                                                               :value (:name form-data)
                                                               :ref "event-name"
                                                               :onChange  #(handle-value-change % owner form-data "event-name" :name)})))

                             ;; TODO: Label / Input pairs as components?
                             ;; DESCRIPTION
                             (dom/div #js {:className "form-group"}
                                      (dom/label #js {:htmlFor "description"
                                                      :className "col-sm-2 control-label"}
                                                 "Event description")

                                      (dom/div #js {:className "col-sm-6"}
                                               (dom/input #js {:id "description"
                                                               :className "form-control"
                                                               :type "text"
                                                               :value (:description form-data)
                                                               :ref "event-description"
                                                               :onChange  #(handle-value-change % owner form-data "event-description" :description)})))


                             ;; EXISTING OPTIONS
                             (dom/div #js {:className "form-group"}

                                      (dom/label #js {:className "col-sm-2 control-label"}
                                                 "Options")

                                      (apply dom/div #js {:className "col-sm-6"}
                                               (om/build-all option-container (:options form-data)
                                                             {:init-state {:delete-chan (:delete-option-chan state)}})))


                             ;; ADD NEW OPTION
                               (om/build new-option-component
                                         form-data
                                         {:state state})


                             ;; CREATE BUTTON
                             (dom/div #js {:className "form-group"}

                                      (dom/div #js {:className "col-sm-2"} nil)
                                      (dom/div #js {:className "col-sm-6"}
                                               (dom/button #js {:type "button"
                                                                :className "btn btn-success"
                                                                :onClick (fn [e] (put! (:save-event-chan state) @form-data))
                                                                }
                                                           (dom/span #js {:className "glyphicon glyphicon-ok"})
                                                           (dom/span nil " Create!")))))

                   )))



(defcomponent participant-component [participant owner]

  (render [this]
          (dom/div nil
                   (dom/h4 nil (:name participant)))))

(defcomponent participant-list [participants owner]
  (render [this]
          (dom/div nil
                   (dom/h1 nil "Participants")
                   (apply dom/div nil
                          (om/build-all participant-component participants)))))



(defcomponent slider [slider owner]

      (render [_]
              (dom/div #js {:className "slider col-sm-6"} nil))

      (did-mount [state]
                 (let [$slider-element ($ (.getDOMNode owner))
                       parameters #js {:start (:value slider)
                                       :range #js {"max" #js [100] "min" #js [0]}
                                       :step 1
                                       :format (js/wNumb #js {:mark "," :decimals 1})
                                       }]


                   (.noUiSlider $slider-element parameters)
                   (.on $slider-element #js {:slide #(handle-slider-change (.val $slider-element) slider owner)}))))



(defcomponent option-slider [option owner]
  (render [this]

          (dom/div #js {:className "row" :style #js {:margin-bottom "15px"}}
            (dom/div #js {:className "col-sm-2"} (:name option))
            (om/build slider option)
            )))



(defcomponent vote-component [options owner]
  (render [this]
          (dom/div #js {:style #js {:border "solid black 1px"}}
                   (apply dom/div nil
                          (om/build-all option-slider options)))))


(defcomponent main-page [app-state owner]

  (render [this]
          (dom/div nil

                   ;; TITLE AND DESCRIPTION (FOR EXISTING EVENT)
                   (om/build title-and-description
                             (select-keys app-state [:name :description])
                             {:state {:visible (:saved app-state)}})


                   ;; LIST PARTICIPANTS
                   (if (:saved app-state)

                     (om/build vote-component (:options app-state))

                     (om/build participant-list (:participants app-state)))


                   ;; NEW EVENT FORM
                   (if (not (:saved app-state))
                     (om/build cloodle-form
                               app-state
                               {:state {:saved (:saved app-state)}}))

                   ;; CLOODLE-CODE SHARING INFO BOX
                   (om/build cloodle-code (:cloodle-code app-state) {:state {:saved (:saved app-state)}}))))






(om/root main-page app-state
         {:target (. js/document (getElementById "my-app"))})




