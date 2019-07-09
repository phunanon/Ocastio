(ns ocastio.pages.con
  (:require
    [ocastio.views :as v]
    [ocastio.db :as db]
    [clojure.string :as str]
    [clojure.set :as set]
    [hiccup.page :as page]
    [ring.util.anti-forgery :as util]))

(def get-para #(get-in % [:params %2])) ;TODO: remove
(def get-sess #(get-in % [:session %2]))

(defn page-new [request]
  (let [get-para  (partial get-para request)
        get-sess  (partial get-sess request)
        session   (:session request)
        uri       (:uri request)
        org-id    (get-para :org_id)
        org-info  (db/org-info org-id)
        org-name  (:name org-info)]
    (v/compose-page request "New constitution" nil
      [:p "Enter the details of the new constitution for " [:a {:href (str "/org/" org-id)} org-name] "."]
      [:form {:action uri :method "POST"}
        (util/anti-forgery-field)
        [:input    {:type "text"    :name "title" :placeholder "Constitution title" :value (:form-title session)}]
        [:textarea {                :name "desc"  :placeholder "Description"        :value (:form-desc  session)}]
        [:input    {:type "submit"  :value "Create"}]])))

; TODO check const doesn't already exist, redirect to view const, check title/desc not nil
(defn new! [request]
  (let [get-para  (partial get-para request)
        get-sess  (partial get-sess request)
        org-id    (get-para :org_id)
        email     (get-sess :email)
        admin?    (db/org-admin? org-id email)
        session   (:session request)
        sess      {}
        redir     (str "/org/" org-id)
        title     (get-para :title)
        desc      (get-para :desc)]
    (if admin?
      (do (db/con-new! org-id title desc)
          {:redir redir :sess (into session sess)})
      {:redir redir :sess (into session sess)})))

(defn add-mem! [{{:keys [con-id org-id exec]} :params
                 {:keys [email] :as sess}     :session :as request}]
  (let [con-id (Integer. (re-find #"\d+$" con-id))
        admin? (db/org-admin? org-id email)
        exec?  (db/con-exec? con-id email)
        exec   (= exec "true")]
    (if exec
      (if exec?
        "") ;Do nothing, for now; TODO
      (if admin?
        (db/add-con-org! org-id con-id false))))
  {:redir (str "/org/" org-id) :sess sess})

(defn del! [{{email :email :as sess} :session
             {con-id :con-id}        :params}]
  (if (db/con-exec? con-id email)
    (db/con-del! con-id))
  {:redir "/orgs" :sess sess})


(defn vote-bar [vote%]
  [:votebar [:forbar {:style (str "width: " vote% "%")}]])

(defn render-leaf [leaf by-parent parent-in]
    (let [vote%        (:vote% leaf)
          vote-in      (> vote% 50)
          can-in       (and parent-in vote-in)
          body         (:body leaf)
          body         (if (= body "") "No body." body)]
    [:leaf  (vote-bar vote%)
      [:a {:href (str "/law/" (:law_id leaf))}
        [:leaftitle {:class (if can-in "in" "out")} (:title leaf)]]
      [:leafvotes {:class (if vote-in "in" "out")} (format "%.2f%%" vote%)]
      [:description body]
      (map #(render-leaf % by-parent can-in) (by-parent (:law_id leaf)))]))

(defn render-tree [by-parent]
  (map #(render-leaf % by-parent {:vote% 100}) (by-parent 0)))

(defn render-laws [con-id]
  (let [all-laws (map #(assoc % :vote% (rand 100)) (db/con-laws con-id))
        by-parent (group-by :parent_id all-laws)
        by-parent (set/rename-keys by-parent {nil 0})]
   [:tree (render-tree by-parent)]))


(defn render-ballots [con-id]
  (map #(v/make-ballot-link % "ballot") ))


(defn make-org-link [{:keys [org_id name is_exec]}]
  (v/li-link
    [:span (if is_exec "Executive" "Member") ": "
      [:bl name]]
    (str "/org/" org_id)))


; TODO admin+exec check
(defn page [request]
  (let [get-para  (partial get-para request)
        get-sess  (partial get-sess request)
        con-id    (get-para :con_id)
        con-id    (Integer. con-id)
        orgs      (db/con-orgs-info con-id)
        laws      (db/con-laws con-id)
        info      (db/con-info con-id)
        title     (:title info)
        desc      (:desc info)
        num-mem   (db/con-num-mem con-id)
        email     (get-sess :email)
        exec?  (db/con-exec? con-id email)

        ballots   (db/con-ballots con-id)]
    (v/compose-page request title (page/include-css "/css/constitution.css")
    (if exec? [:p.admin "You are an admin of an executive organisation."])
    [:h2 title [:grey " | Constitution"]]
      [:quote desc]
      [:p "In total, " [:b num-mem] " people are affected by this constitution."]
    [:h3 "Oraganisations"]
      [:ul (map make-org-link orgs)]
    [:h3 "Ballots"]
      (if (and exec? (not-empty laws)) [:p.admin [:a {:href (str "/ballot/new/" con-id)} "Post a new ballot"]])
      (if (and exec? (empty?    laws)) [:p.admin "You must have Laws to post new ballots."])
      (if (empty? ballots) [:p "No ballots yet."])
      (v/make-ballot-links ballots "ballot")
    [:h3 "Laws"]
      [:p "Note: vote percentages are currently randomised, but in the future will be extrapolated from the latest ballots."]
      (if exec?
        [:p.admin [:a {:href (str "/law/new/" con-id "/0")} "Compose a new law"]])
      ;[:ul (map v/make-law-link laws)]
      (render-laws con-id)
    [:br]
    (if exec?
      (v/make-del-button (str "/con/del/" con-id) "constitution")))))
