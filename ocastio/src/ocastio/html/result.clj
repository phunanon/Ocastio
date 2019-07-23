(ns ocastio.html.result
  (:require
    [ocastio.db      :as db]
    [ocastio.views   :as v]
    [clojure.string :as str]))

(defn ballot-results [ballot-id]
  (let [{:keys [title org_id method_id desc hours start
                num_win majority sco_range] :as info}
          (db/ballot-info ballot-id)
        {is-num-win :num_win is-score :is_score}
          (db/method-info method_id)
        majority    (/ majority 100)
        sco_range   (if is-score (float (dec sco_range)) 1.0)
        results     (db/vot-per-app ballot-id sco_range)
        won?        #(if is-num-win (< %2 num_win) (> (:approval %) majority))
        assoc-won   #(assoc % :won? (won? % %2))
        results     (map assoc-won (sort-by :approval > results) (range))]
    results))

(defn law-result [law-id]
  (def ballot-id   (db/law-latest-ballot law-id))
  (if ballot-id
    (let [ballot-id   (db/law-latest-ballot law-id)
          bal-results (ballot-results ballot-id)
          law-result  (first (filter #(= (:law_id %) law-id) bal-results))
          law-result  (assoc law-result :bal-id ballot-id)]
      law-result)
    nil))

(defn make-result-row [{:keys [text law_id sum approval won?]}]
  [(if won? :tr.won :tr)
    [:td (if text text (v/make-option-text (db/law-info law_id)))]
    [:td sum]
    [:td (format "%.1f" (* approval 100)) "%"]])

(defn render-results [results]
  [:table
    [:th "Option"] [:th "Sum"] [:th "Approval"]
    (map make-result-row results)])

(def ballot-results-html (comp render-results ballot-results))
