package com.github.jostly.fsm

import scala.util.{Failure, Success, Try}

trait CommandReceiver[-Ein, -C, +EOut] {
  def send(state: List[Ein], command: C): List[EOut]
}

trait StateMachine[State, Cmd, Evt] extends CommandReceiver[Evt, Cmd, Evt] {
  def send(state: Option[State], command: Cmd): Try[Evt]
  def handle(state: Option[State], event: Evt): Try[State]

  override def send(state: List[Evt], command: Cmd): List[Evt] = {
    val s = state.foldRight(None.asInstanceOf[Option[State]])(replayFunc)
    send(s, command).get :: state
  }

  def showTransitions(): Unit

  private def replayFunc(e: Evt, s: Option[State]): Option[State] = {
    Some(handle(s, e).get)
  }
}

object StateMachine {
  def apply[State, Cmd, Evt](opsies: List[(Option[State], Op[Cmd, Evt, State])]*): StateMachine[State, Cmd, Evt] =
    new Impl[State, Cmd, Evt](opsies
      .toList
      .flatten
      .groupBy(e => e._1)
      .mapValues(e => e.map(x => x._2))
      .toList
    )

  class Impl[S, C, E](val operations: List[(Option[S], List[Op[C, E, S]])]) extends StateMachine[S, C, E] {
    override def send(state: Option[S], command: C): Try[E] = {

      operations.find(_._1 == state) match {
        case Some((_, ops)) =>
          ops.find(op => op.emitFunc.isDefinedAt(command)) match {
            case Some(op) =>
              val e = op.emitFunc(command)
              Success(e)
            case None =>
              Failure(new IllegalStateException(s"No action for $command in $state"))
          }
        case None =>
          Failure(new IllegalStateException(s"No actions in $state"))
      }
    }
    override def handle(state: Option[S], event: E): Try[S] = {
      operations.find(_._1 == state) match {
        case Some((_, ops)) =>
          ops.find(op => op.handleFunc.isDefinedAt(event)) match {
            case Some(op) =>
              Success(op.handleFunc(event))
            case None =>
              Failure(new IllegalStateException(s"No handler for $event in $state"))
          }
        case None =>
          Failure(new IllegalStateException(s"No event handlers in $state"))
      }
    }
    override def showTransitions(): Unit = {
      for ((from, ops) <- operations;
           op <- ops) {
        val fromState = from.map(_.toString).getOrElse("Idle")
        val toState = op.targetStateDescriptor match {
          case `fromState` => "stay"
          case x => s"go to $x"
        }
        println(s"In $fromState, on ${op.commandDescriptor} emit ${op.eventDescriptor} and $toState")
      }
    }
  }

}

