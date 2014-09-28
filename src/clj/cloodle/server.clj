(ns cloodle.server
  (:require [ring.adapter.jetty :as jetty]
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [ring.middleware.resource :as resources]
            [ring.middleware.json :as middleware]
            [ring.util.response :as ring]
            [compojure.route :as route]
            [cloodle.mongodao :as dao])
  (:gen-class))

(defn render-app []
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body
   (str "<!DOCTYPE html>"
        "<html>"
        "<head>"
        "<link rel=\"stylesheet\" href=\"css/page.css\" />"
        "</head>"
        "<body>"
        "<div>"
        "<p id=\"clickable\">Click me!</p>"
        "</div>"
        "<script src=\"js/cljs.js\"></script>"
        "</body>"
        "</html>")})

(defroutes app-routes
  (GET "/" [] (ring/redirect "/front.html"))
  (POST "/api/event" {params :params} ;; if params are empty, check that you have Content-Type: application/json. br, Jarkko
        (prn params)
        (dao/create-event params))
  (GET "/api/event/:eventhash" [eventhash] 
;       (prn " Getting from mongo! " eventhash) 
       (ring/response (dao/get-by-eventhash eventhash)))
  (route/resources "/")
  (route/not-found "Not found"))

(def app
  (-> (handler/api app-routes)
   (middleware/wrap-json-body)
   (middleware/wrap-json-response)
   (middleware/wrap-json-params)))

(defn -main []
  (cloodle.mongodao/init)
  (jetty/run-jetty app {:port 80}))
