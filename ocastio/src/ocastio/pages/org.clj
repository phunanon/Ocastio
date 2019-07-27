(ns ocastio.pages.org
  (:require
    [ocastio.views :as v]
    [ocastio.db :as db]
    [clojure.string :as str]
    [ring.util.anti-forgery :as util]))

(defn split-by-whitespace [s]
  (clojure.string/split s #"\s+"))

(defn make-org-link [{:keys [org_id name num-mem num-con num-pol img]}]
  (v/li-link
    [:span.org
      [:img.small {:src img}]
      [:bl name] [:br]
      [:stat "👥 " num-mem] [:stat "📜 " num-con] [:stat "📊 " num-pol]]
    (str "/org/" org_id)))

(defn make-con-link [{:keys [con_id title desc is_exec]}]
  (v/li-link
    [:span
      (if is_exec "Executive" "Member") " of " [:bl title]
      [:br]
      [:span "\"" desc "\""]]
    (str "/con/" con_id)))


(defn orgs-page [request compose]
  (compose "Organisations" nil
    [:p "These are all the organisations registered with Ocastio. " [:a {:href "/org/new"} "Register your own."]]
    [:ul (map make-org-link (db/orgs-stats))]))

;TODO: destructuring and general improvement
(defn org-page [request compose]
  (let [session (:session request)
        org-id  (get-in request [:route-params :org_id])
        org-id  (Integer. org-id)
        email   (:email session)
        admin?  (db/org-admin? org-id email)
        info    (db/org-info org-id)
        name    (:name info)
        members (:members info)
        desc    (:desc info)
        cons    (db/con-infos org-id)
        polls   (db/ballot-infos org-id)]
    (compose name nil
      [:h2 name [:grey " | Organisation"]]
      [:p [(if admin? :a :span)
        {:href (str "/org/mems/" org-id)}
        members " " (v/plu "member" members)] "."]
      [:quote desc]
      (if admin?
        [:section.admin
          [:form {:action (str "/con/mem/" org-id "?redir=/org/" org-id) :method "POST"}
            (util/anti-forgery-field)
            [:p "Adopt or remove a Constitution by ID or Ocastio URL:"]
            [:input {:name "con-id" :placeholder "e.g. 7 or .../con/7"}]
            [:input {:type "submit" :name "adopt" :value "Adopt"}]
            [:input {:type "submit" :name "remove" :value "Remove"}]]])
      [:h3 "Constitutions"]
        (if admin? [:p.admin [:a {:href (str "/con/new/" org-id)} "New constitution"]])
        (if (empty? cons) [:p "Not a member of any constitutions."])
        [:ul (map make-con-link cons)]
      [:h3 "Polls"]
        (if admin? [:p.admin [:a {:href (str "/poll/new/" org-id)} "New poll"]])
        (if (empty? polls) [:p "No polls posted."])
        (v/make-ballot-links polls "poll"))))

(defn page-mems [{{:keys [org-id]} :params
                  {:keys [email]}  :session}
                 compose]
  (let [org-id         (Integer. org-id)
        {:keys [name]} (db/org-info org-id)]
    (compose "Members management" nil
      [:p.admin "Administrative section for " (v/org-link org-id name) "."]
      [:h2 "Members Management"]
      [:p "Provide user emails below, one per line. Add ! at the start of emails to make them admins."]
        [:form {:action (str "/org/mod-mem/" org-id) :method "POST"}
          (util/anti-forgery-field)
          [:textarea {:name "emails"}]
          [:input {:type "submit" :name "doadd" :value "Add members"}]
          [:input {:type "submit" :name "dorem" :value "Remove members"}]]
      [:ul
        (map str (db/org-mems org-id))])))

(defn page-new [request compose]
  (compose "New Organisation" nil
    [:p "Enter the details of your new organisation."]
    [:form {:action "/org/new" :method "POST"}
      (util/anti-forgery-field)
      [:input {:placeholder "Organisation name" :name "name"}]
      [:textarea {:name "desc" :placeholder "Organisation description"}]
      [:input {:name "contact" :placeholder "Contact link"}]
      [:input {:type "submit" :value "Register" :name "register"}]]))

(defn new! [{{:keys [name desc contact]} :params
             {:keys [email] :as sess}    :session}]
  (if (db/org-name-exists? name)
    {:redir "/org/new" :sess (into sess {:new-org-error :exists})}
    (do (db/org-new! email name desc contact)
        {:redir "/orgs" :sess sess})))

(defn mod-mems! [{{:keys [org-id emails doadd dorem]} :params
                  {:keys [email] :as sess}            :session}]
  (let [org-id    (Integer. org-id)
        mems      (vec (set (split-by-whitespace emails)))
        admin?    #(str/starts-with? % "!")
        mems      (map #(hash-map :admin? (admin? %) :email (str/replace % #"!" "")) mems)
        mems      (map #(assoc % :user-id (db/email->id (:email %))) mems)
        mems      (filter #(some? (:user-id %)) mems)
        in-org?   #(db/user-in-org? org-id (:user-id %))
        mems      (filter (if dorem in-org? (complement in-org?)) mems)]
    (doseq [{:keys [user-id admin?]} mems]
      (if dorem
        (db/rem-from-org! org-id user-id)
        (db/add-to-org! org-id user-id admin?))))
  {:redir (str "/org/mems/" org-id) :sess sess})
