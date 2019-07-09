(ns ocastio.views
  (:require
    [ocastio.db :as db]
    [clojure.string :as str]
    [hiccup.page :as page]
    [ring.util.anti-forgery :as util])
  (:import (java.time Instant ZoneId LocalDateTime format.DateTimeFormatter)))

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

(def ^:const hour-secs (* 60 60))
(defn ballot-state [start hours]
  "Returns :future :ongoing or :complete"
  (let [times [(System/currentTimeMillis) (inst-ms start)]
        times (map #(quot % 1000) times)
        diff  (apply - times)]
    (if (neg? diff)
      :future
      (if (> diff (* hour-secs hours))
        :complete
        :ongoing))))

(defn make-header [{{email :email :as sess} :session uri :uri}]
  [:navbar
    [:div
      [:a {:href "/"} [:h1 "Ocastio"]]
      [:p (if email email "Online democracy")]]
    [:div
      (nav-link "Organisations" "/orgs")
      ;(nav-link "Constitutions" "/cons")
      ;(nav-link "Ballots" "/ballots")
      (if email
        (nav-link "Sign out" (str "/signout?redir=" uri))
        (nav-link "Account" (str "/signin?redir=" uri)))]
    (page/include-css "/css/page.css")
    (page/include-css "/css/head.css")
    [:a {:href "/"} [:img {:src "/img/logo.svg"}]]])

(defn compose-page [request title head & body]
  (page/html5
    [:head
      [:title (str title " | Ocastio")]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
      [:link {:href "https://fonts.googleapis.com/css?family=Montserrat&display=swap" :rel "stylesheet"}]
      [:link {:rel "shortcut icon" :type "image/png" :href "/img/favicon.png"}]
      (page/include-css "/css/house.css")
      head]
      [:body (make-header request) body]))


(defn make-method-info [{:keys [method method_id num_win num_opt]}]
  [:span method
    (if (#{3 4 5} method_id) [:span " (" num_win "/" num_opt ")"])])

(defn make-ballot-link [{:keys [ballot_id title num_win num_opt method method_id state] :as info} type]
  "Accepts [ballot-info ballot-type]"
  (def num-votes (db/ballot-num-votes ballot_id))
  (li-link
    [:span
      [:bl title] [:br]
      [:b num-votes] " " (plu "vote" num-votes) ", " (make-method-info info)]
    (str "/" type "/" ballot_id)))

(defn make-ballot-links [ballots type]
  (let [get-state #(ballot-state (:start %) (:hours %))
        ballots   (map #(assoc % :state (get-state %)) ballots)
        triage    (group-by :state ballots)]
  (map
    (fn [[state ballots]]
      [:ul
        [:h4 (str/capitalize (name state))]
        (map #(make-ballot-link % type) ballots)])
    triage)))
  ;(map #(make-ballot-link % type) ballots))

(defn make-law-link [{:keys [law_id title]}]
  [:li
    [:a {:href (str "/law/" law_id)}
    [:span title]]])

(defn make-method-option [method]
  (let [id        (:method_id method)
        name      (:name method)
        desc      (:desc method)
        num-win?  (:num_win method)]
    [:option
      {:value id :data-num-win num-win?}
      (str name " - " desc)]))

(defn make-option-text [{:keys [text law_id title]}]
  (if (nil? text)
    [:span [:a {:href (str "/law/" law_id)} "🔗"] " " title]
    text))
