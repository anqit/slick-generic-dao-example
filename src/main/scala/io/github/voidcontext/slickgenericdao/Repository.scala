package io.github.voidcontext.slickgenericdao

import slick.driver.JdbcDriver
import slick.jdbc.JdbcBackend.Database
import slick.lifted.AbstractTable
import slick.ast.BaseTypedType

abstract class Repository[T <: AbstractTable[_], I: BaseTypedType](val driver: JdbcDriver, val db: Database) {
  import driver.api._

  type Id = I
  def table: TableQuery[T]

  def getId(row: T): Rep[Id]

  def filterById(id: Id) = table filter (getId(_) === id)
  def findById(id: Id) = db run filterById(id).result.headOption

  def insert(model: T#TableElementType) = db run (table += model)

  def deleteById(id: Id) = {

    val deleteAction = buildDeleteAction(id)
    db run deleteAction.delete
  }

  private def buildDeleteAction(id: Id) = {
    driver.createDeleteActionExtensionMethods(
      driver.deleteCompiler.run(filterById(id).toNode).tree, ()
    )
  }
}

