package com.example.data.local

import androidx.room.*
import com.example.data.model.Member
import com.example.data.model.MemberStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {
    @Query("SELECT * FROM members ORDER BY name ASC")
    fun getAllMembers(): Flow<List<Member>>

    @Query("SELECT * FROM members WHERE status = :status ORDER BY name ASC")
    fun getMembersByStatus(status: MemberStatus): Flow<List<Member>>

    @Query("SELECT * FROM members WHERE id = :id")
    fun getMemberByIdFlow(id: Long): Flow<Member?>

    @Query("SELECT * FROM members WHERE id = :id")
    suspend fun getMemberById(id: Long): Member?

    @Query("SELECT * FROM members WHERE memberCode = :code OR qrToken = :code LIMIT 1")
    suspend fun getMemberByCode(code: String): Member?

    @Query("SELECT * FROM members WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR memberCode LIKE '%' || :query || '%'")
    fun searchMembers(query: String): Flow<List<Member>>

    @Query("SELECT COUNT(*) FROM members")
    fun getTotalMembersCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM members WHERE status = 'ACTIVE'")
    fun getActiveMembersCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM members WHERE status = 'EXPIRING_SOON'")
    fun getExpiringMembersCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM members WHERE status = 'EXPIRED'")
    fun getExpiredMembersCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM members WHERE status = 'PAYMENT_PENDING'")
    fun getPendingPaymentMembersCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: Member): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMembers(members: List<Member>)

    @Update
    suspend fun updateMember(member: Member)

    @Delete
    suspend fun deleteMember(member: Member)

    @Query("DELETE FROM members")
    suspend fun deleteAll()
}
