-- ==============================================================================
-- MedRad Study Squad - Supabase Cloud Synchronization & Schema Alignment Script
-- ==============================================================================
-- Target Project ID: zvmiquntlxmiotlfpumc
-- Description:
--   1. Ensures all required columns exist with backward-compatible defaults.
--   2. Configures Row-Level Security (RLS) policies allowing public read (SELECT)
--      and authorized write (INSERT, UPDATE, DELETE) access.
--   3. Enables real-time replication for student updates.
-- ==============================================================================

-- 1. Courses Table
ALTER TABLE IF EXISTS courses ADD COLUMN IF NOT EXISTS department_id BIGINT DEFAULT 1;
ALTER TABLE IF EXISTS courses ADD COLUMN IF NOT EXISTS level TEXT DEFAULT '100 Level';
ALTER TABLE IF EXISTS courses ADD COLUMN IF NOT EXISTS semester TEXT DEFAULT 'First Semester';
ALTER TABLE IF EXISTS courses ADD COLUMN IF NOT EXISTS description TEXT DEFAULT '';

-- 2. Exams Table
ALTER TABLE IF EXISTS exams ADD COLUMN IF NOT EXISTS duration_minutes INTEGER DEFAULT 30;
ALTER TABLE IF EXISTS exams ADD COLUMN IF NOT EXISTS total_marks INTEGER DEFAULT 100;
ALTER TABLE IF EXISTS exams ADD COLUMN IF NOT EXISTS total_questions INTEGER DEFAULT 15;
ALTER TABLE IF EXISTS exams ADD COLUMN IF NOT EXISTS pass_mark INTEGER DEFAULT 50;
ALTER TABLE IF EXISTS exams ADD COLUMN IF NOT EXISTS pass_percentage INTEGER DEFAULT 50;
ALTER TABLE IF EXISTS exams ADD COLUMN IF NOT EXISTS is_published BOOLEAN DEFAULT true;
ALTER TABLE IF EXISTS exams ADD COLUMN IF NOT EXISTS is_premium BOOLEAN DEFAULT false;

-- 3. Questions Table
ALTER TABLE IF EXISTS questions ADD COLUMN IF NOT EXISTS exam_id UUID;
ALTER TABLE IF EXISTS questions ADD COLUMN IF NOT EXISTS course_id UUID;
ALTER TABLE IF EXISTS questions ADD COLUMN IF NOT EXISTS topic_id BIGINT DEFAULT 1;
ALTER TABLE IF EXISTS questions ADD COLUMN IF NOT EXISTS correct_answer TEXT DEFAULT 'A';
ALTER TABLE IF EXISTS questions ADD COLUMN IF NOT EXISTS explanation TEXT DEFAULT '';
ALTER TABLE IF EXISTS questions ADD COLUMN IF NOT EXISTS difficulty TEXT DEFAULT 'Medium';
ALTER TABLE IF EXISTS questions ADD COLUMN IF NOT EXISTS question_type TEXT DEFAULT 'MULTIPLE_CHOICE';
ALTER TABLE IF EXISTS questions ADD COLUMN IF NOT EXISTS assessment_title TEXT DEFAULT 'Continuous Assessment';
ALTER TABLE IF EXISTS questions ADD COLUMN IF NOT EXISTS time_limit_seconds INTEGER DEFAULT 60;
ALTER TABLE IF EXISTS questions ADD COLUMN IF NOT EXISTS marks INTEGER DEFAULT 1;
ALTER TABLE IF EXISTS questions ADD COLUMN IF NOT EXISTS image_url TEXT;
ALTER TABLE IF EXISTS questions ADD COLUMN IF NOT EXISTS is_premium BOOLEAN DEFAULT false;
ALTER TABLE IF EXISTS questions ADD COLUMN IF NOT EXISTS is_published BOOLEAN DEFAULT true;
ALTER TABLE IF EXISTS questions ADD COLUMN IF NOT EXISTS is_demo BOOLEAN DEFAULT false;
ALTER TABLE IF EXISTS questions ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT true;

-- 4. Study Materials Table
ALTER TABLE IF EXISTS study_materials ADD COLUMN IF NOT EXISTS topic_id BIGINT DEFAULT 1;
ALTER TABLE IF EXISTS study_materials ADD COLUMN IF NOT EXISTS content_type TEXT DEFAULT 'Lecture Note';
ALTER TABLE IF EXISTS study_materials ADD COLUMN IF NOT EXISTS content_body TEXT DEFAULT '';
ALTER TABLE IF EXISTS study_materials ADD COLUMN IF NOT EXISTS external_url TEXT;
ALTER TABLE IF EXISTS study_materials ADD COLUMN IF NOT EXISTS file_url TEXT;
ALTER TABLE IF EXISTS study_materials ADD COLUMN IF NOT EXISTS is_premium BOOLEAN DEFAULT false;
ALTER TABLE IF EXISTS study_materials ADD COLUMN IF NOT EXISTS is_published BOOLEAN DEFAULT true;

