(ns ocastio.handler
  (:use
    [ring.middleware.gzip])
  (:require
    [ocastio.router :as rou]
    [ocastio.db :as db]
    [compojure.core :refer :all]
    [compojure.route :as route]
    [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
    [ring.util.response :as resp]))

;TODO use - rather than _
(defroutes app-routes
  (GET  "/"                           req (rou/page req :home))
  (GET  "/signin"                     req (rou/page req :signin))
  (POST "/sign"                       req (rou/post req :sign))
  (GET  "/signout"                    req (rou/post req :signout))
  (GET  "/orgs"                       req (rou/page req :orgs))
  (GET  "/org/new"                    req (rou/page req :org-new))
  (POST "/org/new"                    req (rou/post req :org-new))
  (GET  "/org/:org_id"                req (rou/page req :org))
  (POST "/org/add-mem/:org_id"        req (rou/post req :org-mem))
  (GET  "/org/mems/:org-id"           req (rou/page req :org-mems))
  (GET  "/poll/new/:org_id"           req (rou/page req :poll-new))
  (POST "/poll/new/:org_id"           req (rou/post req :poll-new))
  (POST "/bal/del/:ballot-id"         req (rou/post req :bal-del))
  (GET  "/poll/:ballot_id"            req (rou/page req :poll))
  (POST "/vote/:ballot_id"            req (rou/post req :vote))
  (GET  "/con/:con-id"                req (rou/page req :con))
  (GET  "/con/new/:org-id"            req (rou/page req :con-new))
  (POST "/con/new/:org-id"            req (rou/post req :con-new))
  (POST "/con/mem/:org-id"            req (rou/post req :con-mem))
  (POST "/con/exe/:con-id"            req (rou/post req :con-mem))
  (POST "/con/del/:con-id"            req (rou/post req :con-del))
  (GET  "/law/:law_id"                req (rou/page req :law))
  (GET  "/law/new/:con_id/:parent_id" req (rou/page req :law-new))
  (POST "/law/new/:con_id/:parent_id" req (rou/post req :law-new))
  (GET  "/ballots"                    req (rou/page req :ballots))
  (GET  "/ballot/new/:con_id"         req (rou/page req :bal-new))
  (POST "/ballot/new/:con_id"         req (rou/post req :bal-new))
  (GET  "/ballot/:ballot_id"          req (rou/page req :ballot))
  (GET  "/debug"                      req (resp/content-type (resp/response (str req)) "text/html"))
  (POST "/debug"                      req (resp/content-type (resp/response (str req)) "text/html"))
  (route/resources "/")
  (route/not-found "Not found. Go back."))

(defn wrap-cache [handler]
  #((assoc-in (handler %) [:headers "Cache-Control"] "max-age=604800, public")))

(def app (-> app-routes
    (wrap-defaults site-defaults)
    (wrap-gzip)))

;lein ring server-headless
;java -cp ~/.m2/repository/com/h2database/h2/1.4.193/h2-1.4.193.jar org.h2.tools.Server
