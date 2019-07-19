(ns ocastio.router
  (:require
    [ocastio.pages.home    :as hom]
    [ocastio.pages.signin  :as sig]
    [ocastio.pages.org     :as org]
    [ocastio.pages.poll    :as pol]
    [ocastio.pages.ballot  :as bal]
    [ocastio.pages.vote    :as vot]
    [ocastio.pages.con     :as con]
    [ocastio.pages.law     :as law]
    [ocastio.db :as db]
    [ocastio.views :as v]
    [ring.util.response :as resp]))

(defn make-response [body]
  (-> (resp/response body)
      (resp/content-type "text/html")))

(defn no-auth    [para sess] true)
(defn signed?    [para sess] (contains? sess :email))
(defn org-admin? [{:keys [org-id]} {:keys [email]}]
  (db/org-admin? org-id email))
(defn con-exec? [{:keys [con-id]} {:keys [email]}]
  (db/con-exec? con-id email))
(defn law-exec? [{:keys [law-id]} {:keys [email]}]
  (let [{:keys [con_id]} (db/law-basic-info law-id)]
    (db/con-exec? con_id email)))

(def pages {
            ;maker          auth func
  :home     [hom/page       no-auth]
  :signin   [sig/page       no-auth]
  :orgs     [org/orgs-page  no-auth]
  :org-new  [org/page-new   signed?]
  :org      [org/org-page   no-auth]
  :org-mems [org/page-mems  signed?] ;TODO
  :pol-new  [pol/page-new   signed?] ;TODO
  :poll     [pol/page       no-auth]
  :con      [con/page       no-auth]
  :con-new  [con/page-new   signed?] ;TODO
  :law      [law/page       no-auth]
  :law-new  [law/page-new   signed?] ;TODO
  :ballots  [bal/page-all   no-auth]
  :bal-new  [bal/page-new   signed?] ;TODO
  :ballot   [bal/page       no-auth]
})

(defn page [request where]
  (if (contains? pages where)
    (let [para        (:params request)
          sess        (:session request)
          entry       (where pages)
          maker       (entry 0)
          authed?     ((entry 1) para sess)
          compose     (partial v/compose-page request)
          sign-redir  (str "/signin?redir=" (:uri request))]
      (if authed?
        (make-response (maker request compose))
        (resp/redirect sign-redir)))
    (make-response (str "router: unknown page: " where))))


(def posts {
            ;maker          auth func
  :sign     [sig/sign!      no-auth]
  :signout  [sig/signout    no-auth]
  :org-new  [org/new!       signed?]
  :org-mem  [org/add-mems!  org-admin?]
  :pol-new  [vot/new-pol!   org-admin?]
  :con-new  [con/new!       org-admin?]
  :con-mem  [con/add-mem!   signed?]
  :con-del  [con/del!       con-exec?]
  :law-new  [law/new!       con-exec?]
  :law-del  [law/del!       law-exec?]
  :bal-new  [vot/new-bal!   con-exec?]
  :bal-del  [vot/del!       signed?]
  :vote     [vot/vote!      signed?]
})

;TODO: make :sess optional for doer's
(defn do-action [{:keys [params session referer] :as request} what]
  "Returns {:redir location, :sess newSession}"
  (let [entry      (what posts)
        doer       (nth entry 0)
        authed?    ((entry 1) params session)]
    (if authed?
      (doer request)
      {:redir referer :sess session})))

(defn post [request what]
  (if (some? (what posts))
    (let [result   (do-action request what)
          session  (:sess  result)
          redirect (:redir result)] ;TODO: rename :next
      (-> (resp/redirect redirect)
          (assoc :session session)
          (resp/content-type "text/html")))
    (make-response (str "router: unknown post: " what))))

(defn signin-post [request]
  (let [response (resp/redirect "/")
        sign-map (sig/sign! request)
        response (assoc response :session sign-map)]
    (resp/content-type response "text/html")))
