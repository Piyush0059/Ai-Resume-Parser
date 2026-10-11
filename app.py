import os
import json
from flask import Flask, request, jsonify, render_template_string
import pdfplumber
import docx
import google.generativeai as genai
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

app = Flask(__name__)

# Configure Gemini API Key
# Beginners: Create a .env file and set GEMINI_API_KEY=your_actual_api_key
api_key = os.getenv("GEMINI_API_KEY")
if api_key:
    genai.configure(api_key=api_key)

def extract_text_from_pdf(file_path):
    """Utility to extract all text content from a PDF document using pdfplumber."""
    text = ""
    try:
        with pdfplumber.open(file_path) as pdf:
            for page in pdf.pages:
                page_text = page.extract_text()
                if page_text:
                    text += page_text + "\n"
    except Exception as e:
        print(f"Error reading PDF: {e}")
    return text

def extract_text_from_docx(file_path):
    """Utility to extract all paragraph text from a Word DOCX document."""
    text = ""
    try:
        doc = docx.Document(file_path)
        for para in doc.paragraphs:
            if para.text:
                text += para.text + "\n"
    except Exception as e:
        print(f"Error reading DOCX: {e}")
    return text

# Modern HTML/CSS/JS single-page dashboard for the web interface
HTML_TEMPLATE = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AI Resume Parser</title>
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
    <style>
        :root {
            --primary: #4f46e5;
            --primary-hover: #4338ca;
            --bg-gradient: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
            --card-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
        }
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: var(--bg-gradient);
            min-height: 100vh;
            margin: 0;
            color: #1f2937;
        }
        .container {
            max-width: 1100px;
            margin: 0 auto;
            padding: 40px 20px;
        }
        header {
            text-align: center;
            margin-bottom: 40px;
        }
        header h1 {
            font-size: 2.8rem;
            font-weight: 800;
            margin-bottom: 10px;
            color: #312e81;
        }
        header p {
            font-size: 1.1rem;
            color: #4b5563;
        }
        .grid {
            display: grid;
            grid-template-columns: 1fr;
            gap: 30px;
        }
        @media (min-width: 768px) {
            .grid {
                grid-template-columns: 1fr 1fr;
            }
        }
        .card {
            background: #ffffff;
            border-radius: 16px;
            padding: 30px;
            box-shadow: var(--card-shadow);
        }
        .upload-section {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            border: 3.5px dashed #e5e7eb;
            border-radius: 12px;
            padding: 40px 20px;
            text-align: center;
            cursor: pointer;
            transition: border-color 0.2s, background-color 0.2s;
        }
        .upload-section:hover {
            border-color: var(--primary);
            background-color: #f5f3ff;
        }
        .upload-section i {
            font-size: 48px;
            color: var(--primary);
            margin-bottom: 15px;
        }
        #file-input {
            display: none;
        }
        .btn {
            background-color: var(--primary);
            color: white;
            border: none;
            padding: 12px 28px;
            font-size: 1rem;
            font-weight: 600;
            border-radius: 8px;
            cursor: pointer;
            width: 100%;
            transition: background-color 0.15s, transform 0.1s;
            margin-top: 20px;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 10.dp;
        }
        .btn:hover {
            background-color: var(--primary-hover);
            transform: translateY(-1.dp);
        }
        .btn:active {
            transform: translateY(0);
        }
        .btn:disabled {
            background-color: #9ca3af;
            cursor: not-allowed;
        }
        .status-box {
            text-align: center;
            margin-top: 15px;
            font-weight: 500;
            color: var(--primary);
        }
        .results-section {
            opacity: 0.3;
            transition: opacity 0.3s;
        }
        .results-section.active {
            opacity: 1;
        }
        .profile-hdr {
            display: flex;
            align-items: center;
            gap: 15px;
            margin-bottom: 25px;
            border-bottom: 2px solid #f3f4f6;
            padding-bottom: 15px;
        }
        .profile-hdr i {
            font-size: 28px;
            color: var(--primary);
            background: #e0e7ff;
            padding: 12px;
            border-radius: 50%;
        }
        .contact-info {
            display: grid;
            grid-template-columns: 1fr;
            gap: 10px;
            margin-bottom: 25px;
        }
        .contact-item {
            display: flex;
            align-items: center;
            gap: 10px;
            font-size: 0.95rem;
        }
        .contact-item i {
            color: var(--primary);
        }
        .skills-tag {
            background-color: #ecefef;
            padding: 6px 12px;
            border-radius: 6px;
            font-size: 0.85rem;
            font-weight: 500;
            display: inline-block;
            margin: 4px;
        }
        .section-title {
            font-weight: 700;
            font-size: 1.15rem;
            margin-top: 20px;
            margin-bottom: 10px;
            color: #3730a3;
            display: flex;
            align-items: center;
            gap: 8px;
        }
        .list-items {
            list-style-type: none;
            padding-left: 0;
            margin: 0;
        }
        .list-items li {
            position: relative;
            padding-left: 20px;
            margin-bottom: 8px;
            font-size: 0.95rem;
        }
        .list-items li::before {
            content: "•";
            color: var(--primary);
            font-weight: bold;
            font-size: 1.2rem;
            position: absolute;
            left: 0;
            top: -2px;
        }
        .footer-json {
            margin-top: 30px;
            text-align: right;
        }
        .download-btn {
            background-color: #10b981;
            color: white;
            border: none;
            padding: 8px 16px;
            border-radius: 6px;
            cursor: pointer;
            font-size: 0.9rem;
            font-weight: 500;
            transition: background-color 0.2s;
        }
        .download-btn:hover {
            background-color: #059669;
        }
    </style>
