(ns specy.entity
  (:require [#?(:clj  clojure.spec.gen.alpha
                :cljs cljs.spec.gen.alpha) :as gen]
            [#?(:clj  clojure.spec.alpha
                :cljs cljs.spec.alpha) :as s]
            [clojure.test.check.generators :as check-gen]

            [specy.protocols :refer :all]
            [specy.utils :refer [inspect operations parse-opts+specs]]
            [specy.infra.bus :refer [bus]]
            [specy.infra.repository :refer [building-blocks]]

            [spec-tools.data-spec :as ds]
            [clojure.string :as string]))

(defmacro defentity
  "(defentity name [fields*] protocol-name [operations*] options*) "
  {:arglists '([name [& fields] & opts+specs])}
  ([name fields & opts+specs]
   (let [inspected-fields (inspect building-blocks fields)
         fields-name (map :field inspected-fields)
         [interfaces methods opts] (parse-opts+specs opts+specs)
         operations (operations methods)
         ns *ns*]
     `(do
        ~(if (not-empty opts+specs)
           `(do
              (defprotocol ~@opts+specs)
              (defrecord ~name [~@fields-name] ~(first opts+specs)))
           `(defrecord ~name [~@fields-name]))
        ;;TODO create the repository interface associated to that entity ? or build a defrepository macro ?
        (let [entity-desc# (merge {:id         ~(keyword (str ns) (clojure.string/lower-case (str name)))
                                   :name       ~(str name)
                                   :longname   (clojure.reflect/typename ~name)
                                   :ns         ~ns          ;;caller ns
                                   :class      ~name
                                   :kind       :entity
                                   :fields     ~(vec (map #(dissoc % :field) inspected-fields))
                                   :operations ~operations}
                                  ~(when (not-empty opts+specs)
                                     :interface ~(first interfaces)))]
          ;;TODO create spec associated to the entity
          (store! building-blocks entity-desc#)
          (publish! bus entity-desc#)
          entity-desc#)))))



(defmacro defentity2
  "(defentity name doc spec [operations*]) "
  {:arglists '([name spec & behaviors])}
  [name & args]
  (let [;;next lines deal with docstring optionality ...
        doc (when (string? (first args))
              (first args))
        ;;remove docstring from args
        args (if (string? (first args))
               (next args)
               args)
        entity-spec (first args)
        operations (rest args)

        fields (map symbol (keys entity-spec))
        ns *ns*
        id (keyword (str ns) (clojure.string/lower-case (str name)))
        _ (prn (qualified-keyword? id))
        interface (map #(take 2 %) operations)]
    `(do
       (def ~(symbol (str name "-spec")) (ds/spec {:name ~id
                                                   :spec ~entity-spec}))

       (defprotocol ~(symbol (str (string/capitalize name) "Procotol"))
         ~@interface)

       (defrecord ~name [~@fields]
         ~(symbol (str (string/capitalize name) "Procotol"))
         ~@operations)

       (defn ~(symbol (str "->" (string/lower-case name))) [m#]
         (s/assert* ~(symbol (str name "-spec")) m#)
         (~(symbol (str "map->" name)) m#))

       (let [entity-desc# (merge {:id         ~(keyword (str ns) (clojure.string/lower-case (str name)))
                                  :name       ~(str name)
                                  :longname   (clojure.reflect/typename ~name)
                                  :ns         ~ns           ;;caller ns
                                  :class      ~name
                                  :kind       :entity
                                  :spec       ~(symbol (str name "-spec"))
                                  :interface  ~interface
                                  :fields     nil
                                  :operations ~operations
                                  :doc        ~doc})]
         (store! building-blocks entity-desc#)
         (publish! bus entity-desc#)
         entity-desc#))))