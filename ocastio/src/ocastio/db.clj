(ns ocastio.db
  (:require
    [clojure.java.jdbc :as jdbc]
    [pandect.algo.sha256 :refer [sha256-bytes]]))

(def db-spec {:dbtype "h2" :dbname "./ocastio-db"})

;replace all (first (vals (first
(def fvf (comp first vals first))

;TODO: use some
(defn- db-exists? [table where value]
  (not-empty (jdbc/query db-spec [(str "select * from " table " where " where " = ?") value])))

(defn- db-pick [table field where value]
  ((keyword field) (first (jdbc/query db-spec [(str "select " field " from " table " where " where " = ?") value]))))

(defn- db-truthy? [table field where value]
  (true? (db-pick table field where value)))
(defn- db-some? [table field where value]
  (some? (db-pick table field where value)))

(defn num-records [table] (fvf (jdbc/query db-spec [(str "SELECT COUNT(*) FROM " table)])))

(defn email-exists? [email]
  (db-exists? "user" "email" email))

(defn org-name-exists? [org-name]
  (db-exists? "org" "name" org-name))

(def email->id
  (memoize
    (fn [email]
      (first (vals (first (jdbc/query db-spec ["select user_id from user where email = ?" email])))))))

(defn hashpass [plain salt]
  (sha256-bytes (byte-array (concat (.getBytes plain "UTF-8") salt))))

(defn correct-pass? [email password]
  (let [record (jdbc/query db-spec ["select pass, salt from user where email = ?" email])
        record (first record)
        salt   (:salt record)
        pass   (:pass record)
        rehash (hashpass password salt)]
    (= (seq rehash) (seq pass))))

(defn new-user! [email password]
  (let [salt  (sha256-bytes (str (.getTime (java.util.Date.))))
        pass  (hashpass password salt)]
    (jdbc/insert! db-spec :user {:email email :pass pass :salt salt})))


(defn add-to-org! [org-id user-id is-admin]
  (jdbc/insert! db-spec :org2user {:org_id org-id :user_id user-id :is_admin is-admin}))

(defn org-new! [email name desc contact]
  (let [user-id (email->id email)
        org-id  (jdbc/insert! db-spec :org
          {:name name :desc desc :contact contact :img "https://i.imgur.com/gzNZGyi.png"})
        org-id  (first (vals (first org-id)))]
    (add-to-org! org-id user-id true)))

(defn orgs-stats []
  (jdbc/query db-spec ["SELECT org.org_id, org.name, org.img,
(SELECT COUNT(*) FROM org2user WHERE org2user.org_id = org.org_id) members,
(SELECT COUNT(*) FROM org2con WHERE org2con.org_id = org.org_id) cons
FROM org
ORDER BY members DESC"]))

(defn org-info [org-id]
  (first (jdbc/query db-spec ["SELECT org.name, desc, contact, count(org2user.user_id) members
FROM org2user
JOIN org ON org.org_id = org2user.org_id
WHERE org.org_id = ?" org-id])))

(defn org-basic-info [org-id]
  (first (jdbc/query db-spec ["SELECT org.name FROM org WHERE org.org_id = ?" org-id])))

(defn org-mems [org-id]
  (jdbc/query db-spec ["SELECT email, date, is_admin FROM user
JOIN org2user ON org2user.user_id = user.user_id
WHERE org_id = ?" org-id]))

;TODO: make db-true? with multiple WERE
(defn org-admin? [org-id email]
  (def user-id (email->id email))
  (not-empty (jdbc/query db-spec ["SELECT is_admin FROM org2user WHERE user_id = ? AND org_id = ? AND is_admin = true" user-id org-id])))

(defn user-in-org? [org-id user-id]
  (not-empty (jdbc/query db-spec ["SELECT user_id FROM org2user WHERE user_id = ? AND org_id = ?" user-id org-id])))


(defn orgs-by-con [con-id]
  (:org_id (first (jdbc/query db-spec ["select * from org2con where con_id = ?" con-id]))))

(defn add-con-org! [org-id con-id is-exec]
  (jdbc/insert! db-spec :org2con {:org_id org-id :con_id con-id :is_exec is-exec}))

;TODO remove do
(defn con-new! [org-id title desc]
  (let [result   (jdbc/insert! db-spec :con {:title title :desc desc})
        con-id (first (vals (first result)))]
    (do (add-con-org! org-id con-id true)
        con-id)))

(defn con-del! [con-id]
  (jdbc/delete! db-spec :org2con ["con_id = ?" con-id]) ;
  (jdbc/delete! db-spec :law     ["con_id = ?" con-id]) ;TODO fix cascade
  (jdbc/delete! db-spec :con     ["con_id = ?" con-id]))

(defn con-infos [org-id]
  (jdbc/query db-spec ["SELECT con.con_id, title, desc, is_exec
FROM con
JOIN org2con ON org2con.con_id = con.con_id
WHERE org_id = ?" org-id]))
;  (jdbc/query db-spec ["select con_id, title, desc from con where con_id in (select con_id from org2con where org_id = ?)" org-id]))

(defn con-info [con-id]
  (first (jdbc/query db-spec ["select con_id, title, desc from con where con_id = ?" con-id])))

(defn con-orgs-info [con-id]
  (jdbc/query db-spec ["SELECT org.org_id, name, is_exec
FROM org
JOIN org2con ON org2con.org_id = org.org_id
WHERE org2con.con_id = ?" con-id]))

; TODO cache
(defn con-num-mem [con-id]
  (fvf
    (jdbc/query db-spec ["SELECT COUNT(DISTINCT user.user_id)
FROM user
JOIN org2user ON user.user_id = org2user.user_id
JOIN org ON org.org_id = org2user.org_id
JOIN org2con ON org2con.org_id = org.org_id
WHERE org2con.con_id = ?" con-id])))

(defn con-exec? [con-id email]
  "True if user is an executive of this conitution (when one of their orgs are executive)"
  (if (some nil? [con-id email])
    false
    (not-empty
      (jdbc/query db-spec ["SELECT org2con.is_exec, org2user.is_admin
FROM org2con
JOIN org2user ON org2user.org_id = org2con.org_id
WHERE org2user.is_admin AND org2con.is_exec
    AND org2con.con_id = ? AND org2user.user_id = ?" con-id (email->id email)]))))


(defn con-laws [con-id]
  (jdbc/query db-spec ["SELECT law.law_id, title, body, parent_id FROM law WHERE con_id = ?" con-id]))

(defn law-new! [con-id parent-id email title body]
  (let [user-id   (email->id email)
        parent-id (if (= parent-id 0) nil parent-id)
        result    (jdbc/insert! db-spec :law {:con_id con-id :parent_id parent-id :user_id user-id :title title :body body})
        law-id    (first (vals (first result)))]
    law-id))

(defn law-basic-info [law-id]
  (first (jdbc/query db-spec ["SELECT title, body FROM law WHERE law_id = ?" law-id])))

(defn law-info [law-id]
  (first (jdbc/query db-spec ["SELECT law.law_id, law.title title, body, parent_id, law.con_id, con.title con_title
FROM law
JOIN con ON con.con_id = law.con_id
WHERE law.law_id = ?" law-id])))

(defn law-children [parent-id]
  (jdbc/query db-spec ["SELECT law_id, title FROM law WHERE parent_id = ?" parent-id]))


;TODO: memoize
(defn vote-methods [] (jdbc/query db-spec ["SELECT method_id, name, desc, num_win FROM method"]))

(defn ballot-new! [info]
  (fvf (jdbc/insert! db-spec :ballot info)))

(defn bal-del! [ballot-id]
  (jdbc/delete! db-spec :ballot ["ballot_id = ?" ballot-id]))

(defn bal-opt-new! [ballot-id law-id text]
  (jdbc/insert! db-spec :bal_opt {:ballot_id ballot-id :law_id law-id :text text}))

(defn ballot-basic-info [ballot-id]
  (first (jdbc/query db-spec ["SELECT * FROM ballot WHERE ballot_id = ?" ballot-id])))

;TODO: use as confirmation; memoize
(defn poll? [ballot-id] (db-some? "ballot" "org_id" "ballot_id" ballot-id))

(defn ballot-infos [org-id]
  (jdbc/query db-spec ["SELECT ballot.ballot_id, title, ballot.desc, method.name method, method.method_id, ballot.num_win, start, hours, COUNT(opt_id) num_opt
FROM ballot
JOIN bal_opt ON bal_opt.ballot_id = ballot.ballot_id
JOIN method ON method.method_id = ballot.method_id
WHERE org_id = ?
GROUP BY ballot.ballot_id
ORDER BY count(opt_id) DESC" org-id]))

(defn ballot-info [ballot-id]
  (first (jdbc/query db-spec ["SELECT ballot.ballot_id, org_id, title, ballot.desc, method.method_id, method.name method, ballot.num_win, start, hours, preresult, majority, range, COUNT(opt_id) num_opt
FROM ballot
JOIN method ON method.method_id = ballot.method_id
JOIN bal_opt ON bal_opt.ballot_id = ballot.ballot_id
WHERE ballot.ballot_id = ?" ballot-id])))

(defn bal-pol-options [ballot-id]
  (jdbc/query db-spec ["SELECT opt_id, text FROM bal_opt WHERE ballot_id = ?" ballot-id]))

(defn bal-law-options [ballot-id]
  (jdbc/query db-spec ["SELECT opt_id, law.law_id, law.title FROM bal_opt
JOIN law ON law.law_id = bal_opt.law_id
WHERE ballot_id = ?" ballot-id]))

(defn con-ballots [con-id]
  (jdbc/query db-spec ["SELECT ballot.ballot_id, ballot.title, method.method_id, method.name method, ballot.num_win, COUNT(opt_id) num_opt, start, hours
FROM ballot
JOIN method   ON method.method_id = ballot.method_id
JOIN bal_opt  ON bal_opt.ballot_id  = ballot.ballot_id
JOIN law      ON law.law_id         = bal_opt.law_id
WHERE law.con_id = ?
GROUP BY ballot.ballot_id" con-id]))

(defn bal->con [ballot-id]
  (first (vals (first (jdbc/query db-spec ["SELECT con.con_id FROM con
JOIN law      ON law.con_id   = con.con_id
JOIN bal_opt  ON bal_opt.law_id = law.law_id
WHERE ballot_id = ?" ballot-id])))))

(defn poll->org [ballot-id]
  (first (vals (first (jdbc/query db-spec ["SELECT org_id FROM ballot WHERE ballot_id = ?" ballot-id])))))

(defn can-ballot-vote? [ballot-id user-id]
  (if (some nil? [ballot-id user-id])
    false
    (not-empty (jdbc/query db-spec ["SELECT * FROM ballot
JOIN bal_opt ON bal_opt.ballot_id = ballot.ballot_id
JOIN law ON law.law_id = bal_opt.law_id
JOIN con ON con.con_id = law.con_id
JOIN org2con ON org2con.con_id = con.con_id
JOIN org2user ON org2con.org_id = org2user.org_id
WHERE org2con.is_exec AND org2user.user_id = ? AND ballot.ballot_id = ?
LIMIT 1" user-id ballot-id]))))

(defn can-poll-vote? [org-id user-id]
  (if (some nil? [org-id user-id])
    false
    (not-empty (jdbc/query db-spec ["SELECT * FROM ballot 
JOIN org2user ON ballot.org_id = org2user.org_id
WHERE ballot.org_id = ? AND org2user.user_id = ?" org-id user-id]))))

(defn vote-new! [user-id ballot-id choices]
  "Inserts choices {opt-num opt-val,} replacing existing user votes"
  ;Delete existing
  (jdbc/delete! db-spec :vote ["user_id = ? AND opt_id IN (SELECT opt_id FROM bal_opt WHERE ballot_id = ?)" user-id ballot-id])
  ;Conj 0-value all-options hash-map with user choices
  (let [all-opts  (bal-pol-options ballot-id)
        opt+0     (zipmap (map :opt_id all-opts) (repeat 0))
        choices   (conj opt+0 choices)]
    ;Insert new votes
    (doseq [choice choices]
      (jdbc/insert! db-spec :vote {:user_id user-id :opt_id (choice 0) :value (choice 1)}))))

(defn ballot-num-votes [ballot-id]
  (fvf (jdbc/query db-spec ["SELECT COUNT(*) FROM (SELECT COUNT(*) FROM vote
JOIN bal_opt ON bal_opt.opt_id = vote.opt_id
WHERE ballot_id = ?
GROUP BY user_id)" ballot-id])))

(defn num-votes []
  (fvf (jdbc/query db-spec ["SELECT COUNT(*) FROM (SELECT DISTINCT user_id, ballot_id FROM vote
JOIN bal_opt ON bal_opt.opt_id = vote.opt_id)"])))


(defn vot-per-app [max-score ballot-id]
  (jdbc/query db-spec [(str "
SELECT opt_id, text, law_id,
       IFNULL(sum, 0) sum, max * " max-score " max, IFNULL((sum / (max * " max-score ")), 0) approval
FROM (SELECT opt_id, text, law_id,
        (SELECT COUNT(*) max FROM vote
        WHERE vote.opt_id = bal_opt.opt_id) max,
        (SELECT SUM(vote.value) FROM vote
        WHERE vote.opt_id = bal_opt.opt_id) sum
    FROM bal_opt WHERE ballot_id = ?)") ballot-id]))

;TODO useless
(defn vot-high-app [ballot-id]
  (jdbc/query db-spec ["
SELECT vote.opt_id, bal_opt.text, bal_opt.law_id, SUM(vote.value) sum, SUM(vote.value)*1.0/
    NULLIF(SELECT SUM(vote.value)
    FROM vote
    JOIN bal_opt ON bal_opt.opt_id = vote.opt_id
    WHERE bal_opt.ballot_id = ?, 0) approval
FROM vote
JOIN bal_opt ON bal_opt.opt_id = vote.opt_id
WHERE ballot_id = ?
GROUP BY vote.opt_id" ballot-id ballot-id]))


;TODO remove
(comment
(defn add-law-to-db
  [& [con_id parent_id title desc :as args]]
  (let [results (jdbc/insert! db-spec :constitution (zipmap [:con_id :parent_id :title :desc] args))]
    (assert (= (count results) 1))
    (first (vals (first results)))))

(defn delete-law [{:keys [law_id]}]
    (jdbc/delete! db-spec :constitution ["law_id=?" law_id]))

(defn get-all-laws []
  (map #(assoc % :vote% (rand 100))
        (jdbc/query db-spec "select * from constitution")))

(defn get-const-laws [const_id]
  (map #(assoc % :vote% (rand 100))
        (jdbc/query db-spec ["select * from constitution where const_id = ?" const_id]))))