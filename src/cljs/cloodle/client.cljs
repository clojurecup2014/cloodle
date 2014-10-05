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
;;;;;;;;
;;http://localhost:3000/cloodle.html?event=ngKS_s4ovZ_Nn6oOF3cfUg
;;FX8eO5tHq-f2L3_yWiIY6g
;(defn get-existing-event[eventhash]
;  (go
;   (let [response (<! (http/get (str "api/event/" eventhash) {}))
;         status (:status response)]
;       (print (str "GOT FROM SERVER " (:body response)))
;       (let [event (:body response)
;             new-event (assoc event :new-participant {
;                                                      :name ""
;                                                      :selections []
;                                                      })
;             new-event2 (assoc new-event :saved "true")
;             new-event3 (assoc new-event2 :cloodle-code eventhash)]
;         new-event3))))


;;;;;;;;;;;;







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





(defn get-selection-by-id [selections id]

  (first (filter #(= (:id %) id) selections)))

(defn handle-slider-change [val selections option owner]

  (print "Option " (:id @option) " to val " val)

  (if-let [selection (get @selections (:id @option))]

    ;; UPDATE EXISTING SELECTION
    (om/transact! selections (fn [xs]
                               (let [id (:id @option)
                                     selection (get-selection-by-id xs id)]

                                 (update-in @selections [id] (fn [_] val)))))

                                 ; (conj (remove #(= (:id %) id) xs) {:optionId id :value val}))))

    ;; ADD NEW SELECTION
    (om/transact! selections (fn [xs] (assoc xs (:id @option) val)))))


(defcomponent slider [{:keys [option selections]} owner]

      (render [_]
              (dom/div #js {:className "slider col-sm-6"} nil))

      (did-mount [state]
                 (let [$slider-element ($ (.getDOMNode owner))
                       parameters #js {:start (:value option)
                                       :range #js {"max" #js [100] "min" #js [0]}
                                       :step 1
                                       :format (js/wNumb #js {:mark "," :decimals 1})
                                       }]


                   (.noUiSlider $slider-element parameters)
                   (.on $slider-element #js {:slide #(handle-slider-change (js/parseInt (.val $slider-element)) selections option owner)}))))



(defcomponent option-slider [cursors owner]


  (render-state [this state]
          (dom/div #js {:className "row" :style #js {:margin-bottom "15px"}}
            (dom/div #js {:className "col-sm-2"} (:name (:option cursors)))
            (om/build slider cursors))
))



(defn handle-participant-name-change [e owner]
  (om/set-state! owner :participant-name (.. e -target -value)))

(defn initialize-option-votes [options]
  (map (fn [option] {:id (:id option) :name (:name option) :value 50}) options))


(defn vote->save-payload [new-participant app-state new-id]

  ;; TODO GET RID OF THE DUMMY PARTICIPANT! Was added to avoid the issue where the posted json's single element lists where
  ;; converted to just the single element (without the enclosing list) by some middleware. Fix the middleware issue, then
  ;; remove the dummy participant

  (let [new-participant-with-id (assoc-in new-participant [:id] new-id)
        state-with-new-participant

        (assoc-in app-state [:participants]
            (vec (conj (get-in app-state [:participants]) new-participant-with-id)))

        state-with-enough-participants

        (assoc-in state-with-new-participant [:participants]
            (vec (conj (get-in state-with-new-participant [:participants]) {:id -1 :name "dummy" :selections {1 0, 2 0}})))



        ]




        (apply dissoc state-with-enough-participants [:saved :new-participant])

    ))

(defcomponent vote-component [{:keys [app-state new-participant options]} owner]

    (init-state [_]
              {:save-vote-chan (chan)
               :new-participant-id (inc (max-id (:participants app-state)))})


    (will-mount [_]
              (let [save (om/get-state owner :save-vote-chan)]


                ;; SAVE VOTE
                (go (loop []
                      (let [new-participant (<! save)
                            payload (vote->save-payload new-participant @app-state (om/get-state owner :new-participant-id))]



                        (print payload)

                        (go
                         (let [response (<! (http/post "api/event/join" {:json-params payload}))
                               status (:status response)]


                           (print response)

                           ))

                        (recur))))))



  (render-state [this state]

          (dom/div #js {:style #js {:border "solid black 1px"}}

                   (dom/div #js {:className "row form-group"}

                            (dom/label #js {:htmlFor "participant-name"
                                            :className "col-sm-2 control-label"}
                                       "Name")

                            (dom/div #js {:className "col-sm-6"}
                                     (dom/input #js {:id "name"
                                                     :className "form-control"
                                                     :type "text"
                                                     :value (:name new-participant)
                                                     :ref "participant-name"
                                                     :onChange  (fn [e]
                                                                  (om/transact! new-participant :name (fn [_] (.. e -target -value))))
                                                     })))


                   (apply dom/div nil
                          (om/build-all option-slider (map
                                                       (fn [option-cursor] {:option option-cursor
                                                                            :selections (:selections new-participant)})
                                                       options))
                          )



                   (dom/button #js {:type "button"
                                             :className "btn btn-success"
                                             :onClick (fn [e] (put! (:save-vote-chan state) @new-participant))}
                                        "Save vote")


                   ; (dom/pre nil (prn-str state))

                   )))



(defcomponent participant-component [cursors owner]

  (render [this]


          (om/build vote-component cursors)


          ))

(defcomponent participant-list [cursors owner]


  (render [this]


          (dom/div nil
                   (dom/h1 nil "Participants")
                   (apply dom/div nil
                          (om/build-all participant-component
                                        (let [cursors (map
                                                       (fn [participant-cursor] {:app-state (:app-state cursors)
                                                                                 :participant participant-cursor
                                                                                 :options (:options cursors)})
                                                       (:participants cursors))]



                                          cursors))))))



(defcomponent state-debug [app-state owner]

  (render [this]

          (dom/pre nil (prn-str app-state))))

(defcomponent main-page [app-state owner]

  (render [this]
          (dom/div nil

                   ;; TITLE AND DESCRIPTION (FOR EXISTING EVENT)
                   (om/build title-and-description
                             (select-keys app-state [:name :description])
                             {:state {:visible (:saved app-state)}})


                   ;; LIST PARTICIPANTS
                   (if (:saved app-state)

                     (dom/div nil
                     (om/build vote-component {:app-state app-state
                                               :new-participant (:new-participant app-state)
                                               :options (:options app-state)})

                     (om/build participant-list {:participants (:participants app-state)
                                                 :app-state app-state
                                                 :options (:options app-state)
                                                 }))

                     )


                   ;; NEW EVENT FORM
                   (if (not (:saved app-state))
                     (om/build cloodle-form
                               app-state
                               {:state {:saved (:saved app-state)}}))

                   ;; CLOODLE-CODE SHARING INFO BOX
                   (om/build cloodle-code (:cloodle-code app-state) {:state {:saved (:saved app-state)}})




                    (om/build state-debug app-state)


                   )))


(defn get-existing-event[cloodle-code output-channel]
  (go
   (let [response (<! (http/get (str "api/event/" cloodle-code)))
         status (:status response)]
     (print (str "GOT FROM SERVER " (:body response)))
     (let [event (:body response)

           new-event (merge event {:new-participant { :name ""
                                                      :selections {}
                                                      }
                                   :saved true
                                   })]

       (put! output-channel new-event)))))

(defn build-initial-state [state-destination]
  "Builds the initial states and puts it in the given channel"

  (let [cloodle-code (get-cloodle-code)]

    (if cloodle-code
      (do
        (print "Existing Event")
        (get-existing-event cloodle-code state-destination))

      (do
        (print "New Event")
        (let [new-event-state {
                               :name "Movie Night"
                               :description "Let's drink beer and watch an Arnie movie"
                               :options [
                                         {:id 1 :name "Terminator 2"}
                                         {:id 2 :name "Commando"}
                                         {:id 3 :name "Conan The Barbarian"}
                                         {:id 4 :name "Junior"}]
                               :cloodle-code ""
                               :saved false}]
          (put! state-destination new-event-state))))))


(let [initial-state-chan (chan)
      app-state (atom nil)]

  (go

   (build-initial-state initial-state-chan)

   (let [state (<! initial-state-chan)]
        (reset! app-state state)
        (om/root main-page app-state
                 {:target (. js/document (getElementById "my-app"))}))))
















