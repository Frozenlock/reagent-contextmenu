(ns reagent-contextmenu.menu
  (:require [reagent.core :as r]
            [goog.dom :as dom]
            [goog.events :as events])
  (:import [goog.events EventType]))

;;; Make sure to create the context-menu element somewhere in the dom.
;;; Recommended: at the start of the document.



(defonce default-menu-atom (r/atom {:actions [["Action" #(prn "hello")]]
                                    :left 0
                                    :top 0
                                    :display nil}))


(defn- show-context! [menu-atom actions x y]
  (swap! menu-atom assoc
         :actions actions
         :left (- x 10)  ;; we want the menu to appear slightly under the mouse
         :top (- y 10)
         :display "block"))

(defn- hide-context! [menu-atom]
  (swap! menu-atom assoc :display nil))


;;;; container to be included into the document

(declare actions-to-components)

(defn- reposition!
  "Make sure the dom-node is within the viewport. Update the
  `offsets-a' with the necessary :top and :left."
  [offsets-a dom-node]
  (let [{:keys [top left]} @offsets-a
        bcr (.getBoundingClientRect dom-node)
        x (- (.-right bcr) js/window.innerWidth)
        y (- (.-bottom bcr) js/window.innerHeight)
        [new-left new-top] (map - [left top] [(if (pos? x) x 0)
                                              (if (pos? y) y 0)])]
    (swap! offsets-a assoc :left new-left :top new-top)))


(defn- inner-submenu [actions-coll s-menus-a hide-context!]
  (let [dom-node (atom nil)
        offsets (r/atom {:top 0 :left 0})]
    (r/create-class
     {:component-did-mount #(reposition! offsets @dom-node)
      :reagent-render
      (fn []
        (let [{:keys [top left]} @offsets]
          [:ul.dropdown-menu.context-menu
           {:style {:display :block
                    :margin-top top
                    :margin-left left}          
            :ref (fn [this] (reset! dom-node this))}
           (actions-to-components actions-coll s-menus-a hide-context!)]))})))

(defn- submenu-component [showing-submenus-atom id name actions-coll hide-context!]
  (let [show? (r/cursor showing-submenus-atom [id])
        s-menus-a (r/cursor showing-submenus-atom [:sub id])]
    (r/create-class
     {:component-did-mount (fn [])
      :reagent-render
      (fn []
        [:li {:class "context-submenu"}
         [:a {:style {:cursor "pointer"}
              :class (when @show? "selected")
              :on-mouse-over #(reset! showing-submenus-atom {id true})
              :on-click #(do (.stopPropagation %)
                             (swap! show? not))}
          name]
         (if @show?
           [inner-submenu actions-coll s-menus-a hide-context!])])})))

(defn- action-component [name action-fn hide-context!]
  [:a {:on-click #(do (.stopPropagation %)
                      (hide-context!) 
                      (action-fn %))
       :style {:cursor :pointer}} name])

(defn- action-or-submenu [[id item] showing-submenus-atom hide-context!]
  (let [[name fn-or-sub] item
        submenu (when (coll? fn-or-sub) fn-or-sub)
        clear-sub-menus! #(reset! showing-submenus-atom nil)]
    (cond submenu [submenu-component showing-submenus-atom id name submenu hide-context!]
          fn-or-sub [:li {:on-mouse-enter clear-sub-menus!}
                     [action-component name fn-or-sub hide-context!]]
          :else [:li {:class :disabled
                      :on-mouse-enter clear-sub-menus!}
                 [:a name]])))


(defn- actions-to-components [actions-coll showing-submenus-atom hide-context!]
  (for [[id item] (map-indexed vector actions-coll)]
    (let [clear-sub-menus! #(reset! showing-submenus-atom nil)]
      (cond 
        (coll? item) ^{:key id} [action-or-submenu [id item] showing-submenus-atom hide-context!]
        (keyword? item)
        ^{:key id}[:li.divider {:on-mouse-enter clear-sub-menus!}]
        
        :else 
        ^{:key id}[:li.dropdown-header 
                   {:style {:cursor :default}
                    :on-mouse-enter clear-sub-menus!}
                   item]))))


(defn- inner-context-menu
  [menu-atom hide-context!]
  (let [dom-node (atom nil)
        showing-submenus-atom (r/atom {})]
    (r/create-class
     {:component-did-mount #(reposition! menu-atom @dom-node)
      :reagent-render
      (fn []
        (let [{:keys [display actions left top]} @menu-atom
              scroll! (fn [evt]
                        (let [dy (.-deltaY evt)]
                          (swap! menu-atom update-in [:top] #(- % dy))))]
          [:ul.dropdown-menu.context-menu
           {:ref (fn [this]
                   (reset! dom-node this))
            :tab-index -1
            :role "menu"
            :on-wheel scroll!
            :style {:display (or display "none")
                    :left left
                    :top top}}
           (when display
             (when actions
               (actions-to-components actions showing-submenus-atom hide-context!)))]))})))


(defn- backdrop [hide-context!]
  [:div.context-menu-backdrop
   {:style {:position :fixed
            :width "100vw"
            :height "100vh"
            :top 0
            :left 0}
    :on-click hide-context!}])


;; main component for the user


(defn context-menu
  "The context menu component. Will use a default (and global) state
  ratom if none is provided."
  ([] (context-menu default-menu-atom))
  ([menu-atom]
   ;; remove the context menu if we click out of it or press `esc' (like the normal context menu)  
   (let [hide-context! #(hide-context! menu-atom)
         esc-handler! (fn [evt] (when (= (.-keyCode evt) 27) ;; `esc' key
                                  (.stopPropagation evt)
                                  (hide-context!)))
         display (get @menu-atom :display)]
     [:div {:on-context-menu (fn [e]
                               (hide-context!)
                               (.preventDefault e))
            :on-key-up esc-handler!
            :tab-index -1
            :ref #(some-> % (.focus))}
      (when display [backdrop hide-context!])
      (when display
        [inner-context-menu menu-atom hide-context!])])))



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
