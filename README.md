# reagent-contextmenu

Context menu components for Reagent.

Takes care of all the little details for creating a context menu.

(Compatible with Bootstrap; already uses `UL` and `LI` elements along with the `dropdown-menu` class.)

<img src="https://raw.githubusercontent.com/Frozenlock/reagent-contextmenu/master/contextmenu-example.png"
 alt="Context menu demo" title="Context menu demo"/>

## Install
Add this to your project dependencies:

[![Clojars Project](http://clojars.org/org.clojars.frozenlock/reagent-contextmenu/latest-version.svg)](http://clojars.org/org.clojars.frozenlock/reagent-contextmenu)

The Bootstrap CSS is recommended if you don't want to style the menu
yourself, along with `contextmenu.css` (especially for submenus).

## Usage

Include the `context-menu` component in the root of your document:

```clj
(defn main-page []
	[:div
	  [:h1 "Page title"]
	  [:span "Some content"]
	  [menu/context-menu]]) ;  <-------
```

Everytime you want to show a context-menu to the user, you just need to call `context!` by passing it the `:on-context-menu` event and the name-functions collection.

Name-functions pairs should be of the following form: [name fn].
If `fn` is nil, it the menu will mark this item with the `disabled` class.

If you replace the name-fn by a keyword, it will place a divider.

Finally, you if replace the name-fn by a string, it will be considered a
section heading.

Note that the *name* can be any Reagent component.


```clj
[:div {:on-context-menu
       (fn [evt]
         (menu/context! 
          evt
          ["Some title"             ; <---- string is a section title
           ["my-fn" #(prn "my-fn")]
           [[:span "my-other-fn"] #(prn "my-other-fn")] ; <---- the name is a span
           :divider                    ; <--- keyword is a divider
           [[:span 
             [:span.cmd "Copy"] 
             [:span.kbd.text-muted "ctrl-c"]] ; <--- some classes to show a keyboard shortcut
            #(prn "Copy")]
           ["Submenu" 
            ["Submenu title" ["Submenu item 1" #(prn "Item 1")]]]] ; <-- submenus are simply nested menus.
          ))}]
```

You can style your context menu with CSS and the class `context-menu`.
