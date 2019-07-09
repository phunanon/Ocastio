(ns ocastio.html.result
  (:require
    [ocastio.db      :as db]
    [ocastio.views   :as v]
    [clojure.string :as str]
    [hiccup.page :as page]))

(defn make-result-row [{:keys [text law_id sum approval]} won?]
  [(if won? :tr.won :tr)
    [:td (if text text (v/make-option-text (db/law-info law_id)))]
    [:td sum]
    [:td (format "%.1f" (* approval 100)) "%"]])

(defn render-results [results num-win]
  [:table
    [:th "Option"] [:th "Sum"] [:th "Approval"]
    (map
      #(make-result-row %
        (if num-win
          (< %2 num-win)
          (>= (:approval %) 0.5)))
      (sort-by :approval > results)
      (range))])

(def functions {
    ;num-win? func
  0 [nil   str]
  1 [false (partial db/vot-per-app 1.0)]
  2 [false (partial db/vot-per-app 4.0)]
  3 [true  (partial db/vot-per-app 1.0)]
  4 [true  (partial db/vot-per-app 4.0)]})

(defn html [ballot-id method-id num-win]
  (let [func     (functions method-id)
        num-win? (func 0)
        func     (func 1)]
    (render-results
      (func ballot-id)
      (if num-win? num-win))))
