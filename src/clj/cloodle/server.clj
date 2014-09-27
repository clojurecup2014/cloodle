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
  (GET "/api" [] (ring/response {:hello "Howdy ho!"}))
  (POST "/api/event" {params :params}
        (println params)
        (ring/response "All good!"))
  (route/resources "/")
  (route/not-found "Not found"))

(def app
  (-> (handler/api app-routes)
   (middleware/wrap-json-body)
   (middleware/wrap-json-response)
   (middleware/wrap-json-params)))

(defn -main [& args]
;  (dao/test-stuff)
  (jetty/run-jetty app {:port 80}))
