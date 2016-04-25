(ns om-weather.components
  (:require
   [om.dom :as dom]
   [cljs.test :as t :include-macros true]
   [om.next :as om :refer-macros [defui]])
  (:require-macros
   [devcards.core :as dc :refer [defcard deftest]]))

(defui Navigation
  Object
  (render [this]
    (let [query (:query (om/props this))
          {:keys [do-search]} (om/get-computed this)]
      (dom/header nil
        (dom/nav #js {:className "top-nav deep-orange darken-3"}
          (dom/div #js {:className "container"}
            (dom/div #js {:className "nav-wrapper"}
              (dom/div #js {:className "input-field"}
                (dom/i #js {:className "material-icons prefix"})
                (dom/input #js {:placeholder "Enter city to show weather"
                                :type "text"
                                :defaultValue query
                                :onKeyUp (fn [e] (when (= 13 (.. e -nativeEvent -keyCode))
                                                   (do-search (.. e -target -value))))})))))))))

(def navigation (om/factory Navigation))

(defui Chart
  static om/IQuery
  (query [this]
    [:title :image-url])
  Object
  (render [this]
    (let [{:keys [title image-url]} (om/props this)]
      (dom/div #js {:className "photo"}
        (dom/a #js {:href "" :className "thumbnail"}
          (dom/img #js {:src image-url})
          (dom/div #js {:className "caption"}
            (dom/h4 nil title)))))))

(def chart (om/factory Chart))

(defn kelvinToCelcius [kelvin]
  (.round js/Math (- kelvin 275.15)))


(defn weather-card [weather main]
  (dom/div #js {:className "card-panel z-depth-1"}
    (dom/div #js {:className "row valign-wrapper"}
      (dom/div #js {:className "col s2"}
        (dom/img #js {:src (str "http://openweathermap.org/img/w/" (get weather "icon") ".png")}))
      (dom/div #js {:className "col s10"}
        (dom/span #js {:className "black-text"}
          (str "Right now it's " (kelvinToCelcius (get main "temp")) " °C and " (get weather "description") "."
            "It's been at most " (kelvinToCelcius (get main "temp_max")) " °C today."))))))

(defcard uu
  "HEllo devcards")

(defcard t
  (fn [state _]
    (weather-card (:weather @state) (:main @state)))
  {:weather {"icon" "a" "description" "somewhat cloudy"}
   :main {"temp" 300 "temp_max" 350}})

(deftest math
  "Is 1 = 1?"
  (t/is (= 1 10)))

(defn weather-details [data]
  (let [list [["Wind" (str (get-in data ["wind" "speed"]) " m/s")]
              ["Pressure" (str (get-in data ["main" "pressure"]) " hpa")]
              ["Humidity" (str (get-in data ["main" "humidity"]) " %")]
              ["Maximum temp" (str (kelvinToCelcius (get-in data ["main" "max_temp"])) " m/s")]
              ["Min temp" (str (kelvinToCelcius (get-in data ["main" "min_temp"])) " m/s")]]]
    (dom/ul #js {:className "collection z-depth-1"}
      (for [item list]
        (dom/li #js {:key (.toLowerCase (first item))
                     :className "collection-item"}
          (dom/b nil (first item))
          (str " " (second item)))))))

(defui Weather
  Object
  (render [this]
    (let [data (om/props this)]
      (dom/div #js {:className "container"}
        (dom/h4 nil (str (get-in data ["city" "name"]) "," (get-in data ["city" "country"])))
        (dom/div #js {:className "row"}
          (dom/div #js {:className "col s12 m8 offset-m2 l5"}
            (weather-card (first (get data "weather")) (get data "main"))
            (dom/div #js {:className "hide-on-med-and-down2"}
              (weather-details data))))))))

(def weather (om/factory Weather))

(defui Root
  static om/IQueryParams
  (params [this]
    {:q ""})
  static om/IQuery
  (query [this]
    '[(:app/weather-data {:q ?q})])
  Object
  (render [this]
    (let [{:keys [app/weather-data]} (om/props this)]
      (dom/div nil
        (navigation (om/computed
                      {:query (:query (om/params this))}
                      {:do-search (fn [s] (om/set-query! this {:params {:q s}}))}))
        (weather weather-data)))))