-- 5. Course Libraries Table (Google Drive Folders & External Curricula)
ALTER TABLE IF EXISTS course_libraries ADD COLUMN IF NOT EXISTS course_code TEXT DEFAULT 'RAD 101';
ALTER TABLE IF EXISTS course_libraries ADD COLUMN IF NOT EXISTS name TEXT;
ALTER TABLE IF EXISTS course_libraries ADD COLUMN IF NOT EXISTS title TEXT;
ALTER TABLE IF EXISTS course_libraries ADD COLUMN IF NOT EXISTS url TEXT;
ALTER TABLE IF EXISTS course_libraries ADD COLUMN IF NOT EXISTS external_url TEXT;
ALTER TABLE IF EXISTS course_libraries ADD COLUMN IF NOT EXISTS description TEXT DEFAULT '';
ALTER TABLE IF EXISTS course_libraries ADD COLUMN IF NOT EXISTS is_published BOOLEAN DEFAULT true;

-- 6. Announcements Table
ALTER TABLE IF EXISTS announcements ADD COLUMN IF NOT EXISTS author TEXT DEFAULT 'Department of Medical Radiography';
ALTER TABLE IF EXISTS announcements ADD COLUMN IF NOT EXISTS priority TEXT DEFAULT 'Normal';
ALTER TABLE IF EXISTS announcements ADD COLUMN IF NOT EXISTS target TEXT DEFAULT 'All Students';

