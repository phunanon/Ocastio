(ns ocastio.pages.org
  (:require
    [ocastio.views :as v]
    [ocastio.db :as db]
    [clojure.string :as str]
    [ring.util.anti-forgery :as util]))

(defn split-by-whitespace [s]
  (clojure.string/split s #"\s+"))

(defn make-org-link [{:keys [org_id name members cons img]}]
  (v/li-link
    [:span
      [:img.small {:src img}]
      [:bl name] [:br]
      [:span (str members " " (v/plu "member" members) ", "
                  cons " " (v/plu "constitution" cons))]]
    (str "/org/" org_id)))

(defn make-con-link [{:keys [con_id title desc is_exec]}]
  (v/li-link
    [:span
      (if is_exec "Executive" "Member") " of " [:bl title]
      [:br]
      [:span "\"" desc "\""]]
    (str "/con/" con_id)))


(defn orgs-page [request]
  (v/compose-page request "Organisations" nil
    [:p "These are all the organisations registered with Ocastio. " [:a {:href "/org/new"} "Register your own."]]
    [:ul (map make-org-link (db/orgs-stats))]))

(defn org-page [request]
  (let [session (:session request)
        org-id  (get-in request [:route-params :org_id])
        org-id  (Integer/parseInt org-id)
        email   (:email session)
        admin?  (db/org-admin? org-id email)
        info    (db/org-info org-id)
        name    (:name info)
        members (:members info)
        desc    (:desc info)
        cons    (db/con-infos org-id)
        polls   (db/ballot-infos org-id)]
    (v/compose-page request name nil
      [:h2 name [:grey " | Organisation"]]
      [:p [(if admin? :a :span)
        {:href (str "/org/mems/" org-id)}
        members " " (v/plu "member" members)] "."]
      [:quote desc]
      (if admin?
        [:section.admin
          [:form {:action (str "/con/add/" org-id) :method "POST"}
            (util/anti-forgery-field)
            [:input {:type "hidden" :name "exec" :value "false"}]
            [:p "Adopt a Constitution by providing its ID or Ocastio URL:"]
            [:input {:name "con-id"}]
            [:input {:type "submit" :value "Adopt"}]]])
      [:h3 "Constitutions"]
        (if admin? [:p.admin [:a {:href (str "/con/new/" org-id)} "New constitution"]])
        (if (empty? cons) [:p "Not a member of any constitutions."])
        [:ul (map make-con-link cons)]
      [:h3 "Polls"]
        (if admin? [:p.admin [:a {:href (str "/poll/new/" org-id)} "New poll"]])
        (if (empty? polls) [:p "No polls posted."])
        (v/make-ballot-links polls "poll"))))

;TODO: advocate auth func
(defn page-mems [{{email :email :as sess} :session :as request}]
  (let [org-id  (get-in request [:route-params :org-id])
        org-id  (Integer/parseInt org-id)
        admin?  (db/org-admin? org-id email)
        info    (db/org-info org-id)
        name    (:name info)]
    (v/compose-page request "Members management" nil
      [:p.admin "Administrative section for " (v/org-link org-id name) "."]
      [:h2 "Members Management"]
      [:p "Add members by providing their emails below (one per line). Add ! at the start of the email to make them admins."]
        [:form {:action (str "/org/add-mem/" org-id) :method "POST"}
          (util/anti-forgery-field)
          [:textarea {:name "emails"}]
          [:input {:type "submit" :value "Add members"}]]
      [:ul
        (map str (db/org-mems org-id))])))

(defn page-new [request]
  (v/compose-page request "New Organisation" nil
    [:p "Enter the details of your new organisation."]
    [:form {:action "/org/new" :method "POST"}
      (util/anti-forgery-field)
      [:input {:placeholder "Organisation name" :name "name" :value (:form-name (:session request))}] ;TODO remove re-fill logic
      [:textarea {:name "desc" :placeholder "Organisation description"}]
      [:input {:name "contact" :placeholder "Contact link"}]
      [:input {:type "submit" :value "Register" :name "register"}]]))

(def get-param #(get-in % [:params %2])) ;TODO: remove
(def get-sess  #(get-in % [:session %2]))

;TODO generally improve
(defn new! [request]
  (let [get-param (partial get-param request)
        get-sess  (partial get-sess  request)
        name      (get-param :name)
        desc      (get-param :desc)
        contact   (get-param :contact)
        email     (get-sess  :email)
        exists?   (db/org-name-exists? name)
        redirect  (if exists? "/org/new" "/orgs")
        session   (:session request)
        return    (fn [redir sess] {:redir redir :sess (into session sess)})]
    (if exists?
      (return "/org/new" {:new-org-error :exists})
      (do (db/org-new! email name desc contact)
          (return redirect {})))))

;TODO: short-circuit non-admins quicker; advocate POST admin auth functions
(defn add-mems! [request]
  (let [get-param (partial get-param request)
        get-sess  (partial get-sess  request)
        org-id    (get-param :org_id)
        org-id    (Integer/parseInt org-id)
        email     (get-sess :email)
        admin?    (db/org-admin? org-id email)
        new-mems  (get-param :emails)
        new-mems  (vec (set (split-by-whitespace new-mems)))
        admin?    #(str/starts-with? % "!")
        new-mems  (map #(hash-map :admin? (admin? %) :email (str/replace % #"!" "")) new-mems)
        new-mems  (filter #(db/email-exists? (:email %)) new-mems)
        new-mems  (map #(assoc % :user-id (db/email->id (:email %))) new-mems) ;TODO use nil from email->id
        new-mems  (filter #(not (db/user-in-org? org-id (:user-id %))) new-mems)]
    (if admin?
      (doseq [{:keys [user-id admin?]} new-mems]
        (db/add-to-org! org-id user-id admin?)))
    {:redir (str "/org/" org-id) :sess (:session request)}))
