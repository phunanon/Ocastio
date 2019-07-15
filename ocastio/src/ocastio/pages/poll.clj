(ns ocastio.pages.poll
  (:require
    [ocastio.views :as v]
    [ocastio.db :as db]
    [ocastio.pages.vote :as vote]
    [clojure.string :as str]
    [hiccup.page :as h]
    [ring.util.anti-forgery :as util]))

(defn page [request compose] (vote/page request compose true))

(def option-form
  [:section#options
    [:p "Press Enter to add another option, or Backspace in an empty option to delete."]
    [:div#options
      [:input {:type "text" :name "opt0" :placeholder "Option body" :onkeydown "return OptionKey(event, this)"}]]])

(defn page-new [{{:keys [org-id] :as para} :params} compose]
  (let [org-id      (Integer. org-id)
        org-info    (db/org-info org-id)
        org-name    (:name org-info)]
    (compose "New Poll" (h/include-js "/js/vote.js")
      [:p "Enter the details of the new poll in " [:a {:href (str "/org/" org-id)} org-name] "."]
      (vote/make-form-new "poll" "Poll" org-id option-form))))
