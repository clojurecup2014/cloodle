(ns cloodle.client
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]))


(def form-state
  (atom
   {

    :name "Test Cloodle Name"
    :description "Test Description"
    :options []
    :cloodle-code ""

    }))

(defcomponent cloodle-form [form-data owner]
  (render [this]

          (dom/div #js {:className "form-group"}
                   (dom/form #js {:className "form-horizontal" :role "form"}

                             ;; TODO: Label / Input pairs as components?
                             ;; NAME
                             (dom/div #js {:className "form-group"}

                                      ;; TODO: Label / Input pairs as components?
                                      (dom/label #js {:for "name"
                                                      :className "col-sm-2 control-label"}
                                                 "Event name")
                                      (dom/div #js {:className "col-sm-6"}
                                               (dom/input #js {:id "name"
                                                               :className "form-control"
                                                               :type "text"
                                                               :value (:name form-data)})))

                             ;; DESCRIPTION
                             (dom/div #js {:className "form-group"}
                                      (dom/label #js {:for "description"
                                                      :className "col-sm-2 control-label"}
                                                 "Event description")
                                      (dom/div #js {:className "col-sm-10"}
                                               (dom/input #js {:id "description"
                                                               :className "form-control"
                                                               :type "text"
                                                               :value (:description form-data)})))
                             )
          )))

(om/root cloodle-form form-state
         {:target (. js/document (getElementById "my-app"))})


