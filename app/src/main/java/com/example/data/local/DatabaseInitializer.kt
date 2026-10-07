package com.example.data.local

import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.CourseLibraryEntity
import com.example.data.local.entity.DepartmentEntity
import com.example.data.local.entity.ExamEntity
import com.example.data.local.entity.FacultyEntity
import com.example.data.local.entity.QuestionEntity
import com.example.data.local.entity.StudyMaterialEntity
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
      val examDao = database.examDao()
      val materialDao = database.studyMaterialDao()

      // 1. Seed Academic Curriculum Structure (as offline cache only; authenticated identities come from Supabase)
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

      var facultyId: Long = 1
      val faculties = academicDao.getFacultiesByUniversity(uniId)
      facultyId = academicDao.insertFaculty(
        FacultyEntity(
          id = 1,
          universityId = uniId,
          name = "College of Medical Sciences"
        )
      )

      var deptId: Long = 1
      deptId = academicDao.insertDepartment(
        DepartmentEntity(
          id = 1,
          facultyId = facultyId,
          name = "Medical Radiography"
        )
      )

      var courseId: Long = 1
      val existingCourse = academicDao.getCourseById(1)
      if (existingCourse == null) {
        courseId = academicDao.insertCourse(
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

        // Topics
        val topic1Id = academicDao.insertTopic(
          TopicEntity(courseId = courseId, name = "Fundamentals & Scope of Radiography", orderIndex = 1)
        )
        val topic2Id = academicDao.insertTopic(
          TopicEntity(courseId = courseId, name = "Discovery & Nature of X-rays (Röntgen 1895)", orderIndex = 2)
        )
        val topic3Id = academicDao.insertTopic(
          TopicEntity(courseId = courseId, name = "Radiation Protection & ALARA Principles", orderIndex = 3)
        )
        val topic4Id = academicDao.insertTopic(
          TopicEntity(courseId = courseId, name = "Radiographic Equipment & X-ray Tube Construction", orderIndex = 4)
        )
        val topic5Id = academicDao.insertTopic(
          TopicEntity(courseId = courseId, name = "Medical Ethics & Patient Care in Imaging", orderIndex = 5)
        )

        // 3. Seed Clearly Labelled Demo Questions
        val demoQuestions = listOf(
          QuestionEntity(
            courseId = courseId,
            topicId = topic2Id,
            questionText = "[Demo Question] Who discovered X-rays on November 8, 1895, marking the inception of modern medical radiography?",
            optionA = "Marie Curie",
            optionB = "Wilhelm Conrad Röntgen",
            optionC = "Thomas Alva Edison",
            optionD = "Henri Becquerel",
            correctAnswer = "B",
            explanation = "Wilhelm Conrad Röntgen discovered X-radiation on November 8, 1895, at the University of Würzburg, Germany, while experimenting with cathode rays. He was awarded the first Nobel Prize in Physics in 1901.",
            difficulty = "Easy",
            isPremium = false,
            isPublished = true,
            isDemo = true
          ),
          QuestionEntity(
            courseId = courseId,
            topicId = topic3Id,
            questionText = "[Demo Question] In radiation protection and clinical radiology safety, what does the fundamental acronym ALARA represent?",
            optionA = "As Low As Reasonably Achievable",
            optionB = "All Levels Are Radiation Absorbed",
            optionC = "Always Lower Annual Radiation Activity",
            optionD = "As Linear As Radiation Allows",
            correctAnswer = "A",
            explanation = "ALARA stands for 'As Low As Reasonably Achievable'. It is the central regulatory and ethical philosophy in radiation safety, ensuring both occupational personnel and patient doses remain as low as possible without sacrificing diagnostic image quality.",
            difficulty = "Easy",
            isPremium = false,
            isPublished = true,
            isDemo = true
          ),
          QuestionEntity(
            courseId = courseId,
            topicId = topic3Id,
            questionText = "[Demo Question] What are the three cardinal principles of radiation protection used to reduce occupational dose from external ionizing radiation sources?",
            optionA = "Kilovoltage, Milliamperage, Focal Spot",
            optionB = "Time, Distance, Shielding",
            optionC = "Collimation, Filtration, Grids",
            optionD = "Anode Angle, Cathode Current, Heat Units",
            correctAnswer = "B",
            explanation = "The three cardinal rules of radiation protection are: Time (minimize exposure duration), Distance (maximize physical distance from the source), and Shielding (employ lead aprons, thyroid shields, and structural barriers).",
            difficulty = "Easy",
            isPremium = false,
            isPublished = true,
            isDemo = true
          ),
          QuestionEntity(
            courseId = courseId,
            topicId = topic4Id,
            questionText = "[Demo Question] What is the primary function of the cathode filament in a modern diagnostic X-ray tube?",
            optionA = "To produce bremsstrahlung radiation upon deceleration",
            optionB = "To emit projectile electrons via thermionic emission",
            optionC = "To rotate and dissipate thermal heat accumulation",
            optionD = "To absorb low-energy stray photons from exiting the port",
            correctAnswer = "B",
            explanation = "The cathode consists of a tungsten wire filament heated by electrical current. When heated to incandescence, it releases a cloud of free electrons through the physical phenomenon known as thermionic emission.",
            difficulty = "Medium",
            isPremium = false,
            isPublished = true,
            isDemo = true
          ),
          QuestionEntity(
            courseId = courseId,
            topicId = topic4Id,
            questionText = "[Demo Question] Why is Tungsten (W) universally chosen as the primary target material for diagnostic X-ray tube anodes?",
            optionA = "Low atomic number and high malleability",
            optionB = "High atomic number (Z=74) and extremely high melting point (~3,422°C)",
            optionC = "High electrical resistance and low thermal conductivity",
            optionD = "Lightweight structure and low vapor pressure at room temperature",
            correctAnswer = "B",
            explanation = "Tungsten has an atomic number of 74, which enhances X-ray production efficiency, and an extremely high melting point of 3,422°C alongside strong thermal conductivity, enabling it to withstand intense electron bombardment.",
            difficulty = "Medium",
            isPremium = false,
            isPublished = true,
            isDemo = true
          ),
          QuestionEntity(
            courseId = courseId,
            topicId = topic4Id,
            questionText = "[Demo Question] In standard diagnostic radiography, approximately what percentage of projectile electron kinetic energy is converted into heat at the anode target rather than useful X-rays?",
            optionA = "~1%",
            optionB = "~50%",
            optionC = ">99%",
            optionD = "~25%",
            correctAnswer = "C",
            explanation = "At diagnostic operating potentials (below 150 kVp), greater than 99% of kinetic energy is converted into thermal heat through outer-shell electron interactions, with less than 1% resulting in X-ray production.",
            difficulty = "Medium",
            isPremium = false,
            isPublished = true,
            isDemo = true
          ),
          QuestionEntity(
            courseId = courseId,
            topicId = topic3Id,
            questionText = "[Demo Question] According to the Inverse Square Law, if a radiography student doubles their distance from an unshielded patient during mobile radiography, the radiation exposure rate will:",
            optionA = "Be halved (reduced by 50%)",
            optionB = "Decrease to one-fourth (1/4 or 25%) of the original value",
            optionC = "Double in intensity",
            optionD = "Remain unchanged because scatter is omnidirectional",
            correctAnswer = "B",
            explanation = "The Inverse Square Law dictates that radiation intensity is inversely proportional to the square of the distance (I ∝ 1/d²). Doubling the distance (2d) reduces radiation exposure to 1/(2)² = 1/4 of the initial value.",
            difficulty = "Medium",
            isPremium = false,
            isPublished = true,
            isDemo = true
          ),
          QuestionEntity(
            courseId = courseId,
            topicId = topic3Id,
            questionText = "[Demo Question] Which photon interaction with tissue is the primary source of scatter radiation, causing occupational radiation exposure and radiographic image fog?",
            optionA = "Photoelectric absorption",
            optionB = "Compton scattering",
            optionC = "Pair production",
            optionD = "Photodisintegration",
            correctAnswer = "B",
            explanation = "Compton scattering occurs when an incident photon interacts with an outer-shell orbital electron. The scattered photon changes direction and retains most energy, creating image fog and presenting the major source of occupational radiation exposure.",
            difficulty = "Hard",
            isPremium = false,
            isPublished = true,
            isDemo = true
          ),
          QuestionEntity(
            courseId = courseId,
            topicId = topic1Id,
            questionText = "[Demo Question] What is the standard SI unit for quantifying absorbed radiation dose in human tissue?",
            optionA = "Roentgen (R)",
            optionB = "Gray (Gy)",
            optionC = "Becquerel (Bq)",
            optionD = "Curie (Ci)",
            correctAnswer = "B",
            explanation = "The Gray (Gy), representing one Joule of energy absorbed per kilogram of matter (1 J/kg), is the SI unit of absorbed dose. The Sievert (Sv) is used for equivalent and effective dose.",
            difficulty = "Easy",
            isPremium = false,
            isPublished = true,
            isDemo = true
          ),
          QuestionEntity(
            courseId = courseId,
            topicId = topic5Id,
            questionText = "[Demo Question] Under professional radiology ethics, which core bioethical principle requires radiographers to obtain informed consent and respect a patient's personal decisions regarding their diagnostic procedure?",
            optionA = "Paternalism",
            optionB = "Autonomy",
            optionC = "Justice",
            optionD = "Non-disclosure",
            correctAnswer = "B",
            explanation = "Autonomy acknowledges the personal rights and self-determination of individuals to make decisions about their own healthcare, requiring healthcare practitioners to provide adequate procedural information and respect consent.",
            difficulty = "Medium",
            isPremium = false,
            isPublished = true,
            isDemo = true
          ),
          QuestionEntity(
            courseId = courseId,
            topicId = topic4Id,
            questionText = "[Demo Question] What is the clinical purpose of adding aluminum filtration to the diagnostic X-ray tube housing port?",
            optionA = "To enhance patient skin entrance dose",
            optionB = "To absorb low-energy, long-wavelength photons that would only increase patient skin dose without penetrating to the detector",
            optionC = "To magnify anatomical structures on the final radiograph",
            optionD = "To decelerate the rotation speed of the rotating anode",
            correctAnswer = "B",
            explanation = "Filtration 'hardens' the X-ray beam by selectively absorbing low-energy, non-penetrating photons that would otherwise be absorbed by patient skin and superficial tissues, markedly reducing patient radiation dose.",
            difficulty = "Hard",
            isPremium = false,
            isPublished = true,
            isDemo = true
          ),
          QuestionEntity(
            courseId = courseId,
            topicId = topic1Id,
            questionText = "[Demo Question] In radiographic quality assurance, any unwanted density or structural blemish on an image that does not correspond to genuine patient anatomy is termed:",
            optionA = "An anatomical variation",
            optionB = "A radiographic artifact",
            optionC = "Quantum mottle",
            optionD = "Penumbra",
            correctAnswer = "B",
            explanation = "An artifact is any visual density or irregularity on a radiograph that does not represent the real anatomical structure under examination, typically caused by motion, equipment malfunction, processing errors, or external foreign bodies.",
            difficulty = "Easy",
            isPremium = false,
            isPublished = true,
            isDemo = true
          )
        )

        questionDao.insertAllQuestions(demoQuestions)

        // 4. Seed Demo Mock Examination
        examDao.insertExam(
          ExamEntity(
            id = 1,
            courseId = courseId,
            title = "RAD 101 Mock Examination 1",
            description = "Comprehensive timed mock examination on Introductory Radiography principles, Röntgen's discovery, radiation protection, and tube components.",
            durationMinutes = 15,
            totalQuestions = 12,
            isPremium = false,
            isPublished = true
          )
        )

        // 5. Seed Demo Study Materials
        materialDao.insertMaterial(
          StudyMaterialEntity(
            courseId = courseId,
            topicId = topic2Id,
            title = "RAD 101: Lecture 1 - History & Discovery of X-rays",
            description = "Detailed introductory lecture note covering Wilhelm Röntgen's experiments with Crookes tubes, properties of cathode rays, and the first radiograph of Anna Bertha Ludwig's hand.",
            contentType = "Lecture Note",
            contentBody = """
              # RAD 101: Lecture 1 — History & Discovery of X-rays
              
              ## 1. Introduction
              Medical radiography traces its origins to November 8, 1895, when German physicist Wilhelm Conrad Röntgen made a serendipitous discovery in his laboratory at the University of Würzburg.
              
              ## 2. Experimental Apparatus
              Röntgen was investigating cathode rays passing through a gas-discharge Crookes-Hittorf tube wrapped in black cardboard to prevent visible light from escaping. Nearby lay a paper screen coated with barium platinocyanide.
              
              ## 3. The Observation
              Despite the tube being wrapped in opaque cardboard, the barium platinocyanide screen fluoresced brightly in the darkened room. Röntgen reasoned that an unknown invisible ray ('X' for unknown) was emanating from the tube and penetrating the cardboard.
              
              ## 4. Key Properties Discovered
              - Penetrates matter of varying density (bone absorbs more than soft tissue)
              - Causes chemical change on photographic emulsion
              - Causes fluorescence in certain crystalline materials
              - Travels in straight lines at the speed of light
              - Unaffected by magnetic or electrical fields (proving they are electromagnetic waves, not charged particles)
              
              ## 5. The First Human Radiograph
              On December 22, 1895, Röntgen produced the famous radiograph of his wife's (Anna Bertha Ludwig) hand, clearly displaying bones and her wedding ring. In 1901, Röntgen was awarded the inaugural Nobel Prize in Physics.
            """.trimIndent(),
            isPremium = false
          )
        )

        materialDao.insertMaterial(
          StudyMaterialEntity(
            courseId = courseId,
            topicId = topic3Id,
            title = "ALARA & Radiation Protection Guidelines for Radiographers",
            description = "Clinical safety summary detailing the ALARA philosophy, inverse square law calculations, lead shielding equivalents, and dose limits.",
            contentType = "Revision Summary",
            contentBody = """
              # Radiation Protection & ALARA Principles
              
              ## The ALARA Philosophy
              ALARA stands for **As Low As Reasonably Achievable**. Every radiographer has a professional and legal duty to minimize occupational, patient, and public radiation doses.
              
              ## The Three Cardinal Principles:
              1. **TIME**: Keep exposure time as short as clinically possible. Use short exposure times to also arrest involuntary patient motion.
              2. **DISTANCE**: Maximize distance from the scattering patient and tube port. According to the Inverse Square Law: Intensity 1 / Intensity 2 = (Distance 2 / Distance 1)^2.
                 Doubling the distance reduces exposure to one-fourth.
              3. **SHIELDING**: Place appropriate absorbing materials (lead aprons >= 0.25mm to 0.5mm Pb equivalent, thyroid shields, gonad shields) between the radiation source and exposed persons.
              
              ## Filtration & Collimation
              - **Filtration**: Absorbs soft low-energy photons that only increase skin entrance dose without penetrating to the detector.
              - **Collimation**: Restricts primary beam to the anatomical region of interest, reducing scatter generation and patient irradiated volume.
            """.trimIndent(),
            isPremium = false
          )
        )

        materialDao.insertMaterial(
          StudyMaterialEntity(
            courseId = courseId,
            topicId = topic4Id,
            title = "X-ray Tube Construction, Anode Heat Loading & Maintenance",
            description = "Comprehensive technical guide on rotating anodes, tungsten-rhenium focal tracks, space charge effect, line-focus principle, and tube rating charts.",
            contentType = "Lecture Note",
            contentBody = """
              # X-ray Tube Construction & Anode Dynamics (PREMIUM GUIDE)
              
              ## 1. Tube Envelope & Housing
              Diagnostic X-ray tubes utilize a vacuum borosilicate glass or metal envelope enclosed within a protective lead-lined housing containing dielectric oil for thermal dissipation and electrical insulation.
              
              ## 2. Cathode Assembly
              - Heated tungsten filaments (dual focus: large and small focal spots).
              - Molybdenum focusing cup with negative potential to counteract electrostatic repulsion and focus the electron stream onto the anode focal track.
              - **Space Charge Effect**: Cloud of electrons around filament prevents further emission at low kVp.
              
              ## 3. Rotating Anode Target
              - Rotor and stator induction motor rotating at 3,000 to 10,000 RPM.
              - Tungsten (90%) alloyed with Rhenium (10%) on a molybdenum/graphite base.
              - High atomic number (Z = 74) and high melting point (3,422 degrees Celsius).
              
              ## 4. Heat Dissipation & Anode Rating
              Over 99% of kinetic energy is converted to heat! Radiographers must follow warm-up procedures before high-technique exposures to avoid thermal cracking of the anode disc.
            """.trimIndent(),
            isPremium = true
          )
        )

        materialDao.insertMaterial(
          StudyMaterialEntity(
            courseId = courseId,
            topicId = topic1Id,
            title = "UNIMAID First Semester RAD 101 Past Questions & Solutions",
            description = "Solved past examination questions for Introduction to Radiography with examiner notes and scoring rubrics.",
            contentType = "Past Question",
            contentBody = """
              # UNIMAID College of Medical Sciences — Department of Radiography
              ## RAD 101 Past Examination Paper (Solved Archive)
              
              ### Section A: Multiple Choice Questions
              1. *Define the line-focus principle.*
                 - **Answer**: By angling the target anode (typically 7° to 20°), the effective focal spot size is made substantially smaller than the actual focal spot, achieving high spatial resolution while maintaining large heat-loading capacity.
                 
              2. *Explain the heel effect.*
                 - **Answer**: The radiation intensity is greater on the cathode side than on the anode side due to self-absorption of photons within the anode target material. Clinical rule: Position thicker anatomical structures toward the cathode side.
                 
              3. *Discuss the biological risks of low-dose diagnostic ionizing radiation.*
                 - **Answer**: Risks are categorized into deterministic (tissue reactions with dose thresholds, e.g. skin erythema) and stochastic effects (probabilistic without threshold, e.g. radiation-induced carcinogenesis and genetic mutations).
            """.trimIndent(),
            isPremium = true
          )
        )
      }

      // 4. Seed Course Libraries & Google Drive Resources
      val libraryDao = database.courseLibraryDao()
      if (libraryDao.getTotalLibrariesCount() == 0) {
        libraryDao.insertLibrary(
          CourseLibraryEntity(
            courseId = 1,
            courseCode = "RAD 101",
            name = "RAD 101 Lecture Library",
            description = "Official lecture notes, radiographic equipment blueprints, and study resources",
            externalUrl = "https://drive.google.com/drive/folders/1MedRad101OfficialArchive"
          )
        )
        libraryDao.insertLibrary(
          CourseLibraryEntity(
            courseId = 1,
            courseCode = "RAD 101",
            name = "Radiographic Positioning & Anatomy Drive",
            description = "High-definition diagnostic radiographic projection charts and clinical atlases",
            externalUrl = "https://drive.google.com/drive/folders/1RAD101PositioningAtlas"
          )
        )
      }

      // 5. Seed PHY 101 (General Physics for Medical Sciences) if not existing
      val existingPhy = academicDao.getCoursesList().find { it.code == "PHY 101" }
      if (existingPhy == null) {
        val phyId = academicDao.insertCourse(
          CourseEntity(
            departmentId = deptId,
            code = "PHY 101",
            title = "General Physics for Medical Radiography",
            level = "100 Level",
            semester = "First Semester",
            description = "Mechanics, wave properties, electromagnetic spectrum, optics, and radiation physics essential for diagnostic radiographic imaging."
          )
        )
        val phyTopic1 = academicDao.insertTopic(TopicEntity(courseId = phyId, name = "Electromagnetism & Waves", orderIndex = 1))
        val phyTopic2 = academicDao.insertTopic(TopicEntity(courseId = phyId, name = "Optics & Inverse Square Law", orderIndex = 2))

        libraryDao.insertLibrary(
          CourseLibraryEntity(
            courseId = phyId,
            courseCode = "PHY 101",
            name = "PHY 101 Medical Physics Digital Library",
            description = "Comprehensive laboratory guides, mechanics problem sets, and formula sheets",
            externalUrl = "https://drive.google.com/drive/folders/1PHY101MedicalPhysicsUNIMAID"
          )
        )

        materialDao.insertMaterial(
          StudyMaterialEntity(
            courseId = phyId,
            topicId = phyTopic1,
            title = "PHY 101 Course Handout: Inverse Square Law & Photon Attenuation",
            description = "Mathematical derivation and clinical application of the inverse square law in radiation dose management.",
            contentType = "Handout",
            contentBody = "# Inverse Square Law in Medical Physics\n\nRadiation intensity is inversely proportional to the square of the distance from the source:\n\n**I₁ / I₂ = (d₂)² / (d₁)²**\n\nDoubling the distance from an X-ray source reduces exposure to one-quarter.",
            isPremium = false
          )
        )

        questionDao.insertAllQuestions(
          listOf(
            QuestionEntity(
              courseId = phyId,
              topicId = phyTopic2,
              questionText = "[PHY 101] If the distance from an X-ray tube source is doubled, how does the radiation intensity change?",
              optionA = "Doubles (2x)",
              optionB = "Halves (1/2)",
              optionC = "Reduces to one-fourth (1/4)",
              optionD = "Quadruples (4x)",
              correctAnswer = "C",
              explanation = "According to the Inverse Square Law, intensity varies inversely as the square of the distance: (1/2)^2 = 1/4.",
              difficulty = "Easy",
              isPremium = false,
              isPublished = true,
              isDemo = false,
              questionType = "MULTIPLE_CHOICE",
              assessmentTitle = "PHY 101 Mid-Semester Quiz"
            ),
            QuestionEntity(
              courseId = phyId,
              topicId = phyTopic1,
              questionText = "[PHY 101] True or False: Electromagnetic radiation requires a material medium to propagate through space.",
              optionA = "True",
              optionB = "False",
              optionC = "",
              optionD = "",
              correctAnswer = "B",
              explanation = "Electromagnetic waves consist of oscillating electric and magnetic fields and propagate freely through a vacuum.",
              difficulty = "Easy",
              isPremium = false,
              isPublished = true,
              isDemo = false,
              questionType = "TRUE_FALSE",
              assessmentTitle = "PHY 101 Mid-Semester Quiz"
            )
          )
        )

        examDao.insertExam(
          ExamEntity(
            courseId = phyId,
            title = "PHY 101 Medical Physics Mock CBT",
            description = "Standard 100-level medical physics evaluation covering mechanics, waves, and radiation optics.",
            durationMinutes = 25,
            totalQuestions = 10,
            isPremium = false,
            isPublished = true
          )
        )
      }

      // 6. Seed BIO 101 (General Biology for Medical Sciences) if not existing
      val existingBio = academicDao.getCoursesList().find { it.code == "BIO 101" }
      if (existingBio == null) {
        val bioId = academicDao.insertCourse(
          CourseEntity(
            departmentId = deptId,
            code = "BIO 101",
            title = "General Biology & Human Cytology",
            level = "100 Level",
            semester = "First Semester",
            description = "Cellular physiology, radiobiology principles, genetic transcription, and histology fundamentals relevant to human health."
          )
        )
        val bioTopic1 = academicDao.insertTopic(TopicEntity(courseId = bioId, name = "Cell Architecture & Organelles", orderIndex = 1))
        val bioTopic2 = academicDao.insertTopic(TopicEntity(courseId = bioId, name = "Cellular Radiosensitivity & DNA Repair", orderIndex = 2))

        libraryDao.insertLibrary(
          CourseLibraryEntity(
            courseId = bioId,
            courseCode = "BIO 101",
            name = "BIO 101 Cytology & Genetics Drive",
            description = "Color microscopy slides, radiobiology handouts, and cell division tutorials",
            externalUrl = "https://drive.google.com/drive/folders/1BIO101CellBiologyDrive"
          )
        )

        materialDao.insertMaterial(
          StudyMaterialEntity(
            courseId = bioId,
            topicId = bioTopic1,
            title = "BIO 101 Departmental Handout: Bergonie & Tribondeau Law",
            description = "The biological response of tissues to ionizing radiation based on cell mitotic activity and differentiation.",
            contentType = "Handout",
            contentBody = "# Law of Bergonié and Tribondeau (1906)\n\nRadiosensitivity of living cells is directly proportional to their reproductive activity and inversely proportional to their degree of differentiation.",
            isPremium = false
          )
        )

        questionDao.insertAllQuestions(
          listOf(
            QuestionEntity(
              courseId = bioId,
              topicId = bioTopic2,
              questionText = "[BIO 101] According to the Law of Bergonié and Tribondeau, which cells exhibit the highest radiosensitivity?",
              optionA = "Mature nerve cells (neurons)",
              optionB = "Rapidly dividing stem cells and erythroblasts",
              optionC = "Adult muscle fibers",
              optionD = "Osteocytes (mature bone cells)",
              correctAnswer = "B",
              explanation = "Undifferentiated cells with high mitotic indices (such as hematopoietic stem cells) are the most sensitive to radiation damage.",
              difficulty = "Medium",
              isPremium = false,
              isPublished = true,
              isDemo = false,
              questionType = "MULTIPLE_CHOICE",
              assessmentTitle = "BIO 101 Continuous Assessment"
            ),
            QuestionEntity(
              courseId = bioId,
              topicId = bioTopic1,
              questionText = "[BIO 101] True or False: Mitochondria are known as the cellular powerhouse responsible for ATP synthesis.",
              optionA = "True",
              optionB = "False",
              optionC = "",
              optionD = "",
              correctAnswer = "A",
              explanation = "Mitochondria generate most of the chemical energy needed by cells through oxidative phosphorylation.",
              difficulty = "Easy",
              isPremium = false,
              isPublished = true,
              isDemo = false,
              questionType = "TRUE_FALSE",
              assessmentTitle = "BIO 101 Continuous Assessment"
            )
          )
        )

        examDao.insertExam(
          ExamEntity(
            courseId = bioId,
            title = "BIO 101 Human Biology CBT Quiz",
            description = "Continuous assessment test on cell structure, biological molecules, and tissue radiosensitivity.",
            durationMinutes = 20,
            totalQuestions = 10,
            isPremium = false,
            isPublished = true
          )
        )
      }

      // 7. Seed Default Admin & Student Accounts if not existing
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
