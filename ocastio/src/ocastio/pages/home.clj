(ns ocastio.pages.home
  (:require [ocastio.views :as v]
            [ocastio.db :as db]
            [clojure.string :as str]))


(defn num-rec [table] (db/num-records (name table)))

(def stats [
  ["votes"          (db/num-votes)]
  ["ballots/polls"  (num-rec :ballot)]
  ["laws"           (num-rec :law)]
  ["users"          (num-rec :user)]
  ["organisations"  (num-rec :org)]
  ["constitutions"  (num-rec :con)]])

;TODO: cache
(defn gen-stats []
  (map #(vector :stat [:b (% 1)] " " (% 0)) stats))

(def early [["Eodus" "a micronation employing direct-democracy." "https://t.me/Eodusians"]])

(defn page [request compose]
  (compose "Home" nil
    [:div#stats (gen-stats)]
    [:h2 "Welcome"]
    [:p [:b "Ocastio enables organisations to post ballots and compose constitutions."]]
    [:p "Flexibility is at heart. Organisations can adopt constitutions, as executives or members, allowing seperate bodies fine-grain control over their legislation. You can use the same voting systems to post referenda/polls to organisational members."]
    [:p "Organisations implement and interpret the spirit & letter of their legislation, Ocastio offering the platform and management."]
    [:warning
      [:p "This website is in initial development. Everything is liable to break or be incomplete; sessions may terminate unexpectedly; data may be lost (including your account); the website may go offline for indefinite periods."]
      [:p "Currently supported voting systems:"]
      [:ul
        [:li "Majority Approvals - individual options approved by majority"]
        [:li "Score Approvals - individual options approved by score"]
        [:li "Most Approvals - N options approved by majority"]
        [:li "Highest Approvals - N options approved by score"]]]
    [:h3 "Sponsors"]
    [:p "Early adopters of Ocastio are being sponsored for one year."]
    [:ul (map #(v/li-link [:span [:bl (% 0)] ", " [:span (% 1)]] (% 2)) early)]
    [:h3 "The Platform"]
    [:ul
      [:li "People can open User accounts with an email and password."]
      [:li "Users can create Organisations, as admin, and add other admins or members."]
      [:li "Organisation admins can..."]
      [:ol
        [:li "... compose Constitutions, as executives, and add other executive Organisations;"]
        [:li "... adopt other Organisations' Constitutions, themselves as members;"]
        [:li "... post polls, with multiple options."]]
      [:li "Constitutions are a collection of Laws, which can have 'parent' Laws."]
      [:li "Constitution executives can post ballots, which approve of Laws."]]
    [:p "This enables, for example, your Organisation to adopt the pre-written Universal Declaration of Human Rights 'Constitution'. Or, for multi-nation organisations to compose Constitutions for all members - with a body of executives. 'Organisations' don't have to be micronations - you can create specific Organisations for your different government and civil organs."]
    [:p"As Ocastio leaves the legal arbitration to you, Constitutions provide a unique feature: marking child Laws as inactive if the parental Laws have been disapproved through ballot. This allows you to have an automatic and flexible legal history."]
    [:h3 "Technical Info"]
    [:p "Ocastio is currently hosted on a DigitalOcean 2.2GHz 1GB Droplet. The application runs on the JVM (Java Virtual Machine), and is written in Clojure - a modern functional-paradigm programming language. The website is open source and can be found on " [:a {:href "https://github.com/phunanon/Ocastio"} "Github"] "."]
    [:h4 "About the author"]
    [:p "I'm a British professional C# software engineer and undergraduate Computing Science BSc student. I have operated two websites in the past, both written in PHP. A micronational website at " [:a {:href "http://archive.is/LyQGZ"} "eodus.org"] " (defunct since 2016), which pioneered " [:a {:href "https://web.archive.org/web/20160722090243/https://eodus.org/TRS"} "online direct democracy"] " with over 500 votes cast. And an online voting platform (" [:a {:href "https://phunanon.github.io/portfolio/img/Acastio_low.webm"} "demo video"] ") which was a prototype to Ocastio."]))
