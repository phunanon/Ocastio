(ns ocastio.pages.law
  (:require
    [ocastio.views :as v]
    [ocastio.db :as db]
    [ocastio.pages.vote :as vote]
    [clojure.string :as str]
    [ring.util.anti-forgery :as util]))

(def get-para #(get-in % [:params %2]))
(def get-sess #(get-in % [:session %2]))

(defn page [request compose]
  (let [get-para    (partial get-para request)
        get-sess    (partial get-sess request)
        law-id      (get-para :law_id)
        law-id      (Integer/parseInt law-id)
        info        (db/law-info law-id)
        title       (:title   info)
        body        (:body        info)
        con-id      (:con_id    info)
        con-name    (:con_title info)
        parent-id   (:parent_id   info)
        parent-name (:title (db/law-info parent-id))
        children    (db/law-children law-id)
        email       (get-sess :email)
        is-exec?    (db/con-exec? con-id email)]
  (compose (str "Law: " title) nil
    [:navinfo "Part of " [:a {:href (str "/con/" con-id)} con-name] "."
    (if (some? parent-id) [:span " A child of " [:a {:href (str "/law/" parent-id)} parent-name] "."])]
    [:h2 "Law: " title]
    [:quote body]
  (if (not-empty children) [:h3 "Children"])
  (if is-exec?
    [:p.admin [:a {:href (str "/law/new/" con-id "/" law-id)} "Create a child law"] "."])
  [:ul (map v/make-law-link children)])))

(defn page-new [request compose]
  (let [get-para    (partial get-para request)
        get-sess    (partial get-sess request)
        session     (:session request)
        uri         (:uri request)
        parent-id   (get-para :parent-id)
        parent-id   (Integer/parseInt parent-id)
        parent-name (:title (db/law-basic-info parent-id))
        con-id      (get-para :con-id)
        con-id      (Integer/parseInt con-id)
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
        [:input    {:type "text"    :name "title"   :placeholder "Title"    :value (:form-title   session)}]
        [:textarea {:type "text"    :name "body"    :placeholder "Law body" :value (:form-body    session)}]
        [:input    {:type "submit"  :value "Create"}]])))


; TODO check law doesn't already exist
(defn new! [request]
  (let [get-para  (partial get-para request)
        get-sess  (partial get-sess request)
        session   (:session request)
        con-id    (get-para :con-id)
        con-id    (Integer/parseInt con-id)
        parent-id (get-para :parent-id)
        parent-id (Integer/parseInt parent-id)
        title     (get-para :title)
        body      (get-para :body)
        email     (get-sess :email)
        law-id    (db/law-new! con-id parent-id email title body)]
  {:redir (str "/law/" law-id) :sess session}))
