package com.finora.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** startMonth / endMonth are "yyyy-MM". endMonth = last month the member is part of the mess. */
@Entity(tableName = "members")
data class Member(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startMonth: String,
    val endMonth: String? = null
)

/** Money a member gave to the mess fund. date = LocalDate epoch day. */
@Entity(tableName = "deposits")
data class Deposit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memberId: Long,
    val amount: Double,
    val date: Long,
    val note: String = ""
)

/** Shared grocery/bazar expense (divided by meals). */
@Entity(tableName = "bazars")
data class Bazar(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val date: Long,
    val note: String = ""
)

/** Fixed monthly cost (rent, wifi, bills...) divided equally among members of that month. */
@Entity(tableName = "shared_costs")
data class SharedCost(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val date: Long
)

/** Meals eaten by a member on a day. */
@Entity(tableName = "meals", primaryKeys = ["memberId", "date"])
data class Meal(
    val memberId: Long,
    val date: Long,
    val count: Double
)

@Dao
interface MessDao {
    @Query("SELECT * FROM members ORDER BY id") fun members(): Flow<List<Member>>
    @Query("SELECT * FROM deposits ORDER BY date DESC, id DESC") fun deposits(): Flow<List<Deposit>>
    @Query("SELECT * FROM bazars ORDER BY date DESC, id DESC") fun bazars(): Flow<List<Bazar>>
    @Query("SELECT * FROM shared_costs ORDER BY date DESC, id DESC") fun sharedCosts(): Flow<List<SharedCost>>
    @Query("SELECT * FROM meals") fun meals(): Flow<List<Meal>>

    @Insert suspend fun insertMember(m: Member): Long
    @Update suspend fun updateMember(m: Member)
    @Insert suspend fun insertDeposit(d: Deposit)
    @Delete suspend fun deleteDeposit(d: Deposit)
    @Insert suspend fun insertBazar(b: Bazar)
    @Delete suspend fun deleteBazar(b: Bazar)
    @Insert suspend fun insertShared(s: SharedCost)
    @Delete suspend fun deleteShared(s: SharedCost)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertMeal(m: Meal)
}

@Database(
    entities = [Member::class, Deposit::class, Bazar::class, SharedCost::class, Meal::class],
    version = 1,
    exportSchema = false
)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): MessDao

    companion object {
        @Volatile private var instance: AppDb? = null

        fun get(context: Context): AppDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, AppDb::class.java, "finora.db"
            ).build().also { instance = it }
        }
    }
}
