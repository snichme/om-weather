(ns om-weather.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [goog.dom :as gdom]
            [cljs.core.async :as async :refer [<! >! put! chan]]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [cognitect.transit :as t]
            [om-weather.components :as c])
    (:import [goog Uri]
           [goog.net XhrIo]))

(enable-console-print!)

(def r (t/reader :json))

(def weather-url "data/weather.json?q=")
(def forecast-url "data/forecast.json?q=")

(defmulti read om/dispatch)

(defmethod read :app/weather-data
  [{:keys [state] :as env} key _]
  (let [st @state]
    (let [v (get st key [])]
      (if v
        {:value v :remote true}
        {:value v}))))

(defn json [uri]
  (let [c (chan)]
    (.send XhrIo uri #(put! c (t/read r  (.getResponse (.-target %)))))
    c))

(defn search-loop [c]
  (go
    (loop [[query cb] (<! c)]
      (let [results (<! (json (str weather-url query)))]
        (cb {:app/weather-data results}))
      (recur (<! c)))))

(defn send-to-chan [c]
  (fn [{:keys [remote app/weather-data] :as edn} cb]
    (let [{[d] :children} (om/query->ast remote)
          query (:q (:params d))]
      (put! c [query cb]))))

(def my-chan (chan))
(def parser (om/parser {:read read}))

(def reconciler
  (om/reconciler
    {:state (atom {})
     :normalize true
     :merge-tree (fn [a b] (merge a b))
     :parser parser
     :remotes [:remote :app/weather-data]
     :send (send-to-chan my-chan)}))

(search-loop my-chan)

(om/add-root! reconciler c/Root (gdom/getElement "app"))
