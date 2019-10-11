(ns ocastio.external.discord
  (:require
    [ocastio.external.engine :as engine]
    [discljord.connections :as c]
    [discljord.messaging :as m]
    [clojure.core.async :as a]
    [clojure.string :as str]))

(def token (slurp "discord.tkn"))

(defn handler [text user-id send-tx!]
  (let [[command] (str/split text #" ")
        content   (subs text (count command))
        send-tx!  (fn [& txt] (send-tx! (apply str txt)))]
    ((case command
      "!help"     (partial engine/cmd-help "!")
      "!info"     engine/cmd-info
      "!register" engine/cmd-register!
      "!auth"     engine/cmd-auth!
      "!mine"     engine/cmd-mine
      "!vote"     engine/cmd-vote!
      #(do % %2 %3 %4))
      text send-tx! send-tx! (str "dc:" user-id))))

(defn discord-start []
  (let [event-ch (a/chan 100)
        connection-ch (c/connect-bot! token event-ch)
        message-ch (m/start-connection! token)
        msg! #(m/create-message! message-ch % :content %2)]
    (loop []
      (let [[event-type event-data] (a/<!! event-ch)]
        (when (and (= :message-create event-type)
                   (not (:bot (:author event-data))))

          (let [msg! (partial msg! (:channel-id event-data))]
            (handler
              (:content event-data)
              (:id (:author event-data))
              msg!)))

        (when (= :channel-pins-update event-type)
          (a/>!! connection-ch [:disconnect]))
        (when-not (= :disconnect event-type)
          (recur))))
    (c/disconnect-bot! connection-ch)
    (m/stop-connection! message-ch)))

(defn start [] (a/go (a/<! (discord-start))))
