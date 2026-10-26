package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.DailyUsageEntity
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insertUser(user: UserEntity): Long

  @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
  suspend fun getUserByEmail(email: String): UserEntity?

  @Query("SELECT * FROM users WHERE phoneNumber = :phone AND phoneNumber != '' LIMIT 1")
  suspend fun getUserByPhone(phone: String): UserEntity?

  @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
  suspend fun getUserById(userId: Long): UserEntity?

  @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
  fun observeUserById(userId: Long): Flow<UserEntity?>

  @Update
  suspend fun updateUser(user: UserEntity)

  @Query("UPDATE users SET isPremium = :isPremium, premiumPlan = :plan, premiumExpiry = :expiry WHERE id = :userId")
  suspend fun updatePremiumStatus(userId: Long, isPremium: Boolean, plan: String?, expiry: Long?)

  @Query("SELECT * FROM users WHERE role = 'STUDENT' ORDER BY createdAt DESC")
  fun getAllStudents(): Flow<List<UserEntity>>

  @Query("SELECT * FROM users WHERE role = 'STUDENT' ORDER BY createdAt DESC")
  suspend fun getAllStudentsList(): List<UserEntity>

  @Query("SELECT COUNT(*) FROM users WHERE role = 'STUDENT'")
  suspend fun getTotalStudentsCount(): Int

  @Query("SELECT COUNT(*) FROM users WHERE role = 'STUDENT' AND isPremium = 1")
  suspend fun getPremiumStudentsCount(): Int

  @Query("SELECT * FROM users WHERE role = 'ADMIN' ORDER BY createdAt ASC")
  fun getAllAdmins(): Flow<List<UserEntity>>

  @Query("SELECT * FROM users WHERE role = 'ADMIN' ORDER BY createdAt ASC")
  suspend fun getAllAdminsList(): List<UserEntity>

  @Query("SELECT * FROM users ORDER BY fullName ASC")
  fun getAllUsers(): Flow<List<UserEntity>>

  @Query("SELECT * FROM users ORDER BY fullName ASC")
  suspend fun getAllUsersList(): List<UserEntity>

  @androidx.room.Delete
  suspend fun deleteUser(user: UserEntity)
}

@Dao
interface DailyUsageDao {
  @Query("SELECT * FROM daily_usage WHERE userId = :userId AND dateString = :dateString LIMIT 1")
  suspend fun getUsage(userId: Long, dateString: String): DailyUsageEntity?

  @Query("SELECT * FROM daily_usage WHERE userId = :userId AND dateString = :dateString LIMIT 1")
  fun observeUsage(userId: Long, dateString: String): Flow<DailyUsageEntity?>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun saveUsage(usage: DailyUsageEntity)
}
