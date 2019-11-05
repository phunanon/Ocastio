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
  :email-invalid  "The email supplied is invalid."
  :token-invalid  "The password reset token was invalid/expired."})

(defn page [{{:keys [ref token]} :params
             {:keys [sign-err]}  :session}
            compose]
  (compose "Signin" nil
    (if token
      [:p "Confirm your email and enter your new password."]
      [:p "Signin or register as an Ocastio user."])
    (if sign-err [:b (sign-err error-messages)])
    [:form {:action (str "/sign?ref=" (if ref ref "/")) :method "POST"}
      (util/anti-forgery-field)
      (if token [:input {:type "hidden" :value token :name "token"}])
      [:input {:type "text"     :placeholder "Email" :name "email"
               :required true :minlength 6}]
      [:input {:type "password" :placeholder "Password" :name "password"
               :required true :minlength 7}]
      (if token
        [:input {:type "submit"   :value "Reset password" :name "reset"}]
        [:span
          [:input {:type "submit"   :value "Sign in" :name "signin"}]
          [:input {:type "submit"   :value "Register" :name "register"}]
          [:p "If you need to change your password email "
            [:a {:href "mailto:phunanon@gmail.com"} "phunanon@gmail.com"] "."]])]))

(defn sign! [{{:keys [email password ref token
                      signin register reset] :as params} :params}]
  (let [email     (str/lower-case email)
        exists    (db/email-exists? email)
        return    #(hash-map :redir % :sess %2)
        succeed   #(return (if ref ref "/") {:email email})
        err-retry #(return (str "/signin?ref=" ref) {:sign-err %})]
    (if-not (v/valid-email? email)
      (err-retry :email-invalid)
      (if (or signin reset)
        (if-not exists
          (err-retry :email-nonexist)
          (if reset
            (let [{salt :salt} (db/pass-details email)
                  salt         (v/hexify salt)
                  is-correct   (= salt token)]
              (if is-correct
                (do
                  (db/change-pass email password)
                  (succeed))
                (err-retry :token-invalid)))
            (if (db/correct-pass? email password)
              (succeed)
              (err-retry :pass-incorrect))))
        (if exists
          (err-retry :email-exists)
          (do (db/new-user! email password)
              (succeed)))))))

(defn signout [{{ref "referer"} :headers}]
  {:redir (if ref ref "/") :sess {}})
