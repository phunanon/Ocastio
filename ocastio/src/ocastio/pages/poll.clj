(ns ocastio.pages.poll
  (:require
    [ocastio.views :as v]
    [ocastio.db :as db]
    [ocastio.pages.vote :as vote]
    [clojure.string :as str]
    [hiccup.page :as page]
    [ring.util.anti-forgery :as util]))

(defn page [request compose] (vote/page request compose true)) ;
(defn new! [request] (vote/new-poll! request)) ;TODO shortcut

(def option-form
  [:section#options
    [:p "Press Enter to add another option, or Backspace in an empty option to delete."]
    [:div#options
      [:input {:type "text" :name "opt0" :placeholder "Option body" :onkeydown "return OptionKey(event, this)"}]]])

(defn page-new [{para :params :as request} compose]
  (let [methods     (db/vote-methods)
        org-id      (Integer. (:org_id para))
        org-info    (db/org-info org-id)
        org-name    (:name org-info)]
    (compose "New Poll" (page/include-js "/js/vote.js")
      [:p "Enter the details of the new poll in " [:a {:href (str "/org/" org-id)} org-name] "."]
      (vote/make-form-new "poll" "Poll" org-id option-form))))
