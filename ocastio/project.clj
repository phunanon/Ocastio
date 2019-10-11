(defproject ocastio "0.1.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "GNU General Public License v3.0"
            :url "https://www.gnu.org/licenses/gpl-3.0.html"}
  :min-lein-version "2.0.0"
  :dependencies [
    [org.clojure/clojure "1.10.0"]
    [compojure "1.6.1"]
    [ring/ring-defaults "0.3.2"]
    [javax.servlet/servlet-api "2.5"]
    [ring/ring-mock "0.3.2"]
    [hiccup "1.0.5"]
    [org.clojure/java.jdbc "0.6.0"]
    [bk/ring-gzip "0.3.0"]
    [ring/ring-ssl "0.3.0"]
    [com.h2database/h2 "1.4.193"]
    [org.clojure/core.cache "0.8.1"]
    [pandect "0.6.1"]
    [clj-time "0.15.0"]
    [morse "0.4.3" :exclusions [commons-io commons-codec]]
    [org.suskalo/discljord "0.2.4" :exclusions [org.clojure/core.async org.clojure/tools.analyzer.jvm]]
    ;https://github.com/ring-clojure/ring/issues/369#issuecomment-489028469
    [org.eclipse.jetty/jetty-util "9.4.17.v20190418"]
    [org.eclipse.jetty/jetty-http "9.4.17.v20190418"]
    [org.eclipse.jetty/jetty-server "9.4.17.v20190418"]]
  :plugins [
    [lein-ring "0.12.5"]]
  :ring {:handler ocastio.handler/app}
  :profiles {
    :dev {}
    :prod {
      :aot :all
      :clean-targets ^{:protect false} ["/tmp/target"]
      :target-path "/tmp/target"
      :ring {
        :port 80
        :ssl? true
        :ssl-port 443
        :keystore "keystore.jks"
        ;:key-password "abcd"}}})
;https://community.letsencrypt.org/t/how-to-get-certificates-into-java-keystore/25961/18
