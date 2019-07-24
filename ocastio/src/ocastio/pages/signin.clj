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

(defn page [{{:keys [ref]}      :params
             {:keys [sign-err]} :session}
            compose]
  (compose "Signin" nil
           [:p "Signin or register as an Ocastio user."]
           (if (some? sign-err) [:b (sign-err error-messages)])
           [:form {:action (str "/sign?ref=" ref) :method "POST"}
            (util/anti-forgery-field)
            [:input {:type "text"     :placeholder "Email" :name "email"
                     :pattern ".{6,}" :required true :title ">6 char"}]
            [:input {:type "password" :placeholder "Password" :name "password"
                     :pattern ".{7,}" :required true :title ">7 char"}]
            [:input {:type "submit"   :value "Sign in" :name "signin"}]
            [:input {:type "submit"   :value "Register" :name "register"}]]))

(defn sign! [{{:keys [email password ref register]} :params}]
  (let [is-signin (nil? register)
        exists    (db/email-exists? email)
        return    #(hash-map :redir % :sess %2)
        succeed   #(return (if ref ref "/") {:email email})
        err-retry #(return (str "/signin?ref=" ref) {:sign-err %})]
    (if (v/valid-email? email)
      (if is-signin
        (if exists
          (if (db/correct-pass? email password)
            (succeed)
            (err-retry :pass-incorrect))
          (err-retry :email-nonexist))
        (if exists
          (err-retry :email-exists)
          (do (db/new-user! email password)
              (succeed))))
      (err-retry :email-invalid))))

(defn signout [{{ref "referer"} :headers}]
  {:redir (if ref ref "/") :sess {}})
