(ns com.jayway.rps.domain
  (:require [com.jayway.rps.core :as c]))

; MESSAGES

(defrecord CreateGameCommand [aggregate-id player move])
(defrecord DecideMoveCommand [aggregate-id player move])

(defrecord GameCreatedEvent [game-id creator])
(defrecord MoveDecidedEvent [game-id player move])
(defrecord GameWonEvent [game-id winner loser])
(defrecord GameTiedEvent [game-id])

; move rules

(defmulti compare-moves vector)
(defmethod compare-moves [:rock :rock] [x y] :tie)
(defmethod compare-moves [:rock :paper] [x y] :loss)
(defmethod compare-moves [:rock :scissor] [x y] :victory)
(defmethod compare-moves [:paper :rock] [x y] :victory)
(defmethod compare-moves [:paper :paper] [x y] :tie)
(defmethod compare-moves [:paper :scissor] [x y] :loss)
(defmethod compare-moves [:scissor :rock] [x y] :loss)
(defmethod compare-moves [:scissor :paper] [x y] :victory)
(defmethod compare-moves [:scissor :scissor] [x y] :tie)

; game aggregate - event handlers

(defmethod c/apply-event GameCreatedEvent [state event]
  (assoc state
    :state :started
    :creator (:creator event)))

(defmethod c/apply-event MoveDecidedEvent [state event]
  (assoc state
    :move (:move event)))

(defmethod c/apply-event GameWonEvent [state event]
  (assoc state
    :state :completed))

(defmethod c/apply-event GameTiedEvent [state event]
  (assoc state
    :state :completed))

; game aggregate command handler

(extend-protocol c/CommandHandler
  CreateGameCommand
  (c/perform [command state]
    (when (:state state)
      (throw (Exception. "Already in started")))
    [(->GameCreatedEvent (:aggregate-id command) (:player command))
     (->MoveDecidedEvent (:aggregate-id command) (:player command) (:move command))])

  DecideMoveCommand
  (c/perform [command state]
    (when-not (= (:state state) :started)
      (throw (Exception. "Incorrect state")))
    (when (= (:createdBy state) (:player command))
      (throw (Exception. "Cannot play against yourself")))
    [(->MoveDecidedEvent (:aggregate-id command) (:player command) (:move command))
     (case (compare-moves (:move state) (:move command))
       :victory (->GameWonEvent (:aggregate-id command) (:creator state) (:player command))
       :loss (->GameWonEvent (:aggregate-id command) (:player command) (:creator state))
       :tie (->GameTiedEvent (:aggregate-id command)))]))
