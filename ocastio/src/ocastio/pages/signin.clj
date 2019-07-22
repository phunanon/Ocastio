(ns ocastio.pages.signin
  (:require
    [ocastio.views :as v]
    [ocastio.db :as db]
    [clojure.string :as str]
    [ring.util.anti-forgery :as util]))

(def error-messages {
  :pass-incorrect "The password supplied was incorrect."
  :email-nonexist "The email supplied was not recognised."
  :email-exists   "The email supplied is already registered."
  :email-invalid  "The email supplied is invalid."})

; TODO don't allow empty username, password
(defn page [{{sess :session redir :redir :as para} :params :as request}
            compose]
  (let [action    (if (nil? redir) "/sign" (str "/sign?redir=" redir))
        sign-err  (:sign-err sess)]
    (compose "Signin" nil
      [:p "Signin or register as an Ocastio user."]
      (if (some? sign-err) [:b (sign-err error-messages)])
      [:form {:action action :method "POST"}
        (util/anti-forgery-field)
        [:input {:type "text"     :placeholder "Email" :name "email" :value (:form-email sess)}]
        [:input {:type "password" :placeholder "Password" :name "password"}]
        [:input {:type "submit"   :value "Sign in" :name "signin"}]
        [:input {:type "submit"   :value "Register" :name "register"}]])))

(defn sign! [{{email :email pass :password :as para} :params :as request}]
  (let [signin?   (nil? (:register para))
        exists?   (db/email-exists? email)
        session   (:session request)
        session   (dissoc session :sign-err :form-email)
        redirect  (:redir para)
        nextdir   (if (nil? redirect) "/" redirect)
        return    (fn [redir sess] {:redir redir :sess (into session sess)})]
    (if (v/valid-email? email)
      (if signin?
        (if exists?
          (if (db/correct-pass? email pass)
            (return nextdir   {:email email})
            (return "/signin" {:sign-err :pass-incorrect :form-email email}))
          (return "/signin" {:sign-err :email-nonexist :form-email email}))
        (if exists?
          (return "/signin" {:sign-err :email-exists :form-email email})
          (do (db/new-user! email pass)
              (return nextdir {:email email}))))
      (return "/signin" {:sign-err :email-invalid :form-email email}))))

(defn signout [{{redir :redir} :params}]
  {:redir (if redir redir "/") :sess {}})
