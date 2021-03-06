package io.github.voidcontext.test.slickgenericdao

import io.github.voidcontext.slickgenericdao._
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import slick.jdbc.JdbcBackend.Database

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class UserRepositorySpec extends FlatSpec with Matchers with BeforeAndAfterAll {
  val h2db = Database.forConfig("testDB")
  val h2Profile = slick.jdbc.H2Profile

  trait H2Persistence
    extends ProfileComponent
    with DatabaseComponent {

    val profile = h2Profile
    val db: Database = h2db
  }

  object H2UserComponent
    extends UserComponent
    with H2Persistence


  import H2UserComponent._
  import H2UserComponent.profile.api._

  override def beforeAll = {
    val schema = UserRepository.table.schema
    val f = h2db.run(DBIO.seq(schema.create)) flatMap { _ =>
      h2db run UserRepository.table.size.result
    } flatMap {
      case 0 => h2db run (UserRepository.table += User(None, "Foo", "Bar")) map {
        Some(_)
      }
      case _ => Future {
        None
      }
    }

    Await.result(f, 1.seconds)
  }

  override def afterAll = {
    h2db.close
  }

  it should "query users" in {
    val query = UserRepository.table
    val usersList = Await.result(h2db run query.result, 1.seconds)

    usersList should not be empty
    usersList should have length 1
  }

  it should "find a user by id" in {
    val future =  UserRepository.findById(1)
    val userOption = Await.result(future, 1.seconds)
    userOption shouldBe Some(User(Some(1L), "Foo", "Bar" ))
  }

  it should "insert a new user" in {
    val user = User(None, "Test", "User")
    val insertedRows = Await.result(UserRepository.insert(user), 1.seconds)
    insertedRows shouldBe 1

    val future =  UserRepository.findById(2)
    val userOption = Await.result(future, 1.seconds)
    userOption shouldBe Some(User(Some(2), "Test", "User" ))

    Await.result(h2db run UserRepository.table.size.result, 1.seconds) shouldBe 2
  }

  it should "delete an existing user" in {
    val future =  UserRepository.deleteById(1)
    val affectedRows = Await.result(future, 1.seconds)
    affectedRows shouldBe 1

    val userFuture =  UserRepository.findById(1)
    val userOption = Await.result(userFuture, 1.seconds)
    userOption shouldBe None
  }
}
