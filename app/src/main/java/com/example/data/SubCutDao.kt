package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SubCutDao {
    @Query("SELECT * FROM subtitle_projects ORDER BY updatedAt DESC")
    fun observeAllProjects(): Flow<List<SubtitleProjectEntity>>

    @Query("SELECT * FROM subtitle_projects WHERE id = :projectId LIMIT 1")
    fun observeProjectById(projectId: Long): Flow<SubtitleProjectEntity?>

    @Query("SELECT * FROM subtitle_projects ORDER BY updatedAt DESC")
    suspend fun getAllProjectsOnce(): List<SubtitleProjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: SubtitleProjectEntity): Long

    @Update
    suspend fun updateProject(project: SubtitleProjectEntity)

    @Query("DELETE FROM subtitle_projects WHERE id = :projectId")
    suspend fun deleteProjectById(projectId: Long)

    @Query("SELECT * FROM caption_segments WHERE projectId = :projectId ORDER BY startMs ASC, id ASC")
    fun observeSegmentsForProject(projectId: Long): Flow<List<CaptionSegmentEntity>>

    @Query("SELECT * FROM caption_segments WHERE projectId = :projectId ORDER BY startMs ASC, id ASC")
    suspend fun getSegmentsForProjectOnce(projectId: Long): List<CaptionSegmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegment(segment: CaptionSegmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegments(segments: List<CaptionSegmentEntity>)

    @Update
    suspend fun updateSegment(segment: CaptionSegmentEntity)

    @Query("DELETE FROM caption_segments WHERE id = :segmentId")
    suspend fun deleteSegmentById(segmentId: Long)

    @Query("DELETE FROM caption_segments WHERE projectId = :projectId")
    suspend fun deleteSegmentsForProject(projectId: Long)

    @Transaction
    suspend fun replaceProjectSegments(projectId: Long, newSegments: List<CaptionSegmentEntity>) {
        deleteSegmentsForProject(projectId)
        insertSegments(newSegments)
    }

    @Query("SELECT * FROM style_presets ORDER BY isBuiltIn DESC, id ASC")
    fun observeAllPresets(): Flow<List<StylePresetEntity>>

    @Query("SELECT COUNT(*) FROM style_presets")
    suspend fun getPresetCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresets(presets: List<StylePresetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: StylePresetEntity): Long

    @Query("DELETE FROM style_presets WHERE id = :presetId AND isBuiltIn = 0")
    suspend fun deleteCustomPreset(presetId: Long)
}
