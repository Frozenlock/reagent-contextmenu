(ns reagent-contextmenu.menu
  (:require [reagent.core :as r]
            [goog.dom :as dom]
            [goog.events :as events])
  (:import [goog.events EventType]))

;;; Make sure to create the context-menu element somewhere in the dom.
;;; Recommended: at the start of the document.


(def default-menu-atom (r/atom {:actions [["Action" #(prn "hello")]]
                                :left 0
                                :top 0
                                :display nil}))


(defn show-context! [menu-atom actions x y]
  (swap! menu-atom assoc
         :actions actions
         :left (- x 10)  ;; we want the menu to appear slightly under the mouse
         :top (- y 10)
         :display "block"))

(defn hide-context! [menu-atom]
  (swap! menu-atom assoc :display nil))



;;;; container to be included into the document

(declare actions-to-components)

(defn submenu-component [name actions-coll hide-context!]
  (let [show? (r/atom nil)]
    (fn []
      [:li {:class "context-submenu"
            :on-mouse-leave #(reset! show? nil)}
       [:a {:style {:cursor "pointer"}
            :on-mouse-over #(reset! show? true)
            :on-click #(do (.stopPropagation %)
                           (swap! show? not))} 
        name]
       [:ul.dropdown-menu.context-menu
        {:style {:display (if @show? :block :none)}}
        (actions-to-components actions-coll hide-context!)]])))

(defn action-component [name action-fn hide-context!]
  [:li
   [:a {:on-click #(do (.stopPropagation %)
                       (hide-context!) 
                       (action-fn %))
        :style {:cursor :pointer}} name]])

(defn action-or-submenu [item hide-context!]
  (let [[name fn-or-sub] item
        submenu (when (coll? fn-or-sub) fn-or-sub)]
    (cond submenu [submenu-component name submenu hide-context!]
          fn-or-sub [action-component name fn-or-sub hide-context!]
          :else [:li {:class :disabled}
                 [:a name]])))


(defn actions-to-components [actions-coll hide-context!]
  (for [[id item] (map-indexed vector actions-coll)]
    (cond 
      (coll? item) ^{:key id} [action-or-submenu item hide-context!]
      (keyword? item)
      ^{:key id}[:li.divider]
      
      :else 
      ^{:key id}[:li.dropdown-header 
                 {:style {:cursor :default}}
                 item])))

(defn context-menu
  "The context menu component. Will use a default (and global) state
  ratom if none is provided."
  ([] (context-menu default-menu-atom))
  ([menu-atom]
   ;; remove the context menu if we click out of it or press `esc' (like the normal context menu)  
   (r/with-let [hide-context! #(hide-context! menu-atom)
                esc-handler! (fn [evt] (when (= (.-keyCode evt) 27) ;; `esc' key
                                         (hide-context!)))
                click-outside-handler! hide-context!
                _ (events/listen js/window EventType.KEYUP esc-handler!)
                _ (events/listen js/window EventType.CLICK click-outside-handler!)]
     (let [!m-atom @menu-atom
           display (get !m-atom :display)]
       [:ul.dropdown-menu.context-menu
        {;:id context-id 
         :role "menu"
         :style {:display (or display "none")
                 :left (:left !m-atom)
                 :top (:top !m-atom)
                 :position "fixed"}
         :on-context-menu #(.preventDefault %)}
        (when display
          (when-let [actions (:actions !m-atom)]           
            (actions-to-components actions hide-context!)))])
     (finally (events/unlisten js/window EventType.CLICK click-outside-handler!)
              (events/unlisten js/window EventType.KEYUP esc-handler!)))))



;;;;; Main function below

;; Use with a :on-context-menu to activate on right-click

(defn context!
  "Update the context menu with a collection of [name function] pairs.
  When function is nil, consider the button as 'disabled' and do not
  allow any click.  

  When passed a keyword instead of [name function], a divider is
  inserted.

  If a string is passed, convert it into a header.

  [\"Menu header\"
   [my-fn #(+ 1 2)]
   :divider
   [my-other-fn #(prn (str 1 2 3))]]"
  ([evt name-fn-coll] (context! evt default-menu-atom name-fn-coll))
  ([evt menu-atom name-fn-coll]
   (show-context! menu-atom name-fn-coll 
                  (- (.-pageX evt) ;; absolute position
                     (- (.-pageX evt) ;; scrolled
                        (.-clientX evt)))
                  (- (.-pageY evt) ;; absolute position
                     (- (.-pageY evt) ;; scrolled
                        (.-clientY evt))))
   (.preventDefault evt)))
