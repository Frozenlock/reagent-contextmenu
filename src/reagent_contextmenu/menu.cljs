(ns reagent-contextmenu.menu
  (:require [reagent.core :as reagent :refer [atom]]
            [goog.dom :as dom]
            [goog.events :as events])
  (:import [goog.events EventType]))

;;; Make sure to create the context-menu element somewhere in the dom.
;;; Recommended: at the start of the document.

(def context-id "reagent-contextmenu")

(def context-menu-atom (atom {:actions [["Action" #(prn "hello")]]
                              :left 0
                              :top 0
                              :display nil}))
;; init the context menu with some default action

(defn get-menu []
  (dom/getElement context-id))

(defn show-context! [actions x y]
  (swap! context-menu-atom assoc
         :actions actions
         :left (- x 10)  ;; we want the menu to appear slightly under the mouse
         :top (- y 10)
         :display "block"))

(defn hide-context! []
  (swap! context-menu-atom assoc :display nil))



;;;; container to be included into the document

(defn context-menu []
  
  ;; remove the context menu if we click out of it or press `esc' (like the normal context menu)
  (defonce click-out-or-esc ; <--- defonce so we can reload the code
  [(events/listen js/window EventType.CLICK hide-context!)
   (events/listen js/window EventType.KEYUP
                  #(when (= (.-keyCode %) 27) ;; `esc' key
                     (hide-context!)))])
  
  (let [!c-atom @context-menu-atom]
    [:ul.dropdown-menu.context-menu
     {:id context-id :role "menu"
      :style {:display (get !c-atom :display "none")
              :left (:left !c-atom)
              :top (:top !c-atom)}}
     (when-let [actions (:actions !c-atom)]
       (for [item actions]
         (if (coll? item)
           (let [[name func] item]
             ^{:key name}
             [:li [:a {:on-click #(do (hide-context!) (func %))
                       :style {:cursor "pointer"}} name]])
           ^{:key (str item)}[:li.divider])))]))



;;;;; Main function below

;; Use with a :on-context-menu to activate on right-click

(defn context!
  "Update the context menu with a collection of [name function] pairs.
   When passed a keyword instead of [name function], a divider is inserted.

  [[my-fn #(+ 1 2)]
   :divider
   [my-other-fn #(prn (str 1 2 3))]]"
  [evt name-fn-coll]
  (show-context! name-fn-coll (.-pageX evt) (.-pageY evt))
  (.preventDefault evt))
