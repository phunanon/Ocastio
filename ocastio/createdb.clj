(require '[clojure.java.jdbc :as jdbc])

(def db-spec {:dbtype "h2" :dbname "./ocastio-db"})
(defn db-do [funcs]
  (jdbc/db-do-commands db-spec funcs))

(def pk "bigint primary key auto_increment")
(def cascade "ON DELETE CASCADE")
(def date-field [:date :timestamp "NOT NULL" "DEFAULT CURRENT_TIMESTAMP"])
(def org-fk     ["FOREIGN KEY(org_id)     REFERENCES org(org_id)"       cascade])
(def ballot-fk  ["FOREIGN KEY(ballot_id)  REFERENCES ballot(ballot_id)" cascade])
(def user-fk    ["FOREIGN KEY(user_id)    REFERENCES user(user_id)"     cascade])
(def law-fk     ["FOREIGN KEY(law_id)     REFERENCES law(law_id)"       cascade])
(def parent-fk  ["FOREIGN KEY(parent_id)  REFERENCES law(law_id)"       cascade])
(def con-fk     ["FOREIGN KEY(con_id)     REFERENCES con(con_id)"       cascade])
(def opt-fk     ["FOREIGN KEY(opt_id)     REFERENCES bal_opt(opt_id)"   cascade])
(def vote-fk    ["FOREIGN KEY(vot_id)     REFERENCES vote(vot_id)"      cascade])
(def method-fk  ["FOREIGN KEY(method_id)  REFERENCES method(method_id)"])

(db-do [
  (jdbc/create-table-ddl :org
    [[:org_id     pk]
     date-field
     [:name       "varchar(128)"]
     [:desc       "varchar(256)"]
     [:contact    "varchar(48)"]    ;TODO
     [:img        "varchar(128)"]]) ;TODO
  (jdbc/create-table-ddl :user
    [[:user_id    pk]
     date-field
     [:email      "varchar(48)"]
     [:pass       "binary(32)"]
     [:salt       "binary(32)"]
     [:pub_con    "varchar(48)"]])
  (jdbc/create-table-ddl :con
    [[:con_id     pk]
     date-field
     [:title      "varchar(128)"]
     [:desc       "varchar(256)"]
     [:pub_con    "varchar(48)"]])
  (jdbc/create-table-ddl :law
    [[:law_id     pk]
     date-field
     [:con_id     "bigint"]
     [:parent_id  "bigint"]
     [:user_id    "bigint"]
     [:title      "varchar(128)"]
     [:body       "varchar(1024)"]
     con-fk parent-fk user-fk])
  (jdbc/create-table-ddl :ballot
    [[:ballot_id  pk]
     date-field
     [:title      "varchar(64)"]
     [:org_id     "bigint"]
     [:user_id    "bigint"]
     [:method_id  "bigint"]
     [:desc       "varchar(256)"]
     [:num_win    "tinyint"]
     [:start      "timestamp"]
     [:hours      "smallint"]
     [:preresult  "boolean"] ;show early results
     [:majority   "tinyint"] ;winning majority percentage
     [:range      "tinyint"] ;score range, 0-range
     org-fk user-fk method-fk])
  (jdbc/create-table-ddl :bal_opt
    [[:opt_id pk]
     [:ballot_id  "bigint"]
     [:law_id     "bigint"]
     [:text       "varchar(128)"]
     ballot-fk law-fk])
  (jdbc/create-table-ddl :vote
    [[:vot_id pk]
     date-field
     [:user_id    "bigint"]
     [:opt_id     "bigint"]
     [:value      "tinyint"]
     opt-fk user-fk])
  (jdbc/create-table-ddl :org2user
    [[:n2u_id     pk]
     [:org_id     "bigint"]
     [:user_id    "bigint"]
     [:is_admin   "boolean"]
     org-fk user-fk])
  (jdbc/create-table-ddl :org2con
    [[:n2c_id     pk]
     [:org_id     "bigint"]
     [:con_id     "bigint"]
     [:is_exec    "boolean"]
     org-fk con-fk])
  (jdbc/create-table-ddl :method
    [[:method_id  pk]
     [:name       "varchar(48)"]
     [:desc       "varchar(64)"]
     [:num_win    "boolean"]
     [:is_score   "boolean"]])
])

;(jdbc/with-db-connection [conn {:dbtype "h2" :dbname "./ocastio"}]
;  (jdbc/db-do-commands conn (jdbc/drop-table-ddl :constitution)))

(jdbc/insert! db-spec :method {:name "Majority Approvals" :desc "individual options approved by majority" :num_win false})
(jdbc/insert! db-spec :method {:name "Score Approvals" :desc "individual options approved by score" :num_win false})
(jdbc/insert! db-spec :method {:name "Most Approvals" :desc "N options approved by majority" :num_win true})
(jdbc/insert! db-spec :method {:name "Highest Approvals" :desc "N options approved by score" :num_win true})
(jdbc/insert! db-spec :method {:name "Single-Transferable" :desc "N options approved by transferred totals" :num_win true})
(jdbc/insert! db-spec :method {:name "Mass Majority Approval" :desc "all options approved by majority" :num_win false})
(jdbc/insert! db-spec :method {:name "Mass Score Approval" :desc "all options approved by score" :num_win false})

;ALTER TABLE ballot
;ADD name varchar(255);

;SELECT DISTINCT constraint_name FROM information_schema.constraints 
;WHERE table_name='LAW' AND column_list='CON_ID'
;ALTER TABLE law
;DROP CONSTRAINT CONSTRAINT_125 -- or whatever it's called

;ALTER TABLE law
;   ADD CONSTRAINT CONSTRAINT_125
;   FOREIGN KEY(con_id) REFERENCES con(con_id) ON DELETE CASCADE

;https://h2database.com/html/datatypes.html
;http://clojure-doc.org/articles/ecosystem/java_jdbc/home.html

;https://www.roseindia.net/tutorial/java/core/AlterTableExample.html
