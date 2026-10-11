# AI Resume Parser

An intelligent, modern resume parsing and digital profile dashboard application. It utilizes the state-of-the-art **Google Gemini 3.5 Flash** model to parse unstructured resumes (PDF, DOCX, TXT) and transform them into structured, searchable digital profiles.

---

## 🌟 Key Features
- **Multimodal PDF Parsing**: Sends document bytes directly to Google Gemini for extremely high accuracy layout sensing.
- **Micro-extract DOCX Parser**: Fully working, zero-dependency engine to parse XML texts from `.docx` files.
- **Interactive Pasteurization**: Input or paste raw text content into the dashboard easily.
- **Prebuilt Testing Samples**: Click-to-load templates (Software Developer & Marketing Manager) built directly in for instantaneous testing in the streaming emulator.
- **Offline History Database**: Local SQLite Database (via Room) keeps records of past parses.
- **Structured Export**: Download and view extracted data structures as raw formatted JSON files instantly.

---

## 📁 Project Folder Structure

```
├── /app                  # Android Project Sources
│   ├── src/main
│   │   ├── AndroidManifest.xml # Core permissions (Internet)
│   │   ├── java/com/example
│   │   │   ├── MainActivity.kt # Entry point activity
│   │   │   ├── data      # DB, Repository, and Gemini API layer
│   │   │   ├── ui        # Custom screen UI and interactive dashboards
│   │   │   └── util      # File utilities (DOCX extracts & sample files)
│   └── build.gradle.kts  # Android configurations & versions
├── app.py                # Python Flask Web Application (Reference Local Play)
├── requirements.txt      # Python Dependencies Setup
├── metadata.json         # Google AI Studio configurations
└── README.md             # Project Guide (Beginner Friendly)
```

---

## 📱 Deliverable 1: Android Mobile Application (Emulated in Browser)

This is the primary application running inside your **Google AI Studio** emulator workspace. 

### How to run the Android App:
1. Ensure your Gemini API Key is entered in the **Secrets panel** of AI Studio (access this in the bottom left editor menu under **Secrets**).
2. The compilation should launch automatically inside the browser Streaming Emulator.
3. Choose the **Sample chips** on Tab 1 (e.g. "Software Engineer"), tap **Extract Information** — the prompt connects to your secure key, parses with Gemini AI, and displays the structured dashboard instantly!
4. Navigate to the **History** tab to see previously loaded profile logs cached in Room SQL!

---

## 💻 Deliverable 2: Local Python Flask Web Version

For users who want to run this application on their local computers as a standalone web app:

### Prerequisites:
- Python 3.9 or higher installed.

### Installation Steps:
1. **Prepare Python Environment**:
   ```bash
   python -m venv venv
   source venv/bin/activate  # On Windows: venv\Scripts\activate
   ```
2. **Install Required Libraries**:
   ```bash
   pip install -r requirements.txt
   ```
3. **Configure API Key**:
   Create a `.env` file in the root folder, and insert your Google Gemini API secure key:
   ```env
   GEMINI_API_KEY=your_actual_api_key_from_google_ai_studio
   ```
4. **Launch Server**:
   ```bash
   python app.py
   ```
5. **Open Browser**:
   Open [http://localhost:5000](http://localhost:5000) to upload, analyze, and save parsed resume models!

---

## 🤖 Beginner-Friendly Explanations of Code

### 1. Extractor Utility (`ResumeParserUtils.kt`)
- **PDF Extraction**: Reads the byte arrays of mobile documents directly, translates them to a Base64 string, and passes it securely to Gemini's native PDF multimodal reader using the REST interface.
- **DOCX Extraction**: Opens the Word file (.docx) dynamically as a Zip archive stream, parses `word/document.xml`, matches content within standard `<w:t>` text indicators via high-speed Regular Expressions, and builds the raw text profile representation without needing heavy third-party parsing tools.

### 2. Room Local Cache (`AppDatabase.kt`)
- **Entities**: Simple structures storing the parsed metadata (`rawJson`), a custom-tailored analysis title, and a date-specific epoch timestamp.
- **Reactive UI**: Implements standard Repository flow architecture. Room emits any updates reactively inside Kotlin `Flow`, updating Jetpack Compose views instantly without manual screen refreshing.
