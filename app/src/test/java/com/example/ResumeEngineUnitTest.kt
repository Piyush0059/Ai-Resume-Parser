package com.example

import com.example.data.model.AtsDetails
import com.example.data.model.CategorizedSkills
import com.example.data.model.EducationHistory
import com.example.data.model.JdMatchDetails
import com.example.data.model.ResumeConfidence
import com.example.data.model.ResumeData
import com.example.data.model.WorkExperience
import com.example.util.SampleResumes
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ResumeEngineUnitTest {

    private lateinit var moshi: Moshi

    @Before
    fun setup() {
        moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    @Test
    fun testResumeDataDefaultSerialization() {
        val adapter = moshi.adapter(ResumeData::class.java)
        val defaultData = ResumeData(
            name = "John Doe",
            email = "john@example.com",
            phone = "+1-555-0199",
            location = "San Francisco, CA"
        )
        val json = adapter.toJson(defaultData)
        assertNotNull(json)
        assertTrue(json.contains("John Doe"))
        assertTrue(json.contains("john@example.com"))

        val deserialized = adapter.fromJson(json)
        assertNotNull(deserialized)
        assertEquals("John Doe", deserialized?.name)
        assertEquals("john@example.com", deserialized?.email)
        assertEquals("+1-555-0199", deserialized?.phone)
        assertEquals("San Francisco, CA", deserialized?.location)
    }

    @Test
    fun testCompleteResumeDataRoundTrip() {
        val adapter = moshi.adapter(ResumeData::class.java)
        val fullData = ResumeData(
            name = "Jane Smith",
            email = "jane.smith@domain.org",
            phone = "+44 20 7946 0991",
            location = "London, UK",
            linkedinUrl = "https://linkedin.com/in/janesmith",
            githubUrl = "https://github.com/janesmith",
            portfolioUrl = "https://janesmith.dev",
            skills = listOf("Kotlin", "Android", "Jetpack Compose", "Coroutines"),
            technicalSkills = listOf("Kotlin", "Android", "Jetpack Compose"),
            softSkills = listOf("Leadership", "Communication", "Mentorship"),
            languagesKnown = listOf("English", "French"),
            categorizedSkills = CategorizedSkills(
                programmingLanguages = listOf("Kotlin", "Java", "Python"),
                frameworks = listOf("Compose", "Ktor", "Room"),
                databases = listOf("SQLite", "PostgreSQL"),
                cloud = listOf("GCP", "Firebase"),
                devOps = listOf("Docker", "GitHub Actions"),
                aiMl = listOf("Gemini API", "PyTorch")
            ),
            educationHistory = listOf(
                EducationHistory(
                    mainInstitution = "Imperial College London",
                    degree = "B.Sc. in Computer Science",
                    passingYear = "2021"
                )
            ),
            experienceHistory = listOf(
                WorkExperience(
                    companyName = "Apex Mobile Ltd",
                    designation = "Lead Android Engineer",
                    startDate = "Jan 2022",
                    endDate = "Present",
                    duration = "2+ years"
                )
            ),
            projectsList = listOf(
                "Resume Parser Pro: AI-driven resume evaluation engine with Jetpack Compose UI",
                "Cloud Sync Engine: Zero-latency offline caching architecture"
            ),
            achievementsList = listOf(
                "Awarded Top Innovator of the Year 2023",
                "1st Place in Global Android Developer Hackathon"
            ),
            certificationsList = listOf(
                "Google Associate Android Developer (AAD)",
                "AWS Certified Solutions Architect"
            ),
            atsAnalysis = AtsDetails(
                overallScore = 92,
                formattingScore = 95,
                skillCoverageScore = 90,
                keywordOptimizationScore = 88,
                educationQualityScore = 94,
                experienceQualityScore = 93,
                projectsScore = 90,
                certificationsScore = 95,
                improvements = listOf("Quantify business metrics in experience bullets"),
                skillSuggestions = listOf("Add GraphQL exposure")
            ),
            jdMatch = JdMatchDetails(
                matchPercentage = 88,
                matchingSkills = listOf("Kotlin", "Android", "Jetpack Compose"),
                missingSkills = listOf("GraphQL"),
                criticalMissingSkills = emptyList(),
                candidateRanking = "High Fit",
                recommendations = listOf("Highlight Coroutines thread dispatching experience")
            ),
            confidence = ResumeConfidence(
                name = 98.0,
                email = 99.0,
                phone = 95.0,
                location = 90.0,
                skills = 92.0,
                experience = 94.0,
                education = 95.0,
                certifications = 96.0
            ),
            resumeSummary = "Senior Android specialist with 5+ years building scalable mobile apps.",
            careerGuidance = "Target Staff Engineer or Technical Lead roles.",
            interviewQuestions = listOf(
                "Explain the internal recomposition lifecycle of Jetpack Compose.",
                "How do you handle memory allocation when rendering large PDF documents in Android?"
            )
        )

        val json = adapter.toJson(fullData)
        val reconstructed = adapter.fromJson(json)

        assertNotNull(reconstructed)
        assertEquals("Jane Smith", reconstructed?.name)
        assertEquals(92, reconstructed?.atsAnalysis?.overallScore)
        assertEquals(88, reconstructed?.jdMatch?.matchPercentage)
        assertEquals(6, reconstructed?.categorizedSkills?.programmingLanguages?.size?.plus(3)) // 3 languages + 3
        assertEquals(2, reconstructed?.interviewQuestions?.size)
    }

    @Test
    fun testAtsScoreRubricBounds() {
        val details = AtsDetails(
            overallScore = 85,
            formattingScore = 90,
            skillCoverageScore = 80,
            keywordOptimizationScore = 85,
            educationQualityScore = 90,
            experienceQualityScore = 85,
            projectsScore = 75,
            certificationsScore = 80
        )

        assertTrue(details.overallScore in 0..100)
        assertTrue(details.formattingScore in 0..100)
        assertTrue(details.skillCoverageScore in 0..100)
        assertTrue(details.keywordOptimizationScore in 0..100)
        assertTrue(details.educationQualityScore in 0..100)
        assertTrue(details.experienceQualityScore in 0..100)
        assertTrue(details.projectsScore in 0..100)
        assertTrue(details.certificationsScore in 0..100)
    }

    @Test
    fun testSampleResumesIntegrity() {
        assertTrue(SampleResumes.softwareEngineer.isNotEmpty())
        assertTrue(SampleResumes.dataScientist.isNotEmpty())
        assertTrue(SampleResumes.marketingManager.isNotEmpty())
        assertTrue(SampleResumes.defaultJobDescription.isNotEmpty())

        assertTrue(SampleResumes.softwareEngineer.contains("Skills") || SampleResumes.softwareEngineer.contains("EXPERIENCE"))
        assertTrue(SampleResumes.dataScientist.contains("Python") || SampleResumes.dataScientist.contains("Machine Learning"))
        assertTrue(SampleResumes.marketingManager.contains("Marketing") || SampleResumes.marketingManager.contains("Campaign"))
    }
}
