(ns specy.value
  (:require [#?(:clj  clojure.spec.gen.alpha
                :cljs cljs.spec.gen.alpha) :as gen]
            [#?(:clj  clojure.spec.alpha
                :cljs cljs.spec.alpha) :as s]
            [clojure.test.check.generators :as check-gen]

            [specy.protocols :refer :all]
            [specy.infra.bus :refer [bus]]
            [specy.infra.repository :refer [building-blocks]]
            [specy.utils :refer [inspect operations parse-opts+specs]]
            ))

(defmacro doseq-macro
  [macroname & args]
  `(do
     ~@(map (fn [arg] (list macroname arg)) args)))

(defmacro add-valuable-operations [name fields]
  (let [sb (gensym 'sb)
        value (gensym 'value)]
    `(extend-type ~name
       Valueable
       (to-string [~value]
         (let [~sb (StringBuilder.)]
           ~@(map (fn [field] (if (= field :space)
                               `(.append ~sb " ")
                               `(.append ~sb (get ~value ~(keyword (str field)))))) (interpose :space fields))
           (.toString ~sb))))))

(defmacro defvalue
  "(defvalue name [fields*] protocol-name [operations*] options*) "
  {:arglists '([name [& fields] & opts+specs])}
  ([name fields & opts+specs]
   (let [inspected-fields (inspect building-blocks fields)
         fields-name (map :field inspected-fields)
         [interfaces methods opts] (parse-opts+specs opts+specs)
         operations (operations methods)
         ns *ns*]
     `(do
        ~(if (not-empty opts+specs)
           `(defprotocol ~@opts+specs)
           `(defprotocol ~(symbol (str name "able"))))
        (defrecord ~name [~@fields-name]);~(if (not-empty opts+specs) (first opts+specs) (symbol (str name "able")))
        (add-valuable-operations ~name ~fields-name)
        ;;return the value as a data structure
        (let [value-desc# {:name       (clojure.reflect/typename ~name)
                           :longname   ~(str ns "/" (clojure.reflect/typename name))
                           :ns         ~ns ;;caller ns
                           :id         ~(keyword (str ns) (clojure.string/lower-case (str name)))
                           :class      ~name
                           :kind       :value
                           :fields     ~(vec (map #(dissoc % :field) inspected-fields))
                           :interface  ~(first interfaces)
                           :operations ~operations
                          }]
          (store! building-blocks value-desc#)
          (publish! bus value-desc#)
          value-desc#)))))
