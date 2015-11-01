(ns loop-recall.theme
  (:require
   cljsjs.marked
   cljsjs.highlight
   cljsjs.highlight.langs.ruby
   cljsjs.highlight.langs.javascript
   cljsjs.highlight.langs.1c
   cljsjs.highlight.langs.actionscript
   cljsjs.highlight.langs.bash
   cljsjs.highlight.langs.clojure-repl
   cljsjs.highlight.langs.clojure
   cljsjs.highlight.langs.coffeescript
   cljsjs.highlight.langs.cpp
   cljsjs.highlight.langs.css
   cljsjs.highlight.langs.django
   cljsjs.highlight.langs.elixir
   cljsjs.highlight.langs.erlang-repl
   cljsjs.highlight.langs.erlang
   cljsjs.highlight.langs.handlebars
   cljsjs.highlight.langs.haskell
   cljsjs.highlight.langs.java
   cljsjs.highlight.langs.javascript
   cljsjs.highlight.langs.json
   cljsjs.highlight.langs.lasso
   cljsjs.highlight.langs.less
   cljsjs.highlight.langs.lisp
   cljsjs.highlight.langs.lua
   cljsjs.highlight.langs.markdown
   cljsjs.highlight.langs.objectivec
   cljsjs.highlight.langs.perl
   cljsjs.highlight.langs.php
   cljsjs.highlight.langs.python
   cljsjs.highlight.langs.ruby
   cljsjs.highlight.langs.scala
   cljsjs.highlight.langs.scheme
   cljsjs.highlight.langs.scss
   cljsjs.highlight.langs.sql
   cljsjs.highlight.langs.swift
   cljsjs.highlight.langs.typescript
   cljsjs.highlight.langs.vim))

(let [colors js/window.MaterialUI.Styles.Colors
      theme-manager  js/window.MaterialUI.Styles.ThemeManager
      app-theme #js {:spacing js/window.MaterialUI.Styles.Spacing
                     :fontFamily "Roboto, sans-serif"
                     :palette #js {:primary1Color (.-teal900 colors)
                                   :primary2Color (.-teal400 colors)
                                   :primary3Color (.-teal900 colors)

                                   :accent1Color "#2C3E50";(.-teal800 colors)
                                   :accent2Color "#40C4C0";(.-teal700 colors)
                                   :accent3Color "#18252D";(.-teal900 colors)

                                   :textColor (.-darkBlack colors)
                                   :canvasColor (.-white colors)
                                   :borderColor (.-grey300 colors)
                                   :disabledColor (.-grey300 colors)}}]
  (def color-theme
    {:class-properties {:childContextTypes {:muiTheme js/React.PropTypes.object}}
     :child-context    (fn [_]
                         {:muiTheme (.getMuiTheme theme-manager app-theme)})}))

(.setOptions js/marked
             #js {:highlight (fn [code]
                               (.-value (.highlightAuto js/hljs code)))})

(defn markdown->html [text]
  (js/marked text))
