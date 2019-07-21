(ns ocastio.pages.law
  (:require
    [ocastio.views :as v]
    [ocastio.db :as db]
    [ocastio.pages.vote :as vote]
    [clojure.string :as str]
    [ring.util.anti-forgery :as util]))

(defn page [{{:keys [law-id]} :params
             {:keys [email]}  :session}
            compose]
  (let [law-id      (Integer. law-id)
        {:keys [title body con_id con_title parent_id]}
          (db/law-info law-id)
        body        (if (= body "") "No body." body)
        parent-name (:title (db/law-info parent_id))
        children    (db/law-children law-id)
        is-exec     (db/con-exec? con_id email)]
  (compose title nil
    [:navinfo "Part of " [:a {:href (str "/con/" con_id)} con_title] "."
      (if parent_id [:span " A child of " [:a {:href (str "/law/" parent_id)} parent-name] "."])]
    [:h2 title [:grey " | Law"]]
    [:quote [:pre body]]
  (if (not-empty children) [:h3 "Children"])
  (if is-exec
    [:p.admin [:a {:href (str "/law/new/" con_id "/" law-id)} "Create a child law"]])
  (if is-exec
    (v/make-del-button (str "/law/del/" law-id) "law"))
  [:ul (map v/make-law-link children)])))

(defn page-new [{{:keys [uri parent-id con-id]} :params} compose]
  (let [parent-id   (Integer. parent-id)
        parent-name (:title (db/law-basic-info parent-id))
        con-id      (Integer. con-id)
        con-info    (db/con-info con-id)
        con-name    (:title con-info)
        con-link    (str "/con/" con-id)]
    (compose "New law" nil
      [:p "Enter the details of the new law, for " [:a {:href con-link} con-name] "."]
      [:form {:action uri :method "POST"}
        (util/anti-forgery-field)
        [:p [:b "Parent: "]
          (if (= parent-id 0)
              "None (highest level of law)"
              [:a {:href (str "/law/" parent-id)} parent-name])]
        [:input    {:type "text"    :name "title"   :placeholder "Title"}]
        [:textarea {:type "text"    :name "body"    :placeholder "Law body"}]
        [:input    {:type "submit"  :value "Create"}]])))


; TODO check law doesn't already exist
(defn new! [{{:keys [con-id parent-id title body]} :params
             {:keys [email] :as sess}              :session}]
  (let [con-id    (Integer. con-id)
        parent-id (Integer. parent-id)
        law-id    (db/law-new! con-id parent-id email title body)]
  {:redir (str "/law/" law-id) :sess sess}))

(defn del! [{{:keys [law-id]} :params
             sess             :session}]
  (let [{:keys [con_id]} (db/law-basic-info law-id)]
    (db/law-del! law-id)
    {:redir (str "/con/" con_id) :sess sess}))
