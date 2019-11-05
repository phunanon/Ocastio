(ns ocastio.external.engine
  (:require
    [ocastio.db          :as db]
    [ocastio.views       :as v]
    [ocastio.pages.vote  :as vote]
    [ocastio.html.result :as r]
    [clj-time.format     :as f]
    [clj-time.coerce     :as cljt]
    [clojure.string      :as str]
    [clojure.edn         :as edn]
    [clojure.core.async :refer [go <!]]))


(defn cmd-help [prefix text send-tx! _ _]
  (send-tx!
    "How to use this bot:"
    (apply str
      (map #(str "\n" prefix %)
        ["register email password\n  create and link an Ocastio account"
         "auth email password\n  link Telegram and Ocastio accounts"
         "info ballot-id\n  ballot/poll info"
         "mine\n  see ballots/polls you can vote on"
         "vote ballot-id 0-n ...\n  vote on ballots/polls. 0-n is the range, with 0-1 for approval voting."]))))




(defn bal-link [ballot-id title type]
  (str "\"" title "\" https://ocastio.uk/" type "/" ballot-id))

(defn parse-bal-cmd [text]
  (let [text      (str/split text #" ")
        ballot-id (edn/read-string (text 1))
        exists    (and (int? ballot-id)
                       (db/ballot-exists? ballot-id))]
    {:text text :ballot-id ballot-id :exists exists}))

(defn ballot-info [ballot-id]
  (let [{:keys [start hours] :as info}
          (db/ballot-info ballot-id)
        is-poll  (db/poll? ballot-id)
        type     (if is-poll "poll" "ballot")
        Type     (str/capitalize type)]
    (into info
      {:num-vote (db/ballot-num-votes ballot-id)
       :abstains (db/num-abstain ballot-id)
       :state (v/ballot-state info)
       :is-poll is-poll :type type :Type Type})))

(defn bal-opts [{:keys [ballot_id is-poll]}]
  ((if is-poll db/bal-pol-options db/bal-law-options) ballot_id))

(defn assoc-results [opts results]
  (let [sort    (partial sort-by :opt_id)
        opts    (sort opts)
        results (sort results)]
    (map into opts results)))

(defn make-option-item [{:keys [state preresult]}
                        {:keys [text title approval sum won?]}
                        i]
  (let [results? (or preresult (= state :complete))
        approval (int (* approval 100))
        results
          (if results?
            (str
              (format " %,3d%%" approval)
              (format " %,3d" sum) " "
              (if won? "✅" "  ")))]
    (str "`" (inc i) "." results " `" text title)))

(defn cmd-info [text send-tx! send-md! _]
  (let [{:keys [text ballot-id exists]}
          (parse-bal-cmd text)]
    (if exists
      (let [{:keys [title desc start hours state
                    num-vote abstains is-poll type Type] :as bal-info}
              (ballot-info ballot-id)
            desc        (if (seq desc) (str "\"" desc "\"\n") "")
            options     (bal-opts bal-info)
            options     (assoc-results options (r/ballot-results ballot-id))
            options     (map (partial make-option-item bal-info) options (range))
            options     (str/join "\n" options)
            method-info (apply str (drop 1 (v/make-method-info bal-info)))
            remaining   (v/ballot-time-status bal-info)
            info-msg    (str
                          (bal-link ballot-id title type)
                          "\n" remaining
                          "\n" (v/ballot-time-str bal-info) " for " hours "h"
                          "\n" method-info "\n" desc options
                          "\n*" num-vote (v/plu " vote" num-vote) "* with " abstains " abstain.")]
        (send-md! info-msg))
      (send-tx! "Ballot not found."))))




(defn cmd-register! [text send-tx! send-md! contact-id]
  (let [[_ email pass]
          (str/split text #" ")
        email     (str/lower-case email)
        e-exists  (db/email-exists? email)
        a-exists  (db/contact->id contact-id)
        e-invalid (not (v/valid-email? email))
        p-invalid (< (count pass) 8)]
    (if e-invalid (send-tx! "Invalid email."))
    (if p-invalid (send-tx! "Password must be >7 characters."))
    (if e-exists  (send-tx! "Email already registered."))
    (if a-exists  (send-tx! "You're already authorised."))
    (when (not (or e-invalid p-invalid e-exists a-exists))
      (db/new-user! email pass)
      (db/set-user-contact! email contact-id)
      (send-tx! "Email registered and authenticated."))))




(defn cmd-auth! [text send-tx! send-md! contact-id]
  (let [[_ email pass]
          (str/split text #" ")
        email (str/lower-case email)]
    (if (db/correct-pass? email pass)
      (do
        (db/set-user-contact! email contact-id)
        (send-tx! "Authenticated."))
      (send-tx! "Unable to authenticate."))))




(defn simple-item [type {:keys [ballot_id title]}]
  (str ballot_id ": https://ocastio.uk/" type "/" ballot_id "\n   " title))

(defn cmd-mine [text send-tx! send-md! contact-id]
  (def user-id (db/contact->id contact-id))
  (if user-id
    (let [bals   (concat (db/user-polls user-id) (db/user-ballots user-id))
          do-list #(str/join "\n" (map (partial simple-item %) %2))]
      (send-md! "Votes available to you:\n"
        (if (seq bals)
          (do-list "poll" bals)
          "None are available for you at this time.")))
    (send-tx! "You are unauthenticated.")))





(defn test-vote->reason [test-vote]
  ({:auth "all okay"
    :noauth "unauthorised"
    :complete "it's closed"
    :future "it has not yet begun"}
      test-vote))

(defn cmd-vote! [text send-tx! send-md! contact-id]
  (let [{:keys [text ballot-id exists]}
          (parse-bal-cmd text)]
    (if exists
      (let [{:keys [title desc method_id type is-poll start hours sco_range] :as bal-info}
              (ballot-info ballot-id)
            options     (map :opt_id (bal-opts bal-info))
            text        (map #(or ({"y" "1" "n" "0" "a" "abstain"} (str/lower-case %)) %) text)
            choices     (map edn/read-string (drop 2 text))
            is-mass     (<= 6 method_id 7)
            is-abstain  (= (first choices) 'abstain)
            is-valid
              (if is-abstain
                true
                (and
                  (every? int? choices)
                  (not-any? neg? choices)
                  (every? #(<= % sco_range) choices)
                  (= (count choices) (if is-mass 1 (count options)))))
            user-id     (db/contact->id contact-id)
            test-vote   (vote/test-vote bal-info user-id)
            reason      (test-vote->reason test-vote)
            state       (v/ballot-state bal-info)
            link        (bal-link ballot-id title type)
            choices     (if is-mass (repeat (count options) (first choices)) choices)]
        (if is-valid
          (if (= test-vote :authed)
            (if is-abstain
              (do
                (db/vote-new! user-id ballot-id {} true)
                (send-md! "Abstained on " link "!"))
              (let [choices (map hash-map options choices)
                    choices (reduce into choices)]
                (db/vote-new! user-id ballot-id choices false)
                (send-md! "Vote cast on " link)))
            (send-tx! "You are unable to vote on this " type ": " reason "."))
          (send-tx! "Invalid options.")))
      (send-tx! "Ballot not found."))))
