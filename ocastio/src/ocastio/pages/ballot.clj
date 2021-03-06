(ns ocastio.pages.ballot
  (:require
    [ocastio.views :as v]
    [ocastio.db :as db]
    [ocastio.pages.vote :as vote]
    [clojure.string :as str]
    [hiccup.page :as h]
    [ring.util.anti-forgery :as util]))

(defn page-all [request compose]
  (compose "Ballot" nil [:p "ballots"])) ;TODO
(defn page [request compose] (vote/page request compose false))


(defn make-law-row [law-info]
  (let [title   (:title law-info)
        law-id  (:law_id law-info)
        id      (str "law" law-id)]
    [:tr
      [:td
        [:input {:type "checkbox" :name id :id id :onchange "OptionsChanged()"}]
        [:label {:for id}]]
      [:td title]]))

(defn make-law-table [con-laws]
  [:table#laws
    [:tr [:th ""] [:th "Law"]]
    (map make-law-row con-laws)])

(defn page-new [{{:keys [con-id]} :params} compose]
  (let [con-id    (Integer. con-id)
        con-info  (db/con-info con-id)
        con-name  (:title con-info)
        con-laws  (db/con-laws con-id)]
    (compose "New Ballot" (h/include-js "/js/vote.js")
      [:p "Enter the details of the new ballot in " [:a {:href (str "/con/" con-id)} con-name] "."]
      (vote/make-form-new "ballot" "Ballot" con-id (make-law-table con-laws)))))

(defn extract-int [s] (Integer. (re-find  #"\d+" s)))
