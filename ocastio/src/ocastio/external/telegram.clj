(ns ocastio.external.telegram
  (:require
    [ocastio.external.engine :as engine]
    [morse.api           :as t]
    [morse.handlers      :as h]
    [morse.polling       :as p]
    [clojure.core.async :refer [go <!]]))

(def token (slurp "telegram.tkn"))

(defn send-tx! [id & body]
  (t/send-text token id (apply str body)))
(defn send-md! [id & body]
  (t/send-text token id {:parse_mode "Markdown"} (apply str body)))

(defn cmd-start [{{:keys [id]} :chat}]
  (send-tx! id "Type /help for options."))

(defn cmd! [command!
            {{:keys [id]}  :chat
             {user-id :id} :from
             text          :text}]
  (command!
    text
    (partial send-tx! id)
    (partial send-md! id)
    (str "tg:" user-id)))

(h/defhandler telegram-handler
  (h/command-fn "start"    cmd-start)
  (h/command-fn "help"     (partial cmd! (partial engine/cmd-help "/")))
  (h/command-fn "register" (partial cmd! engine/cmd-register!))
  (h/command-fn "auth"     (partial cmd! engine/cmd-auth!))
  (h/command-fn "info"     (partial cmd! engine/cmd-info))
  (h/command-fn "vote"     (partial cmd! engine/cmd-vote!))
  (h/command-fn "mine"     (partial cmd! engine/cmd-mine)))

(defn start [] (go (<! (p/start token telegram-handler))))