</head>
<body>
    <div class="container">
        <header>
            <h1><i class="fa-solid fa-wand-magic-sparkles"></i> AI Resume Parser</h1>
            <p>Upload a document and let Gemini AI organize and extract structured insights instantly</p>
        </header>

        <div class="grid">
            <!-- Left Side: Upload & Action -->
            <div class="card">
                <form id="upload-form" enctype="multipart/form-data">
                    <div class="upload-section" onclick="document.getElementById('file-input').click()">
                        <i class="fa-solid fa-cloud-arrow-up"></i>
                        <h3 id="upload-label">Choose PDF, DOCX or TXT</h3>
                        <p>Limit 16MB • Drag and drop files directly</p>
                        <input type="file" id="file-input" name="file" accept=".pdf,.docx,.txt" onchange="fileSelected(this)">
                    </div>
                    <button type="submit" id="parse-btn" class="btn" disabled>
                        <i class="fa-solid fa-bolt"></i> Extract Information
                    </button>
                </form>
                <div id="status-message" class="status-box" style="display: none;"></div>
            </div>

            <!-- Right Side: Extract Dashboard Display -->
            <div id="results-card" class="card results-section">
                <div id="initial-state" style="text-align: center; padding: 40px 0;">
                    <i class="fa-solid fa-folder-open" style="font-size: 54px; color: #d1d5db; margin-bottom: 15px;"></i>
                    <h3>No Data Extracted</h3>
                    <p style="color: #6b7280;">Once parsing completes, the digital profile cards will load right here.</p>
                </div>

                <div id="result-content" style="display: none;">
                    <div class="profile-hdr">
                        <i class="fa-solid fa-user-tie"></i>
                        <div>
                            <h2 id="res-name" style="margin: 0; font-size: 1.5rem;">John Doe</h2>
                            <span style="font-size: 0.85rem; color: #6b7280;">AI Structurized Profile</span>
                        </div>
                    </div>

                    <div class="contact-info">
                        <div class="contact-item">
                            <i class="fa-solid fa-envelope"></i>
                            <span id="res-email">Email Address</span>
                        </div>
                        <div class="contact-item">
                            <i class="fa-solid fa-phone"></i>
                            <span id="res-phone">Phone Number</span>
                        </div>
                    </div>

                    <div class="section-title"><i class="fa-solid fa-brain"></i> Core Skills</div>
                    <div id="res-skills" style="margin-bottom: 20px;"></div>

                    <div class="section-title"><i class="fa-solid fa-briefcase"></i> Experience</div>
                    <ul id="res-experience" class="list-items"></ul>

                    <div class="section-title"><i class="fa-solid fa-graduation-cap"></i> Education</div>
                    <ul id="res-education" class="list-items"></ul>

                    <div class="section-title"><i class="fa-solid fa-folder-tree"></i> Projects</div>
                    <ul id="res-projects" class="list-items"></ul>

                    <div class="section-title"><i class="fa-solid fa-award"></i> Certifications</div>
                    <ul id="res-certs" class="list-items"></ul>

                    <div class="footer-json">
                        <button class="download-btn" onclick="downloadJson()"><i class="fa-solid fa-download"></i> Save Extracted JSON</button>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script>
        let currentExtractedData = null;

        function fileSelected(input) {
            const label = document.getElementById('upload-label');
            if (input.files &amp;&amp; input.files[0]) {
                label.textContent = input.files[0].name;
                document.getElementById('parse-btn').removeAttribute('disabled');
            } else {
                label.textContent = "Choose PDF, DOCX or TXT";
                document.getElementById('parse-btn').setAttribute('disabled', 'true');
            }
        }

        document.getElementById('upload-form').addEventListener('submit', async (e) => {
            e.preventDefault();
            const fileInput = document.getElementById('file-input');
            if (!fileInput.files || !fileInput.files[0]) return;

            const parseBtn = document.getElementById('parse-btn');
            const statusMsg = document.getElementById('status-message');
            const resultsCard = document.getElementById('results-card');

            parseBtn.setAttribute('disabled', 'true');
            statusMsg.style.display = 'block';
            statusMsg.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Reading & Analyzing with Gemini...';

            const formData = new FormData();
            formData.append('file', fileInput.files[0]);

            try {
                const response = await fetch('/parse', {
                    method: 'POST',
                    body: formData
                });
                const result = await response.json();

                if (result.success) {
                    currentExtractedData = result.data;
                    displayResults(result.data);
                    statusMsg.innerHTML = '<span style="color: #10b981;"><i class="fa-solid fa-circle-check"></i> Analysis Completed!</span>';
                } else {
                    statusMsg.innerHTML = `<span style="color: #ef4444;"><i class="fa-solid fa-circle-xmark"></i> ${result.error}</span>`;
                }
            } catch (err) {
                statusMsg.innerHTML = '<span style="color: #ef4444;"><i class="fa-solid fa-circle-xmark"></i> Connection error occurred.</span>';
            } finally {
                parseBtn.removeAttribute('disabled');
            }
        });

        function displayResults(data) {
            document.getElementById('initial-state').style.display = 'none';
            document.getElementById('result-content').style.display = 'block';
            document.getElementById('results-card').classList.add('active');

            document.getElementById('res-name').textContent = data.name || 'Not Found';
            document.getElementById('res-email').textContent = data.email || 'Not Found';
            document.getElementById('res-phone').textContent = data.phone || 'Not Found';

            // Populate Skills
            const skillsDiv = document.getElementById('res-skills');
            skillsDiv.innerHTML = '';
            if (data.skills &amp;&amp; data.skills.length > 0) {
                data.skills.forEach(skill => {
                    const span = document.createElement('span');
                    span.className = 'skills-tag';
                    span.textContent = skill;
                    skillsDiv.appendChild(span);
                });
            } else {
                skillsDiv.innerHTML = '<em style="color:#9ca3af">None identified</em>';
            }

            // Simple helper lists
            populateList('res-experience', data.experience);
            populateList('res-education', data.education);
            populateList('res-projects', data.projects);
            populateList('res-certs', data.certifications);
        }

        function populateList(id, list) {
            const ul = document.getElementById(id);
            ul.innerHTML = '';
            if (list &amp;&amp; list.length > 0) {
                list.forEach(item => {
                    const li = document.createElement('li');
                    li.textContent = item;
                    ul.appendChild(li);
                });
            } else {
                ul.innerHTML = '<li style="list-style:none; color:#9ca3af; padding-left:0;">None identified</li>';
            }
        }

        function downloadJson() {
            if (!currentExtractedData) return;
            const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(currentExtractedData, null, 2));
            const dlAnchorElem = document.createElement('a');
            dlAnchorElem.setAttribute("href", dataStr);
            dlAnchorElem.setAttribute("download", `resume_${currentExtractedData.name || 'parsed'}.json`);
            dlAnchorElem.click();
        }
    </script>
