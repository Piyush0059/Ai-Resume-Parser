package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WorkExperience(
    val companyName: String = "",
    val designation: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val duration: String = ""
)

@JsonClass(generateAdapter = true)
data class EducationHistory(
    val mainInstitution: String = "", // School, College, or University
    val degree: String = "",
    val passingYear: String = ""
)

@JsonClass(generateAdapter = true)
data class AtsDetails(
    val overallScore: Int = 70,
    val formattingScore: Int = 70,
    val skillCoverageScore: Int = 70,
    val keywordOptimizationScore: Int = 70,
    val educationQualityScore: Int = 70,
    val experienceQualityScore: Int = 70,
    val projectsScore: Int = 70,
    val certificationsScore: Int = 70,
    val improvements: List<String> = emptyList(),
    val skillSuggestions: List<String> = emptyList(),
    val experienceSuggestions: List<String> = emptyList(),
    val educationSuggestions: List<String> = emptyList(),
    val projectsSuggestions: List<String> = emptyList(),
    val certificationsSuggestions: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class JdMatchDetails(
    val matchPercentage: Int = 0,
    val missingSkills: List<String> = emptyList(),
    val matchingSkills: List<String> = emptyList(),
    val criticalMissingSkills: List<String> = emptyList(),
    val candidateRanking: String = "Medium Fit",
    val recommendations: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ResumeConfidence(
    val name: Double = 90.0,
    val email: Double = 95.0,
    val phone: Double = 95.0,
    val location: Double = 80.0,
    val skills: Double = 85.0,
    val experience: Double = 85.0,
    val education: Double = 85.0,
    val certifications: Double = 80.0
)

@JsonClass(generateAdapter = true)
data class CategorizedSkills(
    val programmingLanguages: List<String> = emptyList(),
    val frameworks: List<String> = emptyList(),
    val databases: List<String> = emptyList(),
    val cloud: List<String> = emptyList(),
    val devOps: List<String> = emptyList(),
    val aiMl: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ResumeData(
    // Personal Details
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val dateOfBirth: String = "",
    val location: String = "",
    val linkedinUrl: String = "",
    val githubUrl: String = "",
    val portfolioUrl: String = "",

    // Skills
    val skills: List<String> = emptyList(), // Combined/Legacy fallback
    val technicalSkills: List<String> = emptyList(),
    val softSkills: List<String> = emptyList(),
    val languagesKnown: List<String> = emptyList(),
    val categorizedSkills: CategorizedSkills = CategorizedSkills(),

    // Professional & Educational details
    val educationHistory: List<EducationHistory> = emptyList(),
    val experienceHistory: List<WorkExperience> = emptyList(),
    val projectsList: List<String> = emptyList(),
    val achievementsList: List<String> = emptyList(),
    val certificationsList: List<String> = emptyList(),

    // Career Details
    val expectedSalary: String = "",
    val currentSalary: String = "",
    val noticePeriod: String = "",
    val detectedLanguage: String = "English",

    // ATS Scoring
    val atsAnalysis: AtsDetails = AtsDetails(),

    // Job Matching (optional if matched)
    val jdMatch: JdMatchDetails? = null,

    // Confidence mapping
    val confidence: ResumeConfidence = ResumeConfidence(),

    // Career Companion add-ons
    val resumeSummary: String = "",
    val careerGuidance: String = "",
    val interviewQuestions: List<String> = emptyList(),

    // Backward compatibility for existing references in UI lists:
    val education: List<String> = emptyList(),
    val experience: List<String> = emptyList(),
    val certifications: List<String> = emptyList(),
    val projects: List<String> = emptyList()
)
