(ns ocastio.external.telegram
  (:require
    [ocastio.db          :as db]
    [ocastio.views       :as v]
    [ocastio.pages.vote  :as vote]
    [ocastio.html.result :as r]
    [clj-time.format     :as f]
    [clj-time.coerce     :as cljt]
    [morse.api           :as t]
    [morse.handlers      :as h]
    [morse.polling       :as p]
    [clojure.string      :as str]
    [clojure.edn         :as edn]
    [clojure.core.async :refer [go <!]]))

(def token (slurp "telegram.tkn"))
(defn send-tx! [id & body]
  (t/send-text token id (str/join "" body)))
(defn send-md! [id & body]
  (t/send-text token id {:parse_mode "Markdown"} (str/join "" body)))
(defn bal-link [ballot-id title type]
  (str "[" title "](https://ocastio.uk/" type "/" ballot-id ")"))

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

(defn cmd-start [{{:keys [id]} :chat}]
  (send-tx! id "Type /help for options."))

(defn cmd-help [{{:keys [id]} :chat}]
  (send-md! id "/register email password\n  create and link an Ocastio account"
              "\n/auth email password\n  link Telegram and Ocastio accounts"
              "\n/info ballot-id\n  ballot/poll info"
              "\n/mine\n  see ballots/polls you can vote on"
              "\n/vote ballot-id 0-n ...\n  vote on ballots/polls. 0-n is the range, with 0-1 for approval voting."))

(defn cmd-register! [{{:keys [id]}       :chat
                      {:keys [username]} :from
                      text               :text}]
  (let [text      (str/split text #" ")
        email     (text 1)
        pass      (text 2)
        e-exists  (db/email-exists? email)
        a-exists  (db/contact->id id)
        e-invalid (not (v/valid-email? email))
        p-invalid (< (count pass) 8)]
    (if e-invalid (send-tx! id "Invalid email."))
    (if p-invalid (send-tx! id "Password must be >7 characters."))
    (if e-exists  (send-tx! id "Email already registered."))
    (if a-exists  (send-tx! id "Email already authorised."))
    (when (not (or e-invalid p-invalid e-exists a-exists))
      (db/new-user! email pass)
      (db/set-user-contact! email username)
      (send-tx! id "Email registered and authenticated."))))

(defn cmd-auth! [{{:keys [id]}       :chat
                  {:keys [username]} :from
                  text               :text}]
  (let [text  (str/split text #" ")
        email (text 1)
        pass  (text 2)]
    (if (db/correct-pass? email pass)
      (do
        (db/set-user-contact! email username)
        (send-tx! id "Authenticated."))
      (send-tx! id "Unable to authenticate."))))

(defn make-option-item [{:keys [state preresult]}
                        {:keys [text title approval sum won?]}
                        i]
  (let [can-show (or preresult (= state :complete))
        approval (int (* approval 100))
        approval (format " %,3d%%" approval)
        approval (if can-show approval "")
        sum      (format " %,3d" sum)
        sum      (if can-show sum)
        emoji    (if won?  "✅" "  ")]
    (str "`" (inc i) "." approval sum " " emoji " `" text title)))

(defn cmd-info [{{:keys [id]} :chat
                 text         :text}]
  (let [{:keys [text ballot-id exists]}
          (parse-bal-cmd text)]
    (if exists
      (let [{:keys [title desc start hours state
                    num-vote abstains is-poll type Type] :as bal-info}
              (ballot-info ballot-id)
            start-str   (f/unparse (f/formatter "yy-MM-dd HH:mm") (cljt/from-sql-date start))
            options     (bal-opts bal-info)
            options     (assoc-results options (r/ballot-results ballot-id))
            options     (map (partial make-option-item bal-info) options (range))
            options     (str/join "\n" options)
            method-info (str/join "" (drop 1 (v/make-method-info bal-info)))
            remaining   (str (v/ballot-remain-str bal-info) " remaining")
            remaining   (case state :complete "complete" :future "future" :ongoing remaining)
            info-msg    (str
                          (bal-link ballot-id (str Type " " ballot-id) type)
                          ", " remaining
                          "\n" start-str ", " hours "h long"
                          "\n*" title "*"
                          "\n" method-info "\n_" desc "_\n" options
                          "\n*" num-vote (v/plu " vote" num-vote) "* with " abstains " abstain.")]
        (send-md! id info-msg))
      (send-tx! id "Ballot not found."))))

(defn test-vote->reason [test-vote]
  ({:auth "all okay"
    :noauth "unauthorised"
    :complete "it's closed"
    :future "it has not yet begun"}
      test-vote))

(defn cmd-vote! [{{:keys [id]}       :chat
                  {:keys [username]} :from
                  text               :text}]
  (let [{:keys [text ballot-id exists]}
          (parse-bal-cmd text)]
    (if exists
      (let [{:keys [title desc type is-poll start hours sco_range] :as bal-info}
              (ballot-info ballot-id)
            options     (map :opt_id (bal-opts bal-info))
            text        (map #(or ({"Y" "1" "N" "0" "A" "abstain"} %) %) text)
            choices     (map edn/read-string (drop 2 text))
            is-abstain  (= (first choices) 'abstain)
            is-valid
              (if is-abstain
                true
                (and
                  (= (count options) (count choices))
                  (every? int? choices)
                  (every? #(< % sco_range) choices)))
            user-id     (db/contact->id username)
            test-vote   (vote/test-vote bal-info user-id)
            reason      (test-vote->reason test-vote)
            state       (v/ballot-state bal-info)
            link        (bal-link ballot-id title type)]
        (if is-valid
          (if (= test-vote :authed)
            (if is-abstain
              (do
                (db/vote-new! user-id ballot-id {} true)
                (send-md! id "Abstained on " link "!"))
              (let [choices (map hash-map options choices)
                    choices (reduce into choices)]
                (db/vote-new! user-id ballot-id choices false)
                (send-md! id "Vote cast on " link "!")))
            (send-tx! id "You are unable to vote on this " type ": " reason "."))
          (send-tx! id "Invalid options.")))
      (send-tx! id "Ballot not found."))))


(defn simple-item [type {:keys [ballot_id title]}]
  (str "[🔗 " ballot_id "](https://ocastio.uk/" type "/" ballot_id "): " title))

(defn cmd-mine [{{:keys [id]}       :chat
                 {:keys [username]} :from
                 text               :text}]
  (let [user-id (db/contact->id username)
        polls   (db/user-polls user-id)
        ballots (db/user-ballots user-id)
        are-pol (not-empty polls)
        are-bal (not-empty ballots)
        do-list #(str/join "\n" (map (partial simple-item %) %2))]
    (send-md! id
      (if are-pol (str "*Polls*\n" (do-list "poll" polls)))
      (if are-bal (str "\n*Ballots*\n" (do-list "ballot" ballots)))
      (if (not (or are-pol are-bal)) "None are available for you at this time."))))

(h/defhandler telegram-handler
  (h/command-fn "start" cmd-start)
  (h/command-fn "help"  cmd-help)
  (h/command-fn "register" cmd-register!)
  (h/command-fn "auth"  cmd-auth!)
  (h/command-fn "info"  cmd-info)
  (h/command-fn "vote"  cmd-vote!)
  (h/command-fn "mine"  cmd-mine))

(defn start [] (go (<! (p/start token telegram-handler))))
