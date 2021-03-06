(ns ocastio.html.result
  (:require
    [ocastio.db      :as db]
    [ocastio.views   :as v]
    [clojure.string :as str]
    [clojure.core.cache.wrapped :as cw]))

(defonce law-result-cache (cw/lru-cache-factory {} :threshold 1024))

(defn ballot-results [ballot-id]
  (let [{:keys [title org_id method_id desc hours start
                num_win majority sco_range] :as info}
          (db/ballot-info ballot-id)
        {is-num-win :num_win is-score :is_score}
          (db/method-info method_id)
        majority    (/ majority 100)
        sco_range   (if is-score (float sco_range) 1.0)
        results     (db/vot-per-app ballot-id sco_range)
        won?        #(if is-num-win (< %2 num_win) (> (:approval %) majority))
        assoc-won   #(assoc % :won? (won? % %2))
        results     (map assoc-won (sort-by :approval > results) (range))]
    results))

(defn calc-law-result [[law-id ballot-id]]
  (if ballot-id
    (let [bal-results (ballot-results ballot-id)
          law-result  (first (filter #(= (:law_id %) law-id) bal-results))
          law-result  (assoc law-result :bal-id ballot-id)]
      law-result)
    nil))

(defn law-result [law-id]
  (let [ballot-id (db/law-latest-ballot law-id)
        cache-key [law-id ballot-id]]
    (if ballot-id
      (cw/lookup-or-miss law-result-cache cache-key calc-law-result)
      nil)))

(defn gradient% [per]
  {:style (str "background: linear-gradient(to right, #afa 0%, #afa "
               per "%, white " per "%, white 100%);")})

(defn make-result-row [n {:keys [text law_id sum approval won?]}]
  (let [per (* approval 100)]
    [:tr
      [:td (inc n)]
      [:td (if won? "✅" "")]
      [:td (or text (v/make-option-text (db/law-info law_id)))]
      [:td sum]
      [:td (gradient% per) (format "%.1f" per) "%"]]))

(defn results-html [results]
  [:table
    [:th "#"] [:th] [:th "Option"] [:th "Sum"] [:th "Approval"]
    (map make-result-row (range) results)])

(def ballot-results-html (comp results-html ballot-results))
