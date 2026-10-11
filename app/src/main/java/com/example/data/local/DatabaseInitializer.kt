package com.example.data.local

import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.DepartmentEntity
import com.example.data.local.entity.FacultyEntity
import com.example.data.local.entity.TopicEntity
import com.example.data.local.entity.UniversityEntity
import com.example.data.local.entity.UserEntity
import com.example.data.util.PasswordSecurity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DatabaseInitializer {

  suspend fun populateInitialData(database: AppDatabase) {
    withContext(Dispatchers.IO) {
      val academicDao = database.academicDao()
      val questionDao = database.questionDao()

      // Clean up any old demo questions from offline cache so all content is live & admin-managed
      try {
        val existingQuestions = questionDao.getAllQuestionsList()
        val demoItems = existingQuestions.filter { it.isDemo || it.questionText.startsWith("[Demo Question]") }
        for (dq in demoItems) {
          questionDao.deleteQuestion(dq)
        }
      } catch (_: Exception) {}

      // 1. Ensure basic academic organizational hierarchy exists for University of Maiduguri
      val universities = academicDao.getUniversitiesList()
      var uniId: Long = 1
      if (universities.isEmpty()) {
        uniId = academicDao.insertUniversity(
          UniversityEntity(
            name = "University of Maiduguri",
            shortName = "UNIMAID",
            status = "Active"
          )
        )
      } else {
        uniId = universities.first().id
      }

      val faculties = academicDao.getFacultiesListByUniversity(uniId)
      var facultyId: Long = 1
      if (faculties.isEmpty()) {
        facultyId = academicDao.insertFaculty(
          FacultyEntity(
            id = 1,
            universityId = uniId,
            name = "College of Medical Sciences"
          )
        )
      } else {
        facultyId = faculties.first().id
      }

      val departments = academicDao.getDepartmentsListByFaculty(facultyId)
      var deptId: Long = 1
      if (departments.isEmpty()) {
        deptId = academicDao.insertDepartment(
          DepartmentEntity(
            id = 1,
            facultyId = facultyId,
            name = "Medical Radiography"
          )
        )
      } else {
        deptId = departments.first().id
      }

      // Ensure base RAD 101 course and topics structure exists for departmental mapping
      val existingCourse = academicDao.getCourseById(1)
      if (existingCourse == null) {
        val courseId = academicDao.insertCourse(
          CourseEntity(
            id = 1,
            departmentId = deptId,
            code = "RAD 101",
            title = "Introduction to Radiography",
            level = "100 Level",
            semester = "First Semester",
            description = "Foundational principles of medical radiography, discovery and nature of X-radiation, radiation protection (ALARA), radiographic equipment, and clinical ethics in radiology."
          )
        )

        academicDao.insertTopic(
          TopicEntity(courseId = courseId, name = "Fundamentals & Scope of Radiography", orderIndex = 1)
        )
        academicDao.insertTopic(
          TopicEntity(courseId = courseId, name = "Discovery & Nature of X-rays (Röntgen 1895)", orderIndex = 2)
        )
        academicDao.insertTopic(
          TopicEntity(courseId = courseId, name = "Radiation Protection & ALARA Principles", orderIndex = 3)
        )
        academicDao.insertTopic(
          TopicEntity(courseId = courseId, name = "Radiographic Equipment & X-ray Tube Construction", orderIndex = 4)
        )
        academicDao.insertTopic(
          TopicEntity(courseId = courseId, name = "Medical Ethics & Patient Care in Imaging", orderIndex = 5)
        )
      }

      // 2. Ensure Default Authenticated Admin & Student Accounts exist if not present
      val userDao = database.userDao()
      val adminEmail = "admin@medrad.edu.ng"
      if (userDao.getUserByEmail(adminEmail) == null) {
        userDao.insertUser(
          UserEntity(
            fullName = "Chief Radiographer / Admin",
            email = adminEmail,
            phoneNumber = "+2348000000001",
            passwordHash = PasswordSecurity.hashPassword("MedRadAdmin2026!"),
            role = "ADMIN",
            adminRole = "PRIMARY",
            permissions = "ALL",
            university = "University of Maiduguri",
            faculty = "College of Medical Sciences",
            department = "Medical Radiography",
            level = "Staff / Faculty",
            semester = "Full Session",
            isPremium = true
          )
        )
      }

      val studentEmail = "student@medrad.edu.ng"
      if (userDao.getUserByEmail(studentEmail) == null) {
        userDao.insertUser(
          UserEntity(
            fullName = "Khadija Usman",
            email = studentEmail,
            phoneNumber = "+2348000000002",
            passwordHash = PasswordSecurity.hashPassword("Student123!"),
            role = "STUDENT",
            university = "University of Maiduguri",
            faculty = "College of Medical Sciences",
            department = "Medical Radiography",
            level = "100 Level",
            semester = "First Semester",
            isPremium = false
          )
        )
      }
    }
  }
}
