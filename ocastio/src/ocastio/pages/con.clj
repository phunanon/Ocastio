(ns ocastio.pages.con
  (:require
    [ocastio.views :as v]
    [ocastio.db :as db]
    [clojure.string :as str]
    [clojure.set :as set]
    [hiccup.page :as page]
    [ring.util.anti-forgery :as util]))

(defn page-new [{uri              :uri
                 {:keys [org-id]} :params} compose]
  (let [org-info  (db/org-info org-id)
        org-name  (:name org-info)]
    (compose "New constitution" nil
      [:p "Enter the details of the new constitution for " [:a {:href (str "/org/" org-id)} org-name] "."]
      [:form {:action uri :method "POST"}
        (util/anti-forgery-field)
        [:input    {:type "text"    :name "title" :placeholder "Constitution title"}]
        [:textarea {                :name "desc"  :placeholder "Description"}]
        [:input    {:type "submit"  :value "Create"}]])))

; TODO check const doesn't already exist, redirect to view const, check title/desc not nil
(defn new! [{{:keys [org-id title desc]} :params
             {:keys [email] :as sess}    :session}]
  (db/con-new! org-id title desc)
  {:redir (str "/org/" org-id) :sess sess})

(defn add-mem! [{{:keys [con-id org-id exec adopt add redir]} :params
                 {:keys [email] :as sess}                     :session}]
  "If an org has adopted a con it is made an exec, otherwise org adopts con."
  (let [con-id    (Integer. (re-find #"\d+$" con-id))
        is-admin  (db/org-admin? org-id email)
        is-exec   (db/con-exec? con-id email)
        add-exec  (= exec "true")]
    (if add-exec
      (if (db/org-in-con? org-id con-id)
        (if is-exec
          (if add ;Than remove == true
            (db/add-con-org! org-id con-id true)
            (db/add-con-org! org-id con-id false))))
      (if is-admin
        (if adopt ;Than remove == true
          (db/add-con-org! org-id con-id false)
          (db/rem-con-org! org-id con-id))))
    {:redir redir :sess sess}))

(defn del! [{sess            :session
            {:keys [con-id]} :params}]
  (db/con-del! con-id)
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
(defn page [{{:keys [con-id]} :params
             {:keys [email]}  :session}
             compose]
  (let [con-id    (Integer. con-id)
        orgs      (db/con-orgs-info con-id)
        laws      (db/con-laws con-id)
        info      (db/con-info con-id)
        title     (:title info)
        desc      (:desc info)
        num-mem   (db/con-num-mem con-id)
        is-exec  (db/con-exec? con-id email)

        ballots   (db/con-ballots con-id)]
    (compose title (page/include-css "/css/constitution.css")
    (if is-exec [:p.admin "You are an admin of an executive organisation."])
    [:h2 title [:grey " | Constitution"]]
      [:quote desc]
      [:p "In total, " [:b num-mem] " people are affected by this constitution."]
    [:h3 "Oraganisations"]
    (if is-exec
      [:section.admin
        [:form {:action (str "/con/exe/" con-id"?redir=/con/" con-id) :method "POST"}
          (util/anti-forgery-field)
          [:input {:type "hidden" :name "exec" :value "true"}]
          [:p "Add or remove an executive Organisation by ID or Ocastio URL:"]
          [:input {:name "org-id" :placeholder "e.g. 7 or .../org/7"}]
          [:input {:type "submit" :name "add" :value "Add"}]
          [:input {:type "submit" :name "remove" :value "Remove"}]]
          [:p "An Organisation must already have adopted this Constitution prior."]])
      [:ul (map make-org-link orgs)]
    [:h3 "Ballots"]
      (if (and is-exec (not-empty laws)) [:p.admin [:a {:href (str "/ballot/new/" con-id)} "Post a new ballot"]])
      (if (and is-exec (empty?    laws)) [:p.admin "You must have Laws to post new ballots."])
      (if (empty? ballots) [:p "No ballots yet."])
      (v/make-ballot-links ballots "ballot")
    [:h3 "Laws"]
      [:p "Note: vote percentages are currently randomised, but in the future will be extrapolated from the latest ballots."]
      (if is-exec
        [:p.admin [:a {:href (str "/law/new/" con-id "/0")} "Compose a new law"]])
      ;[:ul (map v/make-law-link laws)]
      (render-laws con-id)
    [:br]
    (if is-exec
      (v/make-del-button (str "/con/del/" con-id) "constitution")))))
