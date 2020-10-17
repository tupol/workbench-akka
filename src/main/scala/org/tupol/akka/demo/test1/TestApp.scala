package org.tupol.akka.demo.test1

import akka.Done
import akka.pattern.after

import scala.concurrent.Future
import scala.concurrent.duration._

class TestApp extends App {

//  ActorSystem[Nothing](DataAccess("da-id-01", new TestDB()), "")

}

//class TestDB extends DB {
//
//  override def update(id: String, value: String): Future[Done] = after(2.seconds)(Future.successful(Done))
//
//  override def load(id: String): Future[String] = Future.successful(id)
//
//  override def create(value: String): Future[(String, String)] = ???
//
//  override def delete(id: String): Future[Done] = ???
//}
