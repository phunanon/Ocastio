(defproject ocastio "0.1.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "GNU General Public License v3.0"
            :url "https://www.gnu.org/licenses/gpl-3.0.html"}
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [compojure "1.6.1"]
                 [ring/ring-defaults "0.3.2"]
                 [javax.servlet/servlet-api "2.5"]
                 [ring/ring-mock "0.3.2"]
                 [hiccup "1.0.5"]
                 [org.clojure/java.jdbc "0.6.0"]
                 [com.h2database/h2 "1.4.193"]
                 [pandect "0.6.1"]
                 [bk/ring-gzip "0.3.0"]
                 [clj-time "0.15.0"]
  :plugins [[lein-ring "0.12.5"]]
  :ring {:handler ocastio.handler/app}
  :profiles {
    :dev {}
    :prod {:aot :all}})
;lein with-profile prod ring uberjar
