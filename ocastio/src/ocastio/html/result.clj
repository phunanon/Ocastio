(ns ocastio.html.result
  (:require
    [ocastio.db      :as db]
    [ocastio.views   :as v]
    [clojure.string :as str]))

(defn make-result-row [{:keys [text law_id sum approval]} won?]
  [(if won? :tr.won :tr)
    [:td (if text text (v/make-option-text (db/law-info law_id)))]
    [:td sum]
    [:td (format "%.1f" (* approval 100)) "%"]])

(defn render-results [results num-win majority]
  [:table
    [:th "Option"] [:th "Sum"] [:th "Approval"]
    (map
      #(make-result-row %
        (if num-win
          (< %2 num-win)
          (>= (:approval %) majority)))
      (sort-by :approval > results)
      (range))])

(defn html [ballot-id method-id num-win score-range majority]
  (let [info        (db/method-info method-id)
        is-num-win  (:num_win info)
        is-score    (:is_score info)
        majority    (/ majority 100)]
    (render-results
      (db/vot-per-app
        ballot-id
        (if is-score
          (float (dec score-range))
          1.0))
      (if is-num-win num-win)
      majority)))
