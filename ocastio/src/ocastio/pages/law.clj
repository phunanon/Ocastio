(ns ocastio.pages.law
  (:require
    [ocastio.views :as v]
    [ocastio.db :as db]
    [ocastio.pages.vote :as vote]
    [clojure.string :as str]
    [ring.util.anti-forgery :as util]))

(defn con-link [con-id]
  [:a {:href (str "/con/" con-id)}
      (:title (db/con-info con-id))])

(defn page [{{:keys [law-id]} :params
             {:keys [email]}  :session}
            compose]
  (let [law-id      (Integer. law-id)
        {:keys [title body con_id con_title parent_id]}
          (db/law-info law-id)
        body        (if (= body "") "No body." body)
        parent-name (:title (db/law-info parent_id))
        children    (db/law-children law-id)
        ballots     (db/law-ballots law-id nil)
        is-exec     (db/con-exec? con_id email)
        btn-new     #(vector :a {:href (str "/law/new/" con_id "/" %)} %2)]
  (compose title nil
    [:navinfo "Part of " [:a {:href (str "/con/" con_id)} con_title] "."
      (if parent_id [:span " A child of " [:a {:href (str "/law/" parent_id)} parent-name] "."])]
    [:h2 title [:grey " | Law"]]
    [:quote [:pre body]]
    [:h3 "Children"]
    (if is-exec
      [:p.admin
        (btn-new law-id "Compose a child law") " or "
        (btn-new (if parent_id parent_id "0") "a sibling law")])
    (if (empty? children) [:p "This law has no child laws."])
    [:ul (map v/make-law-link children)]
    [:h3 "Ballots"]
    [:ul (map #(v/li-link (:title %) (str "/ballot/" (:ballot_id %))) ballots)]
    (if is-exec
      (v/make-del-button (str "/law/del/" law-id) "law")))))

(defn page-new [{{:keys [uri parent-id con-id]} :params} compose]
  (let [parent-id   (Integer. parent-id)
        parent-name (:title (db/law-basic-info parent-id))]
    (compose "New law" nil
      [:p "Enter the details of the new law, for " (con-link con-id) "."]
      [:form {:action uri :method "POST"}
        (util/anti-forgery-field)
        [:p [:b "Parent: "]
          (if (= parent-id 0)
              "None (highest level of law)"
              [:a {:href (str "/law/" parent-id)} parent-name])]
        [:input        {:type "text" :name "title" :placeholder "Title"}]
        [:textarea.big {:type "text" :name "body"  :placeholder "Law body"}]
        [:input        {:type "submit"  :value "Compose"}]])))


; TODO check law doesn't already exist
(defn new! [{{:keys [con-id parent-id title body]} :params
             {:keys [email] :as sess}              :session}]
  (let [con-id    (Integer. con-id)
        parent-id (Integer. parent-id)
        law-id    (db/law-new! con-id parent-id email title body)]
  {:redir (str "/law/" law-id) :sess sess}))

(defn del! [{{:keys [law-id]} :params sess :session}]
  (let [{:keys [con_id]} (db/law-basic-info law-id)]
    (db/law-del! law-id)
    {:redir (str "/con/" con_id) :sess sess}))
