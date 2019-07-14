(ns ocastio.pages.ballot
  (:require
    [ocastio.views :as v]
    [ocastio.db :as db]
    [ocastio.pages.vote :as vote]
    [clojure.string :as str]
    [hiccup.page :as page]
    [ring.util.anti-forgery :as util]))

(defn page-all [request compose]
  (compose "Ballot" nil [:p "ballots"]))
(defn page [request compose] (vote/page request compose false))
(defn new! [request] (vote/new-ballot! request)) ;TODO shortcut


(defn make-law-row [law-info]
  (let [title   (:title law-info)
        law-id  (:law_id law-info)
        id      (str "law" law-id)]
    [:tr
      [:td
        [:input {:type "checkbox" :name id :id id}]
        [:label {:for id}]]
      [:td title]]))

(defn make-law-table [con-laws]
  [:table#laws
    [:tr [:th ""] [:th "Law"]]
    (map make-law-row con-laws)])

(defn page-new [{para :params :as request} compose]
  (let [con-id    (Integer. (:con_id para))
        con-info  (db/con-info con-id)
        con-name  (:title con-info)
        con-laws  (db/con-laws con-id)]
    (compose "New Ballot" (page/include-js "/js/vote.js")
      [:p "Enter the details of the new ballot in " [:a {:href (str "/con/" con-id)} con-name] "."]
      (vote/make-form-new "ballot" "Ballot" con-id (make-law-table con-laws)))))

(defn extract-int [s] (Integer. (re-find  #"\d+" s)))
