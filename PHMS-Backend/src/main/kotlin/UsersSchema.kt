package com.example

import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class ExposedUser(
    val firstName: String,
    val lastName: String,
    val age: Int?,
    val biometricEnabled: Boolean = false
    )

class UserService(database: Database) {
    object Users : Table() {
        val id = integer("id").autoIncrement()
        val firstName = varchar("first_name", 100)
        val lastName = varchar("last_name", 100)
        val age = integer("age").nullable()
        val biometricEnabled = bool("biometric_enabled").default(false)
        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Users)
        }
    }

    suspend fun create(user: ExposedUser): Int = dbQuery {
        Users.insert {
            it[firstName] = user.firstName
            it[lastName] = user.lastName
            it[age] = user.age
            it[biometricEnabled] = user.biometricEnabled
        }[Users.id]
    }

    suspend fun read(id: Int): ExposedUser? {
        return dbQuery {
            Users.selectAll()
                .where { Users.id eq id }
                .map { ExposedUser(it[Users.firstName], it[Users.lastName], it[Users.age],it[Users.biometricEnabled]) }
                .singleOrNull()
        }
    }

    suspend fun update(id: Int, user: ExposedUser) {
        dbQuery {
            Users.update({ Users.id eq id }) {
                it[firstName] = user.firstName
                it[lastName] = user.lastName
                it[age] = user.age
                it[biometricEnabled] = user.biometricEnabled
            }
        }
    }

    suspend fun delete(id: Int) {
        dbQuery {
            Users.deleteWhere { Users.id.eq(id) }
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}

