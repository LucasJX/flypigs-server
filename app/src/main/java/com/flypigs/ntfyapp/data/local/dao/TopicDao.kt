package com.flypigs.ntfyapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.flypigs.ntfyapp.data.local.entity.TopicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {

    @Query("SELECT * FROM topics ORDER BY name ASC")
    fun getAllTopics(): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE serverId = :serverId ORDER BY name ASC")
    fun getTopicsByServer(serverId: String): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE isEnabled = 1 ORDER BY name ASC")
    fun getEnabledTopics(): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE id = :id")
    suspend fun getTopicById(id: String): TopicEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: TopicEntity)

    @Update
    suspend fun updateTopic(topic: TopicEntity)

    @Delete
    suspend fun deleteTopic(topic: TopicEntity)

    @Query("DELETE FROM topics WHERE id = :id")
    suspend fun deleteTopicById(id: String)

    @Query("UPDATE topics SET isEnabled = :enabled WHERE id = :id")
    suspend fun updateEnabled(id: String, enabled: Boolean)
}
