(ns om-chat.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [cljs-time.core :as dt]
            [cljs-time.coerce :as dt2]))

(enable-console-print!)

(def raw-data [
               {
                :id "m_1"
                :threadID "t_1"
                :threadName "Jing and Bill"
                :authorName "Bill"
                :text "Hey Jing want to give a Flux talk at ForwardJS?"
                :timestamp (- (dt/now) 99999)}
               {
                :id "m_2"
                :threadID "t_1"
                :threadName "Jing and Bill"
                :authorName "Bill"
                :text "Seems like a pretty cool conference."
                :timestamp (- (dt/now) 89999)}
               {
                :id "m_3"
                :threadID "t_1"
                :threadName "Jing and Bill"
                :authorName "Jing"
                :text "Sounds good.  Will they be serving dessert?"
                :timestamp (- (dt/now) 79999)}
               {
                :id "m_4"
                :threadID "t_2"
                :threadName "Dave and Bill"
                :authorName "Bill"
                :text "Hey Dave want to get a beer after the conference?"
                :timestamp (- (dt/now) 69999)}
               {
                :id "m_5"
                :threadID "t_2"
                :threadName "Dave and Bill"
                :authorName "Dave"
                :text "Totally!  Meet you at the hotel bar."
                :timestamp (- (dt/now) 59999)}
               {
                :id "m_6"
                :threadID "t_3"
                :threadName "Functional Heads"
                :authorName "Bill"
                :text "Hey Brian are you going to be talking about functional stuff?"
                :timestamp (- (dt/now) 49999)}
               {
                :id "m_7"
                :threadID "t_3"
                :threadName "Bill and Brian"
                :authorName "Brian"
                :text "At ForwardJS?  Yeah of course.  See you there!"
                :timestamp (- (dt/now) 39999)}])

(defn convert-message [{:keys [id authorName text timestamp] :as msg}]
  {:message/id id
   :message/author-name authorName
   :message/text text
   :message/date (dt2/to-date timestamp)
   }
  )

(defn threads-by-id [coll msg]
  (let [{:keys [id threadID threadName timestamp]} msg]
    (if (contains? coll threadID)
      (assoc-in coll [threadID :thread/messages id]
                (convert-message msg))
      (assoc coll threadID
             {:thread/id threadID
              :thread/name threadName
              :thread/messages {id (convert-message msg)}
              })
      ))
  )

(defn threads [coll msg]
  (let [{:keys [id threadID threadName timestamp]} msg
        {:keys [thread/id]} (last coll)
        last-msg-idx (dec (count coll))]
    (if (= id threadID)
      (update-in coll [last-msg-idx :thread/messages] conj (convert-message msg))
      (conj coll
             {:thread/id threadID
              :thread/name threadName
              :thread/messages [(convert-message msg)]
              })
      ))
  )

(defui MessageSection
  static om/Ident
  (ident [this {:keys [message/id]}]
         [:messages/by-id id])
  static om/IQuery
  (query [this]
         [:message/id :message/author-name :message/date :message/text])
  Object
  (render [this]
          (dom/div #js{:className "message-section"}
                   "Message section...")))

(def message-section (om/factory MessageSection))

(defui ThreadSection
  static om/Ident
  (ident [this {:keys [thread/id]}]
         [:threads/by-id id])
  static om/IQuery
  (query [this]
         [:thread/id :thread/name :thread/read :thread/selected
                     {:thread/last-message [:message/date :message/text]}
                     {:thread/messages (om/get-query MessageSection)}])
  Object
  (render [this]
          (dom/div #js{:className "thread-section"}
                   "Thread Section...")
          ))

(def thread-section (om/factory ThreadSection))

(defui ChatApp
  static om/IQuery
  (query [this]
         [{:threads (om/get-query ThreadSection)}])
  Object
  (render [this]
          (let [{:keys [threads]} (om/props this)]
            (dom/div #js{:className "chatapp"}
                     (thread-section threads)
                     (message-section (last threads))))))

(defmulti read om/dispatch)

(defmethod read :default
  [{:keys [state] :as env} k _]
  (let [st @state]
    (if (contains? st k)
      {:value (get st k)}
      {:remote true})))


(def reconciler
  (om/reconciler {:state {:threads (reduce threads [] raw-data)}
                  :parser (om/parser {:read read})}))

(om/add-root! reconciler
              ChatApp
              (gdom/getElement "app"))
