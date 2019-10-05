(ns ocastio.pages.law
  (:require
    [ocastio.views :as v]
    [ocastio.db :as db]
    [ocastio.pages.vote :as vote]
    [clojure.string :as str]
    [hiccup.page :as page]
    [ring.util.anti-forgery :as util]))

(defn con-link [con-id]
  [:a {:href (str "/con/" con-id)}
      (:title (db/con-info con-id))])

(defn page [{{:keys [law-id]} :params
             {:keys [email]}  :session}
            compose]
  (let [law-id      (Integer. law-id)
        {:keys [title body con_id con_title parent_id]}
          (db/law-info law-id)
        body        (if (= body "") "No body." body)
        parent-name (:title (db/law-info parent_id))
        children    (db/law-children law-id)
        ballots     (db/law-ballots law-id nil)
        is-exec     (db/con-exec? con_id email)
        btn-new     #(vector :a {:href (str "/law/new/" con_id "/" %)} %2)]
  (compose title nil
    [:navinfo "Part of " [:a {:href (str "/con/" con_id)} con_title] "."
      (if parent_id [:span " A child of " [:a {:href (str "/law/" parent_id)} parent-name] "."])]
    [:h2 title [:grey " | Law"]]
    [:quote [:pre body]]
    [:h3 "Children"]
    (if is-exec
      [:p.admin
        (btn-new law-id "Compose a child law") " or "
        (btn-new (if parent_id parent_id "0") "a sibling law")])
    (if (empty? children) [:p "This law has no child laws."])
    [:ul (map v/make-law-link children)]
    [:h3 "Ballots"]
    [:ul (map #(v/li-link (:title %) (str "/ballot/" (:ballot_id %))) ballots)]
    (if is-exec
      (v/make-del-button (str "/law/del/" law-id) "law")))))

(defn page-new [{{:keys [uri parent-id con-id]} :params} compose]
  (let [parent-id   (Integer. parent-id)
        parent-name (:title (db/law-basic-info parent-id))]
    (compose "New law" nil
      [:p "Enter the details of the new law, for " (con-link con-id) "."]
      [:form {:action uri :method "POST"}
        (util/anti-forgery-field)
        [:p [:b "Parent: "]
          (if (= parent-id 0)
              "None (highest level of law)"
              [:a {:href (str "/law/" parent-id)} parent-name])]
        [:input        {:type "text" :name "title" :placeholder "Title"}]
        [:textarea.big {:type "text" :name "body"  :placeholder "Law body"}]
        [:input        {:type "submit"  :value "Compose"}]])))


; TODO check law doesn't already exist
(defn new! [{{:keys [con-id parent-id title body]} :params
             {:keys [email] :as sess}              :session}]
  (let [con-id    (Integer. con-id)
        parent-id (Integer. parent-id)
        law-id    (db/law-new! con-id parent-id email title body)]
  {:redir (str "/law/" law-id) :sess sess}))

(defn del! [{{:keys [law-id]} :params sess :session}]
  (let [{:keys [con_id]} (db/law-basic-info law-id)]
    (db/law-del! law-id)
    {:redir (str "/con/" con_id) :sess sess}))


(def default-import-text "#Parent Law
The body of the law.
##A Child Law
Write the body of each law after its title.
It can be multiline.
###A Child of that
Observe the hashes before each law title - it's one hash per level.
##Another Child
Check the preview before submitting.
#Another Parent Law
Remember, in the future you can create ballots for parent laws, and depending on if the child law wins or loses it activates or deactivates all child laws too.")

(defn page-imp [{{:keys [uri con-id]} :params} compose]
  (compose "Import new laws"
    (page/include-css "/css/constitution.min.css")
    (page/include-js "/js/con-preview.js")
    [:p "Use this tool to import law into "
      (con-link con-id) "."]
    [:p "Copy and paste into the editor below and format it like the example below."]
    [:form {:action uri :method "POST"}
      (util/anti-forgery-field)
      [:textarea.big {:name "import"} default-import-text]
      [:button {:type "button" :onclick "UpdatePreview()"} "Update preview"]
      [:br] [:br]
      [:tree]
      [:input {:type "submit" :value "Import"}]]))


(defn recur-imp!
  ([laws info]
    (recur-imp! laws info [0]))
  ([laws {:keys [email con-id] :as info} parents]
    (let [[{:keys [level title body] :as law}]
            laws
          laws      (rest laws)
          parents   (vec (take (inc level) parents))
          parent-id (last parents)
          law-id    (db/law-new! con-id parent-id email title body)
          parents   (conj parents law-id)]
      (if (seq laws)
        (recur laws info parents)))))

(defn import! [{{:keys [con-id import]}  :params
                {:keys [email] :as sess} :session}]
  (let [import (str "\n" import)
        laws   (str/split import #"\n#")
        laws   (filter seq laws)
        hashes (map #(-> (re-seq #"^#+" %) first) laws)
        levels (map count hashes)
        titles (map #(-> (re-seq #"^#*(.+)" %) first second) laws)
        bodies (map #(subs % (str/index-of % "\n")) laws)
        bodies (map str/trim bodies)
        laws   (map vector levels titles bodies)
        laws   (map #(zipmap [:level :title :body] %) laws)]
    (recur-imp! laws {:email email :con-id con-id}))
  {:redir (str "/con/" con-id) :sess sess})
