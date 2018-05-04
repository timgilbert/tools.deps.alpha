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
    [clojure.set :as set]
    [clojure.tools.deps.alpha :as deps]
    [clojure.tools.deps.alpha.util.maven :as mvn]
    [clojure.tools.deps.alpha.libmap :as libmap])
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
  "Add lib at coord to the current runtime environment. All transitive
  dependencies will also be considered (in the context of the current set
  of loaded dependencies) and new transitive dependencies will also be
  loaded. Returns true if any new libs were loaded.

  Note that for successful use, you must be in a REPL environment where a
  valid parent DynamicClassLoader can be found in which to add the new lib
  urls.

  Example:
   (add-lib 'org.clojure/core.memoize {:mvn/version \"0.7.1\"})"
  ([lib coord]
    (add-lib lib coord {:mvn/repos mvn/standard-repos}))
  ([lib coord config]
   (let [existing-libs (libmap/lib-map)]
     (if (contains? existing-libs lib)
       false
       (let [deps (merge config existing-libs {:deps {lib coord}})
             updated-libs (deps/resolve-deps deps nil)
             new-libs (select-keys updated-libs (set/difference (set (keys updated-libs)) (set (keys existing-libs))))
             paths (mapcat :paths (vals new-libs))
             urls (->> paths (map jio/file) (map #(.toURL ^File %)))]
         (run! add-loader-url urls)
         (libmap/add-libs new-libs)
         (pos? (count urls)))))))