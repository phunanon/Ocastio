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
  :org-mems [org/page-mems  org-admin?]
  :pol-new  [pol/page-new   org-admin?]
  :poll     [pol/page       no-auth]
  :con      [con/page       no-auth]
  :con-new  [con/page-new   org-admin?]
  :law      [law/page       no-auth]
  :law-new  [law/page-new   con-exec?]
  :ballots  [bal/page-all   no-auth]
  :bal-new  [bal/page-new   con-exec?]
  :ballot   [bal/page       no-auth]})

(defn page [{:keys [params session uri] :as request} where]
  (if (contains? pages where)
    (let [entry       (where pages)
          maker       (entry 0)
          authed?     ((entry 1) params session)
          compose     (partial v/compose-page request)
          sign-redir  (str "/signin?ref=" uri)]
      (if authed?
        (make-response (maker request compose))
        (resp/redirect sign-redir)))
    (make-response (str "router: unknown page: " where))))


(def posts {
  ;doer           auth func
  :sign     [sig/sign!      no-auth]
  :signout  [sig/signout    no-auth]
  :org-new  [org/new!       signed?]
  :org-mems [org/mod-mems!  org-admin?]
  :org-info [org/set-info!  org-admin?]
  :pol-new  [vot/new-pol!   org-admin?]
  :con-new  [con/new!       org-admin?]
  :con-mem  [con/add-mem!   signed?]
  :con-del  [con/del!       con-exec?]
  :law-new  [law/new!       con-exec?]
  :law-del  [law/del!       law-exec?]
  :bal-new  [vot/new-bal!   con-exec?]
  :bal-del  [vot/del!       signed?]
  :vote     [vot/vote!      signed?]})

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
    (let [{:keys [redir sess]} ;TODO: rename :next
            (do-action request what)]
      (-> (resp/redirect redir)
          (assoc :session sess)
          (resp/content-type "text/html")))
    (make-response (str "router: unknown post: " what))))