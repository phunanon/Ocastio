(ns ocastio.pages.vote
  (:require
    [ocastio.views :as v]
    [ocastio.db :as db]
    [ocastio.html.result :as r]
    [clojure.string :as str]
    [clj-time.core :as t]
    [clj-time.format :as f]
    [ring.util.anti-forgery :as util])
  (:import (java.time Instant ZoneId LocalDateTime format.DateTimeFormatter))) ;TODO phase-out in preference of clj-time


(defn extract-int [s] (Integer. (re-find  #"\d+" s))) ;TODO commonise
(def select-values (comp vals select-keys))

(defn select-opts [para]
  "{:optNum 'opt-val',} -> {opt-num opt-val,}"
  (->> para
    (map #(vector (name (% 0)) (% 1)))
    (filter #(str/starts-with? (% 0) "opt"))
    (map #(vector (extract-int (% 0)) (% 1)))
    (into {})))


; TODO check are in org, org can vote, options are of ballot, values are sane
(defn vote! [request]
  (let [para      (:params request)
        sess      (:session request)
        ballot-id (:ballot_id para)
        user-id   (db/email->id (:email sess))
        method-id (db/ballot-basic-info ballot-id)
        poll?     (db/poll? ballot-id)
        type      (if poll? "poll" "ballot")]
    (db/vote-new! user-id ballot-id (select-opts para))
    {:redir (str "/" type "/" ballot-id) :sess sess}))

(defn make-check-input [{:keys [opt_id text] :as option}]
  (def opt (str "opt" opt_id))
  [:balopt
    [:input {:type "checkbox" :value 1 :name opt :id opt}]
    [:label {:for opt}]
    [:span (v/make-option-text option)]])

(defn make-score-radio [value]
  (def id (str \o opt value))
  [:span
    [:input {:type "radio" :value value :name opt :id id}]
    [:label {:for id} value]])
(defn make-score-input [score-range {:keys [opt_id text] :as option}]
  (def opt (str "opt" opt_id))
  [:balopt
    (map make-score-radio (range 0 score-range))
    [:span (v/make-option-text option)]])

(defn make-option-form [options ballot-id method-id range]
    [:form {:action (str "/vote/" ballot-id) :method "POST"}
      (util/anti-forgery-field)
      [:br]
      (let [check-in make-check-input
            score-in (partial make-score-input range)]
        (map
          (case method-id
            0 "n/a"
            1 check-in
            2 score-in
            3 check-in
            4 score-in)
          options))
      [:input {:type "submit" :value "Cast vote"}]])

(defn make-option-info [options]
  [:ol (map #(vector :li (v/make-option-text %)) options)])

;TODO: Ensure ballot-id is sane
(defn page [{{ballot-id :ballot_id} :params
             {:keys [email]}        :session
             :as request} compose poll?]
  (let [ballot-id (Integer. ballot-id)

        {:keys [title org_id method_id desc hours start num_win majority range preresult] :as info}
          (db/ballot-info ballot-id)

        org-name  (:name (db/org-basic-info org_id))
        options   (if poll? (db/bal-pol-options ballot-id) (db/bal-law-options ballot-id))
        num-votes (db/ballot-num-votes ballot-id)

        user-id   (db/email->id email)
        can-vote? (if poll?
          (db/can-poll-vote? org_id user-id)
          (db/can-ballot-vote? ballot-id user-id))

        con-id    (db/bal->con ballot-id)
        con-name  (:title (db/con-info con-id))

        admin?    (db/org-admin? org_id email)
        exec?     (db/con-exec? con-id email)
        state     (v/ballot-state start hours)
        results?  (or (= state :complete) preresult)

        Type      (if poll? "Poll" "Ballot")
        type      (str/lower-case Type)]
    (compose (str title " | " Type) nil
      (if poll?
        [:navinfo "Conducted by "  [:a {:href (str "/org/" org_id)} org-name] "."]
        [:navinfo "Conducted for " [:a {:href (str "/con/" con-id)} con-name] "."])
      [:h2 title [:grey " | " Type]]
      [:p (v/make-method-info info)]
      [:p "From " [:b (v/format-inst start)] " for " [:b hours "h"] "."]
      [:quote desc]

      (let [{:keys [complete? ongoing? future?]}
              {(keyword (str (name state) "?")) true}]
        [:div
          (if (not complete?)
            [:div
              (if (or can-vote? future?) [:h3 "Options"])
              (if (and future? can-vote?)
                [:div
                  [:p.admin "You will be eligible to vote."]
                  (make-option-info options)])
              (if (and future? (not can-vote?))
                (make-option-info options))
              (if (and ongoing? can-vote?)
                [:div
                  [:p.admin "You are eligible to vote."]
                  (make-option-form options ballot-id method_id range)])])
          (if (and (not future?) results?)
            [:div
              [:h3 "Results"]
              [:p num-votes " " (v/plu "vote" num-votes) " total."]
              (r/html ballot-id method_id num_win range majority)])])

      (if (if poll? admin? exec?)
        (v/make-del-button (str "/bal/del/" ballot-id) type)))))


(defn make-form-new [type Type id option-form]
  (let [date-now  (f/unparse (f/formatter "yyyy-MM-dd") (t/now))
        time-now  (f/unparse (f/formatter "HH:mm") (t/now))]
    [:form {:action (str "/" type "/new/" id) :method "POST"}
        (util/anti-forgery-field)
        [:input {:type "text" :name "title" :placeholder (str Type " title") :maxlength 64}]
        [:textarea {:name "desc" :placeholder "Description" :maxlength 256}]
        [:select {:name "method_id" :onchange "UpdateBallotDOM(this)"}
          (map v/make-method-option (db/vote-methods))]
        option-form
        [:p#num_win.inline
          [:span "Number of winning laws/options: "]
          [:input#num_win {:name "num_win" :type "number" :value 1 :min 1 :max 16}]]
        [:p#majority.inline
          [:span "Majority at "]
          [:input#majority {:name "majority" :type "number" :value 50 :min 1 :max 100}]
          [:span "%"]]
        [:p#range.inline
          [:span "Score range between 0 and "]
          [:input#range {:name "range" :type "number" :value 5 :min 2 :max 16}]]
        [:p "Dates and times are UTC."]
        [:p.inline "Starting at"
          [:input {:type "date" :name "date" :value date-now}]
          [:input {:type "time" :name "time" :value time-now}]]
        [:p "Duration (days: 0-9, hours: 1-23):"]
        [:p.inline
          [:input {:type "range" :name "days"  :id "day"  :value 0 :min 0 :max 9}]
          [:span#days "0 days"]]
        [:p.inline
          [:input {:type "range" :name "hours" :id "hour" :value 1 :min 1 :max 23}]
          [:span#hours "1 hours"]]
        [:p.inline
          [:input {:type "checkbox" :name "preresult" :id "preresult"}
          [:label {:for "preresult"}]
          [:span "Permanently show results"]]]
        [:input {:type "submit"   :value "Post ballot"}]]))


(defn process-new!-post
  [{:keys [title desc date time days hours method_id num_win range majority preresult]}
   {email :email}]
  "Browser [para sess] -> {:setting val}"
  { :title      title
    :desc       desc
    :user_id    (db/email->id email)
    :method_id  (Integer. method_id)
    :num_win    (Integer. num_win)
    :range      (Integer. range)
    :majority   (Integer. majority)
    :start      (LocalDateTime/parse (str date " " time) v/date-format)
    :hours      (+ (Integer. hours) (* (Integer. days) 24))
    :preresult  (boolean preresult)})

;TODO no crash on empty times, >1 options, TODO find more things to do
(defn new-poll! [{para :params sess :session :as request}]
  (let [info    (process-new!-post para sess)
        info    (assoc info :org_id (Integer. (:org_id para)))
        options (map second (select-opts para))
        poll-id (db/ballot-new! info)]
    (doseq [text options] (db/bal-opt-new! poll-id nil text))
    {:redir (str "/poll/" poll-id) :sess (:session request)}))

;TODO is exec, no crash on empty times, check ballot doesn't overlap, check laws are all from same con, perhaps remove /:con_id
(defn new-ballot! [{para :params sess :session :as request}]
  (let [info      (process-new!-post para sess)
        info      (assoc info :org_id nil)
        law-pairs (filter #(str/starts-with? (name (first %)) "law") (vec para))
        law-ids   (map #(extract-int (name (first %))) law-pairs)
        ballot-id (db/ballot-new! info)]
    (doseq [id law-ids] (db/bal-opt-new! ballot-id id nil))
    {:redir (str "/ballot/" ballot-id) :sess (:session request)}))


(defn del! [{{ballot-id :ballot-id}   :params
             {:keys [email] :as sess} :session}]
  (let [poll?   (db/poll? ballot-id)
        id      ((if poll? db/poll->org db/bal->con) ballot-id)
        authed? ((if poll? db/org-admin? db/con-exec?) id email)]
  (if authed? (db/bal-del! ballot-id))
  {:redir (str (if poll? "/org/" "/con/") id)
   :sess  sess}))
