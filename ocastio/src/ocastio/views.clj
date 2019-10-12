(ns ocastio.views
  (:require
    [ocastio.db :as db]
    [clojure.string :as str]
    [clj-time.core :as t]
    [clj-time.format :as f]
    [clj-time.coerce :as tc]
    [hiccup.page :as h]
    [ring.util.anti-forgery :as util])
  (:import (java.time Instant ZoneId LocalDateTime format.DateTimeFormatter)))  ;TODO phase-out in preference of clj-time

(defn state-to-key [state]
  {(keyword (str (name state) "?")) true})

(defn org-link [org-id name] [:a {:href (str "/org/" org-id)} name])

(defn nav-link [name link]
  [:span [:a {:href link} name]])

(defn li-link [text url & [li-el]]
  [:a {:href url} [(or li-el :li) text]])

(defn plu [word num]
  (str word (if (not= num 1) "s")))

(defn make-del-button [action type]
  [:form
    {:action action :method "POST"}
    (util/anti-forgery-field)
    [:input.del {:type "submit" :value (str "Delete " type)}]])

(def date-format (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm"))
(defn- inst->LocalDateTime [inst]
  (.toLocalDateTime (.atZone (Instant/ofEpochMilli (inst-ms inst)) (ZoneId/of"UTC"))))
(defn format-inst [inst]
  (.format date-format (inst->LocalDateTime inst)))

(defn ballot-time-str [{:keys [start]}]
  (str (f/unparse (f/formatter "yy-MM-dd HH:mm") (tc/from-sql-date start))))

(defn ballot-sec-now [{:keys [start]}]
  (let [times (map inst-ms [(t/now) start])
        times (map #(quot % 1000) times)]
    (apply - times)))

(def ^:const hour-secs (* 60 60))
(defn ballot-state [{:keys [hours] :as info}]
  "Returns :future :ongoing or :complete"
  (let [diff (ballot-sec-now info)]
    (if (neg? diff)
      :future
      (if (> diff (* hour-secs hours))
        :complete
        :ongoing))))

(defn ballot-sec-remain [{:keys [start hours]}]
  (let [start (tc/from-sql-date start)
        end   (t/plus start (t/hours hours))
        times (map inst-ms [end (t/now)])
        times (map #(quot % 1000) times)]
    (apply - times)))
(defn ballot-time-status [{:keys [start hours] :as bal-info}]
  (let [state (ballot-state bal-info)
        {:keys [complete? ongoing? future?]}
          (state-to-key state)
        s     ((if ongoing? ballot-sec-remain ballot-sec-now) bal-info)
        s     (Math/abs s)
        m     (/ s 60)
        h     (/ m 60)
        d     (quot h 24)]
    (str
      (if future? "in ")
      d "d " (Math/round (float (mod h 24))) "h "
      (Math/round (float (mod m 60))) "m"
      ({:future "" :ongoing " left" :complete " ago"} state))))

(defn make-header [{{email :email :as sess} :session uri :uri}]
  [:navbar
    [:div
      [:a {:href "/"} [:h1 "Ocastio"]]
      [:p (if email email "Online democracy")]]
    [:div
      (nav-link "Orgs" "/orgs")
      (nav-link "Constitutions" "/cons")
      ;(nav-link "Ballots" "/ballots")
      (if email
        (nav-link "Sign out" (str "/signout?ref=" uri))
        (nav-link "Account" (str "/signin?ref=" uri)))]
    (h/include-css "/css/page.css")
    (h/include-css "/css/head.css")
    [:a {:href "/"} [:img {:src "/img/logo.svg"}]]])

(defn compose-page [request title head & body]
  (h/html5
    [:head
      [:title (str title " | Ocastio")]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
      [:link {:href "https://fonts.googleapis.com/css?family=Montserrat&display=swap" :rel "stylesheet"}]
      [:link {:rel "shortcut icon" :type "image/png" :href "/img/favicon.png"}]
      (h/include-css "/css/house.css")
      head]
      [:body (make-header request) body]))


(defn make-method-info [{:keys [method_id num_win num_opt majority sco_range preresult]}]
  (let [{name :name is-num-win :num_win is-score :is_score}
          (db/method-info method_id)
        facts [(if is-num-win
                 (str num_win "/" num_opt)
                 (str ">" majority "%"))
               (if is-score (str "0-" sco_range))
               (if preresult "early results")]
        facts (filter some? facts)
        facts (str/join ", " facts)]
    [:span name " (" facts ")"]))

(defn make-ballot-link [{:keys [ballot_id title start hours
                                num_win num_opt method_id state] :as info}
                        type]
  "Accepts [ballot-info ballot-type]"
  (let [num-votes (db/ballot-num-votes ballot_id)
        {:keys [complete? ongoing? future?]}
          (state-to-key state)]
    (li-link
      [:span.ballot
        [:bl title] [:br]
        [:stat [:b num-votes] " " (plu "vote" num-votes)]
        [:stat
          {:title (ballot-time-str info)}
          [(if ongoing? :b :span)
           ((if complete? ballot-time-str ballot-time-status) info)]
           (if ongoing? " of " " for ") hours "h"]
        [:stat (make-method-info info)]]
      (str "/" type "/" ballot_id))))

(defn make-ballot-links [ballots type]
  (let [ballots (map #(assoc % :state (ballot-state %)) ballots)
        triage  (group-by :state ballots)
        triage  (map #(vector % (triage %)) [:ongoing :future :complete])
        triage  (filter #(seq (second %)) triage)]
  (map
    (fn [[state ballots]]
      [:ul.list
        [:h4 (str/capitalize (name state)) " (" (count ballots) ")"]
        (if (some? ballots)
          (map #(make-ballot-link % type) ballots))])
    triage)))

(defn make-law-link [{:keys [law_id title]}]
  [:li
    [:a {:href (str "/law/" law_id)}
    [:span title]]])

(defn make-method-option [{:keys [method_id name desc num_win is_score]}]
  [:option
    {:value method_id :data-num-win num_win :data-is-score is_score}
    (str name " - " desc)])

(defn make-option-text [{:keys [text law_id title]}]
  (if (nil? text)
    [:span [:a {:href (str "/law/" law_id)} "🔗"] " " title]
    text))

(defn valid-email? [email]
  (def pattern #"[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?")
  (and (string? email) (re-matches pattern email)))
