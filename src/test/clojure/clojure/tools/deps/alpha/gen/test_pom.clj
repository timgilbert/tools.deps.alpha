(ns clojure.tools.deps.alpha.gen.test-pom
  (:require [clojure.test :refer :all]
            [clojure.string :as string]
            [clojure.data.xml :as xml]
            [clojure.tools.deps.alpha.gen.pom :as pom]))

(defn- repo-to-string [repos]
  (let [gen-pom #'pom/gen-pom]
    (xml/indent-str (gen-pom {} nil repos "project-name"))))

(deftest test-repo-xml-generation
  (are [match deps]
    (string/includes? (repo-to-string deps) match)

    "<id>bar</id>" {"bar" {:url "foo"}}
    "<url>foo</url>" {"bar" {:url "foo"}}
    "<snapshots>" {"bar" {:url "foo" :snapshots {:enabled true}}}
    "<releases" {"bar" {:url "foo" :releases {}}}
    "<enabled>true</enabled>" {"bar" {:url "foo" :snapshots {:enabled true}}}
    "<updatePolicy>never</updatePolicy>" {"bar" {:url "foo" :releases {:update-policy "never"}}}
    "<checksumPolicy>warn</checksumPolicy>" {"bar" {:url "foo" :releases {:checksum-policy "warn"}}}))