-- 7. App Config Table (Key-Value Store)
CREATE TABLE IF NOT EXISTS app_config (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 8. Student Feedback Table
ALTER TABLE IF EXISTS student_feedback ADD COLUMN IF NOT EXISTS student_id UUID;
ALTER TABLE IF EXISTS student_feedback ADD COLUMN IF NOT EXISTS student_name TEXT DEFAULT 'Student';
ALTER TABLE IF EXISTS student_feedback ADD COLUMN IF NOT EXISTS student_email TEXT DEFAULT '';
ALTER TABLE IF EXISTS student_feedback ADD COLUMN IF NOT EXISTS category TEXT DEFAULT 'Academic Inquiry';
ALTER TABLE IF EXISTS student_feedback ADD COLUMN IF NOT EXISTS subject TEXT DEFAULT 'Academic Inquiry';
ALTER TABLE IF EXISTS student_feedback ADD COLUMN IF NOT EXISTS status TEXT DEFAULT 'PENDING';

-- 9. Profiles Table
ALTER TABLE IF EXISTS profiles ADD COLUMN IF NOT EXISTS full_name TEXT;
ALTER TABLE IF EXISTS profiles ADD COLUMN IF NOT EXISTS email TEXT;
ALTER TABLE IF EXISTS profiles ADD COLUMN IF NOT EXISTS matric_number TEXT;
ALTER TABLE IF EXISTS profiles ADD COLUMN IF NOT EXISTS level TEXT DEFAULT '100 Level';
ALTER TABLE IF EXISTS profiles ADD COLUMN IF NOT EXISTS department TEXT DEFAULT 'Medical Radiography';
ALTER TABLE IF EXISTS profiles ADD COLUMN IF NOT EXISTS faculty TEXT DEFAULT 'College of Medical Sciences';
ALTER TABLE IF EXISTS profiles ADD COLUMN IF NOT EXISTS app_role TEXT DEFAULT 'STUDENT';
ALTER TABLE IF EXISTS profiles ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT true;

-- 10. Exam Attempts Table
ALTER TABLE IF EXISTS exam_attempts ADD COLUMN IF NOT EXISTS user_id UUID;
ALTER TABLE IF EXISTS exam_attempts ADD COLUMN IF NOT EXISTS course_id UUID;
ALTER TABLE IF EXISTS exam_attempts ADD COLUMN IF NOT EXISTS total_questions INTEGER DEFAULT 15;
ALTER TABLE IF EXISTS exam_attempts ADD COLUMN IF NOT EXISTS time_spent_seconds INTEGER DEFAULT 0;
ALTER TABLE IF EXISTS exam_attempts ADD COLUMN IF NOT EXISTS is_passed BOOLEAN DEFAULT false;
ALTER TABLE IF EXISTS exam_attempts ADD COLUMN IF NOT EXISTS passed BOOLEAN DEFAULT false;
ALTER TABLE IF EXISTS exam_attempts ADD COLUMN IF NOT EXISTS percentage NUMERIC DEFAULT 0;

-- ==============================================================================
-- ROW-LEVEL SECURITY (RLS) POLICIES
-- ==============================================================================

-- Enable RLS on all tables
ALTER TABLE courses ENABLE ROW LEVEL SECURITY;
ALTER TABLE exams ENABLE ROW LEVEL SECURITY;
ALTER TABLE questions ENABLE ROW LEVEL SECURITY;
ALTER TABLE study_materials ENABLE ROW LEVEL SECURITY;
ALTER TABLE course_libraries ENABLE ROW LEVEL SECURITY;
ALTER TABLE announcements ENABLE ROW LEVEL SECURITY;
ALTER TABLE app_config ENABLE ROW LEVEL SECURITY;
ALTER TABLE student_feedback ENABLE ROW LEVEL SECURITY;
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE exam_attempts ENABLE ROW LEVEL SECURITY;

-- Drop existing restrictive policies if present to prevent duplicate policy errors
DROP POLICY IF EXISTS "Public Read Courses" ON courses;
DROP POLICY IF EXISTS "Allow Write Courses" ON courses;
DROP POLICY IF EXISTS "Public Read Exams" ON exams;
DROP POLICY IF EXISTS "Allow Write Exams" ON exams;
DROP POLICY IF EXISTS "Public Read Questions" ON questions;
DROP POLICY IF EXISTS "Allow Write Questions" ON questions;
DROP POLICY IF EXISTS "Public Read Study Materials" ON study_materials;
DROP POLICY IF EXISTS "Allow Write Study Materials" ON study_materials;
DROP POLICY IF EXISTS "Public Read Course Libraries" ON course_libraries;
DROP POLICY IF EXISTS "Allow Write Course Libraries" ON course_libraries;
DROP POLICY IF EXISTS "Public Read Announcements" ON announcements;
DROP POLICY IF EXISTS "Allow Write Announcements" ON announcements;
DROP POLICY IF EXISTS "Public Read App Config" ON app_config;
DROP POLICY IF EXISTS "Allow Write App Config" ON app_config;
DROP POLICY IF EXISTS "Allow Insert Student Feedback" ON student_feedback;
DROP POLICY IF EXISTS "Public Read Profiles" ON profiles;
DROP POLICY IF EXISTS "Allow Write Profiles" ON profiles;
DROP POLICY IF EXISTS "Allow Insert Exam Attempts" ON exam_attempts;

-- Read policies: Academic content is accessible to all students (anon and authenticated)
CREATE POLICY "Public Read Courses" ON courses FOR SELECT USING (true);
CREATE POLICY "Public Read Exams" ON exams FOR SELECT USING (true);
CREATE POLICY "Public Read Questions" ON questions FOR SELECT USING (true);
CREATE POLICY "Public Read Study Materials" ON study_materials FOR SELECT USING (true);
CREATE POLICY "Public Read Course Libraries" ON course_libraries FOR SELECT USING (true);
CREATE POLICY "Public Read Announcements" ON announcements FOR SELECT USING (true);
CREATE POLICY "Public Read App Config" ON app_config FOR SELECT USING (true);
CREATE POLICY "Public Read Profiles" ON profiles FOR SELECT USING (true);

-- Write policies: Enable app updates and admin content publication
CREATE POLICY "Allow Write Courses" ON courses FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow Write Exams" ON exams FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow Write Questions" ON questions FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow Write Study Materials" ON study_materials FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow Write Course Libraries" ON course_libraries FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow Write Announcements" ON announcements FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow Write App Config" ON app_config FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow Insert Student Feedback" ON student_feedback FOR INSERT WITH CHECK (true);
CREATE POLICY "Allow Write Profiles" ON profiles FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow Insert Exam Attempts" ON exam_attempts FOR ALL USING (true) WITH CHECK (true);

-- Enable Realtime publication for tables so WebSocket instant notifications work
ALTER PUBLICATION supabase_realtime ADD TABLE announcements;
ALTER PUBLICATION supabase_realtime ADD TABLE app_config;
ALTER PUBLICATION supabase_realtime ADD TABLE courses;
ALTER PUBLICATION supabase_realtime ADD TABLE exams;
ALTER PUBLICATION supabase_realtime ADD TABLE questions;
ALTER PUBLICATION supabase_realtime ADD TABLE study_materials;
ALTER PUBLICATION supabase_realtime ADD TABLE course_libraries;
