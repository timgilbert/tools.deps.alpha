;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clojure.tools.deps.alpha.repl
  (:require
    [clojure.java.io :as jio]
    [clojure.tools.deps.alpha :as deps]
    [clojure.tools.deps.alpha.util.maven :as mvn])
  (:import
    [clojure.lang DynamicClassLoader]
    [java.io File]))

(set! *warn-on-reflection* true)

(defn add-loader-url
  "Add url string or URL to the highest level DynamicClassLoader url set."
  [url]
  (let [u (if (string? url) (java.net.URL. url) url)
        loader (loop [loader (.getContextClassLoader (Thread/currentThread))]
                 (let [parent (.getParent loader)]
                   (if (instance? DynamicClassLoader parent)
                     (recur parent)
                     loader)))]
    (if (instance? DynamicClassLoader loader)
      (.addURL ^DynamicClassLoader loader u)
      (throw (IllegalAccessError. "Context classloader is not a DynamicClassLoader")))))

(defn add-lib
  ([lib coord]
    (add-lib lib coord {:mvn/repos mvn/standard-repos}))
  ([lib coord config]
   (let [dep-libs (deps/resolve-deps (merge config {:deps {lib coord}}) nil)
         paths (mapcat :paths (vals dep-libs))
         urls (->> paths (map jio/file) (map #(.toURL ^File %)))]
     (run! add-loader-url urls))))
