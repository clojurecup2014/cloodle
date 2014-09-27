(ns cloodle.client

  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]))


;; TODO: CORE ASYNC

(enable-console-print!)

(def form-state
  (atom
   {

    :name "Test Cloodle Name"
    :description "Test Description"
    :options [
              {:name "Terminator 2"}
              {:name "Commando"}
              {:name "Conan The Barbarian"}
              {:name "Junior"}]
    :cloodle-code ""



    }))



(defcomponent option-container [option-data owner]
  (render [this]

          (dom/div #js {:style #js {:margin-bottom "5px"}}
                   (dom/div #js {:className "pull-left"} (:name option-data))
                   (dom/div #js {:className "pull-right"}

                            (dom/button #js {:className "btn btn-danger btn-xs"
                                              }

                                        (dom/span #js {:className "glyphicon glyphicon-trash"} nil)))

                   (dom/br #js {:className "clearfix"} nil))


  ))

(defn add-option [form-data owner]

  (let [new-option (-> (om/get-node owner "new-option")
                       .-value)]
    (when new-option
      (om/transact! form-data :options #(conj % {:name new-option})))))

(defcomponent new-option-component [form-data owner]
  (render [this]

          (dom/div #js {:className "form-group"}
                   (dom/div #js {:className "col-sm-2"} nil)
                   (dom/div #js {:className "col-sm-2"}

                            (dom/input #js {:id "new-option-name"
                                            :className "form-control"
                                            :type "text"
                                            :ref "new-option"}))

                   (dom/div #js {:className "col-sm-6"}
                            (dom/button #js {:type "button"
                                             :className "btn btn-success"
                                             :onClick #(add-option form-data owner)}
                                        "Add option")))))






(defcomponent cloodle-form [form-data owner]
  (render [this]

          (dom/div #js {:className "form-group"}
                   (dom/form #js {:className "form-horizontal" :role "form"}

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
                                                               :value (:name form-data)})))

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
                                                               :value (:description form-data)})))


                             (dom/div #js {:className "form-group"}
                                      (dom/label #js {:className "col-sm-2 control-label"}
                                                 "Options")

                                      (apply dom/div #js {:className "col-sm-6"}
                                               (om/build-all option-container (:options form-data))))


                             (om/build new-option-component form-data)




                             (dom/div #js {:className "form-group"}

                                      (dom/div #js {:className "col-sm-2"} nil)
                                      (dom/div #js {:className "col-sm-6"}
                                               (dom/button #js {:type "button" :className "btn btn-success"}
                                                           (dom/span #js {:className "glyphicon glyphicon-ok"})
                                                           (dom/span nil " Create!")))





                                      )))))


(om/root cloodle-form form-state
         {:target (. js/document (getElementById "my-app"))})


