package com.example.data.repository

import com.example.data.database.ResumeDao
import com.example.data.model.ResumeEntity
import kotlinx.coroutines.flow.Flow

class ResumeRepository(private val resumeDao: ResumeDao) {
    val allResumes: Flow<List<ResumeEntity>> = resumeDao.getAllResumes()

    suspend fun insertResume(resume: ResumeEntity): Long {
        return resumeDao.insertResume(resume)
    }

    suspend fun deleteResume(id: Int) {
        resumeDao.deleteResume(id)
    }
}