</body>
</html>
"""

@app.route('/')
def home():
    return render_template_string(HTML_TEMPLATE)

@app.route('/parse', methods=['POST'])
def parse_resume():
    if 'file' not in request.files:
        return jsonify({"success": False, "error": "No file part in the request"}), 400

    file = request.files['file']
    if file.filename == '':
        return jsonify({"success": False, "error": "No file selected"}), 400

    filename = file.filename.lower()
    
    # Save file temporarily to extract text
    temp_path = os.path.join("/tmp", file.filename)
    os.makedirs("/tmp", exist_ok=True)
    file.save(temp_path)

    # Extract text content
    extracted_text = ""
    if filename.endswith('.pdf'):
        extracted_text = extract_text_from_pdf(temp_path)
    elif filename.endswith('.docx'):
        extracted_text = extract_text_from_docx(temp_path)
    elif filename.endswith('.txt'):
        try:
            with open(temp_path, 'r', encoding='utf-8') as f:
                extracted_text = f.read()
        except Exception as e:
            return jsonify({"success": False, "error": f"Error reading text file: {e}"}), 500
    else:
        return jsonify({"success": False, "error": "Unsupported file format. Please upload PDF, DOCX or TXT files."}), 400

    # Clean intermediate temp file
    if os.path.exists(temp_path):
        os.remove(temp_path)

    if not extracted_text.strip():
        return jsonify({"success": False, "error": "Failed to extract clean text from document."}), 400

    # Execute Gemini API call
    try:
        if not api_key:
            return jsonify({
                "success": False, 
                "error": "Google Gemini API Key is not configured on this server. Please read the documentation to set up the environment variables."
            }), 500

        model = genai.GenerativeModel('gemini-3.5-flash')
        
        prompt = f"""
        Analyze the resume and return only valid JSON in this format:

        {{
          "name": "Candidate Full Name",
          "email": "Email Address",
          "phone": "Phone Number",
          "skills": ["Skill 1", "Skill 2"],
          "education": ["Education details 1", "Education details 2"],
          "experience": ["Work Experience 1", "Work Experience 2"],
          "certifications": ["Certification 1", "Certification 2"],
          "projects": ["Project 1", "Project 2"]
        }}

        Here is the extracted resume text to analyze:
        {extracted_text}
        """

        response = model.generate_content(
            prompt,
            generation_config={"response_mime_type": "application/json"}
        )

        # Parse AI response safely
        raw_response = response.text.strip()
        
        # Strip markdown tags if any
        if raw_response.startswith("```"):
            raw_response = raw_response.split("```json")[-1].split("```")[0].strip()

        data = json.loads(raw_response)
        return jsonify({"success": True, "data": data})

    except Exception as e:
        return jsonify({"success": False, "error": f"Gemini Parsing Error: {str(e)}"}), 500

if __name__ == '__main__':
    # Run server locally on port 5000
    app.run(host='0.0.0.0', port=5000, debug=True)
