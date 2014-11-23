# reagent-contextmenu

Context menu components for Reagent.

Takes care of all the little details for creating a context menu.

(Compatible with Bootstrap; already uses `UL` and `LI` elements along with the `dropdown` class.)

<img src="https://raw.githubusercontent.com/Frozenlock/reagent-contextmenu/master/contextmenu-example.png"
 alt="Context menu demo" title="Context menu demo"/>

## Install
Add this to your project dependencies:

[![Clojars Project](http://clojars.org/org.clojars.frozenlock/reagent-contextmenu/latest-version.svg)](http://clojars.org/org.clojars.frozenlock/reagent-contextmenu)


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
If you replace the name-fn by a keyword, it will place a divider.

Note that the *name* can be any Reagent component.


```clj
:on-context-menu #(menu/context! % [[ [:span "my-fn"] (fn [] (+ 1 2))] ; <---- the name is a span
	                                :divider
	                                ["my-other-fn" (fn [] (prn (str 1 2 3)))]])
```

You can style your context menu with CSS and the class `context-menu`.
