package org.tupol.akka.demo.bank2.domain

import java.util.UUID

import scala.concurrent.{ ExecutionContext, Future }

case class ClientId(value: UUID = UUID.randomUUID())

case class Client(id: ClientId, name: String, accounts: Set[Account], active: Boolean = true) {
  def label: String = s"$id:$name"
}

trait ClientDao {
  def create(name: String, accounts: Set[Account] = Set()): Future[Client]
  def findById(id: ClientId): Future[Option[Client]]
  def findByName(name: String): Future[Iterable[Client]]
  def update(client: Client): Future[Client]
}

class InMemoryClientDao(implicit ec: ExecutionContext) extends ClientDao {
  private var clients = Map.empty[ClientId, Client]

  override def create(name: String, accounts: Set[Account] = Set()): Future[Client] =
    Future {
      synchronized {
        val client = Client(ClientId(), name, accounts)
        clients = clients + (client.id -> client)
        client
      }
    }

  override def findById(id: ClientId): Future[Option[Client]] =
    Future {
      synchronized {
        clients.get(id)
      }
    }

  override def findByName(name: String): Future[Iterable[Client]] =
    Future {
      synchronized {
        clients.values.filter((_.name == name))
      }
    }

  override def update(client: Client): Future[Client] =
    Future {
      synchronized {
        clients = clients.updated(client.id, client)
      }
      client
    }

}
