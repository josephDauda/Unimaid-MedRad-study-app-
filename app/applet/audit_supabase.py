import urllib.request, json, ssl

url = "https://zvmiquntlxmiotlfpumc.supabase.co"
key = "sb_publishable_-aKCMGYwl5T91Dkkzi4R6w_kJ2uqxrC"
headers = {"apikey": key, "Authorization": "Bearer " + key}

ctx = ssl.create_default_context()

tables = [
    "courses", "exams", "questions", "study_materials", "course_libraries",
    "announcements", "notifications", "notification_reads", "app_config",
    "profiles", "exam_attempts", "student_feedback", "messages", "saved_questions"
]

print("=== CHECKING TABLES ===")
for t in tables:
    req = urllib.request.Request(f"{url}/rest/v1/{t}?limit=1", headers=headers)
    try:
        with urllib.request.urlopen(req, context=ctx) as resp:
            data = json.loads(resp.read().decode())
            print(f"TABLE {t}: EXISTS (HTTP 200). Records: {len(data)}")
            if len(data) > 0:
                print(f"  Sample keys: {list(data[0].keys())}")
                print(f"  Sample record: {data[0]}")
            else:
                # Let's inspect columns using an error probe or OPTIONS
                pass
    except urllib.error.HTTPError as e:
        body = e.read().decode()
        print(f"TABLE {t}: HTTP {e.code} - {body}")
    except Exception as e:
        print(f"TABLE {t}: Error {e}")

print("\n=== CHECKING RPC FUNCTIONS ===")
rpcs = [
    "start_exam_attempt", "save_exam_answer", "submit_exam_attempt",
    "is_admin", "get_exam_questions", "sync_content"
]
for rpc in rpcs:
    req = urllib.request.Request(f"{url}/rest/v1/rpc/{rpc}", headers=headers, data=b"{}", method="POST")
    req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req, context=ctx) as resp:
            data = resp.read().decode()
            print(f"RPC {rpc}: EXISTS. Response: {data}")
    except urllib.error.HTTPError as e:
        body = e.read().decode()
        print(f"RPC {rpc}: HTTP {e.code} - {body}")
    except Exception as e:
        print(f"RPC {rpc}: Error {e}")
