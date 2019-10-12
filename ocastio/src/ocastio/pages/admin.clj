(ns ocastio.pages.admin
  (:require
    [ocastio.views :as v]
    [ocastio.db :as db]
    [clojure.string :as str]
    [hiccup.page :as page]
    [ring.util.anti-forgery :as util]))

(defn page [{uri :uri} compose]
  (compose "Admin panel" nil
    [:p "Enter an email to get its password change link."]
    [:form {:action uri :method "POST"}
      (util/anti-forgery-field)
      [:input {:name "pass-reset-email"}]
      [:input {:type "submit" :value "Get link"}]]))

(defn post [{{:keys [pass-reset-email]} :params
             sess                       :session}]
  (cond
    pass-reset-email
      (let [{salt :salt}
              (db/pass-details pass-reset-email)
            link (str "/signin?token=" (v/hexify salt))]
        {:redir link :sess sess})
    :else {:redir "/admin" :sess sess}))
