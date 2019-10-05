(ns ocastio.pages.con
  (:require
    [ocastio.html.result :as r]
    [ocastio.views :as v]
    [ocastio.db :as db]
    [clojure.string :as str]
    [clojure.set :as set]
    [hiccup.page :as page]
    [ring.util.anti-forgery :as util]))

(defn make-con-stats-link [{:keys [con_id title desc num-bal num-mem num-org] :as info}]
  (v/li-link
    [:span.con
      [:stat "👤 " num-mem] [:stat "👥 " num-org] [:stat "📊 " num-bal] [:bl title]
      [:br]
      [:span desc]]
    (str "/con/" con_id)))

(defn supplement-con-info [{:keys [con_id] :as con-info}]
  (into con-info
    {:num-bal (db/con-num-ballots con_id)
     :num-mem (db/con-num-mem con_id)
     :num-org (db/con-num-org con_id)}))

(defn cons-page [request compose]
  (let [all-cons (db/cons-infos)
        all-cons (map supplement-con-info all-cons)
        all-cons (sort-by :num-mem > all-cons)]
  (compose "Organisations" nil
    [:p "These are all the constitutions hosted on Ocastio. "]
    [:ul (map make-con-stats-link all-cons)])))

(defn page-new [{{:keys [org-id]} :params uri :uri} compose]
  (let [{:keys [name]} (db/org-info org-id)]
    (compose "New constitution" nil
      [:p "Enter the details of the new constitution for " [:a {:href (str "/org/" org-id)} name] "."]
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



(defn render-leaf [{:keys [approval won? law_id bal-id title body]} by-parent parent-active?]
  (let [active?      (and parent-active? won?)
        approval     (if approval (* approval 100) 0)]
  [:leaf
    [:a {:href (str "/law/" law_id)}
      [:leaftitle {:class (if active? "in" "out")} title]]
    (if bal-id
      [:a.leafapp
        {:class (if won? "in" "out")
         :href  (str "/ballot/" bal-id)}
        (format "%.2f%%" approval)])
    [(if (= body "") :leafdesc.none :leafdesc) [:pre body]]
    (map #(render-leaf % by-parent active?)
          (by-parent law_id))]))

(defn render-tree [by-parent]
  (map #(render-leaf % by-parent true) (by-parent 0)))

(defn assoc-result [{:keys [law_id] :as law}]
  (into law
    (select-keys (r/law-result law_id)
      [:bal-id :approval :won?])))

(defn render-laws [all-laws]
  (let [by-parent (group-by :parent_id all-laws)
        by-parent (set/rename-keys by-parent {nil 0})]
   [:tree (render-tree by-parent)]))


(defn make-org-link [{:keys [org_id name is_exec]}]
  (v/li-link
    [:span (if is_exec "Executive" "Member") ": "
      [:bl name]]
    (str "/org/" org_id)))


; TODO admin+exec check
(defn page [{{:keys [con-id]} :params
             {:keys [email]}  :session}
             compose]
  (let [orgs     (db/con-orgs-info con-id)
        laws     (db/con-laws con-id)
        {:keys [title desc]}
          (db/con-info con-id)
        num-mem  (db/con-num-mem con-id)
        is-exec  (db/con-exec? con-id email)

        ballots  (db/con-ballots con-id)
        all-laws (db/con-laws con-id)
        all-laws (map assoc-result all-laws)
        num-act  (count (filter (comp true? :won?) all-laws))
        num-ina  (- (count all-laws) num-act)]
    (compose title (page/include-css "/css/constitution.css")
    (page/include-js "/js/listload.js")
    (if is-exec [:p.admin "You are an admin of an executive organisation."])
    [:h2 title [:grey " | Constitution"]]
      (if (seq desc) [:quote desc])
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
      (if is-exec
        [:p.admin [:a {:href (str "/law/new/" con-id "/0")} "Compose a new law"]])
      (if is-exec
        [:p.admin [:a {:href (str "/law/imp/" con-id)} "Import new law"]])
      [:p num-act " active, " num-ina " inactive."]
      (render-laws all-laws)
    [:br]
    (if is-exec
      (v/make-del-button (str "/con/del/" con-id) "constitution")))))
